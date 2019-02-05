package us.kbase.test.assemblyhomology.minhash;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.isNull;

import org.junit.Test;

import us.kbase.assemblyhomology.minhash.DefaultDistanceCollector;
import us.kbase.assemblyhomology.minhash.DefaultDistanceFilter;
import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashDistanceCollector;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.test.assemblyhomology.TestCommon;

public class DefaultDistanceFilterTest {

	@Test
	public void flush() {
		// does nothing so just check it doesn't throw an error
		new DefaultDistanceFilter(new DefaultDistanceCollector(1)).flush();
	}
	
	@Test
	public void accept() throws Exception {
		final MinHashDistanceCollector col = mock(MinHashDistanceCollector.class);
		
		final DefaultDistanceFilter f = new DefaultDistanceFilter(col);
		
		f.accept(new MinHashDistance(new MinHashSketchDBName("somedb"), "15792_446_1", 0));
		
		verify(col).accept(new MinHashDistance(
				new MinHashSketchDBName("somedb"), "15792_446_1", 0));
		
		f.accept(null); // let the collector deal with nulls
		verify(col).accept(isNull());
	}

	@Test
	public void failConstruct() {
		try {
			new DefaultDistanceFilter(null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new NullPointerException("collector"));
		}
	}
}
