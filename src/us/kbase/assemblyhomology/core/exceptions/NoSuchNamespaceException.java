package us.kbase.assemblyhomology.core.exceptions;

/** Thrown when the specified namespace does not exist.
 * @author gaprice@lbl.gov
 *
 */
@SuppressWarnings("serial")
public class NoSuchNamespaceException extends NoDataException {

	//TODO TEST
	
	public NoSuchNamespaceException(final String message) {
		super(ErrorType.NO_SUCH_NAMESPACE, message);
	}
}
