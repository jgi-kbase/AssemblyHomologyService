package us.kbase.test.assemblyhomology.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.assemblyhomology.TestCommon.set;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.FileUtils;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.assemblyhomology.core.exceptions.AssemblyHomologyException;
import us.kbase.assemblyhomology.core.exceptions.IncompatibleSketchesException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.common.test.RegexMatcher;
import us.kbase.test.assemblyhomology.MapBuilder;
import us.kbase.test.assemblyhomology.MongoStorageTestManager;
import us.kbase.test.assemblyhomology.StandaloneAssemblyHomologyServer;
import us.kbase.test.assemblyhomology.StandaloneAssemblyHomologyServer.ServerThread;
import us.kbase.test.assemblyhomology.data.TestDataManager;
import us.kbase.test.assemblyhomology.service.api.RootTest;
import us.kbase.test.assemblyhomology.TestCommon;

public class ServiceIntegrationTest {
	
	/* These tests check basic integration of the various classes that comprise the service.
	 * They are not intended to provide high levels of coverage - that is the purpose of the
	 * unit tests.
	 * 
	 * These tests just ensure that the basic end to end operations work, and usually test
	 * one happy test and one unhappy test per endpoint.
	 */

	/* Not tested via integration tests:
	 * 1) ignoring IP headers - test manually for now.
	 * 2) logging - test manually.
	 * 3) Mongo with auth - test manually.
	 * 4) The 4 ways of specifying the config file path
	 * 5) Some of the startup code for the server, dealing with cases where there's already
	 * a MongoClient created (not sure if this can actually happen)
	 */
	
	private static final String DB_NAME = "test_assyhomol_service";
	
	private static final Client CLI = ClientBuilder.newClient();
	
	private static MongoStorageTestManager MANAGER = null;
	private static StandaloneAssemblyHomologyServer SERVER = null;
	private static int PORT = -1;
	private static String HOST = null;
	private static Path TEMP_DIR = null;
	
	private static final Path QUERY_K31_S1500 = Paths.get("kb_15792_446_1_k31_s1500.msh");
	private static final Path TARGET_4SEQS = Paths.get("kb_4seqs_k31_s1000.msh");
	private static final Path TARGET_4SEQS_2 = Paths.get("kb_4seqs_k31_s1000_2.msh");
	
	private static final Map<String, Object> EXPECTED_NS1 = MapBuilder.<String, Object>newHashMap()
			.with("id", "id1")
			.with("impl", "mash")
			.with("sketchsize", 1000)
			.with("database", "default")
			.with("datasource", "KBase")
			.with("seqcount", 4)
			.with("kmersize", Arrays.asList(31))
			.with("scaling", null)
			.with("desc", "desc1")
			.build();
	
	private static final Map<String, Object> EXPECTED_NS2 = MapBuilder.<String, Object>newHashMap()
			.with("id", "id2")
			.with("impl", "mash")
			.with("scaling", null)
			.with("database", "default")
			.with("datasource", "KBase")
			.with("seqcount", 4)
			.with("kmersize", Arrays.asList(31))
			.with("sketchsize", 1000)
			.with("desc", "desc2")
			.build();
	
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
		final Path cfgfile = generateTempConfigFile(MANAGER, DB_NAME, serviceTempDir);
		TestCommon.getenv().put("ASSEMBLY_HOMOLOGY_CONFIG", cfgfile.toString());
		SERVER = new StandaloneAssemblyHomologyServer();
		new ServerThread(SERVER).start();
		System.out.println("Main thread waiting for server to start up");
		while (SERVER.getPort() == null) {
			Thread.sleep(1000);
		}
		PORT = SERVER.getPort();
		HOST = "http://localhost:" + PORT;
	}
	
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
		if (SERVER != null) {
			SERVER.stop();
		}
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
	
	private void loadNamespaces() throws Exception {
		MANAGER.storage.createOrReplaceNamespace(Namespace.getBuilder(
				new NamespaceID("id1"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("id1"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
						new MinHashDBLocation(TEMP_DIR.resolve(TARGET_4SEQS)),
						4),
				new LoadID("load1"),
				Instant.ofEpochMilli(10000))
				.withNullableDescription("desc1")
				.build());
		
		MANAGER.storage.createOrReplaceNamespace(Namespace.getBuilder(
				new NamespaceID("id2"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("id2"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
						new MinHashDBLocation(TEMP_DIR.resolve(TARGET_4SEQS_2)),
						4),
				new LoadID("foo"),
				Instant.ofEpochMilli(10000))
				.withNullableDescription("desc2")
				.build());
	}
	
	public static void failRequestJSON(
			final Response res,
			final int httpCode,
			final String httpStatus,
			final AssemblyHomologyException e)
			throws Exception {
		
		if (res.getStatus() != httpCode) {
			String text = null; 
			try {
				text = res.readEntity(String.class);
			} catch (Exception exp) {
				exp.printStackTrace();
			}
			if (text == null) {
				text = "Unable to get entity text - see error stream for exception";
			}
			fail(String.format("unexpected http code %s, wanted %s. Entity contents:\n%s",
					res.getStatus(), httpCode, text));
		}
		
		@SuppressWarnings("unchecked")
		final Map<String, Object> error = res.readEntity(Map.class);
		
		assertErrorCorrect(httpCode, httpStatus, e, error);
	}
	
	public static void assertErrorCorrect(
			final int expectedHTTPCode,
			final String expectedHTTPStatus,
			final AssemblyHomologyException expectedException,
			final Map<String, Object> error) {
		
		final Map<String, Object> innerExpected = new HashMap<>();
		innerExpected.put("httpcode", expectedHTTPCode);
		innerExpected.put("httpstatus", expectedHTTPStatus);
		innerExpected.put("appcode", expectedException.getErr().getErrorCode());
		innerExpected.put("apperror", expectedException.getErr().getError());
		innerExpected.put("message", expectedException.getMessage());
		
		final Map<String, Object> expected = ImmutableMap.of("error", innerExpected);
		
		if (!error.containsKey("error")) {
			fail("error object has no error key");
		}
		
		@SuppressWarnings("unchecked")
		final Map<String, Object> inner = (Map<String, Object>) error.get("error");
		
		final String callid = (String) inner.get("callid");
		final long time = (long) inner.get("time");
		inner.remove("callid");
		inner.remove("time");
		
		assertThat("incorrect error structure less callid and time", error, is(expected));
		assertThat("incorrect call id", callid, RegexMatcher.matches("\\d{16}"));
		TestCommon.assertCloseToNow(time);
	}
	
	@Test
	public void root() throws Exception {
		final URI target = UriBuilder.fromUri(HOST).path("/").build();
		
		final WebTarget wt = CLI.target(target);
		final Builder req = wt.request();

		final Response res = req.get();
		
		assertThat("incorrect response code", res.getStatus(), is(200));
		
		@SuppressWarnings("unchecked")
		final Map<String, Object> r = res.readEntity(Map.class);
		
		final long servertime = (long) r.get("servertime");
		r.remove("servertime");
		TestCommon.assertCloseToNow(servertime);
		
		final String gitcommit = (String) r.get("gitcommithash");
		r.remove("gitcommithash");
		RootTest.assertGitCommitFromRootAcceptable(gitcommit);
		
		final Map<String, Object> expected = ImmutableMap.of(
				"version", RootTest.SERVER_VER,
				"servname", "Assembly Homology service");
		
		assertThat("root json incorrect", r, is(expected));
	}
	
	@Test
	public void listNamespaces() throws Exception {
		loadNamespaces();
		
		final URI target = UriBuilder.fromUri(HOST).path("/namespace").build();
		
		final WebTarget wt = CLI.target(target);
		final Builder req = wt.request();

		final Response res = req.get();
		
		assertThat("incorrect response code", res.getStatus(), is(200));
		
		final List<?> response = res.readEntity(List.class);
		
		assertThat("incorrect namespaces", new HashSet<>(response),
				is(set(EXPECTED_NS1, EXPECTED_NS2)));
	}
	
	@Test
	public void getNamespace() throws Exception {
		loadNamespaces();
		
		final URI target = UriBuilder.fromUri(HOST).path("/namespace/id2").build();
		
		final WebTarget wt = CLI.target(target);
		final Builder req = wt.request();

		final Response res = req.get();
		
		assertThat("incorrect response code", res.getStatus(), is(200));
		
		@SuppressWarnings("unchecked")
		final Map<String, Object> response = res.readEntity(Map.class);
		
		assertThat("incorrect namespace", response, is(EXPECTED_NS2));
	}
	
	@Test
	public void getNamespaceFail() throws Exception {
		loadNamespaces();

		final URI target = UriBuilder.fromUri(HOST).path("/namespace/id3").build();
		
		final WebTarget wt = CLI.target(target);
		final Builder req = wt.request();

		final Response res = req.get();
		
		failRequestJSON(res, 404, "Not Found", new NoSuchNamespaceException("id3"));
	}
	
	@Test
	public void searchNamespace() throws Exception {
		loadNamespaces();
		
		final Instant now = Instant.ofEpochMilli(10000);
		
		MANAGER.storage.saveSequenceMetadata(
				new NamespaceID("id1"),
				new LoadID("load1"),
				Arrays.asList(
						SequenceMetadata.getBuilder("15792_446_1", "15792/446/1", now)
								.withNullableScientificName("sci name")
								.withRelatedID("foo", "bar")
								.build(),
						SequenceMetadata.getBuilder("15792_431_1", "15792/431/1", now).build(),
						SequenceMetadata.getBuilder("15792_3029_1", "15792/3029/1", now).build(),
						SequenceMetadata.getBuilder("15792_341_2", "15792/341/2", now).build()));

		final URI target = UriBuilder.fromUri(HOST).path("/namespace/id1/search")
				.queryParam("notstrict", "foo")
				.queryParam("max", 2)
				.build();
		
		final WebTarget wt = CLI.target(target);
		final Builder req = wt.request();

		final Response res = req.post(Entity.entity(
				Files.newInputStream(TEMP_DIR.resolve(QUERY_K31_S1500)),
				MediaType.APPLICATION_OCTET_STREAM));
		
		assertThat("incorrect response code", res.getStatus(), is(200));

		@SuppressWarnings("unchecked")
		final Map<String, Object> response = res.readEntity(Map.class);
		
		final Map<String, Object> expected = ImmutableMap.of(
				"impl", "mash",
				"implver", "2.0",
				"namespaces", Arrays.asList(EXPECTED_NS1),
				"warnings", Arrays.asList("Namespace id1: Query sketch size 1500 is larger " +
						"than target sketch size 1000"),
				"distances", Arrays.asList(
						MapBuilder.<String, Object>newHashMap()
								.with("sourceid", "15792/446/1")
								.with("namespaceid", "id1")
								.with("sciname", "sci name")
								.with("dist", 0.0)
								.with("relatedids", ImmutableMap.of("foo", "bar"))
								.build(),
						MapBuilder.<String, Object>newHashMap()
								.with("sourceid", "15792/431/1")
								.with("namespaceid", "id1")
								.with("sciname", null)
								.with("dist", 0.00236402)
								.with("relatedids", Collections.emptyMap())
								.build()
						)
				);
		
		assertThat("incorrect response", response, is(expected));
	}
	
	@Test
	public void searchNamespaceFail() throws Exception {
		loadNamespaces();
		
		final URI target = UriBuilder.fromUri(HOST).path("/namespace/id1/search")
				.queryParam("max", 2)
				.build();
		
		final WebTarget wt = CLI.target(target);
		final Builder req = wt.request();

		final Response res = req.post(Entity.entity(
				Files.newInputStream(TEMP_DIR.resolve(QUERY_K31_S1500)),
				MediaType.APPLICATION_OCTET_STREAM));
		
		failRequestJSON(res, 400, "Bad Request", new IncompatibleSketchesException(
				"Unable to query namespace id1 with input sketch: " +
				"Query sketch size 1500 does not match target 1000"));
	}
}
