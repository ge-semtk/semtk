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
     *   Handles the entire sparqlgraphjson.plotSpecs JSON
     */
    function(PlotlyPlotter) {

        var PlotSpecsHandler = function (plotSpecsJson) {
            this.plotSpecs = plotSpecsJson ? plotSpecsJson : [];

            // not saved with Json
            this.defaultIndex = this.plotSpecs.length > 0 ? 0 : -1;
        };


        PlotSpecsHandler.prototype = {
            CONSTANT : 1,

            getNumPlots : function () {
                return this.plotSpecs.length;
            },

            /*
             * return name = or throw exception
             */
            getName : function(index) {
                var json = this.plotSpecs[index];
                if (! json.hasOwnProperty("name")) {
                    throw "Plots json item has no name";
                }
                return json.name;
            },

            getNames : function() {
                var ret = [];
                for (var i=0; i < this.getNumPlots(); i++) {
                    ret[i] = this.getName(i);
                }
                return ret;
            },

            /*
             * return default plotter or null
             */
            getDefaultPlotter : function() {
                if (this.defaultIndex != -1) {
                    return this.getPlotter(this.defaultIndex);
                } else {
                    return null;
                }
            },

            // default plot index or -1
            getDefaultIndex : function () {
                return this.defaultIndex;
            },

            setDefaultIndex : function(i) {
                this.defaultIndex = i;
            },

            getPlotter : function (index) {
                var json = this.plotSpecs[index];

                if (! json.hasOwnProperty("type")) {
                    throw "Plots json item has no type";
                }

                if (! json.hasOwnProperty("spec")) {
                    throw "Plots json item has no spec";
                }

                if (json.type == "plotly") {
                    return new PlotlyPlotter(json);
                }
            },

            setPlotter : function (index, plotter) {
                this.plotSpecs[index] = plotter.toJson();
            },

            addPlotter : function (plotter) {
                this.plotSpecs.push(plotter.toJson());
                if (this.defaultIndex == -1) {
                    this.defaultIndex = 0;
                }
            },

            delPlotter : function (index) {
                this.plotSpecs.splice(index, 1);
                if (this.defaultIndex >= index) {
                    this.defaultIndex--;
                }
            },

            toJson : function() {
                return this.plotSpecs;
            }
        };

        return PlotSpecsHandler;            // return the constructor
	}
);
