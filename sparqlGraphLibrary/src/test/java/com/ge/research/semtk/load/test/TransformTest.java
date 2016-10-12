/**
 ** Copyright 2016 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */


package com.ge.research.semtk.load.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ge.research.semtk.load.transform.HashTransform;
import com.ge.research.semtk.load.transform.ToLowerCaseTransform;


public class TransformTest {

	
	@Test
	public void testHashTransform() throws Exception{
		HashTransform transform = new HashTransform("name");
		assertEquals(transform.applyTransform("some string"),"1395333309");
		assertEquals(transform.applyTransform("some string 2"),"885514639");
		assertEquals(transform.applyTransform("some string 3"),"885514640");			
		assertEquals(transform.applyTransform("some longer string that has a bunch of words in it like this but is almost the same as another string like this other one and now lets make it even longer 1"),"-61650938");
		assertEquals(transform.applyTransform("some longer string that has a bunch of words in it like this but is almost the same as another string like this other one and now lets make it even longer 2"),"-61650937");
	}
	
	@Test
	public void testLowerCaseTransform() throws Exception{	
		ToLowerCaseTransform transform = new ToLowerCaseTransform("name");
		assertEquals(transform.applyTransform("some string"),"some string");
		assertEquals(transform.applyTransform("SOME STRING 5"),"some string 5");
	}

}