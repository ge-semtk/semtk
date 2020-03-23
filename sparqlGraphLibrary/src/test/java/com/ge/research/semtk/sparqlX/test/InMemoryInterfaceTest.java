package com.ge.research.semtk.sparqlX.test;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.InMemoryInterface;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class InMemoryInterfaceTest {

	@Test
	public void testAsOwlString() throws Exception {
		InMemoryInterface sei = new InMemoryInterface("http://name");
		String sparql =  
				"prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#>\n" +
				"INSERT { GRAPH <http://name> { ?job <http://test/name> 'its name'^^XMLSchema:string. } } WHERE { BIND (<http://a/job/uri> as ?job). }";

		sei.executeQuery(sparql, SparqlResultTypes.CONFIRM);
		String owl = sei.dumpToOwl();
		assertTrue("owl doesn't contain 'its name' simple check", owl.contains("<j.0:name>its name</j.0:name>"));
	}
	
	@Test
	public void testInsertSelect() throws Exception {
		InMemoryInterface sei = new InMemoryInterface("http://name");
		
		SimpleResultSet res = (SimpleResultSet) sei.executeQueryAndBuildResultSet(
				"INSERT DATA " + 
				"  { GRAPH <urn:sparql:tests:insert:data>   { " + 
				"        <#book1> <#price> 42  " + 
				"      }  } ", 
				SparqlResultTypes.CONFIRM);
		res.throwExceptionIfUnsuccessful();
		
		TableResultSet tres = (TableResultSet) sei.executeQueryAndBuildResultSet(
				"SELECT * FROM <urn:sparql:tests:insert:data> " + 
				"WHERE { ?s ?p ?o }", 
				SparqlResultTypes.TABLE);
		tres.throwExceptionIfUnsuccessful();
		Table tab = tres.getTable();
		assertTrue("Single row was not returned", tab.getNumRows() == 1);
		
	}
	
	@Test
	public void testLoad1() throws Exception {
		InMemoryInterface sei = new InMemoryInterface("http://name");
		
		
		// upload some owl
		Path path = Paths.get("src/test/resources/testTransforms.owl");
		byte[] owl = Files.readAllBytes(path);
		SimpleResultSet resultSet = SimpleResultSet.fromJson(sei.executeAuthUploadOwl(owl));
		resultSet.throwExceptionIfUnsuccessful();
		String owlStr = sei.dumpToOwl();
		System.out.println(owlStr);
		
		// get the load sgjson
		SparqlGraphJson sgJson = new SparqlGraphJson(Utility.getResourceAsJson(this, "/testTransforms.json"));
		sgJson.setSparqlConn(new SparqlConnection("anon", sei));
		
		// test
		Dataset ds = new CSVDataset("src/test/resources/testTransforms.csv", false);
		DataLoader dl = new DataLoader(sgJson, ds, TestGraph.getUsername(), TestGraph.getPassword());
		dl.importData(true);
		Table err = dl.getLoadingErrorReport();
		if (err.getNumRows() > 0) {
			fail(err.toCSVString());
		}
		assertEquals(dl.getTotalRecordsProcessed(), 3);
	}

}
