package com.ge.research.semtk.belmont.test;
/**
 ** Copyright 2021 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */
import static org.junit.Assert.*;

import java.util.ArrayList;

import org.apache.jena.atlas.json.JSON;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.NodeGroupItemStr;
import com.ge.research.semtk.belmont.ValidationAssistant;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

/**
 * _IT is just for the oInfo
 *  
 * @author 200001934
 *
 */
public class ValidationAssistantTest_IT {

	private static OntologyInfo oInfo = null;
	
	@BeforeClass
	public static void setup() throws Exception {
		TestGraph.clearGraph();
		IntegrationTestUtility.authenticateJunit();
		TestGraph.uploadOwlContents(
				Utility.getResourceAsString(QueryGenTest_IT.class, "AnimalSubProps.owl"));
		oInfo = TestGraph.getOInfo();
	}
	
	@Test
	public void test() throws Exception {
		// animal should match itself
		NodeGroup ng = TestGraph.getNodeGroupFromResource(this, "/animalQuery.json");
		Node node = ng.getNodeBySparqlID("?Animal");
		ArrayList<String> suggestions = ValidationAssistant.suggestNodeClass(oInfo, ng, new NodeGroupItemStr(node));
		String expect = node.getUri();
		String actual = ValidationAssistant.getSuggestionClass(suggestions.get(0));
		assertTrue("Expected first suggestion of to be " + expect + ". Found " + suggestions.toString(), expect.equals(actual));
		
		// change #name property to #scaryName and we should get Tiger
		ng = this.getNodegroupWithUriReplaced("animalQuery.json", "#name", "#scaryName");
		node = ng.getNodeBySparqlID("?Animal");
		suggestions = ValidationAssistant.suggestNodeClass(oInfo, ng, new NodeGroupItemStr(node));
		expect = "http://AnimalSubProps#Tiger";
		actual = ValidationAssistant.getSuggestionClass(suggestions.get(0));
		assertTrue("Expected first suggestion of to be " + expect + ". Found " + suggestions.toString(), expect.equals(actual));
		
		// try one where incoming nodeItem chooses
		// HERE
		ng = this.getNodegroupWithUriReplaced("animalSubPropsCats.json", 
				"\"fullURIName\": \"http://AnimalSubProps#Animal\"", 
				"\"fullURIName\": \"http://AnimalSubProps#Invalid\"");     // change the type of the Demon node
		node = ng.getNodeBySparqlID("?Demon");
		suggestions = ValidationAssistant.suggestNodeClass(oInfo, ng, new NodeGroupItemStr(node));
		expect = "http://AnimalSubProps#Animal";
		actual = ValidationAssistant.getSuggestionClass(suggestions.get(0));
		assertTrue("Expected first suggestion of to be " + expect + ". Found " + suggestions.toString(), expect.equals(actual));
	}
	
	/*
	 * Load a nodegroup and swap a URI.
	 */
	private NodeGroup getNodegroupWithUriReplaced(String resource, String fromUri, String toUri) throws Exception {
		String contents = Utility.getResourceAsString(this,  resource);
		contents = contents.replaceAll(fromUri, toUri);
		SparqlGraphJson sgjson = new SparqlGraphJson((JSONObject) (new JSONParser()).parse(contents));
		return sgjson.getNodeGroup();  // no inflation or validating
	}

}
