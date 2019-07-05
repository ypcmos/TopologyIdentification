package com.ypcl.math;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Vector implements Iterable<Double>{
	public static class Distance {
		public final static int EULER = 1; 
		private int method;
		
		public Distance(int method) {	
			this.method = method;
		}
		
		public Distance() {
			method = EULER;
		}
		
		public Distance setMethod(int method) {	
			this.method = method;
			return this;
		}
		
		public double distance(Vector arg1, Vector arg2) {
			switch (method) {
			case EULER:
				return eulerDistance(arg1, arg2);
			default:
				throw new RuntimeException("Method dosn't exist.");
			}
		}
		
		private double eulerDistance(Vector arg1, Vector arg2) {
			double sum = 0.0;
			int arg1Len = arg1.length(), arg2Len = arg2.length();
			if (arg1Len != arg2Len) {
				throw new RuntimeException("Vectors have different length.(vector1: length " + arg1.length() + " " + arg1 + "\r\n vector2: length " + arg2.length() + " " + arg2);
			}
			
			for (int i = 0; i < arg1Len; i++) {
				sum += Math.pow(arg1.get(i) - arg2.get(i), 2);
			}
			return Math.sqrt(sum);
		}
	
	}
	
	private List<Double> vector = new ArrayList<Double>();
	
	public Vector() {
	}
	
	public Vector(double[] data) {
		for (double v : data) {
			vector.add(v);
		}
	}
	
	public Vector(double d, double... ds) {
		vector.add(d);
		for (double v : ds) {
			vector.add(v);
		}
	}
	
	public Vector(List<Double> arrayList) {
		vector.addAll(arrayList);	
	}
	
	public Vector(Vector vector) {
		this.vector.addAll(vector.vector);	
	}
	
	public Vector addLast(Double element) {
		vector.add(element);
		return this;
	}
	
	public void zeroVector() {
		for (int i = 0; i < length(); i++) {
			vector.set(i, 0.0); 
		}
	}
	
	public static Vector zeroVector(int size) {
		Vector v = new Vector();
		for (int i = 0; i < size; i++) {
			v.addLast(0.0);
		}
		return v;
	}
	
	public static Vector ones(int s) {
		Vector vector = new Vector();
		for (int i = 0; i < s; i++) {
			vector.addLast(1.0); 
		}
		return vector;
	}
	
	public Vector add(Vector right) {	
		if (length() != right.length()) {
			throw new RuntimeException("size is different");
		}
		Vector ret = new Vector();
		for (int i = 0; i < length(); i++) {
			ret.addLast(vector.get(i) + right.get(i)); 
		}
		return ret;
	}
	
	public Vector sub(Vector right) {	
		if (length() != right.length()) {
			throw new RuntimeException("size is different");
		}
		Vector ret = new Vector();
		for (int i = 0; i < length(); i++) {
			ret.addLast(vector.get(i) - right.get(i)); 
		}
		return ret;
	}
	
	public double multiplicationSum(Vector r) {
		int len = length();
		if (len != r.length()) {
			throw new RuntimeException("size is different");
		}
		double sum = 0.0;
		for (int i = 0; i < len; i++) {
			sum += vector.get(i) * r.get(i);
		}
		return sum;
	}
	
	public Vector dotDiv(double r) {
		for (int i = 0; i < length(); i++) {
			vector.set(i, vector.get(i) / r); 
		}
		return this;
	}
	
	public double get(int i) {
		return vector.get(i);
	}
	
	public void set(int i, double elem) {
		vector.set(i, elem);
	}
	
	public int length() {
		return vector.size();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(5);
		for (int i = 0; i < length(); i++) {
			sb.append(df.format(vector.get(i)).toString() + ' ');
		}
		sb.append("]");
		return sb.toString();
	}

	@Override
	public Iterator<Double> iterator() {
		return new Iterator<Double>() {
			protected int begin = 0;
			protected int end = length(); 
			protected int step = 1;
		    public boolean hasNext() {
		        return begin < end;
		    }
		
		    public Double next() {
		        Double ret = vector.get(begin);
		        begin += step;
		        return ret;
		    }
		
		    public void remove() {
		        // no remove
		    }    
		};
	}
	
	public double max() {
		double max = Double.NEGATIVE_INFINITY;
		for (double v : vector) {
			if (max < v) {
				max = v;
			}
		}
		return max;
	}
	
	public double min() {
		double min = Double.POSITIVE_INFINITY;
		for (double v : vector) {
			if (min > v) {
				min = v;
			}
		}
		return min;
	}
	
	public  Vector normalizing() {
		double max = max(), min = min();
		
		for (int i = 0; i < vector.size(); i++) {
			vector.set(i, (vector.get(i) - min) / (max - min));
		}
		return this;
	}	
	
	public double[] toArray() {
		double[] ds = new double[length()];
		
		int i = 0;
		for (double v : vector) {
			ds[i++] = v;
		}
		return ds;
	}

	public int size() {
		return vector.size();
	}
	
	public double normInfinite() {
		double max = Double.NEGATIVE_INFINITY;
		for (double v : vector) {
			double pV = Math.abs(v);
			if (max < pV) {
				max = pV;
			}
		}
		return max;
	}
}
