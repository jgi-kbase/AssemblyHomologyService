package us.kbase.assemblyhomology.load;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.load.ParseHelpers.fromYAML;
import static us.kbase.assemblyhomology.load.ParseHelpers.getString;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.assemblyhomology.core.SequenceMetadata.Builder;
import us.kbase.assemblyhomology.load.exceptions.LoadInputParseException;

public class SeqMetaLoadInfo {
	
	//TODO TEST
	//TODO JAVADOC
	
	private static final String RELATED_IDS = "relatedids";

	public final String id;
	public final String sourceID;
	public final Optional<String> scientificName;
	public final Map<String, String> relatedIDs;
	
	public SeqMetaLoadInfo(final String input, final String sourceInfo)
			throws LoadInputParseException {
		final Object predata = fromYAML(input, sourceInfo);
		if (!(predata instanceof Map)) {
			throw new LoadInputParseException(
					"Expected mapping in top level YAML in " + sourceInfo);
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
				if (e.getKey() == null) {
					throw new  LoadInputParseException(String.format(
							"Null key in mapping at %s in %s", RELATED_IDS, sourceInfo));
				}
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

	public static String getRelatedIds() {
		return RELATED_IDS;
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

	public Map<String, String> getRelatedIDs() {
		return relatedIDs;
	}
	
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
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SeqMetaLoadInfo [id=");
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
	
	public static void main(final String[] args) throws Exception {
		final Map<String, Object> data = ImmutableMap.of(
				"id", "foo",
				"sourceid", "1/2/3",
				"sciname", "E. gallumbits",
				"relatedids", ImmutableMap.of(
						"Genome", "4/5/6",
						"NCBI", "GCF_somestuff"));
		
		final DumperOptions dos = new DumperOptions();
		dos.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		
		final String yaml = new Yaml(dos).dump(data);
		System.out.println(yaml);
		
		final SeqMetaLoadInfo sm = new SeqMetaLoadInfo(yaml, "some yaml or other line 6");
		
		System.out.println(sm);
		
		System.out.println(sm.toSequenceMetadata(Instant.ofEpochMilli(10000)));
	}
	
}
