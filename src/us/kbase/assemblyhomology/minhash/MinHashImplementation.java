package us.kbase.assemblyhomology.minhash;

import java.util.List;

import us.kbase.assemblyhomology.minhash.mash.MashException;

/** An implementation of the MinHash algorithm like Mash or Sourmash.
 * @author gaprice@lbl.gov
 *
 */
public interface MinHashImplementation {
	
	//TODO JAVADOC

	MinHashImplementationInformation getImplementationInformation();
	
	MinHashSketchDatabase getDatabase(MinHashDBLocation location) throws MashException;
	
	List<String> getSketchIDs(MinHashSketchDatabase db) throws MashException;
	
}
