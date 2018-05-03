package us.kbase.assemblyhomology.cli;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.PrintStream;

import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class AssemblyHomologyCLI {

	public static void main(final String[] args) {
		System.exit(new AssemblyHomologyCLI(args, System.out, System.err).execute());
	}
	
	private static final String PROG_NAME = "assembly_homology";
	private static final String CMD_LOAD = "load";
	
	private final String[] args;
	private final PrintStream out;
	private final PrintStream err;
	
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
	
	private int execute() {
		final GlobalArgs globalArgs = new GlobalArgs();
		JCommander jc = new JCommander(globalArgs);
		jc.setProgramName(PROG_NAME);
		
		final LoadArgs loadArgs = new LoadArgs();
		jc.addCommand(CMD_LOAD, loadArgs);
		
		try {
			jc.parse(args);
		} catch (ParameterException e) {
			printError(e, globalArgs);
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
		// TODO Auto-generated method stub
		return 0;
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

	private void printError(final Throwable e, final GlobalArgs a) {
		printError("Error", e, a);
	}
	
	private void printError(
			final String msg,
			final Throwable e,
			final GlobalArgs a) {
		final String message;
		// sigh. hacky hacky hacky
		if (e.getMessage().endsWith("but no main parameter was defined in your arg class")) {
			message = "A positional parameter was provided but this command does not accept " +
					"positional parameters";
		} else {
			message = e.getMessage();
		}
		err.println(msg + ": " + message);
		if (a.verbose) {
			e.printStackTrace(err);
		}
	}

	private static class GlobalArgs {
		@Parameter(names = {"-h","--help"}, help = true,
				description = "Display help and usage information.")
		boolean help = false;
		
		@Parameter(names = {"-v", "--verbose"}, description = "Print full stack trace on error")
		boolean verbose = false;
	}
	
	private static class LoadArgs {
		@Parameter(names = {"-l", "--load-id"}, required = true,
				description = "The id for the load. If one is not supplied, a random ID " +
				"will be generated. Reusing a load ID will cause sequence data for matching " +
				"IDs to be overwritten. Any data associated with IDs exclusive to the " +
				"previous load will remain.")
		String loadID = null;
		
		
	}
}
