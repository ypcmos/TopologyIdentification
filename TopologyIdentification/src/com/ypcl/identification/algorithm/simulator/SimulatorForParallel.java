package com.ypcl.identification.algorithm.simulator;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ypcl.data.FlowData;
import com.ypcl.identification.algorithm.TopologyIdentificationForParallel;
import com.ypcl.struct.IntPair;
import com.ypcl.util.TimeState;

public class SimulatorForParallel extends Simulator {

	public SimulatorForParallel(String input, String tmppath) throws IOException {
		super(input, tmppath);
	}
	
	public int simulate(int n) throws InterruptedException {
		TopologyIdentificationForParallel ti = new TopologyIdentificationForParallel(this);
		List<TopologyIdentificationForParallel> list = ti.piece(n);
		List<Thread> threads = new LinkedList<Thread>();
		final Map<Integer, Double> busMap = new ConcurrentHashMap<Integer, Double>();
		final Map<IntPair, Double> branchMap = new ConcurrentHashMap<IntPair, Double>();
		
		for (final TopologyIdentificationForParallel t : list) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {			
					t.checkPiece();		
					Map<Integer, Double> bus = t.getGoodBuses();
					Map<IntPair, Double> branch = t.getGoodBranches();
					busMap.putAll(bus);
					branchMap.putAll(branch);
					//System.out.println(t.getTopology());
				}					
			});
			thread.start();
			threads.add(thread);
		}
		
		for (Thread thread : threads) {
			thread.join();
		}
		
		ti.merge(busMap, branchMap);	        
		cannot = ti.motherCheck();
		pGoodBuses = ti.getGoodBuses();
		pGoodBranches = ti.getGoodBranches();
		return cannot;
	}
	
	public SimulatorForParallel work(String output, int n) throws IOException, ClassNotFoundException, InterruptedException {		
		File file = new File(output);
	    if (file.isFile() && file.exists()) {  
	        file.delete();  
	    }  
		int count = 0, bad = 0;
		sf.writeFlowInput(path);
		FlowData data = new FlowData(runFlow(path, 0, 100), d, baddp, badmin);
		int iterateTimes = 1;//data.N;
		while (data.index++ < iterateTimes) {
			pVirtualBuses = data.generateBusPower();
			pVirtualBranches = data.generateBranchPower();
			pBuses = data.pBuses;
			pBranches = data.pBranches;
			TimeState s = new TimeState();
			if (simulate(n) != 0)
			{
				count++;
			}
			System.out.println("Time:" + s.past());
			data.afterData(this);
			bad += toHtmlFile2(output);
			data.update(sf);
			sf.writeFlowInput(path);
			data.parse(runFlow(path, 0, 100));
		}
		System.out.println("Count:" + count);
		System.out.println("Bad:" + bad);
		//writeInjectPower("F:/graduate_data/test_4_1/ds.dat" , data);
		//data.drawOriginBuses("F:/graduate_data/test_4_1/ds.png");
		data.drawBus("F:/graduate_data/test_4_1/ds_pi.png", 1);
		data.drawBranch("F:/graduate_data/test_4_1/ds_pij.png", new IntPair(1, 2));
		data.drawBranch("F:/graduate_data/test_4_1/ds_pji.png", new IntPair(2, 1));
		return this;
	}
	
	public static void main(String[] args) throws IOException {
		System.loadLibrary("PowerFlow");
		SimulatorForParallel s = new SimulatorForParallel("F:/graduate_data/test_4_1/014IEEE.DAT" ,"F:/graduate_data/test_4_1/tmp/tmp.txt");
		try {
			s.work("F:/graduate_data/test_4_1/result.html", 2);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
