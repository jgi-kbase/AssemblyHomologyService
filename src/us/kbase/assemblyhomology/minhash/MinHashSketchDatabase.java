package us.kbase.assemblyhomology.minhash;

import static com.google.common.base.Preconditions.checkNotNull;

import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;
import us.kbase.assemblyhomology.minhash.MinHashParameters;

public class MinHashSketchDatabase {
	
	//TODO TEST
	//TODO JAVADOC

	private final MinHashImplementationInformation info;
	private final MinHashParameters parameterSet;
	private final MinHashDBLocation location;
	private final int sequenceCount;
	
	public MinHashSketchDatabase(
			final MinHashImplementationInformation info,
			final MinHashParameters parameterSet,
			final MinHashDBLocation location,
			final int sequenceCount) {
		checkNotNull(info, "info");
		checkNotNull(parameterSet, "parameterSet");
		checkNotNull(location, "location");
		if (sequenceCount < 1) {
			throw new IllegalArgumentException("sequenceCount must be at least 1");
		}
		this.info = info;
		this.parameterSet = parameterSet;
		this.location = location;
		this.sequenceCount = sequenceCount;
	}

	public MinHashImplementationInformation getImplementationInformation() {
		return info;
	}
	
	public MinHashParameters getParameterSet() {
		return parameterSet;
	}

	public MinHashDBLocation getLocation() {
		return location;
	}

	public int getSequenceCount() {
		return sequenceCount;
	}
}
