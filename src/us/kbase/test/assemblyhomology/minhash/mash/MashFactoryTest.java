package us.kbase.test.assemblyhomology.minhash.mash;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.mash.Mash;
import us.kbase.assemblyhomology.minhash.mash.MashFactory;
import us.kbase.test.assemblyhomology.TestCommon;

public class MashFactoryTest {

	@Test
	public void getImplementationName() throws Exception {
		assertThat("incorrect name", new MashFactory().getImplementationName(),
				is(new MinHashImplementationName("mash")));
	}
	
	@Test
	public void getExpectedFileExtension() throws Exception {
		assertThat("incorrect exception", new MashFactory().getExpectedFileExtension(),
				is(Optional.of(Paths.get("msh"))));
	}
	
	@Test
	public void getImplementation() throws Exception {
		final Path temp = TestCommon.getTempDir();
		
		final Mash impl = (Mash) new MashFactory().getImplementation(temp);
		
		assertThat("incorrect temp dir", impl.getTemporaryFileDirectory(), is(temp));
		assertThat("incorrect info", impl.getImplementationInformation(),
				is(new MinHashImplementationInformation(
						new MinHashImplementationName("mash"),
						"2.0", // might need to be smarter about this
						Paths.get("msh"))));
	}
	
}
