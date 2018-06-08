package com.ge.research.semtk.services.nodeGroupService.requests;

import java.util.ArrayList;

public class NodegroupSparqlIdReturnedRequest extends NodegroupRequest {
	ArrayList<SparqlIdReturnedTuple> sparqlIdReturnedTuples = null;

	public ArrayList<SparqlIdReturnedTuple> getSparqlIdBooleanTuples() {
		return sparqlIdReturnedTuples;
	}

	public void setSparqlIdBooleanTuples(ArrayList<SparqlIdReturnedTuple> sparqlIdBooleanTuples) {
		this.sparqlIdReturnedTuples = sparqlIdBooleanTuples;
	}

	public void validate() throws Exception {
		super.validate();
		
		if (this.sparqlIdReturnedTuples == null || this.sparqlIdReturnedTuples.size() < 1) {
			throw new Exception("sparqlIdReturnedTuples is empty");
		} else {
			for (SparqlIdReturnedTuple tuple : this.sparqlIdReturnedTuples) {
				if (tuple.getSparqlId() == null || tuple.getSparqlId().length() < 1) {
					throw new Exception("sparqlIdReturnedTuples contains an empty sparqlId");
				}
			}
		}
	}
}
