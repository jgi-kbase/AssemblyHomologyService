package us.kbase.assemblyhomology.service.exceptions;

import java.time.Clock;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import us.kbase.assemblyhomology.service.Fields;
import us.kbase.assemblyhomology.service.SLF4JAutoLogger;

/** Handler for exceptions thrown by the Assembly Homology service.
 * @author gaprice@lbl.gov
 *
 */
public class ExceptionHandler implements ExceptionMapper<Throwable> {

	private final SLF4JAutoLogger logger;
	private final Clock clock;
	
	/** Construct the handler. This is typically done by the Jersey framework.
	 * @param logger the logger for the service.
	 */
	@Inject
	public ExceptionHandler(final SLF4JAutoLogger logger) {
		this(logger, Clock.systemDefaultZone());
	}

	// for tests to allow mocking the clock
	private ExceptionHandler(final SLF4JAutoLogger logger, final Clock clock) {
		this.logger = logger;
		this.clock = clock;
	}

	@Override
	public Response toResponse(final Throwable ex) {
		
		LoggerFactory.getLogger(getClass()).error("Logging exception:", ex);

		//TODO CODE get rid of the logger.getCallID() method and instead make own call ID handler to decouple logger and exception handler.
		final ErrorMessage em = new ErrorMessage(ex, logger.getCallID(), clock.instant());
		return Response
				.status(em.getHttpcode())
				.entity(ImmutableMap.of(Fields.ERROR, em))
				.build();
	}
}
