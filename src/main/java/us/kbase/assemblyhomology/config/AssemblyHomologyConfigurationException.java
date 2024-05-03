package us.kbase.assemblyhomology.config;

/** Thrown when a configuration is invalid.
 * @author gaprice@lbl.gov
 *
 */
public class AssemblyHomologyConfigurationException extends Exception {

	private static final long serialVersionUID = 1L;

	public AssemblyHomologyConfigurationException(final String message) {
		super(message);
	}
	
	public AssemblyHomologyConfigurationException(
			final String message,
			final Throwable cause) {
		super(message, cause);
	}
}