package us.kbase.assemblyhomology.filters;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.checkNoNullsInCollection;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashDistanceCollector;
import us.kbase.assemblyhomology.minhash.MinHashDistanceFilter;

/** A filter for KBase sequence differences. The filter inspects the sequence IDs in the
 * provided {@link MinHashDistance} instances and determines if the sequence exists in the set of
 * workspaces provided to the filter. If not, the filter does not pass the
 * distance on to the collector.
 * 
 * The filter determines which sequences are allowed based on the provided set of workspace IDs.
 * The filter expects sequence IDs in the format X_Y_Z, where X is the integer workspace ID,
 * Y the integer object ID, and Z the integer version. If X is not contained in the list of
 * workspace IDs, the distance is not passed on to the collector.
 * 
 * @author gaprice@lbl.gov
 *
 */
public class KBaseAuthenticatedFilter implements MinHashDistanceFilter {

	//TODO NOW TEST
	
	private Set<Long> workspaceIDs;
	private MinHashDistanceCollector collector;
	
	/** Create a filter.
	 * @param workspaceIDs the workspace IDs that determine which distances will be passed on
	 * to the collector.
	 * @param collector the final destination of unfiltered distances.
	 */
	public KBaseAuthenticatedFilter(
			final Set<Long> workspaceIDs,
			final MinHashDistanceCollector collector) {
		checkNotNull(collector, "collector");
		checkNoNullsInCollection(workspaceIDs, "workspaceIDs");
		this.workspaceIDs = Collections.unmodifiableSet(new HashSet<>(workspaceIDs));
		this.collector = collector;
	}

	@Override
	public void accept(MinHashDistance dist) {
		//TODO NOW check against ws id set
		collector.accept(dist);
	}

	@Override
	public void flush() {
		// do nothing
	}

	/** Get the workspace IDs provided to the filter.
	 * @return the workspace IDs.
	 */
	public Set<Long> getWorkspaceIDs() {
		return workspaceIDs;
	}
	
}
