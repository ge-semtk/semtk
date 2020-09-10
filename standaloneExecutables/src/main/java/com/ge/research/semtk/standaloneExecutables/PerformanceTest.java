package com.ge.research.semtk.standaloneExecutables;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.aws.S3Connector;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.NeptuneSparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class PerformanceTest {

	private static String resourceFolder;
	private static String taskName;
	private static long startTime;
	private static SparqlEndpointInterface sei;
	private final static boolean NO_PRECHECK = false;
	private final static boolean PRECHECK = true;

	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		if (args.length != 3) {
			throw new Exception("Usage: PerformanceTest server_type server_url resource_folder");
		}
		
		
		String graph = "http://performance_test_0";
		sei = SparqlEndpointInterface.getInstance(args[0], args[1], graph, "dba", "dba");
		
		log("graph: " + graph);
		resourceFolder = args[2];
		if (!Files.exists(Paths.get(resourceFolder))) {
			throw new Exception("Resource folder doesn't exist: " + resourceFolder);
		}
		
		Table table = sei.executeQueryToTable(SparqlToXUtils.generateCountTriplesSparql(sei));
		long triples = table.getCellAsLong(0, 0);
		if (triples > 0) {
			System.err.println("Found " + triples + " triples in performance test graph.  Clearing them.");
			System.err.println("Make sure there are not multiple performance tests running");
		}
		
		try {
			
			addSimpleRows(0, 10000); 
			addBatteryDescriptions(0, 20000);   // 150,20000);
			addBatteryDescriptionsVaryingThreadsAndSize(2000);
			addSimpleBiggerRows(0, 50000);
			
		} finally {
			sei.clearGraph();
			log("cleaned up");
		}
		
		log("fine.");
	}
	
	private static void addSimpleRows(int passes, int pass_size) throws Exception {
		
		// setup
		sei.clearGraph();
		SparqlGraphJson sgJson = getSparqlGraphJsonFromFile("/loadTest.json", sei, sei);
		uploadOwl(sei, "/loadTest.owl");
		
		int pass = -1;
		long triples = 0;
		
		while (++pass < passes) {
			// build 10,000 rows
			int i = 0;

			StringBuilder content = new StringBuilder();
			content.append("battery name, cell id, color\n");
			for (i=0; i < pass_size; i++) {
				int index = pass * pass_size + i;
				content.append("name_" + index + ",cell_" + index + ",red\n");
			}
			
			Dataset ds0 = new CSVDataset(content.toString(), true);
	
			DataLoader dl0 = new DataLoader(sgJson, 30, ds0, "dba", "dba");
			startTask("load " + pass_size + "  totaling," + (pass + 1) * pass_size);
			dl0.importData(NO_PRECHECK);
			endTask();
			
			// get new total triples twice
			triples = sei.executeQueryToTable(SparqlToXUtils.generateCountTriplesSparql(sei)).getCellAsLong(0, 0);
			startTask("count triples, " + triples);
			sei.executeQueryToTable(SparqlToXUtils.generateCountTriplesSparql(sei)).getCellAsLong(0, 0);
			endTask();
		}

	}
	
	private static void addSimpleBiggerRows(int passes, int pass_size) throws Exception {
		
		// setup
		sei.clearGraph();
		SparqlGraphJson sgJson = getSparqlGraphJsonFromFile("/loadTestDuraBattery.json", sei, sei);
		uploadOwl(sei, "/loadTestDuraBattery.owl");
		
		int pass = -1;
		long triples = 0;
		
		while (++pass < passes) {
			// build 10,000 rows
			int i = 0;

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
	
			DataLoader dl0 = new DataLoader(sgJson, 30, ds0, "dba", "dba");
			startTask("load " + pass_size + "  totaling," + (pass + 1) * pass_size);
			dl0.importData(PRECHECK);
			endTask();
			
			// get new total triples twice
			triples = sei.executeQueryToTable(SparqlToXUtils.generateCountTriplesSparql(sei)).getCellAsLong(0, 0);
			startTask("count triples, " + triples);
			sei.executeQueryToTable(SparqlToXUtils.generateCountTriplesSparql(sei)).getCellAsLong(0, 0);
			endTask();
		}

	}
	
	/**
	 
	 * @param passes
	 * @throws Exception
	 */
	private static void addBatteryDescriptions(int passes, int pass_size) throws Exception {
		
		// setup
		SparqlGraphJson sgJson1 = getSparqlGraphJsonFromFile("/loadTestDuraBattery.json", sei, sei);
		SparqlGraphJson sgJson2 = getSparqlGraphJsonFromFile("/lookupBatteryIdAddDesc.json", sei, sei);
		
		sei.clearGraph();
		uploadOwl(sei, "/loadTestDuraBattery.owl");
		long triples = 0;

		int pass = -1;
		
		while (++pass < passes) {
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
			DataLoader dl1 = new DataLoader(sgJson1, 30, ds1, "dba", "dba");
			startTask("load simple " + pass_size + " total," + (pass + 1) * pass_size);
			dl1.importData(NO_PRECHECK);
			endTask();
			
			// add descriptions
			Dataset ds2 = new CSVDataset(content2.toString(), true);
			DataLoader dl2 = new DataLoader(sgJson2, 100, ds2, "dba", "dba");
			startTask("load lookup " + pass_size + "total," + (pass + 1) * pass_size);
			dl2.importData(NO_PRECHECK);
			endTask();
			
			// get new total triples twice
			triples = sei.executeQueryToTable(SparqlToXUtils.generateCountTriplesSparql(sei)).getCellAsLong(0, 0);
			startTask("count triples," + triples );
			sei.executeQueryToTable(SparqlToXUtils.generateCountTriplesSparql(sei)).getCellAsLong(0, 0);
			endTask();

		}

	}
	
	/**
	 
	 * @param passes
	 * @throws Exception
	 */
	private static void addBatteryDescriptionsVaryingThreadsAndSize(int pass_size) throws Exception {
		
		// setup
		SparqlGraphJson sgJson1 = getSparqlGraphJsonFromFile("/loadTestDuraBattery.json", sei, sei);
		SparqlGraphJson sgJson2 = getSparqlGraphJsonFromFile("/lookupBatteryIdAddDesc.json", sei, sei);
		
		sei.clearGraph();
		uploadOwl(sei, "/loadTestDuraBattery.owl");
		long triples = 0;

		int pass = -1;
		
		for (int threads = 1; threads < 10; threads += 1) {
			for (int querySize = 500; querySize < 10000; querySize += 500) {
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
				startTask("load simple " + pass_size + " total," + threads + "," + querySize + "," + (pass + 1) * pass_size);
				dl1.importData(NO_PRECHECK);
				endTask();
				
				// add descriptions
				Dataset ds2 = new CSVDataset(content2.toString(), true);
				DataLoader dl2 = new DataLoader(sgJson2, 8, ds2, "dba", "dba");
				dl1.overrideMaxThreads(threads);
				dl1.overrideInsertQueryIdealSize(querySize);
				startTask("load lookup " + pass_size + "total," + threads + "," + querySize + "," + (pass + 1) * pass_size);
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
	
	private static void endTask() {
		long elapsed = System.nanoTime() - startTime;
		System.out.println("task, " + taskName + ", " + Double.toString(elapsed / 1000000000.0));
	}
	
	public static void uploadOwl(SparqlEndpointInterface sei, String filename) throws Exception {
		File f = Paths.get(resourceFolder + "/" + filename).toFile();
		byte [] owl =  FileUtils.readFileToByteArray(f);
		sei.executeAuthUpload(owl);
	}
	
	public static SparqlGraphJson getSparqlGraphJsonFromFile(String jsonFilename, SparqlEndpointInterface modelSei, SparqlEndpointInterface dataSei) throws Exception {	
		InputStream is = Files.newInputStream(Paths.get(resourceFolder,jsonFilename));
		InputStreamReader reader = new InputStreamReader(is);
		JSONObject jObj = (JSONObject) (new JSONParser()).parse(reader);	
		
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
