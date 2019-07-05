package com.ypcl.storm.client;

import com.ypcl.identification.algorithm.simulator.TopologySimulatorInCloud;
import com.ypcl.storm.IdentificationTopology;

import backtype.storm.utils.DRPCClient;

@Deprecated
public class IdentificationClient {
	public static void main(String[] args) throws Exception {
        DRPCClient client = null;
        client = new DRPCClient("localhost", 3772);
        System.out.println("��ʼִ��DRPC�ͻ��˵���");  
        
        int count = 0;
		for (int i = 0; i < 100; i++) {
			TopologySimulatorInCloud simulator = new TopologySimulatorInCloud("D:/graduate/node_branch_P.txt");
			simulator.createVirtualDataByRandom();
			//simulator.createVirtualDataByFile("D:/graduate/why.txt");
			if (simulator.simulate(client, "identification", IdentificationTopology.boltsNum) != 0) {
				count++;
			}
			simulator.toHtmlFile("D:/graduate/cloud_result.html");
		}
		System.out.println("Count:" + count);
    }
}
