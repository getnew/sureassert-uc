package com.sureassert.uc.builder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.sureassert.uc.annotation.Exemplar;
import org.sureassert.uc.annotation.Exemplars;
import org.sureassert.uc.annotation.MultiUseCase;
import org.sureassert.uc.annotation.UseCase;

import com.sureassert.uc.EclipseUtils;
import com.sureassert.uc.internal.SourceFile;
import com.sureassert.uc.runtime.BasicUtils;

public class InsertUseCaseActionHandler extends AbstractHandler {

	@SuppressWarnings("unchecked")
	public Object execute(ExecutionEvent event) throws ExecutionException {

		try {
			ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection();
			if (selection == null)
				return null;
			if (selection instanceof IStructuredSelection) {
				// Selected method in explorer
				IStructuredSelection strucSelection = (IStructuredSelection) selection;
				for (Iterator<Object> iterator = strucSelection.iterator(); iterator.hasNext();) {
					Object element = iterator.next();
					if (element != null && element instanceof IMethod) {
						addUseCase((IMethod) element, true);
					}
				}
			} else if (selection instanceof ITextSelection) {
				// Selected/cursor over text
				ITextSelection textSelection = (ITextSelection) selection;
				IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				if (activePage != null) {
					IEditorPart editor = activePage.getActiveEditor();
					ITypeRoot typeRoot = JavaUI.getEditorInputTypeRoot(editor.getEditorInput());
					if (!typeRoot.isConsistent())
						typeRoot.makeConsistent(null);
					boolean addedUC = false;

					// If cursor is over method name: quick way
					IJavaElement[] selectedEls = typeRoot.codeSelect(textSelection.getOffset(), textSelection.getLength());
					if (selectedEls != null && selectedEls.length > 0) {
						for (IJavaElement element : selectedEls) {
							if (element instanceof IMethod) {
								addUseCase((IMethod) element, true);
								addedUC = true;
							}
						}
					}

					// Otherwise, check bounds of each method if cursor is inside method
					if (!addedUC && typeRoot instanceof ICompilationUnit) {
						int cursorIdx = textSelection.getOffset();
						ICompilationUnit javaUnit = (ICompilationUnit) typeRoot;
						for (IType javaType : javaUnit.getAllTypes()) {
							for (IMethod method : javaType.getMethods()) {
								int methodIdx = method.getSourceRange().getOffset();
								if (cursorIdx >= methodIdx && //
										cursorIdx <= methodIdx + method.getSourceRange().getLength()) {
									addUseCase(method, true);
									addedUC = true;
								}
							}
						}
					}
					if (!addedUC)
						EclipseUtils.displayDialog("No method selected", "Cannot add UseCase.  Please select or ensure the cursor is placed within a method.", true, IStatus.WARNING);
				}
			}
			return null;
		} catch (Exception e) {
			EclipseUtils.reportError(e);
			return null;
		}
	}

	private void addUseCase(IMethod sourceMethod, boolean isExemplar) throws JavaModelException {

		if (!sourceMethod.isBinary()) {
			try {
				// sourceMethod.getCompilationUnit().becomeWorkingCopy(null);

				int numArgs = sourceMethod.getNumberOfParameters();
				String[] paramTypes = sourceMethod.getParameterTypes();
				// JavaPathData javaPathData = new JavaPathData();
				// IFile file =
				// javaPathData.getFileQuick(sourceMethod.getCompilationUnit().getPath(), //
				// sourceMethod.getJavaProject().getProject().getWorkspace());
				SourceFile sourceFile = new SourceFile(sourceMethod.getCompilationUnit().getSource());

				String source = sourceFile.getSource();
				StringBuilder spacing = new StringBuilder();
				IAnnotation ucAn = isExemplar ? EclipseUtils.getAnnotation(sourceMethod, Exemplar.class.getSimpleName()) : //
				EclipseUtils.getAnnotation(sourceMethod, UseCase.class.getSimpleName());
				IAnnotation mucAn = isExemplar ? EclipseUtils.getAnnotation(sourceMethod, Exemplars.class.getSimpleName()) : //
				EclipseUtils.getAnnotation(sourceMethod, MultiUseCase.class.getSimpleName());
				int namePos = sourceMethod.getNameRange().getOffset();
				int nameLineNum = sourceFile.getLineNum(namePos);
				int nameLinePos = sourceFile.getPosition(nameLineNum);
				for (int i = nameLinePos; i < namePos && Character.isWhitespace(source.charAt(i)); i++) {
					spacing.append(source.charAt(i));
				}
				if (ucAn == null && mucAn == null) {
					// No existing UCs
					TextEdit edit = new InsertEdit(nameLinePos, getInsertString(spacing.toString(), numArgs, paramTypes, isExemplar) + "\n");
					sourceMethod.getCompilationUnit().applyTextEdit(edit, null);
				} else if (ucAn != null) {
					// 1 existing UC
					int anPos = ucAn.getNameRange().getOffset();
					int anLineNum = sourceFile.getLineNum(anPos);
					int anLinePos = sourceFile.getPosition(anLineNum);
					TextEdit edit2 = new InsertEdit(getLastNonWhitespacePos(source, nameLinePos - 1), //
							",\n" + getInsertString(spacing.toString(), numArgs, paramTypes, isExemplar) + " })");
					TextEdit edit1 = new InsertEdit(anLinePos, getMUCInsertString(spacing.toString(), numArgs, paramTypes, isExemplar));
					MultiTextEdit multiEdit = new MultiTextEdit();
					multiEdit.addChild(edit2);
					multiEdit.addChild(edit1);
					sourceMethod.getCompilationUnit().applyTextEdit(multiEdit, null);

				} else if (mucAn != null) {
					// Existing MUCs
					int insertPos = getLastNonWhitespacePos(source, nameLinePos - 1);
					TextEdit edit1 = new InsertEdit(insertPos, //
							",\n" + getInsertString(spacing.toString(), numArgs, paramTypes, isExemplar) + " })");
					int lastBracketPos = getWorkbacktoSecondCloseBracketPos(source, insertPos);
					TextEdit edit2 = new DeleteEdit(lastBracketPos, insertPos - lastBracketPos);
					MultiTextEdit multiEdit = new MultiTextEdit();
					multiEdit.addChild(edit2);
					multiEdit.addChild(edit1);
					sourceMethod.getCompilationUnit().applyTextEdit(multiEdit, null);

				}
			} catch (Exception e) {
				EclipseUtils.reportError(e);
			} finally {
				// sourceMethod.getCompilationUnit().commitWorkingCopy(false, null);
			}
		}
	}

	private int getLastNonWhitespacePos(String str, int pos) {

		int i = pos;
		for (; i >= 0 && Character.isWhitespace(str.charAt(i)); i--)
			;
		return i + 1;
	}

	private int getWorkbacktoSecondCloseBracketPos(String str, int pos) {

		int i = pos;
		for (; i >= 0 && str.charAt(i) != ')'; i--)
			;
		i--;
		for (; i >= 0 && str.charAt(i) != ')'; i--)
			;
		return i + 1;
	}

	protected String getInsertString(String spacer, int numArgs, String[] paramTypes, boolean isExemplar) {

		return spacer + (isExemplar ? "@Exemplar(" : "@UseCase(") + getArgs(numArgs, paramTypes) + "expect=\"\")";
	}

	protected String getMUCInsertString(String spacer, int numArgs, String[] paramTypes, boolean isExemplar) {

		return spacer + (isExemplar ? "@Exemplars(set={\n" : "@MultiUseCase(usecases={\n");
		// + getInsertString(spacer, numArgs, paramTypes) + ",";
	}

	private String getArgs(int numArgs, String[] paramTypes) {

		if (numArgs == 0)
			return "";
		StringBuilder args = new StringBuilder("args={");
		for (int i = 0; i < numArgs; i++) {
			String srcType = BasicUtils.getTypeNameForSrc(//
					paramTypes[i], new HashMap<String, String>(), new HashSet<String>());
			args.append("\"" + BasicUtils.getDefaultSINValueStrForClass(srcType) + "\"");
			if (i < numArgs - 1)
				args.append(",");
		}
		args.append("}, ");
		return args.toString();
	}
}
