package com.ypcl.estimation.topology.parallel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.ypcl.estimation.topology.Branch;
import com.ypcl.estimation.topology.Bus;
import com.ypcl.estimation.topology.Topology;
import com.ypcl.math.Complex;
import com.ypcl.math.Vector;
import com.ypcl.struct.Graph;
import com.ypcl.struct.IntPair;
import com.ypcl.struct.Pair;

public class TopologyManager extends Topology {
	private static final long serialVersionUID = 1L;
	private List<List<Integer>> realIDs = new ArrayList<List<Integer>>();
	private List<String> labels = new ArrayList<String>();
	protected List<Branch> borders = new LinkedList<Branch>();
	
	private List<Integer> realIdList(int realid) {
		for (List<Integer> list : realIDs) {
			if (list.contains(realid)) {
				return list;
			}
		}
		return null;
	}
	
	public void prepareBlock(String path) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		
		String line = null;
		int index = getBuses().size() + 1;
		while (null != (line = br.readLine())) {
			line = line.trim();
			if (!line.isEmpty()) {
				if (line.charAt(0) == '#') {
					continue;
				}
				
				if (line.contains("-")) {
					String [] ps = line.split(":");
					getBuses().put(index, new Bus(index, 0, Bus.VIRTUAL, ps[1].trim()));
					String [] idLabels = ps[0].trim().split("-");
					int i = Integer.parseInt(idLabels[0].trim()),
							j = Integer.parseInt(idLabels[1].trim());
					IntPair pair = new IntPair(i, j);
					
					double r, x, b;
					Branch branch = null;
					Double pij, pji, qij, qji;
					if (branches.get(pair) != null) {
						branch = branches.remove(pair);
						pij = branch.getData(Branch.PIJ);
						pji = branch.getData(Branch.PJI);
						qij = branch.getData(Branch.QIJ);
						qji = branch.getData(Branch.QJI);
					} else {
						branch = branches.remove(pair.exchange());
						pij = branch.getData(Branch.PJI);
						pji = branch.getData(Branch.PIJ);
						qij = branch.getData(Branch.QJI);
						qji = branch.getData(Branch.QIJ);
					}
					borders.add(branch);
					r = branch.getR();
					x = branch.getX();
					b = branch.getB();
					Branch b1 = new Branch(i, index, r / 2, x / 2, b),
							b2 = new Branch(j, index, r / 2, x / 2, b);
					branches.put(b1.getId(), b1);
					branches.put(b2.getId(), b2);
					
					if (pij != null) {
						b1.putData(Branch.PIJ, pij);
					}
					if (qij != null) {
						b1.putData(Branch.QIJ, qij);
					}
					if (pji != null) {
						b2.putData(Branch.PIJ, pji);
					}
					if (qji != null) {
						b2.putData(Branch.QIJ, qji);
					}
					
					realIdList(i).add(index);
					realIdList(j).add(index);
					index++;
				} else {
					String [] ps = line.split(":");
					labels.add(ps[0].trim());
					String [] pts = ps[1].trim().split(",");
					
					List<Integer> ls = new ArrayList<Integer>();
					
					for (String pt : pts) {
						ls.add(Integer.parseInt(pt));
					}
					
					if (ls.size() > 0) {
						realIDs.add(ls);
					}
				}
			}
		}
		br.close();
		//System.out.println("After:" + graph.toString());
	}
	
	public List<TopologyUnit> makeBlocks() {
		List<TopologyUnit> ts = new ArrayList<TopologyUnit>();
		
		int index = 0;
		for (String label : labels) {
			TopologyUnit t = new TopologyUnit();
			t.setName(label);
			Map<Integer, Bus> bs = new TreeMap<Integer, Bus>();
			Map<IntPair, Branch> brs = new TreeMap<IntPair, Branch>();
			List<Integer> list = realIDs.get(index++);
			
			for (int id : list) {
				Bus bus = getBuses().get(id);
				bs.put(id, bus.getType() == Bus.VIRTUAL ? new Bus(bus) : bus);
			}
			
			Iterator<Entry<IntPair, Branch>> it = branches.entrySet().iterator();
			while (it.hasNext()) {
				Entry<IntPair, Branch> entry = it.next();
				if (bs.containsKey(entry.getKey().getI()) && bs.containsKey(entry.getKey().getJ())) {
					brs.put(entry.getKey(), entry.getValue());
					it.remove();
				}
			}
			t.fromMemory(bs, brs, s);
			ts.add(t);
		}
		return ts;
	}
	
	static public TopologyUnit mergeBlocks(TopologyUnit t1, TopologyUnit t2) {		
		int num = 0;
		double sum = 0;
		Iterator<Entry<Integer, Bus>> it = t2.getBuses().entrySet().iterator();
		
		while (it.hasNext()) {
			Bus bus = it.next().getValue();
			if (bus.getType() == Bus.VIRTUAL) {
				Bus b = t1.getBuses().get(bus.getId());
				
				if (b != null) {
					it.remove();
					t1.getBuses().remove(bus.getId());
					num++;
					sum += bus.getData(Bus.CITA_CAL) - b.getData(Bus.CITA_CAL);
					t1.removeBranch(bus.getId());
					t2.removeBranch(bus.getId());
				}	
			}
		}
		
		double average = sum / num;
		for (Entry<Integer, Bus> entry : t2.getBuses().entrySet()) {
			Bus bus = entry.getValue();
			t1.getBuses().put(bus.getId(), bus.putData(Bus.CITA_CAL, bus.getData(Bus.CITA_CAL) - average));
		}
		
		for (Entry<IntPair, Branch> entry : t2.getBranches().entrySet()) {
			t1.getBranches().put(entry.getKey(), entry.getValue());
		}
		
		t1.getBadDataRecord().addAll(t2.getBadDataRecord());
		return t1;
	}
	
	//do not support TRANSFORMER
	public static double citaij(TopologyUnit t1, TopologyUnit t2, int pvi) {	
		int pi = t1.anotherBranch(pvi), pj = t2.anotherBranch(pvi);
		Branch b1 = t1.getBranches().get(new IntPair(pi, pvi)), b2 = t2.getBranches().get(new IntPair(pj, pvi));
		Complex y = new Complex(1).div(new Complex(b1.getR() + b2.getR(), b1.getX() + b2.getX()));
		double gij = y.getReal(), bij = y.getImag(),  gi0 = 0, gj0 = 0;
		double ui = t1.getBuses().get(pi).getData(Bus.V_CAL), uj = t2.getBuses().get(pj).getData(Bus.V_CAL);
		double pij = b1.getData(Branch.PIJ_CAL), 
				pji = b2.getData(Branch.PIJ_CAL);
		return Math.asin(((Math.pow(ui, 2) - Math.pow(uj, 2)) * gij + Math.pow(ui, 2) * gi0 - Math.pow(uj, 2) * gj0
				- (pij - pji)) / (2 * ui * uj * bij));
	}
	
	static public TopologyUnit mergeBlocks2(TopologyUnit t1, TopologyUnit t2) {		
		int num = 0;
		double sum = 0;
		Iterator<Entry<Integer, Bus>> it = t2.getBuses().entrySet().iterator();
		List<Double> s1 = new LinkedList<Double>(), s2 = new LinkedList<Double>();
		while (it.hasNext()) {
			Bus bus = it.next().getValue();
			int id = bus.getId();
			if (bus.getType() == Bus.VIRTUAL) {
				Bus b = t1.getBuses().get(id);
				
				if (b != null) {
					it.remove();
					t1.getBuses().remove(id);
					num++;
					s1.add(bus.getData(Bus.CITA_CAL) - b.getData(Bus.CITA_CAL));
					Bus b1 = t1.getBuses().get(t1.anotherBranch(id)), 
							b2 = t2.getBuses().get(t2.anotherBranch(id));
					double sub = b2.getData(Bus.CITA_CAL) - b1.getData(Bus.CITA_CAL)
							+ citaij(t1, t2, id);
					sum += sub;
					s2.add(sub);
					t1.removeBranch(id);
					t2.removeBranch(id);
				}	
			}
		}
		System.out.println(s1);
		System.out.println(s2);
		double average = sum / num;
		for (Entry<Integer, Bus> entry : t2.getBuses().entrySet()) {
			Bus bus = entry.getValue();
			t1.getBuses().put(bus.getId(), bus.putData(Bus.CITA_CAL, bus.getData(Bus.CITA_CAL) - average));
		}
		
		for (Entry<IntPair, Branch> entry : t2.getBranches().entrySet()) {
			t1.getBranches().put(entry.getKey(), entry.getValue());
		}
		
		t1.getBadDataRecord().addAll(t2.getBadDataRecord());
		return t1;
	}
	
	public boolean execute(final int times) throws IOException, InterruptedException {
		List<TopologyUnit> ts = makeBlocks();
		List<Thread> threads = new LinkedList<Thread>();
		for (final TopologyUnit t : ts) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						t.execute(times);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}	
			});
			thread.start();
			threads.add(thread);
		}
		
		for (Thread thread : threads) {
			thread.join();
		}
		
		buses.clear();
		branches.clear();
		badDataRecords.clear();
		
		int size = ts.size();
		TopologyUnit tu = ts.get(0);
		if (!tu.isOk) {
			return false;
		}
		for (int i = 1; i < size; i++) {	
			TopologyUnit t = ts.get(i);
			if (t.isOk) {
				tu = mergeBlocks2(tu, t);
			} else {
				return false;
			}
		}
		
		buses = tu.getBuses();
		branches = tu.getBranches();
		badDataRecords = tu.getBadDataRecord();
		
		double[] pseux = new double[buses.size() * 2 - 1]; 
		for (Entry<Integer, Bus> entry : buses.entrySet()) {
			int id = entry.getKey();
			Bus bus = entry.getValue();
			if (id != 1) {
				pseux[indexOfCita(id - 1)] = bus.getData(Bus.CITA_CAL);
			}
			pseux[indexOfV(id - 1)] = bus.getData(Bus.V_CAL);
		}
		
		x = new Vector(pseux);
		System.out.println(x.size());
		for (Branch branch : borders) {
			IntPair pair = branch.getId();
			branches.put(pair, branch);
			int i = pair.getI() - 1, j = pair.getJ() - 1;
			branch.putData(Branch.PIJ_CAL, calPij(i, j));
			branch.putData(Branch.QIJ_CAL, calQij(i, j));
			branch.putData(Branch.PJI_CAL, calPij(j, i));
			branch.putData(Branch.QJI_CAL, calQij(j, i));
		}
		return true;
	}
	
	public List<TopologyUnit> debug_makeBlocks() {
		List<TopologyUnit> ts = new ArrayList<TopologyUnit>();
		TopologyUnit t = new TopologyUnit();
		t.fromMemory(getBuses(), branches, s);
		ts.add(t);
		return ts;
	}

	public void makeBlocksConfigure(String conf, String out) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(conf));
		
		String str = null;
		List<Pair<Integer>> list = new LinkedList<Pair<Integer>>();
		while ((str = br.readLine()) != null) {
			str = str.trim();
			
			if (!str.isEmpty()) {
				String[] ps = str.split("-");
				list.add(new Pair<Integer>(Integer.parseInt(ps[0]), Integer.parseInt(ps[1])));
			}
		}
		Graph<Integer> tmp = new Graph<Integer>(graph);
		List<Graph<Integer>> gs = tmp.pieces(list);
		br.close();
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		int i = 1;
		for (Graph<Integer> g : gs) {
			StringBuilder sb = new StringBuilder("AREA" + i++ + "[" + g.size() + "]:");
			for (int node : g.getHeads()) {
				sb.append(node + ",");
			}
			String w = sb.toString();
			w = w.substring(0, w.length() - 1);
			w += "\r\n";
			bw.write(w);
		}
		
		i = 1;
		for (Pair<Integer> pair : list) {
			bw.write(pair.getI() + "-" + pair.getJ() + ":F" + i++ + "\r\n");
		}
		bw.close();
	}
	
	public static void main(String [] args) throws Exception {
		long bt = System.currentTimeMillis(), et;
		TopologyManager t = new TopologyManager();
		t.fromFile("E:/学习资料/研究生大本营/毕业论文/code_data/ieee14_input.txt");
		t.initialY();

		Topology.initialKaTable("E:/学习资料/研究生大本营/毕业论文/code_data/k2.txt");
		String conf = "E:/学习资料/研究生大本营/毕业论文/code_data/distributed_file.txt";
		t.makeBlocksConfigure("E:/学习资料/研究生大本营/毕业论文/code_data/distributed_conf_14.txt", conf);
		t.prepareBlock(conf);
		//System.out.println(t.kaTable);
		if (t.execute(1000)) {
			//t.correctPhaseAngle(30 / 180.0 * Math.PI);
			t.toFile("E:/学习资料/研究生大本营/毕业论文/code_data/ieee_output_" + t.name + ".csv");
		} else {
			System.out.println("不收敛");
		}
		t.showBadData();
		et = System.currentTimeMillis();
		System.out.println("takes:" + (et - bt) + "ms");
	}
}
