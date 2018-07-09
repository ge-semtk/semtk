/**
 ** Copyright 2016-17 General Electric Company
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

// This file is not a typical javascript object, but a script
// that needs to be loaded by the HTML.
// Its other half must also be loaded byt the HTML:  sparqlformlocal.js

// gui constants
var FORM_UNCONSTRAINED_ICON = ""; // "<i class='icon-check-empty'></i>";
var FORM_CONSTRAINED_ICON = "<i class='icon-filter'></i>"; //' icon-check'
var FORM_TRIGGER_CLASS_ICON = "<i class='icon-exchange'></i>";
var EMPTY = "";

//
// globals
//
var g = null;
var gConn = null;
var gNodeGroup = null;
var gExtNodeGroup = null;
var gOInfo = null;
var gOTree = null;
var gDragNode = null;
var gSparqlResults = null;
var gSparqlEdc = null;
var gFormConstraint = null;
var gStoreDialog = null;

var gCancelled = false;

//
//  Require.js tutorial:
//      Put the proper require.js-ized dependencies first.  
//      Give the main function() a parameter for each.  
//      This will look rather like a global inside the function.
//
//      Put the shimmed stuff last.  They will be in scope.

require([	'local/sparqlformconfig',
         
         	'sparqlgraph/js/backcompatutils',
            'sparqlgraph/js/modaliidx',
            'sparqlgraph/js/modalitemdialog',
         	'sparqlgraph/js/iidxhelper',
            'sparqlgraph/js/msiclientnodegroupexec',
            'sparqlgraph/js/msiclientnodegroupservice',
            'sparqlgraph/js/modalstoredialog',
            'sparqlgraph/js/sparqlgraphjson',
         	
         	'jquery',
		
			// rest are shimmed
			'sparqlgraph/js/sparqlconnection', 
			'sparqlgraph/js/belmont',
			'sparqlgraph/js/ontologyinfo',
			'sparqlgraph/js/ontologytree',
			'sparqlgraph/dynatree-1.2.5/jquery.dynatree', 
		],

	function(Config, BackwardCompatibleUtil, ModalIidx, ModalItemDialog, IIDXHelper, MsiClientNodeGroupExec, MsiClientNodeGroupService, ModalStoreDialog, SparqlGraphJson, $) {
		
		gConnSetup = function() {
			// Establish Sparql Connection in gConn, using g		
			gConn = new SparqlConnection();
            
            if (g.hasOwnProperty('conn')) {
                gConn.setName(g.conn.name);
                gConn.setDomain(g.conn.domain);
                var serverType = SparqlConnection.VIRTUOSO_SERVER;

                // backwards-compatible single connection
                if (g.conn.hasOwnProperty('serverURL') && g.conn.hasOwnProperty('dataset')) {
                    gConn.addDataInterface(	
                            serverType,
                            g.conn.serverURL,
                            g.conn.dataset
                            );
                    gConn.addModelInterface(
                            serverType,
                            g.conn.serverURL,
                            g.conn.dataset
                            );

                // backwards-compatible double connection
                } else if (g.conn.hasOwnProperty('dataServerURL') && g.conn.hasOwnProperty('dataDataset') &&
                           g.conn.hasOwnProperty('ontologyServerURL') && g.conn.hasOwnProperty('ontologyDataset')) {

                    gConn.addDataInterface(	
                            serverType,
                            g.conn.dataServerURL,
                            g.conn.dataDataset
                            );
                    gConn.addModelInterface(
                            serverType,
                            g.conn.ontologyServerURL,
                            g.conn.ontologyDataset,
                            g.conn.domain
                            );
                }
            // normal
			} else if (g.hasOwnProperty('model') || g.hasOwnProperty('onURL')) {
                gConn.fromJson(g);
                
            } else {
				throw "Incomplete connection configuration info.  Need either (serverURL and dataset), or (dataServerURL, dataDataset, ontologyServerURL, and ontologyDataset).";
			}
			
			// add gConn to gNodeGroup if it exists
			if (gNodeGroup != null) {
				gNodeGroup.setSparqlConnection(gConn);
			}
		};

		setStatus = function(msg) {
			document.getElementById("status").innerHTML = "<font color='red'>"
					+ msg + "</font>";
		};

		setStatusProgressBar = function(msg, percent) {
			var p = (typeof percent === 'undefined') ? 50 : percent;

			document.getElementById("status").innerHTML = msg
					+ '<div class="progress progress-striped active"> \n'
					+ '  <div class="bar" style="width: ' + p
					+ '%;"></div> \n' + '</div> \n';
		};

		showDebug = function(title, nodeGroup) {
			
		};

		initServices = function () {
            gModalStoreDialog = new ModalStoreDialog(localStorage.getItem("SPARQLgraph_user"),
                                                     Config.services.nodeGroupStore.url);
        };
    
		initOTree = function() {
			// turn this into a stub if some version of a program has no OTree
			if (document.getElementById("treeDiv") == null) return;
			
			// Attach the dynatree widget to an existing <div id="tree"> element
			// and pass the tree options as an argument to the dynatree() function:
	
			$("#treeDiv").dynatree({
				onActivate : function(node) {
					// A DynaTreeNode object is passed to the activation handler
					// Note: we also get this event, if persistence is on, and the page is reloaded.
					//console.log("You activated " + node.data.title);
				},
				onDblClick : function(node) {
					// A DynaTreeNode object is passed to the activation handler
					// Note: we also get this event, if persistence is on, and the page is reloaded.
					//console.log("You double-clicked "+ node.data.title);
	
					// Paul:  We only get this onDblClick for leaf nodes, which for the current functionality is ok.
					//        But, it indicates I don't really understand these callbacks yet.
					// Double-click:  (1) pretend we're dragging.  (2) force a drop with no event (drop function ignores it anyway)
					addRowFromOTree(node);
				},
	
				dnd : {
					onDragStart : function(node) {
						gDragNode = node;
						console.log("onDragStart: " + gOTree.nodeGetURI(gDragNode));
						return true;
					},
					onDragStop : function(node) {
						console.log("onDragStop: "	+ gOTree.nodeGetURI(gDragNode));
	
					},
	
				},
	
				persist : true,
			});
	
			// load the data
			gOTree = new OntologyTree(	$("#treeDiv").dynatree("getTree"), 
										false,                                                                 // don't show enums
										["http://research.ge.com/kdl/sparqlgraph/externalDataConnection"]  );  // collapse sparqlEDC
			gOTree.setSpecialClassFormat('<font color="blue">','<font>');
			gOTree.setSpecialClassFormat("<div>" + FORM_TRIGGER_CLASS_ICON + " ", "</div>");
					
			gOTree.addSpecialClasses(getOntTriggerClassNames(gOInfo));  
			                            
			gOTree.setOInfo(gOInfo); // add ontology but don't display yet
			
			document.getElementById("chkShowAll").checked = true;
			doTreeShowAllButton(); // decide what to display based on the checkbox
	
			// activate relevant buttons
			document.getElementById("btnTreeExpand").className = "btn";
			document.getElementById("btnTreeExpand").disabled = false;
	
			document.getElementById("btnTreeCollapse").className = "btn";
			document.getElementById("btnTreeCollapse").disabled = false;
		};
		
		addFormRow = function(parentSNode, itemKeyName, childSNode, optNoConstraintUpdate) {
			// adds a row to the form PRESUMING that the gNodeGroup has already been updated
			// parentSNode is the node
			// itemKeyName is NULL or the name of the nodeItem or propItem
			// childSNode is  NULL or the child node when itemKeyName is a nodeItem

			// Depending on how this was called, set filterItem to propItem or the right snode
			var noConstraintUpdate = (typeof(optNoConstraintUpdate) == 'undefined') ? false : optNoConstraintUpdate;
			var filterItem;
			if (childSNode !== null) {
				filterItem = childSNode;
			} else if (itemKeyName !== null) {
				filterItem = parentSNode.getPropertyByKeyname(itemKeyName);
			} else {
				filterItem = parentSNode;
			}

			// Choose the icon, name its div, decide if constraint button is enabled
			var iconID = filterItem.getSparqlID().slice(1) + "_txt";
			var icon;
			var constrainableFlag = true;
			
			if (isOntTriggerClassName(parentSNode.getURI()) || 
				 (childSNode !== null && isOntTriggerClassName(childSNode.getURI()))) {
				icon = FORM_TRIGGER_CLASS_ICON;
				constrainableFlag = false;
			} else if (filterItem.hasConstraints()) {
				icon = FORM_CONSTRAINED_ICON;
			} else {
				icon = FORM_UNCONSTRAINED_ICON;
			}

			// insert a table row
			var table = document.getElementById("formTable");
			var row = table.insertRow(-1);

			// ========= invisible keys =========
			// Key #0: rowId
			var c;
			c=row.insertCell(-1);
			c.style.display="none";
			c.innerHTML = gRowId;
			// Key #1: sparqlID of class added or class containing the item added
			c=row.insertCell(-1);
			c.style.display="none";
			c.innerHTML = (parentSNode !== null) ? parentSNode.getSparqlID() : "";
			// Key #2: if this is a propertyItem or nodeItem, the item's keyname
			c=row.insertCell(-1);
			c.style.display="none";
			c.innerHTML = (itemKeyName !== null) ? itemKeyName : "";
			// key #3: if the item is a nodeItem, then the child node
			c=row.insertCell(-1);
			c.style.display="none";
			c.innerHTML = (childSNode !== null) ? childSNode.getSparqlID() : "";

			// ========= visible stuff =========
			// label for user
			var cellStr = (parentSNode.getURI(true) + ((itemKeyName != "") ? ("->" + itemKeyName) : "")).slice(-48);
			row.insertCell(-1).innerHTML = cellStr;
			
			// icon
			row.insertCell(-1).innerHTML = "<div id='" + iconID + "'>" + icon + "</div>";
			
			// constrain button
			if (constrainableFlag) {
				cellStr =  "<button id='b_" + IIDXHelper.getNextId() + "' class='btn btn-info" + (constrainableFlag ? "" : " disabled") + "' " +
					"onclick='javascript:filterCallback(\"" + gRowId + '","' + iconID + "\"); return false;'" +
					(constrainableFlag ? "" : " disabled") + ">Filter</button>";   // <i class='icon-filter icon-white'></i>
			} else {
				cellStr = "";
			}
			row.insertCell(-1).innerHTML = cellStr;
				   
			// delete button
			// PEC TODO: the constraints table does this a different and much cooler way.  share.
			row.insertCell(-1).innerHTML = 
					"<button id='b_" + IIDXHelper.getNextId() + "' class='btn btn-danger' onclick='javascript:delFormRow(\"" + gRowId + "\"); return false;'><i class='icon-trash icon-white'></i></button>";

			gRowId += 1;
			
			if (! noConstraintUpdate) {
				formConstraintsUpdateTable();
			}
			
			kdlLogEvent("SF: Add class", "classURI", parentSNode.getURI(false), "itemKeyName", itemKeyName);
		};
		
		filterCallback = function(rowId, textId) { // nodeSparqlID, itemURI, textId) {
			// callback for filter buttons on the form
			
			// add a constraint to a SparqlID->property
			var row = getFormRow(rowId);
			var sNode;
			var item;
			var tmpSNode;
			var tmpItem;
			var tmpNodeGroup = gNodeGroup.deepCopy();
			
			// ==== find the item in both copies of the nodegroup:  item and tmpItem ====
				// if there's a child node
			if (row.childNodeID !== "") {
				sNode = gNodeGroup.getNodeBySparqlID(row.childNodeID);
				item = sNode;
				
				tmpSNode = tmpNodeGroup.getNodeBySparqlID(row.childNodeID);
				tmpItem = tmpSNode;

				// if there's a parent node and property
			} else if (row.itemKeyName !== "") {
				sNode = gNodeGroup.getNodeBySparqlID(row.parentNodeID);
				item = sNode.getPropertyByKeyname(row.itemKeyName);
				
				tmpSNode = tmpNodeGroup.getNodeBySparqlID(row.parentNodeID);
				tmpItem = tmpSNode.getPropertyByKeyname(row.itemKeyName);

				// else there's only a parent node
			} else {
				sNode = gNodeGroup.getNodeBySparqlID(row.parentNodeID);
				item = sNode;
				
				tmpSNode = tmpNodeGroup.getNodeBySparqlID(row.parentNodeID);
				tmpItem = tmpSNode;
			}

			// Can't filter a trigger class.  Quietly return
			if (isOntTriggerClassName(row.parentNodeID) || isOntTriggerClassName(row.childNodeID))
				return;

			// prepare a query for the constraint dialog
			removeTrigger(tmpNodeGroup);
			tmpNodeGroup.expandOptionalSubgraphs();
			
			showDebug("Constraining " + sNode.getSparqlID() + "->" + item.getSparqlID(), tmpNodeGroup);
			kdlLogEvent("SF: Filter Button", "Node", sNode.getURI(), "itemSparqlID", item.getSparqlID());
			            
			var dialog= new ModalItemDialog(item, 
                                            gNodeGroup, 
                                            runSfSuggestValuesQuery.bind(this, tmpNodeGroup, tmpItem), 
                                            itemDialogCallback,
    				                        {"textId": textId}
    		                                );
            dialog.setLimit(Config.itemDialog.limit);
            dialog.setMaxValues(Config.itemDialog.maxValues);
            
            var sparqlFormFlag = true;
    		dialog.show(sparqlFormFlag);
		};
    
        doCancel = function() {
            gCancelled = true;
        };
    
        checkForCancel = function() {
            if (gCancelled) {
                gCancelled = false;
                return true;
            } else {
                return false;
            }
        };
    
        runSfSuggestValuesQuery = function (ng, item, msiOrQsResultCallback, failureCallback, statusCallback) {
        
            // make sure there is a sparqlID
            var runNodegroup;
            var runId;
            // make sure there is a sparqlID
            if (item.getSparqlID() == "") {
                // set it, make a copy, set it back
                item.setSparqlID("SG_runItemDialogQuery_Temp");
                runNodegroup = ng.deepCopy();
                runId = item.getSparqlID();
                item.setSparqlID("");
            } else {
                runNodegroup = ng;
                runId = item.getSparqlID();
            }

            
            if (typeof gAvoidQueryMicroserviceFlag != "undefined" && gAvoidQueryMicroserviceFlag) {
                // Generate sparql and run via query interface
                
                // get answer for msiOrQsResultCallback
                var sparqlCallback = function (cn, resCallback, failCallback, sparql) {
                    var ssi = cn.getDefaultQueryInterface();
                    ssi.executeAndParseToSuccess(sparql, resCallback, failCallback );
                    
                }.bind(this, gConn, msiOrQsResultCallback, failureCallback);
                
                // generate sparql and send to sparqlCallback
                var ngClient = new MsiClientNodeGroupService(Config.services.nodeGroup.url);
                ngClient.execAsyncGenerateFilter(runNodegroup, gConn, runId, sparqlCallback, failureCallback);
                
            } else {
                // Run nodegroup via Node Group Exec Svc
                var jobIdCallback = MsiClientNodeGroupExec.buildFullJsonCallback(msiOrQsResultCallback,
                                                                                 failureCallback,
                                                                                 statusCallback,
                                                                                 this.checkForCancel.bind(this),
                                                                                 Config.services.status.url,
                                                                                 Config.services.results.url);
                var execClient = new MsiClientNodeGroupExec(Config.services.nodeGroupExec.url, Config.timeout.long);
                statusCallback(1);
                execClient.execAsyncDispatchFilterFromNodeGroup(runNodegroup, gConn, runId, null, null, jobIdCallback, failureCallback);

            }
        };
    
		doTreeSearch = function() {
			//gOTree.find($("#search").val());
			gOTree.powerSearch($("#search").val());
		};

		doTreeSearchClear = function() {
			doTreeShowAllButton();
			doTreeCollapse();
		};

		doTreeCollapse = function() {
			document.getElementById("search").value = "";
			gOTree.collapseAll();
		};

		doTreeExpand = function() {
			gOTree.expandAll();
		};

		initNodeGroup = function() {

			// Build a node group
			gNodeGroup = new SemanticNodeGroup(1000, 700, 'canvas');
            gNodeGroup.drawable = false;
			gNodeGroup.setSparqlConnection(gConn)
			return;

		};
		
		doQueryLoadFile = function(file) {
			// reads json file, clears form, loads new one
			var r = new FileReader();

			r.onload = function() {
				var payload;
				try {
					payload = JSON.parse(r.result);
				} catch (e) {
					alertUser("Error parsing the JSON query file: \n" + e);
					return;
				}

				restoreQueryFromJson(payload);

			};
			r.readAsText(file);

		};

		doRetrieveFromNGStore = function() {
            if (gNodeGroup == null || gNodeGroup.getNodeCount() == 0
					|| confirm("Clearing current form\nbefore loading a new one.\n\n")) {
                
                gModalStoreDialog.launchRetrieveDialog(restoreQueryFromJsonStr);
            }
        };

        doDeleteFromNGStore = function() {
            gModalStoreDialog.launchDeleteDialog();
        };
        
        doStoreNodeGroup = function () {
        
            // save user when done
            var doneCallback = function () {
                localStorage.setItem("SPARQLgraph_user", gModalStoreDialog.getUser());
            }

            gModalStoreDialog.launchStoreDialog(getSGJson(), doneCallback); 
            
        };
        		
		doQueryUpload = function() {
			// menu pick callback
			if (gNodeGroup.getNodeCount() == 0
					|| confirm("Clearing current form\nbefore loading a new one.\n\n")) {

                IIDXHelper.fileDialog(doQueryLoadFile);
			}
		};

		doQueryDownload = function() {
			var sgJson = getSGJson();
			IIDXHelper.downloadFile(JSON.stringify(sgJson.toJson()), "sparqlForm.json", "text/csv;charset=utf8");
			kdlLogEvent("SF: Save Query");
		};

		doTreeShowAllButton = function() {

			gOTree.removeAll();

			if (document.getElementById("chkShowAll").checked) {
				gOTree.showAll();   
			} else {
				gOTree.showSubset(g.simpleClassURIs);
			}
		};
    
		//***********  drag and drop *************//
		allowDrop = function(ev) {
			alertUser("allowDrop5");
			ev.preventDefault();
		};

		drag = function(ev) {
			alertUser("drag5");
			ev.dataTransfer.setData("text", ev.target.id);
		};

		// PEC TODO: this is being called from probably the wrong place(s)
		drop = function(ev) {
			addRowFromOTree(gDragNode);
		};

		gRowId = 0;

		initForm = function() {
			gRowId = 0;
			var table = document.getElementById("formTable");
			table.innerHTML = "";
			
			formConstraintsNew();
			formConstraintsInit();

		};

		addRowFromOTree = function(treeNode) {
			// main function to handle dropping a new property on the form
			// uses gDragNode as the parameter, NOT evIgnored
			
			// PEC TODO: drag handlers should be smarter so this never happens
			if (!gOTree.nodeIsProperty(treeNode)) {
				alertUser("Only properties can be added to the form.");
				return;
			}

			// Get class object.  (Parent of tree node that was dropped)
			var itemKeyName = new OntologyName(gOTree.nodeGetURI(treeNode)).getLocalName();
			var classTreeNode = gOTree.nodeGetParent(treeNode);
			var classObj = gOInfo.getClass(gOTree.nodeGetURI(classTreeNode));

			// does item already exist
			var classUri = classObj.getNameStr();
			var testSNode = gNodeGroup.getNodesBySuperclassURI(classUri, gOInfo);
			if (testSNode.length > 0) {
				if (testSNode.length > 1) {
					alertUser("Internal error in sparqlForm addRowFromOTree:  Node already exists more than once: " + classUri);
					return;
				}
				var prop = testSNode[0].getPropertyByKeyname(itemKeyName);
				if (prop != null && prop.getIsReturned()) {
					alertUser("Property is already on the form: " + itemKeyName);
					return;
				}
				var nodeItem = testSNode[0].getNodeItemByKeyname(itemKeyName);
				if (nodeItem != null && nodeItem.getSNodes().length > 0 && nodeItem.getSNodes()[0].getIsReturned()) {
					alertUser("Item is already on the form: " + itemKeyName);
					return;
				}
			}
			
			// get info on the dropped Node
			var itemSNode = gNodeGroup.getOrAddNode(classObj.getNameStr(), gOInfo, gConn.getDomain(), true);
			
			if (itemSNode == null) {
				alertUser("Internal error in sparqlForm addRowFromOTree:  Can't find a path to add " + classObj.getNameStr());
				return;
			}
			
			// start by presuming that "item" is a property item
			var item = itemSNode.getPropertyByKeyname(itemKeyName);
			var filterItem = null;
			var childSNode = null;

			// if item is not a propertyItem, it is nodeItem.  
			// So get the childSNode
			if (!item) {
				var nodeItem = itemSNode.getNodeItemByKeyname(itemKeyName);
				childSNode = gNodeGroup.getOrAddNode(nodeItem.getUriValueType(), gOInfo, gConn.getDomain(), true);
				if (itemSNode == null) {
					alertUser("Internal error in sparqlForm addRowFromOTree:  Can't find a path to add the child node " + nodeItem.getUriValueType());
					return;
				}
				
				filterItem = childSNode;
			} else {
				childSNode = null;
				filterItem = item;
			}

			// get SparqlID of the item
			var filterItemID = filterItem.getSparqlID();
			// if it doesn't have one, set it
			if (filterItemID === null || filterItemID === "") {
				filterItemID = gNodeGroup.changeSparqlID(item,
						itemKeyName);
				showDebug("drop " + itemSNode.getSparqlID() + ((itemKeyName != "") ? ("->" + itemKeyName) : ""), 
						  gNodeGroup);

			}
			filterItem.setIsReturned(true);

			addFormRow(itemSNode, itemKeyName, childSNode);
		};
		
		itemDialogCallback = function(item, sparqlID, returnFlag, optionalFlag, delMarker_ALWAYS_NULL, rtConstrainedFlag, constraintStr, data) {
			// data.textId is the html element id that holds the filter icon
			
	    	// Note: ModalItemDialog validates that sparqlID is legal
	    	
	    	// Not sure dialog lets you change sparqlID, but function will handle non-event
	    	gNodeGroup.changeSparqlID(item, sparqlID);
            
            item.setIsReturned(returnFlag);
	    	
	    	// Optional
	    	if (item.getItemType() == "PropertyItem") {
	    		item.setIsOptional(optionalFlag);
	    		
	    	} else if (item.getItemType() == "NodeItem") {
	    		alert("Internal Error in sparqlForm.js itemDialogCallback():  item is a nodeItem.  Not implemented");
	    	
	    	} else {
	    		// "SemanticNode"
	    		if (optionalFlag != null) {
	    			var singleNodeItem = this.nodegroup.getSingleConnectedNodeItem(this.item);
	    			if (singleNodeItem != null) {
	    				if (item.ownsNodeItem(singleNodeItem)) {
	    					singleNodeItem.setSNodeOptional(item.getSNodes()[0], optionalFlag ? NodeItem.OPTIONAL_REVERSE : NodeItem.OPTIONAL_FALSE);
						} else {
							singleNodeItem.setSNodeOptional(item, optionalFlag ? NodeItem.OPTIONAL_TRUE : NodeItem.OPTIONAL_FALSE);
						}
	    			}
	    		}
	    	}
	    	
	    	// RuntimeConstrained
	    	item.setIsRuntimeConstrained(rtConstrainedFlag);
	    	
	    	if (constraintStr.length > 0) {
	    		document.getElementById(data.textId).innerHTML = FORM_CONSTRAINED_ICON;
	    	} else {
	    		document.getElementById(data.textId).innerHTML = FORM_UNCONSTRAINED_ICON;
	    	}
	    	
	    	item.setConstraints(constraintStr);
	    	formConstraintsUpdateTable();
	    	
	    	kdlLogEvent("SF: Filtered", "sparqlId", sparqlID, "constraints", constraintStr);
	    };
	    
		// DEPRECATED: used with modalconstraintdialog
		filterDialogCallback = function(textId, item, constraintArr) {
			item.setConstraints(constraintArr[0]);

			var gui = document.getElementById(textId);
			// gui.value = constraintArr[0];
			if (constraintArr.length > 0 && constraintArr[0].length > 0)
				gui.innerHTML = FORM_CONSTRAINED_ICON;
			else
				gui.innerHTML = FORM_UNCONSTRAINED_ICON;

			// PEC TODO: I have no idea how to keep the debug canvas display up to date here.
			//displayLabelOptions(draculaLabel, propItm.getDisplayOptions());
			showDebug("filterCallback", gNodeGroup);
			formConstraintsUpdateTable();
			
			kdlLogEvent("SF: Filtered", "itemKeyName", textId, "constraints", constraintArr[0]);
		};

		getFormRow = function(rowId) {
			// get an object representing a specific valid rowId
			var table = document.getElementById("formTable");
			var i;

			for (i = 0; i < table.rows.length; i++) {
				if (html_decode(table.rows[i].cells[0].innerHTML) === rowId) {
					return {
						table : table,
						rowNum : i,
						parentNodeID : html_decode(table.rows[i].cells[1].innerHTML),
						itemKeyName : html_decode(table.rows[i].cells[2].innerHTML),
						childNodeID : html_decode(table.rows[i].cells[3].innerHTML)
					};
				}
			}
			return null;
		};

		getFormRowsArray = function() {
			// get an array of rows.  Each row is an array of the three keys.
			// (the rowID is not returned)
			var table = document.getElementById("formTable");
			var ret = [];

			for (var i = 0; i < table.rows.length; i++) {
				ret.push([	html_decode(table.rows[i].cells[1].innerHTML),
							html_decode(table.rows[i].cells[2].innerHTML),
							html_decode(table.rows[i].cells[3].innerHTML), ]);
			}

			return ret;
		}

		

		delFormRow = function(rowId) {
			// delete row from form
			// and corresponding nodes from the gNodeGroup
			var r = getFormRow(rowId);
			var item;

			r.table.deleteRow(r.rowNum);

			var parentSNode = gNodeGroup
					.getNodeBySparqlID(r.parentNodeID);
			var pruneSNode = null;
			// just a node
			if (r.itemKeyName == "") {
				item = gNodeGroup.getNodeBySparqlID(r.parentNodeID);
				pruneSNode = parentSNode;

				// node and propertyItem
			} else if (r.childNodeID == "") {
				item = parentSNode.getPropertyByKeyname(r.itemKeyName);
				pruneSNode = parentSNode;

				// node -> nodeItem -> childNode
			} else {
				item = gNodeGroup.getNodeBySparqlID(r.childNodeID);
				pruneSNode = item;
			}

			// remove the constraints and isReturned
			item.setConstraints("");
			item.setIsReturned(false);

			gNodeGroup.pruneUnusedSubGraph(pruneSNode);

			showDebug("Deleted " + r.parentNodeID + "," + r.itemKeyName + "," + r.childNodeID, 
					  gNodeGroup);
			
			formConstraintsUpdateTable();
			
			kdlLogEvent("SF: Delete Row", "Node", pruneSNode.getURI(), "itemKeyName", r.itemKeyName);

		};

		
		doClearFormBut = function() {

			if (gNodeGroup.getNodeCount() == 0 || confirm("Clearing form.")) {
				initForm();
				formConstraintsNew();
				formConstraintsInit();
				initNodeGroup();
				kdlLogEvent("SF: Clear Form");
			}
			clearResults();
		};

		clearResults = function() {
			document.getElementById("gridDiv").innerHTML = "";
			document.getElementById("hrefDiv").innerHTML = "";
		};
		
		guiStartQuery = function () {
            guiDisableAll();
		};
		
		guiEndQuery = function (errorMsg) {
			// query is done.  If (errorMsg) then it failed.
			if (errorMsg) {
				alertUser(errorMsg);
			}
            guiUnDisableAll();
			setStatus("");
		};
    
        disableHash = {};
    
        /*
         * For all buttons with an id:
         *     disable
         *     save state
         */
        guiDisableAll = function () {
            disableHash = {};
            var opposite = [];
            
            // Cancel button works backwards as long as we're not avoiding microservices
            if (typeof gAvoidQueryMicroserviceFlag == "undefined" || !gAvoidQueryMicroserviceFlag) {
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
        guiUnDisableAll = function () {
            var buttons = document.getElementsByTagName("button");
            for (var i = 0; i < buttons.length; i++) {
                // if button has an id, and its state has been hashed
                if (buttons[i].id && buttons[i].id.length > 0 && disableHash[buttons[i].id] != undefined) {
                    buttons[i].disabled =  disableHash[buttons[i].id];
                }
            }
        };
		
		doRunQueryBut = function() {
			// callback for the "Get Data" button
			
			if (formConstraintsValidate() > 0) {
				alertUser("Fix invalid constraints before executing a query.");
				return;
			}
			
			if (gNodeGroup.getNodeCount() === 0) {
				alertUser("Query is empty.");
				return;
			}
			
			guiStartQuery();

			gSparqlResults = null;
			clearResults();

			doQuery();
		};
    
        doQuery = function() {
            // Paul 7/9/18 - always expand optionals in UI
            var tmpNodeGroup = gNodeGroup.deepCopy();
            tmpNodeGroup.expandOptionalSubgraphs();
            
            // DIRECT
            if (typeof gAvoidQueryMicroserviceFlag != "undefined" && gAvoidQueryMicroserviceFlag) {
                 // get answer for msiOrQsResultCallback
                var sparqlCallback = function (cn, resCallback, failCallback, sparql) {
                    setStatusProgressBar("Running direct query", 25);
                    var ssi = cn.getDefaultQueryInterface();
                    ssi.executeAndParseToSuccess(sparql, resCallback, failCallback );
                    
                }.bind(this, gConn, doQueryDirectCallback, guiEndQuery);
                
                // generate sparql and send to sparqlCallback
                setStatusProgressBar("Running direct query", 5);
                var ngClient = new MsiClientNodeGroupService(Config.services.nodeGroup.url);
                ngClient.execAsyncGenerateSelect(tmpNodeGroup, gConn, sparqlCallback, guiEndQuery);
             
            // Microservices
            } else {
                kdlLogEvent("SF: Query vi Dispatcher");
                
                setStatusProgressBar.bind(this, "Running Query", 0);
                var client = new MsiClientNodeGroupExec(Config.services.nodeGroupExec.url, Config.timeout.long);
                var jobIdCallback = MsiClientNodeGroupExec.buildCsvUrlSampleJsonCallback(Config.resultsTable.sampleSize,
                                                                                     doQueryTableResCallback,
                                                                                     guiEndQuery,
                                                                                     setStatusProgressBar.bind(this, "Running Query"),
                                                                                     this.checkForCancel.bind(this),
                                                                                     Config.services.status.url,
                                                                                     Config.services.results.url);
               setStatusProgressBar("Running Query", 1);
               client.execAsyncDispatchSelectFromNodeGroup(tmpNodeGroup, 
                                                           gConn, 
                                                           gFormConstraint ? gFormConstraint.getConstraintSet() : null, 
                                                           null, 
                                                           jobIdCallback, 
                                                           guiEndQuery);

            }
        };
		
        doQueryDirectCallback = function (sparqlServerResult) {
            setStatusProgressBar("Running direct query", 95);
            
            var doneCallback = function () {
                setStatusProgressBar("Running direct query", 100);
                guiEndQuery();
            }
            
            sparqlServerResult.putSparqlResultsDatagridInDiv(	document.getElementById("gridDiv"), doneCallback, true );
            
        };
    
		doQueryTableResCallback = function(csvFilename, fullURL, results) {
                    
            // display anchor for fullURL
            var hdiv = document.getElementById("hrefDiv");
            var filename = fullURL.split('/').pop();
            var anchor = "<a href='" + fullURL + "'>" + csvFilename + "</a>";
            hdiv.innerHTML = "<b>Full Results:</b><br>";
            hdiv.innerHTML += anchor;
            hdiv.innerHTML += "<br>";  
                
            results.setLocalUriFlag(true);
            
            var headerHTML = "";
            if (results.getRowCount() == Config.resultsTable.sampleSize) {
                headerHTML += " <span class='label label-warning'>Showing first " + String(Config.resultsTable.sampleSize) + " rows. </span> "
            }
            results.setAnchorFlag(true);
			results.putTableResultsDatagridInDiv(document.getElementById("gridDiv"), headerHTML);
											
			guiEndQuery();
        };
		
		//***********  save and restore *************//
    
        /**
         **  Get a sparqlGraphJson with 
         **       - expand optional subgraphs
         **       - formRows added
         **/
		getSGJson = function() {
            var tmpNodegroup = gNodeGroup.deepCopy();
            tmpNodegroup.expandOptionalSubgraphs();
            
            var sgJson = new SparqlGraphJson(gConn, tmpNodegroup);
			sgJson.setExtra("formRows", getFormRowsArray());
				
			if (formConstraintsGet() != null) {
				 sgJson.setExtra("constraintSet", formConstraintJson());
			}
            
			return sgJson;
		};

        restoreQueryFromJsonStr = function(str) {
            restoreQueryFromJson(JSON.parse(str));
        };
            
		restoreQueryFromJson = function(json) {
            var sgJson = new SparqlGraphJson();
            sgJson.fromJson(json);

			if (sgJson.getVersion() > 2) {
				alertUser("JSON file is newer than this software version.<br>Aborting.", "JSON Version Error");
                return;
			}

			initNodeGroup();
			initForm();
			formConstraintsInit();
            clearResults();
            
            // set conn
            var tmpConn = sgJson.getSparqlConn();
            if (tmpConn != null) {
                gConn = tmpConn;
                
                // after loading new connection
                var success = function (sgj) {
                    loadSuccess2();
                    restoreQueryFromJson2(sgj);
                }.bind(this, sgJson)
                
                gOInfo = new OntologyInfo();
                BackwardCompatibleUtil.loadSparqlConnection(gOInfo, gConn, Config.services.query.url, 
                                                            setStatus, 
                                                            success, 
                                                            loadFailure);
            } else {
                restoreQueryFromJson2(sgJson);
            }

		};

        /*
         * nodegroup, form rows, and constraints
         */
        restoreQueryFromJson2 = function(sgJson) {
            
            try {
                sgJson.getNodeGroup(gNodeGroup, gOInfo);
            } catch (e) {
                ModalIidx.alert("Error loading nodegroup",
                                 e.hasOwnProperty("message") ? e.message : e
                                 );
                doClearFormBut();               
                return;
            }
            
            var formRows = sgJson.getExtra("formRows");
            if (formRows == null) formRows = [];
            
			// fill up the form
			for (var i = 0; i < formRows.length; i++) {
				// read and check parentSNode
				var parentSNode = gNodeGroup.getNodeBySparqlID(formRows[i][0]);
				if (parentSNode === null) {
					alertUser("Error reading Json.  Can't find parent node: "
							+ formRows[i][0]);
					initNodeGroup();
					initForm();
					formConstraintsInit();
					return;
				}

				// read and check itemKeyName if it is not ""
				var itemKeyName = formRows[i][1];
				if (itemKeyName !== "") {
					if (parentSNode.getNodeItemByKeyname(itemKeyName) == null &&
				        parentSNode.getPropertyByKeyname(itemKeyName) == null     ) {

						alertUser("Error reading Json.  Can't find item: "
								+ parentNode.getSparqlID() + "->"
								+ itemKeyName);
						initNodeGroup();
						initForm();
						formConstraintsInit();
						return;
					}
				} else {
					itemKeyName = null;
				}

				// read and check childSNode if it is not ""
				var childSNode = null;
				if (formRows[i][2] !== "") {
					var childSNode = gNodeGroup
							.getNodeBySparqlID(formRows[i][2]);
					if (childSNode === null) {
						alertUser("Error reading Json.  Can't find child node: "
								+ formRows[i][2]);
						initNodeGroup();
						initForm();
						formConstraintsInit();
						return;
					}
				} else {
					childSNode = null;
				}

				addFormRow(parentSNode, itemKeyName, childSNode, true);
			}
			
            addMissingFormRows();
            
            // if there are constraints, load them
            var constraintSet = sgJson.getExtra("constraintSet");
			
			if (constraintSet != null ) {
				formConstraintSetFromJson(constraintSet);
			}
			
			formConstraintsUpdateTable();
			showDebug("read from JSON", gNodeGroup);
			
			kdlLogEvent("SF: Upload Query");
        };
    
        addMissingFormRows = function() {
            var sNodeList = gNodeGroup.getSNodeList();
            for (var i=0; i < sNodeList.length; i++) {
                
                // if sNode is returned
                if (sNodeList[i].getIsReturned() == true) {
                    
                    var id = sNodeList[i].getSparqlID();
                    // if sNode isn't on the form
                    if (! rowExists(null, null, id)) {
                        var nodeItems = gNodeGroup.getConnectingNodeItems(sNodeList[i]);
                        if (nodeItems.length != 1) {
                            ModalIidx.alert("Error",
                                            "Having trouble adding return to form: " + id +
                                            "<br><br>Expecting a single incoming connection:" + id);
                        } 
                        
                        addFormRow( gNodeGroup.getNodeItemParentSNode(nodeItems[0]),
                                    nodeItems[0].getKeyName(), 
                                    sNodeList[i]);
                    }
                }
                
                // check all returned properties
                var retPropList = sNodeList[i].getReturnedPropertyItems();
                for (var j=0; j < retPropList.length; j++) {
                    if (! rowExists(sNodeList[i].getSparqlID(), retPropList[j].getKeyName(), null)) {
                        addFormRow(sNodeList[i], 
                                   retPropList[j].getKeyName(), 
                                   null);
                    }
                }
            }
        };
    
        rowExists = function(snode, propOrNode, child) {
            // get an object representing a specific valid rowId
			var table = document.getElementById("formTable");

			for (var i = 0; i < table.rows.length; i++) {
                var row = table.rows[i].cells;
                if (  (snode == null || snode == html_decode(row[1].innerHTML)) &&
                      (propOrNode == null || propOrNode == html_decode(row[2].innerHTML)) &&
                      (child == null || child == html_decode(row[3].innerHTML))
                    ) {
                    return true;
                }
			}
			return false;
        };
    
		//***********  misc *************//               // PEC TODO: should be in their own util.js file 

		enableButton = function(id) {
			document.getElementById(id).disabled = false;
		};

		disableButton = function(id) {
			document.getElementById(id).disabled = true;
		};

		html_decode = function(text) {
			var entities = [ [ 'apos', '\'' ], [ 'amp', '&' ],
					[ 'lt', '<'],
        			['gt', '>' ] ];

			for (var i = 0, max = entities.length; i < max; ++i)
				text = text.replace(new RegExp('&' + entities[i][0]
						+ ';', 'g'), entities[i][1]);

			return text;
		};

		alertUser = function(msgHtml, optTitle) {
			ModalIidx.alert( (typeof optTitle == "undefined" ? "Alert" : optTitle),   msgHtml);
			kdlLogEvent("SF: alert", "message", msgHtml);
		};

	}

);