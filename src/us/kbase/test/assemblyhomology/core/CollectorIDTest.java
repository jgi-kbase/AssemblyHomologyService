package us.kbase.test.assemblyhomology.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.core.CollectorID;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.test.assemblyhomology.TestCommon;

public class CollectorIDTest {
	
	@Test
	public void constructor() throws Exception {
		CollectorID n = new CollectorID("    abcdefghijklmnopqrst   ");
		assertThat("incorrect displayname", n.getName(), is("abcdefghijklmnopqrst"));
		assertThat("incorrect toString", n.toString(),
				is("CollectorID [name=abcdefghijklmnopqrst]"));
		
		n = new CollectorID("    uvwxyz   ");
		assertThat("incorrect displayname", n.getName(), is("uvwxyz"));
		assertThat("incorrect toString", n.toString(),
				is("CollectorID [name=uvwxyz]"));
	}
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(NamespaceID.class).usingGetClass().verify();
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, new MissingParameterException("collectorID"));
		failConstruct("   \n  ", new MissingParameterException("collectorID"));
		failConstruct("    fo\no\boΔ\n", new IllegalParameterException(
				"collectorID contains control characters"));
		failConstruct("    fooAoΔ\n", new IllegalParameterException(
				"Illegal character in collector id fooAoΔ: A"));
		failConstruct("    foo1oΔ\n", new IllegalParameterException(
				"Illegal character in collector id foo1oΔ: 1"));
		failConstruct("    foo_oΔ\n", new IllegalParameterException(
				"Illegal character in collector id foo_oΔ: _"));
		failConstruct("    foooΔ\n", new IllegalParameterException(
				"Illegal character in collector id foooΔ: Δ"));
		failConstruct(TestCommon.LONG1001.substring(0, 21), new IllegalParameterException(
				"collectorID size greater than limit 20"));
	}

	private void failConstruct(final String name, final Exception exception) {
		try {
			new CollectorID(name);
			fail("created bad collector ID");
		} catch (Exception e) {
			TestCommon.assertExceptionCorrect(e, exception);
		}
	}
	
	@Test
	public void defaultID() throws Exception {
		assertThat("incorrect default ID", CollectorID.DEFAULT, is(new CollectorID("default")));
	}

}
