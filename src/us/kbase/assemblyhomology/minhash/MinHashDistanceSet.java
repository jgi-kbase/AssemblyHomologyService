package us.kbase.assemblyhomology.minhash;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import us.kbase.assemblyhomology.util.Util;

/** A set of distances from a query sequence to zero or more reference sequences.
 * @author gaprice@lbl.gov
 *
 */
public class MinHashDistanceSet {
	
	// distance to the query sequence
	private final Set<MinHashDistance> distances;
	private final List<String> warnings;

	/** Create a distance set.
	 * @param distances the distances.
	 * @param warnings any warnings associated with the distances.
	 */
	public MinHashDistanceSet(
			final Set<MinHashDistance> distances,
			final List<String> warnings) {
		// check for duplicate sequence IDs?
		Util.checkNoNullsInCollection(distances, "distances");
		Util.checkNoNullsOrEmpties(warnings, "warnings");
		this.distances = Collections.unmodifiableSet(new TreeSet<>(distances));
		this.warnings = Collections.unmodifiableList(new LinkedList<>(warnings));
	}

	/** Returns a sorted set of the distances from the reference sequences to the query.
	 * @return the distances.
	 */
	public Set<MinHashDistance> getDistances() {
		return distances;
	}
	
	/** Get any warnings associated with the distances.
	 * @return the warnings.
	 */
	public List<String> getWarnings() {
		return warnings;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((distances == null) ? 0 : distances.hashCode());
		result = prime * result + ((warnings == null) ? 0 : warnings.hashCode());
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
		MinHashDistanceSet other = (MinHashDistanceSet) obj;
		if (distances == null) {
			if (other.distances != null) {
				return false;
			}
		} else if (!distances.equals(other.distances)) {
			return false;
		}
		if (warnings == null) {
			if (other.warnings != null) {
				return false;
			}
		} else if (!warnings.equals(other.warnings)) {
			return false;
		}
		return true;
	}
}
