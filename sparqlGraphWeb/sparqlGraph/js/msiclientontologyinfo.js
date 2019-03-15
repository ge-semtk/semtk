/**
 ** Copyright 2016-17 General Electric Company
 **
 ** Authors:  Paul Cuddihy, Justin McHugh
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
        	'sparqlgraph/js/msiresultset',

			// shimmed

		],

	function(MicroServiceInterface, MsiResultSet) {


		var MsiClientOntologyInfo = function (serviceURL, optFailureCallback, optTimeout) {

			this.msi = new MicroServiceInterface(serviceURL);
			this.optFailureCallback = optFailureCallback;
			this.optTimeout = optTimeout;
		};


		MsiClientOntologyInfo.prototype = {


			execRetrieveDetailedOntologyInfo : function (dataset, domain, servertype, url, successCallback) {
				var myData = JSON.stringify ({
					"dataset" : dataset,
					"domain" : domain,
					"serverType" : servertype,
					"url" : url,
				});

				this.msi.postToEndpoint("getDetailedOntologyInfo", myData, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

            execGetOntologyInfoJson : function (conn, successCallback) {
				var myData = JSON.stringify ({
					"jsonRenderedSparqlConnection" : JSON.stringify(conn.toJson()),
				});

				this.msi.postToEndpoint("getOntologyInfoJson", myData, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

            execGetDataDictionary : function (conn, successCallback) {
                var myData = JSON.stringify ({
					"sparqlConnectionJson" : JSON.stringify(conn.toJson())
				});

				this.msi.postToEndpoint("getDataDictionary", myData, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
            },

			getRetrieveDetailedOntologyInfoSucceeded : function (resultSet) {
				// may return null
				return resultSet.getSimpleResultField("ontologyInfo");
			},

            execUncacheOntology : function (conn, successCallback) {
                var myData = JSON.stringify ({
					"jsonRenderedSparqlConnection" : JSON.stringify(conn.toJson()),
				});

				this.msi.postToEndpoint("uncacheOntology", myData, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
            },

		};

		return MsiClientOntologyInfo;            // return the constructor
	}
);
