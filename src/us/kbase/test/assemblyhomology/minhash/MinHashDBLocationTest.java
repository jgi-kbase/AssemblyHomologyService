package us.kbase.test.assemblyhomology.minhash;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Optional;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.test.assemblyhomology.TestCommon;

public class MinHashDBLocationTest {
	
	private static Path TEMP_DIR;
	private static Path EMPTY_FILE;
	
	@BeforeClass
	public static void setUp() throws Exception {
		TEMP_DIR = TestCommon.getTempDir().resolve("MashTest_" + UUID.randomUUID().toString());
		Files.createDirectories(TEMP_DIR);
		
		EMPTY_FILE = TEMP_DIR.resolve(UUID.randomUUID().toString());
		Files.createFile(EMPTY_FILE);
	}
	
	@AfterClass
	public static void breakDown() throws Exception {
		final boolean deleteTempFiles = TestCommon.isDeleteTempFiles();
		if (TEMP_DIR != null && Files.exists(TEMP_DIR) && deleteTempFiles) {
			FileUtils.deleteQuietly(TEMP_DIR.toFile());
		}
	}

	@Test
	public void equals() {
		EqualsVerifier.forClass(MinHashDBLocation.class).usingGetClass().verify();
	}
	
	@Test
	public void construct() {
		final MinHashDBLocation loc = new MinHashDBLocation(EMPTY_FILE);
		
		assertThat("incorrect path", loc.getPathToFile(), is(Optional.of(EMPTY_FILE)));
	}
	
	@Test
	public void constructFail() {
		failConstruct(null, new NullPointerException("pathToFile"));
		final Path noExist = TEMP_DIR.resolve(UUID.randomUUID().toString());
		failConstruct(noExist, new IllegalArgumentException(noExist + " does not exist"));
	}
	
	private void failConstruct(final Path loc, final Exception expected) {
		try {
			new MinHashDBLocation(loc);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}

