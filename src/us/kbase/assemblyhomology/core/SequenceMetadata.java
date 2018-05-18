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

	public String getID() {
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creation == null) ? 0 : creation.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((relatedIDs == null) ? 0 : relatedIDs.hashCode());
		result = prime * result + ((scientificName == null) ? 0 : scientificName.hashCode());
		result = prime * result + ((sourceID == null) ? 0 : sourceID.hashCode());
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
		SequenceMetadata other = (SequenceMetadata) obj;
		if (creation == null) {
			if (other.creation != null) {
				return false;
			}
		} else if (!creation.equals(other.creation)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (relatedIDs == null) {
			if (other.relatedIDs != null) {
				return false;
			}
		} else if (!relatedIDs.equals(other.relatedIDs)) {
			return false;
		}
		if (scientificName == null) {
			if (other.scientificName != null) {
				return false;
			}
		} else if (!scientificName.equals(other.scientificName)) {
			return false;
		}
		if (sourceID == null) {
			if (other.sourceID != null) {
				return false;
			}
		} else if (!sourceID.equals(other.sourceID)) {
			return false;
		}
		return true;
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
