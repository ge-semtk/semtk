package com.ge.research.semtk.standaloneExecutables;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.aws.S3Connector;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
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
import com.ge.research.semtk.sparqlX.XSDSupportedType;
import com.ge.research.semtk.utility.Utility;

public class PerformanceTest {

	private static int passSize;
	private static int numberOfItems;
	private static int numberOfLinks;
	private static int numberOfPasses;
	private static int maxTriples;
	private static int circuitBreakerSec;
	private static String resourceFolder;
	private static String taskName;
	private static String serverType;
	private static String serverURL;
	private static long startTime;
	private static String user;
	private static String password;
	private static SparqlEndpointInterface sei;
	private static String whatToTest;
	private final static boolean NO_PRECHECK = false;
	private final static boolean PRECHECK = true;
	private final static boolean LOG_QUERY_PERFORMANCE = false;

	public static void main(String[] args) throws Exception {

		Options options = new Options();

		// option: server-type
		String serverTypeArg = "server-type";
		Option serverTypeOpt = new Option(
			null, serverTypeArg, true,
			"Server type (e.g. fuseki, virtuoso)"
		);
		serverTypeOpt.setRequired(true);
		options.addOption(serverTypeOpt);

		// option: server-url
		String serverURLArg = "server-url";
		Option serverURLOpt = new Option(
			null, serverURLArg, true,
			"Server URL (ending with \"/<dataset>\")"
		);
		serverURLOpt.setRequired(true);
		options.addOption(serverURLOpt);
		
		// option: user
		String userArg = "user";
		Option userOpt = new Option(
			null, userArg, true,
			"userName for triplestore"
		);
		userOpt.setRequired(false);
		options.addOption(userOpt);
		
		// option: password
		String passwordArg = "password";
		Option passwordOpt = new Option(
			null, passwordArg, true,
			"password for triplestore"
		);
		passwordOpt.setRequired(false);
		options.addOption(passwordOpt);

		// option: resources
		String resourcesArg = "resources";
		Option resourcesOpt = new Option(
			null, resourcesArg, true,
			"Path to SparQLGraph library resources folder"
		);
		resourcesOpt.setRequired(true);
		options.addOption(resourcesOpt);

		// option: test
		String testLinkItems = "link-items";
		String testAddSimpleRows = "add-simple-rows";
		String testAddSimpleBiggerRows = "add-simple-bigger-rows";
		String testLookupAddText = "lookup-add-test";

		String whatToTestOptions =
			Stream.of(
				testAddSimpleRows,
				testAddSimpleBiggerRows,
				testLinkItems,
				testLookupAddText
			).collect(Collectors.joining(", "));

		String testArg = "test";
		Option testOpt = new Option(
			null, testArg, true,
			MessageFormat.format("What to test, one of: {0}", whatToTestOptions)
		);
		testOpt.setRequired(true);
		options.addOption(testOpt);

		// which options are requred for which tests
		String testsRequiringItemsAndLinks = Stream.of(new String[] {
				testLinkItems
		}).collect(Collectors.joining(", "));

		String testsRequiringPasses = Stream.of(new String[] {
				testAddSimpleRows,
				testAddSimpleBiggerRows,
				testLookupAddText
		}).collect(Collectors.joining(", "));

		String testsRequiringMaxTriples = Stream.of(new String[] {
				testLookupAddText
		}).collect(Collectors.joining(", "));

		String testsRequiringCircuitBreakerSec = Stream.of(new String[] {
				testLookupAddText
		}).collect(Collectors.joining(", "));

		// option: max-triples
		String maxTriplesArg = "max-triples";
		String defaultMaxTriples = "100000";
		Option maxTriplesOpt = new Option(
				null, maxTriplesArg, true,
				MessageFormat.format(
						"Number of items (for -{0} in {1}, default {2})",
						maxTriplesArg, testsRequiringMaxTriples, defaultMaxTriples
						)
				);
		options.addOption(maxTriplesOpt);
		
		// option: circuit-breaker-sec
		String circuitBreakerSecArg = "circuit-breaker-sec";
		String defaultCircuitBreakerSec = "180";
		Option maxCircuitBreakerSec = new Option(
				null, circuitBreakerSecArg, true,
				MessageFormat.format(
						"Number of items (for -{0} in {1}, default {2})",
						circuitBreakerSecArg, testsRequiringCircuitBreakerSec, defaultCircuitBreakerSec
						)
				);
		options.addOption(maxCircuitBreakerSec);
				
		// option: items
		String numberOfItemsArg = "items";
		String defaultNumberOfItems = "50000";
		Option numberOfItemsOpt = new Option(
			null, numberOfItemsArg, true,
			MessageFormat.format(
				"Number of items (for -{0} in {1}, default {2})",
				testArg, testsRequiringItemsAndLinks, defaultNumberOfItems
			)
		);
		options.addOption(numberOfItemsOpt);

		// option: links
		String numberOfLinksArg = "links";
		String defaultNumberOfLinks = "100000";
		Option numberOfLinksOpt = new Option(
			null, numberOfLinksArg, true,
			MessageFormat.format(
				"Number of links (for -{0} in {1}, default {2})",
				testArg, testsRequiringItemsAndLinks, defaultNumberOfLinks
			)
		);
		options.addOption(numberOfLinksOpt);

		// option: passes
		String numberOfPassesArg = "passes";
		String defaultNumberOfPasses = "10";
		Option numberOfPassesOpt = new Option(
			null, numberOfPassesArg, true,
			MessageFormat.format(
				"Number of passes (for -{0} in {1}, default {2})",
				testArg, testsRequiringPasses, defaultNumberOfPasses
			)
		);
		options.addOption(numberOfPassesOpt);

		// option: pass-size
		String passSizeArg = "pass-size";
		String defaultPassSize = "10000";
		Option passSizeOpt = new Option(
			null, passSizeArg, true,
			MessageFormat.format(
				"Rows per pass (for -{0} in {1}, default {2})",
				testArg, testsRequiringPasses, defaultPassSize
			)
		);
		options.addOption(passSizeOpt);

		CommandLineParser parser = new BasicParser();

		try {
			CommandLine cmd = parser.parse(options, args);
			numberOfItems = Integer.parseInt(cmd.getOptionValue(numberOfItemsArg, defaultNumberOfItems));
			numberOfLinks = Integer.parseInt(cmd.getOptionValue(numberOfLinksArg, defaultNumberOfLinks));
			numberOfPasses = Integer.parseInt(cmd.getOptionValue(numberOfPassesArg, defaultNumberOfPasses));
			passSize = Integer.parseInt(cmd.getOptionValue(passSizeArg, defaultPassSize));
			maxTriples = Integer.parseInt(cmd.getOptionValue(maxTriplesArg, defaultMaxTriples));
			circuitBreakerSec = Integer.parseInt(cmd.getOptionValue(circuitBreakerSecArg, defaultCircuitBreakerSec));
			serverType = cmd.getOptionValue(serverTypeArg);
			serverURL = cmd.getOptionValue(serverURLArg);
			resourceFolder = cmd.getOptionValue(resourcesArg);
			whatToTest = cmd.getOptionValue(testArg);
			user = cmd.getOptionValue(userArg, "dba");
			password = cmd.getOptionValue(passwordArg, "dba");
		} catch (Exception e) {
			System.out.println(e.getMessage());
			(new HelpFormatter()).printHelp("PerformanceTest", options);
			System.exit(1);
		}

		String graph = "http://performance_test_0";
		sei = SparqlEndpointInterface.getInstance(serverType, serverURL, graph, user, password);
		sei.setLogPerformance(LOG_QUERY_PERFORMANCE);
		log("graph: " + graph);
		if (!Files.exists(Paths.get(resourceFolder))) {
			throw new Exception("Resource folder doesn't exist: " + resourceFolder);
		}

		log("clearing graph");
		sei.clearGraph();

		try {

			if (whatToTest.equals(testAddSimpleRows)) {
				addRows(
					numberOfPasses, passSize, "addSimpleRows",
					"battery name, cell id, color",
					(currentPass, rowIndex) -> MessageFormat.format(
						"name_{0},cell_{0},red",
        				Integer.toString((currentPass - 1) * 10000 + rowIndex)
					)
				);
				
			} else if (whatToTest.equals(testAddSimpleBiggerRows)) {
				addRows(
					numberOfPasses, passSize, "addSimpleBiggerRows",
					"batt_id,description_opt,cell1_id_opt,cell1_color,cell2_id_opt,cell2_color,cell3_id_opt,cell3_color,cell4_id_opt,cell4_color",
					(currentPass, rowIndex) -> MessageFormat.format(
						"id{0},desc_{0},cell1_{0},red,cell2_{0},red,cell3_{0},blue,cell4_{0},white",
        				Integer.toString((currentPass - 1) * 10000 + rowIndex)
					)
				);
				
			} else if (whatToTest.equals(testLinkItems)) {
				linkItems(numberOfItems, numberOfLinks);
				
			} else if (whatToTest.equals(testLookupAddText)) {
				addBatteryDescriptions(passSize, maxTriples, circuitBreakerSec);
		
			} else {
				System.out.println(MessageFormat.format(
					"Unknown test option: {0}.\nOption -{1} should be one of {2}.",
					whatToTest, testArg, whatToTestOptions
				));
			}

			// Not yet exposed:
			// addBatteryDescriptionsVaryingThreadsAndSize(0);

		} finally {
			sei.clearGraph();
			log("cleaned up");
		}

		log("fine.");
	}

	/**
	 * A 'Pass' is just a callback that receives the pass number and does its work.
	 */
	@FunctionalInterface
	private interface Pass {
		void apply(int currentPass) throws Exception;
	}

	/**
	 * Runs the given 'Pass' the indicated number of passes.
	 *
	 * @param numberOfPasses How many passes to run
	 * @param pass           Callback to run one pass
	 */
	public static void runMultiplePasses(int numberOfPasses, Pass pass) {
		IntStream.range(0, numberOfPasses).forEach(i -> {
			try {
				// Passes receive 1-based pass number
				pass.apply(i + 1);
			} catch (Exception e) {
				System.out.println(e.getMessage());
				System.exit(1);
			}
		});
	}

	public static void addRows(int numberOfPasses, int passSize, String taskName, String csvHeader,
			BiFunction<Integer, Integer, String> buildCSVRow) throws Exception {

		// setup
		sei.clearGraph();
		uploadOwlFromSGL(sei, "/loadTest.owl");

		runMultiplePasses(numberOfPasses, (Pass) currentPass -> {
			// reload the sgJson so ImportSpec can't share between loads, cheat, or get
			// bogged down.
			SparqlGraphJson sgJson = getSGJsonFromSGL("/loadTest.json", sei, sei);

			// build the rows
			String content = csvHeader + "\n" + IntStream.range(0, passSize)
					.mapToObj(i -> buildCSVRow.apply(currentPass, i)).collect(Collectors.joining("\n"));
			Dataset ds0 = new CSVDataset(content, true);

			// ingest
			DataLoader dl0 = new DataLoader(sgJson, ds0, user, password);
			startTask(MessageFormat.format(
				"{0} load {1,number,#} totaling, {2,number,#}",
				taskName, passSize, currentPass * passSize
			));
			importAndCheck(dl0, NO_PRECHECK);
			
			endTask();

			// get new total triples twice
			long triples = sei.executeQueryToTable(SparqlToXUtils.generateCountTriplesSparql(sei)).getCellAsLong(0, 0);
			startTask(MessageFormat.format(
				"{0} count, {1,number,#}", taskName, triples
			));
			sei.executeQueryToTable(SparqlToXUtils.generateCountTriplesSparql(sei)).getCellAsLong(0, 0);
			endTask();
		});

	}

	/**

	 * @param passes
	 * @throws Exception
	 */
	private static void addBatteryDescriptions(int rows_per_pass, int max_triples, int circuit_breaker_sec) throws Exception {

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
			if (lastSec[0] < circuit_breaker_sec) {
				Dataset ds1 = new CSVDataset(content1.toString(), true);
				DataLoader dl1 = new DataLoader(sgJson1, ds1, user, password);
				startTask("addBatteryDescriptions load simple, rows, " + rows_per_pass + ",total rows," + total_rows);
				importAndCheck(dl1, PRECHECK);
				lastSec[0] = endTask();
			} else {
				System.out.println("Main circuit breaker fired.  Exit.");
			}

			// add descriptions
			if (lastSec[1] < circuit_breaker_sec) {
				Dataset ds2 = new CSVDataset(content2.toString(), true);
				DataLoader dl2 = new DataLoader(sgJson2, ds2, user, password);
				dl2.setLogPerformance(LOG_QUERY_PERFORMANCE);
				startTask("addBatteryDescriptions load lookup class, rows," + rows_per_pass/2 + ",total rows," + total_rows);
				importAndCheck(dl2, PRECHECK);
				lastSec[1] = endTask();
			}

			// add descriptions superclass
			if (lastSec[2] < circuit_breaker_sec) {
				Dataset ds3 = new CSVDataset(content3.toString(), true);
				DataLoader dl3 = new DataLoader(sgJson3, ds3, user, password);
				dl3.setLogPerformance(LOG_QUERY_PERFORMANCE);
				startTask("addBatteryDescriptions load lookup superclass, rows," + rows_per_pass /2 + ",total rows," + total_rows);
				importAndCheck(dl3, PRECHECK);
				lastSec[2] = endTask();
			}
			
			boolean loadedDescriptions = (lastSec[1] < circuit_breaker_sec && lastSec[2] < circuit_breaker_sec);

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
			String sparql;

			// Select filter in
			if (lastSec[3] < circuit_breaker_sec) {
				sparql = SparqlToXUtils.generateSelectSPOSparql(sei, ValueConstraint.buildFilterInConstraint("?o", idList, XSDSupportedType.asSet(XSDSupportedType.STRING), sei));
				startTask("addBatteryDescription select filter in 10, triples," + triples + ",total rows," + total_rows);
				t = sei.executeQueryToTable(sparql);
				lastSec[3] = endTask();
				myAssert(String.format("FILTER IN did not return %d rows:\n%s", SIZE, t.toCSVString()), t.getNumRows() == SIZE);
				if (pass==0) {
					System.out.println("Sample FILTER IN" + sparql);
				}
			}

			// Select regex
			if (lastSec[4] < circuit_breaker_sec) {
				sparql = SparqlToXUtils.generateSelectSPOSparql(sei, ValueConstraint.buildRegexConstraint("?o", regex.toString(), XSDSupportedType.asSet(XSDSupportedType.STRING)));
				startTask("addBatteryDescription select filter regex 10, triples," + triples + ",total rows," + total_rows);
				t = sei.executeQueryToTable(sparql);
				lastSec[4] = endTask();
				myAssert(String.format("FILTER REGEX did not return %d rows:\n%s", SIZE, t.toCSVString()), t.getNumRows() == SIZE);
				if (pass==0) {
					System.out.println("Sample FILTER REGEX" + sparql);
				}
			}

			// Select values
			if (lastSec[5] < circuit_breaker_sec) {
				sparql = SparqlToXUtils.generateSelectSPOSparql(sei, ValueConstraint.buildValuesConstraint("?o", idList, XSDSupportedType.asSet(XSDSupportedType.STRING), sei));
				startTask("addBatteryDescription select values 10, triples," + triples + ",total rows," + total_rows);
				t =sei.executeQueryToTable(sparql);
				lastSec[5] = endTask();
				myAssert(String.format("VALUES clause did not return %d rows:\n%s", SIZE, t.toCSVString()), t.getNumRows() == SIZE);
				if (pass==0) {
					System.out.println("Sample VALUES " + sparql);
				}
			}

			// Select from subclass
			if (loadedDescriptions && lastSec[6] < circuit_breaker_sec) {
				NodeGroup ng = sgJson3.getNodeGroup();
				PropertyItem item = ng.getPropertyItemBySparqlID("?batteryId");
				item.setValueConstraint(new ValueConstraint(ValueConstraint.buildValuesConstraint("?batteryId", idList, XSDSupportedType.asSet(XSDSupportedType.STRING), sei)));
				
				// clean up the nodegroup:  remove optionals and order by so it is a little more fair
				item.setOptMinus(PropertyItem.OPT_MINUS_NONE);
				PropertyItem item2 = ng.getPropertyItemBySparqlID("?batteryDesc");
				item2.setOptMinus(PropertyItem.OPT_MINUS_NONE);
				ng.clearOrderBy();
				
				sparql = ng.generateSparqlSelect();   // since no oInfo, no optimizing of subclass*.  always subclass*
				startTask("addBatteryDescription select subclassOf* 10, triples," + triples + ",total rows," + total_rows);
				t = sei.executeQueryToTable(sparql);
				myAssert(String.format("SUBCLASS VALUES did not return %d rows:\n%s\nSPARQL:\n%s", SIZE, t.toCSVString(), sparql), t.getNumRows() == SIZE);
				lastSec[6] = endTask();
				if (pass==0) {
					System.out.println("Sample SUBCLASS VALUES " + sparql);
				}
				
			}

		}

	}
	
	private static void importAndCheck(DataLoader dl, boolean precheck) throws Exception {
		Table err = dl.importDataGetErrorTable(precheck);
		if (err != null) {
			throw new Exception(err.toCSVString());
		}
	}
	private static void myAssert(String failMessage, boolean b) throws Exception {
		if (!b) {
			throw new Exception(failMessage);
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
		DataLoader loader = new DataLoader(sgJsonItemLoad, ds, user, password);
		startTask("linkItems load items: " + numItems);
		importAndCheck(loader, true);
		endTask();


		// build links
		content = new StringBuilder();
		content.append("itemIdFrom, itemIdTo\n");
		for (int linksBuilt = 0; linksBuilt < numLinks; linksBuilt++) {
			int fromItem = (int) (Math.random() * numItems);
			int toItem = (int) (Math.random() * numItems);
			content.append("id_" + fromItem + ", id_" + toItem + "\n");
		}

		// load links
		ds = new CSVDataset(content.toString(), true);
		loader = new DataLoader(sgJsonItemLoadLinks, ds, user, password);
		loader.overrideMaxThreads(MAX_THREADS);
		startTask("linkItems load links: " + numLinks);
		importAndCheck(loader, true);
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
				DataLoader dl1 = new DataLoader(sgJson1, 8, ds1, user, password);
				dl1.overrideMaxThreads(threads);
				dl1.overrideInsertQueryIdealSize(querySize);
				startTask("addBatteryDescriptionsVaryingThreadsAndSize load simple " + pass_size + " total," + threads + "," + querySize + "," + (pass + 1) * pass_size);
				importAndCheck(dl1, NO_PRECHECK);
				endTask();

				// add descriptions
				Dataset ds2 = new CSVDataset(content2.toString(), true);
				DataLoader dl2 = new DataLoader(sgJson2, 8, ds2, user, password);
				dl1.overrideMaxThreads(threads);
				dl1.overrideInsertQueryIdealSize(querySize);
				startTask("addBatteryDescriptionsVaryingThreadsAndSize load lookup " + pass_size + "total," + threads + "," + querySize + "," + (pass + 1) * pass_size);
				importAndCheck(dl2, NO_PRECHECK);
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
		sei.executeAuthUploadOwl(owl);
	}

	public static void uploadOwlResource(SparqlEndpointInterface sei, String filename) throws Exception {
		byte [] owl = Utility.getResourceAsBytes(PerformanceTest.class, filename);
		sei.executeAuthUploadOwl(owl);
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
