package us.kbase.assemblyhomology.minhash.mash;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashDistanceSet;
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
	private final Path tempFileDirectory;
	
	public Mash(final Path tempFileDirectory) throws MinHashInitException {
		checkNotNull(tempFileDirectory, "tempFileDirectory");
		this.tempFileDirectory = tempFileDirectory;
		try {
			Files.createDirectories(tempFileDirectory);
		} catch (IOException e) {
			throw new MinHashInitException(e.getMessage(), e);
		}
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
		return getMashOutput(null, arguments);
	}

	// returns null if outputPath is not null
	private String getMashOutput(final Path outputPath, final String... arguments)
			throws MashException {
		final List<String> command = new LinkedList<>(Arrays.asList(MASH));
		command.addAll(Arrays.asList(arguments));
		try {
			final ProcessBuilder pb = new ProcessBuilder(command);
			if (outputPath != null) {
				// it's far less complicated if we just redirect to a file rather than have
				// threads consuming output and error so they don't deadlock
				pb.redirectOutput(outputPath.toFile());
			}
			final Process mash = pb.start();
			if (!mash.waitFor(30L, TimeUnit.SECONDS)) {
				throw new MashException(String.format(
						"Timed out waiting for %s to run", MASH));
			}
			if (mash.exitValue() != 0) {
				try (final InputStream is = mash.getErrorStream()) {
					throw new MashException(String.format(
							"Error running %s: %s", MASH, IOUtils.toString(is)));
				}
			}
			if (outputPath != null) {
				return null;
			} else {
				try (final InputStream is = mash.getInputStream()) {
					return IOUtils.toString(is);
				}
			}
		} catch (IOException | InterruptedException e) {
			throw new MashException(String.format(
					"Error running %s: ", MASH) + e.getMessage(), e);
		}
	}

	private String getVersion(final String mashHelpOut) {
		//TODO CODE brittle
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
		final ParamsAndSize pns = getParametersAndSize(location.getPathToFile().get());
		return new MinHashSketchDatabase(info, pns.params, location, pns.size);
	}
	
	@Override
	public List<String> getSketchIDs(final MinHashSketchDatabase db) throws MashException {
		return processMashOutput(
				//TODO CODE make less brittle
				l -> l.split("\\s+")[2].trim(),
				true,
				"info", "-t", db.getLocation().getPathToFile().get().toString());
	}
	
	private interface LineProcessor<T> {
		T processline(String line);
	}
	
	// use for large output, creates a temp file
	private <T> List<T> processMashOutput(
			final LineProcessor<T> lineProcessor,
			final boolean skipHeader,
			final String... command)
			throws MashException {
		Path tempFile = null;
		try {
			tempFile = Files.createTempFile(tempFileDirectory, "mash_output", ".tmp");
			getMashOutput(tempFile, command);
			final List<T> results = new LinkedList<>();
			try (final InputStream is = Files.newInputStream(tempFile)) {
				final BufferedReader br = new BufferedReader(new InputStreamReader(
						is, StandardCharsets.UTF_8));
				if (skipHeader) {
					br.readLine();
				}
				for (String line = br.readLine(); line != null; line = br.readLine()) {
					results.add(lineProcessor.processline(line));
				}
			}
			return results;
		} catch (IOException e) {
			throw new MashException(e.getMessage(), e);
		} finally {
			if (tempFile != null) {
				try {
					Files.delete(tempFile);
				} catch (IOException e) {
					throw new MashException(e.getMessage(), e);
				}
			}
		}
	}
	
	private final LineProcessor<MinHashDistance> PROC = new LineProcessor<MinHashDistance>() {

		@Override
		public MinHashDistance processline(final String line) {
			//TODO CODE nasty & brittle
			final String[] sl = line.trim().split("\\s+");
			final String id = sl[0].trim();
			final double distance = Double.parseDouble(sl[2].trim());
			return new MinHashDistance(id, distance);
		}
		
	};
	
	@Override
	public MinHashDistanceSet computeDistance(
			final MinHashSketchDatabase query,
			final MinHashSketchDatabase reference,
			final int maxReturnCount)
			throws MashException {
		checkNotNull(query, "query");
		checkNotNull(reference, "reference");
		if (query.getSequenceCount() != 1) {
			// may want to relax this, but that'll require changing a bunch of stuff
			throw new IllegalArgumentException("Only 1 query sequence is allowed");
		}
		query.checkCompatibility(reference);
		// may need to be smarter about this for really large collections
		List<MinHashDistance> dists = processMashOutput(PROC, false, "dist", "-d", "0.5",
					reference.getLocation().getPathToFile().get().toString(),
					query.getLocation().getPathToFile().get().toString());
		if (maxReturnCount > 0 && maxReturnCount <= dists.size()) {
			Collections.sort(dists);
			dists = dists.subList(0, maxReturnCount);
		}
		return new MinHashDistanceSet(query, reference, new HashSet<>(dists));
	}
	
	private static class ParamsAndSize {
		final MinHashParameters params;
		final int size;
		private ParamsAndSize(MinHashParameters params, int size) {
			this.params = params;
			this.size = size;
		}
	}
	
	private ParamsAndSize getParametersAndSize(final Path path) throws MashException {
		final String mashout = getMashOutput("info", "-H", path.toString());
		//TODO CODE this is nasty. Use a regex or something so we can have 2 problems
		final String[] lines = mashout.split("\n");
		final int kmerSize = Integer.parseInt(lines[2].trim().split("\\s+")[2].trim());
		final int sketchSize = Integer.parseInt(lines[4].trim().split("\\s+")[4].trim());
		final int size = Integer.parseInt(lines[5].trim().split("\\s+")[1].trim());
		return new ParamsAndSize(
				MinHashParameters.getBuilder(kmerSize)
						.withSketchSize(sketchSize)
						.build(),
				size);
	}

	public static void main(final String[] args) throws MinHashException {
		final MinHashImplementation mash = new Mash(Paths.get("."));
		System.out.println(mash.getImplementationInformation());
		
		final MinHashSketchDatabase db = mash.getDatabase(new MinHashDBLocation(Paths.get(
				"/home/crusherofheads/kb_refseq_sourmash/kb_refseq_ci_1000.msh")));
		System.out.println(db.getImplementationInformation());
		System.out.println(db.getLocation());
		System.out.println(db.getParameterSet());
		System.out.println(db.getSequenceCount());
		final List<String> ids = mash.getSketchIDs(db);
		System.out.println(ids.size());
		System.out.println(ids);
		
		final MinHashSketchDatabase query = mash.getDatabase(new MinHashDBLocation(Paths.get(
				"/home/crusherofheads/kb_refseq_sourmash/kb_refseq_ci_1000_15792_446_1.msh")));
		System.out.println(query.getImplementationInformation());
		System.out.println(query.getLocation());
		System.out.println(query.getParameterSet());
		System.out.println(query.getSequenceCount());
		System.out.println(mash.getSketchIDs(query));
		
		final MinHashDistanceSet dists = mash.computeDistance(query, db, 30);
		System.out.println(dists);
		System.out.println(dists.getDistances().size());
	}

}
