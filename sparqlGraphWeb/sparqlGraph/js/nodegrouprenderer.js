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

         	'jquery',

            'visjs/vis.min',

			// shimmed
            'sparqlgraph/js/belmont'

		],

	function(VisJsHelper, $, vis) {


		//============ local object  ExploreTab =============
		var NodegroupRenderer = function(canvasdiv) {
            this.ctx = document.createElement("canvas").getContext("2d");

            this.nodegroup = null;

            this.propEditorCallback = null;
            this.snodeEditorCallback = null;  //done
            this.snodeRemoverCallback = null;
            this.linkBuilderCallback = null;
            this.linkEditorCallback = null;

            this.canvasdiv = document.createElement("div");
            this.canvasdiv.style.margin="1ch";
            this.canvasdiv.id="ExploreTab.canvasdiv_" + Math.floor(Math.random() * 10000).toString();
            this.canvasdiv.style.height="100%";
            this.canvasdiv.style.width="100%";
            canvasdiv.appendChild(this.canvasdiv);



            this.network = new vis.Network(this.canvasdiv, {}, NodegroupRenderer.getDefaultOptions(this.configdiv));
            this.network.on('click', this.click.bind(this));

            this.nodeCallbackData = {};
        };

        NodegroupRenderer.SIZE = 12;
        NodegroupRenderer.VSPACE = 4;
        NodegroupRenderer.STROKE = 2;
        NodegroupRenderer.INDENT = 6;

        NodegroupRenderer.COLOR_NODE = "#F4F6F6";
        NodegroupRenderer.COLOR_FOREGROUND = 'black';
        NodegroupRenderer.COLOR_BACKGROUND = 'white';
        NodegroupRenderer.COLOR_RETURNED = 'red';
        NodegroupRenderer.COLOR_CONSTRAINED = 'green';
        NodegroupRenderer.COLOR_RET_CONST = 'blue';
        NodegroupRenderer.INDENT = 6;
        NodegroupRenderer.INDENT = 6;

        NodegroupRenderer.getDefaultOptions = function(configdiv) {
            return {
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
                    var xp = (e.pointer.canvas.x - n.shape.left) / n.shape.width;
                    var yp = (e.pointer.canvas.y - n.shape.top) / n.shape.height;

                    var ndCallbackData = this.nodeCallbackData[n.id];
                    var itemData = ndCallbackData[0];
                    for (var i = 1; i < ndCallbackData.length; i++) {
                        if (yp < ndCallbackData[i].y_perc) {
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
                        this.updateNodeSVG(snode);
                        changedIDs.push(id);
                    }
                }

                // new nodes
                var newIDs = [];
                for (var id of nodegroupIDs) {
                    if (graphIDs.indexOf(id) == -1) {
                        var snode = this.nodegroup.getNodeBySparqlID(id);
                        this.updateNodeSVG(snode);
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
                var notYetUpdatedIDs = this.network.body.data.edges.getIds();
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
                                edge.font = {color: NodegroupRenderer.COLOR_FOREGROUND, background: NodegroupRenderer.COLOR_BACKGROUND};
                            }
                            edgeData.push(edge);
                            notYetUpdatedIDs.splice(notYetUpdatedIDs.indexOf(id));
                        }
                    }
                }
                this.network.body.data.edges.update(edgeData);

                // remove any edges no longer in the nodegroup
                this.network.body.data.edges.remove(notYetUpdatedIDs);
            },
            //
            // Change an snode to a network node and call nodes.update
            // (adding or replacing the existing node)
            //
            // Also pushes info to this.callbackData, which is used for callbacks
            //
            updateNodeSVG : function(snode) {
                var y = 0;
                var hwd;
                var maxWidth = 0;
                var svg = document.createElement("svg");
                var myCallbackData = [];

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
                rect.setAttribute('stroke', "black");
                svg.appendChild(rect);
                y += NodegroupRenderer.STROKE;

                // add header
                var line = document.createElement("line");
                line.setAttribute('x1',"0%");
                line.setAttribute('y1', y + NodegroupRenderer.SIZE);
                line.setAttribute('x2',"100%");
                line.setAttribute('y2', y + NodegroupRenderer.SIZE);
                line.setAttribute('stroke-width',NodegroupRenderer.STROKE);
                line.setAttribute('stroke',"black");
                svg.appendChild(line);
                y += NodegroupRenderer.SIZE + NodegroupRenderer.STROKE;
                myCallbackData.push({ y: (y + NodegroupRenderer.SIZE)/2, type: "header", value: ""});

                // add the sparqlID
                hwd = this.appendSparqlID(svg, snode, y);
                y += hwd.height;
                maxWidth = hwd.width > maxWidth ? hwd.width : maxWidth;
                myCallbackData.push(hwd.data);

                // add property items
                for (var p of snode.getPropList()) {
                    hwd = this.appendProperty(svg, p, y);
                    y += hwd.height;
                    maxWidth = hwd.width > maxWidth ? hwd.width : maxWidth;
                    myCallbackData.push(hwd.data);
                }
                // add node items
                for (var n of snode.getNodeList()) {
                    hwd = this.appendProperty(svg, n, y);
                    y += hwd.height;
                    maxWidth = hwd.width > maxWidth ? hwd.width : maxWidth;
                    myCallbackData.push(hwd.data);
                }

                // set height and width of svg
                y += NodegroupRenderer.VSPACE;
                svg.setAttribute("height", y);
                svg.setAttribute("width", maxWidth + NodegroupRenderer.INDENT)

                // add y_perc to all callback data: percentage of y at bottom of item
                for (var i of myCallbackData) {
                    i.y_perc = i.y / y;
                }
                this.nodeCallbackData[snode.getSparqlID()] = myCallbackData;

                // build the svg image
                var im = "data:image/svg+xml;charset=utf-8," + encodeURIComponent(svg.outerHTML);

                // take a swag at node size based on height
                var visjsNodeSize = y / 4;

                this.network.body.data.nodes.update([{id: snode.getSparqlID(), image: im, shape: "image", size: visjsNodeSize }]);
            },

            appendSparqlID : function(svg, snode, y) {

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
                this.addCheckBox(svg, x, bot, size, checked, foreground );

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

            appendProperty : function(svg, item, y, data) {
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
                this.addCheckBox(svg, x, bot, size, checked, foreground);

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
            addCheckBox : function(svg, x, y, size, checked, foreground) {
                // margin twice as big on top and right
                var m = Math.floor(size/7);
                var s = size - m - m - m;
                var top = y - size + m + m;
                var left = x + m;
                var rect = document.createElement('rect');
                rect.setAttribute('x', left);
                rect.setAttribute('y', top);
                rect.setAttribute('width', s + "px");
                rect.setAttribute('height', s + "px");
                rect.setAttribute('fill', NodegroupRenderer.COLOR_NODE);
                rect.setAttribute('stroke', foreground);
                svg.appendChild(rect);

                if (checked) {
                    var x1 = left;
                    var x2 = left + s;
                    var y1 = top;
                    var y2 = top + s;
                    this.addLine(svg, x1,y1,x2,y2, 1,foreground);
                    this.addLine(svg, x1,y2,x2,y1, 1,foreground);
                }
            },

            // add a line
            addLine : function(svg, x1, y1, x2, y2, strokeWidth, strokeColor) {
                var line = document.createElement("line");
                line.setAttribute('x1',x1);
                line.setAttribute('y1', y1);
                line.setAttribute('x2',x2);
                line.setAttribute('y2', y2);
                line.setAttribute('stroke-width', strokeWidth);
                line.setAttribute('stroke', strokeColor);
                svg.appendChild(line);
            },

            measureTextWidth : function (textElem) {
                var f = textElem.getAttribute("font-size") + " " + textElem.getAttribute("font-family");
                this.ctx.font = f;
                return this.ctx.measureText(textElem).width;
            }
        }

		return NodegroupRenderer;            // return the constructor
	}

);
