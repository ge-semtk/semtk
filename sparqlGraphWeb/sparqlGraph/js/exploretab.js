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
		var ExploreTab = function(treediv, canvasdiv, buttondiv) {
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
            this.infospan = document.createElement("span");
            this.infospan.style.marginRight = "3ch";
            this.oInfo = null;
            this.conn = null;
            this.oTree = null;
            this.network = null;

            this.largeLayoutFlag = false;
            this.largeLayoutParams={};

            this.cancelFlag = false;

            this.initTreeButtonDiv();
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
                this.oTree.selectNodesByURI(this.oTree.nodeGetURI(node), flag);
            },

            initCanvas : function() {
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
                        addNode: false,
                        addEdge: false,
                        editNode: false,
                        editEdge: false,
                        deleteNode: true,
                        deleteEdge: true,
                    }
                };
                this.network = new vis.Network(this.canvasdiv, {}, options);
            },

            // little div sitting on top of the otree
            initTreeButtonDiv : function() {

                var div = this.treebuttondiv;
                div.innerHTML = "";
                div.style.padding="1ch";


                var form = IIDXHelper.createSearchForm(this.doSearch, this);
                div.appendChild(form);

                var formhoriz1 = IIDXHelper.buildHorizontalForm(true);
                div.appendChild(formhoriz1);
                formhoriz1.appendChild(IIDXHelper.createButton("Expand", this.doExpand.bind(this)));
                formhoriz1.appendChild(IIDXHelper.createNbspText());
                formhoriz1.appendChild(IIDXHelper.createButton("Collapse", this.doCollapse.bind(this)));


                var select = IIDXHelper.createSelect("etTreeSelect", [["multi",2], ["heirarchy",3]], ["multi"], false, "input-small");
                select.onchange = function() {
                    this.oTree.tree.options.selectMode = parseInt(document.getElementById("etTreeSelect").value);
                }.bind(this);

                formhoriz1.appendChild(document.createTextNode(" select mode:"));
                formhoriz1.appendChild(select);

                var hr = document.createElement("hr");
                hr.style.marginTop="4px";
                hr.style.marginBottom="4px";
                div.appendChild(hr);
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

                // cell 1/3
                var td1 = document.createElement("td");
                tr.appendChild(td1);

                // network... button
                td1.appendChild(IIDXHelper.createButton("network...", function() {$(this.configdiv).dialog("open")}.bind(this)));

                // redraw button
                td1.appendChild(IIDXHelper.createNbspText());
                td1.appendChild(IIDXHelper.createButton("redraw", this.drawCanvas.bind(this)));

                // add button
                td1.appendChild(IIDXHelper.createNbspText());
                td1.appendChild(IIDXHelper.createButton("add", this.addInstanceData.bind(this)));

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
                    this.addInstanceData();
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
                var nodetitle = function (uri, classname) {
                    return "<strong>" + local(classname) + "</strong><br>" + local(uri);
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
                var edgeList = [];

                console.log("Adding to nodeJs START");
                for (var i=0; i < rows.length; i++) {
                    // read a row describing a triple
                    var s = rows[i][0];
                    var s_class = (rows[i][1] == "") ? "data" : rows[i][1];

                    nodeList.push({id: s, label: nodelabel(s, s_class), title: nodetitle(s, s_class), group: s_class});

                    // if this row also has predicate and object
                    if (rows[i].length > 2) {
                        var p = rows[i][2];
                        var o = rows[i][3];
                        var o_class = (rows[i][4] == "") ? "data" : rows[i][4];

                        nodeList.push({id: o, label: nodelabel(o, o_class), title: nodetitle(o, o_class), group: o_class});

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

            clearNetwork : function() {
                this.network.body.data.nodes.clear();
                this.network.body.data.edges.clear();
            },

            // query and add instance data based on the ontologyTree
            addInstanceData : function () {
                var LIMIT = 1000;
                var OFFSET = 0;
                var CALL_NOW = 0;

                this.cancelFlag = false;

                this.updateInfo();

                this.network.setOptions({
                    layout: {
                        hierarchical: false,
                    },
                    physics: {
                        enabled: true,
                        solver: "barnesHut",
                    }
                });

                // get list with items either class name or [domain, prop]
                var workList = this.oTree.getSelectedPropertyPairs()
                                    .concat(this.oTree.getSelectedClassNames());

                if (workList.length == 0) {
                    workList = this.oInfo.getPropertyPairs()
                                        .concat(this.oInfo.getClassNames());
                }
                var workIndex = 0;

                var client = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url, g.shortTimeoutMsec);

                var failureCallback = function(messageHTML) {
                    ModalIidx.alert("Instance data property retrieval", messageHTML);
                    IIDXHelper.progressBarRemove(this.progressDiv);
                    this.cancelFlag = true;
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

                        if (Array.isArray(workList[workIndex])) {
                            client.execAsyncDispatchSelectInstanceDataPredicates(this.conn, [workList[workIndex]], LIMIT, 0, false, asyncCallback0, failureCallback.bind(this));
                        } else {
                            client.execAsyncDispatchSelectInstanceDataSubjects(this.conn, [workList[workIndex]], LIMIT, 0, false, asyncCallback0, failureCallback.bind(this));
                        }

                    } else {
                        // done
                        // handle predicates
                        IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
                        setTimeout(IIDXHelper.progressBarRemove.bind(IIDXHelper, this.progressDiv), 1000);
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

                if (Array.isArray(workList[workIndex])) {
                    client.execAsyncDispatchSelectInstanceDataPredicates(this.conn, [workList[workIndex]], LIMIT, 0, false, asyncCallback, failureCallback.bind(this));
                } else {
                    client.execAsyncDispatchSelectInstanceDataSubjects(this.conn, [workList[workIndex]], LIMIT, 0, false, asyncCallback, failureCallback.bind(this));
                }


            },
            // query and add instance data based on the ontologyTree
            addInstanceDataPREVIOUS : function () {
                var LIMIT = 1000;
                var CALL_NOW = 0;

                this.cancelFlag = false;

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
                    var elapsed = performance.now() - CALL_NOW;
                    console.log("query: " + elapsed);
                    if (total < 0) {
                        // first call: grab the count
                        IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
                        IIDXHelper.progressBarRemove(this.progressDiv);
                        total = tableRes.getRsData(0,0);

                        // pick some almost arbitrary warning colors for large queries
                        var classStr = "progress-striped active";
                        if (total < 10000) {
                            classStr += "progress progress-success progress-striped active";
                        } else if (total < 15000){
                            classStr += "progress progress-warning progress-striped active";
                        } else {
                            classStr += "progress progress-danger progress-striped active";
                        }
                        IIDXHelper.progressBarCreate(this.progressDiv, classStr);
                        IIDXHelper.progressBarSetPercent(this.progressDiv, 0, "Querying instance data");

                    } else {
                        // other calls: add data
                        this.addToNetwork(tableRes);
                        offset += tableRes.getRowCount();
                        IIDXHelper.progressBarSetPercent(this.progressDiv, 100 * offset / total, "Querying instance data");
                    }
                    elapsed = performance.now() - CALL_NOW;
                    console.log("query & work: " + elapsed);

                    // decide whether to make more queries
                    if (offset < total && ! this.cancelFlag) {
                        var asyncCallback1 = MsiClientNodeGroupExec.buildFullJsonCallback(
                                                                                             instanceDataCallback.bind(this, total, offset),
                                                                                             failureCallback.bind(this),
                                                                                             function(){},
                                                                                             checkForCancelCallback.bind(this),
                                                                                             g.service.status.url,
                                                                                             g.service.results.url);


                        CALL_NOW = performance.now();
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
                CALL_NOW = performance.now();
                client.execAsyncDispatchSelectInstanceData(this.conn, selectedClassNames, selectedPredicateNames, -1, -1, true, asyncCallback, failureCallback.bind(this));

                // TODO: this currently resets with every new queryText
                //       should somehow save user's preferences
                this.network.setOptions({
                    physics: {
                        enabled: true,
                        solver: "barnesHut",
                    },
                });
            },

            updateInfo : function () {
                this.infospan.innerHTML = "nodes: " + this.network.body.data.nodes.getIds().length + "  predicates: " + this.network.body.data.edges.getIds().length;
            }
        }

		return ExploreTab;            // return the constructor
	}

);
