package us.kbase.assemblyhomology.minhash.mash;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.checkNoNullsInCollection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashDistanceSet;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashImplementation;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.minhash.exceptions.IncompatibleSketchesException;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashInitException;
import us.kbase.assemblyhomology.minhash.exceptions.NotASketchException;
import us.kbase.assemblyhomology.util.CappedTreeSet;

/** A wrapper for the mash implementation of the MinHash algorithm. Expects the mash binary
 * to be available on the command line.
 * @author gaprice@lbl.gov
 *
 */
public class Mash implements MinHashImplementation {
	
	//TODO ZZLATER CODE consider JNA to bind directly to the mash libs? That would allow controlling the version of Mash.
	
	private final static MinHashImplementationName MASH;
	static {
		try {
			MASH = new MinHashImplementationName("mash");
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException("Well this is unexpected", e);
		}
	}
	private final static Path MASH_FILE_EXT = Paths.get("msh");
	
	/** Get the name of this implementation - in this case mash.
	 * @return the implementation name.
	 */
	public static MinHashImplementationName getImplementationName() {
		return MASH;
	}
	
	/** Get the file extension mash requires for input files.
	 * @return the expected file extension.
	 */
	public static Path getExpectedFileExtension() {
		return MASH_FILE_EXT;
	}
	
	private final MinHashImplementationInformation info;
	private final Path tempFileDirectory;
	
	/** Create a new mash wrapper.
	 * @param tempFileDirectory a directory in which temporary files may be stored.
	 * @throws MinHashInitException if the wrapper could not be initialized.
	 */
	public Mash(final Path tempFileDirectory) throws MinHashInitException {
		checkNotNull(tempFileDirectory, "tempFileDirectory");
		this.tempFileDirectory = tempFileDirectory;
		try {
			Files.createDirectories(tempFileDirectory);
		} catch (IOException e) {
			throw new MinHashInitException(
					"Couldn't create temporary directory: " + e.getMessage(), e);
		}
		info = getInfo();
	}
	
	/** Get the location the wrapper is using to store temporary files.
	 * @return the temporary file directory.
	 */
	public Path getTemporaryFileDirectory() {
		return tempFileDirectory;
	}

	private MinHashImplementationInformation getInfo() throws MinHashInitException {
		try {
			final String version = getVersion(getMashOutput("-h"));
			return new MinHashImplementationInformation(MASH, version, MASH_FILE_EXT);
		} catch (MinHashException e) {
			// don't know how to test this other than leaving mash off the system path
			// so test manually
			throw new MinHashInitException(e.getMessage(), e);
		}
	}
	
	// only use when expecting a small amount of output, otherwise will put entire output in mem
	// or more likely deadlock
	private String getMashOutput(final String... arguments) throws MinHashException {
		return getMashOutput(null, arguments);
	}

	// returns null if outputPath is not null
	private String getMashOutput(final Path outputPath, final String... arguments)
			throws MinHashException {
		final List<String> command = new LinkedList<>(Arrays.asList(MASH.getName()));
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
				// not sure how to test this
				throw new MinHashException(String.format(
						"Timed out waiting for %s to run", MASH.getName()));
			}
			if (mash.exitValue() != 0) {
				try (final InputStream is = mash.getErrorStream()) {
					throw handleMashException(IOUtils.toString(is));
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
			// this is also very difficult to test
			throw new MinHashException(String.format(
					"Error running %s: ", MASH.getName()) + e.getMessage(), e);
		}
	}

	private MinHashException handleMashException(String exceptionText)
			throws MinHashException {
		exceptionText = exceptionText.trim();
		// this is a little bit brittle.
		// the "does not look like a sketch" response must be handled at the point of input
		// there may be other responses though, can't test all possible inputs
		if (exceptionText.contains("terminate called")) {
			throw new NotASketchException(
					MASH.getName() + " could not read sketch", exceptionText);
		}
		// not sure how to test this.
		return new MinHashException(String.format(
				"Error running %s: %s", MASH.getName(), exceptionText));
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
	public MinHashSketchDatabase getDatabase(
			final MinHashSketchDBName dbname,
			final MinHashDBLocation location)
			throws MinHashException {
		checkNotNull(dbname, "dbname");
		checkNotNull(location, "location");
		checkFileExtension(location);
		final ParamsAndSize pns = getParametersAndSize(location.getPathToFile().get());
		return new MinHashSketchDatabase(
				dbname, info.getImplementationName(), pns.params, location, pns.size);
	}

	private void checkFileExtension(final MinHashDBLocation location) throws NotASketchException {
		final String locStr = location.getPathToFile().get().toString();
		if (!locStr.endsWith("." + MASH_FILE_EXT.toString())) {
			throw new NotASketchException(locStr);
		}
	}
	
	@Override
	public List<String> getSketchIDs(final MinHashSketchDatabase db) throws MinHashException {
		checkNotNull(db, "db");
		checkFileExtension(db.getLocation());
		final List<String> ids = new LinkedList<>();
		processMashOutput(
				//TODO CODE make less brittle
				l -> ids.add(l.split("\\s+")[2].trim()),
				true,
				"info", "-t", db.getLocation().getPathToFile().get().toString());
		return ids;
	}
	
	private interface LineCollector {
		void collect(String line);
	}
	
	// use for large output, creates a temp file
	private void processMashOutput(
			final LineCollector lineCollector,
			final boolean skipHeader,
			final String... command)
			throws MinHashException {
		Path tempFile = null;
		try {
			tempFile = Files.createTempFile(tempFileDirectory, "mash_output", ".tmp");
			getMashOutput(tempFile, command);
			try (final InputStream is = Files.newInputStream(tempFile)) {
				final BufferedReader br = new BufferedReader(new InputStreamReader(
						is, StandardCharsets.UTF_8));
				if (skipHeader) {
					br.readLine();
				}
				for (String line = br.readLine(); line != null; line = br.readLine()) {
					lineCollector.collect(line);
				}
			}
		// all of the below is really hard to test
		} catch (IOException e) {
			throw new MinHashException(e.getMessage(), e);
		} finally {
			if (tempFile != null) {
				try {
					Files.delete(tempFile);
				} catch (IOException e) {
					throw new MinHashException(e.getMessage(), e);
				}
			}
		}
	}
	
	private static class DistanceCollector implements LineCollector {
		
		private final CappedTreeSet<MinHashDistance> dists;
		private MinHashSketchDBName dbname;
		
		public DistanceCollector(final int size) {
			dists = new CappedTreeSet<>(size, true);
		}

		@Override
		public void collect(final String line) {
			//TODO CODE nasty & brittle
			final String[] sl = line.trim().split("\\s+");
			final String id = sl[0].trim();
			final double distance = Double.parseDouble(sl[2].trim());
			dists.add(new MinHashDistance(dbname, id, distance));
		}
	}
	
	@Override
	public MinHashDistanceSet computeDistance(
			final MinHashSketchDatabase query,
			final Collection<MinHashSketchDatabase> references,
			final int maxReturnCount,
			final boolean strict)
			throws MinHashException, IncompatibleSketchesException {
		checkNotNull(query, "query");
		checkNoNullsInCollection(references, "references");
		if (query.getSequenceCount() != 1) {
			// may want to relax this, but that'll require changing a bunch of stuff
			throw new IllegalArgumentException("Only 1 query sequence is allowed");
		}
		if (maxReturnCount < 1) {
			throw new IllegalArgumentException("maxReturnCount must be > 0");
		}
		final List<String> warnings = checkQueryable(query, references, strict);
		final DistanceCollector distanceProcessor = new DistanceCollector(maxReturnCount);
		for (final MinHashSketchDatabase ref: references) {
			distanceProcessor.dbname = ref.getName();
			processMashOutput(distanceProcessor, false, "dist", "-d", "0.5",
						ref.getLocation().getPathToFile().get().toString(),
						query.getLocation().getPathToFile().get().toString());
		}
		return new MinHashDistanceSet(distanceProcessor.dists.toTreeSet(), warnings);
	}
	
	private List<String> checkQueryable(
			final MinHashSketchDatabase query,
			final Collection<MinHashSketchDatabase> references,
			final boolean strict)
			throws IncompatibleSketchesException, NotASketchException {
		checkFileExtension(query.getLocation());
		final List<String> warnings = new LinkedList<>();
		for (final MinHashSketchDatabase db: references) {
			checkFileExtension(db.getLocation());
			// may want to change this to a class with more info about the source of the warning, but YAGNI
			warnings.addAll(db.checkIsQueriableBy(query, strict).stream()
					.map(s -> "Sketch DB " + db.getName().getName() + ": " + s)
					.collect(Collectors.toList()));
		}
		return warnings;
	}

	private static class ParamsAndSize {
		final MinHashParameters params;
		final int size;
		private ParamsAndSize(final MinHashParameters params, final int size) {
			this.params = params;
			this.size = size;
		}
	}
	
	private ParamsAndSize getParametersAndSize(final Path path) throws MinHashException {
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
}
