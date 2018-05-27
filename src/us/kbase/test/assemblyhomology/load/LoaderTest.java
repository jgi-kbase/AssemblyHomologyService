package us.kbase.test.assemblyhomology.load;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.InOrder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import us.kbase.assemblyhomology.core.DataSourceID;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.assemblyhomology.load.Loader;
import us.kbase.assemblyhomology.load.exceptions.LoadInputParseException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.storage.AssemblyHomologyStorage;
import us.kbase.assemblyhomology.util.Restreamable;
import us.kbase.test.assemblyhomology.TestCommon;

public class LoaderTest {
	
	private static final ObjectMapper OM = new ObjectMapper();
	
	// mock db location to avoid creating files

	private static class TestSet {
		private final Loader loader;
		private final AssemblyHomologyStorage storageMock;
		private final Clock clockMock;
		
		private TestSet(
				final Loader loader,
				final AssemblyHomologyStorage storageMock,
				final Clock clockMock) {
			this.loader = loader;
			this.storageMock = storageMock;
			this.clockMock = clockMock;
		}
	}
	
	private TestSet getTestClasses() throws Exception {
		final AssemblyHomologyStorage storage = mock(AssemblyHomologyStorage.class);
		final Clock clock = mock(Clock.class);
		final Constructor<Loader> con = Loader.class.getDeclaredConstructor(
				AssemblyHomologyStorage.class, Clock.class);
		con.setAccessible(true);
		final Loader loader = con.newInstance(storage, clock);
		return new TestSet(loader, storage, clock);
	}
	
	private static class StringRestreamable implements Restreamable {

		private final String source;
		private final String sourceInfo;
		
		private StringRestreamable(final String source, final String sourceInfo) {
			this.source = source;
			this.sourceInfo = sourceInfo;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(source.getBytes());
		}

		@Override
		public String getSourceInfo() {
			return sourceInfo;
		}
	}
	
	private Restreamable toRes(
			final List<Map<String, Object>> seqJSONLines,
			final String sourceInfo)
			throws Exception {
		final StringBuilder sb = new StringBuilder();
		for (final Map<String, Object> j: seqJSONLines) {
			sb.append(OM.writeValueAsString(j) + "\n");
		}
		return new StringRestreamable(sb.toString(), sourceInfo);
	}
	
	@Test
	public void constructFail() {
		try {
			new Loader(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("storage"));
		}
	}
	
	@Test
	public void loadMinimalNamespace() throws Exception {
		final String nameSpaceYAML = "id: id1\ndatasource: JGI";
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("id1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(3).withScaling(4).build(),
				loc,
				2);
		final Namespace ns = Namespace.getBuilder(
				new NamespaceID("id1"), db, new LoadID("load 1"), Instant.ofEpochMilli(20000))
				.withNullableDataSourceID(new DataSourceID("JGI"))
				.build();
		loadNamespace(nameSpaceYAML, loc, db, ns);
	}
	
	@Test
	public void loadMaximallNamespace() throws Exception {
		final String nameSpaceYAML =
				"id: id1\ndatasource: JGI\nsourcedatabase: IMG\ndescription: desc";
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("id1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(3).withScaling(4).build(),
				loc,
				2);
		final Namespace ns = Namespace.getBuilder(
				new NamespaceID("id1"), db, new LoadID("load 1"), Instant.ofEpochMilli(20000))
				.withNullableDataSourceID(new DataSourceID("JGI"))
				.withNullableDescription("desc")
				.withNullableSourceDatabaseID("IMG")
				.build();
		loadNamespace(nameSpaceYAML, loc, db, ns);
	}

	private void loadNamespace(
			final String nameSpaceYAML,
			final MinHashDBLocation loc,
			final MinHashSketchDatabase db,
			final Namespace ns)
			throws Exception {
		final TestSet ts = getTestClasses();
		final AssemblyHomologyStorage storage = ts.storageMock;
		final Clock clock = ts.clockMock;
		final Loader loader = ts.loader;
		
		final MinHashImplementation impl = mock(MinHashImplementation.class);
		
		when(impl.getDatabase(new MinHashSketchDBName("id1"), loc)).thenReturn(db);
		when(impl.getSketchIDs(db)).thenReturn(Arrays.asList("seq1", "seq2"));
		when(clock.instant()).thenReturn(Instant.ofEpochMilli(10000), Instant.ofEpochMilli(20000));
		
		loader.load(
				new LoadID("load 1"),
				impl,
				loc,
				new StringRestreamable(nameSpaceYAML, "some file"),
				toRes(Arrays.asList(
						ImmutableMap.of(
								"id", "seq1",
								"sourceid", "source1"),
						ImmutableMap.of(
								"id", "seq2",
								"sourceid", "source2",
								"sciname", "sci name",
								"relatedids", ImmutableMap.of("foo", "bar"))),
						"some file"));
		
		verify(storage).saveSequenceMetadata(
				new NamespaceID("id1"),
				new LoadID("load 1"),
				Arrays.asList(
						SequenceMetadata.getBuilder(
								"seq1", "source1", Instant.ofEpochMilli(10000))
								.build(),
						SequenceMetadata.getBuilder(
								"seq2", "source2", Instant.ofEpochMilli(10000))
								.withNullableScientificName("sci name")
								.withRelatedID("foo", "bar")
								.build()));
		
		verify(storage).createOrReplaceNamespace(ns);
	}
	
	@Test
	public void load100() throws Exception {
		final TestSet ts = getTestClasses();
		final AssemblyHomologyStorage storage = ts.storageMock;
		final Clock clock = ts.clockMock;
		final Loader loader = ts.loader;
		
		final InOrder storageOrder = inOrder(storage);

		final MinHashImplementation impl = mock(MinHashImplementation.class);
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		
		final List<Map<String, Object>> seqIncJson = new LinkedList<>();
		
		for (int i = 1; i <= 100; i++) {
			seqIncJson.add(ImmutableMap.of("id", "seq" + i, "sourceid", "source" + i));
		}
		
		final List<String> seqIDs = seqIncJson.stream().map(m -> (String) m.get("id"))
				.collect(Collectors.toList());
		
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("id1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(3).withScaling(4).build(),
				loc,
				2);
		
		when(impl.getDatabase(new MinHashSketchDBName("id1"), loc)).thenReturn(db);
		when(impl.getSketchIDs(db)).thenReturn(seqIDs);
		when(clock.instant()).thenReturn(
				Instant.ofEpochMilli(10000),
				Instant.ofEpochMilli(40000));
		
		loader.load(
				new LoadID("load 1"),
				impl,
				loc,
				new StringRestreamable("id: id1\ndatasource: JGI", "some file"),
				toRes(seqIncJson, "some file"));
		
		storageOrder.verify(storage).saveSequenceMetadata(
				new NamespaceID("id1"),
				new LoadID("load 1"),
				toSeqMeta(seqIncJson, Instant.ofEpochMilli(10000)));
		
		verify(storage).createOrReplaceNamespace(Namespace.getBuilder(
				new NamespaceID("id1"), db, new LoadID("load 1"), Instant.ofEpochMilli(40000))
				.withNullableDataSourceID(new DataSourceID("JGI"))
				.build());
	}
	
	@Test
	public void loadMany() throws Exception {
		final TestSet ts = getTestClasses();
		final AssemblyHomologyStorage storage = ts.storageMock;
		final Clock clock = ts.clockMock;
		final Loader loader = ts.loader;
		
		final InOrder storageOrder = inOrder(storage);

		final MinHashImplementation impl = mock(MinHashImplementation.class);
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		
		final List<Map<String, Object>> seqIncJson = new LinkedList<>();
		
		for (int i = 1; i <= 210; i++) {
			seqIncJson.add(ImmutableMap.of("id", "seq" + i, "sourceid", "source" + i));
		}
		
		final List<String> seqIDs = seqIncJson.stream().map(m -> (String) m.get("id"))
				.collect(Collectors.toList());
		
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("id1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(3).withScaling(4).build(),
				loc,
				2);
		
		when(impl.getDatabase(new MinHashSketchDBName("id1"), loc)).thenReturn(db);
		when(impl.getSketchIDs(db)).thenReturn(seqIDs);
		when(clock.instant()).thenReturn(
				Instant.ofEpochMilli(10000),
				Instant.ofEpochMilli(20000),
				Instant.ofEpochMilli(30000),
				Instant.ofEpochMilli(40000));
		
		loader.load(
				new LoadID("load 1"),
				impl,
				loc,
				new StringRestreamable("id: id1\ndatasource: JGI", "some file"),
				toRes(seqIncJson, "some file"));
		
		storageOrder.verify(storage).saveSequenceMetadata(
				new NamespaceID("id1"),
				new LoadID("load 1"),
				toSeqMeta(seqIncJson.subList(0, 100), Instant.ofEpochMilli(10000)));
		
		storageOrder.verify(storage).saveSequenceMetadata(
				new NamespaceID("id1"),
				new LoadID("load 1"),
				toSeqMeta(seqIncJson.subList(100, 200), Instant.ofEpochMilli(20000)));
		
		storageOrder.verify(storage).saveSequenceMetadata(
				new NamespaceID("id1"),
				new LoadID("load 1"),
				toSeqMeta(seqIncJson.subList(200, 210), Instant.ofEpochMilli(30000)));
		
		
		verify(storage).createOrReplaceNamespace(Namespace.getBuilder(
				new NamespaceID("id1"), db, new LoadID("load 1"), Instant.ofEpochMilli(40000))
				.withNullableDataSourceID(new DataSourceID("JGI"))
				.build());
	}

	private List<SequenceMetadata> toSeqMeta(
			final List<Map<String, Object>> seqInc,
			final Instant creation) {
		final List<SequenceMetadata> ret = new LinkedList<>();
		for (final Map<String, Object> s: seqInc) {
			ret.add(SequenceMetadata.getBuilder(
					(String) s.get("id"),
					(String) s.get("sourceid"),
					creation)
					.build());
		}
		return ret;
	}
	
	@Test
	public void loadFailNulls() throws Exception {
		final Loader l = getTestClasses().loader;
		final LoadID i = new LoadID("l");
		final MinHashImplementation m = mock(MinHashImplementation.class);
		final MinHashDBLocation d = mock(MinHashDBLocation.class);
		final Restreamable n = mock(Restreamable.class);
		final Restreamable s = mock(Restreamable.class);
		
		failLoad(l, null, m, d, n, s, new NullPointerException("loadID"));
		failLoad(l, i, null, d, n, s, new NullPointerException("minhashImpl"));
		failLoad(l, i, m, null, n, s, new NullPointerException("sketchDBlocation"));
		failLoad(l, i, m, d, null, s, new NullPointerException("namespaceYAML"));
		failLoad(l, i, m, d, n, null, new NullPointerException("sequenceMetaJSONLines"));
	}
	
	@Test
	public void loadFailBadNamespace() throws Exception {
		final Loader l = getTestClasses().loader;
		final LoadID i = new LoadID("l");
		final MinHashImplementation m = mock(MinHashImplementation.class);
		final MinHashDBLocation d = mock(MinHashDBLocation.class);
		final Restreamable s = mock(Restreamable.class);
		
		final Restreamable nsYAML = new StringRestreamable("foo\nid: bar", " some file");
		
		failLoad(l, i, m, d, nsYAML, s, new LoadInputParseException(
				"Error parsing source  some file: class " +
				"org.yaml.snakeyaml.scanner.ScannerException mapping values are not " +
				"allowed here\n in 'reader', line 2, column 3:\n    id: bar\n      ^\n"));
	}
	
	@Test
	public void loadFailBadSeqJSON() throws Exception {
		final Loader loader = getTestClasses().loader;
		final LoadID loadID = new LoadID("l");
		final MinHashImplementation impl = mock(MinHashImplementation.class);
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		
		final Restreamable nsYAML = new StringRestreamable(
				"id: id1\ndatasource: baz", "some file");
		final Restreamable seqJSON = toRes(Arrays.asList(
				ImmutableMap.of("id", "id1", "sourceid", "sid1"),
				ImmutableMap.of("id", "id2", "sourceid", "sid2"),
				ImmutableMap.of("id", "id3")),
				"some other file");
		
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("id1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(3).withScaling(4).build(),
				loc,
				2);
		
		when(impl.getDatabase(new MinHashSketchDBName("id1"), loc)).thenReturn(db);
		
		failLoad(loader, loadID, impl, loc, nsYAML, seqJSON, new LoadInputParseException(
				"Missing value at sourceid. Source: some other file line 3"));
	}
	
	@Test
	public void loadFailExtraSeqIDs() throws Exception {
		final Loader loader = getTestClasses().loader;
		final LoadID loadID = new LoadID("l");
		final MinHashImplementation impl = mock(MinHashImplementation.class);
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		
		final Restreamable nsYAML = new StringRestreamable(
				"id: id1\ndatasource: baz", "some file");
		final Restreamable seqJSON = toRes(Arrays.asList(
				ImmutableMap.of("id", "id1", "sourceid", "sid1"),
				ImmutableMap.of("id", "id2", "sourceid", "sid2"),
				ImmutableMap.of("id", "id3", "sourceid", "sid3"),
				ImmutableMap.of("id", "id4", "sourceid", "sid4"),
				ImmutableMap.of("id", "id5", "sourceid", "sid5")),
				"some other file");
		
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("id1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(3).withScaling(4).build(),
				loc,
				2);
		
		when(impl.getDatabase(new MinHashSketchDBName("id1"), loc)).thenReturn(db);
		when(impl.getSketchIDs(db)).thenReturn(Arrays.asList("id2", "id6", "id7", "id8"));
		
		failLoad(loader, loadID, impl, loc, nsYAML, seqJSON, new LoadInputParseException(
				"IDs in the sketch database and sequence metadata file don't match. " +
				"For example, some other file has extra IDs [id1, id3, id4]"));
	}
	
	@Test
	public void loadFailExtraSketchIDsWithSourceInfo() throws Exception {
		final Optional<Path> sourceInfo = Optional.of(Paths.get("yet another file"));
		final LoadInputParseException expected = new LoadInputParseException(
				"IDs in the sketch database and sequence metadata file don't match. " +
				"For example, yet another file has extra IDs [id2, id6, id7]");
		loadFailExtraSketchIDs(sourceInfo, expected);
	}
	
	@Test
	public void loadFailExtraSketchIDsWithoutSourceInfo() throws Exception {
		final Optional<Path> sourceInfo = Optional.absent();
		final LoadInputParseException expected = new LoadInputParseException(
				"IDs in the sketch database and sequence metadata file don't match. " +
				"For example, the sketch database has extra IDs [id2, id6, id7]");
		loadFailExtraSketchIDs(sourceInfo, expected);
	}

	private void loadFailExtraSketchIDs(
			final Optional<Path> sourceInfo,
			final LoadInputParseException expected)
			throws Exception {
		final Loader loader = getTestClasses().loader;
		final LoadID loadID = new LoadID("l");
		final MinHashImplementation impl = mock(MinHashImplementation.class);
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		
		final Restreamable nsYAML = new StringRestreamable(
				"id: id1\ndatasource: baz", "some file");
		final Restreamable seqJSON = toRes(Arrays.asList(
				ImmutableMap.of("id", "id1", "sourceid", "sid1"),
				ImmutableMap.of("id", "id5", "sourceid", "sid5")),
				"some other file");
		
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("id1"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(3).withScaling(4).build(),
				loc,
				2);
		
		when(impl.getDatabase(new MinHashSketchDBName("id1"), loc)).thenReturn(db);
		when(impl.getSketchIDs(db)).thenReturn(Arrays.asList(
				"id1", "id2","id5", "id6", "id7", "id8"));
		when(loc.getPathToFile()).thenReturn(sourceInfo);
		
		failLoad(loader, loadID, impl, loc, nsYAML, seqJSON, expected);
	}
	
	private void failLoad(
			final Loader loader,
			final LoadID loadID,
			final MinHashImplementation minhashImpl,
			final MinHashDBLocation sketchDBlocation,
			final Restreamable namespaceYAML,
			final Restreamable sequenceMetaJSONLines,
			final Exception expected) {
		try {
			loader.load(
					loadID, minhashImpl, sketchDBlocation, namespaceYAML, sequenceMetaJSONLines);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
