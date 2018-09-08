package us.kbase.assemblyhomology.minhash;

import java.util.List;
import java.util.Map;

import us.kbase.assemblyhomology.minhash.exceptions.IncompatibleSketchesException;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;
import us.kbase.assemblyhomology.minhash.exceptions.NotASketchException;

/** An implementation of the MinHash algorithm like Mash or Sourmash.
 * @author gaprice@lbl.gov
 *
 */
public interface MinHashImplementation {
	
	/** Get information about the implementation.
	 * @return the implementation information.
	 */
	MinHashImplementationInformation getImplementationInformation();
	
	/** Get a sketch database from a MinHash database location.
	 * @param name the name to give to the database.
	 * @param location the location of the database.
	 * @return the database.
	 * @throws MinHashException if the database could not be loaded.
	 */
	MinHashSketchDatabase getDatabase(MinHashSketchDBName name, MinHashDBLocation location)
			throws MinHashException;
	
	/** Get the set of sequence IDs existing within a sketch database.
	 * @param db the database to interrogate.
	 * @return the list of sequence IDs.
	 * @throws MinHashException if the database could not be loaded.
	 */
	List<String> getSketchIDs(MinHashSketchDatabase db) throws MinHashException;
	
	/** Compute distances between a query sequence and set of reference sequence sketch databases.
	 * @param query the query sequence. The database must contain exactly one sequence.
	 * @param references the set of reference databases against which the query will be
	 * measured mapped to the filter to use to filter the results of the search against that
	 * database.
	 * @param strict if false, allow the query sequence sketch size to be larger than the
	 * reference databases' sketch size. Otherwise throw an {@link IncompatibleSketchesException}.
	 * @return a list of warnings regarding the minhash query.
	 * @throws MinHashException if the distances were unable to be calculated.
	 * @throws IncompatibleSketchesException if the sketches have incompatible parameters.
	 * @throws NotASketchException if one of the databases is invalid.
	 */
	List<String> computeDistance(
			MinHashSketchDatabase query,
			Map<MinHashSketchDatabase, MinHashDistanceFilter> references,
			boolean strict)
			throws MinHashException, IncompatibleSketchesException, NotASketchException;
	
}
