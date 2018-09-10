package us.kbase.test.assemblyhomology.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.Test;

import com.google.common.base.Optional;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.core.DataSourceID;
import us.kbase.assemblyhomology.core.FilterID;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.MinHashDistanceFilterFactory;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.NamespaceView;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.test.assemblyhomology.TestCommon;

public class NamespaceViewTest {

	@Test
	public void equals() {
		EqualsVerifier.forClass(NamespaceView.class).usingGetClass().verify();
	}
	
	@Test
	public void constructMinimalNoFilter() throws Exception {
		final MinHashDBLocation loc = mock(MinHashDBLocation.class); // avoid creating temp file
		
		final NamespaceView nv = new NamespaceView(
				Namespace.getBuilder(
						new NamespaceID("id"),
						new MinHashSketchDatabase(
								new MinHashSketchDBName("id"),
								new MinHashImplementationName("impl"),
								MinHashParameters.getBuilder(5).withScaling(400).build(),
								loc,
								42),
						new LoadID("lid"),
						Instant.ofEpochMilli(40000))
						.build());
		
		assertThat("incorrect authsource", nv.getAuthsource(), is(Optional.absent()));
		assertThat("incorrect description", nv.getDescription(), is(Optional.absent()));
		assertThat("incorrect impl", nv.getImplementationName(),
				is(new MinHashImplementationName("impl")));
		assertThat("incorrect mod date", nv.getModification(), is(Instant.ofEpochMilli(40000)));
		assertThat("incorrect ns id", nv.getNamespaceID(), is(new NamespaceID("id")));
		assertThat("incorrect params", nv.getParameterSet(),
				is(MinHashParameters.getBuilder(5).withScaling(400).build()));
		assertThat("incorrect seq count", nv.getSequenceCount(), is(42));
		assertThat("incorrect source DB ID", nv.getSourceDatabaseID(), is("default"));
		assertThat("incorrect source ID", nv.getSourceID(), is(new DataSourceID("KBase")));
	}
	
	@Test
	public void constructMaximalNoFilter() throws Exception {
		final MinHashDBLocation loc = mock(MinHashDBLocation.class); // avoid creating temp file
		
		final NamespaceView nv = new NamespaceView(
				Namespace.getBuilder(
						new NamespaceID("id"),
						new MinHashSketchDatabase(
								new MinHashSketchDBName("id"),
								new MinHashImplementationName("impl"),
								MinHashParameters.getBuilder(5).withScaling(400).build(),
								loc,
								42),
						new LoadID("lid"),
						Instant.ofEpochMilli(40000))
						.withNullableDataSourceID(new DataSourceID("JGI"))
						.withNullableDescription("desc")
						.withNullableSourceDatabaseID("sdb")
						.build());
		
		assertThat("incorrect authsource", nv.getAuthsource(), is(Optional.absent()));
		assertThat("incorrect description", nv.getDescription(), is(Optional.of("desc")));
		assertThat("incorrect impl", nv.getImplementationName(),
				is(new MinHashImplementationName("impl")));
		assertThat("incorrect mod date", nv.getModification(), is(Instant.ofEpochMilli(40000)));
		assertThat("incorrect ns id", nv.getNamespaceID(), is(new NamespaceID("id")));
		assertThat("incorrect params", nv.getParameterSet(),
				is(MinHashParameters.getBuilder(5).withScaling(400).build()));
		assertThat("incorrect seq count", nv.getSequenceCount(), is(42));
		assertThat("incorrect source DB ID", nv.getSourceDatabaseID(), is("sdb"));
		assertThat("incorrect source ID", nv.getSourceID(), is(new DataSourceID("JGI")));
	}

	@Test
	public void constructMinimalWithFilter() throws Exception {
		final MinHashDBLocation loc = mock(MinHashDBLocation.class); // avoid creating temp file
		final MinHashDistanceFilterFactory ffac = mock(MinHashDistanceFilterFactory.class);
		when(ffac.getID()).thenReturn(new FilterID("foo"));
		when(ffac.getAuthSource()).thenReturn(Optional.absent());
		
		final NamespaceView nv = new NamespaceView(
				Namespace.getBuilder(
						new NamespaceID("id"),
						new MinHashSketchDatabase(
								new MinHashSketchDBName("id"),
								new MinHashImplementationName("impl"),
								MinHashParameters.getBuilder(5).withScaling(400).build(),
								loc,
								42),
						new LoadID("lid"),
						Instant.ofEpochMilli(40000))
						.withNullableFilterID(new FilterID("foo"))
						.build(),
				ffac);
		
		assertThat("incorrect authsource", nv.getAuthsource(), is(Optional.absent()));
		assertThat("incorrect description", nv.getDescription(), is(Optional.absent()));
		assertThat("incorrect impl", nv.getImplementationName(),
				is(new MinHashImplementationName("impl")));
		assertThat("incorrect mod date", nv.getModification(), is(Instant.ofEpochMilli(40000)));
		assertThat("incorrect ns id", nv.getNamespaceID(), is(new NamespaceID("id")));
		assertThat("incorrect params", nv.getParameterSet(),
				is(MinHashParameters.getBuilder(5).withScaling(400).build()));
		assertThat("incorrect seq count", nv.getSequenceCount(), is(42));
		assertThat("incorrect source DB ID", nv.getSourceDatabaseID(), is("default"));
		assertThat("incorrect source ID", nv.getSourceID(), is(new DataSourceID("KBase")));
	}
	
	@Test
	public void constructMaximalWithFilter() throws Exception {
		final MinHashDBLocation loc = mock(MinHashDBLocation.class); // avoid creating temp file
		final MinHashDistanceFilterFactory ffac = mock(MinHashDistanceFilterFactory.class);
		when(ffac.getID()).thenReturn(new FilterID("foo"));
		when(ffac.getAuthSource()).thenReturn(Optional.of("as"));
		
		final NamespaceView nv = new NamespaceView(
				Namespace.getBuilder(
						new NamespaceID("id"),
						new MinHashSketchDatabase(
								new MinHashSketchDBName("id"),
								new MinHashImplementationName("impl"),
								MinHashParameters.getBuilder(5).withScaling(400).build(),
								loc,
								42),
						new LoadID("lid"),
						Instant.ofEpochMilli(40000))
						.withNullableFilterID(new FilterID("foo"))
						.withNullableDataSourceID(new DataSourceID("JGI"))
						.withNullableDescription("desc")
						.withNullableSourceDatabaseID("sdb")
						.build(),
				ffac);
		
		assertThat("incorrect authsource", nv.getAuthsource(), is(Optional.of("as")));
		assertThat("incorrect description", nv.getDescription(), is(Optional.of("desc")));
		assertThat("incorrect impl", nv.getImplementationName(),
				is(new MinHashImplementationName("impl")));
		assertThat("incorrect mod date", nv.getModification(), is(Instant.ofEpochMilli(40000)));
		assertThat("incorrect ns id", nv.getNamespaceID(), is(new NamespaceID("id")));
		assertThat("incorrect params", nv.getParameterSet(),
				is(MinHashParameters.getBuilder(5).withScaling(400).build()));
		assertThat("incorrect seq count", nv.getSequenceCount(), is(42));
		assertThat("incorrect source DB ID", nv.getSourceDatabaseID(), is("sdb"));
		assertThat("incorrect source ID", nv.getSourceID(), is(new DataSourceID("JGI")));
	}
	
	@Test
	public void constructFailSingleArg() {
		try {
			new NamespaceView(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("namespace"));
		}
	}
	
	@Test
	public void constructFailTwoArgs() throws Exception {
		final MinHashDBLocation loc = mock(MinHashDBLocation.class); // avoid creating temp file
		final MinHashDistanceFilterFactory ffac = mock(MinHashDistanceFilterFactory.class);
		when(ffac.getID()).thenReturn(new FilterID("bar"));
		
		final Namespace ns = Namespace.getBuilder(
				new NamespaceID("id"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("id"),
						new MinHashImplementationName("impl"),
						MinHashParameters.getBuilder(5).withScaling(400).build(),
						loc,
						42),
				new LoadID("lid"),
				Instant.ofEpochMilli(40000))
				.withNullableFilterID(new FilterID("foo"))
				.build();
		
		final Namespace ns2 = Namespace.getBuilder(
				new NamespaceID("id"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("id"),
						new MinHashImplementationName("impl"),
						MinHashParameters.getBuilder(5).withScaling(400).build(),
						loc,
						42),
				new LoadID("lid"),
				Instant.ofEpochMilli(40000))
				.build();
		
		failConstruct(null, ffac, new NullPointerException("namespace"));
		failConstruct(null, ffac, new NullPointerException("namespace"));
		failConstruct(ns, ffac, new IllegalArgumentException(
				"The namespace filter ID and the filter's ID do not match"));
		failConstruct(ns2, ffac, new IllegalArgumentException(
				"The namespace filter ID and the filter's ID do not match"));
	}
	
	private void failConstruct(
			final Namespace ns,
			final MinHashDistanceFilterFactory filter,
			final Exception expected) {
		try {
			new NamespaceView(ns, filter);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
}
