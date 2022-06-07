package com.ge.research.semtk.belmont;

import java.util.HashMap;

public class UnionFind<T> {
	private HashMap<T, T> links = new HashMap<>();
	
	// Returns the representative element for the clique containing this element
	public T find(T x) {
		T gx = links.get(x);
		
		// x is the representative of this clique
		if (gx == null) {
			return x;
		}
		
		gx = find(gx);
		
		// Path compression
		links.put(x, gx);

		return gx;
	}

	// Combines the cliques containing the two nodes into one
	public void union(T x, T y) {
		T gx = find(x);
		T gy = find(y);
		if (gx != gy) {
			links.put(gx, gy);
		}
	}
}
