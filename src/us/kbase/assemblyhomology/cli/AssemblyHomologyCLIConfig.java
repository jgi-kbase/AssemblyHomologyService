package us.kbase.assemblyhomology.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import java.util.Properties;

import com.google.common.base.Optional;

public class AssemblyHomologyCLIConfig {

	//TODO TESTS
	//TODO JAVADOC

	private static final String MONGO_HOST = "mongo-host";
	private static final String MONGO_DB = "mongo-db";
	private static final String MONGO_USER = "mongo-user";
	private static final String MONGO_PWD = "mongo-pwd";

	private static final String TEMP_DIR = "temp-dir";

	private final String mongoHost;
	private final String mongoDB;
	private final Optional<String> mongoUser;
	private final Optional<char[]> mongoPwd;

	private final String tempDir;

	private AssemblyHomologyCLIConfig(
			final String mongoHost,
			final String mongoDB,
			final String mongoUser,
			String mongoPwd,
			final String tempDir)
			throws AssemblyHomlogyCLIConfigException {

		this.mongoHost = mongoHost;
		this.mongoDB = mongoDB;
		if (mongoUser == null ^ mongoPwd == null) { // xor
			mongoPwd = null; // gc
			throw new AssemblyHomlogyCLIConfigException(String.format(
					"Must provide both %s and %s params in config " +
					" if MongoDB authentication is to be used",
					MONGO_USER, MONGO_PWD));
		}
		this.mongoUser = Optional.fromNullable(mongoUser);
		this.mongoPwd = Optional.fromNullable(mongoPwd == null ?
				null :mongoPwd.toCharArray());
		mongoPwd = null;

		this.tempDir = tempDir;
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

	public String getTempDir() {
		return tempDir;
	}

	public static AssemblyHomologyCLIConfig from(final Properties p)
			throws AssemblyHomlogyCLIConfigException {
		final Map<String, String> cfg = new HashMap<>();
		for (final Entry<Object, Object> e: p.entrySet()) {
			cfg.put((String) e.getKey(), (String) e.getValue());
		}
		return from(cfg);
	}

	public static AssemblyHomologyCLIConfig from(final Map<String, String> cfg)
			throws AssemblyHomlogyCLIConfigException {
		return new AssemblyHomologyCLIConfig(
				getString(MONGO_HOST, cfg, true),
				getString(MONGO_DB, cfg, true),
				getString(MONGO_USER, cfg),
				getString(MONGO_PWD, cfg),
				getString(TEMP_DIR, cfg, true));
	}

	// returns null if no string
	private static String getString(
			final String paramName,
			final Map<String, String> config)
			throws AssemblyHomlogyCLIConfigException {
		return getString(paramName, config, false);
	}

	private static String getString(
			final String paramName,
			final Map<String, String> config,
			final boolean except)
			throws AssemblyHomlogyCLIConfigException {
		final String s = config.get(paramName);
		if (s != null && !s.trim().isEmpty()) {
			return s.trim();
		} else if (except) {
			throw new AssemblyHomlogyCLIConfigException(String.format(
					"Required parameter %s not provided in configuration", paramName));
		} else {
			return null;
		}
	}

	@SuppressWarnings("serial")
	public static class AssemblyHomlogyCLIConfigException extends Exception {

		public AssemblyHomlogyCLIConfigException(final String message) {
			super(message);
		}

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AssemblyHomologyCLIConfig [searchMongoHost=");
		builder.append(mongoHost);
		builder.append(", searchMongoDB=");
		builder.append(mongoDB);
		builder.append(", searchMongoUser=");
		builder.append(mongoUser);
		builder.append(", searchMongoPwd=");
		builder.append(mongoPwd);
		builder.append(", tempDir=");
		builder.append(tempDir);
		builder.append("]");
		return builder.toString();
	}

}
