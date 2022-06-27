package com.ge.research.semtk.services.nodeGroupService.requests;

import org.json.simple.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;

public class PropertyConnectionTriple {
	@Schema(
            description = "SparqlId of the subject node",
            required = true)
	private String subjectSparqlId;
	@Schema(
			description = "Property URI",
            required = true)
	private String propURI;
	@Schema(
			description = "SparqlId of the object node",
            required = true)
	private String objectSparqlId;
	
	public PropertyConnectionTriple() {
		
	}
	
	public PropertyConnectionTriple(String objectSparqlId, String propURI, String subjectSparqlId) {
		this.subjectSparqlId = subjectSparqlId;
		this.propURI = propURI;
		this.objectSparqlId = objectSparqlId;
	}
	
	public String getSubjectSparqlId() {
		return subjectSparqlId;
	}
	
	public String getPropURI() {
		return propURI;
	}
	public String getObjectSparqlId() {
		return objectSparqlId;
	}
	public JSONObject toJson() {
		JSONObject ret = new JSONObject();
		ret.put("subjectSparqlId", this.subjectSparqlId);
		ret.put("propURI", this.propURI);
		ret.put("objectSparqlId", this.objectSparqlId);
		return ret;
	}
}
