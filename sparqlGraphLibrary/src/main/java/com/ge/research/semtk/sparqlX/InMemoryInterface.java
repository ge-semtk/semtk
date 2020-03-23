package com.ge.research.semtk.sparqlX;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.update.UpdateAction;
import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;

// nice tutorial
// https://wiki.uib.no/info216/index.php/Java_Examples

public class InMemoryInterface extends SparqlEndpointInterface {
	Dataset ds = null;
	
	public InMemoryInterface(String graphName) throws Exception {
		super("http://inmemory:0", graphName, "noUser", "noPass");
		
		this.ds = DatasetFactory.createTxnMem();	
		
	}

	public JSONObject executeQueryPost(String query, SparqlResultTypes resultType) throws Exception {
		
		if (resultType == SparqlResultTypes.CONFIRM) {
			this.ds.begin(ReadWrite.WRITE) ;
			try {
				UpdateAction.parseExecute(query, this.ds);

			} finally { this.ds.commit(); this.ds.end() ; }
			
			SimpleResultSet res = new SimpleResultSet(true);
			return res.toJson();
			
		} else {
			ResultSet results = QueryExecutionFactory.create(query, this.ds).execSelect();
			
		    Object [] headers = results.getResultVars().toArray();
		    String [] headStr = new String[headers.length];
		    int i=0;
		    for (Object o : headers) {
		    	headStr[i++] = (String)o;
		    }
		    Table t = new Table(headStr);
		    while (results.hasNext()) {
		    	ArrayList<String> row = new ArrayList<String>();
		    	QuerySolution solution = results.next();
		    	for (String h : headStr) {
		    		RDFNode cell = solution.get(h);
		    		row.add(cell != null ? cell.toString() : "");
		    	}
				t.addRow(row);
			}

		    this.resTable = t;
		    
		    JSONObject ret = new JSONObject();
		    ret.put("@table", t.toJson());
		    return ret;
		}
	}
	
	public String dumpToOwl() {
		return this.dumpToString("RDF/XML");
	}
	
	public String dumpToTurtle() {
		/* Seems to need this dependency, which won't work.
		 * 
		 * Will thrown a runtime exception can't find SnappyCompressorInputStream
		 
	       Could not transfer artifact org.apache.commons:commons-compress:jar:1.17 from/to cloudera (https://repository.cloudera.com/artifactory/cloudera-
		 repos/): Failed to connect to /3.39.251.6:8080 org.eclipse.aether.transfer.ArtifactTransferException: Could not transfer artifact org.apache.commons:commons-
		 compress:jar:1.17 from/to cloudera (https://repository.cloudera.com/artifactory/cloudera-repos/): Failed to connect to /3.39.251.6:8080 at 
	     
			<dependency>
				<groupId>org.apache.commons</groupId>
				<artifactId>commons-compress</artifactId>
				<version>1.17</version>
			</dependency>
		*/
		
		return this.dumpToString("TTL");
	}
	
	private String dumpToString(String lang) {
		ds.begin(ReadWrite.READ) ;
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			ds.getNamedModel(this.graph).write(stream, lang);
			//RDFDataMgr.write(stream, ds.getNamedModel(this.graph), Lang.TURTLE);
			String owl = stream.toString();
			return owl;
		} catch (Exception e) {
			throw e;
		}
			finally { ds.end() ; 
		}
	}

	@Override
	public int getInsertQueryMaxSize() {
		return 20000;
	}

	@Override
	public int getInsertQueryOptimalSize() {
		return 15000;
	}

	@Override
	public String getServerType() {
		return "inmemory";
	}

	@Override
	public String getPostURL(SparqlResultTypes resultType) {
		return "http://no/post/url";
	}

	@Override
	public String getUploadURL() throws Exception {
		return "http://no/post/url";
	}

	@Override
	public String getGetURL() {
		return "http://no/post/url";
	}

	@Override
	public JSONObject handleEmptyResponse(SparqlResultTypes resultType) throws Exception {
		throw new Exception("empty response");
	}

	@Override
	public JSONObject handleNonJSONResponse(String responseTxt, SparqlResultTypes resulttype)
			throws DontRetryException, Exception {
		throw new Exception("non JSON response");

	}

	@Override
	public JSONObject executeUpload(byte[] owl) throws AuthorizationException, Exception {
		
		ByteArrayInputStream stream = new ByteArrayInputStream(owl); 
		
		this.ds.getNamedModel(this.graph).read(stream, "RDF/XML");
		//this.ds.getDefaultModel().read(stream, "RDF/XML");

		return new SimpleResultSet(true).toJson();
	}

	/**
	 * Make sure the copy shares the same in-memory dataset
	 * but not the (should-be-deprecated) query results.
	 */
	@Override
	public SparqlEndpointInterface copy() throws Exception {
		InMemoryInterface ret = new InMemoryInterface(this.graph);
		ret.ds = this.ds;
		return ret;
	}
	
}
