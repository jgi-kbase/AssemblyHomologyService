package us.kbase.assemblyhomology.minhash;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.exceptOnEmpty;

public class MinHashImplementationInformation {
	
	//TODO JAVADOC
	//TODO TEST
	
	private final MinHashImplementationName implementationName;
	private final String implementationVersion;
	
	public MinHashImplementationInformation(
			final MinHashImplementationName implementationName,
			final String implementationVersion) {
		checkNotNull(implementationName, "implementationName");
		exceptOnEmpty(implementationVersion, "implementationVersion");
		this.implementationName = implementationName;
		this.implementationVersion = implementationVersion;
	}

	public MinHashImplementationName getImplementationName() {
		return implementationName;
	}

	public String getImplementationVersion() {
		return implementationVersion;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MinHashImplementationInformation [implementationName=");
		builder.append(implementationName);
		builder.append(", implementationVersion=");
		builder.append(implementationVersion);
		builder.append("]");
		return builder.toString();
	}
}
