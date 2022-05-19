package com.ge.research.semtk.services.nodeGroupExecution.requests;

import java.util.ArrayList;


import io.swagger.v3.oas.annotations.media.Schema;

public class InstanceDataPredicatesRequestBody extends SparqlConnectionLimitOffsetCountRequestBody {
	
	@Schema(
			description = "list of domainURI, predicateURI pairs",
			required = true,
			example = "[[\"http:/namespace#class1\", \"http:/namespace#predicate\"]]")
	private ArrayList<InstanceDataPredicate> predicateList;

	public ArrayList<InstanceDataPredicate> getPredicateList() {
		return predicateList;
	}
	
	/**
	 * Get predicateList as String[][2] so that SparqlGraphLibrary doesn't need to know InstanceDataPredicate (spring annotated)
	 * @return
	 */
	public ArrayList<String[]> buildPredicateListPairs() {
		ArrayList<String[]> ret = new ArrayList<String[]>();
		
		for (int i=0; i < this.predicateList.size(); i++) {
			ret.add(new String [] {this.predicateList.get(i).getDomainURI(), this.predicateList.get(i).getPredicateURI()} );
		}
		return ret;
	}
}
