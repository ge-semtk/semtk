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
        	'sparqlgraph/js/msiclientquery', 
        	'sparqlgraph/js/msiclientontologyinfo', 

			// shimmed
	        'sparqlgraph/js/sparqlconnection', 
	        'sparqlgraph/js/sparqlserverinterface', 
	        'sparqlgraph/js/ontologyinfo', 
	        'sparqlgraph/js/belmont', 
		],

	function(MsiClientQuery, MsiClientOntologyInfo) {

		/**
		 *  optionalFatalErrCallback - Accepts a string message.  Should end with a throw statement or otherwise not return.
		 *                             If not specified, a simple throw is used.
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
			this.clearNodegroup();
			
			// create an empty connection
			this.conn = new SparqlConnection();
			
			// clients
			this.queryServiceURL = null;
			this.queryServiceTimeout = null;
			this.modelQueryServiceClient = null;
			this.dataQueryServiceClient = null;
			
			this.ontologyServiceClient = null;
		};
		
		SemtkAPI.prototype = {
			// TODO:  some callbacks process html and some don't.
				
			//=============== Services ==============	
			
			/**
			 *  
			 */
			setSparqlQueryService : function(serviceURL, failureHTMLCallback, timeoutMs) {
				this.queryServiceURL = serviceURL;
				this.queryServiceTimeout = timeoutMs;
			},
			
			setOntologyService : function(serviceURL, failureHTMLCallback, timeoutMs) {
				// assert
				this.assert(this.conn != null, "setOntologyService", "Model connection must be set first.");
				this.ontologyServiceClient = new MsiClientOntologyInfo(serviceURL, failureHTMLCallback, timeoutMs);
			},
				
			//=============== Nodegroup ==============
				
			/* 
			 *  SparqlGraph Nodegroup->upload or drop nodegroup file
			 */
			loadSessionFile : function(nodegroupJSON) {
				
			},
			
			/* 
			 *  SparqlGraph Nodegroup->download 
			 */
			getSessionFile : function() {
				
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
			setupSparqlConnection : function(name, type, domain) {
				this.conn.setup(name, type, domain);
			},
			
			
			/**
			 * statusCallback(statusString)
			 * successCallback() 
			 * failureCallback(failureString)
			 */
			setSparqlModelConnection : function(url, dataset, optKsUrl) {
				var ksUrl = typeof optKsUrl === "undefined" ? null : optKsUrl;
				
				// assert
				this.assert(this.queryServiceURL != null, "setSparqlModelConnection", "There is no query service set.");
				this.assert(this.conn.name != "", "setSparqlModelConnection", "Sparql connection has not been set up.");
				
				// fill in ontology fields    
				this.conn.setOntologyInterface(url, dataset, ksUrl);
				
				this.modelQueryServiceClient = new MsiClientQuery(this.queryServiceURL, this.conn.getOntologyInterface(), this.raiseError, this.queryServiceTimeout );

				
			},
			
			/* 
			 * Create a connection
			 */
			setSparqlDataConnection : function(url, dataset, optKSUrl) {
				var ksUrl = typeof optKsUrl === "undefined" ? null : optKsUrl;
				
				// assert
				this.assert(this.queryServiceURL != null, "setSparqlDataConnection", "There is no query service set.");
				this.assert(this.conn.name != "", "setSparqlDataConnection", "Sparql connection has not been set up.");
				
				this.conn.setDataInterface(url, dataset, ksUrl);
				
				this.dataQueryServiceClient = new MsiClientQuery(this.queryServiceURL, this.conn.getDataInterface(), this.raiseError, this.queryServiceTimeout );
			},
			
			/**
			 * statusCallback(statusString)
			 * successCallback() 
			 * failureCallback(failureString)
			 */
			loadModelAsync : function( statusCallback, successCallback, failureCallback) {		
				// assert
				this.assert(this.modelQueryServiceClient != null, "loadModelAsync", "There is no sparql model connection set.");
				
				// refresh and reload the oInfo
				this.oInfo = new OntologyInfo();
				this.oInfo.load(this.conn.domain, this.modelQueryServiceClient, statusCallback, successCallback, failureCallback);
			},
			
			/* 
			 * Save created connections to an id
			 */
			saveConnection : function(id) {
				
			},
			
			//=============== Visualization JSON ==============
			
			getModelDrawJSONAsync : function(successCallbackJson) {
				// asserts
				this.assertModelLoaded("getModelDrawJSONAsync");
				
				this.ontologyServiceClient.execRetrieveDetailedOntologyInfo(this.conn.ontologySourceDataset, 
																			this.conn.domain, 
																			this.conn.serverType, 
																			this.conn.ontologyServerUrl, 
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
			
			//=============== SPARQL ============
			/**
			 * limit of zero means no limit
			 */
			getSPARQLSelect : function(limit) {
				return this.nodegroup.generateSparql(SemanticNodeGroup.QUERY_DISTINCT,
													 false,
													 limit);
			},
			
			getSPARQLCount : function() {
				
			},
			
			/**
			 * The basic sparql execute function.  Get a table of results.
			 */
			executeSPARQLAsync : function(sparql, successCallback) {
				
			},
			
			//=============== Model (oInfo) ============
			getClassNames : function() {
				return this.oInfo.getClassNames();
			},
			
			/**
			 * get names of all properties
			 *    - including inherited
			 *    - any range (connections to "nodes" or plain value "properties"
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
			
			// TODO:  this next section with classURI, nodeSparqlID, propURI
			//        demonstrates the need for good and consistent error handling
			//        as bad values currently give difficult-to-understand errors thrown 
			//        and not caught; so the thread dies and a message appears on the console.
			
			/**
			 * Add a node with given classURI via a shortest path.
			 * Return: sparqlID or null
			 * Note: if no path is found, that's a failure.
			 */
			addClassFirstPath : function(classURI) {
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
			 * Erase the nodegroup and start over
			 */
			clearNodegroup : function () {
				this.nodegroup = new SemanticNodeGroup(1000, 700, "canvas_dracula");
				this.nodegroup.drawable = false;
			},
			
			/**
			 * Given a node's sparqlID and property URI, set isReturned to true or false
			 */
			setPropertyReturned : function(nodeSparqlID, propURI, value) {
				// asserts
				var propItem = this.assertGetPropItem(nodeSparqlID, propURI, "setPropertyReturned");
				
				propItem.setIsReturned(value);
				
				this.pFillBlankSparqlID(propItem);
			},
			
			/**
			 * Given a node's sparqlID and property URI, set constraint
			 */
			setPropertyConstraints : function(nodeSparqlID, propURI, constraintStr) {
				// asserts
				var propItem = this.assertGetPropItem(nodeSparqlID, propURI, "setPropertyReturned");
				
				propItem.setConstraints(constraintStr);
				
				this.pFillBlankSparqlID(propItem);
				
			},
			
			/**
			 * return sparqlID, possibly ""
			 */
			getPropertySparqlID : function (nodeSparqlID, propURI) {
				// asserts
				var propItem = this.assertGetPropItem(nodeSparqlID, propURI, "getPropertySparqlID");
				
				return propItem.getSparqlID();
			},
		
			/**
			 * set property's sparqlID to something as close to sparqlID as possible
			 * returns: sparqlID
			 */
			setPropertySparqlID : function (nodeSparqlID, propURI, sparqlID) {
				// asserts
				var propItem = this.assertGetPropItem(nodeSparqlID, propURI, "setPropertySparqlID");
				
				var validID = this.validateSparqlID(sparqlID);
				propItem.setSparqlID(validID);
				
				return validID;
			},
			
			/**
			 * Given a suggestion, return a valid unused sparqlID for this nodegroup
			 */
			validateSparqlID(sparqlID) {
				// asserts
				this.assert(this.nodegroup != null, "validateSparqlID", "Nodegroup has not been initialized.");
						
				var f = new SparqlFormatter();
				var suggestedID = (sparqlID[0] !== "?") ? "?" + sparqlID : suggestedID;
				
				return f.genSparqlID(suggestedID, this.nodegroup.sparqlNameHash);
			},
			
			// ====== meant to be private =======
			
			/**
			 * If an item has a blank sparqlID, fill it with something close to the keyname
			 * Private function with no asserting.
			 */
			pFillBlankSparqlID(item) {
				
				if (item.getSparqlID() == "") {
					// capitalize the keyname
					var keyname = item.getKeyName();
					keyname = keyname[0].toUpperCase() + keyname.slice(1);
					// translate to valid sparqlID
					var validID = this.validateSparqlID(keyname);
					// set
					item.setSparqlID(validID);
				}
			},
			
			/**
			 * Get a property item, doing all the assert-ing
			 */
			assertGetPropItem(nodeSparqlID, propURI, funcName) {
				// asserts
				this.assertModelLoaded(funcName);
				
				var sNode = this.nodegroup.getNodeBySparqlID(nodeSparqlID);
				this.assert(sNode != null, funcName, "Invalid nodeSparqlID: " + nodeSparqlID);
				
				var propItem = sNode.getPropertyByURIRelation(propURI);
				this.assert(sNode != null, funcName, "Invalid propURI: " + propURI + " for node " + nodeSparqlID);
				
				return propItem;
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