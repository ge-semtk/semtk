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
        	'sparqlgraph/js/iidxhelper',
            'sparqlgraph/js/msiresultset',

            'plotly/plotly-latest.min'
            //                       OR should we presume the internet is available?  and pull from there?
			// shimmed

		],

    function(IIDXHelper, MsiResultSet, Plotly) {

        var PlotlyPlotter = function (plotSpec) {
            this.spec = plotSpec;
        };


        PlotlyPlotter.prototype = {
            CONSTANT : 1,

            sample : function () {
                return 1;
            },

            addPlotToDiv : function(div, tableRes) {

                // TODO "graphRows" is just a placeholder - decide what to support in dataSpec
                var graphRows = this.spec.dataSpec.graphRows;  

                // create traces
                var data = [];
                for (var i = 0; i < graphRows.length; i++){
                    row = tableRes.tableGetRows()[graphRows[i]];    // get the row of data      TODO graph by columns, not rows
                    trace = { x: [1, 2], y: row };                  // add data to trace        TODO not all plot types take x and y
                    var traceBase = this.spec.traceBase;
                    $.extend( true, trace, traceBase );             // add base to trace
                    data.push(trace);                               // add the trace to the data object
                }

                var layout = this.spec.layout;
                Plotly.newPlot( div, data, layout, {editable: true} );
            }
        };

        return PlotlyPlotter;            // return the constructor
	}
);
