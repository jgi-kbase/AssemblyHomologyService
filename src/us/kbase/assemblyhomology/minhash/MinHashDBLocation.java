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

	private final Path pathToFile;
	
	/** Create a new location.
	 * @param pathToFile the path to the sketch database file.
	 * @throws IllegalArgumentException if the file does not exist.
	 */
	public MinHashDBLocation(final Path pathToFile) {
		checkNotNull(pathToFile, "pathToFile");
		if (!Files.exists(pathToFile)) {
			// since this path may be used in an exec make sure it's valid
			throw new IllegalArgumentException(pathToFile + " does not exist");
		}
		this.pathToFile = pathToFile;
	}

	/** Get the sketch database location. Currently will always return the path, but returns
	 * an {@link Optional} to allow for expansion in the future.
	 * @return the path.
	 */
	public Optional<Path> getPathToFile() {
		return Optional.of(pathToFile);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pathToFile == null) ? 0 : pathToFile.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MinHashDBLocation other = (MinHashDBLocation) obj;
		if (pathToFile == null) {
			if (other.pathToFile != null) {
				return false;
			}
		} else if (!pathToFile.equals(other.pathToFile)) {
			return false;
		}
		return true;
	}
}
