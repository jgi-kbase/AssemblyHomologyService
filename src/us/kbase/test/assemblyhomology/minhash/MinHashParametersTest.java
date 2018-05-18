package us.kbase.test.assemblyhomology.minhash;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.google.common.base.Optional;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.test.assemblyhomology.TestCommon;

public class MinHashParametersTest {
	
	@Test
	public void equals() {
		EqualsVerifier.forClass(MinHashParameters.class).usingGetClass().verify();
	}

	@Test
	public void buildWithScaling() {
		final MinHashParameters p = MinHashParameters.getBuilder(67)
				.withSketchSize(31) // expect to be blown away
				.withScaling(1042)
				.build();
		
		assertThat("incorrect kmer", p.getKmerSize(), is(67));
		assertThat("incorrect scaling", p.getScaling(), is(Optional.of(1042)));
		assertThat("incorrect size", p.getSketchSize(), is(Optional.absent()));
	}
	
	@Test
	public void buildWithSize() {
		final MinHashParameters p = MinHashParameters.getBuilder(67)
				.withScaling(1042) // expect to be blown away
				.withSketchSize(31)
				.build();
		
		assertThat("incorrect kmer", p.getKmerSize(), is(67));
		assertThat("incorrect scaling", p.getScaling(), is(Optional.absent()));
		assertThat("incorrect size", p.getSketchSize(), is(Optional.of(31)));
	}
	
	@Test
	public void getBuilderFail() {
		try {
			MinHashParameters.getBuilder(0);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalArgumentException("kmerSize < 1"));
		}
	}
	
	@Test
	public void withScalingFail() {
		try {
			MinHashParameters.getBuilder(1).withScaling(0);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalArgumentException("scaling < 1"));
		}
	}
	
	@Test
	public void withSizeFail() {
		try {
			MinHashParameters.getBuilder(1).withSketchSize(0);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalArgumentException("sketchSize < 1"));
		}
	}
	
	@Test
	public void buildFail() {
		try {
			MinHashParameters.getBuilder(31).build();
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, new IllegalStateException(
					"One of scaling or sketchSize must be set"));
		}
	}
}
