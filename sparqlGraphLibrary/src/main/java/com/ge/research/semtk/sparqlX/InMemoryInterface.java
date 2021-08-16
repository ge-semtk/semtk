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
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.update.UpdateAction;
import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.utility.LocalLogger;

// nice tutorial
// https://wiki.uib.no/info216/index.php/Java_Examples

public class InMemoryInterface extends SparqlEndpointInterface {
	Dataset ds = null;
	
	public InMemoryInterface(String graphName) throws Exception {
		super("http://inmemory:0", graphName, "noUser", "noPass");
		
		this.ds = DatasetFactory.createTxnMem();	
		
	}

	/* Timeout is not implemented.  */
	public String getTimeoutSparqlPrefix() { return null; }    
	public String getTimeoutSparqlClause() { return null; } 
	public String getTimeoutPostParamName() { return null; }    
	public String getTimeoutPostParamValue() { return null; } 
	
	public JSONObject executeQueryPost(String query, SparqlResultTypes resultType) throws DontRetryException, Exception {
		
		if (resultType == SparqlResultTypes.CONFIRM) {
			this.ds.begin(ReadWrite.WRITE) ;
			try {
				UpdateAction.parseExecute(query, this.ds);

			} catch (Exception e) {
				// no in-memory exception is retry-able
				// print the query too
				LocalLogger.logToStdErr(e.getMessage() + "\nSPARQL=\n" + query);
				throw new DontRetryException(e.getMessage());
				
			} finally { this.ds.commit(); this.ds.end() ; }
			
			
			JSONObject ret = new JSONObject();
			ret.put(SimpleResultSet.MESSAGE_JSONKEY, "success");
			return ret;
			
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
		return this.dumpToString(Lang.RDFXML);
	}
	
	public String dumpToTurtle() {
	//  https://www.w3.org/TR/turtle/
    //
	//  https://jena.apache.org/documentation/io/
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			RDFDataMgr.write(stream, ds.getNamedModel(this.graph), RDFFormat.TURTLE_PRETTY);
			String str = stream.toString();
			return str;
		} catch (Exception e) {
			throw e;
		}
			finally { ds.end() ; 
		}
	}
	
	private String dumpToString(Lang lang) {
		
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			
			RDFDataMgr.write(stream, ds.getNamedModel(this.graph), lang);
			String str = stream.toString();
			return str;
		} catch (Exception e) {
			throw e;
		}
			finally { ds.end() ; 
		}
	}
	
	public String dumpToStringLegacy(String lang) {
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

	public JSONObject executeUpload(byte[] owl) throws AuthorizationException, Exception {
		return this.executeAuthUploadOwl(owl);
	}
	
	@Override
	public JSONObject executeAuthUploadOwl(byte[] owl) throws AuthorizationException, Exception {
		
		ByteArrayInputStream stream = new ByteArrayInputStream(owl); 
		
		this.ds.getNamedModel(this.graph).read(stream, "RDF/XML");
		//this.ds.getDefaultModel().read(stream, "RDF/XML");

		return new SimpleResultSet(true).toJson();
	}
	
	@Override
	public JSONObject executeAuthUploadTurtle(byte[] ttl) throws AuthorizationException, Exception {
		
		// The parsers will do the necessary character set conversion.  
	    try (ByteArrayInputStream in = new ByteArrayInputStream(ttl)) {
	        RDFParser.create()
	            .source(in)
	            .lang(Lang.TURTLE)
	            .errorHandler(ErrorHandlerFactory.errorHandlerStd)
	            .parse(this.ds.getNamedModel(this.graph)
	            );
	    }

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
		ret.copyRest(this);
		return ret;
	}
	
}
