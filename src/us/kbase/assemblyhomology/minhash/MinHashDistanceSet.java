package us.kbase.assemblyhomology.minhash;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import us.kbase.assemblyhomology.util.Util;

public class MinHashDistanceSet {
	
	//TODO JAVADOC
	//TODO TEST

	private final MinHashSketchDatabase query;
	private final MinHashSketchDatabase reference;
	// distance to the query sequence
	private final Set<MinHashDistance> distances;
	private final List<String> warnings;

	// might want a builder here?
	public MinHashDistanceSet(
			final MinHashSketchDatabase query,
			final MinHashSketchDatabase reference,
			final Set<MinHashDistance> distances,
			final List<String> warnings) {
		checkNotNull(query, "query");
		checkNotNull(reference, "reference");
		// check for duplicate sequence IDs?
		Util.checkNoNullsInCollection(distances, "distances");
		Util.checkNoNullsOrEmpties(warnings, "warnings");
		if (query.getSequenceCount() != 1) { // may want to relax list later
			throw new IllegalArgumentException("Query may only contain 1 sequence");
		}
		// we don't check DB compatibility given that the presence of distance measurements
		// implies they must be compatible
		this.query = query;
		this.reference = reference;
		this.distances = Collections.unmodifiableSet(new TreeSet<>(distances));
		this.warnings = Collections.unmodifiableList(new LinkedList<>(warnings));
	}

	public MinHashSketchDatabase getQuery() {
		return query;
	}

	public MinHashSketchDatabase getReference() {
		return reference;
	}

	
	/** Returns a sorted set of the distances from the reference sequences to the query.
	 * @return the distances.
	 */
	public Set<MinHashDistance> getDistances() {
		return distances;
	}
	
	public List<String> getWarnings() {
		return warnings;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MinHashDistanceSet [query=");
		builder.append(query);
		builder.append(", reference=");
		builder.append(reference);
		builder.append(", distances=");
		builder.append(distances);
		builder.append(", warnings=");
		builder.append(warnings);
		builder.append("]");
		return builder.toString();
	}
}
