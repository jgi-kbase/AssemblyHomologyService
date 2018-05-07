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

@JsonInclude(Include.NON_NULL)
public class ErrorMessage {
	
	//TODO TEST unit tests
	//TODO JAVADOC

	private final int httpcode;
	private final String httpstatus;
	private final Integer appcode;
	private final String apperror;
	private final String message;
	private final String callid;
	private final long time = Instant.now().toEpochMilli();
	// may want to support returning the exception. YAGNI
	
	
	public ErrorMessage(
			final Throwable ex,
			final String callID) {
		checkNotNull(ex, "ex");
		this.callid = callID; // null ok
		message = ex.getMessage();
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
			//TODO NOW document exception mapping
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

	public int getHttpcode() {
		return httpcode;
	}

	public String getHttpstatus() {
		return httpstatus;
	}

	public Integer getAppcode() {
		return appcode;
	}

	public String getApperror() {
		return apperror;
	}

	public String getMessage() {
		return message;
	}

	public String getCallid() {
		return callid;
	}

	public long getTime() {
		return time;
	}
}
