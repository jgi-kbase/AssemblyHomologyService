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
