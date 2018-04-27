package us.kbase.assemblyhomology.minhash;

import com.google.common.base.Optional;

public class MinHashParameters {

	//TODO TEST
	//TODO JAVADOC
	
	private final int kmerSize;
	private final Optional<Integer> hashCount;
	private final Optional<Integer> scaling;
	
	public MinHashParameters(final int kmerSize, final Integer hashCount, final Integer scaling) {
		this.kmerSize = kmerSize;
		this.hashCount = Optional.fromNullable(hashCount);
		this.scaling = Optional.fromNullable(scaling);
	}

	public int getKmerSize() {
		return kmerSize;
	}

	public Optional<Integer> getHashCount() {
		return hashCount;
	}

	public Optional<Integer> getScaling() {
		return scaling;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MinHashParameters [kmerSize=");
		builder.append(kmerSize);
		builder.append(", hashCount=");
		builder.append(hashCount);
		builder.append(", scaling=");
		builder.append(scaling);
		builder.append("]");
		return builder.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hashCount == null) ? 0 : hashCount.hashCode());
		result = prime * result + kmerSize;
		result = prime * result + ((scaling == null) ? 0 : scaling.hashCode());
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
		MinHashParameters other = (MinHashParameters) obj;
		if (hashCount == null) {
			if (other.hashCount != null) {
				return false;
			}
		} else if (!hashCount.equals(other.hashCount)) {
			return false;
		}
		if (kmerSize != other.kmerSize) {
			return false;
		}
		if (scaling == null) {
			if (other.scaling != null) {
				return false;
			}
		} else if (!scaling.equals(other.scaling)) {
			return false;
		}
		return true;
	}

	public static Builder getBuilder(final int kmerSize) {
		return new Builder(kmerSize);
	}
	
	public static class Builder {
		
		private final int kmerSize;
		private Integer hashCount = null;
		private Integer scaling = null;
		
		private Builder(final int kmerSize) {
			if (kmerSize < 1) {
				throw new IllegalArgumentException("kmerSize < 1");
			}
			this.kmerSize = kmerSize;
		}
		
		public Builder withHashCount(final int hashCount) {
			if (hashCount < 1) {
				throw new IllegalArgumentException("hashCount <1");
			}
			this.hashCount = hashCount;
			this.scaling = null;
			return this;
		}
		
		public Builder withScaling(final int scaling) {
			if (scaling < 1) {
				throw new IllegalArgumentException("scaling <1");
			}
			this.hashCount = null;
			this.scaling = scaling;
			return this;
		}
		
		public MinHashParameters build() {
			if (hashCount == null && scaling == null) {
				throw new IllegalStateException("One of scaling or hashCount must be set");
			}
			return new MinHashParameters(kmerSize, hashCount, scaling);
		}
	}
}
