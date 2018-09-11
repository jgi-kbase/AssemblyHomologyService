package us.kbase.assemblyhomology.minhash.exceptions;

/** An exception thrown when a filter encounters an authentication error.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class MinHashDistanceFilterAuthenticationException extends MinHashDistanceFilterException {

	public MinHashDistanceFilterAuthenticationException(final String message) {
		super(message);
	}
	
	public MinHashDistanceFilterAuthenticationException(
			final String message,
			final Throwable cause) {
		super(message, cause);
	}
}
