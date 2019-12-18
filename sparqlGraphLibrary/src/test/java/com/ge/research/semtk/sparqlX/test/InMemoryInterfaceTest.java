package com.ge.research.semtk.sparqlX.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ge.research.semtk.sparqlX.InMemoryInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;

public class InMemoryInterfaceTest {

	@Test
	public void test() throws Exception {
		InMemoryInterface graph = new InMemoryInterface("name");
		String sparql =  
				"prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#>\n" +
				"INSERT { GRAPH <name> { ?job <http://test/name> 'its name'^^XMLSchema:string. } } WHERE { BIND (<http://a/job/uri> as ?job). }";

		graph.executeQuery(sparql, SparqlResultTypes.CONFIRM);
		
		System.out.println(graph.asOwlString());
	}

}
