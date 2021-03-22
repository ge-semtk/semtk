/**
 ** Copyright 2021 General Electric Company
 **
 ** Authors:  Paul Cuddihy, Jenny Williams
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

define([	// properly require.config'ed   bootstrap-modal
            'sparqlgraph/js/plotlyplotter'
            //                       OR should we presume the internet is available?  and pull from there?
			// shimmed

		],

    /*
     *   Handles the entire sparqlgraphjson.plots JSON
     */
    function(PlotlyPlotter) {

        var PlotsHandler = function (plotsJson) {
            this.plots = plotsJson;
        };


        PlotsHandler.prototype = {
            CONSTANT : 1,

            getNumPlots : function () {
                return this.plots.length;
            },

            /*
             * return name = or throw exception
             */
            getName : function(index) {
                var json = this.plots[index];
                if (! json.hasOwnProperty("name")) {
                    throw "Plots json item has no name";
                }
                return json.name;
            },

            /*
             * return default plotter or null
             */
            getDefaultPlotter : function() {
                if (this.getNumPlots() > 0) {
                    return this.getPlotter(0);
                }
            },

            getPlotter : function (index) {
                var json = this.plots[index];

                if (! json.hasOwnProperty("type")) {
                    throw "Plots json item has no type";
                }

                if (! json.hasOwnProperty("spec")) {
                    throw "Plots json item has no spec";
                }

                if (json.type == "plotly") {
                    return new PlotlyPlotter(json.spec);
                }
            },

            toJson : function() {
                return this.plots;
            }
        };

        return PlotsHandler;            // return the constructor
	}
);
