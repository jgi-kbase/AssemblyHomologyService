package us.kbase.assemblyhomology.minhash;

import static us.kbase.assemblyhomology.util.Util.exceptOnEmpty;

public class MinHashImplementationInformation {
	
	//TODO JAVADOC
	//TODO TEST
	
	private final String implementationName;
	private final String implementationVersion;
	
	public MinHashImplementationInformation(
			final String implementationName,
			final String implementationVersion) {
		exceptOnEmpty(implementationName, "implementationName");
		exceptOnEmpty(implementationVersion, "implementationVersion");
		this.implementationName = implementationName;
		this.implementationVersion = implementationVersion;
	}

	public String getImplementationName() {
		return implementationName;
	}

	public String getImplementationVersion() {
		return implementationVersion;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MinHashImplemenationInformation [implementationName=");
		builder.append(implementationName);
		builder.append(", implementationVersion=");
		builder.append(implementationVersion);
		builder.append("]");
		return builder.toString();
	}
}
