package us.kbase.test.assemblyhomology.minhash;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.test.assemblyhomology.TestCommon;

public class MinHashSketchDBNameTest {

	@Test
	public void equals() {
		EqualsVerifier.forClass(MinHashSketchDBName.class).usingGetClass().verify();
	}
	
	@Test
	public void construct() {
		final MinHashSketchDBName n = new MinHashSketchDBName("foo");
		
		assertThat("incorrect name", n.getName(), is("foo"));
	}
	
	@Test
	public void constructFail() {
		failConstruct(null, new IllegalArgumentException(
				"name cannot be null or whitespace only"));
		failConstruct("   \t    \n  ", new IllegalArgumentException(
				"name cannot be null or whitespace only"));
	}
	
	private void failConstruct(final String name, final Exception expected) {
		try {
			new MinHashSketchDBName(name);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void compareTo() {
		final MinHashSketchDBName n1 = new MinHashSketchDBName("b");
		final MinHashSketchDBName n2 = new MinHashSketchDBName("b");
		final MinHashSketchDBName n3 = new MinHashSketchDBName("c");
		
		assertThat("incorrect compare", n1.compareTo(n2), is(0));
		assertThat("incorrect compare", n1.compareTo(n3), is(-1));
		assertThat("incorrect compare", n3.compareTo(n1), is(1));
	}
	
}
