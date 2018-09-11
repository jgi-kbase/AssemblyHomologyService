package us.kbase.assemblyhomology.core;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.core.FilterID;
import us.kbase.assemblyhomology.core.Token;
import us.kbase.assemblyhomology.minhash.MinHashDistanceCollector;
import us.kbase.assemblyhomology.minhash.MinHashDistanceFilter;

/** A factory for produing Minhash filters.
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
	 * source. If the authentication source is present,
	 * {@link #getFilter(MinHashDistanceCollector, Token)} must be used to get the filter from
	 * this factory.
	 */
	Optional<String> getAuthSource();
	
	/** Get a filter that passes on unfiltered distances to a provided collector and that does
	 * not require authentication.
	 * @param collector the collector that is the final destination for any unfiltered distances.
	 * @return the new filter.
	 */
	//TODO NOW throw an exception if a token is required
	MinHashDistanceFilter getFilter(MinHashDistanceCollector collector);
	
	/** Get a filter that passes on unfiltered distances to a provided collector and that
	 * requires authentication.
	 * @param collector the collector that is the final destination for any unfiltered distances.
	 * @param token the authentication token to be used by the filter.
	 * @return the new filter.
	 */
	MinHashDistanceFilter getFilter(MinHashDistanceCollector collector, Token token);
	
}
