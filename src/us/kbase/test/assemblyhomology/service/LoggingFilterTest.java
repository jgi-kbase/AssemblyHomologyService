package us.kbase.test.assemblyhomology.service;

import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static us.kbase.test.assemblyhomology.TestCommon.assertLogEventsCorrect;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import us.kbase.assemblyhomology.config.AssemblyHomologyConfig;
import us.kbase.assemblyhomology.service.LoggingFilter;
import us.kbase.assemblyhomology.service.SLF4JAutoLogger;
import us.kbase.test.assemblyhomology.TestCommon;
import us.kbase.test.assemblyhomology.TestCommon.LogEvent;

public class LoggingFilterTest {
	
	private static final Pattern CALL_ID_MATCH = Pattern.compile("\\d{16}");
	
	private static List<ILoggingEvent> logEvents;
	
	@BeforeClass
	public static void setUp() {
		logEvents = TestCommon.setUpSLF4JTestLoggerAppender("us.kbase.assemblyhomology");
	}
	
	@Before
	public void before() {
		logEvents.clear();
	}
	
	private static class CallIDMatcher implements ArgumentMatcher<String> {

		@Override
		public boolean matches(final String id) {
			return CALL_ID_MATCH.matcher(id).matches();
		}
	}
	
	@Test
	public void filterRequestIgnoreIPHeaders() throws Exception {
		filterRequest(
				"456.789.123.456,789.123.456.789",
				"123.456.789.123",
				true,
				"123.456.789.101");
	}
	
	@Test
	public void filterRequestXFFHeader() throws Exception {
		filterRequest(
				"  456.789.123.456  ,  789.123.456.789  ",
				"123.456.789.123",
				false,
				"456.789.123.456",
				new LogEvent(
						Level.INFO,
						"X-Forwarded-For:   456.789.123.456  ,  789.123.456.789  , " +
								"X-Real-IP: 123.456.789.123, Remote IP: 123.456.789.101",
						LoggingFilter.class));
	}
	
	@Test
	public void filterRequestXFFHeaderRealIPNull() throws Exception {
		filterRequest(
				"  456.789.123.456  ,  789.123.456.789  ",
				null,
				false,
				"456.789.123.456",
				new LogEvent(
						Level.INFO,
						"X-Forwarded-For:   456.789.123.456  ,  789.123.456.789  , " +
								"Remote IP: 123.456.789.101",
						LoggingFilter.class));
	}
	
	@Test
	public void filterRequestXFFHeaderRealIPWhitespace() throws Exception {
		filterRequest(
				"  456.789.123.456  ,  789.123.456.789  ",
				"   \t  ",
				false,
				"456.789.123.456",
				new LogEvent(
						Level.INFO,
						"X-Forwarded-For:   456.789.123.456  ,  789.123.456.789  , " +
								"Remote IP: 123.456.789.101",
						LoggingFilter.class));
	}
	
	@Test
	public void filterRequestRealIPHeaderXFFHeaderNull() throws Exception {
		filterRequest(
				null,
				"   123.456.789.123   ",
				false,
				"123.456.789.123",
				new LogEvent(
						Level.INFO,
						"X-Real-IP:    123.456.789.123   , Remote IP: 123.456.789.101",
						LoggingFilter.class));
	}
	
	@Test
	public void filterRequestRealIPHeaderXFFHeaderWhitespace() throws Exception {
		filterRequest(
				"   \t   ",
				"   123.456.789.123   ",
				false,
				"123.456.789.123",
				new LogEvent(
						Level.INFO,
						"X-Real-IP:    123.456.789.123   , Remote IP: 123.456.789.101",
						LoggingFilter.class));
	}
	
	@Test
	public void filterRequestRemoteAddressXIPHeadersNull() throws Exception {
		filterRequest(null, null, false, "123.456.789.101");
	}
	
	@Test
	public void filterRequestRemoteAddressXIPHeadersWhitespace() throws Exception {
		filterRequest("   \t   ", "   \t   ", false, "123.456.789.101");
	}
	
	private void filterRequest(
			final String xForwardedFor,
			final String xRealIP,
			final boolean ignoreIPHeaders,
			final String expectedIP,
			final LogEvent... expectedLogging)
			throws Exception {
		final HttpServletRequest req = mock(HttpServletRequest.class);
		final SLF4JAutoLogger logger = mock(SLF4JAutoLogger.class);
		final AssemblyHomologyConfig cfg = mock(AssemblyHomologyConfig.class);
		final ContainerRequestContext reqcon = mock(ContainerRequestContext.class);
		
		when(cfg.isIgnoreIPHeaders()).thenReturn(ignoreIPHeaders);
		
		final LoggingFilter log = new LoggingFilter(req, logger, cfg);
		
		when(reqcon.getMethod()).thenReturn("POST");
		when(req.getRemoteAddr()).thenReturn("123.456.789.101");
		when(reqcon.getHeaderString("X-Forwarded-For")).thenReturn(xForwardedFor);
		when(reqcon.getHeaderString("X-Real-IP")).thenReturn(xRealIP);
		
		log.filter(reqcon);
		
		verify(logger).setCallInfo(
				eq("POST"),
				argThat(new CallIDMatcher()),
				eq(expectedIP));
		
		assertLogEventsCorrect(logEvents, expectedLogging);
	}
	
	@Test
	public void filterResponse() throws Exception {
		final HttpServletRequest req = mock(HttpServletRequest.class);
		final SLF4JAutoLogger logger = mock(SLF4JAutoLogger.class);
		final AssemblyHomologyConfig cfg = mock(AssemblyHomologyConfig.class);
		final ContainerRequestContext reqcon = mock(ContainerRequestContext.class);
		final ContainerResponseContext rescon = mock(ContainerResponseContext.class);
		final UriInfo uriInfo = mock(UriInfo.class);
		
		when(cfg.isIgnoreIPHeaders()).thenReturn(false);
		
		final LoggingFilter log = new LoggingFilter(req, logger, cfg);
		
		when(reqcon.getMethod()).thenReturn("GET");
		when(reqcon.getUriInfo()).thenReturn(uriInfo);
		when(uriInfo.getAbsolutePath()).thenReturn(new URI("http://foo.us/fake"));
		when(rescon.getStatus()).thenReturn(400);
		when(reqcon.getHeaderString("User-Agent")).thenReturn("Mozilla or some crap");
		
		log.filter(reqcon, rescon);
		
		assertLogEventsCorrect(logEvents, new LogEvent(
				Level.INFO,
				"GET http://foo.us/fake 400 Mozilla or some crap",
				LoggingFilter.class));
	}
}
