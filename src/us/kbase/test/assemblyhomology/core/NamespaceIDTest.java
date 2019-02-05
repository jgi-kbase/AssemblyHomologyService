package us.kbase.test.assemblyhomology.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.test.assemblyhomology.TestCommon;

public class NamespaceIDTest {
	
	@Test
	public void constructor() throws Exception {
		final NamespaceID n = new NamespaceID("    aA1234567890_   ");
		assertThat("incorrect displayname", n.getName(), is("aA1234567890_"));
		assertThat("incorrect toString", n.toString(),
				is("NamespaceID [name=aA1234567890_]"));
	}
	
	@Test
	public void equals() throws Exception {
		EqualsVerifier.forClass(NamespaceID.class).usingGetClass().verify();
	}
	
	@Test
	public void constructFail() throws Exception {
		failConstruct(null, new MissingParameterException("namespaceID"));
		failConstruct("   \n  ", new MissingParameterException("namespaceID"));
		failConstruct("    fo\no\boΔ\n", new IllegalParameterException(
				"namespaceID contains control characters"));
		failConstruct("    foooΔ\n", new IllegalParameterException(
				"Illegal character in namespace id foooΔ: Δ"));
		failConstruct(TestCommon.LONG1001.substring(0, 257), new IllegalParameterException(
				"namespaceID size greater than limit 256"));
	}

	private void failConstruct(final String name, final Exception exception) {
		try {
			new NamespaceID(name);
			fail("created bad namespace ID");
		} catch (Exception e) {
			TestCommon.assertExceptionCorrect(e, exception);
		}
	}

}
