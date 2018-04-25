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
