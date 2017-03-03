
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

		var SemtkAPI = function() {
			
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
			this.modelClientOrInterface = null;
			
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
				// TODO: error check that this.conn is not null;
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
			setSparqlModelConnectionAsync : function(url, dataset, statusCallback, successCallback, failureCallback, optKsUrl) {
				var ksUrl = typeof optKsUrl === "undefined" ? null : optKsUrl;
				
				// fill in ontology fields    
				this.conn.setOntologyInterface(url, dataset, ksUrl);
				
				// set this modelClientOrInterface
				// if there's a queryServiceURL, create a query client.
				// otherwise go straight to the connection (e.g. virtuoso)
				if (this.queryServiceURL == null) {
					this.modelClientOrInterface = this.conn.getOntologyInterface();
				} else {
					var test_0 = new OntologyInfo();
					this.modelClientOrInterface = new MsiClientQuery(this.queryServiceURL, this.conn.getOntologyInterface(), failureCallback, this.queryServiceTimeout );
				}
								
				// refresh and reload the oInfo
				this.oInfo = new OntologyInfo();
				this.oInfo.load(this.conn.domain, this.modelClientOrInterface, statusCallback, successCallback, failureCallback);
			},
			
			/* 
			 * Create a connection
			 */
			setSparqlDataConnection : function(url, dataset, optKSUrl) {
				this.conn.dataServerUrl = url;
				this.conn.dataKsServerURL = optKSUrl;   
				this.conn.dataSourceDataset = dataset;
			},
			
			/* 
			 * Save created connections to an id
			 */
			saveConnection : function(id) {
				
			},
			
			//=============== Visualization JSON ==============
			
			getModelDrawJSONAsync : function(successCallbackJson) {
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
				var oClass = this.oInfo.getClass(classURI);
				var propList = this.oInfo.getInheritedProperties(oClass);
				var ret = [];
				for (var i=0; i < propList.length; i++) {
					ret.push(propList[i].getNameStr());
				}
				return ret;
			},
			
			modelContainsClass: function (classURI) {
				return this.oInfo.containsClass(classURI);
			},
			
			classContainsProperty: function (classURI, propertyURI) {
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
				var sNode = this.nodegroup.getNodeBySparqlID(nodeSparqlID);
				var propItem = sNode.getPropertyByURIRelation(propURI);
				propItem.setIsReturned(value);
				
				if (propItem.getSparqlID() == "") {
					var keyname = propItem.getKeyName();
					keyname = keyname[0].toUpperCase() + keyname.slice(1);
					this.setPropertySparqlID(nodeSparqlID, propURI, keyname);
				}
			},
			
			/**
			 * return sparqlID, possibly ""
			 */
			getPropertySparqlID : function (nodeSparqlID, propURI) {
				var sNode = this.nodegroup.getNodeBySparqlID(nodeSparqlID);
				var propItem = sNode.getPropertyByURIRelation(propURI);
				return propItem.getSparqlID();
			},
		
			/**
			 * set property's sparqlID to something as close to sparqlID as possible
			 * returns: sparqlID
			 */
			setPropertySparqlID : function (nodeSparqlID, propURI, sparqlID) {
				var sNode = this.nodegroup.getNodeBySparqlID(nodeSparqlID);
				var propItem = sNode.getPropertyByURIRelation(propURI);
				
				var f = new SparqlFormatter();
				// prepend "?"
				var suggestedID = (sparqlID[0] !== "?") ? "?" + sparqlID : suggestedID;
				var newName = f.genSparqlID(suggestedID, this.nodegroup.sparqlNameHash);
				
				propItem.setSparqlID(newName);
				return newName;
			},
		
		};
		
		return SemtkAPI;            // return the constructor
	}
);