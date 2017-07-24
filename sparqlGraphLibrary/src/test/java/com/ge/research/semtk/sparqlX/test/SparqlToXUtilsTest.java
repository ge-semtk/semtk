/**
 ** Copyright 2017 General Electric Company
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

package com.ge.research.semtk.sparqlX.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ge.research.semtk.sparqlX.SparqlToXUtils;


public class SparqlToXUtilsTest {

	
	@Test
	public void testIsLegalURI() throws Exception {
		assertTrue(SparqlToXUtils.isLegalURI("simple"));
		assertTrue(SparqlToXUtils.isLegalURI("/path/form"));
		assertTrue(SparqlToXUtils.isLegalURI("http://tree:05/hi/the_re"));
		assertTrue(SparqlToXUtils.isLegalURI("http://tree:05/hi/there#here"));
		assertTrue(SparqlToXUtils.isLegalURI("http://tree:05/hi/8there#here"));
		assertTrue(SparqlToXUtils.isLegalURI("http://tree:05/hi/there#8here"));       // fragment starts with number
		
		assertFalse(SparqlToXUtils.isLegalURI("http://tree:05/hi/there#-here"));       // fragment starts with -
		assertFalse(SparqlToXUtils.isLegalURI("-http://tree:05/hi/there#-here"));      // fragment starts with -
		assertFalse(SparqlToXUtils.isLegalURI("http://tree:05/hi/there#he#re"));       // two different #
		assertFalse(SparqlToXUtils.isLegalURI("http://tree:05/hi/ther\tre"));          // embedded space
		assertFalse(SparqlToXUtils.isLegalURI("http://tree:05/hi/ther\u0006re"));      // bad char		
	}
	
}