package us.kbase.assemblyhomology.minhash;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.exceptOnEmpty;

import java.nio.file.Path;

import com.google.common.base.Optional;

public class MinHashImplementationInformation {
	
	//TODO JAVADOC
	//TODO TEST
	
	private final MinHashImplementationName implementationName;
	private final String implementationVersion;
	private final Optional<Path> expectedFileExtension;
	
	public MinHashImplementationInformation(
			final MinHashImplementationName implementationName,
			final String implementationVersion,
			final Path mashFileExt) {
		checkNotNull(implementationName, "implementationName");
		exceptOnEmpty(implementationVersion, "implementationVersion");
		checkNotNull(mashFileExt, "expectedFileExtension");
		this.implementationName = implementationName;
		this.implementationVersion = implementationVersion;
		this.expectedFileExtension = Optional.of(mashFileExt);
	}

	public MinHashImplementationName getImplementationName() {
		return implementationName;
	}

	public String getImplementationVersion() {
		return implementationVersion;
	}
	
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MinHashImplementationInformation [implementationName=");
		builder.append(implementationName);
		builder.append(", implementationVersion=");
		builder.append(implementationVersion);
		builder.append(", expectedFileExtension=");
		builder.append(expectedFileExtension);
		builder.append("]");
		return builder.toString();
	}
}
