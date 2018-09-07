package us.kbase.assemblyhomology.minhash;

import java.util.TreeSet;

/** An interface for a collector of MinHash distances. 
 * 
 * Typical implementations may maintain a cap on the number of distances in memory, check that
 * a user has authorization to view the sequences, or other functionality that filters out
 * sequences from the full set returned by the MinHash implementation. The collector is used
 * so the filtering can happen online rather than collecting all the sequences in memory and then
 * filtering.
 * 
 * @author gaprice@lbl.gov
 *
 */
public interface MinHashDistanceCollector {
	
	/** Accept a distance for the collector.
	 * @param dist the distance.
	 */
	void accept(MinHashDistance dist);
	
	/** Get the distances accepted so far that have not been filtered by the collector. 
	 * @return the distances.
	 */
	TreeSet<MinHashDistance> getDistances();

}
