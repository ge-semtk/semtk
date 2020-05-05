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
            this.canvasdiv = canvasdiv;
            this.propEditorCallback = null;
            this.snodeEditorCallback = null;
            this.snodeRemoverCallback = null;
            this.linkBuilderCallback = null;
            this.linkEditorCallback = null;

            var options = {
                physics: { stabilization: false },
                edges: { smooth: false }
            };
            var click = function(e) {
                if (e.nodes.length > 0) {
                    var n = this.network.body.nodes[e.nodes[0]];
                    var x = e.pointer.canvas.x - n.x;
                    var y = e.pointer.canvas.y - n.y;
                    console.log("x: " + x );
                    console.log("y: " + y );
                }
                console.log(e);
            }.bind(this);
            this.network = new vis.Network(this.canvasdiv, {}, options);
            this.network.on('click', click);
        };

		NodegroupRenderer.CONSTANT = 100;

		NodegroupRenderer.prototype = {

            setPropEditorCallback : function (callback) {
                this.propEditorCallback = callback;
            },
            setSNodeEditorCallback : function (callback) {
                this.nodeEditorCallback = callback;
            },
            setSNodeRemoverCallback : function (callback) {
                this.nodeRemoverCallback = callback;
            },
            setLinkBuilderCallback : function (callback) {
                this.linkBuilderCallback = callback;
            },
            setLinkEditorCallback : function (callback) {
                this.linkEditorCallback = callback;
            },

            draw : function (nodegroup) {
                var nodes = null;
                var edges = null;

                var DIR = "img/refresh-cl/";
                var LENGTH_MAIN = 150;
                var LENGTH_SUB = 50;

                var sample = `
                    <svg height="60" width="200" style="border: 1px solid black;">
                    <text x="2" y="15" fill="red" font-size="15px" font-family="Arial">I one SVG</text>
                    <text x="2" y="30" fill="red" font-size="15px" font-weight="Bold">I two SVG</text>
                    <text x="2" y="45" fill="red" font-size="15px" font-style="italic">I three SVG</text>
                    <text x="2" y="60" fill="red" font-size="15px">I four SVG</text>
                    <text x="2" y="75" fill="red" font-size="15px">I five SVG</text>
                    Sorry, your browser does not support inline SVG.
                    </svg>`;

                // https://www.w3schools.com/graphics/tryit.asp?filename=trysvg_text2
                var liveSample = `
                <!DOCTYPE html>
                <html>
                <body>

                <svg xmlns="http://www.w3.org/2000/svg" width="600" height="250">
                  <rect x="0" y="0" width="100%" height="100%" fill="#F4F6F6" stroke-width="30" stroke="black"/>
                  <line x1="0%" y1="50" x2="100%" y2="50"width="100%"  stroke-width="5" stroke="black"/>

                  <rect x="30" y="60" width="10px" height="10px" stroke="black" fill="#F4F6F6"/>
                  <text x="30" y="70" font-size="128px" fill="black">x hello world</text>
                  <text x="30" y="90" font-size="72px" fill="black">hello world</text>
                 </svg>

                </body>
                </html>
                `;

                var svgStr =
                  '<svg xmlns="http://www.w3.org/2000/svg" height="200" width="600">' +
                  '<rect x="0" y="0" width="100%" height="100%" fill="#7890A7" stroke-width="20" stroke="#ffffff" ></rect>' +
                  '<foreignObject x="15" y="10" width="100%" height="100%">' +
                  '<div xmlns="http://www.w3.org/1999/xhtml" style="font-size:40px">' +
                  " <em>I</em> am" +
                  '<span style="color:white; text-shadow:0 0 20px #000000;">' +
                  " HTML in SVG!</span><br></br>hi" +
                  "</div>" +
                  "</foreignObject>" +
                  "</svg>";

                svgStr = `
                  <svg xmlns="http://www.w3.org/2000/svg" height="60" width="200" style="border: 1px solid black;">
                  <text x="2" y="15" fill="red" font-size="15px" font-family="Arial">I one SVG</text>
                  <text x="2" y="30" fill="red" font-size="15px" font-weight="Bold">I two SVG</text>
                  <text x="2" y="45" fill="red" font-size="15px" font-style="italic">I three SVG</text>
                  <text x="2" y="60" fill="red" font-size="15px">I four SVG</text>
                  <text x="2" y="75" fill="red" font-size="15px">I five SVG</text>
                  Sorry, your browser does not support inline SVG.
                  </svg>
                  `;
                var svg = document.createElement("svg");
                svg.setAttribute('xmlns', "http://www.w3.org/2000/svg");
                svg.setAttribute('width', '600');
                svg.setAttribute('height', '250');

                var rect = document.createElement("rect");
                rect.setAttribute('x', "0");
                rect.setAttribute('y', "0");
                rect.setAttribute('width', "100%");
                rect.setAttribute('height', "100%");
                rect.setAttribute('fill', "#F4F6F6");
                rect.setAttribute('stroke-width', "30");
                rect.setAttribute('stroke', "black");
                svg.appendChild(rect);

                var text;
                text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                text.setAttribute('x', 30);
                text.setAttribute('y', 102);
                text.setAttribute('font-size', "128px");
                text.setAttribute('font-family', "Arial");
                text.setAttribute('fill', "black");
                text.innerHTML="&nbsp &nbsp;  ?Element";
                svg.appendChild(text);

                text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                text.setAttribute('x', 30);
                text.setAttribute('y', 102);
                text.setAttribute('font-size', "128px");
                text.setAttribute('font-family', "Arial");
                text.setAttribute('fill', "black");
                text.innerHTML="&nbsp &nbsp;  ?Element";
                svg.appendChild(text);

                var url1 = "data:image/svg+xml;charset=utf-8," + encodeURIComponent(svg.outerHTML);
                console.log(svg.outerHTML);
                var url2 = "data:image/svg+xml;charset=utf-8," + encodeURIComponent(svgStr);
                // Create a data table with nodes.
                nodes = [];

                // Create a data table with links.
				edges = [];

                var n = nodegroup.getNode(0);
                var im = buildNodeImage(n);
                nodes.push({ id: n.getSparqlID(), image: im, shape: "image"});
				nodes.push({ id: 1, label: "Get HTML", image: url1, shape: "image" });
				nodes.push({ id: 2, label: "Using SVG", image: url2, shape: "image" });
				edges.push({ from: 1, to: 2, length: 300 });

				// create a network
				var data = {
                    nodes: nodes,
                    edges: edges
				};
				this.network.setData(data);
            },

            buildNodeImage(node) {

                //var bbox = textElement.getBBox();
                //var width = bbox.width;
                //var height = bbox.height;

                // get name
                var sparqlID = node.getSparqlID();

                // get property items
                var propList = node.getPropList();
                for (var p of propList) {

                }
                // get node items
                var nodeList = node.getNodeList();
                for (var n of nodeList) {

                }

                // find longest string to calculate width
                // draw box
                // draw header
                // draw sparqlID
                // draw property items
                // draw node items
                // can we figure out where the click happened?
            },

            last : function () {
            }
        }

		return NodegroupRenderer;            // return the constructor
	}

);
