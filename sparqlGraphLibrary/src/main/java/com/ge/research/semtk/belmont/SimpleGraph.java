package com.ge.research.semtk.belmont;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * This class represents graphs with nodes of type T and unlabeled edges. 
 *
 * @param <T> Type of graph nodes.
 */
public class SimpleGraph<T> {
	/**
	 * Map of source nodes to outgoing edges.
	 */
	private Map<T, List<T>> adjacencies = new HashMap<>();
	
	/**
	 * Add a new node to the graph. If the node is already in the graph
	 * this function makes no change.
	 * 
	 * @param node New node
	 */
	public void addNode(T node) {
		adjacencies.putIfAbsent(node, new ArrayList<>());
	}
	
	/**
	 * Add an edge between two nodes that are already in the graph.
	 * 
	 * @param source source node 
	 * @param target target node
	 */
	public void addEdge(T source, T target) {
		adjacencies.get(source).add(target);
	}
	
	/**
	 * Check if the given graph is a forest.
	 * 
	 * A forest is a graph that is composed of zero or more disjoint trees.
	 * 
	 * A tree is a graph where each node can have at most one parent and have no cycles.
	 * 
	 * @return true when the graph is a forest
	 */
	public boolean isForest() {
		
		// Determine all the nodes with no incoming edges. These are the roots of the forest
		Set<T> headNodes = new HashSet<>(adjacencies.keySet());
		for (List<T> targets : adjacencies.values()) {
			headNodes.removeAll(targets);
		}
		
		Set<T> seen = new HashSet<>();
		Stack<T> work = new Stack<T>();
		work.addAll(headNodes);
		
		while (!work.isEmpty()) {
			T next = work.pop();
			if (!seen.add(next)) { return false; } // check not previously visited
			work.addAll(adjacencies.get(next)); // add reachable nodes to work stack
		}

		return seen.size() == adjacencies.size(); // check all nodes visited
	}

	/**
	 * This function computes a graph corresponding to the input graph
	 * where all edges have their source and targets transposed.
	 * 
	 * The resulting graph has all the same nodes and the same number
	 * of edges.
	 * 
	 * @return Inverted graph
	 */
	public SimpleGraph<T> invert() {
		SimpleGraph<T> result = new SimpleGraph<>();
		
		for (Map.Entry<T, List<T>> entry : adjacencies.entrySet()) {
			result.addNode(entry.getKey());
		}
		for (Map.Entry<T, List<T>> entry : adjacencies.entrySet()) {
			T source = entry.getKey();
			for (T target : entry.getValue()) {
				result.addEdge(target, source);
			}
		}

		return result;
	}
	
	/**
	 * Returns the set of nodes paired with the outgoing edges from those
	 * nodes.
	 * 
	 * @return Adjacency set view
	 */
	public Set<Map.Entry<T, List<T>>> entrySet() {
		return adjacencies.entrySet();
	}
}
