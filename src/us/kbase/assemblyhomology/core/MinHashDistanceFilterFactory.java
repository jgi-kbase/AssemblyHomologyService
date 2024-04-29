package us.kbase.assemblyhomology.core;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.minhash.MinHashDistanceCollector;
import us.kbase.assemblyhomology.minhash.MinHashDistanceFilter;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashDistanceFilterException;

/** A factory for producing Minhash filters.
 * @see MinHashDistanceFilter
 * @author gaprice@lbl.gov
 *
 */
public interface MinHashDistanceFilterFactory {

	/** Get the ID of this factory.
	 * @return the ID.
	 */
	FilterID getID();
	
	/** Get the authentication source used by this filter, if any.
	 * @return A string providing the name of the authentication source used by the this
	 * filter (e.g. KBase, JGI, etc.) or absent() if the filter does not use an authentication
	 * source.
	 */
	Optional<String> getAuthSource();
	
	/** Get a filter that passes on unfiltered distances to a provided collector.
	 * @param collector the collector that is the final destination for any unfiltered distances.
	 * @param token the authentication token to be used by the filter. Pass null if no token
	 * is available. If the filter requires a token, the filter will thrown an error.
	 * @return the new filter.
	 * @throws MinHashDistanceFilterException if the filter could not be built.
	 */
	MinHashDistanceFilter getFilter(MinHashDistanceCollector collector, Token token)
			throws MinHashDistanceFilterException;
	
	
	/** Validate that a sequence ID is a valid ID for this filter.
	 * @param id the ID to validate.
	 * @return true if the sequence ID is valid, false otherwise.
	 */
	boolean validateID(String id);
}
