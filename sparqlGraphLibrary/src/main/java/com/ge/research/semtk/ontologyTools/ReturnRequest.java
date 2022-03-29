package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;

import org.json.simple.JSONObject;

public class ReturnRequest {
	public ArrayList<String> dataPropUris = new ArrayList<String>();  
	public String returnUri;      // URI to be set to isReturned
	
	public ReturnRequest(String returnUri) {
		super();
		this.returnUri = returnUri;
	}
	
	public ReturnRequest(String returnUri, ArrayList<String> dataPropUris) {
		super();
		this.dataPropUris = (dataPropUris == null) ? (new ArrayList<String>() ) : dataPropUris;
		this.returnUri = returnUri;
	}
	
	/**
	 * 
	 * @return Array of data properties, possibly empty, never null.
	 */
	public ArrayList<String> getDataPropUris() {
		return this.dataPropUris;
	}
	public void setDataPropUris(ArrayList<String> val) {
		this.dataPropUris = (val == null) ? (new ArrayList<String>() ) : val;;
	}
	public String getReturnUri() {
		return returnUri;
	}
	public void setReturnUri(String returnUri) {
		this.returnUri = returnUri;
	}
	public String toString() {
		return "return: " + returnUri + " dataPropUris: [" + String.join(",", this.dataPropUris) + "]";
	}

	public static ReturnRequest fromJSON(JSONObject o) {
		if (o.containsKey("return")) {
			// deprecated
			return new ReturnRequest((String) o.get("return"), null);
		} else {
			return new ReturnRequest((String) o.get("returnUri"), (ArrayList<String>) o.get("dataPropUris"));
		}
	}
}
