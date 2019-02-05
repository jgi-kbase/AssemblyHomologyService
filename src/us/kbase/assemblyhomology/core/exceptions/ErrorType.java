package us.kbase.assemblyhomology.core.exceptions;

/** An enum representing the type of a particular error.
 * @author gaprice@lbl.gov
 *
 */
public enum ErrorType {
	
	/** The authentication service returned an error. */
	AUTHENTICATION_FAILED	(10000, "Authentication failed"),
	/** No token was provided when required */
	NO_TOKEN				(10010, "No authentication token"),
	/** Two or more different authentication sources are requested. */
	INCOMPATIBLE_AUTH		(10020, "Incompatible authentication"),
	/** The user is not authorized to perform the requested action. */
	UNAUTHORIZED			(20000, "Unauthorized"),
	/** A required input parameter was not provided. */
	MISSING_PARAMETER		(30000, "Missing input parameter"),
	/** An input parameter had an illegal value. */
	ILLEGAL_PARAMETER		(30001, "Illegal input parameter"),
	/** The input sketch was not a valid sketch. */
	INVALID_SKETCH			(30010, "Invalid sketch"),
	/** An operation was requested on multiple namespaces that are not compatible for that
	 * operation. */
	INCOMPATIBLE_NAMESPACES	(30020, "Incompatible namespaces"),
	/** Sketches have incompatible parameters for measuring distances. */
	INCOMPATIBLE_SKETCHES	(30030, "Incompatible sketches"),
	/** There is no namespace with the specified name. */
	NO_SUCH_NAMESPACE		(50000, "No such namespace"),
	/** There is no sequence with the specified name. */
	NO_SUCH_SEQUENCE		(50010, "No such sequence"),
	/** The requested operation is not supported. */
	UNSUPPORTED_OP			(70000, "Unsupported operation");
	
	private final int errcode;
	private final String error;
	
	private ErrorType(final int errcode, final String error) {
		this.errcode = errcode;
		this.error = error;
	}

	/** Get the error code for the error type.
	 * @return the error code.
	 */
	public int getErrorCode() {
		return errcode;
	}

	/** Get a text description of the error type.
	 * @return the error.
	 */
	public String getError() {
		return error;
	}

}
