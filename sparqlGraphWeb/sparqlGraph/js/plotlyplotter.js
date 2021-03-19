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
            this.spec = plotSpec;   // TODO formalize this - must have "layout" and what else?
        };


        PlotlyPlotter.prototype = {
            CONSTANT : 1,

            sample : function () {
                return 1;
            },

            addPlotToDiv : function(div, tableRes) {

                row0 = tableRes.tableGetRows()[0];
                row1 = tableRes.tableGetRows()[1];
                row2 = tableRes.tableGetRows()[2];
 
                var trace0 = { x: [1, 2], y: row0, type: 'bar' };
                var trace1 = { x: [1, 2], y: row1, type: 'scatter', mode: 'lines+markers' };
                var trace2 = { x: [1, 2], y: row2, type: 'scatter', mode: 'lines+markers' };
                var data = [trace0, trace1, trace2];

                var layout = this.spec.layout;

                Plotly.newPlot( div, data, layout, {editable: true} );
            }
        };


        return PlotlyPlotter;            // return the constructor
	}
);
