package us.kbase.test.assemblyhomology.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

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
		assertThat("incorrect max size", cts.isDescending(), is(false));
		assertThat("incorrect max size", cts.size(), is(0));
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

}
