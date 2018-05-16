package us.kbase.assemblyhomology.core.exceptions;

/** Thrown when a parameter has an illegal value.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class InvalidSketchException extends AssemblyHomologyException {

	//TODO TEST
	
	public InvalidSketchException(final String message) {
		super(ErrorType.INVALID_SKETCH, message);
	}

	public InvalidSketchException(
			final ErrorType type,
			final String message) {
		super(type, message);
	}

	public InvalidSketchException(
			final String message,
			final Throwable cause) {
		super(ErrorType.INVALID_SKETCH, message, cause);
	}
}
