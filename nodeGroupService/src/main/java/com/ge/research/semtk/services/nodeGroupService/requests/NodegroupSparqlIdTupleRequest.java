package com.ge.research.semtk.services.nodeGroupService.requests;

import java.util.ArrayList;
import java.util.List;

import com.ge.research.semtk.nodeGroupService.SparqlIdTuple;

public class NodegroupSparqlIdTupleRequest extends NodegroupRequest {
	List<SparqlIdTuple> sparqlIdTuples = null;

	public List<SparqlIdTuple> getSparqlIdTuples() {
		return sparqlIdTuples;
	}

	public void setSparqlIdTuples(ArrayList<SparqlIdTuple> sparqlIdTuples) {
		this.sparqlIdTuples = sparqlIdTuples;
	}

	public void validate() throws Exception {
		super.validate();
		
		if (this.sparqlIdTuples == null || this.sparqlIdTuples.size() < 1) {
			throw new Exception("sparqlIdTuples is empty");
		} else {
			for (SparqlIdTuple tuple : this.sparqlIdTuples) {
				if (tuple.getSparqlIdFrom()== null || tuple.getSparqlIdFrom().length() < 1) {
					throw new Exception("sparqlIdReturnedTuples contains an empty sparqlIdFrom");
				}
				if (tuple.getSparqlIdTo() == null || tuple.getSparqlIdTo().length() < 1) {
					throw new Exception("sparqlIdReturnedTuples contains an empty sparqlIdTo");
				}
			}
		}
	}

}
