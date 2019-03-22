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
            'sparqlgraph/js/ontologyinfo',

         	'jquery',

            'visjs/vis.min',

			// shimmed
         	'sparqlgraph/dynatree-1.2.5/jquery.dynatree',
            'sparqlgraph/js/ontologytree',
            'sparqlgraph/js/belmont'

		],

	function(IIDXHelper, ModalIidx, OntologyInfo, $, vis) {


		//============ local object  ExploreTab =============
		var ExploreTab = function(treediv, canvasdiv, buttondiv, searchtxt) {
		    this.treediv = document.createElement("div");
            this.treediv.id = "etTreeDiv";
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
            this.buttonspan = document.createElement("span");
            this.buttonspan.style.marginRight = "3ch";
            this.searchtxt = searchtxt;
            this.oInfo = null;
            this.oTree = null;
            this.network = null;

            this.largeLayoutFlag = false;
            this.largeLayoutParams={};

            this.initDynaTree();
            this.initButtonDiv();
            this.initCanvas();
        };

		ExploreTab.MAX_LAYOUT_ELEMENTS = 100;


		ExploreTab.prototype = {
            setOInfo : function (oInfo) {
                this.oInfo = new OntologyInfo(oInfo.toJson());  // deepCopy
                this.oTree.setOInfo(this.oInfo);
                this.setModeToOntology();
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
                    selectMode: 3,
                    checkbox: true,
                });

                this.oTree = new OntologyTree($(treeSelector).dynatree("getTree"));

            },

            initCanvas : function() {
                // create an array with nodes
                var data = {};
                var options = {
                    configure: {
                        enabled: true,
                        container: this.configdiv,
                        filter: "layout physics",
                    },
                    groups: {
                        useDefaultGroups: true,
                    }
                };
                this.network = new vis.Network(this.canvasdiv, data, options);
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
                td1.appendChild(IIDXHelper.createButton("Left Button1", this.leftButton1.bind(this)));

                // cell 2/3
                var td2 = document.createElement("td");
                tr.appendChild(td2);
                td2.appendChild(this.buttonspan);

                // cell 3/3
                var td3 = document.createElement("td");
                tr.appendChild(td3);

                var div3 = document.createElement("div");
                td3.appendChild(div3);
                td3.align="right";
                var select = IIDXHelper.createSelect("etSelect", ["Ontology", "Instance Data"], ["Ontology"]);
                select.onchange = this.draw.bind(this);
                td3.appendChild(select);
                td3.appendChild(IIDXHelper.createNbspText());


                var but1 = IIDXHelper.createButton("layout", this.butLayout.bind(this));
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

            butLayout : function() {
                this.startLayout();
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

                if (this.getMode() == "Ontology") {
                    this.drawOntology();
                } else {
                    this.drawInstanceData();
                }

                // stop the layout after stopAfterMsec
                if (typeof stopAfterMsec != "undefined") {
                    setTimeout(this.stopLayout.bind(this), stopAfterMsec);
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
                    // namespace members
                    var namespace = this.oInfo.getClass(className).getNamespaceStr();
                    edgeData.push({from: className, to: namespace, label: '', arrows: 'to'});

                    var blackObj = {
                        color:'black',
                        highlight:'black',
                        hover: 'black',
                        inherit: false,
                        opacity:1.0
                    }
                    // subclassof
                    for (var subclassName of this.oInfo.getSubclassNames(className)) {
                        edgeData.push({from: subclassName, to: className, label: 'subClassOf', arrows: 'to', color: blackObj, dashes: true});
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
            },

            drawInstanceData : function () {
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
                addToNetwork = function(i0, i1) {
                    var nodeData = [];
                    var edgeData = [];

                    for (var i=i0; i < i1; i++) {

                        var groupName = (i<SQRT) ? "group1" : "group2";

                        nodeData.push({id: "id_"+i, label: "Node_" + i, group: groupName});
                        edgeData.push({from: "id_"+i, to: "id_"+Math.floor(Math.random() * SQRT)});
                    }

                    this.network.body.data.nodes.add(nodeData);
                    this.network.body.data.edges.add(edgeData);
                };

                // add nodes a batch at a time...threaded so they render
                var msec = 0;
                for (i=0; i < SIZE; i += BATCH) {
                    var bot = i;
                    var top = Math.min(SIZE-1, i + BATCH - 1);
                    setTimeout(addToNetwork.bind(this, bot, top), msec += 250);
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


            leftButton1 : function () {

            },
        }

		return ExploreTab;            // return the constructor
	}

);
