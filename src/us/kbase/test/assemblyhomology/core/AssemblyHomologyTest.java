package us.kbase.test.assemblyhomology.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static us.kbase.test.assemblyhomology.TestCommon.set;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.core.AssemblyHomology;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationFactory;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.storage.AssemblyHomologyStorage;
import us.kbase.test.assemblyhomology.TestCommon;

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

	@Test
	public void constructWithEmptyFactories() {
		// should pass
		new AssemblyHomology(
				mock(AssemblyHomologyStorage.class),
				Collections.emptyList(),
				Paths.get("foo"));
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
		
		failConstruct(null, fs, t, new NullPointerException("storage"));
		failConstruct(s, null, t, new NullPointerException("implementationFactories"));
		failConstruct(s, Arrays.asList(f1, null), t, new NullPointerException(
				"Null item in collection implementationFactories"));
		failConstruct(s, fs, null, new NullPointerException("tempFileDirectory"));
		failConstruct(s, Arrays.asList(f1, f2), t,
				new IllegalArgumentException("Duplicate implementation: foo"));
	}
	
	private void failConstruct(
			final AssemblyHomologyStorage storage,
			final Collection<MinHashImplementationFactory> implementationFactories,
			final Path tempFileDirectory,
			final Exception expected) {
		try {
			new AssemblyHomology(storage, implementationFactories, tempFileDirectory);
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
				s, Arrays.asList(f1, f2), Paths.get("foo"));
		
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
		
		final AssemblyHomology ah = new AssemblyHomology(s, Arrays.asList(f1), Paths.get("foo"));
		
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
				s, Collections.emptyList(), Paths.get("foo"));
		
		when(s.getNamespaces()).thenReturn(new HashSet<>(Arrays.asList(NS1, NS2)));
		
		assertThat("incorrect namespaces", ah.getNamespaces(), is(
				new HashSet<>(Arrays.asList(NS2, NS1))));
	}
	
	@Test
	public void getNamespace() throws Exception {
		final AssemblyHomologyStorage s = mock(AssemblyHomologyStorage.class);
		
		final AssemblyHomology ah = new AssemblyHomology(
				s, Collections.emptyList(), Paths.get("foo"));
		
		when(s.getNamespace(new NamespaceID("baz"))).thenReturn(NS2);
		
		assertThat("incorrect namespace", ah.getNamespace(new NamespaceID("baz")), is(NS2));
	}
	
	@Test
	public void getNamespaceFail() throws Exception {
		final AssemblyHomologyStorage s = mock(AssemblyHomologyStorage.class);
		
		final AssemblyHomology ah = new AssemblyHomology(
				s, Collections.emptyList(), Paths.get("foo"));
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
				s, Collections.emptyList(), Paths.get("foo"));
		
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
				s, Collections.emptyList(), Paths.get("foo"));
		
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
}

