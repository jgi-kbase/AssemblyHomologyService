package us.kbase.assemblyhomology.minhash;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.base.Optional;

/** The location of a MinHash database. Currently only supports a Path based location, but
 * returns an optional to allow specifying other locations in the future.
 * @author gaprice@lbl.gov
 *
 */
public class MinHashDBLocation {

	//TODO JAVADOC
	//TODO TEST
	
	private final Path pathToFile;
	
	public MinHashDBLocation(final Path pathToFile) {
		checkNotNull(pathToFile, "pathToFile");
		if (!Files.exists(pathToFile)) {
			// since this path may be used in an exec make sure it's valid
			throw new IllegalArgumentException(pathToFile + " does not exist");
		}
		this.pathToFile = pathToFile;
	}

	public Optional<Path> getPathToFile() {
		return Optional.of(pathToFile);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MinHashDBLocation [pathToFile=");
		builder.append(pathToFile);
		builder.append("]");
		return builder.toString();
	}
	
}
