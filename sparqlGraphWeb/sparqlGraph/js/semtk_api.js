
define([	// properly require.config'ed   bootstrap-modal
        	'sparqlgraph/js/msiclientquery', 
        	
			// shimmed
	        'sparqlgraph/js/sparqlconnection', 
	        'sparqlgraph/js/sparqlserverinterface', 
	        'sparqlgraph/js/ontologyinfo', 
	        'sparqlgraph/js/belmont', 
		],

	function(MsiClientQuery) {

		var SemtkAPI = function() {
			
			// create invisible canvas for nodegroup dracula
			var elemDiv = document.createElement('div');
			elemDiv.style.display = 'none';
			elemDiv.id = "canvas_dracula";
			document.body.appendChild(elemDiv);
			
			// create empty ontology info
			this.oInfo = null;
			
			// create empty nodegroup
			this.nodegroup = new SemanticNodeGroup(1000, 700, "canvas_dracula");
			this.nodegroup.drawable = false;
			
			// create an empty connection
			this.conn = new SparqlConnection();
			
			// clients
			this.queryServiceURL = null;
			this.queryServiceTimeout = null;
		};
		
		SemtkAPI.prototype = {
			// TODO:  some callbacks process html and some don't.
				
			//=============== Services ==============	
			
			/**
			 *  
			 */
			setSparqlQueryService(serviceURL, timeoutMs) {
				this.queryServiceURL = serviceURL;
				this.queryServiceTimeout = timeoutMs;
			},
				
			//=============== Nodegroup ==============
				
			/* 
			 *  SparqlGraph Nodegroup->upload or drop nodegroup file
			 */
			loadSessionFile(nodegroupJSON) {
				
			},
			
			/* 
			 *  SparqlGraph Nodegroup->download 
			 */
			getSessionFile() {
				
			},
				
			/* 
			 * Get nodegroup from nodegroup store
			 */
			retrieveNodegroupFromStore(id) {
				
			},
			
			saveNodegroupToStore(id, comments) {
				
			},
			
			clearNodegroup() {
				
			},
			
			//=============== Connection ==============
			/* 
			 * Get connection from nodegroup store
			 */
			retrieveConnections(id) {
				
			},
			
			/* 
			 * Create a connection.
			 * Internally this loads the oInfo.
			 * type: "virtuoso"
			 * callbacks: accept a single non-html string
			 * optKSUrl: for SADL server.  omit it.
			 */
			setupSparqlConnection(name, type, domain) {
				this.conn.setup(name, type, domain);
			},
			
			/**
			 * statusCallback(statusString)
			 * successCallback() 
			 * failureCallback(failureString)
			 */
			setSparqlModelConnectionAsync(url, dataset, statusCallback, successCallback, failureCallback, optKsUrl) {
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
					this.modelClientOrInterface = new MsiClientQuery(this.queryServiceURL,this.conn.getOntologyInterface(), failureCallback, this.queryServiceTimeout );
				}
				
				// TODO use query client instead of virtuoso
				
				// refresh and reload the oInfo
				this.oInfo = new OntologyInfo();
				this.oInfo.load(this.conn.domain, this.modelClientOrInterface, statusCallback, successCallback, failureCallback);
			},
			
			/* 
			 * Create a connection
			 */
			setSparqlDataConnection(url, dataset, optKSUrl) {
				this.conn.dataServerUrl = url;
				this.conn.dataKsServerURL = optKSUrl;   
				this.conn.dataSourceDataset = dataset;
			},
			
			/* 
			 * Save created connections to an id
			 */
			saveConnection(id) {
				
			},
			
			//=============== Visualization JSON ==============
			
			getModelVizJSON() {
				
			},
			
			getNodegroupVizJSON() {
				
			},
			
			//=============== SPARQL ============
			getSPARQLSelect() {
				
			},
			
			getSPARQLCount() {
				
			},
			
			//=============== Model (oInfo) ============
			getClassNames() {
				return this.oInfo.getClassNames();
			},
			
			//=============== Manuiplate nodegroup ============
			getPropertyValues(nodeSparqlId, propKeyname, limit) {
				
			},
			
			//addNode(classURI, )
		
		};
		
		return SemtkAPI;            // return the constructor
	}
);