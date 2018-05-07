package us.kbase.assemblyhomology.service;

import static us.kbase.assemblyhomology.util.Util.isNullOrEmpty;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;

import org.slf4j.LoggerFactory;

/** The logger for the service. Sets up the logging info (e.g. the method, a random call ID,
 * and the IP address) for each request and logs the method, path, status code, and user agent on
 * a response.
 * @author gaprice@lbl.gov
 *
 */
public class LoggingFilter implements ContainerRequestFilter,
		ContainerResponseFilter {
	
	//TODO TEST
	
	private static final String X_FORWARDED_FOR = "X-Forwarded-For";
	private static final String X_REAL_IP = "X-Real-IP";
	private static final String USER_AGENT = "User-Agent";
	
	//TODO CODE is there a way to inject these via a constructor? Makes testing a lot easier
	@Context
	private HttpServletRequest servletRequest;
	
	@Inject
	private SLF4JAutoLogger logger;
	
	@Override
	public void filter(final ContainerRequestContext reqcon)
			throws IOException {
		//TODO NOW get dont-trust-ip-headers from config
		boolean ignoreIPheaders = false;
		logger.setCallInfo(
				reqcon.getMethod(),
				(String.format("%.16f", Math.random())).substring(2),
				getIpAddress(reqcon, ignoreIPheaders));
		
		logHeaders(reqcon, ignoreIPheaders);
	}
	
	private void logHeaders(
			final ContainerRequestContext request,
			final boolean ignoreIPsInHeaders) {
		if (!ignoreIPsInHeaders) {
			final List<String> log = new LinkedList<>();
			final String xFF = request.getHeaderString(X_FORWARDED_FOR);
			final String realIP = request.getHeaderString(X_REAL_IP);
			if (!isNullOrEmpty(xFF)) {
				log.add(X_FORWARDED_FOR + ": " + xFF);
			}
			if (!isNullOrEmpty(realIP)) {
				log.add(X_REAL_IP + ": " + realIP);
			}
			if (!isNullOrEmpty(realIP) || !isNullOrEmpty(xFF)) {
				log.add("Remote IP: " + servletRequest.getRemoteAddr());
				logInfo(String.join(", ", log));
			}
		}
	}
	
	private void logInfo(final String format, final Object... args) {
		LoggerFactory.getLogger(getClass()).info(format, args);
		
	}

	private String getIpAddress(
			final ContainerRequestContext request,
			final boolean ignoreIPsInHeaders) {
		final String xFF = request.getHeaderString(X_FORWARDED_FOR);
		final String realIP = request.getHeaderString(X_REAL_IP);

		if (!ignoreIPsInHeaders) {
			if (!isNullOrEmpty(xFF)) {
				return xFF.split(",")[0].trim();
			}
			if (!isNullOrEmpty(realIP)) {
				return realIP.trim();
			}
		}
		return servletRequest.getRemoteAddr();
	}

	@Override
	public void filter(
			final ContainerRequestContext reqcon,
			final ContainerResponseContext rescon)
			throws IOException {
		logInfo("{} {} {} {}",
				reqcon.getMethod(),
				reqcon.getUriInfo().getAbsolutePath(),
				rescon.getStatus(),
				reqcon.getHeaderString(USER_AGENT));
	}

}
