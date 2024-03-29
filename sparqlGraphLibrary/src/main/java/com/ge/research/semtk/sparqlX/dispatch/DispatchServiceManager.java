/**
 ** Copyright 2016-2020 General Electric Company
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
package com.ge.research.semtk.sparqlX.dispatch;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;

import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.Node;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.belmont.PropertyItem;
import com.ge.research.semtk.edc.client.ExecuteClientConfig;
import com.ge.research.semtk.edc.client.OntologyInfoClient;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyClass;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyName;
import com.ge.research.semtk.querygen.client.QueryExecuteClient;
import com.ge.research.semtk.querygen.client.QueryGenClient;
import com.ge.research.semtk.querygen.client.QueryGenClientConfig;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.services.client.RestClientConfig;
import com.ge.research.semtk.servlet.utility.StartupUtilities;
import com.ge.research.semtk.sparqlX.BadQueryException;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.client.SparqlQueryAuthClientConfig;
import com.ge.research.semtk.sparqlX.client.SparqlQueryClientConfig;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

public class DispatchServiceManager {
	
	private static final String EDC_VALUE = "http://research.ge.com/kdl/sparqlgraph/externalDataConnection#ExternalDataValue";
	private static final String EDC_SOURCE = "http://research.ge.com/kdl/sparqlgraph/externalDataConnection#ExternalDataSource";
	private static final String UNSET = "DispatchServiceManager.UNSET";
	
	// static cache of EDC Config information
	private static Table edcTypeCache = null;
	private static Table edcParamsCache = null;
	private static Table edcNgRestrictionsCache = null;
	private static Table edcQueryConstraintsCache = null;
	
	private NodeGroup nodegroup = null;
	private NodeGroup edcNodegroup = null;

	private OntologyInfo oInfo = null;
	private String domainDeprecated = null;
	private SparqlEndpointInterface nodegroupSei;
	private SparqlEndpointInterface extConfigSei;

	private boolean heedRestrictions = true;
	
	private String executeClientType = null;
	private String generateClientType = null;
	private ExecuteClientConfig executeClientConfig = null;
	private RestClientConfig generateClientConfig = null;
	
	private String constraintType = UNSET;
	private String constraintVarClassname = null;
	private String constraintVarKeyname = null;

	private Table mneParams = null;
	private Table mneRestrictions = null;
	private String serviceMnemonic = UNSET;
	private String edcSourceClass = null;
	private boolean calculated = false;
	
	/**
	 * Constructor
	 * @param edcQueryClient client used to perform SPARQL queries on the services dataset
	 * @param nodegroup
	 * @param oInfo
	 * @param domain
	 * @param nodegroupSei
	 * @throws Exception
	 */

	@Deprecated
	public DispatchServiceManager(SparqlEndpointInterface extConfigSei, NodeGroup nodegroup, OntologyInfo oInfo, String domainDeprecated, SparqlEndpointInterface nodegroupSei, OntologyInfoClient oInfoClient) throws Exception {
		
		this(extConfigSei, nodegroup, oInfo, nodegroupSei, oInfoClient, true);
	}
	
	public DispatchServiceManager(SparqlEndpointInterface extConfigSei, NodeGroup nodegroup, OntologyInfo oInfo, SparqlEndpointInterface nodegroupSei, OntologyInfoClient oInfoClient) throws Exception {
		
		this(extConfigSei, nodegroup, oInfo, nodegroupSei, oInfoClient, true);
	}
	
	/**
	 * 
	 * @param extConfigSei - edc services sei
	 * @param nodegroup - the nodegroup being dispatched
	 * @param oInfo - oInfo of nodegroup being dispatched
	 * @param domainDeprecated - (deprecated) domain of the query connection
	 * @param nodegroupSei - the nodegroup's default query sei
	 * @param heedRestrictions
	 * @throws Exception
	 */
	public DispatchServiceManager(SparqlEndpointInterface extConfigSei, NodeGroup nodegroup, OntologyInfo oInfo, SparqlEndpointInterface nodegroupSei, OntologyInfoClient oInfoClient, boolean heedRestrictions) throws Exception {
		this.nodegroup = nodegroup;
		this.edcNodegroup = null;
		this.oInfo = oInfo;
		this.domainDeprecated = domainDeprecated;
		this.nodegroupSei = nodegroupSei;
		this.extConfigSei = extConfigSei;
		this.heedRestrictions = heedRestrictions;
		
		if (DispatchServiceManager.edcTypeCache == null) {
			DispatchServiceManager.cacheEdcConfig(new SparqlConnection("servicesgraph", extConfigSei), oInfoClient);
		}
		
		this.calculate();
	}
	
	/**
	 * Query all the edc mnemonic information.
	 * This cache will be used by all instances of DispatchServiceManager
	 * @param servicesGraphConnection
	 * @throws Exception
	 */
	public static void cacheEdcConfig(SparqlConnection servicesGraphConnection, OntologyInfoClient oInfoClient) throws Exception {
		
		try {
			AuthorizationManager.setSemtkSuper();
			
			LocalLogger.logToStdOut("DispatchServiceManager: START querying the services graph for all mnemonic infomation");
			// double-check the services graph connection.  Ontology should be in ModelInterface[0]
			if (servicesGraphConnection.getModelInterfaceCount() != 1) {
				throw new Exception("Edc services connection does not have exactly one model endpoint");
			}
			
			SparqlEndpointInterface sei = servicesGraphConnection.getModelInterface(0);
			
			// load the owl if needed, so that nodegroups will work
		
			StartupUtilities.updateOwlIfNeeded(sei, oInfoClient, DispatchServiceManager.class, "/semantics/OwlModels/sparqlEdcServices.owl");
			
			// run the queries to build static mnemonics cache
			DispatchServiceManager.edcTypeCache = SparqlGraphJson.executeSelectToTable(
					Utility.getResourceAsJson(servicesGraphConnection, "/nodegroups/GetEdcMnemonicInfo.json"), servicesGraphConnection, oInfoClient);
			DispatchServiceManager.edcParamsCache = SparqlGraphJson.executeSelectToTable(
					Utility.getResourceAsJson(servicesGraphConnection, "/nodegroups/GetEdcMnemonicParams.json"), servicesGraphConnection, oInfoClient);
			DispatchServiceManager.edcNgRestrictionsCache = SparqlGraphJson.executeSelectToTable(
					Utility.getResourceAsJson(servicesGraphConnection, "/nodegroups/GetEdcMnemonicNgRestrictions.json"), servicesGraphConnection, oInfoClient);
			DispatchServiceManager.edcQueryConstraintsCache = SparqlGraphJson.executeSelectToTable(
					Utility.getResourceAsJson(servicesGraphConnection, "/nodegroups/GetEdcMnemonicQueryConstraints.json"), servicesGraphConnection, oInfoClient);
			
			LocalLogger.logToStdOut("DispatchServiceManager: FINISHED querying the services graph for all mnemonic infomation");
			
		} finally {
			AuthorizationManager.clearSemtkSuper();
		}
			
	}
	
	/**
	 * Runs all the queries to populate this object.
	 * 
	 * Note that BadQueryExceptions are meant to make it back to the user interface
	 * While others are internal errors.
	 * 
	 * @return mnemonic or null if straight SPARQL
	 * @throws BadQueryException, Exception 
	 */
	private void calculate() throws BadQueryException, Exception {
		
		// return if already calculated
		if (calculated) return;
		calculated = true;
				
		this.calcServiceMnemonic();
		
		if (this.serviceMnemonic != null ) {
			this.calcConstraintType();
			this.calcMnemonicParams();
			this.calcMnemonicRestrictions();
			this.calcClientConfigs();
			this.calcEdcNodegroup();
		} else {
			this.constraintType = null;
			this.constraintVarClassname = null;
			this.constraintVarKeyname = null;
			this.mneParams = null;
			this.mneRestrictions = null;
			this.executeClientConfig = null;
			this.generateClientConfig = null;
			this.edcNodegroup = null;
		}
		
		
		
	}
	
	private SparqlQueryClientConfig calcNodegroupQueryConfig(SparqlQueryClientConfig queryConfig, SparqlEndpointInterface sei) throws Exception {
		SparqlQueryClientConfig config;
		if(queryConfig instanceof SparqlQueryAuthClientConfig){
			config = new SparqlQueryAuthClientConfig(	
					queryConfig.getServiceProtocol(),
					queryConfig.getServiceServer(), 
					queryConfig.getServicePort(), 
					queryConfig.getServiceEndpoint(),
					sei.getServerAndPort(),
					sei.getServerType(),
					sei.getGraph(),
					((SparqlQueryAuthClientConfig)queryConfig).getSparqlServerUser(),
					((SparqlQueryAuthClientConfig)queryConfig).getSparqlServerPassword());
		}else{
			config = new SparqlQueryClientConfig(	
					queryConfig.getServiceProtocol(),
					queryConfig.getServiceServer(), 
					queryConfig.getServicePort(), 
					queryConfig.getServiceEndpoint(),
					sei.getServerAndPort(),
					sei.getServerType(),
					sei.getGraph());
		}
		return config;
	}
	
	
	/**
	 * Create a config for the generate query client
	 * Depends upon serviceMnemonic 
	 * @return
	 * @throws Exception
	 */
	private void calcClientConfigs() throws Exception {
		
		Table tab = DispatchServiceManager.edcTypeCache.getSubsetWhereMatches("mnemonic", this.serviceMnemonic);
		if (tab.getNumRows() == 0) {
			throw new Exception("No generate service found for: " + this.serviceMnemonic);
		} else if (tab.getNumRows() > 1) {
			throw new Exception("Not implemented: multiple generate services found for: " + this.serviceMnemonic);
		}

		this.generateClientType = tab.getCell(0, "genClientType");
		this.generateClientConfig = new QueryGenClientConfig(	
				tab.getCell(0, "genProtocol"),
				tab.getCell(0, "genUrl"),
				tab.getCellAsInt(0, "genPort"),
				tab.getCell(0, "genEndpoint")
				);
		
		this.executeClientType = tab.getCell(0, "exeClientType");
		this.executeClientConfig = new ExecuteClientConfig(	
				tab.getCell(0, "exeProtocol"),
				tab.getCell(0, "exeUrl"),
				tab.getCellAsInt(0, "exePort"),
				tab.getCell(0, "exeEndpoint"),
				null
				);
	}
	
	
	/**
	 * Query for service mnemonic.  ONLY QUERIES ONCE IN THIS OBJECT'S LIFETIME.
	 * @return
	 * @throws Exception
	 */
	private void calcServiceMnemonic() throws Exception {
		
		if (this.serviceMnemonic != null && ! this.serviceMnemonic.equals(DispatchServiceManager.UNSET)) {
			return;
		}
		
		// get trigger nodes
		ArrayList<Node> valNodes = this.nodegroup.getNodesBySuperclassURI(EDC_VALUE, this.oInfo);
		ArrayList<Node> srcNodes = this.nodegroup.getNodesBySuperclassURI(EDC_SOURCE, this.oInfo);
		String valURI = null;
		String srcURI = null;

		// get value URI (only allow 1)
		if(valNodes.size() == 1){
			valURI = valNodes.get(0).getFullUriName(); 
		}else if(valNodes.size() > 1){
			String valNodeURIList = "";
			for(Node valNode : valNodes){
				valNodeURIList += valNode.getUri(true) + ", ";  // local name
			}
			throw new Exception("Nodegroup has more than one EDC value node: " + valNodeURIList.substring(0, valNodeURIList.length()-2));
		}else{ 
			// no value node
			this.serviceMnemonic = null;
			return;
		}
		
		// get source URI, if any (allow 1 or 0)
		if(srcNodes.size() == 1){
			srcURI = srcNodes.get(0).getFullUriName();
		}else if(srcNodes.size() > 1){	
			String srcNodeURIList = "";
			for(Node srcNode : srcNodes){
				srcNodeURIList += srcNode.getUri(true) + ", ";  // local name
			}
			throw new Exception("Nodegroup has more than one EDC source node: " + srcNodeURIList.substring(0, srcNodeURIList.length()-2));
		} 
		
		// get fill list of possible trigger superclasses from the oInfo
		HashSet<String> valClasses = oInfo.getSuperclassNames(valURI);
		valClasses.add(valURI);
		
		HashSet<String> srcClasses = null;
		if (srcURI != null) {
			srcClasses =  oInfo.getSuperclassNames(srcURI);
			srcClasses.add(srcURI);
		}
		
		// extract the serviceMnemonic from the results
		this.serviceMnemonic = null;
		this.edcSourceClass = null;
		int match = -1;
		for (int row = 0; row < DispatchServiceManager.edcTypeCache.getNumRows(); row ++) {
			if (valClasses.contains(DispatchServiceManager.edcTypeCache.getCell(row, "triggerValueClassname"))) {
				if (srcClasses == null || srcClasses.contains(DispatchServiceManager.edcTypeCache.getCell(row, "triggerSourceClassname"))) {
					if (match == -1) {
						this.serviceMnemonic = DispatchServiceManager.edcTypeCache.getCell(row, "mnemonic");
						this.edcSourceClass = DispatchServiceManager.edcTypeCache.getCell(row, "triggerSourceClassname");
					} else {
						throw new Exception("Found multiple matching mnemonics for nodegroup");
					}
				}
			}
		}
	}
	/**
	 * pull constraint type info out of the static cache
	 * @return 
	 * @param mnemonic
	 * @throws Exception
	 */
	private void calcConstraintType() throws Exception {
		
		// return if this was already calculated
		if (this.constraintType == null || !this.constraintType.equals(UNSET)) { return; }
		
		// initialize constraintType
		this.constraintType = null;
		
		// nothing to do
		if (this.serviceMnemonic == null) {
			return;
		}
		// check for programmer error
		if (this.serviceMnemonic.equals(UNSET)) {
			throw new Exception("Error: called calcConstraintType with UNSET mnemonic");
		}
		
		Table tab = DispatchServiceManager.edcQueryConstraintsCache.getSubsetWhereMatches("mnemonic", this.serviceMnemonic);
		if (tab.getNumRows() == 0) {
			this.constraintType = null;
		} else if (tab.getNumRows() > 1) {
			throw new Exception("Not implemented: multiple constraint types found for: " + this.serviceMnemonic);
		} else {
			this.constraintType = tab.getCell(0, "edcConstraintName");
			this.constraintVarClassname = tab.getCell(0, "edcConstraintVarClassname");
			this.constraintVarKeyname = tab.getCell(0, "edcConstraintVarKeyname");
		}
	}
	
	/**
	 * pull mnemonic params out of the static cache
	 * @return table of results
	 * @param mnemonic
	 * @throws Exception
	 */
	private void calcMnemonicParams() throws Exception {
		
		// for compatibility with previous code, build an mneParams table from this.edcTypeCache table.
		this.mneParams = DispatchServiceManager.edcParamsCache.getSubsetWhereMatches(
				"mnemonic", this.serviceMnemonic,
				 new String [] {"paramClassname","keyname", "sparqlId"}
				);
	}
	
	/**
	 * pull mnemonic restrictions out of the static cache
	 * @return table of results
	 * @param mnemonic
	 * @throws Exception
	 */
	private void calcMnemonicRestrictions() throws Exception {
		
		if (! this.heedRestrictions) return;
		
		
		this.mneRestrictions = DispatchServiceManager.edcNgRestrictionsCache.getSubsetWhereMatches(
				"mnemonic", this.serviceMnemonic,
				new String [] {"restrictionClassname","operator","operand"}
				);
				
	}
	
	private void calcEdcNodegroup() throws Exception {
		this.edcNodegroup = NodeGroup.deepCopy(this.nodegroup);
		this.edcNodegroup.inflateAndValidate(this.oInfo);
		this.addReturns(this.edcNodegroup);
		this.removeTriggers(this.edcNodegroup);
		
		this.edcNodegroupCheckRestrictions();
	}
	
	/**
	 * Get nodegroup for filter queries:  edc triggers removed only.
	 * @param mnemonic
	 * @return
	 * @throws Exception
	 */
	public NodeGroup getFilterNodegroup() throws Exception {
		NodeGroup ret = NodeGroup.deepCopy(this.nodegroup);
		this.removeTriggers(ret);
		return ret;
	}
	
	/**
	 * Add in nodes from this.mneParams
	 * @throws Exception
	 */
	private void addReturns(NodeGroup ng) throws Exception {
		
		if (this.mneParams == null) return;
		
		// in rare case that nodegroup does not have the EdcSourceClass in the mneumonic
		// add it so that the returns' paths pass through it
		ng.getOrAddNode(this.edcSourceClass, oInfo, true);
		
		String [] classURI = this.mneParams.getColumn(0);
		String [] keyName  = this.mneParams.getColumn(1);
		String [] sparqlID = this.mneParams.getColumn(2);
		 
		for (int i=0; i < classURI.length; i++) {
			String uri = classURI[i];
			
			// add the node if needed
			Node snode = ng.getOrAddNode(uri, oInfo, true);
			
			//**** Return the class if requested - Java version empty keyName  ****//
			if (keyName[i].length() < 1) {
				// change sparql id if it is wrong
				String targetID = '?' + sparqlID[i];
				if (! snode.getSparqlID().equals(targetID)) {
					String newID = ng.changeSparqlID(snode, targetID);
					if (!newID.equals(targetID)) {
						throw new Exception("Internal error: tried to build sparqlID return '"
								+ targetID
								+ "' but got '"
								+ newID
								+ "'.\nReserved sparqlID may be in use in nodegroup.");
					}
				}
				// set returned: no harm if it is already returned
				snode.setIsReturned(true);
			}
			
			//**** Set properties to return ****//
			else {
				
				// return uri->keyName as "sparqlID"
				
				String targetID = '?' + sparqlID[i];

				// look up the property
				PropertyItem prop = snode.getPropertyByKeyname(keyName[i]);
				if (prop == null) {
					throw new Exception("Internal error in initNodeGroup(): can't find property '"
							+ keyName[i]
							+ "' of '"
							+ classURI[i] + "'");
				}

				// change sparqlID if it is wrong
				if (! prop.getSparqlID().equals(targetID)) {
					String newID = ng.changeSparqlID(prop, targetID);
					if (! newID.equals(targetID)) {
						throw new Exception("Internal error: tried to build sparqlID return '"
								+ targetID
								+ "' but got '"
								+ newID + "'.  \nSame sparqlID is used twice.");
					}
				}

				// set returned: no harm if it is already returned
				prop.setIsReturned(true);
				prop.setOptMinus(PropertyItem.OPT_MINUS_NONE);
			}
			
		}
	}
	
	/** run queries to perform checks on nodegroup and throw Exceptions if fails
	 * 
	 * @throws BadQueryException (check failed), Exception (internal errors)
	 */
	private void edcNodegroupCheckRestrictions() throws BadQueryException, Exception {
		if (this.mneRestrictions == null || ! this.heedRestrictions) return;
		
		String [] classURI = this.mneRestrictions.getColumn(0);
		String [] operator = this.mneRestrictions.getColumn(1);
		String [] operand = this.mneRestrictions.getColumn(2);
		String [] sparqlID = new String[classURI.length];
		
		NodeGroup tmpNodegroup = NodeGroup.deepCopy(edcNodegroup);
		
		// loop through the checks
		for (int i=0; i < classURI.length; i++) {			
			// Find the required class (or it's superclass)
			// If it doesn't exist, add it so we can count how many will come back
			boolean includeSubclasses = true;
			Node snode = tmpNodegroup.getOrAddNode(classURI[i], this.oInfo, includeSubclasses);
			snode.setIsReturned(true);
			sparqlID[i] = snode.getSparqlID();
			
		}
			
		// run the query
		String sparql = tmpNodegroup.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, false, -1, null);
		Table table = this.extConfigSei.executeQueryToTable(sparql);
				
		for (int i=0; i < classURI.length; i++) {			
			int count = table.getColumnUniqueValues(sparqlID[i]).length;
	
			int intOperand = Integer.parseInt(operand[i]);
			String shortURI = new OntologyName(classURI[i]).getLocalName();

			if (operator[i].equals("==")) {

				if (count != intOperand) {
					String msg = String.format("EDC query restriction requires exactly %d values for %s.  %d found.", intOperand, shortURI, count);
					throw new BadQueryException(msg);
				}

			} else if (operator[i].equals("<=")) {

				if (count > intOperand) {
					String msg = String.format("EDC query restriction requires at most %d values for %s.  %d found.", intOperand, shortURI, count);
					throw new BadQueryException(msg);
				}

			} else if (operator[i].equals(">=")) {

				if (count < intOperand) {
					String msg = String.format("EDC query restriction requires at least %d values for %s.  %d found.", intOperand, shortURI, count);
					throw new BadQueryException(msg);
				}
			}
		}
	}
	
	public String [] getAddedSparqlIds() throws Exception {
		return this.mneParams.getColumn("sparqlId");
	}
	
	/**
	 * Calculates the service mnemonic and constraint type if they aren't known yet, and..
	 * @return {String} constraintType
	 * @throws Exception
	 */
	public String getConstraintType() throws Exception {
		this.calcConstraintType();
		return constraintType;
	}

	public boolean isPartOfEDCLocation(String paramSparqlId) throws Exception {
		return this.sparqlIdIsPartOf(paramSparqlId, "http://research.ge.com/kdl/sparqlgraph/externalDataConnection#EDCLocation");
	}
	
	public boolean isPartOfEDCValueGenerator(String paramSparqlId) throws Exception {
		return this.sparqlIdIsPartOf(paramSparqlId, "http://research.ge.com/kdl/sparqlgraph/externalDataConnection#EDCValueGenerator");
	}
	
	/**
	 * Look up a parameter by sparqlID and see if it's class isA className
	 * @param sparqlId
	 * @param className
	 * @return
	 * @throws Exception
	 */
	private boolean sparqlIdIsPartOf(String sparqlId, String className) throws Exception {
		int CLASS_URI = 0;
		int SPARQL_ID = 2;
		OntologyClass classComparedTo = this.oInfo.getClass(className);
		
		for (int i=0; i < this.mneParams.getNumRows(); i++) {
			ArrayList<String> row = this.mneParams.getRow(i);
			if (row.get(SPARQL_ID).equals(sparqlId)) {
				OntologyClass thisClass = this.oInfo.getClass(row.get(CLASS_URI));
				return this.oInfo.classIsA(thisClass, classComparedTo);
			}
		}
		throw new Exception(String.format("Internal Error in DispatchServiceManager.sparqlIdIsPartOf(): %s is not a parameter sparqlId.", sparqlId));
	}
	
	/**
	 * If there is a serviceMnemonic, remove any EDC_VALUE nodes
	 */
	private void removeTriggers(NodeGroup ng) throws Exception {
		if (this.serviceMnemonic == null) { return; }
		
		// sparqledc.js extQueryRemoveTrigger
		ArrayList<Node> triggerNodeList = new ArrayList<Node>();
		OntologyClass edcValue = new OntologyClass(EDC_VALUE);
		
		// First pass: find trigger nodes
		for (Node node : ng.getNodeList()) {
			String uri = node.getFullUriName();
			OntologyClass classCompared = this.oInfo.getClass(uri);
			if (this.oInfo.classIsA(classCompared, edcValue)) {
				triggerNodeList.add(node);
			}
		}
		
		// Second pass: delete trigger nodes
		for (Node node : triggerNodeList) {
			ng.deleteNode(node, false);
		}
		
		// PEC NOTE:  sparqledc.js line 721 looks like we do something silly
		//            with ExternalFiles: ie setIsReturned(false) instead of deleteNode()
		
	}
	
	/**
	 * Make sure service mnemonic is calculated, then return it.
	 * @return service MNEmonic or null if none
	 */
	public String getServiceMnemonic() throws Exception {
		
		this.calcServiceMnemonic();
		return this.serviceMnemonic;
	}
	
	public NodeGroup getEdcNodegroup() throws Exception {
		
		return this.edcNodegroup;
	}
	
	/**
	 * Get all legal constraint variable names for a nodegroup.
	 *     - null if there is no constraint type for this mnemonic
	 *     - might be an empty array
	 * @return String array, possibly empty, or null
	 * @throws Exception on bad query
	 * */
	public String [] getConstraintVariableNames() throws Exception {
		
		if (this.constraintType == null) {
			return null;
		}
		
		
		// find variable name node in the edcNodegroup
		ArrayList<Node> varNodeList = this.edcNodegroup.getNodesBySuperclassURI(this.constraintVarClassname, this.oInfo);
		if (varNodeList.size() < 1) {
			return null;
		} else if (varNodeList.size() > 1) {
			throw new Exception ("Not Implemented: nodegroup has multiple variable nodes: " + this.constraintVarClassname);
		} else {
		
			// find variable name property
			PropertyItem prop = varNodeList.get(0).getPropertyByKeyname(this.constraintVarKeyname);
			if (prop == null) {
				throw new Exception (String.format("Internal error: can't find constraint variable property %s->%s ", 
						                           this.constraintVarClassname, this.constraintVarKeyname));
			}
			
			// execute query with prop as the target prop
			String sparql = this.edcNodegroup.generateSparql(AutoGeneratedQueryTypes.SELECT_DISTINCT, false, null, prop);
			TableResultSet res = (TableResultSet) this.extConfigSei.executeQueryAndBuildResultSet(sparql, SparqlResultTypes.TABLE);
			
			// check the results
			if (res.getSuccess()) {
				return res.getTable().getColumn(0);
			} else {
				throw new Exception ("Internal error: constraint variable names query. " + res.getRationaleAsString("\n"));
			}
		}
		
	}
	
	/**
	 * Get a client to a query generation service (e.g. RDBTimeCoherentTimeSeriesQueryGenClient)
	 */
	public QueryGenClient getGenerateClient() throws Exception {
		Object client;
		try{
			//Class<?> clazz = Class.forName("com.ge.research.semtk.services.client." + this.generateClientType);
			Class<?> clazz = Class.forName(this.generateClientType);
			Constructor<?> ctor = clazz.getConstructor(RestClientConfig.class);
			client = ctor.newInstance(new Object[] { this.generateClientConfig });
		}catch(Exception e){
			throw new Exception("Error instantiating generator client " + this.generateClientType + ": " + e.getMessage());
		}
		return (QueryGenClient) client;
	}
	
	/**
	 * Return a NEW instance of the execute client each time it is called.
	 * thread-safe
	 * @return
	 * @throws Exception 
	 */
	public QueryExecuteClient getExecuteClient(JSONObject configJson, String jobId) throws Exception {
		Object client;		
		try{
			// clone a config object, substituting in configJson
			ExecuteClientConfig config = this.executeClientConfig.clone(configJson);
			
			// get the class of the execute client given type returned by edc query
			Class<?> clazz = Class.forName(this.executeClientType);
			// find the client's constructor which takes an ExecuteClientConfig param
			Constructor<?> ctor = clazz.getConstructor(ExecuteClientConfig.class);
			// instantiate the execute client, with config as the constructor param
			client = ctor.newInstance(new Object[] { config });
			((QueryExecuteClient)client).setJobId(jobId);
		}catch(Exception e){
			throw new Exception("Error instantiating executor client " + this.executeClientType + ": " + e.getMessage());
		}		
		return (QueryExecuteClient) client;
	}
	
	public SparqlEndpointInterface getNodegroupSei() {
		return this.nodegroupSei;
	}
}
