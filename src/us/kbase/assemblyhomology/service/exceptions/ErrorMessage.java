package us.kbase.assemblyhomology.service.exceptions;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonMappingException;

import us.kbase.assemblyhomology.core.exceptions.AssemblyHomologyException;
import us.kbase.assemblyhomology.core.exceptions.NoDataException;

/** An error message to be returned to the server client. Expected to be serialized to JSON.
 * 
 * Exception classes are mapped to response status as:
 * {@link AssemblyHomologyException} and subclasses - 400
 * {@link NoDataException} and subclasses - 404
 * {@link WebApplicationException} and subclasses - as exception
 * {@link JsonMappingException} - 400
 * All others - 500
 * @author gaprice@lbl.gov
 *
 */
@JsonInclude(Include.NON_NULL)
public class ErrorMessage {
	
	private final int httpcode;
	private final String httpstatus;
	private final Integer appcode;
	private final String apperror;
	private final String message;
	private final String callid;
	private final long time;
	// may want to support returning the exception. YAGNI
	
	/** Create a new error message.
	 * @param ex the exception to be wrapped by the error message.
	 * @param callID the call ID under which the error occurred. This is typically logged
	 * along with the exception.
	 * @param time the time the error occurred.
	 */
	public ErrorMessage(
			final Throwable ex,
			final String callID,
			final Instant time) {
		checkNotNull(ex, "ex");
		checkNotNull(time, "time");
		this.callid = callID; // null ok
		message = ex.getMessage();
		this.time = time.toEpochMilli();
		final StatusType status;
		if (ex instanceof AssemblyHomologyException) {
			final AssemblyHomologyException ae = (AssemblyHomologyException) ex;
			appcode = ae.getErr().getErrorCode();
			apperror = ae.getErr().getError();
			// may need these later
//			if (ae instanceof AuthenticationException) {
//				status = Response.Status.UNAUTHORIZED;
//			} else if (ae instanceof UnauthorizedException) {
//				status = Response.Status.FORBIDDEN;
//			} else
			if (ae instanceof NoDataException) {
				status = Response.Status.NOT_FOUND;
			} else {
				status = Response.Status.BAD_REQUEST;
			}
		} else if (ex instanceof WebApplicationException) {
			appcode = null;
			apperror = null;
			status = ((WebApplicationException) ex).getResponse()
					.getStatusInfo();
		} else if (ex instanceof JsonMappingException) {
			/* we assume that any json exceptions are because the client sent bad JSON data.
			 * This may not 100% accurate, but if we're attempting to return unserializable data
			 * that should be caught in tests.
			 */
			appcode = null;
			apperror = null;
			status = Response.Status.BAD_REQUEST;
		} else {
			appcode = null;
			apperror = null;
			status = Response.Status.INTERNAL_SERVER_ERROR;
		}
		httpcode = status.getStatusCode();
		httpstatus = status.getReasonPhrase();
	}

	/** Get the HTTP code for the error message.
	 * @return the HTTP code.
	 */
	public int getHttpcode() {
		return httpcode;
	}

	/** Get the HTTP status string for the error message.
	 * @return the HTTP status.
	 */
	public String getHttpstatus() {
		return httpstatus;
	}

	/** Get the application code for the error message.
	 * @return the application code.
	 */
	public Integer getAppcode() {
		return appcode;
	}

	/** Get the application error string for the error message
	 * @return the application error.
	 */
	public String getApperror() {
		return apperror;
	}

	/** Get the error message.
	 * @return the error message.
	 */
	public String getMessage() {
		return message;
	}

	/** Get the call ID under which the error occurred.
	 * @return the call ID.
	 */
	public String getCallid() {
		return callid;
	}

	/** Get the time the error occurred.
	 * @return the error time.
	 */
	public long getTime() {
		return time;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((appcode == null) ? 0 : appcode.hashCode());
		result = prime * result + ((apperror == null) ? 0 : apperror.hashCode());
		result = prime * result + ((callid == null) ? 0 : callid.hashCode());
		result = prime * result + httpcode;
		result = prime * result + ((httpstatus == null) ? 0 : httpstatus.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + (int) (time ^ (time >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ErrorMessage other = (ErrorMessage) obj;
		if (appcode == null) {
			if (other.appcode != null) {
				return false;
			}
		} else if (!appcode.equals(other.appcode)) {
			return false;
		}
		if (apperror == null) {
			if (other.apperror != null) {
				return false;
			}
		} else if (!apperror.equals(other.apperror)) {
			return false;
		}
		if (callid == null) {
			if (other.callid != null) {
				return false;
			}
		} else if (!callid.equals(other.callid)) {
			return false;
		}
		if (httpcode != other.httpcode) {
			return false;
		}
		if (httpstatus == null) {
			if (other.httpstatus != null) {
				return false;
			}
		} else if (!httpstatus.equals(other.httpstatus)) {
			return false;
		}
		if (message == null) {
			if (other.message != null) {
				return false;
			}
		} else if (!message.equals(other.message)) {
			return false;
		}
		if (time != other.time) {
			return false;
		}
		return true;
	}
	
}
