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
        	'sparqlgraph/js/microserviceinterface',
         	'sparqlgraph/js/msiresultset'

			// shimmed

		],

	function(MicroServiceInterface, MsiResultSet) {


		var MsiClientUtility = function (serviceURL, optFailureCallback, optTimeout) {
			this.msi = new MicroServiceInterface(serviceURL);
			this.optFailureCallback = optFailureCallback;
			this.optTimeout = optTimeout;
		};


		MsiClientUtility.prototype = {

			execProcessPlotSpec : function (plotSpecJson, tableJson, successCallback) {
                var data = JSON.stringify ({
                    "plotSpecJson": JSON.stringify(plotSpecJson),
                    "tableJson": JSON.stringify(tableJson),
                });
				this.msi.postAndCheckSuccess("processPlotSpec", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

		};

		return MsiClientUtility;            // return the constructor
	}
);
