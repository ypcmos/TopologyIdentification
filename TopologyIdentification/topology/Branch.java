package com.ypcl.estimation.topology;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.ypcl.math.Complex;
import com.ypcl.struct.IntPair;

final public class Branch implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final int PIJ = 1, PJI = 2, PIJ_CAL = 3, PJI_CAL = 4, QIJ = 5, QJI = 6, QIJ_CAL = 7, QJI_CAL = 8;
	public static final int NORMAL = 1, TRANSFORMER = 2;
	private IntPair id;
	private Map<Integer, Double> data = new HashMap<Integer, Double>();
	private double r, x, b, k;
	private int type;
	private String name;
	
	public Branch(IntPair id, int type, double r, double x, double b, double k, String name) {
		this.id = id;
		this.type = type;
		this.r = r;
		this.x = x;
		this.b = b;
		this.k = k;
		this.name = name;
	}
	
	public Branch(IntPair id, double r, double x, double b, String name) {
		this(id, NORMAL, r, x, b, 0, name);
	}
	
	public Branch(IntPair id, double r, double x, double b) {
		this(id, NORMAL, r, x, b, 0, "branch_" + String.valueOf(id));
	}
	
	public Branch(int i, int j, double r, double x, double b) {
		this(new IntPair(i, j), r, x, b);
	}
	
	public Branch(int i, int j, double r, double x, double b, String name) {
		this(new IntPair(i, j), r, x, b, name);
	}
	
	public Branch(int i, int j, int type, double r, double x, double b, double k, String name) {
		this(new IntPair(i, j), type, r, x, b, k, name);
	}

	public Branch eat(Branch branch) {
		Complex z1 = new Complex(r, x), z2 = new Complex(branch.r, branch.x);
		Complex z = z1.mul(z2).div(z1.add(z2));
		r = z.getReal();
		x = z.getImag();
		b = b + branch.b;
		
		if (branch.data.size() != 0) {
			data.putAll(branch.data);
		}
		return this;
	}
	
	public Double getData(int key) {
		return data.get(key);
	}

	public Branch putData(int key, double value) {
		this.data.put(key, value);
		return this;
	}

	public IntPair getId() {
		return id;
	}

	public Branch setId(IntPair pair) {
		this.id = pair;
		return this;
	}
	
	public double getR() {
		return r;
	}

	public double getX() {
		return x;
	}

	public double getB() {
		return b;
	}

	public double getK() {
		return k;
	}

	public int getType() {
		return type;
	}

	public String getName() {
		return name;
	}
	
	public Branch removeData(int key) {
		data.remove(key);
		return this;
	}
	
	@Override
	public String toString() {
		return id + "," + type + "," + name + "," + r + "," + x + "," + b + "," + k + ',' + data;
	}
}
