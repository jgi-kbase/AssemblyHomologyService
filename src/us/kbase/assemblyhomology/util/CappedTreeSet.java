package us.kbase.assemblyhomology.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.SortedSet;
import java.util.TreeSet;

public class CappedTreeSet<T extends Comparable<T>> {
	
	//TODO JAVADOC
	//TODO TESTS

	// for a generally useful implementation allow a comparator in the constructor
	// and other constructors with default args other than size
	
	private final TreeSet<T> tree;
	private final int size;
	private final boolean descending;
	
	public CappedTreeSet(final int size, final boolean descending) {
		if (size < 0) {
			throw new IllegalArgumentException("size must be > 0");
		}
		tree = new TreeSet<>();
		this.size = size;
		this.descending = descending;
	}
	
	public SortedSet<T> toSortedSet() {
		return new TreeSet<>(tree);
	}

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
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CappedTreeSet [tree=");
		builder.append(tree);
		builder.append(", size=");
		builder.append(size);
		builder.append(", descending=");
		builder.append(descending);
		builder.append("]");
		return builder.toString();
	}
	
	public static void main(final String[] args) {
		final CappedTreeSet<Integer> cts = new CappedTreeSet<>(3, false);
		cts.add(10);
		cts.add(5);
		cts.add(20);
		System.out.println(cts);
		cts.add(30);
		System.out.println(cts);
		cts.add(4);
		System.out.println(cts);
		cts.add(15);
		System.out.println(cts);
		cts.add(7);
		System.out.println(cts);
	}
}
