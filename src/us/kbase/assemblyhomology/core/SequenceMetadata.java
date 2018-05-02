package us.kbase.assemblyhomology.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.exceptOnEmpty;
import static us.kbase.assemblyhomology.util.Util.isNullOrEmpty;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Optional;

public class SequenceMetadata {
	
	//TODO JAVADOC
	//TODO TEST
	
	public final String id;
	public final String sourceID;
	public final Optional<String> scientificName;
	public Map<String, String> relatedIDs;
	private Instant creation;
	
	private SequenceMetadata(
			final String id,
			final String sourceID,
			final Instant creation,
			final String scientificName,
			final Map<String, String> relatedIDs) {
		this.id = id;
		this.sourceID = sourceID;
		this.creation = creation;
		this.scientificName = Optional.fromNullable(scientificName);
		this.relatedIDs = Collections.unmodifiableMap(relatedIDs);
	}

	public Map<String, String> getRelatedIDs() {
		return relatedIDs;
	}

	public void setRelatedIDs(Map<String, String> relatedIDs) {
		this.relatedIDs = relatedIDs;
	}

	public String getId() {
		return id;
	}

	public Instant getCreation() {
		return creation;
	}

	public String getSourceID() {
		return sourceID;
	}

	public Optional<String> getScientificName() {
		return scientificName;
	}

	@Override
	public String toString() {
		StringBuilder builder2 = new StringBuilder();
		builder2.append("SequenceMetadata [id=");
		builder2.append(id);
		builder2.append(", sourceID=");
		builder2.append(sourceID);
		builder2.append(", scientificName=");
		builder2.append(scientificName);
		builder2.append(", relatedIDs=");
		builder2.append(relatedIDs);
		builder2.append(", creation=");
		builder2.append(creation);
		builder2.append("]");
		return builder2.toString();
	}

	public static Builder getBuilder(
			final String id,
			final String sourceID,
			final Instant creation) {
		return new Builder(id, sourceID, creation);
	}
	
	public static class Builder {
		
		private final String id;
		private final String sourceID;
		private final Instant creation;
		private String scientificName = null;
		private final Map<String, String> relatedIDs = new HashMap<>();
		
		private Builder(final String id, final String sourceID, final Instant creation) {
			exceptOnEmpty(id, "id");
			exceptOnEmpty(sourceID, "sourceID");
			checkNotNull(creation, "creation");
			this.id = id;
			this.sourceID = sourceID;
			this.creation = creation;
		}
		
		public Builder withNullableScientificName(final String scientificName) {
			if (isNullOrEmpty(scientificName)) {
				this.scientificName = null;
			} else {
				this.scientificName = scientificName;
			}
			return this;
		}
		
		public Builder withRelatedID(final String idType, final String id) {
			exceptOnEmpty(idType, "idType");
			exceptOnEmpty(id, "id");
			relatedIDs.put(idType, id);
			return this;
		}
		
		public SequenceMetadata build() {
			return new SequenceMetadata(id, sourceID, creation, scientificName, relatedIDs);
		}
	}
	
}
