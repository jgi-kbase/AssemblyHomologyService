package us.kbase.assemblyhomology.cli;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.isNullOrEmpty;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.UUID;

import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.mongodb.MongoClient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import us.kbase.assemblyhomology.build.AssemblyHomologyBuilder;
import us.kbase.assemblyhomology.config.AssemblyHomologyConfig;
import us.kbase.assemblyhomology.config.AssemblyHomologyConfigurationException;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.load.Loader;
import us.kbase.assemblyhomology.load.exceptions.LoadInputParseException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashInitException;
import us.kbase.assemblyhomology.minhash.mash.Mash;
import us.kbase.assemblyhomology.storage.exceptions.AssemblyHomologyStorageException;
import us.kbase.assemblyhomology.util.FileOpener;
import us.kbase.assemblyhomology.util.PathRestreamable;

/** A CLI for the assembly homology software package.
 * @author gaprice@lbl.gov
 *
 */
public class AssemblyHomologyCLI {
	
	/* Note that testing the default location of the deploy.cfg file is difficult since
	 * three's no reliable way to change the JVM's working directory, and the standard
	 * deploy.cfg file lives there. Hence testing with the default location needs to be
	 * done manually.
	 */

	/** The main method.
	 * @param args the arguments to the program. Use -h to get help.
	 */
	public static void main(final String[] args) {
		// this line is only tested manually.
		System.exit(new AssemblyHomologyCLI(args, System.out, System.err).execute());
	}
	
	private static final String PROG_NAME = "assembly_homology";
	private static final String CMD_LOAD = "load";
	private static final String MASH = "mash";
	
	private final String[] args;
	private final PrintStream out;
	private final PrintStream err;
	
	/** Create a new CLI.
	 * @param args the arguments to the program. Use -h to get help.
	 * @param out the output stream.
	 * @param err the error stream.
	 */
	public AssemblyHomologyCLI(
			final String[] args,
			final PrintStream out,
			final PrintStream err) {
		checkNotNull(args, "args");
		checkNotNull(out, "out");
		checkNotNull(err, "err");
		this.args = args;
		this.out = out;
		this.err = err;
		quietLogger();
	}
	
	/** Execute the command.
	 * @return the return code.
	 */
	public int execute() {
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
		final AssemblyHomologyConfig cfg;
		try {
			cfg = new AssemblyHomologyConfig(Paths.get(globalArgs.configPath), true);
		} catch (AssemblyHomologyConfigurationException e) {
			printError(e, globalArgs.verbose);
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
	
	private void load(final LoadArgs loadArgs, final AssemblyHomologyConfig cfg)
			throws AssemblyHomologyStorageException, MissingParameterException,
				IllegalParameterException, MinHashInitException, MinHashException, IOException,
				LoadInputParseException {
		if (!MASH.equals(loadArgs.implementation)) {
			throw new MinHashException("Unsupported implementation: " + loadArgs.implementation);
		}
		final AssemblyHomologyBuilder builder = new AssemblyHomologyBuilder(cfg);
		try (final MongoClient mc = builder.getMongoClient()) {
			new Loader(builder.getStorage()).load(
					getLoadID(loadArgs),
					new Mash(cfg.getPathToTemporaryFileDirectory(), cfg.getMinhashTimoutSec()),
					new MinHashDBLocation(Paths.get(loadArgs.sketchDBPath)),
					new PathRestreamable(Paths.get(loadArgs.namespaceYAML), new FileOpener()),
					new PathRestreamable(Paths.get(loadArgs.sequeneceMetadataPath),
							new FileOpener()));
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
		
		@Parameter(names = {"-c", "--config"}, 
				description = "Path to the assembly homology configuration file.")
		String configPath = "./deploy.cfg";
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
