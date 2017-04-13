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


   
    var gOTree = null;
    var gOInfo = null;
    var gConn = null;
    var gQueryClient = null;
    var gTimeseriesResults = null;
    var gQueryResults = null;
    
    // PEC TODO: suspicious: are these used?  why?
    var gServerURL = null;
    var gKSURL = null;
    var gSource = null;
    
    var globalModalDialogue = null;
    
    // drag stuff
    var gDragLabel = "hi";
    var gLoadDialog;
    
    var gNodeGroup = null;
    var gOInfoLoadTime = "";

    var gCurrentTab = g.tab.query ;
    
    var SPARQL_LIMIT = 50;
    var gMappingTab = null;
    var gUploadTab = null;
    var gReady = false;
    
    var gAvoidQueryMicroserviceFlag = false;
    
    // READY FUNCTION 
    $('document').ready(function(){
    
    	document.getElementById("upload-tab-but").disabled = true;
    	document.getElementById("mapping-tab-but").disabled = true;
    	
    	// checkBrowser();
    	
    	initDynatree(); 
    	initCanvas();
    	
	    require([ 'sparqlgraph/js/mappingtab',
	              'sparqlgraph/js/uploadtab' ], function (MappingTab, UploadTab) {
	    
	    	console.log(".ready()");
	    	
	    	// create the modal dialogue 
	    	gLoadDialog = new ModalLoadDialog(document, "gLoadDialog");
	    	globalModalDialogue = new ModalDialog(document, "globalModalDialogue");
	    	
	    	 // set up the node group
	        gNodeGroup = new SemanticNodeGroup(1000, 700, 'canvas');
	        gNodeGroup.setAsyncPropEditor(launchPropertyItemDialog);
	        gNodeGroup.setAsyncSNodeEditor(launchSNodeItemDialog);
	        
	    	// load gUploadTab
	    	gUploadTab =  new UploadTab(document.getElementById("uploadtabdiv"), 
	    								document.getElementById("uploadtoolsdiv"), 
	    								doLoadConnection,
	    			                    g.service.ingestion.url,
	    			                    g.service.sparqlQuery.url);
	    	
	    	document.getElementById("upload-tab-but").disabled = false;
	    	
	    	// load gMappingTab
			gMappingTab =  new MappingTab(importoptionsdiv, importcanvasdiv, importcolsdiv, gUploadTab.setDataFile.bind(gUploadTab), logAndAlert );
	    	
	    	document.getElementById("mapping-tab-but").disabled = false;
	    
	    	// get the query local flag gAvoidQueryMicroservice
	        initAvoidQueryMicroservice();
	        // load last connection
			var conn = gLoadDialog.getLastConnectionInvisibly();
			if (conn) {
				doLoadConnection(conn);
			}
			
	    	// SINCE CODE PRE-DATES PROPER USE OF REQUIRE.JS THROUGHOUT...
	    	// gReady is at the end of the ready function
	    	//        and tells us everything is loaded.
	   	    gReady = true;
	   	    console.log("Ready");
	   	    logEvent("SG Page Load");
	   	    
		});
    });
    
    checkBrowser = function() {
     	// Detect Browser
    	var isFirefox = navigator.userAgent.toLowerCase().indexOf('firefox') > -1;
        if (! isFirefox) {
        	logAndAlert("This application uses right-clicks, which may be blocked by this browser.<br>Firefox is recommended.")
        }
    };
    
    initCanvas = function() {
    	$("#canvas").droppable({
    	    hoverClass: "drophover",
    	    addClasses: true,
    	    over: function(event, ui) {
    	      logMsg("droppable.over, %o, %o", event, ui);
    	    },
    	    drop: function(event, ui) {
    	    	// drop nodes onto graph
    	    	
    	    	var gSource = ui.helper.data("dtSourceNode") || ui.draggable;
    			
    		  	// add the node to the canvas
    			var tsk = gOInfo.containsClass(gDragLabel);
    			
    			if ( gOInfo.containsClass(gDragLabel) ){
    				// the class was found. let's use it.
    				var nodelist = gNodeGroup.getArrayOfURINames();
    				var paths = gOInfo.findAllPaths(gDragLabel, nodelist, gConn.getDomain());
    				logEvent("SG Drop Class", "label", gDragLabel);
    				
    				// Add the arbitary 0th path to return.
    				if (paths.length == 0) {
    					gNodeGroup.addNode(gDragLabel, gOInfo);
    			  		gNodeGroup.drawNodes();
    			  		guiGraphNonEmpty();
    			
    				} else {
    					// find possible anchor node(s) for each path
    			  		var pathStrList = [];
    			  		var valList = [];
    			  		
    			  		// for each path
    			  		for (var p=0; p < paths.length; p++) {
    			  			// for each instance of the anchor class
    			  			var nlist = gNodeGroup.getNodesByURI(paths[p].getAnchorClassName());
    			  			for (var n=0; n < nlist.length; n++) {
    			  				
    			  				pathStrList.push(genPathString(paths[p], nlist[n], false));
    			  				valList.push( [paths[p], nlist[n], false ] );
    			  				
    			  				// push it again backwards if it is a special singleLoop
    			  				if ( paths[p].isSingleLoop()) {
    			  					pathStrList.push(genPathString(paths[p], nlist[n], true));
    				  				valList.push( [paths[p], nlist[n], true ] );
    			  				}
    			  			}
    			  		}
    			  		
    			  		if (valList.length > 1) {
    			  			globalModalDialogue.listDialog("Choose the path", "Submit", pathStrList, valList, 0, dropCallback, "90%");
    			  		} else {
    			  			dropCallback(valList[0]);
    			  		}
    				}
    			}
    			else{
    				// not found
    				logAndAlert("Only classes can be dropped on the graph.");
    				
    			}
    	    }
      	});

    };
    
    initDynatree = function() {
    	
        // Attach the dynatree widget to an existing <div id="tree"> element
        // and pass the tree options as an argument to the dynatree() function:
        $("#treeDiv").dynatree({
            onActivate: function(node) {
                // A DynaTreeNode object is passed to the activation handler
                // Note: we also get this event, if persistence is on, and the page is reloaded.
                console.log("You activated " + node.data.title);
            },
            onDblClick: function(node) {
                // A DynaTreeNode object is passed to the activation handler
                // Note: we also get this event, if persistence is on, and the page is reloaded.
                console.log("You double-clicked " + node.data.title);
            },
    		
            dnd: {
            	onDragStart: function(node) {
                /** This function MUST be defined to enable dragging for the tree.
                 *  Return false to cancel dragging of node.
                 */
                	logMsg("tree.onDragStart(%o)", node);
                	console.log("dragging " + gOTree.nodeGetURI(node));
                	gDragLabel = gOTree.nodeGetURI(node);
                	return true;
            	},
            	onDragStop: function(node) {
        			logMsg("tree.onDragStop(%o)", node);
        			console.log("dragging " + gOTree.nodeGetURI(node) + " stopped.");
      			}
            	
        	},	
        	
            
            persist: true,
        });
        gOTree = new OntologyTree($("#treeDiv").dynatree("getTree"));
  	}; 
  	
    // PEC LOGGING
    // temporary logging require.js workaround
    logEvent = function (action, optDetailKey1, optDetailVal1, optDetailKey2, optDetailVal2) { 
    		kdlLogEvent(action, optDetailKey1, optDetailVal1, optDetailKey2, optDetailVal2);
    }
    logAndAlert = function (msgHtml, optTitle) {
    	var title = typeof optTitle === "undefined" ? "Alert" : optTitle
    	kdlLogEvent("SG: alert", "message", msgHtml);
    	   
    	require(['sparqlgraph/js/modaliidx'], 
    	         function (ModalIidx) {
					ModalIidx.alert(title, msgHtml);
				});
    };
    
    logAndThrow = function (msg) {
    		kdlLogAndThrow(msg);
    };
    
    logNewWindow = function (msg) {
    		kdlLogNewWindow(msg);
    };
    
    // application-specific sub-class choosing
    subclassChooserDialog = function (oInfo, classUri, callback) {
    	var subClassUris = [classUri];
    	subClassUris.concat(oInfo.getSubclassNames(classUri));
    	
    	if (subClassUris.length == 1) { 
    		return callback(classUri); 
    		
    	} else {
    		
    	}
    	
    	
    };
    
    // application-specific property editing
    launchPropertyItemDialog = function (propItem, draculaLabel) {
    	require([ 'sparqlgraph/js/modalitemdialog',
	            ], function (ModalItemDialog) {
    		
    		var dialog= new ModalItemDialog(propItem, gNodeGroup, getQueryClientOrInterface(), propertyItemDialogCallback,
    				                        {"draculaLabel" : draculaLabel}
    		                                );
    		dialog.show();
		});
    };
    
    launchSNodeItemDialog = function (snodeItem, draculaLabel) {
    	require([ 'sparqlgraph/js/modalitemdialog',
  	            ], function (ModalItemDialog) {
      		
      		var dialog= new ModalItemDialog(snodeItem, gNodeGroup, getQueryClientOrInterface(), snodeItemDialogCallback,
      				                        {"draculaLabel" : draculaLabel}
      		                                );
      		dialog.show();
  		});
     };
    
    propertyItemDialogCallback = function(propItem, sparqlID, optionalFlag, rtConstrainedFlag, constraintStr, data) {    	
    	// Note: ModalItemDialog validates that sparqlID is legal
    	
    	// update the property
    	propItem.setReturnName(sparqlID);
    	propItem.setIsOptional(optionalFlag);
    	propItem.setIsRuntimeConstrained(rtConstrainedFlag);
    	propItem.setConstraints(constraintStr);
    	
    	// PEC TODO: pass draculaLabel through the dialog
    	displayLabelOptions(data.draculaLabel, propItem.getDisplayOptions());
    };
    
    snodeItemDialogCallback = function(snodeItem, sparqlID, optionalFlag, rtConstrainedFlag, constraintStr, data) {    	
    	// Note: ModalItemDialog validates that sparqlID is legal
    	
    	// don't un-set an SNode's sparqlID
    	if (sparqlID == "") {
    		snodeItem.setIsReturned(false);
    	} else {
    		snodeItem.setSparqlID(sparqlID);
        	snodeItem.setIsReturned(true);
    	}
    	
    	// optional snode, so find nodeItem: optItem
    	var optItem = gNodeGroup.itemGetOptionalItem(snodeItem);
		if (optItem != null) {
			// If optional then set to right direction
			if (optionalFlag) {
				optItem.setIsOptional(  (snodeItem.nodeList.indexOf(optItem) > -1) ? NodeItem.OPTIONAL_REVERSE : NodeItem.OPTIONAL_TRUE);
			// Only enforce the false if INCOMING optional was true
			} else {
				if (gNodeGroup.isIncomingOptional(snodeItem, optItem)) {
					optItem.setIsOptional(NodeItem.OPTIONAL_FALSE);
				}
			}
		}
		
		// runtime constrained
    	snodeItem.setIsRuntimeConstrained(rtConstrainedFlag);

    	// constraints
    	snodeItem.setConstraints(constraintStr);
    	
    	// PEC TODO: pass draculaLabel through the dialog
    	changeLabelText(data.draculaLabel, snodeItem.getSparqlID());
    	displayLabelOptions(data.draculaLabel, snodeItem.getDisplayOptions());
    	gNodeGroup.drawNodes();
    };
    
    downloadFile = function (data, filename) {
    	// build an anchor and click on it
		$('<a>invisible</a>')
			.attr('id','downloadFile')
			.attr('href','data:text/csv;charset=utf8,' + encodeURIComponent(data))
			.attr('download', filename)
			.appendTo('body');
		$('#downloadFile').ready(function() {
			$('#downloadFile').get(0).click();
		});
		
		// remove the evidence
		var parent = document.getElementsByTagName("body")[0];
		var child = document.getElementById("downloadFile");
		parent.removeChild(child);
    };
    
    
    doLoad = function() {
    	logEvent("SG Menu: File->Load");
    	gLoadDialog.loadDialog(gConn, doLoadConnection);
    };
    
    //**** Start new load code *****//
    doLoadOInfoSuccess = function() {
    	// now load gOInfo into gOTree
		gOTree.addOntInfo(gOInfo);
    	gOTree.showAll(); 
	    gOInfoLoadTime = new Date();
		setStatus("");
		guiTreeNonEmpty();
		gNodeGroup.setCanvasOInfo(gOInfo);
		gMappingTab.updateNodegroup(gNodeGroup);
		gUploadTab.setNodeGroup(gConn, gNodeGroup, gMappingTab, gOInfoLoadTime);

		logEvent("SG Load Success");
    };
    
    doLoadFailure = function(msg) {
    	logAndAlert(msg);
    	setStatus("");    		
    	clearTree();
    	gOInfo = new OntologyInfo();
	    gOInfoLoadTime = new Date();
    	
    	gMappingTab.updateNodegroup(gNodeGroup);
		gUploadTab.setNodeGroup(gConn, gNodeGroup, gMappingTab, gOInfoLoadTime);
 		// retains gConn
    };
    
    doLoadConnection = function(connProfile, optCallback) {
    	// Callback from the load dialog
    	var callback = (typeof optCallback === "undefined") ? function(){} : optCallback;
    	
    	require(['sparqlgraph/js/msiclientquery',
    	         'jquery', 
    	         'jsonp'], function(MsiClientQuery) {
    		
    		
	    	// Clean out existing GUI
	    	clearEverything();
	    	
	    	// Get connection info from dialog return value
	    	gConn = connProfile;
	    	gQueryClient =       new MsiClientQuery(g.service.sparqlQuery.url, gConn.getDataInterface());
	    	var ontQueryClient = new MsiClientQuery(g.service.sparqlQuery.url, gConn.getOntologyInterface(), logAndAlert);
	    	
	    	logEvent("SG Loading", "connection", gConn.toString());
    		
	    	// never true any more
	    	// old code which goes straight to triple-store without micro-services
    		if (gAvoidQueryMicroserviceFlag) {
    			gOInfo.loadAsync(gConn, setStatus, function(){doLoadOInfoSuccess(); callback();}, doLoadFailure);
    			
    		} else {
		    	// load ontology via microservice
				gOInfo.load(gConn.getDomain(), ontQueryClient, setStatus, function(){doLoadOInfoSuccess(); callback();}, doLoadFailure);
    		}
    	});
    };
    
    getQueryClientOrInterface = function() {
    	return gAvoidQueryMicroserviceFlag ? gConn.getDataInterface() : gQueryClient;
    };
    
    doQueryLoadFile = function (file) {
    	var r = new FileReader();
    	
    	r.onload = function () {
    		
    		require(['sparqlgraph/js/sparqlgraphjson'], function(SparqlGraphJson) {
				
	    		var sgJson = new SparqlGraphJson();
	    		try {
	    			sgJson.parse(r.result);
	    		} catch (e) {
	    			logAndAlert("Error parsing the JSON sparqlGraph file: \n" + e);
	    			return;
	    		}
	    		
	    		try {
	    			var grpJson = sgJson.getSNodeGroupJson();
	    			var conn = sgJson.getSparqlConn();
	    			var importJson = sgJson.getMappingTabJson();
	    			
	    			
	    			// ask user to confirm if load will cause a connection switch
	    			if (gConn && ! conn.equals(gConn, true)) {
	    				var ans = confirm("Nodegroup is from a different SPARQL connection\n\nDo you want to close the current connection and continue?");
	    				if (! ans) {
	    					return;
	    				}
	    			}
	    			
	    			// if no conn is loaded or something different is loaded then load the connection
	    			// asynchronous.
	    			if (!gConn || ! conn.equals(gConn, true)) {
	    				
	    				var existName = gLoadDialog.connectionIsKnown(conn, true);     // true: make this selected in cookies
	    				if (! existName) {
	    					var ans = confirm("New connection.\n\nDo you want to save it?");
	    					if (ans) {
		    					gLoadDialog.addConnection(conn);
	    					}
	    					
	    				} else {
	    					// conn already exists in cookies.  Use the name in cookies, so we don't get duplicates
	    					conn.name = existName;
	    					gLoadDialog.writeProfiles();    // write so we save this as the selected connection
	    				}
	    				
	    				// now load the right connection, then load the file
	    				doLoadConnection(conn, 
	    								 function (){
	    									doQueryLoadFile2(grpJson, importJson);
	    								 });
	    				
	    				
	    			} else {
			        	
	    				// Go straight to loading the nodegroup, 
	    				// since the conn and oInfo are already OK
			        	doQueryLoadFile2(grpJson, importJson);
	    			}
		    		
	    		} catch (e) {
	    			logAndAlert("Error loading query file onto the canvas:\n" + e);
	    			console.log(e.stack);
	    			clearGraph();
	    		}
    		});
    	};
	    r.readAsText(file);
    	
    };
    /**
     * loads a nodegroup onto the graph
     * @param {JSON} grpJson    node group
     * @param {JSON} importJson import spec
     */
    doQueryLoadFile2 = function(grpJson, importJson) {
    	// by the time this is called, the correct oInfo is loaded.
    	// and the gNodeGroup is empty.
    	
    	clearGraph();
    	logEvent("SG Loaded Nodegroup");
		gNodeGroup.addJson(grpJson, gOInfo); 
		gNodeGroup.drawNodes();
		guiGraphNonEmpty();
		
		gMappingTab.load(gNodeGroup, importJson);
    };
    
    doNodeGroupUploadCallback = function (evt) {
    	// fileInput callback
    	doQueryLoadFile(evt.target.files[0]);
    };
    
    doNodeGroupUpload = function () {
    	// menu pick callback
    	logEvent("SG menu: File->Upload");
		if (gNodeGroup.getNodeCount() > 0) {
    		logAndAlert("Clear the current query before uploading a new one.");
    		
    	} else {
    		
	    	var fileInput = document.getElementById("fileInput");
	    	fileInput.addEventListener('change', doNodeGroupUploadCallback, false);
	    	fileInput.click();
    	}
    };
    
    doNodeGroupDownload = function () {
    	logEvent("SG menu: File->Download");
    	if (gNodeGroup == null || gNodeGroup.getNodeCount() == 0) {
    		logAndAlert("Query canvas is empty.  Nothing to download.");
    		
    	} else {
    		require(['sparqlgraph/js/sparqlgraphjson'], function(SparqlGraphJson) {
    			// make sure importSpec is in sync
    			gMappingTab.updateNodegroup(gNodeGroup);
    			
				var sgJson = new SparqlGraphJson(gConn, gNodeGroup, gMappingTab, true);
	    		
				gMappingTab.setChangedFlag(false);	
	    		downloadFile(sgJson.stringify(), "sparql_graph.json");
    		});
    	}
    };
    
    // ======= drag-and-drop version of query-loading =======
    	
    noOpHandler = function (evt) {
		 evt.stopPropagation();
		 evt.preventDefault();
   	};
   	
   	fileDrop = function (evt) {
   		
   		if (! gReady) {
   			console.log("Ignoring file drop because I'm not ready.");
   			noOpHandler(evt);
   			return;
   		}
   		// drag-and-drop handler for files
   		logEvent("SG Drop Query File");
   		noOpHandler(evt);
   		var files = evt.dataTransfer.files;
   		if (gNodeGroup.getNodeCount() == 0 || confirm("Clearing current query to load new one.")) {
   			
	   		if (files.length == 1 && files[0].name.slice(-5).toLowerCase() == ".json") {
	   			var fname = files[0].name;
	   			doQueryLoadFile(files[0]);
	   		
	   		} else if (files.length != 1) {
	   			logAndAlert("Can't handle drop of " + files.length.toString() + " files.");
	   			
	   		} else {
	   			logAndAlert("Can't handle drop of file with unrecognized filename extenstion:" + files[0].name)
	   		}
    	}
   		
   	};
   	
   	//-----  query directly -----//
	var AQM_COOKIE = "avoidMs";
	doAvoidQueryMicroservice = function(flag) {
		gAvoidQueryMicroserviceFlag = flag;
		var cookies = new CookieManager(document);
		cookies.setCookie(AQM_COOKIE, gAvoidQueryMicroserviceFlag ? "t" : "f");
	};
	
	initAvoidQueryMicroservice = function() {
		var cookies = new CookieManager(document);
		var flag = cookies.getCookie(AQM_COOKIE);
		gAvoidQueryMicroserviceFlag = flag && flag == "t";
		document.getElementById("chkboxAvoidMicroSvc").checked = gAvoidQueryMicroserviceFlag;
	};
	
   	
   	doTest = function () {
   		
	   	 // test move OptionalsUpstream and generateSparql2
   		
	   	 var qElem = document.getElementById("queryText");
	   	 gNodeGroup.expandOptionalSubgraphs();
	     document.getElementById('queryText').value = gNodeGroup.generateSparql(SemanticNodeGroup.QUERY_DISTINCT, false, 50);
	
	     guiQueryNonEmpty();	
   	};
   	
   	doLayout = function() {
   		setStatus("Laying out graph...");
   		gNodeGroup.layouter.layoutLive(gNodeGroup.renderer, setStatus.bind(null, "")); 		
   	};
    
    doTestMsi = function () {
    	require(['sparqlgraph/js/microserviceinterface',
    	         'sparqlgraph/js/msiclientquery',
    	         'sparqlgraph/js/modaliidx'], 
    	         function (MicroServiceInterface, MsiQueryClient, ModalIidx) {
    		
			var successCallback = function (resultSet) { 
				if (! resultSet.isSuccess()) {
					ModalIidx.alert("Service failed", resultSet.getGeneralResultHtml());
				} else {
					ModalIidx.alert("ResultSet", JSON.stringify(resultSet)); 
				}
			};
			
    		var mq = new MsiQueryClient(g.service.sparqlQuery.url, gConn.getDataInterface());
    		mq.execAuthQuery("select ?x ?y ?z where {?x ?y ?z.} limit 10", successCallback);
    	});
    	
    };
    
    // only used for non-microservice code
    // Almost DEPRECATED
    getNamespaceFlag = function () {
		var ret = document.getElementById("namespace").checked? SparqlServerResult.prototype.NAMESPACE_YES: SparqlServerResult.prototype.NAMESPACE_NO;
		// for sparqlgraph we always want raw HTML in the results.  No links or markup, etc.
		return ret + SparqlServerResult.prototype.ESCAPE_HTML;
    };
    
    doUnload = function () {
    	clearEverything();
    	
    	gMappingTab.updateNodegroup(gNodeGroup);
		gUploadTab.setNodeGroup(gConn, gNodeGroup, gMappingTab, gOInfoLoadTime);
    };
    
    doSearch = function() {
    	gOTree.find($("#search").val());
    };
    
    doCollapse = function() {
    	document.getElementById("search").value="";
    	gOTree.collapseAll();
    };
    
    doExpand = function() {
    	gOTree.expandAll();
    };
     
    setStatus = function(msg) {
    	document.getElementById("status").innerHTML= "<font color='red'>" + msg + "</font>";
    };
    
    graphExecute = function() {
    	logEvent("SG Execute");
    	buildQuery();
    	
    	var query = document.getElementById('queryText').value
    	
    	if (query.match("#Error")) {
    		var q = query.split('\n');
    		var msg = "Invalid query:";
    		var i = 0;
    		while (q[i].match("#Error")) {
    			msg += "\n- " + q[i].slice(7);
    			i += 1;
    		}
    		logAndAlert(msg);
    	} else {
    		runQuery();
    	}
    };
    
    buildQuery = function() {
    	logEvent("SG Build");
        var qElem = document.getElementById("queryText");
        document.getElementById('queryText').value = gNodeGroup.generateSparql(SemanticNodeGroup.QUERY_DISTINCT, document.getElementById("optional").checked, SPARQL_LIMIT);

        guiQueryNonEmpty();
        
    };
    buildConstruct = function() {
        var qElem = document.getElementById("queryText");
        document.getElementById('queryText').value = gNodeGroup.generateSparqlConstruct();
		var query = document.getElementById("queryText").value;
   
   		var dataInterface = gConn.getDataInterface();
		   
   		var testInterface = new SparqlServerInterface(SparqlServerInterface.VIRTUOSO_SERVER, dataInterface.serverURL, dataInterface.dataset, SparqlServerInterface.GRAPH_RESULTS);		
		
		testInterface.executeAndParse(query, constructQryCallback);

        guiQueryNonEmpty();    
    };
    
    constructQryCallback = function(qsresult) {
    	// HTML: tell user query is done
		setStatus("");
		
		if (qsresult.isSuccess()) {
			// try drawing the result to the graph
			this.gNodeGroup.clear();
			this.gNodeGroup.fromConstructJson(qsresult.getAtGraph(), gOInfo);
			this.gNodeGroup.drawNodes();
		}
		else {
			logAndAlert(qsresult.getStatusMessage());
		}
    
    
    };
    
    runQuery = function (){
    	var query = document.getElementById("queryText").value;
    	logEvent("SG Run Query", "sparql", query);
    	
		clearResults();

		if (query.length < 1) {
			logAndAlert("Can't run empty query.  Use 'build' button first.");
			
		} else {
			// HTML: tell user query is running
			setStatus("running query...");
			clearResults();
			
			if (gAvoidQueryMicroserviceFlag) {
				/* Old non-microservice code */
				gConn.getDataInterface().executeAndParse(query, runQueryCallback);
		    	
			} else {
				gQueryClient.executeAndParse(query, runQueryCallback);
			}
			
	    	
		}
	};
		
	// The query callback  
	runQueryCallback = function(results) {
	
		// HTML: tell user query is done
		setStatus("");
		
		if (results.isSuccess()) {
   	
			logEvent("SG Display Query Results", "rows", results.getRowCount());
			
			if (gAvoidQueryMicroserviceFlag) {
				/* old non-microservice */
				results.getResultsInDatagridDiv(	document.getElementById("resultsParagraph"), 
													"resultsTableName",
													"resultsTableId",
													"table table-bordered table-condensed", 
													results.getRowCount() > 10, // filter flag
													"gQueryResults",
													null,
													null,
													getNamespaceFlag());
			} 
			else {
			    // new microservice results grid functionality
				results.setLocalUriFlag(! document.getElementById("namespace").checked);
				results.setEscapeHtmlFlag(true);
				results.putTableResultsDatagridInDiv(document.getElementById("resultsParagraph"), "");
			}
			
			guiResultsNonEmpty();
			
			gQueryResults = results;

		}
		else {
			logAndAlert(results.getStatusMessage());
		}
	};
	
	
	// Gui Functions
    // Inform the GUI which sections are empty
    // NOT Nested
    
	guiTreeNonEmpty = function () {
    	document.getElementById("btnTreeExpand").className = "btn";
    	document.getElementById("btnTreeExpand").disabled = false;
    	
    	document.getElementById("btnTreeCollapse").className = "btn";
    	document.getElementById("btnTreeCollapse").disabled = false;

    };
    
    guiTreeEmpty = function () {
    	document.getElementById("btnTreeExpand").className = "btn disabled";
    	document.getElementById("btnTreeExpand").disabled = true;
    	
    	document.getElementById("btnTreeCollapse").className = "btn disabled";
    	document.getElementById("btnTreeCollapse").disabled = true;
    };
   
    guiGraphNonEmpty = function () {
    	document.getElementById("btnLayout").className = "btn";
    	document.getElementById("btnLayout").disabled = false;
    	
    	document.getElementById("btnGraphClear").className = "btn";
    	document.getElementById("btnGraphClear").disabled = false;
    	
    	document.getElementById("btnGraphExecute").className = "btn";
    	document.getElementById("btnGraphExecute").disabled = false;
    	
    	document.getElementById("btnQueryBuild").className = "btn";
    	document.getElementById("btnQueryBuild").disabled = false;

    };
    
    giuGraphEmpty = function () {
    	document.getElementById("btnLayout").className = "btn disabled";
    	document.getElementById("btnLayout").disabled = true;
    	
    	document.getElementById("btnGraphClear").className = "btn disabled";
    	document.getElementById("btnGraphClear").disabled = true;
    	
    	document.getElementById("btnGraphExecute").className = "btn disabled";
    	document.getElementById("btnGraphExecute").disabled = true;
    	
    	document.getElementById("btnQueryBuild").className = "btn disabled";
    	document.getElementById("btnQueryBuild").disabled = true;

    };
    
    guiQueryEmpty = function () {
    	document.getElementById("btnQueryRun").className = "btn disabled";
    	document.getElementById("btnQueryRun").disabled = true;
    };
    
    guiQueryNonEmpty = function () {
    	document.getElementById("btnQueryRun").className = "btn-primary";
    	document.getElementById("btnQueryRun").disabled = false;
    };
    
    guiResultsEmpty = function () {
    	//document.getElementById("btnDownloadCSV").className = "btn disabled";
    	//document.getElementById("btnDownloadCSV").disabled = true;
    };
    
    guiResultsNonEmpty = function () {
    	
    	//document.getElementById("btnDownloadCSV").className = "btn";
    	//document.getElementById("btnDownloadCSV").disabled = false;
    };
	
	// Clear functions
	// NESTED:  Each one clears other things that depend upon it.
	clearResults = function () {
		document.getElementById("resultsParagraph").innerHTML = "<table id='resultsTable'></table>";
		gQueryResults = null;
		gTimeseriesResults = null;
		guiResultsEmpty();
	};
    
	clearQuery = function () {
	 	document.getElementById('queryText').value = "";
	 	clearResults();
	 	guiQueryEmpty();
	};
	
    clearGraph = function () {
    	gNodeGroup.clear();
    	clearQuery();
    	giuGraphEmpty();
    };
    
    clearMappingTab = function () {
       gMappingTab.clear();
    };
    
    clearTree = function () {
    	gOTree.removeAll();
    	clearGraph();
    	guiTreeEmpty();  //guiTreeEmpty();
    	clearMappingTab();

    };
    
    clearEverything = function () {
    	clearTree();
    	gOInfo = new OntologyInfo();
    	gConn = null;
	    gOInfoLoadTime = new Date();
    };
    
		// ===  Tabs ====
		$(function() {
			$( "#tabs" ).tabs({
			    activate: function(event) {
			        // Enable / disable buttons on the navigation bar
			        if (event.currentTarget.id == "anchorTab1") {
			        	tabSparqlGraphActivated();
			        	
			        } else if (event.currentTarget.id == "anchorTab2") {
			        	tabMappingActivated();
			     
				    } else if (event.currentTarget.id == "anchorTab3") {
			        	tabUploadActivated();
			        }
			    }
			});
		});
		
		tabSparqlGraphActivated = function() {
			 gCurrentTab = g.tab.query;
			 this.document.getElementById("query-tab-but").disabled = true;
			 this.document.getElementById("mapping-tab-but").disabled = false;
			 this.document.getElementById("upload-tab-but").disabled = false;

		};
		
		tabMappingActivated = function() {
			gCurrentTab = g.tab.mapping;
			
			this.document.getElementById("query-tab-but").disabled = false;
			this.document.getElementById("mapping-tab-but").disabled = true;
			this.document.getElementById("upload-tab-but").disabled = false;
			
			// PEC TODO: this overwrites everything each time
			gMappingTab.updateNodegroup(gNodeGroup);
		};
		
		tabUploadActivated = function() {
			 gCurrentTab = g.tab.upload;
			 
			 this.document.getElementById("query-tab-but").disabled = false;
			 this.document.getElementById("mapping-tab-but").disabled = false;
			 this.document.getElementById("upload-tab-but").disabled = true;
			 
			 gUploadTab.setNodeGroup(gConn, gNodeGroup, gMappingTab, gOInfoLoadTime);
	
		};