/**
 ** Copyright 2016 General Electric Company
 **
 ** Authors:  Paul Cuddihy, Justin McHugh
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
 *   NOTES:
 *   
 *   Terminology:
 *        "property" belmont nodeItem or propItem
 * 
 *   Interface:
 *        pass & return full URI strings wherever possible
 * 
 * 
 *   TODO: 
 *   	cohesive error handling better than throwing errors to console
 */



define([	// properly require.config'ed   bootstrap-modal
         	'sparqlgraph/js/backcompatutils',        	
        	'sparqlgraph/js/msiclientquery', 
        	'sparqlgraph/js/msiclientontologyinfo', 
        	'sparqlgraph/js/msiresultset', 
        	'sparqlgraph/js/mappingtab',
        	'sparqlgraph/js/semtk_api_import',
        	'sparqlgraph/js/sparqlgraphjson',

			// shimmed
        	'sparqlgraph/js/graphGlue',
	        'sparqlgraph/js/sparqlconnection', 
	        'sparqlgraph/js/sparqlserverinterface', 
	        'sparqlgraph/js/ontologyinfo', 
	        'sparqlgraph/js/belmont', 
		],

	function(BackwardCompatibleUtil, MsiClientQuery, MsiClientOntologyInfo, MsiResultSet, MappingTab, SemtkImportAPI, SparqlGraphJson) {
	
		/**
		 * @description <font color="red">Users of {@link SemtkAPI} should not call this constructor.</font><br>Use {@link semtk_api_loader} instead 
		 * @alias SemtkAPI
		 * @class
		 * @constructor
		 * @param {SemtkAPI~SemtkErrorCallback} optionalFatalErrCallback fatal error callback
		 */
		var SemtkAPI = function(optionalFatalErrCallback) {
			// save error callback. 
			this.errCallback = typeof optionalFatalErrCallback === "undefined" ? null: optionalFatalErrCallback;	
			
			// create invisible canvas for nodegroup dracula
			var elemDiv = document.createElement('div');
			elemDiv.style.display = 'none';
			elemDiv.id = "canvas_dracula";
			document.body.appendChild(elemDiv);
			
			// create empty ontology info
			this.oInfo = null;
			
			// create empty nodegroup
			this.nodegroup = null;
			this.clearNodegroup();
			
			// create an empty connection
			this.conn = new SparqlConnection();
			
			// create an empty MappingTab.  No divs or callbacks.  Just a pass-through for the ImportSpec
			this.mappingTab = new MappingTab();
			
			// clients
			this.queryServiceURL = null;
			this.queryServiceTimeout = null;
			this.dataQueryServiceClient = null;
			
			this.ontologyServiceClient = null;
		};
		
		/**
		 * Also known as SEMTK_ERROR_CALLBACK, this is called for fatal errors
		 * @callback SemtkAPI~SemtkErrorCallback
		 * @param {string} responseMessage A short string message
		 * @return This callback should not return.  It should end by throwing an exception.
		 */
		
		/**
		 * Html error callback explaining the failure
		 * @callback SemtkAPI~HTMLErrorCallback
		 * @param {string} HTMLMessage A message that may have html markup
		 */
		
		/**
		 * String error callback explaining the failure
		 * @callback SemtkAPI~StringErrorCallback
		 * @param {string} message The error message
		 */
		
		/**
		 * Status callback
		 * @callback SemtkAPI~StatusCallback
		 * @param {string} message Short string message for a status bar
		 */
		
		/**
		 * Success callback
		 * @callback SemtkAPI~SuccessCallback
		 */
		
		/**
		 * Success Data callback
		 * @callback SemtkAPI~SuccessDataCallback
		 * @param {data} data
		 */
		
		
		SemtkAPI.prototype = {
			// TODO:  some callbacks process html and some don't.
				
			//=============== Setup ==============	
				
			/**
			 * set the SPARQL Query Service
			 * @param {string}                     serviceURL           full url of SparqlQueryService
			 * @param {SemtkAPI~HTMLErrorCallback} failureHTMLCallback  failure callback
			 * @param {int}                        timeoutMs            timeout in millisec
			 * @return                             none
			 */
			setSparqlQueryService : function(serviceURL, failureHTMLCallback, timeoutMs) {
				this.queryServiceURL = serviceURL;
				this.queryServiceTimeout = timeoutMs;
			},
			
			/**
			 * set the SPARQL Query Service
			 * @param {string}                     serviceURL           full url of OntologyService
			 * @param {SemtkAPI~HTMLErrorCallback} failureHTMLCallback  failure callback
			 * @param {int}                        timeoutMs            timeout in millisec
			 * @return                             none
			 */
			setOntologyService : function(serviceURL, failureHTMLCallback, timeoutMs) {
				// assert
				this.assert(this.conn != null, "setOntologyService", "Model connection must be set first.");
				this.ontologyServiceClient = new MsiClientOntologyInfo(serviceURL, failureHTMLCallback, timeoutMs);
			},
				
			//=============== Nodegroup ==============
			
			/**
			 * Load everything from a nodegroup JSON 
			 * @param {JSON}                       nodegroupJson JSON representing extended conn/nodegroup/importspec
			 * @param {SemtkAPI~statusCallback}     statusCallback
			 * @param {SemtkAPI~successCallback}    successCallback
			 * @param {SemtkAPI~SemtkErrorCallback} failureCallback
			 */
			loadNodegroupJsonFullAsync : function (nodegroupJson, statusCallback, successCallback, failureCallback) {
				var sgJson = new SparqlGraphJson();
				sgJson.fromJson(nodegroupJson);
				this.conn = sgJson.getSparqlConn();
				
				this.loadModelAsync(statusCallback,
						            this.loadNodegroupJsonFullCallback.bind(this, nodegroupJson, statusCallback, successCallback, failureCallback), 
						            failureCallback);
			},
			
			/**
			 * Finishes the work of loadNodegroupJsonFullAsync<br>
			 * After the model is loaded
			 * @private
			 */
			loadNodegroupJsonFullCallback : function (nodegroupJson, statusCallback, successCallback, failureCallback) {
				statusCallback("loading nodegroup");
				try {	
					this.loadNodegroupJson(nodegroupJson);
				} catch(err) {
					statusCallback("");
					failureCallback("SemtkAPI.loadNodegroupJsonFullAsync: " + err.message);
				}
				statusCallback("");
				successCallback();
			},
			
			/**
			 * Load nodegroup and importspec nodegroupJson<br>
			 * See {@link SemtkAPI#loadNodegroupJsonFullAsync} source code for usage<br>
			 * and modify if you want to change the way connections are handled.<br>
			 * <font color="red"><b> 
			 *        Connections must be loaded separately
			 * </font>
			 * @throws error if the model connection doesn't match
			 * @param {JSON} nodegroupJson JSON representing extended conn/nodegroup/importspec
			 * @private
			 */
			loadNodegroupJson : function(nodegroupJson) {
				var sgJson = new SparqlGraphJson();
				
	    		try {
	    			sgJson.fromJson(nodegroupJson);
		    		
	    			var grpJson = sgJson.getSNodeGroupJson();
	    			var importJson = sgJson.getMappingTabJson();
	    			
	    			// nodegroup
	    			this.clearNodegroup();
	    			this.nodegroup.addJson(grpJson, this.oInfo);
	    			
	    			// import spec
	    			this.mappingTab.load(this.nodegroup, importJson);
	    			
	    			// connections must be done separately
		    		
	    		} catch (e) {
	    			this.errCallback("SemtkAPI.loadNodegroupJson: Error loading JSON:\n" + e);
	    		}
			},
			
			/**
			 * @description Get json representing connection, nodegroup, import spec
			 * @returns {JSON}
			 */
			getNodegroupJson : function() {
				var sgJson = new SparqlGraphJson(this.conn, this.nodegroup, this.mappingTab);
				return sgJson.toJson();
			},
				
			/* 
			 * Get nodegroup from nodegroup store
			 */
			retrieveNodegroupFromStore : function(id) {
				
			},
			
			saveNodegroupToStore : function(id, comments) {
				
			},
			
			//=============== Connection ==============
			/* 
			 * Get connection from nodegroup store
			 */
			retrieveConnections : function(id) {
				
			},
			
			/* 
			 * Create a connection.
			 * Internally this loads the oInfo.
			 * type: "virtuoso"
			 * callbacks: accept a single non-html string
			 * optKSUrl: for SADL server.  omit it.
			 */
			
			/**
			 * set up SPARQL connection fields common to both the model and data connections
			 * @param {string} name    a name for the connection         
			 * @param {string} type    typically "virtuoso"              
			 * @param {string} domain  URI prefix considered to be "inside" the model       
			 * @return         none     
			 * 
			 * @deprecated
			 * @example
			 * Replaced by:
			 * setSparqlConnectionName("name");
			 * addSparqlModelGraph("virtuoso", url, dataset, domain, "my fav model");
			 * addSparqlDataGraph("virtuoso", url, dataset, "data #1");
			 * 
			 */
			setupSparqlConnection : function(name, type, domain) {
				throw new Error ("semtk_api.js: using DERECATED setupSparqlConnection().");
			},
			
			
			/**
			 * set up SPARQL model connection 
			 * @par^am {string} url      server holding the data         
			 * @param {string} dataset  depending on server type, this is dataset or graph name             
			 * @param {string} optKsUrl for SADLserver, otherwise ignored      
			 * @return         none  
			 * @deprecated   
			 * @example
			 * Replaced by:
			 * setSparqlConnectionName("name");
			 * addSparqlModelGraph("virtuoso", url, dataset, domain, "my fav model");
			 * addSparqlDataGraph("virtuoso", url, dataset, "data #1");    
			 */
			setSparqlModelConnection : function(url, dataset, optKsUrl) {
				throw new Error ("semtk_api.js: using DERECATED setupSparqlConnection().");
			},
			
			/**
			 * set up SPARQL data connection 
			 * @param {string} url      server holding the data         
			 * @param {string} dataset  depending on server type, this is dataset or graph name             
			 * @param {string} optKsUrl for SADLserver, otherwise ignored      
			 * @return         none         
			 * @deprecated
			 */
			setSparqlDataConnection : function(url, dataset, optKSUrl) {
				this.console.log("semtk_api.js: using DERECATED setupSparqlConnection().");
				var ksUrl = typeof optKsUrl === "undefined" ? null : optKsUrl;
				
				// assert
				this.assert(this.queryServiceURL != null, "setSparqlDataConnection", "There is no query service set.");
				this.assert(this.conn.name != "", "setSparqlDataConnection", "Sparql connection has not been set up.");
				
				this.conn.setDataInterface(url, dataset, ksUrl);
				
				this.dataQueryServiceClient = new MsiClientQuery(this.queryServiceURL, this.conn.getDataInterface(), this.raiseError, this.queryServiceTimeout );
			},
			
			/**
			 * @param {string} name    a name for the connection 
			 * @return         void   
			 * @example
			 * semtk.setConnectionInfo("name", "domain");
			 * semtk.addModelGraph("virtuoso", url, dataset);
			 * semtk.addDataGraph("virtuoso", url, dataset);
			 * semtk.addDataGraph("virtuoso", url, dataset);
			 * 
			 */
			setConnectionInfo : function(name, domain) {
				this.conn.setName(name);
				this.conn.setDomain(domain);
			},
			
			/**
			 * add a SPARQL model connection 
			 * @param {string} sType    "virtuoso" or "fuseki"         
			 * @param {string} url      full URL of sparql endpoint            
			 * @param {string} dataset  dataset or graph name     
			 * @return         none  
			 */
			addModelGraph : function(sType, url, dataset) {
				
				// assert
				this.assert(this.queryServiceURL != null, "addModelGraph", "There is no query service set.");
				this.assert(this.conn.getName() != "", "addModelGraph", "Sparql connection has not been set up.");
				
				// fill in ontology fields    
				this.conn.addModelInterface(sType, url, dataset);
				
			},
			
			/**
			 * add a SPARQL data connection 
			 * @param {string} sType    "virtuoso" or "fuseki"         
			 * @param {string} url      full URL of sparql endpoint            
			 * @param {string} dataset  dataset or graph name     
			 * @param {string} name     name for this endpoint     
			 * @return         none  
			 */
			addDataGraph : function(sType, url, dataset) {
				// assert
				this.assert(this.queryServiceURL != null, "addDataGraph", "There is no query service set.");
				this.assert(this.conn.getName() != "", "addDataGraph", "Sparql connection has not been set up.");
				
				this.conn.addDataInterface(sType, url, dataset);
				
				// build a client the first time a data interface is added
				if (this.conn.getDataInterfaceCount() == 1) {
					this.dataQueryServiceClient = new MsiClientQuery(this.queryServiceURL, this.conn.getDataInterface(0), this.raiseError, this.queryServiceTimeout );
				}
			},
			
			/**
			 * Asynchronously load the model using the model connection
			 * @param {SemtkAPI~StatusCallback}      statusCallback  called periodically
			 * @param {SemtkAPI~SuccessCallback}     successCallback called on successful completion
			 * @param {SemtkAPI~StringErrorCallback} failureCallback called on error
			 */
			loadModelAsync : function( statusCallback, successCallback, failureCallback) {		
				// assert
				this.assert(this.queryServiceURL != null, "loadModelAsync", "There is no sparql query servce URL set.");
				
				// refresh and reload the oInfo
				this.oInfo = new OntologyInfo();
				
	    		BackwardCompatibleUtil.loadSparqlConnection(this.oInfo, this.conn, this.queryServiceURL, statusCallback, successCallback, failureCallback);

			},
			
			/* 
			 * Save created connections to an id
			 */
			saveConnection : function(id) {
				
			},
			
			//=============== Visualization JSON ==============
			// PEC HERE commenting
			getModelDrawJSONAsync : function(successCallbackJson) {
				// asserts
				this.assertModelLoaded("getModelDrawJSONAsync");
				if (this.conn.getModelInterfaceCount() != 1) {
					throw new Error("OInfo service does not handle models with multiple connections.");
				}
				
				var mi = this.conn.getModelInterface(0);
				this.ontologyServiceClient.execRetrieveDetailedOntologyInfo(mi.getServerURL(), 
																			this.conn.getDomain(), 
																			mi.getServerType(), 
																			mi.getServerURL(), 
																			this.getModelDrawJSONCallback.bind(this, successCallbackJson)
																			);
				
			},
			
			getModelDrawJSONCallback : function(successCallbackJson, result) {
				if (! result.isSuccess()) {
					// intercept additional failures from a "successful" rest call
					// we've already forced this callback to exist.  
					this.ontologyServiceClient.optFailureCallback(result.getGeneralResultHtml());
					
				} else {
					successCallbackJson(this.ontologyServiceClient.getRetrieveDetailedOntologyInfoSucceeded(result));
				}
				
			},
			
			getNodegroupDrawJSON : function() {
				return this.nodegroup.toJson();
			},
			
			//=============== Import spec ============
			
			/**
			* Get the importSpec API object
			* @returns SemtkImport
			* 
			* @example
			* importApi = semTk.getSemtkImportAPI();
			*/
			getSemtkImportAPI : function() {
				return new SemtkImportAPI(this);
			},
			
			//=============== SPARQL ============
			/**
			 * limit of zero means no limit
			 */
			getSparqlSelect : function(limit) {
				return this.nodegroup.generateSparql(SemanticNodeGroup.QUERY_DISTINCT,
													 false,
													 limit);
			},
			
			getSparqlCount : function() {
				
			},
			
			// PEC TODO table json is neither documented nor abstracted from virtuoso
			
			/**
			 * Run the current node group's select query 
			 * @param {int}                          limit SPARQL query limit.  0 means no limit.
			 * @param {SemtkAPI~SuccessDataCallback} successCallback receives table JSON
			 * @param {SemtkAPI~StringErrorCallback} failureCallback query failed.  Note that internal failures call semtk fatalErrorCallback.
			 */
			executeSparqlSelectAsync(limit, successCallback, failureCallback) {
				this.executeSparqlAsync(this.getSparqlSelect(limit), successCallback, failureCallback);
			},
			
			/**
			 * Query for all existing values of a property, given the rest of the nodegroup and constraints
			 * @param {string} nodeSparqlID          SPARQL Id of an existing node
			 * @param {string} propURI               URI of the node's property
			 * @param {int}                          limit SPARQL query limit.  0 means no limit.
			 * @param {SemtkAPI~SuccessDataCallback} successCallback receives list of strings
			 * @param {SemtkAPI~StringErrorCallback} failureCallback query failed.  Note that internal failures call semtk fatalErrorCallback.
			 */
			getPropertyConstraintValuesAsync(nodeSparqlID, propURI, limit, successCallback, failureCallback) {
				var sNode = this.assertGetNode(nodeSparqlID, "addNodeConnectFrom");
				var propItem = this.assertGetPropItem(nodeSparqlID, propURI, "setPropertyReturned");
				
				// make sure there's a SPARQL ID
				var blankFlag = this.pFillBlankSparqlID(propItem);
				
				// generate SPARQL
				var sparql = this.nodegroup.generateSparql(SemanticNodeGroup.QUERY_CONSTRAINT,
						 									false,
						 									limit,
						 									propItem);
				// remove temp SPARQL ID
				if (blankFlag) { propItem.setSparqlID("");	}
				
				// execute
				this.assertModelAndDataConnected("executeSparqlAsync");
				this.executeSparqlAsync(sparql, this.getPropertyConstraintValuesCallback.bind(this, successCallback), failureCallback);
			},
			
			getPropertyConstraintValuesCallback : function(successCallback, table ) {
				var list = [];
				
				for (var i=0; i < table.row_count; i++) {
					list.push(table.rows[i][0]);
				}
				successCallback(list);
			},
			
			/**
			 * The basic sparql execute function.  Get a table of results.
			 * @param {string} sparql SPARQL select query
			 * @param {SemtkAPI~SuccessDataCallback} successCallback receives table JSON
			 * @param {SemtkAPI~StringErrorCallback} failureCallback query failed.  Note that internal failures call semtk fatalErrorCallback.
			 */
			executeSparqlAsync : function(sparql, successCallback, failureCallback) {
				this.assertModelAndDataConnected("executeSparqlAsync");
				this.dataQueryServiceClient.executeAndParse(sparql, this.executeSparqlCallback.bind(this, successCallback, failureCallback));
			},
			
			executeSparqlCallback : function(successCallback, failureCallback, resultset ) {
				if (!resultset.isSuccess()) {
					failureCallback(resultset.getStatusMessage());
					
				}  else {
					this.assert(resultset.isTableResults(), "executeSparqlAsync", "Internal: query did not return a table");
					successCallback(resultset.getTable());
				}
			},
			
			//=============== Model (oInfo) ============
			/**
			 * Get all classes in the model
			 * @return{list} list of class URIs
			 */
			getClassNames : function() {
				return this.oInfo.getClassNames();
			},
			
			/**
			 * get names of all properties for a class<br><pre>
			 *   - including inherited
			 *   - any range (connections to "nodes" or plain value "properties")
			 * </pre>
			 * @param {string} classURI URI of the class 
			 * @return{list} list of property URIs
			 */
			getAllPropertyNames : function (classURI) {
				// asserts
				this.assertModelLoaded("getAllPropertyNames");
				this.assertValidClassURI(classURI, "getAllPropertyNames");
				
				var oClass = this.oInfo.getClass(classURI);
				var propList = this.oInfo.getInheritedProperties(oClass);
				var ret = [];
				for (var i=0; i < propList.length; i++) {
					ret.push(propList[i].getNameStr());
				}
				return ret;
			},
			
			modelContainsClass: function (classURI) {
				// asserts
				this.assertModelLoaded("modelContainsClass");
				
				return this.oInfo.containsClass(classURI);
			},
			
			classContainsProperty: function (classURI, propertyURI) {
				// asserts
				this.assertModelLoaded("classContainsProperty");
				this.assertValidClassURI(classURI, "classContainsProperty");
				
				var propNames = this.getAllPropertyNames(classURI);
				return (propNames.indexOf(propertyURI) > -1);
			},
			
			
			
			//=============== Manuiplate nodegroup ============
			
			/**
			 * Get all legal values for a property
			 */
			getPropertyLegalValues : function(nodeSparqlId, propURI, limit) {
				
			},
						
			// PEC HERE: test these add functions
			
			/**
			 * Add a new node with no connections
			 * @param {string} classURI URI of the new node
			 * @return {string} SPARQL ID of added node
			 */
			addNodeOrphan : function(classURI) {
				// asserts
				this.assertModelLoaded("addClassOrphan");
				this.assertValidClassURI(classURI, "addClassOrphan");
				
				var sNode = this.nodegroup.returnBelmontSemanticNode(classURI, this.oInfo);
				this.nodegroup.addOneNode(sNode, null, null, null);
				return sNode.getSparqlID();
			},
			
			/**
			 * Add a new node connected from existing node
			 * @param {string} classURI     URI of the new node
			 * @param {string} nodeSparqlID SPARQL id of the existing node
			 * @param {string} propURI      URI of the existing node's property that will link to new node
			 * @return {string}             SPARQL ID of added node
			 */
			addNodeConnectFrom : function (classURI, nodeSparqlId, propURI) {
				// asserts
				this.assertModelLoaded("addNodeConnectFrom");
				this.assertValidClassURI(classURI, "addNodeConnectFrom");
				
				// get nodes
				var sNode = this.assertGetNode(nodeSparqlId, "addNodeConnectFrom");
				var newNode = this.nodegroup.returnBelmontSemanticNode(classURI, this.oInfo);
				
				// check legality
				var nodeItem = this.assertGetSnodeNodeItem(sNode, propURI);
				var range = nodeItem.getUriValueType();
				this.assertClassIsA(classURI, range, "addNodeConnectFrom");
				
				// add
				this.nodegroup.addOneNode(newNode, sNode, null, propURI);
				
				return newNode.getSparqlID();
			},
			
			/**
			 * Add a new node connected to an existing node
			 * @param {string} classURI     URI of the new node
			 * @param {string} propURI      URI of the new node's property that will link to existing node
			 * @param {string} nodeSparqlID SPARQL Id of the existing node
			 * @return {string}             SPARQL Id of added node
			 */
			addNodeConnectTo : function(classURI, propURI, nodeSparqlId) {
				// asserts
				this.assertModelLoaded("addNodeConnectTo");
				this.assertValidClassURI(classURI, "addNodeConnectTo");
				
				// get nodes
				var sNode = this.assertGetNode(nodeSparqlId, "addNodeConnectTo");
				var newNode = this.nodegroup.returnBelmontSemanticNode(classURI, this.oInfo);
				
				// check legality
				var nodeItem = this.assertGetSnodeNodeItem(newNode, propURI);
				var range = nodeItem.getUriValueType();
				this.assertClassIsA(classURI, range, "addNodeConnectTo");
				
				// add
				this.nodegroup.addOneNode(newNode, sNode, propURI, null);
			},
			
			addClassFirstPath : function () {throw "Function changed to addNodeFirstPath"; },
			// sorry Lacksmi -Paul
			
			/**
			 * Add a new node via a shortest path, or no path if nodegroup is empty
			 * @param {string} classURI URI of the new node
			 * @return {string} SPARQL ID of added node, or null if no path is found.
			 */
			addNodeFirstPath : function(classURI) {
				// asserts
				this.assertModelLoaded("addClassFirstPath");
				this.assertValidClassURI(classURI, "addClassFirstPath");
				
				// if there are no nodes yet, just add it.
				if (this.nodegroup.getNodeCount() === 0) {
					sNode = this.nodegroup.addNode(classURI, this.oInfo);
				
				// otherwise add via first path
				} else {
					// optionalFlag is false
					sNode = this.nodegroup.addClassFirstPath(classURI, this.oInfo, this.conn.domain, false)
				}
				
				return (sNode != null) ?  sNode.getSparqlID() : null;
			}, 
			
			/**
			 * Create a new node with the given URI attached via the given path
			 * @param {string} classURI URI of the new node
			 * @param {list}   pathData a value returned by findPathsToAdd(classURI)
			 * @return {string} SPARQL ID of added node
			 */
			addPath : function(classURI, pathData) {
				// unpack pathList
				var sparqlID = pathData[0];
				var pathTriples = pathData[1];
				
				// asserts
				this.assertModelLoaded("addPath");
				this.assertValidClassURI(classURI, "addPath");
				var sNode = this.assertGetNode(sparqlID, "addPath");
				
				// make path object
				var path = new OntologyPath(classURI);
				path.addTriples(pathTriples);
				
				var newNode = this.nodegroup.addPath(path, sNode, this.oInfo, false, false);
				return newNode.getSparqlID();
			},
			
			/**
			 * Find paths to add a new node to the nodegroup
			 * @param {string} classURI URI of the new node
			 * @return {hash}  ret[display_string] = [sparqlID, [[t0, t1, t2], [t3, t4, t5], ... ]] pathData for addClassUsingPath()
			 */
			findPathsToAdd : function(classURI) {
				// asserts
				this.assertModelLoaded("findPathsToAdd");
				this.assertValidClassURI(classURI, "findPathsToAdd");
				
				var ret = {};
				
				// get paths
				var nodeURIs = this.nodegroup.getArrayOfURINames();
				var paths = this.oInfo.findAllPaths(classURI, nodeURIs, this.conn.getDomain());
				
				// build return
				// unfortunately this code is copied from sparqlgraph.js drop()
		  		for (var p=0; p < paths.length; p++) {
		  			// for each instance of the anchor class
		  			var nlist = this.nodegroup.getNodesByURI(paths[p].getAnchorClassName());
		  			for (var n=0; n < nlist.length; n++) {
		  				
		  				var pathStr = genPathString(paths[p], nlist[n], false);
		  				var pathTriples = paths[n].asList();
		  				ret[pathStr] = [nlist[n].getSparqlID(), pathTriples];
		  				
		  				// push it again backwards if it is a special singleLoop
		  				if ( paths[p].isSingleLoop()) {
		  					var pathStr = genPathString(paths[p], nlist[n], true);
		  					var pathTriples = paths[n].asList();
		  					ret[pathStr] = [nlist[n].getSparqlID(), pathTriples];
		  				}
		  			}
		  		}
				
				return ret;
			},
			
			/**
			 * Erase the nodegroup and start over
			 * @return none
			 */
			clearNodegroup : function () {
				
				this.nodegroup = new SemanticNodeGroup(1000, 700, "canvas_dracula");
				this.nodegroup.drawable = false;
			},
			
			/**
			 * Delete a node from the nodegroup
			 * @param {string} nodeSparqlID SPARQL id of the node
			 * @return none
			 */
			deleteNode : function(sparqlID) {
				// asserts
				this.assertModelLoaded("deleteNode");
				
				var sNode = this.nodegroup.getNodeBySparqlID(sparqlID);
				this.assert(sNode != null, "deleteNode", "Nodegroup does not contain node with sparqlID of " + sparqlID);
				this.nodegroup.deleteNode(sNode, false);
			},
			
			
			/**
			 * Given a node's sparqlID and property URI, get type string
			 * @param {string} nodeSparqlID SPARQL id of the node
			 * @param {string} propURI      URI of the node's property
			 * @return {string} type.       e.g. "string" "int" "long" "float" "uri" "date" "dateTime"...
			 */
			getPropertyType : function(nodeSparqlID, propURI) {
				// asserts
				var propItem = this.assertGetPropItem(nodeSparqlID, propURI, "setPropertyReturned");
				
				return propItem.getValueType();
			},
			
			/**
			 * Set a property's constraints to a VALUE clause
			 * @param {string} nodeSparqlID SPARQL id of the node
			 * @param {string} propURI      URI of the node's property
			 * @param {string[]} valList    a list of values
			 * @return none
			 */
			setPropertyValueConstraint(nodeSparqlID, propURI, valList) {
				// asserts
				var propItem = this.assertGetPropItem(nodeSparqlID, propURI, "setPropertyReturned");
				
				var f = new SparqlFormatter();
				var constraintStr = f.buildValueConstraint(propItem, valList);
				
				this.pFillBlankSparqlID(propItem);			
				propItem.setConstraints(constraintStr);	
			},
			
			/**
			 * Set a property's constraints to a FILTER clause
			 * @method
			 * @param {string} nodeSparqlID SPARQL id of the node
			 * @param {string} propURI      URI of the node's property
			 * @param {string} op           arithmetic operator for FILTER
			 * @param {string} val          value for right hand side of operator
			 * @return none
			 */
			setPropertyFilterConstraint(nodeSparqlID, propURI, op, val) {
				// asserts
				var propItem = this.assertGetPropItem(nodeSparqlID, propURI, "setPropertyReturned");
				
				var f = new SparqlFormatter();
				var constraintStr = f.buildFilterConstraint(propItem, op, val);
				
				this.pFillBlankSparqlID(propItem);			
				propItem.setConstraints(constraintStr);	
			},
			
			/**
			 * Set a property's constraints to a SPARQL snippet
			 * @param {string} nodeSparqlID     SPARQL id of the node
			 * @param {string} propURI          URI of the node's property
			 * @param {string} constraintSPARQL SPARQL constraint on the property's value
			 * @return none
			 */
			setPropertySparqlConstraint : function(nodeSparqlID, propURI, constraintSPARQL) {
				// asserts
				var propItem = this.assertGetPropItem(nodeSparqlID, propURI, "setPropertyConstraints");
				
				this.pFillBlankSparqlID(propItem);
				
				propItem.setConstraints(constraintStr);
			},
			
			/**
			 * Given a node's sparqlID and property URI, get constraint <br>
			 * If blank, SPARQL id is first set, based on the property keyname.
			 * @param {string} nodeSparqlID SPARQL id of the node
			 * @param {string} propURI      URI of the node's property
			 * @return {string} constraint, could be ""
			 */
			getPropertyConstraint: function(nodeSparqlID, propURI) {
				// asserts
				var propItem = this.assertGetPropItem(nodeSparqlID, propURI, "setPropertyReturned");
				return propItem.getConstraints();
			},
			
			/**
			 * Given a node's sparqlID and property URI, get SPARQL id <br>
			 * If blank, SPARQL id is first set, based on the property keyname.
			 * @param {string}  nodeSparqlID SPARQL id of the node
			 * @param {string}  propURI URI of the node's property
			 * @return {string} SPARQL id
			 */
			getPropertySparqlID: function(nodeSparqlID, propURI) {
				// asserts
				var propItem = this.assertGetPropItem(nodeSparqlID, propURI, "setPropertyReturned");
				
				this.pFillBlankSparqlID(propItem);
				return propItem.getSparqlID();
			},
			
			/**
			 * Given a node's sparqlID and property URI, set whether the property will be returned by the query
			 * @param {string}  nodeSparqlID SPARQL id of the node
			 * @param {string}  propURI URI of the node's property
			 * @param {boolean} new value for isReturned
			 * @return          none
			 */
			setPropertyReturned : function(nodeSparqlID, propURI, value) {
				// asserts
				var propItem = this.assertGetPropItem(nodeSparqlID, propURI, "setPropertyReturned");
				
				propItem.setIsReturned(value);
				
				this.pFillBlankSparqlID(propItem);
			},
		
			/**
			 * set property's sparqlID to something as close to sparqlID as possible
			 * @param {string}  nodeSparqlID SPARQL id of the node
			 * @param {string}  propURI URI of the node's property
			 * @param {string}  sparqlID proposed SPARQL Id
			 * @return {string} actual SPARQL id applied to the property
			 */
			setPropertySparqlID : function (nodeSparqlID, propURI, sparqlID) {
				// asserts
				var propItem = this.assertGetPropItem(nodeSparqlID, propURI, "setPropertySparqlID");
				
				var validID = this.validateSparqlID(sparqlID);
				propItem.setSparqlID(validID);
				
				return validID;
			},
			
			// ====== meant to be private =======
			
			validateSparqlID(sparqlID) {
				/**
				 * Given a suggestion, return a valid unused sparqlID for this nodegroup
				 */
				
				// asserts
				this.assert(this.nodegroup != null, "validateSparqlID", "Nodegroup has not been initialized.");
						
				var f = new SparqlFormatter();
				var suggestedID = (sparqlID[0] !== "?") ? "?" + sparqlID : suggestedID;
				
				return f.genSparqlID(suggestedID, this.nodegroup.sparqlNameHash);
			},
			
			
			
			pFillBlankSparqlID(item) {
				/**
				 * If an item has a blank sparqlID, fill it with something close to the keyname
				 * Private function with no asserting.
				 * Returns true if it did anything
				 */
				if (item.getSparqlID() == "") {
					// capitalize the keyname
					var keyname = item.getKeyName();
					keyname = keyname[0].toUpperCase() + keyname.slice(1);
					// translate to valid sparqlID
					var validID = this.validateSparqlID(keyname);
					// set
					item.setSparqlID(validID);
					return true;
				} else {
					return false;
				}
			},
			
			
			assertGetNode : function (nodeSparqlID, funcName) {
				var sNode = this.nodegroup.getNodeBySparqlID(nodeSparqlID);
				this.assert(sNode != null, funcName, "Invalid nodeSparqlID: " + nodeSparqlID);
				return sNode;
			},
			
			assertGetPropItem : function (nodeSparqlID, propURI, funcName) {
				/**
				 * Get a property item, doing all the assert-ing
				 * Uses Belmont definition of PropertyItem
				 */
				
				// asserts
				this.assertModelLoaded(funcName);
				
				var sNode = this.assertGetNode(nodeSparqlID, funcName);
				
				var propItem = sNode.getPropertyByURIRelation(propURI);
				this.assert(sNode != null, funcName, "Invalid propURI: " + propURI + " for node " + nodeSparqlID);
				
				return propItem;
			},
			
			assertGetNodeItem : function (nodeSparqlID, propURI, funcName) {
				/**
				 * Get a node item, doing all the assert-ing
				 * Uses Belmont definition of NodeItem
				 */
				
				// asserts
				this.assertModelLoaded(funcName);
				
				var sNode = this.assertGetNode(nodeSparqlID, funcName);
				
				return this.assertGetSnodeNodeItem(sNode, propURI, funcName);
			},
			
			assertGetSnodePropItem : function (sNode, propURI, funcName) {
				/**
				 * Get a sNode's property item.
				 */
				item = sNode.getPropertyByURIRelation(propURI);
				this.assert(item != null, funcName, "Invalid plain property propURI: " + propURI + " for node " + sNode.getSparqlID());
				return item;
			},
			
			assertGetSnodeNodeItem : function (sNode, propURI, funcName) {
				/**
				 * Get a sNode's node item. 
				 */
				item = sNode.getNodeItemByURIConnectBy(propURI);
				this.assert(item != null, funcName, "Invalid node connection property propURI: " + propURI + " for node " + sNode.getSparqlID());
				return item;
			},
			
			raiseError : function (msgString) {
				if (this.errCallback != null) {
					this.errCallback(msgString);
					throw "SemtkAPI fatalErrCallback incorrectly returned.\nIt should have its own \'throw\' or otherwise end the thread.";
					
				} else {
					throw msgString;
				}
			},
			
			assert : function (boolean, funcName, msg) {
				if (! boolean) {
					this.raiseError("SemTk API error in " + funcName + "(): \n" + msg);
				}
			},
			
			assertClassIsA : function (classURI1, classURI2, funcName) {
				this.assertModelLoaded(funcName);
				var c1 = new OntologyClass(classURI1);
				var c2 = new OntologyClass(classURI2);
				this.assert(this.oInfo.classIsA(c1, c2), funcName, classURI1 + " is not a type of " + classURI2);
			},
			
			assertModelAndDataConnected :  function (funcName) {
				this.assertModelLoaded(funcName);
				this.assert(this.conn.dataServierUrl != "", funcName, "Data connection has not been set.");
			},
			
			assertModelLoaded : function (funcName) {
				this.assert(this.oInfo != null, funcName, "Model has not been successfully loaded.");
			},
			
			assertValidClassURI : function (classURI, funcName) {
				this.assert(this.oInfo.containsClass(classURI), funcName, "Invalid classURI: " + classURI);
			}
		
		};
		
		return SemtkAPI;            // return the constructor
	}
);