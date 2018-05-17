package us.kbase.test.assemblyhomology.minhash;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.nio.file.Paths;

import org.junit.Test;

import com.google.common.base.Optional;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.test.assemblyhomology.TestCommon;

public class MinHashImplementationInformationTest {

	@Test
	public void equals() {
		EqualsVerifier.forClass(MinHashImplementationInformation.class).usingGetClass().verify();
	}
	
	@Test
	public void construct() throws Exception {
		final MinHashImplementationInformation i = new MinHashImplementationInformation(
				new MinHashImplementationName("foo"),
				"bar",
				Paths.get("baz"));
		
		assertThat("incorrect name", i.getImplementationName(),
				is(new MinHashImplementationName("foo")));
		assertThat("incorrect ver", i.getImplementationVersion(), is("bar"));
		assertThat("incorrect ext", i.getExpectedFileExtension(),
				is(Optional.of(Paths.get("baz"))));
	}
	
	
	@Test
	public void constructNullPathExt() throws Exception {
		final MinHashImplementationInformation i = new MinHashImplementationInformation(
				new MinHashImplementationName("foo"),
				"bar",
				null);
		
		assertThat("incorrect name", i.getImplementationName(),
				is(new MinHashImplementationName("foo")));
		assertThat("incorrect ver", i.getImplementationVersion(), is("bar"));
		assertThat("incorrect ext", i.getExpectedFileExtension(),
				is(Optional.absent()));
	}
	
	@Test
	public void constructFail() throws Exception {
		final MinHashImplementationName n = new MinHashImplementationName("n");
		failConstruct(null, "v", new NullPointerException("implementationName"));
		failConstruct(n, null, new IllegalArgumentException(
				"implementationVersion cannot be null or whitespace only"));
		failConstruct(n, "    \t   \n   ", new IllegalArgumentException(
				"implementationVersion cannot be null or whitespace only"));
	}
	
	private void failConstruct(
			final MinHashImplementationName name,
			final String version,
			final Exception expected) {
		try {
			new MinHashImplementationInformation(name, version, null);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
}
