/**
 ** Copyright 2017 General Electric Company
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
		var ExploreTab = function(treediv, canvasdiv, buttondiv, searchtxt) {
		    this.treediv = document.createElement("div");
            this.treediv.id = "etTreeDiv";

            // TODO: move somewhere that doesn't look awful and interfere with ontologytree
            this.configdiv = document.createElement("div");
            treediv.appendChild(this.treediv);
            treediv.appendChild(document.createElement("hr"));
            treediv.appendChild(this.configdiv);

            this.canvasdiv = document.createElement("div");
            this.canvasdiv.style.margin="1ch";
            this.canvasdiv.id="ExploreTab.canvasdiv_" + Math.floor(Math.random() * 10000).toString();
            this.canvasdiv.style.height="100%";
            this.canvasdiv.style.width="100%";
            canvasdiv.appendChild(this.canvasdiv);

            this.buttondiv = buttondiv;
            this.infospan = document.createElement("span");
            this.infospan.style.marginRight = "3ch";
            this.searchtxt = searchtxt;
            this.oInfo = null;
            this.conn = null;
            this.oTree = null;
            this.network = null;

            this.largeLayoutFlag = false;
            this.largeLayoutParams={};

            this.cancelFlag = false;

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
                    onActivate: function(node) {
                        // A DynaTreeNode object is passed to the activation handler
                        // Note: we also get this event, if persistence is on, and the page is reloaded.

                        this.selectedNodeCallback(node);
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

            initCanvas : function() {
                // create an array with nodes
                var options = {
                    configure: {
                        enabled: true,
                        container: this.configdiv,
                        filter: "layout physics",
                    },
                    groups: {
                        useDefaultGroups: true,
                        data: {color:{background:'white'}, shape: 'box'}
                    }
                };
                this.network = new vis.Network(this.canvasdiv, {}, options);
            },

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

                // cell 1/3
                var td1 = document.createElement("td");
                tr.appendChild(td1);
                td1.appendChild(IIDXHelper.createButton("Refresh", this.drawCanvas.bind(this)));

                // cell 2/3
                var td2 = document.createElement("td");
                tr.appendChild(td2);
                td2.appendChild(this.infospan);

                // cell 3/3
                var td3 = document.createElement("td");
                tr.appendChild(td3);

                var div3 = document.createElement("div");
                td3.appendChild(div3);
                td3.align="right";
                var select = IIDXHelper.createSelect("etSelect", ["Ontology", "Instance Data"], ["Ontology"]);
                select.onchange = this.drawCanvas.bind(this);
                td3.appendChild(select);
                td3.appendChild(IIDXHelper.createNbspText());


                var but1 = IIDXHelper.createButton("stop query", this.butSetCancelFlag.bind(this));
                td3.appendChild(but1);
                var but2 = IIDXHelper.createButton("stop layout", this.stopLayout.bind(this));
                but2.id = "butStopLayout";
                but2.disabled = true;

                td3.appendChild(IIDXHelper.createNbspText());
                td3.appendChild(but2);
                var but3 = IIDXHelper.createButton("rightButton3", this.butRight3.bind(this));
                td3.appendChild(IIDXHelper.createNbspText());
                td3.appendChild(but3);
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

            butRight3 : function() {
                this.cy.batch(
                    function(){
                        this.cy.nodes().style({'background-color': '#666'});
                    }.bind(this)
                );
            },

            doSearch : function() {
                this.oTree.find(this.searchtxt.value);
            },

            doCollapse : function() {
                this.searchtxt.value="";
                this.oTree.collapseAll();
            },

            doExpand : function() {
                this.oTree.expandAll();
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
                    this.drawInstanceData();
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
                var nodeData = [];
                var edgeData = [];

                // efficiently (?) grab columns by name only once
                var rows = tableRes.tableGetNamedRows(["s", "s_class", "p", "o", "o_class"]);
                var s = 0;
                var s_class = 1;
                var p = 2;
                var o = 3;
                var o_class = 4;
                var nodeList = [];
                var edgeList = [];

                console.log("Adding to nodeJs START");
                for (var i=0; i < rows.length; i++) {
                    // ugly but efficiency mattered
                    var s = rows[i][0];
                    var s_class = (rows[i][1] == "") ? "data" : rows[i][1];
                    var p = rows[i][2];
                    var o = rows[i][3];
                    var o_class = (rows[i][4] == "") ? "data" : rows[i][4];

                    var local = function(uri) {
                        var ret = (new OntologyName(uri)).getLocalName();
                        return (ret == undefined) ? uri : ret;
                    };

                    var namespace = function(uri) {
                        var ret = (new OntologyName(uri)).getNamespace();
                        return (ret = undefined) ? uri : ret;
                    };

                    nodeList.push({id: s, label: local(s), title: local(s_class), group: s_class});

                    // if this row also has predicate and object
                    if (p != "") {
                        nodeList.push({id: o, label: local(o), title: local(o_class), group: o_class});

                        // add the predicate
                        var p_id = s + "_" + p + "_" + o;
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

            drawInstanceData : function () {
                var LIMIT = 1000;
                this.cancelFlag = false;

                this.network.body.data.nodes.clear();
                this.network.body.data.edges.clear();
                this.updateInfo();

                var selectedClassNames = this.oTree.getSelectedClassNames();
                var selectedPredicateNames = this.oTree.getSelectedPropertyNames();

                this.network.setOptions({
                    layout: {
                        hierarchical: false,
                    },
                    physics: {
                        enabled: false,
                    }
                });

                var client = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url, g.shortTimeoutMsec);

                var failureCallback = function(messageHTML) {
                    ModalIidx.alert("Instance data retrieval", messageHTML);
                    IIDXHelper.progressBarRemove(this.progressDiv);
                    this.cancelFlag = true;
                };

                var checkForCancelCallback = function() {
                    return this.cancelFlag;
                };

                var instanceDataCallback = function(total, offset, tableRes) {
                    if (total < 0) {
                        // first call: grab the count
                        IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
                        IIDXHelper.progressBarSetPercent(this.progressDiv, 0, "Querying instance data");
                        total = tableRes.getRsData(0,0);
                    } else {
                        // other calls: add data
                        this.addToNetwork(tableRes);
                        offset += tableRes.getRowCount();
                        IIDXHelper.progressBarSetPercent(this.progressDiv, 100 * offset / total, "Querying instance data");
                    }

                    // decide whether to make more queries
                    if (offset < total && ! this.cancelFlag) {
                        var asyncCallback1 = MsiClientNodeGroupExec.buildFullJsonCallback(
                                                                                             instanceDataCallback.bind(this, total, offset),
                                                                                             failureCallback.bind(this),
                                                                                             function(){},
                                                                                             checkForCancelCallback.bind(this),
                                                                                             g.service.status.url,
                                                                                             g.service.results.url);



                        client.execAsyncDispatchSelectInstanceData(this.conn, selectedClassNames, selectedPredicateNames, LIMIT, offset, false,
                                                                    asyncCallback1,
                                                                    failureCallback.bind(this));
                    } else {
                        IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
                        setTimeout(IIDXHelper.progressBarRemove.bind(IIDXHelper, this.progressDiv), 1000);
                    }

                };

                var countStatusCallback = function(percent) {
                    IIDXHelper.progressBarSetPercent(this.progressDiv, percent, "Counting instance data");
                };

                var asyncCallback = MsiClientNodeGroupExec.buildFullJsonCallback(
                                                                                     instanceDataCallback.bind(this, -1, 0),
                                                                                     failureCallback.bind(this),
                                                                                     countStatusCallback.bind(this),
                                                                                     checkForCancelCallback.bind(this),
                                                                                     g.service.status.url,
                                                                                     g.service.results.url);

                IIDXHelper.progressBarCreate(this.progressDiv, "progress-info progress-striped active");
                countStatusCallback.bind(this)(0);

                client.execAsyncDispatchSelectInstanceData(this.conn, selectedClassNames, selectedPredicateNames, -1, -1, true, asyncCallback, failureCallback.bind(this));

                // TODO: don't reset options each time if user changed them
                this.network.setOptions({
                    physics: {
                        enabled: true,
                        solver: "forceAtlas2Based",
                    },
                });
            },

            drawFakeInstanceData : function () {
                var SIZE = 1000;
                var SQRT = Math.floor(Math.sqrt(SIZE));
                var BATCH = 100;

                this.network.body.data.nodes.clear();
                this.network.body.data.edges.clear();

                this.network.setOptions({
                    layout: {
                        hierarchical: false,
                    },
                    physics: {
                        enabled: false,
                    }
                });

                // add a group of nodes
                addFakesToNetwork = function(i0, i1) {
                    var nodeData = [];
                    var edgeData = [];

                    for (var i=i0; i < i1; i++) {

                        var groupName = (i<SQRT) ? "group1" : "group2";

                        nodeData.push({id: "id_"+i, label: "Node_" + i, group: groupName});
                        edgeData.push({from: "id_"+i, to: "id_"+Math.floor(Math.random() * SQRT)});
                    }

                    this.network.body.data.nodes.add(nodeData);
                    this.network.body.data.edges.add(edgeData);
                    this.updateInfo();
                };

                // add nodes a batch at a time...threaded so they render
                var msec = 0;
                for (i=0; i < SIZE; i += BATCH) {
                    var bot = i;
                    var top = Math.min(SIZE-1, i + BATCH - 1);
                    setTimeout(addFakesToNetwork.bind(this, bot, top), msec += 250);
                }

                this.network.setOptions({
                    physics: {
                        enabled: true,
                        solver: "forceAtlas2Based",
                    },
                });
            },

            selectedNodeCallback : function (node) {



            },

            updateInfo : function () {
                this.infospan.innerHTML = "nodes: " + this.network.body.data.nodes.getIds().length + "  predicates: " + this.network.body.data.edges.getIds().length;
            }
        }

		return ExploreTab;            // return the constructor
	}

);
