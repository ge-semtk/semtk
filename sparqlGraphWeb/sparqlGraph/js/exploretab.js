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
            'sparqlgraph/js/msiclientstatus',
            'sparqlgraph/js/msiclientresults',
            'sparqlgraph/js/msiresultset',
            'sparqlgraph/js/ontologyinfo',
            'sparqlgraph/js/visjshelper',

         	'jquery',

            'visjs/vis.min',

			// shimmed
         	'sparqlgraph/dynatree-1.2.5/jquery.dynatree',
            'sparqlgraph/js/ontologytree',
            'sparqlgraph/js/belmont'

		],

    // TODO: this isn't leveraging VisJsHelper properly.  Code duplication.
	function(IIDXHelper, ModalIidx, MsiClientNodeGroupExec, MsiClientOntologyInfo, MsiClientStatus, MsiClientResults, MsiResultSet, OntologyInfo, VisJsHelper, $, vis) {


		//============ local object  ExploreTab =============
		var ExploreTab = function(treediv, canvasdiv, buttondiv, topControlForm, oInfoClientURL) {
            this.controlDivParent = treediv;
            this.canvasDivParent = canvasdiv;

            this.botomButtonDiv = buttondiv;
            this.topControlForm = topControlForm;
            this.oInfoClientURL = oInfoClientURL;

            this.infospan = document.createElement("span");
            this.infospan.style.marginRight = "3ch";
            this.oInfo = null;
            this.conn = null;
            this.oTree = null;

            this.controlDivHash = this.initControlDivHash();
            this.canvasDivHash = this.initCanvasDivHash();
            this.configDivHash = this.initConfigDivHash();
            this.networkHash = this.initNetworkHash();

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
        ExploreTab.MODE_ONTOLOGY = "Ontology";
        ExploreTab.MODE_INSTANCE = "Instance Data";
        ExploreTab.MODE_STATS = "Instance Counts";
        ExploreTab.MODES = [ExploreTab.MODE_ONTOLOGY, ExploreTab.MODE_INSTANCE, ExploreTab.MODE_STATS ];

		ExploreTab.prototype = {

            initControlDivHash : function() {
                this.controlDivHash = {};
                for (var m of ExploreTab.MODES) {
                    this.controlDivHash[m] = document.createElement("div");
                    this.controlDivHash[m].id="exploreTabControl_" + m.replace(" ", "-");
                }
                return this.controlDivHash;
            },

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

            initNetworkHash : function() {
                this.networkHash = {};
                for (var m of ExploreTab.MODES) {
                    this.networkHash[m] = new vis.Network(this.canvasDivHash[m], {}, this.getDefaultOptions(m));
                }
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
                        initiallyActive: false,
                        deleteNode: true,
                        deleteEdge: true,
                    }
                };

                // options special to each mode
                if (mode == ExploreTab.MODE_ONTOLOGY) {
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
                var select = IIDXHelper.createSelect("etSelect", [ExploreTab.MODE_ONTOLOGY, ExploreTab.MODE_INSTANCE, ExploreTab.MODE_STATS], [ExploreTab.MODE_ONTOLOGY]);
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
                var but = IIDXHelper.createButton("Clear", this.clearNetwork.bind(this));
                td.appendChild(but);
            },

            // add controls to empty control divs
            initControlDivs : function() {
                    this.initControlDivInstance();
                    this.initControlDivOntology();
                    this.initControlDivStats();
            },

            initControlDivStats : function() {
                var div = this.controlDivHash[ExploreTab.MODE_STATS];
                div.style.margin="1ch";

                var h = document.createElement("h3");
                div.appendChild(h);
                h.innerHTML = ExploreTab.MODE_STATS;
                div.appendChild(IIDXHelper.buildList(["Number of predicates connecting each exact class.","Color by namespace."]));
            },

            initControlDivOntology : function() {
                var div = this.controlDivHash[ExploreTab.MODE_ONTOLOGY];
                div.style.margin="1ch";
                var h = document.createElement("h3");
                div.appendChild(h);
                h.innerHTML = ExploreTab.MODE_ONTOLOGY;
                div.appendChild(IIDXHelper.buildList(["Show superclass relationships.","Color by namespace."]));
            },

            initControlDivInstance : function() {
                // INSTANCE
                var div = this.controlDivHash[ExploreTab.MODE_INSTANCE];
                div.style.margin="1ch";

                var dom = IIDXHelper.createSearchDiv(this.doSearch, this);

                var but = IIDXHelper.createIconButton("icon-folder-open", this.doExpand.bind(this), undefined, undefined, undefined, "Expand all");
                but.style.marginLeft = "1ch";
                dom.appendChild(but);

                but = IIDXHelper.createIconButton("icon-folder-close", this.doCollapse.bind(this), undefined, undefined, undefined, "Collapse all");
                but.style.marginLeft = "1ch";
                dom.appendChild(but);

                div.appendChild(dom);

                var hform1 = IIDXHelper.buildHorizontalForm(true)
                div.appendChild(hform1);

                var select = IIDXHelper.createSelect("etTreeSelect", [["single",2], ["sub-tree",3]], ["multi"], false, "input-small");
                select.onchange = function() {
                    this.oTree.tree.options.selectMode = parseInt(document.getElementById("etTreeSelect").value);
                }.bind(this);

                hform1.appendChild(document.createTextNode(" select mode:"));
                hform1.appendChild(select);
                this.initDynaTree();
            },

            /*
             * Initialize an empty dynatree into this.controlDivHash[ExploreTab.MODE_INSTANCE]
             */
			initDynaTree : function() {

                this.controlDivParent.innerHtml = "";
                this.controlDivParent.appendChild(this.controlDivHash[ExploreTab.MODE_INSTANCE]);
                var treeSelector = "#" + this.controlDivHash[ExploreTab.MODE_INSTANCE].id;
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
                            // no drag and drop on explore tab
                            return false;
                        }.bind(this),

                        onDragStop: function(node, x, y, z, aa) {

                        }.bind(this)
                    },

                    persist: false,    // true causes a cookie error with large trees
                    selectMode: 2,    // 1 single, 2 multi, 3 multi hierarchical
                    checkbox: true,

                });

                this.oTree = new OntologyTree($(treeSelector).dynatree("getTree"));

            },

            selectedNodeCallback : function (flag, node) {

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
                    this.addInstanceData(workList);
                } else {
                    this.deleteInstanceData(workList);
                }
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

            // get the etSelect value ExploreTab.MODE_ONTOLOGY, or ExploreTab.MODE_INSTANCE
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

                    if (this.getMode() == ExploreTab.MODE_ONTOLOGY) {
                        this.drawOntology();

                    } else if (this.getMode() == ExploreTab.MODE_INSTANCE) {
                        this.oTree.showAll();
                        var workList = this.oTree.getSelectedPropertyPairs();

                        // add selected classes as lists [className]
                        var classList = this.oTree.getSelectedClassNames();
                        for (var c of classList) {
                            workList.push([c]);
                        }
                        this.addInstanceData(workList);

                    } else {  // ExploreTab.MODE_STATS
                        var client = new MsiClientOntologyInfo(this.oInfoClientURL, ModalIidx.alert.bind(this, "Error"));
                        var drawStatsCallback = this.buildStatusResultsCallback(
                            this.drawPredicateStats.bind(this),
                            MsiClientResults.prototype.execGetJsonBlobRes
                        );
                        client.execGetPredicateStats(gConn, drawStatsCallback);
                    }
                    this.networkHash[this.getMode()].fit();

                }
                this.updateInfo();
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

                this.updateInfo();
            },

            drawOntology : function () {
                var nodeData = [];
                var edgeData = [];
                var SHOW_NAMESPACE = false;

                var fontHash = this.setupGroups(this.oInfo.getNamespaceNames(), this.networkHash[ExploreTab.MODE_ONTOLOGY]);

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
                    for (var parentName of this.oInfo.getClass(className).getParentNameStrs()) {
                        edgeData.push({from: className, to: parentName, label: 'subClassOf', arrows: 'to', color: blackObj, dashes: true});
                    }
                }

                // add any left-over data
                this.networkHash[ExploreTab.MODE_ONTOLOGY].body.data.nodes.add(nodeData);
                this.networkHash[ExploreTab.MODE_ONTOLOGY].body.data.edges.add(edgeData);

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

                //console.log("Adding to nodeJs START");
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
                this.updateInfo();
                //console.log("Adding to nodeJs END");

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

                //console.log("1st pass time: " + (performance.now() - START));
                START = performance.now();

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

                //console.log("2nd pass time: " + (performance.now() - START));
                START = performance.now();

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
                //console.log("3rd pass time: " + (performance.now() - START));

                IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
                IIDXHelper.progressBarRemove(this.progressDiv);
                this.busy(false);
                this.updateInfo();
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

                // if (this.network !== null) {
                //    this.network.destroy();
                // }

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
                    //console.log("query time: " + elapsed);

                    this.addToNetwork(tableRes);
                    elapsed = performance.now() - CALL_NOW;
                    //console.log("query & work: " + elapsed);

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

                IIDXHelper.progressBarCreate(this.progressDiv, "progress-info progress-striped active", this.butSetCancelFlag.bind(this));
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
                this.infospan.innerHTML = "nodes: " + this.networkHash[this.getMode()].body.data.nodes.getIds().length + "  predicates: " + this.networkHash[this.getMode()].body.data.edges.getIds().length;
            },

            // Build a callback for single 1-100 job
            // Creates the progressBar, and
            buildStatusResultsCallback : function(successCallback, resultsCall) {

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
                            resultsCall.bind(resultsClient)(resultsSuccessCallback);
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
