package us.kbase.assemblyhomology.minhash;

import static com.google.common.base.Preconditions.checkNotNull;

/** A pass through filter that passes all distances directly to a {@link MinHashDistanceCollector}.
 * @author gaprice@lbl.gov
 *
 */
public class DefaultDistanceFilter implements MinHashDistanceFilter {

	private final MinHashDistanceCollector collector;
	
	/** Create the filter.
	 * @param collector the collector that is the final destination for any distances passed into
	 * the filter.
	 */
	public DefaultDistanceFilter(final MinHashDistanceCollector collector) {
		checkNotNull(collector, "collector");
		this.collector = collector;
	}
	
	@Override
	public void accept(MinHashDistance dist) {
		collector.accept(dist);
	}

	@Override
	public void flush() {
		// nothing to do
	}

}
