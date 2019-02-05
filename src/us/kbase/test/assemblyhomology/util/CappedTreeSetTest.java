package us.kbase.test.assemblyhomology.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.util.CappedTreeSet;
import us.kbase.test.assemblyhomology.TestCommon;

public class CappedTreeSetTest {
	
	@Test
	public void equals() {
		EqualsVerifier.forClass(CappedTreeSet.class).usingGetClass().verify();
	}
	
	@Test
	public void constructEmpty() {
		final CappedTreeSet<Integer> cts = new CappedTreeSet<>(5, false);
		
		assertThat("incorrect max size", cts.getMaximumSize(), is(5));
		assertThat("incorrect desc", cts.isDescending(), is(false));
		assertThat("incorrect size", cts.size(), is(0));
		assertThat("incorrect to set", cts.toTreeSet(), is(new HashSet<>()));
	}
	
	@Test
	public void failConstruct() {
		try {
			new CappedTreeSet<>(0, true);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(
					got, new IllegalArgumentException("size must be > 0"));
		}
	}
	
	@Test
	public void ascending() {
		final CappedTreeSet<Integer> cts = new CappedTreeSet<>(3, false);
		cts.add(5);
		cts.add(10);
		cts.add(15);
		assertOrder(cts.toTreeSet(), Arrays.asList(5, 10, 15));
		cts.add(3);
		assertOrder(cts.toTreeSet(), Arrays.asList(5, 10, 15));
		cts.add(-1);
		assertOrder(cts.toTreeSet(), Arrays.asList(5, 10, 15));
		cts.add(7);
		assertOrder(cts.toTreeSet(), Arrays.asList(7, 10, 15));
		cts.add(20);
		assertOrder(cts.toTreeSet(), Arrays.asList(10, 15, 20));
		cts.add(17);
		assertOrder(cts.toTreeSet(), Arrays.asList(15, 17, 20));
	}
	
	@Test
	public void descending() {
		final CappedTreeSet<Integer> cts = new CappedTreeSet<>(3, true);
		cts.add(5);
		cts.add(10);
		cts.add(15);
		assertOrder(cts.toTreeSet(), Arrays.asList(5, 10, 15));
		cts.add(20);
		assertOrder(cts.toTreeSet(), Arrays.asList(5, 10, 15));
		cts.add(Integer.MAX_VALUE);
		assertOrder(cts.toTreeSet(), Arrays.asList(5, 10, 15));
		cts.add(13);
		assertOrder(cts.toTreeSet(), Arrays.asList(5, 10, 13));
		cts.add(1);
		assertOrder(cts.toTreeSet(), Arrays.asList(1, 5, 10));
		cts.add(3);
		assertOrder(cts.toTreeSet(), Arrays.asList(1, 3, 5));
	}
	
	private void assertOrder(final Set<Integer> inputSet, final List<Integer> expected) {
		int pos = 0;
		for (final int entry: inputSet) {
			assertThat("incorrect entry in sorted set at position " + pos, entry,
					is(expected.get(pos)));
			pos++;
		}
	}

	@Test
	public void failAdd() {
		try {
			new CappedTreeSet<>(1, true).add(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("item"));
		}
	}

}
