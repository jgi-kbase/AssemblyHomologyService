package us.kbase.assemblyhomology.core.exceptions;

import static com.google.common.base.Preconditions.checkNotNull;

/** Base class of all assembly homology exceptions.
 * @author gaprice@lbl.gov 
 */
@SuppressWarnings("serial")
public class AssemblyHomologyException extends Exception {
	
	//TODO TEST
	
	private final ErrorType err;
	
	public AssemblyHomologyException(final ErrorType err, final String message) {
		super(getMsg(err, message));
		this.err = err;
	}

	private static String getMsg(final ErrorType err, final String message) {
		checkNotNull(err, "err");
		return err.getErrorCode() + " " + err.getError() + 
				(message == null || message.trim().isEmpty() ? "" : ": " + message);
	}
	
	public AssemblyHomologyException(
			final ErrorType err,
			final String message,
			final Throwable cause) {
		super(getMsg(err, message), cause);
		this.err = err;
	}

	/** Get the error type for this exception.
	 * @return the error type.
	 */
	public ErrorType getErr() {
		return err;
	}
}
