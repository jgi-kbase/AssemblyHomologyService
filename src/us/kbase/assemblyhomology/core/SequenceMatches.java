package us.kbase.assemblyhomology.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.checkNoNullsInCollection;
import static us.kbase.assemblyhomology.util.Util.checkNoNullsOrEmpties;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;

/** A set of MinHash matches to a query sequence.
 * 
 * Currently this class does no consistency checking between the various inputs.
 * @author gaprice@lbl.gov
 *
 */
public class SequenceMatches {
	
	// could add some consistency checks at some point but doesn't seem particularly important
	
	private final Set<Namespace> namespaces;
	private final MinHashImplementationInformation implementationInformation;
	private final List<SequenceDistanceAndMetadata> distances;
	private final List<String> warnings;
	
	/** Create a set of matches.
	 * @param namespaces the namespaces containing sequences that were matched against.
	 * @param implementationInformation information about the MinHash implementation that
	 * generated the matches.
	 * @param distances the match distance information.
	 * @param warnings any warnings that were generated while processing the matches.
	 */
	public SequenceMatches(
			final Set<Namespace> namespaces,
			final MinHashImplementationInformation implementationInformation,
			final List<SequenceDistanceAndMetadata> distances,
			final List<String> warnings) {
		checkNoNullsInCollection(namespaces, "namespaces");
		checkNotNull(implementationInformation, "implementationInformation");
		checkNoNullsInCollection(distances, "distances");
		checkNoNullsOrEmpties(warnings, "warnings");
		this.namespaces = Collections.unmodifiableSet(new HashSet<>(namespaces));
		this.implementationInformation = implementationInformation;
		this.distances = Collections.unmodifiableList(new LinkedList<>(distances));
		this.warnings = Collections.unmodifiableList(new LinkedList<>(warnings));
	}

	/** Get the namespaces containing sequences that were matched against.
	 * @return the namespaces.
	 */
	public Set<Namespace> getNamespaces() {
		return namespaces;
	}

	/** Get information about the MinHash implementation that generated the matches.
	 * @return implemetation information.
	 */
	public MinHashImplementationInformation getImplementationInformation() {
		return implementationInformation;
	}

	/** Get the distance information for the matched sequences.
	 * @return the distances.
	 */
	public List<SequenceDistanceAndMetadata> getDistances() {
		return distances;
	}
	
	/** Get any warnings that were generated during matches.
	 * @return the warnings, if any.
	 */
	public List<String> getWarnings() {
		return warnings;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((distances == null) ? 0 : distances.hashCode());
		result = prime * result + ((implementationInformation == null) ? 0 : implementationInformation.hashCode());
		result = prime * result + ((namespaces == null) ? 0 : namespaces.hashCode());
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
		SequenceMatches other = (SequenceMatches) obj;
		if (distances == null) {
			if (other.distances != null) {
				return false;
			}
		} else if (!distances.equals(other.distances)) {
			return false;
		}
		if (implementationInformation == null) {
			if (other.implementationInformation != null) {
				return false;
			}
		} else if (!implementationInformation.equals(other.implementationInformation)) {
			return false;
		}
		if (namespaces == null) {
			if (other.namespaces != null) {
				return false;
			}
		} else if (!namespaces.equals(other.namespaces)) {
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

	/** Distance measurements from a query sequence to a reference sequence and
	 * metadata for the reference sequence.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class SequenceDistanceAndMetadata {
		
		// could flatten these to remove redundancy between the classes
		private final MinHashDistance distance;
		private final SequenceMetadata metadata;
		private final NamespaceID namespaceID;
		
		public SequenceDistanceAndMetadata(
				final NamespaceID namespaceID,
				final MinHashDistance distance,
				final SequenceMetadata metadata) {
			checkNotNull(namespaceID, "namespaceID");
			checkNotNull(distance, "distance");
			checkNotNull(metadata, "metadata");
			this.distance = distance;
			this.metadata = metadata;
			this.namespaceID = namespaceID;
		}

		/** Get the namespace ID for the reference sequence.
		 * @return
		 */
		public NamespaceID getNamespaceID() {
			return namespaceID;
		}

		/** Get the distance information for the reference sequence.
		 * @return
		 */
		public MinHashDistance getDistance() {
			return distance;
		}

		/** Get the metadata for the reference sequence.
		 * @return the reference sequence metadata.
		 */
		public SequenceMetadata getMetadata() {
			return metadata;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((distance == null) ? 0 : distance.hashCode());
			result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
			result = prime * result + ((namespaceID == null) ? 0 : namespaceID.hashCode());
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
			SequenceDistanceAndMetadata other = (SequenceDistanceAndMetadata) obj;
			if (distance == null) {
				if (other.distance != null) {
					return false;
				}
			} else if (!distance.equals(other.distance)) {
				return false;
			}
			if (metadata == null) {
				if (other.metadata != null) {
					return false;
				}
			} else if (!metadata.equals(other.metadata)) {
				return false;
			}
			if (namespaceID == null) {
				if (other.namespaceID != null) {
					return false;
				}
			} else if (!namespaceID.equals(other.namespaceID)) {
				return false;
			}
			return true;
		}
	}

}