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

		assertEquals(transform.applyTransform("some string"),"z8b45e4bd1c6acb88bebf6407d16205f567e62a3e");
		assertEquals(transform.applyTransform("some string"),"z8b45e4bd1c6acb88bebf6407d16205f567e62a3e");
		assertEquals(transform.applyTransform("\u0002garbage"),"z67feeda2e157ac556f0de821c104ba04eac9cf97");			
		assertEquals(transform.applyTransform(" string that has a bunch of words in it like this but is almost the same as another string like this other one and now lets make it eve"),"z63c5166681fdd181df7d6a1da7df552c8070058a");
		assertEquals(transform.applyTransform(" one with '!@#$%^&*()_-+={[}]|\\\\:P;\\\"'<,>.?/~"), "za7e33f5d20ba17715936db84442fd46a2721bb84");
	}
	
	@Test
	public void testLowerCaseTransform() throws Exception{	
		ToLowerCaseTransform transform = new ToLowerCaseTransform("name");
		assertEquals(transform.applyTransform("some string"),"some string");
		assertEquals(transform.applyTransform("SOME STRING 5"),"some string 5");
	}

}