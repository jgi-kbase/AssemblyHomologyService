package us.kbase.test.assemblyhomology.minhash;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.test.assemblyhomology.TestCommon;

public class MinHashImplementationNameTest {

	@Test
	public void constructor() throws Exception {
		final MinHashImplementationName n = new MinHashImplementationName("    foooΔ   ");
		assertThat("incorrect displayname", n.getName(), is("foooΔ"));
		assertThat("incorrect toString", n.toString(),
				is("MinHashImplementationName [name=foooΔ]"));
	}
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(MinHashImplementationName.class).usingGetClass().verify();
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, new MissingParameterException("minhash implementation name"));
		failConstruct("   \n  ", new MissingParameterException("minhash implementation name"));
		failConstruct("    fo\no\boΔ\n", new IllegalParameterException(
				"minhash implementation name contains control characters"));
		failConstruct(TestCommon.LONG1001.substring(0, 257), new IllegalParameterException(
				"minhash implementation name size greater than limit 256"));
	}

	private void failConstruct(final String name, final Exception exception) {
		try {
			new MinHashImplementationName(name);
			fail("created bad display name");
		} catch (Exception e) {
			TestCommon.assertExceptionCorrect(e, exception);
		}
	}
	
}
