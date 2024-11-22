package com.ge.research.semtk.ontologyTools.test;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.ArrayList;
import com.ge.research.semtk.ontologyTools.PermutationGenerator;

public class PermutationGeneratorTest {

	@Test
	public void test() throws Exception {
		
		PermutationGenerator generator = null;
		ArrayList<Integer> expectedPerm = null;
		
		generator = new PermutationGenerator(1);
		assertEquals(generator.numPermutations(), 1);
		expectedPerm = new ArrayList<Integer>();
		expectedPerm.add(0);
		assertEquals(generator.getPermutation(0), expectedPerm);
		
		generator = new PermutationGenerator(2);
		assertEquals(generator.numPermutations(), 2);
		expectedPerm = new ArrayList<Integer>();
		expectedPerm.add(0);
		expectedPerm.add(1);
		assertEquals(generator.getPermutation(0), expectedPerm);
		expectedPerm = new ArrayList<Integer>();
		expectedPerm.add(1);
		expectedPerm.add(0);
		assertEquals(generator.getPermutation(1), expectedPerm);
		
		generator = new PermutationGenerator(3);
		assertEquals(generator.numPermutations(), 6);
		expectedPerm = new ArrayList<Integer>();
		expectedPerm.add(0);
		expectedPerm.add(1);
		expectedPerm.add(2);
		assertEquals(generator.getPermutation(0), expectedPerm);
		expectedPerm = new ArrayList<Integer>();
		expectedPerm.add(2);
		expectedPerm.add(1);
		expectedPerm.add(0);
		assertEquals(generator.getPermutation(5), expectedPerm);
	}
}
