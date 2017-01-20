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