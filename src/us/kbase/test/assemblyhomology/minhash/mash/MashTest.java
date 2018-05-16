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
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;
import us.kbase.assemblyhomology.minhash.exceptions.NotASketchException;
import us.kbase.assemblyhomology.minhash.mash.Mash;
import us.kbase.test.assemblyhomology.TestCommon;
import us.kbase.test.assemblyhomology.data.TestDataInstaller;

public class MashTest {
	
	private static Path tempDir;
	private static final Path QUERY_K21_S1000 = Paths.get("kb_15792_446_1_k21_s1000.msh");
	private static final Path QUERY_K31_S1000 = Paths.get("kb_15792_446_1_k31_s1000.msh");
	private static final Path QUERY_K31_S500 = Paths.get("kb_15792_446_1_k31_s500.msh");
	private static final Path QUERY_K31_S1500 = Paths.get("kb_15792_446_1_k31_s1500.msh");
	private static final Path TARGET_4SEQS = Paths.get("kb_4seqs_k31_s1000.msh");
	
	@BeforeClass
	public static void setUp() throws Exception {
		tempDir = TestCommon.getTempDir().resolve("MashTest_" + UUID.randomUUID().toString());
		Files.createDirectories(tempDir);
		for (final Path f: Arrays.asList(
				QUERY_K21_S1000, QUERY_K31_S1000, QUERY_K31_S500, QUERY_K31_S1500, TARGET_4SEQS)) {
			TestDataInstaller.install(f, tempDir.resolve(f));
		}
	}
	
	@AfterClass
	public static void breakDown() throws Exception {
		final boolean deleteTempFiles = TestCommon.isDeleteTempFiles();
		if (tempDir != null && Files.exists(tempDir) && deleteTempFiles) {
			FileUtils.deleteQuietly(tempDir.toFile());
		}
	}

	@Test
	public void staticMethods() throws Exception {
		assertThat("incorrect impl name", Mash.getImplementationName(),
				is(new MinHashImplementationName("mash")));
		assertThat("incorrect file ext", Mash.getExpectedFileExtension(), is(Paths.get("msh")));
	}
	
	@Test
	public void construct() throws Exception {
		final Mash m = new Mash(tempDir);
		assertThat("incorrect tempDir", m.getTemporaryFileDirectory(), is(tempDir));
		assertThat("incorrect impl info", m.getImplementationInformation(),
				is(new MinHashImplementationInformation(
						// might need to be smarter about the version
						new MinHashImplementationName("mash"), "2.0", Paths.get("msh"))));
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, new NullPointerException("tempFileDirectory"));
		final Path tempFile = tempDir.resolve("temp_file");
		try {
			Files.createFile(tempFile);
		} catch (FileAlreadyExistsException e) {
			// ignore
		}
		try {
			failConstruct(tempFile, new MinHashException("Couldn't create temporary directory: " +
					tempFile.toString()));
		} finally {
			Files.delete(tempFile);
		}
	}
	
	private void failConstruct(final Path tempDir, final Exception expected) {
		try {
			new Mash(tempDir);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getDatabase() throws Exception {
		final Mash m = new Mash(tempDir);
		
		final MinHashSketchDatabase db = m.getDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashDBLocation(tempDir.resolve(QUERY_K21_S1000)));
		
		assertThat("incorrect db", db, is(new MinHashSketchDatabase(
				new MinHashSketchDBName("myname"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(21).withSketchSize(1000).build(),
				new MinHashDBLocation(tempDir.resolve(QUERY_K21_S1000)),
				1)));
		
		final MinHashSketchDatabase db2 = m.getDatabase(
				new MinHashSketchDBName("myname2"),
				new MinHashDBLocation(tempDir.resolve(TARGET_4SEQS)));
		
		assertThat("incorrect db", db2, is(new MinHashSketchDatabase(
				new MinHashSketchDBName("myname2"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(tempDir.resolve(TARGET_4SEQS)),
				4)));
	}
	
	@Test
	public void getDataBaseFail() throws Exception {
		final MinHashSketchDBName name = new MinHashSketchDBName("bar");
		final MinHashDBLocation db = new MinHashDBLocation(tempDir.resolve(QUERY_K21_S1000));
		
		failGetDatabase(null, db, new NullPointerException("dbname"));
		failGetDatabase(name, null, new NullPointerException("location"));
		
		final Path emptyFile = tempDir.resolve(UUID.randomUUID().toString());
		Files.createFile(emptyFile);
		Exception got = failGetDatabase(name, new MinHashDBLocation(emptyFile),
				new NotASketchException(emptyFile.toString()));
		
		assertThat("incorrect mash output",
				((NotASketchException) got).getMinHashErrorOutput().isPresent(), is(false));
		
		final Path mashEmptyFile = Paths.get(emptyFile.toString() + ".msh");
		Files.createFile(mashEmptyFile);
		
		got = failGetDatabase(name, new MinHashDBLocation(mashEmptyFile),
				new NotASketchException("mash could not read sketch"));
		assertThat("incorrect mash output", ((NotASketchException) got).getMinHashErrorOutput().get(),
				containsString("terminate called"));
	}

	private Exception failGetDatabase(
			final MinHashSketchDBName name,
			final MinHashDBLocation db,
			final Exception expected) {
		try {
			new Mash(tempDir).getDatabase(name, db);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
			return got;
		}
		return null;
	}
	
}
