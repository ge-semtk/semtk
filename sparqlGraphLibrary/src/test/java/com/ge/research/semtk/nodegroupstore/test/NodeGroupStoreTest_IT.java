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

package com.ge.research.semtk.nodegroupstore.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.nodegroupStore.NgStore;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

/**
 * Test the nodegroup store
 */
public class NodeGroupStoreTest_IT {

	// for stored nodegroup execution
	private static NodeGroupStoreRestClient nodeGroupStoreClient = null;
		
	// id to use for stored nodegroups
	private final static String ID = "test" + UUID.randomUUID();
	private final static String COMMENTS = "pop music nodegroup for testing";
	private final static String CREATOR = "Junit NodeGroupStoreTest_IT.java";
	private final static String NG_JSON_STRING = "{ \"sparqlConn\": { \"name\": \"pop music test\", \"type\": \"virtuoso\", \"dsURL\": \"http://host:2420\", \"dsKsURL\": \"\", \"dsDataset\": \"http://research.ge.com/test/popmusic/data\", \"domain\": \"http://\", \"onDataset\": \"http://research.ge.com/test/popmusic/model\" }, \"sNodeGroup\": { \"version\": 1, \"sNodeList\": [ { \"propList\": [ { \"KeyName\": \"name\", \"ValueType\": \"string\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#string\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#name\", \"Constraints\": \"FILTER regex(%id, \\\"the Beatles\\\")\", \"fullURIName\": \"\", \"SparqlID\": \"?name\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] } ], \"nodeList\": [], \"NodeName\": \"Artist\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"subClassNames\": [ \"http://com.ge.research/knowledge/test/popMusic#Band\" ], \"SparqlID\": \"?Artist\", \"isReturned\": false, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null }, { \"propList\": [ { \"KeyName\": \"songTitle\", \"ValueType\": \"string\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#string\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#songTitle\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"?songTitle\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] } ], \"nodeList\": [ { \"SnodeSparqlIDs\": [], \"KeyName\": \"composer\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"\", \"Connected\": false, \"UriConnectBy\": \"\", \"isOptional\": 0 }, { \"SnodeSparqlIDs\": [ \"?Artist\" ], \"KeyName\": \"originalArtrist\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"originalArtrist\", \"Connected\": true, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#originalArtrist\", \"isOptional\": 0 } ], \"NodeName\": \"Song\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#Song\", \"subClassNames\": [], \"SparqlID\": \"?Song\", \"isReturned\": false, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null }, { \"propList\": [ { \"KeyName\": \"durationInSeconds\", \"ValueType\": \"int\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#int\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#durationInSeconds\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"?durationInSeconds\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": true, \"instanceValues\": [] }, { \"KeyName\": \"recordingDate\", \"ValueType\": \"date\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#date\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#recordingDate\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"\", \"isReturned\": false, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] }, { \"KeyName\": \"trackNumber\", \"ValueType\": \"int\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#int\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#trackNumber\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"?trackNumber\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] } ], \"nodeList\": [ { \"SnodeSparqlIDs\": [], \"KeyName\": \"recordingArtist\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"\", \"Connected\": false, \"UriConnectBy\": \"\", \"isOptional\": 0 }, { \"SnodeSparqlIDs\": [ \"?Song\" ], \"KeyName\": \"song\", \"ValueType\": \"Song\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Song\", \"ConnectBy\": \"song\", \"Connected\": true, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#song\", \"isOptional\": 0 } ], \"NodeName\": \"AlbumTrack\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#AlbumTrack\", \"subClassNames\": [], \"SparqlID\": \"?AlbumTrack\", \"isReturned\": false, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null }, { \"propList\": [ { \"KeyName\": \"name\", \"ValueType\": \"string\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#string\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#name\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"?name_0\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] } ], \"nodeList\": [ { \"SnodeSparqlIDs\": [], \"KeyName\": \"member\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"\", \"Connected\": false, \"UriConnectBy\": \"\", \"isOptional\": 0 } ], \"NodeName\": \"Band\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#Band\", \"subClassNames\": [], \"SparqlID\": \"?Band\", \"isReturned\": false, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null }, { \"propList\": [ { \"KeyName\": \"albumTtitle\", \"ValueType\": \"string\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#string\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#albumTtitle\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"?albumTtitle\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] }, { \"KeyName\": \"releaseDate\", \"ValueType\": \"date\", \"relationship\": \"http://www.w3.org/2001/XMLSchema#date\", \"UriRelationship\": \"http://com.ge.research/knowledge/test/popMusic#releaseDate\", \"Constraints\": \"\", \"fullURIName\": \"\", \"SparqlID\": \"?releaseDate\", \"isReturned\": true, \"isOptional\": false, \"isRuntimeConstrained\": false, \"instanceValues\": [] } ], \"nodeList\": [ { \"SnodeSparqlIDs\": [ \"?Band\" ], \"KeyName\": \"band\", \"ValueType\": \"Band\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Band\", \"ConnectBy\": \"band\", \"Connected\": true, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#band\", \"isOptional\": 0 }, { \"SnodeSparqlIDs\": [], \"KeyName\": \"genre\", \"ValueType\": \"Genre\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Genre\", \"ConnectBy\": \"\", \"Connected\": false, \"UriConnectBy\": \"\", \"isOptional\": 0 }, { \"SnodeSparqlIDs\": [], \"KeyName\": \"producer\", \"ValueType\": \"Artist\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"ConnectBy\": \"\", \"Connected\": false, \"UriConnectBy\": \"\", \"isOptional\": 0 }, { \"SnodeSparqlIDs\": [ \"?AlbumTrack\" ], \"KeyName\": \"track\", \"ValueType\": \"AlbumTrack\", \"UriValueType\": \"http://com.ge.research/knowledge/test/popMusic#AlbumTrack\", \"ConnectBy\": \"track\", \"Connected\": true, \"UriConnectBy\": \"http://com.ge.research/knowledge/test/popMusic#track\", \"isOptional\": 0 } ], \"NodeName\": \"Album\", \"fullURIName\": \"http://com.ge.research/knowledge/test/popMusic#Album\", \"subClassNames\": [], \"SparqlID\": \"?Album\", \"isReturned\": false, \"isRuntimeConstrained\": false, \"valueConstraint\": \"\", \"instanceValue\": null } ] }, \"importSpec\": { \"version\": \"1\", \"baseURI\": \"\", \"columns\": [ { \"colId\": \"col_0\", \"colName\": \"Album\" }, { \"colId\": \"col_1\", \"colName\": \"AlbumTrack\" }, { \"colId\": \"col_2\", \"colName\": \"Arelease\" }, { \"colId\": \"col_3\", \"colName\": \"Artist\" }, { \"colId\": \"col_4\", \"colName\": \"Atitle\" }, { \"colId\": \"col_5\", \"colName\": \"Band\" }, { \"colId\": \"col_6\", \"colName\": \"artName\" }, { \"colId\": \"col_7\", \"colName\": \"bName\" }, { \"colId\": \"col_8\", \"colName\": \"durationInSec\" }, { \"colId\": \"col_9\", \"colName\": \"recordingDate\" }, { \"colId\": \"col_10\", \"colName\": \"song\" }, { \"colId\": \"col_11\", \"colName\": \"songTitle\" }, { \"colId\": \"col_12\", \"colName\": \"trackNumber\" } ], \"texts\": [], \"transforms\": [], \"nodes\": [ { \"sparqlID\": \"?Album\", \"type\": \"http://com.ge.research/knowledge/test/popMusic#Album\", \"mapping\": [ { \"colId\": \"col_0\", \"colName\": \"Album\" } ], \"props\": [ { \"URIRelation\": \"http://com.ge.research/knowledge/test/popMusic#albumTtitle\", \"mapping\": [ { \"colId\": \"col_4\", \"colName\": \"Atitle\" } ] }, { \"URIRelation\": \"http://com.ge.research/knowledge/test/popMusic#releaseDate\", \"mapping\": [ { \"colId\": \"col_2\", \"colName\": \"Arelease\" } ] } ] }, { \"sparqlID\": \"?Band\", \"type\": \"http://com.ge.research/knowledge/test/popMusic#Band\", \"mapping\": [ { \"colId\": \"col_5\", \"colName\": \"Band\" } ], \"props\": [ { \"URIRelation\": \"http://com.ge.research/knowledge/test/popMusic#name\", \"mapping\": [ { \"colId\": \"col_7\", \"colName\": \"bName\" } ] } ] }, { \"sparqlID\": \"?AlbumTrack\", \"type\": \"http://com.ge.research/knowledge/test/popMusic#AlbumTrack\", \"mapping\": [ { \"colId\": \"col_1\", \"colName\": \"AlbumTrack\" } ], \"props\": [ { \"URIRelation\": \"http://com.ge.research/knowledge/test/popMusic#durationInSeconds\", \"mapping\": [ { \"colId\": \"col_8\", \"colName\": \"durationInSec\" } ] }, { \"URIRelation\": \"http://com.ge.research/knowledge/test/popMusic#trackNumber\", \"mapping\": [ { \"colId\": \"col_12\", \"colName\": \"trackNumber\" } ] } ] }, { \"sparqlID\": \"?Song\", \"type\": \"http://com.ge.research/knowledge/test/popMusic#Song\", \"mapping\": [ { \"colId\": \"col_10\", \"colName\": \"song\" } ], \"props\": [ { \"URIRelation\": \"http://com.ge.research/knowledge/test/popMusic#songTitle\", \"mapping\": [ { \"colId\": \"col_11\", \"colName\": \"songTitle\" } ] } ] }, { \"sparqlID\": \"?Artist\", \"type\": \"http://com.ge.research/knowledge/test/popMusic#Artist\", \"mapping\": [ { \"colId\": \"col_3\", \"colName\": \"Artist\" } ], \"props\": [ { \"URIRelation\": \"http://com.ge.research/knowledge/test/popMusic#name\", \"mapping\": [ { \"colId\": \"col_6\", \"colName\": \"artName\" } ] } ] } ] } }";		
	private static JSONObject NG_JSON;
	
	private SimpleResultSet result = null;
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();
		NG_JSON = Utility.getJsonObjectFromString(NG_JSON_STRING);
		nodeGroupStoreClient = IntegrationTestUtility.getNodeGroupStoreRestClient(); // instantiate client, with configurations from properties file
		
		IntegrationTestUtility.cleanupNodegroupStore(nodeGroupStoreClient, CREATOR);

	}
	
	@AfterClass
    public static void teardown() throws Exception {
        // delete stored nodegroup when done with all tests
		IntegrationTestUtility.cleanupNodegroupStore(nodeGroupStoreClient, CREATOR);
    } 
	
	/**
	 * Store a nodegroup
	 */
	@Test
	public void testStoreNodegroup() throws Exception{
		result = nodeGroupStoreClient.executeStoreNodeGroup(ID, COMMENTS, CREATOR, NG_JSON);
		assertTrue(result.getSuccess());		
		nodeGroupStoreClient.deleteStoredNodeGroup(ID);	// delete when done
	}
	
	/**
	 * Ensure fails when sending null nodegroup
	 * (Need try/catch here because fails with exception in client)
	 */
	@Test
	public void testStoreNodegroup_NullNodegroup() throws Exception{
		boolean exceptionThrown = false;
		try{
			nodeGroupStoreClient.executeStoreNodeGroup(ID, COMMENTS, CREATOR, null);			
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot store null nodegroup"));
		}
		assertTrue(exceptionThrown);
	}
	
	
	/**
	 * Ensure fails when sending null/empty id
	 */
	@Test
	public void testStoreNodegroup_NullOrEmptyId() throws Exception{
		// null id
		try {
			result = nodeGroupStoreClient.executeStoreNodeGroup(null, COMMENTS, CREATOR, NG_JSON);
			// failure would be ok
			assertFalse(result.getSuccess());
		} catch (Exception e) {
			// exception would be ok
		}
		
		// empty id
		try {
			result = nodeGroupStoreClient.executeStoreNodeGroup("  ", COMMENTS, CREATOR, NG_JSON);
			// failure would be ok
			assertFalse(result.getSuccess());
		} catch (Exception e) {
			// exception would be ok
		}
	}
	
	
	/**
	 * Ensure fails with null comments...and succeeds with empty comments
	 */
	@Test
	public void test_NullOrEmptyComments() throws Exception{
		// null comments - should fail
		result = nodeGroupStoreClient.executeStoreNodeGroup(ID, null, CREATOR, NG_JSON);
		assertFalse(result.getSuccess());
		assertTrue(result.getRationaleAsString("").contains(" comments "));		

		// empty comments - should succeed
		result = nodeGroupStoreClient.executeStoreNodeGroup(ID, "", CREATOR, NG_JSON);
		assertTrue(result.getSuccess());	
		Table res = nodeGroupStoreClient.executeGetNodeGroupById(ID).getTable();
		assertEquals(res.getNumRows(), 1);
		assertEquals(res.getCell(0,0), ID);
		res = nodeGroupStoreClient.executeGetNodeGroupList().getTable();
		assertTrue(Arrays.asList(res.getColumnUniqueValues("ID")).contains(ID));
		
		// clean up
		nodeGroupStoreClient.deleteStoredNodeGroup(ID);
	}
	
	
	/**
	 * Ensure fails with null creator...and succeeds with empty creator
	 */
	@Test
	public void testStoreNodegroup_NullOrEmptyCreator() throws Exception{
		// null creator
		result = nodeGroupStoreClient.executeStoreNodeGroup(ID, COMMENTS, null, NG_JSON);
		assertFalse(result.getSuccess());
		assertTrue(result.getRationaleAsString("").contains(" creator "));		

		// empty creator
		result = nodeGroupStoreClient.executeStoreNodeGroup(ID, COMMENTS, "", NG_JSON);
		assertTrue(result.getSuccess());
		nodeGroupStoreClient.deleteStoredNodeGroup(ID);
	}
	
	
	/**
	 * Basic test of the getNodeGroupMetadata endpoint.
	 */
	@Test
	public void testGetNodegroupMetadata() throws Exception {
		try {
			TableResultSet res;
			res = nodeGroupStoreClient.executeGetStoredItemsMetadata(NgStore.StoredItemTypes.PrefabNodeGroup);
			int countBefore = res.getResults().getNumRows();
			nodeGroupStoreClient.executeStoreNodeGroup(ID + "a", COMMENTS, CREATOR, NG_JSON);
			nodeGroupStoreClient.executeStoreNodeGroup(ID + "b", COMMENTS, CREATOR, NG_JSON);
			nodeGroupStoreClient.executeStoreNodeGroup(ID + "c", "", "", NG_JSON);
			res = nodeGroupStoreClient.executeGetStoredItemsMetadata(NgStore.StoredItemTypes.PrefabNodeGroup);
			int countAfter = res.getResults().getNumRows();
			assertEquals(countBefore + 3, countAfter);		 // confirm that we get metadata for 3 more nodegroups (imperfect test)
			assertEquals(res.getTable().getNumColumns(),5);  // confirm that there are 4 columns of metadata
			// could add more tests.
		} finally {
			try {
				nodeGroupStoreClient.deleteStoredNodeGroup(ID + "a");
				nodeGroupStoreClient.deleteStoredNodeGroup(ID + "b");
				nodeGroupStoreClient.deleteStoredNodeGroup(ID + "c");
			} catch (Exception e) {}
		}
	}
	
	/**
	 * Ensure that nodegroups are looked up by exact match (e.g. not regex)
	 */
	@Test
	public void test_NodegroupIdExactMatch() throws Exception{
		String ID2 = ID + "2";
		try{
			SimpleResultSet ret = null;
			// store two nodegroups.  The 1st nodegroup id is a subset of the 2nd nodegroup id.
			ret = nodeGroupStoreClient.executeStoreNodeGroup(ID, COMMENTS, CREATOR, NG_JSON);
			ret.throwExceptionIfUnsuccessful();
			ret = nodeGroupStoreClient.executeStoreNodeGroup(ID2, COMMENTS, CREATOR, NG_JSON); 
			ret.throwExceptionIfUnsuccessful();
			
			Table res = nodeGroupStoreClient.executeGetNodeGroupById(ID).getTable();
			assertEquals(1, res.getNumRows());  	// ensure 1 nodegroup returned (we're getting an exact match, not a regex match)
			Table res2 = nodeGroupStoreClient.executeGetNodeGroupById(ID2).getTable();
			assertEquals(1, res2.getNumRows());  	// ensure 1 nodegroup returned
			
			// make sure json is valid
			nodeGroupStoreClient.executeGetNodeGroupByIdToSGJson(ID);
			nodeGroupStoreClient.executeGetNodeGroupByIdToSGJson(ID2);

		}finally{
			// clean up
			nodeGroupStoreClient.deleteStoredNodeGroup(ID);
			nodeGroupStoreClient.deleteStoredNodeGroup(ID2);
		}
	}
	
	@Test
	public void testVeryBigNodegroup() throws Exception {
		try{
			// get very big nodegroup
			JSONObject json = Utility.getJSONObjectFromFilePath("src/test/resources/very-big-nodegroup.json");
			SparqlGraphJson orig = new SparqlGraphJson(json);
			
			// store very big nodegroup
			SimpleResultSet ret = nodeGroupStoreClient.executeStoreNodeGroup(ID, COMMENTS, CREATOR, json);
			ret.throwExceptionIfUnsuccessful();
			
			// retrieve it
			SparqlGraphJson stored = nodeGroupStoreClient.executeGetNodeGroupByIdToSGJson(ID);
			
			assertEquals(orig.getNodeGroup().generateSparqlSelect(), stored.getNodeGroup().generateSparqlSelect());
			assertEquals(orig.getMappingTabJson().toJSONString(),    stored.getMappingTabJson().toJSONString());
			assertEquals(orig.getSparqlConnJson().toJSONString(),    stored.getSparqlConnJson().toJSONString());
		}finally{
			// clean up
			nodeGroupStoreClient.deleteStoredNodeGroup(ID);
		}
	}

	@Test 
	public void testLoadStoreDataCsv() throws Exception {
		IntegrationTestUtility.cleanupNodegroupStore("junit");
		try {
			File tempDir = TestGraph.unzipAndUniquifyJunitGraphs(this, "/store_data.zip");
			
			nodeGroupStoreClient.loadStoreDataCsv(Paths.get(tempDir.getAbsolutePath(), "store_data.csv").toString(), null, new PrintWriter(System.out));
	    	assertEquals(4, IntegrationTestUtility.countItemsInStoreByCreator("junit"));
		} finally {
			try {
				IntegrationTestUtility.cleanupNodegroupStore("junit");
			} catch (Exception e) {}
		}
	}
	
	
}
