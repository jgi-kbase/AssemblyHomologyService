package us.kbase.assemblyhomology.core.exceptions;

/** Thrown when a parameter has an illegal value.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class IllegalParameterException extends AssemblyHomologyException {

	//TODO TEST
	
	public IllegalParameterException(final String message) {
		super(ErrorType.ILLEGAL_PARAMETER, message);
	}

	public IllegalParameterException(
			final ErrorType type,
			final String message) {
		super(type, message);
	}

	public IllegalParameterException(
			final String message,
			final Throwable cause) {
		super(ErrorType.ILLEGAL_PARAMETER, message, cause);
	}
}
