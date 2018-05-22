package us.kbase.assemblyhomology.core.exceptions;

/** Thrown when an operation is requested on multiple namespaces that are not compatible for said
 * operation.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class IncompatibleNamespacesException extends AssemblyHomologyException {

	//TODO TEST
	
	public IncompatibleNamespacesException(final String message) {
		super(ErrorType.INCOMPATIBLE_NAMESPACES, message);
	}

	public IncompatibleNamespacesException(
			final ErrorType type,
			final String message) {
		super(type, message);
	}

	public IncompatibleNamespacesException(
			final String message,
			final Throwable cause) {
		super(ErrorType.INCOMPATIBLE_NAMESPACES, message, cause);
	}
}
