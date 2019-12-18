package com.ge.research.semtk.sparqlX;

import java.io.ByteArrayOutputStream;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.AuthorizationException;
import com.ge.research.semtk.resultSet.SimpleResultSet;

public class InMemoryInterface extends SparqlEndpointInterface {
	Dataset ds = null;
	
	public InMemoryInterface(String graphName) throws Exception {
		super("http:noserver:80", graphName, "noUser", "noPass");
		this.ds = DatasetFactory.createTxnMem();
		this.ds.addNamedModel("<name>", ModelFactory.createDefaultModel() );
	}

	public JSONObject executeQueryPost(String query, SparqlResultTypes resultType) throws Exception {
		
		if (resultType == SparqlResultTypes.CONFIRM) {
			ds.begin(ReadWrite.WRITE) ;
			try {
				
				UpdateRequest request = UpdateFactory.create(query) ;
				UpdateProcessor proc = UpdateExecutionFactory.create(request, ds) ;
				proc.execute() ;

			} finally { ds.commit(); ds.end() ; }
			
			JSONObject retval = new JSONObject();
			retval.put(SimpleResultSet.MESSAGE_JSONKEY, "success"); 
			return retval;
			
		} else {
			throw new Exception("non CONFIRM queries are not implemented");
		}
	}
	
	public String asOwlString() {
		ds.begin(ReadWrite.READ) ;
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			RDFDataMgr.write(stream, ds.getUnionModel(), Lang.RDFXML);

			// TODO owl printed here doesn't have datatype attribute
			// TODO attempts to print ttl or some other format instead end up with exceptions
			// https://jena.apache.org/documentation/io/rdf-output.html#normal-printing
			
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
		throw new Exception("not implemented");

	}

	@Override
	public SparqlEndpointInterface copy() throws Exception {
		return this;
	}
	
}
