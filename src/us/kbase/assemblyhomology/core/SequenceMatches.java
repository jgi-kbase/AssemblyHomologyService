package us.kbase.assemblyhomology.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;

public class SequenceMatches {
	
	//TODO TEST
	//TODO JAVADOC
	
	private final Namespace namespace;
	private final MinHashImplementationInformation implementationInformation;
	private final List<SequenceDistanceAndMetadata> distances;
	
	public SequenceMatches(
			final Namespace namespace,
			final MinHashImplementationInformation implementationInformation,
			final List<SequenceDistanceAndMetadata> distances) {
		checkNotNull(namespace, "namespace");
		checkNotNull(implementationInformation, "implementationInformation");
		checkNotNull(distances, "distances");
		this.namespace = namespace;
		this.implementationInformation = implementationInformation;
		this.distances = Collections.unmodifiableList(new LinkedList<>(distances));
	}

	public Namespace getNamespace() {
		return namespace;
	}

	public MinHashImplementationInformation getImplementationInformation() {
		return implementationInformation;
	}

	public List<SequenceDistanceAndMetadata> getDistances() {
		return distances;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SequenceMatches [namespace=");
		builder.append(namespace);
		builder.append(", implementationInformation=");
		builder.append(implementationInformation);
		builder.append(", distances=");
		builder.append(distances);
		builder.append("]");
		return builder.toString();
	}

	public static class SequenceDistanceAndMetadata {
		
		private final MinHashDistance distance;
		private final SequenceMetadata metadata;
		
		public SequenceDistanceAndMetadata(
				final MinHashDistance distance,
				final SequenceMetadata metadata) {
			checkNotNull(distance, "distance");
			checkNotNull(metadata, "metadata");
			this.distance = distance;
			this.metadata = metadata;
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
			builder.append("]");
			return builder.toString();
		}
		
	}

}
