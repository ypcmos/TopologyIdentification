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
	private double r = 0, x = 0, bl = 0, br = 0, k = 0, gl = 0, gr = 0;
	private int type;
	private String name;
	
	public Branch(IntPair id, int type, double r, double x, double b, double k, String name) {
		this.id = id;
		this.type = type;
		this.r = r;
		this.x = x;
		this.bl = b;
		this.br = b;
		this.k = k;
		this.name = name;
		
		if (type == TRANSFORMER) {
			transformerToLine();
		}
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

	public Branch transformerToLine() {
		Complex y = new Complex(1).div(new Complex(r, x));
		Complex cbl = y.mul((k - 1) / k), cbr = y.mul((1 - k) / Math.pow(k, 2)); 
		gl += cbl.getReal();
		bl += cbl.getImag();
		gr += cbr.getReal();
		br += cbr.getImag();
		r *= k;
		x *= k;
		return this;
	}
	
	public Branch eat(Branch branch) {
		Complex z1 = new Complex(r, x), z2 = new Complex(branch.r, branch.x);
		Complex z = z1.mul(z2).div(z1.add(z2));
		r = z.getReal();
		x = z.getImag();
		bl += branch.bl;
		br += branch.br;
		
		gl += branch.gl;
		gr += branch.gr;
		
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

	public Branch setBl(double v) {
		bl = v;
		return this;
	}
	
	public Branch setBr(double v) {
		br = v;
		return this;
	}
	
	public Branch setGl(double v) {
		gl = v;
		return this;
	}
	
	public Branch setGr(double v) {
		gr = v;
		return this;
	}
	
	public double getBl() {
		return bl;
	}
	
	public double getBr() {
		return br;
	}
	
	public double getGl() {
		return gl;
	}
	
	public double getGr() {
		return gr;
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
		return id + "," + type + "," + name + "," + r + ","
				+ x + ",(" + gl + "," + bl + ")," + ",(" + gr + "," + br + ")," + k + ',' + data;
	}
}
