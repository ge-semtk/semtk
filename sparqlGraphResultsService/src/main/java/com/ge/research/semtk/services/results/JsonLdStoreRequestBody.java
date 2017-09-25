package com.ge.research.semtk.services.results;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ge.research.semtk.resultSet.NodeGroupResultSet;

public class JsonLdStoreRequestBody extends ResultsRequestBody {

	private String jsonRenderedGraph;
	private String jsonRenderedHeader;
	
	public JSONObject getJsonRenderedHeader() throws ParseException {
		
		JSONParser jParse = new JSONParser();
		
		return (JSONObject) jParse.parse(jsonRenderedHeader);
	}

	public void setJsonRenderedHeader(String jsonRenderedHeader) {
		this.jsonRenderedHeader = jsonRenderedHeader;
	}

	public String getJsonRenderedGraph() {
		return jsonRenderedGraph;
	}

	public void setJsonRenderedGraph(String jsonRenderedGraph) {
		this.jsonRenderedGraph = jsonRenderedGraph;
	}
	
	
}
