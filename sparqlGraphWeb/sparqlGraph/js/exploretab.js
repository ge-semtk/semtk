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
            'sparqlgraph/js/ontologyinfo',

         	'jquery',

            'visjs/vis.min',

			// shimmed
         	'sparqlgraph/dynatree-1.2.5/jquery.dynatree',
            'sparqlgraph/js/ontologytree',
            'sparqlgraph/js/belmont'

		],

	function(IIDXHelper, ModalIidx, MsiClientNodeGroupExec, OntologyInfo, $, vis) {


		//============ local object  ExploreTab =============
		var ExploreTab = function(treediv, canvasdiv, buttondiv, searchForm) {
            this.treebuttondiv = document.createElement("div");
            this.treebuttondiv.id = "etTreeButtonDiv";
            treediv.appendChild(this.treebuttondiv);

            this.treediv = document.createElement("div");
            this.treediv.id = "etTreeDiv";
            treediv.appendChild(this.treediv);

            // TODO: move somewhere that doesn't look awful and interfere with ontologytree
            this.configdiv = document.createElement("div");
            this.configdiv.style.margin="1ch";
            this.configdiv.id="etConfigDiv";
            this.configdiv.style.display="table";
            this.configdiv.style.background = "rgba(32, 16, 16, 0.2)";

            $(this.configdiv).dialog({  'autoOpen': false,
                                        buttons: [
                                        {
                                          text: "Ok",
                                          icon: "ui-icon-heart",
                                          click: function() {
                                            $( this ).dialog( "close" );
                                          }
                                        }],
                                        dialogClass: "modal",
                                        open: function(event, ui) {
                                            $(".ui-dialog-titlebar-close", ui.dialog | ui).hide();
                                        },
                                      });
            // treediv.appendChild(document.createElement("hr"));
            // treediv.appendChild(this.configdiv);

            this.canvasdiv = document.createElement("div");
            this.canvasdiv.style.margin="1ch";
            this.canvasdiv.id="ExploreTab.canvasdiv_" + Math.floor(Math.random() * 10000).toString();
            this.canvasdiv.style.height="100%";
            this.canvasdiv.style.width="100%";
            canvasdiv.appendChild(this.canvasdiv);

            this.buttondiv = buttondiv;
            this.searchForm = searchForm;

            this.infospan = document.createElement("span");
            this.infospan.style.marginRight = "3ch";
            this.oInfo = null;
            this.conn = null;
            this.oTree = null;
            this.network = null;

            this.busyFlag = false;
            this.ignoreSelectFlag = false;

            this.cancelFlag = false;

            this.initSearchForm();
            this.initDynaTree();
            this.initButtonDiv();
            this.initCanvas();

            this.progressDiv = document.createElement("div");
            this.progressDiv.id = "etProgressDiv";
            this.buttondiv.appendChild(this.progressDiv);


        };

		ExploreTab.MAX_LAYOUT_ELEMENTS = 100;


		ExploreTab.prototype = {

            setOInfo : function (oInfo) {
                this.oInfo = new OntologyInfo(oInfo.toJson());  // deepCopy
                this.oTree.setOInfo(this.oInfo);
                this.setModeToOntology();
            },

            setConn: function(conn) {
                this.conn = conn;
            },

            /*
             * Initialize an empty dynatree
             */
			initDynaTree : function() {

                var treeSelector = "#" + this.treediv.id;

                $(treeSelector).dynatree({
                    onSelect: function(flag, node) {

                        this.selectedNodeCallback(flag, node);
                    }.bind(this),

                    onDblClick: function(node) {
                        // A DynaTreeNode object is passed to the activation handler
                        // Note: we also get this event, if persistence is on, and the page is reloaded.
                        // console.log("You double-clicked " + node.data.title);
                    }.bind(this),

                    dnd: {
                        onDragStart: function(node) {
                            return true;
                        }.bind(this),

                        onDragStop: function(node, x, y, z, aa) {

                        }.bind(this)
                    },

                    persist: true,
                    selectMode: 2,    // 1 single, 2 multi, 3 multi hierarchical
                    checkbox: true,

                });

                this.oTree = new OntologyTree($(treeSelector).dynatree("getTree"));

            },

            selectedNodeCallback : function (flag, node) {
                this.oTree.selectIdenticalNodes(node, flag);

                if (this.ignoreSelectFlag) return;

                if (this.getMode() != "Instance Data") return;

                var workList = [];

                if (this.oTree.tree.options.selectMode == 3) {
                    workList = this.oTree.getPropertyPairsFamily(node);
                } else {
                    workList.push(this.oTree.getPropertyPair(node));
                }

                if (flag) {
                    this.addInstanceData(workList);
                } else {
                    this.deleteInstanceData(workList);
                }
            },

            initCanvas : function() {
                this.clearNetwork();
            },

            // little div sitting on top of the otree
            initSearchForm : function() {

                var div = this.treebuttondiv;
                div.innerHTML = "";
                div.style.padding="1ch";

                var butTable = document.createElement("table");
                this.searchForm.appendChild(butTable);
                butTable.width="100%";
                var tr = document.createElement("tr");
                butTable.appendChild(tr);

                var td = document.createElement("td");
                tr.appendChild(td);
                td.align="left";
                td.appendChild(IIDXHelper.createSearchDiv(this.doSearch, this));

                td.appendChild(IIDXHelper.createButton("Expand", this.doExpand.bind(this)));
                td.appendChild(IIDXHelper.createButton("Collapse", this.doCollapse.bind(this)));

            },

            // main section of buttons
            initButtonDiv : function() {

                this.buttondiv.innerHTML = "";
                var table = document.createElement("table");
                this.buttondiv.appendChild(table);
                table.width = "100%";

                // match first column's width to treediv
                var colgroup = document.createElement("colgroup");
                table.appendChild(colgroup);

                var col = document.createElement("col");
                colgroup.appendChild(col);
                col.width = this.treediv.offsetWidth;

                var tbody = document.createElement("tbody");
                table.appendChild(tbody);

                var tr = document.createElement("tr");
                tbody.appendChild(tr);

                // -------- cell 1/3 --------
                var td1 = document.createElement("td");
                tr.appendChild(td1);
                td1.align="left";
                var hform1 = IIDXHelper.buildHorizontalForm(true)
                td1.appendChild(hform1);

                var select = IIDXHelper.createSelect("etTreeSelect", [["single",2], ["sub-tree",3]], ["multi"], false, "input-small");
                select.onchange = function() {
                    this.oTree.tree.options.selectMode = parseInt(document.getElementById("etTreeSelect").value);
                }.bind(this);

                hform1.appendChild(document.createTextNode(" select mode:"));
                hform1.appendChild(select);
                
                hform1.appendChild(IIDXHelper.createNbspText());
                hform1.appendChild(IIDXHelper.createButton("Select all", this.treeSelectAll.bind(this, true)));

                hform1.appendChild(IIDXHelper.createNbspText());
                hform1.appendChild(IIDXHelper.createButton("Clear all", this.treeSelectAll.bind(this, false)));

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

                // network... button
                hform3.appendChild(IIDXHelper.createButton("network...", function() {$(this.configdiv).dialog("open")}.bind(this)));

                // redraw button
                hform3.appendChild(IIDXHelper.createNbspText());
                hform3.appendChild(IIDXHelper.createButton("redraw", this.drawCanvas.bind(this)));

                var select = IIDXHelper.createSelect("etSelect", ["Ontology", "Instance Data"], ["Ontology"]);
                select.onchange = this.drawCanvas.bind(this);
                hform3.appendChild(IIDXHelper.createNbspText());
                hform3.appendChild(select);

                var but1 = IIDXHelper.createButton("stop query", this.butSetCancelFlag.bind(this));
                hform3.appendChild(IIDXHelper.createNbspText());
                hform3.appendChild(but1);

                var but2 = IIDXHelper.createButton("stop layout", this.stopLayout.bind(this));
                but2.id = "butStopLayout";
                but2.disabled = true;
                hform3.appendChild(IIDXHelper.createNbspText());
                hform3.appendChild(but2);

            },

            treeSelectAll : function(flag) {
                this.ignoreSelectFlag = true;
                this.oTree.selectAll(flag);
                this.ignoreSelectFlag = false;
                this.drawCanvas();
            },

            // get the etSelect value "Ontology", or "Instance Data"
            getMode : function() {
                var sel = document.getElementById("etSelect");
                var value = sel.options[sel.selectedIndex].text;
                return value;
            },

            setModeToOntology : function() {
                var sel = document.getElementById("etSelect");
                sel.selectedIndex = 0;
            },

            butSetCancelFlag : function() {
                this.cancelFlag = true;
            },

            startLayout : function() {
                if (this.network) {
                    this.network.startSimulation();
                }
            },

            stopLayout : function() {
                if (this.network) {
                    this.network.stopSimulation();
                }
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

            // Redraws the entire graph (presuming there's new data)
            // This can be processor-intensive,
            // so, when needed, send in msec of layout time to stop it
            draw : function (stopAfterMsec) {
                this.oTree.showAll();

                this.drawCanvas();

                // stop the layout after stopAfterMsec
                if (typeof stopAfterMsec != "undefined") {
                    setTimeout(this.stopLayout.bind(this), stopAfterMsec);
                }
            },

            drawCanvas : function() {
                if (this.getMode() == "Ontology") {
                    this.drawOntology();
                } else {
                    this.clearNetwork();
                    var workList = this.oTree.getSelectedPropertyPairs();

                    // add selected classes as lists [className]
                    var classList = this.oTree.getSelectedClassNames();
                    for (var c of classList) {
                        workList.push([c]);
                    }
                    this.addInstanceData(workList);
                }
            },

            drawOntology : function () {
                this.network.body.data.nodes.clear();
                this.network.body.data.edges.clear();
                var nodeData = [];
                var edgeData = [];

                // namespace nodes
                for (var namespace of this.oInfo.getNamespaceNames()) {
                    nodeData.push({id: namespace, label: namespace, group: namespace , shape: 'box'});
                }

                // class nodes
                for (var className of this.oInfo.getClassNames()) {
                    var oClass = this.oInfo.getClass(className);
                    var localName = oClass.getNameStr(true);
                    var namespace = oClass.getNamespaceStr();

                    nodeData.push({id: className, label: localName, group: namespace });
                }

                // edges
                for (var className of this.oInfo.getClassNames()) {
                    var oClass = this.oInfo.getClass(className);

                    // namespace members
                    var namespace = this.oInfo.getClass(className).getNamespaceStr();
                    edgeData.push({from: className, to: namespace, label: '', arrows: 'to'});

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
                    for (var parentName of this.oInfo.getClass(className).getParentNameStrs()) {
                        edgeData.push({from: className, to: parentName, label: 'subClassOf', arrows: 'to', color: blackObj, dashes: true});
                    }
                }

                // add any left-over data
                this.network.body.data.nodes.add(nodeData);
                this.network.body.data.edges.add(edgeData);

                var options = {
                  "layout": {
                    "hierarchical": {
                      "enabled": true,
                      "levelSeparation": -150,
                      "direction": "DU",
                      "sortMethod": "directed"
                    }
                  },
                  "physics": {
                    "hierarchicalRepulsion": {
                      "centralGravity": 0
                    },
                    "minVelocity": 0.75,
                    "solver": "hierarchicalRepulsion"
                  }
                }
                this.network.setOptions(options);
                this.updateInfo();
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

                console.log("Adding to nodeJs START");
                for (var i=0; i < rows.length; i++) {
                    // read a row describing a triple
                    var s = rows[i][0];
                    var s_class = (rows[i][1] == "") ? "data" : rows[i][1];

                    // --- handle multiple classes ---
                    var classList = [];
                    var existsNode =  this.network.body.data.nodes.get(s);
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
                        if (this.network.body.data.edges.get(p_id) == null) {
                            edgeList.push({  id: p_id, from: s, to: o, label: local(p),
                                                                arrows: 'to',
                                                                color: {inherit: false},
                                                                group: namespace(p)});
                        }
                    }
                }
                this.network.body.data.nodes.update(nodeList);
                this.network.body.data.edges.update(edgeList);
                this.updateInfo();
                console.log("Adding to nodeJs END");

            },

            // delete worklist items from the Network
            //
            deleteInstanceData(workList) {


                this.updateInfo();
                this.stopLayout();

                if (!workList || workList.length == 0) {
                    return;
                }

                IIDXHelper.progressBarCreate(this.progressDiv, "progress-info progress-striped active");
                IIDXHelper.progressBarSetPercent(this.progressDiv, 0, "");
                this.busy(true);

                var vNodesLostEdgesList = [];
                var vEdgesToDelete = [];

                var START = performance.now();
                var i = 0;
                // first pass: remove edges
                for (var w of workList) {
                    var vEdgeList = this.network.body.data.edges.get();
                    if (w.length == 2) {
                        // delete edges
                        var domainUri = w[0];
                        var predicateUri = w[1];
                        // for all edges
                        for (vEdge of vEdgeList) {

                            if (++i % 100 == 1) {
                                var percent = 0 + 40 * i++ / (workList.length * vEdgeList.length);

                                console.log("first pass " + percent + "%");
                                IIDXHelper.progressBarSetPercent(this.progressDiv, percent, "Removing_edges");
                            }

                            // for all potential matches

                            var spo = vEdge.id.split(',');
                            // if predicate matches
                            if (spo[1] == predicateUri) {
                                // if fromNode is a member of domainUri
                                var fromNode = this.network.body.data.nodes.get(spo[0]);
                                if (fromNode.group.split(",").indexOf(domainUri) > -1) {
                                    vNodesLostEdgesList.push(vEdge.from);
                                    vNodesLostEdgesList.push(vEdge.to);
                                    vEdgesToDelete.push(vEdge);
                                }
                            }
                        }
                    }
                }
                this.network.body.data.edges.remove(vEdgesToDelete);

                console.log("1st pass time: " + (performance.now() - START));
                START = performance.now();

                // count edges for each node
                var edgeCountHash = {};
                for (var vEdge of this.network.body.data.edges.get()) {
                    console.log("edge hash");
                    edgeCountHash[vEdge.from] = (edgeCountHash[vEdge.from] ? edgeCountHash[vEdge.from] : 0) + 1;
                    edgeCountHash[vEdge.to] = (edgeCountHash[vEdge.to] ? edgeCountHash[vEdge.to] : 0) + 1;
                }

                // second pass: remove nodes (must also have no edges)
                i=0;
                for (var w of workList){
                    console.log("remove nodes");
                    // delete nodes
                    if (w.length == 1) {
                        var classUri = w[0];
                        var vNodeList = this.network.body.data.nodes.get();
                        for (var vNode of vNodeList) {

                            if (++i % 200 == 1) {
                                var percent = 40 + 40 * i++ / (workList.length * vNodeList.length);
                                IIDXHelper.progressBarSetPercent(this.progressDiv, percent, "Removing_nodes");
                            }

                            if (vNode.group == classUri) {
                                // simple single-class exact match if no edges remain
                                if (!(vNode.id in edgeCountHash)) {
                                    this.network.body.data.nodes.remove(vNode.id);
                                }
                            } else if (vNode.group.indexOf(classUri) > -1) {
                                // remove class from multi-class nodes
                                vNode.group = vNode.group.split(",").filter(function(uri, x) {return x != uri;}.bind(this, classUri)).toString();
                                this.network.body.data.nodes.update(vNode);
                            }
                        }
                    }
                }

                console.log("2nd pass time: " + (performance.now() - START));
                START = performance.now();

                var vNodesToRemove = [];
                var vNodesLostEdgesSet = new Set(vNodesLostEdgesList);
                // third pass: remove orphan nodes
                var selectedClasses = this.oTree.getSelectedClassNames();
                i=0;
                for (var vNodeId of vNodesLostEdgesSet) {
                    if (++i % 200 == 1) {
                        var percent = 80 + 20 * i / vNodesLostEdgesSet.size;
                        console.log("third pass " + percent + "%");
                        IIDXHelper.progressBarSetPercent(this.progressDiv, percent, "Removing_orphans");
                    }
                    var vNode = this.network.body.data.nodes.get(vNodeId);
                    // remove iff still exists, only one class, class is not selected in oTree, no edges
                    if (vNode && vNode.group.split(",").length == 1 && !(vNode.id in edgeCountHash) && selectedClasses.indexOf(vNode.group) == -1) {
                        vNodesToRemove.push(vNode);
                    }
                }
                this.network.body.data.nodes.remove(vNodesToRemove);
                console.log("3rd pass time: " + (performance.now() - START));

                IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
                IIDXHelper.progressBarRemove(this.progressDiv);
                this.busy(false);
                this.updateInfo();
                this.startLayout();
            },

            busy : function (flag) {
                this.oTree.setAllSelectable(!flag);
            },

            clearNetwork : function() {
                this.configdiv.innerHTML = "";
                // create an array with nodes
                var options = {
                    configure: {
                        enabled: true,
                        container: this.configdiv,
                        filter: "layout physics",
                        showButton: true
                    },
                    groups: {
                        useDefaultGroups: true,
                        data: {color:{background:'white'}, shape: 'box'}
                    },
                    interaction: {
                        multiselect: true,
                    },
                    manipulation: {
                        initiallyActive: false,
                        deleteNode: true,
                        deleteEdge: true,
                    }
                };
                this.network = new vis.Network(this.canvasdiv, {}, options);
            },

            // query and add instance data based on the ontologyTree
            // worklist: list of class uris and/or property pairs
            addInstanceData : function (workList) {
                var LIMIT = 1000;
                var OFFSET = 0;
                var CALL_NOW = 0;

                this.updateInfo();

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
                    var elapsed = performance.now() - CALL_NOW;
                    console.log("query time: " + elapsed);

                    this.addToNetwork(tableRes);
                    elapsed = performance.now() - CALL_NOW;
                    console.log("query & work: " + elapsed);

                    if (tableRes.getRowCount() < LIMIT) {
                        workIndex += 1;
                        OFFSET = 0;
                        IIDXHelper.progressBarSetPercent(this.progressDiv, 100 * workIndex / workList.length, "Querying instance data");
                    } else {
                        OFFSET += LIMIT;
                    }

                    if (workIndex < workList.length && ! this.cancelFlag) {
                        var asyncCallback0 = MsiClientNodeGroupExec.buildFullJsonCallback(
                                                                                             instanceDataCallback.bind(this),
                                                                                             failureCallback.bind(this),
                                                                                             function(){},
                                                                                             checkForCancelCallback.bind(this),
                                                                                             g.service.status.url,
                                                                                             g.service.results.url);

                        CALL_NOW = performance.now();

                        if (workList[workIndex].length == 2) {
                            this.oTree.activateByPropertyPair(workList[workIndex]);
                            client.execAsyncDispatchSelectInstanceDataPredicates(this.conn, [workList[workIndex]], LIMIT, OFFSET, false, asyncCallback0, failureCallback.bind(this));
                        } else {
                            this.oTree.activateByValue(workList[workIndex][0]);
                            client.execAsyncDispatchSelectInstanceDataSubjects(this.conn, [workList[workIndex][0]], LIMIT, OFFSET, false, asyncCallback0, failureCallback.bind(this));
                        }

                    } else {
                        // done
                        // handle predicates
                        IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
                        IIDXHelper.progressBarRemove(this.progressDiv);
                        this.busy(false);
                        this.cancelFlag = false;
                    }
                };

                var asyncCallback = MsiClientNodeGroupExec.buildFullJsonCallback(
                                                                                     instanceDataCallback.bind(this),
                                                                                     failureCallback.bind(this),
                                                                                     function(){},
                                                                                     checkForCancelCallback.bind(this),
                                                                                     g.service.status.url,
                                                                                     g.service.results.url);

                IIDXHelper.progressBarCreate(this.progressDiv, "progress-info progress-striped active");
                IIDXHelper.progressBarSetPercent(this.progressDiv, 0, "Querying instance data");
                CALL_NOW = performance.now();

                if (workList[workIndex].length == 2) {
                    this.oTree.activateByPropertyPair(workList[workIndex]);
                    client.execAsyncDispatchSelectInstanceDataPredicates(this.conn, [workList[workIndex]], LIMIT, OFFSET, false, asyncCallback, failureCallback.bind(this));
                } else {
                    this.oTree.activateByValue(workList[workIndex][0]);
                    client.execAsyncDispatchSelectInstanceDataSubjects(this.conn, [workList[workIndex][0]], LIMIT, OFFSET, false, asyncCallback, failureCallback.bind(this));
                }

                this.updateInfo();

            },

            updateInfo : function () {
                this.infospan.innerHTML = "nodes: " + this.network.body.data.nodes.getIds().length + "  predicates: " + this.network.body.data.edges.getIds().length;
            }
        }

		return ExploreTab;            // return the constructor
	}

);
