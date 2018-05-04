package us.kbase.assemblyhomology.minhash;

import java.nio.file.Path;

import us.kbase.assemblyhomology.minhash.exceptions.MinHashInitException;

public interface MinHashImplementationFactory {
	
	//TODO JAVADOC
	
	MinHashImplementation getImplementation(final Path tempFileDirectory)
			throws MinHashInitException;
	
	MinHashImplementationName getImplementationName();

}
