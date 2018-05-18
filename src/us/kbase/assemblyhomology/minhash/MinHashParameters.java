package us.kbase.assemblyhomology.minhash;

import com.google.common.base.Optional;

/** A set of parameters for a MinHash sketch database.
 * @author gaprice@lbl.gov
 *
 */
public class MinHashParameters {

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

	/** Get the kmer size used to create the sketch.
	 * @return
	 */
	public int getKmerSize() {
		return kmerSize;
	}

	/** Get the number of hashes in the sketch. Mutually exclusive with scaling.
	 * @return the number of hashes in the sketch, or absent if the sketch is scaled.
	 */
	public Optional<Integer> getSketchSize() {
		return sketchSize;
	}

	/** Get the scaling parameter for the sketch. Mutually exclusive with an absolute size.
	 * @return the sketch's scaling parameter, or absent if the sketch was generated with
	 * an absolute size.
	 */
	public Optional<Integer> getScaling() {
		return scaling;
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

	/** Get a builder for {@link MinHashParameters}.
	 * @param kmerSize the kmer size used to create the sketch.
	 * @return a new builder.
	 */
	public static Builder getBuilder(final int kmerSize) {
		return new Builder(kmerSize);
	}
	
	/** A builder for a {@link MinHashParameters}.
	 * @author gaprice@lbl.gov
	 *
	 */
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
		
		/** Add a sketch size to the builder. Removes any scaling already set.
		 * @param sketchSize the sketch size.
		 * @return this builder.
		 */
		public Builder withSketchSize(final int sketchSize) {
			if (sketchSize < 1) {
				throw new IllegalArgumentException("sketchSize < 1");
			}
			this.sketchSize = sketchSize;
			this.scaling = null;
			return this;
		}
		
		/** Add a scaling parameter to the builder. Removes any sketch size already set.
		 * @param scaling the scaling parameter.
		 * @return this builder.
		 */
		public Builder withScaling(final int scaling) {
			if (scaling < 1) {
				throw new IllegalArgumentException("scaling < 1");
			}
			this.sketchSize = null;
			this.scaling = scaling;
			return this;
		}
		
		/** Builder the {@link MinHashParameters}.
		 * One of the scaling parameter or the sketch size must have been set.
		 * @return a new {@link MinHashParameters}.
		 */
		public MinHashParameters build() {
			if (sketchSize == null && scaling == null) {
				throw new IllegalStateException("One of scaling or sketchSize must be set");
			}
			return new MinHashParameters(kmerSize, sketchSize, scaling);
		}
	}
}
