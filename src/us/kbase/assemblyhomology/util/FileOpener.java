package us.kbase.assemblyhomology.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** A trivial, but easily mockable, class for opening a file for reading. */
public class FileOpener {

	public FileOpener() {}
	
	public InputStream open(final Path file) throws IOException {
		return Files.newInputStream(file);
	}
}
