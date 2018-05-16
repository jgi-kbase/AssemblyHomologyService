package us.kbase.test.assemblyhomology.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import us.kbase.common.test.TestException;

public class TestDataInstaller {

	public static void install(final Path filename, final Path target) throws IOException {
		final InputStream is = TestDataInstaller.class.getResourceAsStream(filename.toString());
		if (is == null) {
			throw new TestException("Can't open file " + filename);
		} else {
			Files.copy(is, target);
		}
	}

}
