package us.kbase.test.assemblyhomology.filters;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static us.kbase.test.assemblyhomology.TestCommon.set;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import us.kbase.assemblyhomology.filters.KBaseAuthenticatedFilter;
import us.kbase.assemblyhomology.minhash.DefaultDistanceCollector;
import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashDistanceCollector;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashDistanceFilterException;
import us.kbase.test.assemblyhomology.TestCommon;

public class KBaseAuthenticatedFilterTest {
	
	@Test
	public void construct() throws Exception {
		final MinHashDistanceCollector col = new DefaultDistanceCollector(10);
		
		KBaseAuthenticatedFilter fil = new KBaseAuthenticatedFilter(set(), col);
		assertThat("incorrect ws ids", fil.getWorkspaceIDs(), is(set()));
		
		fil = new KBaseAuthenticatedFilter(set(5L, 8L, 10L), col);
		assertThat("incorrect ws ids", fil.getWorkspaceIDs(), is(set(10L, 8L, 5L)));
		
		fil.accept(new MinHashDistance(new MinHashSketchDBName("d"), "8_1_1", 0.1));
		
		assertThat("incorrect distances", col.getDistances(), is(set(
				new MinHashDistance(new MinHashSketchDBName("d"), "8_1_1", 0.1))));
	}
	
	@Test
	public void constructFail() {
		final MinHashDistanceCollector col = new DefaultDistanceCollector(10);
		
		failConstruct(null, col, new NullPointerException("workspaceIDs"));
		failConstruct(set(), null, new NullPointerException("collector"));
		failConstruct(set(1L, null), col, new NullPointerException(
				"Null item in collection workspaceIDs"));
	}
	
	private void failConstruct(
			final Set<Long> wsids,
			final MinHashDistanceCollector col,
			final Exception expected) {
		try {
			new KBaseAuthenticatedFilter(wsids, col);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
		
	}

	@Test
	public void flush() throws Exception {
		// does nothing so just test there's no error
		new KBaseAuthenticatedFilter(set(), new DefaultDistanceCollector(10)).flush();
	}
	
	@Test
	public void accept() throws Exception {
		final MinHashDistanceCollector col = new DefaultDistanceCollector(10);
		
		KBaseAuthenticatedFilter fil = new KBaseAuthenticatedFilter(set(8L, 2L, 6L), col);
		
		fil.accept(new MinHashDistance(new MinHashSketchDBName("d1"), "8_23_6", 0.1));
		fil.accept(new MinHashDistance(new MinHashSketchDBName("d2"), "1_4_18", 0.2));
		fil.accept(new MinHashDistance(new MinHashSketchDBName("d3"), "2_1_1", 0.3));
		fil.accept(new MinHashDistance(new MinHashSketchDBName("d4"), "100_2_7", 0.4));
		fil.accept(new MinHashDistance(new MinHashSketchDBName("d5"), "7_6_2", 0.5));
		fil.accept(new MinHashDistance(new MinHashSketchDBName("d6"), "6_10000_33", 0.6));
		
		assertThat("incorrect distances", col.getDistances(), is(set(
				new MinHashDistance(new MinHashSketchDBName("d1"), "8_23_6", 0.1),
				new MinHashDistance(new MinHashSketchDBName("d3"), "2_1_1", 0.3),
				new MinHashDistance(new MinHashSketchDBName("d6"), "6_10000_33", 0.6))));
	}
	
	@Test
	public void acceptFail() throws Exception {
		failAccept(null, new NullPointerException("dist"));
		failAcceptStr("foo", new MinHashDistanceFilterException("Invalid workspace UPA: foo"));
		failAcceptStr("1/2/3", new MinHashDistanceFilterException("Invalid workspace UPA: 1/2/3"));
		failAcceptStr("1_2", new MinHashDistanceFilterException("Invalid workspace UPA: 1_2"));
		failAcceptStr("_1_2_3", new MinHashDistanceFilterException(
				"Invalid workspace UPA: _1_2_3"));
		failAcceptStr("1_2_3_", new MinHashDistanceFilterException(
				"Invalid workspace UPA: 1_2_3_"));
		
		failAcceptStr("1_2_X", new MinHashDistanceFilterException(
				"In workspace UPA 1_2_X, version is not an integer"));
		failAcceptStr("1_X_2", new MinHashDistanceFilterException(
				"In workspace UPA 1_X_2, object id is not an integer"));
		failAcceptStr("X_1_2", new MinHashDistanceFilterException(
				"In workspace UPA X_1_2, workspace id is not an integer"));
		
		failAcceptStr("1_2_3 ", new MinHashDistanceFilterException(
				"In workspace UPA 1_2_3 , version is not an integer"));
		failAcceptStr("1_2_ 3", new MinHashDistanceFilterException(
				"In workspace UPA 1_2_ 3, version is not an integer"));
		failAcceptStr("1_2 _3", new MinHashDistanceFilterException(
				"In workspace UPA 1_2 _3, object id is not an integer"));
		failAcceptStr("1_ 2_3", new MinHashDistanceFilterException(
				"In workspace UPA 1_ 2_3, object id is not an integer"));
		failAcceptStr("1 _2_3", new MinHashDistanceFilterException(
				"In workspace UPA 1 _2_3, workspace id is not an integer"));
		failAcceptStr(" 1_2_3", new MinHashDistanceFilterException(
				"In workspace UPA  1_2_3, workspace id is not an integer"));
		
		failAcceptStr("1_2_0", new MinHashDistanceFilterException(
				"In workspace UPA 1_2_0, version must be > 0"));
		failAcceptStr("1_0_2", new MinHashDistanceFilterException(
				"In workspace UPA 1_0_2, object id must be > 0"));
		failAcceptStr("0_2_1", new MinHashDistanceFilterException(
				"In workspace UPA 0_2_1, workspace id must be > 0"));
	}
	
	private void failAcceptStr(final String id, final Exception expected) {
		failAccept(new MinHashDistance(new MinHashSketchDBName("d"), id, 0.1), expected);
	}
	
	private void failAccept(final MinHashDistance dist, final Exception expected) {
		
		try {
			new KBaseAuthenticatedFilter(set(), new DefaultDistanceCollector(10)).accept(dist);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void validateUPA() throws Exception {
		assertThat("bad validation", KBaseAuthenticatedFilter.validateUPA("1_1_1"), is(1L));
		assertThat("bad validation", KBaseAuthenticatedFilter.validateUPA(
				"10000000000000_1000000000000000_1000000000000"), is(10000000000000L));
	}
	
	@Test
	public void validateUPAFail() {
		failValidateUPA(null, new NullPointerException("upa"));
		failValidateUPA("", new MinHashDistanceFilterException(
				"Invalid workspace UPA: "));
		failValidateUPA("   \t   ", new MinHashDistanceFilterException(
				"Invalid workspace UPA:    \t   "));
		failValidateUPA("foo", new MinHashDistanceFilterException(
				"Invalid workspace UPA: foo"));
		failValidateUPA("1/2/3", new MinHashDistanceFilterException(
				"Invalid workspace UPA: 1/2/3"));
		failValidateUPA("1_2", new MinHashDistanceFilterException(
				"Invalid workspace UPA: 1_2"));
		failValidateUPA("_1_2_3", new MinHashDistanceFilterException(
				"Invalid workspace UPA: _1_2_3"));
		failValidateUPA("1_2_3_", new MinHashDistanceFilterException(
				"Invalid workspace UPA: 1_2_3_"));
		
		failValidateUPA("1_2_X", new MinHashDistanceFilterException(
				"In workspace UPA 1_2_X, version is not an integer"));
		failValidateUPA("1_X_2", new MinHashDistanceFilterException(
				"In workspace UPA 1_X_2, object id is not an integer"));
		failValidateUPA("X_1_2", new MinHashDistanceFilterException(
				"In workspace UPA X_1_2, workspace id is not an integer"));
		
		failValidateUPA("1_2_3 ", new MinHashDistanceFilterException(
				"In workspace UPA 1_2_3 , version is not an integer"));
		failValidateUPA("1_2_ 3", new MinHashDistanceFilterException(
				"In workspace UPA 1_2_ 3, version is not an integer"));
		failValidateUPA("1_2 _3", new MinHashDistanceFilterException(
				"In workspace UPA 1_2 _3, object id is not an integer"));
		failValidateUPA("1_ 2_3", new MinHashDistanceFilterException(
				"In workspace UPA 1_ 2_3, object id is not an integer"));
		failValidateUPA("1 _2_3", new MinHashDistanceFilterException(
				"In workspace UPA 1 _2_3, workspace id is not an integer"));
		failValidateUPA(" 1_2_3", new MinHashDistanceFilterException(
				"In workspace UPA  1_2_3, workspace id is not an integer"));
		
		failValidateUPA("1_2_0", new MinHashDistanceFilterException(
				"In workspace UPA 1_2_0, version must be > 0"));
		failValidateUPA("1_0_2", new MinHashDistanceFilterException(
				"In workspace UPA 1_0_2, object id must be > 0"));
		failValidateUPA("0_2_1", new MinHashDistanceFilterException(
				"In workspace UPA 0_2_1, workspace id must be > 0"));
	}
	
	private void failValidateUPA(final String upa, final Exception expected) {
		try {
			KBaseAuthenticatedFilter.validateUPA(upa);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void immutable() {
		final Set<Long> wsids = new HashSet<>();
		wsids.add(3L);
		final KBaseAuthenticatedFilter f = new KBaseAuthenticatedFilter(wsids,
				new DefaultDistanceCollector(10));
		
		assertThat("incorrect wsids", f.getWorkspaceIDs(), is(set(3L)));
		wsids.add(6L);
		assertThat("incorrect wsids", f.getWorkspaceIDs(), is(set(3L)));
		
		
		try {
			f.getWorkspaceIDs().add(6L);
			fail("expected exception");
		} catch (UnsupportedOperationException got) {
			// test passed.
		}
	}
}
