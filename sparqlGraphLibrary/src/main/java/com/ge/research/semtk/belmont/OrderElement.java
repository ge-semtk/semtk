package com.ge.research.semtk.belmont;

import org.json.simple.JSONObject;

public class OrderElement {
	private String sparqlID = null;
	private String func = "";
	
	public OrderElement(String sparqlID) {
		this.sparqlID = sparqlID;
		this.fixID();
	}
	
	public OrderElement(String sparqlID, String func) {
		this.sparqlID = sparqlID;
		this.func = func;
		this.fixID();
	}
	
	public OrderElement(JSONObject jObj) throws Exception {
		this.sparqlID = (String) jObj.get("sparqlID");
		if (jObj.containsKey("func")) {
			this.func = (String) jObj.get("func");
		}
		this.fixID();
	}
	
	private void fixID() {
		if (!this.sparqlID.isEmpty() && ! this.sparqlID.startsWith("?")) {
			this.sparqlID = "?" + this.sparqlID;
		}
	}
	
	public String getSparqlID() {
		return sparqlID;
	}

	public void setSparqlID(String sparqlID) {
		this.sparqlID = sparqlID;
	}

	public String getFunc() {
		return func;
	}

	public void setFunc(String func) {
		this.func = func;
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject jObj = new JSONObject();
		jObj.put("sparqlID", this.sparqlID);
		if (! this.func.isEmpty()) {
			jObj.put("func", this.func);
		}
		return jObj;
	}
	
	public String toSparql() {
		if (! this.func.isEmpty()) {
			return String.format("%s(%s)", this.func, this.sparqlID);
		} else {
			return this.sparqlID;
		}
	}
	
}
