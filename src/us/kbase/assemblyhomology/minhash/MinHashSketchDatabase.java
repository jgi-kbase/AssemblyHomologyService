package us.kbase.assemblyhomology.minhash;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedList;
import java.util.List;

import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.exceptions.IncompatibleSketchesException;

/** A database of MinHash sketches.
 * @author gaprice@lbl.gov
 *
 */
public class MinHashSketchDatabase {
	
	private final MinHashSketchDBName name;
	private final MinHashImplementationName minHashImplementationName;
	private final MinHashParameters parameterSet;
	private final MinHashDBLocation location;
	private final int sequenceCount;
	
	// a builder would be nice. Everything's required though.
	/** Create a sketch database.
	 * @param dbname the name of the database.
	 * @param minHashImplName the name of the implementation that created the database.
	 * @param parameterSet the parameter set used to create the database.
	 * @param location the location of the database.
	 * @param sequenceCount the number of sequences / sketches in the database.
	 */
	public MinHashSketchDatabase(
			final MinHashSketchDBName dbname,
			final MinHashImplementationName minHashImplName,
			final MinHashParameters parameterSet,
			final MinHashDBLocation location,
			final int sequenceCount) {
		checkNotNull(dbname, "dbname");
		checkNotNull(minHashImplName, "minHashImplName");
		checkNotNull(parameterSet, "parameterSet");
		checkNotNull(location, "location");
		if (sequenceCount < 1) {
			throw new IllegalArgumentException("sequenceCount must be at least 1");
		}
		this.name = dbname;
		this.minHashImplementationName = minHashImplName;
		this.parameterSet = parameterSet;
		this.location = location;
		this.sequenceCount = sequenceCount;
	}

	/** Get the database name.
	 * @return the database name.
	 */
	public MinHashSketchDBName getName() {
		return name;
	}

	/** Get the name of the MinHash implementation that created the database.
	 * @return the implementation name.
	 */
	public MinHashImplementationName getImplementationName() {
		return minHashImplementationName;
	}
	
	/** Get the parameter set used when creating the database.
	 * @return the database parameters.
	 */
	public MinHashParameters getParameterSet() {
		return parameterSet;
	}

	/** Get the location of the database.
	 * @return the database location.
	 */
	public MinHashDBLocation getLocation() {
		return location;
	}

	/** Get the number of sequences / sketches in the database.
	 * @return the sequence count.
	 */
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
	 * @throws IncompatibleSketchesException if the database parameters don't match.
	 */
	public List<String> checkIsQueriableBy(final MinHashSketchDatabase query, final boolean strict)
			throws IncompatibleSketchesException {
		checkNotNull(query, "query");
		final List<String> warnings = new LinkedList<>();
		if (!getImplementationName().equals(query.getImplementationName())) {
			// need to check version?
			throw new IncompatibleSketchesException(String.format(
					"Implementations for sketches do not match: %s %s",
					getImplementationName().getName(), query.getImplementationName().getName()));
		}
		final MinHashParameters rp = getParameterSet();
		final MinHashParameters qp = query.getParameterSet();
		if (rp.getKmerSize() != qp.getKmerSize()) {
			throw new IncompatibleSketchesException(String.format(
					"Kmer size for sketches are not compatible: %s %s",
					rp.getKmerSize(), qp.getKmerSize()));
		}
		if (rp.getScaling().isPresent() ^ qp.getScaling().isPresent()) { // xor
			throw new IncompatibleSketchesException(
					"Both sketches must use either absolute sketch counts or scaling");
		}
		if (rp.getScaling().isPresent()) {
			// may need to adjust this when we have an implementation that supports scaling
			if (!rp.getScaling().get().equals(qp.getScaling().get())) {
				throw new IncompatibleSketchesException(String.format(
						"Scaling parameters for sketches are not compatible: %s %s",
						rp.getScaling().get(), qp.getScaling().get()));
			}
		} else {
			if (!rp.getSketchSize().get().equals(qp.getSketchSize().get())) {
				if (strict) {
					throw new IncompatibleSketchesException(String.format(
							"Query sketch size %s does not match target %s",
							qp.getSketchSize().get(), rp.getSketchSize().get()));
				}
				if (qp.getSketchSize().get() > rp.getSketchSize().get()) {
					warnings.add(String.format(
							"Query sketch size %s is larger than target sketch size %s",
							qp.getSketchSize().get(), rp.getSketchSize().get()));
				} else {
					throw new IncompatibleSketchesException(String.format(
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + ((minHashImplementationName == null) ? 0 : minHashImplementationName.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((parameterSet == null) ? 0 : parameterSet.hashCode());
		result = prime * result + sequenceCount;
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
		MinHashSketchDatabase other = (MinHashSketchDatabase) obj;
		if (location == null) {
			if (other.location != null) {
				return false;
			}
		} else if (!location.equals(other.location)) {
			return false;
		}
		if (minHashImplementationName == null) {
			if (other.minHashImplementationName != null) {
				return false;
			}
		} else if (!minHashImplementationName.equals(other.minHashImplementationName)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (parameterSet == null) {
			if (other.parameterSet != null) {
				return false;
			}
		} else if (!parameterSet.equals(other.parameterSet)) {
			return false;
		}
		if (sequenceCount != other.sequenceCount) {
			return false;
		}
		return true;
	}
}
