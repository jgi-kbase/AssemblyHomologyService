package us.kbase.test.assemblyhomology.storage;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.assemblyhomology.TestCommon.set;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.assemblyhomology.storage.exceptions.StorageInitException;
import us.kbase.assemblyhomology.storage.mongo.MongoAssemblyHomologyStorage;
import us.kbase.test.assemblyhomology.MongoStorageTestManager;
import us.kbase.test.assemblyhomology.TestCommon;

public class MongoAssemblyHomologyStorageStartupTest {

	private static MongoStorageTestManager manager;

	@BeforeClass
	public static void setUp() throws Exception {
		manager = new MongoStorageTestManager("test_mongoahstorage");
	}
	
	@AfterClass
	public static void tearDownClass() throws Exception {
		if (manager != null) {
			manager.destroy();
		}
	}
	
	@Before
	public void clearDB() throws Exception {
		manager.reset();
	}
	
	@Test
	public void nullConstructor() throws Exception {
		try {
			new MongoAssemblyHomologyStorage(null);
			fail("expected exception");
		} catch (NullPointerException e) {
			assertThat("incorrect exception message", e.getMessage(), is("db"));
		}
	}
	
	@Test
	public void startUpAndCheckConfigDoc() throws Exception {
		final MongoDatabase db = manager.mc.getDatabase("startUpAndCheckConfigDoc");
		new MongoAssemblyHomologyStorage(db);
		final MongoCollection<Document> col = db.getCollection("config");
		assertThat("Only one config doc", col.count(), is(1L));
		final FindIterable<Document> c = col.find();
		final Document d = c.first();
		
		assertThat("correct config key & value", (String)d.get("schema"), is("schema"));
		assertThat("not in update", (Boolean)d.get("inupdate"), is(false));
		assertThat("schema v1", (Integer)d.get("schemaver"), is(1));
		
		//check startup works with the config object in place
		final MongoAssemblyHomologyStorage ms = new MongoAssemblyHomologyStorage(db);
		ms.saveSequenceMetadata(new NamespaceID("foo"), new LoadID("bar"), Arrays.asList(
				SequenceMetadata.getBuilder("s", "source", Instant.ofEpochMilli(10000)).build()));
		assertThat("failed basic storage operation", ms.getSequenceMetadata(
				new NamespaceID("foo"), new LoadID("bar"), Arrays.asList("s")),
				is(Arrays.asList(SequenceMetadata.getBuilder(
						"s", "source", Instant.ofEpochMilli(10000)).build())));
	}
	
	@Test
	public void startUpWith2ConfigDocs() throws Exception {
		final MongoDatabase db = manager.mc.getDatabase("startUpWith2ConfigDocs");
		
		final Document m = new Document("schema", "schema")
				.append("inupdate", false)
				.append("schemaver", 1);
		
		db.getCollection("config").insertMany(Arrays.asList(m,
				// need to create a new document to create a new mongo _id
				new Document(m)));
		
		final Pattern errorPattern = Pattern.compile(
				"Failed to create index: Write failed with error code 11000 and error message " +
				"'(exception: )?E11000 duplicate key error (index|collection): " +
				"startUpWith2ConfigDocs.config( index: |\\.\\$)schema_1\\s+dup key: " +
				"\\{ : \"schema\" \\}'");
		try {
			new MongoAssemblyHomologyStorage(db);
			fail("started mongo with bad config");
		} catch (StorageInitException e) {
			final Matcher match = errorPattern.matcher(e.getMessage());
			assertThat("exception did not match: \n" + e.getMessage(), match.matches(), is(true));
		}
	}
	
	@Test
	public void startUpWithBadSchemaVersion() throws Exception {
		final MongoDatabase db = manager.mc.getDatabase("startUpWithBadSchemaVersion");
		
		final Document m = new Document("schema", "schema")
				.append("inupdate", false)
				.append("schemaver", 4);
		
		db.getCollection("config").insertOne(m);
		
		failMongoStart(db, new StorageInitException(
				"Incompatible database schema. Server is v1, DB is v4"));
	}
	
	@Test
	public void startUpWithUpdateInProgress() throws Exception {
		final MongoDatabase db = manager.mc.getDatabase("startUpWithUpdateInProgress");
		
		final Document m = new Document("schema", "schema")
				.append("inupdate", true)
				.append("schemaver", 1);
		
		db.getCollection("config").insertOne(m);
		
		failMongoStart(db, new StorageInitException(
				"The database is in the middle of an update from v1 of the " +
				"schema. Aborting startup."));
	}
	
	private void failMongoStart(final MongoDatabase db, final Exception exp)
			throws Exception {
		try {
			new MongoAssemblyHomologyStorage(db);
			fail("started mongo with bad config");
		} catch (Exception e) {
			TestCommon.assertExceptionCorrect(e, exp);
		}
	}
	
	/* The following tests ensure that all indexes are created correctly. The collection names
	 * are tested so that if a new collection is added the test will fail without altering 
	 * said test, at which time the coder will hopefully read this notice and add index tests
	 * for the new collection.
	 */
	
	@Test
	public void checkCollectionNames() throws Exception {
		final Set<String> names = new HashSet<>();
		final Set<String> expected = set(
				"config",
				"namesp",
				"seqmeta");
		if (manager.includeSystemIndexes) {
			expected.add("system.indexes");
		}
		// this is annoying. MongoIterator has two forEach methods with different signatures
		// and so which one to call is ambiguous for lambda expressions.
		manager.db.listCollectionNames().forEach((Consumer<String>) names::add);
		assertThat("incorrect collection names", names, is(expected));
	}
	
	@Test
	public void indexesConfig() {
		final Set<Document> indexes = new HashSet<>();
		manager.db.getCollection("config").listIndexes()
				.forEach((Consumer<Document>) indexes::add);
		assertThat("incorrect indexes", indexes, is(set(
				new Document("v", manager.indexVer)
						.append("unique", true)
						.append("key", new Document("schema", 1))
						.append("name", "schema_1")
						.append("ns", "test_mongoahstorage.config"),
				new Document("v", manager.indexVer)
						.append("key", new Document("_id", 1))
						.append("name", "_id_")
						.append("ns", "test_mongoahstorage.config")
				)));
	}
	
	@Test
	public void indexesNamespace() {
		final Set<Document> indexes = new HashSet<>();
		manager.db.getCollection("namesp").listIndexes()
				.forEach((Consumer<Document>) indexes::add);
		assertThat("incorrect indexes", indexes, is(set(
				new Document("v", manager.indexVer)
						.append("unique", true)
						.append("key", new Document("id", 1))
						.append("name", "id_1")
						.append("ns", "test_mongoahstorage.namesp"),
				new Document("v", manager.indexVer)
						.append("key", new Document("_id", 1))
						.append("name", "_id_")
						.append("ns", "test_mongoahstorage.namesp")
				)));
	}
	
	@Test
	public void indexesSeqMeta() {
		final Set<Document> indexes = new HashSet<>();
		manager.db.getCollection("seqmeta").listIndexes()
				.forEach((Consumer<Document>) indexes::add);
		assertThat("incorrect indexes", indexes, is(set(
				new Document("v", manager.indexVer)
						.append("unique", true)
						.append("key", new Document("nsid", 1)
								.append("load", 1).append("seqid", 1))
						.append("name", "nsid_1_load_1_seqid_1")
						.append("ns", "test_mongoahstorage.seqmeta"),
				new Document("v", manager.indexVer)
						.append("key", new Document("_id", 1))
						.append("name", "_id_")
						.append("ns", "test_mongoahstorage.seqmeta")
				)));
	}
}
