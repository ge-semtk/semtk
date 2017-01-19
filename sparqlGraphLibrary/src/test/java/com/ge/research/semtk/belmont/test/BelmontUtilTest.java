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

import static org.junit.Assert.*;

import org.junit.Test;

import com.ge.research.semtk.belmont.BelmontUtil;

public class BelmontUtilTest {

	@Test
	public void testPrefixQuery() throws Exception {	
		
		String query = "select ?s ?type {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type.}";

		String prefixedQuery = BelmontUtil.prefixQuery(query);		
			
		assertTrue(prefixedQuery.startsWith("prefix 22-rdf-syntax-ns:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"));
		assertTrue(prefixedQuery.endsWith("select ?s ?type {?s 22-rdf-syntax-ns:type ?type.}"));
	}
	
	@Test
	public void testMultiplePrefixQuery() throws Exception {
		
		String query = "select ?s ?type ?prop {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type."
				+ " ?s <http://foo.research.com/test#bar> ?prop.}";

		String prefixedQuery = BelmontUtil.prefixQuery(query);
			
		assertTrue(prefixedQuery.contains("prefix 22-rdf-syntax-ns:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"));
		assertTrue(prefixedQuery.contains("prefix test:<http://foo.research.com/test#>"));
		assertTrue(prefixedQuery.endsWith("select ?s ?type ?prop {?s 22-rdf-syntax-ns:type ?type. ?s test:bar ?prop.}"));		

	}
	
	@Test
	public void testPartiallyAlreadyPrefixed() throws Exception {
		
		String query = "prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n "
				+ " select ?s ?type ?prop {?s rdf:type ?type. \n"
				+ " ?s <http://foo.research.com/test#bar> ?prop.}";
		
		String prefixedQuery = BelmontUtil.prefixQuery(query);
			
		String expected = "prefix rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
				"prefix test:<http://foo.research.com/test#>\n\n" +
				"  select ?s ?type ?prop {?s rdf:type ?type.  ?s test:bar ?prop.}";

		assertTrue(prefixedQuery.equals(expected));
	}
	
}
