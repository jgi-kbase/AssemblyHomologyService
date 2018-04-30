package us.kbase.assemblyhomology.storage.mongo;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;

import com.google.common.base.Optional;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;

import us.kbase.assemblyhomology.core.DataSourceID;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.storage.AssemblyHomologyStorage;
import us.kbase.assemblyhomology.storage.exceptions.AssemblyHomologyStorageException;
import us.kbase.assemblyhomology.storage.exceptions.StorageInitException;

public class MongoAssemblyHomologyStorage implements AssemblyHomologyStorage {

	//TODO JAVADOC
	//TODO TEST

	/* Don't use mongo built in object mapping to create the returned objects
	 * since that tightly couples the classes to the storage implementation.
	 * Instead, if needed, create classes specific to the implementation for
	 * mapping purposes that produce the returned classes.
	 */
	
	/* Testing the (many) catch blocks for the general mongo exception is pretty hard, since it
	 * appears as though the mongo clients have a heartbeat, so just stopping mongo might trigger
	 * the heartbeat exception rather than the exception you're going for.
	 * 
	 * Mocking the mongo client is probably not the answer:
	 * http://stackoverflow.com/questions/7413985/unit-testing-with-mongodb
	 * https://github.com/mockito/mockito/wiki/How-to-write-good-tests
	 */
	
	private static final int SCHEMA_VERSION = 1;
	
	// collection names
	private static final String COL_CONFIG = "config";
	private static final String COL_NAMESPACES = "namesp";
	
	private static final Map<String, Map<List<String>, IndexOptions>> INDEXES;
	private static final IndexOptions IDX_UNIQ = new IndexOptions().unique(true);
	private static final IndexOptions IDX_SPARSE = new IndexOptions().sparse(true);
	private static final IndexOptions IDX_UNIQ_SPARSE =
			new IndexOptions().unique(true).sparse(true);
	static {
		//hardcoded indexes
		INDEXES = new HashMap<String, Map<List<String>, IndexOptions>>();
		
		// namespace config indexes
		final Map<List<String>, IndexOptions> namespace = new HashMap<>();
		namespace.put(Arrays.asList(Fields.NAMESPACE_ID), IDX_UNIQ);
		INDEXES.put(COL_NAMESPACES, namespace);
		
		//config indexes
		final Map<List<String>, IndexOptions> cfg = new HashMap<>();
		//ensure only one config object
		cfg.put(Arrays.asList(Fields.DB_SCHEMA_KEY), IDX_UNIQ);
		INDEXES.put(COL_CONFIG, cfg);
	}
	
	private final MongoDatabase db;
	
	public MongoAssemblyHomologyStorage(final MongoDatabase db) throws StorageInitException {
		checkNotNull(db, "db");
		this.db = db;
		checkConfig();
		ensureIndexes();
	}
	
	private void checkConfig() throws StorageInitException  {
		final MongoCollection<Document> col = db.getCollection(COL_CONFIG);
		final Document cfg = new Document(Fields.DB_SCHEMA_KEY, Fields.DB_SCHEMA_VALUE);
		cfg.put(Fields.DB_SCHEMA_UPDATE, false);
		cfg.put(Fields.DB_SCHEMA_VERSION, SCHEMA_VERSION);
		try {
			col.insertOne(cfg);
		} catch (MongoWriteException dk) {
			if (!DuplicateKeyExceptionChecker.isDuplicate(dk)) {
				throw new StorageInitException("There was a problem communicating with the " +
						"database: " + dk.getMessage(), dk);
			}
			// ok, duplicate key means the version doc is already there, this isn't the first
			// startup
			if (col.count() != 1) {
				// if this occurs the indexes are broken, so there's no way to test without
				// altering ensureIndexes()
				throw new StorageInitException(
						"Multiple config objects found in the database. " +
						"This should not happen, something is very wrong.");
			}
			final FindIterable<Document> cur = db.getCollection(COL_CONFIG)
					.find(Filters.eq(Fields.DB_SCHEMA_KEY, Fields.DB_SCHEMA_VALUE));
			final Document doc = cur.first();
			if ((Integer) doc.get(Fields.DB_SCHEMA_VERSION) != SCHEMA_VERSION) {
				throw new StorageInitException(String.format(
						"Incompatible database schema. Server is v%s, DB is v%s",
						SCHEMA_VERSION, doc.get(Fields.DB_SCHEMA_VERSION)));
			}
			if ((Boolean) doc.get(Fields.DB_SCHEMA_UPDATE)) {
				throw new StorageInitException(String.format(
						"The database is in the middle of an update from " +
								"v%s of the schema. Aborting startup.", 
								doc.get(Fields.DB_SCHEMA_VERSION)));
			}
		} catch (MongoException me) {
			throw new StorageInitException(
					"There was a problem communicating with the database: " + me.getMessage(), me);
		}
	}

	private void ensureIndexes() throws StorageInitException {
		for (final String col: INDEXES.keySet()) {
			for (final List<String> idx: INDEXES.get(col).keySet()) {
				final Document index = new Document();
				final IndexOptions opts = INDEXES.get(col).get(idx);
				for (final String field: idx) {
					index.put(field, 1);
				}
				final MongoCollection<Document> dbcol = db.getCollection(col);
				try {
					if (opts == null) {
						dbcol.createIndex(index);
					} else {
						dbcol.createIndex(index, opts);
					}
				} catch (MongoException me) {
					throw new StorageInitException(
							"Failed to create index: " + me.getMessage(), me);
				}
			}
		}
	}

	private static class DuplicateKeyExceptionChecker {
		// super hacky and fragile, but doesn't seem another way to do this.
		
		private final Pattern keyPattern = Pattern.compile("dup key:\\s+\\{ : \"(.*)\" \\}");
		private final Pattern indexPattern = Pattern.compile(
				"duplicate key error (index|collection): " +
				"\\w+\\.(\\w+)( index: |\\.\\$)([\\.\\w]+)\\s+");
		
		private final boolean isDuplicate;
		private final Optional<String> collection;
		private final Optional<String> index;
		private final Optional<String> key;
		
		public DuplicateKeyExceptionChecker(final MongoWriteException mwe)
				throws AssemblyHomologyStorageException {
			// split up indexes better at some point - e.g. in a Document
			isDuplicate = isDuplicate(mwe);
			if (isDuplicate) {
				final Matcher indexMatcher = indexPattern.matcher(mwe.getMessage());
				if (indexMatcher.find()) {
					collection = Optional.of(indexMatcher.group(2));
					index = Optional.of(indexMatcher.group(4));
				} else {
					throw new AssemblyHomologyStorageException(
							"Unable to parse duplicate key error: " +
							// could include a token hash as the key, so split it out if it's there
							mwe.getMessage().split("dup key")[0], mwe);
				}
				final Matcher keyMatcher = keyPattern.matcher(mwe.getMessage());
				if (keyMatcher.find()) {
					key = Optional.of(keyMatcher.group(1));
				} else { // some errors include the dup key, some don't
					key = Optional.absent();
				}
			} else {
				collection = Optional.absent();
				index = Optional.absent();
				key = Optional.absent();
			}
			
		}
		
		public static boolean isDuplicate(final MongoWriteException mwe) {
			return mwe.getError().getCategory().equals(ErrorCategory.DUPLICATE_KEY);
		}

		public boolean isDuplicate() {
			return isDuplicate;
		}

		public Optional<String> getCollection() {
			return collection;
		}

		public Optional<String> getIndex() {
			return index;
		}

		@SuppressWarnings("unused") // may need later
		public Optional<String> getKey() {
			return key;
		}
	}
	
	@Override
	public void createOrReplaceNamespace(final Namespace namespace)
			throws AssemblyHomologyStorageException {
		checkNotNull(namespace, "namespace");
		final MinHashSketchDatabase sketchDB = namespace.getSketchDatabase();
		final Document ns = new Document("$set", new Document()
				.append(Fields.NAMESPACE_LOAD_ID, namespace.getLoadID().getName())
				.append(Fields.NAMESPACE_DATASOURCE_ID, namespace.getSourceID().getName())
				.append(Fields.NAMESPACE_DATABASE_ID, namespace.getSourceDatabaseID())
				.append(Fields.NAMESPACE_DESCRIPTION, namespace.getDescription().orNull())
				.append(Fields.NAMESPACE_IMPLEMENTATION,
						sketchDB.getImplementationInformation().getImplementationName())
				.append(Fields.NAMESPACE_IMPLEMENTATION_VERSION,
						sketchDB.getImplementationInformation().getImplementationVersion())
				.append(Fields.NAMESPACE_KMER_SIZE, sketchDB.getParameterSet().getKmerSize())
				.append(Fields.NAMESPACE_SKETCH_SIZE,
						sketchDB.getParameterSet().getSketchSize().orNull())
				.append(Fields.NAMESPACE_SCALING_FACTOR,
						sketchDB.getParameterSet().getScaling().orNull())
				.append(Fields.NAMESPACE_SKETCH_DB_PATH,
						sketchDB.getLocation().getPathToFile().get().toString())
				.append(Fields.NAMESPACE_SEQUENCE_COUNT, sketchDB.getSequenceCount()));
		
		final Document query = new Document(Fields.NAMESPACE_ID, namespace.getId().getName());
		try {
			db.getCollection(COL_NAMESPACES).updateOne(
					query,
					ns,
					new UpdateOptions().upsert(true));
		} catch (MongoException e) {
			throw new AssemblyHomologyStorageException(
					"Connection to database failed: " + e.getMessage(), e);
		}
	}

	@Override
	public Namespace getNamespace(final NamespaceID namespace)
			throws AssemblyHomologyStorageException, NoSuchNamespaceException {
		checkNotNull(namespace, "namespace");
		final Document ns = findOne(
				COL_NAMESPACES, new Document(Fields.NAMESPACE_ID, namespace.getName()));
		if (ns == null) {
			throw new NoSuchNamespaceException(namespace.getName());
		}
		try {
			return Namespace.getBuilder(
					namespace,
					new MinHashSketchDatabase(
							new MinHashImplementationInformation(
									ns.getString(Fields.NAMESPACE_IMPLEMENTATION),
									ns.getString(Fields.NAMESPACE_IMPLEMENTATION_VERSION)),
							buildParameters(ns),
							new MinHashDBLocation(
									Paths.get(ns.getString(Fields.NAMESPACE_SKETCH_DB_PATH))),
							ns.getInteger(Fields.NAMESPACE_SEQUENCE_COUNT)),
					new LoadID(ns.getString(Fields.NAMESPACE_LOAD_ID)),
					new DataSourceID(ns.getString(Fields.NAMESPACE_DATASOURCE_ID)))
					.withNullableSourceDatabaseID(ns.getString(Fields.NAMESPACE_DATABASE_ID))
					.withNullableDescription(ns.getString(Fields.NAMESPACE_DESCRIPTION))
					.build();
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new AssemblyHomologyStorageException(
					"Unexpected value in database: " + e.getMessage(), e);
		}
	}

	private MinHashParameters buildParameters(final Document ns) {
		final MinHashParameters.Builder b = MinHashParameters.getBuilder(
				ns.getInteger(Fields.NAMESPACE_KMER_SIZE));
		final Integer sketchSize = ns.getInteger(Fields.NAMESPACE_SKETCH_SIZE);
		if (sketchSize == null) {
			return b.withScaling(ns.getInteger(Fields.NAMESPACE_SCALING_FACTOR)).build();
		} else {
			return b.withSketchSize(sketchSize).build();
		}
	}

	/* Use this for finding documents where indexes should force only a single
	 * document. Assumes the indexes are doing their job.
	 */
	private Document findOne(
			final String collection,
			final Document query)
			throws AssemblyHomologyStorageException {
		return findOne(collection, query, null);
	}
	
	/* Use this for finding documents where indexes should force only a single
	 * document. Assumes the indexes are doing their job.
	 */
	private Document findOne(
			final String collection,
			final Document query,
			final Document projection)
			throws AssemblyHomologyStorageException {
		try {
			return db.getCollection(collection).find(query).projection(projection).first();
		} catch (MongoException e) {
			throw new AssemblyHomologyStorageException(
					"Connection to database failed: " + e.getMessage(), e);
		}
	}
	
	
	@Override
	public void deleteNamespace(final NamespaceID namespace) {
		// TODO FEATURE delete namespace and sequence data
		
	}
	
	public static void main(final String[] args) throws Exception {
		@SuppressWarnings("resource")
		final MongoClient mc = new MongoClient("localhost");
		final AssemblyHomologyStorage storage = new MongoAssemblyHomologyStorage(
				mc.getDatabase("assemblyhomology"));
		
		
		final Namespace ns = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashImplementationInformation("Mash", "2.0"),
						MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
						new MinHashDBLocation(Paths.get("/tmp/fake")),
						2400),
				new LoadID("some UUID"),
				new DataSourceID("KBase"))
				.withNullableDescription("desc")
				.withNullableSourceDatabaseID("CI Refseq")
				.build();
		
		storage.createOrReplaceNamespace(ns);
		
		System.out.println(storage.getNamespace(new NamespaceID("foo")));
		
		final Namespace ns2 = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashImplementationInformation("Mash", "2.0"),
						MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
						new MinHashDBLocation(Paths.get("/tmp/fake2")),
						2400),
				new LoadID("some UUID2"),
				new DataSourceID("KBase2"))
				.withNullableDescription("desc2")
				.withNullableSourceDatabaseID("CI Refseq2")
				.build();
		
		storage.createOrReplaceNamespace(ns2);
		
		System.out.println(storage.getNamespace(new NamespaceID("foo")));
	}

}
