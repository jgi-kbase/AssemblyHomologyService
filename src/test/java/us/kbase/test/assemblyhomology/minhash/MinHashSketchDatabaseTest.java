package us.kbase.test.assemblyhomology.minhash;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.minhash.exceptions.IncompatibleSketchesException;
import us.kbase.test.assemblyhomology.TestCommon;

public class MinHashSketchDatabaseTest {
	
	// mocking location to avoid the file exists check
	
	@Test
	public void equals() {
		EqualsVerifier.forClass(MinHashSketchDatabase.class).usingGetClass().verify();
	}
	
	@Test
	public void construct() throws Exception {
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				loc,
				42);
		
		assertThat("incorrect name", db.getName(), is(new MinHashSketchDBName("foo")));
		assertThat("incorrect impl name", db.getImplementationName(),
				is(new MinHashImplementationName("bar")));
		assertThat("incorrect params", db.getParameterSet(), is(
				MinHashParameters.getBuilder(31).withSketchSize(1000).build()));
		assertThat("incorrect loc", db.getLocation(), is(loc));
		assertThat("incorrect count", db.getSequenceCount(), is(42));
	}
	
	@Test
	public void constructFail() throws Exception {
		final MinHashSketchDBName n = new MinHashSketchDBName("foo");
		final MinHashImplementationName i = new MinHashImplementationName("bar");
		final MinHashParameters p = MinHashParameters.getBuilder(31).withSketchSize(1000).build();
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		
		failConstruct(null, i, p, loc, 1, new NullPointerException("dbname"));
		failConstruct(n, null, p, loc, 1, new NullPointerException("minHashImplName"));
		failConstruct(n, i, null, loc, 1, new NullPointerException("parameterSet"));
		failConstruct(n, i, p, null, 1, new NullPointerException("location"));
		failConstruct(n, i, p, loc, 0, new IllegalArgumentException(
				"sequenceCount must be at least 1"));
	}

	private void failConstruct(
			final MinHashSketchDBName name,
			final MinHashImplementationName impl,
			final MinHashParameters param,
			final MinHashDBLocation loc,
			final int count,
			final Exception expected) {
		try {
			new MinHashSketchDatabase(name, impl, param, loc, count);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void checkIsQueryableSketchSizeStrict() throws Exception {
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				mock(MinHashDBLocation.class),
				42);
		
		final MinHashSketchDatabase db2 = new MinHashSketchDatabase(
				new MinHashSketchDBName("whee"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				mock(MinHashDBLocation.class),
				77);
		
		assertThat("incorrect warnings", db.checkIsQueriableBy(db2, true),
				is(Collections.emptyList()));
	}
	
	@Test
	public void checkIsQueryableSketchSizeNonStrict() throws Exception {
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				mock(MinHashDBLocation.class),
				42);
		
		final MinHashSketchDatabase db2 = new MinHashSketchDatabase(
				new MinHashSketchDBName("whee"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withSketchSize(1001).build(),
				mock(MinHashDBLocation.class),
				77);
		
		assertThat("incorrect warnings", db.checkIsQueriableBy(db2, false), is(Arrays.asList(
				"Query sketch size 1001 is larger than target sketch size 1000")));
	}
	
	@Test
	public void checkIsQueryableScaling() throws Exception {
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withScaling(1000).build(),
				mock(MinHashDBLocation.class),
				42);
		
		final MinHashSketchDatabase db2 = new MinHashSketchDatabase(
				new MinHashSketchDBName("whee"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withScaling(1000).build(),
				mock(MinHashDBLocation.class),
				77);
		
		assertThat("incorrect warnings", db.checkIsQueriableBy(db2, true),
				is(Collections.emptyList()));
	}
	
	@Test
	public void checkIsQueryableFailNull() throws Exception {
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				mock(MinHashDBLocation.class),
				42);
		failIsQueryable(db, null, true, new NullPointerException("query"));
	}
	
	@Test
	public void checkIsQueryableFailImpl() throws Exception {
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				mock(MinHashDBLocation.class),
				42);
		
		final MinHashSketchDatabase db2 = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar1"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				mock(MinHashDBLocation.class),
				42);
		failIsQueryable(db, db2, true, new IncompatibleSketchesException(
				"Implementations for sketches do not match: bar bar1"));
	}
	
	@Test
	public void checkIsQueryableFailKmerSize() throws Exception {
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				mock(MinHashDBLocation.class),
				42);
		
		final MinHashSketchDatabase db2 = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(30).withSketchSize(1000).build(),
				mock(MinHashDBLocation.class),
				42);
		failIsQueryable(db, db2, true, new IncompatibleSketchesException(
				"Kmer size for sketches are not compatible: 31 30"));
	}
	
	@Test
	public void checkIsQueryableFailSketchSizeStrict() throws Exception {
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				mock(MinHashDBLocation.class),
				42);
		
		final MinHashSketchDatabase db2 = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withSketchSize(1001).build(),
				mock(MinHashDBLocation.class),
				42);
		failIsQueryable(db, db2, true, new IncompatibleSketchesException(
				"Query sketch size 1001 does not match target 1000"));
	}
	
	@Test
	public void checkIsQueryableFailScaling() throws Exception {
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withScaling(1000).build(),
				mock(MinHashDBLocation.class),
				42);
		
		final MinHashSketchDatabase db2 = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withScaling(1001).build(),
				mock(MinHashDBLocation.class),
				42);
		failIsQueryable(db, db2, true, new IncompatibleSketchesException(
				"Scaling parameters for sketches are not compatible: 1000 1001"));
	}
	
	@Test
	public void checkIsQueryableFailSketchSizeTooSmall() throws Exception {
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				mock(MinHashDBLocation.class),
				42);
		
		final MinHashSketchDatabase db2 = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withSketchSize(999).build(),
				mock(MinHashDBLocation.class),
				42);
		failIsQueryable(db, db2, false, new IncompatibleSketchesException(
				"Query sketch size 999 may not be smaller than the target sketch size 1000"));
		failIsQueryable(db, db2, true, new IncompatibleSketchesException(
				"Query sketch size 999 does not match target 1000"));
	}
	
	@Test
	public void checkIsQueryableFailScalingSizeMismatch() throws Exception {
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withSketchSize(1000).build(),
				mock(MinHashDBLocation.class),
				42);
		
		final MinHashSketchDatabase db2 = new MinHashSketchDatabase(
				new MinHashSketchDBName("foo"),
				new MinHashImplementationName("bar"),
				MinHashParameters.getBuilder(31).withScaling(1000).build(),
				mock(MinHashDBLocation.class),
				42);
		failIsQueryable(db, db2, true, new IncompatibleSketchesException(
				"Both sketches must use either absolute sketch counts or scaling"));
	}
	
	private void failIsQueryable(
			final MinHashSketchDatabase target,
			final MinHashSketchDatabase query,
			final boolean strict,
			final Exception expected) {
		try {
			target.checkIsQueriableBy(query, strict);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
