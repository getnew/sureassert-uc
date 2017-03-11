package _sauc;

/**
 * Exception thrown from method advice to encapsulate a stub invocation
 * in order to prevent execution of the method itself.
 * 
 * @author Nathan Dolan
 * 
 */
public class SAUCStubbedMethodException extends Exception {

	private static final long serialVersionUID = 1L;

	public final Object stubRetval;

	public SAUCStubbedMethodException(Object stubRetval) {

		super();
		this.stubRetval = stubRetval;
	}
}
