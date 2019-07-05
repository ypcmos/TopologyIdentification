package com.ypcl.math;

import static com.ypcl.math.Constant.*;

import java.util.List;

public class Matrix {
	static public interface Each {
		double cal(double v);
	}
	
	private double[][] data;
	
	public Matrix(int row) {
		data = new double[row][1];
	}
	
	public Matrix(Vector vector) {
		int r = vector.size();
		data = new double[r][1];
		
		for (int i = 0; i < r; i++) {
			data[i][0] = vector.get(i);
		}
	}

	public Matrix(int row, int col) {
		data = new double[row][col];
	}

	public Matrix(Matrix m) {
		this(m.data);
	}

	public Matrix(double[][] d) {
		int row = d.length,
				col = d[0].length;
		data = new double[row][col];
		
		for (int i = 0; i < row; i++) {
			for (int j = 0; j < col; j++) {
				data[i][j] = d[i][j];
			}
		}
	}
	
	public Matrix(List<Vector> vs) {
		int row = vs.size(),
				col = vs.get(0).size();
		data = new double[row][col];
		
		for (int i = 0; i < row; i++) {
			Vector v = vs.get(i);
			for (int j = 0; j < col; j++) {
				data[i][j] = v.get(j);
			}
		}
	}
	
	public Matrix(double[] d) {
		int col = data.length;
		data = new double[1][col];
		for (int j = 0; j < col; j++) {
			data[0][j] = d[j];
		}
	}
	
	public Matrix(double d, double... ds) {
		int col = 1 + ds.length;
		data = new double[1][col];
		data[0][0] =  d;
		for (int j = 1; j < col; j++) {
			data[0][j] = ds[j - 1];
		}
	}
		
	public static Matrix unit(int r) {
		Matrix ret = new Matrix(r, r);
		ret.setUnit();
		return ret;
	}
	
	public static Matrix ones(int r, int c) {
		Matrix ret = new Matrix(r, c);
		ret.setValue(1);
		return ret;
	}
	
	public static Matrix zero(int r, int c) {
		Matrix ret = new Matrix(r, c);
		ret.setValue(0);
		return ret;
	}
	
	public static Matrix diag(int dimension, double value) {
		Matrix matrix = new Matrix(dimension, dimension);
		for (int i=0; i<dimension; i++)
			for (int j=0; j<dimension; j++)
				matrix.data[i][j] = ((i==j) ? value:0);
		return matrix;
	}

	public static Matrix diag(Matrix m) {
		int row = m.getRowCount(), col = m.getColumnCount();
		Matrix matrix = new Matrix(row, col);
		for (int i=0; i<row; i++)
			for (int j=0; j<col; j++)
				matrix.data[i][j] = ((i==j) ? m.get(i, j) : 0);
		return matrix;
	}
	
	public Matrix each(Each e) {
		Matrix m = new Matrix(this);
		
		int r = m.getRowCount(), c = m.getColumnCount();
		
		for (int i = 0; i < r; i++) {
			for (int j = 0; j < c; j++) {
				m.data[i][j] = e.cal(m.data[i][j]);
			}
		}
		return m;
	}
	
	public Matrix setUnit() {
		int row = data.length,
				col = data[0].length;
		for (int i=0;i<row;i++)
			for (int j=0;j<col;j++)
				data[i][j] = ((i==j) ? 1:0);
		return this;
	}
	public Matrix setValue(double v) {
		int row = data.length,
				col = data[0].length;
		for(int i=0; i<row; i++)
			for(int j=0; j<col; j++)
				data[i][j] = v;
		return this;
	}

	public int getRowCount() {
		return data.length;
	}

	public int getColumnCount() {
		return data[0].length;
	}

	private Matrix exchange(int i, int j) {
		double temp;
		int col = data[0].length;
		for (int k=0; k<col; k++) {
			temp = data[i][k];
			data[i][k] = data[j][k];
			data[j][k] = temp;
		}
		return this;
	}

	private Matrix multiple(int index, double mul) {
		int col = data[0].length;
		for (int j=0; j<col; j++) {
			data[index][j] *= mul;
		}
		return this;
	}

	private Matrix multipleAdd(int index, int src, double mul) {
		int col = data[0].length;
		for (int j=0; j<col; j++) {
			data[index][j] += data[src][j]*mul;
		}

		return this;
	}

	public Matrix transpose() {
		int row = data.length,
				col = data[0].length;
		Matrix ret = new Matrix(col, row);

		for (int i=0; i<row; i++) {
			for (int j=0; j<col; j++) {
				ret.data[j][i] = data[i][j];
			}
		}
		return ret;
	}

	public Matrix add(Matrix rhs) {
		int row = data.length,
				col = data[0].length;
		assert(row == rhs.getRowCount() && col == rhs.getColumnCount());

		Matrix ret = new Matrix(row, col);

		for (int i=0; i<row; i++) {
			for (int j=0; j<col; j++) {
				ret.data[i][j] = data[i][j] + rhs.data[i][j];
			}
		}
		return ret;
	}

	public Matrix sub(Matrix rhs) {
		int row = data.length,
				col = data[0].length;
		assert(row == rhs.getRowCount() && col == rhs.getColumnCount());

		Matrix ret = new Matrix(row, col);

		for (int i=0; i<row; i++) {
			for (int j=0; j<col; j++) {
				ret.data[i][j] = data[i][j] - rhs.data[i][j];
			}
		}
		return ret;

	}

	public Matrix mul(Matrix rhs) {
		int row = data.length,
				col = data[0].length;
		assert(col == rhs.getRowCount());
		
		Matrix ret = new Matrix(row, rhs.getColumnCount());
		double temp;
		for (int i=0; i<row; i++) {
			for (int j=0; j<rhs.getColumnCount(); j++) {
				temp = 0;
				for(int k=0; k<col; k++) {
					temp += data[i][k] * rhs.data[k][j];
				}
				ret.data[i][j] = temp;
			}
		}
		return ret;
	}
	
	public Vector mul(Vector v) {
		int row = data.length,
				col = data[0].length;
		assert(col == v.size());
		
		Vector ret = new Vector();
		double temp;
		for (int i=0; i<row; i++) {	
			temp = 0;
			for(int k=0; k<col; k++) {
				temp += data[i][k] * v.get(k);
			}
			ret.addLast(temp);	
		}
		return ret;
	}

	public Matrix div(Matrix rhs) throws Exception {
		return mul(rhs.inverse());
	}

	public Matrix mul(double d) {
		int row = data.length,
				col = data[0].length;
		Matrix ret = new Matrix(this);
		for (int i=0; i<row; i++)
			for (int j=0; j<col; j++)
				ret.data[i][j] *= d;
		return ret;
	}

	public Matrix divLeft(double d) throws Exception {
		return inverse().mul(d);
	}

	private int pivot(int row) {
		int index = row, r = getRowCount();

		for (int i=row+1; i<r; i++) {
			if (Math.abs(data[i][row]) > Math.abs(data[index][row]))
				index = i;
		}
		return index;
	}

	public Matrix inverse() {
		int row = data.length,
				col = data[0].length;
		assert (row == col);

		Matrix tmp = new Matrix(this);
		Matrix ret = Matrix.unit(row);    

		int maxIndex;
		double dMul;

		for (int i=0; i<row; i++) {
			maxIndex = tmp.pivot(i);
			if (Math.abs(tmp.data[maxIndex][i]) < NEARZERO) {
				throw new RuntimeException("Matrix is singular.");
			}


			if (maxIndex != i) {
				tmp.exchange(i,maxIndex);
				ret.exchange(i,maxIndex);
			}

			ret.multiple(i, 1.0 / tmp.data[i][i]);
			tmp.multiple(i, 1.0 / tmp.data[i][i]);

			for (int j=i+1; j<row; j++) {
				dMul = -tmp.data[j][i] / tmp.data[i][i];
				tmp.multipleAdd(j,i,dMul);
				ret.multipleAdd(j,i,dMul);
			}
		}

		for(int i=row-1; i>0; i--) {
			for(int j=i-1; j>=0; j--) {
				dMul = -tmp.data[j][i] / tmp.data[i][i];
				tmp.multipleAdd(j, i, dMul);
				ret.multipleAdd(j, i, dMul);
			}
		}       
		return ret;
	}

	public boolean isSquare() {
		return data.length == data[0].length;
	}

	public boolean isSymmetric() {
		int row = data.length,
				col = data[0].length;
		if(row != col) 
			return false;

		for (int i=0; i<row; i++)
			for (int j=i+1; j<col; j++)
				if (data[i][j] != data[j][i])
					return false;

		return true;
	}

	public double value() {
		int row = data.length,
				col = data[0].length;
		assert(row == 1 && col == 1);
		return data[0][0];
	}

	public double get(int i, int j) {
		return data[i][j];
	}
	
	public Matrix set(int i, int j, double value) {
		data[i][j] = value;
		return this;
	}
	
	public Vector vector() {
		assert(getColumnCount() == 1);
		Vector vector = new Vector();
		
		for (int i = 0; i < data.length; i++) {
			vector.addLast(data[i][0]);
		}
		return vector;	
	}
	
	public double[][] getData() {
		return data;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		int row = data.length,
				col = data[0].length;

		for (int i = 0; i < row; i++) {
			for (int j = 0; j < col; j++) {
				sb.append(data[i][j] + ",");
			}
			sb.append("\r\n");
		}
		return sb.toString();
	}
}
