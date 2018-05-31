package us.kbase.test.assemblyhomology.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;

import us.kbase.common.test.TestException;

public class TestDataManager {

	public static void install(final Path filename, final Path target) throws IOException {
		try (final InputStream is = getStream(filename)) {
			Files.copy(is, target);
		}
	}

	private static InputStream getStream(final Path filename) {
		final InputStream is = TestDataManager.class.getResourceAsStream(filename.toString());
		if (is == null) {
			throw new TestException("Can't open file " + filename);
		}
		return is;
	}
	
	public static String get(final Path filename) throws IOException {
		try (final InputStream is = getStream(filename)) {
			return IOUtils.toString(is);
		}
	}

}
