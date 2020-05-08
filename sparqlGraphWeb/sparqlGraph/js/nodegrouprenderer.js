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

//  https://cdnjs.cloudflare.com/ajax/libs/vis/4.21.0/vis.min.css

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

            var click = function(e) {
                if (e.nodes.length > 0) {
                    var n = this.network.body.nodes[e.nodes[0]];
                    var xp = (e.pointer.canvas.x - n.shape.left) / n.shape.width;
                    var yp = (e.pointer.canvas.y - n.shape.top) / n.shape.height;

                    var ndImageData = this.nodeImageData[n.id];
                    var itemData = ndImageData[0];
                    for (var i = 1; i < ndImageData.length; i++) {
                        if (yp < ndImageData[i].y_perc) {
                            break;
                        } else {
                            itemData = ndImageData[i];
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
                    var nItem = snode.getNodeItemByKeyname(edge.options.label);
                    var targetSNode = this.nodegroup.getNodeBySparqlID(edge.toId);
                    this.linkEditorCallback(snode, nItem, targetSNode);
                }
                console.log(e);
            }.bind(this);

            this.network = new vis.Network(this.canvasdiv, {}, NodegroupRenderer.getDefaultOptions(this.configdiv));
            this.network.on('click', click);

            this.nodeImageData = {};
        };

        NodegroupRenderer.SIZE = 12;
        NodegroupRenderer.VSPACE = 4;
        NodegroupRenderer.STROKE = 2;
        NodegroupRenderer.INDENT = 6;

        NodegroupRenderer.getDefaultOptions = function(configdiv) {
            return {
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

            // unchangedIDs - nodes who don't need their images regenerated
            draw : function (nodegroup, unchangedIDs) {
                console.log("UNCHANGED: " + unchangedIDs);
                this.nodegroup = nodegroup;
                var nodegroupIDs = nodegroup.getSNodeSparqlIDs().slice();
                var graphIDs = this.network.body.data.nodes.getIds();

                // changed nodes
                var changedIDs = [];
                for (var id of graphIDs) {
                    if (nodegroupIDs.indexOf(id) > -1 && unchangedIDs.indexOf(id) == -1) {
                        var snode = this.nodegroup.getNodeBySparqlID(id);
                        var im = this.buildNodeImage(snode);
                        this.network.body.data.nodes.update([{id: id, image: im, shape: "image"}]);
                        changedIDs.push(id);
                    }
                }

                // new nodes
                var newIDs = [];
                for (var id of nodegroupIDs) {
                    if (graphIDs.indexOf(id) == -1) {
                        var snode = this.nodegroup.getNodeBySparqlID(id);
                        var im = this.buildNodeImage(snode);
                        this.network.body.data.nodes.add([{id: id, image: im, shape: "image"}]);
                        newIDs.push(id);
                    }
                }

                // deleted
                var deletedIDs = [];
                for (var id of graphIDs) {
                    if (nodegroupIDs.indexOf(id) == -1) {
                        deletedIDs.push(id);
                        delete this.nodeImageData["id"]
                    }
                }
                this.network.body.data.nodes.remove(deletedIDs);

                // edges: update all since it is cheap
                var leftoverEdgeIds = this.network.body.data.edges.getIds();
                var edgeData = [];
                for (var snode of nodegroup.getSNodeList()) {
                    for (var nItem of snode.getNodeList()) {
                        for (var snode2 of nItem.getSNodes()) {
                            var fromID = snode.getSparqlID();
                            var toID = snode2.getSparqlID();
                            var id = fromID + "-" + toID;
                            edgeData.push({
                                id:     id,
                                from:   fromID,
                                to:     toID,
                                label:  nItem.getKeyName(),
                            });
                            leftoverEdgeIds.splice(leftoverEdgeIds.indexOf(id));
                        }
                    }
                }
                this.network.body.data.edges.update(edgeData);
                if (leftoverEdgeIds.length) {
                    this.network.body.data.edges.remove(leftoverEdgeIds);
                }
            },


            // build this.imageData and return the node img
            buildNodeImage : function(node) {

                var y = 0;
                var hwd = [];
                var HEIGHT=0;
                var WIDTH=1;
                var DATA=2;
                var maxWidth = 0;
                var svg = document.createElement("svg");
                var imageData = [];


                svg.setAttribute('xmlns', "http://www.w3.org/2000/svg");
                svg.setAttribute('width', '200');
                svg.setAttribute('height', '200');

                var rect = document.createElement("rect");
                rect.setAttribute('x', "0");
                rect.setAttribute('y', "0");
                rect.setAttribute('width', "100%");
                rect.setAttribute('height', "100%");
                rect.setAttribute('fill', "#F4F6F6");
                rect.setAttribute('stroke-width', NodegroupRenderer.STROKE);
                rect.setAttribute('stroke', "black");
                svg.appendChild(rect);
                y += NodegroupRenderer.STROKE;

                var line = document.createElement("line");
                line.setAttribute('x1',"0%");
                line.setAttribute('y1', y + NodegroupRenderer.SIZE);
                line.setAttribute('x2',"100%");
                line.setAttribute('y2', y + NodegroupRenderer.SIZE);
                line.setAttribute('stroke-width',NodegroupRenderer.STROKE);
                line.setAttribute('stroke',"black");
                svg.appendChild(line);
                y += NodegroupRenderer.SIZE + NodegroupRenderer.STROKE;
                imageData.push({ y: (y + NodegroupRenderer.SIZE)/2, type: "header", value: ""});


                hwd = this.appendSparqlID(svg, node, y);
                y += hwd[HEIGHT];
                maxWidth = hwd[WIDTH] > maxWidth ? hwd[WIDTH] : maxWidth;
                imageData.push(hwd[DATA]);

                // get property items
                var propList = node.getPropList();
                for (var p of propList) {
                    hwd = this.appendProperty(svg, p, y);
                    y += hwd[HEIGHT];
                    maxWidth = hwd[WIDTH] > maxWidth ? hwd[WIDTH] : maxWidth;
                    imageData.push(hwd[DATA]);
                }
                // get node items
                var nodeList = node.getNodeList();
                for (var n of nodeList) {
                    hwd = this.appendProperty(svg, n, y);
                    y += hwd[HEIGHT];
                    maxWidth = hwd[WIDTH] > maxWidth ? hwd[WIDTH] : maxWidth;
                    imageData.push(hwd[DATA]);
                }

                // set height and width of svg
                y += NodegroupRenderer.VSPACE;
                //svg.setAttribute("height", y);
                //svg.setAttribute("width", maxWidth + NodegroupRenderer.INDENT)

                // add y_perc to all data
                for (var i of imageData) {
                    i.y_perc = i.y / y;
                }
                this.nodeImageData[node.getSparqlID()] = imageData;

                console.log("=================================\n" + svg.outerHTML + "\n=================================");
                return "data:image/svg+xml;charset=utf-8," + encodeURIComponent(svg.outerHTML);

            },

            appendSparqlID : function(svg, node, y) {

                var bot = y + NodegroupRenderer.VSPACE + NodegroupRenderer.SIZE;
                var x = NodegroupRenderer.INDENT;
                var size = NodegroupRenderer.SIZE * 1.5;

                this.addCheckBox(svg, x, bot, size, node.getIsReturned() || node.getIsTypeReturned() );

                var text = document.createElement('text');
                text.setAttribute('x', x + size + NodegroupRenderer.INDENT);
                text.setAttribute('y', bot);
                text.setAttribute('font-size', size + "px");
                text.setAttribute('font-family', "Arial");
                text.setAttribute('fill', "black");
                text.innerHTML = node.getSparqlID();
                svg.appendChild(text);

                var height = NodegroupRenderer.VSPACE + size;
                var width = NodegroupRenderer.INDENT + size + NodegroupRenderer.INDENT + this.measureTextWidth(text);
                var data = { y: y, type: node.getItemType(), value: text.innerHTML };

                return([height, width, data]);
            },

            appendProperty : function(svg, item, y, data) {
                var bot = y + NodegroupRenderer.VSPACE + NodegroupRenderer.SIZE;
                var x = NodegroupRenderer.INDENT;
                var size = NodegroupRenderer.SIZE;

                this.addCheckBox(svg, x, bot, size, item.getIsReturned());

                var text = document.createElement('text');
                text.setAttribute('x', x + size + NodegroupRenderer.INDENT);
                text.setAttribute('y', bot);
                text.setAttribute('font-size', size + "px");
                text.setAttribute('font-family', "Arial");
                text.setAttribute('fill', "black");
                text.innerHTML = item.getKeyName() + " : " + item.getValueType();
                svg.appendChild(text);

                var height = NodegroupRenderer.VSPACE + size;
                var width = NodegroupRenderer.INDENT + size + NodegroupRenderer.INDENT + this.measureTextWidth(text);
                var data = { y: y, type: item.getItemType(), value: item.getKeyName() };

                return([height, width, data]);
            },

            // Add a checkbox.
            // Black when unchecked, Red when checked
            // x,y is bottom left to match text
            addCheckBox : function(svg, x, y, size, checked) {
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
                rect.setAttribute('fill', "#F4F6F6");
                rect.setAttribute('stroke', checked ? "red" : "black");
                svg.appendChild(rect);

                if (checked) {
                    var x1 = left;
                    var x2 = left + s;
                    var y1 = top;
                    var y2 = top + s;
                    this.addLine(svg, x1,y1,x2,y2, 1,"red");
                    this.addLine(svg, x1,y2,x2,y1, 1,"red");
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
