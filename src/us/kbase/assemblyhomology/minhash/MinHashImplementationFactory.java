package us.kbase.assemblyhomology.minhash;

import java.nio.file.Path;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.minhash.exceptions.MinHashInitException;

/** A factory for a MinHash implementation.
 * @author gaprice@lbl.gov
 *
 */
public interface MinHashImplementationFactory {
	
	/** Get the implementation from this factory.
	 * @param tempFileDirectory a directory the implementation can use to create temporary files.
	 * @param minhashTimeout the timeout to use for the minhash process.
	 * @return the implementation.
	 * @throws MinHashInitException if the implementation could not be created.
	 */
	MinHashImplementation getImplementation(Path tempFileDirectory, int minhashTimeout)
			throws MinHashInitException;
	
	/** Get the name of the implementation.
	 * @return the implementation name.
	 */
	MinHashImplementationName getImplementationName();
	
	/** Get the file extension, if any, that the implementation expects.
	 * @return the expected file extension.
	 */
	Optional<Path> getExpectedFileExtension();

}
