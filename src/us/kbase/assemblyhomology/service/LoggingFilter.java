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

import org.slf4j.LoggerFactory;

import us.kbase.assemblyhomology.config.AssemblyHomologyConfig;

/** The logger for the service. Sets up the logging info (e.g. the method, a random call ID,
 * and the IP address) for each request and logs the method, path, status code, and user agent on
 * a response.
 * @author gaprice@lbl.gov
 *
 */
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
	
	private static final String X_FORWARDED_FOR = "X-Forwarded-For";
	private static final String X_REAL_IP = "X-Real-IP";
	private static final String USER_AGENT = "User-Agent";
	
	private final HttpServletRequest servletRequest;
	private final SLF4JAutoLogger logger;
	private final boolean ignoreIPheaders;
	
	/** Create the logging filter. This is normally done by the Jersey framework.
	 * @param servletRequest the request to be logged.
	 * @param logger the logger to receive the call information.
	 * @param cfg the service configuration.
	 */
	@Inject
	public LoggingFilter(
			final HttpServletRequest servletRequest,
			final SLF4JAutoLogger logger,
			final AssemblyHomologyConfig cfg) {
		this.servletRequest = servletRequest;
		this.logger = logger;
		ignoreIPheaders = cfg.isIgnoreIPHeaders();
	}
	
	@Override
	public void filter(final ContainerRequestContext reqcon) throws IOException {
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
