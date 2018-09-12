package us.kbase.test.assemblyhomology.service.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static us.kbase.test.assemblyhomology.TestCommon.set;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import us.kbase.assemblyhomology.config.AssemblyHomologyConfig;
import us.kbase.assemblyhomology.core.AssemblyHomology;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.SequenceMatches;
import us.kbase.assemblyhomology.core.SequenceMatches.SequenceDistanceAndMetadata;
import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.assemblyhomology.core.Token;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.IncompatibleNamespacesException;
import us.kbase.assemblyhomology.core.exceptions.InvalidSketchException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.service.api.Namespaces;
import us.kbase.test.assemblyhomology.MapBuilder;
import us.kbase.test.assemblyhomology.TestCommon;

public class NamespacesTest {

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
				Instant.ofEpochMilli(100000))
				.build();
			
			NS2 = Namespace.getBuilder(
					new NamespaceID("baz"),
					new MinHashSketchDatabase(
							new MinHashSketchDBName("baz"),
							new MinHashImplementationName("mash"),
							MinHashParameters.getBuilder(5).withSketchSize(10000).build(),
							mock(MinHashDBLocation.class),
							21),
					new LoadID("boo"),
					Instant.ofEpochMilli(300000))
					.withNullableDescription("some desc")
					.build();
		} catch (IllegalParameterException | MissingParameterException e) {
			throw new RuntimeException("Fix yer tests newb");
		}
	}
	private static final Map<String, Object> EXPECTED_NS1 = MapBuilder.<String, Object>newHashMap()
			.with("id", "foo")
			.with("impl", "mash")
			.with("sketchsize", null)
			.with("database", "default")
			.with("datasource", "KBase")
			.with("seqcount", 42)
			.with("kmersize", Arrays.asList(3))
			.with("scaling", 4)
			.with("desc", null)
			.with("lastmod", 100000L)
			.build();
	
	private static final Map<String, Object> EXPECTED_NS2 = MapBuilder.<String, Object>newHashMap()
			.with("id", "baz")
			.with("impl", "mash")
			.with("scaling", null)
			.with("database", "default")
			.with("datasource", "KBase")
			.with("seqcount", 21)
			.with("kmersize", Arrays.asList(5))
			.with("sketchsize", 10000)
			.with("desc", "some desc")
			.with("lastmod", 300000L)
			.build();
	
	private static Path TEMP_DIR;
	
	@BeforeClass
	public static void setUp() throws Exception {
		final String pre = UUID.randomUUID().toString();
		TEMP_DIR = TestCommon.getTempDir().resolve("NamespacesTest_" + pre);
		Files.createDirectories(TEMP_DIR);
	}
	
	@AfterClass
	public static void breakDown() throws Exception {
		final boolean deleteTempFiles = TestCommon.isDeleteTempFiles();
		if (TEMP_DIR != null && Files.exists(TEMP_DIR) && deleteTempFiles) {
			FileUtils.deleteQuietly(TEMP_DIR.toFile());
		}
	}
	
	@After
	public void ensureNoTempFiles() throws Exception {
		final List<Path> files = Files.list(TEMP_DIR).collect(Collectors.toList());
		assertThat("test left temp files", files, is(Collections.emptyList()));
	}
	
	private Namespaces getNamespaceInstance(final AssemblyHomology ah) {
		final AssemblyHomologyConfig cfg = mock(AssemblyHomologyConfig.class);
		
		when(cfg.getPathToTemporaryFileDirectory()).thenReturn(TEMP_DIR);
		
		return new Namespaces(ah, cfg);
	}

	@Test
	public void getNamespaces() throws Exception {
		final AssemblyHomology ah = mock(AssemblyHomology.class);
		final Namespaces ns = getNamespaceInstance(ah);
		
		when(ah.getNamespaces()).thenReturn(set(NS1, NS2));
		
		assertThat("incorrect namespaces", ns.getNamespaces(),
				is(set(EXPECTED_NS1, EXPECTED_NS2)));
	}
	
	@Test
	public void getNamespace() throws Exception {
		final AssemblyHomology ah = mock(AssemblyHomology.class);
		final Namespaces ns = getNamespaceInstance(ah);

		when(ah.getNamespace(new NamespaceID("foo"))).thenReturn(NS1);
		when(ah.getNamespace(new NamespaceID("baz"))).thenReturn(NS2);
		
		assertThat("incorrect namespace", ns.getNamespace("foo"), is(EXPECTED_NS1));
		assertThat("incorrect namespace", ns.getNamespace("baz"), is(EXPECTED_NS2));
	}
	
	@Test
	public void getNamespaceFailBadID() {
		final AssemblyHomology ah = mock(AssemblyHomology.class);
		final Namespaces ns = getNamespaceInstance(ah);
		
		failGetNamespace(ns, null, new MissingParameterException("namespaceID"));
		failGetNamespace(ns, "    \t   \n  ", new MissingParameterException("namespaceID"));
		failGetNamespace(ns, "fooΔ", new IllegalParameterException(
				"Illegal character in namespace id fooΔ: Δ"));
	}
	
	@Test
	public void getNamespaceFailNoSuchNamespace() throws Exception {
		final AssemblyHomology ah = mock(AssemblyHomology.class);
		final Namespaces ns = getNamespaceInstance(ah);
		
		when(ah.getNamespace(new NamespaceID("bar")))
				.thenThrow(new NoSuchNamespaceException("bar"));
		
		failGetNamespace(ns, "bar", new NoSuchNamespaceException("bar"));
	}
	
	private void failGetNamespace(
			final Namespaces ns,
			final String namespace,
			final Exception expected) {
		try {
			ns.getNamespace(namespace);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	private static class ByteArrayServletInputStream extends ServletInputStream {

		private final ByteArrayInputStream input;
		
		public ByteArrayServletInputStream(final byte[] input) {
			this.input = new ByteArrayInputStream(input);
		}
		
		@Override
		public int read() throws IOException {
			return input.read();
		}
		
		@Override
		public int read(final byte[] b) throws IOException {
			return input.read(b);
		}
		
		@Override
		public int read(final byte[] b, final int off, final int len) {
			return input.read(b, off, len);
		}
		
	}
	
	private static class TempFileMatcher implements ArgumentMatcher<Path> {

		private String extension;
		private String contents;
		
		public TempFileMatcher(final String extension, final String contents) {
			this.extension = extension;
			this.contents = contents;
		}
		
		@Override
		public boolean matches(final Path tempFile) {
			if (!tempFile.toString().endsWith(extension)) {
				return false;
			}
			try {
				final String got = new String(Files.readAllBytes(tempFile));
				if (!contents.equals(got)) {
					return false;
				}
			} catch (IOException e) {
				e.printStackTrace();
				fail("got IOException, printed stack trace");
				throw new RuntimeException();
			}
			return true;
		}
		
	}
	
	@Test
	public void searchNoExtensionNoMaxNoWarningsNoNotStrict() throws Exception {
		final AssemblyHomology ah = mock(AssemblyHomology.class);
		final HttpServletRequest req = mock(HttpServletRequest.class);
		final Namespaces ns = getNamespaceInstance(ah);
		
		when(ah.getNamespaces(set(new NamespaceID("foo"), new NamespaceID("baz"))))
				.thenReturn(set(NS1, NS2));
		when(ah.getExpectedFileExtension(new MinHashImplementationName("mash")))
				.thenReturn(Optional.absent());
		
		when(req.getInputStream())
				.thenReturn(new ByteArrayServletInputStream("file content".getBytes()));
		
		when(ah.measureDistance(
				eq(set(new NamespaceID("foo"), new NamespaceID("baz"))),
				argThat(new TempFileMatcher(".tmp", "file content")),
				eq(-1),
				eq(true),
				isNull()))
				.thenReturn(new SequenceMatches(
						set(NS1, NS2),
						new MinHashImplementationInformation(
								new MinHashImplementationName("mash"), "2.0", Paths.get("msh")),
						Arrays.asList(
								new SequenceDistanceAndMetadata(
										new NamespaceID("foo"),
										new MinHashDistance(
												new MinHashSketchDBName("foo"), "s1", 0.1),
										SequenceMetadata.getBuilder(
												"s1", "ss1", Instant.ofEpochMilli(10000)).build()),
								new SequenceDistanceAndMetadata(
										new NamespaceID("baz"),
										new MinHashDistance(
												new MinHashSketchDBName("baz"), "s1", 0.2),
										SequenceMetadata.getBuilder(
												"s1", "ss1", Instant.ofEpochMilli(10000))
												.withRelatedID("id1", "castle")
												.withRelatedID("id2", "arrg")
												.build()),
								new SequenceDistanceAndMetadata(
										new NamespaceID("foo"),
										new MinHashDistance(
												new MinHashSketchDBName("foo"), "s2", 0.3),
										SequenceMetadata.getBuilder(
												"s2", "ss2", Instant.ofEpochMilli(10000))
												.withNullableScientificName("sci name")
												.build())
								),
						Collections.emptySet()));
		
		final Map<String, Object> ret = ns.searchNamespaces(
				req, null, "  foo ,   \tbaz  ", null, null);
		
		final Map<String, Object> expected = ImmutableMap.of(
				"impl", "mash",
				"implver", "2.0",
				"warnings", Collections.emptySet(),
				"namespaces", set(EXPECTED_NS1, EXPECTED_NS2),
				"distances", Arrays.asList(
						MapBuilder.<String, Object>newHashMap()
								.with("sourceid", "ss1")
								.with("sciname", null)
								.with("namespaceid", "foo")
								.with("dist", 0.1)
								.with("relatedids", Collections.emptyMap())
								.build(),
						MapBuilder.<String, Object>newHashMap()
								.with("sourceid", "ss1")
								.with("sciname", null)
								.with("namespaceid", "baz")
								.with("dist", 0.2)
								.with("relatedids",
										ImmutableMap.of("id1", "castle", "id2", "arrg"))
								.build(),
						MapBuilder.<String, Object>newHashMap()
								.with("sourceid", "ss2")
								.with("sciname", "sci name")
								.with("namespaceid", "foo")
								.with("dist", 0.3)
								.with("relatedids", Collections.emptyMap())
								.build()
						));
		
		assertThat("incorrect distances", ret, is(expected));
	}
	
	@Test
	public void searchWithExtensionMaxWarningsNotStrict() throws Exception {
		// also tests a whitespace only token
		final AssemblyHomology ah = mock(AssemblyHomology.class);
		final HttpServletRequest req = mock(HttpServletRequest.class);
		final Namespaces ns = getNamespaceInstance(ah);
		
		when(ah.getNamespaces(set(new NamespaceID("foo"), new NamespaceID("baz"))))
				.thenReturn(set(NS1, NS2));
		when(ah.getExpectedFileExtension(new MinHashImplementationName("mash")))
				.thenReturn(Optional.of(Paths.get("msh")));
		
		when(req.getInputStream())
				.thenReturn(new ByteArrayServletInputStream("file content".getBytes()));
		
		when(ah.measureDistance(
				eq(set(new NamespaceID("foo"), new NamespaceID("baz"))),
				argThat(new TempFileMatcher(".tmp.msh", "file content")),
				eq(7),
				eq(false),
				isNull()))
				.thenReturn(new SequenceMatches(
						set(NS1, NS2),
						new MinHashImplementationInformation(
								new MinHashImplementationName("mash"), "2.0", Paths.get("msh")),
						Arrays.asList(
								new SequenceDistanceAndMetadata(
										new NamespaceID("foo"),
										new MinHashDistance(
												new MinHashSketchDBName("foo"), "s1", 0.1),
										SequenceMetadata.getBuilder(
												"s1", "ss1", Instant.ofEpochMilli(10000)).build()),
								new SequenceDistanceAndMetadata(
										new NamespaceID("baz"),
										new MinHashDistance(
												new MinHashSketchDBName("baz"), "s1", 0.2),
										SequenceMetadata.getBuilder(
												"s1", "ss1", Instant.ofEpochMilli(10000))
												.withRelatedID("id1", "castle")
												.withRelatedID("id2", "arrg")
												.build()),
								new SequenceDistanceAndMetadata(
										new NamespaceID("foo"),
										new MinHashDistance(
												new MinHashSketchDBName("foo"), "s2", 0.3),
										SequenceMetadata.getBuilder(
												"s2", "ss2", Instant.ofEpochMilli(10000))
												.withNullableScientificName("sci name")
												.build())
								),
						set("warn1", "warn2")));
		
		final Map<String, Object> ret = ns.searchNamespaces(
				req, "   \t   ", "  foo ,   \tbaz  ", "", "7");
		
		final Map<String, Object> expected = ImmutableMap.of(
				"impl", "mash",
				"implver", "2.0",
				"warnings", set("warn1", "warn2"),
				"namespaces", set(EXPECTED_NS1, EXPECTED_NS2),
				"distances", Arrays.asList(
						MapBuilder.<String, Object>newHashMap()
								.with("sourceid", "ss1")
								.with("sciname", null)
								.with("namespaceid", "foo")
								.with("dist", 0.1)
								.with("relatedids", Collections.emptyMap())
								.build(),
						MapBuilder.<String, Object>newHashMap()
								.with("sourceid", "ss1")
								.with("sciname", null)
								.with("namespaceid", "baz")
								.with("dist", 0.2)
								.with("relatedids",
										ImmutableMap.of("id1", "castle", "id2", "arrg"))
								.build(),
						MapBuilder.<String, Object>newHashMap()
								.with("sourceid", "ss2")
								.with("sciname", "sci name")
								.with("namespaceid", "foo")
								.with("dist", 0.3)
								.with("relatedids", Collections.emptyMap())
								.build()
						));
		
		assertThat("incorrect distances", ret, is(expected));
	}
	
	@Test
	public void searchWithToken() throws Exception {
		// keep this simple otherwise
		final AssemblyHomology ah = mock(AssemblyHomology.class);
		final HttpServletRequest req = mock(HttpServletRequest.class);
		final Namespaces ns = getNamespaceInstance(ah);
		
		when(ah.getNamespaces(set(new NamespaceID("foo")))).thenReturn(set(NS1));
		when(ah.getExpectedFileExtension(new MinHashImplementationName("mash")))
				.thenReturn(Optional.of(Paths.get("msh")));
		
		when(req.getInputStream())
				.thenReturn(new ByteArrayServletInputStream("file content".getBytes()));
		
		when(ah.measureDistance(
				eq(set(new NamespaceID("foo"))),
				argThat(new TempFileMatcher(".tmp.msh", "file content")),
				eq(7),
				eq(false),
				eq(new Token("livetoken"))))
				.thenReturn(new SequenceMatches(
						set(NS1),
						new MinHashImplementationInformation(
								new MinHashImplementationName("mash"), "2.0", Paths.get("msh")),
						Arrays.asList(
								new SequenceDistanceAndMetadata(
										new NamespaceID("foo"),
										new MinHashDistance(
												new MinHashSketchDBName("foo"), "s1", 0.1),
										SequenceMetadata.getBuilder(
												"s1", "ss1", Instant.ofEpochMilli(10000)).build())
								),
						set("warn1", "warn2")));
		
		final Map<String, Object> ret = ns.searchNamespaces(
				req, "   livetoken   ", "  foo ", "", "7");
		
		final Map<String, Object> expected = ImmutableMap.of(
				"impl", "mash",
				"implver", "2.0",
				"warnings", set("warn1", "warn2"),
				"namespaces", set(EXPECTED_NS1),
				"distances", Arrays.asList(
						MapBuilder.<String, Object>newHashMap()
								.with("sourceid", "ss1")
								.with("sciname", null)
								.with("namespaceid", "foo")
								.with("dist", 0.1)
								.with("relatedids", Collections.emptyMap())
								.build()
						));
		
		assertThat("incorrect distances", ret, is(expected));
	}
	
	@Test
	public void searchFailBadInput() throws Exception {
		final AssemblyHomology ah = mock(AssemblyHomology.class);
		final HttpServletRequest req = mock(HttpServletRequest.class);
		final Namespaces ns = getNamespaceInstance(ah);
		
		failSearch(ns, req, "foo", "foo", new IllegalParameterException(
				"Illegal value for max: foo"));
		failSearch(ns, req, null, null, new MissingParameterException("namespaces"));
		failSearch(ns, req, "foo, ", null, new MissingParameterException("namespaceID"));
		failSearch(ns, req, "foo, Δ", null, new IllegalParameterException(
				"Illegal character in namespace id Δ: Δ"));
	}
	
	@Test
	public void searchFailNoSuchNamespace() throws Exception {
		final AssemblyHomology ah = mock(AssemblyHomology.class);
		final HttpServletRequest req = mock(HttpServletRequest.class);
		final Namespaces ns = getNamespaceInstance(ah);
		
		when(ah.getNamespaces(set(new NamespaceID("foo"))))
				.thenThrow(new NoSuchNamespaceException("foo"));
		
		failSearch(ns, req, "foo", null, new NoSuchNamespaceException("foo"));
	}
	
	@Test
	public void searchFailIncompatibleNamespaces() throws Exception {
		final AssemblyHomology ah = mock(AssemblyHomology.class);
		final HttpServletRequest req = mock(HttpServletRequest.class);
		final Namespaces ns = getNamespaceInstance(ah);
		
		when(ah.getNamespaces(set(new NamespaceID("foo"), new NamespaceID("whee"))))
				.thenReturn(set(
						NS1,
						Namespace.getBuilder(
								new NamespaceID("whee"),
								new MinHashSketchDatabase(
										new MinHashSketchDBName("whee"),
										new MinHashImplementationName("sourmash"),
										MinHashParameters.getBuilder(5).withSketchSize(10000)
												.build(),
										mock(MinHashDBLocation.class),
										21),
								new LoadID("boo"),
								Instant.ofEpochMilli(20000))
								.build()));
		
		failSearch(ns, req, "foo, whee", null, new IncompatibleNamespacesException(
				"Selected namespaces must have the same MinHash implementation"));
	}
	
	@Test
	public void searchFailGetInputStream() throws Exception {
		final AssemblyHomology ah = mock(AssemblyHomology.class);
		final HttpServletRequest req = mock(HttpServletRequest.class);
		final Namespaces ns = getNamespaceInstance(ah);
		
		when(ah.getNamespaces(set(new NamespaceID("foo")))).thenReturn(set(NS1));
		when(ah.getExpectedFileExtension(new MinHashImplementationName("mash")))
				.thenReturn(Optional.absent());
		
		when(req.getInputStream()).thenThrow(new IOException("aw heck"));
		
		failSearch(ns, req, "foo", null, new IOException("aw heck"));
	}
	
	@Test
	public void searchFailMeasurementException() throws Exception {
		final AssemblyHomology ah = mock(AssemblyHomology.class);
		final HttpServletRequest req = mock(HttpServletRequest.class);
		final Namespaces ns = getNamespaceInstance(ah);
		
		when(ah.getNamespaces(set(new NamespaceID("foo")))).thenReturn(set(NS1));
		when(ah.getExpectedFileExtension(new MinHashImplementationName("mash")))
				.thenReturn(Optional.absent());
		
		when(req.getInputStream())
				.thenReturn(new ByteArrayServletInputStream("file content".getBytes()));
		
		when(ah.measureDistance(
				eq(set(new NamespaceID("foo"))),
				argThat(new TempFileMatcher(".tmp", "file content")),
				eq(-1),
				eq(true),
				isNull()))
				.thenThrow(new InvalidSketchException("this sketch is like sooooooo lame"));
		
		failSearch(ns, req, "foo", null, new InvalidSketchException(
				"this sketch is like sooooooo lame"));
	}
	
	private void failSearch(
			final Namespaces ns,
			final HttpServletRequest req,
			final String nsIDs,
			final String max,
			final Exception expected) {
		try {
			ns.searchNamespaces(req, null, nsIDs, null, max);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
