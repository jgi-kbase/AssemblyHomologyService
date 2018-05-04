package us.kbase.assemblyhomology.minhash;

import static com.google.common.base.Preconditions.checkNotNull;

import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashParameters;

public class MinHashSketchDatabase {
	
	//TODO TEST
	//TODO JAVADOC

	private final MinHashImplementationName minHashImplementationName;
	private final MinHashParameters parameterSet;
	private final MinHashDBLocation location;
	private final int sequenceCount;
	
	public MinHashSketchDatabase(
			final MinHashImplementationName minHashImplName,
			final MinHashParameters parameterSet,
			final MinHashDBLocation location,
			final int sequenceCount) {
		checkNotNull(minHashImplName, "minHashImplName");
		checkNotNull(parameterSet, "parameterSet");
		checkNotNull(location, "location");
		if (sequenceCount < 1) {
			throw new IllegalArgumentException("sequenceCount must be at least 1");
		}
		this.minHashImplementationName = minHashImplName;
		this.parameterSet = parameterSet;
		this.location = location;
		this.sequenceCount = sequenceCount;
	}

	public MinHashImplementationName getImplementationName() {
		return minHashImplementationName;
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
	
	public void checkCompatibility(final MinHashSketchDatabase otherDB) {
		if (!getImplementationName().equals(otherDB.getImplementationName())) {
			// need to check version?
			throw new IllegalArgumentException(String.format(
					"Implementations for databases do not match: {} {}",
					getImplementationName(), otherDB.getImplementationName()));
		}
		if (!getParameterSet().equals(otherDB.getParameterSet())) {
			/* is this check necessary? what happens if you run with differing
			 * sketch sizes?
			 * for mash:
			 *		if the kmer size is different it skips the file
			 * 		if the query hash count is < reference it skips the file
			 *		if the query hash count is > reference, it warns and reduces the overall
			 *		hash count
			 *		either way we shouldn't allow it (maybe with explicit permission for the
			 *		lower hash count, but then need to ignore the warning in the output)
			 */
			throw new IllegalArgumentException(
					"Parameter sets for databases do not match");
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MinHashSketchDatabase [minHashImplementationName=");
		builder.append(minHashImplementationName);
		builder.append(", parameterSet=");
		builder.append(parameterSet);
		builder.append(", location=");
		builder.append(location);
		builder.append(", sequenceCount=");
		builder.append(sequenceCount);
		builder.append("]");
		return builder.toString();
	}
}
