/**
 ** Copyright 2016 General Electric Company
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


package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;

import org.json.simple.JSONArray;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlToXLib.SparqlToXLibUtil;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;

public class RestrictionChecker {

	private SparqlConnection modelConn = null;
	private SparqlConnection dataConn = null;
	private OntologyInfo oInfo = null;
	private Table cardinalityRestrictionsTable = null;
	private JobTracker tracker = null;
	private String jobId = null;
	private int startPercent = 0;
	private int endPercent = 100;
	
	private Hashtable<String,Integer> maxHash = new Hashtable<String,Integer>();
	private Hashtable<String,Integer> minHash = new Hashtable<String,Integer>();
	private Hashtable<String,Integer> exactHash = new Hashtable<String,Integer>();
	
	// table column headers
	private String COL_CLASS = "class";
	private String COL_PROPERTY = "property";
	private String COL_RESTRICTION = "restriction";
	private String COL_LIMIT = "limit";
	private String COL_SUBJECT = "subject";
	private String COL_PROPERTYCOUNT = "property_count";
	private String COL_SUBJECTLIST = "subject_list";
	private String COL_INSTANCECOUNT = "instance_count";
	
	/**
	 * This class checks restrictions outside of query and ingestion process.
	 * e.g. cardinality is impossible (e.g. min) or too expensive (e.g. max) to check during ingestion, and meaningless during a SELECT
	 *                     
	 * It may also re-run oinfo restrictions to confirm nothing missed during query/ingestion.
	 */
	public RestrictionChecker(SparqlConnection conn, OntologyInfo oInfo) throws Exception {
		this(conn, oInfo, null, null, 0, 100);
	}
	
	public RestrictionChecker(SparqlConnection conn, OntologyInfo oInfo, JobTracker tracker, String jobId, int percentStart, int percentEnd) throws Exception {
		
		this.tracker = tracker;
		this.jobId = jobId;
		this.startPercent = percentStart;
		this.endPercent = percentEnd;
		
		// conn with only the model : set data(0) to model(0)
		this.modelConn = new SparqlConnection();
		this.modelConn.setName("model");
		this.modelConn.addDataInterface(conn.getModelInterface(0));
		for (SparqlEndpointInterface m : conn.getModelInterfaces()) {
			this.modelConn.addModelInterface(m);
		}
		
		// conn with only data: set model(0) to data(0)
		this.dataConn = new SparqlConnection();
		this.dataConn.setName("data");
		this.dataConn.addModelInterface(conn.getDataInterface(0));
		for (SparqlEndpointInterface d : conn.getDataInterfaces()) {
			this.dataConn.addDataInterface(d);
		}
		
		this.oInfo = oInfo;
		
		// query for restrictions, create hashes
		this.queryAndBuildCardinalityHashes();
	}
	
	/**
	 * Generate a table of cardinality violations
	 */
	public Table getCardinalityViolations() throws Exception {
		return this.getCardinalityViolations(0, false);
	}
	
	/**
	 * Generate a table of cardinality violations
	 * @param conciseFormat return table in format that reduces redundant data and adds class instance count column
	 */
	public Table getCardinalityViolations(boolean conciseFormat) throws Exception {
		return this.getCardinalityViolations(0, conciseFormat);
	}

	/**
	 * Generate a table of cardinality violations
	 * @param maxRows maximum number of violation rows
	 * @param conciseFormat return table in format that reduces redundant data and adds class instance count column
	 */
	public Table getCardinalityViolations(int maxRows, boolean conciseFormat) throws Exception {
		
		HashSet<String> COUNT_FUNC = new HashSet<String>();
		COUNT_FUNC.add("COUNT");
	
		// create return table to populate
		Table retTable = new Table(new String[] {COL_CLASS, COL_PROPERTY, COL_RESTRICTION, COL_LIMIT, COL_SUBJECT, COL_PROPERTYCOUNT}, new String[] {"string", "string", "string", "integer", "string", "integer"} );
		
		boolean hitMaxRows = false;
		// run a query to check each restriction
		for (int i=0; i < this.cardinalityRestrictionsTable.getNumRows(); i++) {

			if(hitMaxRows) { break; }
			
			// update job status
			if (this.tracker != null) {
				int currentPercent = (int)(this.startPercent + (this.endPercent - this.startPercent) * (((double)i) / this.cardinalityRestrictionsTable.getNumRows()));
				this.tracker.setJobPercentComplete(jobId, currentPercent, "Querying cardinality restrictions");
			}

			String className = this.cardinalityRestrictionsTable.getCell(i, 0);
			String propName = this.cardinalityRestrictionsTable.getCell(i, 1);
			String restriction = new OntologyName(this.cardinalityRestrictionsTable.getCell(i, 2).toLowerCase()).getLocalName();
			int limit = this.cardinalityRestrictionsTable.getCellAsInt(i, 3);
			
			String negOp = null;
			if (this.isMax(restriction)) 
				negOp = ">";
			else if (this.isMin(restriction)) 
				negOp = "<";
			else if (this.isExact(restriction)) 
				negOp = "!=";
			else {
				LocalLogger.logToStdOut("RestrictionChecker.java warning: model contains unrecognized cardinality restriction: " + restriction);
				continue;
			}
				
			String sparql = SparqlToXLibUtil.generateCheckCardinalityRestrictions(this.dataConn, this.oInfo, className, propName, negOp, limit);
			
			Table failureTab = this.dataConn.getDefaultQueryInterface().executeToTable(sparql);
			for (ArrayList<String> row : failureTab.getRows()) {
				retTable.addRow(new String[] {className, propName, restriction, Integer.toString(limit), row.get(0), row.get(1)});
				
				// return if hit maxRows
				if (maxRows > 0 && retTable.getNumRows() >= maxRows) {
					hitMaxRows = true;
					break;
				}
			}
		}
		
		// return table in concise or original format
		if(conciseFormat) {
			return applyConciseFormat(retTable);
		} else {
			return retTable;
		}
	}
	
	
	/**
	 * Apply concise formatting to cardinality violations table
	 * @param violationsTable table in original format
	 * @return table in concise format
	 */
	private Table applyConciseFormat(Table violationsTable) throws Exception {
		
		// structure for return table
		Table retTable = new Table(new String[] {COL_CLASS, COL_PROPERTY, COL_RESTRICTION, COL_LIMIT, COL_INSTANCECOUNT, COL_PROPERTYCOUNT, COL_SUBJECTLIST}, new String[] {"string", "string", "string", "integer", "integer", "integer", "string"} );
		Table retTableSubset = retTable.copy();		// use return table subsets to achieve desired sorting
		
		// for each class in the violations table
		for(String clazz : violationsTable.getColumnUniqueValues(COL_CLASS)) {
			Table violationsForClass = violationsTable.getSubsetWhereMatches(COL_CLASS, clazz);	// subset of violations for this class
			int instanceCount = getInstanceCount(clazz);
			
			// for each class+property in the violations table
			for(String property : violationsForClass.getColumnUniqueValues(COL_PROPERTY)) {
				Table violationsForClassAndProperty = violationsForClass.getSubsetWhereMatches(COL_PROPERTY, property);  // subset of violations for this class+property

				// get restriction (expect all rows here to have same restriction)
				String[] tmp = violationsForClassAndProperty.getColumnUniqueValues(COL_RESTRICTION);
				if(tmp.length > 1)
					throw new Exception("Unexpectedly got multiple unique restrictions for " + clazz + " and " + property);
				String restriction = tmp[0];
				
				// get limit (expect all rows here to have same limit)
				tmp = violationsForClassAndProperty.getColumnUniqueValues(COL_LIMIT);
				if(tmp.length > 1)
					throw new Exception("Unexpectedly got multiple unique limits for " + clazz + " and " + property);
				int limit = Integer.valueOf(tmp[0]).intValue();
				
				// create map of property count => list of subjects violating the restriction with this number of properties
				// e.g. 0 => subject1, subject2 if subject1 and subject 2 both offend by having 0 of these properties
				Hashtable<Integer, ArrayList<String>> propertyCountHash = new Hashtable<Integer, ArrayList<String>>();				
				for(ArrayList<String> row : violationsForClassAndProperty.getRows()){
					String subject = row.get(4);					// URI of subject
					Integer propertyCount = Integer.valueOf(row.get(5));	// actual number of properties that this subject has
					ArrayList<String> uriList = propertyCountHash.get(propertyCount);
					if(uriList == null) {
						propertyCountHash.put(propertyCount, new ArrayList<>(Arrays.asList(subject)));  // new list with just the single subject
					}else {
						uriList.add(subject);	// add this subject to the existing list
					}
				}
				
				// for each propertyCount entry, add row to table
				for(Integer i : propertyCountHash.keySet()) {
					String uriListJson = JSONArray.toJSONString(propertyCountHash.get(i));
					retTableSubset.addRow(new String[] {clazz, property, restriction, Integer.toString(limit), Integer.toString(instanceCount), Integer.toString(i), uriListJson});
				}
				
				// sort the results subset table and add it to the results table
				retTableSubset.sortByColumnInt(COL_PROPERTYCOUNT);
				retTable.append(retTableSubset);
				retTableSubset.clearRows();
			}
		}
		return retTable;
	}
	
	// gets the number of instances (including subclasses) present in the dataset
	private int getInstanceCount(String className) throws Exception {
		String sparql = SparqlToXLibUtil.generateCountInstances(dataConn, oInfo, className);
		try {
			return this.dataConn.getDefaultQueryInterface().executeToTable(sparql).getCellAsInt(0, 0);
		}catch(Exception e) {
			throw new Exception("Unexpected result querying for instance count: " + e.getMessage());
		}
	}
	
	// determines whether given restriction text indicates max/min/exact restriction
	private boolean isMax(String restriction) {
		return restriction.equals("maxcardinality") || restriction.endsWith("maxqualifiedcardinality");
	}
	private boolean isMin(String restriction) {
		return restriction.endsWith("mincardinality") || restriction.endsWith("minqualifiedcardinality");
	}
	private boolean isExact(String restriction) {
		return restriction.endsWith("cardinality") || restriction.endsWith("qualifiedcardinality");
	}

	/**
	 * Get all cardinality restrictions in the model connection
	 * @throws Exception
	 */
	private Table getCardinalityRestrictions() throws Exception {
		String query = SparqlToXLibUtil.generateGetCardinalityRestrictions(this.modelConn, this.oInfo);
		SparqlEndpointInterface sei = this.modelConn.getDefaultQueryInterface();
		return sei.executeToTable(query);
	}
	
	/**
	 * Would n instances of property propUri satisfy all existing cardinality rules for classUris
	 * @param classUris array of one or more classes to which a particular instance belongs
	 * @param propUri	the property
	 * @param n			the number to check
	 * @return			true or false
	 * @throws Exception
	 */
	public boolean satisfiesCardinality(String[] classUris, String propUri, int n) throws Exception {
		for (String c : classUris) {
			if (! this.satisfiesCardinality(c, propUri, n))
				return false;
		}
		return true;
	}

	public boolean satisfiesCardinality(String classUri, String propUri, int n) throws Exception {
		String key = this.buildKey(classUri, propUri);
		
		Integer min = this.minHash.get(key);
		if (min != null && n < min) return false;
		
		Integer max = this.maxHash.get(key);
		if (max != null && n > max) return false;
		
		Integer exact = this.exactHash.get(key);
		if (exact != null && n != exact) return false;
		
		return true;
	}
	
	/**
	 * Determines whether a cardinality restriction is present for a given class/property
	 * @param classUris array of one or more classes to which a particular instance belongs
	 * @param propUri	the property
	 * @return			true or false
	 * @throws Exception
	 */
	public boolean hasCardinalityRestriction(String[] classUris, String propUri) throws Exception {
		for (String c : classUris) {
			if (this.hasCardinalityRestriction(c, propUri))
				return true;
		}
		if (classUris.length == 0) throw new Exception("Internal error: classUris is empty");
		return false;
	}

	public boolean hasCardinalityRestriction(String classUri, String propUri) throws Exception {
		String key = this.buildKey(classUri, propUri);
		return this.minHash.containsKey(key) || this.maxHash.containsKey(key) || this.exactHash.containsKey(key);
	}
	
	// populate cardinalityRestrictionsTable, maxHash, minHash, exactHash
	private void queryAndBuildCardinalityHashes() throws Exception {
		
		// populate restriction table
		this.cardinalityRestrictionsTable = getCardinalityRestrictions();
		
		// populate hashes using restriction table
		for (int i=0; i < this.cardinalityRestrictionsTable.getNumRows(); i++) {
		
			String className = this.cardinalityRestrictionsTable.getCell(i, 0);
			String propName = this.cardinalityRestrictionsTable.getCell(i, 1);
			String restriction = new OntologyName(this.cardinalityRestrictionsTable.getCell(i, 2).toLowerCase()).getLocalName();	
			int limit = this.cardinalityRestrictionsTable.getCellAsInt(i, 3);

			HashSet<String> classNames = this.oInfo.getSubclassNames(className);
			classNames.add(className);
			
			// build hashes for all restrictions.
			// for retrieval efficiency, enter all the subclasses too
			// when restrictions overlap for (sub)classes, save the tightest one
			if (this.isMax(restriction)) {
				for (String c : classNames) {
					String key = this.buildKey(c, propName);
					if (!this.maxHash.contains(key) || this.maxHash.get(key) > limit)
						this.maxHash.put(key, limit);
				}
			} else if (this.isMin(restriction)) {
				for (String c : classNames) {
					String key = this.buildKey(c, propName);
					if (!this.minHash.contains(key) || this.minHash.get(key) < limit)
						this.minHash.put(key, limit);
				}
			} else if (this.isExact(restriction)) {
				for (String c : classNames) {
					String key = this.buildKey(c, propName);
					if (!this.exactHash.contains(key) || this.exactHash.get(key) != limit)
						this.exactHash.put(key, limit);
				}
			} else {
				LocalLogger.logToStdOut("RestrictionChecker.java warning: model contains unrecognized cardinality restriction: " + restriction);
				continue;
			}
		}
	}
	
	// generate a key to use in the hashes
	private String buildKey(String className, String propName) {
		return className + ":" + propName;
	}

}
