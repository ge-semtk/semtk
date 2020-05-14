/**
 ** Copyright 2020 General Electric Company
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

//  Docs
//  https://ww3.arb.ca.gov/ei/tools/lib/vis/docs/network.html

define([	// properly require.config'ed

            'sparqlgraph/js/visjshelper',
            'sparqlgraph/js/modaliidx',
         	'jquery',

            'visjs/vis.min',

			// shimmed
            'sparqlgraph/js/belmont'

		],

	function(VisJsHelper, ModalIidx, $, vis) {


		//============ local object  ExploreTab =============
		var NodegroupRenderer = function(canvasdiv) {
            this.ctx = document.createElement("canvas").getContext("2d");

            this.nodegroup = null;

            this.propEditorCallback = null;
            this.snodeEditorCallback = null;  //done
            this.snodeRemoverCallback = null;
            this.linkBuilderCallback = null;
            this.linkEditorCallback = null;

            this.configdiv = document.createElement("div");
            this.configdiv.style.margin="1ch";
            this.configdiv.id="ngrConfigDiv";
            this.configdiv.style.display="table";
            this.configdiv.style.background = "rgba(32, 16, 16, 0.2)";

            this.canvasdiv = document.createElement("div");
            this.canvasdiv.style.margin="1ch";
            this.canvasdiv.id="ExploreTab.canvasdiv_" + Math.floor(Math.random() * 10000).toString();
            this.canvasdiv.style.height="100%";
            this.canvasdiv.style.width="100%";
            canvasdiv.appendChild(this.canvasdiv);

            this.network = new vis.Network(this.canvasdiv, {}, NodegroupRenderer.getDefaultOptions(this.configdiv));
            this.network.on('click', this.click.bind(this));

            // data for click(), sorted by Y ascending
            this.nodeCallbackData = {};
        };

        NodegroupRenderer.SIZE = 12;
        NodegroupRenderer.VSPACE = 4;
        NodegroupRenderer.STROKE = 2;
        NodegroupRenderer.INDENT = 6;

        NodegroupRenderer.COLOR_NODE = "#f7f8fa";
        NodegroupRenderer.COLOR_GRAB_BAR = '#e1e2e5';
        NodegroupRenderer.COLOR_FOREGROUND = 'black';
        NodegroupRenderer.COLOR_CANVAS = 'white';
        NodegroupRenderer.COLOR_RETURNED = '#cf1e10';
        NodegroupRenderer.COLOR_CONSTRAINED = '#3b73b9';
        NodegroupRenderer.COLOR_RET_CONST = '#3ca17a';
        NodegroupRenderer.INDENT = 6;

        NodegroupRenderer.getDefaultOptions = function(configdiv) {
            return {
                interaction: {
                    navigationButtons: true,
                    keyboard: true
                },
                configure: {
                    enabled: true,
                    container: configdiv,
                    filter: "layout physics",
                    showButton: true
                },
                physics: {
                    barnesHut: {
                      gravitationalConstant: -2450,
                      springLength: 110,
                      avoidOverlap: 0.01
                    },
                    minVelocity: 0.75
                },
                edges: {
                    arrows: {
                        to: {
                            enabled: true,
                            scaleFactor: 0.3
                        }
                    },
                    color: {
                        color: NodegroupRenderer.COLOR_FOREGROUND,
                        hover: 'blue'
                    },

                    font: {
                        color: NodegroupRenderer.COLOR_FOREGROUND,
                        size: 10,
                        face: 'arial',
                        multi: 'html',  // allows <b> in label
                    },
                    width: 1
                }
            };
        };

		NodegroupRenderer.prototype = {

            setPropEditorCallback : function (callback) {
                this.propEditorCallback = callback;
            },
            setSNodeEditorCallback : function (callback) {
                this.snodeEditorCallback = callback;
            },
            setSNodeRemoverCallback : function (callback) {
                this.snodeRemoverCallback = callback;
            },
            setLinkBuilderCallback : function (callback) {
                this.linkBuilderCallback = callback;
            },
            setLinkEditorCallback : function (callback) {
                this.linkEditorCallback = callback;
            },

            click : function(e) {
                if (e.nodes.length > 0) {
                    var n = this.network.body.nodes[e.nodes[0]];
                    var x = e.pointer.canvas.x - n.shape.left;
                    var y = e.pointer.canvas.y - n.shape.top
                    var x_perc = x / n.shape.width;
                    var y_perc = y / n.shape.height;

                    var ndCallbackData = this.nodeCallbackData[n.id];
                    var itemData = ndCallbackData[0];
                    for (var i = 1; i < ndCallbackData.length; i++) {
                        if (y_perc < ndCallbackData[i].y_perc) {
                            break;
                        } else {
                            itemData = ndCallbackData[i];
                        }
                    }
                    var snode = this.nodegroup.getNodeBySparqlID(n.id);
                    if (itemData.type == "SemanticNode") {
                        this.snodeEditorCallback(snode);
                    } else if (itemData.type == "PropertyItem") {
                        this.propEditorCallback(snode.getPropertyByKeyname(itemData.value),
                                                n.id);
                    }  else if (itemData.type == "NodeItem") {
                        this.linkBuilderCallback(snode,
                                                snode.getNodeItemByKeyname(itemData.value));
                    } else if (itemData.type = "header") {
                        if (x_perc > itemData.x_close_perc) {
                            this.snodeRemoverCallback(snode);
                        } else if (x_perc > itemData.x_expand_perc) {
                            // toggle the grabBar (position[0]) expandFlag and redraw
                            this.setExpandFlag(snode, !this.getExpandFlag(snode));
                            this.buildAndUpdateNodeSVG(snode);
                        }
                    }
                } else if (e.edges.length > 0) {
                    var edge = this.network.body.edges[e.edges[0]];
                    var snode = this.nodegroup.getNodeBySparqlID(edge.fromId);
                    var nItem = snode.getNodeItemByKeyname(edge.options.label.replace(/[\W]$/, ""));
                    var targetSNode = this.nodegroup.getNodeBySparqlID(edge.toId);
                    this.linkEditorCallback(snode, nItem, targetSNode);
                }
                console.log(e);
            },

            // redraw as if it had been dragged in fresh
            // default behavior is to collapse any unused nodes
            drawCollapsingUnused : function() {
                for (var snode of this.nodegroup.getSNodeList()) {
                    var flag = this.calcExpandNeeded(snode);
                    this.setExpandFlag(snode, flag);
                }
                this.draw(this.nodegroup, []);
            },

            // redraw with all nodes expanded
            drawExpandAll : function() {
                for (var snode of this.nodegroup.getSNodeList()) {
                    this.setExpandFlag(snode, true);
                }

                this.draw(this.nodegroup, []);
            },

            // Update the display to reflect the nodegroup
            // unchangedIDs - nodes who don't need their images regenerated
            draw : function (nodegroup, unchangedIDs) {

                this.nodegroup = nodegroup;  // mostly for callbacks

                this.drawNodes(unchangedIDs);
                this.drawEdges();

            },

            drawNodes : function(unchangedIDs) {
                var nodegroupIDs = this.nodegroup.getSNodeSparqlIDs().slice();
                var graphIDs = this.network.body.data.nodes.getIds();

                // changed nodes
                var changedIDs = [];
                for (var id of graphIDs) {
                    if (nodegroupIDs.indexOf(id) > -1 && unchangedIDs.indexOf(id) == -1) {
                        var snode = this.nodegroup.getNodeBySparqlID(id);
                        this.buildAndUpdateNodeSVG(snode);
                        changedIDs.push(id);
                    }
                }

                // new nodes
                var newIDs = [];
                for (var id of nodegroupIDs) {
                    if (graphIDs.indexOf(id) == -1) {
                        var snode = this.nodegroup.getNodeBySparqlID(id);
                        this.buildAndUpdateNodeSVG(snode);
                        newIDs.push(id);
                    }
                }

                // deleted
                var deletedIDs = [];
                for (var id of graphIDs) {
                    if (nodegroupIDs.indexOf(id) == -1) {
                        deletedIDs.push(id);
                        delete this.nodeCallbackData["id"]
                    }
                }
                this.network.body.data.nodes.remove(deletedIDs);
            },

            drawEdges : function() {
                // edges: update all since it is cheap

                var edgeIDsToRemove = this.network.body.data.edges.getIds();
                var edgeData = [];
                for (var snode of this.nodegroup.getSNodeList()) {
                    for (var nItem of snode.getNodeList()) {
                        for (var snode2 of nItem.getSNodes()) {
                            var fromID = snode.getSparqlID();
                            var toID = snode2.getSparqlID();
                            var id = fromID + "-" + toID;
                            var label = nItem.getKeyName() + nItem.getQualifier(snode2);
                            var edge = {
                                id:     id,
                                from:   fromID,
                                to:     toID,
                                label:  label,
                            };
                            if (nItem.getOptionalMinus(snode2) != NodeItem.OPTIONAL_FALSE) {
                                edge.font = {color: 'red', background: 'lightgray'};
                            } else  {
                                edge.font = {color: NodegroupRenderer.COLOR_FOREGROUND, background: NodegroupRenderer.COLOR_CANVAS};
                            }
                            edgeData.push(edge);

                            // remove this edge from edgeIDsToRemove
                            var removeIndex = edgeIDsToRemove.indexOf(id);
                            if (removeIndex > -1) {
                                edgeIDsToRemove.splice(removeIndex, 1);
                            }
                        }
                    }
                }
                // update all the new edge data
                this.network.body.data.edges.update(edgeData);

                // remove any edges no longer in the nodegroup
                this.network.body.data.edges.remove(edgeIDsToRemove);
            },
            //
            // Change an snode to a network node and call nodes.update
            // (adding or replacing the existing node)
            //
            // Also pushes info to this.callbackData, which is used for callbacks
            //
            buildAndUpdateNodeSVG : function(snode) {
                var y = 0;
                var x = 0;
                var hwd;
                var maxWidth = 0;
                var svg = document.createElement("svg");
                var myCallbackData = [];
                var expandFlag = this.getExpandFlag(snode);

                //  build support vector graphic with a default size
                svg.setAttribute('xmlns', "http://www.w3.org/2000/svg");
                svg.setAttribute('width', 200);
                svg.setAttribute('height', 60);

                // fill with a rectangle
                var rect = document.createElement("rect");
                rect.setAttribute('x', "0");
                rect.setAttribute('y', "0");
                rect.setAttribute('width', "100%");
                rect.setAttribute('height', "100%");
                rect.setAttribute('fill', NodegroupRenderer.COLOR_NODE);
                rect.setAttribute('stroke-width', NodegroupRenderer.STROKE);
                rect.setAttribute('stroke', NodegroupRenderer.COLOR_FOREGROUND);
                svg.appendChild(rect);
                y += NodegroupRenderer.STROKE;

                // skip enough room for the grab bar at the end
                y += this.getGrabBarHeight();
                y += NodegroupRenderer.VSPACE;

                // add the sparqlID
                hwd = this.addSparqlID(svg, snode, y);
                y += hwd.height;
                maxWidth = hwd.width > maxWidth ? hwd.width : maxWidth;
                myCallbackData.push(hwd.data);

                if (expandFlag) {
                    // add property items
                    for (var p of snode.getPropList()) {
                        hwd = this.addProperty(svg, p, y);
                        y += hwd.height;
                        maxWidth = hwd.width > maxWidth ? hwd.width : maxWidth;
                        myCallbackData.push(hwd.data);
                    }
                    // add node items
                    for (var n of snode.getNodeList()) {
                        hwd = this.addProperty(svg, n, y);
                        y += hwd.height;
                        maxWidth = hwd.width > maxWidth ? hwd.width : maxWidth;
                        myCallbackData.push(hwd.data);
                    }
                }

                // add a little padding on height and width
                x = maxWidth + NodegroupRenderer.INDENT;
                y += NodegroupRenderer.VSPACE * 2;

                // set height and width of svg
                svg.setAttribute("height", y);
                svg.setAttribute("width", x)

                // now that width is set, add the grab bar (with it's right-justified stuff)
                var callbackData = this.addGrabBar(svg, expandFlag)
                myCallbackData.unshift(callbackData);   // sneak it on the beginning of the list since it it supposed to be sorted by Y ascending

                // add y_perc to all callback data: percentage of y at bottom of item
                for (var i of myCallbackData) {
                    i.y_perc = i.y / y;
                }

                // change grab bar special cases to percent
                myCallbackData[0].x_close_perc = myCallbackData[0].x_close / x;
                myCallbackData[0].x_expand_perc = myCallbackData[0].x_expand / x;

                this.nodeCallbackData[snode.getSparqlID()] = myCallbackData;

                // build the svg image
                var im = "data:image/svg+xml;charset=utf-8," + encodeURIComponent(svg.outerHTML);

                // take a swag at node size based on height
                var visjsNodeSize = y / 4;

                this.network.body.data.nodes.update([{id: snode.getSparqlID(), image: im, shape: "image", size: visjsNodeSize }]);
            },

            calcExpandNeeded : function(snode) {
                return (snode.getReturnedPropertyItems().length + snode.getConstrainedPropertyItems().length) > 0;
            },

            // figure out if node is expanded
            getExpandFlag : function(snode) {

                // if node has been drawn before, retrieve
                if (this.nodeCallbackData.hasOwnProperty(snode.getSparqlID())) {
                    expandFlag = this.nodeCallbackData[snode.getSparqlID()][0].expandFlag;
                } else {
                    // first time draw: always expand
                    expandFlag = true;
                }
                return expandFlag;
            },

            // change the expandFlag of an already-drawn node
            setExpandFlag : function(snode, val) {
                this.nodeCallbackData[snode.getSparqlID()][0].expandFlag = val;
            },

            deleteExpandFlag : function(snode) {
                delete this.nodeCallbackData[snode.getSparqlID()][0].expandFlag;
            },

            // In order to allow items to be right-justified, grab bar must be added last
            // So there are separate calls
            //  1.  get the height to skip at the beginning of the node-building
            //  2.  add the bar at the end (in the empty skipped space)-- after width is set
            getGrabBarHeight : function (svg) {
                return Math.floor(1.5 * NodegroupRenderer.SIZE);
            },

            addGrabBar : function(svg, expandFlag) {

                var height = this.getGrabBarHeight();

                // draw the basic rectangle
                var rect = document.createElement("rect");
                rect.setAttribute('x', "0");
                rect.setAttribute('y', "0");
                rect.setAttribute('width', "100%");
                rect.setAttribute('height', height);
                rect.setAttribute('fill', NodegroupRenderer.COLOR_GRAB_BAR);
                rect.setAttribute('stroke-width', NodegroupRenderer.STROKE);
                rect.setAttribute('stroke', NodegroupRenderer.COLOR_FOREGROUND);
                svg.appendChild(rect);

                var width = svg.getAttribute("width");
                var callbackData = {};

                // draw X
                var elementTop = height * 0.2;
                var elementSize = height * 0.6;
                var elementBot = elementTop + elementSize;
                var x = width - elementSize * 2;
                this.drawX(svg, x, elementTop, x+elementSize, elementTop+ elementSize, 1,NodegroupRenderer.COLOR_FOREGROUND);
                callbackData.x_close = x ;


                // draw expand / collapse
                x = width - elementSize * 4;
                if (expandFlag) {
                    var y = elementTop + elementSize * .75;
                    this.drawLine(svg, x, y , x+elementSize, y, 1, NodegroupRenderer.COLOR_FOREGROUND);
                } else {
                    var y = elementTop;
                    this.drawBox(svg, x, y, x+elementSize, y+elementSize, NodegroupRenderer.COLOR_GRAB_BAR, NodegroupRenderer.COLOR_FOREGROUND);
                }
                callbackData.x_expand = x;

                // draw mover
                x = NodegroupRenderer.INDENT;
                elementTop = height * 0.2;
                elementSize = height * 1;
                elementBot = elementTop + height * .6 ;
                for (var yy=elementTop; yy <= elementBot; yy += (elementBot - elementTop) / 3) {
                    this.drawLine(svg, x, yy , x+elementSize, yy, 1,NodegroupRenderer.COLOR_FOREGROUND);
                }

                callbackData.y = height;
                callbackData.type = "header";
                callbackData.value="";
                callbackData.expandFlag = expandFlag;

                return(callbackData);
            },

            // add SparqlID line
            // generate a [height, width, data]
            addSparqlID : function(svg, snode, y) {

                var bot = y + NodegroupRenderer.VSPACE + NodegroupRenderer.SIZE;
                var x = NodegroupRenderer.INDENT;
                var size = NodegroupRenderer.SIZE * 1.5;

                var checked = false;
                var foreground = NodegroupRenderer.COLOR_FOREGROUND;

                if (snode.getIsReturned() || snode.getIsTypeReturned()) {
                    checked = true;
                    if (snode.hasConstraints()) {
                        foreground = NodegroupRenderer.COLOR_RET_CONST;
                    } else {
                        foreground = NodegroupRenderer.COLOR_RETURNED;
                    }
                } else if (snode.hasConstraints()) {
                    checked = true;
                    foreground = NodegroupRenderer.COLOR_CONSTRAINED;
                }
                this.drawCheckBox(svg, x, bot, size, checked, foreground );

                var text = document.createElement('text');
                text.setAttribute('x', x + size + NodegroupRenderer.INDENT);
                text.setAttribute('y', bot);
                text.setAttribute('font-size', size + "px");
                text.setAttribute('font-family', "Arial");
                text.setAttribute('fill', foreground);
                text.innerHTML = snode.getSparqlID();
                svg.appendChild(text);

                var height = NodegroupRenderer.VSPACE + size;
                var width = NodegroupRenderer.INDENT + size + NodegroupRenderer.INDENT + this.measureTextWidth(text);
                var callbackData = { y: y, type: snode.getItemType(), value: text.innerHTML };

                return({"height":height, "width":width, "data":callbackData});
            },

            // add property line
            // generate a [height, width, data]
            addProperty : function(svg, item, y, data) {
                var bot = y + NodegroupRenderer.VSPACE + NodegroupRenderer.SIZE;
                var x = NodegroupRenderer.INDENT;
                var size = NodegroupRenderer.SIZE;

                var checked = false;
                var foreground = NodegroupRenderer.COLOR_FOREGROUND;
                if (item.getIsReturned() ) {
                    checked = true;
                    if (item.hasConstraints()) {
                        foreground = NodegroupRenderer.COLOR_RET_CONST;
                    } else {
                        foreground = NodegroupRenderer.COLOR_RETURNED;
                    }
                } else if (item.hasConstraints()) {
                    checked = true;
                    foreground = NodegroupRenderer.COLOR_CONSTRAINED;
                }
                this.drawCheckBox(svg, x, bot, size, checked, foreground);

                var text = document.createElement('text');
                text.setAttribute('x', x + size + NodegroupRenderer.INDENT);
                text.setAttribute('y', bot);
                text.setAttribute('font-size', size + "px");
                text.setAttribute('font-family', "Arial");
                text.setAttribute('fill', foreground);
                text.innerHTML = item.getKeyName() + " : " + item.getValueType();
                svg.appendChild(text);

                var height = NodegroupRenderer.VSPACE + size;
                var width = NodegroupRenderer.INDENT + size + NodegroupRenderer.INDENT + this.measureTextWidth(text);
                var callbackData = { y: y, type: item.getItemType(), value: item.getKeyName() };

                return({"height":height, "width":width, "data":callbackData});
            },

            // Add a checkbox.
            // Black when unchecked, Red when checked
            // x,y is bottom left to match text
            drawCheckBox : function(svg, x, y, size, checked, foreground) {
                // margin twice as big on top and right
                var m = Math.floor(size/7);
                var s = size - m - m - m;
                var top = y - size + m + m;
                var left = x + m;

                this.drawBox(svg, left, top, left + s, top + s, NodegroupRenderer.COLOR_NODE, foreground)

                if (checked) {
                    var x1 = left;
                    var x2 = left + s;
                    var y1 = top;
                    var y2 = top + s;
                    this.drawX(svg, x1, y1, x2, y2, 1, foreground);
                }
            },

            // add a line
            drawBox : function(svg, x1, y1, x2, y2, fillColor, strokeColor) {
                var rect = document.createElement('rect');
                rect.setAttribute('x', x1);
                rect.setAttribute('y', y1);
                rect.setAttribute('width', (x2 - x1) + "px");
                rect.setAttribute('height', (y2 - y1) + "px");
                rect.setAttribute('fill', fillColor);
                rect.setAttribute('stroke', strokeColor);
                svg.appendChild(rect);
            },

            // add a line
            drawLine : function(svg, x1, y1, x2, y2, strokeWidth, strokeColor) {
                var line = document.createElement("line");
                line.setAttribute('x1',x1);
                line.setAttribute('y1', y1);
                line.setAttribute('x2',x2);
                line.setAttribute('y2', y2);
                line.setAttribute('stroke-width', strokeWidth);
                line.setAttribute('stroke', strokeColor);
                svg.appendChild(line);
            },

            drawX : function (svg, x1, y1, x2, y2, strokeWidth, strokeColor) {
                this.drawLine(svg, x1, y1, x2, y2, strokeWidth,strokeColor);
                this.drawLine(svg, x1, y2, x2, y1, strokeWidth,strokeColor);
            },

            measureTextWidth : function (textElem) {
                var f = textElem.getAttribute("font-size") + " " + textElem.getAttribute("font-family");
                this.ctx.font = f;
                return this.ctx.measureText(textElem).width;
            },

            showConfigDialog : function() {

                // hack at getting the UI colors so they don't look terrible
                for (var e of this.configdiv.children) {
                    e.style.backgroundColor='white';
                    for (var ee of e.children) {
                        if (! ee.innerHTML.startsWith("generate")) {
                            ee.style.backgroundColor='white';
                        }
                    }
                }

                var m = new ModalIidx("ModalIidxAlert");
                m.showOK("Network physics", this.configdiv, function(){});
            }
        }

		return NodegroupRenderer;            // return the constructor
	}

);
