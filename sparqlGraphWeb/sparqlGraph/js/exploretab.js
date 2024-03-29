/**
 ** Copyright 2019 General Electric Company
 **
 ** Authors:  Paul Cuddihy
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

/*
 *  ExploreTab - the GUI elements for building an ImportSpec
 *
 *  Basic programmer notes:
 *     ondragover
 *         - preventDefault will allow a drop
 *         - stopPropagation will stop the question from being bubbled up to a parent.
 */

//  https://cdnjs.cloudflare.com/ajax/libs/vis/4.21.0/vis.min.css

define([	// properly require.config'ed
         	'sparqlgraph/js/iidxhelper',
            'sparqlgraph/js/modaliidx',
            'sparqlgraph/js/msiclientnodegroupexec',
            'sparqlgraph/js/msiclientontologyinfo',
            'sparqlgraph/js/msiclientutility',
            'sparqlgraph/js/msiclientstatus',
            'sparqlgraph/js/msiclientresults',
            'sparqlgraph/js/msiresultset',
            'sparqlgraph/js/ontologyinfo',
            'sparqlgraph/js/restrictiontree',
            'sparqlgraph/js/shacltree',
            'sparqlgraph/js/visjshelper',

         	'jquery',

            'visjs/vis.min',

			// shimmed
         	'sparqlgraph/dynatree-1.2.5/jquery.dynatree',
            'sparqlgraph/js/ontologytree',
            'sparqlgraph/js/belmont'

		],

    // TODO: this isn't leveraging VisJsHelper properly.  Code duplication.
	function(IIDXHelper, ModalIidx, MsiClientNodeGroupExec, MsiClientOntologyInfo, MsiClientUtility, MsiClientStatus, MsiClientResults, MsiResultSet, OntologyInfo, RestrictionTree, ShaclTree, VisJsHelper, $, vis) {


		//============ local object  ExploreTab =============
		var ExploreTab = function(treediv, canvasdiv, buttondiv, topControlForm, oInfoClientURL, utilityClientURL) {
            this.controlDivParent = treediv;
            this.canvasDivParent = canvasdiv;

            this.botomButtonDiv = buttondiv;
            this.topControlForm = topControlForm;
            this.oInfoClientURL = oInfoClientURL;
            this.utilityClientURL = utilityClientURL;

            this.infospan = document.createElement("span");
            this.infospan.style.marginRight = "3ch";
            this.oInfo = null;
            this.conn = null;
            this.oTree = null;

            this.controlDivHash = this.initControlDivHash();  	// left-hand pane for each mode	(usually contains a tree)
            this.canvasDivHash = this.initCanvasDivHash();		// right-hand pane for each mode (usually contains a network)
            this.configDivHash = this.initConfigDivHash();
            this.networkHash = this.initNetworkHash();			// network for each mode

            this.busyFlag = false;
            this.ignoreSelectFlag = false;
            this.cancelFlag = false;

            this.initTopControlForm();
            this.initControlDivs();
            this.initBottomButtonDiv();

            this.progressDiv = document.createElement("div");
            this.progressDiv.id = "etProgressDiv";
            this.botomButtonDiv.appendChild(this.progressDiv);
        };

		ExploreTab.MAX_LAYOUT_ELEMENTS = 100;
        ExploreTab.MODE_ONTOLOGY_CLASSES = "Ontology Classes";
        ExploreTab.MODE_ONTOLOGY_DETAIL = "Ontology Detail";
        ExploreTab.MODE_INSTANCE = "Instance Data";
        ExploreTab.MODE_STATS = "Instance Counts";
        ExploreTab.MODE_RESTRICTIONS = "Restrictions";
        ExploreTab.MODE_SHACL = "SHACL Validation";
        ExploreTab.MODES = [ExploreTab.MODE_ONTOLOGY_CLASSES, ExploreTab.MODE_ONTOLOGY_DETAIL, ExploreTab.MODE_INSTANCE, ExploreTab.MODE_STATS, ExploreTab.MODE_RESTRICTIONS, ExploreTab.MODE_SHACL];

		ExploreTab.prototype = {

			// initialize left-hand pane for each mode
            initControlDivHash : function() {
                this.controlDivHash = {};
                for (var m of ExploreTab.MODES) {
                    this.controlDivHash[m] = document.createElement("div");
                    this.controlDivHash[m].id="exploreTabControl_" + m.replace(" ", "-");
                }
                return this.controlDivHash;
            },

			// initialize right-hand pane for each mode
            initCanvasDivHash : function() {
                this.canvasDivHash = {};
                for (var m of ExploreTab.MODES) {
                    this.canvasDivHash[m] = document.createElement("div");
                    this.canvasDivHash[m].style.width="100%";
                    this.canvasDivHash[m].style.height="100%";
                }
                return this.canvasDivHash;
            },

            initConfigDivHash : function() {
                this.configDivHash = {};
                for (var m of ExploreTab.MODES) {
                    this.configDivHash[m] = document.createElement("div");
                }
                return this.configDivHash;
            },

			// initialize network for each mode
            initNetworkHash : function() {
                this.networkHash = {};
                for (var m of ExploreTab.MODES) {
					this.networkHash[m] = new vis.Network(this.canvasDivHash[m], {}, this.getDefaultOptions(m));
                }
                
                // in some modes, enable double-click to expand node
            	this.networkHash[ExploreTab.MODE_RESTRICTIONS].on('doubleClick', VisJsHelper.expandSelectedNodes.bind(this, this.canvasDivHash[ExploreTab.MODE_RESTRICTIONS], this.networkHash[ExploreTab.MODE_RESTRICTIONS], ""));
            	this.networkHash[ExploreTab.MODE_SHACL].on('doubleClick', VisJsHelper.expandSelectedNodes.bind(this, this.canvasDivHash[ExploreTab.MODE_SHACL], this.networkHash[ExploreTab.MODE_SHACL], ""));
                
                return this.networkHash;
            },

            // generate options for each mode
            getDefaultOptions : function(mode) {

                // options shared by all.  Inject the correct configdiv
                var options = {
                    configure: {
                        enabled: true,
                        container: this.configDivHash[mode],
                        filter: "layout physics",
                        showButton: true
                    },
                    groups: {
                        useDefaultGroups: true,
                        data: {color:{background:'white'}, shape: 'box'}
                    },
                    interaction: {
                        multiselect: true,
                        navigationButtons: true,
                        keyboard: {
                            bindToWindow: false
                        }
                    },
                    manipulation: {
                        enabled: true,   
                        initiallyActive: false,
                        addNode: false,
                        addEdge: false,
                        editEdge: false,
                        
                        deleteNode: true,
                        deleteEdge: true
                    }
                };

                // options special to each mode
                if (mode == ExploreTab.MODE_ONTOLOGY_CLASSES) {
                    options.layout =  {
                        "hierarchical": {
                          "enabled": true,
                          "levelSeparation": -150,
                          "direction": "DU",
                          "sortMethod": "directed"
                        }
                    };
                    options.physics = {
                            "hierarchicalRepulsion": {
                            "centralGravity": 0,
                            "springLength": 30,
                            "nodeDistance": 260,
                            "damping": 0.21
                          },
                        "minVelocity": 0.75,
                        "solver": "hierarchicalRepulsion"
                    };

                } else if (mode == ExploreTab.MODE_ONTOLOGY_DETAIL) {
                    options.physics ={
                              "barnesHut": {
                                "centralGravity": 0.15,
                                "springLength": 180
                              },
                              "maxVelocity": 38,
                              "minVelocity": 0.75,
                              "solver": "barnesHut"
                          };

                } else if (mode == ExploreTab.MODE_INSTANCE) {
                    options.physics ={
                              "barnesHut": {
                                "centralGravity": 0.15,
                                "springLength": 180
                              },
                              "maxVelocity": 38,
                              "minVelocity": 0.75,
                              "solver": "barnesHut"
                          };

                } else if (mode == ExploreTab.MODE_STATS) {
                    options.physics ={
                              "barnesHut": {
                                "centralGravity": 0.15,
                                "springLength": 180
                              },
                              "maxVelocity": 38,
                              "minVelocity": 0.75,
                              "solver": "barnesHut"
                          };
                }

                return options;
            },

            setConn: function(conn, oInfo) {
                this.conn = conn;

                this.oInfo = new OntologyInfo(oInfo.toJson());  // deepCopy
                this.oTree.setOInfo(this.oInfo);

				this.restrictionTree.clear();
				this.shaclTree.clear();
                this.clearNetwork(ExploreTab.MODES);
            },

            // Use opened this tab
            // Draw it it isn't drawn yet.
            // Otherwise everything should be the same as when they left last time.
            takeFocus : function() {
                if (this.networkHash[this.getMode()].body.data.nodes.getIds().length == 0) {
                    this.draw();
                }
            },

            // User left this tab
            // Make sure it isn't using any resources
            releaseFocus : function() {
                this.stopLayout();
            },

            initTopControlForm : function() {
                var table = document.createElement("table");
                table.style.width="100%";
                this.topControlForm.appendChild(table);

                // left
                var tr = document.createElement("tr");
                table.appendChild(tr);
                var td = document.createElement("td");
                td.align="right";
                tr.appendChild(td);

                // center
                td = document.createElement("td");
                td.align="right";
                tr.appendChild(td);

                var bold = document.createElement("b");
                td.appendChild(bold);
                bold.innerHTML = "Explore mode: ";
                var select = IIDXHelper.createSelect("etSelect", [ExploreTab.MODE_ONTOLOGY_CLASSES, ExploreTab.MODE_ONTOLOGY_DETAIL, ExploreTab.MODE_INSTANCE, ExploreTab.MODE_STATS, ExploreTab.MODE_RESTRICTIONS, ExploreTab.MODE_SHACL], [ExploreTab.MODE_ONTOLOGY_CLASSES]);
                select.onchange = this.draw.bind(this);
                td.appendChild(select);

                // right
                td = document.createElement("td");
                td.align="right";
                tr.appendChild(td);

                // network config physics
                var showConfig = function() {
                    VisJsHelper.showConfigDialog(this.configDivHash[this.getMode()], function(){});
                    return false;
                }.bind(this);

                but = IIDXHelper.createIconButton("icon-magnet", showConfig, undefined, undefined, undefined, "Network physics");
                td.appendChild(but);
                IIDXHelper.appendSpace(td);

                // redraw button
                td.appendChild(IIDXHelper.createIconButton("icon-refresh", function () {this.clearNetwork(); this.draw(); return false;}.bind(this), undefined, undefined, undefined, "Redraw network"));
                IIDXHelper.appendSpace(td);

                // stop layout
                td.appendChild(IIDXHelper.createIconButton("icon-off", this.stopLayout.bind(this), undefined, undefined, undefined, "Stop layout"));
                IIDXHelper.appendSpace(td);

                // clear
                var but = IIDXHelper.createButton("Clear", function () {this.clearNetwork();}.bind(this));
                td.appendChild(but);
            },

            /**
			 * Initialize control divs (left-hand panes) for all modes
			 */
            initControlDivs : function() {
				this.initControlDiv_InstanceMode();
				this.initControlDiv_OntologyClassesMode();
				this.initControlDiv_OntologyDetailMode();
				this.initControlDiv_StatsMode();
				this.initControlDiv_RestrictionsMode();
				this.initControlDiv_ShaclMode();
            },

			// Initialize control div for ontology classes mode
            initControlDiv_OntologyClassesMode : function() {
                var div = this.controlDivHash[ExploreTab.MODE_ONTOLOGY_CLASSES];
                div.style.margin="1ch";
                var h = document.createElement("h3");
                div.appendChild(h);
                h.innerHTML = ExploreTab.MODE_ONTOLOGY_CLASSES;
                div.appendChild(IIDXHelper.buildList(["Show superclass relationships.","Color by namespace."]));
            },
            
            // Initialize control div for ontology detail mode
            initControlDiv_OntologyDetailMode : function() {
                var div = this.controlDivHash[ExploreTab.MODE_ONTOLOGY_DETAIL];
                div.style.margin="1ch";
                var h = document.createElement("h3");
                div.appendChild(h);
                h.innerHTML = ExploreTab.MODE_ONTOLOGY_DETAIL;
                div.appendChild(IIDXHelper.buildList(["Show all raw triples in the ontology graph(s).","Color by type."]));
            },

			// Initialize control div for instance data mode
            initControlDiv_InstanceMode : function() {
                // INSTANCE
                var div = this.controlDivHash[ExploreTab.MODE_INSTANCE];
                div.style.margin="1ch";

				// add search bar, expand/collapse buttons
                var dom = IIDXHelper.createSearchDiv(this.doSearch, this);
                var but = IIDXHelper.createIconButton("icon-folder-open", this.doExpand.bind(this), undefined, undefined, undefined, "Expand all");
                but.style.marginLeft = "1ch";
                dom.appendChild(but);
                but = IIDXHelper.createIconButton("icon-folder-close", this.doCollapse.bind(this), undefined, undefined, undefined, "Collapse all");
                but.style.marginLeft = "1ch";
                dom.appendChild(but);
                div.appendChild(dom);

				// add dropdown for single vs subtree mode
                var hform1 = IIDXHelper.buildHorizontalForm(true)
                div.appendChild(hform1);
                var select = IIDXHelper.createSelect("etTreeSelect", [["single",2], ["sub-tree",3]], ["multi"], false, "input-small");
                select.onchange = function() {
                    this.oTree.tree.options.selectMode = parseInt(document.getElementById("etTreeSelect").value);
                }.bind(this);
                hform1.appendChild(document.createTextNode(" select mode:"));
                hform1.appendChild(select);
                
                // initialize network
                this.initDynaTree_InstanceMode();
            },
            
            // Initialize control div for stats mode
            initControlDiv_StatsMode : function() {
                var div = this.controlDivHash[ExploreTab.MODE_STATS];
                div.style.margin="1ch";

                var h = document.createElement("h3");
                div.appendChild(h);
                h.innerHTML = ExploreTab.MODE_STATS;
                div.appendChild(IIDXHelper.buildList(["Number of predicates connecting each exact class.","Color by namespace."]));
            },
            
            // Initialize control div for restrictions mode
            initControlDiv_RestrictionsMode : function() {
                var div = this.controlDivHash[ExploreTab.MODE_RESTRICTIONS];
                div.style.margin="1ch";
                
                // add 2 dropdowns, using a table for formatting
                // dropdown to show violations only (exceeds maximum cardinality) or also incomplete data (does not meet minimum cardinality)
                var modeSelectDropdown = IIDXHelper.createSelect("restrictionTreeModeSelect", [["violations only",0], ["violations and incomplete data",1]], ["violations and incomplete data"], false, "input-large");
                modeSelectDropdown.onchange = function() {
					this.restrictionTree.setExceedsOnlyMode(parseInt(document.getElementById("restrictionTreeModeSelect").value) == 0);
                    this.restrictionTree.draw();
                }.bind(this);
				// dropdown to pick sort option
                var sortSelectDropdown = IIDXHelper.createSelect("restrictionTreeSortSelect", [["class",0], ["count",1], ["percentage",2]], ["multi"], false, "input-large");
                sortSelectDropdown.onchange = function() {
                    this.restrictionTree.setSortMode(parseInt(document.getElementById("restrictionTreeSortSelect").value));
                    this.restrictionTree.sort();
                }.bind(this);
				var table = div.appendChild(document.createElement("table"));
				table.appendChild(document.createElement("col"));
				var tr = table.insertRow();
				tr.insertCell().appendChild(document.createTextNode("Show:"));
				tr.insertCell().appendChild(modeSelectDropdown);	// severity dropdown
				tr = table.insertRow();
				tr.insertCell().appendChild(document.createTextNode("Sort by:"));
				tr.insertCell().appendChild(sortSelectDropdown);	// sort-by dropdown
                
                // initialize tree
                this.initDynaTree_RestrictionsMode();
                this.restrictionTree.setSortMode(0);  // set sort to default
            },
            
			// Initialize control div for SHACL mode
			initControlDiv_ShaclMode: function() {
				var div = this.controlDivHash[ExploreTab.MODE_SHACL];
				div.style.margin = "1ch";

				// button to select SHACL rules file
				var runSelectedShaclFile = function(e) {
					// after user selects file, query for shacl results and display them in tree
					if (e.target.files.length > 0) {		// If user hit cancel, then get 0 files here.  File picker disallows multiple files.					
						var shaclCallback = this.buildStatusResultsCallback(
							this.shaclTree.draw.bind(this.shaclTree),	// after retrieving table, draw the tree
							MsiClientResults.prototype.execGetJsonBlobRes
						).bind(this);

						this.clearNetwork(); // clear the graph
						this.shaclTree.clear();
						var client = new MsiClientUtility(this.utilityClientURL, ModalIidx.alert.bind(this, "Error"));
						client.execGetShaclResults(gConn, e.target.files[0], "Info", shaclCallback);
						shaclTtlFileUploader.value = null;  // reset so that reload the same file triggers the change event
					}
				}.bind(this);
				var shaclTtlFileUploader = document.createElement("input");
				shaclTtlFileUploader.type = "file";
				shaclTtlFileUploader.accept = ".ttl";  						// accept files with ttl extension only
				shaclTtlFileUploader.style = "color: rgba(0, 0, 0, 0)";  	// hides the "No file chosen" text
				shaclTtlFileUploader.addEventListener('change', runSelectedShaclFile, false);

				// dropdown for severity
				var modeSelectDropdown = IIDXHelper.createSelect("shaclTreeSeverityModeSelect", [
					["violations only", ShaclTree.SEVERITYMODE_VIOLATION],
					["violations & warnings", ShaclTree.SEVERITYMODE_WARNING],
					["violations, warnings & info", ShaclTree.SEVERITYMODE_INFO]
				], ["violations, warnings & info"], false, "input-large");
				modeSelectDropdown.onchange = function() {				
					document.body.style.cursor="wait";	// re-drawing the tree may take a few seconds, show wait cursor
					setTimeout(() => {   				// use setTimeout to give wait cursor a chance to appear
						this.shaclTree.setSeverityMode(document.getElementById("shaclTreeSeverityModeSelect").value);
						this.shaclTree.draw();					// redraw tree
						this.clearNetwork(); 					// clear anything in the graph
						document.body.style.cursor="default"; 	// remove wait cursor
						}, 600);
				}.bind(this);

				// dropdown to pick sort option
				var sortSelectDropdown = IIDXHelper.createSelect("shaclTreeSortSelect", [["shape", 0], ["count", 1]], ["multi"], false, "input-large");
				sortSelectDropdown.onchange = function() {
					this.shaclTree.setSortMode(parseInt(document.getElementById("shaclTreeSortSelect").value));
					this.shaclTree.sort();
				}.bind(this);

				// put the 3 elements above into a table
				var table = div.appendChild(document.createElement("table"));
				table.appendChild(document.createElement("col"));
				var tr = table.insertRow();
				tr.insertCell().appendChild(document.createTextNode("Select SHACL file:"));
				tr.insertCell().appendChild(shaclTtlFileUploader);	// file uploader button
				tr = table.insertRow();
				tr.insertCell().appendChild(document.createTextNode("Show:"));
				tr.insertCell().appendChild(modeSelectDropdown);	// severity dropdown
				tr = table.insertRow();
				tr.insertCell().appendChild(document.createTextNode("Sort by:"));
				tr.insertCell().appendChild(sortSelectDropdown);	// sort-by dropdown

				// initialize tree
				this.initDynaTree_ShaclMode();
				this.shaclTree.setSortMode(0);  // set sort to default
            },

            /*
             * Initialize an empty dynatree into this.controlDivHash[ExploreTab.MODE_INSTANCE]
             */
			initDynaTree_InstanceMode : function() {

                this.controlDivParent.innerHtml = "";
                this.controlDivParent.appendChild(this.controlDivHash[ExploreTab.MODE_INSTANCE]);
                var treeSelector = "#" + this.controlDivHash[ExploreTab.MODE_INSTANCE].id;
                $(treeSelector).dynatree({

                    onSelect: function(flag, node) {
                        this.modifyNetwork_InstanceMode(flag, node);
                    }.bind(this),

                    persist: false,    // true causes a cookie error with large trees
                    selectMode: 2,    // 1 single, 2 multi, 3 multi hierarchical
                    checkbox: true,
                });

                this.oTree = new OntologyTree($(treeSelector).dynatree("getTree"));
            },

			/**
			 * For instance mode, modify network to match selected tree nodes
			 * (flag is true if node selected, false if node deselected)
			 */
            modifyNetwork_InstanceMode : function (flag, node) {

                if (this.ignoreSelectFlag) return;

                // tricky threading:  turn on ignoreSelectFlag while selecting duplicates
                this.ignoreSelectFlag = true;
                this.oTree.selectIdenticalNodes(node, flag);
                this.ignoreSelectFlag = false;

                if (this.getMode() != ExploreTab.MODE_INSTANCE) return;

                var workList = [];
                if (this.oTree.tree.options.selectMode == 3) {
                    workList = this.oTree.getPropertyPairsFamily(node);
                } else {
                    var pair = this.oTree.getPropertyPair(node);
                    if (pair) {
                        workList.push(pair);
                    }
                }

                if (flag) {
                    this.addInstanceData(workList);		// add instance data to graph
                } else {
                    this.deleteInstanceData(workList);	// remove instance data from graph
                }
            },

            /*
             * Initialize an empty dynatree into this.controlDivHash[ExploreTab.MODE_RESTRICTIONS]
             */
			initDynaTree_RestrictionsMode : function() {

                this.controlDivParent.innerHtml = "";
                this.controlDivParent.appendChild(this.controlDivHash[ExploreTab.MODE_RESTRICTIONS]);
                var treeSelector = "#" + this.controlDivHash[ExploreTab.MODE_RESTRICTIONS].id;
                $(treeSelector).dynatree({
                    onSelect: function(flag, node) {
						this.modifyNetwork_RestrictionsMode(flag, node); // user selects/deselects a node
                    }.bind(this),
                    persist: false,    	// true causes a cookie error with large trees
                    selectMode: 1,    	// 1 single, 2 multi, 3 multi hierarchical
                    checkbox: true,
                });
                this.restrictionTree = new RestrictionTree($(treeSelector).dynatree("getTree"));
            },
            
			/*
			 * Initialize an empty dynatree into this.controlDivHash[ExploreTab.MODE_SHACL]
			 */
			initDynaTree_ShaclMode: function() {

				this.controlDivParent.innerHtml = "";
				this.controlDivParent.appendChild(this.controlDivHash[ExploreTab.MODE_SHACL]);
				var treeSelector = "#" + this.controlDivHash[ExploreTab.MODE_SHACL].id;
				$(treeSelector).dynatree({
					onSelect: function(flag, node) {
						this.modifyNetwork_ShaclMode(flag, node); // user selects/deselects a node
					}.bind(this),
					persist: false,    	// true causes a cookie error with large trees
					selectMode: 1,    	// 1 single, 2 multi, 3 multi hierarchical
					checkbox: true,
				});
				this.shaclTree = new ShaclTree($(treeSelector).dynatree("getTree"));
			},

			/**
			 * For restrictions mode, construct network when user selects a URI.  Clear the network if user de-selects a URI.
			 * flag - true if selected, false if de-selected
			 * node - the node containing the uri
			 */
            modifyNetwork_RestrictionsMode : function (flag, node) {

				const canvasDiv = this.canvasDivHash[ExploreTab.MODE_RESTRICTIONS];
				const network = this.networkHash[ExploreTab.MODE_RESTRICTIONS];

				if(!flag){
					// URI was de-selected, clear the network (tree allows only one URI selected at a time)
					this.clearNetwork();
					return;	
				}
				this.clearNetwork(); // clear any prior network (tree allows only one URI selected at a time)
				VisJsHelper.networkBusy(canvasDiv, true);

				var client = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url, g.longTimeoutMsec);
				var resultsCallback = MsiClientNodeGroupExec.buildJsonLdOrTriplesCallback(
					VisJsHelper.addTriples.bind(this, canvasDiv, network, ""),  // add triples to graph
					networkFailureCallback.bind(this, canvasDiv),
					function() { }, // no status updates
					function() { }, // no check for cancel
					g.service.status.url,
					g.service.results.url
				);

				// query to construct the instance with relevant predicates
				const uri = node.data.title;
				const classUri = node.data.classUri;
				const predicate = node.data.predicate;
				client.execAsyncConstructInstanceWithPredicates(uri, classUri, [predicate],	gConn, resultsCallback, networkFailureCallback.bind(this, canvasDiv));
            },

			/**
			 * For SHACL mode, construct network when user selects an offending item.  Clear the network if user de-selects.
			 * flag - true if selected, false if de-selected
			 * node - the node
			 */
			modifyNetwork_ShaclMode: function(flag, node) {

				const canvasDiv = this.canvasDivHash[ExploreTab.MODE_SHACL];
				const network = this.networkHash[ExploreTab.MODE_SHACL];

				if (!flag) {
					// URI was de-selected, clear the network (tree allows only one URI selected at a time)
					this.clearNetwork();
					return;
				}
				this.clearNetwork(); // clear any prior network (tree allows only one URI selected at a time)
				VisJsHelper.networkBusy(canvasDiv, true);

				var client = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url, g.longTimeoutMsec);
				var resultsCallback = MsiClientNodeGroupExec.buildJsonLdOrTriplesCallback(
					VisJsHelper.addTriples.bind(this, canvasDiv, network, ""),  // add triples to graph
					networkFailureCallback.bind(this, canvasDiv),
					function() { }, // no status updates
					function() { }, // no check for cancel
					g.service.status.url,
					g.service.results.url
				);

				// construct all connected data
				const nodeTitle = node.data.title;  	// this could be an instance URI or a literal
				// TODO line below uses instance type "node_uri" even though the nodeTitle may be a literal.  Currently works for literal strings (e.g. "id0") but need to revisit.
				client.execAsyncConstructConnectedData(nodeTitle, "node_uri", null, SemanticNodeGroup.RT_NTRIPLES, VisJsHelper.ADD_TRIPLES_MAX, gConn, resultsCallback, networkFailureCallback.bind(this, canvasDiv));
			},


            // main section of buttons along the bottom.
            // Should have controls that are useful for all modes
            initBottomButtonDiv : function() {

                this.botomButtonDiv.innerHTML = "";
                var table = document.createElement("table");
                this.botomButtonDiv.appendChild(table);
                table.width = "100%";

                // match first column's width to treediv
                var colgroup = document.createElement("colgroup");
                table.appendChild(colgroup);

                var col = document.createElement("col");
                colgroup.appendChild(col);
                col.width = this.controlDivHash[ExploreTab.MODE_INSTANCE].offsetWidth;

                var tbody = document.createElement("tbody");
                table.appendChild(tbody);

                var tr = document.createElement("tr");
                tbody.appendChild(tr);

                // -------- cell 1/3 --------
                var td1 = document.createElement("td");
                tr.appendChild(td1);

                //  -------- cell 2/3 --------
                var td2 = document.createElement("td");
                tr.appendChild(td2);
                var hform2 = IIDXHelper.buildHorizontalForm(true)
                td2.appendChild(hform2);

                hform2.appendChild(this.infospan);

                //  -------- cell 3/3 --------
                var td3 = document.createElement("td");
                tr.appendChild(td3);
                td3.align="right";

                var hform3 = IIDXHelper.buildHorizontalForm(true)
                td3.appendChild(hform3);

                // moved everything out of here

            },

            treeSelectAll : function(flag) {
                this.ignoreSelectFlag = true;
                this.oTree.selectAll(flag);
                this.ignoreSelectFlag = false;
                this.draw();
            },

            // get the etSelect value ExploreTab.MODE_ONTOLOGY_CLASSES, or ExploreTab.MODE_INSTANCE
            getMode : function() {
                var sel = document.getElementById("etSelect");
                var value = sel.options[sel.selectedIndex].text;
                return value;
            },

            setModeToOntology : function() {
                var sel = document.getElementById("etSelect");
                sel.selectedIndex = 0;
            },
            
            setModeToRestrictions : function() {
                var sel = document.getElementById("etSelect");
                sel.selectedIndex = 4;
            },

            butSetCancelFlag : function() {
                this.cancelFlag = true;
            },

            startLayout : function() {
                if (this.networkHash[this.getMode()]) {
                    this.networkHash[this.getMode()].startSimulation();
                }
                return false;   // in case this is a callback
            },

            stopLayout : function() {
                if (this.networkHash[this.getMode()]) {
                    this.networkHash[this.getMode()].stopSimulation();
                }
                return false;   // in case this is a callback
            },

            doSearch : function(textElem) {
                this.oTree.search(textElem.value);
                return false;
            },

            doCollapse : function() {
                this.oTree.collapseAll();
                return false;
            },

            doExpand : function() {
                this.oTree.expandAll();
                return false;
            },

            // Retrieve the correct control and canvas divs and show them
            // Draw if empty, otherwise presume what is there is ok.
			draw : function() {

				// display correct controls and canvas
				this.controlDivParent.innerHTML = "";
				this.controlDivParent.appendChild(this.controlDivHash[this.getMode()]);
				this.canvasDivParent.innerHTML = "";
				this.canvasDivParent.appendChild(this.canvasDivHash[this.getMode()]);

				// if network is empty it might never have been drawn yet.
				// if it is supposed to be empty then this is cheap.
				// Either way, redraw.
				if (this.networkHash[this.getMode()].body.data.nodes.getIds().length == 0) {

					if (this.getMode() == ExploreTab.MODE_ONTOLOGY_CLASSES) {

						this.drawOntologyClasses();
						this.updateGraphInfoBar(); // update nodes/predicate count

					} else if (this.getMode() == ExploreTab.MODE_ONTOLOGY_DETAIL) {

						this.drawOntologyDetail();
						this.updateGraphInfoBar(); // update nodes/predicate count

					} else if (this.getMode() == ExploreTab.MODE_INSTANCE) {

						this.oTree.showAll();
						var workList = this.oTree.getSelectedPropertyPairs();

						// add selected classes as lists [className]
						var classList = this.oTree.getSelectedClassNames();
						for (var c of classList) {
							workList.push([c]);
						}
						this.addInstanceData(workList);
						this.updateGraphInfoBar(); // update nodes/predicate count

					} else if (this.getMode() == ExploreTab.MODE_STATS) {

						var client = new MsiClientOntologyInfo(this.oInfoClientURL, ModalIidx.alert.bind(this, "Error"));
						var drawStatsCallback = this.buildStatusResultsCallback(
							this.drawPredicateStats.bind(this),
							MsiClientResults.prototype.execGetJsonBlobRes
						);
						client.execGetPredicateStats(gConn, drawStatsCallback);
						this.updateGraphInfoBar(); // update nodes/predicate count

					} else if (this.getMode() == ExploreTab.MODE_RESTRICTIONS) {

						this.clearGraphInfoBar();	// remove nodes/predicates info bar

						if(this.restrictionTree.isEmpty()){ // if tree is empty, then populate it, else leave as is
							
							const MAX_RESTRICTION_ROWS = 5000;  // max (concise format) rows
							
							// query for restriction violations and display them in tree
							var client = new MsiClientOntologyInfo(this.oInfoClientURL, ModalIidx.alert.bind(this, "Error"));
							var cardinalityCallback = this.buildStatusResultsCallback(
								this.restrictionTree.draw.bind(this.restrictionTree),
								MsiClientResults.prototype.execGetTableResultsJsonTableRes,
								MAX_RESTRICTION_ROWS
							);
							client.execGetCardinalityViolations(gConn, MAX_RESTRICTION_ROWS, true, cardinalityCallback);
						}
					} else if (this.getMode() == ExploreTab.MODE_SHACL) {

						// don't draw anything, user must select a SHACL file
						this.clearGraphInfoBar();	// remove nodes/predicates info bar
					}

					this.networkHash[this.getMode()].fit();  // fit the graph to the canvas
				}
			},

            // https://stackoverflow.com/questions/12043187/how-to-check-if-hex-color-is-too-black
            // 40 or less is pretty black
            getLuminocity : function(colorCode) {
                var c = colorCode.substring(1);      // strip #
                var rgb = parseInt(c, 16);   // convert rrggbb to decimal
                var r = (rgb >> 16) & 0xff;  // extract red
                var g = (rgb >>  8) & 0xff;  // extract green
                var b = (rgb >>  0) & 0xff;  // extract blue

                var luma = 0.2126 * r + 0.7152 * g + 0.0722 * b; // per ITU-R BT.709
                return luma
            },

            // "get" the groups in sorted order so they are the same color regardless of in what order they appear in data
            //
            // set up and return a hash of fonts with color black or white to match background
            setupGroups : function(nameList, network) {
                var fontHash = {};

                // get groups alphabetically instead of getting them in the order the classes might appear in exactTab.
                // This hopefully forces order.
                // Set up foreground color hash
                for (var ns of nameList.sort()) {
                    // force order
                    var g = network.groups.get(ns);
                    // set foreground based on luminocity
                    if (this.getLuminocity(g.color.background) < 80 ) {
                        fontHash[ns] = { color: "white"};
                    } else {
                        fontHash[ns] = { color: "black"};
                    }
                }
                return fontHash;
            },

            drawPredicateStats : function(json) {
                var SHOW_DATA = true;
                this.clearNetwork();

                var nodeData = [];
                var edgeData = [];

                var blob = json.xhr;
                var fontHash = this.setupGroups(this.oInfo.getNamespaceNames(), this.networkHash[ExploreTab.MODE_STATS]);

                // first pass: add nodes for each type with count
                for (var key in blob.exactTab) {
                    var jObj = JSON.parse(key);

                    // only visualize one-hops
                    if (jObj.triples.length == 1) {
                        var count = blob.exactTab[key];
                        var oSubjectClass = new OntologyName(jObj.triples[0].s);
                        var oPredicate = new OntologyName(jObj.triples[0].p);
                        var oObjectClass = new OntologyName(jObj.triples[0].o);

                        // skipping Type since w already have oSubjectClass
                        if ( oPredicate.getLocalName() == "type") {
                            var myLabel = oSubjectClass.getLocalName() + " " + count;
                            var ns = oSubjectClass.getNamespace();
                            nodeData.push({id: oSubjectClass.getFullName(), font: fontHash[ns], label: myLabel, title: oSubjectClass.getFullName(), group: ns });
                        }
                    }
                }

                // second pass: add edges
                for (var key in blob.exactTab) {
                    var jObj = JSON.parse(key);

                    // only visualize one-hops
                    if (jObj.triples.length == 1) {
                        var count = blob.exactTab[key];
                        var oSubjectClass = new OntologyName(jObj.triples[0].s);
                        var oPredicate = new OntologyName(jObj.triples[0].p);
                        var oObjectClass = new OntologyName(jObj.triples[0].o);

                        // skipping Type since w already have oSubjectClass
                        if ( oPredicate.getLocalName() != "type") {
                            var width = Math.ceil(Math.log10(count));

                            if (oObjectClass.getFullName() == "") {
                                // connection to data, not a class
                                if (SHOW_DATA) {
                                    // data: separate each into it's own node
                                    var dataId = oSubjectClass.getFullName() + "|" + oPredicate.getFullName() + "|data";
                                    nodeData.push({id: dataId, label: " ", group: "data" });
                                    edgeData.push({from: oSubjectClass.getFullName(), to: dataId, label: oPredicate.getLocalName() + " " + count, arrows: 'to', width: width});
                                }

                            } else {
                                // normal class-to-class (all class nodes already added by pass1)
                                edgeData.push({from: oSubjectClass.getFullName(), to: oObjectClass.getFullName(), label: oPredicate.getLocalName() + " " + count, arrows: 'to', width: width});
                            }
                        }
                    }
                }

                // add any left-over data
                this.networkHash[ExploreTab.MODE_STATS].body.data.nodes.add(nodeData);
                this.networkHash[ExploreTab.MODE_STATS].body.data.edges.add(edgeData);

                this.updateGraphInfoBar();
            },

           drawOntologyClasses : function () {
                var nodeData = [];
                var edgeData = [];
                var SHOW_NAMESPACE = false;

                var fontHash = this.setupGroups(this.oInfo.getNamespaceNames(), this.networkHash[ExploreTab.MODE_ONTOLOGY_CLASSES]);

                // namespace nodes
                if (SHOW_NAMESPACE) {
                    for (var namespace of this.oInfo.getNamespaceNames()) {
                        nodeData.push({id: namespace, font: fontHash[namespace], label: namespace, group: namespace , shape: 'box'});
                    }
                }

                // class nodes
                for (var className of this.oInfo.getClassNames()) {
                    var oClass = this.oInfo.getClass(className);

                    nodeData.push({id: className, label: oClass.getNameStr(true), font: fontHash[oClass.getNamespaceStr()], title: oClass.getNameStr(false), group: oClass.getNamespaceStr() });
                }

                // edges
                for (var className of this.oInfo.getClassNames()) {
                    var oClass = this.oInfo.getClass(className);

                    // namespace members
                    if (SHOW_NAMESPACE) {
                        var namespace = this.oInfo.getClass(className).getNamespaceStr();
                        edgeData.push({from: className, to: namespace, label: '', arrows: 'to'});
                    }

                    // make a subclass arrow style
                    // TODO move this
                    var blackObj = {
                        color:'black',
                        highlight:'black',
                        hover: 'black',
                        inherit: false,
                        opacity:1.0
                    }

                    // subclassof
                    for (var parentName of this.oInfo.getClass(className).getParentNameStrings()) {
                        edgeData.push({from: className, to: parentName, label: 'subClassOf', arrows: 'to', color: blackObj, dashes: true});
                    }
                }

                // add any left-over data
                this.networkHash[ExploreTab.MODE_ONTOLOGY_CLASSES].body.data.nodes.add(nodeData);
                this.networkHash[ExploreTab.MODE_ONTOLOGY_CLASSES].body.data.edges.add(edgeData);

                this.updateGraphInfoBar();

            },
            
            drawOntologyDetail : function () {
				var sparql = OntologyInfo.getConstructOntologyQuery(this.conn);
				
            	var client = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url, g.service.status.url, g.service.results.url, g.longTimeoutMsec);

				var progressCallback = function(percent, msg) {
					IIDXHelper.progressBarSetPercent(this.progressDiv, percent, msg);
				}.bind(this);
				
				var failureCallback = function(msg) {
					ModalIidx.alert("Construct query failed", msg);
					this.busy(false);
                    IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
                	IIDXHelper.progressBarRemove(this.progressDiv);                                           
				}.bind(this);
				
 				var triplesCallback = MsiClientNodeGroupExec.buildJsonLdOrTriplesCallback(
																this.drawOntologyDetailCallback.bind(this),
                                                                failureCallback,
                                                                progressCallback,
                                                                function() {var checkForCancel=""; return false;},
                                                                g.service.status.url,
                                                                g.service.results.url);
            
            	IIDXHelper.progressBarCreate(this.progressDiv, "progress-info progress-striped active");
                IIDXHelper.progressBarSetPercent(this.progressDiv, 0, "");
                this.busy(true);
				client.execAsyncDispatchRawSparql(sparql, gConn, triplesCallback, failureCallback, SemanticNodeGroup.RT_NTRIPLES);
			},
			
			drawOntologyDetailCallback : function (res) {
				IIDXHelper.progressBarSetPercent(this.progressDiv, 90);
               	
				var network = this.networkHash[ExploreTab.MODE_ONTOLOGY_DETAIL];
				
				// delete all
				network.selectNodes(network.body.data.nodes.getIds());
				network.deleteSelected();
				
				// add new
	            var edgeList = [];
	            var nodeDict = {};
	            
	            if (res.isNtriplesResults()) {
					triples = res.getNtriplesArray();
					for (var i=0; i < triples.length; i++) {
	                	VisJsHelper.addTripleToDicts(network, triples[i], nodeDict, edgeList, true, false);
	            		if (i % 20 == 0) {
		                    network.body.data.nodes.update(Object.values(nodeDict));
		                    network.body.data.edges.update(edgeList);
		                }
		            }
	            } else {
                	ModalIidx.alert("Failure", "<b>Error:</b> Results returned from service are not N_TRIPLES");
                	return;
            	}

	            network.body.data.nodes.update(Object.values(nodeDict));
	            network.body.data.edges.update(edgeList);
	
	            network.startSimulation();
            
            	this.busy(false);
                IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
               	IIDXHelper.progressBarRemove(this.progressDiv);    
                this.updateGraphInfoBar();
            },

            // add instance data returned by from /dispatchSelectInstanceData REST call
            addToNetwork : function(tableRes) {
                // abbreviate local uri
                var local = function(uri) {
                    var ret = (new OntologyName(uri)).getLocalName();
                    return (ret == undefined) ? uri : ret;
                };

                // abbreviate uri namespace
                var namespace = function(uri) {
                    var ret = (new OntologyName(uri)).getNamespace();
                    return (ret = undefined) ? uri : ret;
                };

                var nodelabel = function (uri, classname) {
                    if (classname == "data") {
                        return local(uri);
                    } else {
                        return "";
                    }
                };
 
                var nodetitle = function (uri, classnames) {
                    var classList = classnames.split(',');
                    for (var i=0; i < classList.length; i++) {
                        classList[i] = local(classList[i]);
                    }
                    return "<strong>" + classList.toString() + "</strong><br>" + local(uri);
                };

                // efficiently (?) grab columns by name only once
                // two styles of table possible
                var rows;
                if (tableRes.getColumnNumber("p") > -1) {
                    rows = tableRes.tableGetNamedRows(["s", "s_class", "p", "o", "o_class"]);
                } else {
                    rows = tableRes.tableGetNamedRows(["s", "s_class"]);
                }
                var s = 0;
                var s_class = 1;
                var p = 2;
                var o = 3;
                var o_class = 4;
                var nodeList = [];
                var nodeHash = {};
                var edgeList = [];

                for (var i=0; i < rows.length; i++) {
                    // read a row describing a triple
                    var s = rows[i][0];
                    var s_class = (rows[i][1] == "") ? "data" : rows[i][1];

                    // --- handle multiple classes ---
                    var classList = [];
                    var existsNode =  this.networkHash[ExploreTab.MODE_INSTANCE].body.data.nodes.get(s);
                    if (existsNode) {
                        classList = classList.concat(existsNode.group.split(","));
                    }
                    if (s in nodeHash) {
                        classList = classList.concat(nodeHash[s].split(","));
                    }
                    // got list of existing classes.
                    // if non-empty: uniquify, sort, stringify
                    if (classList.length > 0) {
                        var classSet = new Set(classList);
                        classSet.add(s_class);
                        s_class = Array.from(classSet).sort().toString();
                    }

                    nodeList.push({id: s, label: nodelabel(s, s_class), title: nodetitle(s, s_class), group: s_class});
                    nodeHash[s] = s_class;

                    // if this row also has predicate and object
                    if (rows[i].length > 2) {
                        var p = rows[i][2];
                        var o = rows[i][3];
                        var o_class = (rows[i][4] == "") ? "data" : rows[i][4];

                        nodeList.push({id: o, label: nodelabel(o, o_class), title: nodetitle(o, o_class), group: o_class});

                        // add the predicate
                        var p_id = s + "," + p + "," + o;
                        if (this.networkHash[ExploreTab.MODE_INSTANCE].body.data.edges.get(p_id) == null) {
                            edgeList.push({  id: p_id, from: s, to: o, label: local(p),
                                                                arrows: 'to',
                                                                color: {inherit: false},
                                                                group: namespace(p)});
                        }
                    }
                }
                this.networkHash[ExploreTab.MODE_INSTANCE].body.data.nodes.update(nodeList);
                this.networkHash[ExploreTab.MODE_INSTANCE].body.data.edges.update(edgeList);
                this.updateGraphInfoBar();
            },

            // delete worklist items from the Network
            //
            deleteInstanceData(workList) {

                this.updateGraphInfoBar();
                this.stopLayout();

                if (!workList || workList.length == 0) {
                    return;
                }

                IIDXHelper.progressBarCreate(this.progressDiv, "progress-info progress-striped active");
                IIDXHelper.progressBarSetPercent(this.progressDiv, 0, "");
                this.busy(true);

                var vNodesLostEdgesList = [];
                var vEdgesToDelete = [];
                
                var i = 0;
                // first pass: remove edges
                for (var w of workList) {
                    var vEdgeList = this.networkHash[ExploreTab.MODE_INSTANCE].body.data.edges.get();
                    if (w.length == 2) {
                        // delete edges
                        var domainUri = w[0];
                        var predicateUri = w[1];
                        // for all edges
                        for (vEdge of vEdgeList) {

                            if (++i % 100 == 1) {
                                var percent = 0 + 40 * i++ / (workList.length * vEdgeList.length);

                                //console.log("first pass " + percent + "%");
                                IIDXHelper.progressBarSetPercent(this.progressDiv, percent, "Removing_edges");
                            }

                            // for all potential matches

                            var spo = vEdge.id.split(',');
                            // if predicate matches
                            if (spo[1] == predicateUri) {
                                // if fromNode is a member of domainUri
                                var fromNode = this.networkHash[ExploreTab.MODE_INSTANCE].body.data.nodes.get(spo[0]);
                                if (fromNode.group.split(",").indexOf(domainUri) > -1) {
                                    vNodesLostEdgesList.push(vEdge.from);
                                    vNodesLostEdgesList.push(vEdge.to);
                                    vEdgesToDelete.push(vEdge);
                                }
                            }
                        }
                    }
                }
                this.networkHash[ExploreTab.MODE_INSTANCE].body.data.edges.remove(vEdgesToDelete);

                // count edges for each node
                var edgeCountHash = {};
                for (var vEdge of this.networkHash[ExploreTab.MODE_INSTANCE].body.data.edges.get()) {
                    //console.log("edge hash");
                    edgeCountHash[vEdge.from] = (edgeCountHash[vEdge.from] ? edgeCountHash[vEdge.from] : 0) + 1;
                    edgeCountHash[vEdge.to] = (edgeCountHash[vEdge.to] ? edgeCountHash[vEdge.to] : 0) + 1;
                }

                // second pass: remove nodes (must also have no edges)
                i=0;
                for (var w of workList){
                    //console.log("remove nodes");
                    // delete nodes
                    if (w.length == 1) {
                        var classUri = w[0];
                        var vNodeList = this.networkHash[ExploreTab.MODE_INSTANCE].body.data.nodes.get();
                        for (var vNode of vNodeList) {

                            if (++i % 200 == 1) {
                                var percent = 40 + 40 * i++ / (workList.length * vNodeList.length);
                                IIDXHelper.progressBarSetPercent(this.progressDiv, percent, "Removing_nodes");
                            }

                            if (vNode.group == classUri) {
                                // simple single-class exact match if no edges remain
                                if (!(vNode.id in edgeCountHash)) {
                                    this.networkHash[ExploreTab.MODE_INSTANCE].body.data.nodes.remove(vNode.id);
                                }
                            } else if (vNode.group.indexOf(classUri) > -1) {
                                // remove class from multi-class nodes
                                vNode.group = vNode.group.split(",").filter(function(uri, x) {return x != uri;}.bind(this, classUri)).toString();
                                this.networkHash[ExploreTab.MODE_INSTANCE].body.data.nodes.update(vNode);
                            }
                        }
                    }
                }

                var vNodesToRemove = [];
                var vNodesLostEdgesSet = new Set(vNodesLostEdgesList);
                // third pass: remove orphan nodes
                var selectedClasses = this.oTree.getSelectedClassNames();
                i=0;
                for (var vNodeId of vNodesLostEdgesSet) {
                    if (++i % 200 == 1) {
                        var percent = 80 + 20 * i / vNodesLostEdgesSet.size;
                        //console.log("third pass " + percent + "%");
                        IIDXHelper.progressBarSetPercent(this.progressDiv, percent, "Removing_orphans");
                    }
                    var vNode = this.networkHash[ExploreTab.MODE_INSTANCE].body.data.nodes.get(vNodeId);
                    // remove iff still exists, only one class, class is not selected in oTree, no edges
                    if (vNode && vNode.group.split(",").length == 1 && !(vNode.id in edgeCountHash) && selectedClasses.indexOf(vNode.group) == -1) {
                        vNodesToRemove.push(vNode);
                    }
                }
                this.networkHash[ExploreTab.MODE_INSTANCE].body.data.nodes.remove(vNodesToRemove);

                IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
                IIDXHelper.progressBarRemove(this.progressDiv);
                this.busy(false);
                this.updateGraphInfoBar();
                this.startLayout();
            },

            busy : function (flag) {
                this.oTree.setAllSelectable(!flag);
            },

            clearNetwork : function(modeList) {
                var modes = modeList ? modeList : [this.getMode()];
                for (var m of modes) {
                    this.networkHash[m].body.data.nodes.clear();
                    this.networkHash[m].body.data.edges.clear();
                }
            },

            // query and add instance data based on the ontologyTree
            // worklist: list of class uris and/or property pairs
            addInstanceData : function (workList) {
                var LIMIT = 1000;
                var OFFSET = 0;

                this.updateGraphInfoBar();

                if (!workList || workList.length == 0) {
                    return;
                }

                this.cancelFlag = false;
                this.busy(true);

                var workIndex = 0;

                var client = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url, g.shortTimeoutMsec);

                var failureCallback = function(messageHTML) {
                    ModalIidx.alert("Instance data property retrieval", messageHTML);
                    IIDXHelper.progressBarRemove(this.progressDiv);
                    this.cancelFlag = true;
                    this.busy(false);
                }.bind(this);

                var checkForCancelCallback = function() {
                    return this.cancelFlag;
                };

                var instanceDataCallback = function(tableRes) {

                    this.addToNetwork(tableRes);

                    if (tableRes.getRowCount() < LIMIT) {
                        workIndex += 1;
                        OFFSET = 0;
                        IIDXHelper.progressBarSetPercent(this.progressDiv, 100 * workIndex / workList.length, "Querying instance data");
                    } else {
                        OFFSET += LIMIT;
                    }

                    if (workIndex < workList.length && ! this.cancelFlag) {
                        var asyncCallback0 = MsiClientNodeGroupExec.buildFullJsonCallback(	 instanceDataCallback.bind(this),
                                                                                             failureCallback.bind(this),
                                                                                             function(){},
                                                                                             checkForCancelCallback.bind(this),
                                                                                             g.service.status.url,
                                                                                             g.service.results.url);
                        if (workList[workIndex].length == 2) {
                            this.oTree.activateByPropertyPair(workList[workIndex]);
                            client.execAsyncDispatchSelectInstanceDataPredicates(this.conn, [workList[workIndex]], LIMIT, OFFSET, false, asyncCallback0, failureCallback.bind(this));
                        } else {
                            this.oTree.activateByValue(workList[workIndex][0]);
                            client.execAsyncDispatchSelectInstanceDataSubjects(this.conn, [workList[workIndex][0]], LIMIT, OFFSET, false, asyncCallback0, failureCallback.bind(this));
                        }

                    } else {
                        // done
                        IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
                        IIDXHelper.progressBarRemove(this.progressDiv);
                        this.busy(false);
                        this.cancelFlag = false;
                    }
                };

                var asyncCallback = MsiClientNodeGroupExec.buildFullJsonCallback(	 instanceDataCallback.bind(this),
                                                                                     failureCallback.bind(this),
                                                                                     function(){},
                                                                                     checkForCancelCallback.bind(this),
                                                                                     g.service.status.url,
                                                                                     g.service.results.url);

                IIDXHelper.progressBarCreate(this.progressDiv, "progress-info progress-striped active", this.butSetCancelFlag.bind(this));
                IIDXHelper.progressBarSetPercent(this.progressDiv, 0, "Querying instance data");

                if (workList[workIndex].length == 2) {
                    this.oTree.activateByPropertyPair(workList[workIndex]);
                    client.execAsyncDispatchSelectInstanceDataPredicates(this.conn, [workList[workIndex]], LIMIT, OFFSET, false, asyncCallback, failureCallback.bind(this));
                } else {
                    this.oTree.activateByValue(workList[workIndex][0]);
                    client.execAsyncDispatchSelectInstanceDataSubjects(this.conn, [workList[workIndex][0]], LIMIT, OFFSET, false, asyncCallback, failureCallback.bind(this));
                }

                this.updateGraphInfoBar();
            },

			// display nodes/predicates count under the graph
            updateGraphInfoBar : function () {
                this.infospan.innerHTML = "nodes: " + this.networkHash[this.getMode()].body.data.nodes.getIds().length + "  predicates: " + this.networkHash[this.getMode()].body.data.edges.getIds().length;
            },
            // remove the node/predicates count from under the graph
            clearGraphInfoBar : function () {
				this.infospan.innerHTML = "";
			},

            // Build a callback for single 1-100 job
            // Creates the progressBar, and
            buildStatusResultsCallback : function(successCallback, resultsCall, optMaxRows) {

                var failureCallback = function(msg) {
                    ModalIidx.alert("Alert", msg);
                    IIDXHelper.progressBarRemove(this.progressDiv);
                    this.cancelFlag = false;
                }.bind(this);
                var progressCallback = function(msg, percent) {
                    IIDXHelper.progressBarSetPercent(this.progressDiv, percent, msg);
                }.bind(this);
                var checkForCancelCallback = function() { return this.cancelFlag }.bind(this);

                IIDXHelper.progressBarCreate(this.progressDiv, "progress-info progress-striped active", this.butSetCancelFlag.bind(this));
                this.cancelFlag = false;

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
                                successCallback(results);
                                progressCallback("finishing up", 99);
                                setTimeout(function () {
                                    IIDXHelper.progressBarRemove(this.progressDiv);
                                }.bind(this),
                                200);
                            }.bind(this);
                            var resultsClient = new MsiClientResults(g.service.results.url, jobId);
							if (typeof optMaxRows === "undefined") {
								resultsCall.bind(resultsClient)(resultsSuccessCallback);
							} else {
								resultsCall.bind(resultsClient)(optMaxRows, resultsSuccessCallback);
							}
                        }.bind(this);

                        progressCallback("", 1);

                        // call status service loop
                        var statusClient = new MsiClientStatus(g.service.status.url, jobId, failureCallback);
                        statusClient.execAsyncWaitUntilDone(statusSuccessCallback, checkForCancelCallback, progressCallback);
                    }
                }.bind(this);

                return simpleResCallback;
            },
        }

		return ExploreTab;            // return the constructor
	}

);
