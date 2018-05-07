package us.kbase.assemblyhomology.minhash.mash;

import java.nio.file.Path;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.minhash.MinHashImplementation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationFactory;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashInitException;

public class MashFactory implements MinHashImplementationFactory {

	//TODO JAVADOC
	//TODO TEST
	
	@Override
	public MinHashImplementation getImplementation(final Path tempFileDirectory)
			throws MinHashInitException {
		return new Mash(tempFileDirectory);
	}

	@Override
	public MinHashImplementationName getImplementationName() {
		return Mash.getImplementationName();
	}
	
	@Override
	public Optional<Path> getExpectedFileExtension() {
		return Mash.getExpectedFileExtension();
	}

}
