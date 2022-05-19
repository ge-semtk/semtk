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
import java.util.HashSet;

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
	private Table cardinalityTable = null;
	private JobTracker tracker = null;
	private String jobId = null;
	private int startPercent = 0;
	private int endPercent = 100;

	/**
	 * This class checks restrictions outside of query and ingestion process.
	 * e.g. cardinality is either impossible (e.g. min) or too expensive (e.g. max) to check during ingestion
	 *                     and meaningless during a SELECT
	 *                     
	 * It may also re-run oinfo restrictions (more needed for SELECT and INSERT)
	 * just to double-check nothing slipped by.
	 * 
	 * @param conn
	 * @param oInfo
	 */
	public RestrictionChecker(SparqlConnection conn, OntologyInfo oInfo) throws Exception {
		this(conn, oInfo, null, null, 0, 100);
	}
	
	public RestrictionChecker(SparqlConnection conn, OntologyInfo oInfo, JobTracker tracker, String jobId, int percentStart, int percentEnd) throws Exception {
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
	}
	public Table checkCardinality() throws Exception {
		return this.checkCardinality(0);
	}

	public Table checkCardinality(int maxRows) throws Exception {
		int percent = this.startPercent;
		
		// get restrictions
		if (this.tracker != null) {
			this.tracker.setJobPercentComplete("Querying cardinality restrictions", percent);
			percent += (this.endPercent - percent) / 10;
		}
		this.queryCardinalityRestrictions();
		
		
		HashSet<String> COUNT_FUNC = new HashSet<String>();
		COUNT_FUNC.add("COUNT");
	
		Table retTable = new Table(new String[] {"class", "property", "restriction", "limit", "subject", "actual_cardinality"}, new String[] {"string", "string", "string", "integer", "string", "integer"} );
		
		// run a query to check each restriction
		for (int i=0; i < this.cardinalityTable.getNumRows(); i++) {
			if (this.tracker != null) {
				this.tracker.setJobPercentComplete("Checking cardinality restrictions", percent + (this.endPercent - percent) * (i / this.cardinalityTable.getNumRows()));
			}
			String className = this.cardinalityTable.getCell(i, 0);
			String propName = this.cardinalityTable.getCell(i, 1);
			String restriction = new OntologyName(this.cardinalityTable.getCell(i, 2).toLowerCase()).getLocalName();
	
			int limit = this.cardinalityTable.getCellAsInt(i, 3);
			
			String negOp = null;
			if (restriction.equals("maxcardinality") || restriction.endsWith("maxqualifiedcardinality")) 
				negOp = ">";
			else if (restriction.endsWith("mincardinality") || restriction.endsWith("minqualifiedcardinality")) 
				negOp = "<";
			else if (restriction.endsWith("cardinality") || restriction.endsWith("qualifiedcardinality")) 
				negOp = "!=";
			else {
				LocalLogger.logToStdOut("ModelRestrictions.java warning: model contains unrecognized cardinality restriction: " + restriction);
				continue;
			}
				
			String sparql = SparqlToXLibUtil.generateCheckCardinalityRestrictions(this.dataConn, this.oInfo, className, propName, negOp, limit);
			
			Table failureTab = this.dataConn.getDefaultQueryInterface().executeToTable(sparql);
			for (ArrayList<String> row : failureTab.getRows()) {
				retTable.addRow(new String[] {className, propName, restriction, Integer.toString(limit), row.get(0), row.get(1)});
				
				// return if hit maxRows
				if (maxRows > 0 && retTable.getNumRows() >= maxRows) {
					return retTable;
				}
			}
		}
		// return if finished without hitting maxRows
		return retTable;
	}
	
	/**
	 * Populate cardinalityTable with all cardinality restrictions in the model connection
	 * @throws Exception
	 */
	private void queryCardinalityRestrictions() throws Exception {
		String query = SparqlToXLibUtil.generateGetCardinalityRestrictions(this.modelConn, this.oInfo);
		SparqlEndpointInterface sei = this.modelConn.getDefaultQueryInterface();
		this.cardinalityTable = sei.executeToTable(query);
	}
}
