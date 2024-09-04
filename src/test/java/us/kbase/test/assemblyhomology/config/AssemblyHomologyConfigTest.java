package us.kbase.test.assemblyhomology.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static us.kbase.test.assemblyhomology.TestCommon.set;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import us.kbase.assemblyhomology.config.AssemblyHomologyConfig;
import us.kbase.assemblyhomology.config.AssemblyHomologyConfigurationException;
import us.kbase.assemblyhomology.config.FilterConfiguration;
import us.kbase.assemblyhomology.service.SLF4JAutoLogger;
import us.kbase.assemblyhomology.util.FileOpener;
import us.kbase.test.assemblyhomology.TestCommon;

public class AssemblyHomologyConfigTest {

	private AssemblyHomologyConfig getConfig(final FileOpener opener) throws Throwable {
		final Constructor<AssemblyHomologyConfig> con =
				AssemblyHomologyConfig.class.getDeclaredConstructor(FileOpener.class);
		con.setAccessible(true);
		try {
			return con.newInstance(opener);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}
	
	private AssemblyHomologyConfig getConfig(
			final Path iniFilePath,
			final boolean nullLogger,
			final FileOpener opener)
			throws Throwable {
		final Constructor<AssemblyHomologyConfig> con =
				AssemblyHomologyConfig.class.getDeclaredConstructor(
						Path.class, boolean.class, FileOpener.class);
		con.setAccessible(true);
		try {
			return con.newInstance(iniFilePath, nullLogger, opener);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}
	
	@Test
	public void sysPropAssemHomolNoUserNoIgnoreIPNoTimeout() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		final AssemblyHomologyConfig cfg;
		try {
			System.setProperty(AssemblyHomologyConfig.ENV_VAR_ASSYHOM, "some file");
			System.setProperty(AssemblyHomologyConfig.ENV_VAR_KB_DEP, "some file2");
			TestCommon.getenv().put(AssemblyHomologyConfig.ENV_VAR_ASSYHOM, "some file2");
			TestCommon.getenv().put(AssemblyHomologyConfig.ENV_VAR_KB_DEP, "some file2");
			when(fo.open(Paths.get("some file"))).thenReturn(new ByteArrayInputStream(
					("[assemblyhomology]\n" +
					 "mongo-host=mongo\n" +
					 "mongo-db=database\n" +
					 "temp-dir=/foo/bar/baz")
					.getBytes()));
			cfg = getConfig(fo);
		} finally {
			System.clearProperty(AssemblyHomologyConfig.ENV_VAR_ASSYHOM);
			System.clearProperty(AssemblyHomologyConfig.ENV_VAR_KB_DEP);
			TestCommon.getenv().remove(AssemblyHomologyConfig.ENV_VAR_ASSYHOM);
			TestCommon.getenv().remove(AssemblyHomologyConfig.ENV_VAR_KB_DEP);
		}
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.absent()));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd(), is(Optional.absent()));
		assertThat("incorrect retry writes", cfg.getMongoRetryWrites(), is(false));
		assertThat("incorrect minhash timeout", cfg.getMinhashTimeoutSec(), is(30));
		assertThat("incorrect temp dir", cfg.getPathToTemporaryFileDirectory(),
				is(Paths.get("/foo/bar/baz")));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(false));
		assertThat("incorrect filters", cfg.getFilterConfigurations(), is(Collections.emptySet()));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void sysPropKBNoUserNoIgnoreIPNoTimout() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		final AssemblyHomologyConfig cfg;
		try {
			System.setProperty(AssemblyHomologyConfig.ENV_VAR_KB_DEP, "some file2");
			TestCommon.getenv().put(AssemblyHomologyConfig.ENV_VAR_KB_DEP, "some file");
			when(fo.open(Paths.get("some file2"))).thenReturn(new ByteArrayInputStream(
					("[assemblyhomology]\n" +
					 "mongo-host=mongo\n" +
					 "mongo-db=database\n" +
					 "mongo-user=\n" +
					 "mongo-pwd=\n" +
					 "mongo-retrywrites=\n" +
					 "minhash-timeout=\n" +
					 "dont-trust-x-ip-headers=true1\n" +
					 "temp-dir=/foo/bar/baz\n" +
					 "filters=   ,    \t   ,   ")
					.getBytes()));
			cfg = getConfig(fo);
		} finally {
			System.clearProperty(AssemblyHomologyConfig.ENV_VAR_KB_DEP);
			TestCommon.getenv().remove(AssemblyHomologyConfig.ENV_VAR_KB_DEP);
		}
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.absent()));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd(), is(Optional.absent()));
		assertThat("incorrect retry writes", cfg.getMongoRetryWrites(), is(false));
		assertThat("incorrect minhash timeout", cfg.getMinhashTimeoutSec(), is(30));
		assertThat("incorrect temp dir", cfg.getPathToTemporaryFileDirectory(),
				is(Paths.get("/foo/bar/baz")));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(false));
		assertThat("incorrect filters", cfg.getFilterConfigurations(), is(Collections.emptySet()));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void envVarAssemHomolWithUserWithIgnoreIPWithTimeoutWithFilters() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		final AssemblyHomologyConfig cfg;
		try {
			TestCommon.getenv().put(AssemblyHomologyConfig.ENV_VAR_ASSYHOM, "some file");
			System.setProperty(AssemblyHomologyConfig.ENV_VAR_KB_DEP, "some file2");
			TestCommon.getenv().put(AssemblyHomologyConfig.ENV_VAR_KB_DEP, "some file2");
			when(fo.open(Paths.get("some file"))).thenReturn(new ByteArrayInputStream(
					("[assemblyhomology]\n" +
					 "mongo-host=mongo\n" +
					 "mongo-db=database\n" +
					 "mongo-user=userfoo\n" +
					 "mongo-pwd=somepwd\n" +
					 "mongo-retrywrites=true\n" +
					 "minhash-timeout=600\n" +
					 "dont-trust-x-ip-headers=true\n" +
					 "temp-dir=/foo/bar/baz\n" +
					 "filters=foo,  \t   ,   bar  \n" +
					 "filter-foo-factory-class=fooclass\n" +
					 "filter-bar-factory-class=barclass\n" +
					 "filter-bar-init-whee=whoo\n" +
					 "filter-bar-init-wugga=foobar\n")
					.getBytes()));
			cfg = getConfig(fo);
		} finally {
			TestCommon.getenv().remove(AssemblyHomologyConfig.ENV_VAR_ASSYHOM);
			TestCommon.getenv().remove(AssemblyHomologyConfig.ENV_VAR_KB_DEP);
			System.clearProperty(AssemblyHomologyConfig.ENV_VAR_KB_DEP);
		}
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.of("userfoo")));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd().get(),
				equalTo("somepwd".toCharArray()));
		assertThat("incorrect retry writes", cfg.getMongoRetryWrites(), is(true));
		assertThat("incorrect minhash timeout", cfg.getMinhashTimeoutSec(), is(600));
		assertThat("incorrect temp dir", cfg.getPathToTemporaryFileDirectory(),
				is(Paths.get("/foo/bar/baz")));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(true));
		assertThat("incorrect filters", cfg.getFilterConfigurations(), is(set(
				new FilterConfiguration("fooclass", Collections.emptyMap()),
				new FilterConfiguration("barclass", ImmutableMap.of(
						"whee", "whoo", "wugga", "foobar")))));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void envVarKBWithUserWithIgnoreIPWithTimeout() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		final AssemblyHomologyConfig cfg;
		try {
			TestCommon.getenv().put(AssemblyHomologyConfig.ENV_VAR_KB_DEP, "some file2");
			when(fo.open(Paths.get("some file2"))).thenReturn(new ByteArrayInputStream(
					("[assemblyhomology]\n" +
					 "mongo-host=mongo\n" +
					 "mongo-db=database\n" +
					 "mongo-user=userfoo\n" +
					 "mongo-pwd=somepwd\n" +
					 "mongo-retrywrites=true\n" +
					 "minhash-timeout=15\n" +
					 "dont-trust-x-ip-headers=true\n" +
					 "temp-dir=/foo/bar/baz")
					.getBytes()));
			cfg = getConfig(fo);
		} finally {
			TestCommon.getenv().remove(AssemblyHomologyConfig.ENV_VAR_KB_DEP);
		}
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.of("userfoo")));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd().get(),
				equalTo("somepwd".toCharArray()));
		assertThat("incorrect retry writes", cfg.getMongoRetryWrites(), is(true));
		assertThat("incorrect minhash timeout", cfg.getMinhashTimeoutSec(), is(15));
		assertThat("incorrect temp dir", cfg.getPathToTemporaryFileDirectory(),
				is(Paths.get("/foo/bar/baz")));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(true));
		assertThat("incorrect filters", cfg.getFilterConfigurations(), is(Collections.emptySet()));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void pathNoUserNoIgnoreIPStdLogger() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file2"))).thenReturn(new ByteArrayInputStream(
				("[assemblyhomology]\n" +
				 "mongo-host=mongo\n" +
				 "mongo-db=database\n" +
				 "temp-dir=/foo/bar/baz")
				.getBytes()));
		final AssemblyHomologyConfig cfg = getConfig(Paths.get("some file2"), false, fo);
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.absent()));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd(), is(Optional.absent()));
		assertThat("incorrect retry writes", cfg.getMongoRetryWrites(), is(false));
		assertThat("incorrect minhash timeout", cfg.getMinhashTimeoutSec(), is(30));
		assertThat("incorrect temp dir", cfg.getPathToTemporaryFileDirectory(),
				is(Paths.get("/foo/bar/baz")));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(false));
		assertThat("incorrect filters", cfg.getFilterConfigurations(), is(Collections.emptySet()));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void pathWithUserWithIgnoreIPNullLoggerWithFilters() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file2"))).thenReturn(new ByteArrayInputStream(
				("[assemblyhomology]\n" +
				 "mongo-host=mongo\n" +
				 "mongo-db=database\n" +
				 "mongo-user=userfoo\n" +
				 "mongo-pwd=somepwd\n" +
				 "mongo-retrywrites=true\n" +
				 "dont-trust-x-ip-headers=true\n" +
				 "temp-dir=/foo/bar/baz\n" + 
				 "filters=foo,  \t   ,   bar  \n" +
				 "filter-foo-factory-class=fooclass\n" +
				 "filter-bar-factory-class=barclass\n" +
				 "filter-bar-init-whee=whoo\n" +
				 "filter-bar-init-wugga=foobar\n")
				.getBytes()));
		final AssemblyHomologyConfig cfg = getConfig(Paths.get("some file2"), true, fo);
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.of("userfoo")));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd().get(),
				equalTo("somepwd".toCharArray()));
		assertThat("incorrect retry writes", cfg.getMongoRetryWrites(), is(true));
		assertThat("incorrect minhash timeout", cfg.getMinhashTimeoutSec(), is(30));
		assertThat("incorrect temp dir", cfg.getPathToTemporaryFileDirectory(),
				is(Paths.get("/foo/bar/baz")));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(true));
		assertThat("incorrect filters", cfg.getFilterConfigurations(), is(set(
				new FilterConfiguration("fooclass", Collections.emptyMap()),
				new FilterConfiguration("barclass", ImmutableMap.of(
						"whee", "whoo", "wugga", "foobar")))));
		testLogger(cfg.getLogger(), true);
	}
	
	@Test
	public void immutable() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		final AssemblyHomologyConfig cfg;
		try {
			System.setProperty(AssemblyHomologyConfig.ENV_VAR_ASSYHOM, "some file");
			when(fo.open(Paths.get("some file"))).thenReturn(new ByteArrayInputStream(
					("[assemblyhomology]\n" +
					 "mongo-host=mongo\n" +
					 "mongo-db=database\n" +
					 "temp-dir=/foo/bar/baz")
					.getBytes()));
			cfg = getConfig(fo);
		} finally {
			System.clearProperty(AssemblyHomologyConfig.ENV_VAR_ASSYHOM);
		}
		try {
			cfg.getFilterConfigurations().add(new FilterConfiguration(
					"a", Collections.emptyMap()));
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
	}

	private void testLogger(final SLF4JAutoLogger logger, final boolean nullLogger) {
		// too much of a pain to really test. Just test manually which is trivial.
		logger.setCallInfo("GET", "foo", "0.0.0.0");
		
		assertThat("incorrect ID", logger.getCallID(), is(nullLogger ? (String) null : "foo"));
	}
	
	@Test
	public void configFailNoEnvPath() throws Throwable {
		failConfig(new FileOpener(), new AssemblyHomologyConfigurationException(
				"Could not find deployment configuration file from either permitted " +
				"environment variable / system property: ASSEMBLY_HOMOLOGY_CONFIG, " +
				"KB_DEPLOYMENT_CONFIG"));
	}
	
	@Test
	public void configFailWhiteSpaceEnvPath() throws Throwable {
		// can't put nulls into the sysprops or env
		failConfig("     \t    ", new FileOpener(), new AssemblyHomologyConfigurationException(
					"Could not find deployment configuration file from either permitted " +
					"environment variable / system property: ASSEMBLY_HOMOLOGY_CONFIG, " +
					"KB_DEPLOYMENT_CONFIG"));
	}
	
	@Test
	public void configFail1ArgExceptionOnOpen() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file"))).thenThrow(new IOException("yay"));
		
		failConfig("some file", fo, new AssemblyHomologyConfigurationException(
				"Could not read configuration file some file: yay"));
	}
	
	@Test
	public void configFail3ArgExceptionOnOpen() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file"))).thenThrow(new IOException("yay"));
		
		failConfig(Paths.get("some file"), fo, new AssemblyHomologyConfigurationException(
				"Could not read configuration file some file: yay"));
	}
	
	@Test
	public void configFailBadIni() throws Throwable {
		failConfigBoth("foobar", new AssemblyHomologyConfigurationException(
				"Could not read configuration file some file: parse error (at line: 1): foobar"));
	}
	
	@Test
	public void configFailNoSection() throws Throwable {
		failConfigBoth("", new AssemblyHomologyConfigurationException(
				"No section assemblyhomology in config file some file"));
	}
	
	@Test
	public void configFailNoTempDir() throws Throwable {
		failConfigBoth(
				"[assemblyhomology]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar",
				new AssemblyHomologyConfigurationException(
						"Required parameter temp-dir not provided in configuration file " +
						"some file, section assemblyhomology"));
		
		failConfigBoth(
				"[assemblyhomology]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"temp-dir=     \t     \n",
				new AssemblyHomologyConfigurationException(
						"Required parameter temp-dir not provided in configuration file " +
						"some file, section assemblyhomology"));
	}
	
	@Test
	public void configFailMinhashTimeoutNotInt() throws Throwable {
		failConfigBoth(
				"[assemblyhomology]\n" +
				"minhash-timeout=baz\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar",
				new AssemblyHomologyConfigurationException(
						"Parameter minhash-timeout in configuration file " +
						"some file, section assemblyhomology, must be an integer, was baz"));
	}
	
	@Test
	public void configFailMinhashTimeoutBelowMinimum() throws Throwable {
		failConfigBoth(
				"[assemblyhomology]\n" +
				"minhash-timeout=0\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar",
				new AssemblyHomologyConfigurationException(
						"Parameter minhash-timeout in configuration file some file, section " +
						"assemblyhomology, must have a minimum value of 1, was 0"));
	}
	
	@Test
	public void configFailNoHost() throws Throwable {
		failConfigBoth(
				"[assemblyhomology]\n" +
				"temp-dir=foo\n" +
				"mongo-db=bar",
				new AssemblyHomologyConfigurationException(
						"Required parameter mongo-host not provided in configuration file " +
						"some file, section assemblyhomology"));
		
		failConfigBoth(
				"[assemblyhomology]\n" +
				"temp-dir=foo\n" +
				"mongo-db=bar\n" +
				"mongo-host=     \t     \n",
				new AssemblyHomologyConfigurationException(
						"Required parameter mongo-host not provided in configuration file " +
						"some file, section assemblyhomology"));
	}
	
	@Test
	public void configFailNoDB() throws Throwable {
		failConfigBoth(
				"[assemblyhomology]\n" +
				"mongo-host=foo\n" +
				"temp-dir=bar",
				new AssemblyHomologyConfigurationException(
						"Required parameter mongo-db not provided in configuration file " +
						"some file, section assemblyhomology"));
		
		failConfigBoth(
				"[assemblyhomology]\n" +
				"mongo-host=foo\n" +
				"temp-dir=bar\n" +
				"mongo-db=     \t     \n",
				new AssemblyHomologyConfigurationException(
						"Required parameter mongo-db not provided in configuration file " +
						"some file, section assemblyhomology"));
	}
	
	@Test
	public void configFailUserNoPwd() throws Throwable {
		failConfigBoth(
				"[assemblyhomology]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"temp-dir=baz\n" +
				"mongo-user=user",
				new AssemblyHomologyConfigurationException(
						"Must provide both mongo-user and mongo-pwd params in config file " +
						"some file section assemblyhomology if MongoDB authentication is to " +
						"be used"));
		
		failConfigBoth(
				"[assemblyhomology]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"temp-dir=baz\n" +
				"mongo-user=user\n" +
				"mongo-pwd=   \t    ",
				new AssemblyHomologyConfigurationException(
						"Must provide both mongo-user and mongo-pwd params in config file " +
						"some file section assemblyhomology if MongoDB authentication is to " +
						"be used"));
	}
	
	@Test
	public void configFailPwdNoUser() throws Throwable {
		failConfigBoth(
				"[assemblyhomology]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"temp-dir=baz\n" +
				"mongo-pwd=pwd",
				new AssemblyHomologyConfigurationException(
						"Must provide both mongo-user and mongo-pwd params in config file " +
						"some file section assemblyhomology if MongoDB authentication is to " +
						"be used"));
		
		failConfigBoth(
				"[assemblyhomology]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"temp-dir=baz\n" +
				"mongo-pwd=pwd\n" +
				"mongo-user=   \t    ",
				new AssemblyHomologyConfigurationException(
						"Must provide both mongo-user and mongo-pwd params in config file " +
						"some file section assemblyhomology if MongoDB authentication is to " +
						"be used"));
	}
	
	@Test
	public void configFailNoFilterClass() throws Throwable {
		failConfigBoth(
				"[assemblyhomology]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"temp-dir=baz\n" +
				"filters=foo\n",
				new AssemblyHomologyConfigurationException(
						"Required parameter filter-foo-factory-class not provided in " +
						"configuration file some file, section assemblyhomology"));
		
		failConfigBoth(
				"[assemblyhomology]\n" +
				"mongo-host=foo\n" +
				"mongo-db=bar\n" +
				"temp-dir=baz\n" +
				"filters=foo\n" +
				"filter-foo-factory-class=\n",
				new AssemblyHomologyConfigurationException(
						"Required parameter filter-foo-factory-class not provided in " +
						"configuration file some file, section assemblyhomology"));
	}
	
	private InputStream toStr(final String input) {
		return new ByteArrayInputStream(input.getBytes());
	}
	
	private void failConfig(final FileOpener opener, final Exception expected) throws Throwable {
		try {
			getConfig(opener);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	private void failConfig(
			final String filename,
			final FileOpener opener,
			final Exception expected)
			throws Throwable {
		try {
			TestCommon.getenv().put(AssemblyHomologyConfig.ENV_VAR_KB_DEP, filename);
			failConfig(opener, expected);
		} finally {
			TestCommon.getenv().remove(AssemblyHomologyConfig.ENV_VAR_KB_DEP);
		}
	}
	
	private void failConfig1Arg(final String fileContents, final Exception expected)
			throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file"))).thenReturn(toStr(fileContents));
		
		failConfig("some file", fo, expected);
	}
	
	private void failConfig(
			final Path pathToIni,
			final FileOpener opener,
			final Exception expected)
			throws Throwable {
		try {
			getConfig(pathToIni, false, opener);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	private void failConfig3Arg(final String fileContents, final Exception expected)
			throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file"))).thenReturn(toStr(fileContents));
		
		failConfig(Paths.get("some file"), fo, expected);
	}
	
	private void failConfigBoth(final String fileContents, final Exception expected)
			throws Throwable {
		failConfig1Arg(fileContents, expected);
		failConfig3Arg(fileContents, expected);
	}
}
