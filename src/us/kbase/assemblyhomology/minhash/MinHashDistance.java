package us.kbase.assemblyhomology.minhash;

import static us.kbase.assemblyhomology.util.Util.exceptOnEmpty;

public class MinHashDistance implements Comparable<MinHashDistance>{

	//TODO JAVADOC
	//TODO TEST
	
	private final String sequenceID;
	private final double distance;
	
	public MinHashDistance(final String sequenceID, final double distance) {
		exceptOnEmpty(sequenceID, "sequenceID");
		if (distance < 0 || distance > 1) {
			throw new IllegalArgumentException("Illegal distance value: " + distance);
		}
		this.sequenceID = sequenceID;
		this.distance = distance;
	}

	public String getSequenceID() {
		return sequenceID;
	}

	public double getDistance() {
		return distance;
	}

	@Override
	public int compareTo(final MinHashDistance o) {
		final int dc = Double.compare(distance, o.distance);
		return dc == 0 ? sequenceID.compareTo(o.sequenceID) : dc;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MinHashDistance [sequenceID=");
		builder.append(sequenceID);
		builder.append(", distance=");
		builder.append(distance);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(distance);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
