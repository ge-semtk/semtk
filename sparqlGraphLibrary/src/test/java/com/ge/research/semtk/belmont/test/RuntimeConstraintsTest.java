/**
 ** Copyright 2017 General Electric Company
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

package com.ge.research.semtk.belmont.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraintManager;
import com.ge.research.semtk.belmont.runtimeConstraints.RuntimeConstraintMetaData;
import com.ge.research.semtk.belmont.runtimeConstraints.SupportedOperations;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.utility.Utility;

public class RuntimeConstraintsTest {

	public static final String testJsonString = "{ \"sparqlConn\": { \"name\": \"pop music test\", \"type\": \"virtuoso\", \"dsURL\": \"http://fakeserver:2420\", \"dsKsURL\": \"\", \"dsDataset\": \"http://research.ge.com/test/popmusic/data\", \"domain\": \"http://\", \"onDataset\": \"http://research.ge.com/test/popmusic/model\" }, \"sNodeGroup\": { \"version\": 1, \"sNodeList\": [ { \"propList\": [ { \"KeyName\": \"name\", \"ValueType\": \"string\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#string\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#name\", \"Constraints\": \"FILTER regex(%id, \\\"the Beatles\\\")\", \"fullURIName\": \"\", \"SparqlID\": \"?name_0\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] } ], \"nodeList\": [ { \"SnodeSparqlIDs\": [], \"KeyName\": \"member\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"\", \"Connected\": false, \"UriConnectBy\": \"\", \"isOptional\": 0 } ], \"NodeName\": \"Band\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#Band\", \"subClassNames\": [], \"SparqlID\": \"?OriginalBand\", \"isReturned\": true, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null }, { \"propList\": [ { \"KeyName\": \"songTitle\", \"ValueType\": \"string\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#string\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#songTitle\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"?songTitle\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] } ], \"nodeList\": [ { \"SnodeSparqlIDs\": [], \"KeyName\": \"composer\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"\", \"Connected\": false, \"UriConnectBy\": \"\", \"isOptional\": 0 }, { \"SnodeSparqlIDs\": [ \"?OriginalBand\" ], \"KeyName\": \"originalArtrist\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"originalArtrist\", \"Connected\": true, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#originalArtrist\", \"isOptional\": 0 } ], \"NodeName\": \"Song\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#Song\", \"subClassNames\": [], \"SparqlID\": \"?Song\", \"isReturned\": false, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null }, { \"propList\": [ { \"KeyName\": \"durationInSeconds\", \"ValueType\": \"int\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#int\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#durationInSeconds\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"?durationInSeconds\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": true, \"instanceValues\": [] }, { \"KeyName\": \"recordingDate\", \"ValueType\": \"date\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#date\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#recordingDate\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"\", \"isReturned\": false, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] }, { \"KeyName\": \"trackNumber\", \"ValueType\": \"int\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#int\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#trackNumber\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"\", \"isReturned\": false, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] } ], \"nodeList\": [ { \"SnodeSparqlIDs\": [], \"KeyName\": \"recordingArtist\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"\", \"Connected\": false, \"UriConnectBy\": \"\", \"isOptional\": 0 }, { \"SnodeSparqlIDs\": [ \"?Song\" ], \"KeyName\": \"song\", \"ValueType\": \"Song\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Song\", \"ConnectBy\": \"song\", \"Connected\": true, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#song\", \"isOptional\": 0 } ], \"NodeName\": \"AlbumTrack\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#AlbumTrack\", \"subClassNames\": [], \"SparqlID\": \"?AlbumTrack\", \"isReturned\": false, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null }, { \"propList\": [ { \"KeyName\": \"style\", \"ValueType\": \"string\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#string\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#style\", \"Constraints\": \"FILTER regex(%id, \\\"Rock and Roll\\\")\", \"fullURIName\": \"\", \"SparqlID\": \"?style\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] } ], \"nodeList\": [], \"NodeName\": \"Genre\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#Genre\", \"subClassNames\": [], \"SparqlID\": \"?Genre\", \"isReturned\": false, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null }, { \"propList\": [ { \"KeyName\": \"name\", \"ValueType\": \"string\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#string\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#name\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"?name\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": true, \"instanceValues\": [] } ], \"nodeList\": [ { \"SnodeSparqlIDs\": [], \"KeyName\": \"member\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"\", \"Connected\": false, \"UriConnectBy\": \"\", \"isOptional\": 0 } ], \"NodeName\": \"Band\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#Band\", \"subClassNames\": [], \"SparqlID\": \"?RelaeaseBand\", \"isReturned\": true, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null }, { \"propList\": [ { \"KeyName\": \"albumTtitle\", \"ValueType\": \"string\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#string\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#albumTtitle\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"?albumTtitle\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] }, { \"KeyName\": \"releaseDate\", \"ValueType\": \"date\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#date\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#releaseDate\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"\", \"isReturned\": false, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] } ], \"nodeList\": [ { \"SnodeSparqlIDs\": [ \"?RelaeaseBand\" ], \"KeyName\": \"band\", \"ValueType\": \"Band\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Band\", \"ConnectBy\": \"band\", \"Connected\": true, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#band\", \"isOptional\": 0 }, { \"SnodeSparqlIDs\": [ \"?Genre\" ], \"KeyName\": \"genre\", \"ValueType\": \"Genre\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Genre\", \"ConnectBy\": \"genre\", \"Connected\": true, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#genre\", \"isOptional\": 0 }, { \"SnodeSparqlIDs\": [], \"KeyName\": \"producer\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"producer\", \"Connected\": false, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#producer\", \"isOptional\": 0 }, { \"SnodeSparqlIDs\": [ \"?AlbumTrack\" ], \"KeyName\": \"track\", \"ValueType\": \"AlbumTrack\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#AlbumTrack\", \"ConnectBy\": \"track\", \"Connected\": true, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#track\", \"isOptional\": 0 } ], \"NodeName\": \"Album\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#Album\", \"subClassNames\": [], \"SparqlID\": \"?Album\", \"isReturned\": false, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null } ] }, \"importSpec\": { \"version\": \"1\", \"baseURI\": \"\", \"columns\": [], \"texts\": [], \"transforms\": [], \"nodes\": [] }, \"RuntimeConstraints\" :[ { \"SparqlID\" : \"?durationInSeconds\", \"Operator\" : \"GREATERTHANOREQUALS\", \"Operands\" : [ 300 ] }, { \"SparqlID\" : \"?name\", \"Operator\" : \"MATCHES\", \"Operands\" : [ \"AFI\" ] } ] }";
	public NodeGroup ng;
	public JSONObject nodeGroupJSON = null;
	public JSONArray runtimeConstraintsJSON = null;
	
	private void setup() throws Exception{
		SparqlGraphJson sparqlGraphJson = new SparqlGraphJson(testJsonString);
		ng = sparqlGraphJson.getNodeGroup();	
		runtimeConstraintsJSON = sparqlGraphJson.getRuntimeConstraintsJson();
	}
	
	@Test 
	public void getRuntimeConstraintsFromNodeGroup() throws Exception{
		this.setup();
		
		// actually try getting the rt constraints. 
		HashMap<String, RuntimeConstraintMetaData> retval = this.ng.getRuntimeConstrainedItems();
		
		// check for the actual values we expect
		
		if( retval.containsKey("?durationInSeconds") && retval.containsKey("?name")){
			// do nothing
			System.err.println("getRuntimeConstraintsFromNodeGroup() :: Both expected runtime constraints found: ( ?durationInSeconds & ?name )" );
		}
		else{
			fail();
		}		
	}
	
	@Test
	public void getRuntimeConstraintsFromJson() throws Exception{
		// simple test to confirm given json format is as expected. not a funcitonal code test.
		setup();
		
		// check size
		if(runtimeConstraintsJSON.size() == 2){
			System.err.println("getRuntimeConstraintsFromJson() :: Both expected runtime constraints found" );			
		}
		else{ fail(); }
		//check sparqlIDs
		int count = 0;
		for(Object jCurr : runtimeConstraintsJSON){
			JSONObject inUse = (JSONObject) jCurr;
			
			if(count == 0){
				if(inUse.get("SparqlID").toString().equals("?durationInSeconds") && inUse.get("Operator").toString().equals("GREATERTHANOREQUALS")){
					System.err.println("getRuntimeConstraintsFromJson() :: first constraint sparqlId and operation as expected" );		
				}
				else{
					fail();
				}
			}
			else{
				if(inUse.get("SparqlID").toString().equals("?name") && inUse.get("Operator").toString().equals("MATCHES")){
					System.err.println("getRuntimeConstraintsFromJson() :: second constraint sparqlId and operation as expected" );		
				}
				else{
					fail();
				}
			}
			count++;
		}
	}
	
	@Test
	public void attemptOverrideUsingRuntimeConstraints() throws Exception{
		// check that runtime constraints are applied to nodegroup without generating an exception.
		this.setup();
		
		RuntimeConstraintManager rtConstraints = new RuntimeConstraintManager(ng);
		rtConstraints.applyConstraintJson(runtimeConstraintsJSON);
		
		// if we got this far, let's check the constraints.
		
		// check the ?name constraint.
		String c0 = ng.getItemBySparqlID("?name").getValueConstraint().toString();
		String c1 = ng.getItemBySparqlID("?durationInSeconds").getValueConstraint().toString();
		
		assertTrue("attemptOverrideUsingRuntimeConstraints() string FILTER IN", c0.contains("FILTER (?name IN") && c0.contains("AFI"));
		
		assertTrue("attemptOverrideUsingRuntimeConstraints() int FILTER >=", c1.equals("FILTER (?durationInSeconds >= 300)"));
	}
	
	
	/**
	 * Confirm that we get a good error message if try to apply runtime constraint with an invalid sparqlId ("durationInYears")
	 */
	@Test
	public void failIfBadRuntimeConstraints() throws Exception{
		boolean exceptionThrown = false;
		try{
			this.setup();
			RuntimeConstraintManager rtConstraints = new RuntimeConstraintManager(ng);
			JSONArray invalidRuntimeConstraintJSON = Utility.getJsonArrayFromString("[ { \"SparqlID\" : \"?durationInYears\", \"Operator\" : \"GREATERTHANOREQUALS\", \"Operands\" : [ 300 ] } ]");
			rtConstraints.applyConstraintJson(invalidRuntimeConstraintJSON);
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot find runtime-constrainable item in nodegroup.  sparqlID: ?durationInYears"));
		}
		assertTrue(exceptionThrown);
	}
	
	
	@Test 
	public void getSparqlUsingRuntimeConstraints() throws Exception{
		// check that the generated sparql select contains the expected constraints.
		this.setup();
		
		RuntimeConstraintManager rtConstraints = new RuntimeConstraintManager(ng);
		rtConstraints.applyConstraintJson(runtimeConstraintsJSON);
		
		// check for the constraints in generated sparql.
		String sparql = ng.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, false, 10000000, null);
		
		if(sparql.contains("FILTER (?name IN ( \"AFI\"")){
			System.err.println("getSparqlUsingRuntimeConstraints() :: constraint for ?name was as expected") ;
				
		}
		else{ fail(); }
		
		if(sparql.contains("FILTER (?durationInSeconds >= 300")){
			System.err.println("getSparqlUsingRuntimeConstraints() :: constraint for ?durationInSeconds was as expected") ;
				
		}
		else{ fail(); }		
	}
	
	// easy functional tests after this point. these are mostly to make sure we have good coverage
	
	@Test
	public void testContraintsSetByType() throws Exception{
		setup();
		
		// alter the node group for more RT constraints so we have good coverage.
		// there is already a numeric rtc and a string rtc. we need date and URI for basic coverage.
		
		// add a constraint to releaseDate in album.
		ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").setIsRuntimeConstrained(true);
		ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").setSparqlID("?releaseDate"); // otherwise, no sparqlID
		ng.getNodeBySparqlID("?Album").setIsRuntimeConstrained(true);
		
		// build the runtimeConstrainedItems so we get the new ones.
		RuntimeConstraintManager constraints = new RuntimeConstraintManager(ng);
		
		// check all constraints listed.
		if( constraints.getConstrainedItemIds().size() == 4 ){
			System.err.println("testContraintsSetByType() :: expected number of runtime constraints found (4)") ;
			int x = 1;
			for(String i :  constraints.getConstrainedItemIds()){
				System.err.println("testContraintsSetByType() :: " +  i + " (" +  x + ")") ;
				x++;
			}
		}
		else{ fail(); }
		
		
		// attempt to set various constraints directly. 
		ArrayList<String> names = new ArrayList<String>();

		// numeric
		names.add("23");
		names.add("37");

		// set matches
		constraints.applyConstraint("?durationInSeconds", SupportedOperations.MATCHES, names); 

		// check matches
		String constraintStr = ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint().toString();
		if(constraintStr.contains("FILTER (?durationInSeconds IN") && constraintStr.contains("23") && constraintStr.contains("37")){
			System.err.println("testContraintsSetByType() :: expected FILTER IN for ?durationInSeconds uri with Matches clause") ;
		}
		else{ 
			System.err.println(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint());
			fail(); }

		// set gt
		constraints.applyConstraint("?durationInSeconds", SupportedOperations.GREATERTHAN, names);

		// check gt
		if(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint().toString().contains("FILTER (?durationInSeconds > 23")){
			System.err.println("testContraintsSetByType() :: expected value for ?durationInSeconds uri with GreaterThan clause") ;
		}
		else{ 
			System.err.println(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint());
			fail(); }

		// set lt
		constraints.applyConstraint("?durationInSeconds", SupportedOperations.LESSTHAN, names);

		// check lt
		if(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint().toString().contains("FILTER (?durationInSeconds < 23")){
			System.err.println("testContraintsSetByType() :: expected value for ?durationInSeconds uri with LessThan clause") ;
		}
		else{ 
			System.err.println(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint());
			fail(); }

		// set gte
		constraints.applyConstraint("?durationInSeconds", SupportedOperations.GREATERTHANOREQUALS, names);

		// check gte
		if(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint().toString().contains("FILTER (?durationInSeconds >= 23")){
			System.err.println("testContraintsSetByType() :: expected value for ?durationInSeconds uri with GreaterThanOrEquals clause") ;
		}
		else{ 
			System.err.println(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint());
			fail(); }

		// set lte
		constraints.applyConstraint("?durationInSeconds", SupportedOperations.LESSTHANOREQUALS, names);

		// check lte
		if(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint().toString().contains("FILTER (?durationInSeconds <= 23")){
			System.err.println("testContraintsSetByType() :: expected value for ?durationInSeconds uri with LessThanOrEquals clause") ;
		}
		else{ 
			System.err.println(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint());
			fail(); }

		// set value between
		constraints.applyConstraint("?durationInSeconds", SupportedOperations.VALUEBETWEEN, names);

		// check value between
		if(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint().toString().equals("FILTER ( ?durationInSeconds > 23  &&  ?durationInSeconds < 37 )")){
			System.err.println("testContraintsSetByType() :: expected value for ?durationInSeconds uri with ValueBetween clause") ;
		}
		else{ 
			System.err.println(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint());
			fail(); }

		// set value between uninclusive
		constraints.applyConstraint("?durationInSeconds", SupportedOperations.VALUEBETWEENUNINCLUSIVE, names);

		// check value between uniclusive
		if(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint().toString().equals("FILTER ( ?durationInSeconds >= 23  &&  ?durationInSeconds <= 37 )")){
			System.err.println("testContraintsSetByType() :: expected value for ?durationInSeconds uri with ValueBetweenUninclusive clause") ;
		}
		else{ 
			System.err.println(ng.getNodeBySparqlID("?AlbumTrack").getPropertyByKeyname("durationInSeconds").getValueConstraint());
			fail(); }

		// date
		names = new ArrayList<String>();	
		names.add("2017-01-17T00:00");  // 2014-05-23T10:20:13+05:30
		names.add("1955-11-05T22:04");

		// set matches
		constraints.applyConstraint("?releaseDate", SupportedOperations.MATCHES, names);
		constraintStr = ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint().toString();
		// check matches
		if(constraintStr.contains("FILTER (?releaseDate IN (") && constraintStr.contains("2017-01-17T00:00") && constraintStr.contains("1955-11-05T22:04")){
			System.err.println("testContraintsSetByType() :: expected value for ?releaseDate uri with Matches clause") ;
		}
		else{ 
			System.err.println(ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint());
			fail(); }
		// check greater than
		constraints.applyConstraint("?releaseDate", SupportedOperations.GREATERTHAN, names);
		if(ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint().toString().equals("FILTER (?releaseDate > \"2017-01-17T00:00\"^^<http://www.w3.org/2001/XMLSchema#date>)")){
			System.err.println("testContraintsSetByType() :: expected value for ?releaseDate uri with greater than clause") ;
		}
		else{ 
			System.err.println("greater : "  + ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint());
			fail(); }
		// check less than
		constraints.applyConstraint("?releaseDate", SupportedOperations.LESSTHAN, names);
		if(ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint().toString().equals("FILTER (?releaseDate < \"2017-01-17T00:00\"^^<http://www.w3.org/2001/XMLSchema#date>)")){
			System.err.println("testContraintsSetByType() :: expected value for ?releaseDate uri with less than clause") ;
		}
		else{ 
			System.err.println("lesser : "  + ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint());
			fail(); }
		// check interval
		constraints.applyConstraint("?releaseDate", SupportedOperations.VALUEBETWEEN, names);
		if(ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint().toString().equals("FILTER ( ?releaseDate > \"2017-01-17T00:00\"^^<http://www.w3.org/2001/XMLSchema#date>  &&  ?releaseDate < \"1955-11-05T22:04\"^^<http://www.w3.org/2001/XMLSchema#date> )")){
			System.err.println("testContraintsSetByType() :: expected value for ?releaseDate uri with interval clause") ;
		}
		else{ 
			System.err.println("between : "  + ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint());
			fail(); }
		constraints.applyConstraint("?releaseDate", SupportedOperations.VALUEBETWEENUNINCLUSIVE, names);
		if(ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint().toString().equals("FILTER ( ?releaseDate >= \"2017-01-17T00:00\"^^<http://www.w3.org/2001/XMLSchema#date>  &&  ?releaseDate <= \"1955-11-05T22:04\"^^<http://www.w3.org/2001/XMLSchema#date> )")){
			System.err.println("testContraintsSetByType() :: expected value for ?releaseDate uri with interval clause") ;
		}
		else{ 
			System.err.println("between : "  + ng.getNodeBySparqlID("?Album").getPropertyByKeyname("releaseDate").getValueConstraint());
			fail(); }


		// string

		names = new ArrayList<String>();
		names.add("30 seconds to mars");
		names.add("parabelle");
		names.add("soundgarden");

		// set matches
		constraints.applyConstraint("?name", SupportedOperations.MATCHES, names);

		// check matches
		String vc = ng.getNodeBySparqlID("?RelaeaseBand").getPropertyByKeyname("name").getValueConstraint().toString();
		if(vc.contains("FILTER (?name IN") &&
				vc.contains("\"30 seconds to mars\"") &&
				vc.contains("\"parabelle\"") &&
				vc.contains("\"soundgarden\"")){
			//System.err.println("testContraintsSetByType() :: expected value for ?name uri with Matches clause") ;
		}
		else{ 
			System.err.println(ng.getNodeBySparqlID("?RelaeaseBand").getPropertyByKeyname("name").getValueConstraint());
			fail(); 
		}

		// set regex
		constraints.applyConstraint("?name", SupportedOperations.REGEX, names);

		// check regex
		if(ng.getNodeBySparqlID("?RelaeaseBand").getPropertyByKeyname("name").getValueConstraint().toString().equals("FILTER regex(?name ,\"30 seconds to mars\")")){
			//System.err.println("testContraintsSetByType() :: expected value for ?name uri with Regex clause") ;
		}
		else{ 
			System.err.println(ng.getNodeBySparqlID("?RelaeaseBand").getPropertyByKeyname("name").getValueConstraint());
			fail(); }


		// NODE_URI


		// no prefix, no angle brackets.
		names = new ArrayList<String>();
		names.add("test://music#decemberunderground");

		// set matches
		constraints.applyConstraint("?Album", SupportedOperations.MATCHES, names);
		constraintStr = ng.getNodeBySparqlID("?Album").getValueConstraintStr();
		// check matches
		if(constraintStr.contains("FILTER (?Album IN") && constraintStr.contains("<test://music#decemberunderground>")){
			//System.err.println("testContraintsSetByType() :: expected value for ?Album uri with Matches clause (no angle brackets)") ;
		}
		else { fail(); }

		// no prefix, angle brackets included
		names = new ArrayList<String>();
		names.add("<test://music#decemberunderground>");

		// set matches
		constraints.applyConstraint("?Album", SupportedOperations.MATCHES, names);
		constraintStr = ng.getNodeBySparqlID("?Album").getValueConstraintStr();

		if(constraintStr.contains("FILTER (?Album IN") && constraintStr.contains("<test://music#decemberunderground>")){
			//System.err.println("testContraintsSetByType() :: expected value for ?Album uri with Matches clause (angle brackets included)") ;
		}
		else { fail(); }

		// prefixed
		names = new ArrayList<String>();
		names.add("music:decemberunderground");

		// set matches
		constraints.applyConstraint("?Album", SupportedOperations.MATCHES, names);
		constraintStr = ng.getNodeBySparqlID("?Album").getValueConstraintStr();

		if(constraintStr.contains("FILTER (?Album IN") && constraintStr.contains("music:decemberunderground")){
			//System.err.println("testContraintsSetByType() :: expected value for ?Album uri with Matches clause (prefixed)") ;
		}
		else { fail(); }
	}

	@Test
	public void generateJson() throws Exception{
		// apply constraint to only one constrainable
		// and generate json string without exception
		this.setup();

		RuntimeConstraintManager constraints = new RuntimeConstraintManager(ng);
		ArrayList<String> names = new ArrayList<String>();
		names.add("music:decemberunderground");
		constraints.applyConstraint("?name", SupportedOperations.MATCHES, names);

		String str = constraints.toJSONString();
		assertTrue(str.contains("name"));
		assertTrue(str.contains("MATCHES"));
		assertTrue(str.contains("december"));
		assertFalse(str.contains("Album"));
	}

}
