package us.kbase.assemblyhomology.minhash;

import us.kbase.assemblyhomology.minhash.exceptions.MinHashDistanceFilterException;

/** A filter for MinHash distances. The properties of the filter depend on the implementation,
 * but often checks that a user has authorization to view the sequences or other functionality
 * that filters out sequences from the full set returned by the MinHash implementation.
 * 
 * The filters typically feed into a {@link MinHashDistanceCollector}, and may buffer the
 * distances so that filtering can be performed batch-wise.
 * @author gaprice@lbl.gov
 *
 */
public interface MinHashDistanceFilter {

	/** Accept a distance into the filter. The distance may be transferred directly to the final
	 * collection point for the distance or be buffered for batch processing.
	 * @param dist the distance.
	 * @throws MinHashDistanceFilterException if the filter encounters a problem.
	 */
	void accept(MinHashDistance dist) throws MinHashDistanceFilterException;
	
	/** Perform any necessary processing on any distances held by the filter and pass them on
	 * to their final destination.
	 * @throws MinHashDistanceFilterException if the filter encounters a problem.
	 */
	void flush() throws MinHashDistanceFilterException;
	
}
