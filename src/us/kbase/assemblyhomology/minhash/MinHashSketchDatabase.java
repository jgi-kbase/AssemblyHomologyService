package us.kbase.assemblyhomology.minhash;

import java.util.List;

/** A database of MinHash sketches.
 * @author gaprice@lbl.gov
 *
 */
public interface MinHashSketchDatabase {
	
	//TODO JAVADOC

	MinHashParameters getParameterSet();
	
	MinHashDBLocation getLocation();
	
	int getSketchCount();
	
	List<String> getSketchIDs();

	MinHashImplementationInformation getImplementationInformation(); 
	
}
