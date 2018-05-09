package us.kbase.assemblyhomology.minhash;

import java.util.List;

import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;

/** An implementation of the MinHash algorithm like Mash or Sourmash.
 * @author gaprice@lbl.gov
 *
 */
public interface MinHashImplementation {
	
	//TODO JAVADOC

	MinHashImplementationInformation getImplementationInformation();
	
	MinHashSketchDatabase getDatabase(MinHashDBLocation location) throws MinHashException;
	
	List<String> getSketchIDs(MinHashSketchDatabase db) throws MinHashException;
	
	// if maxReturnCount < 1 it is treated as infinite
	MinHashDistanceSet computeDistance(
			MinHashSketchDatabase query,
			MinHashSketchDatabase reference,
			int maxReturnCount,
			boolean strict)
			throws MinHashException;
	
}
