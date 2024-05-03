package us.kbase.test.assemblyhomology.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.assemblyhomology.TestCommon.set;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.mongodb.client.MongoDatabase;

import us.kbase.assemblyhomology.core.FilterID;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.MinHashDistanceFilterFactory;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.assemblyhomology.core.Token;
import us.kbase.assemblyhomology.core.exceptions.AssemblyHomologyException;
import us.kbase.assemblyhomology.core.exceptions.AuthenticationException;
import us.kbase.assemblyhomology.core.exceptions.ErrorType;
import us.kbase.assemblyhomology.core.exceptions.IncompatibleSketchesException;
import us.kbase.assemblyhomology.core.exceptions.MinHashFilterFactoryInitializationException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.filters.KBaseAuthenticatedFilter;
import us.kbase.assemblyhomology.filters.KBaseAuthenticatedFilterFactory;
import us.kbase.assemblyhomology.minhash.DefaultDistanceCollector;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashDistanceCollector;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashDistanceFilterAuthenticationException;
import us.kbase.auth.AuthToken;
import us.kbase.testutils.RegexMatcher;
import us.kbase.test.assemblyhomology.MapBuilder;
import us.kbase.test.assemblyhomology.MongoStorageTestManager;
import us.kbase.test.assemblyhomology.StandaloneAssemblyHomologyServer;
import us.kbase.test.assemblyhomology.StandaloneAssemblyHomologyServer.ServerThread;
import us.kbase.test.assemblyhomology.data.TestDataManager;
import us.kbase.test.assemblyhomology.service.api.RootTest;
import us.kbase.test.auth2.authcontroller.AuthController;
import us.kbase.workspace.CreateWorkspaceParams;
import us.kbase.workspace.SetPermissionsParams;
import us.kbase.workspace.WorkspaceClient;
import us.kbase.test.assemblyhomology.TestCommon;
import us.kbase.test.assemblyhomology.controllers.workspace.WorkspaceController;

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
	private static AuthController AUTH = null;
	private static WorkspaceController WS = null;
	private static URL WS_URL;
	private static WorkspaceClient WS_CLI1 = null;
	private static WorkspaceClient WS_CLI2 = null;
	private static MongoDatabase WSDB = null;
	private static StandaloneAssemblyHomologyServer SERVER = null;
	private static int PORT = -1;
	private static String HOST = null;
	private static Path TEMP_DIR = null;

	private static String TOKEN1 = null;
	private static String TOKEN2 = null;


	private static final Path QUERY_K31_S1500 = Paths.get("kb_15792_446_1_k31_s1500.msh");
	private static final Path TARGET_4SEQS = Paths.get("kb_4seqs_k31_s1000.msh");
	private static final Path TARGET_4SEQS_2 = Paths.get("kb_4seqs_k31_s1000_2.msh");
	// actually 6 seqs. rename file & vars at some point
	private static final Path TARGET_4SEQS_WS = Paths.get("kb_4seqsLowWSNums_k31_s1000.msh");
	
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
			.with("lastmod", 100000)
			.with("authsource", null)
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
			.with("lastmod", 300000)
			.with("authsource", null)
			.build();
	
	private static final Map<String, Object> EXP_NSFILTER = MapBuilder.<String, Object>newHashMap()
			.with("id", "kbasefilter")
			.with("impl", "mash")
			.with("scaling", null)
			.with("database", "default")
			.with("datasource", "KBase")
			.with("seqcount", 6)
			.with("kmersize", Arrays.asList(31))
			.with("sketchsize", 1000)
			.with("desc", "desc3")
			.with("lastmod", 500000)
			.with("authsource", "kbaseappdev")
			.build();
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		TestCommon.stfuLoggers();
		TEMP_DIR = TestCommon.getTempDir().resolve("ServiceIntegTest_" +
					UUID.randomUUID().toString());
		final Path serviceTempDir = TEMP_DIR.resolve("temp_files");
		Files.createDirectories(serviceTempDir);
		for (final Path f: Arrays.asList(QUERY_K31_S1500, TARGET_4SEQS, TARGET_4SEQS_2,
				TARGET_4SEQS_WS)) {
			TestDataManager.install(f, TEMP_DIR.resolve(f));
		}
		
		MANAGER = new MongoStorageTestManager(DB_NAME);

		// set up auth
		AUTH = new AuthController(
				"localhost:" + MANAGER.mongo.getServerPort(),
				"AssemblyHomologyServiceIntgrationTestAuth",
				TEMP_DIR);
		final URL authURL = new URL("http://localhost:" + AUTH.getServerPort() + "/testmode");
		System.out.println("started auth server at " + authURL);
		TestCommon.createAuthUser(authURL, "user1", "display1");
		TOKEN1 = TestCommon.createLoginToken(authURL, "user1");
		TestCommon.createAuthUser(authURL, "user2", "display2");
		TOKEN2 = TestCommon.createLoginToken(authURL, "user2");

		// set up Workspace
		WS = new WorkspaceController(
				TestCommon.getJarsDir(),
				"localhost:" + MANAGER.mongo.getServerPort(),
				"AssemblyHomologyServiceIntegTestWSDB",
				"fakeadmin",
				authURL,
				TEMP_DIR);
		WSDB = MANAGER.mc.getDatabase("AssemblyHomologyServiceIntegTestWSDB");

		WS_URL = new URL("http://localhost:" + WS.getServerPort());
		WS_CLI1 = new WorkspaceClient(WS_URL, new AuthToken(TOKEN1, "user1"));
		WS_CLI1.setIsInsecureHttpConnectionAllowed(true);
		WS_CLI2 = new WorkspaceClient(WS_URL, new AuthToken(TOKEN2, "user2"));
		WS_CLI2.setIsInsecureHttpConnectionAllowed(true);
		System.out.println(String.format("Started workspace service %s at %s",
				WS_CLI1.ver(), WS_URL));

		// set up the AH server
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
		sec.add("filters", "kbase");
		sec.add("filter-kbase-factory-class", KBaseAuthenticatedFilterFactory.class.getName());
		sec.add("filter-kbase-init-workspace-url", WS_URL.toString());
		sec.add("filter-kbase-init-env", "appdev");
		
		final Path deploy = Files.createTempFile(TEMP_DIR, "cli_test_deploy", ".cfg");
		ini.store(deploy.toFile());
		deploy.toFile().deleteOnExit();
		System.out.println("Generated temporary config file " + deploy);
		return deploy.toAbsolutePath();
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		final boolean deleteTempFiles = TestCommon.isDeleteTempFiles();
		if (SERVER != null) {
			SERVER.stop();
		}
		if (WS != null) {
			WS.destroy(deleteTempFiles);
		}
		if (AUTH != null) {
			AUTH.destroy(deleteTempFiles);
		}
		if (MANAGER != null) {
			MANAGER.destroy();
		}
		if (TEMP_DIR != null && Files.exists(TEMP_DIR) && deleteTempFiles) {
			FileUtils.deleteQuietly(TEMP_DIR.toFile());
		}
	}
	
	@Before
	public void clean() {
		TestCommon.destroyDB(MANAGER.db);
		TestCommon.destroyDB(WSDB);
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
				Instant.ofEpochMilli(100000))
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
				Instant.ofEpochMilli(300000))
				.withNullableDescription("desc2")
				.build());
		
		MANAGER.storage.createOrReplaceNamespace(Namespace.getBuilder(
				new NamespaceID("kbasefilter"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("kbasefilter"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
						new MinHashDBLocation(TEMP_DIR.resolve(TARGET_4SEQS_WS)),
						6),
				new LoadID("load2"),
				Instant.ofEpochMilli(500000))
				.withNullableFilterID(new FilterID("kbaseappdev"))
				.withNullableDescription("desc3")
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
				is(set(EXPECTED_NS1, EXPECTED_NS2, EXP_NSFILTER)));
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
	public void searchNamespacesWithFilter() throws Exception {
		/* mash output for the sequence file used for this test:
		 
		mash dist kb_4seqsLowWSNums_k31_s1000.msh kb_15792_446_1_k31_s1000.msh
		1_341_2   15792_446_1  0.00921302  0            602/1000   out private
		2_3029_1  15792_446_1  1           1            0/1000     out distance
		2_446_1   15792_446_1  0           0            1000/1000  in, owned
		2_506_1   15792_446_1  0.200503    4.70033e-10  1/1000     out limit
		3_431_1   15792_446_1  0.00236402  0            868/1000   in, public
		4_509_1   15792_446_1  0.178176    1.10824e-19  2/1000     in, readable
		 */
		loadNamespaces();
		
		// tests with public, private, readable, and owned workspaces.
		WS_CLI2.createWorkspace(new CreateWorkspaceParams().withWorkspace("foo1"));
		WS_CLI1.createWorkspace(new CreateWorkspaceParams().withWorkspace("foo2"));
		WS_CLI2.createWorkspace(new CreateWorkspaceParams().withWorkspace("foo3")
				.withGlobalread("r"));
		WS_CLI2.createWorkspace(new CreateWorkspaceParams().withWorkspace("foo4"));
		WS_CLI2.setPermissions(new SetPermissionsParams().withId(4L)
				.withNewPermission("r").withUsers(Arrays.asList("user1")));
		
		final Instant now = Instant.ofEpochMilli(10000);
		
		MANAGER.storage.saveSequenceMetadata(
				new NamespaceID("kbasefilter"),
				new LoadID("load2"),
				Arrays.asList(
						SequenceMetadata.getBuilder("2_446_1", "2/446/1", now)
								.withNullableScientificName("sci name")
								.withRelatedID("foo", "bar")
								.build(),
						SequenceMetadata.getBuilder("3_431_1", "3/431/1", now).build(),
						SequenceMetadata.getBuilder("2_3029_1", "2/3029/1", now).build(),
						SequenceMetadata.getBuilder("2_506_1", "2/506/1", now).build(),
						SequenceMetadata.getBuilder("4_509_1", "4/509/1", now).build(),
						SequenceMetadata.getBuilder("1_341_2", "1/341/2", now).build()));

		final URI target = UriBuilder.fromUri(HOST).path("/namespace/kbasefilter/search")
				.queryParam("notstrict", "foo")
				.queryParam("max", 3)
				.build();
		
		final WebTarget wt = CLI.target(target);
		
		// with token
		Builder req = wt.request().header("Authorization", TOKEN1);
		List<Map<String, Object>> distances = Arrays.asList(
				MapBuilder.<String, Object>newHashMap()
				.with("sourceid", "2/446/1")
				.with("namespaceid", "kbasefilter")
				.with("sciname", "sci name")
				.with("dist", 0.0)
				.with("relatedids", ImmutableMap.of("foo", "bar"))
				.build(),
				MapBuilder.<String, Object>newHashMap()
				.with("sourceid", "3/431/1")
				.with("namespaceid", "kbasefilter")
				.with("sciname", null)
				.with("dist", 0.00236402)
				.with("relatedids", Collections.emptyMap())
				.build(),
				MapBuilder.<String, Object>newHashMap()
				.with("sourceid", "4/509/1")
				.with("namespaceid", "kbasefilter")
				.with("sciname", null)
				.with("dist", 0.178176)
				.with("relatedids", Collections.emptyMap())
				.build()
				);

		checkSearchNamespaceWithFilterResult(req, distances);
		
		// without token
		req = wt.request();
		distances = Arrays.asList(
				MapBuilder.<String, Object>newHashMap()
				.with("sourceid", "3/431/1")
				.with("namespaceid", "kbasefilter")
				.with("sciname", null)
				.with("dist", 0.00236402)
				.with("relatedids", Collections.emptyMap())
				.build()
				);

		checkSearchNamespaceWithFilterResult(req, distances);
	}

	private void checkSearchNamespaceWithFilterResult(
			final Builder req,
			final List<Map<String, Object>> expecteDistances)
			throws IOException {
		final Response res = req.post(Entity.entity(
				Files.newInputStream(TEMP_DIR.resolve(QUERY_K31_S1500)),
				MediaType.APPLICATION_OCTET_STREAM));
		
		assertThat("incorrect response code", res.getStatus(), is(200));

		@SuppressWarnings("unchecked")
		final Map<String, Object> response = res.readEntity(Map.class);
		
		final Map<String, Object> expected = ImmutableMap.of(
				"impl", "mash",
				"implver", "2.0",
				"namespaces", Arrays.asList(EXP_NSFILTER),
				"warnings", Arrays.asList("Namespace kbasefilter: Query sketch size 1500 is " +
						"larger than target sketch size 1000"),
				"distances", expecteDistances
				);
		
		assertThat("incorrect response", response, is(expected));
	}
	
	@Test
	public void searchNamespaceFailSketchSize() throws Exception {
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
	
	@Test
	public void searchNamespaceFailBadToken() throws Exception {
		loadNamespaces();
		
		final URI target = UriBuilder.fromUri(HOST).path("/namespace/kbasefilter/search")
				.queryParam("notstrict", "")
				.build();
		
		final WebTarget wt = CLI.target(target);
		final Builder req = wt.request().header("Authorization", "badnaughtytokenneedsaspanking");

		final Response res = req.post(Entity.entity(
				Files.newInputStream(TEMP_DIR.resolve(QUERY_K31_S1500)),
				MediaType.APPLICATION_OCTET_STREAM));
		
		failRequestJSON(res, 401, "Unauthorized", new AuthenticationException(
				ErrorType.AUTHENTICATION_FAILED, "Invalid token"));
	}
	
	/* ******************************************
	 * KBase authenticated filter tests
	 * 
	 * Testing this here vs. a unit test to avoid starting up the workspace twice and avoid using
	 * a mock server. Since the workspace is already started here might as well take advantage
	 * of it.
	 * 
	 * This tests the filter factory. The filter itself has its own unit tests since it does
	 * not contact the workspace once built.
	 * ******************************************/
	
	@Test
	public void kbaseFilterBuildFilterWithWorkspaces() throws Exception {
		// tests with public, private, and owned workspaces.
		WS_CLI1.createWorkspace(new CreateWorkspaceParams().withWorkspace("foo1"));
		WS_CLI2.createWorkspace(new CreateWorkspaceParams().withWorkspace("foo2"));
		WS_CLI1.createWorkspace(new CreateWorkspaceParams().withWorkspace("foo3"));
		WS_CLI2.createWorkspace(new CreateWorkspaceParams().withWorkspace("foo4")
				.withGlobalread("r"));
		
		final MinHashDistanceFilterFactory fac = new KBaseAuthenticatedFilterFactory(
				ImmutableMap.of("workspace-url", WS_URL.toString()));
		
		final MinHashDistanceCollector col = new DefaultDistanceCollector(10);
		KBaseAuthenticatedFilter kbf = (KBaseAuthenticatedFilter) fac.getFilter(
				col, new Token(TOKEN1));
		assertThat("incorrect filter", kbf.getWorkspaceIDs(), is(set(1L, 3L, 4L)));
		
		kbf.accept(new MinHashDistance(new MinHashSketchDBName("d"), "1_1_1", 0.1));
		assertThat("incorrect collector", col.getDistances(),
				is(set(new MinHashDistance(new MinHashSketchDBName("d"), "1_1_1", 0.1))));
		
		kbf = (KBaseAuthenticatedFilter) fac.getFilter(col, new Token(TOKEN2));
		assertThat("incorrect filter", kbf.getWorkspaceIDs(), is(set(2L, 4L)));
		
		kbf = (KBaseAuthenticatedFilter) fac.getFilter(col, null);
		assertThat("incorrect filter", kbf.getWorkspaceIDs(), is(set(4L)));
	}
	
	@Test
	public void kbaseFilterFactoryFilterIDAndAuthsource() throws Exception {
		// tests creating with the various environments.

		// default
		MinHashDistanceFilterFactory fac = new KBaseAuthenticatedFilterFactory(
				ImmutableMap.of("workspace-url", WS_URL.toString()));
		assertThat("incorrect filter ID", fac.getID(), is(new FilterID("kbaseprod")));
		assertThat("incorrect auth source", fac.getAuthSource(), is(Optional.of("kbaseprod")));
		
		for (final String env: Arrays.asList("prod", "appdev", "next", "ci")) {
			fac = new KBaseAuthenticatedFilterFactory(ImmutableMap.of(
					"workspace-url", WS_URL.toString(),
					"env", env));
			assertThat("incorrect filter ID", fac.getID(), is(new FilterID("kbase" + env)));
			assertThat("incorrect auth source", fac.getAuthSource(),
					is(Optional.of("kbase" + env)));
		}
	}
	
	@Test
	public void kbaseFilterConstructFailNullInput() {
		kbaseFilterFailConstruct(null, new NullPointerException("config"));
	}
	
	@Test
	public void kbaseFilterConstructFailBadEnv() {
		kbaseFilterFailConstruct(ImmutableMap.of("workspace-url", WS_URL.toString(), "env", "foo"),
				new MinHashFilterFactoryInitializationException(
						"Illegal KBase filter environment value: foo"));
	}
	
	@Test
	public void kbaseFilterConstructFailBadUrl() {
		final Map<String, String> config = new HashMap<>();
		config.put("workspace-url", null);
		kbaseFilterFailConstruct(config, new MinHashFilterFactoryInitializationException(
				"KBase filter requires key 'workspace-url' in config"));
		kbaseFilterFailConstruct(ImmutableMap.of("workspace-url", "    \t    \n  "),
				new MinHashFilterFactoryInitializationException(
						"KBase filter requires key 'workspace-url' in config"));
		
		kbaseFilterFailConstruct(ImmutableMap.of("workspace-url", "htps://thisisabadurl.com"),
				new MinHashFilterFactoryInitializationException(
						"KBase filter url malformed: htps://thisisabadurl.com"));
		
		kbaseFilterFailConstruct(ImmutableMap.of(
				"workspace-url", "http://ihopethisisanonexistenturlorthistestwillfailforsure.com"),
				new MinHashFilterFactoryInitializationException(
						"KBase filter failed contacting workspace at url " +
						"http://ihopethisisanonexistenturlorthistestwillfailforsure.com: " +
						"ihopethisisanonexistenturlorthistestwillfailforsure.com"));
	}

	private void kbaseFilterFailConstruct(
			final Map<String, String> config,
			final Exception expected) {
		try {
			new KBaseAuthenticatedFilterFactory(config);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void kbaseFilterBuildFailUnauthorized() throws Exception {
		final MinHashDistanceFilterFactory fac = new KBaseAuthenticatedFilterFactory(
				ImmutableMap.of("workspace-url", WS_URL.toString()));
		
		try {
			fac.getFilter(new DefaultDistanceCollector(10), new Token("bustedasstoken"));
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got,
					new MinHashDistanceFilterAuthenticationException("Invalid token"));
		}
	}
	
	@Test
	public void kbaseFilterValidateID() throws Exception {
		final MinHashDistanceFilterFactory fac = new KBaseAuthenticatedFilterFactory(
				ImmutableMap.of("workspace-url", WS_URL.toString()));
		
		assertThat("bad validation", fac.validateID(""), is(false));
		assertThat("bad validation", fac.validateID("foo"), is(false));
		assertThat("bad validation", fac.validateID("1/2/3"), is(false));
		assertThat("bad validation", fac.validateID("1_2"), is(false));
		assertThat("bad validation", fac.validateID("1_2_X"), is(false));
		assertThat("bad validation", fac.validateID("1_X_2"), is(false));
		assertThat("bad validation", fac.validateID("X_1_2"), is(false));
		assertThat("bad validation", fac.validateID("1_2_0"), is(false));
		assertThat("bad validation", fac.validateID("1_0_2"), is(false));
		assertThat("bad validation", fac.validateID("0_1_2"), is(false));
		assertThat("bad validation", fac.validateID("0_ 1_2"), is(false));
		assertThat("bad validation", fac.validateID("0_1_ 2"), is(false));
		assertThat("bad validation", fac.validateID("1_1_1"), is(true));
		
		try {
			fac.validateID(null);
			fail("expected exception");
		} catch (NullPointerException e) {
			assertThat("incorrect exception message", e.getMessage(), is("id"));
		}
	}
}
