package us.kbase.test.assemblyhomology.service.exceptions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static us.kbase.test.assemblyhomology.TestCommon.assertLogEventsCorrect;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import us.kbase.assemblyhomology.core.exceptions.NoSuchSequenceException;
import us.kbase.assemblyhomology.service.SLF4JAutoLogger;
import us.kbase.assemblyhomology.service.exceptions.ErrorMessage;
import us.kbase.assemblyhomology.service.exceptions.ExceptionHandler;
import us.kbase.test.assemblyhomology.TestCommon;
import us.kbase.test.assemblyhomology.TestCommon.LogEvent;

public class ExceptionHandlerTest {
	
	private static List<ILoggingEvent> logEvents;
	
	private static class TestSet {
		private final ExceptionHandler handler;
		private final SLF4JAutoLogger loggerMock;
		private final Clock clockMock;
		
		private TestSet(
				final ExceptionHandler handler,
				final SLF4JAutoLogger loggerMock,
				final Clock clockMock) {
			this.handler = handler;
			this.loggerMock = loggerMock;
			this.clockMock = clockMock;
		}
	}
	
	private TestSet getTestClasses() throws Exception {
		final SLF4JAutoLogger logger = mock(SLF4JAutoLogger.class);
		final Clock clock = mock(Clock.class);
		final Constructor<ExceptionHandler> con = ExceptionHandler.class.getDeclaredConstructor(
				SLF4JAutoLogger.class, Clock.class);
		con.setAccessible(true);
		final ExceptionHandler handler = con.newInstance(logger, clock);
		return new TestSet(handler, logger, clock);
	}
	
	@BeforeClass
	public static void setUp() {
		logEvents = TestCommon.setUpSLF4JTestLoggerAppender("us.kbase.assemblyhomology");
	}
	
	@Before
	public void before() {
		logEvents.clear();
	}
	
	@Test
	public void handle() throws Exception {
		final TestSet ts = getTestClasses();
		final ExceptionHandler eh = ts.handler;
		final SLF4JAutoLogger logger = ts.loggerMock;
		final Clock clock = ts.clockMock;
		
		when(logger.getCallID()).thenReturn("call id");
		when(clock.instant()).thenReturn(Instant.ofEpochMilli(10000));
		
		final Response r = eh.toResponse(new NoSuchSequenceException("seq1"));
		
		assertThat("incorrect code", r.getStatus(), is(404));
		assertThat("incorrect entity", r.getEntity(), is(ImmutableMap.of(
				"error", new ErrorMessage(
						new NoSuchSequenceException("seq1"),
						"call id",
						Instant.ofEpochMilli(10000)))));
		assertThat("incorrect media type", r.getMediaType(), is(MediaType.APPLICATION_JSON_TYPE));
		
		assertLogEventsCorrect(logEvents, new LogEvent(
				Level.ERROR,
				"Logging exception:",
				ExceptionHandler.class,
				new NoSuchSequenceException("seq1")));
	}

}
