package us.kbase.assemblyhomology.build;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import us.kbase.assemblyhomology.config.AssemblyHomologyConfig;
import us.kbase.assemblyhomology.core.AssemblyHomology;
import us.kbase.assemblyhomology.minhash.mash.MashFactory;
import us.kbase.assemblyhomology.storage.AssemblyHomologyStorage;
import us.kbase.assemblyhomology.storage.exceptions.StorageInitException;
import us.kbase.assemblyhomology.storage.mongo.MongoAssemblyHomologyStorage;

public class AssemblyHomologyBuilder {

	//TODO TEST
	//TODO JAVADOC
	
	private final MongoClient mc;
	private final AssemblyHomology assyhomol;
	private final AssemblyHomologyStorage storage;
	
	public AssemblyHomologyBuilder(final AssemblyHomologyConfig cfg)
			throws StorageInitException {
		checkNotNull(cfg, "cfg");
		mc = buildMongo(cfg);
		storage = buildStorage(cfg, mc);
		assyhomol = buildAssemblyHomology(cfg, storage);
	}
	
	public AssemblyHomologyBuilder(
			final AssemblyHomologyConfig cfg,
			final MongoClient mc)
			throws StorageInitException {
		checkNotNull(cfg, "cfg");
		checkNotNull(mc, "mc");
		this.mc = mc;
		storage = buildStorage(cfg, mc);
		assyhomol = buildAssemblyHomology(cfg, storage);
	}
	
	private MongoClient buildMongo(final AssemblyHomologyConfig c) throws StorageInitException {
		//TODO ZLATER MONGO handle shards & replica sets
		try {
			if (c.getMongoUser().isPresent()) {
				final List<MongoCredential> creds = Arrays.asList(MongoCredential.createCredential(
						c.getMongoUser().get(), c.getMongoDatabase(), c.getMongoPwd().get()));
				// unclear if and when it's safe to clear the password
				return new MongoClient(new ServerAddress(c.getMongoHost()), creds);
			} else {
				return new MongoClient(new ServerAddress(c.getMongoHost()));
			}
		} catch (MongoException e) {
			LoggerFactory.getLogger(getClass()).error(
					"Failed to connect to MongoDB: " + e.getMessage(), e);
			throw new StorageInitException("Failed to connect to MongoDB: " + e.getMessage(), e);
		}
	}
	
	private AssemblyHomology buildAssemblyHomology(
			final AssemblyHomologyConfig c,
			final AssemblyHomologyStorage storage)
			throws StorageInitException {
		return new AssemblyHomology(
				storage,
				new HashSet<>(Arrays.asList(new MashFactory())),
				c.getPathToTemporaryFileDirectory(),
				c.getMinhashTimoutSec());
	}

	private AssemblyHomologyStorage buildStorage(
			final AssemblyHomologyConfig c,
			final MongoClient mc)
			throws StorageInitException {
		final MongoDatabase db;
		try {
			db = mc.getDatabase(c.getMongoDatabase());
		} catch (MongoException e) {
			LoggerFactory.getLogger(getClass()).error(
					"Failed to get database from MongoDB: " + e.getMessage(), e);
			throw new StorageInitException("Failed to get database from MongoDB: " +
					e.getMessage(), e);
		}
		//TODO TEST authenticate to db, write actual test with authentication
		return new MongoAssemblyHomologyStorage(db);
	}
	
	public MongoClient getMongoClient() {
		return mc;
	}

	public AssemblyHomology getAssemblyHomology() {
		return assyhomol;
	}
	
	public AssemblyHomologyStorage getStorage() {
		return storage;
	}
	
}
