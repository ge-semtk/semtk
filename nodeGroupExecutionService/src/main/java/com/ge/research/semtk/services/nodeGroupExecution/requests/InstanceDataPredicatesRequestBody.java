package com.ge.research.semtk.services.nodeGroupExecution.requests;

import static org.hamcrest.CoreMatchers.anything;

import java.util.ArrayList;
import java.util.Arrays;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.json.simple.JSONArray;
import org.mortbay.util.ajax.JSON;

import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;

import io.swagger.annotations.ApiModelProperty;

public class InstanceDataPredicatesRequestBody extends SparqlConnectionLimitOffsetCountRequestBody {
	
	@ApiModelProperty(
			value = "list of domainURI, predicateURI pairs",
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
