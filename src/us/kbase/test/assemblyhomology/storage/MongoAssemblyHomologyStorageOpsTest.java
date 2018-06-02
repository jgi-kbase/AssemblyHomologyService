package us.kbase.test.assemblyhomology.storage;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static us.kbase.test.assemblyhomology.TestCommon.set;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchSequenceException;
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
	public void createNamespaceFail() {
		try {
			manager.storage.createOrReplaceNamespace(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("namespace"));
		}
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
	
	@Test
	public void sequenceMetaSaveAndGet() throws Exception {
		final SequenceMetadata sm1 = SequenceMetadata.getBuilder(
				"id1", "sid1", Instant.ofEpochMilli(10000))
				.build();
		
		final SequenceMetadata sm2 = SequenceMetadata.getBuilder(
				"id2", "sid2", Instant.ofEpochMilli(20000))
				.withNullableScientificName("sciname")
				.withRelatedID("foo", "bar")
				.withRelatedID("baz", "bat")
				.build();
		
		manager.storage.saveSequenceMetadata(new NamespaceID("ns"), new LoadID("l"),
				Arrays.asList(sm1, sm2));
		
		List<SequenceMetadata> got = manager.storage.getSequenceMetadata(
				new NamespaceID("ns"), new LoadID("l"), Arrays.asList("id2", "id1"));
		
		assertThat("incorrect seqs", got, is(Arrays.asList(
				SequenceMetadata.getBuilder("id2", "sid2", Instant.ofEpochMilli(20000))
						.withNullableScientificName("sciname")
						.withRelatedID("foo", "bar")
						.withRelatedID("baz", "bat")
						.build(),
				SequenceMetadata.getBuilder("id1", "sid1", Instant.ofEpochMilli(10000)).build())));
		
		got = manager.storage.getSequenceMetadata(
				new NamespaceID("ns"), new LoadID("l"), Arrays.asList("id2"));
		
		assertThat("incorrect seqs", got, is(Arrays.asList(
				SequenceMetadata.getBuilder("id2", "sid2", Instant.ofEpochMilli(20000))
						.withNullableScientificName("sciname")
						.withRelatedID("foo", "bar")
						.withRelatedID("baz", "bat")
						.build())));
		
		got = manager.storage.getSequenceMetadata(
				new NamespaceID("ns"), new LoadID("l"), Arrays.asList("id1"));
		
		assertThat("incorrect seqs", got, is(Arrays.asList(
				SequenceMetadata.getBuilder("id1", "sid1", Instant.ofEpochMilli(10000)).build())));
	}
	
	@Test
	public void sequenceMetaSaveAndGetWithoutLoadID() throws Exception {
		final SequenceMetadata sm1 = SequenceMetadata.getBuilder(
				"id1", "sid1", Instant.ofEpochMilli(10000))
				.build();
		
		manager.storage.createOrReplaceNamespace(Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withScaling(2500).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						16),
				new LoadID("bar"),
				Instant.ofEpochMilli(10000))
				.build());
		
		manager.storage.saveSequenceMetadata(new NamespaceID("foo"), new LoadID("bar"),
				Arrays.asList(sm1));
		
		final List<SequenceMetadata> got = manager.storage.getSequenceMetadata(
				new NamespaceID("foo"), Arrays.asList("id1"));
		
		assertThat("incorrect seqs", got, is(Arrays.asList(
				SequenceMetadata.getBuilder("id1", "sid1", Instant.ofEpochMilli(10000)).build())));
	}
	
	@Test
	public void sequenceMetaReplace() throws Exception {
		final SequenceMetadata sm1 = SequenceMetadata.getBuilder(
				"id1", "sid1", Instant.ofEpochMilli(10000))
				.build();
		
		manager.storage.saveSequenceMetadata(new NamespaceID("foo"), new LoadID("bar"),
				Arrays.asList(sm1));
		
		final SequenceMetadata sm2 = SequenceMetadata.getBuilder(
				"id1", "sid2", Instant.ofEpochMilli(20000))
				.withNullableScientificName("sciname")
				.withRelatedID("foo", "bar")
				.withRelatedID("baz", "bat")
				.build();
		
		manager.storage.saveSequenceMetadata(new NamespaceID("foo"), new LoadID("bar"),
				Arrays.asList(sm2));
		
		final List<SequenceMetadata> got = manager.storage.getSequenceMetadata(
				new NamespaceID("foo"), new LoadID("bar"), Arrays.asList("id1"));
		
		assertThat("incorrect seqs", got, is(Arrays.asList(
				SequenceMetadata.getBuilder("id1", "sid2", Instant.ofEpochMilli(20000))
						.withNullableScientificName("sciname")
						.withRelatedID("foo", "bar")
						.withRelatedID("baz", "bat")
						.build())));
	}
	
	@Test
	public void sequenceMetadataEmpty() throws Exception {
		// just test this doesn't fail
		manager.storage.saveSequenceMetadata(new NamespaceID("foo"), new LoadID("bar"),
				Collections.emptyList());
	}
	
	@Test
	public void saveSequenceMetadataFail() throws Exception {
		final MongoAssemblyHomologyStorage storage = manager.storage;
		final NamespaceID n = new NamespaceID("baz");
		final LoadID l = new LoadID("bar");
		final SequenceMetadata sm1 = SequenceMetadata.getBuilder(
				"id1", "sid1", Instant.ofEpochMilli(10000))
				.build();
		final List<SequenceMetadata> s = Arrays.asList(sm1);
		
		failSaveSequenceMetadata(storage, null, l, s, new NullPointerException("namespaceID"));
		failSaveSequenceMetadata(storage, n, null, s, new NullPointerException("loadID"));
		failSaveSequenceMetadata(storage, n, l, null, new NullPointerException("seqmeta"));
		failSaveSequenceMetadata(storage, n, l, Arrays.asList(sm1, null),
				new NullPointerException("Null item in collection seqmeta"));
	}
	
	private void failSaveSequenceMetadata(
			final MongoAssemblyHomologyStorage storage,
			final NamespaceID nsID,
			final LoadID loadID,
			final Collection<SequenceMetadata> seqs,
			final Exception expected) {
		try {
			storage.saveSequenceMetadata(nsID, loadID, seqs);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getSequenceMetadataFailBadInput() throws Exception {
		final MongoAssemblyHomologyStorage storage = manager.storage;
		final NamespaceID n = new NamespaceID("foo");
		final LoadID l = new LoadID("bar");
		final List<String> i = Arrays.asList("id1");
		
		storage.createOrReplaceNamespace(Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withScaling(2500).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						16),
				new LoadID("bar"),
				Instant.ofEpochMilli(10000))
				.build());
		
		failGetSequenceMetadata(storage, null, i, new NullPointerException("namespaceID"));
		failGetSequenceMetadata(storage, null, l, i, new NullPointerException("namespaceID"));
		failGetSequenceMetadata(storage, n, null, i, new NullPointerException("loadID"));
		failGetSequenceMetadata(storage, n, null, new NullPointerException("sequenceIDs"));
		failGetSequenceMetadata(storage, n, l, null, new NullPointerException("sequenceIDs"));
		failGetSequenceMetadata(storage, n, Arrays.asList("id", null),
				new IllegalArgumentException(
						"Null or whitespace only string in collection sequenceIDs"));
		failGetSequenceMetadata(storage, n, l, Arrays.asList("id", null),
				new IllegalArgumentException(
						"Null or whitespace only string in collection sequenceIDs"));
		failGetSequenceMetadata(storage, n, Arrays.asList("id", "  \t   \n "),
				new IllegalArgumentException(
						"Null or whitespace only string in collection sequenceIDs"));
		failGetSequenceMetadata(storage, n, l, Arrays.asList("id", "  \t   \n "),
				new IllegalArgumentException(
						"Null or whitespace only string in collection sequenceIDs"));
	}
	
	@Test
	public void getSequenceMetadataFailNoSuchNamespace() throws Exception {
		final MongoAssemblyHomologyStorage storage = manager.storage;
		final NamespaceID n = new NamespaceID("foo1");
		final LoadID l = new LoadID("bar");
		final SequenceMetadata sm1 = SequenceMetadata.getBuilder(
				"id1", "sid1", Instant.ofEpochMilli(10000))
				.build();
		final List<String> i = Arrays.asList("id1");
		
		storage.saveSequenceMetadata(n, l, Arrays.asList(sm1));
		
		storage.createOrReplaceNamespace(Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withScaling(2500).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						16),
				new LoadID("bar"),
				Instant.ofEpochMilli(10000))
				.build());
		
		failGetSequenceMetadata(storage, n, i, new NoSuchNamespaceException("foo1"));
	}
	
	@Test
	public void getSequenceMetadataFailNoSuchSequence() throws Exception {
		final MongoAssemblyHomologyStorage storage = manager.storage;
		final NamespaceID n = new NamespaceID("foo");
		final LoadID l = new LoadID("bar");
		
		storage.saveSequenceMetadata(n, l, Arrays.asList(SequenceMetadata.getBuilder(
				"id1", "sid1", Instant.ofEpochMilli(10000))
				.build()));
		
		storage.saveSequenceMetadata(new NamespaceID("other"), l,
				Arrays.asList(SequenceMetadata.getBuilder(
						"id2", "sid2", Instant.ofEpochMilli(10000))
						.build()));
		
		storage.saveSequenceMetadata(new NamespaceID("foo"), new LoadID("other"),
				Arrays.asList(SequenceMetadata.getBuilder(
						"id3", "sid3", Instant.ofEpochMilli(10000))
						.build()));
		
		storage.createOrReplaceNamespace(Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withScaling(2500).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						16),
				new LoadID("bar"),
				Instant.ofEpochMilli(10000))
				.build());
		
		failGetSequenceMetadata(storage, n, Arrays.asList("id1", "id2"),
				new NoSuchSequenceException(
						"Missing sequence(s) in namespace foo with load id bar: id2"));
		failGetSequenceMetadata(storage, n, l, Arrays.asList("id1", "id2"),
				new NoSuchSequenceException(
						"Missing sequence(s) in namespace foo with load id bar: id2"));
		failGetSequenceMetadata(storage, n, Arrays.asList("id2", "id4", "id5", "id1", "id3"),
				new NoSuchSequenceException(
						"Missing sequence(s) in namespace foo with load id bar: id2 id3 id4"));
		failGetSequenceMetadata(storage, n, l, Arrays.asList("id2", "id4", "id5", "id1", "id3"),
				new NoSuchSequenceException(
						"Missing sequence(s) in namespace foo with load id bar: id2 id3 id4"));
	}
	
	private void failGetSequenceMetadata(
			final MongoAssemblyHomologyStorage storage,
			final NamespaceID nsID,
			final List<String> ids,
			final Exception expected) {
		try {
			storage.getSequenceMetadata(nsID, ids);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	private void failGetSequenceMetadata(
			final MongoAssemblyHomologyStorage storage,
			final NamespaceID nsID,
			final LoadID loadID,
			final List<String> ids,
			final Exception expected) {
		try {
			storage.getSequenceMetadata(nsID, loadID, ids);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void deleteNamespaceFail() {
		try {
			manager.storage.deleteNamespace(new NamespaceID("foo"));
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new UnsupportedOperationException());
		}
	}
	
	@Test
	public void removeInactiveData() throws Exception {
		final MongoAssemblyHomologyStorage s = manager.storage;
		
		final NamespaceID ns1 = new NamespaceID("id1");
		final LoadID ld1 = new LoadID("load1");
		s.createOrReplaceNamespace(Namespace.getBuilder(
				ns1,
				new MinHashSketchDatabase(
						new MinHashSketchDBName("id1"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withScaling(2500).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						16),
				ld1,
				Instant.ofEpochMilli(10000))
				.build());
		
		final NamespaceID ns2 = new NamespaceID("id2");
		final LoadID ld2 = new LoadID("load2");
		s.createOrReplaceNamespace(Namespace.getBuilder(
				ns2,
				new MinHashSketchDatabase(
						new MinHashSketchDBName("id2"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withScaling(2500).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH),
						16),
				ld2,
				Instant.ofEpochMilli(10000))
				.build());
		
		final SequenceMetadata noNS_100 = SequenceMetadata.getBuilder(
				"noNS_100", "sid1", Instant.ofEpochMilli(100000)).build();
		final SequenceMetadata noNS_200 = SequenceMetadata.getBuilder(
				"noNS_200", "sid2", Instant.ofEpochMilli(200000)).build();
		final SequenceMetadata noNS_300 = SequenceMetadata.getBuilder(
				"noNS_300", "sid3", Instant.ofEpochMilli(300000)).build();
		
		final NamespaceID nons = new NamespaceID("noNS");
		// use the same load id as extant namespace in case it's just checking load ids
		s.saveSequenceMetadata(nons, ld1, set(noNS_100, noNS_200, noNS_300));
		
		final SequenceMetadata id1_safe = SequenceMetadata.getBuilder(
				"id1", "sid1", Instant.ofEpochMilli(10000)).build();
		
		s.saveSequenceMetadata(ns1, ld1, set(id1_safe));
		
		final SequenceMetadata id2_safe = SequenceMetadata.getBuilder(
				"id2", "sid2", Instant.ofEpochMilli(20000)).build();
		
		s.saveSequenceMetadata(ns2, ld2, set(id2_safe));
		
		final SequenceMetadata id1_100 = SequenceMetadata.getBuilder(
				"id1_100", "sid1", Instant.ofEpochMilli(100000)).build();
		final SequenceMetadata id1_200 = SequenceMetadata.getBuilder(
				"id1_200", "sid2", Instant.ofEpochMilli(200000)).build();
		final SequenceMetadata id1_300 = SequenceMetadata.getBuilder(
				"id1_300", "sid3", Instant.ofEpochMilli(300000)).build();
		
		final LoadID nold1 = new LoadID("noload1"); // extant ns, old load id
		s.saveSequenceMetadata(ns1, nold1, set(id1_100, id1_200, id1_300));
		
		final SequenceMetadata id2_200 = SequenceMetadata.getBuilder(
				"id2_200", "sid1", Instant.ofEpochMilli(200000)).build();
		final SequenceMetadata id2_300 = SequenceMetadata.getBuilder(
				"id2_300", "sid2", Instant.ofEpochMilli(300000)).build();
		final SequenceMetadata id2_400 = SequenceMetadata.getBuilder(
				"id2_400", "sid3", Instant.ofEpochMilli(400000)).build();
		
		final LoadID nold2 = new LoadID("noload2"); // extant ns, old load id
		s.saveSequenceMetadata(ns2, nold2, set(id2_200, id2_300, id2_400));
		
		assertThat("incorrect seqmeta", s.getSequenceMetadata(), is(set(
				id1_safe, id2_safe,
				noNS_100, noNS_200, noNS_300,
				id1_100, id1_200, id1_300,
				id2_200, id2_300, id2_400)));
		
		when(manager.clockMock.instant()).thenReturn(Instant.ofEpochMilli(500000));
		
		s.removeInactiveData(Duration.of(450000, ChronoUnit.MILLIS));
		
		assertThat("incorrect seqmeta", s.getSequenceMetadata(), is(set(
				id1_safe, id2_safe,
				noNS_100, noNS_200, noNS_300,
				id1_100, id1_200, id1_300,
				id2_200, id2_300, id2_400)));
		
		s.removeInactiveData(Duration.of(350000, ChronoUnit.MILLIS));
		
		assertThat("incorrect seqmeta", s.getSequenceMetadata(), is(set(
				id1_safe, id2_safe,
				noNS_200, noNS_300,
				id1_200, id1_300,
				id2_200, id2_300, id2_400)));
		
		s.removeInactiveData(Duration.of(250000, ChronoUnit.MILLIS));
		
		assertThat("incorrect seqmeta", s.getSequenceMetadata(), is(set(
				id1_safe, id2_safe,
				noNS_300,
				id1_300,
				id2_300, id2_400)));
		
		s.removeInactiveData(Duration.of(150000, ChronoUnit.MILLIS));
		
		assertThat("incorrect seqmeta", s.getSequenceMetadata(), is(set(
				id1_safe, id2_safe,
				id2_400)));
		
		s.removeInactiveData(Duration.of(50000, ChronoUnit.MILLIS));
		
		assertThat("incorrect seqmeta", s.getSequenceMetadata(), is(set(id1_safe, id2_safe)));
	}
	
	@Test
	public void removeInactiveDataFail() {
		try {
			manager.storage.removeInactiveData(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("olderThan"));
		}
	}
}
