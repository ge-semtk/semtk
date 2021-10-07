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

            /**#
                These return result sets.   Look to execAsync*() below for more complete functionality
            ***/
            execGenerateAsk : function (nodegroup, conn, successCallback)              { return this.execNodegroupOnly("generateAsk",           nodegroup, conn, successCallback); },
            execGenerateConstruct : function (nodegroup, conn, successCallback)        { return this.execNodegroupOnly("generateConstruct",     nodegroup, conn, successCallback); },
            execGenerateCountAll : function (nodegroup, conn, successCallback)         { return this.execNodegroupOnly("generateCountAll",      nodegroup, conn, successCallback); },
            execGenerateDelete : function (nodegroup, conn, successCallback)           { return this.execNodegroupOnly("generateDelete",        nodegroup, conn, successCallback); },
            execGenerateSelect : function (nodegroup, conn, successCallback)           { return this.execNodegroupOnly("generateSelect",        nodegroup, conn, successCallback); },
            execGetRuntimeConstraints : function (nodegroup, conn, successCallback)    { return this.execNodegroupOnly("getRuntimeConstraints", nodegroup, conn, successCallback); },

            execGenerateFilter : function (nodegroup, conn, sparqlId, successCallback) { return this.execNodegroupSparqlId("generateFilter",    nodegroup, conn, sparqlId, successCallback); },
            execGetServerTypes : function (successCallback) { return this.msi.postToEndpoint("getServerTypes", {}, "application/json", successCallback, this.optFailureCallback, this.optTimeout);},

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

            execInflateAndValidate : function (sgJson, successCallback) {
                var data = JSON.stringify ({
                      "jsonRenderedNodeGroup": JSON.stringify(sgJson.toJson())
                    });
                this.msi.postToEndpoint("inflateAndValidate", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
            },

            execGetSampleIngestionCSV : function (sgJson, format, successCallback) {
                var data = JSON.stringify ({
					  "jsonRenderedNodeGroup": JSON.stringify(sgJson.toJson()),
                      "format": format
					});
				this.msi.postToEndpoint("getSampleIngestionCSV", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
            },


            execSuggestNodeClass : function (nodegroup, conn, snode, classListCallback) {
                var sgJson = new SparqlGraphJson(conn, nodegroup);
                var data = JSON.stringify ({
					  "jsonRenderedNodeGroup": JSON.stringify(sgJson.toJson()),
                      "itemStr": nodegroup.buildItemStr(snode)
					});
                this.msi.postToEndpoint("suggestNodeClass", data, "application/json", classListCallback, this.optFailureCallback, this.optTimeout);
            },

            execGetSampleIngestionCSV : function (sgJson, format, successCallback) {
                var data = JSON.stringify ({
					  "jsonRenderedNodeGroup": JSON.stringify(sgJson.toJson()),
                      "format": format
					});
				this.msi.postToEndpoint("getSampleIngestionCSV", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
            },

            execAddSamplePlot : function (sgJson, columnNames, graphType, plotName, plotType, successCallback) {
                var data = JSON.stringify ({
					  "jsonRenderedNodeGroup": JSON.stringify(sgJson.toJson()),
                      "columnNames" : columnNames,
                      "graphType" : graphType,
                      "plotName" : plotName,
                      "plotType" : plotType
					});
				this.msi.postToEndpoint("plot/addSamplePlot", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
            },

            execChangeItemURI : function (sgJson, itemStr, newURI, domainOrRange, successCallback) {
                var data = JSON.stringify ({
					  "jsonRenderedNodeGroup": JSON.stringify(sgJson.toJson()),
                      "itemStr" : itemStr,
                      "newURI" : newURI,
                      "domainOrRange" : domainOrRange
					});
				this.msi.postToEndpoint("changeItemURI", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
            },

            execFindAllPaths : function (sgJson, addClassURI, propsInDataFlag, nodegroupInDataFlag, simpleResultsCallback) {
                var data = JSON.stringify ({
					  "jsonRenderedNodeGroup": JSON.stringify(sgJson.toJson()),
                      "addClass" : addClassURI,
                      "propsInDataFlag" : propsInDataFlag,
                      "nodegroupInDataFlag" : nodegroupInDataFlag
					});
				this.msi.postToEndpoint("findAllPaths", data, "application/json", simpleResultsCallback, this.optFailureCallback, this.optTimeout);
            },

            /* this sync call has no "execAsync" Version
            */
            execCreateConstructAllConnected : function (conn, classUri, instanceUri, sgjsonCallback) {
                var data = JSON.stringify ({
					  "conn": JSON.stringify(conn.toJson()),
                      "className" : classUri,
                      "instanceUri" : instanceUri
					});

                var cb = this.asyncSgJsonCallback.bind(this, "createConstructAllConnected", sgjsonCallback, this.optFailureCallback);
				this.msi.postToEndpoint("createConstructAllConnected", data, "application/json", cb, this.optFailureCallback, this.optTimeout);
            },

            /*
            **  Asynchronous functions: perform the whole async chain and return a "real" value.  failureCalback on any error.
            **  (Name is confusing. All these functions in the file are Async.)
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

            execAsyncSuggestNodeClass : function (nodegroup, conn, snode, classListCallback, failureCallback) {
                this.execSuggestNodeClass(nodegroup, conn, snode,
                                        this.asyncSimpleValueCallback.bind(this, "classList", classListCallback, failureCallback));
            },

            // Success calls ngMessagesItemsCallback(nodegroup, modelErrorMessages, invalidItemStrings)
            // FailureCallback(html) if not successful
            execAsyncInflateAndValidate : function (sgjson, ngMessagesItemsCallback, failureCallback) {
                this.execInflateAndValidate(sgjson,
                                            this.asyncInflateAndValidateCallback.bind(this, ngMessagesItemsCallback, failureCallback));
            },

            execAsyncAddSamplePlot : function (sgjson, columnNames, graphType, plotName, plotType, sgjsonCallback, failureCallback) {
                this.execAddSamplePlot(sgjson, columnNames, graphType, plotName, plotType,
                                        this.asyncSgJsonCallback.bind(this, "plot/addSamplePlot", sgjsonCallback, failureCallback));
            },

            execAsyncChangeItemURI : function (sgJson, itemStr, newURI, domainOrRange, sgjsonCallback, failureCallback) {
                this.execChangeItemURI(sgJson, itemStr, newURI, domainOrRange,
                                        this.asyncSgJsonCallback.bind(this, "changeItemURI", sgjsonCallback, failureCallback));
            },

            execAsyncFindAllPaths : function (sgJson, newURI, propsInDataFlag, nodegroupInDataFlag, simpleResultsCallback, failureCallback) {
                this.execFindAllPaths(gJson, newURI, propsInDataFlag, nodegroupInDataFlag,
                                        this.asyncSimpleResultsCallback.bind(this, simpleResultsCallback, failureCallback));
            },

            /*
             * @private
             */
            asyncSgJsonCallback(endpoint, sgjsonCallback, failureCallback, resultSet) {
                if (resultSet.isSuccess()) {
                    // get the sgjson
                    var sgJsonJson = resultSet.getSimpleResultField("nodegroup");
                    if (sgJsonJson) {
                        var sgjson = new SparqlGraphJson();
                        sgjson.fromJson(sgJsonJson);
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
                    // get the field
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
            asyncSimpleResultCallback(simpleResultCallback, failureCallback, resultSet) {
                if (resultSet.isSuccess()) {
                    simpleResultCallback(resultSet);
                } else {
                    failureCallback(resultSet.getFailureHtml());
                }
            },

            /*
             * Check for success and all proper return fields
             * @private
             */
            asyncInflateAndValidateCallback(ngMessagesItemsCallback, failureCallback, resultSet) {
                if (! resultSet.isSuccess()) {
                    failureCallback(resultSet.getFailureHtml());
                } else {
                    var f = ["nodegroup", "modelErrorMessages", "invalidItemStrings", "warnings"];
                    for (var field of f) {
                        if (resultSet.getSimpleResultField(field) == null) {
                            failureCallback("InflateAndValidate return did not contain field: " + field);
                        }
                    }
                    ngMessagesItemsCallback(
                        resultSet.getSimpleResultField(f[0]),
                        resultSet.getSimpleResultField(f[1]),
                        resultSet.getSimpleResultField(f[2]),
                        resultSet.getSimpleResultField(f[3])
                    );
                }
            },

            /*
             * @private
             */
            asyncSparqlCallback(endpoint, sparqlCallback, failureCallback, resultSet) {
                if (resultSet.isSuccess()) {
                    // get the jobId
                    var sparql = resultSet.getSimpleResultField("SparqlQuery");
                    var msg = resultSet.getSimpleResultField("QueryMessage");
                    if (sparql) {
                        sparqlCallback(sparql, msg);
                    } else {
                        failureCallback(resultSet.getFailureHtml("did not return a SparqlQuery"));
                    }
                } else {
                    failureCallback(resultSet.getFailureHtml(), this.getNoValidSparqlMessage(resultSet));
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
