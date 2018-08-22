package us.kbase.test.assemblyhomology.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static us.kbase.test.assemblyhomology.TestCommon.set;
import static us.kbase.test.assemblyhomology.TestCommon.assertLogEventsCorrect;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Optional;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import us.kbase.assemblyhomology.core.AssemblyHomology;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.SequenceMatches;
import us.kbase.assemblyhomology.core.SequenceMatches.SequenceDistanceAndMetadata;
import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.IncompatibleNamespacesException;
import us.kbase.assemblyhomology.core.exceptions.IncompatibleSketchesException;
import us.kbase.assemblyhomology.core.exceptions.InvalidSketchException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchSequenceException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashDistanceSet;
import us.kbase.assemblyhomology.minhash.MinHashImplementation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationFactory;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashInitException;
import us.kbase.assemblyhomology.minhash.exceptions.NotASketchException;
import us.kbase.assemblyhomology.storage.AssemblyHomologyStorage;
import us.kbase.test.assemblyhomology.TestCommon;
import us.kbase.test.assemblyhomology.TestCommon.LogEvent;

public class AssemblyHomologyTest {
	
	private static final Namespace NS1;
	private static final Namespace NS2;
	static {
		try {
			NS1 = Namespace.getBuilder(
				new NamespaceID("foo"),
				new MinHashSketchDatabase(
						new MinHashSketchDBName("foo"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(3).withScaling(4).build(),
						mock(MinHashDBLocation.class),
						42),
				new LoadID("bat"),
				Instant.ofEpochMilli(10000))
				.build();
			
			NS2 = Namespace.getBuilder(
					new NamespaceID("baz"),
					new MinHashSketchDatabase(
							new MinHashSketchDBName("baz"),
							new MinHashImplementationName("mash"),
							MinHashParameters.getBuilder(5).withScaling(7).build(),
							mock(MinHashDBLocation.class),
							21),
					new LoadID("boo"),
					Instant.ofEpochMilli(20000))
					.build();
		} catch (IllegalParameterException | MissingParameterException e) {
			throw new RuntimeException("Fix yer tests newb");
		}
	}
	
	private static Path TEMP_DIR;
	private static Path EMPTY_FILE_MSH;
	private static Path EMPTY_FILE_MSH2;
	private static Path EMPTY_FILE_MSH3;
	
	private static List<ILoggingEvent> logEvents;

	@BeforeClass
	public static void setUp() throws Exception {
		logEvents = TestCommon.setUpSLF4JTestLoggerAppender("us.kbase.assemblyhomology");
		
		final String pre = UUID.randomUUID().toString();
		TEMP_DIR = TestCommon.getTempDir()
				.resolve("AssemblyHomologyTest_" + pre);
		Files.createDirectories(TEMP_DIR);
		EMPTY_FILE_MSH = TEMP_DIR.resolve(pre + "1.msh");
		Files.createFile(EMPTY_FILE_MSH);
		EMPTY_FILE_MSH2 = TEMP_DIR.resolve(pre + "2.msh");
		Files.createFile(EMPTY_FILE_MSH2);
		EMPTY_FILE_MSH3 = TEMP_DIR.resolve(pre + "3.msh");
		Files.createFile(EMPTY_FILE_MSH3);
	}
	
	@AfterClass
	public static void breakDown() throws Exception {
		final boolean deleteTempFiles = TestCommon.isDeleteTempFiles();
		if (TEMP_DIR != null && Files.exists(TEMP_DIR) && deleteTempFiles) {
			FileUtils.deleteQuietly(TEMP_DIR.toFile());
		}
	}
	
	@Before
	public void before() {
		logEvents.clear();
	}

	@Test
	public void constructWithEmptyFactories() {
		// should pass
		new AssemblyHomology(
				mock(AssemblyHomologyStorage.class),
				Collections.emptyList(),
				Paths.get("foo"),
				30);
	}
	
	@Test
	public void constructFail() throws Exception {
		final AssemblyHomologyStorage s = mock(AssemblyHomologyStorage.class);
		final MinHashImplementationFactory f1 = mock(MinHashImplementationFactory.class);
		when(f1.getImplementationName()).thenReturn(new MinHashImplementationName("Foo"));
		final MinHashImplementationFactory f2 = mock(MinHashImplementationFactory.class);
		when(f2.getImplementationName()).thenReturn(new MinHashImplementationName("foo"));
		final List<MinHashImplementationFactory> fs = Arrays.asList(f1);
		final Path t = Paths.get("foo");
		
		failConstruct(null, fs, t, 1, new NullPointerException("storage"));
		failConstruct(s, null, t, 1, new NullPointerException("implementationFactories"));
		failConstruct(s, Arrays.asList(f1, null), t, 1, new NullPointerException(
				"Null item in collection implementationFactories"));
		failConstruct(s, fs, null, 1, new NullPointerException("tempFileDirectory"));
		failConstruct(s, Arrays.asList(f1, f2), t, 1,
				new IllegalArgumentException("Duplicate implementation: foo"));
		failConstruct(s, fs, t, 0, new IllegalArgumentException("minhashTimeout must be > 0"));
	}
	
	private void failConstruct(
			final AssemblyHomologyStorage storage,
			final Collection<MinHashImplementationFactory> implementationFactories,
			final Path tempFileDirectory,
			final int timeout,
			final Exception expected) {
		try {
			new AssemblyHomology(storage, implementationFactories, tempFileDirectory, timeout);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getExpectedFileExtension() throws Exception {
		final AssemblyHomologyStorage s = mock(AssemblyHomologyStorage.class);
		final MinHashImplementationFactory f1 = mock(MinHashImplementationFactory.class);
		final MinHashImplementationFactory f2 = mock(MinHashImplementationFactory.class);
		
		when(f1.getImplementationName()).thenReturn(new MinHashImplementationName("foo"));
		when(f1.getExpectedFileExtension()).thenReturn(Optional.of(Paths.get("bar")));
		
		when(f2.getImplementationName()).thenReturn(new MinHashImplementationName("Baz"));
		when(f2.getExpectedFileExtension()).thenReturn(Optional.of(Paths.get("bat")));
		
		final AssemblyHomology as = new AssemblyHomology(
				s, Arrays.asList(f1, f2), Paths.get("foo"), 30);
		
		assertThat("incorrect ext",
				as.getExpectedFileExtension(new MinHashImplementationName("Foo")),
				is(Optional.of(Paths.get("bar"))));
		
		assertThat("incorrect ext",
				as.getExpectedFileExtension(new MinHashImplementationName("baz")),
				is(Optional.of(Paths.get("bat"))));
	}
	
	@Test
	public void getExpectedFileExtensionFail() throws Exception {
		final AssemblyHomologyStorage s = mock(AssemblyHomologyStorage.class);
		final MinHashImplementationFactory f1 = mock(MinHashImplementationFactory.class);
		
		when(f1.getImplementationName()).thenReturn(new MinHashImplementationName("foo"));
		when(f1.getExpectedFileExtension()).thenReturn(Optional.of(Paths.get("bar")));
		
		final AssemblyHomology ah = new AssemblyHomology(
				s, Arrays.asList(f1), Paths.get("foo"), 30);
		
		failGetExpectedFileExtension(ah, null, new NullPointerException("impl"));
		failGetExpectedFileExtension(ah, new MinHashImplementationName("foo1"),
				new IllegalArgumentException("No such implementation: foo1"));
	}
	
	private void failGetExpectedFileExtension(
			final AssemblyHomology ah,
			final MinHashImplementationName name,
			final Exception expected) {
		try {
			ah.getExpectedFileExtension(name);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getNamespacesNoArgs() throws Exception {
		// not much to test here
		final AssemblyHomologyStorage s = mock(AssemblyHomologyStorage.class);
		
		final AssemblyHomology ah = new AssemblyHomology(
				s, Collections.emptyList(), Paths.get("foo"), 30);
		
		when(s.getNamespaces()).thenReturn(new HashSet<>(Arrays.asList(NS1, NS2)));
		
		assertThat("incorrect namespaces", ah.getNamespaces(), is(
				new HashSet<>(Arrays.asList(NS2, NS1))));
	}
	
	@Test
	public void getNamespace() throws Exception {
		final AssemblyHomologyStorage s = mock(AssemblyHomologyStorage.class);
		
		final AssemblyHomology ah = new AssemblyHomology(
				s, Collections.emptyList(), Paths.get("foo"), 30);
		
		when(s.getNamespace(new NamespaceID("baz"))).thenReturn(NS2);
		
		assertThat("incorrect namespace", ah.getNamespace(new NamespaceID("baz")), is(NS2));
	}
	
	@Test
	public void getNamespaceFail() throws Exception {
		final AssemblyHomologyStorage s = mock(AssemblyHomologyStorage.class);
		
		final AssemblyHomology ah = new AssemblyHomology(
				s, Collections.emptyList(), Paths.get("foo"), 30);
		failGetNamespace(ah, null, new NullPointerException("namespaceID"));
		
		when(s.getNamespace(new NamespaceID("bar")))
				.thenThrow(new NoSuchNamespaceException("bar"));
		
		failGetNamespace(ah, new NamespaceID("bar"), new NoSuchNamespaceException("bar"));
		
	}
	
	private void failGetNamespace(
			final AssemblyHomology ah,
			final NamespaceID id,
			final Exception expected) {
		try {
			ah.getNamespace(id);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getNamespaces() throws Exception {
		final AssemblyHomologyStorage s = mock(AssemblyHomologyStorage.class);
		
		final AssemblyHomology ah = new AssemblyHomology(
				s, Collections.emptyList(), Paths.get("foo"), 30);
		
		assertThat("incorrect namespaces", ah.getNamespaces(set()), is(set()));
		
		when(s.getNamespace(new NamespaceID("foo"))).thenReturn(NS1);
		when(s.getNamespace(new NamespaceID("baz"))).thenReturn(NS2);
		
		assertThat("incorrect namespaces", ah.getNamespaces(set(
				new NamespaceID("baz"), new NamespaceID("foo"))), is(set(NS2, NS1)));
	}
	
	@Test
	public void getNamespacesFail() throws Exception {
		final AssemblyHomologyStorage s = mock(AssemblyHomologyStorage.class);
		
		final AssemblyHomology ah = new AssemblyHomology(
				s, Collections.emptyList(), Paths.get("foo"), 1);
		
		failGetNamespaces(ah, null, new NullPointerException("ids"));
		failGetNamespaces(ah, set(new NamespaceID("foo"), null),
				new NullPointerException("Null item in collection ids"));
		
		when(s.getNamespace(new NamespaceID("foo"))).thenReturn(NS1);
		when(s.getNamespace(new NamespaceID("whee")))
				.thenThrow(new NoSuchNamespaceException("whee"));
		
		failGetNamespaces(ah, set(new NamespaceID("foo"), new NamespaceID("whee")),
				new NoSuchNamespaceException("whee"));
	}
	
	private void failGetNamespaces(
			final AssemblyHomology ah,
			final Set<NamespaceID> ids,
			final Exception expected) {
		try {
			ah.getNamespaces(ids);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void measureDistance() throws Exception {
		measureDistance(MinHashParameters.getBuilder(31).withSketchSize(1000).build(), true,
				Collections.emptySet(), 7, 7);
	}
	
	@Test
	public void measureDistanceWithSmallReturn() throws Exception {
		measureDistance(MinHashParameters.getBuilder(31).withSketchSize(1000).build(), true,
				Collections.emptySet(), 0, 10);
	}
	
	@Test
	public void measureDistanceWithLargeReturn() throws Exception {
		measureDistance(MinHashParameters.getBuilder(31).withSketchSize(1000).build(), true,
				Collections.emptySet(), 101, 10);
	}
	
	@Test
	public void measureDistanceWithWarnings() throws Exception {
		final Set<String> warnings = set(
				"Namespace ns1: Query sketch size 1500 is larger than target sketch size 1000",
				"Namespace ns2: Query sketch size 1500 is larger than target sketch size 1000");
		measureDistance(MinHashParameters.getBuilder(31).withSketchSize(1500).build(), false,
				warnings, 5, 5);
	}

	private void measureDistance(
			final MinHashParameters queryParams,
			final boolean strict,
			final Set<String> expectedWarnings,
			final int returnSize,
			final int expectedReturnSize)
			throws Exception {
		final AssemblyHomologyStorage storage = mock(AssemblyHomologyStorage.class);
		final MinHashImplementationFactory fac = mock(MinHashImplementationFactory.class);
		final MinHashImplementation mash = mock(MinHashImplementation.class);
		
		when(fac.getImplementationName()).thenReturn(new MinHashImplementationName("mash"));
		
		final AssemblyHomology ah = new AssemblyHomology(
				storage, Arrays.asList(fac), Paths.get("temp_dir"), 5);
		
		final MinHashSketchDatabase ref1 = new MinHashSketchDatabase(
				new MinHashSketchDBName("ns1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH),
				2000);
		final Namespace ns1 = Namespace.getBuilder(
				new NamespaceID("ns1"), ref1, new LoadID("load1"), Instant.ofEpochMilli(10000))
				.build();
		when(storage.getNamespace(new NamespaceID("ns1"))).thenReturn(ns1);
		
		final MinHashSketchDatabase ref2 = new MinHashSketchDatabase(
				new MinHashSketchDBName("ns2"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH2),
				4000);
		final Namespace ns2 = Namespace.getBuilder(
				new NamespaceID("ns2"), ref2, new LoadID("load2"), Instant.ofEpochMilli(20000))
				.build();
		when(storage.getNamespace(new NamespaceID("ns2"))).thenReturn(ns2);
		
		when(fac.getImplementation(Paths.get("temp_dir"), 5)).thenReturn(mash);
		
		final MinHashSketchDatabase query = new MinHashSketchDatabase(
				new MinHashSketchDBName("<query>"),
				new MinHashImplementationName("mash"),
				queryParams,
				new MinHashDBLocation(EMPTY_FILE_MSH3),
				1);
		when(mash.getDatabase(
				new MinHashSketchDBName("<query>"), new MinHashDBLocation(EMPTY_FILE_MSH)))
				.thenReturn(query);
		
		when(mash.computeDistance(query, set(ref1, ref2), expectedReturnSize, strict))
				.thenReturn(new MinHashDistanceSet(
						set(
								new MinHashDistance(new MinHashSketchDBName("ns1"), "seq1", 0.1),
								new MinHashDistance(new MinHashSketchDBName("ns2"), "seq2", 0.2),
								new MinHashDistance(new MinHashSketchDBName("ns1"), "seq5", 0.4)),
						Collections.emptyList())); // minhash warnings are ignored
		
		when(storage.getSequenceMetadata(
				new NamespaceID("ns1"), new LoadID("load1"), Arrays.asList("seq1", "seq5")))
				.thenReturn(Arrays.asList(
						SequenceMetadata.getBuilder("seq1", "ss1", Instant.ofEpochMilli(10000))
								.build(),
						SequenceMetadata.getBuilder("seq5", "ss5", Instant.ofEpochMilli(50000))
								.build()));
		
		when(storage.getSequenceMetadata(
				new NamespaceID("ns2"), new LoadID("load2"), Arrays.asList("seq2")))
				.thenReturn(Arrays.asList(
						SequenceMetadata.getBuilder("seq2", "ss2", Instant.ofEpochMilli(20000))
								.build()));
		
		when(mash.getImplementationInformation()).thenReturn(new MinHashImplementationInformation(
				new MinHashImplementationName("mash"), "2.0", Paths.get("msh")));

		final SequenceMatches res = ah.measureDistance(
				set(new NamespaceID("ns1"), new NamespaceID("ns2")),
				EMPTY_FILE_MSH, // needs to exist or exception will be thrown. 
				returnSize,
				strict);
		
		final SequenceMatches expected = new SequenceMatches(
				set(ns1, ns2),
				new MinHashImplementationInformation(
						new MinHashImplementationName("mash"), "2.0", Paths.get("msh")),
				Arrays.asList(
						new SequenceDistanceAndMetadata(
								new NamespaceID("ns1"),
								new MinHashDistance(new MinHashSketchDBName("ns1"), "seq1", 0.1),
								SequenceMetadata.getBuilder(
										"seq1", "ss1", Instant.ofEpochMilli(10000)).build()),
						new SequenceDistanceAndMetadata(
								new NamespaceID("ns2"),
								new MinHashDistance(new MinHashSketchDBName("ns2"), "seq2", 0.2),
								SequenceMetadata.getBuilder(
										"seq2", "ss2", Instant.ofEpochMilli(20000)).build()),
						new SequenceDistanceAndMetadata(
								new NamespaceID("ns1"),
								new MinHashDistance(new MinHashSketchDBName("ns1"), "seq5", 0.4),
								SequenceMetadata.getBuilder(
										"seq5", "ss5", Instant.ofEpochMilli(50000)).build())
						),
				expectedWarnings);
		
		assertThat("incorrect matches", res, is(expected));
	}
	
	@Test
	public void measureDistanceFailNullsAndEmpties() throws Exception {
		final AssemblyHomologyStorage storage = mock(AssemblyHomologyStorage.class);
		
		final AssemblyHomology ah = new AssemblyHomology(
				storage, Collections.emptyList(), Paths.get("temp_dir"), 5);
		
		final Path p = Paths.get("query");
		final Set<NamespaceID> i = set(new NamespaceID("n"));
		
		failMeasureDistance(ah, null, p, true, new NullPointerException("namespaceIDs"));
		failMeasureDistance(ah, set(new NamespaceID("n"), null), p, true,
				new NullPointerException("Null item in collection namespaceIDs"));
		failMeasureDistance(ah, set(), p, true, new IllegalArgumentException(
				"No namespace IDs provided"));
		failMeasureDistance(ah, i, null, true, new NullPointerException("sketchDB"));
	}
	
	@Test
	public void measureDistanceFailNoNamespace() throws Exception {
		final AssemblyHomologyStorage storage = mock(AssemblyHomologyStorage.class);
		
		final AssemblyHomology ah = new AssemblyHomology(
				storage, Collections.emptyList(), Paths.get("temp_dir"), 5);
		
		when(storage.getNamespace(new NamespaceID("foo")))
				.thenThrow(new NoSuchNamespaceException("foo"));
		
		failMeasureDistance(ah, set(new NamespaceID("foo")), Paths.get("foo"), true,
				new NoSuchNamespaceException("foo"));
	}
	
	@Test
	public void measureDistanceFailIncompatibleNamespaces() throws Exception {
		final AssemblyHomologyStorage storage = mock(AssemblyHomologyStorage.class);
		
		final AssemblyHomology ah = new AssemblyHomology(
				storage, Collections.emptyList(), Paths.get("temp_dir"), 5);
		
		final MinHashSketchDatabase ref1 = new MinHashSketchDatabase(
				new MinHashSketchDBName("ns1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH),
				2000);
		final Namespace ns1 = Namespace.getBuilder(
				new NamespaceID("ns1"), ref1, new LoadID("load1"), Instant.ofEpochMilli(10000))
				.build();
		when(storage.getNamespace(new NamespaceID("ns1"))).thenReturn(ns1);
		
		final MinHashSketchDatabase ref2 = new MinHashSketchDatabase(
				new MinHashSketchDBName("ns2"),
				new MinHashImplementationName("sourmash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH2),
				4000);
		final Namespace ns2 = Namespace.getBuilder(
				new NamespaceID("ns2"), ref2, new LoadID("load2"), Instant.ofEpochMilli(20000))
				.build();
		when(storage.getNamespace(new NamespaceID("ns2"))).thenReturn(ns2);
		
		failMeasureDistance(ah, set(new NamespaceID("ns1"), new NamespaceID("ns2")),
				Paths.get("foo"), true, new IncompatibleNamespacesException(
						"The selected namespaces must share the same implementation"));
	}

	@Test
	public void measureDistanceFailGetImplementationFactory() throws Exception {
		final AssemblyHomologyStorage storage = mock(AssemblyHomologyStorage.class);
		
		final AssemblyHomology ah = new AssemblyHomology(
				storage, Collections.emptyList(), Paths.get("temp_dir"), 5);
		
		final MinHashSketchDatabase ref1 = new MinHashSketchDatabase(
				new MinHashSketchDBName("ns1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH),
				2000);
		final Namespace ns1 = Namespace.getBuilder(
				new NamespaceID("ns1"), ref1, new LoadID("load1"), Instant.ofEpochMilli(10000))
				.build();
		when(storage.getNamespace(new NamespaceID("ns1"))).thenReturn(ns1);
		
		failMeasureDistance(ah, set(new NamespaceID("ns1")), EMPTY_FILE_MSH2, true,
				new IllegalStateException("Application is misconfigured. Implementation " +
						"mash stored in database but not available."));
	}
	
	@Test
	public void measureDistanceFailBuildImplementation() throws Exception {
		final AssemblyHomologyStorage storage = mock(AssemblyHomologyStorage.class);
		final MinHashImplementationFactory fac = mock(MinHashImplementationFactory.class);
		
		when(fac.getImplementationName()).thenReturn(new MinHashImplementationName("mash"));
		
		final AssemblyHomology ah = new AssemblyHomology(
				storage, Arrays.asList(fac), Paths.get("temp_dir"), 600);
		
		final MinHashSketchDatabase ref1 = new MinHashSketchDatabase(
				new MinHashSketchDBName("ns1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH),
				2000);
		final Namespace ns1 = Namespace.getBuilder(
				new NamespaceID("ns1"), ref1, new LoadID("load1"), Instant.ofEpochMilli(10000))
				.build();
		when(storage.getNamespace(new NamespaceID("ns1"))).thenReturn(ns1);
		
		when(fac.getImplementation(Paths.get("temp_dir"), 600))
				.thenThrow(new MinHashInitException("aw crap"));
		
		failMeasureDistance(ah, set(new NamespaceID("ns1")), EMPTY_FILE_MSH2, true,
				new IllegalStateException("Application is misconfigured. Error attempting " +
						"to build the mash MinHash implementation."));
	}
	
	@Test
	public void measureDistanceFailNotSketch() throws Exception {
		final AssemblyHomologyStorage storage = mock(AssemblyHomologyStorage.class);
		final MinHashImplementationFactory fac = mock(MinHashImplementationFactory.class);
		final MinHashImplementation impl = mock(MinHashImplementation.class);
		
		when(fac.getImplementationName()).thenReturn(new MinHashImplementationName("mash"));
		
		final AssemblyHomology ah = new AssemblyHomology(
				storage, Arrays.asList(fac), Paths.get("temp_dir"), 1);
		
		final MinHashSketchDatabase ref1 = new MinHashSketchDatabase(
				new MinHashSketchDBName("ns1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH),
				2000);
		final Namespace ns1 = Namespace.getBuilder(
				new NamespaceID("ns1"), ref1, new LoadID("load1"), Instant.ofEpochMilli(10000))
				.build();
		when(storage.getNamespace(new NamespaceID("ns1"))).thenReturn(ns1);
		
		when(fac.getImplementation(Paths.get("temp_dir"), 1)).thenReturn(impl);
		
		when(impl.getDatabase(
				new MinHashSketchDBName("<query>"), new MinHashDBLocation(EMPTY_FILE_MSH2)))
				.thenThrow(new NotASketchException("foo"));
		
		failMeasureDistance(ah, set(new NamespaceID("ns1")), EMPTY_FILE_MSH2, true,
				new InvalidSketchException("The input sketch is not a valid sketch."));
		
		assertThat("unexpected logging", logEvents, is(Collections.emptyList()));
	}
	
	@Test
	public void measureDistanceFailNotSketchWithErr() throws Exception {
		final AssemblyHomologyStorage storage = mock(AssemblyHomologyStorage.class);
		final MinHashImplementationFactory fac = mock(MinHashImplementationFactory.class);
		final MinHashImplementation impl = mock(MinHashImplementation.class);
		
		when(fac.getImplementationName()).thenReturn(new MinHashImplementationName("mash"));
		
		final AssemblyHomology ah = new AssemblyHomology(
				storage, Arrays.asList(fac), Paths.get("temp_dir"), 42);
		
		final MinHashSketchDatabase ref1 = new MinHashSketchDatabase(
				new MinHashSketchDBName("ns1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH),
				2000);
		final Namespace ns1 = Namespace.getBuilder(
				new NamespaceID("ns1"), ref1, new LoadID("load1"), Instant.ofEpochMilli(10000))
				.build();
		when(storage.getNamespace(new NamespaceID("ns1"))).thenReturn(ns1);
		
		when(fac.getImplementation(Paths.get("temp_dir"), 42)).thenReturn(impl);
		
		when(impl.getDatabase(
				new MinHashSketchDBName("<query>"), new MinHashDBLocation(EMPTY_FILE_MSH2)))
				.thenThrow(new NotASketchException("foo", "stderr output"));
		
		failMeasureDistance(ah, set(new NamespaceID("ns1")), EMPTY_FILE_MSH2, true,
				new InvalidSketchException("The input sketch is not a valid sketch."));
		
		assertLogEventsCorrect(logEvents, new LogEvent(Level.ERROR,
				"minhash implementation stderr:\nstderr output", AssemblyHomology.class));
	}
	
	@Test
	public void measureDistanceFailLoadQuery() throws Exception {
		final AssemblyHomologyStorage storage = mock(AssemblyHomologyStorage.class);
		final MinHashImplementationFactory fac = mock(MinHashImplementationFactory.class);
		final MinHashImplementation impl = mock(MinHashImplementation.class);
		
		when(fac.getImplementationName()).thenReturn(new MinHashImplementationName("mash"));
		
		final AssemblyHomology ah = new AssemblyHomology(
				storage, Arrays.asList(fac), Paths.get("temp_dir"), 6022);
		
		final MinHashSketchDatabase ref1 = new MinHashSketchDatabase(
				new MinHashSketchDBName("ns1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH),
				2000);
		final Namespace ns1 = Namespace.getBuilder(
				new NamespaceID("ns1"), ref1, new LoadID("load1"), Instant.ofEpochMilli(10000))
				.build();
		when(storage.getNamespace(new NamespaceID("ns1"))).thenReturn(ns1);
		
		when(fac.getImplementation(Paths.get("temp_dir"), 6022)).thenReturn(impl);
		
		when(impl.getDatabase(
				new MinHashSketchDBName("<query>"), new MinHashDBLocation(EMPTY_FILE_MSH2)))
				.thenThrow(new MinHashException("some error"));
		
		failMeasureDistance(ah, set(new NamespaceID("ns1")), EMPTY_FILE_MSH2, true,
				new IllegalStateException("Error loading query sketch database: some error"));
	}
	
	@Test
	public void measureDistanceFailOneQuerySequence() throws Exception {
		final AssemblyHomologyStorage storage = mock(AssemblyHomologyStorage.class);
		final MinHashImplementationFactory fac = mock(MinHashImplementationFactory.class);
		final MinHashImplementation impl = mock(MinHashImplementation.class);
		
		when(fac.getImplementationName()).thenReturn(new MinHashImplementationName("mash"));
		
		final AssemblyHomology ah = new AssemblyHomology(
				storage, Arrays.asList(fac), Paths.get("temp_dir"), 31415);
		
		final MinHashSketchDatabase ref1 = new MinHashSketchDatabase(
				new MinHashSketchDBName("ns1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH),
				2000);
		final Namespace ns1 = Namespace.getBuilder(
				new NamespaceID("ns1"), ref1, new LoadID("load1"), Instant.ofEpochMilli(10000))
				.build();
		when(storage.getNamespace(new NamespaceID("ns1"))).thenReturn(ns1);
		
		when(fac.getImplementation(Paths.get("temp_dir"), 31415)).thenReturn(impl);
		
		when(impl.getDatabase(
				new MinHashSketchDBName("<query>"), new MinHashDBLocation(EMPTY_FILE_MSH2)))
				.thenReturn(new MinHashSketchDatabase(
						new MinHashSketchDBName("<query>"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH2),
						2));
		
		failMeasureDistance(ah, set(new NamespaceID("ns1")), EMPTY_FILE_MSH2, true,
				new InvalidSketchException("Query sketch database must have exactly one sketch"));
	}
	
	@Test
	public void measureDistanceFailIncompatibleSketchStrict() throws Exception {
		final AssemblyHomologyStorage storage = mock(AssemblyHomologyStorage.class);
		final MinHashImplementationFactory fac = mock(MinHashImplementationFactory.class);
		final MinHashImplementation impl = mock(MinHashImplementation.class);
		
		when(fac.getImplementationName()).thenReturn(new MinHashImplementationName("mash"));
		
		final AssemblyHomology ah = new AssemblyHomology(
				storage, Arrays.asList(fac), Paths.get("temp_dir"), 6626);
		
		final MinHashSketchDatabase ref1 = new MinHashSketchDatabase(
				new MinHashSketchDBName("ns1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH),
				2000);
		final Namespace ns1 = Namespace.getBuilder(
				new NamespaceID("ns1"), ref1, new LoadID("load1"), Instant.ofEpochMilli(10000))
				.build();
		when(storage.getNamespace(new NamespaceID("ns1"))).thenReturn(ns1);
		
		when(fac.getImplementation(Paths.get("temp_dir"), 6626)).thenReturn(impl);
		
		when(impl.getDatabase(
				new MinHashSketchDBName("<query>"), new MinHashDBLocation(EMPTY_FILE_MSH2)))
				.thenReturn(new MinHashSketchDatabase(
						new MinHashSketchDBName("<query>"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(31).withSketchSize(1500).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH2),
						1));
		
		failMeasureDistance(ah, set(new NamespaceID("ns1")), EMPTY_FILE_MSH2, true,
				new IncompatibleSketchesException("Unable to query namespace ns1 with input " +
						"sketch: Query sketch size 1500 does not match target 1000"));
//						"sketch: Kmer size for sketches are not compatible: 31 21"));
	}
	
	@Test
	public void measureDistanceFailIncompatibleSketchNonStrict() throws Exception {
		final AssemblyHomologyStorage storage = mock(AssemblyHomologyStorage.class);
		final MinHashImplementationFactory fac = mock(MinHashImplementationFactory.class);
		final MinHashImplementation impl = mock(MinHashImplementation.class);
		
		when(fac.getImplementationName()).thenReturn(new MinHashImplementationName("mash"));
		
		final AssemblyHomology ah = new AssemblyHomology(
				storage, Arrays.asList(fac), Paths.get("temp_dir"), 299792);
		
		final MinHashSketchDatabase ref1 = new MinHashSketchDatabase(
				new MinHashSketchDBName("ns1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH),
				2000);
		final Namespace ns1 = Namespace.getBuilder(
				new NamespaceID("ns1"), ref1, new LoadID("load1"), Instant.ofEpochMilli(10000))
				.build();
		when(storage.getNamespace(new NamespaceID("ns1"))).thenReturn(ns1);
		
		when(fac.getImplementation(Paths.get("temp_dir"), 299792)).thenReturn(impl);
		
		when(impl.getDatabase(
				new MinHashSketchDBName("<query>"), new MinHashDBLocation(EMPTY_FILE_MSH2)))
				.thenReturn(new MinHashSketchDatabase(
						new MinHashSketchDBName("<query>"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(21).withSketchSize(1500).build(),
						new MinHashDBLocation(EMPTY_FILE_MSH2),
						1));
		
		failMeasureDistance(ah, set(new NamespaceID("ns1")), EMPTY_FILE_MSH2, false,
				new IncompatibleSketchesException("Unable to query namespace ns1 with input " +
						"sketch: Kmer size for sketches are not compatible: 31 21"));
	}
	
	@Test
	public void measureDistanceFailComputeDistance() throws Exception {
		final AssemblyHomologyStorage storage = mock(AssemblyHomologyStorage.class);
		final MinHashImplementationFactory fac = mock(MinHashImplementationFactory.class);
		final MinHashImplementation impl = mock(MinHashImplementation.class);
		
		when(fac.getImplementationName()).thenReturn(new MinHashImplementationName("mash"));
		
		final AssemblyHomology ah = new AssemblyHomology(
				storage, Arrays.asList(fac), Paths.get("temp_dir"), 6674);
		
		final MinHashSketchDatabase ref1 = new MinHashSketchDatabase(
				new MinHashSketchDBName("ns1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH),
				2000);
		final Namespace ns1 = Namespace.getBuilder(
				new NamespaceID("ns1"), ref1, new LoadID("load1"), Instant.ofEpochMilli(10000))
				.build();
		when(storage.getNamespace(new NamespaceID("ns1"))).thenReturn(ns1);
		
		when(fac.getImplementation(Paths.get("temp_dir"), 6674)).thenReturn(impl);
		
		final MinHashSketchDatabase query = new MinHashSketchDatabase(
				new MinHashSketchDBName("<query>"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH2),
				1);
		when(impl.getDatabase(
				new MinHashSketchDBName("<query>"), new MinHashDBLocation(EMPTY_FILE_MSH2)))
				.thenReturn(query);
		
		when(impl.computeDistance(query, set(ref1), 1, true))
				.thenThrow(new MinHashException("he must have died while carving it"));
		
		when(impl.getImplementationInformation()).thenReturn(new MinHashImplementationInformation(
				new MinHashImplementationName("mash"), "2.0", Paths.get("msh")));
		
		failMeasureDistance(ah, set(new NamespaceID("ns1")), EMPTY_FILE_MSH2, true,
				new IllegalStateException("Unexpected error running MinHash implementation mash"));
	}
	
	@Test
	public void measureDistanceFailGetMeta() throws Exception {
		final AssemblyHomologyStorage storage = mock(AssemblyHomologyStorage.class);
		final MinHashImplementationFactory fac = mock(MinHashImplementationFactory.class);
		final MinHashImplementation impl = mock(MinHashImplementation.class);
		
		when(fac.getImplementationName()).thenReturn(new MinHashImplementationName("mash"));
		
		final AssemblyHomology ah = new AssemblyHomology(
				storage, Arrays.asList(fac), Paths.get("temp_dir"), 1602);
		
		final MinHashSketchDatabase ref1 = new MinHashSketchDatabase(
				new MinHashSketchDBName("ns1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH),
				2000);
		final Namespace ns1 = Namespace.getBuilder(
				new NamespaceID("ns1"), ref1, new LoadID("load1"), Instant.ofEpochMilli(10000))
				.build();
		when(storage.getNamespace(new NamespaceID("ns1"))).thenReturn(ns1);
		
		when(fac.getImplementation(Paths.get("temp_dir"), 1602)).thenReturn(impl);
		
		final MinHashSketchDatabase query = new MinHashSketchDatabase(
				new MinHashSketchDBName("<query>"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				new MinHashDBLocation(EMPTY_FILE_MSH2),
				1);
		when(impl.getDatabase(
				new MinHashSketchDBName("<query>"), new MinHashDBLocation(EMPTY_FILE_MSH2)))
				.thenReturn(query);
		
		when(impl.computeDistance(query, set(ref1), 1, true))
				.thenReturn(new MinHashDistanceSet(
						set(
								new MinHashDistance(new MinHashSketchDBName("ns1"), "seq1", 0.1),
								new MinHashDistance(new MinHashSketchDBName("ns1"), "seq5", 0.4)),
						Collections.emptyList())); // minhash warnings are ignored

		when(storage.getSequenceMetadata(
				new NamespaceID("ns1"), new LoadID("load1"), Arrays.asList("seq1", "seq5")))
				.thenThrow(new NoSuchSequenceException("seq1 seq5"));
		
		failMeasureDistance(ah, set(new NamespaceID("ns1")), EMPTY_FILE_MSH2, true,
				new IllegalStateException("Database is corrupt. Unable to find sequences " +
						"from sketch file for namespace ns1: 50010 No such sequence: seq1 seq5"));
	}

	private void failMeasureDistance(
			final AssemblyHomology ah,
			final Set<NamespaceID> namespaceIDs,
			final Path query,
			final boolean strict,
			final Exception expected) {
		try {
			ah.measureDistance(namespaceIDs, query, 1, strict);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}

