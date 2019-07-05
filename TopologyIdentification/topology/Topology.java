package com.ypcl.estimation.topology;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.ypcl.estimation.datasource.ITextDataSource;
import com.ypcl.math.Complex;
import com.ypcl.math.Constant;
import com.ypcl.math.KaTable;
import com.ypcl.math.Matrix;
import com.ypcl.math.Matrix.Each;
import com.ypcl.math.SimpleSparseMatrix;
import com.ypcl.math.Vector;
import com.ypcl.struct.Graph;
import com.ypcl.struct.IntPair;

public class Topology implements ITextDataSource, Serializable {
	private static final long serialVersionUID = 1L;

	protected static class Record implements Serializable{
		private static final long serialVersionUID = 1L;
		final public static int BUS = 1, BRANCH = 2; 
		public Object id;
		public double value;
		public int deviceType, pointType;
		
		public Record(Object id, int deviceType, int pointType, double val) {
			this.id = id;
			this.deviceType = deviceType;
			this.pointType = pointType;
			this.value = val;
		}
		
		@Override
		public String toString() {
			String ret = "";
			switch (deviceType) {
			case BUS:
				int id = (int)this.id;
				ret += id + ",";
				switch (pointType) {
				case Bus.V:
					ret += "V:";
					break;
				case Bus.P:
					ret += "P:";
					break;
				case Bus.Q:
					ret += "Q:";
					break;
				}
				break;
			case BRANCH:
				IntPair bid = (IntPair)this.id;
				ret += bid.getI() + "->" + bid.getJ() + ",";
				switch (pointType) {
				case Branch.PIJ:
					ret += "Pij:";
					break;
				case Branch.QIJ:
					ret += "Qij:";
					break;
				case Branch.PJI:
					ret += "Pji:";
					break;
				case Branch.QJI:
					ret += "Qji:";
					break;
				}
				break;
			}
			ret += value;
			return ret;
		}
	}
	
	private final double error = 2.81; //99.5% 
	protected String name = "topology";
	protected Map<Integer, Bus> buses = new TreeMap<Integer, Bus>();
	protected Graph<Integer> graph = new Graph<Integer>();
	protected Map<IntPair, Branch> branches = new TreeMap<IntPair, Branch>();
	transient private List<Record> mRecords;
	protected List<Record> badDataRecords = new LinkedList<Record>();
	protected double s;
	transient private Matrix invG, R;
	transient protected SimpleSparseMatrix yr, yi;
	transient private Vector z, h;
	transient protected Vector x;
	transient private List<IFunction> funcs;
	transient private Matrix H;
	public static double e = 1.0e-5;
	public static KaTable kaTable = null;
	
	private interface IFunction {
		void cal();
	}
	
	public Topology setName(String name) {
		this.name = name;
		return this;
	}
	
	public Topology setBadDataRecord(List<Record> r) {
		badDataRecords = r;
		return this;
	}
	
	public List<Record> getBadDataRecord() {
		return badDataRecords;
	}
	
	public String getName() {
		return name;
	}
	
	private void initialBuses(BufferedReader br) throws IOException {
		String str = null;
		
		while ((str = br.readLine()) != null) {
			str = str.trim();
			if (!str.isEmpty()) {
				if (str.charAt(0) == '#') {
					continue;
				} 
				
				if (str.equals("END")) {
					break;
				}
				String [] sp = str.split(";", -1);
				String b = sp[3].trim(), name = sp[1].trim(), type = sp[2].trim();
				
				double db;
				int itype = Bus.NORMAL;
				
				if (b.isEmpty()) {
					db = 0;
				} else {
					db = Double.parseDouble(b);
				}
				
				if (!type.isEmpty()) {
					itype = Bus.SLACK;
				}
				
				Bus bus = null;
				
				if (name.isEmpty()) {
					bus = new Bus(Integer.parseInt(sp[0].trim()), db, itype);
				} else {
					bus = new Bus(Integer.parseInt(sp[0].trim()), db, itype, name);
				}
				
				if (sp.length >= 5) {
					String data = sp[4].trim();
					String [] datas = data.split(",");
					
					for (String piece : datas) {
						String [] ps = piece.trim().split(":");
						String dataType = ps[0].trim();
						if (dataType.equals("P")) {
							bus.putData(Bus.P, Double.parseDouble(ps[1].trim()));
						} else if (dataType.equals("Q")) {
							bus.putData(Bus.Q, Double.parseDouble(ps[1].trim()));
						} else if (dataType.equals("V")) {
							bus.putData(Bus.V, Double.parseDouble(ps[1].trim()));
						} else if (dataType.equals("CITA")) {
							bus.putData(Bus.CITA, Double.parseDouble(ps[1].trim()));
						}
					}
				}
				
				getBuses().put(bus.getId(), bus);
				graph.putHeads(bus.getId());
				//System.out.println(bus);
			}
		}
	}
	
	private void initialBranches(BufferedReader br) throws IOException {
		String str = null;
		
		while ((str = br.readLine()) != null) {
			str = str.trim();
			if (!str.isEmpty()) {
				if (str.charAt(0) == '#') {
					continue;
				} 
				
				if (str.equals("END")) {
					break;
				}
				String [] sp = str.split(";", -1);
				String type = sp[3];
				int itype = Branch.NORMAL;
				double k = 0;		
				if (!type.isEmpty()) {
					itype = Branch.TRANSFORMER;
					k = Double.parseDouble(sp[7]);
				}
				Branch branch = new Branch(Integer.parseInt(sp[0].trim()), Integer.parseInt(sp[1].trim()), 
							itype, Double.parseDouble(sp[4].trim()), Double.parseDouble(sp[5].trim()), Double.parseDouble(sp[6].trim()), k, sp[2]);		
				
				if (sp.length >= 9) {
					String data = sp[8].trim();
					
					if (!data.isEmpty()) {
						String [] datas = data.split(",");
						for (String piece : datas) {
							String [] ps = piece.trim().split(":");
							String dataType = ps[0].trim();
							if (dataType.equals("Pij")) {
								branch.putData(Branch.PIJ, Double.parseDouble(ps[1].trim()));
							} else if (dataType.equals("Pji")) {
								branch.putData(Branch.PJI, Double.parseDouble(ps[1].trim()));
							} else if (dataType.equals("Qij")) {
								branch.putData(Branch.QIJ, Double.parseDouble(ps[1].trim()));
							} else if (dataType.equals("Qji")) {
								branch.putData(Branch.QJI, Double.parseDouble(ps[1].trim()));
							} 
						}
					}
				}
				
				Branch alBranch = branches.get(branch.getId());
				if (alBranch != null) {
					alBranch.eat(branch);
				} else {
					branches.put(branch.getId(), branch);
				}
				graph.addConnection(branch.getId().getI(), branch.getId().getJ());
				//System.out.println(branch);
			}
		}
	}
	
	static public void initialKaTable(String path) throws Exception {
		kaTable = new KaTable() {
			@Override
			public KaTable fromFile(String path) throws Exception {
				BufferedReader br = new BufferedReader(new FileReader(path));
				String str = br.readLine();
				
				while ((str = br.readLine()) != null) {
					if (!str.isEmpty()) {
						String [] ps = str.split("\t");
						put(Integer.parseInt(ps[0]), Double.parseDouble(ps[9]));
					}
				}
				br.close();
				return this;
			}	
		}.fromFile(path);
	}
	
	@Override
	public void fromFile(String path) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(path));
			String str = null;
			
			str = br.readLine();
			
			if (str == null || !str.trim().equals("LOVE_YP_STATEESTIMATION_DATA")) {
				br.close();
				System.out.println("Wrong document");
				return;
			}
			
			try {
				while ((str = br.readLine()) != null) {
					str = str.trim();
					if (!str.isEmpty()) {
						if (str.charAt(0) == '#') {
							continue;
						}
						
						if (str.charAt(0) == 'S') {
							String [] sp = str.split(":");
							
							if (sp.length == 2) {
								s = Double.parseDouble(sp[1]);
							}
						}
						
						if (str.equals("BUS_INFO")) {
							initialBuses(br);
						}
						
						if (str.equals("BRANCH_INFO")) {
							initialBranches(br);
						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			br.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
	}
	
	public Topology initialY() {
		yr = new SimpleSparseMatrix();
		yi = new SimpleSparseMatrix();
		for (Entry<Integer, Bus> entry : getBuses().entrySet()) {
			Bus bus = entry.getValue();
			Complex ym = new Complex(0, bus.getB());
			int i = bus.getId();
			Set<Integer> links = graph.getNodeLinks(i);
			for (int link : links) {
				boolean left = true;
				Branch branch =  branches.get(new IntPair(i, link));
				
				if (branch == null) {
					branch = branches.get(new IntPair(link, i));
					left = false;
				}

				Complex y = new Complex(-1).div(new Complex(branch.getR(), branch.getX()));
				Complex ympart = y.mul(new Complex(-1));
				
				switch (branch.getType()) {
				case Branch.TRANSFORMER:
					y = y.div(new Complex(branch.getK()));
					if (!left) {
						ympart = ympart.div(new Complex(Math.pow(branch.getK(), 2.0)));
					}
					break;
				}
				yr.set(i - 1, link - 1, y.getReal());
				yi.set(i - 1, link - 1, y.getImag());
				ym = ym.add(new Complex(0, branch.getB())).add(ympart);
			}
			yr.set(i - 1, i - 1, ym.getReal());
			yi.set(i - 1, i - 1, ym.getImag());
		}
		return this;
	}
	
	protected double calPi(int i) {
		Set<Integer> links = graph.getNodeLinks(i + 1);
		
		double ret = v(i) * yr.get(i, i);
		for (int link : links) {
			int j = link - 1;
			double citaij = cita(i) - cita(j);
			ret += v(j) * (yr.get(i, j) * Math.cos(citaij) + yi.get(i, j) * Math.sin(citaij));
		}
		return ret * v(i);
	}
	
	protected double calDPiDCitai(int i) {
		Set<Integer> links = graph.getNodeLinks(i + 1);
		
		double ret = 0;
		for (int link : links) {
			int j = link - 1;
			double citaij = cita(i) - cita(j);
			ret += v(j) * (-yr.get(i, j) * Math.sin(citaij) + yi.get(i, j) * Math.cos(citaij));
		}
		return ret * v(i);
	}
	
	protected double calDPiDVi(int i) {
		Set<Integer> links = graph.getNodeLinks(i + 1);
		
		double ret = 2 * v(i) * yr.get(i, i);
		for (int link : links) {
			int j = link - 1;
			double citaij = cita(i) - cita(j);
			ret += v(j) * (yr.get(i, j) * Math.cos(citaij) + yi.get(i, j) * Math.sin(citaij));
		}
		return ret;
	}
	
	protected double calDPiDCitaj(int i, int j) {
		double ret = 0;
		if (i == j) {
			ret = calDPiDCitai(i);
		} else {
			double citaij = cita(i) - cita(j);
			ret = v(i) * v(j) * (yr.get(i, j) * Math.sin(citaij) - yi.get(i, j) * Math.cos(citaij));
		}
		return ret; 
	}
	
	protected double calDPiDVj(int i, int j) {
		double ret = 0;
		if (i == j) {
			ret = calDPiDVi(i);
		} else {
			double citaij = cita(i) - cita(j);
			ret = v(i) * (yr.get(i, j) * Math.cos(citaij) + yi.get(i, j) * Math.sin(citaij));
		}
		return ret; 
	}
	
	protected double calQi(int i) {
		Set<Integer> links = graph.getNodeLinks(i + 1);
		
		double ret = - yi.get(i, i) * v(i);
		for (int link : links) {
			int j = link - 1;
			double citaij = cita(i) - cita(j);
			ret += v(j) * (yr.get(i, j) * Math.sin(citaij) - yi.get(i, j) * Math.cos(citaij));
		}
		return ret * v(i);
	}
	
	protected double calDQiDCitai(int i) {
		Set<Integer> links = graph.getNodeLinks(i + 1);
		
		double ret = 0;
		for (int link : links) {
			int j = link - 1;
			double citaij = cita(i) - cita(j);
			ret += v(j) * (yr.get(i, j) * Math.cos(citaij) + yi.get(i, j) * Math.sin(citaij));
		}
		return ret * v(i);
	}
	
	protected double calDQiDVi(int i) {
		Set<Integer> links = graph.getNodeLinks(i + 1);
		
		double ret = - 2 * v(i) * yi.get(i, i);
		for (int link : links) {
			int j = link - 1;
			double citaij = cita(i) - cita(j);
			ret += v(j) * (yr.get(i, j) * Math.sin(citaij) - yi.get(i, j) * Math.cos(citaij));
		}
		return ret;
	}
	
	protected double calDQiDCitaj(int i, int j) {
		double ret = 0;
		if (i == j) {
			ret = calDQiDCitai(i);
		} else {
			double citaij = cita(i) - cita(j);
			ret = -v(i) * v(j) * (yr.get(i, j) * Math.cos(citaij) + yi.get(i, j) * Math.sin(citaij));
		}
		return ret; 
	}
	
	protected double calDQiDVj(int i, int j) {
		double ret = 0;
		if (i == j) {
			ret = calDQiDVi(i);
		} else {
			double citaij = cita(i) - cita(j);
			ret = v(i) * (yr.get(i, j) * Math.sin(citaij) - yi.get(i, j) * Math.cos(citaij));
		}
		return ret; 
	}
	
	protected double calVi(int i) {
		return v(i);
	}
	
	protected double calPij(int i, int j) {
		IntPair pair = new IntPair(i + 1, j + 1);
		Branch branch = branches.get(pair);
		double ret = 0.0, gi0 = 0.0;
		double gij = -yr.get(i, j), bij = -yi.get(i, j);
		
		if (branch != null) {
			if (branch.getType() == Branch.TRANSFORMER) {
				gi0 = gij * (branch.getK() - 1);
			}
		} else {
			branch = branches.get(pair.exchange());
			if (branch != null) {
				if (branch.getType() == Branch.TRANSFORMER) {
					gi0 = gij * (1 - branch.getK()) / branch.getK();
				} 
			} else {
				return ret;
			}
		}
		double citaij = cita(i) - cita(j);
		ret = Math.pow(v(i), 2) * (gij + gi0) - v(i) * v(j) * (gij * Math.cos(citaij) + bij * Math.sin(citaij));
		return ret;
	}
	
	protected double calDPijDCitai(int i, int j) {
		double gij = -yr.get(i, j), bij = -yi.get(i, j), citaij = cita(i) - cita(j);
		return v(i) * v(j) * (gij * Math.sin(citaij) - bij * Math.cos(citaij));
	}
	
	protected double calDPijDCitaj(int i, int j) {
		return -calDPijDCitai(i, j);
	}
	
	protected double calDPijDVi(int i, int j) {
		IntPair pair = new IntPair(i + 1, j + 1);
		Branch branch = branches.get(pair);
		double ret = 0.0, gi0 = 0.0;
		double gij = -yr.get(i, j), bij = -yi.get(i, j);
		
		if (branch != null) {
			if (branch.getType() == Branch.TRANSFORMER) {
				gi0 = gij * (branch.getK() - 1);
			}
		} else {
			branch = branches.get(pair.exchange());
			if (branch != null) {
				if (branch.getType() == Branch.TRANSFORMER) {
					gi0 = gij * (1 - branch.getK()) / branch.getK();
				} 
			} else {
				return ret;
			}
		}
		double citaij = cita(i) - cita(j);
		ret = 2 * v(i) * (gij + gi0) - v(j) * (gij * Math.cos(citaij) + bij * Math.sin(citaij));
		return ret;
	}
	
	protected double calDPijDVj(int i, int j) {
		double gij = -yr.get(i, j), bij = -yi.get(i, j), citaij = cita(i) - cita(j);
		return -v(i) * (gij * Math.cos(citaij) + bij * Math.sin(citaij));
	}
	
	protected double calQij(int i, int j) {
		IntPair pair = new IntPair(i + 1, j + 1);
		Branch branch = branches.get(pair);
		double ret = 0.0, bi0 = 0.0;
		double gij = -yr.get(i, j), bij = -yi.get(i, j);
		
		if (branch != null) {
			if (branch.getType() == Branch.TRANSFORMER) {
				bi0 = bij * (branch.getK() - 1);
			} else {
				bi0 = branch.getB();
			}
		} else {
			branch = branches.get(pair.exchange());
			if (branch != null) {
				if (branch.getType() == Branch.TRANSFORMER) {
					bi0 = bij * (1 - branch.getK()) / branch.getK();
				} else {
					bi0 = branch.getB();
				}
			} else {
				return ret;
			}
		}
		double citaij = cita(i) - cita(j);
		ret = -Math.pow(v(i), 2) * (bij + bi0) + v(i) * v(j) * (bij * Math.cos(citaij) - gij * Math.sin(citaij));
		return ret;
	}
	
	protected double calDQijDCitai(int i, int j) {
		double gij = -yr.get(i, j), bij = -yi.get(i, j), citaij = cita(i) - cita(j);
		return -v(i) * v(j) * (bij * Math.sin(citaij) + gij * Math.cos(citaij));
	}
	
	protected double calDQijDCitaj(int i, int j) {
		//bug fix, add '-'
		return -calDQijDCitai(i, j);
	}
	
	protected double calDQijDVi(int i, int j) {
		IntPair pair = new IntPair(i + 1, j + 1);
		Branch branch = branches.get(pair);
		double ret = 0.0, bi0 = 0.0;
		double gij = -yr.get(i, j), bij = -yi.get(i, j);
		
		if (branch != null) {
			if (branch.getType() == Branch.TRANSFORMER) {
				bi0 = bij * (branch.getK() - 1);
			} else {
				bi0 = branch.getB();
			}
		} else {
			branch = branches.get(pair.exchange());
			if (branch != null) {
				if (branch.getType() == Branch.TRANSFORMER) {
					bi0 = bij * (1 - branch.getK()) / branch.getK();
				} else {
					bi0 = branch.getB();
				}
			} else {
				return ret;
			}
		}
		double citaij = cita(i) - cita(j);
		ret = -2 * v(i) * (bij + bi0) + v(j) * (bij * Math.cos(citaij) - gij * Math.sin(citaij));
		return ret;
	}
	
	protected double calDQijDVj(int i, int j) {
		double gij = -yr.get(i, j), bij = -yi.get(i, j), citaij = cita(i) - cita(j);
		return v(i) * (bij * Math.cos(citaij) - gij * Math.sin(citaij));
	}
	
	protected double cita(int i) {
		return i == 0 ? 0.0 : x.get(indexOfCita(i));
	}
	
	protected double v(int i) {
		return x.get(indexOfV(i));
	}
	
	protected static int indexOfV(int i) {
		return i * 2;
	}
	
	protected static int indexOfCita(int i) {
		assert(i != 0);
		return i * 2 - 1;
	}
	
	protected void prepare() {
		int [] labels = {Bus.P, Bus.Q, Bus.V};
		int index = 0;
		for (Entry<Integer, Bus> entry : getBuses().entrySet()) {
			int id = entry.getKey();
			Bus bus = entry.getValue();
			if (id == 1) {
				x.addLast(1.0);
			} else {
				x.addLast(0.0).addLast(1.0);
			}
			
			final int i = id - 1;
			for (int label : labels) {
				Double v = bus.getData(label);
				if (v != null) {
					double value = label == Bus.V ? v : v / s;
					z.addLast(value);
					mRecords.add(new Record(id, Record.BUS, label, value));
					final int now = index;
					IFunction func = null;
					switch (label) {
					case Bus.V:
						func = new IFunction() {
							@Override
							public void cal() {
								h.set(now, calVi(i));
								H.set(now, indexOfV(i), 1);					
							}						
						};
						break;
					case Bus.P:
						func = new IFunction() {
							@Override
							public void cal() {
								h.set(now, calPi(i));
								Set<Integer> links = graph.getNodeLinks(i + 1);
								H.set(now, indexOfV(i), calDPiDVi(i));
								if (i >= 1) {
									H.set(now, indexOfCita(i), calDPiDCitai(i));
								}
								
								for (int link : links) {
									int j = link - 1;
									
									H.set(now, indexOfV(j), calDPiDVj(i, j));
									if (j < 1) {
										continue;
									}
									H.set(now, indexOfCita(j), calDPiDCitaj(i, j));
								}
							}						
						};
						break;
					case Bus.Q:
						func = new IFunction() {
							@Override
							public void cal() {
								h.set(now, calQi(i));
								Set<Integer> links = graph.getNodeLinks(i + 1);
								H.set(now, indexOfV(i), calDQiDVi(i));
								if (i >= 1) {
									H.set(now, indexOfCita(i), calDQiDCitai(i));
								}
								for (int link : links) {
									int j = link - 1;
									
									H.set(now, indexOfV(j), calDQiDVj(i, j));
									if (j < 1) {
										continue;
									}
									H.set(now, indexOfCita(j), calDQiDCitaj(i, j));
								}
							}						
						};
						break;
					}
					funcs.add(func);
					index++;
				}
			}
		}
		
		for (Entry<IntPair, Branch> entry : branches.entrySet()) {
			IntPair pair = entry.getKey();
			Branch branch = entry.getValue();
			final int i = pair.getI() - 1, j = pair.getJ() - 1;
			Double val = branch.getData(Branch.PIJ);			
			if (val != null) {
				final int now = index++;
				z.addLast(val);	
				mRecords.add(new Record(pair, Record.BRANCH, Branch.PIJ, val));
				funcs.add(new IFunction() {
					@Override
					public void cal() {
						h.set(now, calPij(i, j));
						
						H.set(now, indexOfV(i), calDPijDVi(i, j));
						H.set(now, indexOfV(j), calDPijDVj(i, j));
						if (i >= 1) {
							H.set(now, indexOfCita(i), calDPijDCitai(i, j));
						}
						
						if (j >= 1) {
							H.set(now, indexOfCita(j), calDPijDCitaj(i, j));
						}
					}						
				});
			}
			
			val = branch.getData(Branch.PJI);
			if (val != null) {
				final int now = index++;
				z.addLast(val);
				mRecords.add(new Record(pair, Record.BRANCH, Branch.PJI, val));
				funcs.add(new IFunction() {
					@Override
					public void cal() {
						h.set(now, calPij(j, i));
						
						H.set(now, indexOfV(j), calDPijDVi(j, i));
						H.set(now, indexOfV(i), calDPijDVj(j, i));
						if (j >= 1) {
							H.set(now, indexOfCita(j), calDPijDCitai(j, i));
						}
						
						if (i >= 1) {
							H.set(now, indexOfCita(i), calDPijDCitaj(j, i));
						}
					}						
				});
			}
			
			val = branch.getData(Branch.QIJ);
			if (val != null) {
				final int now = index++;
				z.addLast(val);
				mRecords.add(new Record(pair, Record.BRANCH, Branch.QIJ, val));
				funcs.add(new IFunction() {
					@Override
					public void cal() {
						h.set(now, calQij(i, j));
						
						H.set(now, indexOfV(i), calDQijDVi(i, j));
						H.set(now, indexOfV(j), calDQijDVj(i, j));
						if (i >= 1) {
							H.set(now, indexOfCita(i), calDQijDCitai(i, j));
						}
						
						if (j >= 1) {
							H.set(now, indexOfCita(j), calDQijDCitaj(i, j));
						}
					}
				});	
			}
			
			val = branch.getData(Branch.QJI);
			if (val != null) {
				final int now = index++;
				z.addLast(val);
				mRecords.add(new Record(pair, Record.BRANCH, Branch.QJI, val));
				funcs.add(new IFunction() {
					@Override
					public void cal() {
						h.set(now, calQij(j, i));
						
						H.set(now, indexOfV(j), calDQijDVi(j, i));
						H.set(now, indexOfV(i), calDQijDVj(j, i));
						if (j >= 1) {
							H.set(now, indexOfCita(j), calDQijDCitai(j, i));
						}
						
						if (i >= 1) {
							H.set(now, indexOfCita(i), calDQijDCitaj(j, i));
						}
					}						
				});
			}
		}
	}
	
	protected boolean cal() {
		for (IFunction func : funcs) {
			func.cal();
		}
		int zSize = z.size();
		R = Matrix.diag(zSize, 1e-4);
		Matrix W = R.each(new Each() {
			@Override
			public double cal(double v) {
				return Constant.isZero(v) ? 0 : 1 / v;
			}	
		}), G = H.transpose().mul(W).mul(H);
		Vector B = H.transpose().mul(W).mul(z.sub(h));
		invG = G.inverse();
		Vector dx = invG.mul(B);
		//System.out.println("/**H**/\r\n" + H);
		//System.out.println("/**B" + B.size() + "**/\r\n" + B);
		//System.out.println("/**z**/\r\n" + z);
		//System.out.println("/**h**/\r\n" + h);
		//System.out.println("/**r**/\r\n" + z.sub(h));
		//System.out.println("/**b**/\r\n" + dx);
		
		//System.out.println(H);
		/*System.out.println(z);
		System.out.println(h);
		System.out.println(h.size());
		System.out.println(dx);*/
		
		double maxerror = dx.normInfinite();
		
		System.out.println("最大误差：" + maxerror);
		if (maxerror < e) {
			return true;
		}
		
		x = x.add(dx);
		//System.out.println(name + "最后误差:" + dx.normInfinite());
		return false;
	}
	
	public boolean stateEstimate(int max) {	
		funcs = new LinkedList<IFunction>();
		z = new Vector();	
		mRecords = new LinkedList<Record>();
		x = new Vector();
		prepare();
		int zSize = z.size();
		h = Vector.zeroVector(zSize);
		System.out.println("测点数：" + zSize + ",状态量数：" + x.size());
		H = Matrix.zero(zSize, x.size());	
		int count = 0;
		
		while (count++ < max) {
			if (cal()) {
				System.out.println(name + " 本次迭代:" + count);
				return true;
			}
		}
		return false;
	}
	
	public boolean verify() {
		int K = z.size() - x.size();
		Double k2 = kaTable.get(K);
		
		if (k2 == null) {
			//throw new RuntimeException("Can not Verify without corresponding chi-square value.");
			k2 = Double.MAX_VALUE;
		}
			
		Vector r = z.sub(h);
		Matrix invR = R.each(new Each() {
			@Override
			public double cal(double v) {
				return Constant.isZero(v) ? 0 : 1 / v;
			}	
		});
		System.out.println("残差：" + r);
		double jx = new Matrix(r).transpose().mul(invR).mul(r).get(0);
		
		//System.out.println(invR);
		Matrix W = Matrix.unit(z.size()).sub(H.mul(invG).mul(H.transpose()).mul(invR));
		//System.out.println(W);
		Matrix D = Matrix.diag(W.mul(R));
		//System.out.println(R);
		Vector rn = D.each(new Each() {
			@Override
			public double cal(double v) {	
				return Constant.isZero(v) ? 0 : Math.pow(v, -0.5);
			}		
		}).mul(r);
			
		double max = -1;
		int index = -1;
		int i = 0;
		for (double val : rn) {	
			double pv = Math.abs(val);
			if (pv > max) {
				max = pv;
				index = i;
			}
			i++;
		}
		
		System.out.println("j(x)=" + jx + "[" + k2 + "],rn=" + max + "[" + error + "]");
		System.out.println("本次标准残差:" + rn);
		if (jx < k2 && max < error) {
			return true;
		}
		//System.out.println(z);
		//System.out.println(h);
		//System.out.println(v);
		
		if (index != -1) {
			Record record = mRecords.get(index);
			badDataRecords.add(record);
			switch (record.deviceType) {
			case Record.BUS:
				Bus bus = getBuses().get(record.id);
				bus.removeData(record.pointType);
				break;
			case Record.BRANCH:
				Branch branch = branches.get(record.id);
				branch.removeData(record.pointType);
				break;
			}
		} else {
			showBadData();
			throw new RuntimeException("Unknown Error.");
		}	
		return false;	
	}
	
	public boolean work(int times) {
		int i = 1;
		while (true) {
			
			System.out.println("***************第" + i++ + "次状态估计******************");
			if (stateEstimate(times)) {
				if (verify()) {
					return true;
				}
			} else {
				return false;
			}
		}
	}
	
	public Topology correctPhaseAngle() {
		return correctPhaseAngle(0);
	}
	
	public Topology correctPhaseAngle(double s) {
		double angle = 0;
		
		for (Entry<Integer, Bus> entry : getBuses().entrySet()) {
			Bus bus = entry.getValue();
			if (bus.getType() == Bus.SLACK) {
				angle = bus.getData(Bus.CITA_CAL) - s;
				break;
			}
		}
		
		for (Entry<Integer, Bus> entry : getBuses().entrySet()) {
			Bus bus = entry.getValue();
			bus.putData(Bus.CITA_CAL, bus.getData(Bus.CITA_CAL) - angle);
		}
		return this;
	}
	
	public Topology fillBus() {
		for (Entry<Integer, Bus> entry : getBuses().entrySet()) {
			Bus bus = entry.getValue();
			int i = bus.getId() - 1;
			bus.putData(Bus.CITA_CAL, cita(i));
			bus.putData(Bus.V_CAL, v(i));
			bus.putData(Bus.P_CAL, calPi(i));
			bus.putData(Bus.Q_CAL, calQi(i));
		}
		return this;
	}
	
	public Topology realFlow() {
		double[] pseux = new double[buses.size() * 2 - 1]; 
		double slack = buses.get(1).getData(Bus.CITA);
		for (Entry<Integer, Bus> entry : buses.entrySet()) {
			int id = entry.getKey();
			Bus bus = entry.getValue();
			if (id != 1) {
				pseux[indexOfCita(id - 1)] = bus.getData(Bus.CITA) - slack;
			}
			pseux[indexOfV(id - 1)] = bus.getData(Bus.V);
		}
		
		x = new Vector(pseux);
		System.out.println(x.size());
		for (Branch branch : branches.values()) {
			IntPair pair = branch.getId();
			branches.put(pair, branch);
			int i = pair.getI() - 1, j = pair.getJ() - 1;
			branch.putData(Branch.PIJ, calPij(i, j));
			branch.putData(Branch.QIJ, calQij(i, j));
			branch.putData(Branch.PJI, calPij(j, i));
			branch.putData(Branch.QJI, calQij(j, i));
		}
		return this;
	}
	
	public void realflowToFile(String path) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(path));
		for (Entry<IntPair, Branch> entry : branches.entrySet()) {
			Branch branch = entry.getValue();
			bw.write(branch.getId().getI() + "->" + branch.getId().getJ() + ","
			+ branch.getData(Branch.PIJ) + ","
			+ branch.getData(Branch.QIJ) + ","
			+ branch.getData(Branch.PJI) + ","
			+ branch.getData(Branch.QJI) + "\r\n");
		}
		bw.close();
	}
	
	public void realflowToInputFile(String path) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(path));
		
		bw.write("LOVE_YP_STATEESTIMATION_DATA\r\nS:" + s + "\r\nBUS_INFO\r\n");
		
		for (Bus bus : buses.values()) {
			bw.write(bus.getId() + ";"
					+ bus.getName() + ";"
					+ (bus.getType() == Bus.SLACK ? "1" : "") + ";"
					+ bus.getB() + ";"
					+ "\r\n");
		}
		bw.write("END\r\nBRANCH_INFO\r\n");
		
		int index = 1;
		for (Branch branch : branches.values()) {
			int i, j;
			i = branch.getId().getI();
			j = branch.getId().getJ();
			
			bw.write(i + ";"
					+ j + ";"
					+ index++ + ";"
					+ (branch.getType() == Branch.TRANSFORMER ? "1" : "") + ";"
					+ branch.getR() + ";"
					+ branch.getX() + ";"
					+ branch.getB() / 2 + ";"
					+ branch.getK() + ";"
					+ "Pij:" + branch.getData(Branch.PIJ) + ","
					+ "Qij:" + branch.getData(Branch.QIJ) + ","
					+ "Pji:" + branch.getData(Branch.PJI) + ","
					+ "Qji:" + branch.getData(Branch.QJI) 
					+ "\r\n");
		}
		bw.write("END");
		bw.close();
	}
	
	public Topology fillBranch() {
		for (Entry<IntPair, Branch> entry : branches.entrySet()) {
			Branch branch = entry.getValue();
			int i = entry.getKey().getI() - 1, j = entry.getKey().getJ() - 1;
			branch.putData(Branch.PIJ_CAL, calPij(i, j));
			branch.putData(Branch.QIJ_CAL, calQij(i, j));
			branch.putData(Branch.PJI_CAL, calPij(j, i));
			branch.putData(Branch.QJI_CAL, calQij(j, i));
		}
		return this;
	}
	
	public void toFile(String path) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(path));
		for (Entry<Integer, Bus> entry : getBuses().entrySet()) {
			Bus bus = entry.getValue();
			bw.write(bus.getId() + "," + bus.getData(Bus.V_CAL) + "," + 
			bus.getData(Bus.CITA_CAL) * 180 / Math.PI + "," + 
			bus.getData(Bus.P_CAL) * s + "," +
			bus.getData(Bus.Q_CAL) * s + "\r\n");
		}
		
		bw.write("\r\n");
		for (Entry<IntPair, Branch> entry : branches.entrySet()) {
			Branch branch = entry.getValue();
			bw.write(branch.getId().getI() + "->" + branch.getId().getJ() + ","
			+ branch.getData(Branch.PIJ_CAL) + ","
			+ branch.getData(Branch.QIJ_CAL) + ","
			+ branch.getData(Branch.PJI_CAL) + ","
			+ branch.getData(Branch.QJI_CAL) + "\r\n");
		}
		
		bw.write("\r\n");
		for (Record r : badDataRecords) {
			bw.write(r.toString() + "\r\n");
		}
		
		bw.close();
	}
	
	public void showBadData() {
		System.out.println(name + " Bad Data:");
		for (Record r : badDataRecords) {
			System.out.println(r.toString());
		}
	}
	public static void main(String [] args) throws Exception {
		long bt = System.currentTimeMillis(), et;
		Topology t = new Topology();
		t.fromFile("E:/学习资料/研究生大本营/毕业论文/code_data/ieee14_input.txt");
		Topology.initialKaTable("E:/学习资料/研究生大本营/毕业论文/code_data/k2.txt");
		//System.out.println(t.kaTable);
		t.initialY();
		//System.out.println(t.yi.toMatrix(14, 14));
		
		/*try {
			t.realFlow().realflowToFile("E:/学习资料/研究生大本营/毕业论文/code_data/realFlow.csv");
			t.realflowToInputFile("E:/学习资料/研究生大本营/毕业论文/code_data/ieee118_flow.dat");
			System.out.println("退出");
			System.exit(-1);
		} catch (Exception e) {
			System.out.println("可忽略的异常");
			e.printStackTrace();
		}*/
		/*System.out.println(t.graph);
		System.out.println(t.buses);
		System.out.println(t.branches);
		System.out.println(t.yr.toMatrix(t.buses.size(), t.buses.size()));*/
		
		//t.stateEstimate(1000);
		if (t.work(1000)) {
			t.fillBus().fillBranch()/*.correctPhaseAngle(30 / 180.0 * Math.PI)*/.toFile("E:/学习资料/研究生大本营/毕业论文/code_data/ieee_output.csv");
			System.out.println(t.x);		
		} else {
			System.out.println("不收敛。");
		}
		t.showBadData();
		et = System.currentTimeMillis();
		System.out.println("takes:" + (et - bt) + "ms");
		//System.out.println(t.x.get(0));
		//System.out.println(t.yi.toMatrix(14, 14));
	}

	public Map<Integer, Bus> getBuses() {
		return buses;
	}

	public void setBuses(Map<Integer, Bus> buses) {
		this.buses = buses;
	}
	
	public Map<IntPair, Branch> getBranches() {
		return branches;
	}

	public void setBranches(Map<IntPair, Branch> branches) {
		this.branches = branches;
	}
}
