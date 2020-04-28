package com.ge.research.semtk.querygen.timeseries.kairosdb.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import org.apache.commons.lang.ArrayUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import com.ge.research.semtk.querygen.QueryList;
import com.ge.research.semtk.querygen.timeseries.kairosdb.KairosDBQueryGenerator;
import com.ge.research.semtk.resultSet.Table;


public class KairosDBQueryGeneratorTest {

	private final String[] COLS = (String[]) ArrayUtils.addAll(KairosDBQueryGenerator.REQUIRED_COLS, KairosDBQueryGenerator.OPTIONAL_COLS);
	private final String[] COL_TYPES_4 = {"String","String","String","String"};
	private final String[] COL_TYPES_3 = {"String","String","String"};

	private final String KAIROS_CONNECTION_URL_1 = "http://kairos1:8080";
	private final String KAIROS_CONNECTION_URL_2 = "http://kairos2:8080"; 

	@Test
	public void testWithPrefixes() throws Exception {

		ArrayList<ArrayList<String>> rows;
		UUID uuid1 = UUID.randomUUID();
		UUID uuid2 = UUID.randomUUID();
		Table locationAndValueInfo;
		KairosDBQueryGenerator g;
		HashMap<UUID, Object> queriesHash;
		QueryList queriesForUUID1;
		QueryList queriesForUUID2;

		// create a table
		rows = new ArrayList<ArrayList<String>>();
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),KAIROS_CONNECTION_URL_1,"SPEED1","build1a")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),KAIROS_CONNECTION_URL_1,"SPEED2","build1b")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid2.toString(),KAIROS_CONNECTION_URL_2,"TEMPERATURE","build2a")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid2.toString(),KAIROS_CONNECTION_URL_2,"PRESSURE","build2b")));
		locationAndValueInfo = new Table(COLS, COL_TYPES_4, rows);

		g = new KairosDBQueryGenerator(locationAndValueInfo, null);

		queriesHash = g.getQueries();  // each Object is an QueryList
		assertEquals(queriesHash.size(),2);			
		queriesForUUID1 = (QueryList)queriesHash.get(uuid1);
		queriesForUUID2 = (QueryList)queriesHash.get(uuid2);
		assertEquals(queriesForUUID1.getQueries().size(), 1);
		assertEquals(queriesForUUID2.getQueries().size(), 1);
		
		// check for a query like this, but can't assume order: "{\"start_relative\":{\"value\":10,\"unit\":\"YEARS\"},\"cacheTime\":0,\"metrics\":[{\"name\":\"build1a.SPEED1\",\"tags\":{},\"group_by\":[],\"aggregators\":[]},{\"name\":\"build1b.SPEED2\",\"tags\":{},\"group_by\":[],\"aggregators\":[]}]}	
		assertTrue(queriesForUUID1.getQueries().get(0).getQuery().contains("\"start_relative\":{\"value\":10,\"unit\":\"YEARS\"}"));
		assertTrue(queriesForUUID1.getQueries().get(0).getQuery().contains("{\"name\":\"build1a.SPEED1\",\"tags\":{},\"group_by\":[],\"aggregators\":[]}"));
		assertTrue(queriesForUUID1.getQueries().get(0).getQuery().contains("{\"name\":\"build1b.SPEED2\",\"tags\":{},\"group_by\":[],\"aggregators\":[]}"));
		assertEquals(queriesForUUID1.getConfig().get("kairosDBUrl"), KAIROS_CONNECTION_URL_1);
		assertTrue(queriesForUUID2.getQueries().get(0).getQuery().contains("\"start_relative\":{\"value\":10,\"unit\":\"YEARS\"}"));
		assertTrue(queriesForUUID2.getQueries().get(0).getQuery().contains("{\"name\":\"build2a.TEMPERATURE\",\"tags\":{},\"group_by\":[],\"aggregators\":[]}"));
		assertTrue(queriesForUUID2.getQueries().get(0).getQuery().contains("{\"name\":\"build2b.PRESSURE\",\"tags\":{},\"group_by\":[],\"aggregators\":[]}"));
		assertEquals(queriesForUUID2.getConfig().get("kairosDBUrl"), KAIROS_CONNECTION_URL_2);
	}
	
	
	@Test
	public void testWithoutPrefixes() throws Exception {

		ArrayList<ArrayList<String>> rows;
		UUID uuid1 = UUID.randomUUID();
		UUID uuid2 = UUID.randomUUID();
		Table locationAndValueInfo;
		KairosDBQueryGenerator g;
		HashMap<UUID, Object> queriesHash;
		QueryList queriesForUUID1;
		QueryList queriesForUUID2;

		// create a table
		rows = new ArrayList<ArrayList<String>>();
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),KAIROS_CONNECTION_URL_1,"SPEED1")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),KAIROS_CONNECTION_URL_1,"SPEED2")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid2.toString(),KAIROS_CONNECTION_URL_2,"TEMPERATURE")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid2.toString(),KAIROS_CONNECTION_URL_2,"PRESSURE")));
		locationAndValueInfo = new Table(KairosDBQueryGenerator.REQUIRED_COLS, COL_TYPES_3, rows);

		g = new KairosDBQueryGenerator(locationAndValueInfo, null);

		queriesHash = g.getQueries();  // each Object is an QueryList
		assertEquals(queriesHash.size(),2);			
		queriesForUUID1 = (QueryList)queriesHash.get(uuid1);
		queriesForUUID2 = (QueryList)queriesHash.get(uuid2);
		assertEquals(queriesForUUID1.getQueries().size(), 1);
		assertEquals(queriesForUUID2.getQueries().size(), 1);
		
		// check for a query like this, but can't assume order: {\"start_relative\":{\"value\":10,\"unit\":\"YEARS\"},\"cacheTime\":0,\"metrics\":[{\"name\":\"TEMPERATURE\",\"tags\":{},\"group_by\":[],\"aggregators\":[]},{\"name\":\"PRESSURE\",\"tags\":{},\"group_by\":[],\"aggregators\":[]}]}		 
		assertTrue(queriesForUUID1.getQueries().get(0).getQuery().contains("\"start_relative\":{\"value\":10,\"unit\":\"YEARS\"}"));
		assertTrue(queriesForUUID1.getQueries().get(0).getQuery().contains("{\"name\":\"SPEED1\",\"tags\":{},\"group_by\":[],\"aggregators\":[]}"));
		assertTrue(queriesForUUID1.getQueries().get(0).getQuery().contains("{\"name\":\"SPEED2\",\"tags\":{},\"group_by\":[],\"aggregators\":[]}"));
		assertEquals(queriesForUUID1.getConfig().get("kairosDBUrl"), KAIROS_CONNECTION_URL_1);
		assertTrue(queriesForUUID2.getQueries().get(0).getQuery().contains("\"start_relative\":{\"value\":10,\"unit\":\"YEARS\"}"));
		assertTrue(queriesForUUID2.getQueries().get(0).getQuery().contains("{\"name\":\"TEMPERATURE\",\"tags\":{},\"group_by\":[],\"aggregators\":[]}"));
		assertTrue(queriesForUUID2.getQueries().get(0).getQuery().contains("{\"name\":\"PRESSURE\",\"tags\":{},\"group_by\":[],\"aggregators\":[]}"));
		assertEquals(queriesForUUID2.getConfig().get("kairosDBUrl"), KAIROS_CONNECTION_URL_2);
	}
	
	@Test
	public void testFailsIfSameUUIDUsesDifferentDatabases() throws Exception{
		
		ArrayList<ArrayList<String>> rows;
		UUID uuid1 = UUID.randomUUID();
		Table locationAndValueInfo;
		KairosDBQueryGenerator g;

		// create a table - single UUID mapping to different Kairos instances
		rows = new ArrayList<ArrayList<String>>();
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),KAIROS_CONNECTION_URL_1,"TEMPERATURE","build1")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),KAIROS_CONNECTION_URL_2,"PRESSURE","build1")));
		locationAndValueInfo = new Table(COLS, COL_TYPES_4, rows);

		boolean exceptionThrown = false;
		try{
			g = new KairosDBQueryGenerator(locationAndValueInfo, null);
			g.getQueries();  
			fail(); // should not get here
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot support querying more than one KairosDB database for a single UUID"));
		}
		assertTrue(exceptionThrown);		
	}
	
	
	@Test
	public void testGetUnsupportedMessageForConstraints() throws Exception{
		
		ArrayList<ArrayList<String>> rows;
		UUID uuid1 = UUID.randomUUID();
		Table locationAndValueInfo;
		KairosDBQueryGenerator g;

		// create a table - single UUID mapping to different Kairos instances
		rows = new ArrayList<ArrayList<String>>();
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),KAIROS_CONNECTION_URL_1,"TEMPERATURE","build1")));
		rows.add(new ArrayList<String>(Arrays.asList(uuid1.toString(),KAIROS_CONNECTION_URL_1,"PRESSURE","build1")));
		locationAndValueInfo = new Table(COLS, COL_TYPES_4, rows);
		
		// random pasted constraints from elsewhere - don't expect these to work when supported
		String constraintJsonStr = "{\"@constraintSet\":{\"@op\":\"AND\",\"@constraints\":[{\"@var\":\"SPEED1\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"80\",\"@type\":\"number\"}},{\"@var\":\"SPEED2\",\"@operator1\":\">\",\"@value1\":{\"@value\":\"40000\",\"@type\":\"number\"},\"@operator2\":\"<\",\"@value2\":{\"@value\":\"70000\",\"@type\":\"number\"}}]},\"@timeConstraint\":{\"@var\":\"_Time_\",\"@operator1\":\">=\",\"@value1\":{\"@value\":\"10/08/2014 10:00:00 AM\",\"@type\":\"datetime\"},\"@operator2\":\"<=\",\"@value2\":{\"@value\":\"10/08/2014 11:00:00 AM\",\"@type\":\"datetime\"}}}";
		JSONObject constraintJson = (JSONObject) (new JSONParser()).parse(constraintJsonStr);
		
		boolean exceptionThrown = false;
		try{
			g = new KairosDBQueryGenerator(locationAndValueInfo, constraintJson);
			g.getQueries();  
			fail(); // should not get here
		}catch(Exception e){
			exceptionThrown = true;
			assertTrue(e.getMessage().contains("Cannot yet support constraints in KairosDB queries"));
		}
		assertTrue(exceptionThrown);

	}

}
