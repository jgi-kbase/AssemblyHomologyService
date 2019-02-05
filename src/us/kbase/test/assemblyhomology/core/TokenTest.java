package us.kbase.test.assemblyhomology.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.core.Token;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.test.assemblyhomology.TestCommon;

public class TokenTest {

	@Test
	public void equals() {
		EqualsVerifier.forClass(Token.class).usingGetClass().verify();
	}
	
	@Test
	public void construct() throws Exception {
		final Token t = new Token("foo");
		assertThat("incorrect token", t.getToken(), is("foo"));
	}
	
	@Test
	public void constructFail() {
		failConstruct(null, new MissingParameterException("token"));
		failConstruct("    \t    ", new MissingParameterException("token"));
	}
	
	private void failConstruct(final String token, final Exception expected) {
		try {
			new Token(token);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
