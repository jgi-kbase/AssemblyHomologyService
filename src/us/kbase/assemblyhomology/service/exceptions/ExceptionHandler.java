package us.kbase.assemblyhomology.service.exceptions;

import java.time.Instant;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import us.kbase.assemblyhomology.service.Fields;
import us.kbase.assemblyhomology.service.SLF4JAutoLogger;

public class ExceptionHandler implements ExceptionMapper<Throwable> {

	//TODO TEST
	//TODO JAVADOC

	private SLF4JAutoLogger logger;
	
	@Inject
	public ExceptionHandler(final SLF4JAutoLogger logger) {
		this.logger = logger;
	}

	@Override
	public Response toResponse(final Throwable ex) {
		
		LoggerFactory.getLogger(getClass()).error("Logging exception:", ex);

		//TODO CODE get rid of the logger.getCallID() method and instead make own call ID handler to decouple logger and exception handler.
		final ErrorMessage em = new ErrorMessage(ex, logger.getCallID(), Instant.now());
		return Response
				.status(em.getHttpcode())
				.entity(ImmutableMap.of(Fields.ERROR, em))
				.build();
	}
}
