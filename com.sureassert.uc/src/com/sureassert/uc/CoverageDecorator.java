package com.sureassert.uc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import com.sureassert.uc.builder.CoveragePrinter;
import com.sureassert.uc.internal.JavaPathData;
import com.sureassert.uc.internal.SaUCPreferences;
import com.sureassert.uc.runtime.PersistentDataFactory;
import com.sureassert.uc.runtime.Timer;

// Class extends LabelProvider because LabelProvider
// provides methods for getting images and text labels from objects 
public class CoverageDecorator extends LabelProvider implements ILightweightLabelDecorator {

	public static final String COVERAGE_DECORATOR_ID = "com.sureassert.uc.coverageDecorator";

	private static final ImageDescriptor COVERAGE_OK_IMAGE_DESCRIPTOR = //
	ImageDescriptor.createFromFile(CoverageDecorator.class, "/icons/coverage-ok.png");
	private static final ImageDescriptor COVERAGE_WARN_IMAGE_DESCRIPTOR = //
	ImageDescriptor.createFromFile(CoverageDecorator.class, "/icons/coverage-warning.png");
	private static final ImageDescriptor COVERAGE_ERROR_IMAGE_DESCRIPTOR = //
	ImageDescriptor.createFromFile(CoverageDecorator.class, "/icons/coverage-error.png");

	public static final String MARKER_TYPE_COVERAGE_REPORT = "com.sureassert.uc.suacCoverageReport";

	public CoverageDecorator() {

		super();
	}

	public void decorate(Object resourceObj, IDecoration decoration) {

		if (!SaUCPreferences.getIsCoverageFileDecorationEnabled() && //
				!SaUCPreferences.getIsCoverageProblemsEnabled())
			return;

		IResource resource = getResource(resourceObj);
		if (resource == null)
			return;

		IJavaElement javaEl = JavaCore.create(resource);
		PersistentDataFactory pdf = PersistentDataFactory.getInstance();
		
		if (javaEl != null && javaEl instanceof IJavaProject && ((IJavaProject)javaEl).isOpen()) {

			// Project-level decoration
			if (SaUCPreferences.getIsCoverageProjectDecorationEnabled()) {
				Timer time = new Timer("Calculating project coverage: " + javaEl.getElementName());
				int coverage = (int)CoveragePrinter.getProjectCoverage(javaEl.getElementName());
				if (coverage != CoveragePrinter.NO_COVERAGE_REQUIRED) {
					time.printExpiredTime();
					if (SaUCPreferences.getIsCoverageFilePercentEnabled())
						decoration.addSuffix(String.format(" (c=%d%s)", coverage, "%"));
	
					if (coverage >= SaUCPreferences.getCoverageWarnThreshold()) {
						if (SaUCPreferences.getIsCoverageFileDecorationEnabled()) {
							decoration.addOverlay(COVERAGE_OK_IMAGE_DESCRIPTOR, IDecoration.UNDERLAY);
						}
					} else if (coverage >= SaUCPreferences.getCoverageErrorThreshold()) {
						if (SaUCPreferences.getIsCoverageFileDecorationEnabled()) {
							decoration.addOverlay(COVERAGE_WARN_IMAGE_DESCRIPTOR, IDecoration.UNDERLAY);
						}
					} else {
						if (SaUCPreferences.getIsCoverageFileDecorationEnabled()) {
							decoration.addOverlay(COVERAGE_ERROR_IMAGE_DESCRIPTOR, IDecoration.UNDERLAY);
						}
					}
				}
			}
				
		} 
		
		if (javaEl != null && javaEl instanceof ICompilationUnit) {

			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			JavaPathData jpd = new JavaPathData();
			IFile file = jpd.getFileQuick(resource.getFullPath(), workspace);
			if (file != null) {
				try {
					file.deleteMarkers(MARKER_TYPE_COVERAGE_REPORT, false, IResource.DEPTH_ZERO);
				} catch (CoreException e) {
					EclipseUtils.reportError(e, false);
				}
			}

			ICompilationUnit compUnit = (ICompilationUnit) javaEl;
			IType[] types;
			float[] coverage = null;
			try {
				types = compUnit.getAllTypes();
				for (IType type : types) {
					String className = type.getFullyQualifiedName();
					if (pdf.getClassDoubleForDoubledClassName(className) == null) // ignore doubled classes
						coverage = CoveragePrinter.combine(coverage, pdf.getCoverage(className));
				}
			} catch (JavaModelException e) {
				return;
			}
			Double classCoverageD = CoveragePrinter.getCombinedCoverage(coverage);
			if (classCoverageD != null) {
				int classCoverage = classCoverageD.intValue();

				if (SaUCPreferences.getIsCoverageFilePercentEnabled())
					decoration.addSuffix(String.format(" (c=%d%s)", classCoverage, "%"));

				if (classCoverage >= SaUCPreferences.getCoverageWarnThreshold()) {
					if (SaUCPreferences.getIsCoverageFileDecorationEnabled()) {
						decoration.addOverlay(COVERAGE_OK_IMAGE_DESCRIPTOR, IDecoration.UNDERLAY);
					}
					if (SaUCPreferences.getIsCoverageProblemsEnabled()) {
						addFileMarker(compUnit, file, classCoverage, IMarker.SEVERITY_INFO);
					}

				} else if (classCoverage >= SaUCPreferences.getCoverageErrorThreshold()) {
					if (SaUCPreferences.getIsCoverageFileDecorationEnabled()) {
						decoration.addOverlay(COVERAGE_WARN_IMAGE_DESCRIPTOR, IDecoration.UNDERLAY);
					}
					if (SaUCPreferences.getIsCoverageProblemsEnabled()) {
						addFileMarker(compUnit, file, classCoverage, IMarker.SEVERITY_WARNING);
					}

				} else {
					if (SaUCPreferences.getIsCoverageFileDecorationEnabled()) {
						decoration.addOverlay(COVERAGE_ERROR_IMAGE_DESCRIPTOR, IDecoration.UNDERLAY);
					}
					if (SaUCPreferences.getIsCoverageProblemsEnabled()) {
						addFileMarker(compUnit, file, classCoverage, IMarker.SEVERITY_ERROR);
					}
				}
			} else {
				// Mark files with no coverage requirement as okay for consistency
				// if (SaUCPreferences.getIsCoverageFileDecorationEnabled())
				// decoration.addOverlay(COVERAGE_OK_IMAGE_DESCRIPTOR, IDecoration.UNDERLAY);
			}
		}
	}

	private void addFileMarker(ICompilationUnit compUnit, IFile file, int coverage, int severity) {

		IResource resource = compUnit.getResource();
		if (resource != null && resource instanceof IFile) {
			String javaSrcName = compUnit.getElementName();
			String message = String.format("%d%s Test coverage in %s", coverage, "%", javaSrcName);
			IMarker covRepMarker;
			try {
				covRepMarker = file.createMarker(MARKER_TYPE_COVERAGE_REPORT);
				covRepMarker.setAttribute(IMarker.SEVERITY, severity);
				covRepMarker.setAttribute(IMarker.MESSAGE, message);
				file.refreshLocal(IResource.DEPTH_ZERO, null);
			} catch (CoreException e) {
				EclipseUtils.reportError(e, false);
			}
		}
	}

	/**
	 * Returns the resource for the given input object, or
	 * null if there is no resource associated with it.
	 * 
	 * @param object the object to find the resource for
	 * @return the resource for the given object, or null
	 */
	private IResource getResource(Object object) {

		if (object instanceof IResource) {
			return (IResource) object;
		}
		if (object instanceof IAdaptable) {
			return (IResource) ((IAdaptable) object).getAdapter(IResource.class);
		}
		return null;
	}

	public void update(IResource[] resources) {

		LabelProviderChangedEvent event = new LabelProviderChangedEvent(this);
		fireLabelProviderChanged(event);
	}
}
