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

/**
 * Security note:  JobTracker checks job ownership and job admin before taking action.
 *                 All queries against triplestore are done as super-user
 */

package com.ge.research.semtk.load;

import java.io.InputStream;

import java.time.Instant;
import java.util.ArrayList;

import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.Returnable;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.servlet.utility.StartupUtilities;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.XSDSupportedType;
import com.ge.research.semtk.utility.Utility;


/**
 *   LoadTracker tracks ingestion activities
 *
 */
public class LoadTracker {
	public static final String KEY_COL = "fileKey";
	
	static boolean firstConstruct = true;
	SparqlEndpointInterface modelSei = null;
	SparqlEndpointInterface dataSei = null;
	SparqlGraphJson sgjson = null;
	String dbUser = null;
	String dbPassword = null;
	private OntologyInfoClient oInfoClient = null;

	
	public static String CLEAR = "ClearGraph";	

	public LoadTracker (SparqlEndpointInterface modelSei, SparqlEndpointInterface dataSei, String dbUser, String dbPassword, OntologyInfoClient oInfoClient) throws Exception {
		// make this thread safe
		this.modelSei = modelSei.copy();
		this.dataSei = dataSei.copy();
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
		this.oInfoClient = oInfoClient;
		
		this.uploadOwlModelIfNeeded(this);
		
		this.sgjson = new SparqlGraphJson(Utility.getResourceAsJson(this, "/nodegroups/LoadTracker.json"));
		this.sgjson.setSparqlConn(new SparqlConnection("loadTrackConn", modelSei, dataSei));
	}
	
	
	public SparqlEndpointInterface getModelSei() {
		return this.modelSei;
	}
	public SparqlEndpointInterface getDataSei() {
		return this.dataSei;
	}

	/**
	 * 
	 * @param fileKey
	 * @param fileName
	 * @param sei
	 * @param user - from HeadersManager - which is called from a SpringUtil project, not sparqlGraphLibrary
	 */
	public void trackLoad(String fileKey, String fileName, SparqlEndpointInterface sei) throws Exception {
		
		int rows = DataLoader.loadFromCsvString(
				this.sgjson, 
				this.buildCsvString("load",fileKey, fileName, sei), 
				this.dbUser, 
				this.dbPassword, 
				true);
		
		if (rows != 1) {
			throw new Exception("LoadTracker ingest internal error: rows ingested != 1: " + String.valueOf(rows));
		}
		
	}
	
	/** clear graph with tracking **/
	public void trackClear(SparqlEndpointInterface sei) throws Exception {
		
		int rows = DataLoader.loadFromCsvString(
				this.sgjson, 
				this.buildCsvString("clear", CLEAR, null, sei), 
				this.dbUser, 
				this.dbPassword, 
				true);
		
		if (rows != 1) {
			throw new Exception("LoadTracker ingest internal error: rows ingested != 1: " + String.valueOf(rows));
		}
		
	}
	
	/**
	 * throw exception of quick query fails
	 * @throws Exception
	 */
	public void test() throws Exception {
		// query should return no rows.  might return garbage.
		Long epoch = Long.valueOf(1);
		this.query(null, null, null, epoch, epoch);
	}
	
	public Table queryAll() throws Exception {
		return this.query(null, null, null, null, null);
	}
	
	public Table query(String fileKey, SparqlEndpointInterface sei, String user, Long startEpoch, Long endEpoch) throws Exception {
		NodeGroup ng = this.getConstrainedNodeGroup(fileKey, sei, user, startEpoch, endEpoch);

		String query = ng.generateSparqlSelect();
		return this.dataSei.executeQueryToTable(query);
	}
	
	public void deleteAll() throws Exception {
		this.delete(null, null, null, null, null);
	}
	
	public void delete(String fileKey, SparqlEndpointInterface sei, String user, Long startEpoch, Long endEpoch) throws Exception {
		NodeGroup ng = this.getConstrainedNodeGroup(fileKey, sei, user, startEpoch, endEpoch);
		
		// nodegroup is too complex for virtuoso??
		// for FULL_DELETE delete query, remove anything unconstrained
		ArrayList<Returnable> retList = ng.getReturnedItems();
		for (Returnable r : retList) {
			if (r.getValueConstraint() == null) {
				r.setIsReturned(false);
			}
		}
		String query = ng.generateSparqlDelete();
		this.dataSei.executeQueryAndConfirm(query);
	}
	
	private NodeGroup getConstrainedNodeGroup(String fileKey, SparqlEndpointInterface sei, String user, Long startEpoch, Long endEpoch) throws Exception {
		NodeGroup ng = this.sgjson.getNodeGroup();
		PropertyItem pItem;
		
		if (fileKey != null) {
			pItem = ng.getPropertyItemBySparqlID(KEY_COL);
			pItem.addConstraint(ValueConstraint.buildFilterInConstraint(KEY_COL, fileKey, XSDSupportedType.asSet(XSDSupportedType.STRING)));	
		}
		
		if (sei != null) {
			pItem = ng.getPropertyItemBySparqlID("seiServerAndPort");
			pItem.addConstraint(ValueConstraint.buildFilterInConstraint("seiServerAndPort", sei.getServerAndPort(), XSDSupportedType.asSet(XSDSupportedType.STRING)));
			
			pItem = ng.getPropertyItemBySparqlID("graphName");
			pItem.addConstraint(ValueConstraint.buildFilterInConstraint("graphName", sei.getGraph(), XSDSupportedType.asSet(XSDSupportedType.STRING)));
		
		}
		
		if (user != null) {
			pItem = ng.getPropertyItemBySparqlID("user");
			pItem.addConstraint(ValueConstraint.buildFilterInConstraint("user", user, XSDSupportedType.asSet(XSDSupportedType.STRING)));	
		}
		
		if (startEpoch != null && endEpoch != null ) {
			pItem = ng.getPropertyItemBySparqlID("epoch");
			pItem.addConstraint(ValueConstraint.buildRangeConstraint("epoch", String.valueOf(startEpoch), String.valueOf(endEpoch), XSDSupportedType.asSet(XSDSupportedType.LONG), true, true));	
		} else if (startEpoch != null ) {
			pItem = ng.getPropertyItemBySparqlID("epoch");
			pItem.addConstraint(ValueConstraint.buildGreaterThanConstraint("epoch", String.valueOf(startEpoch), XSDSupportedType.asSet(XSDSupportedType.LONG), true));	
		} else if (endEpoch != null ) {
			pItem = ng.getPropertyItemBySparqlID("epoch");
			pItem.addConstraint(ValueConstraint.buildLessThanConstraint("epoch", String.valueOf(endEpoch), XSDSupportedType.asSet(XSDSupportedType.LONG), true));	
		}
		
		return ng;
	}
	
	private String buildCsvString(String event, String fileKey, String fileName, SparqlEndpointInterface sei) {
		return "event, fileKey, fileName, graphName, seiServerAndPort, serverType, epoch, user\n" +
				event + "," +
				fileKey + "," + 
				((fileName == null) ? "" : fileName) + "," + 
				sei.getGraph() + "," + 
				sei.getServerAndPort() + "," + 
				sei.getServerType() + "," +
				this.getNowEpoch() + "," + 
				ThreadAuthenticator.getThreadUserName() + "\n";
	}
	private long getNowEpoch() {
		Instant instant = Instant.now();
		long timeStampSeconds = instant.getEpochSecond();
		return timeStampSeconds;  
	}

	/**
	 * If needed, upload model to given tracker's model sei
	 * @param tracker
	 * @throws Exception
	 */
	private void uploadOwlModelIfNeeded(LoadTracker tracker) throws Exception {
		
		if (LoadTracker.firstConstruct) {
			StartupUtilities.updateOwlIfNeeded(tracker.getModelSei(), this.oInfoClient, getClass(), "/semantics/OwlModels/loadLog.owl");
			LoadTracker.firstConstruct = false;
		}
	}
	
	public static String buildBaseURI(String fileKey) {
		return "http://" + fileKey + "/data";
	}

	/**
	 * Delete any data from a load, provided it was loaded with overrideBaseURI "$TRACK_KEY"
	 * Also track this event.
	 * @param fileKey
	 * @throws Exception
	 */
	public void undoLoad(String fileKey) throws Exception {
		Table tab = this.query(fileKey, null, null, null, null);
		if (tab.getNumRows() != 1) {
			throw new Exception("found " + String.valueOf(tab.getNumRows()) + " loads associated with key: " + fileKey);
		}
		SparqlEndpointInterface sei = SparqlEndpointInterface.getInstance(
				tab.getCell(0, "serverType"), 
				tab.getCell(0, "seiServerAndPort"), 
				tab.getCell(0, "graphName"),
				this.dbUser, 
				this.dbPassword);
		
		sei.clearPrefix(buildBaseURI(fileKey));
		
		// track this
		int rows = DataLoader.loadFromCsvString(
				this.sgjson, 
				this.buildCsvString("undo",fileKey, "", sei), 
				this.dbUser, 
				this.dbPassword, 
				true);
		
		if (rows != 1) {
			throw new Exception("LoadTracker ingest internal error: rows ingested != 1: " + String.valueOf(rows));
		}
	}
}
