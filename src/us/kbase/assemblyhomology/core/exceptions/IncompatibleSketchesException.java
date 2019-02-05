package us.kbase.assemblyhomology.core.exceptions;

/** Thrown when sketches do not have compatible parameters for distance calculation.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class IncompatibleSketchesException extends AssemblyHomologyException {

	//TODO TEST
	
	public IncompatibleSketchesException(final String message) {
		super(ErrorType.INCOMPATIBLE_SKETCHES, message);
	}

	public IncompatibleSketchesException(
			final ErrorType type,
			final String message) {
		super(type, message);
	}

	public IncompatibleSketchesException(
			final String message,
			final Throwable cause) {
		super(ErrorType.INCOMPATIBLE_SKETCHES, message, cause);
	}
}
