package us.kbase.test.assemblyhomology.minhash.mash;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import us.kbase.assemblyhomology.minhash.DefaultDistanceCollector;
import us.kbase.assemblyhomology.minhash.DefaultDistanceFilter;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashDistanceCollector;
import us.kbase.assemblyhomology.minhash.MinHashDistanceFilter;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.minhash.exceptions.IncompatibleSketchesException;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;
import us.kbase.assemblyhomology.minhash.exceptions.NotASketchException;
import us.kbase.assemblyhomology.minhash.mash.Mash;
import us.kbase.test.assemblyhomology.TestCommon;
import us.kbase.test.assemblyhomology.data.TestDataManager;

public class MashTest {
	
	private static Path TEMP_DIR;
	private static Path MASH_TEMP_DIR;
	private static Path EMPTY_FILE;
	private static Path EMPTY_FILE_MSH;
	private static final Path QUERY_K21_S1000 = Paths.get("kb_15792_446_1_k21_s1000.msh");
	private static final Path QUERY_K31_S1000 = Paths.get("kb_15792_446_1_k31_s1000.msh");
	private static final Path QUERY_K31_S500 = Paths.get("kb_15792_446_1_k31_s500.msh");
	private static final Path QUERY_K31_S1500 = Paths.get("kb_15792_446_1_k31_s1500.msh");
	private static final Path TARGET_4SEQS = Paths.get("kb_4seqs_k31_s1000.msh");
	private static final Path TARGET_4SEQS_2 = Paths.get("kb_4seqs_k31_s1000_2.msh");
	
	@BeforeClass
	public static void setUp() throws Exception {
		TEMP_DIR = TestCommon.getTempDir().resolve("MashTest_" + UUID.randomUUID().toString());
		MASH_TEMP_DIR = TEMP_DIR.resolve("mash");
		Files.createDirectories(MASH_TEMP_DIR);
		for (final Path f: Arrays.asList(QUERY_K21_S1000, QUERY_K31_S1000, QUERY_K31_S500,
				QUERY_K31_S1500, TARGET_4SEQS, TARGET_4SEQS_2)) {
			TestDataManager.install(f, TEMP_DIR.resolve(f));
		}
		EMPTY_FILE = TEMP_DIR.resolve(UUID.randomUUID().toString());
		Files.createFile(EMPTY_FILE);
		
		EMPTY_FILE_MSH = Paths.get(EMPTY_FILE.toString() + ".msh");
		Files.createFile(EMPTY_FILE_MSH);
	}
	
	@AfterClass
	public static void breakDown() throws Exception {
		final boolean deleteTempFiles = TestCommon.isDeleteTempFiles();
		if (TEMP_DIR != null && Files.exists(TEMP_DIR) && deleteTempFiles) {
			FileUtils.deleteQuietly(TEMP_DIR.toFile());
		}
	}
	
	@After
	public void ensureNoMashTempFiles() throws Exception {
		final List<Path> files = Files.list(MASH_TEMP_DIR).collect(Collectors.toList());
		assertThat("mash left temp files", files, is(Collections.emptyList()));
	}

	@Test
	public void staticMethods() throws Exception {
		assertThat("incorrect impl name", Mash.getImplementationName(),
				is(new MinHashImplementationName("mash")));
		assertThat("incorrect file ext", Mash.getExpectedFileExtension(), is(Paths.get("msh")));
	}
	
	@Test
	public void construct() throws Exception {
		final Mash m = new Mash(MASH_TEMP_DIR, 1);
		assertThat("incorrect tempDir", m.getTemporaryFileDirectory(), is(MASH_TEMP_DIR));
		assertThat("incorrect timeout", m.getMashTimeoutSec(), is(1));
		assertThat("incorrect impl info", m.getImplementationInformation(),
				is(new MinHashImplementationInformation(
						// might need to be smarter about the version
						new MinHashImplementationName("mash"), "2.0", Paths.get("msh"))));
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, 1, new NullPointerException("tempFileDirectory"));
		failConstruct(TEMP_DIR.resolve("temp_test"), 0, new IllegalArgumentException(
				"mashTimeout must be > 0"));
		final Path tempFile = TEMP_DIR.resolve("temp_file");
		try {
			Files.createFile(tempFile);
		} catch (FileAlreadyExistsException e) {
			// ignore
		}
		try {
			failConstruct(tempFile, 1, new MinHashException(
					"Couldn't create temporary directory: " + tempFile.toString()));
		} finally {
			Files.delete(tempFile);
		}
	}
	
	private void failConstruct(final Path tempDir, final int timeout, final Exception expected) {
		try {
			new Mash(tempDir, timeout);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getDatabase() throws Exception {
		final Mash m = new Mash(MASH_TEMP_DIR, 600);
		
		final MinHashSketchDatabase db = m.getDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashDBLocation(TEMP_DIR.resolve(QUERY_K21_S1000)));
		
		assertThat("incorrect db", db, is(new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(21).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(QUERY_K21_S1000)),
				1)));
		
		final MinHashSketchDatabase db2 = m.getDatabase(
				new MinHashSketchDBName("myname2"),
				new MinHashDBLocation(TEMP_DIR.resolve(TARGET_4SEQS)));
		
		assertThat("incorrect db", db2, is(new MinHashSketchDatabase(
				new MinHashSketchDBName("myname2"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(TARGET_4SEQS)),
				4)));
	}
	
	@Test
	public void getDataBaseFail() throws Exception {
		final MinHashSketchDBName name = new MinHashSketchDBName("bar");
		final MinHashDBLocation db = new MinHashDBLocation(TEMP_DIR.resolve(QUERY_K21_S1000));
		
		failGetDatabase(null, db, new NullPointerException("dbname"));
		failGetDatabase(name, null, new NullPointerException("location"));
		
		Exception got = failGetDatabase(name, new MinHashDBLocation(EMPTY_FILE),
				new NotASketchException(EMPTY_FILE.toString() + " is not a mash sketch"));
		
		assertThat("incorrect mash output",
				((NotASketchException) got).getMinHashErrorOutput().isPresent(), is(false));
		
		
		got = failGetDatabase(name, new MinHashDBLocation(EMPTY_FILE_MSH),
				new NotASketchException("mash could not read sketch"));
		
		assertThat("incorrect mash output",
				((NotASketchException) got).getMinHashErrorOutput().get(),
				containsString("terminate called"));
	}

	private Exception failGetDatabase(
			final MinHashSketchDBName name,
			final MinHashDBLocation db,
			final Exception expected) {
		try {
			new Mash(MASH_TEMP_DIR, 1).getDatabase(name, db);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
			return got;
		}
		return null;
	}
	
	@Test
	public void getSketchIDs() throws Exception {
		final Mash m = new Mash(MASH_TEMP_DIR, 5);
		
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(21).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(QUERY_K21_S1000)),
				1);
		
		assertThat("incorrect ids", m.getSketchIDs(db), is(Arrays.asList("15792_446_1")));
		
		final MinHashSketchDatabase db2 = new MinHashSketchDatabase(
				new MinHashSketchDBName("myname2"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(TARGET_4SEQS)),
				4);
		
		assertThat("incorrect ids", m.getSketchIDs(db2),
				is(Arrays.asList("15792_446_1", "15792_431_1", "15792_3029_1", "15792_341_2")));
	}
	
	@Test
	public void getSketchIDsFail() throws Exception {
		failGetSketchIDs(null, new NullPointerException("db"));
		
		final MinHashSketchDatabase mtdb = new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(21).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(EMPTY_FILE)),
				1);
		
		Exception got = failGetSketchIDs(
				mtdb, new NotASketchException(EMPTY_FILE.toString() + " is not a mash sketch"));
		
		assertThat("incorrect mash output",
				((NotASketchException) got).getMinHashErrorOutput().isPresent(), is(false));
		
		final MinHashSketchDatabase bad = new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(21).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(EMPTY_FILE_MSH)),
				1);
		
		got = failGetSketchIDs(bad, new NotASketchException("mash could not read sketch"));
		assertThat("incorrect mash output",
				((NotASketchException) got).getMinHashErrorOutput().get(),
				containsString("terminate called"));
	}

	private Exception failGetSketchIDs(final MinHashSketchDatabase db, final Exception expected) {
		try {
			new Mash(MASH_TEMP_DIR, 2000).getSketchIDs(db);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
			return got;
		}
		return null;
	}
	
	@Test
	public void computeDistanceSingleTarget() throws Exception {
		final MinHashSketchDatabase query = new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(QUERY_K31_S1000)),
				1);
		computeDistance(query, true, Collections.emptyList());
	}
	
	@Test
	public void computeDistanceSingleTargetNonStrict() throws Exception {
		final MinHashSketchDatabase query = new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1500).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(QUERY_K31_S1500)),
				1);
		computeDistance(query, false, Arrays.asList("Sketch DB myname2: Query sketch size 1500 " +
				"is larger than target sketch size 1000"));
	}

	private void computeDistance(
			final MinHashSketchDatabase query,
			final boolean strict,
			final List<String> warnings)
			throws Exception {
		final MinHashSketchDBName targName = new MinHashSketchDBName("myname2");
		final MinHashDistance dist1 = new MinHashDistance(targName, "15792_446_1", 0);
		final MinHashDistance dist2 = new MinHashDistance(targName, "15792_431_1", 0.00236402);
		final MinHashDistance dist3 = new MinHashDistance(targName, "15792_341_2", 0.00921302);
		
		TreeSet<MinHashDistance> expected = new TreeSet<>(Arrays.asList(dist1, dist2, dist3));

		computeDistance(targName, query, 100, strict, expected, warnings);
		computeDistance(targName, query, 3, strict, expected, warnings);
		
		expected = new TreeSet<>(Arrays.asList( dist1, dist2));
		
		computeDistance(targName, query, 2, strict, expected, warnings);
		
		expected = new TreeSet<>(Arrays.asList(dist1));
		
		computeDistance(targName, query, 1, strict, expected, warnings);
	}
	
	private void computeDistance(
			final MinHashSketchDBName targName,
			final MinHashSketchDatabase query,
			final int maxReturnCount,
			final boolean strict,
			final TreeSet<MinHashDistance> expected,
			final List<String> expectedWarnings)
			throws Exception {
		final Mash m = new Mash(MASH_TEMP_DIR, 7);
		
		final MinHashSketchDatabase target1 = new MinHashSketchDatabase(
				targName,
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(TARGET_4SEQS)),
				4);
		
		final MinHashDistanceCollector col = new DefaultDistanceCollector(maxReturnCount);
		final List<String> warn = m.computeDistance(
				query, ImmutableMap.of(target1, new DefaultDistanceFilter(col)), strict);
		
		assertThat("incorrect distances", col.getDistances(), is(expected));
		assertThat("incorrect warnings", warn, is(expectedWarnings));
	}
	
	@Test
	public void computeDistanceTwoTargets() throws Exception {
		final MinHashSketchDatabase query = new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(QUERY_K31_S1000)),
				1);
		
		computeDistanceTwoTargets(query, true, Collections.emptyList());
	}
	
	@Test
	public void computeDistanceTwoTargetsNonStrict() throws Exception {
		final MinHashSketchDatabase query = new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1500).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(QUERY_K31_S1500)),
				1);
		
		computeDistanceTwoTargets(query, false, Arrays.asList(
				"Sketch DB myname2: Query sketch size 1500 is larger than target sketch " +
				"size 1000",
				"Sketch DB myname4: Query sketch size 1500 is larger than target sketch " +
				"size 1000"));
	}
	
	private void computeDistanceTwoTargets(
			final MinHashSketchDatabase query,
			final boolean strict,
			final List<String> warnings)
			throws Exception {
		final MinHashSketchDBName targName1 = new MinHashSketchDBName("myname2");
		final MinHashSketchDBName targName2 = new MinHashSketchDBName("myname4");
		final MinHashDistance dist1_1 = new MinHashDistance(targName1, "15792_446_1", 0);
		final MinHashDistance dist1_2 = new MinHashDistance(targName1, "15792_431_1", 0.00236402);
		final MinHashDistance dist1_3 = new MinHashDistance(targName1, "15792_341_2", 0.00921302);
		final MinHashDistance dist2_1 = new MinHashDistance(targName2, "15792_326_2", 0.00664804);
		final MinHashDistance dist2_2 = new MinHashDistance(targName2, "15792_467_1", 0.00673197);
		final MinHashDistance dist2_3 = new MinHashDistance(targName2, "15792_314_2", 0.00917961);
		
		TreeSet<MinHashDistance> expected = new TreeSet<>(Arrays.asList(
				dist1_1, dist1_2, dist2_1, dist2_2, dist2_3, dist1_3));
		
		computeDistanceTwoTargets(targName1, targName2, query, 100, strict, expected, warnings);
		computeDistanceTwoTargets(targName1, targName2, query, 6, strict, expected, warnings);
		
		expected = new TreeSet<>(Arrays.asList(dist1_1, dist1_2, dist2_1));
		
		computeDistanceTwoTargets(targName1, targName2, query, 3, strict, expected, warnings);
		
		expected = new TreeSet<>(Arrays.asList(dist1_1));
		
		computeDistanceTwoTargets(targName1, targName2, query, 1, strict, expected, warnings);
	}
	
	private class BufferedDistanceFilter implements MinHashDistanceFilter {

		private final List<MinHashDistance> buffer = new LinkedList<>();
		private final MinHashDistanceCollector collector;
		
		public BufferedDistanceFilter(final MinHashDistanceCollector collector) {
			this.collector = collector;
		}
		
		@Override
		public void accept(final MinHashDistance dist) {
			buffer.add(dist);
		}

		@Override
		public void flush() {
			for (final MinHashDistance d: buffer) {
				collector.accept(d);
			}
			buffer.clear();
		}
		
	}
	
	private void computeDistanceTwoTargets(
			final MinHashSketchDBName targName1,
			final MinHashSketchDBName targName2,
			final MinHashSketchDatabase query,
			final int maxReturnCount,
			final boolean strict,
			final TreeSet<MinHashDistance> expected,
			final List<String> expectedWarnings)
			throws Exception {
		/* note that this also tests a buffered filter where the flush() call is required for
		 * the filter to function. For the default filter the flush call does nothing.
		 */
		final Mash m = new Mash(MASH_TEMP_DIR, 30);
		
		final MinHashSketchDatabase target1 = new MinHashSketchDatabase(
				targName1,
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(TARGET_4SEQS)),
				4);
		
		final MinHashSketchDatabase target2 = new MinHashSketchDatabase(
				targName2,
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(TARGET_4SEQS_2)),
				4);
		
		final MinHashDistanceCollector col = new DefaultDistanceCollector(maxReturnCount);
		final List<String> dist = m.computeDistance(query,
				ImmutableMap.of(
						target1, new DefaultDistanceFilter(col),
						target2, new BufferedDistanceFilter(col)),
				strict);
		
		assertThat("incorrect distances", col.getDistances(), is(expected));
		assertThat("incorrect warnings", dist, is(expectedWarnings));
	}
	
	@Test
	public void computeDistanceFailBasicInputs() throws Exception {
		final MinHashSketchDatabase query = new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(QUERY_K31_S1000)),
				1);
		
		final MinHashSketchDatabase target = new MinHashSketchDatabase(
				new MinHashSketchDBName("target"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(TARGET_4SEQS)),
				4);
		final DefaultDistanceFilter filter = new DefaultDistanceFilter(
				new DefaultDistanceCollector(1));
		final Map<MinHashSketchDatabase, MinHashDistanceFilter> targets = ImmutableMap.of(
				target, filter);
		
		final Map<MinHashSketchDatabase, MinHashDistanceFilter> nullKey = new HashMap<>();
		nullKey.put(null, filter);
		final Map<MinHashSketchDatabase, MinHashDistanceFilter> nullVal = new HashMap<>();
		nullVal.put(target, null);
		
		
		failComputeDistance(null, targets, false, new NullPointerException("query"));
		failComputeDistance(query, null, false, new NullPointerException("references"));
		failComputeDistance(query, nullKey, false,
				new NullPointerException("Null key in references map"));
		failComputeDistance(query, nullVal, false,
				new NullPointerException("Null value in references map"));
		failComputeDistance(target, targets, false, new IllegalArgumentException(
				"Only 1 query sequence is allowed"));
	}
	
	@Test
	public void computeDistanceFailMismatchedSketchDbs() throws Exception {
		final MinHashSketchDatabase target = new MinHashSketchDatabase(
				new MinHashSketchDBName("target"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(TARGET_4SEQS)),
				4);
		final Map<MinHashSketchDatabase, MinHashDistanceFilter> targets = ImmutableMap.of(
				target, new DefaultDistanceFilter(new DefaultDistanceCollector(1)));

		
		final MinHashSketchDatabase strict = new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1500).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(QUERY_K31_S1500)),
				1);
		
		failComputeDistance(strict, targets, true,
				new IncompatibleSketchesException(
						"Query sketch size 1500 does not match target 1000"));
		
		final MinHashSketchDatabase kmer = new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(21).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(QUERY_K21_S1000)),
				1);
		
		failComputeDistance(kmer, targets, false,
				new IncompatibleSketchesException(
						"Kmer size for sketches are not compatible: 31 21"));
		
		final MinHashSketchDatabase small = new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(500).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(QUERY_K31_S500)),
				1);
		
		failComputeDistance(small, targets, false,
				new IncompatibleSketchesException(
						"Query sketch size 500 may not be smaller than the target sketch " +
						"size 1000"));
	}
	
	
	@Test
	public void computeDistanceFailBadFiles() throws Exception {
		
		final MinHashSketchDatabase query = new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(QUERY_K31_S1000)),
				1);
		
		final MinHashSketchDatabase target = new MinHashSketchDatabase(
				new MinHashSketchDBName("target"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(TARGET_4SEQS)),
				4);
		
		final MinHashSketchDatabase extension = new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(EMPTY_FILE)),
				1);
		
		final MinHashDistanceCollector c = new DefaultDistanceCollector(1);
		final MinHashDistanceFilter f = new DefaultDistanceFilter(c);
		
		NotASketchException got = (NotASketchException) failComputeDistance(
				extension, ImmutableMap.of(target, f), false, new NotASketchException(
						EMPTY_FILE.toString() + " is not a mash sketch"));
		
		assertThat("incorrect mash output",
				((NotASketchException) got).getMinHashErrorOutput().isPresent(), is(false));
		
		got = (NotASketchException) failComputeDistance(
				query, ImmutableMap.of(target, f, extension, f), false, new NotASketchException(
						EMPTY_FILE.toString() + " is not a mash sketch"));
		
		assertThat("incorrect mash output",
				((NotASketchException) got).getMinHashErrorOutput().isPresent(), is(false));
		
		final MinHashSketchDatabase emptyFile = new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(TEMP_DIR.resolve(EMPTY_FILE_MSH)),
				1);
		
		got = (NotASketchException) failComputeDistance(
				emptyFile, ImmutableMap.of(target, f), false, new NotASketchException(
						"mash could not read sketch"));
		assertThat("incorrect mash output",
				((NotASketchException) got).getMinHashErrorOutput().get(),
				containsString("terminate called"));
		
		got = (NotASketchException) failComputeDistance(
				query, ImmutableMap.of(target, f, emptyFile, f), false, new NotASketchException(
						"mash could not read sketch"));
		assertThat("incorrect mash output",
				((NotASketchException) got).getMinHashErrorOutput().get(),
				containsString("terminate called"));
	}
	
	private Exception failComputeDistance(
			final MinHashSketchDatabase query,
			final Map<MinHashSketchDatabase, MinHashDistanceFilter> references,
			final boolean strict,
			final Exception expected) {
		try {
			new Mash(MASH_TEMP_DIR, 60).computeDistance(query, references, strict);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
			return got;
		}
		return null;
	}
}
