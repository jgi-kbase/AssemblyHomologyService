package us.kbase.assemblyhomology.minhash.exceptions;

import com.google.common.base.Optional;

/** Thrown when a file is not a MinHash sketch database.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class NotASketchException extends MinHashException {

	private final String mashOutput;
	
	/** Create an exception.
	 * @param message the exception message.
	 */
	public NotASketchException(final String message) {
		super(message);
		this.mashOutput = null;
	}
	
	/** Create an exception with error output from the MinHash implemenation
	 * @param message the exception message.
	 * @param minHashOutput any output from the MinHash implementation related to the error.
	 * Typically standard out from a CLI. This information is not included in the exception
	 * message. May be null.
	 */
	public NotASketchException(final String message, final String minHashOutput) {
		super(message);
		this.mashOutput = minHashOutput;
	}

	/** Get the output from the MinHash implementation associated with this error. Typically
	 * standard out from a CLI.
	 * @return The output, or absent().
	 */
	public Optional<String> getMinHashErrorOutput() {
		return Optional.fromNullable(mashOutput);
	}
}
