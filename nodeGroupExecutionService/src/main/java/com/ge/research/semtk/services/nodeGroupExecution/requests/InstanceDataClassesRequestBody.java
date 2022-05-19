package com.ge.research.semtk.services.nodeGroupExecution.requests;

import static org.hamcrest.CoreMatchers.anything;

import java.util.ArrayList;
import java.util.Arrays;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.json.simple.JSONArray;

import com.ge.research.semtk.springutilib.requests.SparqlConnectionRequest;

import io.swagger.v3.oas.annotations.media.Schema;

public class InstanceDataClassesRequestBody extends SparqlConnectionLimitOffsetCountRequestBody {
	
	
	@Schema(
			required = true,
			example = "[\"http:/namespace#class1\", \"http:/namespace#class2\"]")
	private String [] classValues = new String[0];
	
	

	public ArrayList<String> getClassValues() {
		return new ArrayList<String> ( Arrays.asList(this.classValues));
	}
	
}
