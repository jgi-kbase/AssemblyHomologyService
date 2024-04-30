package us.kbase.test.assemblyhomology.minhash;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.test.assemblyhomology.TestCommon;

public class MinHashDistanceTest {
	
	@Test
	public void equals() {
		EqualsVerifier.forClass(MinHashDistance.class).usingGetClass().verify();
	}
	
	@Test
	public void construct() {
		final MinHashDistance dist = new MinHashDistance(
				new MinHashSketchDBName("foo"), "bar", 0.3);
		
		assertThat("incorrect db name", dist.getReferenceDBName(),
				is(new MinHashSketchDBName("foo")));
		assertThat("incorrect seq id", dist.getSequenceID(), is("bar"));
		assertThat("incorrect dist", dist.getDistance(), is(0.3));
	}
	
	@Test
	public void constructFail() {
		final MinHashSketchDBName n = new MinHashSketchDBName("foo");
		failConstruct(null, "i", 0.1, new NullPointerException("referenceDBName"));
		failConstruct(n, null, 0.1, new IllegalArgumentException(
				"sequenceID cannot be null or whitespace only"));
		failConstruct(n, "   \t  \n  ", 0.1, new IllegalArgumentException(
				"sequenceID cannot be null or whitespace only"));
		failConstruct(n, "i", -0.00000000001, new IllegalArgumentException(
				"Illegal distance value: -1.0E-11"));
		failConstruct(n, "i", 1.00000000001, new IllegalArgumentException(
				"Illegal distance value: 1.00000000001"));
	}
	
	private void failConstruct(
			final MinHashSketchDBName dbname,
			final String id,
			final double distance,
			final Exception expected) {
		try {
			new MinHashDistance(dbname, id, distance);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void compareToEqual() {
		final MinHashDistance d1 = new MinHashDistance(new MinHashSketchDBName("d"), "i", 0.25);
		final MinHashDistance d2 = new MinHashDistance(new MinHashSketchDBName("d"), "i", 0.25);
		
		assertThat("incorrect compare", d1.compareTo(d2), is(0));
		assertThat("incorrect compare", d2.compareTo(d1), is(0));
	}
	
	@Test
	public void compareToDistance() {
		final MinHashDistance d1 = new MinHashDistance(new MinHashSketchDBName("d"), "i", 0.25);
		final MinHashDistance d2 = new MinHashDistance(new MinHashSketchDBName("c"), "h", 0.26);
		
		assertThat("incorrect compare", d1.compareTo(d2), is(-1));
		assertThat("incorrect compare", d2.compareTo(d1), is(1));
	}
	
	@Test
	public void compareToRefDB() {
		final MinHashDistance d1 = new MinHashDistance(new MinHashSketchDBName("d"), "i", 0.25);
		final MinHashDistance d2 = new MinHashDistance(new MinHashSketchDBName("e"), "h", 0.25);
		
		assertThat("incorrect compare", d1.compareTo(d2), is(-1));
		assertThat("incorrect compare", d2.compareTo(d1), is(1));
	}
	
	@Test
	public void compareToSeqID() {
		final MinHashDistance d1 = new MinHashDistance(new MinHashSketchDBName("e"), "i", 0.25);
		final MinHashDistance d2 = new MinHashDistance(new MinHashSketchDBName("e"), "h", 0.25);
		
		assertThat("incorrect compare", d1.compareTo(d2), is(1));
		assertThat("incorrect compare", d2.compareTo(d1), is(-1));
	}

}
