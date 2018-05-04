package us.kbase.assemblyhomology.cli;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.isNullOrEmpty;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import us.kbase.assemblyhomology.cli.AssemblyHomologyCLIConfig.AssemblyHomlogyCLIConfigException;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.load.Loader;
import us.kbase.assemblyhomology.load.exceptions.LoadInputParseException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashInitException;
import us.kbase.assemblyhomology.minhash.mash.Mash;
import us.kbase.assemblyhomology.storage.AssemblyHomologyStorage;
import us.kbase.assemblyhomology.storage.exceptions.AssemblyHomologyStorageException;
import us.kbase.assemblyhomology.storage.mongo.MongoAssemblyHomologyStorage;
import us.kbase.assemblyhomology.util.FileOpener;
import us.kbase.assemblyhomology.util.PathRestreamable;

public class AssemblyHomologyCLI {

	public static void main(final String[] args) {
		System.exit(new AssemblyHomologyCLI(args, System.out, System.err, new FileOpener())
				.execute());
	}
	
	private static final String PROG_NAME = "assembly_homology";
	private static final String CMD_LOAD = "load";
	private static final String MASH = "mash";
	
	private final String[] args;
	private final PrintStream out;
	private final PrintStream err;
	private final FileOpener fileOpener;
	
	public AssemblyHomologyCLI(
			final String[] args,
			final PrintStream out,
			final PrintStream err,
			final FileOpener fileOpener) {
		checkNotNull(args, "args");
		checkNotNull(out, "out");
		checkNotNull(err, "err");
		checkNotNull(fileOpener, "fileOpener");
		this.args = args;
		this.out = out;
		this.err = err;
		this.fileOpener = fileOpener;
		quietLogger();
	}
	
	private int execute() {
		final GlobalArgs globalArgs = new GlobalArgs();
		JCommander jc = new JCommander(globalArgs);
		jc.setProgramName(PROG_NAME);
		
		final LoadArgs loadArgs = new LoadArgs();
		jc.addCommand(CMD_LOAD, loadArgs);
		
		try {
			jc.parse(args);
		} catch (ParameterException e) {
			printError(e, globalArgs.verbose);
			return 1;
		}
		if (globalArgs.help) {
			usage(jc);
			return 0;
		}
		if (jc.getParsedCommand() == null) {
			usage(jc);
			return 1;
		}
		final AssemblyHomologyCLIConfig cfg;
		try {
			cfg = getConfig(globalArgs.configPath);
		} catch (NoSuchFileException e) {
			printError("No such file", e, globalArgs.verbose);
			return 1;
		} catch (AccessDeniedException e) {
			printError("Access denied", e, globalArgs.verbose);
			return 1;
		} catch (IOException e) {
			printError(e, globalArgs.verbose);
			return 1;
		} catch (AssemblyHomlogyCLIConfigException e) {
			printError("For config file " + globalArgs.configPath, e, globalArgs.verbose);
			return 1;
		}
		if (jc.getParsedCommand().equals(CMD_LOAD)) {
			try {
				load(loadArgs, cfg);
			} catch (AssemblyHomologyStorageException | MissingParameterException |
					IllegalParameterException | MinHashException | IOException |
					LoadInputParseException e) {
				printError(e, globalArgs.verbose);
				return 1;
			}
		}
		return 0;
	}
	
	private AssemblyHomologyCLIConfig getConfig(final String configPath)
			throws IOException, AssemblyHomlogyCLIConfigException {
		final Path path = Paths.get(configPath);
		final Properties p = new Properties();
		p.load(Files.newInputStream(path));
		return AssemblyHomologyCLIConfig.from(p);
	}
	
	private MongoClient buildMongo(final AssemblyHomologyCLIConfig c)
			throws AssemblyHomologyStorageException {
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
			throw new AssemblyHomologyStorageException(
					"Failed to connect to MongoDB: " + e.getMessage(), e);
		}
	}
	
	private void load(final LoadArgs loadArgs, final AssemblyHomologyCLIConfig config)
			throws AssemblyHomologyStorageException, MissingParameterException,
				IllegalParameterException, MinHashInitException, MinHashException, IOException,
				LoadInputParseException {
		if (!MASH.equals(loadArgs.implementation)) {
			throw new MinHashException("Unsupported implementation: " + loadArgs.implementation);
		}
		try (final MongoClient mc = buildMongo(config)) {
			final AssemblyHomologyStorage storage = new MongoAssemblyHomologyStorage(
					mc.getDatabase(config.getMongoDatabase()));
			new Loader(storage).load(
					getLoadID(loadArgs),
					new Mash(Paths.get(config.getTempDir())),
					new MinHashDBLocation(Paths.get(loadArgs.sketchDBPath)),
					new PathRestreamable(Paths.get(loadArgs.namespaceYAML), fileOpener),
					new PathRestreamable(Paths.get(loadArgs.sequeneceMetadataPath), fileOpener));
		}
	}

	private LoadID getLoadID(final LoadArgs loadArgs)
			throws MissingParameterException, IllegalParameterException {
		final LoadID loadID;
		if (isNullOrEmpty(loadArgs.loadID)) {
			loadID = new LoadID(UUID.randomUUID().toString());
			out.println("Generated load id " + loadID.getName());
		} else {
			loadID = new LoadID(loadArgs.loadID);
		}
		return loadID;
	}

	private void usage(final JCommander jc) {
		final StringBuilder sb = new StringBuilder();
		jc.usage(sb);
		out.println(sb.toString());
	}

	private void quietLogger() {
		((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
				.setLevel(Level.OFF);
	}

	private void printError(final Throwable e, final boolean verbose) {
		printError("Error", e, verbose);
	}
	
	private void printError(
			final String msg,
			final Throwable e,
			final boolean verbose) {
		final String message;
		// sigh. hacky hacky hacky
		if (e.getMessage().endsWith("but no main parameter was defined in your arg class")) {
			message = "A positional parameter was provided but this command does not accept " +
					"positional parameters";
		} else {
			message = e.getMessage();
		}
		err.println(msg + ": " + message);
		if (verbose) {
			e.printStackTrace(err);
		}
	}

	private static class GlobalArgs {
		
		@Parameter(names = {"-h","--help"}, help = true,
				description = "Display help and usage information.")
		boolean help = false;
		
		@Parameter(names = {"-v", "--verbose"}, description = "Print full stack trace on error.")
		boolean verbose = false;
		
		@Parameter(names = {"-c", "--config"}, required = true,
				description = "Path to the assembly_homology configuration file.")
		String configPath;
	}
	
	@Parameters(commandDescription = "Load data into the database")
	private static class LoadArgs {
		
		@Parameter(names = {"-k", "--sketch-db"}, required = true,
				description = "The path to the sketch database. This path will be recorded in " +
				"the assembly homology service database as the path to the sketch database, so " +
				"ensure the sketch database is in its permanent location.")
		String sketchDBPath;
		
		@Parameter(names = {"-n", "--namespace-yaml"}, required = true,
				description = "The path to the YAML file containing information about the " +
				"namespace to be created or updated. See the documentation for the syntax.")
		String namespaceYAML;
		
		@Parameter(names = {"-s", "--sequence-metadata"}, required = true,
				description = "the path to the file containing information about each sequence " +
				"in the sketch DB. See the documentation for the syntax.")
		String sequeneceMetadataPath;
		
		@Parameter(names = {"-l", "--load-id"},
				description = "The id for the load. If one is not supplied, a random ID " +
				"will be generated. Reusing a load ID will cause sequence data for matching " +
				"IDs to be overwritten. Any data associated with IDs exclusive to the " +
				"previous load will remain.")
		String loadID = null;
		
		@Parameter(names = {"-i", "--implementation"},
				description = "The MinHash implementation to use.")
		String implementation = MASH;
	}
}
