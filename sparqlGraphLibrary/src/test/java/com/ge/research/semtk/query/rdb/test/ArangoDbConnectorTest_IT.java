/**
 ** Copyright 2020 General Electric Company
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

package com.ge.research.semtk.query.rdb.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.arangodb.ArangoDB;
import com.arangodb.entity.BaseDocument;
import com.ge.research.semtk.query.rdb.ArangoDbConnector;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.utility.Utility;

public class ArangoDbConnectorTest_IT {

	private static String HOST;
	private static int PORT;
	private static String USER;  
	private static String PASSWORD;	
	private static String DATABASE;
	
	private static ArangoDB arangoDB;
	private static ArangoDbConnector arangoDbConnector;
	
	
	/**
	 * Setup: create a test database in ArangoDB
	 */
	@BeforeClass
	public static void setup() throws Exception {

		HOST = IntegrationTestUtility.get("arangodb.server");
		
		// if needed variables are not configured, then skip test(s)
		Assume.assumeTrue("No ArangoDB server configured in environment", HOST != null && !HOST.trim().isEmpty());  // if false, will skip rest of setup and tests, but still run cleanup
		
		IntegrationTestUtility.authenticateJunit();	
		PORT = IntegrationTestUtility.getInt("arangodb.port");
		USER = IntegrationTestUtility.get("arangodb.username");
		PASSWORD = IntegrationTestUtility.get("arangodb.password");
	
		// set up test database
		DATABASE = "JUnitTemporaryDatabase" + UUID.randomUUID().toString().replaceAll("-",""); 
		arangoDB = new ArangoDB.Builder().host(HOST, PORT).user(USER).password(PASSWORD).build();		
		arangoDB.createDatabase(DATABASE);
		System.out.println("Created database " + DATABASE);
		
		// ArangoDbConnector is the object we're testing
		arangoDbConnector = new ArangoDbConnector(HOST, PORT, DATABASE, USER, PASSWORD);
	}
	
	
	/**
	 * Delete the test database after tests are complete
	 */
	@AfterClass
	public static void cleanup() throws Exception {
		if(DATABASE != null && DATABASE.startsWith("JUnitTemporaryDatabase")){	// small safety net in case someone is temporarily experimenting on a real database
			arangoDB.db(DATABASE).drop();	// delete the database
		}
		System.out.println("Deleted database " + DATABASE);
	}


	/**
	 * Basic test
	 */
	@Test
	public void test() throws Exception {		
		String collection = createNewCollection();
		
		// insert data
		int NUM_DOCS = 200;
		for(int i = 0; i < NUM_DOCS; i++){
			BaseDocument doc = new BaseDocument();
			doc.setKey("key" + i);
			doc.addAttribute("a", "Foo" + i);
			doc.addAttribute("b", 0 + i);
			arangoDB.db(DATABASE).collection(collection).insertDocument(doc);
		}

		// query for data
		String query = "FOR document IN " + collection + " RETURN { a: document.a, b: document.b }";		
		Table results = arangoDbConnector.query(query);
		assertEquals(results.getRows().size(), NUM_DOCS);
		assertTrue(results.getRows().get(0).get(results.getColumnIndex("a")).startsWith("Foo"));
	}	
	
	/**
	 * Test where items in the table are JSON objects
	 */
	@Test
	public void testJsonValue() throws Exception {		
		String collection = createNewCollection();
		
		// insert data
		BaseDocument doc = new BaseDocument();
		doc.setKey("key");
		doc.addAttribute("a", "{\"grammar book\": { \"title\": \"a book about grammar\", \"subject\": \"grammar\" }}");
		doc.addAttribute("b", "{\"vocab book\": { \"title\": \"a book about vocabulary\", \"subject\": \"vocabulary\" }}");
		arangoDB.db(DATABASE).collection(collection).insertDocument(doc);

		// query for data
		String query = "FOR document IN " + collection + " RETURN { a: document.a, b: document.b }";		
		Table results = arangoDbConnector.query(query);
		JSONObject o = (JSONObject)(Utility.getJsonObjectFromString(results.getRows().get(0).get(results.getColumnIndex("a"))).get("grammar book"));
		assertTrue(o.get("subject").equals("grammar"));
		assertTrue(o.get("title").equals("a book about grammar"));
	}

	
	/**
	 * Test what happens when the query is requesting properties that are not present in all documents.
	 */
	@Test
	public void testMissingProperty() throws Exception {		
		String collection = createNewCollection();

		// insert test data: some documents have properties "a" and "b", some have "a" and "c"
		int NUM_DOCS = 3;
		for(int i = 0; i < NUM_DOCS; i++){
			BaseDocument doc = new BaseDocument();
			doc.setKey("key" + i);
			doc.addAttribute("a", "Foo" + i);
			doc.addAttribute("b", 0 + i);
			arangoDB.db(DATABASE).collection(collection).insertDocument(doc);
		}
		BaseDocument doc = new BaseDocument();
		doc.setKey("key");
		doc.addAttribute("a", "Foo");
		doc.addAttribute("c", 5);
		arangoDB.db(DATABASE).collection(collection).insertDocument(doc);

		// AQL query returns this
		// BaseDocument [documentRevision=null, documentHandle=null, documentKey=null, properties={a=Foo0, b=0}]
		// BaseDocument [documentRevision=null, documentHandle=null, documentKey=null, properties={a=Foo1, b=1}]
		// BaseDocument [documentRevision=null, documentHandle=null, documentKey=null, properties={a=Foo2, b=2}]
		// BaseDocument [documentRevision=null, documentHandle=null, documentKey=null, properties={a=Foo, b=null}]
		
		// query for data
		String query = "FOR document IN " + collection + " RETURN { a: document.a, b: document.b }";		
		Table results = arangoDbConnector.query(query);
		assertEquals(results.getNumRows(), 4);
		assertEquals(results.getSubsetWhereMatches("a", "Foo0").getRow(0).get(results.getColumnIndex("b")),"0");
		assertEquals(results.getSubsetWhereMatches("a", "Foo1").getRow(0).get(results.getColumnIndex("b")),"1");
		assertEquals(results.getSubsetWhereMatches("a", "Foo2").getRow(0).get(results.getColumnIndex("b")),"2");
		assertEquals(results.getSubsetWhereMatches("a", "Foo").getRow(0).get(results.getColumnIndex("b")),"null");	
	}	

	
	/**
	 * Test that we get informative failure messages
	 */
	@Test
	public void testFailures() throws Exception {
		boolean thrown;
		
		// test bad host
		thrown = false;
		try {
			new ArangoDbConnector("somehost", PORT, DATABASE, USER, PASSWORD).query("");
		} catch (Exception e) {
			thrown = true;
			assert(e.getMessage().contains("java.net.UnknownHostException: somehost")); 
		}
		assertTrue(thrown);	
		
		// test bad port
		thrown = false;
		try {
			new ArangoDbConnector(HOST, 8888, DATABASE, USER, PASSWORD).query("");
		} catch (Exception e) {
			thrown = true;
			assert(e.getMessage().contains("ConnectException"));
		}
		assertTrue(thrown);	
		
		// test bad database
		thrown = false;
		try {
			new ArangoDbConnector(HOST, PORT, "somedatabase", USER, PASSWORD).query("");
		} catch (Exception e) {
			thrown = true;
			assert(e.getMessage().contains("database not found")); 	
		}
		assertTrue(thrown);	
		
		// test bad user
		thrown = false;
		try {
			new ArangoDbConnector(HOST, PORT, DATABASE, "someuser", PASSWORD).query("");
		} catch (Exception e) {
			thrown = true;
			assert(e.getMessage().contains("unauthorized"));
		}
		assertTrue(thrown);	
		
		// test bad password
		thrown = false;
		try {
			new ArangoDbConnector(HOST, PORT, DATABASE, USER, "somepassword").query("");
		} catch (Exception e) {
			thrown = true;
			assert(e.getMessage().contains("unauthorized"));
		}
		assertTrue(thrown);
		
		// test bad query
		thrown = false;
		try {			
			new ArangoDbConnector(HOST, PORT, DATABASE, USER, PASSWORD).query("this is a bad query");
		} catch (Exception e) {
			thrown = true;
			assert(e.getMessage().contains("AQL: syntax error, unexpected identifier near 'this is a bad query' at position 1:1")); 
		}
		assertTrue(thrown);	
		
		// test bad collection
		thrown = false;
		try {			
			new ArangoDbConnector(HOST, PORT, DATABASE, USER, PASSWORD).query("FOR document IN SomeCollection RETURN { k: document.key }");
		} catch (Exception e) {
			thrown = true;
			assert(e.getMessage().contains("AQL: collection or view not found: SomeCollection")); 
		}
		assertTrue(thrown);	
	}	
	
	/**
	 * Create a temporary collection.
	 * NOTE: it will later get deleted with the entire test database
	 */
	private String createNewCollection(){
		String collectionName = "JUnitCollection" + UUID.randomUUID().toString().replaceAll("-","");
		arangoDB.db(DATABASE).createCollection(collectionName); 
		System.out.println("Created collection " + collectionName);
		return collectionName;
	}
}
