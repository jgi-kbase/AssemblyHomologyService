package us.kbase.assemblyhomology.minhash;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import us.kbase.assemblyhomology.util.Util;

public class MinHashDistanceSet {
	
	//TODO JAVADOC
	//TODO TEST

	// distance to the query sequence
	private final Set<MinHashDistance> distances;
	private final List<String> warnings;

	// might want a builder here?
	public MinHashDistanceSet(
			final Set<MinHashDistance> distances,
			final List<String> warnings) {
		// check for duplicate sequence IDs?
		Util.checkNoNullsInCollection(distances, "distances");
		Util.checkNoNullsOrEmpties(warnings, "warnings");
		// we don't check DB compatibility given that the presence of distance measurements
		// implies they must be compatible
		this.distances = Collections.unmodifiableSet(new TreeSet<>(distances));
		this.warnings = Collections.unmodifiableList(new LinkedList<>(warnings));
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
		builder.append("MinHashDistanceSet [distances=");
		builder.append(distances);
		builder.append(", warnings=");
		builder.append(warnings);
		builder.append("]");
		return builder.toString();
	}
}
