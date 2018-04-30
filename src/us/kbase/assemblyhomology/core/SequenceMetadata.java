package us.kbase.assemblyhomology.core;

import static us.kbase.assemblyhomology.util.Util.exceptOnEmpty;
import static us.kbase.assemblyhomology.util.Util.isNullOrEmpty;

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
	
	private SequenceMetadata(
			final String id,
			final String sourceID,
			final String scientificName,
			final Map<String, String> relatedIDs) {
		this.id = id;
		this.sourceID = sourceID;
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

	public String getSourceID() {
		return sourceID;
	}

	public Optional<String> getScientificName() {
		return scientificName;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SequenceMetadata [id=");
		builder.append(id);
		builder.append(", sourceID=");
		builder.append(sourceID);
		builder.append(", scientificName=");
		builder.append(scientificName);
		builder.append(", relatedIDs=");
		builder.append(relatedIDs);
		builder.append("]");
		return builder.toString();
	}

	public static Builder getBuilder(final String id, final String sourceID) {
		return new Builder(id, sourceID);
	}
	
	public static class Builder {
		
		private final String id;
		private final String sourceID;
		private String scientificName = null;
		private final Map<String, String> relatedIDs = new HashMap<>();
		
		private Builder(final String id, final String sourceID) {
			exceptOnEmpty(id, "id");
			exceptOnEmpty(sourceID, "sourceID");
			this.id = id;
			this.sourceID = sourceID;
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
			return new SequenceMetadata(id, sourceID, scientificName, relatedIDs);
		}
	}
	
}
