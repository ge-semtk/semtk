package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;

import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * Get every permutation of a list of integers 0 to <size>
 * Intended to help explore every permutation of an arbitrary list
 * (i.e. this class generates every permutation of the indices)
 * @author 200001934
 *
 */
public class PermutationGenerator {
	private ArrayList<Integer> list;
	
	public PermutationGenerator(int size) {
		
		this.list = new ArrayList<Integer>();
		for (int i=0; i < size; i++) {
			this.list.add(i);
		}
	}
	
	public int numPermutations() {
		return (this.list.size()==0) ? 0 : (int) CombinatoricsUtils.factorial(this.list.size());
	}
	
	public ArrayList<Integer> getPermutation(int index) {
		ArrayList<Integer> ret = new ArrayList<Integer>();
		ArrayList<Integer> clone = (ArrayList<Integer>) list.clone();
		for (int i=0; i < list.size(); i++) {
			int m = i % clone.size();
			ret.add(clone.remove(m));
		}
		return ret;
	}
}
