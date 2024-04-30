package us.kbase.test.assemblyhomology.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.time.Instant;

import org.junit.Test;

import com.google.common.base.Optional;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.core.FilterID;
import us.kbase.assemblyhomology.core.DataSourceID;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.test.assemblyhomology.TestCommon;

public class NamespaceTest {
	
	// mock the db location to avoid the file exists check
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(Namespace.class).usingGetClass().verify();
	}
	
	@Test
	public void buildMinimal() throws Exception {
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		final Namespace ns = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(3).withScaling(4).build(),
						loc,
						42),
				new LoadID("bat"),
				Instant.ofEpochMilli(10000))
				.build();
		
		assertThat("incorrect mod date", ns.getModification(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect desc", ns.getDescription(), is(Optional.absent()));
		assertThat("incorrect id", ns.getID(), is(new NamespaceID("foo")));
		assertThat("incorrect collector id", ns.getFilterID(), is(Optional.absent()));
		assertThat("incorrect load ID", ns.getLoadID(), is(new LoadID("bat")));
		assertThat("incorrect sketch db", ns.getSketchDatabase(),
				is(new MinHashSketchDatabase(
					new MinHashSketchDBName("foo"),
					new MinHashImplementationName("mash"),
					MinHashParameters.getBuilder(3).withScaling(4).build(),
					loc,
					42)));
		assertThat("incorrect source DB ID", ns.getSourceDatabaseID(), is("default"));
		assertThat("incorrect source ID", ns.getSourceID(), is(new DataSourceID("KBase")));
	}
	
	@Test
	public void buildMaximal() throws Exception {
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		final Namespace ns = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(3).withScaling(4).build(),
						loc,
						42),
				new LoadID("bat"),
				Instant.ofEpochMilli(10000))
				.withNullableFilterID(new FilterID("yay"))
				.withNullableDataSourceID(new DataSourceID("JGI"))
				.withNullableDescription("desc")
				.withNullableSourceDatabaseID("IMG")
				.build();
		
		assertThat("incorrect mod date", ns.getModification(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect desc", ns.getDescription(), is(Optional.of("desc")));
		assertThat("incorrect id", ns.getID(), is(new NamespaceID("foo")));
		assertThat("incorrect load id", ns.getLoadID(), is(new LoadID("bat")));
		assertThat("incorrect collector id", ns.getFilterID(),
				is(Optional.of(new FilterID("yay"))));
		assertThat("incorrect sketch db", ns.getSketchDatabase(),
				is(new MinHashSketchDatabase(
					new MinHashSketchDBName("foo"),
					new MinHashImplementationName("mash"),
					MinHashParameters.getBuilder(3).withScaling(4).build(),
					loc,
					42)));
		assertThat("incorrect source DB id", ns.getSourceDatabaseID(), is("IMG"));
		assertThat("incorrect source id", ns.getSourceID(), is(new DataSourceID("JGI")));
	}
	
	@Test
	public void buildMaximalWithNulls() throws Exception {
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		final Namespace ns = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(3).withScaling(4).build(),
						loc,
						42),
				new LoadID("bat"),
				Instant.ofEpochMilli(10000))
				.withNullableFilterID(new FilterID("yay"))
				.withNullableDataSourceID(new DataSourceID("JGI"))
				.withNullableDescription("desc")
				.withNullableSourceDatabaseID("IMG")
				.withNullableFilterID(null)
				.withNullableDataSourceID(null)
				.withNullableDescription(null)
				.withNullableSourceDatabaseID(null)
				.build();
		
		assertThat("incorrect mod date", ns.getModification(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect desc", ns.getDescription(), is(Optional.absent()));
		assertThat("incorrect id", ns.getID(), is(new NamespaceID("foo")));
		assertThat("incorrect load id", ns.getLoadID(), is(new LoadID("bat")));
		assertThat("incorrect collector id", ns.getFilterID(), is(Optional.absent()));
		assertThat("incorrect sketch db", ns.getSketchDatabase(),
				is(new MinHashSketchDatabase(
					new MinHashSketchDBName("foo"),
					new MinHashImplementationName("mash"),
					MinHashParameters.getBuilder(3).withScaling(4).build(),
					loc,
					42)));
		assertThat("incorrect source DB id", ns.getSourceDatabaseID(), is("default"));
		assertThat("incorrect source id", ns.getSourceID(), is(new DataSourceID("KBase")));
	}
	
	@Test
	public void buildMaximalWithWhitespace() throws Exception {
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		final Namespace ns = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(3).withScaling(4).build(),
						loc,
						42),
				new LoadID("bat"),
				Instant.ofEpochMilli(10000))
				.withNullableFilterID(new FilterID("yay"))
				.withNullableFilterID(new FilterID("yo"))
				.withNullableDataSourceID(new DataSourceID("JGI"))
				.withNullableDescription("desc")
				.withNullableSourceDatabaseID("IMG")
				.withNullableDescription("   \t   \n  ")
				.withNullableSourceDatabaseID("   \t   \n  ")
				.build();
		
		assertThat("incorrect mod date", ns.getModification(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect desc", ns.getDescription(), is(Optional.absent()));
		assertThat("incorrect id", ns.getID(), is(new NamespaceID("foo")));
		assertThat("incorrect load id", ns.getLoadID(), is(new LoadID("bat")));
		assertThat("incorrect collector id", ns.getFilterID(),
				is(Optional.of(new FilterID("yo"))));
		assertThat("incorrect sketch db", ns.getSketchDatabase(),
				is(new MinHashSketchDatabase(
					new MinHashSketchDBName("foo"),
					new MinHashImplementationName("mash"),
					MinHashParameters.getBuilder(3).withScaling(4).build(),
					loc,
					42)));
		assertThat("incorrect source DB id", ns.getSourceDatabaseID(), is("default"));
		assertThat("incorrect source id", ns.getSourceID(), is(new DataSourceID("JGI")));
	}
	
	@Test
	public void getBuilderFail() throws Exception {
		final NamespaceID id = new NamespaceID("foo");
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(3).withScaling(4).build(),
				mock(MinHashDBLocation.class),
				42);
		final LoadID l = new LoadID("l");
		final Instant i = Instant.ofEpochMilli(10000);
		
		failGetBuilder(null, db, l, i, new NullPointerException("id"));
		failGetBuilder(id, null, l, i, new NullPointerException("sketchDatabase"));
		failGetBuilder(id, db, null, i, new NullPointerException("loadID"));
		failGetBuilder(id, db, l, null, new NullPointerException("modification"));
		failGetBuilder(new NamespaceID("foo1"), db, l, i, new IllegalArgumentException(
				"Namespace ID must equal sketch DB ID"));
		
	}
	
	private void failGetBuilder(
			final NamespaceID id,
			final MinHashSketchDatabase sketchDatabase,
			final LoadID loadID,
			final Instant creation,
			final Exception expected) {
		try {
			Namespace.getBuilder(id, sketchDatabase, loadID, creation);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

}
