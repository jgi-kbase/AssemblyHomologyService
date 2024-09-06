package us.kbase.assemblyhomology.minhash.exceptions;

/** An exception thrown when a filter fails.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class MinHashDistanceFilterException extends Exception {

	public MinHashDistanceFilterException(final String message) {
		super(message);
	}
	
	public MinHashDistanceFilterException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
