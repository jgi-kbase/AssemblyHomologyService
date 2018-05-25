package us.kbase.assemblyhomology.load;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.load.ParseHelpers.fromYAML;
import static us.kbase.assemblyhomology.load.ParseHelpers.getString;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.assemblyhomology.core.SequenceMetadata.Builder;
import us.kbase.assemblyhomology.load.exceptions.LoadInputParseException;

/** Represents load information for a single sequence instantiated from a JSON or YAML string.
 * 
 * A typical example:
 * {"sourceid": "15792/4/3", "id": "15792_4_3", "sciname": "E. coli", "relatedids": {"NCBI": "GCF_001735525.1"}}
 * @author gaprice@lbl.gov
 *
 */
public class SeqMetaLoadInfo {
	
	private static final String RELATED_IDS = "relatedids";

	private final String id;
	private final String sourceID;
	private final Optional<String> scientificName;
	private final Map<String, String> relatedIDs;
	
	/** Create the load information for a sequence.
	 * @param input a JSON or YAML string containing the load information.
	 * @param sourceInfo information about the source of the data, often a file name, possibly
	 * with a line number.
	 * @throws LoadInputParseException if the input couldn't be parsed.
	 */
	public SeqMetaLoadInfo(final String input, final String sourceInfo)
			throws LoadInputParseException {
		final Object predata = fromYAML(input, sourceInfo); // checks sourceInfo not null / ws
		if (!(predata instanceof Map)) {
			throw new LoadInputParseException(
					"Expected mapping at / in " + sourceInfo);
		}
		@SuppressWarnings("unchecked")
		final Map<String, Object> data = (Map<String, Object>) predata;
		id = getString(data, "id", sourceInfo, false);
		sourceID = getString(data, "sourceid", sourceInfo, false);
		scientificName = Optional.fromNullable(getString(data, "sciname", sourceInfo, true));
		relatedIDs = Collections.unmodifiableMap(getRelatedIDs(data, sourceInfo));
	}

	private Map<String, String> getRelatedIDs(
			final Map<String, Object> data,
			final String sourceInfo)
			throws LoadInputParseException {
		final Object relIDs = data.get(RELATED_IDS);
		final Map<String, String> goodIDs = new HashMap<>();
		if (relIDs != null) {
			if (!(relIDs instanceof Map)) {
				throw new  LoadInputParseException(String.format(
						"Expected mapping at %s in %s", RELATED_IDS, sourceInfo));
			}
			@SuppressWarnings("unchecked")
			final Map<String, Object> relIDs2 = (Map<String, Object>) relIDs;
			for (final Entry<String, Object> e: relIDs2.entrySet()) {
				// key can't be null, yaml parser won't allow it
				// and gives you a huge exception, so we don't bother testing that
				if (!(e.getValue() instanceof String)) {
					throw new  LoadInputParseException(String.format(
							"Expected string, got %s at %s/%s in %s",
							e.getValue(), RELATED_IDS, e.getKey(), sourceInfo));
				}
				goodIDs.put(e.getKey(), (String) e.getValue());
			}
		}
		return goodIDs;
	}

	/** Get the ID of the sequence.
	 * @return the ID.
	 */
	public String getId() {
		return id;
	}

	/** Get the ID of the sequence at the data source.
	 * @return the source ID.
	 */
	public String getSourceID() {
		return sourceID;
	}

	/** Get the scientific name of the organism associated with the sequence.
	 * @return the scientific name.
	 */
	public Optional<String> getScientificName() {
		return scientificName;
	}

	/** Get any related IDs associated with the sequence, e.g. an NCBI ID.
	 * @return the related IDs.
	 */
	public Map<String, String> getRelatedIDs() {
		return relatedIDs;
	}
	
	/** Get a sequence metadata object from the load info.
	 * @param creation the time the sequence metadata object was created.
	 * @return the sequence metadata.
	 */
	public SequenceMetadata toSequenceMetadata(final Instant creation) {
		checkNotNull(creation, "creation");
		final Builder b = SequenceMetadata.getBuilder(id, sourceID, creation)
				.withNullableScientificName(scientificName.orNull());
		for (final Entry<String, String> e: relatedIDs.entrySet()) {
			b.withRelatedID(e.getKey(), e.getValue());
		}
		return b.build();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		SeqMetaLoadInfo other = (SeqMetaLoadInfo) obj;
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

}
