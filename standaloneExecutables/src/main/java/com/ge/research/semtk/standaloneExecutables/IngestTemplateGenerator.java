package com.ge.research.semtk.standaloneExecutables;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;

import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.NodeItem;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.load.utility.ImportSpec;
import com.ge.research.semtk.load.utility.IngestionNodegroupBuilder;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyName;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.services.nodegroupStore.NgStore;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;


public class IngestTemplateGenerator {

	// ARGS if this were a real program
	private static String ID_REGEX =null;
	private static String SEI_TYPE = null;
	private static String SEI_URL = null;
	private static String MODEL_GRAPH = null;
	private static String DATA_GRAPH = null;
	private static String JSON_DIR = null;
	private static String CSV_DIR = null;
	private static String BASE_URI_REGEX = null;
	private static String DELETE_FROM_FILENAMES = null;
	
	
	
	/**
	 * Set constants from cmd line
	 */
	private static void processCmdLineArgs(String[] args) {
		Options options = new Options();
		Option o;
		
		String idRegex = "id-regex";
		String idRegexDefault = "identifier";
		o = new Option(null, idRegex, true, "Regular express to match id fields.  URI lookup create-if-missing, default: " + idRegexDefault);
		o.setRequired(false);
		options.addOption(o);
		
		String baseUriRegex = "base-uri-regex";
		String baseUriRegexDefault = ".";
		o = new Option(null, baseUriRegex, true, "Regular express to model classes to be processed.  Default: " + baseUriRegexDefault + " (all)");
		o.setRequired(false);
		options.addOption(o);
		
		String seiType = "server-type";
		String seiTypeDefault = "fuseki";
		o = new Option(null, seiType, true, "Server type. default: " + seiTypeDefault);
		o.setRequired(false);
		options.addOption(o);
		
		String seiUrl = "server-url";
		String seiUrlDefault = "http://localhost:3030/RACK";
		o = new Option(null, seiUrl, true, "Server url. default: " + seiUrlDefault);
		o.setRequired(false);
		options.addOption(o);
		
		String modelGraph = "model-graph";
		String modelGraphDefault = "http://rack001/model";
		o = new Option(null, modelGraph, true, "Graph containing ontology. default: " + modelGraphDefault);
		o.setRequired(false);
		options.addOption(o);
		
		String dataGraph = "data-graph";
		String dataGraphDefault = "http://rack001/data";
		o = new Option(null, dataGraph, true, "Graph where data will be ingested. default: " + dataGraphDefault);
		o.setRequired(false);
		options.addOption(o);
		
		String jsonFolder = "json-dir";
		String jsonFolderDefault = "ingestion";
		o = new Option(null, jsonFolder, true, "Folder to place generated subfolders of json templates. default: " + jsonFolderDefault);
		o.setRequired(false);
		options.addOption(o);
		
		String csvFolder = "csv-dir";
		String csvFolderDefault = "CDR";
		o = new Option(null, csvFolder, true, "Folder to place sample csv files. default: " + csvFolderDefault);
		o.setRequired(false);
		options.addOption(o);
		
		String delFromFilenames = "del-from-filenames";
		String delFromFilenamesDefault = "arcos\\.rack,arcos\\.";
		o = new Option(null, delFromFilenames, true, "Comma-separated strings to delete from generated filenames. default: " + delFromFilenamesDefault);
		o.setRequired(false);
		options.addOption(o);
		
		CommandLineParser parser = new BasicParser();

		try {
			CommandLine cmd = parser.parse(options, args);
			ID_REGEX = cmd.getOptionValue(idRegex, idRegexDefault);
			SEI_TYPE = cmd.getOptionValue(seiType, seiTypeDefault);
			SEI_URL = cmd.getOptionValue(seiUrl, seiUrlDefault);
			MODEL_GRAPH = cmd.getOptionValue(modelGraph, modelGraphDefault);
			DATA_GRAPH = cmd.getOptionValue(dataGraph, dataGraphDefault);
			JSON_DIR = cmd.getOptionValue(jsonFolder, jsonFolderDefault);
			CSV_DIR = cmd.getOptionValue(csvFolder, csvFolderDefault);
			BASE_URI_REGEX = cmd.getOptionValue(baseUriRegex, baseUriRegexDefault);
			DELETE_FROM_FILENAMES = cmd.getOptionValue(delFromFilenames, delFromFilenamesDefault);
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
			(new HelpFormatter()).printHelp("IngestTemplateGenerator", options);
			System.exit(1);
		}
	}
	
	public static void main(String[] args) throws Exception {		
		
		processCmdLineArgs(args);
		
		Pattern baseURIPattern = Pattern.compile(BASE_URI_REGEX);
		
		// Build a connection
		SparqlEndpointInterface modelSei = SparqlEndpointInterface.getInstance(SEI_TYPE, SEI_URL, MODEL_GRAPH);
		SparqlEndpointInterface dataSei =  SparqlEndpointInterface.getInstance(SEI_TYPE, SEI_URL, DATA_GRAPH);
		SparqlConnection conn = new SparqlConnection("RACK", modelSei, dataSei);
		
		// load the ontology
		OntologyInfo oInfo = new OntologyInfo();
		oInfo.load(modelSei, true);
		ArrayList<String> classList = oInfo.getClassNames();
		
		// set up nodegroup storeTable
		String fileName;
		Path path;
		
		// first pass, set up hashes for sub-dirs
		Hashtable<String, String> dirHash = new Hashtable<String, String>();       // class to dirPath
		Hashtable<String, Table> storeTableHash = new Hashtable<String, Table>();  // dirPath to StoreCsvTable
		HashSet<String> dirSet = new HashSet<String>();
		for (String className : classList) {
			if ( baseURIPattern.matcher(className).find()) {
				String dir = className.split("/")[2];
				String dirPath = Paths.get(JSON_DIR, dir).toString();
				dirHash.put(className, dirPath);
				dirSet.add(dirPath);
				if (!storeTableHash.containsKey(dirPath)) {
					storeTableHash.put(dirPath, NgStore.createEmptyStoreCsvTable());
				}
			}
		}
		
		// remove existing csvs
		cleanOrCreateDir(CSV_DIR, ".csv");

		// remove existing jsons
		for (String dirPath : dirSet) {
			cleanOrCreateDir(dirPath, ".json");
		}
		
		HashSet<String> idHash = new HashSet<String>();
				
		// loop through classes
		for (String className : classList) {
			String classDir = dirHash.get(className);
			String classDirName = Paths.get(classDir).getFileName().toString();
			
			if ( baseURIPattern.matcher(className).find()) {
				// generate
				IngestionNodegroupBuilder builder = new IngestionNodegroupBuilder(className, conn, oInfo);
				builder.setIdRegex(ID_REGEX);
				builder.build();
				
				// write json
				String localClassName = new OntologyName(className).getLocalName();
				String id;
				String prefix = classDirName;
				if (DELETE_FROM_FILENAMES != null) {
					String [] delList = DELETE_FROM_FILENAMES.split(",");
					for (String pat : delList) {
						prefix = prefix.replaceAll(pat, "");
					}
				}
				if (!prefix.isEmpty()) prefix += "_";
				
				id = "ingest_" + prefix + localClassName;
				
				if (idHash.contains(id)) {
					throw new Exception("Can't build duplicate nodegroup id (check for duplicate class names in model): " + id);
				} 
				idHash.add(id);
				
				String comments = "Auto-generated ingestion for " + localClassName;
	
				fileName = id + ".json";
				path = Paths.get(dirHash.get(className), fileName);
				System.out.println(path.toString());
				FileUtils.writeStringToFile(path.toFile(), builder.getNodegroupJsonStr(), Charset.defaultCharset());
				
				// add to store.csv
				NgStore.addRowToStoreCsvTable(storeTableHash.get(classDir), id, comments, "auto", fileName);
				
				// write csv
				fileName = id + ".csv";
				path = Paths.get(CSV_DIR, fileName);
				
				if (Files.exists(path)) {
					throw new Exception("File already exists (check for duplicate class names in model) : " + path.toString());
				}
				System.out.println(path.toString());
				FileUtils.writeStringToFile(path.toFile(), builder.getCsvTemplate(), Charset.defaultCharset());
			} else {
				System.out.println("skipping: " + className);
			}
		}	
		fileName = "store_data.csv";
		for (String dirPath : storeTableHash.keySet()) {
			path = Paths.get(dirPath, fileName);
			System.out.println(path.toString());
			FileUtils.writeStringToFile(path.toFile(), storeTableHash.get(dirPath).toCSVString(), Charset.defaultCharset());
		}		
		
		System.out.println("done.");
	}
	
	/**
	 * If dir exists, clean out files ending in cleanExtension, otherwise try to create the dir
	 * @param dirName
	 * @param cleanExtension
	 * @throws Exception
	 */
	private static void cleanOrCreateDir(String dirName, String cleanExtension) throws Exception {
		boolean first = true;
		File d = new File(dirName);
		if (d.exists()) {
			for (File f : d.listFiles()) {
				if (first) {
					System.out.println("Deleting *" + cleanExtension + " in " + dirName);
					first = false;
				}
				
				if (f.getName().toLowerCase().endsWith(cleanExtension)) {
					f.delete();
				}
			}
		} else if (! d.mkdirs()) {
			throw new Exception("Directory could not be created:" + dirName);
		} else {
			System.out.println("Creating " + dirName);
		}
	}
	
	
}
