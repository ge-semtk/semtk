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
            'sparqlgraph/js/msiclientstatus',
            'sparqlgraph/js/msiclientresults'
        	
			// shimmed
			
		],

	function(MicroServiceInterface, MsiResultSet, MsiClientStatus, MsiClientResults) {
	
		/*
         * optFailureCallback(messageHTML) 
         */
		var MsiClientNodeGroupExec = function (serviceURL, optTimeout) {
			
			this.msi = new MicroServiceInterface(serviceURL);
            this.optTimeout = optTimeout;
		};
		
		
		MsiClientNodeGroupExec.prototype = {
           

            execGetJobCompletionPercentage : function(jobId, percentCallback, failureCallback) {
                
                // Callback checks for success and gets a percent int before calling percentCallback
                var successCallback = function(percCallback, fCallback, resultSet) {
                    if (resultSet.isSuccess()) {
                        var thisPercent = resultSet.getSimpleResultField("percent");
                        if (thisPercent == null) {
                            fCallback(this.getFailureMessage(resultSet, 
                                              "NodeGroupExecution/getJobCompletionPercentage did not return a percent."));
                        } else {
                            percCallback(parseInt(thisPercent));
                        } 
                    } else {
                        fCallback(this.getFailureMessage(resultSet));
                    }
                }.bind(this, percentCallback, failureCallback);
                
                this.execJobId("getJobCompletionPercentage", jobId, successCallback, failureCallback);
            },
            
            
            execJobStatus :  function(jobId, successBoolCallback, failureCallback) {
                
                var successCallback = function(successBoolCallback0, failureCallback0, resultSet) {
                        
                    // job is finished
                    if (resultSet.isSuccess()) {
                        var status = resultSet.getSimpleResultField("status");

                        if ( status == null) {
                            failureCallback0(this.getFailureMessage(resultSet, 
                                                                    "NodeGroupExecution/jobStatus did not return a status."));
                        } else if (status == "Success") {
                            successBoolCallback0(true);
                        } else {
                            successBoolCallback0(false);
                        }
                    } else {
                        failureCallback0(this.getFailureMessage(resultSet, null, failureCallback0));
                    }
                }.bind(this, successBoolCallback, failureCallback);
                
                this.execJobId("jobStatus", jobId, successCallback, failureCallback);
            },
            
            execJobStatusMessage :  function(jobId, messageCallback, failureCallback) {
                
                var successCallback = function(messageCallback0, failureCallback0, resultSet) {
                        
                    // job is finished
                    if (resultSet.isSuccess()) {
                        var message = resultSet.getSimpleResultField("message");

                        if ( message == null) {
                            failureCallback0(this.getFailureMessage(resultSet, 
                                                   "NodeGroupExecution/jobStatusMessage did not return a message.",
                                                   ));
                        } else {
                            messageCallback0(message);
                        }
                    } else {
                        failureCallback0(this.getFailureMessage(resultSet));
                    }
                }.bind(this, messageCallback, failureCallback);
                
                this.execJobId("jobStatusMessage", jobId, successCallback, failureCallback);
            },
            
            execAsyncDispatchSelectFromNodeGroup : function(nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback) {
                this.runAsyncNodegroup("dispatchSelectFromNodegroup",
                                        nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback);
            },
            
            execAsyncDispatchDeleteFromNodeGroup : function(nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback) {
                this.runAsyncNodegroup("dispatchDeleteFromNodegroup",
                                        nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback);
            },
            
            execAsyncDispatchCountFromNodeGroup : function(nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback) {
                this.runAsyncNodegroup("dispatchCountFromNodegroup",
                                        nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback);
            },
            
            execAsyncDispatchRawSparql : function(sparql, conn, jobIdCallback, failureCallback) {
                this.runAsyncSparql("dispatchRawSparql",
                                        sparql, conn, jobIdCallback, failureCallback);
            },

            /* ===================================================================================== */
            
            /*
             * successCallback(resultSet)
             * @private
             */
            execJobId : function (endpoint, jobId, successCallback, failureCallback) {
                var data = JSON.stringify ({
                    "jobID": jobId.toString()
                });
                
                this.msi.postToEndpoint(endpoint, data, "application/json", 
                                        successCallback, 
                                        failureCallback, 
                                        this.optTimeout);
            },
            
            runAsyncSparql : function (endpoint, sparql, conn, jobIdCallback, failureCallback) {
                
				var data = JSON.stringify ({
                    "sparql":           sparql,
                    "sparqlConnection": JSON.stringify(conn.toJson()),
                });

				this.runAsync(endpoint, data, jobIdCallback, failureCallback);
			},
            
            /**
              * Package data for endpoint requiring nodegroup, conn, edcConstraints, runtimeconstraints
              * and call runAsync()
              *
              * tableResCallback(table)
              * failureCallback(html)
              * statusCallback(percentCompleteInt)     hint for statusCallback: setStatus.bind(this, "Running an XYZ")
              *
              * @private
              */
			runAsyncNodegroup : function (endpoint, nodegroup, conn, edcConstraints, runtimeConstraints, jobIdCallback, failureCallback) {
                
				var data = JSON.stringify ({
                    "jsonRenderedNodeGroup": JSON.stringify(nodegroup.toJson()),
                    "sparqlConnection":      JSON.stringify(conn.toJson()),
                    "runtimeConstraints":    (typeof runtimeConstraints == "undefined" || runtimeConstraints == null) ? "" : JSON.stringify(runtimeConstraints.toJson()),
                    "externalDataConnectionConstraints": (typeof edcConstraints == "undefined" || edcConstraints == null) ? "" : JSON.stringify(edcConstraints.toJson())
                });

				this.runAsync(endpoint, data, jobIdCallback, failureCallback);
			},
            
            /*
             * start an async chain where results service is used.
             * @private
             */
            runAsync : function (endpoint, data, jobIdCallback, failureCallback) {
                
                this.msi.postToEndpoint(endpoint, data, "application/json", 
                                        this.runAsyncJobIdCallback.bind(this, endpoint, jobIdCallback, failureCallback), 
                                        failureCallback, 
                                        this.optTimeout);
            },
            
            /*
             * first callback in runAsync chain
             * Expecting to receive the jobId
             * @private
             */
            runAsyncJobIdCallback : function (endpoint, jobIdCallback, failureCallback, resultSet) {
                if (resultSet.isSuccess()) {
                    // get the jobId
                    var jobId = resultSet.getSimpleResultField("JobId");
                    if (jobId) {
                        jobIdCallback(jobId);
                    } else {
                        failureCallback(this.getFailureMessage(resultSet, endpoint + " did not return a requestID."));
                    }
                } else {
                    failureCallback(this.getFailureMessage(resultSet));
                }
            },
            
            runAsynctableResCallback : function (tableRes) {
                this.tableResCallback(tableRes);
            },
            
            // Add a header and convert resultSet to html
            getFailureMessage : function (resultSet, optHeader) {
                var html = (typeof optHeader == "undefined" || optHeader == null) ? "" : "<b>" + optHeader + "</b><hr>";
                html += resultSet.getSimpleResultsHtml();
                
                return html;
            },
			
		};
	
		return MsiClientNodeGroupExec;            // return the constructor
	}
);