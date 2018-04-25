package us.kbase.assemblyhomology.minhash.mash;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;
import us.kbase.assemblyhomology.minhash.MinHashImplementation;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashInitException;

public class Mash implements MinHashImplementation {
	
	//TODO JAVADOC
	//TODO TEST
	
	private final static String MASH = "mash";
	
	private final MinHashImplementationInformation info;
	
	public Mash() throws MinHashInitException {
		info = getInfo();
	}

	private MinHashImplementationInformation getInfo() throws MinHashInitException {
		try {
			final String version = getVersion(getMashOutput("-h"));
			return new MinHashImplementationInformation(MASH, version);
		} catch (MashException e) {
			throw new MinHashInitException(e.getMessage(), e);
		}
	}
	
	// only use when expecting a small amount of output, otherwise will put entire output in mem
	// or more likely deadlock
	private String getMashOutput(final String... arguments) throws MashException {
		final List<String> command = new LinkedList<>(Arrays.asList(MASH));
		command.addAll(Arrays.asList(arguments));
		try {
			final Process mash = new ProcessBuilder(command).start();
			if (!mash.waitFor(10L, TimeUnit.SECONDS)) {
				throw new MashException(String.format(
						"Timed out waiting for %s to run", MASH));
			}
			if (mash.exitValue() != 0) {
				try (final InputStream is = mash.getErrorStream()) {
					throw new MashException(String.format(
							"Error running %s: %s", MASH, IOUtils.toString(is)));
				}
			}
			try (final InputStream is = mash.getInputStream()) {
				return IOUtils.toString(is);
			}
		} catch (IOException | InterruptedException e) {
			throw new MashException(String.format(
					"Error running %s: ", MASH) + e.getMessage(), e);
		}
	}
	
	private String getVersion(final String mashHelpOut) {
		//TODO CODE use regex later
		final String[] lines = mashHelpOut.split("\n");
		final String[] verline = lines[1].trim().split("\\s+");
		return verline[verline.length - 1].trim();
	}

	@Override
	public MinHashImplementationInformation getImplementationInformation() {
		return info;
	}

	@Override
	public MinHashSketchDatabase getDatabase(final MinHashDBLocation location)
			throws MashException {
		checkNotNull(location, "location");
		final MinHashParameters params = getParameters(location.getPathToFile().get());
		
		//TODO CODE add IDs
		return new MashSketchDatabase(info, params, location, Collections.emptyList());
	}
	
	private MinHashParameters getParameters(final Path path) throws MashException {
		final String mashout = getMashOutput("info", "-H", path.toString());
		//TODO CODE this is nasty. Use a regex or something
		final String[] lines = mashout.split("\n");
		final int kmerSize = Integer.parseInt(lines[2].trim().split("\\s+")[2].trim());
		final int hashCount = Integer.parseInt(lines[4].trim().split("\\s+")[4].trim());
		return MinHashParameters.getBuilder(kmerSize).withHashCount(hashCount).build();
	}

	public static void main(final String[] args) throws MinHashException {
		final MinHashImplementation mash = new Mash();
		System.out.println(mash.getImplementationInformation());
		
		final MinHashSketchDatabase db = mash.getDatabase(new MinHashDBLocation(Paths.get(
				"/home/crusherofheads/kb_refseq_sourmash/kb_refseq_ci_1000.msh")));
		System.out.println(db.getImplementationInformation());
		System.out.println(db.getLocation());
		System.out.println(db.getParameterSet());
		System.out.println(db.getSketchCount());
		System.out.println(db.getSketchIDs());
	}

}
