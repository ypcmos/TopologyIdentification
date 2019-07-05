package com.ypcl.ieee;

public class Bus {
	public static final int SLACK = 1, PV = 2, PQ = 3;
	private int id;
	private String name;
	private int type;
	
	double v, thta, lp, lq, gp, gq, qmax, qmin, b;
	
	public Bus() {
		
	}
	
	public Bus(int id, String name, int type, double v, double thta, double lp, double lq, double gp, double gq, 
			double qmax, double qmin, double b) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.v = v;
		this.thta = thta;
		this.lp = lp;
		this.lq = lq;
		this.gp = gp;
		this.gq = gq;
		this.qmax = qmax;
		this.qmin = qmin;
		this.b= b;	
	}

	public int getId() {
		return id;
	}

	public Bus setId(int id) {
		this.id = id;
		return this;
	}

	public String getName() {
		return name;
	}

	public Bus setName(String name) {
		this.name = name;
		return this;
	}

	public int getType() {
		return type;
	}

	public Bus setType(int type) {
		this.type = type;
		return this;
	}

	public double getV() {
		return v;
	}

	public Bus setV(double v) {
		this.v = v;
		return this;
	}

	public double getThta() {
		return thta;
	}

	public Bus setThta(double thta) {
		this.thta = thta;
		return this;
	}

	public double getLp() {
		return lp;
	}

	public Bus setLp(double lp) {
		this.lp = lp;
		return this;
	}

	public double getLq() {
		return lq;
	}

	public Bus setLq(double lq) {
		this.lq = lq;
		return this;
	}

	public double getGp() {
		return gp;
	}

	public Bus setGp(double gp) {
		this.gp = gp;
		return this;
	}

	public double getGq() {
		return gq;
	}

	public Bus setGq(double gq) {
		this.gq = gq;
		return this;
	}

	public double getQmax() {
		return qmax;
	}

	public Bus setQmax(double qmax) {
		this.qmax = qmax;
		return this;
	}

	public double getQmin() {
		return qmin;
	}

	public Bus setQmin(double qmin) {
		this.qmin = qmin;
		return this;
	}

	public double getB() {
		return b;
	}

	public Bus setB(double b) {
		this.b = b;
		return this;
	}
}
