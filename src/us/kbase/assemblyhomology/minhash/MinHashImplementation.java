package us.kbase.assemblyhomology.minhash;

/** An implementation of the MinHash algorithm like Mash or Sourmash.
 * @author gaprice@lbl.gov
 *
 */
public interface MinHashImplementation {
	
	//TODO JAVADOC

	MinHashImplemenationInformation getImplementationInformation();
	
	MinHashSketchDatabase getDatabase(MinHashDBLocation location);
	
}
