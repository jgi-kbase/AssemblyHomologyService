package us.kbase.test.assemblyhomology.storage;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.assemblyhomology.TestCommon.set;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.assemblyhomology.core.DataSourceID;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.storage.exceptions.AssemblyHomologyStorageException;
import us.kbase.assemblyhomology.storage.mongo.MongoAssemblyHomologyStorage;
import us.kbase.test.assemblyhomology.MongoStorageTestManager;
import us.kbase.test.assemblyhomology.TestCommon;

public class MongoAssemblyHomologyStorageOpsTest {

	private static MongoStorageTestManager manager;
	private static Path TEMP_DIR;
	private static Path EMPTY_FILE_MSH;
	
	@BeforeClass
	public static void setUp() throws Exception {
		manager = new MongoStorageTestManager("test_mongoahstorage");
		TEMP_DIR = TestCommon.getTempDir().resolve("StorageTest_" + UUID.randomUUID().toString());
		Files.createDirectories(TEMP_DIR);
		EMPTY_FILE_MSH = TEMP_DIR.resolve(UUID.randomUUID().toString() + ".msh");
		Files.createFile(EMPTY_FILE_MSH);
	}
	
	@AfterClass
	public static void tearDownClass() throws Exception {
		if (manager != null) {
			manager.destroy();
		}
		final boolean deleteTempFiles = TestCommon.isDeleteTempFiles();
		if (TEMP_DIR != null && Files.exists(TEMP_DIR) && deleteTempFiles) {
			FileUtils.deleteQuietly(TEMP_DIR.toFile());
		}
	}
	
	@Before
	public void clearDB() throws Exception {
		manager.reset();
	}
	
	@Test
	public void nameSpaceCreateAndGetWithSketchSizeMinimal() throws Exception {
		final Namespace ns = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						16),
				new LoadID("bar"),
				Instant.ofEpochMilli(10000))
				.build();
		
		manager.storage.createOrReplaceNamespace(ns);
		
		final Namespace got = manager.storage.getNamespace(new NamespaceID("foo"));
		
		final Namespace expected = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						16),
				new LoadID("bar"),
				Instant.ofEpochMilli(10000))
				.build();
		
		assertThat("incorrect namespace", got, is(expected));
		
		final Set<Namespace> got2 = manager.storage.getNamespaces();
		assertThat("incorrect namespaces", got2, is(set(expected)));
	}
	
	@Test
	public void nameSpaceCreateAndGetWithScalingMaximal() throws Exception {
		final Namespace ns = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withScaling(2500).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						16),
				new LoadID("bar"),
				Instant.ofEpochMilli(10000))
				.withNullableDataSourceID(new DataSourceID("wugga"))
				.withNullableSourceDatabaseID("some source db")
				.withNullableDescription("some description")
				.build();
		
		manager.storage.createOrReplaceNamespace(ns);
		
		final Namespace got = manager.storage.getNamespace(new NamespaceID("foo"));
		
		final Namespace expected = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withScaling(2500).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						16),
				new LoadID("bar"),
				Instant.ofEpochMilli(10000))
				.withNullableDataSourceID(new DataSourceID("wugga"))
				.withNullableSourceDatabaseID("some source db")
				.withNullableDescription("some description")
				.build();
		
		assertThat("incorrect namespace", got, is(expected));
		
		final Set<Namespace> got2 = manager.storage.getNamespaces();
		assertThat("incorrect namespaces", got2, is(set(expected)));
	}
	
	@Test
	public void nameSpaceReplace() throws Exception {
		final Namespace ns = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withScaling(2500).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						16),
				new LoadID("bar"),
				Instant.ofEpochMilli(10000))
				.withNullableDataSourceID(new DataSourceID("wugga"))
				.withNullableSourceDatabaseID("some source db")
				.withNullableDescription("some description")
				.build();
		
		manager.storage.createOrReplaceNamespace(ns);
		
		final Namespace ns2 = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("sourmash"),
						MinHashParameters.getBuilder(21).withSketchSize(1000).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						32),
				new LoadID("baz"),
				Instant.ofEpochMilli(20000))
				.withNullableDataSourceID(new DataSourceID("fairy godmother"))
				.withNullableSourceDatabaseID("here you are, dear")
				.withNullableDescription("oh thank you mother")
				.build();
		
		manager.storage.createOrReplaceNamespace(ns2);
		
		final Namespace got = manager.storage.getNamespace(new NamespaceID("foo"));
		
		final Namespace expected = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("sourmash"),
						MinHashParameters.getBuilder(21).withSketchSize(1000).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						32),
				new LoadID("baz"),
				Instant.ofEpochMilli(20000))
				.withNullableDataSourceID(new DataSourceID("fairy godmother"))
				.withNullableSourceDatabaseID("here you are, dear")
				.withNullableDescription("oh thank you mother")
				.build();
		
		assertThat("incorrect namespace", got, is(expected));
		
		final Set<Namespace> got2 = manager.storage.getNamespaces();
		assertThat("incorrect namespaces", got2, is(set(expected)));
	}
	
	@Test
	public void nameSpaceGetMultiple() throws Exception {
		final Namespace ns = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withScaling(2500).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						16),
				new LoadID("bar"),
				Instant.ofEpochMilli(10000))
				.withNullableDataSourceID(new DataSourceID("wugga"))
				.withNullableSourceDatabaseID("some source db")
				.withNullableDescription("some description")
				.build();
		
		manager.storage.createOrReplaceNamespace(ns);
		
		final Namespace ns2 = Namespace.getBuilder(
				new NamespaceID("foo2"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo2"),
						new MinHashImplementationName("sourmash"),
						MinHashParameters.getBuilder(21).withSketchSize(1000).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						32),
				new LoadID("baz"),
				Instant.ofEpochMilli(20000))
				.withNullableDataSourceID(new DataSourceID("fairy godmother"))
				.withNullableSourceDatabaseID("here you are, dear")
				.withNullableDescription("oh thank you mother")
				.build();
		
		manager.storage.createOrReplaceNamespace(ns2);
		
		final Namespace expected1 = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withScaling(2500).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						16),
				new LoadID("bar"),
				Instant.ofEpochMilli(10000))
				.withNullableDataSourceID(new DataSourceID("wugga"))
				.withNullableSourceDatabaseID("some source db")
				.withNullableDescription("some description")
				.build();
		
		final Namespace expected2 = Namespace.getBuilder(
				new NamespaceID("foo2"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo2"),
						new MinHashImplementationName("sourmash"),
						MinHashParameters.getBuilder(21).withSketchSize(1000).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						32),
				new LoadID("baz"),
				Instant.ofEpochMilli(20000))
				.withNullableDataSourceID(new DataSourceID("fairy godmother"))
				.withNullableSourceDatabaseID("here you are, dear")
				.withNullableDescription("oh thank you mother")
				.build();
		
		final Set<Namespace> got2 = manager.storage.getNamespaces();
		assertThat("incorrect namespaces", got2, is(set(expected1, expected2)));
	}
	
	@Test
	public void getNamespaceFail() throws Exception {
		final MongoAssemblyHomologyStorage s = manager.storage;
		final Namespace ns = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withScaling(2500).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						16),
				new LoadID("bar"),
				Instant.ofEpochMilli(10000))
				.withNullableDataSourceID(new DataSourceID("wugga"))
				.withNullableSourceDatabaseID("some source db")
				.withNullableDescription("some description")
				.build();
		
		s.createOrReplaceNamespace(ns);
		
		failGetNamespace(s, null, new NullPointerException("namespaceID"));
		failGetNamespace(s, new NamespaceID("foo2"), new NoSuchNamespaceException("foo2"));
	}
	
	@Test
	public void namespaceIllegalData() throws Exception {
		/* there are multiple places where exceptions could be thrown when reinstantiating
		 * a namespace, but we only test one just to be sure the exceptions are caught.
		 * If someone is going into mongo manually and doing surgery the solution is to
		 * disembowel them with a garden weasel.
		 */
		final Namespace ns = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withScaling(2500).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						16),
				new LoadID("bar"),
				Instant.ofEpochMilli(10000))
				.withNullableDataSourceID(new DataSourceID("wugga"))
				.withNullableSourceDatabaseID("some source db")
				.withNullableDescription("some description")
				.build();
		
		manager.storage.createOrReplaceNamespace(ns);
		
		manager.db.getCollection("namesp").updateOne(
				new Document("id", "foo"), new Document("$set", new Document("load", null)));
		failGetNamespace(manager.storage, new NamespaceID("foo"),
				new AssemblyHomologyStorageException(
						"Unexpected value in database: 30000 Missing input parameter: loadID"));
		
		manager.db.getCollection("namesp").updateOne(
				new Document("id", "foo"),
				new Document("$set", new Document("load", TestCommon.LONG1001)));
		failGetNamespace(manager.storage, new NamespaceID("foo"),
				new AssemblyHomologyStorageException(
						"Unexpected value in database: 30001 Illegal input parameter: " +
						"loadID size greater than limit 256"));
	}
	
	private void failGetNamespace(
			final MongoAssemblyHomologyStorage storage,
			final NamespaceID nsid,
			final Exception expected) {
		try {
			storage.getNamespace(nsid);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	
}
