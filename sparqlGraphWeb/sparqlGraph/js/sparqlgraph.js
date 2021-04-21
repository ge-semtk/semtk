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
    var gNodegroupInvalidItems = [];   // nodegroup does not match the model

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

    var gNodeGroupName = null;
    var gNodeGroupChangedFlag = false;
    var gQueryTextChangedFlag = false;

    var gCancelled = false;

    var gQueryTypeIndex = 0;   // sel index of QueryType
    var gQuerySource = "SERVICES";

    var RESULTS_MAX_ROWS = 5000; // 5000 sample rows

    // READY FUNCTION
    $('document').ready(function(){

    	setTabButton("upload-tab-but", true);
    	setTabButton("mapping-tab-but", true);

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
                  'sparqlgraph/js/nodegrouprenderer',
                  'sparqlgraph/js/uploadtab',

                  // shim
                  'sparqlgraph/js/belmont',

	              'local/sparqlgraphlocal'
                ],
                function (ExploreTab, MappingTab, ModalIIDX, ModalLoadDialog, ModalStoreDialog, MsiClientNodeGroupService, MsiClientNodeGroupStore, NodegroupRenderer, UploadTab) {

	    	console.log(".ready()");

	    	// create the modal dialogue
            var ngClient = new MsiClientNodeGroupService(g.service.nodeGroup.url);
	    	gLoadDialog = new ModalLoadDialog(document, "gLoadDialog", ngClient);

	    	 // set up the node group
	        gNodeGroup = new SemanticNodeGroup();

            canvasWrapper = document.getElementById("canvasWrapper");
            canvasWrapper.innerHTML = "";
            gRenderer = new NodegroupRenderer(canvasWrapper);
	        gRenderer.setPropEditorCallback(launchPropertyItemDialog);
	        gRenderer.setSNodeEditorCallback(launchSNodeItemDialog);
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

            // edit tab
            gExploreTab = new ExploreTab( document.getElementById("exploreTreeDiv"),
                                       document.getElementById("exploreCanvasDiv"),
                                       document.getElementById("exploreButtonDiv"),
                                       document.getElementById("exploreSearchForm")
                                      );
            setTabButton("explore-tab-but", false);

	    	// load gMappingTab
            var ngClient = new MsiClientNodeGroupService(g.service.nodeGroup.url);
			gMappingTab =  new MappingTab(importoptionsdiv, importcanvasdiv, importcolsdiv, gUploadTab.setDataFile.bind(gUploadTab), logAndAlert, ngClient );
	    	setTabButton("mapping-tab-but", false);

            // load gExploreTab

	        // load last connection
			var conn = gLoadDialog.getLastConnectionInvisibly();

            if (conn) {
				doLoadConnection(conn);

			} else if (g.customization.autoRunDemo.toLowerCase() == "true") {

                ModalIIDX.okCancel( "Demo",
                                    "Loading demo nodegroup, and<br>Launching demo documentation pop-up.<br>(You may need to override your pop-up blocker.)<br>",
                                    function() {
                                        window.open(g.help.url.base + "/" + g.help.url.demo, "_blank","location=yes");

                                        var mq = new MsiClientNodeGroupStore(g.service.nodeGroupStore.url);
                                        mq.getNodeGroupByIdToJsonStr("demoNodegroup", doQueryLoadJsonStr);
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
                ModalIIDX.alert(g.customization.startupDialogTitle, g.customization.startupDialogHtml);
            }

            if (g.customization.bannerText != 'none' && g.customization.bannerText.length > 2) {
                var span = document.getElementById("sparqlgraph-banner-span");
                span.className = "label-warning right";
                span.innerHTML = "&nbsp" + g.customization.bannerText + "&nbsp";
            }

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

            // Handle no paths or shift key during drag: drop node with no connections
            if (noPathFlag) {
                gNodeGroup.addNode(dragLabel, gOInfo);
                nodeGroupChanged(true);
                guiGraphNonEmpty();

            } else {
                var nodelist = gNodeGroup.getArrayOfURINames();
                var paths = gOInfo.findAllPaths(dragLabel, nodelist, gConn.getDomain());

                if (paths.length == 0) {
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
    var launchPropertyItemDialog = function (propItem, snodeID) {
        checkQueryTextUnsavedThen(launchPropertyItemDialog1.bind(this, propItem, snodeID));
    };

    var launchPropertyItemDialog1 = function (propItem, snodeID) {
        // TODO temporary test
        // This is not working, and leaves two properties with the same name.
        // Should somehow delete existing property and combine fields.
        if (false && gNodegroupInvalidItems.length > 0) {
            propItem.setKeyName("identifier");
            propItem.relation("http://arcos.rack/PROV-S#identifier");
            reValidateNodegroup();
            return;
        }

    	require([ 'sparqlgraph/js/modalitemdialog',
	            ], function (ModalItemDialog) {

    		var dialog= new ModalItemDialog(propItem,
                                            gNodeGroup,
                                            this.runSuggestValuesQuery.bind(this, g, gConn, gNodeGroup, null, propItem),
                                            propertyItemDialogCallback,
    				                        {"snodeID" : snodeID}
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
    			unlinkedTargetNames.push(targetSNodes[i].getBindingOrSparqlID());
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

    var launchLinkEditor = function(snode, nItem, targetSNode) {
        checkQueryTextUnsavedThen(launchLinkEditor1.bind(this, snode, nItem, targetSNode));
    };

    var launchLinkEditor1 = function(snode, nItem, targetSNode) {

        // TODO temporary test
        // this changes all the links, and prabably leaves two node items with the same name
        // should somehow combine existing
        // do we want to allow creation of new invalid node?
        // should inflateAndValidate silently tolerate-and-remove invalid unused things
        if (false && gNodegroupInvalidItems.length > 0) {
            nItem.setKeyName("employedBy");
            nItem.setConnectBy("employedBy");
            nItem.setUriConnectBy("http://arcos.rack/AGENTS#employedBy");
            reValidateNodegroup();
            return;
        }
		require([ 'sparqlgraph/js/modallinkdialog',
		            ], function (ModalLinkDialog) {

	    		var dialog= new ModalLinkDialog(nItem, snode, targetSNode, gNodeGroup, linkEditorCallback, {});
	    		dialog.show();
			});
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

            nodeGroupChanged(true, gNodeGroup.getSNodeSparqlIDs());
        });
	};

    var launchSNodeItemDialog = function (snodeItem) {
        checkQueryTextUnsavedThen(launchSNodeItemDialog1.bind(this, snodeItem));
    };

    var launchSNodeItemDialog1 = function (snodeItem) {
        require([ 'sparqlgraph/js/modalitemdialog',
                ], function (ModalItemDialog) {

            var dialog= new ModalItemDialog(snodeItem,
                                            gNodeGroup,
                                            this.runSuggestValuesQuery.bind(this, g, gConn, gNodeGroup, null, snodeItem),
                                            snodeItemDialogCallback,
                                            {} // no data
                                            );
            dialog.show();
        });
    };

    var snodeRemover = function (snode) {
        checkQueryTextUnsavedThen(snodeRemover1.bind(this, snode));
    };

    var snodeRemover1 = function (snode) {
        snode.removeFromNodeGroup(false);
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

    var propertyItemDialogCallback = function(propItem, varName, returnFlag, returnTypeFlag, optMinus, union, delMarker, rtConstrainedFlag, constraintStr, data) {
        // Note: ModalItemDialog validates that sparqlID is legal

        require([ 'sparqlgraph/js/modalitemdialog',
                ], function (ModalItemDialog) {

            // update the binding or sparqlID based on varName and returnFalg
            if (propItem.getSparqlID() == "") {
                gNodeGroup.changeSparqlID(propItem, varName);
            }
            if (varName == propItem.getSparqlID()) {
                // varName is sparqlID: shut of binding
                propItem.setBinding(null);
                propItem.setIsBindingReturned(false);
            } else {
                // varName is NOT sparqlID: use binding
                propItem.setBinding(varName);
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

            gNodeGroup.removeInvalidOrderBy();

            nodeGroupChanged(true);
        });
    };

    var snodeItemDialogCallback = function(snodeItem, varName, returnFlag, returnTypeFlag, optMinus, union, delMarker, rtConstrainedFlag, constraintStr, data) {
        require([ 'sparqlgraph/js/modalitemdialog',
                ], function (ModalItemDialog) {

            // Note: ModalItemDialog validates that sparqlID is legal

            // update the binding or sparqlID based on varName and returnFalg
            if (varName == snodeItem.getSparqlID()) {
                // varname is sparqlID: shut off binding
                snodeItem.setBinding(null);
                snodeItem.setIsBindingReturned(false);
            } else {
                // varname is NOT sparqlID: use binding
                snodeItem.setBinding(varName);
                snodeItem.setIsReturned(false);
            }
            gNodeGroup.setIsReturned(varName, returnFlag);

            snodeItem.setIsTypeReturned(returnTypeFlag);

        	// ignore optMinus in sparqlGraph.  It is still used in sparqlForm

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

            nodeGroupChanged(true);
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

        gExploreTab.setOInfo(gOInfo);
        gExploreTab.setConn(gConn);
        gExploreTab.draw();

		setStatus("");
		guiTreeNonEmpty();
		gMappingTab.updateNodegroup(gNodeGroup, gConn);
		gUploadTab.setNodeGroup(gConn, gNodeGroup, gOInfo, gMappingTab, gOInfoLoadTime);

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

            gExploreTab.setOInfo(gOInfo);
            gExploreTab.setConn(gConn);
            gExploreTab.draw();

	    	gMappingTab.updateNodegroup(gNodeGroup, gConn);
			gUploadTab.setNodeGroup(gConn, gNodeGroup, gOInfo, gMappingTab, gOInfoLoadTime);

    	});
 		// retains gConn
    };

    var doLoadConnection = function(connProfile, optCallback) {
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
                setStatus("clearing ontology cache");
                oInfoClient.execUncacheOntology(gConn,
                    function() {
                        setStatus("");
                        gOInfo.loadFromService(oInfoClient, gConn, setStatus, function(){doLoadOInfoSuccess(); callback();}, doLoadFailure);
                    }
                );
	    	}
    	});
    };

    // update display of nodegroup store id and connection
    var updateStoreConnStr = function() {
        var connStr = "";
        if (gNodeGroupName != null ) {
            connStr = "<b>Name: </b>" + gNodeGroupName;

            if (gNodeGroupChangedFlag == true) {
                connStr += "*";
            }
            connStr += "&nbsp&nbsp&nbsp";
        }
        connStr += (gConn != null && gConn.getName() != null) ? ("<b>Conn: </b>" + gConn.getName()) : "";

        document.getElementById("spanConnection").innerHTML = connStr;
    };

    var setConn = function (conn) {
        gConn = conn;
        this.updateStoreConnStr();
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

    var doQueryLoadJsonStr = function(jsonStr, optNgName) {
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
                                 ["Cancel",     "Keep Current",                     "From Nodegroup"],
                                 [function(){}, doQueryLoadFile2.bind(this, sgJson, optNgName), doQueryLoadConn.bind(this, sgJson, conn, optNgName)]
                                 );

            // use the new connection
            } else if (!gConn) {
                doQueryLoadConn(sgJson, conn, optNgName);

            // keep the old connection
            } else {
                doQueryLoadFile2(sgJson, optNgName);
            }
		});

    };

    /*
     * loads connection and makes call to load rest of sgJson
     * part of doQueryLoadJsonStr callback chain
     */
    var doQueryLoadConn = function(sgJson, conn, optNgName) {
    	require(['sparqlgraph/js/sparqlgraphjson',
                 'sparqlgraph/js/modaliidx'],
                function(SparqlGraphJson, ModalIidx) {

            var existName = gLoadDialog.connectionIsKnown(conn, true);     // true: make this selected in cookies

            // function pointer for the thing we do next no matter what happens:
            //    doLoadConnection() with doQueryLoadFile2() as the callback
            var doLoadConnectionCall = doLoadConnection.bind(this, conn, doQueryLoadFile2.bind(this, sgJson, optNgName));

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
     * All nodegroup loading passes through here
     *
     * @param {JSON} grpJson    node group
     * @param {JSON} importJson import spec
     */
    var doQueryLoadFile2 = function(sgJson, optNgName) {
    	// by the time this is called, the correct oInfo is loaded.
    	// and the gNodeGroup is empty.
    	require(['sparqlgraph/js/modaliidx', 'sparqlgraph/js/iidxhelper', 'sparqlgraph/js/msiclientnodegroupservice', 'sparqlgraph/js/sparqlgraphjson'],
                function(ModalIidx, IIDXHelper, MsiClientNodeGroupService, SparqlGraphJson) {

            clearGraph();
            logEvent("SG Loaded Nodegroup");

            gNodeGroupName = optNgName != undefined ? optNgName : null;
            gNodeGroupChangedFlag = false;

            try {
                // get nodegroup explicitly w/o the oInfo
                sgJson.getNodeGroup(gNodeGroup);
                gNodeGroup.setSparqlConnection(gConn);
                gPlotSpecsHandler = sgJson.getPlotSpecsHandler();
            } catch (e) {
                // real non-model error loading the nodegroup
                console.log(e.stack);
                clearGraph();
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

            // inflate and validate the nodegroup
            var client = new MsiClientNodeGroupService(g.service.nodeGroup.url, validateFailure.bind(this));
            client.execAsyncInflateAndValidate(new SparqlGraphJson(gConn, gNodeGroup), validateCallback, validateFailure)
        });
    };
    var validateFailure = function(msgHtml) {
        require(['sparqlgraph/js/modaliidx'], function (ModalIidx) {
            nodeGroupChanged(false);
            ModalIidx.alert("Nodegroup validation call failed", msgHtml, false);
            clearGraph();
        });
    };

    // callback: successfully determined whether there are modelErrors
    var validateCallback = function(nodegroupJson, modelErrors, invalidItemStrings) {

        if (modelErrors.length > 0) {
            // some errors
            require(['sparqlgraph/js/modaliidx'], function (ModalIidx) {
                var msgHtml = "<list>Nodegroup validation errors:<li>" + modelErrors.join("</li><li>") + "</li></list>";
                ModalIidx.alert("Nodegroup / model mismatch", msgHtml, false);
            });
            gNodegroupInvalidItems = invalidItemStrings;
            setStatus("Nodegroup is not valid to ontology");
        } else {
            // no errors
            gNodegroupInvalidItems = [];
            setStatus("");
        }

        // do either way
        gNodeGroup.clear();
        gNodeGroup.addJson(nodegroupJson);
        nodeGroupChanged(false);
        buildQuery();
    };

    var reValidateNodegroup = function() {
        require(['sparqlgraph/js/msiclientnodegroupservice', 'sparqlgraph/js/sparqlgraphjson'],
                function(MsiClientNodeGroupService, SparqlGraphJson) {
            // inflate and validate the nodegroup
            var client = new MsiClientNodeGroupService(g.service.nodeGroup.url, validateFailure.bind(this));
            client.execAsyncInflateAndValidate(new SparqlGraphJson(gConn, gNodeGroup), reValidateCallback, validateFailure);
        });
    };

    // callback: successfully determined whether there are modelErrors
    var reValidateCallback = function(nodegroupJson, modelErrors, invalidItemStrings) {


        if (modelErrors.length == 0) {
            require(['sparqlgraph/js/modaliidx'], function (ModalIidx) {
                ModalIidx.alert("Validation success", "Nodegroup is now valid against ontology", false);
            });

        }
        gNodegroupInvalidItems = invalidItemStrings;
        gNodeGroup.clear();
        gNodeGroup.addJson(nodegroupJson);
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

    var doNodeGroupDownload = function (optDeflateFlag) {
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
        doNodeGroupDownload(false);
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
                nodeGroupChanged(true);
            };

            var dialog = new ModalOrderByDialog(gNodeGroup.getReturnedSparqlIDs(), gNodeGroup.getOrderBy(), callback);
            dialog.launchOrderByDialog();
        });
   	};


    var gStopChecking = true;

    var checkServices = function () {
        require(['sparqlgraph/js/microserviceinterface',
                 'sparqlgraph/js/modaliidx'],
    	         function (MicroServiceInterface, ModalIidx) {

            // build div with all service names and ...
            var div = document.createElement("div");
            for (var key in g.service) {
                if (g.service.hasOwnProperty(key)) {
                    div.innerHTML += g.service[key].url + ' is .</br>';
                }
            }

            var m = new ModalIidx();
            m.showOK("Services:  Ping every 5 sec", div,  function(){ gStopChecking = true; });

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
                                                                                     queryFailureCallback,
                                                                                     setStatusProgressBar.bind(this, "Running Query"),
                                                                                     this.checkForCancel.bind(this),
                                                                                     g.service.status.url,
                                                                                     g.service.results.url);

            var jsonLdCallback = MsiClientNodeGroupExec.buildJsonLdCallback(queryJsonLdCallback,
                                                                            queryFailureCallback,
                                                                            setStatusProgressBar.bind(this, "Running Query"),
                                                                            this.checkForCancel.bind(this),
                                                                            g.service.status.url,
                                                                            g.service.results.url);
            setStatusProgressBar("Running Query", 1);
            switch (getQueryType()) {
			case "SELECT":
                client.execAsyncDispatchSelectFromNodeGroup(gNodeGroup, gConn, null, rtConstraints, csvJsonCallback, queryFailureCallback);
                break;
			case "COUNT" :
                client.execAsyncDispatchCountFromNodeGroup(gNodeGroup, gConn, null, rtConstraints, csvJsonCallback, queryFailureCallback);
                break;
            case "CONSTRUCT":
                client.execAsyncDispatchConstructFromNodeGroup(gNodeGroup, gConn, null, rtConstraints, jsonLdCallback, queryFailureCallback);
                break;
			case "DELETE":
                var okCallback = client.execAsyncDispatchDeleteFromNodeGroup.bind(client, gNodeGroup, gConn, null, rtConstraints, csvJsonCallback, queryFailureCallback);
                var cancelCallback = function () {
                    guiUnDisableAll();
                    setStatus("");
                };

                ModalIidx.okCancel("Delete query", "Confirm SPARQL DELETE operation.", okCallback, "Run Delete", cancelCallback);
                break;
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
                                                                                      queryFailureCallback,
                                                                                      setStatusProgressBar.bind(this, "Running Query"),
                                                                                      this.checkForCancel.bind(this),
                                                                                      g.service.status.url,
                                                                                      g.service.results.url);
            guiDisableAll();
            setStatusProgressBar("Running Query", 1);
            var sparql = document.getElementById('queryText').value;

            if (sparql.toLowerCase().indexOf("delete") > -1) {
                var okCallback = client.execAsyncDispatchRawSparql.bind(client, sparql, gConn, csvJsonCallback, queryFailureCallback);

                var cancelCallback = function () {
                    guiUnDisableAll();
                    setStatus("");
                };

                ModalIidx.okCancel("Delete query", "Query may write / delete triples.<br>Confirm you want to run this query.", okCallback, "Run Query", cancelCallback);
            } else {
                client.execAsyncDispatchRawSparql(sparql, gConn, csvJsonCallback, queryFailureCallback);
            }

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

            // build select
            select = IIDXHelper.createSelect(null, textValArray, selectedTexts);
            select.onchange = function(select) {
                var i = parseInt(select.value);

                // empty w/o shrinking so screen might now bounce
                resultsDiv.style.minHeight = resultsDiv.offsetHeight + "px";
                resultsDiv.innerHTML = "";

                if (i < 0) {
                    tableResults.putTableResultsDatagridInDiv(resultsDiv, undefined, noSort);
                } else {
                    gPlotSpecsHandler.getPlotter(i).addPlotToDiv(resultsDiv, tableResults);
                }
            }.bind(this, select);

            // build button
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

            var but = IIDXHelper.createIconButton("icon-picture", plotsLauncher, undefined, undefined, "Plots");

            // assemble the span
            var span = document.createElement("span");
            span.style.margin="2";
            select.style.margin="0";
            span.appendChild(select);
            span.appendChild(document.createTextNode(" "));
            span.appendChild(but);

            // add header to results
            var headerTable = IIDXHelper.buildResultsHeaderTable(headerHtml, ["Save table csv"], [tableResults.tableDownloadCsv.bind(tableResults)], span);
            resultsPara.innerHTML = "";
            resultsPara.appendChild(headerTable);
            resultsPara.appendChild(resultsDiv);

            // display results
            var plotter = gPlotSpecsHandler == null ? null : gPlotSpecsHandler.getDefaultPlotter();
            if (plotter != null) {
                plotter.addPlotToDiv(resultsDiv, tableResults);
            } else {
                tableResults.putTableResultsDatagridInDiv(resultsDiv, undefined, noSort);
            }

            guiUnDisableAll();
            guiResultsNonEmpty();
            setStatus("");
         });
    };



    var queryJsonLdCallback = function(jsonLdResults) {
        require(['sparqlgraph/js/iidxhelper'], function(IIDXHelper) {

            var anchor = IIDXHelper.buildAnchorWithCallback(    "results.json",
                                                                function (res) {
                                                                    var str = JSON.stringify(res.getGraphResultsJson(), null, 4);
                                                                    IIDXHelper.downloadFile(str, "results.json",  "application/json");
                                                                }.bind(this, jsonLdResults)
                                                            );

            var header = document.createElement("span");
            header.innerHTML =  "Download json: ";
            header.appendChild(anchor);
            header.appendChild(document.createElement("hr"));

            jsonLdResults.putJsonLdResultsInDiv(document.getElementById("resultsParagraph"), header);

            guiUnDisableAll();
            guiResultsNonEmpty();
            setStatus("");
        });
    };

   	var doRetrieveFromNGStore = function() {
        // check that nodegroup is saved
        // launch the retrieval dialog
        // callback to the dialog is doQueryLoadJsonStr
        checkAnythingUnsavedThen(
            gStoreDialog.launchRetrieveDialog.bind(gStoreDialog, doQueryLoadJsonStr)
        );
    };

   	var doDeleteFromNGStore = function() {
        gStoreDialog.launchDeleteDialog();
    };

  	var doStoreNodeGroup = function () {

        require(['sparqlgraph/js/sparqlgraphjson'], function(SparqlGraphJson) {

            gMappingTab.updateNodegroup(gNodeGroup, gConn);

            // save user when done
            var doneCallback = function () {
                localStorage.setItem("SPARQLgraph_user", gStoreDialog.getUser());
                nodeGroupChanged(false);
            }

            var sgJson = new SparqlGraphJson(gConn, gNodeGroup, gMappingTab.getImportSpec(), true, gPlotSpecsHandler);
            gStoreDialog.launchStoreDialog(sgJson, doneCallback);

        });

  	};

  	var doLayout = function() {
        gRenderer.showConfigDialog();
   	};

   	var doCollapseUnused = function() {
        gRenderer.drawCollapsingUnused();
    };

    var doExpandAll = function() {
        gRenderer.drawExpandAll();
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

    var getQueryShowNamespace = function () {
    	return document.getElementById("SGQueryNamespace").checked;
    };

    // Set nodeGroupChangedFlag and update GUI for new nodegroup
    // unchangeSNodeIDs : snodes who shouldn't be redrawn and potentially moved
    var nodeGroupChanged = function(flag) {
        gNodeGroupChangedFlag = flag;

        guiUpdateGraphRunButton();

        // check up ORDER BY
        gNodeGroup.removeInvalidOrderBy();
        if (gNodeGroup.getOrderBy().length > 0) {
            document.getElementById("SGOrderBy").classList.add("btn-primary");
        } else {
            document.getElementById("SGOrderBy").classList.remove("btn-primary");
        }

        // check up on LIMIT
        var limit = gNodeGroup.getLimit();
        var elem = document.getElementById("SGQueryLimit");
        elem.value = (limit < 1) ? "" : limit;

        gRenderer.draw(gNodeGroup, gNodegroupInvalidItems);
        if (flag) {
            buildQuery();
        } else {
            gMappingTab.setChangedFlag(false);
            queryTextChanged(false);
        }
        updateStoreConnStr();
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

    	gMappingTab.updateNodegroup(gNodeGroup, gConn);
		gUploadTab.setNodeGroup(gConn, gNodeGroup, gOInfo, gMappingTab, gOInfoLoadTime);
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

    var setStatusProgressBar = function(msg, percent, optMessageOverride) {
		var p = (typeof percent === 'undefined') ? 50 : percent;
        var m = (typeof optMessageOverride === 'undefined' && optMessageOverride != "") ? msg : optMessageOverride;
		document.getElementById("status").innerHTML = m
				+ '<div class="progress progress-info progress-striped active"> \n'
				+ '  <div class="bar" style="width: ' + p
				+ '%;"></div></div>';
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
                client.execAsyncGenerateSelect(gNodeGroup, gConn, buildQuerySuccess.bind(this), buildQueryFailure.bind(this));
                break;
            case "COUNT":
                client.execAsyncGenerateCountAll(gNodeGroup, gConn, buildQuerySuccess.bind(this), buildQueryFailure.bind(this));
                break;
            case "CONSTRUCT":
                client.execAsyncGenerateConstruct(gNodeGroup, gConn, buildQuerySuccess.bind(this), buildQueryFailure.bind(this));
                break;
            case "DELETE":
                client.execAsyncGenerateDelete(gNodeGroup, gConn, buildQuerySuccess.bind(this), buildQueryFailure.bind(this));
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

    var buildQueryFailure = function (msgHtml, sparqlMsgOrCallback) {
        var sparql = "";
        if (typeof sparqlMsgOrCallback == "string") {
            sparql = "#" + sparqlMsgOrCallback.replace(/\n/g, '#');

        } else {
            require(['sparqlgraph/js/modaliidx'],
    	         function (ModalIidx) {
					ModalIidx.alert("Query Generation Failed", msgHtml, false, sparqlMsgOrCallback);
				});

        }

        document.getElementById('queryText').value = sparql;
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

                var ngsClient = new MsiClientNodeGroupService(g.service.nodeGroup.url, queryFailureCallback);
                ngsClient.execAsyncGetRuntimeConstraints(gNodeGroup, gConn, runGraphWithConstraints, queryFailureCallback);
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

    };

    var giuGraphEmpty = function () {
        document.getElementById("btnExpandAll").disabled = true;
        document.getElementById("btnCollapseUnused").disabled = true;
        document.getElementById("btnLayout").disabled = true;
        document.getElementById("SGOrderBy").disabled = true;
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
        var d = gNodeGroup == null || gNodeGroup.getSNodeList().length < 1 || getQuerySource() == "DIRECT" || gNodegroupInvalidItems.length > 0;
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
        if (runViaServiceFlag) {
            opposite.push("btnFormCancel");
        }

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

	 	clearResults();
	 	guiQueryEmpty();
	};

	var clearGraph = function () {
    	gNodeGroup.clear();
        gNodegroupInvalidItems = [];
        gNodeGroup.setSparqlConnection(gConn);
        gMappingTab.clear();
        gNodeGroupName = null;
        gNodeGroupChangedFlag = false;
        gPlotSpecsHandler = null;
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
        gExploreTab.setOInfo(gOInfo);
        gExploreTab.setConn(gConn);
        gExploreTab.draw();
    	setConn(null);
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

                } else if (event.currentTarget.id == "anchorTabX") {
		        	tabExploreActivated();
		        }
		    }
		});
	});

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

        gExploreTab.stopLayout();
	};

    var tabExploreActivated = function() {
		gCurrentTab = g.tab.explore;

		setTabButton("query-tab-but", false);
        setTabButton("explore-tab-but", true);
		setTabButton("mapping-tab-but", false);
		setTabButton("upload-tab-but", false);

        gExploreTab.startLayout();
	};

    var tabMappingActivated = function() {
		gCurrentTab = g.tab.mapping;

		setTabButton("query-tab-but", false);
        setTabButton("explore-tab-but", false);
 		setTabButton("mapping-tab-but", true);
		setTabButton("upload-tab-but", false);

        gExploreTab.stopLayout();

		gMappingTab.updateNodegroup(gNodeGroup, gConn);

        resizeWindow();
	};

	var tabUploadActivated = function() {
		 gCurrentTab = g.tab.upload;

		setTabButton("query-tab-but", false);
        setTabButton("explore-tab-but", false);
  		setTabButton("mapping-tab-but", false);
		setTabButton("upload-tab-but", true);

        gExploreTab.stopLayout();

		gUploadTab.setNodeGroup(gConn, gNodeGroup, gOInfo, gMappingTab, gOInfoLoadTime);

	};
