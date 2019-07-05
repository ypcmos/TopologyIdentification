package com.ypcl.struct;

public class SortablePair<T extends Comparable<T>> extends Pair<T> implements Comparable<SortablePair<T>> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3988990993206300078L;

	public SortablePair(T i, T j) {
		super(i, j);
	}

	@Override
	public int compareTo(SortablePair<T> o) {
		int cmp = i.compareTo(o.i);

		if (cmp != 0) {
			return cmp;
		}
		return j.compareTo(o.j);
	}
}
