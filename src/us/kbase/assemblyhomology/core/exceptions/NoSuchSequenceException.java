package us.kbase.assemblyhomology.core.exceptions;

/** Thrown when the specified sequence does not exist.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class NoSuchSequenceException extends AssemblyHomologyException {

	//TODO TEST
	
	public NoSuchSequenceException(final String message) {
		super(ErrorType.NO_SUCH_SEQUENCE, message);
	}

	public NoSuchSequenceException(
			final ErrorType type,
			final String message) {
		super(type, message);
	}

	public NoSuchSequenceException(
			final String message,
			final Throwable cause) {
		super(ErrorType.NO_SUCH_SEQUENCE, message, cause);
	}
}
