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
	this.conn = new SparqlConnection('{"name": "","type": "","dsURL": "","dsKsURL": "","dsDataset": "","domain": ""}');
	
};

SemtkAPI.prototype = {
		
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
		// TODO: move callbacks to the constructor
		//       they need to be async (not "alert")
		
		// fill in ontology fields    
		// PEC TODO this is awful
		this.conn.serverType = type;
		this.conn.ontologyServerUrl = url;
		this.conn.ontologyKsServerURL = optKSUrl;   
		this.conn.ontologySourceDataset = dataset;
		this.conn.domain = domain;
		
		// TODO use query client instead of virtuoso
		// refresh and reload the oInfo
		this.oInfo = new OntologyInfo();
		this.oInfo.load(this.conn.domain, this.conn.createOntologyInterface(), statusCallback, successCallback, failureCallback);
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