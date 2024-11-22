package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;

/**
 * Get every permutation of a list of integers 0 to <size>
 * Intended to help explore every permutation of an arbitrary list
 */
public class PermutationGenerator {

	private ArrayList<ArrayList<Integer>> permutations;
	
	/** 
	 * Constructor
	 * @param size number of indices to permute
	 */
	public PermutationGenerator(int size) {	
		ArrayList<Integer> numbers = new ArrayList<>();
		for(int n = 0; n < size; n++) {
			numbers.add(n);
		}
		permutations = permute(numbers);
	}	

	/**
	 * Get the number of permutations that exist
	 */
	public int numPermutations() {
		return permutations.size();
	}	
	
	/**
	 * Get a given permutation
	 * @param index the permutation index
	 */
	public ArrayList<Integer> getPermutation(int index) {
		return permutations.get(index);
	}
	
	/**
	 * Perform the permutation
	 */
	private static ArrayList<ArrayList<Integer>> permute(ArrayList<Integer> nums) {
    	ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        backtrack(result, new ArrayList<>(), nums);
        return result;
    }

	/**
	 * Recursive method to help perform the permutation
	 */
    private static void backtrack(ArrayList<ArrayList<Integer>> result, ArrayList<Integer> tempList, ArrayList<Integer> nums) {
        if (tempList.size() == nums.size()) {
            result.add(new ArrayList<>(tempList));
        } else {
            for (int i = 0; i < nums.size(); i++) {
                if (tempList.contains(nums.get(i))) continue; // element already exists, skip
                tempList.add(nums.get(i));
                backtrack(result, tempList, nums);
                tempList.remove(tempList.size() - 1);
            }
        }
    }	

}
