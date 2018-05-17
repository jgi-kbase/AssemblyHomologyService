package us.kbase.assemblyhomology.minhash;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.exceptOnEmpty;

/** A MinHash distance from a query sequence to a reference sequence.
 * @author gaprice@lbl.gov
 *
 */
public class MinHashDistance implements Comparable<MinHashDistance> {

	private final MinHashSketchDBName referenceDBName;
	private final String sequenceID;
	private final double distance;
	
	/** Create a distance.
	 * @param referenceDBName the name of the database containing the reference sequence.
	 * @param sequenceID the ID of the reference sequence.
	 * @param distance the distance between the query sequence and the reference sequence.
	 */
	public MinHashDistance(
			final MinHashSketchDBName referenceDBName,
			final String sequenceID,
			final double distance) {
		checkNotNull(referenceDBName, "referenceDBName");
		exceptOnEmpty(sequenceID, "sequenceID");
		if (distance < 0 || distance > 1) {
			throw new IllegalArgumentException("Illegal distance value: " + distance);
		}
		this.referenceDBName = referenceDBName;
		this.sequenceID = sequenceID;
		this.distance = distance;
	}

	/** Get the name of the reference database containing the reference sequence.
	 * @return the reference database name.
	 */
	public MinHashSketchDBName getReferenceDBName() {
		return referenceDBName;
	}
	
	/** Get the ID of the reference sequence.
	 * @return the sequence ID.
	 */
	public String getSequenceID() {
		return sequenceID;
	}

	/** Get the MinHash distance between the query sequence and the reference sequence.
	 * @return
	 */
	public double getDistance() {
		return distance;
	}

	@Override
	public int compareTo(final MinHashDistance o) {
		final int dc = Double.compare(distance, o.distance);
		if (dc != 0) {
			return dc;
		}
		final int refdb = referenceDBName.compareTo(o.referenceDBName);
		return refdb == 0 ? sequenceID.compareTo(o.sequenceID) : refdb;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(distance);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((referenceDBName == null) ? 0 : referenceDBName.hashCode());
		result = prime * result + ((sequenceID == null) ? 0 : sequenceID.hashCode());
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
		MinHashDistance other = (MinHashDistance) obj;
		if (Double.doubleToLongBits(distance) != Double.doubleToLongBits(other.distance)) {
			return false;
		}
		if (referenceDBName == null) {
			if (other.referenceDBName != null) {
				return false;
			}
		} else if (!referenceDBName.equals(other.referenceDBName)) {
			return false;
		}
		if (sequenceID == null) {
			if (other.sequenceID != null) {
				return false;
			}
		} else if (!sequenceID.equals(other.sequenceID)) {
			return false;
		}
		return true;
	}
	
	
}
