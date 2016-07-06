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


package com.ge.research.semtk.belmont.test;

import org.junit.Test;

import com.ge.research.semtk.belmont.BelmontUtil;

public class BelmontUtilTest {

	@Test
	public void testPrefixQuery() {
		
		System.out.println("Running prefix query test");
		
		String query = "select ?s ?type {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type.}";
		
		try {
			String prefixedQuery = BelmontUtil.prefixQuery(query);
			
			System.out.println(prefixedQuery);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testMultiplePrefixQuery() {
		
		System.out.println("Running multiple prefix test");
		
		String query = "select ?s ?type ?prop {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type."
				+ " ?s <http://foo.research.com/test#bar> ?prop.}";
		
		try {
			String prefixedQuery = BelmontUtil.prefixQuery(query);
			
			System.out.println(prefixedQuery);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testPartiallyAlreadyPrefixed() {
		System.out.println("Running partial prefix test");
		
		String query = "prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n "
				+ " select ?s ?type ?prop {?s rdf:type ?type. \n"
				+ " ?s <http://foo.research.com/test#bar> ?prop.}";
		
		try {
			String prefixedQuery = BelmontUtil.prefixQuery(query);
			
			System.out.println(prefixedQuery);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
