package us.kbase.assemblyhomology.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** A trivial, but easily mockable, class for opening a file for reading. */
public class FileOpener {
	
	// note - this class is not unit tested. If changes are made test manually.

	/** Create a file opener. */
	public FileOpener() {}
	
	/** Open a file. Delegates to {@link Files#newInputStream(Path, java.nio.file.OpenOption...)}
	 * with default options.
	 * @param file the file to open.
	 * @return a new input stream.
	 * @throws IOException if an IO error occurs.
	 */
	public InputStream open(final Path file) throws IOException {
		return Files.newInputStream(file);
	}
}
