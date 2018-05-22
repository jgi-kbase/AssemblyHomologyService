package us.kbase.test.assemblyhomology.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static us.kbase.test.assemblyhomology.TestCommon.set;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.SequenceMatches;
import us.kbase.assemblyhomology.core.SequenceMatches.SequenceDistanceAndMetadata;
import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.test.assemblyhomology.TestCommon;

public class SequenceMatchesTest {
	
	// mocking the db location in order to avoid a location check

	@Test
	public void equalsSequenceMatches() {
		EqualsVerifier.forClass(SequenceMatches.class).usingGetClass().verify();
	}
	
	@Test
	public void equalsSequenceDistanceAndMetadata() {
		EqualsVerifier.forClass(SequenceDistanceAndMetadata.class).usingGetClass().verify();
	}
	
	@Test
	public void seqDistMetaConstruct() throws Exception {
		final SequenceDistanceAndMetadata sdm = new SequenceDistanceAndMetadata(
				new NamespaceID("foo"),
				new MinHashDistance(new MinHashSketchDBName("bar"), "sid", 0.2),
				SequenceMetadata.getBuilder("sid", "source", Instant.ofEpochMilli(10000))
						.build());
		
		assertThat("incorrect nsid", sdm.getNamespaceID(), is(new NamespaceID("foo")));
		assertThat("incorrect dist", sdm.getDistance(), is(new MinHashDistance(
						new MinHashSketchDBName("bar"), "sid", 0.2)));
		assertThat("incorrect meta", sdm.getMetadata(), is(SequenceMetadata.getBuilder(
				"sid", "source", Instant.ofEpochMilli(10000)).build()));
	}
	
	@Test
	public void seqDistMetaFailConstruct() throws Exception {
		final NamespaceID n = new NamespaceID("foo");
		final MinHashDistance d = new MinHashDistance(new MinHashSketchDBName("bar"), "s", 1.0);
		final SequenceMetadata m = SequenceMetadata.getBuilder(
				"sid", "source", Instant.ofEpochMilli(10000)).build();
		
		failSeqDistMetaConstruct(null, d, m, new NullPointerException("namespaceID"));
		failSeqDistMetaConstruct(n, null, m, new NullPointerException("distance"));
		failSeqDistMetaConstruct(n, d, null, new NullPointerException("metadata"));
	}
	
	private void failSeqDistMetaConstruct(
			final NamespaceID namespaceID,
			final MinHashDistance distance,
			final SequenceMetadata metadata,
			final Exception expected) {
		try {
			new SequenceDistanceAndMetadata(namespaceID, distance, metadata);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void seqMatchConstruct() throws Exception {
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		final SequenceMatches sm = new SequenceMatches(
				new HashSet<>(Arrays.asList(
						Namespace.getBuilder(new NamespaceID("foo"),
								new MinHashSketchDatabase(
										new MinHashSketchDBName("foo"),
										new MinHashImplementationName("mash"),
										MinHashParameters.getBuilder(3).withScaling(3).build(),
										loc,
										42),
								new LoadID("baz"),
								Instant.ofEpochMilli(10000))
								.build(),
						Namespace.getBuilder(new NamespaceID("bar"),
								new MinHashSketchDatabase(
										new MinHashSketchDBName("bar"),
										new MinHashImplementationName("mash"),
										MinHashParameters.getBuilder(3).withScaling(3).build(),
										loc,
										42),
								new LoadID("bat"),
								Instant.ofEpochMilli(20000))
								.build()
						)),
				new MinHashImplementationInformation(
						new MinHashImplementationName("mash"), "2.0", Paths.get("msh")),
				Arrays.asList(
						new SequenceDistanceAndMetadata(
								new NamespaceID("foo"),
								new MinHashDistance(new MinHashSketchDBName("foo"), "sid", 0.34),
								SequenceMetadata.getBuilder(
										"sid", "source", Instant.ofEpochMilli(10000)).build()),
						new SequenceDistanceAndMetadata(
								new NamespaceID("bar"),
								new MinHashDistance(new MinHashSketchDBName("bar"), "sid2", 0.45),
								SequenceMetadata.getBuilder(
										"sid2", "source2", Instant.ofEpochMilli(20000)).build())
						),
				set("warn1", "warn2"));
		
		final Set<Namespace> expectedNS = new HashSet<>(Arrays.asList(
				Namespace.getBuilder(new NamespaceID("foo"),
						new MinHashSketchDatabase(
								new MinHashSketchDBName("foo"),
								new MinHashImplementationName("mash"),
								MinHashParameters.getBuilder(3).withScaling(3).build(),
								loc,
								42),
						new LoadID("baz"),
						Instant.ofEpochMilli(10000))
						.build(),
				Namespace.getBuilder(new NamespaceID("bar"),
						new MinHashSketchDatabase(
								new MinHashSketchDBName("bar"),
								new MinHashImplementationName("mash"),
								MinHashParameters.getBuilder(3).withScaling(3).build(),
								loc,
								42),
						new LoadID("bat"),
						Instant.ofEpochMilli(20000))
						.build()
				));
		
		final List<SequenceDistanceAndMetadata> expectedDist = Arrays.asList(
				new SequenceDistanceAndMetadata(
						new NamespaceID("foo"),
						new MinHashDistance(new MinHashSketchDBName("foo"), "sid", 0.34),
						SequenceMetadata.getBuilder(
								"sid", "source", Instant.ofEpochMilli(10000)).build()),
				new SequenceDistanceAndMetadata(
						new NamespaceID("bar"),
						new MinHashDistance(new MinHashSketchDBName("bar"), "sid2", 0.45),
						SequenceMetadata.getBuilder(
								"sid2", "source2", Instant.ofEpochMilli(20000)).build())
				);
		
		assertThat("incorrect namespaces", sm.getNamespaces(), is(expectedNS));
		assertThat("incorrect impl", sm.getImplementationInformation(), is(
				new MinHashImplementationInformation(
						new MinHashImplementationName("mash"), "2.0", Paths.get("msh"))));
		assertThat("incorrect dists", sm.getDistances(), is(expectedDist));
		assertThat("incorrect warnings", sm.getWarnings(), is(set("warn1", "warn2")));
	}
	
	@Test
	public void seqMatchConstructFail() throws Exception {
		final Namespace ns = Namespace.getBuilder(new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(3).withScaling(3).build(),
						mock(MinHashDBLocation.class),
						42),
				new LoadID("baz"),
				Instant.ofEpochMilli(10000))
				.build();
		
		final SequenceDistanceAndMetadata sm = new SequenceDistanceAndMetadata(
				new NamespaceID("foo"),
				new MinHashDistance(new MinHashSketchDBName("foo"), "sid", 0.34),
				SequenceMetadata.getBuilder(
						"sid", "source", Instant.ofEpochMilli(10000)).build());
		
		final Set<Namespace> nss = new HashSet<>(Arrays.asList(ns));
		final MinHashImplementationInformation ii = new MinHashImplementationInformation(
				new MinHashImplementationName("mash"), "2.0", Paths.get("msh"));
		final List<SequenceDistanceAndMetadata> d = Arrays.asList(sm);
		final Set<String> w = set("warn1");
		
		failSeqMatchConstruct(null, ii, d, w, new NullPointerException("namespaces"));
		failSeqMatchConstruct(new HashSet<>(Arrays.asList(ns, null)), ii, d, w,
				new NullPointerException("Null item in collection namespaces"));
		failSeqMatchConstruct(nss, null, d, w,
				new NullPointerException("implementationInformation"));
		failSeqMatchConstruct(nss, ii, null, w, new NullPointerException("distances"));
		failSeqMatchConstruct(nss, ii, Arrays.asList(sm, null), w,
				new NullPointerException("Null item in collection distances"));
		failSeqMatchConstruct(nss, ii, d, null, new NullPointerException("warnings"));
		failSeqMatchConstruct(nss, ii, d, set("warn1", null),
				new IllegalArgumentException(
						"Null or whitespace only string in collection warnings"));
		failSeqMatchConstruct(nss, ii, d, set("warn1", "  \t   \n   "),
				new IllegalArgumentException(
						"Null or whitespace only string in collection warnings"));
	}
	
	private void failSeqMatchConstruct(
			final Set<Namespace> namespaces,
			final MinHashImplementationInformation implementationInformation,
			final List<SequenceDistanceAndMetadata> distances,
			final Set<String> warnings,
			final Exception expected) {
		try {
			new SequenceMatches(namespaces, implementationInformation, distances, warnings);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void immutableInput() throws Exception {
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		final Namespace ns1 = Namespace.getBuilder(new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(3).withScaling(3).build(),
						loc,
						42),
				new LoadID("baz"),
				Instant.ofEpochMilli(10000))
				.build();
		final HashSet<Namespace> namespaces = new HashSet<>(Arrays.asList(ns1));
		final Namespace ns2 = Namespace.getBuilder(new NamespaceID("bar"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("bar"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(3).withScaling(3).build(),
						loc,
						42),
				new LoadID("bat"),
				Instant.ofEpochMilli(20000))
				.build();
		
		final List<SequenceDistanceAndMetadata> dists = new LinkedList<>();
		final SequenceDistanceAndMetadata dist1 = new SequenceDistanceAndMetadata(
						new NamespaceID("foo"),
						new MinHashDistance(new MinHashSketchDBName("foo"), "sid", 0.34),
						SequenceMetadata.getBuilder(
								"sid", "source", Instant.ofEpochMilli(10000)).build());
		dists.add(dist1);
		final SequenceDistanceAndMetadata dist2 = new SequenceDistanceAndMetadata(
				new NamespaceID("bar"),
				new MinHashDistance(new MinHashSketchDBName("bar"), "sid2", 0.45),
				SequenceMetadata.getBuilder(
						"sid2", "source2", Instant.ofEpochMilli(20000)).build());
		
		final Set<String> warnings = new HashSet<>();
		warnings.add("warn1");
		
		final SequenceMatches sm = new SequenceMatches(
				namespaces,
				new MinHashImplementationInformation(
						new MinHashImplementationName("mash"), "2.0", Paths.get("msh")),
				dists,
				warnings);
		
		namespaces.add(ns2);
		dists.add(dist2);
		warnings.add("warn2");
		
		assertThat("incorrect namespaces", sm.getNamespaces(),
				is(new HashSet<>(Arrays.asList(ns1))));
		assertThat("incorrect impl", sm.getImplementationInformation(), is(
				new MinHashImplementationInformation(
						new MinHashImplementationName("mash"), "2.0", Paths.get("msh"))));
		assertThat("incorrect dists", sm.getDistances(), is(Arrays.asList(dist1)));
		assertThat("incorrect warnings", sm.getWarnings(), is(set("warn1")));
	}
	
	@Test
	public void immutableOutput() throws Exception {
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		
		final SequenceMatches sm = new SequenceMatches(
				new HashSet<>(Arrays.asList(
						Namespace.getBuilder(new NamespaceID("foo"),
								new MinHashSketchDatabase(
										new MinHashSketchDBName("foo"),
										new MinHashImplementationName("mash"),
										MinHashParameters.getBuilder(3).withScaling(3).build(),
										loc,
										42),
								new LoadID("baz"),
								Instant.ofEpochMilli(10000))
								.build())),
				new MinHashImplementationInformation(
						new MinHashImplementationName("mash"), "2.0", Paths.get("msh")),
				Arrays.asList(
						new SequenceDistanceAndMetadata(
								new NamespaceID("foo"),
								new MinHashDistance(new MinHashSketchDBName("foo"), "sid", 0.34),
								SequenceMetadata.getBuilder(
										"sid", "source", Instant.ofEpochMilli(10000)).build())),
				set("warn1"));
		
		try {
			sm.getNamespaces().add(
					Namespace.getBuilder(
							new NamespaceID("bar"),
							new MinHashSketchDatabase(
									new MinHashSketchDBName("bar"),
									new MinHashImplementationName("mash"),
									MinHashParameters.getBuilder(3).withScaling(3).build(),
									loc,
									42),
							new LoadID("bat"),
							Instant.ofEpochMilli(20000))
							.build());
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new UnsupportedOperationException());
		}
		
		try {
			sm.getDistances().add(
					new SequenceDistanceAndMetadata(
							new NamespaceID("bar"),
							new MinHashDistance(new MinHashSketchDBName("bar"), "sid2", 0.45),
							SequenceMetadata.getBuilder(
									"sid2", "source2", Instant.ofEpochMilli(20000)).build()));
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new UnsupportedOperationException());
		}
		
		try {
			sm.getWarnings().add("warn2");
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new UnsupportedOperationException());
		}
	}
	
}
