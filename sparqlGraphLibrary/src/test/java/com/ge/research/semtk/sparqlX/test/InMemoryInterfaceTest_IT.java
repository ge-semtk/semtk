package com.ge.research.semtk.sparqlX.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.update.UpdateAction;
import org.junit.BeforeClass;
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
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class InMemoryInterfaceTest_IT {

	@BeforeClass
	public static void setup() throws Exception {
	    IntegrationTestUtility.authenticateJunit();
	}
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
				"  { GRAPH <http://name>   { " + 
				"        <#book1> <#price> 42  " + 
				"      }  } ", 
				SparqlResultTypes.CONFIRM);
		res.throwExceptionIfUnsuccessful();
		
		TableResultSet tres = (TableResultSet) sei.executeQueryAndBuildResultSet(
				"SELECT * FROM <http://name> " + 
				"WHERE { ?s ?p ?o }", 
				SparqlResultTypes.TABLE);
		tres.throwExceptionIfUnsuccessful();
		Table tab = tres.getTable();
		assertTrue("Single row was not returned", tab.getNumRows() == 1);
		
	}
	
	@Test
	public void testInsertSelectTypedString() throws Exception {
		// Make sure that "Name"^^XMLSchema:string maintains its type
		// by selecting it back out in a values clause
		//
		// Try a second time after dumpToOwl and uploadOwl to new in-memory sei
		//
		InMemoryInterface sei = new InMemoryInterface("http://name");
		
		SimpleResultSet res = (SimpleResultSet) sei.executeQueryAndBuildResultSet(
				"prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
				"INSERT DATA " + 
				"  { GRAPH <http://name>   { " + 
				"        <#book1> <#name> \"Name\"^^XMLSchema:string  " + 
				"      }  } ", 
				SparqlResultTypes.CONFIRM);
		res.throwExceptionIfUnsuccessful();
		
		String query = "prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
				"SELECT * FROM <http://name> " + 
				"WHERE { ?s ?p ?o .  VALUES ?o {\"Name\"^^XMLSchema:string } }";
		
		TableResultSet tres = (TableResultSet) sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		tres.throwExceptionIfUnsuccessful();
		Table tab = tres.getTable();
		assertTrue("Single row was not returned", tab.getNumRows() == 1);
		
		
	}
	
	@Test
	public void testInsertSelectTypedStringDumpOwl() throws Exception {
		// Make sure that "Name"^^XMLSchema:string maintains its type
		// by selecting it back out in a values clause
		//
		// Try a second time after dumpToOwl and uploadOwl to new in-memory sei
		//
		InMemoryInterface sei = new InMemoryInterface("http://name");
		
		SimpleResultSet res = (SimpleResultSet) sei.executeQueryAndBuildResultSet(
				"prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
				"INSERT DATA " + 
				"  { GRAPH <http://name>   { " + 
				"        <#book1> <#name> \"Name\"^^XMLSchema:string  " + 
				"      }  } ", 
				SparqlResultTypes.CONFIRM);
		res.throwExceptionIfUnsuccessful();
		
		
		//
		//  dump to owl and retry
		//
		String queryOwl= "prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
				"SELECT * FROM <http://owl> " + 
				"WHERE { ?s ?p ?o .  VALUES ?o {\"Name\"^^XMLSchema:string } }";
		String owl = sei.dumpToOwl();
		InMemoryInterface seiOwl = new InMemoryInterface("http://owl");
		seiOwl.executeAuthUploadOwl(owl.getBytes());
		
		TableResultSet tres = (TableResultSet) seiOwl.executeQueryAndBuildResultSet(queryOwl, SparqlResultTypes.TABLE);
		tres.throwExceptionIfUnsuccessful();
		Table tab = tres.getTable();
		assertTrue("Single row was not returned after dump/restore via owl", tab.getNumRows() == 1);
	}
	
	@Test
	public void testInsertSelectTypedStringDumpTtl() throws Exception {
		// Make sure that "Name"^^XMLSchema:string maintains its type
		// by selecting it back out in a values clause
		//
		// Try a second time after dumpToTurtle and uploadTurtle to new in-memory sei
		//
		InMemoryInterface sei = new InMemoryInterface("http://name");
		
		// insert a typed literal
		SimpleResultSet res = (SimpleResultSet) sei.executeQueryAndBuildResultSet(
				"prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
				"INSERT DATA " + 
				"  { GRAPH <http://name>   { " + 
				"        <#book1> <#name> \"Name\"^^XMLSchema:string  " + 
				"      }  } ", 
				SparqlResultTypes.CONFIRM);
		res.throwExceptionIfUnsuccessful();
		
		//
		// repeat testInsertSelectTypedString() just for completion
		// select a typed literal
		//
		String query = "prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
				"SELECT * FROM <http://name> " + 
				"WHERE { ?s ?p ?o .  VALUES ?o {\"Name\"^^XMLSchema:string } }";
		
		TableResultSet tres0 = (TableResultSet) sei.executeQueryAndBuildResultSet(query, SparqlResultTypes.TABLE);
		tres0.throwExceptionIfUnsuccessful();
		Table tab0 = tres0.getTable();
		assertTrue("Single row was not returned", tab0.getNumRows() == 1);
		
		//
		//  dump to turtle and retry
		//
		String queryTtl= "prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
				"SELECT * FROM <http://ttl> " + 
				"WHERE { ?s ?p ?o .  VALUES ?o {\"Name\"^^XMLSchema:string } }";
		String ttl = sei.dumpToTurtle();
		InMemoryInterface seiTtl = new InMemoryInterface("http://ttl");
		seiTtl.executeAuthUploadTurtle(ttl.getBytes());
		
		TableResultSet tres = (TableResultSet) seiTtl.executeQueryAndBuildResultSet(queryTtl, SparqlResultTypes.TABLE);
		tres.throwExceptionIfUnsuccessful();
		Table tab = tres.getTable();
		assertTrue("Single row was not returned after dump/restore via ttl", tab.getNumRows() == 1);
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
		assertTrue("owl failed spot check", owlStr.contains("printInitialWeightGm"));
		
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
	
	@Test
	public void stackOverflowQuestion() throws Exception {
		// TODO delete me
		String insertQuery = "prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> " +
				"INSERT DATA " + 
				"  { GRAPH <http://name>   { " + 
				"        <#book1> <#name> \"Name\"^^XMLSchema:string  " + 
				"      }  } ";
		
		org.apache.jena.query.Dataset ds = DatasetFactory.createTxnMem();
		ds.begin(ReadWrite.WRITE);
		try {
			UpdateAction.parseExecute(insertQuery, ds);
		} finally { ds.commit(); ds.end() ; }
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RDFDataMgr.write(stream, ds.getNamedModel("http://name"), RDFFormat.TURTLE_PRETTY);
		String str = stream.toString();
		System.out.println(str);
	}

}
