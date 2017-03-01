var SemtkAPI = function() {
	this.oInfo = null;
	
	this.nodegroup = new SemanticNodeGroup(0, 0, "noDiv");
	this.nodegroup.drawable = false;
	
	this.conn = new SparqlConnection('{"name": "","type": "","dsURL": "","dsKsURL": "","dsDataset": "","domain": ""}');
	
};

SemtkPI.prototype = {
		
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
	createModelConnection(type, url, dataset, domain, statusCallback, successCallback, failureCallback, optKSUrl) {
		
		// fill in ontology fields
		this.conn.serverType = type;
		this.conn.ontologyServerUrl = url;
		this.conn.ontologyKsServerURL = optKSUrl;   
		this.conn.ontologySourceDataset = dataset;
		this.conn.domain = domain;
		this.createOntologyInterface();
		
		// refresh and reload the oInfo
		this.oInfo = new OntologyInfo();
		this.oInfo.load(this.conn.domain, this.conn.getOntologyInterface, statusCallback, successCallback, failureCallback);
	},
	
	/* 
	 * Create a connection
	 */
	createDataConnection(url, dataset, optKSUrl) {
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
	
	//=============== Manuiplate nodegroup ============
	getPropertyValues(nodeSparqlId, propKeyname, limit) {
		
	},
	
	//addNode(classURI, )

};