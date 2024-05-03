package us.kbase.test.assemblyhomology.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.Collections;

import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.test.assemblyhomology.TestCommon;

public class SequenceMetadataTest {
	
	@Test
	public void equals() {
		EqualsVerifier.forClass(SequenceMetadata.class).usingGetClass().verify();
	}
	
	@Test
	public void buildMinimal() {
		final SequenceMetadata sm = SequenceMetadata.getBuilder(
				"id", "sid", Instant.ofEpochMilli(10000))
				.build();
		
		assertThat("incorrect id", sm.getID(), is("id"));
		assertThat("incorrect id", sm.getCreation(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect id", sm.getRelatedIDs(), is(Collections.emptyMap()));
		assertThat("incorrect id", sm.getScientificName(), is(Optional.absent()));
		assertThat("incorrect id", sm.getSourceID(), is("sid"));
	}
	
	@Test
	public void buildMaximal() {
		final SequenceMetadata sm = SequenceMetadata.getBuilder(
				"id", "sid", Instant.ofEpochMilli(10000))
				.withNullableScientificName("sciname")
				.withRelatedID("Genome", "foo")
				.withRelatedID("NCBI", "some string or other")
				.build();
		
		assertThat("incorrect id", sm.getID(), is("id"));
		assertThat("incorrect id", sm.getCreation(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect id", sm.getRelatedIDs(), is(ImmutableMap.of(
				"Genome", "foo", "NCBI", "some string or other")));
		assertThat("incorrect id", sm.getScientificName(), is(Optional.of("sciname")));
		assertThat("incorrect id", sm.getSourceID(), is("sid"));
	}
	
	@Test
	public void buildNullSciName() {
		final SequenceMetadata sm = SequenceMetadata.getBuilder(
				"id", "sid", Instant.ofEpochMilli(10000))
				.withNullableScientificName(null)
				.build();
		
		assertThat("incorrect id", sm.getID(), is("id"));
		assertThat("incorrect id", sm.getCreation(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect id", sm.getRelatedIDs(), is(Collections.emptyMap()));
		assertThat("incorrect id", sm.getScientificName(), is(Optional.absent()));
		assertThat("incorrect id", sm.getSourceID(), is("sid"));
	}
	
	@Test
	public void buildWhitespaceSciName() {
		final SequenceMetadata sm = SequenceMetadata.getBuilder(
				"id", "sid", Instant.ofEpochMilli(10000))
				.withNullableScientificName("   \t  \n ")
				.build();
		
		assertThat("incorrect id", sm.getID(), is("id"));
		assertThat("incorrect id", sm.getCreation(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect id", sm.getRelatedIDs(), is(Collections.emptyMap()));
		assertThat("incorrect id", sm.getScientificName(), is(Optional.absent()));
		assertThat("incorrect id", sm.getSourceID(), is("sid"));
	}

	@Test
	public void immutable() {
		final SequenceMetadata sm = SequenceMetadata.getBuilder(
				"id", "sid", Instant.ofEpochMilli(10000))
				.withRelatedID("Genome", "foo")
				.withRelatedID("NCBI", "some string or other")
				.build();
		try {
			sm.getRelatedIDs().put("foo", "bar");
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new UnsupportedOperationException());
		}
	}
	
	@Test
	public void buildFail() {
		final Instant c = Instant.ofEpochMilli(10000);
		failBuild(null, "s", c, new IllegalArgumentException(
				"id cannot be null or whitespace only"));
		failBuild("    \t  \n ", "s", c, new IllegalArgumentException(
				"id cannot be null or whitespace only"));
		failBuild("i", null, c, new IllegalArgumentException(
				"sourceID cannot be null or whitespace only"));
		failBuild("i", "  \t  \n ", c, new IllegalArgumentException(
				"sourceID cannot be null or whitespace only"));
		failBuild("i", "s", null, new NullPointerException("creation"));
	}
	
	private void failBuild(
			final String id,
			final String sourceID,
			final Instant creation,
			final Exception expected) {
		try {
			SequenceMetadata.getBuilder(id, sourceID, creation);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void addRelatedIDFail() {
		failAddRelatedID(null, "i", new IllegalArgumentException(
				"idType cannot be null or whitespace only"));
		failAddRelatedID("  \t   \n ", "i", new IllegalArgumentException(
				"idType cannot be null or whitespace only"));
		failAddRelatedID("t", null, new IllegalArgumentException(
				"id cannot be null or whitespace only"));
		failAddRelatedID("t", " \t    \n  ", new IllegalArgumentException(
				"id cannot be null or whitespace only"));
	}
	
	private void failAddRelatedID(final String idType, final String id, final Exception expected) {
		try {
			SequenceMetadata.getBuilder("i", "s", Instant.ofEpochMilli(10000))
				.withRelatedID(idType, id);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

}
