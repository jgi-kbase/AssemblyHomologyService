package us.kbase.assemblyhomology.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.exceptOnEmpty;
import static us.kbase.assemblyhomology.util.Util.isNullOrEmpty;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Optional;

/** Metadata about a genomic sequence.
 * @author gaprice@lbl.gov
 *
 */
public class SequenceMetadata {
	
	private final String id;
	private final String sourceID;
	private final Optional<String> scientificName;
	private final Map<String, String> relatedIDs;
	private final Instant creation;
	
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

	/** Get the local ID for the sequence. This is typically an ID used locally to match the
	 * sequence with other data.
	 * @return the sequence's ID.
	 */
	public String getID() {
		return id;
	}

	/** Get the time this sequence record was created.
	 * @return the creation date.
	 */
	public Instant getCreation() {
		return creation;
	}

	/** Get related, but not primary IDs for the the sequence.
	 * @return the related IDs.
	 */
	public Map<String, String> getRelatedIDs() {
		return relatedIDs;
	}

	/** Get the ID for the sequence from the source database where the sequence originates.
	 * @return the source ID.
	 */
	public String getSourceID() {
		return sourceID;
	}

	/** Get the scientific name for the organism associated with the sequence, if any.
	 * @return the scientific name or absent().
	 */
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

	/** Get a {@link SequenceMetadata} builder.
	 * @param id the ID of the sequence.
	 * @param sourceID the ID of the sequence in the source database from which the sequence
	 * originates.
	 * @param creation the time the sequence record was created.
	 * @return a new {@link SequenceMetadata} builder.
	 */
	public static Builder getBuilder(
			final String id,
			final String sourceID,
			final Instant creation) {
		return new Builder(id, sourceID, creation);
	}
	
	/** A builder for {@link SequenceMetadata}.
	 * @author gaprice@lbl.gov
	 *
	 */
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
		
		/** Add a scientific name for the sequence to the builder. Ignored if null or
		 * whitespace only.
		 * @param scientificName the scientific name.
		 * @return this builder.
		 */
		public Builder withNullableScientificName(final String scientificName) {
			if (isNullOrEmpty(scientificName)) {
				this.scientificName = null;
			} else {
				this.scientificName = scientificName;
			}
			return this;
		}
		
		/** Add a related ID for the sequence to the builder.
		 * @param idType the type of the ID, for example NBCI or ReferenceGenome.
		 * @param id the ID.
		 * @return this builder.
		 */
		public Builder withRelatedID(final String idType, final String id) {
			exceptOnEmpty(idType, "idType");
			exceptOnEmpty(id, "id");
			relatedIDs.put(idType, id);
			return this;
		}
		
		/** Builder the {@link SequenceMetadata}.
		 * @return the metadata.
		 */
		public SequenceMetadata build() {
			return new SequenceMetadata(id, sourceID, creation, scientificName, relatedIDs);
		}
	}
	
}
