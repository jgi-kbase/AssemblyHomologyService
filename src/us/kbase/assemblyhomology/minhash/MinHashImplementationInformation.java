package us.kbase.assemblyhomology.minhash;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.exceptOnEmpty;

import java.nio.file.Path;

import com.google.common.base.Optional;

/** Information about an implementation of the MinHash algorithm - the name of the implementation,
 * the version of the implementation, and the file extension, if any, that the implementation
 * expects.
 * @author gaprice@lbl.gov
 *
 */
public class MinHashImplementationInformation {
	
	private final MinHashImplementationName implementationName;
	private final String implementationVersion;
	private final Optional<Path> expectedFileExtension;
	
	/** Create the implementation information.
	 * @param implementationName the name of the implementation.
	 * @param implementationVersion the version of the implementation.
	 * @param expectedFileExt the file extension expected by the implementation.
	 * Null is acceptable.
	 */
	public MinHashImplementationInformation(
			final MinHashImplementationName implementationName,
			final String implementationVersion,
			final Path expectedFileExt) {
		checkNotNull(implementationName, "implementationName");
		exceptOnEmpty(implementationVersion, "implementationVersion");
		this.implementationName = implementationName;
		this.implementationVersion = implementationVersion;
		this.expectedFileExtension = Optional.fromNullable(expectedFileExt);
	}

	/** Get the implementation name.
	 * @return the implementation name.
	 */
	public MinHashImplementationName getImplementationName() {
		return implementationName;
	}

	/** Get the version of the implementation.
	 * @return the implementation version.
	 */
	public String getImplementationVersion() {
		return implementationVersion;
	}
	
	/** Get the expected file extension for the implementation if one exists.
	 * @return the expected file extension or {@link Optional#absent()}.
	 */
	public Optional<Path> getExpectedFileExtension() {
		return expectedFileExtension;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((expectedFileExtension == null) ? 0 : expectedFileExtension.hashCode());
		result = prime * result + ((implementationName == null) ? 0 : implementationName.hashCode());
		result = prime * result + ((implementationVersion == null) ? 0 : implementationVersion.hashCode());
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
		MinHashImplementationInformation other = (MinHashImplementationInformation) obj;
		if (expectedFileExtension == null) {
			if (other.expectedFileExtension != null) {
				return false;
			}
		} else if (!expectedFileExtension.equals(other.expectedFileExtension)) {
			return false;
		}
		if (implementationName == null) {
			if (other.implementationName != null) {
				return false;
			}
		} else if (!implementationName.equals(other.implementationName)) {
			return false;
		}
		if (implementationVersion == null) {
			if (other.implementationVersion != null) {
				return false;
			}
		} else if (!implementationVersion.equals(other.implementationVersion)) {
			return false;
		}
		return true;
	}
}
