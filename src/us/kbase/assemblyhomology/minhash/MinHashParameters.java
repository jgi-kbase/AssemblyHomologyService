package us.kbase.assemblyhomology.minhash;

import com.google.common.base.Optional;

public class MinHashParameters {

	//TODO TEST
	//TODO JAVADOC
	
	private final int kmerSize;
	private final Optional<Integer> sketchSize;
	private final Optional<Integer> scaling;
	
	private MinHashParameters(
			final int kmerSize,
			final Integer sketchSize,
			final Integer scaling) {
		this.kmerSize = kmerSize;
		this.sketchSize = Optional.fromNullable(sketchSize);
		this.scaling = Optional.fromNullable(scaling);
	}

	public int getKmerSize() {
		return kmerSize;
	}

	public Optional<Integer> getSketchSize() {
		return sketchSize;
	}

	public Optional<Integer> getScaling() {
		return scaling;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MinHashParameters [kmerSize=");
		builder.append(kmerSize);
		builder.append(", sketchSize=");
		builder.append(sketchSize);
		builder.append(", scaling=");
		builder.append(scaling);
		builder.append("]");
		return builder.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sketchSize == null) ? 0 : sketchSize.hashCode());
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
		if (sketchSize == null) {
			if (other.sketchSize != null) {
				return false;
			}
		} else if (!sketchSize.equals(other.sketchSize)) {
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
		private Integer sketchSize = null;
		private Integer scaling = null;
		
		private Builder(final int kmerSize) {
			if (kmerSize < 1) {
				throw new IllegalArgumentException("kmerSize < 1");
			}
			this.kmerSize = kmerSize;
		}
		
		public Builder withSketchSize(final int sketchSize) {
			if (sketchSize < 1) {
				throw new IllegalArgumentException("sketchSize <1");
			}
			this.sketchSize = sketchSize;
			this.scaling = null;
			return this;
		}
		
		public Builder withScaling(final int scaling) {
			if (scaling < 1) {
				throw new IllegalArgumentException("scaling <1");
			}
			this.sketchSize = null;
			this.scaling = scaling;
			return this;
		}
		
		public MinHashParameters build() {
			if (sketchSize == null && scaling == null) {
				throw new IllegalStateException("One of scaling or sketchSize must be set");
			}
			return new MinHashParameters(kmerSize, sketchSize, scaling);
		}
	}
}
