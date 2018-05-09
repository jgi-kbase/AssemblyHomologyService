package us.kbase.assemblyhomology.minhash;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedList;
import java.util.List;

import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;

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
	
	/** Check if this database may be queried by another sketch database.
	 * @param query the query database.
	 * @param strict throw an exception for any mismatch in sketch database parameters if true. If
	 * false, parameter differences that don't preclude a query will not throw an exception but
	 * will return warnings. Some parameter differences (e.g. kmer size) will always cause an
	 * exception to be thrown.
	 * @return warnings regarding database parameter mismatches, if any.
	 * @throws MinHashException if the database parameters don't match.
	 */
	public List<String> checkIsQueriableBy(final MinHashSketchDatabase query, final boolean strict)
			throws MinHashException {
		final List<String> warnings = new LinkedList<>();
		if (!getImplementationName().equals(query.getImplementationName())) {
			// need to check version?
			throw new MinHashException(String.format(
					"Implementations for sketches do not match: %s %s",
					getImplementationName(), query.getImplementationName()));
		}
		final MinHashParameters rp = getParameterSet();
		final MinHashParameters qp = query.getParameterSet();
		//TODO FEATURE allow multiple kmer sizes
		if (rp.getKmerSize() != qp.getKmerSize()) {
			throw new MinHashException(String.format(
					"Kmer size for sketches are not compatible: %s %s",
					rp.getKmerSize(), qp.getKmerSize()));
		}
		if (rp.getScaling().isPresent() ^ qp.getScaling().isPresent()) { // xor
			throw new MinHashException(
					"Both sketches must use either absolute sketch counts or scaling");
		}
		if (rp.getScaling().isPresent()) {
			// may need to adjust this when we have an implementation that supports scaling
			if (!rp.getScaling().get().equals(qp.getScaling().get())) {
				throw new MinHashException(String.format(
						"Scaling paramters for sketches are not compatible: %s %s",
						rp.getScaling().get(), qp.getScaling().get()));
			}
		} else {
			if (!rp.getSketchSize().get().equals(qp.getSketchSize().get())) {
				if (strict) {
					throw new MinHashException(String.format(
							"Query sketch size %s does not match target %s",
							qp.getSketchSize().get(), rp.getSketchSize().get()));
				}
				if (qp.getSketchSize().get() > rp.getSketchSize().get()) {
					warnings.add(String.format(
							"Query sketch size %s is larger than target sketch size %s",
							qp.getSketchSize().get(), rp.getSketchSize().get()));
				} else {
					throw new MinHashException(String.format(
							"Query sketch size %s may not be smaller than the target sketch " +
							"size %s",
							qp.getSketchSize().get(), rp.getSketchSize().get()));
				}
			}
		}
		/*
		 * when you run mash:
		 *		if the kmer size is different it skips the file
		 * 		if the query hash count is < reference it skips the file
		 *		if the query hash count is > reference, it warns and reduces the overall
		 *		hash count
		 *		either way we shouldn't allow it (except with explicit permission for the
		 *		reduced hash count)
		 */
		return warnings;
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
