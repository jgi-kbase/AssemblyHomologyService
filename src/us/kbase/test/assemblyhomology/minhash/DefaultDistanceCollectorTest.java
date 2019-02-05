package us.kbase.test.assemblyhomology.minhash;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.junit.Test;

import us.kbase.assemblyhomology.minhash.DefaultDistanceCollector;
import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.test.assemblyhomology.TestCommon;

public class DefaultDistanceCollectorTest {
	
	private static final MinHashSketchDBName DBNAME = new MinHashSketchDBName("myname");

	private static final MinHashDistance D1 = new MinHashDistance(DBNAME, "15792_446_1", 0);
	private static final MinHashDistance D2 = new MinHashDistance(
			DBNAME, "15792_431_1", 0.00236402);
	private static final MinHashDistance D3 = new MinHashDistance(
			DBNAME, "15792_341_2", 0.00921302);
	
	private static final List<MinHashDistance> DISTS = Arrays.asList(D2, D1, D3);
	
	@Test
	public void acceptAndGet() {
		acceptAndGet(100, new TreeSet<>(Arrays.asList(D1, D2, D3)));
		acceptAndGet(3, new TreeSet<>(Arrays.asList(D1, D2, D3)));
		acceptAndGet(2, new TreeSet<>(Arrays.asList(D1, D2)));
		acceptAndGet(1, new TreeSet<>(Arrays.asList(D1)));
	}
	
	private void acceptAndGet(final int size, final TreeSet<MinHashDistance> expected) {
		final DefaultDistanceCollector col = new DefaultDistanceCollector(size);
		for (final MinHashDistance d: DISTS) {
			col.accept(d);
		}
		assertThat("incorrect distances", col.getDistances(), is(expected));
	}
	
	@Test
	public void failConstruct() {
		try {
			new DefaultDistanceCollector(0);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got,
					new IllegalArgumentException("size must be > 0"));
		}
	}
	
	@Test
	public void failAccept() {
		try {
			new DefaultDistanceCollector(1).accept(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("item"));
		}
	}

}
