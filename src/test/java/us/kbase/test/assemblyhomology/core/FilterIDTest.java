package us.kbase.test.assemblyhomology.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.core.FilterID;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.test.assemblyhomology.TestCommon;

public class FilterIDTest {
	
	@Test
	public void constructor() throws Exception {
		FilterID n = new FilterID("    abcdefghijklmnopqrst   ");
		assertThat("incorrect displayname", n.getName(), is("abcdefghijklmnopqrst"));
		assertThat("incorrect toString", n.toString(),
				is("FilterID [name=abcdefghijklmnopqrst]"));
		
		n = new FilterID("    uvwxyz   ");
		assertThat("incorrect displayname", n.getName(), is("uvwxyz"));
		assertThat("incorrect toString", n.toString(),
				is("FilterID [name=uvwxyz]"));
	}
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(NamespaceID.class).usingGetClass().verify();
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, new MissingParameterException("filter id"));
		failConstruct("   \n  ", new MissingParameterException("filter id"));
		failConstruct("    fo\no\boΔ\n", new IllegalParameterException(
				"filter id contains control characters"));
		failConstruct("    fooAoΔ\n", new IllegalParameterException(
				"Illegal character in filter id fooAoΔ: A"));
		failConstruct("    foo1oΔ\n", new IllegalParameterException(
				"Illegal character in filter id foo1oΔ: 1"));
		failConstruct("    foo_oΔ\n", new IllegalParameterException(
				"Illegal character in filter id foo_oΔ: _"));
		failConstruct("    foooΔ\n", new IllegalParameterException(
				"Illegal character in filter id foooΔ: Δ"));
		failConstruct(TestCommon.LONG1001.substring(0, 21), new IllegalParameterException(
				"filter id size greater than limit 20"));
	}

	private void failConstruct(final String name, final Exception exception) {
		try {
			new FilterID(name);
			fail("created bad filter ID");
		} catch (Exception e) {
			TestCommon.assertExceptionCorrect(e, exception);
		}
	}
}
