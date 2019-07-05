package com.ypcl.struct;

import java.io.Serializable;

public class Pair<T> implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2977964068161656839L;
	protected T i, j;

	public Pair(T i, T j) {
		set(i, j);
	}

	public Pair<T> set(T i, T j) {
		this.i = i;
		this.j = j;
		return this;
	}


	public Pair<T> setI(T i) {
		this.i = i;
		return this;
	}

	public Pair<T> setJ(T j) {
		this.j = j;
		return this;
	}


	public T getI() {
		return i;
	}

	public T getJ() {
		return j;
	}

	public Pair<T> exchange() {
		return new Pair<T>(j, i);
	}

	@Override
	public int hashCode() {
		return i.hashCode() * 47 + j.hashCode();
	}

	@Override 
	public String toString() {
		return "(" + i + "," + j + ")";
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (this == (Pair<T>)o) {
			return true;
		}

		if (o instanceof Pair) {
			Pair<T> tp = (Pair<T>)o;
			return i.equals(tp.i) && j.equals(tp.j);
		}

		return false;
	}

	public Pair<T> set(Pair<T> pair) {
		return set(pair.getI(), pair.getJ());
	}
}
