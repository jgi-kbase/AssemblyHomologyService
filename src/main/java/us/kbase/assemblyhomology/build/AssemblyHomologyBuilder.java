package us.kbase.assemblyhomology.build;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import us.kbase.assemblyhomology.config.AssemblyHomologyConfig;
import us.kbase.assemblyhomology.config.AssemblyHomologyConfigurationException;
import us.kbase.assemblyhomology.config.FilterConfiguration;
import us.kbase.assemblyhomology.core.AssemblyHomology;
import us.kbase.assemblyhomology.core.MinHashDistanceFilterFactory;
import us.kbase.assemblyhomology.minhash.mash.MashFactory;
import us.kbase.assemblyhomology.storage.AssemblyHomologyStorage;
import us.kbase.assemblyhomology.storage.exceptions.StorageInitException;
import us.kbase.assemblyhomology.storage.mongo.MongoAssemblyHomologyStorage;
import us.kbase.assemblyhomology.util.Util;

/** A class for building a {@link AssemblyHomology} instance given a {@link AssemblyHomologyConfig}
 * configuration instance.
 * @author gaprice@lbl.gov
 *
 */
public class AssemblyHomologyBuilder {

	//TODO TEST
	
	private final MongoClient mc;
	private final AssemblyHomology assyhomol;
	private final AssemblyHomologyStorage storage;
	private final Set<MinHashDistanceFilterFactory> filterFactories;
	
	/** Build an assembly homology instance.
	 * @param cfg the configuration to build to.
	 * @throws StorageInitException if the storage system could not be initialized.
	 * @throws AssemblyHomologyConfigurationException if the filters specified in the configuration
	 * could not be initialized.
	 */
	public AssemblyHomologyBuilder(final AssemblyHomologyConfig cfg)
			throws StorageInitException, AssemblyHomologyConfigurationException {
		checkNotNull(cfg, "cfg");
		mc = buildMongo(cfg);
		storage = buildStorage(cfg, mc);
		filterFactories = Collections.unmodifiableSet(buildFilterFactories(cfg));
		assyhomol = buildAssemblyHomology(cfg, storage, filterFactories);
	}
	
	/** Build an assembly homology instance with a previously existing MongoDB client. MongoDB
	 * recommends creating only one client per process. The client must have been retrieved
	 * from {@link #getMongoClient()}.
	 * @param cfg the configuration to build to.
	 * @param mc the MongoDB client.
	 * @throws StorageInitException if the storage system could not be initialized.
	 * @throws AssemblyHomologyConfigurationException if the filters specified in the configuration
	 * could not be initialized.
	 */
	public AssemblyHomologyBuilder(
			final AssemblyHomologyConfig cfg,
			final MongoClient mc)
			throws StorageInitException, AssemblyHomologyConfigurationException {
		checkNotNull(cfg, "cfg");
		checkNotNull(mc, "mc");
		this.mc = mc;
		storage = buildStorage(cfg, mc);
		filterFactories = Collections.unmodifiableSet(buildFilterFactories(cfg));
		assyhomol = buildAssemblyHomology(cfg, storage, filterFactories);
	}
	
	private MongoClient buildMongo(final AssemblyHomologyConfig c) throws StorageInitException {
		//TODO ZLATER MONGO handle shards & replica sets
		final MongoClientSettings.Builder mongoBuilder = MongoClientSettings.builder().applyToClusterSettings(
				builder -> builder.hosts(Arrays.asList(new ServerAddress(c.getMongoHost()))));
		try {
			if (c.getMongoUser().isPresent()) {
				final MongoCredential creds = MongoCredential.createCredential(
						c.getMongoUser().get(), c.getMongoDatabase(), c.getMongoPwd().get());
				// unclear if and when it's safe to clear the password
				return MongoClients.create(mongoBuilder.credential(creds).build());
			} else {
				return MongoClients.create(mongoBuilder.build());
			}
		} catch (MongoException e) {
			LoggerFactory.getLogger(getClass()).error(
					"Failed to connect to MongoDB: " + e.getMessage(), e);
			throw new StorageInitException("Failed to connect to MongoDB: " + e.getMessage(), e);
		}
	}
	
	private AssemblyHomology buildAssemblyHomology(
			final AssemblyHomologyConfig c,
			final AssemblyHomologyStorage storage,
			final Set<MinHashDistanceFilterFactory> filterFactories)
			throws StorageInitException {
		return new AssemblyHomology(
				storage,
				new HashSet<>(Arrays.asList(new MashFactory())),
				filterFactories,
				c.getPathToTemporaryFileDirectory(),
				c.getMinhashTimeoutSec());
	}

	private Set<MinHashDistanceFilterFactory> buildFilterFactories(final AssemblyHomologyConfig c)
			throws AssemblyHomologyConfigurationException {
		final Set<MinHashDistanceFilterFactory> facs = new HashSet<>();
		for (final FilterConfiguration fcfg: c.getFilterConfigurations()) {
			facs.add(Util.loadClassWithInterface(
					fcfg.getFactoryClassName(),
					MinHashDistanceFilterFactory.class,
					fcfg.getConfig()));
		}
		return facs;
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
	
	/** Get the mongo client associated with the assembly homology instance.
	 * @return the mongo client.
	 */
	public MongoClient getMongoClient() {
		return mc;
	}

	/** Get the assembly homology instance.
	 * @return the assembly homology instance.
	 */
	public AssemblyHomology getAssemblyHomology() {
		return assyhomol;
	}
	
	/** Get the storage system for the assembly homology instance.
	 * @return the storage system.
	 */
	public AssemblyHomologyStorage getStorage() {
		return storage;
	}
	
	/** Get the filter factories associated with the assembly homology instance.
	 * @return the filter factories.
	 */
	public Set<MinHashDistanceFilterFactory> getFilterFactories() {
		return filterFactories;
	}
	
}
