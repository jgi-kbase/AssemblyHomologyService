package us.kbase.test.assemblyhomology;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.Document;
import org.ini4j.Ini;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.mongodb.client.MongoDatabase;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import us.kbase.common.test.TestException;

public class TestCommon {

	public static final String MONGOEXE = "test.mongo.exe";
	public static final String MONGO_USE_WIRED_TIGER = "test.mongo.wired_tiger";
	
	public static final String JARS_PATH = "test.jars.dir";
	
	public static final String TEST_TEMP_DIR = "test.temp.dir";
	public static final String KEEP_TEMP_DIR = "test.temp.dir.keep";
	
	public static final String TEST_CONFIG_FILE_PROP_NAME = "ASSEMHOMOL_TEST_CONFIG";
	public static final String TEST_CONFIG_FILE_SECTION = "assemblyhomologytest";
	
	public static final String LONG101;
	public static final String LONG1001;
	static {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			sb.append("a");
		}
		final String s100 = sb.toString();
		final StringBuilder sb2 = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			sb2.append(s100);
		}
		LONG101 = s100 + "a";
		LONG1001 = sb2.toString() + "a";
	}
	
	private static Map<String, String> testConfig = null;
	
	public static void stfuLoggers() {
		java.util.logging.Logger.getLogger("com.mongodb")
				.setLevel(java.util.logging.Level.OFF);
		// these don't work to shut off the jetty logger
		((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
				.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME))
				.setLevel(ch.qos.logback.classic.Level.OFF);
		((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
				.getLogger("org.eclipse.jetty"))
				.setLevel(ch.qos.logback.classic.Level.OFF);
		((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
				.getLogger("us.kbase"))
				.setLevel(ch.qos.logback.classic.Level.OFF);
		System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
		System.setProperty("us.kbase.LEVEL", "OFF");
		// these do work
		((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
				.getLogger("us.kbase.assemblyhomology.service.exceptions.ExceptionHandler"))
				.setLevel(ch.qos.logback.classic.Level.OFF);
		((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
				.getLogger("us.kbase.assemblyhomology.service.LoggingFilter"))
				.setLevel(ch.qos.logback.classic.Level.OFF);
		((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
				.getLogger("us.kbase.assemblyhomology.core.AssemblyHomology"))
				.setLevel(ch.qos.logback.classic.Level.OFF);
	}
	
	public static void assertExceptionCorrect(
			final Exception got,
			final Exception expected) {
		assertThat("incorrect exception. trace:\n" + ExceptionUtils.getStackTrace(got),
				got.getMessage(), is(expected.getMessage()));
		assertThat("incorrect exception type", got, instanceOf(expected.getClass()));
	}
	
	public static void assertExceptionMessageContains(
			final Exception got,
			final String expectedMessagePart) {
		assertThat("incorrect exception message. trace:\n" + ExceptionUtils.getStackTrace(got),
				got.getMessage(), containsString(expectedMessagePart));
	}
	
	/** See https://gist.github.com/vorburger/3429822
	 * Returns a free port number on localhost.
	 *
	 * Heavily inspired from org.eclipse.jdt.launching.SocketUtil (to avoid a
	 * dependency to JDT just because of this).
	 * Slightly improved with close() missing in JDT. And throws exception
	 * instead of returning -1.
	 *
	 * @return a free port number on localhost
	 * @throws IllegalStateException if unable to find a free port
	 */
	public static int findFreePort() {
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(0);
			socket.setReuseAddress(true);
			int port = socket.getLocalPort();
			try {
				socket.close();
			} catch (IOException e) {
				// Ignore IOException on close()
			}
			return port;
		} catch (IOException e) {
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
		throw new IllegalStateException("Could not find a free TCP/IP port");
	}
	
	@SafeVarargs
	public static <T> Set<T> set(T... objects) {
		return new HashSet<T>(Arrays.asList(objects));
	}
	
	public static void assertClear(final byte[] bytes) {
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] != 0) {
				fail(String.format("found non-zero byte at position %s: %s", i, bytes[i]));
			}
		}
	}
	
	public static Path getMongoExe() {
		return Paths.get(getTestProperty(MONGOEXE)).toAbsolutePath().normalize();
	}

	public static Path getJarsDir() {
		return Paths.get(getTestProperty(JARS_PATH)).toAbsolutePath().normalize();
	}

	public static Path getTempDir() {
		return Paths.get(getTestProperty(TEST_TEMP_DIR)).toAbsolutePath().normalize();
	}
	
	public static boolean isDeleteTempFiles() {
		return !"true".equals(getTestProperty(KEEP_TEMP_DIR));
	}

	public static boolean useWiredTigerEngine() {
		return "true".equals(getTestProperty(MONGO_USE_WIRED_TIGER));
	}
	
	private static String getTestProperty(final String propertyKey) {
		getTestConfig();
		final String prop = testConfig.get(propertyKey);
		if (prop == null || prop.trim().isEmpty()) {
			throw new TestException(String.format(
					"Property %s in section %s of test file %s is missing",
					propertyKey, TEST_CONFIG_FILE_SECTION, getConfigFilePath()));
		}
		return prop;
	}

	private static void getTestConfig() {
		if (testConfig != null) {
			return;
		}
		final Path testCfgFilePath = getConfigFilePath();
		final Ini ini;
		try {
			ini = new Ini(testCfgFilePath.toFile());
		} catch (IOException ioe) {
			throw new TestException(String.format(
					"IO Error reading the test configuration file %s: %s",
					testCfgFilePath, ioe.getMessage()), ioe);
		}
		testConfig = ini.get(TEST_CONFIG_FILE_SECTION);
		if (testConfig == null) {
			throw new TestException(String.format("No section %s found in test config file %s",
					TEST_CONFIG_FILE_SECTION, testCfgFilePath));
		}
	}

	private static Path getConfigFilePath() {
		final String testCfgFilePathStr = System.getProperty(TEST_CONFIG_FILE_PROP_NAME);
		if (testCfgFilePathStr == null || testCfgFilePathStr.trim().isEmpty()) {
			throw new TestException(String.format("Cannot get the test config file path." +
					" Ensure the java system property %s is set to the test config file location.",
					TEST_CONFIG_FILE_PROP_NAME));
		}
		return Paths.get(testCfgFilePathStr).toAbsolutePath().normalize();
	}
	
	public static void destroyDB(MongoDatabase db) {
		for (String name: db.listCollectionNames()) {
			if (!name.startsWith("system.")) {
				// dropping collection also drops indexes
				db.getCollection(name).deleteMany(new Document());
			}
		}
	}
	
	//http://quirkygba.blogspot.com/2009/11/setting-environment-variables-in-java.html
	@SuppressWarnings("unchecked")
	public static Map<String, String> getenv()
			throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException {
		Map<String, String> unmodifiable = System.getenv();
		Class<?> cu = unmodifiable.getClass();
		Field m = cu.getDeclaredField("m");
		m.setAccessible(true);
		return (Map<String, String>) m.get(unmodifiable);
	}
	
	public static String getCurrentMethodName() {
		return Thread.currentThread().getStackTrace()[2].getMethodName();
	}
	
	public static String getTestExpectedData(final Class<?> clazz, final String methodName)
			throws Exception {
		final String expectedFile = clazz.getSimpleName() + "_" + methodName + ".testdata";
		final InputStream is = clazz.getResourceAsStream(expectedFile);
		if (is == null) {
			throw new FileNotFoundException(expectedFile);
		}
		return IOUtils.toString(is);
	}
	
	public static void assertCloseToNow(final long epochMillis) {
		final long now = Instant.now().toEpochMilli();
		assertThat(String.format("time (%s) not within 10000ms of now: %s", epochMillis, now),
				Math.abs(epochMillis - now) < 10000, is(true));
	}
	
	public static void assertCloseToNow(final Instant creationDate) {
		assertCloseToNow(creationDate.toEpochMilli());
	}
	
	public static void assertCloseTo(final long num, final long expected, final int range) {
		assertThat(String.format("number (%s) not within %s of target: %s",
				num, range, expected),
				Math.abs(expected - num) < range, is(true));
	}
	
	public static class LogEvent {
		
		public final Level level;
		public final String message;
		public final String className;
		public final Throwable ex;
		
		public LogEvent(final Level level, final String message, final Class<?> clazz) {
			this.level = level;
			this.message = message;
			this.className = clazz.getName();
			ex = null;
		}

		public LogEvent(final Level level, final String message, final String className) {
			this.level = level;
			this.message = message;
			this.className = className;
			ex = null;
		}
		
		public LogEvent(
				final Level level,
				final String message,
				final Class<?> clazz,
				final Throwable ex) {
			this.level = level;
			this.message = message;
			this.className = clazz.getName();
			this.ex = ex;
		}
		
		public LogEvent(
				final Level level,
				final String message,
				final String className,
				final Throwable ex) {
			this.level = level;
			this.message = message;
			this.className = className;
			this.ex = ex;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("LogEvent [level=");
			builder.append(level);
			builder.append(", message=");
			builder.append(message);
			builder.append(", className=");
			builder.append(className);
			builder.append(", ex=");
			builder.append(ex);
			builder.append("]");
			return builder.toString();
		}
	}
	
	public static List<ILoggingEvent> setUpSLF4JTestLoggerAppender(final String package_) {
		final Logger authRootLogger = (Logger) LoggerFactory.getLogger(package_);
		authRootLogger.setAdditive(false);
		authRootLogger.setLevel(Level.ALL);
		final List<ILoggingEvent> logEvents = new LinkedList<>();
		final AppenderBase<ILoggingEvent> appender =
				new AppenderBase<ILoggingEvent>() {
			@Override
			protected void append(final ILoggingEvent event) {
				logEvents.add(event);
			}
		};
		appender.start();
		authRootLogger.addAppender(appender);
		return logEvents;
	}
	
	public static void assertLogEventsCorrect(
			final List<ILoggingEvent> logEvents,
			final LogEvent... expectedlogEvents) {
		
		assertThat("incorrect log event count for list: " + logEvents, logEvents.size(),
				is(expectedlogEvents.length));
		final Iterator<ILoggingEvent> iter = logEvents.iterator();
		for (final LogEvent le: expectedlogEvents) {
			final ILoggingEvent e = iter.next();
			assertThat("incorrect log level", e.getLevel(), is(le.level));
			assertThat("incorrect originating class", e.getLoggerName(), is(le.className));
			assertThat("incorrect message", e.getFormattedMessage(), is(le.message));
			final IThrowableProxy err = e.getThrowableProxy();
			if (err != null) {
				if (le.ex == null) {
					fail(String.format("Logged exception where none was expected: %s %s %s",
							err.getClassName(), err.getMessage(), le));
				} else {
					assertThat("incorrect error class for event " + le, err.getClassName(),
							is(le.ex.getClass().getName()));
					assertThat("incorrect error message for event " + le, err.getMessage(),
							is(le.ex.getMessage()));
				}
			} else if (le.ex != null) { 
				fail("Expected exception but none was logged: " + le);
			}
		}
	}

	public static void createAuthUser(
			final URL authURL,
			final String userName,
			final String displayName)
			throws Exception {
		final URL target = new URL(authURL.toString() + "/api/V2/testmodeonly/user");
		final HttpURLConnection conn = (HttpURLConnection) target.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("content-type", "application/json");
		conn.setRequestProperty("accept", "application/json");
		conn.setDoOutput(true);

		final DataOutputStream writer = new DataOutputStream(conn.getOutputStream());
		writer.writeBytes(new ObjectMapper().writeValueAsString(ImmutableMap.of(
				"user", userName,
				"display", displayName)));
		writer.flush();
		writer.close();

		if (conn.getResponseCode() != 200) {
			final String err = IOUtils.toString(conn.getErrorStream()); 
			System.out.println(err);
			throw new TestException(err.substring(1, 200));
		}
	}

	public static String createLoginToken(final URL authURL, String user) throws Exception {
		final URL target = new URL(authURL.toString() + "/api/V2/testmodeonly/token");
		final HttpURLConnection conn = (HttpURLConnection) target.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("content-type", "application/json");
		conn.setRequestProperty("accept", "application/json");
		conn.setDoOutput(true);

		final DataOutputStream writer = new DataOutputStream(conn.getOutputStream());
		writer.writeBytes(new ObjectMapper().writeValueAsString(ImmutableMap.of(
				"user", user,
				"type", "Login")));
		writer.flush();
		writer.close();

		if (conn.getResponseCode() != 200) {
			final String err = IOUtils.toString(conn.getErrorStream()); 
			System.out.println(err);
			throw new TestException(err.substring(1, 200));
		}
		final String out = IOUtils.toString(conn.getInputStream());
		@SuppressWarnings("unchecked")
		final Map<String, Object> resp = new ObjectMapper().readValue(out, Map.class);
		return (String) resp.get("token");
	}

}
