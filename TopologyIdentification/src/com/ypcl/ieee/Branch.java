package com.ypcl.ieee;

public class Branch {
	public static final int LINE = 1, TRANSFORMER = 2;
	private int id;
	
	private int i, j;
	private int type;
	private double r, x, b;
	private double k;
	
	private double p1, p2, q1, q2;
	
	public Branch() {
		
	}
	
	public Branch(int id, int i, int j, int type, double r, double x, double b, double k) {
		this.id = id;
		this.i = i;
		this.j = j;
		this.type = type;
		this.r = r;
		this.x = x;
		this.b = b;
		this.k = k;
	}
	
	public Branch(int id, int i, int j, int type, double r, double x, double b) {
		this.id = id;
		this.i = i;
		this.j = j;
		this.type = type;
		this.r = r;
		this.x = x;
		this.b = b;
		this.k = 0;
	}
	
	public int getId() {
		return id;
	}
	
	public Branch setId(int id) {
		this.id = id;
		return this;
	}
	
	public int getI() {
		return i;
	}
	
	public Branch setI(int i) {
		this.i = i;
		return this;
	}
	
	public int getJ() {
		return j;
	}
	
	public Branch setJ(int j) {
		this.j = j;
		return this;
	}
	
	public int getType() {
		return type;
	}
	
	public Branch setType(int type) {
		this.type = type;
		return this;
	}
	
	public double getR() {
		return r;
	}
	
	public Branch setR(double r) {
		this.r = r;
		return this;
	}
	
	public double getX() {
		return x;
	}
	
	public Branch setX(double x) {
		this.x = x;
		return this;
	}
	
	public double getB() {
		return b;
	}
	
	public Branch setB(double b) {
		this.b = b;
		return this;
	}

	public double getK() {
		return k;
	}

	public Branch setK(double k) {
		this.k = k;
		return this;
	}

	public double getP1() {
		return p1;
	}

	public Branch setP1(double p1) {
		this.p1 = p1;
		return this;
	}

	public double getP2() {
		return p2;
	}

	public Branch setP2(double p2) {
		this.p2 = p2;
		return this;
	}

	public double getQ1() {
		return q1;
	}

	public Branch setQ1(double q1) {
		this.q1 = q1;
		return this;
	}

	public double getQ2() {
		return q2;
	}

	public Branch setQ2(double q2) {
		this.q2 = q2;
		return this;
	}
	
	
}
