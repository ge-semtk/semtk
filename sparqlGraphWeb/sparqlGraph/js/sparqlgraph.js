/*
 ** Copyright 2016-2021 General Electric Company
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
    var gInvalidItems = [];   // nodegroup does not match the model

    // drag stuff
    var gDragLabel = null;
    var gLoadDialog;
    var gStoreDialog = null;

    var gNodeGroup = null;
    var gOInfoLoadTime = "";
    var gPlotSpecsHandler = null;

    var gCurrentTab = g.tab.query ;

    var gMappingTab = null;
    var gUploadTab = null;
    var gReady = false;

	
    var gNodeGroupName = "";          // set in nodeGroupChanged.  Read anywhere
    var gNodeGroupChangedFlag = false;  // set in nodeGroupChanged.  Read anywhere
    
    var gQueryTextChangedFlag = false;

    var gCancelled = false;

    var gQueryTypeIndex = 0;   // sel index of QueryType
    var gQuerySource = "SERVICES";

    var RESULTS_MAX_ROWS = 5000; // 5000 sample rows
    var ALLOW_CIRCULAR_NODEGROUPS = true;

    // READY FUNCTION
    $('document').ready(function(){
		setupTabs();
		
    	setTabButton("upload-tab-but", true);
    	setTabButton("mapping-tab-but", true);
        setTabButton("explore-tab-but", true);
        setTabButton("report-tab-but", true);

    	// checkBrowser();

    	initDynatree();

	    require([
                  'sparqlgraph/js/exploretab',
                  'sparqlgraph/js/mappingtab',
                  'sparqlgraph/js/modaliidx',
	              'sparqlgraph/js/modalloaddialog',
                  'sparqlgraph/js/modalstoredialog',
                  'sparqlgraph/js/msiclientnodegroupservice',
                  'sparqlgraph/js/msiclientnodegroupstore',
                  'sparqlgraph/js/msiclientquery',
                  'sparqlgraph/js/nodegrouprenderer',
                  'sparqlgraph/js/reporttab',
                  'sparqlgraph/js/undomanager',
                  'sparqlgraph/js/uploadtab',

                  // shim
                  'sparqlgraph/js/belmont',

	              'local/sparqlgraphlocal'
                ],
                function (ExploreTab, MappingTab, ModalIidx, ModalLoadDialog, ModalStoreDialog, MsiClientNodeGroupService, MsiClientNodeGroupStore, MsiClientQuery, NodegroupRenderer, ReportTab, UndoManager, UploadTab) {

	    	console.log(".ready()");

            gUndoManager = new UndoManager();

	    	// create the modal dialogue
            var ngClient = new MsiClientNodeGroupService(g.service.nodeGroup.url);
	    	gLoadDialog = new ModalLoadDialog(document, "gLoadDialog", ngClient, g.service.sparqlQuery.url);

	    	 // set up the node group
	        gNodeGroup = new SemanticNodeGroup();

            canvasWrapper = document.getElementById("canvasWrapper");
            canvasWrapper.innerHTML = "";
            canvasWrapper.onkeyup = onkeyupCanvas;
            gRenderer = new NodegroupRenderer(canvasWrapper);
	        gRenderer.setPropEditorCallback(launchPropertyItemDialog);
	        gRenderer.setSNodeEditorCallback(launchSNodeItemDialog);
	        gRenderer.setSNodeMenuCallback(launchSNodeMenuDialog);
            gRenderer.setSNodeRemoverCallback(snodeRemover);
	        gRenderer.setLinkBuilderCallback(launchLinkBuilder);
	        gRenderer.setLinkEditorCallback(launchLinkEditor);

	    	// load gUploadTab
	    	gUploadTab =  new UploadTab(document.getElementById("uploadtabdiv"),
	    								document.getElementById("uploadtoolsdiv"),
                                        document.getElementById("uploadmiscdiv"),
	    								doLoadConnection,
	    			                    g.service.ingestion.url,
	    			                    g.service.sparqlQuery.url);
	    	setTabButton("upload-tab-but", false);

            // explore tab
            gExploreTab = new ExploreTab( document.getElementById("exploreTreeDiv"),
                                       document.getElementById("exploreCanvasDiv"),
                                       document.getElementById("exploreButtonDiv"),
                                       document.getElementById("exploreSearchForm"),
                                       g.service.ontologyInfo.url
                                      );
            setTabButton("explore-tab-but", false);

	    	// load gMappingTab
            var ngClient = new MsiClientNodeGroupService(g.service.nodeGroup.url);
			gMappingTab =  new MappingTab(importoptionsdiv, importcanvasdiv, importcolsdiv, gUploadTab.setDataFile.bind(gUploadTab), logAndAlert, ngClient );
	    	setTabButton("mapping-tab-but", false);

            // init gExploreTab
            setTabButton("explore-tab-but", false);

            // init gReportTab
            setTabButton("report-tab-but", false);
            var user = localStorage.getItem("SPARQLgraph_user");

            gReportTab = new ReportTab(
                                    document.getElementById("reportToolForm"),
                                    document.getElementById("reportOptionsDiv"),
                                    document.getElementById("reportEditDiv"),
                                    document.getElementById("reportDiv"),
                                    g,
                                    user || "",
                                    function(u) {localStorage.setItem("SPARQLgraph_user", u);});
                                    

	        // load last connection
	        var conn = gLoadDialog.getLastConnectionInvisibly();
	        
	        // override with URL parameter if any
	        var connStr = getUrlParameter("conn");
	        if (connStr) {
				try {
					// parse the URL param into a connection
					conn = new SparqlConnection(connStr);
					
					var existName = gLoadDialog.connectionIsKnown(conn, true);
					if (!existName) {
						// add to cookies if it doesn't exist'
						gLoadDialog.addConnection(conn);
					} else {
						// update name to match existing cookie
						conn.setName(existName);
					}
				} catch (e) {
					ModalIidx.alert("Error loading conn parameter", "Can't load poorly formed 'conn' parameter on URL:<br><br>" + connStr);
					console.log(e.stack);
				}
			}
			
			var nodegroupId = getUrlParameter("nodegroupId");
			var reportId = getUrlParameter("reportId");
			var constraintsStr = getUrlParameter("constraints");
			var runFlagStr = getUrlParameter("runFlag");  // defaults to true, next line
	        var runFlag = (! runFlagStr) || runFlagStr.toLowerCase() == "true" || runFlagStr.toLowerCase() == "t";
	        
	        if (nodegroupId) {
				// give error if both nodegroup and report are specified.
				if (reportId) {
					ModalIidx.alert("Ignoring reportId param", "Both nodegroupId and reportId URL parameters were specified.<br>Ignoring the report.");
				}
					
				// run a nodegroup at startup
				runQueryFromUrl(conn, connStr, nodegroupId, constraintsStr, runFlag);
				
            } else if (reportId) {
				runReportFromUrl(conn, connStr, reportId, runFlag);
				
			} else if (conn) {
				// load last connection (from cookies) or conn param
				doLoadConnection(conn);

			} else if (g.customization.autoRunDemo.toLowerCase() == "true") {
				// no params or cookies: try the demo
                ModalIidx.okCancel( "Demo",
                                    "Loading demo nodegroup, and<br>Launching demo documentation pop-up.<br>(You may need to override your pop-up blocker.)<br>",
                                    function() {
                                        window.open(g.help.url.base + "/" + g.help.url.demo, "_blank","location=yes");

                                        var mq = new MsiClientNodeGroupStore(g.service.nodeGroupStore.url);
                                        mq.getStoredItemByIdToStr("demoNodegroup", MsiClientNodeGroupStore.TYPE_NODEGROUP, doQueryLoadJsonStr);
                                    }
                                  );

            }
			// make sure Query Source and Type disables are reset
			onchangeQueryType();

            var user = localStorage.getItem("SPARQLgraph_user");
            gStoreDialog = new ModalStoreDialog(user || "",
                                                     g.service.nodeGroupStore.url);


            resizeWindow();
	        window.onresize = resizeWindow;

            authenticate(document.getElementById("nav-but-span"));

            // SINCE CODE PRE-DATES PROPER USE OF REQUIRE.JS THROUGHOUT...
	    	// gReady is at the end of the ready function
	    	//        and tells us everything is loaded.
	   	    gReady = true;
	   	    console.log("Ready");
	   	    logEvent("SG Page Load");

            if (g.customization.startupDialogHtml != 'none' && g.customization.startupDialogHtml.length > 2) {
                ModalIidx.alert(g.customization.startupDialogTitle, g.customization.startupDialogHtml);
            }

            if (g.customization.bannerText != 'none' && g.customization.bannerText.length > 2) {
                var span = document.getElementById("sparqlgraph-banner-span");
                span.className = "label-warning right";
                span.innerHTML = "&nbsp" + g.customization.bannerText + "&nbsp";
            }
            
            // launch the checkServices dialog if any service pings fail.
            checkServicesOnce(function(downList) { if (downList.length > 0) { 
														checkServices("Some services did not respond at start-up. Retrying...<hr>"); 
												    }
												 });

		});
    });

	/* 
		URL used to launch SPARQLgraph contains a nodegroupId to run now
	  
		@param {SparqlConnection} conn -  the connection from last session OR the connection URL parameter OR null
		@param {str} connStr - conn URL param, or falsey
		@param {str} nodegroupId - nodegroup Id
		@param {str} constraintsStr - constraints string, or falsey
		@param {boolean} runFlag = should the nodegroup be run
	*/
	var runQueryFromUrl = function(conn, connStr, nodegroupId, constraintsStr, runFlag) {
		if (connStr) {
			doLoadConnection(conn, runQueryFromUrl1.bind(this, nodegroupId, constraintsStr, runFlag));
		} else {
			runQueryFromUrl1(nodegroupId, constraintsStr, runFlag);
		}
	};
	
	/* 
		Now that correct or no override conn is loaded,
		continue running nodegroupId at startup

		@param {str} nodegroupId - nodegroupId
		@param {str} constraintsStr - constraints string, or falsey
		@param {boolean} runFlag = should the nodegroup be run

	*/
	var runQueryFromUrl1= function(nodegroupId, constraintsStr, runFlag) {
		 require(	[ 'sparqlgraph/js/msiclientnodegroupstore',
					], function (MsiClientNodeGroupStore) {
						  
			var mq = new MsiClientNodeGroupStore(g.service.nodeGroupStore.url);
	        mq.getStoredItemByIdToStr(nodegroupId, MsiClientNodeGroupStore.TYPE_NODEGROUP, runQueryFromUrl2.bind(this, nodegroupId, constraintsStr, runFlag));
		});
	};
	
	/* 
		Load nodegroup, keeping current connection if any, and run

		@param {str} constraintsStr - constraints string, or falsey
		@param {str} nodegroupId - nodegroupId
		@param {boolean} runFlag = should the nodegroup be run
		@param {str} jsonStr - the nodegroup json as str
		
	*/
	var runQueryFromUrl2= function(nodegroupId, constraintsStr, runFlag, jsonStr) {
		
		require(	[ 'sparqlgraph/js/runtimeconstraints',
					], function (RuntimeConstraints) {
			var forceKeepCurrent = true;
			var skipValidation = false;
			
			var callbackRunQuery = function () {
				if (runFlag) {
					// change from possibly falsey string to possibly-undefined RuntimeConstraints object
					var optConstraints = constraintsStr ? new RuntimeConstraints(constraintsStr) : undefined;
					runGraphByQueryType(optConstraints);
				}
			};
			
			doQueryLoadJsonStr(jsonStr, nodegroupId, skipValidation, forceKeepCurrent, callbackRunQuery);
		});
	};
	
	/* 
		URL used to launch SPARQLgraph contains a nodegroupId to run now
	  
		@param {SparqlConnection} conn -  the connection from last session OR the connection URL parameter OR null
		@param {str} connStr - conn URL param, or falsey
		@param {str} reportId - reportId Id
		@param {boolean} runFlag = should the nodegroup be run
	*/
	var runReportFromUrl = function(conn, connStr, reportId, runFlag) {
		if (connStr) {
			doLoadConnection(conn, runReportFromUrl1.bind(this, reportId, runFlag));
		} else {
			runReportFromUrl1(reportId, runFlag);
		}
	};
	
	/* 
		Now that correct or no override conn is loaded,
		continue running reportId at startup

		@param {str} reportId - nodegroupId
		@param {boolean} runFlag = should the nodegroup be run

	*/
	var runReportFromUrl1= function(reportId, runFlag) {
		 require(	[ 'sparqlgraph/js/msiclientnodegroupstore',
					], function (MsiClientNodeGroupStore) {
						  
			var mq = new MsiClientNodeGroupStore(g.service.nodeGroupStore.url);
	        mq.getStoredItemByIdToStr(reportId, MsiClientNodeGroupStore.TYPE_REPORT, runReportFromUrl2.bind(this, reportId, runFlag));
		});
	};
	
	/* 
		Load report, keeping current connection if any, and run

		@param {str} constraintsStr - constraints string, or falsey
		@param {str} nodegroupId - nodegroupId
		@param {boolean} runFlag = should the nodegroup be run
		@param {str} jsonStr - the nodegroup json as str
		
	*/
	var runReportFromUrl2= function(reportId, runFlag, jsonStr) {
		//$("#tabs").tabs();  // make sure they're initialized
		setupTabs();
		selectTab(4);
		tabReportActivated();
		gReportTab.setReport(jsonStr);
		
		if (runFlag) {
			gReportTab.drawReport(JSON.parse(jsonStr));
		}

	};
	
	
	var getUrlParameter = function getUrlParameter(sParam) {
	    var sPageURL = window.location.search.substring(1);
	    var sURLVariables = sPageURL.split('&');
	    var sParameterName;
	
	    for (var i = 0; i < sURLVariables.length; i++) {
	        sParameterName = sURLVariables[i].split('=');
	
	        if (sParameterName[0] === sParam) {
	            return sParameterName[1] === undefined ? true : decodeURIComponent(sParameterName[1]);
	        }
	    }
	    return false;
	};
	
    var onkeyupCanvas = function(e) {
        if (e.ctrlKey) {
            if (e.key == 'z') {
                doUndo();
            } else if (e.key == 'y') {
                doRedo();
            }
        }
    };

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

            // Handle no paths or shift key during drag: drop node with no connections
            if (noPathFlag) {

                gNodeGroup.addNode(dragLabel, gOInfo);
                saveUndoState();
                nodeGroupChanged(true);
                guiGraphNonEmpty();

            } else {
                require([ 'sparqlgraph/js/msiclientresults',
                          'sparqlgraph/js/msiclientstatus',
                          'sparqlgraph/js/msiclientnodegroupservice',
                          'sparqlgraph/js/msiresultset',
                          'sparqlgraph/js/sparqlgraphjson',
                      ], function (MsiClientResults, MsiClientStatus, MsiClientNodeGroupService, MsiResultSet, SparqlGraphJson) {

                    var ngClient = new MsiClientNodeGroupService(g.service.nodeGroup.url);

                    //var nodelist = gNodeGroup.getArrayOfURINames();
                    //var paths = gOInfo.findAllPaths(dragLabel, nodelist, gConn.getDomain());
                    var sgJson = new SparqlGraphJson(gConn, gNodeGroup, null, false, null);

                    var successCallback = function(resJson) {
                        // no way to check for success from json blob
                        var resObj = new MsiResultSet(resJson.serviceURL, resJson.xhr);
                        findAllPathsCallback(dragLabel, resObj);
                    };

                    var resultsCall = MsiClientResults.prototype.execGetJsonBlobRes;
                    var callback = buildStatusResultsCallback(successCallback, resultsCall, MsiClientStatus, MsiClientResults, MsiResultSet);

                    var predicateMode = getPathFindingMode() == 1;
                    var ngMode = getPathFindingMode() == 2;
                    ngClient.execFindAllPaths(sgJson, dragLabel, predicateMode, ngMode, callback);
                });
            }
        }
        else{
            // not found
            logAndAlert("Only classes can be dropped on the graph.");

        }

    };

    var findAllPathsCallback = function(addClassStr, simpleRes) {
        var pathListJson = simpleRes.getSimpleResultField("pathList");
        var pathWarnings = simpleRes.getSimpleResultField("pathWarnings");

        if (pathListJson.length == 0) {

            gNodeGroup.addNode(addClassStr, gOInfo);
            saveUndoState();
            nodeGroupChanged(true);
            guiGraphNonEmpty();

        } else {
            // find possible anchor node(s) for each path
            // start with disconnected option
            var pathStrList = ["** Disconnected " + addClassStr];
            var valList = [[new OntologyPath(addClassStr), null, false]];

            // for each path
            for (var p of pathListJson) {
                var oPath = OntologyPath.fromJson(p);
                // for each instance of the anchor class
                var nlist = gNodeGroup.getNodesByURI(oPath.getAnchorClassName());
                for (var n=0; n < nlist.length; n++) {
                    pathStrList.push(oPath.genPathString(nlist[n], false));
                    valList.push( [oPath, nlist[n], false ] );

                    // push it again backwards if it is a special singleLoop
                    if (oPath.getStartClassName() == oPath.getEndClassName()) {
                        pathStrList.push(oPath.genPathString(nlist[n], true));
                        valList.push( [oPath, nlist[n], true ] );
                    }
                }
            }

            // if choices are more than "** Disconnected" plus one other path...
            if (valList.length > 2) {
                 require([ 'sparqlgraph/js/modaliidx',
                         ], function (ModalIidx) {

                    var extraDOM = undefined;
                    if (pathWarnings && pathWarnings.length > 0) {
                        extraDOM = document.createElement("div");
                        extraDOM.align="left";
                        for (var warnStr of pathWarnings) {
                            var alert = document.createElement("span");
                            alert.classList.add("alert");
                            alert.classList.add("alert-info");
                            alert.innerHTML = warnStr;
                            extraDOM.appendChild(alert);
                        }
                    }
                    // offer a choice, defaulting to the shortest non-disconnected path
                    ModalIidx.listDialog("Choose the path", "Submit", pathStrList, valList, 1, dropClassCallback, 80, extraDOM, true);

                 });

            } else {
                // automatically add using the only path
                dropClassCallback(valList[1]);
            }
        }
    }
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
        saveUndoState();
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
        var canvasWrapper = document.getElementById("canvasWrapper");
        canvasWrapper.addEventListener("mouseup",
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

                       // only folders (classes) and those with parents (non-namespaces) can be dragged here
                       if (node.data.isFolder && node.parent.parent != null) {
                           gDragLabel = gOTree.nodeGetURI(node);
                           return true;
                       } else {
                           gDragLabel = null;
                           return false;
                       }
                    },

                    onDragStop: function(node, x, y, z, aa) {
                       // console.log("dragging " + gOTree.nodeGetURI(node) + " stopped.");
                        gDragLabel = null;
                    }
                },


                persist: false,    // true causes a cookie error with large trees
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
	
	var getTemplateDialogCallback =function (classUri, sgjsonJson) {
		var newNgName = "ingest_" + classUri.split("#")[1];
		checkAnythingUnsavedThen(doQueryLoadJsonStr.bind(this, JSON.stringify(sgjsonJson), newNgName));
	};
	
	var doLaunchGetTemplateDialog = function () {
		require(['sparqlgraph/js/modalgettemplatedialog'],
    	         function (ModalGetTemplateDialog) {
                     dialog = new ModalGetTemplateDialog(g.service.ingestion.url, gConn, gOInfo);
                     dialog.launch(getTemplateDialogCallback.bind(this));
				});
	};
	
	
    // application-specific property editing
    var launchPropertyItemDialog = function (propItem, snodeID) {
        checkQueryTextUnsavedThen(launchPropertyItemDialog1.bind(this, propItem, snodeID));
    };

    var launchPropertyItemDialog1 = function (propItem, snodeID) {


    	require([ 'sparqlgraph/js/modaliidx',
                  'sparqlgraph/js/modalitemdialog',
                  'sparqlgraph/js/modalinvaliditemdialog',
                  'sparqlgraph/js/msiclientnodegroupservice'
              ], function (ModalIidx, ModalItemDialog, ModalInvalidItemDialog, MsiClientNodeGroupService) {

            var raisePropItemDialog = function () {
                // javascript black magic:
                // when this is finally called, propItem and gNodeGroup have changed
                // gNodeGroup - fine, it's a global
                // propItem - need to find it in the current gNodeGroup
                var p = gNodeGroup.getNodeBySparqlID(snodeID).getPropertyByURIRelation(propItem.getURI());

                var dialog= new ModalItemDialog(p,
                                                gNodeGroup,
                                                runSuggestValuesQuery.bind(this, g, gConn, gNodeGroup, null, p),
                                                propertyItemDialogCallback,
                                                {"snodeID" : snodeID}
                                                );
                dialog.setLimit(RESULTS_MAX_ROWS);
                dialog.show();
            };

            if (gInvalidItems.length > 0) {
                var parentNode = gNodeGroup.getNodeBySparqlID(snodeID);
                var itemStr = gNodeGroup.buildItemStr(parentNode, propItem);

                if (gInvalidItems.indexOf(itemStr) > -1) {
                    // this property item is invalid

                    var oClass = gOInfo.getClass(gNodeGroup.getPropertyItemParentSNode(propItem).getURI());
                    if (oClass.getProperty(propItem.getURI()) != null) {
                        // Domain is already correct.   Range is wrong.
                        // There must be constraints or else this wouldn't have been flagged as an error.
                        var oRangeStr = oClass.getProperty(propItem.getURI()).getRange(oClass, gOInfo).getSimpleURI();
                        var pRangeStr = propItem.getRangeURI();
                        var hasConstraints = (propItem.getConstraints().length > 0);
                        var hasMapping = gMappingTab.getImportSpec().hasMapping(snodeID, propItem.getURI());

                        // call REST to make the change, then raise the good dialog
                        var changePropItemURI = function() {
                            // same javascript black magic
                            // propItem - need to find it in the current gNodeGroup
                            var p = gNodeGroup.getNodeBySparqlID(snodeID).getPropertyByURIRelation(propItem.getURI());
                            changeItemURI(p, undefined, oRangeStr, "range", hasConstraints ? raisePropItemDialog : undefined);
                        };

                        var msg = "Repairing property range<list><li>from " + pRangeStr + "</li><li>to " + oRangeStr + "</li></list><br>" +
                                  (hasConstraints ? "<br>Review constraints to make sure they are compatible." : "") +
                                  (hasMapping ? "<br>Review item's import mapping" : "");

                        ModalIidx.alert("Repair property range", msg, false, changePropItemURI);
                    } else {
                        // Domain is illegal
                        var dialog= new ModalInvalidItemDialog( new MsiClientNodeGroupService(g.service.nodeGroup.url),
                                                                propItem, null,
                                                                gNodeGroup,
                                                                gConn,
                                                                gOInfo,
                                                                gMappingTab.getImportSpec(),
                                                                changeItemURI
                                                                );
                        dialog.show();
                    }


                } else {
                    raisePropItemDialog();
                }
            } else {
    		    raisePropItemDialog();
            }
		});
    };

    var launchLinkBuilder = function(snode, nItem) {
        checkQueryTextUnsavedThen(launchLinkBuilder1.bind(this, snode, nItem));
    };

    var launchLinkBuilder1 = function(snode, nItem) {
        // check if entire NodeItem is invalid (domain or range)
        var itemStr = gNodeGroup.buildItemStr(snode, nItem, null);

        if (gInvalidItems.indexOf(itemStr) > -1) {
            var oClass = gOInfo.getClass(snode.getURI());
            var oProp = oClass.getProperty(nItem.getURI());
            if (oProp != null) {
                require(['sparqlgraph/js/modaliidx',
                         'sparqlgraph/js/msiclientnodegroupservice'],
                         function (ModalIidx, MsiClientNodeGroupService) {
					var oRange = oProp.getRange(oClass, gOInfo);
                    var correctRange = oRange.getDisplayString(false);
                    var correctRangeAbbrev = oRange.getDisplayString(true);
                    var propName = oProp.getLocalName();
                    ModalIidx.okCancel("Correct range",
                                        "Correct " + propName + " range to " + correctRangeAbbrev,
                                        changeItemURI.bind(this, nItem, null, correctRange, "range"));
                });
            } else {
                require(['sparqlgraph/js/modalinvaliditemdialog',
                         'sparqlgraph/js/msiclientnodegroupservice'],
                         function (ModalInvalidItemDialog, MsiClientNodeGroupService) {

                    // Domain is unknown.
                    // Domain is illegal
                    var dialog= new ModalInvalidItemDialog( new MsiClientNodeGroupService(g.service.nodeGroup.url),
                                                            nItem, null,
                                                            gNodeGroup,
                                                            gConn,
                                                            gOInfo,
                                                            gMappingTab.getImportSpec(),
                                                            changeItemURI
                                                            );
                    dialog.show();
                });

            }
        } else {
            launchLinkBuilder2(snode, nItem);
        }
    };

    var launchLinkBuilder2 = function(snode, nItem) {
		// callback when user clicks on a nodeItem
    	var rangeUriList = nItem.getRangeUris();
    	var targetSNodes = [];
    	var unlinkedTargetNames = [];
    	var unlinkedUriOrSNodes = [];
        
		for (var rangeURI of rangeUriList) {
			// find nodes that might connect
			targetSNodes = targetSNodes.concat(gNodeGroup.getNodesBySuperclassURI(rangeURI, gOInfo));
			// suggest new node
			unlinkedTargetNames.push("New " + rangeURI);
			unlinkedUriOrSNodes.push(rangeURI);
			for (var subURI of gOInfo.getSubclassNames(rangeURI)) {
				unlinkedTargetNames.push("New " + subURI);
				unlinkedUriOrSNodes.push(subURI)
			}
		}
		
		// uniquify
		targetSNodes = Array.from(new Set(targetSNodes));
		unlinkedTargetNames = Array.from(new Set(unlinkedTargetNames));
    	
    	// find existing snodes to link to
    	var mySubgraph = gNodeGroup.getSubGraph(snode, []);

    	for (var i=0; i < targetSNodes.length; i++) {
            if (nItem.getSNodes().indexOf(targetSNodes[i]) == -1) {  // not already connected
                if ( ALLOW_CIRCULAR_NODEGROUPS ||
                       (mySubgraph.indexOf(targetSNodes[i]) == -1 && targetSNodes[i] != snode ) ){    // not cicular && not self
                    
                    // Insert after "New" alphabetically by bindingOrSparqlID
                    var insertAt = 0;
                    var newName = targetSNodes[i].getBindingOrSparqlID();
                    while (insertAt < unlinkedTargetNames.length && 
                    	(unlinkedTargetNames[insertAt].startsWith("New") || newName > unlinkedTargetNames[insertAt] )) {
						insertAt += 1;
					}
					
    				unlinkedTargetNames.splice(insertAt, 0, newName);
    				unlinkedUriOrSNodes.splice(insertAt, 0, targetSNodes[i]);
    			}
    		}
    	}

    	// if only possibility is one unlinked snode
    	if (unlinkedUriOrSNodes.length == 1 && typeof unlinkedUriOrSNodes[0] == 'string') {
    		buildLink(snode, nItem, unlinkedUriOrSNodes[0]);
    	} else {
            require([ 'sparqlgraph/js/modaliidx',
                             ], function (ModalIidx) {

                       ModalIidx.listDialog("Choose node to connect", "Submit", unlinkedTargetNames, unlinkedUriOrSNodes, 0, buildLink.bind(this, snode, nItem), 75, undefined, true);

                     });
    	}
	};

    var launchLinkEditor = function(snode, nItem, targetSNode) {
        checkQueryTextUnsavedThen(launchLinkEditor1.bind(this, snode, nItem, targetSNode));
    };

    var launchLinkEditor1 = function(snode, nItem, targetSNode) {

        if (gInvalidItems.length > 0) {
            require([ 'sparqlgraph/js/modalinvaliditemdialog', 'sparqlgraph/js/msiclientnodegroupservice'],
                    function (ModalInvalidItemDialog, MsiClientNodeGroupService) {
                        var dialog= new ModalInvalidItemDialog( new MsiClientNodeGroupService(g.service.nodeGroup.url),
                                                        nItem, targetSNode,
                                                        gNodeGroup,
                                                        gConn,
                                                        gOInfo,
                                                        gMappingTab.getImportSpec(),
                                                        changeItemURI
                                                        );
                        dialog.show();
            });
        } else {
    		require([ 'sparqlgraph/js/modallinkdialog',], function (ModalLinkDialog) {
	    		var dialog= new ModalLinkDialog(nItem, snode, targetSNode, gNodeGroup, linkEditorCallback, {});
	    		dialog.show();
			});
        }
	};

	var linkEditorCallback = function(snode, nItem, targetSNode, data, optionalMinusVal, qualifierVal, union, unionReverse, deleteMarkerVal, deleteFlag) {
        require([ 'sparqlgraph/js/modallinkdialog',
                         ], function (ModalLinkDialog) {



            // optionalFlag
    		nItem.setOptionalMinus(targetSNode, optionalMinusVal);
            nItem.setQualifier(targetSNode, qualifierVal);
    		nItem.setSnodeDeletionMarker(targetSNode, deleteMarkerVal);

            // union
            gNodeGroup.rmFromUnions(snode, nItem, targetSNode);
            if (union == ModalLinkDialog.UNION_NONE) {
            } else if (union == ModalLinkDialog.UNION_NEW) {
                gNodeGroup.addToUnion(gNodeGroup.newUnion(), snode, nItem, targetSNode, unionReverse);
            } else {
                gNodeGroup.addToUnion(union, snode, nItem, targetSNode, unionReverse);
            }


    		// deleteFlag
    		if (deleteFlag) {
    			gNodeGroup.removeLink(nItem, targetSNode);
    		}

            nodeGroupChanged(true);
            saveUndoState();
        });
	};


	/**
	Hashes at top left of a node's grab bar
	 */
    var launchSNodeMenuDialog = function (snodeItem) {
        checkQueryTextUnsavedThen(launchSNodeMenuDialog1.bind(this, snodeItem));
    };


    var launchSNodeMenuDialog1 = function (snodeItem) {
        require([ 'sparqlgraph/js/modalitemdialog',
                  'sparqlgraph/js/modalinvaliditemdialog',
                  'sparqlgraph/js/msiclientnodegroupservice',
              ], function (ModalItemDialog, ModalInvalidItemDialog, MsiClientNodeGroupService) {

            	// Changing a node's type uses the functionality built for validating an invalid node type
                var ngClient = new MsiClientNodeGroupService(g.service.nodeGroup.url);

                var dialog= new ModalInvalidItemDialog( ngClient,
                                                        snodeItem, null,
                                                        gNodeGroup,
                                                        gConn,
                                                        gOInfo,
                                                        gMappingTab.getImportSpec(),
                                                        editNodeURI
                                                        );
                dialog.show();
        });
    };
 
    
    var launchSNodeItemDialog = function (snodeItem) {
        checkQueryTextUnsavedThen(launchSNodeItemDialog1.bind(this, snodeItem));
    };

    var launchSNodeItemDialog1 = function (snodeItem) {
        require([ 'sparqlgraph/js/modalitemdialog',
                  'sparqlgraph/js/modalinvaliditemdialog',
                  'sparqlgraph/js/msiclientnodegroupservice',
              ], function (ModalItemDialog, ModalInvalidItemDialog, MsiClientNodeGroupService) {

            if (gInvalidItems.length > 0) {
                var ngClient = new MsiClientNodeGroupService(g.service.nodeGroup.url);

                var dialog= new ModalInvalidItemDialog( ngClient,
                                                        snodeItem, null,
                                                        gNodeGroup,
                                                        gConn,
                                                        gOInfo,
                                                        gMappingTab.getImportSpec(),
                                                        changeItemURI
                                                        );
                dialog.show();
            } else {
                var dialog= new ModalItemDialog(snodeItem,
                                                gNodeGroup,
                                                this.runSuggestValuesQuery.bind(this, g, gConn, gNodeGroup, null, snodeItem),
                                                snodeItemDialogCallback,
                                                {} // no data
                                                );
                dialog.setLimit(RESULTS_MAX_ROWS);
				dialog.show();
            }
        });
    };

	/*
        ModalInvalidItemDialog callback:
        item = node, propItem, nodeItem
        target = target snode if nodeItem
        newURI = new URI
    */
    var editNodeURI = function(item, optTarget, newURI, domainOrRange, optSuccessCallback) {
        require(['sparqlgraph/js/modaliidx',
                 'sparqlgraph/js/msiclientnodegroupservice',
                 'sparqlgraph/js/sparqlgraphjson'],
    	            function (ModalIidx, MsiClientNodeGroupService, SparqlGraphJson) {

            var ngClient = new MsiClientNodeGroupService(g.service.nodeGroup.url);
            var sgJson = new SparqlGraphJson(gConn, gNodeGroup, gMappingTab.getImportSpec(), false, undefined);

            ngClient.execAsyncChangeItemURI(sgJson, gNodeGroup.buildItemStr(item),  newURI, domainOrRange, editNodeURICallback.bind(this, optSuccessCallback), ModalIidx.alert.bind(this, "NodeGroup Service failure"));

        });

    };
    
     var editNodeURICallback = function(optCallback, sgJson) {
        sgJson.getNodeGroup(gNodeGroup);
        gMappingTab.load(gNodeGroup, gConn, sgJson.getImportSpecJson());

		var skipSuccessMessage = (gInvalidItems.length == 0);
        reValidateNodegroup(optCallback, skipSuccessMessage);
    };

    /*
        ModalInvalidItemDialog callback:
        item = node, propItem, nodeItem
        target = target snode if nodeItem
        newURI = new URI
    */
    var changeItemURI = function(item, optTarget, newURI, domainOrRange, optSuccessCallback) {
        require(['sparqlgraph/js/modaliidx',
                 'sparqlgraph/js/msiclientnodegroupservice',
                 'sparqlgraph/js/sparqlgraphjson'],
    	            function (ModalIidx, MsiClientNodeGroupService, SparqlGraphJson) {

            var ngClient = new MsiClientNodeGroupService(g.service.nodeGroup.url);
            var sgJson = new SparqlGraphJson(gConn, gNodeGroup, gMappingTab.getImportSpec(), false, undefined);

            if (item.getItemType() == "SemanticNode") {

                ngClient.execAsyncChangeItemURI(sgJson, gNodeGroup.buildItemStr(item),  newURI, domainOrRange, changeItemURICallback.bind(this, optSuccessCallback), ModalIidx.alert.bind(this, "NodeGroup Service failure"));

            } else if (item.getItemType() == "PropertyItem") {
                var parent = gNodeGroup.getPropertyItemParentSNode(item);
                ngClient.execAsyncChangeItemURI(sgJson, gNodeGroup.buildItemStr(parent, item),  newURI, domainOrRange, changeItemURICallback.bind(this, optSuccessCallback), ModalIidx.alert.bind(this, "NodeGroup Service failure"));

            } else if (item.getItemType() == "NodeItem") {
                var parent = gNodeGroup.getNodeItemParentSNode(item);
                ngClient.execAsyncChangeItemURI(sgJson, gNodeGroup.buildItemStr(parent, item, optTarget),  newURI, domainOrRange, changeItemURICallback.bind(this, optSuccessCallback), ModalIidx.alert.bind(this, "NodeGroup Service failure"));
            }
        });

    };

    var changeItemURICallback = function(optCallback, sgJson) {
        sgJson.getNodeGroup(gNodeGroup);
        gMappingTab.load(gNodeGroup, gConn, sgJson.getImportSpecJson());

        reValidateNodegroup(optCallback);
    };

    var snodeRemover = function (snode) {
        checkQueryTextUnsavedThen(snodeRemover1.bind(this, snode));
    };

    var snodeRemover1 = function (snode) {

        snode.removeFromNodeGroup(false);
        saveUndoState();

        if (gInvalidItems.length > 0) {
            reValidateNodegroup();
        } else {
            nodeGroupChanged(true)
        }
    };

    /**
	 * Link from snode through it's nItem to rangeSNode
	 * @param snode - starting point
	 * @param nItem - nodeItem
	 * @param rangeOrSnode - range URI to create or exsiting range SNode
	 */
	var buildLink = function(snode, nItem, rangeOrSnode) {

		var snodeClass = gOInfo.getClass(snode.fullURIName);
		var domainStr = gOInfo.getInheritedPropertyByKeyname(snodeClass, nItem.getKeyName()).getNameStr();
		if (typeof rangeOrSnode == 'string') {
			// rangeOrSnode is a URI for Snode to create and connect
			var newNode = gNodeGroup.returnBelmontSemanticNode(rangeOrSnode, gOInfo);
			gNodeGroup.addOneNode(newNode, snode, null, domainStr);
		} else {
			// rangeOrSnode is an Snode to connect
			snode.setConnection(rangeOrSnode, domainStr);
		}
        saveUndoState();
        nodeGroupChanged(true);
	};

    var propertyItemDialogCallback = function(propItem, varName, returnFlag, returnTypeFlag, optMinus, union, delMarker, rtConstrainedFlag, constraintStr, data, functions, undefinedConstructFlag) {
        // Note: ModalItemDialog validates that sparqlID is legal

        require([ 'sparqlgraph/js/modalitemdialog',
                ], function (ModalItemDialog) {



            // update the binding or sparqlID based on varName and returnFalg
            if (propItem.getSparqlID() == "") {
                gNodeGroup.changeSparqlID(propItem, varName);
            }
            if (varName == propItem.getSparqlID()) {
                // varName is sparqlID: shut of binding
                gNodeGroup.setBinding(propItem, null);
                propItem.setIsBindingReturned(false);
            } else {
                // varName is NOT sparqlID: use binding
                gNodeGroup.setBinding(propItem, varName);
                propItem.setIsReturned(false);
            }
            gNodeGroup.setIsReturned(varName, returnFlag);

            // returnTypeFlag is not used for properties

            propItem.setOptMinus(optMinus);

            // union: presume that u is legal
            gNodeGroup.rmFromUnions(gNodeGroup.getPropertyItemParentSNode(propItem), propItem);

            if (union == ModalItemDialog.UNION_NONE) {
            } else if (union == ModalItemDialog.UNION_NEW) {
                gNodeGroup.addToUnion(gNodeGroup.newUnion(), gNodeGroup.getPropertyItemParentSNode(propItem), propItem);
            } else {
                gNodeGroup.addToUnion(union, gNodeGroup.getPropertyItemParentSNode(propItem), propItem);
            }


            propItem.setIsRuntimeConstrained(rtConstrainedFlag);
            propItem.setConstraints(constraintStr);
            propItem.setIsMarkedForDeletion(delMarker);
            propItem.setFunctions(functions);

            nodeGroupChanged(true);
            saveUndoState();
        });
    };

    var snodeItemDialogCallback = function(snodeItem, varName, returnFlag, returnTypeFlag, optMinus, union, delMarker, rtConstrainedFlag, constraintStr, data, functions, constructFlag) {
        require([ 'sparqlgraph/js/modalitemdialog',
                ], function (ModalItemDialog) {

            // Note: ModalItemDialog validates that sparqlID is legal


            // update the binding or sparqlID based on varName and returnFalg
            if (varName == snodeItem.getSparqlID()) {
                // varname is sparqlID: shut off binding
                gNodeGroup.setBinding(snodeItem, null);
                snodeItem.setIsBindingReturned(false);
            } else {
                // varname is NOT sparqlID: use binding
                gNodeGroup.setBinding(snodeItem, varName);
                snodeItem.setIsReturned(false);
            }
            gNodeGroup.setIsReturned(varName, returnFlag);

            snodeItem.setIsTypeReturned(returnTypeFlag);

            // union
            gNodeGroup.rmFromUnions(snodeItem);

            if (union == ModalItemDialog.UNION_NONE) {
            } else if (union == ModalItemDialog.UNION_NEW) {
                gNodeGroup.addToUnion(gNodeGroup.newUnion(), snodeItem);
            } else {
                gNodeGroup.addToUnion(union, snodeItem);
            }

    		// runtime constrained
        	snodeItem.setIsRuntimeConstrained(rtConstrainedFlag);
        	snodeItem.setDeletionMode(delMarker);

        	// constraints
        	snodeItem.setConstraints(constraintStr);
            snodeItem.setFunctions(functions);

            snodeItem.setIsConstructed(constructFlag);
            nodeGroupChanged(true);
            saveUndoState();
        });
    };

    // window's onresize event
    var resizeWindow = function() {
        resizeElem("importcanvasdiv", -1, 95);
        resizeElem("importcolsdiv", -1, 95);
    };

    var resizeElem = function(name, xPercent, yPercent) {

        var elem = document.getElementById(name);
        if (xPercent > 0) {
            elem.style.width = Math.round((window.innerWidth - elem.getBoundingClientRect().left) * (xPercent / 100)) + "px";
        }
        if (yPercent > 0) {
            elem.style.height = Math.round((window.innerHeight - elem.getBoundingClientRect().top) * (yPercent / 100)) + "px";
        }
    };

    var doLoad = function() {
    	logEvent("SG Menu: File->Load");
    	gLoadDialog.loadDialog(gConn, doLoadConnection);
    };

    //**** Start new load code *****//
    var doLoadOInfoSuccess = function() {

        var warnings = gOInfo.getLoadWarnings().slice();
        if (gOInfo.getClassNames().length < 1) {
            warnings.unshift("Warning: connection doesn't contain any classes.");
        }
        // Connection is empty: spin off a warning but continue
        if (warnings.length > 0) {
            require(['sparqlgraph/js/modaliidx'], function(ModalIidx) {
                ModalIidx.alert("Ontology loaded with warnings", "Load warnings were encountered<br><list><li>" +
                                                 warnings.join("</li><li>") +
                                                 "</li></list>");
            });
        }

    	// now load gOInfo into gOTree
		gOTree.setOInfo(gOInfo);
    	gOTree.showAll();
	    gOInfoLoadTime = new Date();

        gExploreTab.setConn(gConn, gOInfo);
        gReportTab.setConn(gConn);

		setStatus("");
		guiTreeNonEmpty();
		gMappingTab.updateNodegroup(gNodeGroup, gConn);
		gUploadTab.setNodeGroup(gConn, gNodeGroup, gNodeGroupName, gOInfo, gMappingTab, gOInfoLoadTime);

		logEvent("SG Load Success");
    };

    var htmlize = function(msg) {
        return msg.replace(/\n/g, "<br>")
                    .replace(/\\n/g, "<br>");
    };

    var doLoadFailure = function(msg) {
    	require(['sparqlgraph/js/ontologyinfo'],
   	         function () {

	    	logAndAlert(htmlize(msg));
	    	setStatus("");
	    	clearTree();
	    	gOInfo = new OntologyInfo();
		    gOInfoLoadTime = new Date();

            gExploreTab.setConn(gConn, gOInfo);
            gReportTab.setConn(gConn);

	    	gMappingTab.updateNodegroup(gNodeGroup, gConn);
			gUploadTab.setNodeGroup(gConn, gNodeGroup, gNodeGroupName, gOInfo, gMappingTab, gOInfoLoadTime);

    	});
 		// retains gConn
    };

    var doLoadConnection = function(connProfile, optCallback, optClearCache) {
    	// Callback from the load dialog
    	var callback = (typeof optCallback === "undefined") ? function(){} : optCallback;

    	require(['sparqlgraph/js/msiclientontologyinfo',
                 'sparqlgraph/js/msiclientquery',
    	         'sparqlgraph/js/backcompatutils',
    	         'jquery',
    	         'jsonp'], function(MsiClientOntologyInfo, MsiClientQuery, BCUtils) {


	    	// Clean out existing GUI
	    	clearEverything();

	    	// Get connection info from dialog return value
	    	setConn(connProfile);

	    	gNodeGroup.setSparqlConnection(gConn);

	    	if (gConn != null) {

                oInfoClient = new MsiClientOntologyInfo(g.service.ontologyInfo.url, doLoadFailure);

                var continueWithLoad = function() {
                    gOInfo.loadFromService(oInfoClient, gConn, setStatus, function(){doLoadOInfoSuccess(); callback();}, doLoadFailure);
                };

                if (optClearCache) {
                    oInfoClient.execUncacheOntology(gConn, continueWithLoad);
                } else {
                    continueWithLoad();
                }
	    	}
    	});
    };

    // update display of nodegroup store id and connection
    var updateNgAndConnDisplay = function() {
        var connStr = "";
        if (gNodeGroupName) {
            connStr = "<b>Name: </b>" + gNodeGroupName;

            if (gNodeGroupChangedFlag == true) {
                connStr += "*";
            }
            connStr += "&nbsp&nbsp&nbsp";
        }
        if (gConn != null && gConn.getName()) {
			connStr = connStr + "<b>Conn: </b>" + gConn.getName();
		}
        

        document.getElementById("spanConnection").innerHTML = connStr;
    };

    var setConn = function (conn) {
        resetUndo();   // changing connection clears out Undo
        gConn = conn;
        this.updateNgAndConnDisplay();
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
                item.setSparqlID("?SG_SuggestedValues");
                runNodegroup = ng.deepCopy();
                runId = item.getSparqlID();
                item.setSparqlID("");
            } else {
                runNodegroup = ng.deepCopy();
                runId = item.getSparqlID();
            }

            runNodegroup.setLimit(0);
            runNodegroup.setOffset(0);


            if (getQuerySource() == "DIRECT") {
                // Generate sparql and run via query interface

                // get answer for msiOrQsResultCallback
                var sparqlCallback = function (cn, resCallback, failCallback, sparql) {
                    var ssi = cn.getDefaultQueryInterface();
                    ssi.executeAndParseToSuccess(sparql, resCallback, failCallback );

                }.bind(this, conn, msiOrQsResultCallback, failureCallback);

                // generate sparql and send to sparqlCallback
                var ngClient = new MsiClientNodeGroupService(g.service.nodeGroup.url);

                // NOTE: Jira PESQS-281   no way to get query with runtime constraints
                ngClient.execAsyncGenerateFilter(runNodegroup, conn, runId, sparqlCallback, failureCallback);

            } else {
                var checkForCancel = function() { return false; };
                // Run nodegroup via Node Group Exec Svc
                
                // set limit of query same as dialog.setLimit() to make sure we don't get too many results
                runNodegroup.setLimit(RESULTS_MAX_ROWS);
    
                var jsonCallback = MsiClientNodeGroupExec.buildFullJsonCallback(msiOrQsResultCallback,
                                                                                 failureCallback,
                                                                                 statusCallback,
                                                                                 checkForCancel,
                                                                                 g.service.status.url,
                                                                                 g.service.results.url);
                var execClient = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url, g.longTimeoutMsec);

                statusCallback(1);
                execClient.execAsyncDispatchFilterFromNodeGroup(runNodegroup, conn, runId, null, rtConstraints, jsonCallback, failureCallback);

            }
        });
    };

    var doQueryLoadFile = function (file) {
    	var r = new FileReader();

    	r.onload = function (name) {
                checkAnythingUnsavedThen(doQueryLoadJsonStr.bind(this, r.result, name));

    	}.bind(this, file.name);
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
                                          doNodeGroupDownload(undefined, function() {
                                              nodeGroupChanged(false);
                                              action();
                                          });
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

	// load a nodegroup
	//
	// jsonStr - str : sgjson string
	// optNgName - str : nodegroup name
	// optSkipValidation - boolean : don't validate
	// optForceKeepCurrent - boolean : keep current connection if any w/o asking
	// optCallback - func() : call this when done
    var doQueryLoadJsonStr = function(jsonStr, optNgName, optSkipValidation, optForceKeepCurrent, optCallback) {
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
				clearNodeGroup();
			}

			if (optForceKeepCurrent) {
				// force use current conn if loaded
				if (gConn) {
                    doQueryLoadFile2(sgJson, optNgName, optSkipValidation, optCallback);
                } else if (conn == null) {
				    ModalIidx.alert("No connection", "This JSON has no connection information.<br>Load a connection first.");
				} else {
                    doQueryLoadConn(sgJson, conn, optNgName, optSkipValidation, optCallback);
                }
           
			} else if (conn == null) {
            	// no conn provided in json
            
                if (!gConn) {
                    ModalIidx.alert("No connection", "This JSON has no connection information.<br>Load a connection first.");
                    clearNodeGroup();
                } else {
                    ModalIidx.alert("No connection", "This JSON has no connection information.<br>Attempting to load against existing connection.");
                    doQueryLoadFile2(sgJson);
                }

            // have to choose between old and new connection
            } else if (gConn && ! conn.equals(gConn, true)) {
                ModalIidx.choose("New Connection",
                                 "Nodegroup is from a different SPARQL connection<br><br>Which one do you want to use?",
                                 ["Cancel",     "Keep Current",                     "From Nodegroup"],
                                 [function(){}, doQueryLoadFile2.bind(this, sgJson, optNgName, optSkipValidation), doQueryLoadConn.bind(this, sgJson, conn, optNgName, optSkipValidation)]
                                 );

            // use the new connection
            } else if (!gConn) {
                doQueryLoadConn(sgJson, conn, optNgName, optSkipValidation);

            // keep the old connection
            } else {
                doQueryLoadFile2(sgJson, optNgName, optSkipValidation);
            }

		});

    };

    /*
     * loads connection and makes call to load rest of sgJson
     * part of doQueryLoadJsonStr callback chain
     */
    var doQueryLoadConn = function(sgJson, conn, optNgName, optSkipValidation, optCallback) {
    	require(['sparqlgraph/js/sparqlgraphjson',
                 'sparqlgraph/js/modaliidx'],
                function(SparqlGraphJson, ModalIidx) {

            var existName = gLoadDialog.connectionIsKnown(conn, true);     // true: make this selected in cookies

            // function pointer for the thing we do next no matter what happens:
            //    doLoadConnection() with doQueryLoadFile2() as the callback
            var doLoadConnectionCall = doLoadConnection.bind(this, conn, doQueryLoadFile2.bind(this, sgJson, optNgName, optSkipValidation, optCallback));

            if (! existName) {
                // new connection: ask if user wants to save it locally
                ModalIidx.choose("New Connection",
                                 "Connection is not saved locally.<br><br>Do you want to save it?",
                                 ["Cancel",     "Don't Save",                     "Save"],
                                 [function(){},
                                  function() {
												doLoadConnectionCall()
											},
                                  function(){ 
												gLoadDialog.addConnection(conn);
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
     * All nodegroup loading passes through here
     *
     * @param {JSON} grpJson    node group
     * @param {JSON} importJson import spec
     */
    var doQueryLoadFile2 = function(sgJson, optNgName, optSkipValidation, optCallback) {
    	// by the time this is called, the correct oInfo is loaded.
    	// and the gNodeGroup is empty.
    	require(['sparqlgraph/js/modaliidx', 'sparqlgraph/js/iidxhelper', 'sparqlgraph/js/msiclientnodegroupservice', 'sparqlgraph/js/sparqlgraphjson'],
                function(ModalIidx, IIDXHelper, MsiClientNodeGroupService, SparqlGraphJson) {

            clearNodeGroup();
            logEvent("SG Loaded Nodegroup");

            var newNgName = (optNgName != undefined) ? optNgName.replace(".json","") : null;
			
            try {
                // get nodegroup explicitly w/o the oInfo
                sgJson.getNodeGroup(gNodeGroup);
                gNodeGroup.setSparqlConnection(gConn);
                gPlotSpecsHandler = sgJson.getPlotSpecsHandler();

                setQueryTypeSelect();
            } catch (e) {
                // real non-model error loading the nodegroup
                console.log(e.stack);
                clearNodeGroup();
                ModalIidx.choose("Error reading nodegroup JSON",
                 (e.hasOwnProperty("message") ? e.message : e) + "<br><hr>Would you like to save a copy of the nodegroup?",
                 ["Yes", "No"],
                 [IIDXHelper.downloadFile.bind(this, sgJson.stringify(), "nodegroup_err.json", "text/csv;charset=utf8"), function(){}]
                 );
                return;
            }

            // get ready to draw, and set mapping tab
            guiGraphNonEmpty();
            gMappingTab.load(gNodeGroup, gConn, sgJson.getImportSpecJson());

			if (optSkipValidation) {
				nodeGroupChanged(false, newNgName);
        		saveUndoState();
        		buildQuery(optCallback);
			} else {
	            // inflate and validate the nodegroup
	            var client = new MsiClientNodeGroupService(g.service.nodeGroup.url, validateFailure.bind(this));
	            client.execAsyncInflateAndValidate(new SparqlGraphJson(gConn, gNodeGroup, gMappingTab.getImportSpec()), validateCallback.bind(this, newNgName, optCallback), validateFailure);
	        }
        });
    };
    var validateFailure = function(msgHtml) {
        require(['sparqlgraph/js/modaliidx'], function (ModalIidx) {
            doNodeGroupDownload(undefined, function() {
                ModalIidx.alert("Nodegroup validation call failed", msgHtml + "<hr>Nodegroup downloaded", false);
                clearNodeGroup();
            });
        });
    };

    // callback: successfully determined whether there are modelErrors
    var validateCallback = function(newNgName, optSuccessCallback, nodegroupJson, modelErrors, invalidItemStrings, warnings) {

        if (modelErrors.length > 0) {
            // some errors
            require(['sparqlgraph/js/modaliidx'], function (ModalIidx) {
                var msgHtml = "<list>Nodegroup validation errors:<li>" + modelErrors.join("</li><li>") + "</li></list>";

                if (warnings.length > 0) {
                    msgHtml += "<list>Auto-corrected these errors:<li>" + warnings.join("</li><li>") + "</li></list>";
                }
                // skip warnings.  they only make it more confusing to user.
                ModalIidx.alert("Error: model mis-matches", msgHtml, false);
            });
            gInvalidItems = invalidItemStrings;
            setStatus("Nodegroup is not valid to ontology");
        } else {
            if (warnings.length > 0) {
                require(['sparqlgraph/js/modaliidx'], function (ModalIidx) {
                    var msgHtml = "Nodegroup errors have been corrected to match model. <br><b>Re-save the nodegroup</b><list><li>" + warnings.join("</li><li>") + "</li></list>";
                    ModalIidx.alert("Warning: corrected model mis-matches", msgHtml, false);
                });
            }
            // no errors
            gInvalidItems = [];
            setStatus("");
        }

        // do either way

        gNodeGroup = new SemanticNodeGroup();
        gNodeGroup.addJson(nodegroupJson);
        nodeGroupChanged(false, newNgName);
        saveUndoState();
        buildQuery(optSuccessCallback);
    };

    var reValidateNodegroup = function(successCallback, optSkipSuccessDialog) {
        require(['sparqlgraph/js/msiclientnodegroupservice', 'sparqlgraph/js/sparqlgraphjson'],
                function(MsiClientNodeGroupService, SparqlGraphJson) {
            // inflate and validate the nodegroup
            var client = new MsiClientNodeGroupService(g.service.nodeGroup.url, validateFailure.bind(this));
            client.execAsyncInflateAndValidate(new SparqlGraphJson(gConn, gNodeGroup), reValidateCallback.bind(this, successCallback, optSkipSuccessDialog), validateFailure);
        });
    };

    // callback: successfully determined whether there are modelErrors
    // don't re-display errors.
    var reValidateCallback = function(callback, optSkipSuccessDialog, nodegroupJson, modelErrors, invalidItemStrings, warnings) {

        gInvalidItems = invalidItemStrings;

        gNodeGroup = new SemanticNodeGroup();
        gNodeGroup.addJson(nodegroupJson);
        saveUndoState();
        nodeGroupChanged(true);
        buildQuery();

        if (modelErrors.length == 0 && !optSkipSuccessDialog) {
            require(['sparqlgraph/js/modaliidx'], function (ModalIidx) {
                setStatus("");
                var msgHtml = "Nodegroup is now valid against ontology.";
                if (warnings.length > 0) {
                    msgHtml = "<list>Auto-corrected additional errors:<li>" + warnings.join("</li><li>") + "</li></list><br>" + msgHtml;
                }
                ModalIidx.alert("Validation success", msgHtml, false, callback);
            });

        } else if (callback != undefined) {
            callback();
        }
        gNodegroupInvalidItems = invalidItemStrings;

        gNodeGroup = new SemanticNodeGroup();
        gNodeGroup.addJson(nodegroupJson);
        saveUndoState();
        nodeGroupChanged(true);
        buildQuery();
    };

    var doNodeGroupUpload = function () {
    	// menu pick callback
    	logEvent("SG menu: File->Upload");
		require(['sparqlgraph/js/iidxhelper'], function(IIDXHelper) {
            IIDXHelper.fileDialog(doQueryLoadFile);
        });
    };

    var doNodeGroupDownload = function (optDeflateFlag, optCallback) {
        let deflateFlag = (typeof optDeflateFlag != "undefined") ? optDeflateFlag : true;
    	logEvent("SG menu: File->Download");
    	if (gNodeGroup == null || gNodeGroup.getNodeCount() == 0) {
    		logAndAlert("Query canvas is empty.  Nothing to download.");

    	} else {
    		require(['sparqlgraph/js/sparqlgraphjson',
                     'sparqlgraph/js/iidxhelper'],
                    function(SparqlGraphJson, IIDXHelper) {
    			// make sure importSpec is in sync
    			gMappingTab.updateNodegroup(gNodeGroup, gConn);

				var sgJson = new SparqlGraphJson(gConn, gNodeGroup, gMappingTab.getImportSpec(), deflateFlag, gPlotSpecsHandler);

				// name for downloaded file
				var filename;
				if( gNodeGroupName ) {
					if (!gNodeGroupChangedFlag) {
						fileName = gNodeGroupName + ".json";
					} else {
						fileName = gNodeGroupName + " - modified.json";
					}
					nodeGroupChanged(false);
				} else {
					fileName = "nodegroup.json";
					nodeGroupChanged(false, fileName);
				}

				IIDXHelper.downloadFile(sgJson.stringify(), fileName, "text/csv;charset=utf8");

                if (optCallback) {
                    optCallback();
                }
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
        var e = document.getElementById("optionNgPathFinding");
        e.disabled=false;
        e.style.backgroundColor="white";
        require(['sparqlgraph/js/modaliidx'],
             function (ModalIidx) {
                ModalIidx.alert("Test mode", "Nodegroup path-finding is enabled.<br>Test mode<br><b>Warning:<b>this has been known to lock up a triplestore.", false);
            });
    };

    // append user button to an elem
    // silently doing nothing if /user authentication fails
    var authenticate = function(buttonParent) {
        var gotUser = function(elem, user) {
            gUserName = user;
            if (user != null) {
                require(['sparqlgraph/js/iidxhelper'], function(IIDXHelper) {
                    elem.appendChild(IIDXHelper.buildUserIdButton(user, window.location.origin + "/logout"));
                });
            }
        };

        getUser(gotUser.bind(this, buttonParent));
    };

    // call callback with user_name or null
    var getUser = function (callback) {
        var success = function(x) {
            if (x.hasOwnProperty("name")) {
                callback(x.name);
            } else {
                console.log("/user did not return a name: \n" + x);
                callback(null);
            }

        };
        var failure = function(x) {
            console.log("/user callback failed");
            callback(null);
        }
        $.get(window.location.origin + g.userEndpoint, success).fail(failure);
    };

    var editOrderBy = function () {

        require(['sparqlgraph/js/modalorderbydialog'],
    	         function (ModalOrderByDialog) {

            var callback = function (x) {

                gNodeGroup.setOrderBy(x);
                saveUndoState();
                nodeGroupChanged(true);
            };

            var dialog = new ModalOrderByDialog(gNodeGroup.getReturnedSparqlIDs(), gNodeGroup.getOrderBy(), callback);
            dialog.launchOrderByDialog();
        });
   	};

    var editGroupBy = function () {

        require(['sparqlgraph/js/modalgroupbydialog'],
    	         function (ModalGroupByDialog) {

            var callback = function (x) {

                gNodeGroup.setGroupBy(x);
                saveUndoState();
                nodeGroupChanged(true);
            };

            var skipFuncs = true;
            var dialog = new ModalGroupByDialog(gNodeGroup.getReturnedSparqlIDs(skipFuncs), gNodeGroup.getGroupBy(), callback);
            dialog.launch();
        });
   	};

	// ping all services
	// send (possibly empty) list of down services to servicesPingCallback(list)
	var checkServicesOnce = function (servicesPingCallback) {
        require(['sparqlgraph/js/microserviceinterface',
                 'sparqlgraph/js/modaliidx'],
    	         function (MicroServiceInterface, ModalIidx) {

			var urlsDownList = [];
			var urlsPinged = 0;
			          
            var pingCallback = function(url, resultSet_or_html) {
				urlsPinged += 1;
                // failure callbacks use a string parameter
                if (typeof(resultSet_or_html) == "string") {         
					urlsDownList.push(url);
                } 
                
                if (urlsPinged == Object.keys(g.service).length) {
					servicesPingCallback(urlsDownList);
				}
            };
          
            for (var key in g.service) {
                if (g.service.hasOwnProperty(key)) {
                    var url = g.service[key].url;
                    var msi = new MicroServiceInterface(url);
                    msi.ping(pingCallback.bind(this, url),
                             pingCallback.bind(this, url),
                             4000);
                }
            }
        });
    };


    var gStopChecking = true;

    var checkServices = function (optMsgHTML) {
        require(['sparqlgraph/js/microserviceinterface',
                 'sparqlgraph/js/modaliidx'],
    	         function (MicroServiceInterface, ModalIidx) {

            // build div with all service names and ...
            var div = document.createElement("div");
            if (optMsgHTML) {
				div.innerHTML += optMsgHTML;
			}
            for (var key in g.service) {
                if (g.service.hasOwnProperty(key)) {
                    div.innerHTML += g.service[key].url + ' is .</br>';
                }
            }

            var m = new ModalIidx();
            m.showOK("Ping services every 5 sec", div,  function(){ gStopChecking = true; });

            // replaces the line with the service url with a result
            var pingCallback = function(div, url, resultSet_or_html) {

                // failure callbacks use a string parameter
                if (typeof(resultSet_or_html) == "string") {
                //    ModalIidx.alert("Microservice Ping Failed", resultSet_or_html)
                    div.innerHTML = div.innerHTML.replace(new RegExp(url + "[^\.]*"), url + ' is <font color="red">down</font>' );

                } else {
                    div.innerHTML = div.innerHTML.replace(new RegExp(url + "[^\.]*"), url + ' is <font color="green">up</font>' );
                }
            }.bind(div);

            var showWaiting = function(url) {
                div.innerHTML = div.innerHTML.replace(new RegExp(url + "[^\.]*"), url + ' is <font color="gold">waiting</font>' );
            };

            var checkAll = function() {
                if (gStopChecking) return;

                for (var key in g.service) {
                    if (g.service.hasOwnProperty(key)) {
                        var url = g.service[key].url;
                        showWaiting(url);
                        var msi = new MicroServiceInterface(url);
                        msi.ping(pingCallback.bind(this, div, url),
                                 pingCallback.bind(this, div, url),
                                 4000);
                    }
                }

                setTimeout(checkAll, 5000);  // repeat every 5 sec
            };

            // launch service for each
            gStopChecking = false;
            setTimeout(checkAll, 1);   // make the first call async

        });

    };

    var doCancel = function() {
        gCancelled = true;
    };

    var checkForCancel = function() {
        if (gCancelled) {
            gCancelled = false;
            return true;
        } else {
            return false;
        }
    };

    var runGraphByQueryType = function (optRtConstraints) {
        var rtConstraints = (typeof optRtConstraints == "undefined") ? null : optRtConstraints;

    	require(['sparqlgraph/js/msiclientnodegroupexec',
                 'sparqlgraph/js/modaliidx'],
    	         function (MsiClientNodeGroupExec, ModalIidx) {

            var runViaServices = true;
            guiDisableAll(runViaServices);
    		var client = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url, g.longTimeoutMsec);

            var csvJsonCallback = MsiClientNodeGroupExec.buildCsvUrlSampleJsonCallback(RESULTS_MAX_ROWS,
                                                                                     queryTableResCallback,
                                                                                     asyncFailureCallback,
                                                                                     setStatusProgressBar.bind(this, "Running Query"),
                                                                                     this.checkForCancel.bind(this),
                                                                                     g.service.status.url,
                                                                                     g.service.results.url);

            var jsonLdCallback = MsiClientNodeGroupExec.buildJsonLdCallback(queryJsonLdCallback,
                                                                            asyncFailureCallback,
                                                                            setStatusProgressBar.bind(this, "Running Query"),
                                                                            this.checkForCancel.bind(this),
                                                                            g.service.status.url,
                                                                            g.service.results.url);
            setStatusProgressBar("Running Query", 1);
            switch (gNodeGroup.getQueryType()) {
    			case SemanticNodeGroup.QT_DISTINCT:
                    client.execAsyncDispatchSelectFromNodeGroup(gNodeGroup, gConn, null, rtConstraints, csvJsonCallback, asyncFailureCallback);
                    break;
    			case SemanticNodeGroup.QT_COUNT:
                    client.execAsyncDispatchCountFromNodeGroup(gNodeGroup, gConn, null, rtConstraints, csvJsonCallback, asyncFailureCallback);
                    break;
                case SemanticNodeGroup.QT_CONSTRUCT:
                    // Results service has trouble protecting the browser memory on CONSTRUCT queries
                    // so use a different strategy of putting a limit on the query
                    var realLimit = gNodeGroup.getLimit();
                    gNodeGroup.setLimit(Math.min(realLimit, RESULTS_MAX_ROWS));
                    client.execAsyncDispatchConstructFromNodeGroup(gNodeGroup, gConn, null, rtConstraints, jsonLdCallback, asyncFailureCallback);
                    gNodeGroup.setLimit(realLimit);
                    break;
    			case SemanticNodeGroup.QT_DELETE:
                    var okCallback = client.execAsyncDispatchDeleteFromNodeGroup.bind(client, gNodeGroup, gConn, null, rtConstraints, csvJsonCallback, asyncFailureCallback);
                    var cancelCallback = function () {
                        guiUnDisableAll();
                        setStatus("");
                    };

                    ModalIidx.okCancel("Delete query", "Confirm SPARQL DELETE operation.", okCallback, "Run Delete", cancelCallback);
                    break;
                case SemanticNodeGroup.QT_ASK:
                	// newer generic endpoint could be used by others above
                	client.execAsyncDispatchQueryFromNodeGroup(gNodeGroup, gConn, SemanticNodeGroup.QT_ASK, null, rtConstraints, csvJsonCallback, asyncFailureCallback);
                    break;
                default:
                    throw new Error("Internal error: Unknown query type.");
			}

    	});

    };

    var runQueryText = function () {
        require(['sparqlgraph/js/msiclientnodegroupexec',
    	         'sparqlgraph/js/modaliidx'],
    	         function (MsiClientNodeGroupExec, ModalIidx) {

    		var client = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url, g.service.status.url, g.service.results.url, g.longTimeoutMsec);

             var csvJsonCallback = MsiClientNodeGroupExec.buildCsvUrlSampleJsonCallback(RESULTS_MAX_ROWS,
                                                                                      queryTableResCallback,
                                                                                      asyncFailureCallback,
                                                                                      setStatusProgressBar.bind(this, "Running Query"),
                                                                                      this.checkForCancel.bind(this),
                                                                                      g.service.status.url,
                                                                                      g.service.results.url);
                        var jsonLdCallback = MsiClientNodeGroupExec.buildJsonLdCallback(queryJsonLdCallback,
                                                                            asyncFailureCallback,
                                                                            setStatusProgressBar.bind(this, "Running Query"),
                                                                            this.checkForCancel.bind(this),
                                                                            g.service.status.url,
                                                                            g.service.results.url);
            guiDisableAll();
            setStatusProgressBar("Running Query", 1);
            var sparql = document.getElementById('queryText').value;
            var sel = document.getElementById("SGQueryType");

            if (sparql.toLowerCase().indexOf("delete") > -1) {
                var okCallback = client.execAsyncDispatchRawSparql.bind(client, sparql, gConn, csvJsonCallback, asyncFailureCallback);

                var cancelCallback = function () {
                    guiUnDisableAll();
                    setStatus("");
                };

                ModalIidx.okCancel("Delete query", "Query may write / delete triples.<br>Confirm you want to run this query.", okCallback, "Run Query", cancelCallback);
            
            } else if (sel.options[sel.selectedIndex].value == SemanticNodeGroup.QT_CONSTRUCT ){
	 			// query type menu is set to CONSTRUCT
	 			// another way:  sparql.toLowerCase().indexOf("construct") > -1
                client.execAsyncDispatchRawSparql(sparql, gConn, jsonLdCallback, asyncFailureCallback, "GRAPH_JSONLD");
            
            } else {
                client.execAsyncDispatchRawSparql(sparql, gConn, csvJsonCallback, asyncFailureCallback, "TABLE");
            }

    	});
    };

    var asyncFailureCallback = function (html) {
        require(['sparqlgraph/js/modaliidx'],
                function(ModalIidx) {

            ModalIidx.alert("Failure", html);
            guiUnDisableAll();
            setStatus("");
        });
    };

    /*
     * Results success.  Display them.
     * @private
     */
    var queryTableResCallback = function (csvFilename, fullURL, tableResults) {
        require([   'sparqlgraph/js/iidxhelper',
                    'sparqlgraph/js/modaliidx',
                    'sparqlgraph/js/modalplotsdialog',
                    'sparqlgraph/js/msiclientresults',
                    'sparqlgraph/js/msiclientnodegroupservice',
                    'sparqlgraph/js/plotlyplotter',
                    'sparqlgraph/js/sparqlgraphjson'
                ],
                function(IIDXHelper, ModalIidx, ModalPlotsDialog, MsiClientResults, MsiClientNodeGroupService, PlotlyPlotter, SparqlGraphJson) {
            var headerHtml = "";
            if (tableResults.getRowCount() >= RESULTS_MAX_ROWS) {
                headerHtml = "<span class='label label-warning'>Showing first " + RESULTS_MAX_ROWS.toString() + " rows. </span> ";
            }
            headerHtml += "Full csv: <a href='" + fullURL + "' download>"+ csvFilename + "</a>";

            tableResults.setLocalUriFlag(! getQueryShowNamespace());
            tableResults.setEscapeHtmlFlag(true);
            tableResults.setAnchorFlag(true);
            var resultsClient = new MsiClientResults(g.service.results.url, "no_job");
            tableResults.tableApplyTransformFunctions(resultsClient.getResultTransformFunctions());
            var noSort = [];

            var resultsPara = document.getElementById("resultsParagraph");
            var resultsDiv = document.createElement("div");



            var plotter = null;
            var select = undefined;
            var textValArray = [["table", -1]];
            var selectedTexts = ["table"];

            // add plots if any
            if (gPlotSpecsHandler) {
                // get plotter or null
                plotter = gPlotSpecsHandler.getDefaultPlotter();
                var plotNames = gPlotSpecsHandler.getNames();

                for (var i=0; i < plotNames.length; i++) {
                    textValArray.push([plotNames[i], i]);
                }

                if (plotter) {
                    selectedTexts = [plotter.getName()];
                }
            }

            // build column order button

            var butColOrder = IIDXHelper.createIconButton("icon-random", null,  ["btn"], undefined, "Save column order");
            butColOrder.onclick = saveColumnOrder.bind(this, butColOrder);

            // build select
            select = IIDXHelper.createSelect(null, textValArray, selectedTexts);
            select.onchange = function(select, butCO) {
                var i = parseInt(select.value);

                // empty w/o shrinking so screen might now bounce
                resultsDiv.style.minHeight = resultsDiv.offsetHeight + "px";
                resultsDiv.innerHTML = "";

                if (i < 0) {
                    tableResults.putTableResultsDatagridInDiv(resultsDiv, undefined, noSort);
                    butCO.disabled = false;
                } else {
                    gPlotSpecsHandler.getPlotter(i).addPlotToDiv(resultsDiv, tableResults);
                    butCO.disabled = true;
                }
            }.bind(this, select, butColOrder);

            // build plots button
            var plotsCallback = function(plotSpecsHandler) {
                gPlotSpecsHandler = plotSpecsHandler;
                queryTableResCallback(csvFilename, fullURL, tableResults);
            }.bind(this);

            var sgJson = new SparqlGraphJson(gConn, gNodeGroup, gMappingTab.getImportSpec(), true, gPlotSpecsHandler);
            var ngsClient = new MsiClientNodeGroupService(g.service.nodeGroup.url, ModalIidx.alert.bind(this, "NodeGroup Service failure"));
            var plotsDialog = new ModalPlotsDialog(ngsClient, sgJson, tableResults, plotsCallback);

            var plotsLauncher = function(dialog, sel) {
                dialog.show(parseInt(sel.value));
            }.bind(this, plotsDialog, select);

            var butPlots = IIDXHelper.createIconButton("icon-picture", plotsLauncher, ["btn"], undefined, "Plots");

            // assemble the span
            var span = document.createElement("span");
            span.style.margin="2";
            select.style.margin="0";
            span.appendChild(select);
            span.appendChild(document.createTextNode(" "));
            span.appendChild(butPlots);
            span.appendChild(document.createTextNode(" "));
            span.appendChild(butColOrder);

            // add header to results
            var headerTable = IIDXHelper.buildResultsHeaderTable(headerHtml, ["Save table csv"], [tableResults.tableDownloadCsv.bind(tableResults)], span);
            resultsPara.innerHTML = "";
            resultsPara.appendChild(headerTable);
            resultsPara.appendChild(resultsDiv);

			var scrollCallback = function() { myScrollIntoViewIfNeeded(resultsDiv); }
            // display results
            var plotter = gPlotSpecsHandler == null ? null : gPlotSpecsHandler.getDefaultPlotter();
            if (plotter != null) {
                plotter.addPlotToDiv(resultsDiv, tableResults, undefined, scrollCallback);
                butColOrder.disabled = true;
            } else {
                tableResults.putTableResultsDatagridInDiv(resultsDiv, scrollCallback, noSort);
                butColOrder.disabled = false;
            }
			
            guiUnDisableAll();
            guiResultsNonEmpty();
            setStatus("");
         });
    };

    var saveColumnOrder = function(button) {
        // make it a little clear that something happened
        button.classList.add("btn-info");
        setTimeout(function(){button.classList.remove("btn-info")}, 600);

        var tables = document.getElementById("resultsParagraph").getElementsByTagName("table");
        if (tables == null || tables.length < 2) {
            throw new Error("Internal: no table in the results section");
        }
        var thList = tables[1].getElementsByTagName("thead")[0].getElementsByTagName("tr")[0].getElementsByTagName("th");
        var colNames = [];
        for (var th of thList) {
            colNames.push('?'+th.innerHTML);
        }
        gNodeGroup.setColumnOrder(colNames);
    };

    // simplified status callback for quick-ish network operations
    var networkBusy = function(canvasDiv, flag) {
        var canvas = canvasDiv.getElementsByTagName("canvas")[0];
        canvas.style.cursor = (flag ? "wait" : "");
    };

    var networkFailureCallback = function (canvas, html) {
        require(['sparqlgraph/js/modaliidx'],
                function(ModalIidx) {

            ModalIidx.alert("Service Failure", html);
            networkBusy(canvas, false);
        });
    };

    var queryJsonLdCallback = function(jsonLdResults) {
        require(['sparqlgraph/js/iidxhelper'], function(IIDXHelper) {

            var anchor = IIDXHelper.buildAnchorWithCallback(    "results.json",
                                                                function (res) {
                                                                    var str = JSON.stringify(res.getGraphResultsJsonArr(), null, 4);
                                                                    IIDXHelper.downloadFile(str, "results.json",  "application/json");
                                                                }.bind(this, jsonLdResults)
                                                            );

            var linkdom = document.createElement("span");
            linkdom.innerHTML =  "Download json: ";
            linkdom.appendChild(anchor);

            setStatus("");
            displayConstructResults(jsonLdResults, linkdom);

            guiUnDisableAll();
            guiResultsNonEmpty();
        });
    };

    /**
     * Put json-ld results into a visjs display
     *
     * params:
     *    res - json results
     *    linkdom - standard SPARQLgraph link to download results
     */
    var displayConstructResults = function (res, linkdom) {
		
        require(['sparqlgraph/js/iidxhelper',
                 'sparqlgraph/js/modaliidx',
                'sparqlgraph/js/visjshelper',
                'visjs/vis.min'],
                function(IIDXHelper, ModalIidx, VisJsHelper, vis) {
	
            var div = document.getElementById("resultsParagraph");

            if (! res.isJsonLdResults()) {
                div.innerHTML =  "<b>Error:</b> Results returned from service are not JSON-LD";
                return;
            }
			
			
			var rawJsonArr = res.getGraphResultsJsonArr();
			if (rawJsonArr.length >= RESULTS_MAX_ROWS) {
                div.innerHTML =  "<span class='label label-warning'>Graphing first " + RESULTS_MAX_ROWS.toString() + " data points. </span>";
            }
            
            var jsonDownloadStr = JSON.stringify(rawJsonArr, null, 4);

            // make a menu button bar
            var editDom = document.createElement("span");
            editDom.appendChild(document.createTextNode("Results: "));
            var removeButton = IIDXHelper.createIconButton("icon-trash", function(){}, ["btn","btn-danger"], undefined, "Remove", "Remove selected item(s) from this display" );
            removeButton.disabled = true;
            editDom.appendChild(removeButton);
            IIDXHelper.appendSpace(editDom);
            
            var expandButton = IIDXHelper.createIconButton("icon-sitemap", function(){}, ["btn"], undefined, "Expand", "Add all connected instance data" );
            expandButton.disabled = true;
            editDom.appendChild(expandButton);
            IIDXHelper.appendSpace(editDom);
       
            var buildButton = IIDXHelper.createIconButton("icon-gears", function(){}, ["btn"], undefined, "Build", "Build a new nodegroup from selected item(s)" );
            buildButton.disabled = true;
            editDom.appendChild(buildButton);

			// new feature section
			var NG_BUTTON_FEATURE=false;
			if (NG_BUTTON_FEATURE) {
				for (var i=0; i < 5; i++) {
					IIDXHelper.appendSpace(editDom);
				}
				editDom.appendChild(document.createTextNode("Nodegroup: "));
	            var addToNgButton = IIDXHelper.createIconButton("icon-plus", function(){}, ["btn"], undefined, "Add", "Add to nodegroup" );
	            addToNgButton.disabled = true;
	            editDom.appendChild(addToNgButton);
	        }
           	
            var headerTable = IIDXHelper.buildResultsHeaderTable(
                (jsonDownloadStr === "{}") ? "No results returned" : linkdom,
                [ "Save JSON" ] ,
                [ IIDXHelper.downloadFile.bind(IIDXHelper, jsonDownloadStr, "results.json", "text/json;charset=utf8") ],
                editDom
            );
            div.appendChild(headerTable);
            var hr = document.createElement("hr");
            hr.style.margin="2px";
            div.appendChild(hr);

            // canvas
            var canvasDiv = document.createElement("div");
            canvasDiv.style.width="100%";
            canvasDiv.style.height="650px";
            canvasDiv.style.margin="1ch";
            div.appendChild(canvasDiv);

            // add network config to bottom
            var configDiv = document.createElement("div");
            configDiv.style.width="100%";
            configDiv.style.height="100%";
            div.appendChild(document.createElement("hr"));
            div.appendChild(configDiv);

            // setup empty network
            var nodeDict = {};   // dictionary of nodes with @id as the key
            var edgeList = [];   // "normal" list of edges

            var options = VisJsHelper.getDefaultOptions(configDiv);
            options = VisJsHelper.setCustomEditingOptions(options);
			options.interaction.selectConnectedEdges = false;
            var network = new vis.Network(canvasDiv, {nodes: Object.values(nodeDict), edges: edgeList }, options);

            // callback: when selection changes, disable/enable buttons
            network.on('select', function(n) {
                // count non-data nodes
                var nodeCount = n.getSelectedNodes().length;
                var edgeCount = n.getSelectedEdges().length;
                var nonDataNodeCount = 0;
                
                for (var id of n.getSelectedNodes()) {
                    if (network.body.data.nodes.get(id).group != VisJsHelper.DATA_NODE) {
                        nonDataNodeCount += 1;
                    }
                };
               
                removeButton.disabled = (nodeCount == 0) && (edgeCount == 0);
                expandButton.disabled = (nodeCount == 0);
                buildButton.disabled = (nodeCount == 0);
            }.bind(this, network));

            // button callbacks
            network.on('doubleClick', this.constructExpandCallback.bind(this, res, canvasDiv, network));
            expandButton.onclick = this.constructExpandCallback.bind(this, res, canvasDiv, network);
            buildButton.onclick = this.constructBuildNodegroupCallback.bind(this, network);
            removeButton.onclick = this.constructRemoveCallback.bind(this, network);

			myScrollIntoViewIfNeeded(div);
			
            // add data
            var jsonLd = res.getGraphResultsJsonArr(true, true, true);
            jsonLd.slice(0, RESULTS_MAX_ROWS);
			addDataToNetwork(network, jsonLd, nodeDict, edgeList);
        });
    };
    
    // scroll into view if the top isn't already showing
    var myScrollIntoViewIfNeeded = function (el) {
		var r = el.getBoundingClientRect();
		
		if (r.top <= 0 || r.top > window.innerHeight) {
			el.scrollIntoView();
		}
	};
	
    //
    // add data to network one CHUNK_SIZE at a time
    // use setTimeout to keep UI updates smooth
    // update progress bar
    //
    var addDataToNetwork = function(network, jsonLd, nodeDict, edgeList, optStart) {
		require(['sparqlgraph/js/modaliidx', 'sparqlgraph/js/msiclientnodegroupservice', 'sparqlgraph/js/visjshelper'
					    ], function(ModalIidx, MsiClientNodeGroupService, VisJsHelper) {
			var CHUNK_SIZE = 400;
			var thisStart = optStart || 0;
			var nextStart = Math.min(jsonLd.length, thisStart + CHUNK_SIZE);
			
			setStatusProgressBar("Rendering results graph", 100 * thisStart/jsonLd.length);
			for (var i=thisStart; i < nextStart; i++) {
				VisJsHelper.addJsonLdObject(jsonLd[i], nodeDict, edgeList);
			}
			
			network.body.data.nodes.update(Object.values(nodeDict));
	        network.body.data.edges.update(edgeList);
	        
	        if (! gCancelled && nextStart < jsonLd.length) {
				// recurse
	        	setTimeout(addDataToNetwork.bind(this, network, jsonLd, nodeDict, edgeList, nextStart), 1);
	        } else {
				if (gCancelled) {
					ModalIidx.alert("Cancelled", "Rendering of network cancelled by user.<br>Network is incomplete.");
				}
				// done
				setStatus("");
				network.startSimulation();
			}
	     });
	};

    var constructRemoveCallback = function(n) {
        var nodeList = n.getSelectedNodes();
        var edgeList = n.getSelectedEdges();
        n.body.data.nodes.remove(nodeList);
        n.body.data.edges.remove(edgeList);
    };
    
    var constructBuildNodegroupCallback = function(n) {
		require(['sparqlgraph/js/modaliidx', 'sparqlgraph/js/msiclientnodegroupservice', 'sparqlgraph/js/visjshelper'
				    ], function(ModalIidx, MsiClientNodeGroupService, VisJsHelper) {
	        var formatter = new SparqlFormatter();
	        var nodeList = n.getSelectedNodes();
	        var edgeList = n.getSelectedEdges();
	        var nodesParam = [];
	        var edgesParam = [];
	        var DATA = "::data::";
	        var classNodeCount = 0;
	        
	        sparqlIdHash = {};   // sparqlIdHash[ng_sparql_id] = vis_node_id
	        visIdHash = {};      // visIdHash[vis_node_id]     = ng_sparql_id if classed node, or DATA if data
	        
	        // pre-add any nodes that aren't selected but edges are selected
	        // GUI USER ASSIST 
	        for (var edgeId of edgeList) {
				var fromToIds = VisJsHelper.getNetworkEdgeFromTo(n, edgeId);
				if (nodeList.indexOf(fromToIds[0]) == -1) {
					nodeList.push(fromToIds[0]);
				}
				if (nodeList.indexOf(fromToIds[1]) == -1) {
					nodeList.push(fromToIds[1]);
				}
			}
	        
	        // build node inputs: sparqlid and class
	        for (var visId of nodeList) {
				var classURI = VisJsHelper.getNetworkNodeUri(n, visId);
				
				if (classURI) {
					// is a class node
					classNodeCount += 1;
					
					// generate a sparqlID and hash it both ways
					var sparqlId = formatter.genSparqlID(classURI.split("#")[1], sparqlIdHash);
					sparqlIdHash[sparqlId] = visId;
					visIdHash[visId] = sparqlId;
					
					nodesParam.push({ 'sparqlId': sparqlId, 'classURI':classURI});
				} else {
					
					// data node is ignored by the builder since it isn't a node
					visIdHash[visId] = DATA;
					
					// GUI USER ASSIST: if there is one edge to the data node and it isn't selected
					// then select it, presuming user forgot it and what else could they have wanted.
					var connectedEdges = n.getConnectedEdges(visId);
					var selectedFlag = false;
					for (var e of connectedEdges) {
						if (edgeList.indexOf(e) > -1)
							selectedFlag = true;
					}
					
					if (! selectedFlag) {
						ModalIidx.alert("Can Not Build", "Can't build nodegroup with disconnected data node.<br>Connect data to a class by selecting an edge.");
						return;
					}
				}
			}
			
			if (classNodeCount == 0) {
				ModalIidx.alert("Can Not Build", "Can't build nodegroup with only data nodes.  <br>Select at least one class node.");
				return;
			}
			
			for (var edgeId of edgeList) {
				var fromToIds = VisJsHelper.getNetworkEdgeFromTo(n, edgeId);
				var edgeUri = VisJsHelper.getNetworkEdgeUri(n, edgeId);
				
				// if both ends are nodes that are selected
				if (visIdHash[fromToIds[0]] &&  visIdHash[fromToIds[1]]) {
					// note: objectSparqlId might be DATA.  object SparqlIds of data properties are ignored 
					edgesParam.push({'subjectSparqlId' : visIdHash[fromToIds[0]], 'objectSparqlId': visIdHash[fromToIds[1]], 'propURI': edgeUri});
				}
			}
			
			var client = new MsiClientNodeGroupService(g.service.nodeGroup.url, buildQueryFailure.bind(this));
            client.execBuildNodeGroup(gConn, nodesParam, edgesParam, constructBuildNodeGroupCallback1);
       });

    };
    
    var constructBuildNodeGroupCallback1 = function(sgjson) {
	 	var name = "constructed";
	 	var ngJsonStr = JSON.stringify(sgjson.toJson());
		checkAnythingUnsavedThen(doQueryLoadJsonStr.bind(this, ngJsonStr, name));
	};

    // user clicked to add to CONSTRUCT graph
    var constructExpandCallback = function(origRes, canvasDiv, network) {
        require(['sparqlgraph/js/modaliidx', 'sparqlgraph/js/msiclientnodegroupexec', 'sparqlgraph/js/visjshelper'
			    ], function(ModalIidx, MsiClientNodeGroupExec, VisJsHelper) {

            networkBusy(canvasDiv, true);
            var client = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url, g.longTimeoutMsec);
            var jsonLdCallback = MsiClientNodeGroupExec.buildJsonLdCallback(
                constructExpandCallbackGotJson.bind(this, canvasDiv, network),
                networkFailureCallback.bind(this, canvasDiv),
                function() {}, // no status updates
                function() {}, // no check for cancel
                g.service.status.url,
                g.service.results.url);
            var idList = network.getSelectedNodes();
            var classList = [];
            for (var id of idList) {
                var classUri = network.body.data.nodes.get(id).group;
             	if (classUri == VisJsHelper.BLANK_NODE) {
					networkBusy(canvasDiv, false);
					ModalIidx.alert("Blank node error", "Can not expand a blank node returned from a previous query.")
				} else if (classUri == VisJsHelper.DATA_NODE) {
                    var instanceUri = id;
                    client.execAsyncConstructConnectedData(instanceUri, null, gConn, jsonLdCallback, networkFailureCallback.bind(this, canvasDiv));

                } else {
                    // get classname and instance name with ':' prefex expanded out to full '#' uri
                    var instanceUri = id;
                    client.execAsyncConstructConnectedData(instanceUri, "node_uri", gConn, jsonLdCallback, networkFailureCallback.bind(this, canvasDiv));
                }
            }
        });
    };

    var constructExpandCallbackGotJson = function(canvasDiv, network, res) {
        require(['sparqlgraph/js/modaliidx',
                 'sparqlgraph/js/visjshelper'],
                 function (ModalIidx, VisJsHelper) {
            if (! res.isJsonLdResults()) {
                ModalIidx.alert("NodeGroup Exec Service Failure", "<b>Error:</b> Results returned from service are not JSON-LD");
                return;
            }

            // add data
            var jsonLd = res.getGraphResultsJsonArr(true, true, true);
            var nodeDict = {};   // dictionary of nodes with @id as the key
            var edgeList = [];
            for (var i=0; i < jsonLd.length; i++) {
                VisJsHelper.addJsonLdObject(jsonLd[i], nodeDict, edgeList);
            }
            network.body.data.nodes.update(Object.values(nodeDict));
            network.body.data.edges.update(edgeList);
            networkBusy(canvasDiv, false);
        });

    };

	// has evolved to either retrieve or delete items from store
   	var doRetrieveFromNGStore = function() {
        // check that nodegroup is saved
        // launch the retrieval dialog
        // callback to the dialog is doQueryLoadJsonStr
        checkAnythingUnsavedThen(
            gStoreDialog.launchOpenStoreDialog.bind(gStoreDialog, doQueryLoadJsonStr, gReportTab.reloadNodegroupIDs.bind(gReportTab))
        );
    };

  	var doStoreNodeGroup = function () {

        require(['sparqlgraph/js/sparqlgraphjson'], function(SparqlGraphJson) {

            gMappingTab.updateNodegroup(gNodeGroup, gConn);

            // save user when done
            var doneCallback = function () {
                localStorage.setItem("SPARQLgraph_user", gStoreDialog.getUser());
                nodeGroupChanged(false, gStoreDialog.getId());
                gReportTab.reloadNodegroupIDs();
            }

            var sgJson = new SparqlGraphJson(gConn, gNodeGroup, gMappingTab.getImportSpec(), true, gPlotSpecsHandler);
            gStoreDialog.launchStoreDialog(JSON.stringify(sgJson.toJson()), gNodeGroupName, doneCallback);

        });

  	};

  	var doLayout = function() {
        gRenderer.showConfigDialog();
   	};

   	var doCollapseUnused = function() {
        gRenderer.drawCollapsingUnused(gOInfo, gInvalidItems);
    };

    var doExpandAll = function() {
        gRenderer.drawExpandAll(gOInfo, gInvalidItems);
    };

    // only used for non-microservice code
    // Almost DEPRECATED
    var getNamespaceFlag = function () {
		var ret = document.getElementById("SGQueryNamespace").checked? SparqlServerResult.prototype.NAMESPACE_YES: SparqlServerResult.prototype.NAMESPACE_NO;
		// for sparqlgraph we always want raw HTML in the results.  No links or markup, etc.
		return ret + SparqlServerResult.prototype.ESCAPE_HTML;
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

    var getQueryShowNamespace = function () {
    	return document.getElementById("SGQueryNamespace").checked;
    };

	//
    // Tell GUI that nodegroup has changed, and display needs updating
    // Check that ORDER BY and GROUP BY are valid, fix them
    // Fix button statuses:  Run, Group, Order
    // Params:
    //     flag - there are unsaved changes
    //     optNewName - only way that gNodeGroupName should be changed.  
    //                     undefined/null - leave as-is.  
    //                     "" - valid empty name.
    //
    var nodeGroupChanged = function(flag, optNewName) {
		gNodeGroupChangedFlag = flag;
		
		if (optNewName != undefined)
			gNodeGroupName = optNewName;
			
        guiUpdateGraphRunButton();

		setQueryTypeSelect();
		
        // check up ORDER BY and GROUP BY
        gNodeGroup.removeInvalidOrderBy();
        gNodeGroup.removeInvalidGroupBy();

        if (gNodeGroup.getOrderBy().length > 0) {
            document.getElementById("SGOrderBy").classList.add("btn-primary");
        } else {
            document.getElementById("SGOrderBy").classList.remove("btn-primary");
        }

        if (gNodeGroup.getGroupBy().length > 0) {
            document.getElementById("SGGroupBy").classList.add("btn-primary");
        } else {
            document.getElementById("SGGroupBy").classList.remove("btn-primary");
        }

        // check up on LIMIT
        var limit = gNodeGroup.getLimit();
        var elem = document.getElementById("SGQueryLimit");
        elem.value = (limit < 1) ? "" : limit;

		// draw canvas
        gRenderer.draw(gNodeGroup, gOInfo, gInvalidItems);
        
        if (flag) {
			// changes must be on cavas of query tab.  No load or validate has occurred.
            buildQuery();
            
        } else {
			// save or load
			
			// if we're on a different tab, set that tab's nodegroup since that usually occurs at tab-switching time
			if (gCurrentTab == g.tab.upload) {
				gUploadTab.setNodeGroup(gConn, gNodeGroup, gNodeGroupName, gOInfo, gMappingTab, gOInfoLoadTime);
			} else if (gCurrentTab == g.tab.mapping) {
            	gMappingTab.updateNodegroup(gNodeGroup, gConn);
			}
			
			// set various changed Flags.
            gMappingTab.setChangedFlag(false);
            queryTextChanged(false);
        }
        
        // update dislay of conn and ng name
        updateNgAndConnDisplay();
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

                gNodeGroup.setLimit(isNaN(newLimit) ? 0 : newLimit);
                saveUndoState();
                nodeGroupChanged(true);
            },

            // cancel
            function() {
                // put it back to the old value
                document.getElementById("SGQueryLimit").value = gNodeGroup.getLimit().toString();
            });

    };

    var onchangePathFindingMode = function() {
        if (getPathFindingMode() != 0) {
            // if not model, call for getPredicateStats() to make sure they're cached in service Layer
            // but don't even bother retrieving them
            require(['sparqlgraph/js/msiclientontologyinfo',
                    'sparqlgraph/js/msiclientstatus',
                    'sparqlgraph/js/msiclientresults',
                    'sparqlgraph/js/msiresultset'
                    ],
                function(MsiClientOntologyInfo, MsiClientStatus, MsiClientResults, MsiResultSet) {
                    var client = new MsiClientOntologyInfo(g.service.ontologyInfo.url);
                    var resultsCall = MsiClientResults.prototype.doNothing;  // don't try to get the actual predicate stats
                    var successCallback = function(){setStatus("");};                      // there is no callback from doNothing
                    var callback = buildStatusResultsCallback(successCallback, resultsCall, MsiClientStatus, MsiClientResults, MsiResultSet, 20, 100);
                    client.execGetPredicateStats(gConn, callback);
                }
            );
        }
    };

    // 0 "model", 1 "predicate data", 2 "nodegroup data"
    var getPathFindingMode = function() {
        return document.getElementById("selectPathFindingMode").value;
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

    //
    // Translate the query type select into gNodeGroup queryType and setReturnTypeOverride
    //
    var onchangeQueryType1 = function () {
        var s = document.getElementById("SGQueryType");
        var choice = s.options[s.selectedIndex].value;
        
        switch (choice) {
            case SemanticNodeGroup.QT_DISTINCT:
            case SemanticNodeGroup.QT_COUNT:
            case SemanticNodeGroup.QT_ASK:
            case SemanticNodeGroup.QT_CONSTRUCT:
            case SemanticNodeGroup.QT_DELETE:
                gNodeGroup.setQueryType(choice);
                gNodeGroup.setReturnTypeOverride(null);
                saveUndoState();
                break;
        
            default:
                throw new Error("Internal error: Unknown query type: " + choice);
        }
        gRenderer.draw(gNodeGroup, gOInfo, gInvalidItems);
    	document.getElementById('queryText').value = "";
        buildQuery();
    };

    setQueryTypeSelect = function() {
        require(['sparqlgraph/js/iidxhelper'],
            function(IIDXHelper) {
            var name;
            switch (gNodeGroup.getQueryType()) {
	            case SemanticNodeGroup.QT_COUNT:
	            case SemanticNodeGroup.QT_ASK:
	            case SemanticNodeGroup.QT_CONSTRUCT:
	            case SemanticNodeGroup.QT_DELETE:
	            	name = gNodeGroup.getQueryType();
	            	break;
                   
                case SemanticNodeGroup.QT_DISTINCT:
                default:
                    name = SemanticNodeGroup.QT_DISTINCT;
                    break;
            }
            IIDXHelper.selectFirstMatchingVal(document.getElementById("SGQueryType"), name);
        });
    }

    // for every key press.
    // Note that onchange will not work if the user presses a button do delete a node
    var onkeyupQueryText = function () {
        queryTextChanged(true);
    };

    var doUnload = function () {
    	clearEverything();

    	gMappingTab.updateNodegroup(gNodeGroup, gConn);
		gUploadTab.setNodeGroup(gConn, gNodeGroup, gNodeGroupName, gOInfo, gMappingTab, gOInfoLoadTime);
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

    // set status to a message or "" to finish progress.
    //
    // optHighlightCanvas - mark the treeCanvasWrapper with bold red border
    var setStatus = function(msg) {
        var div = document.getElementById("status");
        
    	div.innerHTML= "<font color='red'>" + msg + "</font><br>";
        if (!msg || msg.length == 0) {
            gCancelled = false;
            div.style.margin = "";
        } else {
            div.style.margin = "1em";
        }
    };

    var setStatusProgressBarScaled = function(lo, hi, msg, percent) {
        var newPercent = lo + (percent / 100 * (hi - lo));
        setStatusProgressBar(msg, newPercent);
    };

    var setStatusProgressBar = function(msg, percent) {

		var p = (typeof percent === 'undefined') ? 50 : percent;
        var m =  msg  || "";
        var table = document.createElement("table");
        var tr = document.createElement("tr");
        table.appendChild(tr);
        table.style.width = "100%";
        table.tableLayout="auto";
        var tdLeft = document.createElement("td");
        tr.appendChild(tdLeft);
        var tdRight = document.createElement("td");
        tdRight.style.width = "1%";
        tdRight.style.whiteSpace = "no-wrap";
        tr.appendChild(tdRight);
		tdLeft.innerHTML = m
				+ '<div class="progress progress-info progress-striped active"> \n'
				+ '  <div class="bar" style="width: ' + p + '%;">'
				+ '</div></div>';
        tdRight.innerHTML = "<button class='btn btn-danger' onclick='javascript:doCancel()' title='Cancel'><icon class='icon-remove-sign'></i></button>";
        var status = document.getElementById("status");
        status.innerHTML="";
        status.appendChild(table);
	};

    var buildQuery = function(optSuccessCallback) {

        if (gNodeGroup.getNodeCount() == 0) {
            document.getElementById('queryText').value = "";
            guiQueryEmpty();
            gRenderer.setError("");
            return;
        }

		var successCallback = optSuccessCallback ? optSuccessCallback : function(){};
		
        require(['sparqlgraph/js/msiclientnodegroupservice',
			    	        ], function(MsiClientNodeGroupService) {

            logEvent("SG Build");
            var sparql = "";
            var client = new MsiClientNodeGroupService(g.service.nodeGroup.url, buildQueryFailure.bind(this));
            switch (gNodeGroup.getQueryType()) {
            case SemanticNodeGroup.QT_DISTINCT:
                client.execAsyncGenerateSelect(gNodeGroup, gConn, buildQuerySuccess.bind(this, successCallback), buildQueryFailure.bind(this));
                break;
            case SemanticNodeGroup.QT_COUNT:
                client.execAsyncGenerateCountAll(gNodeGroup, gConn, buildQuerySuccess.bind(this, successCallback), buildQueryFailure.bind(this));
                break;
            case SemanticNodeGroup.QT_ASK:
                client.execAsyncGenerateAsk(gNodeGroup, gConn, buildQuerySuccess.bind(this, successCallback), buildQueryFailure.bind(this));
                break;
            case SemanticNodeGroup.QT_CONSTRUCT:
                client.execAsyncGenerateConstruct(gNodeGroup, gConn, buildQuerySuccess.bind(this, successCallback), buildQueryFailure.bind(this));
                break;
            case SemanticNodeGroup.QT_DELETE:
                client.execAsyncGenerateDelete(gNodeGroup, gConn, buildQuerySuccess.bind(this, successCallback), buildQueryFailure.bind(this));
                break;
            default:
                throw new Error("Internal error: Unknown query type.");
            }
        });
    };

    var buildQuerySuccess = function (successCallback, sparql, optMsg) {
        document.getElementById('queryText').value = sparql;
        if (optMsg) {
            setStatus(optMsg, true);
        } else {
            setStatus("");
        }
	    gRenderer.setError("");
	    
        if (sparql.length > 0) {
            guiQueryNonEmpty();
        } else {
            guiQueryEmpty();
        }
		
		successCallback();

    };

    var buildQueryFailure = function (msgHtml, sparqlMsgOrCallback) {
        if (typeof sparqlMsgOrCallback == "string") {
            	
            gRenderer.setError(sparqlMsgOrCallback.replace(/\n/g,' '), true);
        } else {
            gRenderer.setError("");
            require(['sparqlgraph/js/modaliidx'],
    	         function (ModalIidx) {
					ModalIidx.alert("Query Generation Failed", msgHtml, false, sparqlMsgOrCallback);
				});

        }

        document.getElementById('queryText').value = "";
        guiQueryEmpty();
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

                var ngsClient = new MsiClientNodeGroupService(g.service.nodeGroup.url, asyncFailureCallback);
                ngsClient.execAsyncGetRuntimeConstraints(gNodeGroup, gConn, runGraphWithConstraints, asyncFailureCallback);
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
        document.getElementById("btnExpandAll").disabled = false;
    	document.getElementById("btnCollapseUnused").disabled = false;
    	document.getElementById("btnLayout").disabled = false;
    	document.getElementById("btnGraphClear").disabled = false;
    	document.getElementById("SGOrderBy").disabled = false;
        document.getElementById("SGGroupBy").disabled = false;

    };


    var giuGraphEmpty = function () {
        document.getElementById("btnExpandAll").disabled = true;
        document.getElementById("btnCollapseUnused").disabled = true;
        document.getElementById("btnLayout").disabled = true;
        document.getElementById("SGOrderBy").disabled = true;
        document.getElementById("SGGroupBy").disabled = true;
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
        var d = gNodeGroup == null || gNodeGroup.getSNodeList().length < 1 || getQuerySource() == "DIRECT" || gInvalidItems.length > 0;
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
    var guiDisableAll = function (runViaServiceFlag) {
        disableHash = {};
        var opposite = [];

        // Cancel button works backwards if we're running via services

        var buttons = document.getElementsByTagName("button");
        for (var i = 0; i < buttons.length; i++) {
            if (buttons[i].id && buttons[i].id.length > 0) {
                disableHash[buttons[i].id] = buttons[i].disabled;

                buttons[i].disabled = (opposite.indexOf(buttons[i].id) == -1);
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

		gRenderer.setError(null);
	 	clearResults();
	 	guiQueryEmpty();
	};

	var clearNodeGroup = function () {
        gNodeGroup = new SemanticNodeGroup();
        gInvalidItems = [];
        gNodeGroup.setSparqlConnection(gConn);
        gMappingTab.clear();
        gPlotSpecsHandler = null;
        saveUndoState();
        nodeGroupChanged(false, "");
    	clearQuery();
    	giuGraphEmpty();

    };

    var clearMappingTab = function () {
       gMappingTab.clear();
    };

    var clearTree = function () {
    	gOTree.removeAll();
    	clearNodeGroup();
    	guiTreeEmpty();  //guiTreeEmpty();
    	clearMappingTab();

    };

    var clearEverything = function () {
    	clearTree();
    	gOInfo = new OntologyInfo();
        gExploreTab.setConn(gConn, gOInfo);
        gReportTab.setConn(gConn);
    	setConn(null);
	    gOInfoLoadTime = new Date();
    };

	// ===  Tabs ====
	var setupTabs = function() {
		$( "#tabs" ).tabs({
		    activate: function(event) {
		        // Enable / disable buttons on the navigation bar
		        if (!event.currentTarget) {
					return;  // this happens in selectTab
					
				} else if (event.currentTarget.id == "anchorTab1") {
		        	tabSparqlGraphActivated();

			    } else if (event.currentTarget.id == "anchorTab2") {
		        	tabMappingActivated();

			    } else if (event.currentTarget.id == "anchorTab3") {
		        	tabUploadActivated();

                } else if (event.currentTarget.id == "anchorTabX") {
		        	tabExploreActivated();

                } else if (event.currentTarget.id == "anchorTabR") {
		        	tabReportActivated();
		        }
		    }
		});
	};
	
	var selectTab = function(index) {
		$("#tabs").tabs("option", "active", index); 
	};
		

    var setTabButton = function(id, onTabFlag) {
        var but = this.document.getElementById(id);
        but.disabled = onTabFlag;
        if (onTabFlag) {
            but.classList.remove("semtktabdeselect");
        } else {
            but.classList.add("semtktabdeselect");
        }
    };

	var tabSparqlGraphActivated = function() {
		gCurrentTab = g.tab.query;
        setTabButton("query-tab-but", true);
        setTabButton("explore-tab-but", false);
 		setTabButton("mapping-tab-but", false);
		setTabButton("upload-tab-but", false);
        setTabButton("report-tab-but", false);

        gExploreTab.releaseFocus();
	};



    var tabMappingActivated = function() {
		gCurrentTab = g.tab.mapping;

		setTabButton("query-tab-but", false);
        setTabButton("explore-tab-but", false);
 		setTabButton("mapping-tab-but", true);
		setTabButton("upload-tab-but", false);
        setTabButton("report-tab-but", false);

        gExploreTab.releaseFocus();

		gMappingTab.updateNodegroup(gNodeGroup, gConn);

        resizeWindow();
	};
    var tabExploreActivated = function() {
        gCurrentTab = g.tab.explore;

        setTabButton("query-tab-but", false);
        setTabButton("explore-tab-but", true);
        setTabButton("mapping-tab-but", false);
        setTabButton("upload-tab-but", false);
        setTabButton("report-tab-but", false);

        gExploreTab.takeFocus();
    };

	var tabUploadActivated = function() {
		 gCurrentTab = g.tab.upload;

		setTabButton("query-tab-but", false);
        setTabButton("explore-tab-but", false);
  		setTabButton("mapping-tab-but", false);
		setTabButton("upload-tab-but", true);
        setTabButton("report-tab-but", false);

        gExploreTab.releaseFocus();

		// make sure gMappingTab has the same nodegroup in it before passing to UploadTab
		gMappingTab.updateNodegroup(gNodeGroup, gConn);
		gUploadTab.setNodeGroup(gConn, gNodeGroup, gNodeGroupName, gOInfo, gMappingTab, gOInfoLoadTime);

	};

    var tabReportActivated = function() {
		 gCurrentTab = g.tab.report;

		setTabButton("query-tab-but", false);
        setTabButton("explore-tab-but", false);
  		setTabButton("mapping-tab-but", false);
		setTabButton("upload-tab-but", false);
        setTabButton("report-tab-but", true);

        gExploreTab.releaseFocus();

	};


    // Just changed (or maybe changed) the nodegroup.
    // Save states
    var saveUndoState = function() {
        console.log("undo SAVE");
        gUndoManager.saveState(gNodeGroup.toJson())
        updateUndoButtons();
    };

    var resetUndo = function() {
        gUndoManager.reset();
        gUndoManager.saveState((new SemanticNodeGroup()).toJson());
        updateUndoButtons();
    };

    var doUndo = function() {
        var stateJson = gUndoManager.undo();
        updateUndoButtons();
        if (stateJson == undefined) return;

        gNodeGroup = new SemanticNodeGroup();
        if (stateJson) {
            gNodeGroup.addJson(stateJson);
        }
        nodeGroupChanged(true);
    };

    var doRedo = function() {
        var stateJson = gUndoManager.redo();
        updateUndoButtons();
        if (stateJson == undefined) return;

        gNodeGroup = new SemanticNodeGroup();
        if (stateJson) {
            gNodeGroup.addJson(stateJson);
        }
        nodeGroupChanged(true);
    };

    var updateUndoButtons = function() {
        document.getElementById("btnUndo").disabled = (gUndoManager.getUndoSize() < 1);
        document.getElementById("btnRedo").disabled = (gUndoManager.getRedoSize() < 1);
    }
    //
    // Build a callback which uses the status and results service to get to completion.
    //     callback parameter:  simpleResults with jobID
    // GUI: disables screen when called, handles cancel and unDisable at completion.
    // successCallback: called by resultsCall
    // resultsCall: a resultsClient function that takes a callback(successJson) as parameter
    //            e.g. MsiClientResults.prototype.execGetJsonBlobRes

    buildStatusResultsCallback = function(successCallback, resultsCall, MsiClientStatus, MsiClientResults, MsiResultSet, optLoPercent, optHiPercent) {

        var failureCallback = this.asyncFailureCallback.bind(this);
        var progressCallback = this.setStatusProgressBarScaled.bind(this, optLoPercent || 0, optHiPercent || 100);
        var checkForCancelCallback = this.checkForCancel.bind(this);
        guiDisableAll(true);

        // callback for the nodegroup execution service to send jobId
        var simpleResCallback = function(simpleResJson) {

            var resultSet = new MsiResultSet(simpleResJson.serviceURL, simpleResJson.xhr);
            if (!resultSet.isSuccess()) {
                failureCallback(resultSet.getFailureHtml());
            } else {
                var jobId = resultSet.getSimpleResultField("JobId");
                // callback for status service after job successfully finishes
                var statusSuccessCallback = function() {
                    // callback for results service
                    var resultsSuccessCallback = function (results) {
                        guiUnDisableAll();
                        successCallback(results);
                        progressCallback("finishing up", 99);
                        setStatus("");
                    };
                    var resultsClient = new MsiClientResults(g.service.results.url, jobId);
                    resultsCall.bind(resultsClient)(resultsSuccessCallback);
                };

                progressCallback("", 1);

                // call status service loop
                var statusClient = new MsiClientStatus(g.service.status.url, jobId, failureCallback);
                statusClient.execAsyncWaitUntilDone(statusSuccessCallback, checkForCancelCallback, progressCallback);
            }

        };

        return simpleResCallback;
    };
