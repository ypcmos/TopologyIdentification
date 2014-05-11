package com.ypcl.struct;

import java.io.Serializable;

public class IntPair implements Comparable<IntPair>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4377079087101300460L;
	protected Integer i, j;
	
	public IntPair(int i, int j) {
		set(i, j);
	}
	
	public IntPair set(int i, int j) {
		this.i = i;
		this.j = j;
		return this;
	}
	
	
	public IntPair setI(int i) {
		this.i = i;
		return this;
	}
	
	public IntPair setJ(int j) {
		this.j = j;
		return this;
	}
	
	
	public int getI() {
		return i;
	}
	
	public int getJ() {
		return j;
	}

	public IntPair exchange() {
		return new IntPair(j, i);
	}
	
	@Override
	public int compareTo(IntPair tp) {
		int cmp = i.compareTo(tp.i);
		
		if (cmp != 0) {
			return cmp;
		}
		
		return j.compareTo(tp.j);
	}
	
	@Override
	public int hashCode() {
		return i.hashCode() * 47 + j.hashCode();
	}
	
	@Override 
	public String toString() {
		return "(" + i + "," + j + ")";
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == (IntPair)o) {
			return true;
		}
		
		if (o instanceof IntPair) {
			IntPair tp = (IntPair)o;
			return i.equals(tp.i) && j.equals(tp.j);
		}
		
		return false;
	}

	public IntPair set(IntPair pair) {
		return set(pair.getI(), pair.getJ());
	}	
}

