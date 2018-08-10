package us.kbase.test.assemblyhomology.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import us.kbase.assemblyhomology.cli.AssemblyHomologyCLI;
import us.kbase.assemblyhomology.core.DataSourceID;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.assemblyhomology.core.SequenceMetadata.Builder;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchSequenceException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.storage.exceptions.AssemblyHomologyStorageException;
import us.kbase.test.assemblyhomology.MongoStorageTestManager;
import us.kbase.test.assemblyhomology.TestCommon;
import us.kbase.test.assemblyhomology.data.TestDataManager;

public class CLIIntegrationTest {
	
	/* These tests also act as unit tests for the CLI. */
	
	private static final boolean PRINT_STREAMS = false;
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final String DB_NAME = "test_assyhomol_cli";
	
	private static MongoStorageTestManager MANAGER = null;
	private static Path TEMP_DIR = null;
	private static Path CONFIG_FILE = null;
	
	private static final Path QUERY_K31_S1500 = Paths.get("kb_15792_446_1_k31_s1500.msh");
	private static final Path TARGET_4SEQS = Paths.get("kb_4seqs_k31_s1000.msh");
	private static final Path TARGET_4SEQS_2 = Paths.get("kb_4seqs_k31_s1000_2.msh");

	@BeforeClass
	public static void beforeClass() throws Exception {
		TestCommon.stfuLoggers();
		TEMP_DIR = TestCommon.getTempDir().resolve("ServiceIntegTest_" +
					UUID.randomUUID().toString());
		final Path serviceTempDir = TEMP_DIR.resolve("temp_files");
		Files.createDirectories(serviceTempDir);
		for (final Path f: Arrays.asList(QUERY_K31_S1500, TARGET_4SEQS, TARGET_4SEQS_2)) {
			TestDataManager.install(f, TEMP_DIR.resolve(f));
		}
		
		MANAGER = new MongoStorageTestManager(DB_NAME);
		CONFIG_FILE = generateTempConfigFile(MANAGER, DB_NAME, serviceTempDir);
	}
	
	// duplicated in the other integration test, move to helper
	public static Path generateTempConfigFile(
			final MongoStorageTestManager manager,
			final String dbName,
			final Path tempDir)
			throws IOException {
		
		final Ini ini = new Ini();
		final Section sec = ini.add("assemblyhomology");
		sec.add("mongo-host", "localhost:" + manager.mongo.getServerPort());
		sec.add("mongo-db", dbName);
		sec.add("temp-dir", tempDir.toString());
		
		final Path deploy = Files.createTempFile(TEMP_DIR, "cli_test_deploy", ".cfg");
		ini.store(deploy.toFile());
		deploy.toFile().deleteOnExit();
		System.out.println("Generated temporary config file " + deploy);
		return deploy.toAbsolutePath();
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		if (MANAGER != null) {
			MANAGER.destroy();
		}
		final boolean deleteTempFiles = TestCommon.isDeleteTempFiles();
		if (TEMP_DIR != null && Files.exists(TEMP_DIR) && deleteTempFiles) {
			FileUtils.deleteQuietly(TEMP_DIR.toFile());
		}
	}
	
	@Before
	public void clean() {
		TestCommon.destroyDB(MANAGER.db);
	}
	
	@Test
	public void help() throws Exception {
		final String expectedOut = TestDataManager.get(Paths.get("ah_help.txt"));
		testCLI(0, expectedOut, "", "-h");
		testCLI(0, expectedOut, "", "--help");
	}
	
	@Test
	public void noCommand() throws Exception {
		final String expectedOut = TestDataManager.get(Paths.get("ah_help.txt"));
		testCLI(1, expectedOut, "");
	}
	
	@Test
	public void noArgsToLoad() throws Exception {
		final String expectedOut = "Error: The following options are required: " +
				"[-k | --sketch-db], [-n | --namespace-yaml], [-s | --sequence-metadata]\n";
		testCLI(1, "", expectedOut, "load");
	}
	
	@Test
	public void noArgsToLoadVerbose() throws Exception {
		final List<String> errorSubStrings = Arrays.asList(
				"Error: The following options are required: " +
				"[-k | --sketch-db], [-n | --namespace-yaml], [-s | --sequence-metadata]\n",
				"com.beust.jcommander.ParameterException"
				);
		testCLI(1, errorSubStrings, false, "-v", "load");
		
		testCLI(1, errorSubStrings, false, "--verbose", "load");
	}
	
	@Test
	public void illegalCommand() throws Exception {
		testCLI(1, "", "Error: Expected a command, got fake\n", "fake");
	}
	
	@Test
	public void illegalCommandVerbose() throws Exception {
		final List<String> errorSubStrings = Arrays.asList(
				"Error: Expected a command, got fake",
				"com.beust.jcommander.MissingCommandException"
				);
		testCLI(1, errorSubStrings, false, "-v", "fake");
		
		testCLI(1, errorSubStrings, false, "--verbose", "fake");
	}
	
	@Test
	public void positionalParam() throws Exception {
		final String errorString = "Error: A positional parameter was provided but this " +
				"command does not accept positional parameters\n";
		testCLI(1, "", errorString, "load", "-a");
	}
	
	@Test
	public void positionalParamVerbose() throws Exception {
		final List<String> errorSubStrings = Arrays.asList(
				"Error: A positional parameter was provided but this " +
						"command does not accept positional parameters\n",
				"com.beust.jcommander.ParameterException"
				);
		testCLI(1, errorSubStrings, false, "-v", "load", "fake");
		
		testCLI(1, errorSubStrings, false, "--verbose", "load", "fake");
	}
	
	@Test
	public void loadNoIDNoImplProvidedShortArgs() throws Exception {
		final Path sketchDB = TEMP_DIR.resolve(TARGET_4SEQS);
		final Path nsInfo = TEMP_DIR.resolve("namespace.yaml");
		final Path seqInfo = TEMP_DIR.resolve("seqmeta.jsonlines");
		final List<Map<String, Object>> seqmeta = createInputFiles(nsInfo, seqInfo);
		final CLIRunResult crr = testCLI(0, Arrays.asList("Generated load id"), true,
				"-c", CONFIG_FILE.toString(),
				"load",
				"-k", sketchDB.toString(),
				"-n", nsInfo.toString(),
				"-s", seqInfo.toString());
		
		final LoadID loadID = new LoadID(last(crr.out.split("\\s+")));
		
		checkNamespaceAndSeqs(sketchDB, seqmeta, loadID);
	}
	
	@Test
	public void loadIDAndImplProvidedShortArgs() throws Exception {
		final Path sketchDB = TEMP_DIR.resolve(TARGET_4SEQS);
		final Path nsInfo = TEMP_DIR.resolve("namespace.yaml");
		final Path seqInfo = TEMP_DIR.resolve("seqmeta.jsonlines");
		final List<Map<String, Object>> seqmeta = createInputFiles(nsInfo, seqInfo);
		testCLI(0, "", "",
				"-c", CONFIG_FILE.toString(),
				"load",
				"-i", "mash",
				"-l", "myneatid",
				"-k", sketchDB.toString(),
				"-n", nsInfo.toString(),
				"-s", seqInfo.toString());
		
		final LoadID loadID = new LoadID("myneatid");
		
		checkNamespaceAndSeqs(sketchDB, seqmeta, loadID);
	}
	
	@Test
	public void loadIDAndImplProvidedLongArgs() throws Exception {
		final Path sketchDB = TEMP_DIR.resolve(TARGET_4SEQS);
		final Path nsInfo = TEMP_DIR.resolve("namespace.yaml");
		final Path seqInfo = TEMP_DIR.resolve("seqmeta.jsonlines");
		final List<Map<String, Object>> seqmeta = createInputFiles(nsInfo, seqInfo);
		testCLI(0, "", "",
				"--config", CONFIG_FILE.toString(),
				"load",
				"--implementation", "mash",
				"--load-id", "myneatid",
				"--sketch-db", sketchDB.toString(),
				"--namespace-yaml", nsInfo.toString(),
				"--sequence-metadata", seqInfo.toString());
		
		final LoadID loadID = new LoadID("myneatid");
		
		checkNamespaceAndSeqs(sketchDB, seqmeta, loadID);
	}
	
	@Test
	public void badConfig() throws Exception {
		testCLI(1, "", "Error: Could not read configuration file " + TEMP_DIR.toString() +
				": Is a directory\n",
				"--config", TEMP_DIR.toString(), "load", "-k", "foo", "-n", "bar", "-s", "baz");
	}
	
	@Test
	public void badConfigVerbose() throws Exception {
		final List<String> errStr = Arrays.asList(
				"Error: Could not read configuration file " + TEMP_DIR.toString() +
				": Is a directory\n",
				"us.kbase.assemblyhomology.config.AssemblyHomologyConfigurationException");
		testCLI(1, errStr, false,
				"-v", "--config", TEMP_DIR.toString(),
				"load", "-k", "foo", "-n", "bar", "-s", "baz");
		testCLI(1, errStr, false,
				"--verbose", "--config", TEMP_DIR.toString(),
				"load", "-k", "foo", "-n", "bar", "-s", "baz");
	}
	
	@Test
	public void badImpl() throws Exception {
		// done enough verbose testing at this point
		testCLI(1, "", "Error: Unsupported implementation: supermash\n",
				"--config", CONFIG_FILE.toString(),
				"load", "-i", "supermash", "-k", "foo", "-n", "bar", "-s", "baz");
		
		testCLI(1, "", "Error: Unsupported implementation: supermash\n",
				"--config", CONFIG_FILE.toString(),
				"load", "--implementation", "supermash", "-k", "foo", "-n", "bar", "-s", "baz");
	}
	
	@Test
	public void badLoadDB() throws Exception {
		// enough verbose testing
		final Path nsInfo = TEMP_DIR.resolve("namespace.yaml");
		final Path seqInfo = TEMP_DIR.resolve("seqmeta.jsonlines");
		createInputFiles(nsInfo, seqInfo);
		testCLI(1, "", "Error: " + TEMP_DIR.toString() + " is not a mash sketch\n",
				"-c", CONFIG_FILE.toString(),
				"load",
				"-l", "loadid",
				"-k", TEMP_DIR.toString(),
				"-n", nsInfo.toString(),
				"-s", seqInfo.toString());
	}

	private void checkNamespaceAndSeqs(final Path sketchDB, final List<Map<String, Object>> seqmeta,
			final LoadID loadID) throws AssemblyHomologyStorageException, NoSuchNamespaceException,
			MissingParameterException, IllegalParameterException, NoSuchSequenceException {
		final Namespace ns = MANAGER.storage.getNamespace(new NamespaceID("id1"));
		
		final Namespace expected = Namespace.getBuilder(
				new NamespaceID("id1"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("id1"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
						new MinHashDBLocation(sketchDB),
						4),
				loadID,
				ns.getModification())
				.withNullableDataSourceID(new DataSourceID("JGI"))
				.withNullableDescription("desc")
				.withNullableSourceDatabaseID("IMG")
				.build();
		
		assertThat("incorrect namespace", ns, is(expected));
		
		final List<SequenceMetadata> sm = MANAGER.storage.getSequenceMetadata(
				new NamespaceID("id1"), loadID, Arrays.asList(
						"15792_446_1", "15792_431_1", "15792_3029_1", "15792_341_2"));
		
		final List<SequenceMetadata> expectedsm = toSeqMeta(seqmeta, sm.get(0).getCreation());
		
		assertThat("incorrect seqmeta", new HashSet<>(sm), is(new HashSet<>(expectedsm)));
	}

	private List<Map<String, Object>> createInputFiles(final Path nsInfo, final Path seqInfo)
			throws IOException, JsonProcessingException {
		try (final BufferedWriter nsWriter = Files.newBufferedWriter(
				nsInfo, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
			nsWriter.write("id: id1\ndatasource: JGI\nsourcedatabase: IMG\ndescription: desc");
		}
		final List<Map<String, Object>> seqmeta = Arrays.asList(
				ImmutableMap.of(
						"id", "15792_446_1",
						"sourceid", "15792/446/1",
						"sciname", "sci name",
						"relatedids", ImmutableMap.of("foo", "bar")),
				ImmutableMap.of(
						"id", "15792_431_1",
						"sourceid", "15792/431/1"),
				ImmutableMap.of(
						"id", "15792_3029_1",
						"sourceid", "15792/2039/1",
						"sciname", "sci name2",
						"relatedids", Collections.emptyMap()),
				ImmutableMap.of(
						"id", "15792_341_2",
						"sourceid", "15792/341/2")
				);
		try (final BufferedWriter seqWriter = Files.newBufferedWriter(
				seqInfo, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
			for (final Map<String, Object> seqm: seqmeta) {
				seqWriter.write(MAPPER.writeValueAsString(seqm) + "\n");
			}
		}
		return seqmeta;
	}

	private List<SequenceMetadata> toSeqMeta(
			final List<Map<String, Object>> seqmeta,
			final Instant creation) {
		final List<SequenceMetadata> ret = new LinkedList<>();
		for (final Map<String, Object> smin: seqmeta) {
			final Builder sm = SequenceMetadata.getBuilder(
					(String) smin.get("id"), (String) smin.get("sourceid"), creation)
					.withNullableScientificName((String) smin.get("sciname"));
			@SuppressWarnings("unchecked")
			final Map<String, String> relids = (Map<String, String>) smin.get("relatedids");
			if (relids != null) {
				for (final Entry<String, String> e: relids.entrySet()) {
					sm.withRelatedID(e.getKey(), e.getValue());
				}
			}
			ret.add(sm.build());
		}
		return ret;
	}

	private String last(final String[] array) {
		return array[array.length - 1];
	}

	private static class CLIRunResult {
		private final int exitCode;
		private final String out;
		private final String err;
		
		private CLIRunResult(int exitCode, String out, String err) {
			this.exitCode = exitCode;
			this.out = out;
			this.err = err;
		}
	}
	
	private void testCLI(
			final int exitCode,
			final String expectedOut,
			final String expectedErr,
			final String... args) {
		final CLIRunResult crr = runCLI(args);
		
		assertThat("incorrect exit value", crr.exitCode, is(exitCode));
		assertThat("incorrect out", crr.out, is(expectedOut));
		assertThat("incorrect err", crr.err, is(expectedErr));
	}
	
	private CLIRunResult testCLI(
			final int exitCode,
			final List<String> substrings,
			final boolean out,
			final String... args) {
		final CLIRunResult crr = runCLI(args);
		
		assertThat("incorrect exit value", crr.exitCode, is(exitCode));
		final String checkContents = out ? crr.out : crr.err;
		final String expectEmpty = out ? crr.err : crr.out;
		assertThat("incorrect " + (out ? "err": "out"), expectEmpty, is(""));
		for (final String substring: substrings) {
			assertThat("incorrect " + (out ? "out": "err"), checkContents,
					containsString(substring));
		}
		return crr;
	}

	private CLIRunResult runCLI(final String... args) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final ByteArrayOutputStream err = new ByteArrayOutputStream();
		final int res = new AssemblyHomologyCLI(
				args,
				new PrintStream(out),
				new PrintStream(err))
				.execute();
		
		final CLIRunResult crr = new CLIRunResult(res, out.toString(), err.toString());
		if (PRINT_STREAMS) {
			System.out.println("*** STDOUT ***\n" + crr.out + "\n");
			System.out.println("*** STDERR ***\n" + crr.err + "\n************\n");
		}
		return crr;
	}
	
	@Test
	public void constructFail() {
		final PrintStream out = new PrintStream(new ByteArrayOutputStream());
		final PrintStream err = new PrintStream(new ByteArrayOutputStream());
		final String[] args = new String[0];
		failConstruct(null, out, err, new NullPointerException("args"));
		failConstruct(args, null, err, new NullPointerException("out"));
		failConstruct(args, out, null, new NullPointerException("err"));
	}
	
	private void failConstruct(
			final String[] args,
			final PrintStream out,
			final PrintStream err,
			final Exception expected) {
		try {
			new AssemblyHomologyCLI(args, out, err);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
			
	}
	
}
