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



import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.auth.ThreadAuthenticator;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.belmont.ValueConstraint;
import com.ge.research.semtk.belmont.XSDSupportedType;

import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;

import com.ge.research.semtk.utility.Utility;


/**
 *   JobTracker instantiates a connection to the tripleStore
 *   and uses it to fulfill requests for info about a jobId.
 *
 */
public class LoadTracker {
	static boolean firstConstruct = true;
	SparqlEndpointInterface modelSei = null;
	SparqlEndpointInterface dataSei = null;
	SparqlGraphJson sgjson = null;
	String dbUser = null;
	String dbPassword = null;

	
	public static String CLEAR = "ClearGraph";	

	public LoadTracker (SparqlEndpointInterface modelSei, SparqlEndpointInterface dataSei, String dbUser, String dbPassword) throws Exception {
		// make this thread safe
		this.modelSei = modelSei.copy();
		this.dataSei = dataSei.copy();
		this.dbUser = dbUser;
		this.dbPassword = dbPassword;
		
		LoadTracker.uploadOwlModelIfNeeded(this);
		
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
				this.sgjson.toJson(), 
				this.getCsv(fileKey, fileName, sei), 
				this.dbUser, 
				this.dbPassword, 
				true);
		
		if (rows != 1) {
			throw new Exception("LoadTracker ingest internal error: rows ingested != 1: " + String.valueOf(rows));
		}
		
	}
	
	public void trackClear(SparqlEndpointInterface sei) throws Exception {
		
		int rows = DataLoader.loadFromCsvString(
				this.sgjson.toJson(), 
				this.getCsv(CLEAR, null, sei), 
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

		String query = ng.generateSparqlDelete(null);
		this.dataSei.executeQueryAndConfirm(query);
	}
	
	private NodeGroup getConstrainedNodeGroup(String fileKey, SparqlEndpointInterface sei, String user, Long startEpoch, Long endEpoch) throws Exception {
		NodeGroup ng = this.sgjson.getNodeGroup();
		PropertyItem pItem;
		
		if (fileKey != null) {
			pItem = ng.getPropertyItemBySparqlID("fileKey");
			pItem.addConstraint(ValueConstraint.buildFilterInConstraint("fileKey", fileKey, XSDSupportedType.STRING));	
		}
		
		if (sei != null) {
			pItem = ng.getPropertyItemBySparqlID("seiServerAndPort");
			pItem.addConstraint(ValueConstraint.buildFilterInConstraint("seiServerAndPort", sei.getServerAndPort(), XSDSupportedType.STRING));
			
			pItem = ng.getPropertyItemBySparqlID("graphName");
			pItem.addConstraint(ValueConstraint.buildFilterInConstraint("graphName", sei.getGraph(), XSDSupportedType.STRING));
		
		}
		
		if (user != null) {
			pItem = ng.getPropertyItemBySparqlID("user");
			pItem.addConstraint(ValueConstraint.buildFilterInConstraint("user", user, XSDSupportedType.STRING));	
		}
		
		if (startEpoch != null && endEpoch != null ) {
			pItem = ng.getPropertyItemBySparqlID("epoch");
			pItem.addConstraint(ValueConstraint.buildRangeConstraint("epoch", String.valueOf(startEpoch), String.valueOf(endEpoch), XSDSupportedType.LONG, true, true));	
		} else if (startEpoch != null ) {
			pItem = ng.getPropertyItemBySparqlID("epoch");
			pItem.addConstraint(ValueConstraint.buildGreaterThanConstraint("epoch", String.valueOf(startEpoch), XSDSupportedType.LONG, true));	
		} else if (endEpoch != null ) {
			pItem = ng.getPropertyItemBySparqlID("epoch");
			pItem.addConstraint(ValueConstraint.buildLessThanConstraint("epoch", String.valueOf(endEpoch), XSDSupportedType.LONG, true));	
		}
		
		return ng;
	}
	
	private String getCsv(String fileKey, String fileName, SparqlEndpointInterface sei) {
		return "fileKey, fileName, graphName, seiServerAndPort, epoch, user\n" +
				fileKey + "," + 
				((fileName == null) ? "" : fileName) + "," + 
				sei.getGraph() + "," + 
				sei.getServerAndPort() + "," + 
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
	private static void uploadOwlModelIfNeeded(LoadTracker tracker) throws Exception {
		
		if (LoadTracker.firstConstruct) {
		
			try {
				AuthorizationManager.setSemtkSuper();

				InputStream owlStream = LoadTracker.class.getResourceAsStream("/semantics/OwlModels/loadLog.owl");
				tracker.getModelSei().uploadOwlModelIfNeeded(owlStream);
				owlStream.close();
				
				LoadTracker.firstConstruct = false;
			} finally {
				AuthorizationManager.clearSemtkSuper();
			}
		}
	}
	
}
