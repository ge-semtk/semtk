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
            'sparqlgraph/js/msiresultset'

            //'visjs/vis.min'   <- should we duplicate this (download stable version of plotly)
            //                       OR should we presume the internet is available?  and pull from there?
			// shimmed

		],
    // sample of how 'vis' was used
    // function(IIDXHelper, MsiResultSet, vis) {
    // TODO: Plotly is MIT licensed.  There's a list somewhere in code and wiki?

    function(IIDXHelper, MsiResultSet) {

        var PlotlyPlotter = function (plotSpec) {
            this.spec = plotSpec;
        };


        PlotlyPlotter.prototype = {
            CONSTANT : 1,

            sample : function () {
                return 1;
            },

            addPlotToDiv : function(div, tableRes) {
                div.innerHTML = "Change this into a plot<br>" + tableRes.tableGetHtml();
            }
        };


        return PlotlyPlotter;            // return the constructor
	}
);
