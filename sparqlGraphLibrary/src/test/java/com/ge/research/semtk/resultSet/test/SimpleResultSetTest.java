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


package com.ge.research.semtk.resultSet.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.simple.JSONObject;
import org.junit.Test;

import com.ge.research.semtk.resultSet.SimpleResultSet;


public class SimpleResultSetTest {

	@Test
	public void test1() {
		try{
			SimpleResultSet resultSet = new SimpleResultSet(true, "Monkeys are brown");
			// TODO ADD TESTS
		}catch(Exception e){
			e.printStackTrace();
			fail();
		}
	}	
	
	@Test
	public void testJsonRationale1() {
		try{
			SimpleResultSet resultSet = new SimpleResultSet(true, "Monkeys are brown");
			JSONObject j = resultSet.toJson();
			SimpleResultSet set2 = SimpleResultSet.fromJson(j);
			set2.getRationaleAsString(null).equals("Monkeys are brown");
		}catch(Exception e){
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void testJsonResultString() {
		try{
			SimpleResultSet resultSet = new SimpleResultSet(true, "Monkeys are brown");
			resultSet.addResult("result", "test");
			JSONObject j = resultSet.toJson();
			SimpleResultSet set2 = SimpleResultSet.fromJson(j);
			assertTrue(set2.getResult("result").equals("test"));
		}catch(Exception e){
			e.printStackTrace();
			fail();
		}
	}	
	
	@Test
	public void testJsonResultInt() {
		try{
			SimpleResultSet resultSet = new SimpleResultSet(true, "Monkeys are brown");
			resultSet.addResult("result", 100);
			JSONObject j = resultSet.toJson();
			SimpleResultSet set2 = SimpleResultSet.fromJson(j);
			assertTrue(set2.getResultInt("result") == 100);
		}catch(Exception e){
			e.printStackTrace();
			fail();
		}
	}	
	
	@Test
	public void testJsonResultBadName() {
		try{
			SimpleResultSet resultSet = new SimpleResultSet(true, "Monkeys are brown");
			resultSet.addResult("result", 100);
			JSONObject j = resultSet.toJson();
			SimpleResultSet set2 = SimpleResultSet.fromJson(j);
			
			try {
				set2.getResultInt("BAD_NAME");
				fail();
			} catch (Exception e) {

				// success
			}
		}catch(Exception e){
			e.printStackTrace();
			fail();
		}
	}	
	
}
