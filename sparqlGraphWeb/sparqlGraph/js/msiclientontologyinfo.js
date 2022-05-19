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
            'sparqlgraph/js/msiclientstatus',
        	'sparqlgraph/js/msiclientresults',
        	'sparqlgraph/js/msiresultset',

			// shimmed

		],

	function(MicroServiceInterface, MsiClientStatus, MsiClientResults, MsiResultSet) {


		var MsiClientOntologyInfo = function (serviceURL, optFailureCallback, optTimeout) {

			this.msi = new MicroServiceInterface(serviceURL);
			this.optFailureCallback = optFailureCallback;
			this.optTimeout = optTimeout;
		};
        MsiClientOntologyInfo.buildPredicateStatsCallback = function(jsonBlobCallback, failureCallback, progressCallback, checkForCancelCallback, optLoPercent, optHiPercent) {
            return MsiClientOntologyInfo.buildAsyncCallback("blob", jsonBlobCallback, failureCallback, progressCallback, checkForCancelCallback, optLoPercent, optHiPercent);
        };
        MsiClientOntologyInfo.buildCardinalityViolationsCallback = function(tableCallback, failureCallback, progressCallback, checkForCancelCallback, optLoPercent, optHiPercent) {
            return MsiClientOntologyInfo.buildAsyncCallback("table", tableCallback, failureCallback, progressCallback, checkForCancelCallback, optLoPercent, optHiPercent);
        };

        //
        // Build a callback for a table or Blob
        //
        MsiClientOntologyInfo.buildAsyncCallback = function(tableOrBlob, successCallback, failureCallback, progressCallback, checkForCancelCallback, optLoPercent, optHiPercent) {

            // callback for the nodegroup execution service to send jobId
            var simpleResCallback = function(simpleResJson) {

                var resultSet = new MsiResultSet(simpleResJson.serviceURL, simpleResJson.xhr);
                if (!resultSet.isSuccess()) {
                    failureCallback(resultSet.getFailureHtml());
                } else {
                    var jobId = resultSet.getSimpleResultField("JobId");
                    // callback for status service after job successfully finishes
                    var statusSuccessCallback = function() {
                        // callback for results service
                        var resultsSuccessCallback = function (results) {
                            progressCallback("finishing up", 99);
                            successCallback(results);
                            progressCallback("");
                        };
                        var resultsClient = new MsiClientResults(g.service.results.url, jobId);
                        if (tableOrBlob == "blob") {
                            resultsClient.execGetJsonBlobRes(resultsSuccessCallback);
                        } else {
                            resultsClient.execGetTableResultsJsonTableRes(5000, resultsSuccessCallback);
                        }
                    };

                    progressCallback("", 1);

                    // call status service loop
                    var statusClient = new MsiClientStatus(g.service.status.url, jobId, failureCallback);
                    statusClient.execAsyncWaitUntilDone(statusSuccessCallback, checkForCancelCallback, progressCallback);
                }

            };

            return simpleResCallback;
        };

		MsiClientOntologyInfo.prototype = {


			execRetrieveDetailedOntologyInfo : function (graph, domain, servertype, url, successCallback) {
				var myData = JSON.stringify ({
					"dataset" : graph,
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

            // Service layer should normally take care of this.  Special-purpose use only.
            execUncacheChangedConn : function (conn, successCallback) {
                var myData = JSON.stringify ({
					"jsonRenderedSparqlConnection" : JSON.stringify(conn.toJson()),
				});

				this.msi.postToEndpoint("uncacheChangedConn", myData, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
            },

            // Get a predicate stats.
            // Handy way to pre-cache them in the service Layer
            // Returns simpleResults with jobId.
            execGetPredicateStats : function (conn, successCallback) {
                var myData = JSON.stringify ({
					"conn" : JSON.stringify(conn.toJson()),
				});

				this.msi.postToEndpoint("getPredicateStats", myData, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
            },

		
			// Get cardinality violations
            execGetCardinalityViolations : function (conn, maxRows, successCallback) {
                var myData = JSON.stringify ({
					"conn" : JSON.stringify(conn.toJson()),
					"maxRows" : maxRows
				});

				this.msi.postToEndpoint("getCardinalityViolations", myData, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
            },
		};

		return MsiClientOntologyInfo;            // return the constructor
	}
);
