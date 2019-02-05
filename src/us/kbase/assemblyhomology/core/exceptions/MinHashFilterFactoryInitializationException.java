package us.kbase.assemblyhomology.core.exceptions;

/** An exception thrown when a filter factory cannot initialize.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class MinHashFilterFactoryInitializationException extends Exception {

	public MinHashFilterFactoryInitializationException(final String message) {
		super(message);
	}
	
	public MinHashFilterFactoryInitializationException(
			final String message,
			final Throwable cause) {
		super(message, cause);
	}
	
}
