package com.ypcl.ieee;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.ypcl.struct.IntPair;

public class StandardFile {
	private List<Bus> buses;
	private List<Branch> branches;
	private double sb;
	
	private Bus bus(int i) {
		for (Bus bus : buses) {
			if (bus.getId() == i) {
				return bus;
			}
		}
		return null;
	}
	
	private Branch branch(int i, int j) {
		for (Branch branch : branches) {
			if (branch.getI() == i && branch.getJ() == j) {
				return branch;
			}
		}
		return null;
	}
	
	private Branch branch(IntPair pair) {	
		return branch(pair.getI(), pair.getJ());
	}
	
	public StandardFile setBusPower(int i, double v) {
		bus(i).setGp(v);
		bus(i).setLp(0);
		return this;
	}
	
	public StandardFile setBusQ(int i, double v) {
		bus(i).setGq(v);
		bus(i).setLq(0);
		return this;
	}
	
	public StandardFile setBusVoltage(int i, double v) {
		bus(i).setV(v);
		return this;
	}
	
	public StandardFile setBranchPower(IntPair pair, double v) {
		Branch b = branch(pair);
		
		if (b != null) {
			b.setP1(v);
		} else {
			b = branch(pair.exchange());
			b.setP2(v);
		}
		return this;
	}
	
	public StandardFile setBranchPowerQ(IntPair pair, double v) {
		Branch b = branch(pair);
		
		if (b != null) {
			b.setQ1(v);
		} else {
			b = branch(pair.exchange());
			b.setQ2(v);
		}
		return this;
	}
	
	public void readFile(String path) throws IOException {
		BufferedReader br;
		
		br = new BufferedReader(new FileReader(path));
		String str = null;
		
		str = br.readLine();
		str = str.trim();
		String[] ps = removeEmpty(str.split(" "));
		
		sb = Double.parseDouble(ps[1]);
		
		buses = new LinkedList<Bus>();
		branches = new LinkedList<Branch>();
		
		while ((str = br.readLine()) != null) {
			str = str.trim();
			
			if (str.isEmpty()) {
				continue;
			}
			
			if (str.startsWith("BUS DATA FOLLOWS")) {
				readBuses(br);
			}
			
			if (str.startsWith("BRANCH DATA FOLLOWS")) {
				readBranches(br);
			}
		}
		br.close();
	}
	
	private String[] removeEmpty(String[] as) {
		List<String> ls = new LinkedList<String>();
		
		for (String a : as) {
			if (!a.isEmpty()) {
				ls.add(a);
			}
		}
		return (String[])ls.toArray(new String[ls.size()]);
	}
	
	private void readBuses(BufferedReader br) throws IOException {
		String str = null;
		
		while ((str = br.readLine()) != null) {
			str = str.trim();
			
			if (str.isEmpty()) {
				continue;
			}
			
			if (str.equals("-999")) {
				break;
			}
			
			String[] pieces = removeEmpty(str.split(" "));
			
			Bus bus = null;
			if (pieces.length == 19) {
				bus = new Bus(Integer.parseInt(pieces[0]),
						pieces[1],
						Bus.PV,
						Double.parseDouble(pieces[6]),
						Double.parseDouble(pieces[7]),
						Double.parseDouble(pieces[8]),
						Double.parseDouble(pieces[9]),
						Double.parseDouble(pieces[10]),
						Double.parseDouble(pieces[11]),
						Double.parseDouble(pieces[14]),
						Double.parseDouble(pieces[15]),
						Double.parseDouble(pieces[17]));
				
				if (Integer.parseInt(pieces[5]) == 3) {
					bus.setType(Bus.SLACK);
				}
			} else if (pieces.length == 18) {
				bus = new Bus(Integer.parseInt(pieces[0]),
						pieces[1],
						Bus.PQ,
						Double.parseDouble(pieces[5]),
						Double.parseDouble(pieces[6]),
						Double.parseDouble(pieces[7]),
						Double.parseDouble(pieces[8]),
						Double.parseDouble(pieces[9]),
						Double.parseDouble(pieces[10]),
						Double.parseDouble(pieces[13]),
						Double.parseDouble(pieces[14]),
						Double.parseDouble(pieces[16]));
			} else {
				System.out.println("Bad bus infomation.");
				System.out.println(Arrays.asList(pieces));
			}
			
			if (bus != null) {
				buses.add(bus);
			}
			
		}
	}
	
	private void readBranches(BufferedReader br) throws IOException {
		String str = null;
		int id = 1;
		
		while ((str = br.readLine()) != null) {
			str = str.trim();
			
			if (str.isEmpty()) {
				continue;
			}
			
			if (str.equals("-999")) {
				break;
			}
			
			String[] pieces = removeEmpty(str.split(" "));
			
			Branch branch = null;
			if (pieces.length == 20) {
				int type = Branch.LINE;
				if (pieces[4].equals("2")) {
					type = Branch.TRANSFORMER;
					System.out.println("TRANSFORMER");
					System.out.println(Arrays.asList(pieces));
				} 
				branch = new Branch(id, Integer.parseInt(pieces[0]), 
						Integer.parseInt(pieces[1]),
						type,
						Double.parseDouble(pieces[5]),
						Double.parseDouble(pieces[6]),
						Double.parseDouble(pieces[7]),
						Double.parseDouble(pieces[13]));
				
			} else if (pieces.length == 19) {
				branch = new Branch(id, Integer.parseInt(pieces[0]), 
						Integer.parseInt(pieces[1]),
						Branch.LINE,
						Double.parseDouble(pieces[4]),
						Double.parseDouble(pieces[5]),
						Double.parseDouble(pieces[6]));
			} else {
				System.out.println("Bad branch infomation.");
				System.out.println(Arrays.asList(pieces));
			}
			
			if (branch != null) {
				branches.add(branch);
				id++;
			}
		}
	}
	
	public void writeFlowInput(String path) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(path));
		bw.write("LOVE_YP_POWERFLOW_DATA\r\nBUS_INFO_BEGIN:" + buses.size() + "\r\n");
		
		for (Bus bus : buses) {
			int type;
			
			if (bus.getType() == Bus.SLACK) {
				type = 1;
			} else if (bus.getType() == Bus.PV) {
				type = 2;
			} else {
				type = 3;
			}
			bw.write(bus.getId() + "\t"
		+ bus.getName() + "\t"
		+ type + "\t" 
		+ bus.getV() + "\t"
		+ bus.getThta() + "\t"
		+ sb + "\t"
		+ bus.getGp() + "\t"
		+ bus.getGq() + "\t"
		+ bus.getLp() + "\t" 
		+ bus.getLq() + "\t"	
		+ bus.getQmax() + "\t"
		+ bus.getQmin() + "\t"
		+ bus.getB() + "\r\n");
		}
		
		bw.write("BUS_INFO_END\r\nBRANCH_INFO_BEGIN:" + branches.size() + "\r\n");
		for (Branch branch : branches) {
			int type;
			int i, j;
			if (branch.getType() == Branch.LINE) {
				type = 1;
				i = branch.getI();
				j = branch.getJ();
			} else {
				type = 2;
				j = branch.getI();
				i = branch.getJ();
			}
			
			bw.write(i + "\t"
					+ j + "\t"
					+ branch.getId() + "\t"
					+ type + "\t"
					+ branch.getR() + "\t"
					+ branch.getX() + "\t"
					+ branch.getB() / 2 + "\t"
					+ branch.getK() + "\r\n");
		}
		bw.write("BRANCH_INFO_END");
		bw.close();
	}
	
	
	public void writeStateEstimationInput(String path) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(path));
		bw.write("LOVE_YP_STATEESTIMATION_DATA\r\nS:" + sb + "\r\nBUS_INFO\r\n");
		
		for (Bus bus : buses) {
			bw.write(bus.getId() + ";"
					+ bus.getName() + ";"
					+ (bus.getType() == Bus.SLACK ? "1" : "") + ";"
					+ bus.b + ";"
					+ "V:" + bus.getV() + ","
					+ "CITA:" + bus.getThta() / 180.0 * Math.PI + ","
					+ "P:" + (bus.getGp() - bus.getLp()) + ","
					+ "Q:" + (bus.getGq() - bus.getLq()) + "\r\n");
		}
		bw.write("END\r\nBRANCH_INFO\r\n");
		
		for (Branch branch : branches) {
			int i, j;
			if (branch.getType() == Branch.LINE) {
				i = branch.getI();
				j = branch.getJ();
			} else {
				j = branch.getI();
				i = branch.getJ();
			}
			bw.write(i + ";"
					+ j + ";"
					+ branch.getId() + ";"
					+ (branch.getType() == Branch.TRANSFORMER ? "1" : "") + ";"
					+ branch.getR() + ";"
					+ branch.getX() + ";"
					+ branch.getB() / 2 + ";"
					+ branch.getK() + ";\r\n");
		}
		bw.write("END");
		bw.close();
	}
	
	public void writeStateEstimationFullInput(String path) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(path));
		bw.write("LOVE_YP_STATEESTIMATION_DATA\r\nS:" + sb + "\r\nBUS_INFO\r\n");
		
		for (Bus bus : buses) {
			bw.write(bus.getId() + ";"
					+ bus.getName() + ";"
					+ (bus.getType() == Bus.SLACK ? "1" : "") + ";"
					+ bus.b + ";"
					+ "V:" + bus.getV() + ","
					+ "CITA:" + bus.getThta() / 180.0 * Math.PI + ","
					+ "P:" + (bus.getGp() - bus.getLp()) + ","
					+ "Q:" + (bus.getGq() - bus.getLq()) + "\r\n");
		}
		bw.write("END\r\nBRANCH_INFO\r\n");
		
		for (Branch branch : branches) {
			int i, j;
			double pij, pji, qij, qji;
			if (branch.getType() == Branch.LINE) {
				i = branch.getI();
				j = branch.getJ();
				pij = branch.getP1();
				pji = branch.getP2();
				qij = branch.getQ1();
				qji = branch.getQ2();
			} else {
				j = branch.getI();
				i = branch.getJ();
				pji = branch.getP1();
				pij = branch.getP2();
				qji = branch.getQ1();
				qij = branch.getQ2();
			}
			
			bw.write(i + ";"
					+ j + ";"
					+ branch.getId() + ";"
					+ (branch.getType() == Branch.TRANSFORMER ? "1" : "") + ";"
					+ branch.getR() + ";"
					+ branch.getX() + ";"
					+ branch.getB() / 2 + ";"
					+ branch.getK() + ";"
					+ "Pij:" + pij + "," 
					+ "Pji:" + pji + "," 
					+ "Qij:" + qij + "," 
					+ "Qji:" + qji + "," 
					+ "\r\n");
		}
		bw.write("END");
		bw.close();
	}
	public static void main(String[] args) throws IOException {
		StandardFile sf = new StandardFile();
		String bs = "E:/学习资料/研究生大本营/毕业论文/tmp_data/";
		sf.readFile(bs + "118IEEE.DAT");
		sf.writeFlowInput(bs + "test14.txt");
		sf.writeStateEstimationInput(bs + "ieee118es.dat");
		System.out.println("finished");
	}
}
