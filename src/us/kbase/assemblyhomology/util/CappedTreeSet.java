package us.kbase.assemblyhomology.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.TreeSet;

/** A {@link TreeSet} with a maximum size. As new elements are added to the set, if the set is at
 * capacity the new elements are compared to the minimum element, and if greater,
 * the minimum element is ejected and the new item inserted into the set.
 * @author gaprice@lbl.gov
 *
 * @param <T> the type of objects in the set.
 */
public class CappedTreeSet<T extends Comparable<T>> {

	// for a generally useful implementation allow a comparator in the constructor
	// and other constructors with default args other than size
	
	private final TreeSet<T> tree;
	private final int size;
	private final boolean descending;
	
	/** Create a new capped set.
	 * @param size the maximum capacity of the set.
	 * @param descending if true, the largest element is ejected from the set when inserting a new
	 * element into a set at maximum capacity if the new item is smaller than the largest element.
	 */
	public CappedTreeSet(final int size, final boolean descending) {
		if (size < 1) {
			throw new IllegalArgumentException("size must be > 0");
		}
		tree = new TreeSet<>();
		this.size = size;
		this.descending = descending;
	}
	
	/** Get the maximum size of the set.
	 * @return the maximum size of the set.
	 */
	public int getMaximumSize() {
		return size;
	}

	/** True if the the largest elements, rather than the smallest, are ejected from the set.
	 * @return true if the set is handled as a descending sort.
	 */
	public boolean isDescending() {
		return descending;
	}
	
	/** Get the current size of the set.
	 * @return the set's size.
	 */
	public int size() {
		return tree.size();
	}

	/** Returns a new set containing the current contents of this set.
	 * @return the new set.
	 */
	public TreeSet<T> toTreeSet() {
		return new TreeSet<>(tree);
	}

	/** Add an item to the set.
	 * @param item the item to add to the set.
	 */
	public void add(final T item) {
		checkNotNull(item, "item");
		if (tree.size() < size) {
			tree.add(item);
		} else {
			if (descending) {
				if (tree.last().compareTo(item) > 0) {
					tree.add(item);
					tree.pollLast();
				}
			} else {
				if (tree.first().compareTo(item) < 0) {
					tree.add(item);
					tree.pollFirst();
				}
			}
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (descending ? 1231 : 1237);
		result = prime * result + size;
		result = prime * result + ((tree == null) ? 0 : tree.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		CappedTreeSet other = (CappedTreeSet) obj;
		if (descending != other.descending) {
			return false;
		}
		if (size != other.size) {
			return false;
		}
		if (tree == null) {
			if (other.tree != null) {
				return false;
			}
		} else if (!tree.equals(other.tree)) {
			return false;
		}
		return true;
	}

}
