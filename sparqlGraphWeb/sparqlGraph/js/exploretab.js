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

define([	// properly require.config'ed
         	'sparqlgraph/js/iidxhelper',
            'sparqlgraph/js/modaliidx',
            'sparqlgraph/js/ontologyinfo',

         	'jquery',
            'cytoscape/dist/cytoscape.umd',
            'cytoscape/../node_modules/cytoscape-cola/cytoscape-cola',

			// shimmed
         	'sparqlgraph/dynatree-1.2.5/jquery.dynatree',
            'sparqlgraph/js/ontologytree',
            'sparqlgraph/js/belmont'

		],

	function(IIDXHelper, ModalIidx, OntologyInfo, $, cytoscape, cola) {


		//============ local object  ExploreTab =============
		var ExploreTab = function(treediv, canvasdiv, buttondiv, searchtxt) {
		    this.treediv = treediv;

            this.canvasdiv = document.createElement("div");
            this.canvasdiv.style.margin="1ch";
            this.canvasdiv.id="ExploreTab.canvasdiv_" + Math.floor(Math.random() * 10000).toString();
            this.canvasdiv.style.height="100%";
            this.canvasdiv.style.width="100%";
            canvasdiv.appendChild(this.canvasdiv);

            new ResizeObserver(this.resize.bind(this)).observe(canvasdiv);

            this.cy = null;

            this.buttondiv = buttondiv;
            this.buttonspan = document.createElement("span");
            this.buttonspan.style.marginRight = "3ch";
            this.searchtxt = searchtxt;
            this.oInfo = null;
            this.oTree = null;

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
                        /** This function MUST be defined to enable dragging for the tree.
                         *  Return false to cancel dragging of node.
                         */
                           // console.log("dragging " + gOTree.nodeGetURI(node));
                           // gDragLabel = gOTree.nodeGetURI(node);
                            return true;
                        }.bind(this),

                        onDragStop: function(node, x, y, z, aa) {
                           // console.log("dragging " + gOTree.nodeGetURI(node) + " stopped.");
                           // gDragLabel = null;
                        }.bind(this)
                    },

                    persist: true,
                    selectMode: 3,
                    checkbox: true,
                });

                this.oTree = new OntologyTree($(treeSelector).dynatree("getTree"));

            },

            initCanvas : function() {
                cytoscape.use(cola);
                //cytoscape.use(euler)

                this.cy = cytoscape({
                    container: this.canvasdiv,
                    elements: [ // list of graph elements to start with
                        { // node a
                            data: { id: 'a' }
                        },
                        { // node b
                            data: { id: 'b' }
                        },
                        { // node b
                            data: { id: 'c' }
                        },
                        { // node b
                            data: { id: 'd' }
                        },
                        { // node b
                            data: { id: 'e' }
                        },
                        { // edge ab
                            data: { id: 'ab', source: 'a', target: 'b' }
                        },
                        { // edge ab
                            data: { id: 'ac', source: 'a', target: 'c' }
                        },
                        { // edge ab
                            data: { id: 'ad', source: 'a', target: 'd' }
                        },
                        { // edge ab
                            data: { id: 'de', source: 'd', target: 'e' }
                        }
                    ],
                    style: [ // the stylesheet for the graph
                        {
                            selector: 'node',
                            style: {
                                'background-color': '#666',
                                //'label': 'data(id)'
                            }
                        },
                        {
                            selector: 'edge',
                            style: {
                                'width': 3,
                                'line-color': '#ccc',
                                'target-arrow-color': '#ccc',
                                'target-arrow-shape': 'triangle'
                            }
                        }
                    ],

                    // initial viewport state:
                    zoom: 1,
                    pan: { x: 10, y: 10 },

                    // interaction options:
                    minZoom: 1e-50,
                    maxZoom: 1e50,
                    zoomingEnabled: true,
                    userZoomingEnabled: true,
                    panningEnabled: true,
                    userPanningEnabled: true,
                    boxSelectionEnabled: false,
                    selectionType: 'single',
                    touchTapThreshold: 8,
                    desktopTapThreshold: 4,
                    autolock: false,
                    autoungrabify: false,
                    autounselectify: false,

                    //rendering options:
                    headless: false,
                    styleEnabled: true,
                    hideEdgesOnViewport: false,
                    hideLabelsOnViewport: false,
                    textureOnViewport: false,
                    motionBlur: false,
                    motionBlurOpacity: 0.2,
                    wheelSensitivity: 1,
                    pixelRatio: 'auto'
                });

                this.cy.resize();
                this.cy.fit();
                this.layout();

            },

            resize : function() {
                this.cy.resize();
                this.cy.fit();
            },

            layout : function() {
                if (this.cy.elements().size() > ExploreTab.MAX_LAYOUT_ELEMENTS) {
                    // large layout
                    this.layoutLarge(ExploreTab.MAX_LAYOUT_ELEMENTS);
                } else {

                    // normal layout
                    var layout = this.cy.layout({
                        name: 'cola',
                        animate: true,
                        maxSimulationTime: 3000,
                        ready: this.preLayout.bind(this),
                        stop: this.postLayout.bind(this),
                    });

                    layout.run();
                }

            },

            preLayout : function() {
                this.cy.edges().style({visibility: "hidden"});
            },

            postLayout : function() {
                this.cy.edges().style({visibility: "visible"});
            },

            layoutLarge : function(targetNodes) {
                // https://github.com/cytoscape/cytoscape.js-cola
                this.largeLayoutParams = {
                    name: 'cola',
                    animate: false,
                    fit: false,
                    maxSimulationTime: 3000,
                    ready: function(){console.log("ready");},
                    stop: function() {console.log("stop");},
                };
                this.largeLayoutFlag = true;
                document.getElementById("butStopLayout").disabled = false;
                setStatus("layout..");

                this.largeLayoutIteration(targetNodes);

            },

            sleep : function(ms) {
              return new Promise(resolve => setTimeout(resolve, ms));
            },

            largeLayoutIteration : function(targetNodes) {


                this.cy.center();
                this.cy.fit();
                this.sleep(250);

                if (this.largeLayoutFlag) {
                    console.log("layout iteration");
                    var size = this.cy.elements().size();
                    // get random group of elements approximately targetNodes
                    var eles = this.cy.elements().filter((x)=>Math.random()*size/targetNodes <= 1);
                    this.cy.batch(
                        function(eles){
                            eles.style({'background-color': '#600'});
                        }.bind(this, eles)
                    );
                    eles.layout(this.largeLayoutParams).run();
                }

            },

            largeLayoutStop : function() {
                this.largeLayoutFlag = false;
                document.getElementById("butStopLayout").disabled = true;
                setStatus("");
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
                td3.appendChild(IIDXHelper.createBoldText("Download: "));

                var but1 = IIDXHelper.createButton("layout", this.butLayout.bind(this));
                td3.appendChild(but1);
                var but2 = IIDXHelper.createButton("stop layout", this.butStopLayout.bind(this));
                but2.id = "butStopLayout";
                but2.disabled = true;

                td3.appendChild(IIDXHelper.createNbspText());
                td3.appendChild(but2);
                var but3 = IIDXHelper.createButton("rightButton3", this.butRight3.bind(this));
                td3.appendChild(IIDXHelper.createNbspText());
                td3.appendChild(but3);
            },

            butLayout : function() {
                this.layout();;
            },

            butStopLayout : function() {
                this.largeLayoutStop();
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

            draw : function () {
                this.oTree.showAll();
                this.cy.resize();
                this.cy.fit();
            },

            selectedNodeCallback : function (node) {
                // add a node
                var first = Math.floor(Math.random() * 100000).toString();
                this.addRandomNode(first, 'a');

                // add a hundred nodes attached to first
                for (var i=0; i < 100; i++) {
                    var target = Math.floor(Math.random() * 100000).toString();
                    this.addRandomNode(target, first);

                    // add 0 to 10 attached to node i
                    for (var j=Math.floor(Math.random() * 10); j >0; j--) {
                        var id = Math.floor(Math.random() * 100000).toString();
                        this.addRandomNode(id, target);
                    }
                }
                console.log(this.cy.nodes().length.toString() + " nodes");
                console.log(this.cy.elements().length.toString() + " elements");


            },
            addRandomNode : function (id, target) {

                this.cy.add([
                  { group: 'nodes', data: { id: id }, position: { x: 100, y: 100 } }
                ]);
                // link to target
                if (typeof target !== 'undefined') {
                    this.cy.add([
                      { group: 'edges', data: { id: id + "_" + target, source: id, target: target } }
                    ]);
                }
                return id;
            },

            leftButton1 : function () {

            },
        }

		return ExploreTab;            // return the constructor
	}

);
