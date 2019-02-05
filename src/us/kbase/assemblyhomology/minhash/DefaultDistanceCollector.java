package us.kbase.assemblyhomology.minhash;

import java.util.TreeSet;

import us.kbase.assemblyhomology.util.CappedTreeSet;

/** The default MinHash distance collector. Collects all provided sequences up to a
 * maximum count. After the maximum count is reached, the largest distance is discarded when a
 * new distance is added.
 * @author gaprice@lbl.gov
 *
 */
public class DefaultDistanceCollector implements MinHashDistanceCollector {

	private final CappedTreeSet<MinHashDistance> dists;
	
	/** Create the collector.
	 * @param size the maximum number of distances to collect.
	 */
	public DefaultDistanceCollector(final int size) {
		dists = new CappedTreeSet<>(size, true);
	}
	
	@Override
	public void accept(final MinHashDistance dist) {
		dists.add(dist);
	}

	@Override
	public TreeSet<MinHashDistance> getDistances() {
		return dists.toTreeSet();
	}

}
