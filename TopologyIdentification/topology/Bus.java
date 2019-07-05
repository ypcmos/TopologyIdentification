package com.ypcl.estimation.topology;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

final public class Bus implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final int V = 1, CITA = 2, P = 3, Q = 4, V_CAL = 5, CITA_CAL = 6, P_CAL = 7, Q_CAL = 8;
	public static final int SLACK = 1, NORMAL = 2, VIRTUAL = 3;
	
	private int id;
	private double b;
	int type;
	String name;
	private Map<Integer, Double> data = new HashMap<Integer, Double>();
	
	public Bus(int id, double b, int type, String name) {
		this.id = id;
		this.b = b;
		this.type = type;
		this.name = name;
	}
	
	public Bus(Bus bus) {
		this(bus.id, bus.b, bus.type, bus.name);
	}
	
	public Bus(int id, double b, int type) {
		this(id, b, type, "bus_" + String.valueOf(id));
	}
	
	public Bus putData(int key, double val) {
		data.put(key, val);
		return this;
	}
	
	public Bus removeData(int key) {
		data.remove(key);
		return this;
	}
	
	public Double getData(int key) {
		return data.get(key);
	}
	
	public int getId() {
		return id;
	}

	public Bus setId(int id) {
		this.id = id;
		return this;
	}
	
	public double getB() {
		return b;
	}

	public int getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	public Bus setName(String name) {
		this.name = name;
		return this;
	}
	
	@Override
	public String toString() {
		return id + "," + type + "," + name + "," + b + "," + data;
	}
}
