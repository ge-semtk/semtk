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
        
    // drag stuff
    var gDragLabel = null;
    var gLoadDialog;
    var gStoreDialog = null;
    
    var gNodeGroup = null;
    var gOInfoLoadTime = "";

    var gCurrentTab = g.tab.query ;
    
    var gEditTab = null;
    var gMappingTab = null;
    var gUploadTab = null;
    var gReady = false;

    var gNodeGroupChangedFlag = false;
    var gQueryTextChangedFlag = false;

    var gQueryTypeIndex = 0;   // sel index of QueryType
    var gQuerySource = "SERVICES";

    var RESULTS_MAX_ROWS = 5000; // 5000 sample rows
    var SHORT_TIMEOUT = 5000;    // 5 sec
        
    // READY FUNCTION 
    $('document').ready(function(){
    
    	document.getElementById("upload-tab-but").disabled = true;
    	document.getElementById("mapping-tab-but").disabled = true;
    	
    	// checkBrowser();
    	
    	initDynatree(); 
    	
	    require([ 'sparqlgraph/js/edittab',
                  'sparqlgraph/js/mappingtab',
	              'sparqlgraph/js/modalloaddialog',
                  'sparqlgraph/js/modalstoredialog',
                  'sparqlgraph/js/uploadtab',

                  // shim
                  'sparqlgraph/js/belmont',
                 
	              'local/sparqlgraphlocal'
                ], 
                function (EditTab, MappingTab, ModalLoadDialog, ModalStoreDialog, UploadTab) {
	    
	    	console.log(".ready()");
	    	
	    	// create the modal dialogue 
	    	gLoadDialog = new ModalLoadDialog(document, "gLoadDialog");
	    	
	    	 // set up the node group
	        gNodeGroup = new SemanticNodeGroup(1000, 700, 'canvas');
	        gNodeGroup.setAsyncPropEditor(launchPropertyItemDialog);
	        gNodeGroup.setAsyncSNodeEditor(launchSNodeItemDialog);
            gNodeGroup.setAsyncSNodeRemover(snodeRemover);
	        gNodeGroup.setAsyncLinkBuilder(launchLinkBuilder);
	        gNodeGroup.setAsyncLinkEditor(launchLinkEditor);
            
            // edit tab
            gEditTab = new EditTab(document.getElementById("editTreeDiv"),
                                   document.getElementById("editCanvasDiv"),
                                   document.getElementById("editSearch")
                                  );
            document.getElementById("edit-tab-but").disabled = false;
	        
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
	    
	        // load last connection
			var conn = gLoadDialog.getLastConnectionInvisibly();
			if (conn) {
				doLoadConnection(conn);
			}
			
			// make sure Query Source and Type disables are reset
			onchangeQueryType(); 
			
            gModalStoreDialog = new ModalStoreDialog(localStorage.getItem("SPARQLgraph_user") || "",
                                                     g.service.nodeGroupStore.url); 

            
            // SINCE CODE PRE-DATES PROPER USE OF REQUIRE.JS THROUGHOUT...
	    	// gReady is at the end of the ready function
	    	//        and tells us everything is loaded.
	   	    gReady = true;
	   	    console.log("Ready");
	   	    logEvent("SG Page Load");
		});
    });
    
    var checkBrowser = function() {
     	// Detect Browser
    	//var isFirefox = navigator.userAgent.toLowerCase().indexOf('firefox') > -1;
        //if (! isFirefox) {
        //	logAndAlert("This application uses right-clicks, which may be blocked by this browser.<br>Firefox is recommended.")
        //}
    };
    
    /*
     * Dropped a class onto the canvas
     */
    var dropClass = function (dragLabel, noPathFlag) {
        checkQueryTextUnsavedThen(dropClass1.bind(this, dragLabel, noPathFlag));
    };

    var dropClass1 = function (dragLabel, noPathFlag) {
        logEvent("SG Drop Class", "label", dragLabel);
        
        // add the node to the canvas
        var tsk = gOInfo.containsClass(dragLabel);

        if ( gOInfo.containsClass(dragLabel) ){
            // the class was found. let's use it.
            var nodelist = gNodeGroup.getArrayOfURINames();
            var paths = gOInfo.findAllPaths(dragLabel, nodelist, gConn.getDomain());

            // Handle no paths or shift key during drag: drop node with no connections
            if (noPathFlag || paths.length == 0) {
                gNodeGroup.addNode(dragLabel, gOInfo);
                nodeGroupChanged(true);
                guiGraphNonEmpty();

            } else {
                // find possible anchor node(s) for each path
                // start with disconnected option
                var pathStrList = ["** Disconnected " + dragLabel];
                var valList = [[new OntologyPath(dragLabel), null, false]];

                // for each path
                for (var p=0; p < paths.length; p++) {
                    // for each instance of the anchor class
                    var nlist = gNodeGroup.getNodesByURI(paths[p].getAnchorClassName());
                    for (var n=0; n < nlist.length; n++) {

                        pathStrList.push(paths[p].genPathString(nlist[n], false));
                        valList.push( [paths[p], nlist[n], false ] );

                        // push it again backwards if it is a special singleLoop
                        if ( paths[p].isSingleLoop()) {
                            pathStrList.push(paths[p].genPathString(nlist[n], true));
                            valList.push( [paths[p], nlist[n], true ] );
                        }
                    }
                }
                
                // if choices are more than "** Disconnected" plus one other path...
                if (valList.length > 2) {
                     require([ 'sparqlgraph/js/modaliidx',
                             ], function (ModalIidx) {
                         
                        // offer a choice, defaulting to the shortest non-disconnected path
                        ModalIidx.listDialog("Choose the path", "Submit", pathStrList, valList, 1, dropClassCallback, 80);

                     });
                    
                } else {
                    // automatically add using the only path
                    dropClassCallback(valList[1]);
                }
            }
        }
        else{
            // not found
            logAndAlert("Only classes can be dropped on the graph.");

        }
        
    };

    /**
     * Add a node via path from anchorSNode
     * If there is no anchorSNode then just add path.startClass
     * @param val[] -  [path, anchorSNode, revFlag]
     */
    var dropClassCallback = function(val) {
    	var path = val[0];
    	var anchorNode = val[1];
    	var singleLoopFlag = val[2];
    	
    	if (anchorNode == null) {
    		gNodeGroup.addNode(path.getStartClassName(), gOInfo);
    	} else {
    		gNodeGroup.addPath(path, anchorNode, gOInfo, singleLoopFlag);
    	}
        nodeGroupChanged(true);
      	guiGraphNonEmpty();
    };
    
    var initDynatree = function() {
        
        // set up dropping files
    	var dropbox = document.getElementById("treeCanvasWrapper");
        dropbox.addEventListener("drop",      fileDrop, false);

        // set up dropping from dynatree
        // "real" system drops get "drop" and 
        // dynatree drops are "mouseup" while gDragLabel is set         
        var canvas = document.getElementById("canvas");
        canvas.addEventListener("mouseup", 
                                function(e) {
                                    if (gDragLabel != null) {
                                        dropClass(gDragLabel, e.shiftKey);
                                    }
                                }.bind(this)
                               );

        require([ 'sparqlgraph/dynatree-1.2.5/jquery.dynatree',
                  'sparqlgraph/js/ontologytree',
	            ], function () {
                     
                     

            // Attach the dynatree widget to an existing <div id="tree"> element
            // and pass the tree options as an argument to the dynatree() function:
            $("#treeDiv").dynatree({
                onActivate: function(node) {
                    // A DynaTreeNode object is passed to the activation handler
                    // Note: we also get this event, if persistence is on, and the page is reloaded.
                    // console.log("You activated " + node.data.title);
                },
                onDblClick: function(node) {
                    // A DynaTreeNode object is passed to the activation handler
                    // Note: we also get this event, if persistence is on, and the page is reloaded.
                    // console.log("You double-clicked " + node.data.title);
                },

                dnd: {
                    onDragStart: function(node) {
                    /** This function MUST be defined to enable dragging for the tree.
                     *  Return false to cancel dragging of node.
                     */
                       // console.log("dragging " + gOTree.nodeGetURI(node));
                        gDragLabel = gOTree.nodeGetURI(node);
                        return true;
                    },
                    
                    onDragStop: function(node, x, y, z, aa) {
                       // console.log("dragging " + gOTree.nodeGetURI(node) + " stopped.");
                        gDragLabel = null;
                    }
                },	


                persist: true,
            });
                     
            gOTree = new OntologyTree($("#treeDiv").dynatree("getTree")); 
        
        });   
  	}; 
  	
    // PEC LOGGING
    // temporary logging require.js workaround
  	var logEvent = function (action, optDetailKey1, optDetailVal1, optDetailKey2, optDetailVal2) { 
    		kdlLogEvent(action, optDetailKey1, optDetailVal1, optDetailKey2, optDetailVal2);
    };
    
    var logAndAlert = function (msgHtml, optTitle) {
    	var title = typeof optTitle === "undefined" ? "Alert" : optTitle
    
    	require(['sparqlgraph/js/modaliidx'], 
    	         function (ModalIidx) {
                    // note: ModalIidx.alert() logs with logger
					ModalIidx.alert(title, msgHtml);
				});
    };
    
    var logAndThrow = function (msg) {
    		kdlLogAndThrow(msg);
    };
    
    var logNewWindow = function (msg) {
    		kdlLogNewWindow(msg);
    };
    
    // application-specific sub-class choosing
    var subclassChooserDialog = function (oInfo, classUri, callback) {
    	var subClassUris = [classUri];
    	subClassUris.concat(oInfo.getSubclassNames(classUri));
    	
    	if (subClassUris.length == 1) { 
    		return callback(classUri); 
    		
    	} else {
    		
    	}
    	
    	
    };
    
    // application-specific property editing
    var launchPropertyItemDialog = function (propItem, draculaLabel) {
        checkQueryTextUnsavedThen(launchPropertyItemDialog1.bind(this, propItem, draculaLabel));
    };

    var launchPropertyItemDialog1 = function (propItem, draculaLabel) {
    	require([ 'sparqlgraph/js/modalitemdialog',
	            ], function (ModalItemDialog) {
    		
    		var dialog= new ModalItemDialog(propItem, 
                                            gNodeGroup, 
                                            this.runSuggestValuesQuery.bind(this, g, gConn, gNodeGroup, null, propItem), 
                                            propertyItemDialogCallback,
    				                        {"draculaLabel" : draculaLabel}
    		                                );
    		dialog.show();
		});
    };
    
    var launchLinkBuilder = function(snode, nItem) {
        checkQueryTextUnsavedThen(launchLinkBuilder1.bind(this, snode, nItem));
    };

    var launchLinkBuilder1 = function(snode, nItem) {
		// callback when user clicks on a nodeItem	
    	var rangeStr = nItem.getUriValueType();
    	
    	// find nodes that might connect
    	var targetSNodes = gNodeGroup.getNodesBySuperclassURI(rangeStr, gOInfo);
    	// disqualify nodes already linked
    	var unlinkedTargetSNodes = [null];
    	var unlinkedTargetNames = ["New " + rangeStr + ""];

    	for (var i=0; i < targetSNodes.length; i++) {
    		if (nItem.getSNodes().indexOf(targetSNodes[i]) == -1) {
    			unlinkedTargetNames.push(targetSNodes[i].getSparqlID());
    			unlinkedTargetSNodes.push(targetSNodes[i]);
    		}
    	}
    	
    	// if there are no possible connections, just add a new node and connect.
    	if (unlinkedTargetSNodes.length == 1) {
    		buildLink(snode, nItem, null);			
    	} else {
            require([ 'sparqlgraph/js/modaliidx',
                             ], function (ModalIidx) {
                         
                       ModalIidx.listDialog("Choose node to connect", "Submit", unlinkedTargetNames, unlinkedTargetSNodes, 0, buildLink.bind(this, snode, nItem), 75);

                     });
    	}
	};
	
    var launchLinkEditor = function(snode, nItem, targetSNode, edge) {
        checkQueryTextUnsavedThen(launchLinkEditor1.bind(this, snode, nItem, targetSNode, edge));
    };

    var launchLinkEditor1 = function(snode, nItem, targetSNode, edge) {
		
		require([ 'sparqlgraph/js/modallinkdialog',
		            ], function (ModalLinkDialog) {
	    		
	    		var dialog= new ModalLinkDialog(nItem, snode, targetSNode, gNodeGroup, linkEditorCallback, {"edge" : edge});
	    		dialog.show();
			});
	};
	
	var linkEditorCallback = function(snode, nItem, targetSNode, data, optionalVal, deleteMarkerVal, deleteFlag) {
		
		// optionalFlag
		nItem.setSNodeOptional(targetSNode, optionalVal);
		nItem.setSnodeDeletionMarker(targetSNode, deleteMarkerVal);
		// deleteFlag
		if (deleteFlag) {
			snode.removeLink(nItem, targetSNode);
		} 
		
        nodeGroupChanged(true);
	};
	
    var launchSNodeItemDialog = function (snodeItem, draculaLabel) {
        checkQueryTextUnsavedThen(launchSNodeItemDialog1.bind(this, snodeItem, draculaLabel));
    };

    var launchSNodeItemDialog1 = function (snodeItem, draculaLabel) {
        require([ 'sparqlgraph/js/modalitemdialog',
                ], function (ModalItemDialog) {

            var dialog= new ModalItemDialog(snodeItem, 
                                            gNodeGroup, 
                                            this.runSuggestValuesQuery.bind(this, g, gConn, gNodeGroup, null, snodeItem), 
                                            snodeItemDialogCallback,
                                            {"draculaLabel" : draculaLabel}
                                            );
            dialog.show();
        });
    };

    var snodeRemover = function (snode) {
        checkQueryTextUnsavedThen(snodeRemover1.bind(this, snode));
    };

    var snodeRemover1 = function (snode) {
        snode.removeFromNodeGroup(false);
        gNodeGroup.drawNodes();
        nodeGroupChanged(true)
    };

    /**
	 * Link from snode through it's nItem to rangeSNode
	 * @param snode - starting point
	 * @param nItem - nodeItem
	 * @param rangeSnode - range node, if null then create it
	 */
	var buildLink = function(snode, nItem, rangeSnode) {
		var snodeClass = gOInfo.getClass(snode.fullURIName);
		var domainStr = gOInfo.getInheritedPropertyByKeyname(snodeClass, nItem.getKeyName()).getNameStr();
		
		if (rangeSnode == null) {
			var rangeStr = nItem.getUriValueType();
			var newNode = gNodeGroup.returnBelmontSemanticNode(rangeStr, gOInfo);
			gNodeGroup.addOneNode(newNode, snode, null, domainStr);
		} else {
			snode.setConnection(rangeSnode, domainStr);
		}
        nodeGroupChanged(true);
	};

    var propertyItemDialogCallback = function(propItem, sparqlID, returnFlag, optionalFlag, delMarker, rtConstrainedFlag, constraintStr, data) {    	
        // Note: ModalItemDialog validates that sparqlID is legal

        // update the property
        propItem.setAndReserveSparqlID(sparqlID);
        propItem.setIsReturned(returnFlag);
        propItem.setIsOptional(optionalFlag);
        propItem.setIsRuntimeConstrained(rtConstrainedFlag);
        propItem.setConstraints(constraintStr);
        propItem.setIsMarkedForDeletion(delMarker);

        // PEC TODO: pass draculaLabel through the dialog
        displayLabelOptions(data.draculaLabel, propItem.getDisplayOptions());

        nodeGroupChanged(true);
    };
    
    var snodeItemDialogCallback = function(snodeItem, sparqlID, returnFlag, optionalFlag, delMarker, rtConstrainedFlag, constraintStr, data) {    	
    	// Note: ModalItemDialog validates that sparqlID is legal
    	
    	// don't un-set an SNode's sparqlID
    	if (sparqlID != "") {
    		snodeItem.setSparqlID(sparqlID);
    	}
        snodeItem.setIsReturned(returnFlag);
    	
    	// ignore optionalFlag in sparqlGraph.  It is still used in sparqlForm
		
		// runtime constrained
    	snodeItem.setIsRuntimeConstrained(rtConstrainedFlag);
    	snodeItem.setDeletionMode(delMarker);

    	// constraints
    	snodeItem.setConstraints(constraintStr);
    	
    	// PEC TODO: pass draculaLabel through the dialog
    	changeLabelText(data.draculaLabel, snodeItem.getSparqlID());
    	displayLabelOptions(data.draculaLabel, snodeItem.getDisplayOptions());
        
        nodeGroupChanged(true);
    };
      
    var doLoad = function() {
    	logEvent("SG Menu: File->Load");
    	gLoadDialog.loadDialog(gConn, doLoadConnection);
    };
    
    //**** Start new load code *****//
    var doLoadOInfoSuccess = function() {
    	// now load gOInfo into gOTree
		gOTree.setOInfo(gOInfo);
    	gOTree.showAll(); 
	    gOInfoLoadTime = new Date();
        
        gEditTab.setOInfo(gOInfo);
        gEditTab.draw();
        
		setStatus("");
		guiTreeNonEmpty();
		//gNodeGroup.setCanvasOInfo(gOInfo);
		gMappingTab.updateNodegroup(gNodeGroup);
		gUploadTab.setNodeGroup(gConn, gNodeGroup, gMappingTab, gOInfoLoadTime);

		logEvent("SG Load Success");
    };
    
    var doLoadFailure = function(msg) {
    	require(['sparqlgraph/js/ontologyinfo'], 
   	         function () {
    		
	    	logAndAlert(msg);
	    	setStatus("");    		
	    	clearTree();
	    	gOInfo = new OntologyInfo();
		    gOInfoLoadTime = new Date();
	    	
            gEditTab.setOInfo(gOInfo);
            gEditTab.draw();
	    	gMappingTab.updateNodegroup(gNodeGroup);
			gUploadTab.setNodeGroup(gConn, gNodeGroup, gMappingTab, gOInfoLoadTime);
		
    	});
 		// retains gConn
    };
    
    var doLoadConnection = function(connProfile, optCallback) {
    	// Callback from the load dialog
    	var callback = (typeof optCallback === "undefined") ? function(){} : optCallback;
    	
    	require(['sparqlgraph/js/msiclientquery',
    	         'sparqlgraph/js/backcompatutils',
    	         'jquery', 
    	         'jsonp'], function(MsiClientQuery, BCUtils) {
    		
    		
	    	// Clean out existing GUI
	    	clearEverything();
	    	
	    	// Get connection info from dialog return value
	    	gConn = connProfile;
	    	gNodeGroup.setSparqlConnection(gConn);
	    	
	    	if (gConn != null) {
		    	gQueryClient = new MsiClientQuery(g.service.sparqlQuery.url, gConn.getDefaultQueryInterface());
		    	
		    	logEvent("SG Loading", "connection", gConn.toString());
		    	
		    	// load through query service unless "DIRECT"
		    	var queryServiceUrl = (getQuerySource() == "DIRECT") ? null : g.service.sparqlQuery.url;
		    	
		    	// note: clearEverything creates a new gOInfo
	    		BCUtils.loadSparqlConnection(gOInfo, gConn, queryServiceUrl, setStatus, function(){doLoadOInfoSuccess(); callback();}, doLoadFailure);
	    	}
    	});
    };
    
    var getQueryClientOrInterface = function() {
    	return (getQuerySource() == "DIRECT") ? gConn.getDefaultQueryInterface() : gQueryClient;
    };

    var runSuggestValuesQuery = function(g, conn, ng, rtConstraints, itemOrId, msiOrQsResultCallback, failureCallback, statusCallback) {
        require(['sparqlgraph/js/msiclientnodegroupexec',
                 'sparqlgraph/js/msiclientnodegroupservice'], 
    	            function (MsiClientNodeGroupExec, MsiClientNodeGroupService) {
            
            
            // translate itemOrId into item
            var item;
            if (typeof itemOrId == "string") {
                item = ng.getItemBySparqlID(itemOrId);
            } else {
                item = itemOrId;
            }
            
            // make sure there is a sparqlID
            var runNodegroup;
            var runId;
            // make sure there is a sparqlID
            if (item.getSparqlID() == "") {
                // set it, make a copy, set it back
                item.setSparqlID("SG_runSuggestValuesQuery_Temp");
                runNodegroup = ng.deepCopy();
                runId = item.getSparqlID();
                item.setSparqlID("");
            } else {
                runNodegroup = ng;
                runId = item.getSparqlID();
            }

            
            if (getQuerySource() == "DIRECT") {
                // Generate sparql and run via query interface
                
                // get answer for msiOrQsResultCallback
                var sparqlCallback = function (cn, resCallback, failCallback, sparql) {
                    var ssi = cn.getDefaultQueryInterface();
                    ssi.executeAndParseToSuccess(sparql, resCallback, failCallback );
                    
                }.bind(this, conn, msiOrQsResultCallback, failureCallback);
                
                // generate sparql and send to sparqlCallback
                var ngClient = new MsiClientNodeGroupService(g.service.nodeGroup.url);
                
                // PEC TODO:  Jira PESQS-281   no way to get query with runtime constraints
                ngClient.execAsyncGenerateFilter(runNodegroup, runId, sparqlCallback, failureCallback);
                
            } else {
                // Run nodegroup via Node Group Exec Svc
                var jsonCallback = MsiClientNodeGroupExec.buildFullJsonCallback(msiOrQsResultCallback,
                                                                                 failureCallback,
                                                                                 statusCallback,
                                                                                 g.service.status.url,
                                                                                 g.service.results.url);
                var execClient = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url, SHORT_TIMEOUT);

                execClient.execAsyncDispatchFilterFromNodeGroup(runNodegroup, conn, runId, null, rtConstraints, jsonCallback, failureCallback);

            }
        }); 
    };

    var doQueryLoadFile = function (file) {
    	var r = new FileReader();
    	
    	r.onload = function () {
    			
                checkAnythingUnsavedThen(doQueryLoadJsonStr.bind(this, r.result));
	    		
    	};
	    r.readAsText(file);
    	
    };
    
    var checkAnythingUnsavedThen = function(action, optCancel) {
        var cancel = (typeof optCancel == "undefined") ? function(){} : optCancel;
        checkQueryTextUnsavedThen(checkNodeGroupUnsavedThen.bind(this, action, cancel));
    };

    /*
     * Perform an action that will overwrite nodeGroup
     * so first check that it is saved or ask user to save it
     *
     */
    var checkNodeGroupUnsavedThen = function(action, optCancel) {
        var cancel = (typeof optCancel == "undefined") ? function(){} : optCancel;

        if (gNodeGroup.getNodeCount() > 0 && (gNodeGroupChangedFlag || gMappingTab.getChangedFlag()) ) {
            require(['sparqlgraph/js/modaliidx'], 
                function(ModalIidx) {
                
                ModalIidx.choose("Save your work",
                                     "Changes to the nodegroup have not been saved<br><br>Do you want to download it first?",
                                     ["Cancel", "Discard", "Download"],
                                     [cancel, 
                                      function(){
                                          nodeGroupChanged(false);
                                          action();
                                      },
                                      function(){
                                          doNodeGroupDownload();
                                          nodeGroupChanged(false);
                                          action();
                                      },
                                     ]
                                );
            });
        } else {
            action();
        }
    };

    var checkQueryTextUnsavedThen = function(action, optCancel) {
        var cancel = (typeof optCancel == "undefined") ? function(){} : optCancel;
        if (gQueryTextChangedFlag ) {
            require(['sparqlgraph/js/modaliidx', 
                     'sparqlgraph/js/iidxhelper'], 
                function(ModalIidx, IIDXHelper) {
                
                ModalIidx.choose("Save custom query",
                                     "Edits to the SPARQL have not been saved<br><br>Do you want to download it first?",
                                     ["Cancel", "Discard", "Download"],
                                     [cancel, 
                                      function(){
                                          queryTextChanged(false);
                                          action();
                                      },
                                      function(){
                                          IIDXHelper.downloadFile(document.getElementById('queryText').value, "sparql.txt", "text/csv;charset=utf8");
                                          queryTextChanged(false);
                                          action();
                                      },
                                     ]
                                );
            });
        } else {
            action();
        }
    };

    var doQueryLoadJsonStr = function(jsonStr) {
    	require(['sparqlgraph/js/sparqlgraphjson',
                 'sparqlgraph/js/modaliidx'], 
                function(SparqlGraphJson, ModalIidx) {
			
	    	var sgJson = new SparqlGraphJson();
	    	
			try {
				sgJson.parse(jsonStr);
			} catch (e) {
				logAndAlert("Error parsing the JSON sparqlGraph file: \n" + e);
				return;
			}
			
			try {
				var conn = sgJson.getSparqlConn();
                
			} catch (e) {
				logAndAlert("Error reading connection from JSON file.\n" + e);
				console.log(e.stack);
				clearGraph();
			}	
            
            // no conn provided in json
            if (conn == null) {
                if (!gConn) {
                    ModalIidx.alert("No connection", "This JSON has no connection information.<br>Load a connection first.");
                    clearGraph();
                } else {
                    ModalIidx.alert("No connection", "This JSON has no connection information.<br>Attempting to load against existing connection.");
                    doQueryLoadFile2(sgJson);
                }
            
            // have to choose between old and new connection
            } else if (gConn && ! conn.equals(gConn, true)) {
                ModalIidx.choose("New Connection",
                                 "Nodegroup is from a different SPARQL connection<br><br>Which one do you want to use?",
                                 ["Cancel",     "Keep Current",                     "Load New"],
                                 [function(){}, doQueryLoadFile2.bind(this, sgJson), doQueryLoadConn.bind(this, sgJson, conn)]
                                 );
            
            // use the new connection
            } else if (!gConn) {
                doQueryLoadConn(sgJson, conn);
              
            // keep the old connection
            } else {
                doQueryLoadFile2(sgJson);
            }
		});

    };
    
    /* 
     * loads connection and makes call to load rest of sgJson
     * part of doQueryLoadJsonStr callback chain
     */
    var doQueryLoadConn = function(sgJson, conn) {
    	require(['sparqlgraph/js/sparqlgraphjson',
                 'sparqlgraph/js/modaliidx'], 
                function(SparqlGraphJson, ModalIidx) {
						
            var existName = gLoadDialog.connectionIsKnown(conn, true);     // true: make this selected in cookies
            
            // function pointer for the thing we do next no matter what happens:
            //    doLoadConnection() with doQueryLoadFile2() as the callback
            var doLoadConnectionCall = doLoadConnection.bind(this, conn, doQueryLoadFile2.bind(this, sgJson));
            
            if (! existName) {
                // new connection: ask if user wants to save it locally
                ModalIidx.choose("New Connection",
                                 "Connection is not saved locally.<br><br>Do you want to save it?",
                                 ["Cancel",     "Don't Save",                     "Save"],
                                 [function(){}, 
                                  doLoadConnectionCall,
                                  function(){ gLoadDialog.addConnection(conn); 
                                              doLoadConnectionCall(); 
                                            }
                                 ]
                                );

            } else {
                // conn already exists in cookies.  Use the name in cookies, so we don't get duplicates
                conn.setName(existName);
                gLoadDialog.writeProfiles();    // write so we save this as the selected connection

                // now load the right connection, then load the file
                doLoadConnectionCall();
            }
			
		});

    };

    /**
     * loads a nodegroup and importspec onto the graph
     * part of doQueryLoadJsonStr callback chain
     *
     * @param {JSON} grpJson    node group
     * @param {JSON} importJson import spec
     */
    var doQueryLoadFile2 = function(sgJson) {
    	// by the time this is called, the correct oInfo is loaded.
    	// and the gNodeGroup is empty.
    	clearGraph();
    	logEvent("SG Loaded Nodegroup");
        
    	sgJson.getNodeGroup(gNodeGroup, gOInfo);
	    gNodeGroup.setSparqlConnection(gConn);
        
        drawNodeGroup();
		guiGraphNonEmpty();
        nodeGroupChanged(false);
		
        buildQuery();
        
		gMappingTab.load(gNodeGroup, sgJson.getMappingTabJson());
    };

    var doNodeGroupUpload = function () {
    	// menu pick callback
    	logEvent("SG menu: File->Upload");
		require(['sparqlgraph/js/iidxhelper'], function(IIDXHelper) {
            IIDXHelper.fileDialog(doQueryLoadFile);
        });
    };
    
    var doNodeGroupDownload = function () {
    	logEvent("SG menu: File->Download");
    	if (gNodeGroup == null || gNodeGroup.getNodeCount() == 0) {
    		logAndAlert("Query canvas is empty.  Nothing to download.");
    		
    	} else {
    		require(['sparqlgraph/js/sparqlgraphjson',
                     'sparqlgraph/js/iidxhelper'], 
                    function(SparqlGraphJson, IIDXHelper) {
    			// make sure importSpec is in sync
    			gMappingTab.updateNodegroup(gNodeGroup);
    			
				var sgJson = new SparqlGraphJson(gConn, gNodeGroup, gMappingTab, true);
	    		
	    		IIDXHelper.downloadFile(sgJson.stringify(), "sparql_graph.json", "text/csv;charset=utf8");
                nodeGroupChanged(false);
    		});
    	}
    };
    
    var doMenuQuerySource = function (val) {
        gQuerySource = val;
        
        // grab the <li> and the <icon>
        var svc = document.getElementById("menu-query-services");  // class="disabled" looks sloppy
        var sIcon = svc.getElementsByTagName("i")[0];
        var dir = document.getElementById("menu-query-direct");
        var dIcon = dir.getElementsByTagName("i")[0];

        if (val == "SERVICES") {
            sIcon.className = "icon-circle";
            dIcon.className = "icon-circle-blank";
        } else {
            sIcon.className = "icon-circle-blank";
            dIcon.className = "icon-circle";
        }
        
        guiUpdateGraphRunButton();
    };

    // ======= drag-and-drop version of query-loading =======
    	
    var noOpHandler = function (evt) {
		 evt.stopPropagation();
		 evt.preventDefault();
   	};
   	
   	var fileDrop = function (evt) {
   		
   		if (! gReady) {
   			console.log("Ignoring file drop because I'm not ready.");
   			noOpHandler(evt);
   			return;
   		}
   		// drag-and-drop handler for files
   		logEvent("SG Drop Query File");
   		noOpHandler(evt);
   		var files = evt.dataTransfer.files;
   			
        if (files.length == 1 && files[0].name.slice(-5).toLowerCase() == ".json") {
            var fname = files[0].name;
            doQueryLoadFile(files[0]);

        } else if (files.length != 1) {
            logAndAlert("Can't handle drop of " + files.length.toString() + " files.");

        } else {
            logAndAlert("Can't handle drop of file with unrecognized filename extenstion:" + files[0].name)
        }
    	
   		
   	};

   	var doTest = function () {
        document.getElementById("edit-tab-but").style="";
        document.getElementById("edit-tab-but").disabled=false;
   	};

    var checkServices = function () {
        require(['sparqlgraph/js/microserviceinterface',
                 'sparqlgraph/js/modaliidx'], 
    	         function (MicroServiceInterface, ModalIidx) {
            
            // build div with all service names and ...
            var div = document.createElement("div");
            for (var key in g.service) {
                if (g.service.hasOwnProperty(key)) {
                    div.innerHTML += g.service[key].url + "...</br>";
                }
            }
            
            var m = new ModalIidx();
            m.showOK("Services", div,  function(){});
            
            // callback replaces the line with the service url with a result
            var callback = function(div, url, resultSet_or_html) {
                
                // failure callbacks use a string parameter
                if (typeof(resultSet_or_html) == "string") {
                //    ModalIidx.alert("Microservice Ping Failed", resultSet_or_html)
                    div.innerHTML = div.innerHTML.replace(url + "...", url + ' is <font color="red">down</font>' );
                    
                } else {
                    div.innerHTML = div.innerHTML.replace(url + "...", url + ' is <font color="green">up</font>' );
                }
            }.bind(div);
            
            // launch service for each
            for (var key in g.service) {
                if (g.service.hasOwnProperty(key)) {
                    var msi = new MicroServiceInterface(g.service[key].url);
                    msi.ping(callback.bind(this, div, g.service[key].url),
                             callback.bind(this, div, g.service[key].url), 
                             10000);  // 10 seconds
                }
            }
            
        });
        
    };
   

    var runGraphByQueryType = function (optRtConstraints) {
        // PEC HERE
        var rtConstraints = (typeof optRtConstraints == "undefined") ? null : optRtConstraints;
        
    	require(['sparqlgraph/js/msiclientnodegroupexec'], 
    	         function (MsiClientNodeGroupExec) {
			
            guiDisableAll();
    		var client = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url, SHORT_TIMEOUT);
    		
            var csvJsonCallback = MsiClientNodeGroupExec.buildCsvUrlSampleJsonCallback(RESULTS_MAX_ROWS,
                                                                                     queryTableResCallback,
                                                                                     queryFailureCallback,
                                                                                     setStatusProgressBar.bind(this, "Running Query"),
                                                                                     g.service.status.url,
                                                                                     g.service.results.url);
            switch (getQueryType()) {
			case "SELECT":
                client.execAsyncDispatchSelectFromNodeGroup(gNodeGroup, gConn, null, rtConstraints, csvJsonCallback, queryFailureCallback);
                break;
			case "COUNT" :
                client.execAsyncDispatchCountFromNodeGroup(gNodeGroup, gConn, null, rtConstraints, csvJsonCallback, queryFailureCallback);
                break;
            case "CONSTRUCT":
                alert("not implemented");
                break;
			case "DELETE":
                client.execAsyncDispatchDeleteFromNodeGroup(gNodeGroup, gConn, null, rtConstraints, csvJsonCallback, queryFailureCallback);
                break;
			}
            
    	});
    	
    };

    var runQueryText = function () {
        require(['sparqlgraph/js/msiclientnodegroupexec',
    	         'sparqlgraph/js/modaliidx'], 
    	         function (MsiClientNodeGroupExec, ModalIidx) {
			
    		var client = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url, g.service.status.url, g.service.results.url, SHORT_TIMEOUT);
    		
             var csvJsonCallback = MsiClientNodeGroupExec.buildCsvUrlSampleJsonCallback(RESULTS_MAX_ROWS,
                                                                                      queryTableResCallback,
                                                                                      queryFailureCallback,
                                                                                      setStatusProgressBar.bind(this, "Running Query"),
                                                                                      g.service.status.url,
                                                                                      g.service.results.url);
            
            client.execAsyncDispatchRawSparql(document.getElementById('queryText').value, gConn, csvJsonCallback, queryFailureCallback);

    	});
    };

    var queryFailureCallback = function (html) {
        require(['sparqlgraph/js/modaliidx'], 
                function(ModalIidx) {
            
            ModalIidx.alert("Query Failed", html);
            guiUnDisableAll();
            setStatus("");
        });
    };
    
    /*
     * Results success.  Display them.
     * @private
     */
    var queryTableResCallback = function (csvFilename, fullURL, tableResults) { 
        var headerHtml = "";
        if (tableResults.getRowCount() >= RESULTS_MAX_ROWS) {
            headerHtml = "<span class='label label-warning'>Showing first " + RESULTS_MAX_ROWS.toString() + " rows. </span> ";
        }
        headerHtml += "Full csv: <a href='" + fullURL + "' download>"+ csvFilename + "</a>";
        tableResults.setLocalUriFlag(! getQueryShowNamespace());
        tableResults.setEscapeHtmlFlag(true);
        tableResults.putTableResultsDatagridInDiv(document.getElementById("resultsParagraph"), headerHtml);
        
        guiUnDisableAll();
        guiResultsNonEmpty();
        setStatus("");
    };

   	var doRetrieveFromNGStore = function() {
        // check that nodegroup is saved
        // launch the retrieval dialog
        // callback to the dialog is doQueryLoadJsonStr
        checkAnythingUnsavedThen(
            gModalStoreDialog.launchRetrieveDialog.bind(gModalStoreDialog, doQueryLoadJsonStr)
        );
    };

   	var doDeleteFromNGStore = function() {
        gModalStoreDialog.launchDeleteDialog();
    };
   	
  	var doStoreNodeGroup = function () {
        
        require(['sparqlgraph/js/sparqlgraphjson'], function(SparqlGraphJson) {
            
            gMappingTab.updateNodegroup(gNodeGroup);
        
            // save user when done
            var doneCallback = function () {
                localStorage.setItem("SPARQLgraph_user", gModalStoreDialog.getUser());
            }

            var sgJson = new SparqlGraphJson(gConn, gNodeGroup, gMappingTab, true);
            gModalStoreDialog.launchStoreDialog(sgJson, doneCallback); 
            
        });
        		
  	};
  	
  	var doLayout = function() {
   		setStatus("Laying out graph...");
   		gNodeGroup.layouter.layoutLive(gNodeGroup.renderer, setStatus.bind(null, "")); 		
   	};
    
   	
    
    // only used for non-microservice code
    // Almost DEPRECATED
    var getNamespaceFlag = function () {
		var ret = document.getElementById("SGQueryNamespace").checked? SparqlServerResult.prototype.NAMESPACE_YES: SparqlServerResult.prototype.NAMESPACE_NO;
		// for sparqlgraph we always want raw HTML in the results.  No links or markup, etc.
		return ret + SparqlServerResult.prototype.ESCAPE_HTML;
    };
    
    /** Get query options **/
    
    // returns "SELECT", "COUNT", "CONSTRUCT", or "DELETE"
    var getQueryType = function () {
    	var s = document.getElementById("SGQueryType");
    	return s.options[s.selectedIndex].value;
    };
    
    // returns "DIRECT", or "SERVICES"
    var getQuerySource = function () {
    	return gQuerySource;
    };
    
    var getQueryLimit = function () {
    	// input already guarantees only digits
    	var value = document.getElementById("SGQueryLimit").value;
    	if (value.length == 0) {
    		return  0;
    	} else {
    		return parseInt(value);
    	}
    };
    
    var drawNodeGroup = function () {
        // query limit
        var limit = gNodeGroup.getLimit();
        var elem = document.getElementById("SGQueryLimit");
        elem.value = (limit < 1) ? "" : limit;
        
        // canvas
        gNodeGroup.drawNodes();
    }
    
    var getQueryShowNamespace = function () {
    	return document.getElementById("SGQueryNamespace").checked;
    };
    
    // Set nodeGroupChangedFlag and update GUI for new nodegroup
    var nodeGroupChanged = function(flag) {
        gNodeGroupChangedFlag = flag;
        
        guiUpdateGraphRunButton();
        
        if (flag) {
            gNodeGroup.drawNodes();
            buildQuery();
            
        } else {
            gMappingTab.setChangedFlag(false);	
            queryTextChanged(false);
        }
    };
        
    var queryTextChanged = function(flag) {
        gQueryTextChangedFlag = flag;
    };

    var onkeyupLimit = function () {
        
        // now change it if it's ok
        checkQueryTextUnsavedThen(
            // success
            function(el, l) {
                // get legal new value
                var newLimit = parseInt(document.getElementById("SGQueryLimit").value.replace(/\D/g,''), 10);
                gNodeGroup.setLimit(newLimit);
                nodeGroupChanged(true);
            },
            
            // cancel
            function() {
                // put it back to the old value
                document.getElementById("SGQueryLimit").value = gNodeGroup.getLimit().toString();
            });
        
    };

    var onchangeQueryType = function () {
        
        // verify it's ok to move forward
        checkQueryTextUnsavedThen(
            // success
            function () {
                gQueryTypeIndex = document.getElementById("SGQueryType").selectedIndex;
                onchangeQueryType1();
            },
            
            // cancel
            function() {
                document.getElementById("SGQueryType").selectedIndex = gQueryTypeIndex;
            });
        
    };

    var onchangeQueryType1 = function () {
    	// clear query test
    	document.getElementById('queryText').value = "";
    	
        buildQuery();
    };
    
    // for every key press.  
    // Note that onchange will not work if the user presses a button do delete a node
    var onkeyupQueryText = function () {
        queryTextChanged(true);
    };

    var doUnload = function () {
    	clearEverything();
    	
    	gMappingTab.updateNodegroup(gNodeGroup);
		gUploadTab.setNodeGroup(gConn, gNodeGroup, gMappingTab, gOInfoLoadTime);
    };
    
    var doSearch = function() {
    	gOTree.find(document.getElementById("search").value);
    };
    
    var doCollapse = function() {
    	document.getElementById("search").value="";
    	gOTree.collapseAll();
    };
    
    var doExpand = function() {
    	gOTree.expandAll();
    };
     
    var doQueryDownload = function() {
        var query = document.getElementById('queryText').value;
        
        require(['sparqlgraph/js/iidxhelper'], function(IIDXHelper) {
            IIDXHelper.downloadFile(query, "query.txt", "text/csv;charset=utf8");
        });
        
        queryTextChanged(false);
    };

    var setStatus = function(msg) {
    	document.getElementById("status").innerHTML= "<font color='red'>" + msg + "</font><br>";
    };
    
    var setStatusProgressBar = function(msg, percent) {
		var p = (typeof percent === 'undefined') ? 50 : percent;

		document.getElementById("status").innerHTML = msg
				+ '<div class="progress progress-info progress-striped active"> \n'
				+ '  <div class="bar" style="width: ' + p
				+ '%;"></div></div>';
	};
    
    // PEC TODO
    // build query using javascript
    var buildQueryLocal = function() {
            console.log("sparqlgraph.js: Called DEPRECATED buildQueryLocal");
            logEvent("SG Build Local");
            var sparql = "";
            switch (getQueryType()) {
            case "SELECT":
                sparql = gNodeGroup.generateSparql(SemanticNodeGroup.QUERY_DISTINCT, false, -1);
                break;
            case "COUNT":
                sparql = gNodeGroup.generateSparql(SemanticNodeGroup.QUERY_COUNT, false, -1);
                break;
            case "CONSTRUCT":
                sparql = gNodeGroup.generateSparqlConstruct();
                break;
            case "DELETE":
                sparql = gNodeGroup.generateSparqlDelete("", null);
                break;
            default:
                throw new Error("Unknown query type.");	
            }

            document.getElementById('queryText').value = sparql;
            guiQueryNonEmpty();
    };

    var buildQuery = function() {
        
        if (gNodeGroup.getNodeCount() == 0) {
            document.getElementById('queryText').value = "";
            guiQueryEmpty();
            return;
        }
        
        require(['sparqlgraph/js/msiclientnodegroupservice',
			    	        ], function(MsiClientNodeGroupService) {
		
            logEvent("SG Build");
            var sparql = "";
            var client = new MsiClientNodeGroupService(g.service.nodeGroup.url, buildQueryFailure.bind(this));
            switch (getQueryType()) {
            case "SELECT":
                client.execAsyncGenerateSelect(gNodeGroup, buildQuerySuccess.bind(this), buildQueryFailure.bind(this));
                break;
            case "COUNT":
                client.execAsyncGenerateCountAll(gNodeGroup, buildQuerySuccess.bind(this), buildQueryFailure.bind(this));
                break;
            case "CONSTRUCT":
                client.execAsyncGenerateConstruct(gNodeGroup, buildQuerySuccess.bind(this), buildQueryFailure.bind(this));
                break;
            case "DELETE":
                client.execAsyncGenerateDelete(gNodeGroup, buildQuerySuccess.bind(this), buildQueryFailure.bind(this));
                break;
            default:
                throw new Error("Unknown query type.");	
            }
        });   
    };

    var buildQuerySuccess = function (sparql) {
        document.getElementById('queryText').value = sparql;

        if (sparql.length > 0) {
            guiQueryNonEmpty();
        } else {
            guiQueryEmpty();
        }

    };

    var buildQueryFailure = function (msgHtml, optNoValidSparqlMessage) {
        var sparql = "";
        if (typeof optNoValidSparqlMessage != "undefined") {
            sparql = "#" + optNoValidSparqlMessage.replace(/\n/g, '#');
            
        } else {
            require(['sparqlgraph/js/modaliidx'], 
    	         function (ModalIidx) {
					ModalIidx.alert("Query Generation Failed", msgHtml);
				});
            
        } 
        
        document.getElementById('queryText').value = sparql;
        guiQueryEmpty();
    }
    
    // PEC TODO unused
    var constructQryCallback = function(qsresult) {
    	// HTML: tell user query is done
		setStatus("");
		
		if (qsresult.isSuccess()) {
			// try drawing the result to the graph
			this.gNodeGroup.clear();
            this.gNodeGroup.setSparqlConn(gConn);
            
			this.gNodeGroup.fromConstructJson(qsresult.getAtGraph(), gOInfo);
			nodeGroupChanged(true);
		}
		else {
			logAndAlert(qsresult.getStatusMessage());
		}
    
    
    };
    

    /* 
     * top-level callback to run the nodegroup
     */

    var runGraph = function() {
    	logEvent("SG Run graph");
    	
        // choose how to run the nodegroup
        if (getQuerySource() == "DIRECT") {
            logAndAlert("Attempting direct query of nodegroup.  Button should be disabled.");
            
        } else {
            require(['sparqlgraph/js/msiclientnodegroupservice',
                    ], function(MsiClientNodeGroupService) {
                clearResults();

                var ngsClient = new MsiClientNodeGroupService(g.service.nodeGroup.url, queryFailureCallback);
                ngsClient.execAsyncGetRuntimeConstraints(gNodeGroup, runGraphWithConstraints, queryFailureCallback);
            });
    	}
    };

    var runGraphWithConstraints = function(resultSet) {
        if (resultSet.getRowCount() > 0) {
            require(['sparqlgraph/js/modalruntimeconstraintdialog',
                    ], function(ModalRuntimeConstraintDialog) {
                var dialog = new ModalRuntimeConstraintDialog(this.runSuggestValuesQuery.bind(this, g, gConn, gNodeGroup));
                dialog.launchDialog(resultSet, runGraphByQueryType.bind(this));
                
            });
        } else {
            runGraphByQueryType();
        }
    };

    /* 
     * top-level call back to execute raw SPARQL
     */
    var runQuery = function () {
    	
    	var query = document.getElementById("queryText").value;
    	
		if (query.length < 1) {
			logAndAlert("Can't run empty query.  Use 'build' button first.");
			
		} else {
            
			logEvent("SG Run Query", "sparql", query);
			clearResults();
			
            if (getQuerySource() == "DIRECT") {
                runQueryDirect(query);
            } else {
                runQueryText();
            }
        }
	};

    // run direct query given SPARQL
    var runQueryDirect = function (sparql) {
        setStatusProgressBar("running DIRECT query...", 50);
        guiDisableAll();

        require(['sparqlgraph/js/sparqlserverinterface',
                ], function(SparqlServerInterface) {
            
            var failureCallback = function (html) {
                logAndAlert(html);
                setStatus("");
                guiUnDisableAll();
            }

            var successCallback = function (sparqlServerResult) {
                logEvent("SG Display Query Results", "rows", sparqlServerResult.getRowCount());

                if (getQuerySource() == "DIRECT") {
                    sparqlServerResult.putSparqlResultsDatagridInDiv(   document.getElementById("resultsParagraph"), 
                                                                        null,
                                                                        getNamespaceFlag());
                } 
                
                guiResultsNonEmpty();
                setStatus("");
                guiUnDisableAll();
                
            }
            
            gConn.getDefaultQueryInterface().executeAndParseToSuccess(sparql, successCallback, failureCallback);
        });
    };

	
	// The query callback for anything where no results are expected
	var runNoResultsQueryCallback = function(results) {
	
		// HTML: tell user query is done
		setStatus("");
		guiUnDisableAll();
		
		if (results.isSuccess()) {
			var res = results.getRsData(0,0);
			
			gQueryResults = null;
			guiResultsEmpty();
			
			document.getElementById("resultsParagraph").innerHTML = 	'<div class="alert alert-info"> <strong>Query Response</strong><p>' + res + '</div>';
		}
		else {
			logAndAlert(results.getStatusMessage());
		}
	};
	
	// Gui Functions
    // Inform the GUI which sections are empty
    // NOT Nested
    
	var guiTreeNonEmpty = function () {
    	document.getElementById("btnTreeExpand").disabled = false;
    	document.getElementById("btnTreeCollapse").disabled = false;
    };
    
    var guiTreeEmpty = function () {
    	document.getElementById("btnTreeExpand").disabled = true;
    	document.getElementById("btnTreeCollapse").disabled = true;
    };
   
    var guiGraphNonEmpty = function () {
    	document.getElementById("btnLayout").disabled = false;
    	document.getElementById("btnGraphClear").disabled = false;
    };
    
    var giuGraphEmpty = function () {
    	document.getElementById("btnLayout").disabled = true;
    	guiUpdateGraphRunButton();
    };
    
    var guiQueryEmpty = function () {
    	document.getElementById("btnQueryTextMenu").disabled = true;
    	document.getElementById("btnQueryRun").disabled = true;
    	guiUpdateGraphRunButton();
    };
    
    var guiQueryNonEmpty = function () {
    	document.getElementById("btnQueryTextMenu").disabled = false;
    	document.getElementById("btnQueryRun").disabled = false;
    	
    };

    var guiUpdateGraphRunButton = function () {
        var d = gNodeGroup == null || gNodeGroup.getSNodeList().length < 1 || getQuerySource() == "DIRECT";
        document.getElementById("btnGraphExecute").disabled = d;
    };
    
    var guiResultsEmpty = function () {
    	
    };
    
    var guiResultsNonEmpty = function () {
    	
    };
	
    var disableHash = {};
    
    /*
     * For all buttons with an id:
     *     disable
     *     save state
     */
    var guiDisableAll = function () {
        disableHash = {};
        
    	var buttons = document.getElementsByTagName("button");
        for (var i = 0; i < buttons.length; i++) {
        	if (buttons[i].id && buttons[i].id.length > 0) {
                disableHash[buttons[i].id] = buttons[i].disabled;

                buttons[i].disabled = true;
            }
        }
    };
    
    /*
     * For all buttons with an id:
     *     restore state from last call to guiDisableAll()
     */
    var guiUnDisableAll = function () {
    	var buttons = document.getElementsByTagName("button");
        for (var i = 0; i < buttons.length; i++) {
            // if button has an id, and its state has been hashed
            if (buttons[i].id && buttons[i].id.length > 0 && disableHash[buttons[i].id] != undefined) {
                buttons[i].disabled =  disableHash[buttons[i].id];
            }
        }
    };
    
	// Clear functions
	// NESTED:  Each one clears other things that depend upon it.
    var clearResults = function () {
		document.getElementById("resultsParagraph").innerHTML = "<table id='resultsTable'></table>";
		gQueryResults = null;
		gTimeseriesResults = null;
		guiResultsEmpty();
	};
    
	var clearQuery = function () {
	 	document.getElementById('queryText').value = "";
	 	
	 	document.getElementById('SGQueryType').selectedIndex = 0;
	 	document.getElementById('SGQueryLimit').value = "";
	 	document.getElementById('SGQueryNamespace').checked = true;
	 	
	 	clearResults();
	 	guiQueryEmpty();
	};
	
	var clearGraph = function () {
    	gNodeGroup.clear();
        gNodeGroup.setSparqlConnection(gConn);
        gNodeGroup.drawNodes();
        nodeGroupChanged(false);
    	clearQuery();
    	giuGraphEmpty();
    };
    
    var clearMappingTab = function () {
       gMappingTab.clear();
    };
    
    var clearTree = function () {
    	gOTree.removeAll();
    	clearGraph();
    	guiTreeEmpty();  //guiTreeEmpty();
    	clearMappingTab();

    };
    
    var clearEverything = function () {
    	clearTree();
    	gOInfo = new OntologyInfo();
        gEditTab.setOInfo(gOInfo);
        gEditTab.draw();
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
		        	
		        } else if (event.currentTarget.id == "anchorTabE") {
		        	tabEditActivated();
		     
			    } else if (event.currentTarget.id == "anchorTab2") {
		        	tabMappingActivated();
		     
			    } else if (event.currentTarget.id == "anchorTab3") {
		        	tabUploadActivated();
		        }
		    }
		});
	});
	
	var tabSparqlGraphActivated = function() {
		 gCurrentTab = g.tab.query;
		 this.document.getElementById("query-tab-but").disabled = true;
		 this.document.getElementById("edit-tab-but").disabled = false;
		 this.document.getElementById("mapping-tab-but").disabled = false;
		 this.document.getElementById("upload-tab-but").disabled = false;

	};
	
	var tabEditActivated = function() {
		gCurrentTab = g.tab.mapping;
		
		this.document.getElementById("query-tab-but").disabled = false;
        this.document.getElementById("edit-tab-but").disabled = true;
		this.document.getElementById("mapping-tab-but").disabled = false;
		this.document.getElementById("upload-tab-but").disabled = false;
		
	};
	
    var tabMappingActivated = function() {
		gCurrentTab = g.tab.mapping;
		
		this.document.getElementById("query-tab-but").disabled = false;
        this.document.getElementById("edit-tab-but").disabled = false;
		this.document.getElementById("mapping-tab-but").disabled = true;
		this.document.getElementById("upload-tab-but").disabled = false;
		
		// PEC TODO: this overwrites everything each time
		gMappingTab.updateNodegroup(gNodeGroup);
	};
	
	var tabUploadActivated = function() {
		 gCurrentTab = g.tab.upload;
		 
		 this.document.getElementById("query-tab-but").disabled = false;
		 this.document.getElementById("edit-tab-but").disabled = false;
		 this.document.getElementById("mapping-tab-but").disabled = false;
		 this.document.getElementById("upload-tab-but").disabled = true;
		 
		 gUploadTab.setNodeGroup(gConn, gNodeGroup, gMappingTab, gOInfoLoadTime);

	};