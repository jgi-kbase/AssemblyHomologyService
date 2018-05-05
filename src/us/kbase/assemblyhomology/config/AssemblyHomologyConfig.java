package us.kbase.assemblyhomology.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.ini4j.Ini;

import com.google.common.base.Optional;

public class AssemblyHomologyConfig {
	
	// we may want different configuration implementations for different environments. YAGNI for now.
	
	//TODO JAVADOC
	//TODO TEST
	
	private static final String ENV_VAR_ASSYHOM = "ASSEMBLY_HOMOLOGY_CONFIG";
	private static final String ENV_VAR_KB_DEP = "KB_DEPLOYMENT_CONFIG";
	
	private static final String CFG_LOC = "assemblyhomology";
	private static final String TEMP_KEY_CFG_FILE = "temp-key-config-file";
	
	private static final String KEY_MONGO_HOST = "mongo-host";
	private static final String KEY_MONGO_DB = "mongo-db";
	private static final String KEY_MONGO_USER = "mongo-user";
	private static final String KEY_MONGO_PWD = "mongo-pwd";
	private static final String KEY_TEMP_DIR = "temp-dir";
	
	
	private final String mongoHost;
	private final String mongoDB;
	private final Optional<String> mongoUser;
	private final Optional<char[]> mongoPwd;
	private final Path tempDir;

	public AssemblyHomologyConfig() throws AssemblyHomologyConfigurationException {
		this(getConfigPathFromEnv());
	}
	
	public AssemblyHomologyConfig(final Path filepath) 
			throws AssemblyHomologyConfigurationException {
		final Map<String, String> cfg = getConfig(filepath);
		tempDir = Paths.get(getString(KEY_TEMP_DIR, cfg, true));
		mongoHost = getString(KEY_MONGO_HOST, cfg, true);
		mongoDB = getString(KEY_MONGO_DB, cfg, true);
		mongoUser = Optional.fromNullable(getString(KEY_MONGO_USER, cfg));
		Optional<String> mongop = Optional.fromNullable(getString(KEY_MONGO_PWD, cfg));
		if (mongoUser.isPresent() ^ mongop.isPresent()) {
			mongop = null; //GC
			throw new AssemblyHomologyConfigurationException(String.format(
					"Must provide both %s and %s params in config file " +
					"%s section %s if MongoDB authentication is to be used",
					KEY_MONGO_USER, KEY_MONGO_PWD, cfg.get(TEMP_KEY_CFG_FILE), CFG_LOC));
		}
		mongoPwd = mongop.isPresent() ?
				Optional.of(mongop.get().toCharArray()) : Optional.absent();
		mongop = null; //GC
	}
	
	// returns null if no string
	private String getString(
			final String paramName,
			final Map<String, String> config)
			throws AssemblyHomologyConfigurationException {
		return getString(paramName, config, false);
	}
	
	private String getString(
			final String paramName,
			final Map<String, String> config,
			final boolean except)
			throws AssemblyHomologyConfigurationException {
		final String s = config.get(paramName);
		if (s != null && !s.trim().isEmpty()) {
			return s.trim();
		} else if (except) {
			throw new AssemblyHomologyConfigurationException(String.format(
					"Required parameter %s not provided in configuration file %s, section %s",
					paramName, config.get(TEMP_KEY_CFG_FILE), CFG_LOC));
		} else {
			return null;
		}
	}

	private static Path getConfigPathFromEnv()
			throws AssemblyHomologyConfigurationException {
		String file = System.getProperty(ENV_VAR_ASSYHOM) == null ?
				System.getenv(ENV_VAR_ASSYHOM) : System.getProperty(ENV_VAR_ASSYHOM);
		if (file == null) {
			file = System.getProperty(ENV_VAR_KB_DEP) == null ?
					System.getenv(ENV_VAR_KB_DEP) : System.getProperty(ENV_VAR_KB_DEP);
		}
		if (file == null || file.trim().isEmpty()) {
			throw new AssemblyHomologyConfigurationException(String.format(
					"Could not find deployment configuration file from either " +
					"permitted environment variable / system property: %s, %s",
					ENV_VAR_ASSYHOM, ENV_VAR_KB_DEP));
		}
		return Paths.get(file);
	}
	
	private Map<String, String> getConfig(final Path file)
			throws AssemblyHomologyConfigurationException {
		final File deploy = file.normalize().toAbsolutePath().toFile();
		final Ini ini;
		try {
			ini = new Ini(deploy);
		} catch (IOException ioe) {
			throw new AssemblyHomologyConfigurationException(String.format(
					"Could not read configuration file %s: %s",
					deploy, ioe.getMessage()), ioe);
		}
		final Map<String, String> config = ini.get(CFG_LOC);
		if (config == null) {
			throw new AssemblyHomologyConfigurationException(String.format(
					"No section %s in config file %s", CFG_LOC, deploy));
		}
		config.put(TEMP_KEY_CFG_FILE, deploy.getAbsolutePath());
		return config;
	}
	
	public String getMongoHost() {
		return mongoHost;
	}

	public String getMongoDatabase() {
		return mongoDB;
	}

	public Optional<String> getMongoUser() {
		return mongoUser;
	}

	public Optional<char[]> getMongoPwd() {
		return mongoPwd;
	}
	
	public Path getPathToTemporaryFileDirectory() {
		return tempDir;
	}
}
