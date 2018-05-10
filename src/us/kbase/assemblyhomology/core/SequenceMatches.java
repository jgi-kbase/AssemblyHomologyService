package us.kbase.assemblyhomology.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.checkNoNullsInCollection;
import static us.kbase.assemblyhomology.util.Util.checkNoNullsOrEmpties;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;

public class SequenceMatches {
	
	//TODO TEST
	//TODO JAVADOC
	
	private final Set<Namespace> namespaces;
	private final MinHashImplementationInformation implementationInformation;
	private final List<SequenceDistanceAndMetadata> distances;
	private final List<String> warnings;
	
	public SequenceMatches(
			final Set<Namespace> namespaces,
			final MinHashImplementationInformation implementationInformation,
			final List<SequenceDistanceAndMetadata> distances,
			final List<String> warnings) {
		checkNoNullsInCollection(namespaces, "namespaces");
		checkNotNull(implementationInformation, "implementationInformation");
		checkNotNull(distances, "distances");
		checkNoNullsOrEmpties(warnings, "warnings");
		this.namespaces = namespaces;
		this.implementationInformation = implementationInformation;
		this.distances = Collections.unmodifiableList(new LinkedList<>(distances));
		this.warnings = Collections.unmodifiableList(new LinkedList<>(warnings));
	}

	public Set<Namespace> getNamespaces() {
		return namespaces;
	}

	public MinHashImplementationInformation getImplementationInformation() {
		return implementationInformation;
	}

	public List<SequenceDistanceAndMetadata> getDistances() {
		return distances;
	}
	
	public List<String> getWarnings() {
		return warnings;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SequenceMatches [namespaces=");
		builder.append(namespaces);
		builder.append(", implementationInformation=");
		builder.append(implementationInformation);
		builder.append(", distances=");
		builder.append(distances);
		builder.append(", warnings=");
		builder.append(warnings);
		builder.append("]");
		return builder.toString();
	}

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

		public NamespaceID getNamespaceID() {
			return namespaceID;
		}

		public MinHashDistance getDistance() {
			return distance;
		}

		public SequenceMetadata getMetadata() {
			return metadata;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("SequenceDistanceAndMetadata [distance=");
			builder.append(distance);
			builder.append(", metadata=");
			builder.append(metadata);
			builder.append(", namespaceID=");
			builder.append(namespaceID);
			builder.append("]");
			return builder.toString();
		}
		
	}

}