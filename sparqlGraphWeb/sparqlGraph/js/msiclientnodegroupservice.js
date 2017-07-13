/**
 ** Copyright 2016 General Electric Company
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
             * All these may use 
             *     resultSet.isSuccess()
             *     this.getSuccessSparql(resultSet)
             *     this.getFailedResultHtml(resultSet)
             */
            execGenerateAsk : function (nodegroup, successCallback)       { return this.execNodegroupOnly("generateAsk",       nodegroup, successCallback); },
            execGenerateConstruct : function (nodegroup, successCallback) { return this.execNodegroupOnly("generateConstruct", nodegroup, successCallback); },
            execGenerateCountAll : function (nodegroup, successCallback)  { return this.execNodegroupOnly("generateCountAll",  nodegroup, successCallback); },
            execGenerateDelete : function (nodegroup, successCallback)    { return this.execNodegroupOnly("generateDelete",    nodegroup, successCallback); },
            execGenerateSelect : function (nodegroup, successCallback)    { return this.execNodegroupOnly("generateSelect",    nodegroup, successCallback); },
            execGetRuntimeConstraints : function (nodegroup, successCallback)    { return this.execNodegroupOnly("getRuntimeConstraints",    nodegroup, successCallback); },

            execGenerateFilter : function (nodegroup, sparqlId, successCallback)    { return this.execNodegroupSparqlId("generateFilter", nodegroup, sparqlId, successCallback); },
            
            /*==== sparqlCallback functions ====*/
            execAsyncGenerateFilter : function (nodegroup, sparqlId, sparqlCallback, failureCallback) {
                this.execGenerateFilter(nodegroup, sparqlId, 
                                        this.asyncSparqlCallback.bind(this, "generateFilter", sparqlCallback, failureCallback));
            },
            execAsyncGenerateSelect : function (nodegroup, sparqlCallback, failureCallback) {
                this.execGenerateSelect(nodegroup, 
                                        this.asyncSparqlCallback.bind(this, "generateSelect", sparqlCallback, failureCallback));
            },
            execAsyncGenerateCountAll : function (nodegroup, sparqlCallback, failureCallback) {
                this.execGenerateCountAll(nodegroup, 
                                        this.asyncSparqlCallback.bind(this, "generateCountAll", sparqlCallback, failureCallback));
            },
            execAsyncGenerateConstruct : function (nodegroup, sparqlCallback, failureCallback) {
                this.execGenerateConstruct(nodegroup, 
                                        this.asyncSparqlCallback.bind(this, "generateConstruct", sparqlCallback, failureCallback));
            },
            execAsyncGenerateDelete : function (nodegroup, sparqlCallback, failureCallback) {
                this.execGenerateDelete(nodegroup, 
                                        this.asyncSparqlCallback.bind(this, "generateDelete", sparqlCallback, failureCallback));
            },
            
            execAsyncGetRuntimeConstraints : function (nodegroup, tableResCallback, failureCallback) {
                this.execGetRuntimeConstraints(nodegroup, 
                                        this.asyncTableCallback.bind(this, "getRuntimeConstraints", tableResCallback, failureCallback));
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
                        failureCallback(resultSet.buildFailureHtml("did not return a SparqlQuery"));
                    }
                } else {
                    failureCallback(resultSet.buildFailureHtml(), this.getNoValidSparqlMessage(resultSet));
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
                        failureCallback(resultSet.buildFailureHtml("did not return a table result."));
                    }
                } else {
                    if (resultSet.isTableResults()) {
                        // PEC TODO: this is confusing.  Empty table is "success"
                        //   have justin change microservice behavior
                        tableResCallback(resultSet);
                    } else {
                        failureCallback(resultSet.buildFailureHtml());
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
			execNodegroupOnly : function (endpoint, nodegroup, successCallback) {
				var data = JSON.stringify ({
					  "jsonRenderedNodeGroup": JSON.stringify(nodegroup.toJson()),
					});
				this.msi.postToEndpoint(endpoint, data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},
            
            /**
              * @private
              */
            execNodegroupSparqlId : function (endpoint, nodegroup, sparqlId, successCallback) {
				var data = JSON.stringify ({
					  "jsonRenderedNodeGroup": JSON.stringify(nodegroup.toJson()),
                      "targetObjectSparqlId": sparqlId
					});
				this.msi.postToEndpoint(endpoint, data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},
			
		};
	
		return MsiClientNodeGroupService;            // return the constructor
	}
);