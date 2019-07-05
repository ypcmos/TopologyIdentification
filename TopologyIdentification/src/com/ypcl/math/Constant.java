package com.ypcl.math;

public class Constant {
	public final static double NEARZERO = 1e-9;
	
	public static boolean isZero(double v) {
		if (Math.abs(v) < NEARZERO) {
			return true;
		} else {
			return false;
		}
	}
}
