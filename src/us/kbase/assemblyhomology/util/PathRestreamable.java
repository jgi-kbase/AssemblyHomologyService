package us.kbase.assemblyhomology.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/** Produces multiple input streams from the same {@link Path} source. Each new stream is
 * produced from the source as it currently exists.
 * @author gaprice@lbl.gov
 *
 */
public class PathRestreamable implements Restreamable {
	
	private final Path input;
	private final FileOpener fileOpener;
	
	/** Create a new restreamable from a {@link Path} source.
	 * @param input the source of the stream.
	 * @param fileOpener an opener for files.
	 */
	public PathRestreamable(final Path input, final FileOpener fileOpener) {
		checkNotNull(fileOpener, "fileOpener");
		checkNotNull(input, "input");
		this.input = input;
		this.fileOpener = fileOpener;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return fileOpener.open(input);
	}

	@Override
	public String getSourceInfo() {
		return input.toString();
	}

}
