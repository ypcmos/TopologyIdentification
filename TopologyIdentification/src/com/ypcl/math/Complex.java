package com.ypcl.math;

public class Complex implements Cloneable{
	private double real;
	private double imag;
	
	public Complex() {
		real = 0.0;
		imag = 0.0;
	}
	
	public Complex(double r) {
		real = r;
		imag = 0.0;
	}
	
	public Complex(double r, double i) {
		real = r;
		imag = i;
	}
	
	public Object clone() {
		Complex newComplex = null;
		try {
			newComplex=(Complex)super.clone();
		} catch(CloneNotSupportedException e) {
			return null;
		}
		return newComplex;
	}
	
	public double getReal() {
		return real;
	}
	
	public void setReal(double d) {
		real = d;
	}
	
	public double getImag() {
		return imag;
	}
	
	public void setImag(double d) {
		imag = d;
	}
	
	public static Complex add(Complex l, Complex r) {
		Complex temp = new Complex();
		temp.real = l.real + r.real;
		temp.imag = l.imag + r.imag;
		return temp;
	}
	
	public Complex add(Complex r) {
		return add(this, r);
	}
	
	public static Complex sub(Complex l, Complex r) {
		Complex temp = new Complex();
		temp.real = l.real - r.real;
		temp.imag = l.imag - r.imag;
		return temp;
	}
	
	public Complex sub(Complex r) {
		return sub(this, r);
	}
	
	public static Complex mul(Complex l, Complex r) {
		Complex temp = new Complex();
		temp.real = l.real * r.real - l.imag * r.imag;
		temp.imag = l.real * r.imag + l.imag * r.real;
		return temp;
	}
	
	public Complex mul(Complex r) {
		return mul(this, r);
	}
	
	public Complex mul(double r) {
		return new Complex(real * r, imag * r);
	}
	
	public Complex div(double r) {
		return new Complex(real / r, imag / r);
	}
	
	public void conjugate() {
		imag = -imag;
	}
	
	public static Complex div(Complex l, Complex r) {
		Complex temp = (Complex)r.clone();
		temp.conjugate();
		temp = mul(mul(l, temp), (new Complex(1 / (Math.pow(r.real, 2.0) + Math.pow(r.imag, 2.0)))));
		
		return temp;
	}
	
	public Complex div(Complex r) {
		return div(this, r);
	}
	
	public static double abs(Complex c) {
		return Math.sqrt(Math.pow(c.real, 2.0) + Math.pow(c.imag, 2.0));
	}
	
	@Override
	public String toString() {		
		return "(" + real + "," + this.imag + ")";				
	}
}
