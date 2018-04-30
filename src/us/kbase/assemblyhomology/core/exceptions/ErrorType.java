package us.kbase.assemblyhomology.core.exceptions;

import java.util.HashMap;
import java.util.Map;

/** An enum representing the type of a particular error.
 * @author gaprice@lbl.gov
 *
 */
public enum ErrorType {
	
	//TODO TEST
	
	/** The authentication service returned an error. */
	AUTHENTICATION_FAILED	(10000, "Authentication failed"),
	/** No token was provided when required */
	NO_TOKEN				(10010, "No authentication token"),
	/** The user is not authorized to perform the requested action. */
	UNAUTHORIZED			(20000, "Unauthorized"),
	/** A required input parameter was not provided. */
	MISSING_PARAMETER		(30000, "Missing input parameter"),
	/** An input parameter had an illegal value. */
	ILLEGAL_PARAMETER		(30001, "Illegal input parameter"),
	/** There is no namespace with the specified name. */
	NO_SUCH_NAMESPACE		(50000, "No such namespace"),
	/** There is no sequence with the specified name. */
	NO_SUCH_SEQUENCE		(50010, "No such sequence"),
	/** The requested operation is not supported. */
	UNSUPPORTED_OP			(70000, "Unsupported operation");
	
	private static final Map<Integer, ErrorType> ERROR_MAP = new HashMap<>();
	static {
		for (final ErrorType t: ErrorType.values()) {
			ERROR_MAP.put(t.getErrorCode(), t);
		}
	}
	
	/** Get an ErrorType given the error code.
	 * @param code the error code.
	 * @return the ErrorType corresponding to the error code.
	 */
	public static ErrorType fromErrorCode(final int code) {
		if (!ERROR_MAP.containsKey(code)) {
			throw new IllegalArgumentException("Invalid error code: " + code);
		}
		return ERROR_MAP.get(code);
	}
	
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
