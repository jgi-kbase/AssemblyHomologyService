package us.kbase.test.assemblyhomology.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.test.assemblyhomology.TestCommon;

public class LoadIDTest {
	
	@Test
	public void constructor() throws Exception {
		final LoadID n = new LoadID("    foooΔ   ");
		assertThat("incorrect displayname", n.getName(), is("foooΔ"));
		assertThat("incorrect toString", n.toString(),
				is("LoadID [name=foooΔ]"));
	}
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(LoadID.class).usingGetClass().verify();
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, new MissingParameterException("loadID"));
		failConstruct("   \n  ", new MissingParameterException("loadID"));
		failConstruct("    fo\no\boΔ\n", new IllegalParameterException(
				"loadID contains control characters"));
		failConstruct(TestCommon.LONG1001.substring(0, 257), new IllegalParameterException(
				"loadID size greater than limit 256"));
	}

	private void failConstruct(final String name, final Exception exception) {
		try {
			new LoadID(name);
			fail("created bad display name");
		} catch (Exception e) {
			TestCommon.assertExceptionCorrect(e, exception);
		}
	}

}
