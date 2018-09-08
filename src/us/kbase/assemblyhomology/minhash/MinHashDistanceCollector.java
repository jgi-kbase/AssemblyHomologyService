package us.kbase.assemblyhomology.minhash;

import java.util.TreeSet;

/** An interface for a collector of MinHash distances. 
 * 
 * Typical implementations maintain a cap on the number of distances in memory.The collector is
 * used so any filtering via {@link MinHashDistanceFilter} can happen online rather than
 * collecting all the sequences in memory and then filtering.
 * 
 * There is typically one collector per data set and one filter per target sketch database in
 * the dataset. The filters feed into the collector.
 * 
 * @author gaprice@lbl.gov
 *
 */
public interface MinHashDistanceCollector {
	
	/** Accept a distance for the collector.
	 * @param dist the distance.
	 */
	void accept(MinHashDistance dist);
	
	/** Get the distances accepted so far.
	 * @return the distances.
	 */
	TreeSet<MinHashDistance> getDistances();

}
