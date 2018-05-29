package us.kbase.test.assemblyhomology.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.config.AssemblyHomologyConfig;
import us.kbase.assemblyhomology.config.AssemblyHomologyConfigurationException;
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
	public void sysPropAssemHomolNoUserNoIgnoreIP() throws Throwable {
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
		assertThat("incorrect temp dir", cfg.getPathToTemporaryFileDirectory(),
				is(Paths.get("/foo/bar/baz")));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(false));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void sysPropKBNoUserNoIgnoreIP() throws Throwable {
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
					 "dont-trust-x-ip-headers=true1\n" +
					 "temp-dir=/foo/bar/baz")
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
		assertThat("incorrect temp dir", cfg.getPathToTemporaryFileDirectory(),
				is(Paths.get("/foo/bar/baz")));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(false));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void envVarAssemHomolWithUserWithIgnoreIP() throws Throwable {
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
					 "dont-trust-x-ip-headers=true\n" +
					 "temp-dir=/foo/bar/baz")
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
		assertThat("incorrect temp dir", cfg.getPathToTemporaryFileDirectory(),
				is(Paths.get("/foo/bar/baz")));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(true));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void envVarKBWithUserWithIgnoreIP() throws Throwable {
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
		assertThat("incorrect temp dir", cfg.getPathToTemporaryFileDirectory(),
				is(Paths.get("/foo/bar/baz")));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(true));
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
		assertThat("incorrect temp dir", cfg.getPathToTemporaryFileDirectory(),
				is(Paths.get("/foo/bar/baz")));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(false));
		testLogger(cfg.getLogger(), false);
	}
	
	@Test
	public void pathWithUserWithIgnoreIPNullLogger() throws Throwable {
		final FileOpener fo = mock(FileOpener.class);
		when(fo.open(Paths.get("some file2"))).thenReturn(new ByteArrayInputStream(
				("[assemblyhomology]\n" +
				 "mongo-host=mongo\n" +
				 "mongo-db=database\n" +
				 "mongo-user=userfoo\n" +
				 "mongo-pwd=somepwd\n" +
				 "dont-trust-x-ip-headers=true\n" +
				 "temp-dir=/foo/bar/baz")
				.getBytes()));
		final AssemblyHomologyConfig cfg = getConfig(Paths.get("some file2"), true, fo);
		
		assertThat("incorrect mongo host", cfg.getMongoHost(), is("mongo"));
		assertThat("incorrect mongo db", cfg.getMongoDatabase(), is("database"));
		assertThat("incorrect mongo user", cfg.getMongoUser(), is(Optional.of("userfoo")));
		assertThat("incorrect mongo pwd", cfg.getMongoPwd().get(),
				equalTo("somepwd".toCharArray()));
		assertThat("incorrect temp dir", cfg.getPathToTemporaryFileDirectory(),
				is(Paths.get("/foo/bar/baz")));
		assertThat("incorrect ignore ip headers", cfg.isIgnoreIPHeaders(), is(true));
		testLogger(cfg.getLogger(), true);
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
