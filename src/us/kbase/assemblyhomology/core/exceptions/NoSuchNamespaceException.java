package us.kbase.assemblyhomology.core.exceptions;

/** Thrown when the specified namespace does not exist.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class NoSuchNamespaceException extends AssemblyHomologyException {

	//TODO TEST
	
	public NoSuchNamespaceException(final String message) {
		super(ErrorType.NO_SUCH_NAMESPACE, message);
	}

	public NoSuchNamespaceException(
			final ErrorType type,
			final String message) {
		super(type, message);
	}

	public NoSuchNamespaceException(
			final String message,
			final Throwable cause) {
		super(ErrorType.NO_SUCH_NAMESPACE, message, cause);
	}
}
