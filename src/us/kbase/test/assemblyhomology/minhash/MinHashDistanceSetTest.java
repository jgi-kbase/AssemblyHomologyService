package us.kbase.test.assemblyhomology.minhash;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import static us.kbase.test.assemblyhomology.TestCommon.set;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashDistanceSet;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.test.assemblyhomology.TestCommon;

public class MinHashDistanceSetTest {
	
	@Test
	public void equals() {
		EqualsVerifier.forClass(MinHashDistanceSet.class).usingGetClass().verify();
	}
	
	@Test
	public void construct() {
		final MinHashSketchDBName n = new MinHashSketchDBName("n");
		final MinHashDistanceSet dists = new MinHashDistanceSet(
				set(new MinHashDistance(n, "1", 0.1),
						new MinHashDistance(n, "2", 1),
						new MinHashDistance(n, "3", 0.9),
						new MinHashDistance(n, "4", 0),
						new MinHashDistance(n, "5", 0.5)
						),
				Arrays.asList("foo", "bar"));
		
		assertThat("incorrect dists", dists.getDistances(), is(
				set(new MinHashDistance(n, "1", 0.1),
						new MinHashDistance(n, "2", 1),
						new MinHashDistance(n, "3", 0.9),
						new MinHashDistance(n, "4", 0),
						new MinHashDistance(n, "5", 0.5)
						)));
		
		final List<MinHashDistance> ordered = dists.getDistances().stream()
				.collect(Collectors.toList());
		assertThat("incorrect order", ordered, is(Arrays.asList(
				new MinHashDistance(n, "4", 0),
				new MinHashDistance(n, "1", 0.1),
				new MinHashDistance(n, "5", 0.5),
				new MinHashDistance(n, "3", 0.9),
				new MinHashDistance(n, "2", 1)
				)));
		
		assertThat("incorrect warnings", dists.getWarnings(), is(Arrays.asList("foo", "bar")));
	}
	
	@Test
	public void immutable() {
		final MinHashSketchDBName n = new MinHashSketchDBName("n");
		final MinHashDistanceSet dists = new MinHashDistanceSet(
				set(new MinHashDistance(n, "1", 0.1),
						new MinHashDistance(n, "2", 1)
						),
				Arrays.asList("foo", "bar"));
		
		try {
			dists.getDistances().add(new MinHashDistance(n, "3", 0.5));
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new UnsupportedOperationException());
		}
		
		try {
			dists.getWarnings().add("whee");
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new UnsupportedOperationException());
		}
	}
	
	@Test
	public void constructFail() {
		final List<String> mt = Collections.emptyList();
		final MinHashSketchDBName n = new MinHashSketchDBName("n");
		failConstruct(null, mt, new NullPointerException("distances"));
		failConstruct(set(new MinHashDistance(n, "4", 0), null), mt,
				new NullPointerException("Null item in collection distances"));
		failConstruct(set(), null, new NullPointerException("warnings"));
		failConstruct(set(), Arrays.asList("f", null), new IllegalArgumentException(
				"Null or whitespace only string in collection warnings"));
		failConstruct(set(), Arrays.asList("f", "   \t    \t   "), new IllegalArgumentException(
				"Null or whitespace only string in collection warnings"));
	}
	
	private void failConstruct(
			final Set<MinHashDistance> dists,
			final List<String> warnings,
			final Exception expected) {
		try {
			new MinHashDistanceSet(dists, warnings);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
		
	}

}
