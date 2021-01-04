/**
 ** Copyright 2017 General Electric Company
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
            'sparqlgraph/js/sparqlgraphjson'

			// shimmed

		],

	function(MicroServiceInterface, MsiResultSet, SparqlGraphJson) {

		/*
         *  FailureCallback(htmlMessage, optionalNoValidSparqlMessage)
         *     where optionalNoValidSparqlMessage is the reason no valid sparql was generated, if that's the failure
         */
		var MsiClientNodeGroupService = function (serviceURL, optFailureCallback, optTimeout) {
			// optFailureCallback(html)
			this.optFailureCallback = optFailureCallback;
			this.optTimeout = optTimeout;
			this.msi = new MicroServiceInterface(serviceURL);
		};


		MsiClientNodeGroupService.prototype = {
            /*
             * Synchronous.  All these may use
             *     resultSet.isSuccess()
             *     this.getSuccessSparql(resultSet)
             *     this.getFailedResultHtml(resultSet)
             */
            execGenerateAsk : function (nodegroup, conn, successCallback)              { return this.execNodegroupOnly("generateAsk",           nodegroup, conn, successCallback); },
            execGenerateConstruct : function (nodegroup, conn, successCallback)        { return this.execNodegroupOnly("generateConstruct",     nodegroup, conn, successCallback); },
            execGenerateCountAll : function (nodegroup, conn, successCallback)         { return this.execNodegroupOnly("generateCountAll",      nodegroup, conn, successCallback); },
            execGenerateDelete : function (nodegroup, conn, successCallback)           { return this.execNodegroupOnly("generateDelete",        nodegroup, conn, successCallback); },
            execGenerateSelect : function (nodegroup, conn, successCallback)           { return this.execNodegroupOnly("generateSelect",        nodegroup, conn, successCallback); },
            execGetRuntimeConstraints : function (nodegroup, conn, successCallback)    { return this.execNodegroupOnly("getRuntimeConstraints", nodegroup, conn, successCallback); },

            execGenerateFilter : function (nodegroup, conn, sparqlId, successCallback) { return this.execNodegroupSparqlId("generateFilter",    nodegroup, conn, sparqlId, successCallback); },

            execSetImportSpecFromReturns : function (sgJson, action, lookupRegex, lookupMode, successCallback) {
                var deflateFlag = false;
                var data = {
                      "jsonRenderedNodeGroup": JSON.stringify(sgJson.toJson()),
                    };
                if (action.length > 0) {
                    data.action = action;   // add action if it isn't blank
                }
                if (lookupRegex.length > 0) {
                    data.lookupRegex = lookupRegex;  // add lookupRegex and lookupMode if regex isn't blank
                    data.lookupMode = lookupMode;
                }

                this.msi.postToEndpoint("setImportSpecFromReturns", JSON.stringify (data), "application/json", successCallback, this.optFailureCallback, this.optTimeout);
            },

            execGetSampleIngestionCSV : function (sgJson, format, successCallback) {
                var data = JSON.stringify ({
					  "jsonRenderedNodeGroup": JSON.stringify(sgJson.toJson()),
                      "format": format
					});
				this.msi.postToEndpoint("getSampleIngestionCSV", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
            },

            /*
            **  Asynchronous functions
            */
            execAsyncGenerateFilter : function (nodegroup, conn, sparqlId, sparqlCallback, failureCallback) {
                this.execGenerateFilter(nodegroup, conn, sparqlId,
                                        this.asyncSparqlCallback.bind(this, "generateFilter", sparqlCallback, failureCallback));
            },
            execAsyncGenerateSelect : function (nodegroup, conn, sparqlCallback, failureCallback) {
                this.execGenerateSelect(nodegroup, conn,
                                        this.asyncSparqlCallback.bind(this, "generateSelect", sparqlCallback, failureCallback));
            },
            execAsyncGenerateCountAll : function (nodegroup, conn, sparqlCallback, failureCallback) {
                this.execGenerateCountAll(nodegroup, conn,
                                        this.asyncSparqlCallback.bind(this, "generateCountAll", sparqlCallback, failureCallback));
            },
            execAsyncGenerateConstruct : function (nodegroup, conn, sparqlCallback, failureCallback) {
                this.execGenerateConstruct(nodegroup, conn,
                                        this.asyncSparqlCallback.bind(this, "generateConstruct", sparqlCallback, failureCallback));
            },
            execAsyncGenerateDelete : function (nodegroup, conn, sparqlCallback, failureCallback) {
                this.execGenerateDelete(nodegroup, conn,
                                        this.asyncSparqlCallback.bind(this, "generateDelete", sparqlCallback, failureCallback));
            },

            execAsyncGetRuntimeConstraints : function (nodegroup, conn, tableResCallback, failureCallback) {
                this.execGetRuntimeConstraints(nodegroup, conn,
                                        this.asyncTableCallback.bind(this, "getRuntimeConstraints", tableResCallback, failureCallback));
            },

            execAsyncSetImportSpecFromReturns : function (sgjson, action, lookupRegex, lookupMode, sgjsonCallback, failureCallback) {
                this.execSetImportSpecFromReturns(sgjson, action, lookupRegex, lookupMode,
                                        this.asyncSgJsonCallback.bind(this, "setImportSpecFromReturns", sgjsonCallback, failureCallback));
            },

            execAsyncGetSampleIngestionCSV : function (sgjson, format, csvTextCallback, failureCallback) {
                this.execGetSampleIngestionCSV(sgjson, format,
                                        this.asyncSimpleValueCallback.bind(this, "sampleCSV", csvTextCallback, failureCallback));
            },




            /*
             * @private
             */
            asyncSgJsonCallback(endpoint, sgjsonCallback, failureCallback, resultSet) {
                if (resultSet.isSuccess()) {
                    // get the jobId
                    var sgjson = resultSet.getSimpleResultField("nodegroup");
                    if (sgjson) {
                        sgjsonCallback(sgjson);
                    } else {
                        failureCallback(resultSet.getFailureHtml("did not return a nodegroup"));
                    }
                } else {
                    failureCallback(resultSet.getFailureHtml());
                }
            },

            /*
             * @private
             */
            asyncSimpleValueCallback(valueName, simpleValueCallback, failureCallback, resultSet) {
                if (resultSet.isSuccess()) {
                    // get the jobId
                    var value = resultSet.getSimpleResultField(valueName);
                    if (value) {
                        simpleValueCallback(value);
                    } else {
                        failureCallback(resultSet.getFailureHtml("did not return a " + valueName));
                    }
                } else {
                    failureCallback(resultSet.getFailureHtml());
                }
            },

            /*
             * @private
             */
            asyncSparqlCallback(endpoint, sparqlCallback, failureCallback, resultSet) {
                if (resultSet.isSuccess()) {
                    // get the jobId
                    var sparql = resultSet.getSimpleResultField("SparqlQuery");
                    if (sparql) {
                        sparqlCallback(sparql);
                    } else {
                        failureCallback(resultSet.getFailureHtml("did not return a SparqlQuery"));
                    }
                } else {
                    failureCallback(this.getNoValidSparqlMessage(resultSet));
                }
            },

            /*
             * @private
             */
            asyncTableCallback(endpoint, tableResCallback, failureCallback, resultSet) {
                if (resultSet.isSuccess()) {
                    if (resultSet.isTableResults()) {
                        tableResCallback(resultSet);
                    } else {
                        failureCallback(resultSet.getFailureHtml("did not return a table result."));
                    }
                } else {
                    if (resultSet.isTableResults()) {
                        // PEC TODO: this is confusing.  Empty table is "success"
                        //   have justin change microservice behavior
                        tableResCallback(resultSet);
                    } else {
                        failureCallback(resultSet.getFailureHtml());
                    }
                }
            },

            getSuccessSparql : function (resultSet) {
				return resultSet.getSimpleResultField("SparqlQuery");
			},

            getFailedResultHtml : function (resultSet) {
				return resultSet.getGeneralResultHtml();
			},

            /*
             * returns the Invalid Sparql Rationale if it exists
             * otherwise undefined
             *
             * undefined - allows this to be used as an optional param in the usual way
             */
            getNoValidSparqlMessage : function (resultSet) {
                var msg = resultSet.getSimpleResultField("InvalidSparqlRationale");
                return (msg == null ? undefined : msg);
            },

            /**
              * @private
              */
			execNodegroupOnly : function (endpoint, nodegroup, conn, successCallback) {
                var sgJson = new SparqlGraphJson(conn, nodegroup, null);
				var data = JSON.stringify ({
					  "jsonRenderedNodeGroup": JSON.stringify(sgJson.toJson()),
					});
				this.msi.postToEndpoint(endpoint, data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

            /**
              * @private
              */
            execNodegroupSparqlId : function (endpoint, nodegroup, conn, sparqlId, successCallback) {
                var deflateFlag = false;
				var sgJson = new SparqlGraphJson(conn, nodegroup, null, deflateFlag);
                var data = JSON.stringify ({
					  "jsonRenderedNodeGroup": JSON.stringify(sgJson.toJson()),
                      "sparqlID": sparqlId
					});
				this.msi.postToEndpoint(endpoint, data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

            /**
              * @private
              */
			execSgJsonOnly : function (endpoint, sgJson, successCallback) {
				var data = JSON.stringify ({
					  "jsonRenderedNodeGroup": JSON.stringify(sgJson.toJson()),
					});
				this.msi.postToEndpoint(endpoint, data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

		};

		return MsiClientNodeGroupService;            // return the constructor
	}
);
