package com.ypcl.struct;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ypcl.struct.exception.NodeNotInGraphException;

public class Graph<T>  implements Serializable {
	private static final long serialVersionUID = 4906129860298473914L;

	public static class GraphNode<T> implements Serializable {
		private static final long serialVersionUID = -3009155809194752088L;
		private Set<T> set = new TreeSet<T>();
		
		public GraphNode() {
		}
		
		public boolean isConnected(T other) {
			return set.contains(other);
		}
		
		public GraphNode<T> addConnection(T other) {
			set.add(other);
			return this;
		}
		
		@Override
		public String toString() {
			return set.toString();
		}
	}
	
	private Map<T, GraphNode<T>> heads = new TreeMap<T, GraphNode<T>>();
	
	public Graph<T> putHeads(T node) {	
		return putHeads(node, new GraphNode<T>());
	}
	
	public Graph<T> putHeads(T node, GraphNode<T> graphNode) {
		heads.put(node, graphNode);	
		return this;
	}
	
	public Set<T> getHeads() {
		return heads.keySet();
	}
	
	public Set<T> getNodeLinks(T index) {
		return heads.get(index).set;
	}
	
	public boolean isConnected(T first, T second) {
		GraphNode<T> node = heads.get(first);
		
		if (node == null) {
			throw new NodeNotInGraphException();
		}
		
		return node.isConnected(second);	
	}
	
	public Graph<T> addConnection(T first, T second) {
		GraphNode<T> node1 = heads.get(first), node2 = heads.get(second);
		
		if (node1 == null || node2 == null) {
			throw new NodeNotInGraphException();
		}
		
		node1.addConnection(second);
		node2.addConnection(first);
		return this;
	}
	
	public Set<T> getConnectedNodes(T index) {
		GraphNode<T> node = heads.get(index);
		
		if (node == null) {
			throw new NodeNotInGraphException();
		}
		return node.set;
	}
	
	public T getFirstNode() {
		return getNodeByIntegerIndex(0);
	}
	
	public T getNodeByIntegerIndex(int i) {
		int count = 0;
		T ret = null;
		for (T key : heads.keySet()) {
			if (count == i) {
				ret = key;
				break;
			}
			count++;
		}
		return ret;
	}
	
	public List<T> bfs() {
		int size = heads.size();
		
		if (size == 0) {
			return null;
		}
		
		List<T> ret = new ArrayList<T>(size);
		Map<T, Boolean> sign = new HashMap<T, Boolean>();
		
		for (T key : heads.keySet()) {
			sign.put(key, false);
		}
		
		Queue<T> temp = new LinkedList<T>();
		T s = getFirstNode();
		temp.add(s);
		sign.put(s, true);
		
		while (!temp.isEmpty()) {
			T h = temp.poll();
			ret.add(h);
			Set<T> links = getConnectedNodes(h);
			
			for (T link : links) {
				if (!sign.get(link)) {
					sign.put(link, true);
					temp.add(link);
				}
			}
			
		}
		return ret;
	}
	
	public Graph<T> childGraph(List<T> nodes) {
		Graph<T> graph = new Graph<T>();
		
		for (T node : nodes) {		
			Set<T> links = getConnectedNodes(node);
			GraphNode<T> graphNode = new GraphNode<T>();
			for (T link : links) {
				if (nodes.contains(link)) {
					graphNode.addConnection(link);
				}
			}
			graph.putHeads(node, graphNode);
		}
		return graph;
	}
	
	public Graph<T> childGraph(int limit) {
		List<T> ns = bfsPart(limit);
		return childGraph(ns);
	}
	
	public Graph<T> separateChileGraph(Graph<T> childGraph) {
		Set<T> childHeads = childGraph.getHeads();
		List<T> ns = new LinkedList<T>();
		
		for (T h : heads.keySet()) {
			if (!childHeads.contains(h)) {
				ns.add(h);
			}
		}
		return childGraph(ns);
	}
	
	public List<Graph<T>> pieces(int n) {
		List<Graph<T>> list = new LinkedList<Graph<T>>();
		int size = heads.size();
		int each = size / n;
		
		Graph<T> mother = this;
		for (int i = 0; i < n - 1; i++) {
			Graph<T> target = mother.childGraphKeepConnection(each);
			list.add(target);
			mother = mother.separateChileGraph(target);	
			
			if (mother.size() == 0) {
				return list;
			}
		}
		list.add(mother);
		return list;
	}
	
	public Graph<T> childGraphKeepConnection(int suggestLimit) {
		List<T> ns = bfsPart(suggestLimit);
		
		for (Entry<T, GraphNode<T>> entry : heads.entrySet()) {
			T n = entry.getKey();
			
			if (!ns.contains(n)) {
				boolean need = true;
				for (T link : entry.getValue().set) {
					if (!ns.contains(link)) {
						need = false;
						break;
					}
				}
				
				if (need) {
					ns.add(n);
				}
			}
		}
		return childGraph(ns);
	}
	
	public List<T> bfsPart(int limit) {
		int size = heads.size();
		
		if (limit >= size) {
			return bfs();
		}
		
		List<T> ret = new ArrayList<T>(size);
		Map<T, Boolean> sign = new HashMap<T, Boolean>();
		
		for (T key : heads.keySet()) {
			sign.put(key, false);
		}
		
		Queue<T> temp = new LinkedList<T>();
		T s = getFirstNode();
		temp.add(s);
		sign.put(s, true);
		
		while (!temp.isEmpty() && limit > 0) {
			limit--;
			T h = temp.poll();
			ret.add(h);
			Set<T> links = getConnectedNodes(h);
			
			for (T link : links) {
				if (!sign.get(link)) {
					sign.put(link, true);
					temp.add(link);
				}
			}	
		}
		return ret;
	}
	
	public List<Pair<T>> disconnectLinks(Graph<T> son) {
		List<Pair<T>> list = new LinkedList<Pair<T>>();
		
		for (Entry<T, GraphNode<T>> entry : son.heads.entrySet()) {
			Set<T> full = this.getConnectedNodes(entry.getKey());
			
			for (T s : full) {
				if (!entry.getValue().set.contains(s)) {
					list.add(new Pair<T>(entry.getKey(), s));
				}
			}
		}
		return list;
	}
	
	public int size() {
		return heads.size();
	}
	
	@Override
	public String toString() {
		return heads.toString();
	}
}


