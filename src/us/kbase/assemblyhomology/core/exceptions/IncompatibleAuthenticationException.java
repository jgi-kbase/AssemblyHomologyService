package us.kbase.assemblyhomology.core.exceptions;


/** Thrown when at least two different authentication sources are required for an operation.
 * @author gaprice@lbl.gov
 */
@SuppressWarnings("serial")
public class IncompatibleAuthenticationException extends AuthenticationException {

	public IncompatibleAuthenticationException(final String message) {
		super(ErrorType.INCOMPATIBLE_AUTH, message);
	}
}
