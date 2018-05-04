package us.kbase.assemblyhomology.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class PathRestreamable implements Restreamable {
	
	//TODO TEST
	//TODO JAVADOC

	private final Path input;
	private final FileOpener fileOpener;
	
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
