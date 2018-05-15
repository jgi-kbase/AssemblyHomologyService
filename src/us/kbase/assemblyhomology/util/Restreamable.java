package us.kbase.assemblyhomology.util;

import java.io.IOException;
import java.io.InputStream;

/** A source of input streams that can be streamed multiple times, as opposed to a general input
 * stream, which is exhausted when used. Each invocation of {@link #getInputStream()} produces
 * a new input stream from the stream source.
 * @author gaprice@lbl.gov
 *
 */
public interface Restreamable {

	/** Generate an input stream from the source data.
	 * @return the input stream.
	 * @throws IOException if an IOException occurs getting the stream.
	 */
	InputStream getInputStream() throws IOException;
	
	/** Get information about the source of a stream. Typically a file name. */
	String getSourceInfo();
	
}
