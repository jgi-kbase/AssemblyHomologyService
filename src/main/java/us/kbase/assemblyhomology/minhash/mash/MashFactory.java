package us.kbase.assemblyhomology.minhash.mash;

import java.nio.file.Path;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.minhash.MinHashImplementation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationFactory;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashInitException;

/** A factory for a Mash implementation of {@link MinHashImplementation}.
 * @author gaprice@lbl.gov
 *
 */
public class MashFactory implements MinHashImplementationFactory {

	@Override
	public MinHashImplementation getImplementation(
			final Path tempFileDirectory,
			final int minhashTimeout)
			throws MinHashInitException {
		return new Mash(tempFileDirectory, minhashTimeout);
	}

	@Override
	public MinHashImplementationName getImplementationName() {
		return Mash.getImplementationName();
	}
	
	@Override
	public Optional<Path> getExpectedFileExtension() {
		return Optional.of(Mash.getExpectedFileExtension());
	}

}
