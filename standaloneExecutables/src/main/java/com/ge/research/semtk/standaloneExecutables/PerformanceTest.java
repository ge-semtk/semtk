package com.ge.research.semtk.standaloneExecutables;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.aws.S3Connector;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.belmont.XSDSupportedType;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.NeptuneSparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;
import com.ge.research.semtk.utility.Utility;

public class PerformanceTest {

	private static String resourceFolder;
	private static String taskName;
	private static long startTime;
	private static SparqlEndpointInterface sei;
	private final static boolean NO_PRECHECK = false;
	private final static boolean PRECHECK = true;
	private final static boolean LOG_QUERY_PERFORMANCE = false;
	private final static Double  CIRCUIT_BREAKER_SEC = 60.0 * 3;

	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		if (args.length != 3) {
			throw new Exception("Usage: PerformanceTest server_type server_url sglib_src_test_resources");
		}
		
		String graph = "http://performance_test_0";
		sei = SparqlEndpointInterface.getInstance(args[0], args[1], graph, "dba", "dba");
		sei.setLogPerformance(LOG_QUERY_PERFORMANCE);
		log("graph: " + graph);
		resourceFolder = args[2];
		if (!Files.exists(Paths.get(resourceFolder))) {
			throw new Exception("Resource folder doesn't exist: " + resourceFolder);
		}
		
		log("clearing graph");
		sei.clearGraph();
		
		try {
			
			linkItems(60000, 100000);  // 1/100th grammatech
			//addBatteryDescriptions(40000, 75000);  
			
			
			
			//addSimpleRows(10, 10000); 
			//addBatteryDescriptions(1000, 500000);  
			//addBatteryDescriptionsVaryingThreadsAndSize(0);
			//addSimpleBiggerRows(10, 50000);
			
		} finally {
			sei.clearGraph();
			log("cleaned up");
		}
		
		log("fine.");
	}
	
	/**
	 * Import rows and count
	 * @param passes - how many passes
	 * @param pass_size - rows per pass
	 * @throws Exception
	 */
	private static void addSimpleRows(int passes, int pass_size) throws Exception {
		
		// setup
		sei.clearGraph();
		uploadOwlFromSGL(sei, "/loadTest.owl");
		
		int pass = -1;
		long triples = 0;
		
		while (++pass < passes) {
			// build 10,000 rows
			int i = 0;
			
			// reload the sgJson so ImportSpec can't share between loads, cheat, or get bogged down.
			SparqlGraphJson sgJson = getSGJsonFromSGL("/loadTest.json", sei, sei);
			
			// build the rows
			StringBuilder content = new StringBuilder();
			content.append("battery name, cell id, color\n");
			for (i=0; i < pass_size; i++) {
				int index = pass * pass_size + i;
				content.append("name_" + index + ",cell_" + index + ",red\n");
			}
			
			Dataset ds0 = new CSVDataset(content.toString(), true);
	
			// ingest
			DataLoader dl0 = new DataLoader(sgJson, ds0, "dba", "dba");
			startTask("addSimpleRows load " + pass_size + "  totaling," + (pass + 1) * pass_size);
			dl0.importData(NO_PRECHECK);
			endTask();
			
			// get new total triples twice
			triples = sei.executeQueryToTable(SparqlToXUtils.generateCountTriplesSparql(sei)).getCellAsLong(0, 0);
			startTask("addSimpleRows count, " + triples);
			sei.executeQueryToTable(SparqlToXUtils.generateCountTriplesSparql(sei)).getCellAsLong(0, 0);
			endTask();
		}

	}
	
	private static void addSimpleBiggerRows(int passes, int pass_size) throws Exception {
		
		// setup
		sei.clearGraph();
		uploadOwlFromSGL(sei, "/loadTestDuraBattery.owl");
		
		int pass = -1;
		long triples = 0;
		
		while (++pass < passes) {
			// build 10,000 rows
			int i = 0;

			// reload the sgJson so ImportSpec can't share between loads, cheat, or get bogged down.
			SparqlGraphJson sgJson = getSGJsonFromSGL("/loadTestDuraBattery.json", sei, sei);
			
			StringBuilder content = new StringBuilder();
			content.append("batt_id,description_opt,cell1_id_opt,cell1_color,cell2_id_opt,cell2_color,cell3_id_opt,cell3_color,cell4_id_opt,cell4_color\n");
			for (i=0; i < pass_size; i++) {
				int index = pass * pass_size + i;
				content.append("id" + index + 
						",desc_" + index + 
						",cell1_" + index + 
						",red" +
						",cell2_" + index + 
						",red" +
						",cell3_" + index + 
						",blue" +
						",cell4_" + index + 
						",white" +
						"\n"
						);
			}
			
			Dataset ds0 = new CSVDataset(content.toString(), true);
	
			DataLoader dl0 = new DataLoader(sgJson, ds0, "dba", "dba");
			startTask("addSimpleBiggerRows load " + pass_size + "  totaling," + (pass + 1) * pass_size);
			dl0.importData(PRECHECK);
			endTask();
			
			// get new total triples twice
			triples = sei.executeQueryToTable(SparqlToXUtils.generateCountTriplesSparql(sei)).getCellAsLong(0, 0);
			startTask("addSimpleBiggerRows count triples, " + triples);
			sei.executeQueryToTable(SparqlToXUtils.generateCountTriplesSparql(sei)).getCellAsLong(0, 0);
			endTask();
		}

	}
	
	/**
	 
	 * @param passes
	 * @throws Exception
	 */
	private static void addBatteryDescriptions(int rows_per_pass, int max_triples) throws Exception {
		
		// setup loads
		SparqlGraphJson sgJson1 = getSGJsonFromSGL("/loadTestDuraBattery.json", sei, sei);
		SparqlGraphJson sgJson2 = getSGJsonFromSGL("/lookupBatteryIdAddDesc.json", sei, sei);
		SparqlGraphJson sgJson3 = getSGJsonFromSGL("/lookupSuperclassIdAddDesc.json", sei, sei);
		
		// set up a query
		OntologyInfo oInfo = new OntologyInfo(new SparqlConnection("Perftest", sei));
		NodeGroup ng2 = sgJson2.getNodeGroupNoInflateNorValidate(oInfo);
		int limit = rows_per_pass;
		ng2.setLimit(limit);
		
		sei.clearGraph();
		uploadOwlFromSGL(sei, "/loadTestDuraBattery.owl");
		long triples = 0;

		int pass = -1;
		int index = 0;
		Double lastSec[] = new Double[20];
		for (int i=0; i < 20; i++) {
			lastSec[i]=0.0;
		}
		
		while (triples < max_triples) {
			++pass;
			int total_rows = (pass + 1) * rows_per_pass;
			
			// reload the sgJson so ImportSpec can't share between loads, cheat, or get bogged down.
			sgJson1 = getSGJsonFromSGL("/loadTestDuraBattery.json", sei, sei);
			sgJson2 = getSGJsonFromSGL("/lookupBatteryIdAddDesc.json", sei, sei);
			sgJson3 = getSGJsonFromSGL("/lookupSuperclassIdAddDesc.json", sei, sei);
			
			// build some rows
			StringBuilder content1 = new StringBuilder();
			StringBuilder content2 = new StringBuilder();
			StringBuilder content3 = new StringBuilder();

			content1.append("description_opt, batt_ID\n");
			content2.append("description, batt_ID\n");
			content3.append("description, batt_ID\n");
			for (int i=0; i < rows_per_pass; i++) {
				index++;
				content1.append(",id_" + index + "\n");
				// send half the ingest data to each of 2 and 3
				if (i % 2 == 0) {
					content2.append("description_" + index + ",id_" + index + "\n");
				} else {
					content3.append("description_" + index + ",id_" + index + "\n");
				}
			}
			
			// ingest rows
			if (lastSec[0] < CIRCUIT_BREAKER_SEC) {
				Dataset ds1 = new CSVDataset(content1.toString(), true);
				DataLoader dl1 = new DataLoader(sgJson1, ds1, "dba", "dba");
				startTask("addBatteryDescriptions load simple, rows, " + rows_per_pass + ",total rows," + total_rows);
				dl1.importData(PRECHECK);
				lastSec[0] = endTask();
			}
			
			// add descriptions
			if (lastSec[1] < CIRCUIT_BREAKER_SEC) {
				Dataset ds2 = new CSVDataset(content2.toString(), true);
				DataLoader dl2 = new DataLoader(sgJson2, ds2, "dba", "dba");
				dl2.setLogPerformance(LOG_QUERY_PERFORMANCE);
				startTask("addBatteryDescriptions load lookup class, rows," + rows_per_pass/2 + ",total rows," + total_rows);
				dl2.importData(PRECHECK);
				lastSec[1] = endTask();
			}
			
			// add descriptions superclass
			if (lastSec[2] < CIRCUIT_BREAKER_SEC) {
				Dataset ds3 = new CSVDataset(content3.toString(), true);
				DataLoader dl3 = new DataLoader(sgJson3, ds3, "dba", "dba");
				dl3.setLogPerformance(LOG_QUERY_PERFORMANCE);
				startTask("addBatteryDescriptions load lookup superclass, rows," + rows_per_pass /2 + ",total rows," + total_rows);
				dl3.importData(PRECHECK);
				lastSec[2] = endTask();
			}
			
			// get new total triples
			triples = countTriples("addBatteryDescriptions", total_rows);
			
			// create list of predicates
			int SIZE = 10;
			StringBuilder regex = new StringBuilder();
			ArrayList<String> idList = new ArrayList<String>();
			for (int i=index; i > index-SIZE; i--) {
				idList.add("id_" + i);
				regex.append( (i == index) ? "(" : "|");
				regex.append("id_" + i);
			}
			regex.append(")");
			
			Table t;
			
			// Select filter in
			if (lastSec[3] < CIRCUIT_BREAKER_SEC) {
				startTask("addBatteryDescription select filter in 10, triples," + triples + ",total rows," + total_rows);
				t = sei.executeQueryToTable(SparqlToXUtils.generateSelectSPOSparql(sei, ValueConstraint.buildFilterInConstraint("?p", idList, XSDSupportedType.STRING, sei)));
				assert(t.getNumRows() == SIZE);
				lastSec[3] = endTask();
			}
			
			// Select regex
			if (lastSec[4] < CIRCUIT_BREAKER_SEC) {
				startTask("addBatteryDescription select filter regex 10, triples," + triples + ",total rows," + total_rows);
				t = sei.executeQueryToTable(SparqlToXUtils.generateSelectSPOSparql(sei, ValueConstraint.buildRegexConstraint("?p", regex.toString(), XSDSupportedType.STRING)));
				assert(t.getNumRows() == SIZE);
				lastSec[4] = endTask();
			}
			
			// Select values
			if (lastSec[5] < CIRCUIT_BREAKER_SEC) {
				startTask("addBatteryDescription select values 10, triples," + triples + ",total rows," + total_rows);
				t =sei.executeQueryToTable(SparqlToXUtils.generateSelectSPOSparql(sei, ValueConstraint.buildValuesConstraint("?p", idList, XSDSupportedType.STRING, sei)));
				assert(t.getNumRows() == SIZE);
				lastSec[5] = endTask();
			}
			
			// Select from subclass
			if (lastSec[6] < CIRCUIT_BREAKER_SEC) {
				startTask("addBatteryDescription select subclassOf* 10, triples," + triples + ",total rows," + total_rows);
				NodeGroup ng = sgJson3.getNodeGroup();
				PropertyItem item = ng.getPropertyItemBySparqlID("?batteryId");
				item.setValueConstraint(new ValueConstraint(ValueConstraint.buildValuesConstraint("?batteryId", idList, XSDSupportedType.STRING, sei)));
				t = sei.executeQueryToTable(ng.generateSparqlSelect());
				assert(t.getNumRows() == SIZE);
				lastSec[6] = endTask();
			}

		}

	}
	
	
	/**
	 
	 * @param passes
	 * @throws Exception
	 */
	private static void linkItems(int numItems, int numLinks) throws Exception {
		final int MAX_THREADS = 9;
		System.out.println("max threads " + MAX_THREADS);
		
		// setup loads
		SparqlGraphJson sgJsonItemLoad = getSGJsonResource("/itemLoad.json", sei, sei);
		SparqlGraphJson sgJsonItemLoadLinks = getSGJsonResource("/itemLoadLinks.json", sei, sei);

		// set up a query
		OntologyInfo oInfo = new OntologyInfo(new SparqlConnection("Perftest", sei));
		NodeGroup ngItemLoad = sgJsonItemLoad.getNodeGroupNoInflateNorValidate(oInfo);
		NodeGroup ngItemLoadLinks = sgJsonItemLoadLinks.getNodeGroupNoInflateNorValidate(oInfo);
		
		sei.clearGraph();
		uploadOwlResource(sei, "/item.owl");
		
		// build items
		StringBuilder content = new StringBuilder();
		content.append("itemId\n");
		for (int i=0; i < numItems; i++) {
			content.append("id_" + i + "\n");
		}
		
		// load items
		Dataset ds = new CSVDataset(content.toString(), true);
		DataLoader loader = new DataLoader(sgJsonItemLoad, ds, "dba", "dba");
		loader.overrideMaxThreads(MAX_THREADS);
		startTask("linkItems load items: " + numItems);
		loader.importData(true);
		endTask();
		
		
		// build links
		content = new StringBuilder();
		content.append("itemIdFrom, itemIdTo\n");
		int linksBuilt = 0;
		for (int i=0; i < numItems-1 && linksBuilt < numLinks; i++) {
			for (int j=i+1; j < numItems && linksBuilt < numLinks; j++) {
				content.append("id_" + i + ", id_" + j + "\n");
				linksBuilt ++;
			}
		}

		// load links
		ds = new CSVDataset(content.toString(), true);
		loader = new DataLoader(sgJsonItemLoadLinks, ds, "dba", "dba");
		loader.overrideMaxThreads(MAX_THREADS);
		startTask("linkItems load links: " + linksBuilt);
		loader.importData(true);
		endTask();
		
	}
	
	private static long countTriples(String name, int total_rows) throws Exception {
		startTask(name + " count triples, rows," + total_rows);
		long triples = sei.executeQueryToTable(SparqlToXUtils.generateCountTriplesSparql(sei)).getCellAsLong(0, 0);
		endTask();
		return triples;
	}
	
	/**
	 * Muck with different number of threads and ideal query sizes
	 * @param passes
	 * @throws Exception
	 */
	private static void addBatteryDescriptionsVaryingThreadsAndSize(int pass_size) throws Exception {
		
		// setup
		
		sei.clearGraph();
		uploadOwlFromSGL(sei, "/loadTestDuraBattery.owl");
		long triples = 0;

		int pass = -1;
		
		for (int threads = 1; threads < 10; threads += 1) {
			for (int querySize = 500; querySize < 10000; querySize += 500) {
				
				// reload sgJson objects so it ImportSpec can't cheat or get bogged down between loads
				SparqlGraphJson sgJson1 = getSGJsonFromSGL("/loadTestDuraBattery.json", sei, sei);
				SparqlGraphJson sgJson2 = getSGJsonFromSGL("/lookupBatteryIdAddDesc.json", sei, sei);
				
				// build 10,000 rows
				int i = 0;
				StringBuilder content1 = new StringBuilder();
				StringBuilder content2 = new StringBuilder();
	
				content1.append("description_opt, batt_ID\n");
				content2.append("description, batt_ID\n");
				for (i=0; i < pass_size; i++) {
					int index = pass * pass_size + i;
					content1.append(",id_" + index + "\n");
					content2.append("description_" + index + ",id_" + index + "\n");
				}
				
				// create ids
				Dataset ds1 = new CSVDataset(content1.toString(), true);
				DataLoader dl1 = new DataLoader(sgJson1, 8, ds1, "dba", "dba");
				dl1.overrideMaxThreads(threads);
				dl1.overrideInsertQueryIdealSize(querySize);
				startTask("addBatteryDescriptionsVaryingThreadsAndSize load simple " + pass_size + " total," + threads + "," + querySize + "," + (pass + 1) * pass_size);
				dl1.importData(NO_PRECHECK);
				endTask();
				
				// add descriptions
				Dataset ds2 = new CSVDataset(content2.toString(), true);
				DataLoader dl2 = new DataLoader(sgJson2, 8, ds2, "dba", "dba");
				dl1.overrideMaxThreads(threads);
				dl1.overrideInsertQueryIdealSize(querySize);
				startTask("addBatteryDescriptionsVaryingThreadsAndSize load lookup " + pass_size + "total," + threads + "," + querySize + "," + (pass + 1) * pass_size);
				dl2.importData(NO_PRECHECK);
				endTask();
				
			}

		}

	}

	private static void log(String s) {
		System.err.println(s);
	}
	
	private static void startTask(String name) {
		taskName = name;
		startTime = System.nanoTime();
	}
	
	private static Double endTask() {
		long elapsed = System.nanoTime() - startTime;
		Double seconds = elapsed / 1000000000.0;
		
		System.out.println("task, " + taskName + ", " + Double.toString(seconds));
		
		return seconds;
	}
	
	public static void uploadOwlFromSGL(SparqlEndpointInterface sei, String filename) throws Exception {
		File f = Paths.get(resourceFolder + "/" + filename).toFile();
		byte [] owl =  FileUtils.readFileToByteArray(f);
		sei.executeAuthUpload(owl);
	}
	
	public static void uploadOwlResource(SparqlEndpointInterface sei, String filename) throws Exception {
		byte [] owl = Utility.getResourceAsBytes(PerformanceTest.class, filename);
		sei.executeAuthUpload(owl);
	}
	
	public static SparqlGraphJson getSGJsonFromSGL(String jsonFilename, SparqlEndpointInterface modelSei, SparqlEndpointInterface dataSei) throws Exception {	
		InputStream is = Files.newInputStream(Paths.get(resourceFolder,jsonFilename));
		InputStreamReader reader = new InputStreamReader(is);
		JSONObject jObj = (JSONObject) (new JSONParser()).parse(reader);	
		return buildSGJSON(jObj, modelSei, dataSei);

		
		
	}	
	
	public static SparqlGraphJson getSGJsonResource(String jsonFilename, SparqlEndpointInterface modelSei, SparqlEndpointInterface dataSei) throws Exception {	
		JSONObject jObj = Utility.getResourceAsJson(PerformanceTest.class, jsonFilename);
		return buildSGJSON(jObj, modelSei, dataSei);
	}	
	
	public static SparqlGraphJson buildSGJSON(JSONObject jObj, SparqlEndpointInterface modelSei, SparqlEndpointInterface dataSei) throws Exception {		
		
		SparqlGraphJson ret = new SparqlGraphJson(jObj);
		
		SparqlConnection conn = ret.getSparqlConn();
		conn.clearDataInterfaces();
		conn.addDataInterface(dataSei);
		conn.clearModelInterfaces();
		conn.addModelInterface(modelSei);
		ret.setSparqlConn(conn);
		
		return ret;
	}	

}
