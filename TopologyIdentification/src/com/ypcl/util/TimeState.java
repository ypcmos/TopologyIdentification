package com.ypcl.util;

public class TimeState {
	private long begin = System.currentTimeMillis();
	
	public long past() {
		return System.currentTimeMillis() - begin;
	}
	
}
