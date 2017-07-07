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
                        failureCallback(this.getFailureMessage(resultSet, endpoint + " did not return a SparqlQuery."));
                    }
                } else {
                    failureCallback(this.getFailureMessage(resultSet));
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
                        failureCallback(this.getFailureMessage(resultSet, endpoint + " did not return a table result."));
                    }
                } else {
                    if (resultSet.isTableResults()) {
                        // PEC TODO: this is confusing.  Empty table is "success"
                        //   have justin change microservice behavior
                        tableResCallback(resultSet);
                    } else {
                        failureCallback(this.getFailureMessage(resultSet));
                    }
                }
            },
            
            getSuccessSparql : function (resultSet) {
				return resultSet.getSimpleResultField("SparqlQuery");
			},
            
            getFailedResultHtml : function (resultSet) {
				return resultSet.getGeneralResultHtml();
			},
            
            // Temporary:  if failure was just no valid SPARQL
            //             then return a SPARQL comment.
            //             Otherwise null.
            getFailedTEMPBadSparql : function (resultSet) {
                if (resultSet.getGeneralField("message") == "operations failed.") {
                    var rationale = resultSet.getGeneralField("rationale");
                    if (rationale != null) {
                        if (rationale.indexOf("No values selected") > -1) {
                            return "# Error: nothing to select";
                        } else if (rationale.indexOf("nothing given to delete") > -1) {
                            return "# Error: nothing to delete";
                        }
                    }
                }
                return null;
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
			
            // Add a header and convert resultSet to html
            getFailureMessage : function (resultSet, optHeader) {
                var html = (typeof optHeader == "undefined" || optHeader == null) ? "" : "<b>" + optHeader + "</b><hr>";
                html += resultSet.getSimpleResultsHtml();
                
                return html;
            },
		};
	
		return MsiClientNodeGroupService;            // return the constructor
	}
);