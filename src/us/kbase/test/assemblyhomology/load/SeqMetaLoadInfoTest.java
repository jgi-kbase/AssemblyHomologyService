package us.kbase.test.assemblyhomology.load;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.assemblyhomology.load.SeqMetaLoadInfo;
import us.kbase.assemblyhomology.load.exceptions.LoadInputParseException;
import us.kbase.test.assemblyhomology.MapBuilder;
import us.kbase.test.assemblyhomology.TestCommon;

public class SeqMetaLoadInfoTest {

	private static final ObjectMapper OM = new ObjectMapper();
	
	private static String toStr(final Object value) throws Exception {
		return OM.writeValueAsString(value);
	}
	
	@Test
	public void equals() {
		EqualsVerifier.forClass(SeqMetaLoadInfo.class).usingGetClass().verify();
	}
	
	@Test
	public void constructMinimal() throws Exception {
		final Map<String, String> input = ImmutableMap.of("id", "foo", "sourceid", "bar");
		
		final SeqMetaLoadInfo i = new SeqMetaLoadInfo(toStr(input), "source");
		
		assertThat("incorrect ID", i.getId(), is("foo"));
		assertThat("incorrect related IDs", i.getRelatedIDs(), is(Collections.emptyMap()));
		assertThat("incorrect sci name", i.getScientificName(), is(Optional.absent()));
		assertThat("incorrect source ID", i.getSourceID(), is("bar"));
	}
	
	@Test
	public void constructMaximal() throws Exception {
		final Map<String, Object> input = ImmutableMap.of(
				"id", "foo",
				"sourceid", "bar",
				"sciname", "super sciency",
				"relatedids", ImmutableMap.of("NCBI", "some id", "Genome", "some other id"));
		
		final SeqMetaLoadInfo i = new SeqMetaLoadInfo(toStr(input), "source");
		
		assertThat("incorrect ID", i.getId(), is("foo"));
		assertThat("incorrect related IDs", i.getRelatedIDs(), is(
				ImmutableMap.of("NCBI", "some id", "Genome", "some other id")));
		assertThat("incorrect sci name", i.getScientificName(), is(Optional.of("super sciency")));
		assertThat("incorrect source ID", i.getSourceID(), is("bar"));
	}
	
	@Test
	public void immutable() throws Exception {
		final Map<String, String> input = ImmutableMap.of("id", "foo", "sourceid", "bar");
		
		final SeqMetaLoadInfo i = new SeqMetaLoadInfo(toStr(input), "source");
		
		try {
			i.getRelatedIDs().put("foo", "bar");
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new UnsupportedOperationException());
		}
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, "s", new IllegalArgumentException(
				"input cannot be null or whitespace only"));
		failConstruct("  \t   \n   ", "s", new IllegalArgumentException(
				"input cannot be null or whitespace only"));
		failConstruct("{}", null, new IllegalArgumentException(
				"sourceInfo cannot be null or whitespace only"));
		failConstruct("{}", "   \t   \n   ", new IllegalArgumentException(
				"sourceInfo cannot be null or whitespace only"));
		
		failConstruct("[]", "mysource",
				new LoadInputParseException("Expected mapping at / in mysource"));
		
		failConstruct(toStr(ImmutableMap.of("sourceid", "foo")), "mysource",
				new LoadInputParseException("Missing value at id. Source: mysource"));
		failConstruct(toStr(ImmutableMap.of("id", "foo")), "mysource",
				new LoadInputParseException("Missing value at sourceid. Source: mysource"));
		
		failConstruct(toStr(
				ImmutableMap.of("sourceid", "foo", "id", Collections.emptyMap())),
				"mysource",
				new LoadInputParseException("Expected string, got {} at id. Source: mysource"));
		failConstruct(toStr(
				ImmutableMap.of("id", "foo", "sourceid", Collections.emptyList())),
				"mysource",
				new LoadInputParseException(
						"Expected string, got [] at sourceid. Source: mysource"));
		
		failConstruct(
				toStr(ImmutableMap.of("id", "foo", "sourceid", "bar", "sciname", 1)),
				"mysource",
				new LoadInputParseException(
						"Expected string, got 1 at sciname. Source: mysource"));
		
		failConstruct(
				toStr(ImmutableMap.of("id", "foo", "sourceid", "bar", "relatedids", 1)),
				"mysource",
				new LoadInputParseException(
						"Expected mapping at relatedids in mysource"));
		
		failConstruct(
				toStr(ImmutableMap.of(
						"id", "foo",
						"sourceid", "bar",
						"relatedids", MapBuilder.<String, Object>newHashMap()
								.with("baz", null)
								.build())),
				"mysource",
				new LoadInputParseException(
						"Expected string, got null at relatedids/baz in mysource"));
	}
	
	private void failConstruct(
			final String input,
			final String sourceInfo,
			final Exception expected) {
		try {
			new SeqMetaLoadInfo(input, sourceInfo);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void toSequenceMetaMinimal() throws Exception{
		final Map<String, String> input = ImmutableMap.of("id", "foo", "sourceid", "bar");
		
		final SeqMetaLoadInfo i = new SeqMetaLoadInfo(toStr(input), "source");
		
		final SequenceMetadata sm = i.toSequenceMetadata(Instant.ofEpochMilli(10000));
		
		assertThat("incorrect id", sm.getID(), is("foo"));
		assertThat("incorrect creation", sm.getCreation(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect rel ids", sm.getRelatedIDs(), is(Collections.emptyMap()));
		assertThat("incorrect sci name", sm.getScientificName(), is(Optional.absent()));
		assertThat("incorrect source id", sm.getSourceID(), is("bar"));
	}
	
	@Test
	public void toSequenceMetaMaximal() throws Exception{
		final Map<String, Object> input = ImmutableMap.of(
				"id", "foo",
				"sourceid", "bar",
				"sciname", "super sciency",
				"relatedids", ImmutableMap.of("NCBI", "some id", "Genome", "some other id"));
		
		final SeqMetaLoadInfo i = new SeqMetaLoadInfo(toStr(input), "source");
		
		final SequenceMetadata sm = i.toSequenceMetadata(Instant.ofEpochMilli(10000));
		
		assertThat("incorrect id", sm.getID(), is("foo"));
		assertThat("incorrect creation", sm.getCreation(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect rel ids", sm.getRelatedIDs(), is(
				ImmutableMap.of("NCBI", "some id", "Genome", "some other id")));
		assertThat("incorrect sci name", sm.getScientificName(), is(Optional.of("super sciency")));
		assertThat("incorrect source id", sm.getSourceID(), is("bar"));
	}
	
	@Test
	public void toSequenceMetaFail() throws Exception {
		final Map<String, String> input = ImmutableMap.of("id", "foo", "sourceid", "bar");
		
		final SeqMetaLoadInfo i = new SeqMetaLoadInfo(toStr(input), "source");
		
		try {
			i.toSequenceMetadata(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("creation"));
		}
	}
}
