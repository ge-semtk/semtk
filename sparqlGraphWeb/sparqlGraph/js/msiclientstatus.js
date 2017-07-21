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
            'sparqlgraph/js/modaliidx'
        	
			// shimmed
			
		],

	function(MicroServiceInterface, MsiResultSet, ModalIidx) {
	
		
		var MsiClientStatus = function (serviceURL, jobId, optFailureCallback, optTimeout) {
			
			this.msi = new MicroServiceInterface(serviceURL);
			this.jobId = jobId;
			this.optFailureCallback = optFailureCallback;
			this.optTimeout = optTimeout;
			
		};
		
		
		MsiClientStatus.prototype = {
				
			getJobIdData : function () {
				return JSON.stringify ({
					"jobId" : this.jobId,
				});	
			},
			
            /* 
             * Old-fashioned successCallback(resultsSet)
             */
			execGetPercentComplete : function (successCallback) {			
				
				this.msi.postToEndpoint("getPercentComplete", this.getJobIdData(), "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},
			
			execGetStatus : function (successCallback) {
				
				this.msi.postToEndpoint("getStatus", this.getJobIdData(), "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},
			
			execGetStatusMessage : function (successCallback) {
				
				this.msi.postToEndpoint("getStatusMessage", this.getJobIdData(), "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},
			
            execWaitForPercentComplete : function (percent, timeoutMsec, successCallback) {
				var myData = JSON.stringify ({
					"jobId" : this.jobId,
					"maxWaitMsec" : timeoutMsec,
					"percentComplete" : percent,
				});				
				
				this.msi.postToEndpoint("waitForPercentComplete", myData, "application/json", successCallback, this.optFailureCallback, timeoutMsec + 5000);
			},
            
            /*
             * New-fashioned callbacks get success values
             * otherwise failureCallback
             *
             */
            execGetPercentCompleteInt : function (percentCallback) {
                // Callback checks for success and gets a percent int before calling percentCallback
                var successCallback = function(percCallback, resultSet) {
                    if (resultSet.isSuccess()) {
                        var thisPercent = resultSet.getSimpleResultField("percentComplete");
                        if (thisPercent == null) {
                            this.doFailureCallback(resultSet, 
                                                    "Status service getPercentComplete did not return a percent.");
                        } else {
                            percCallback(parseInt(thisPercent));
                        } 
                    } else {
                        this.doFailureCallback(resultSet, null, fCallback);
                    }
                }.bind(this, percentCallback);
                
                this.execGetPercentComplete(successCallback);
            },
            
            execGetStatusBoolean :  function(booleanCallback) {
                
                var successCallback = function(successBoolCallback0, resultSet) {
                        
                    // job is finished
                    if (resultSet.isSuccess()) {
                        var status = resultSet.getSimpleResultField("status");

                        if ( status == null) {
                            this.doFailureCallback(resultSet, 
                                                   "Status service getStatus did not return a status.",
                                                   );
                        } else if (status == "Success") {
                            successBoolCallback0(true);
                        } else {
                            successBoolCallback0(false);
                        }
                    } else {
                        this.doFailureCallback(resultSet, null);
                    }
                }.bind(this, booleanCallback);
                
                this.execGetStatus(successCallback);
            },
			
            execJobStatusMessageString :  function(messageCallback) {
                
                var successCallback = function(messageCallback0, resultSet) {
                        
                    // job is finished
                    if (resultSet.isSuccess()) {
                        var message = resultSet.getSimpleResultField("statusMessage");

                        if ( message == null) {
                            this.doFailureCallback(resultSet, 
                                                   "Status service getStatusMessage did not return a statusMessage.",
                                                   );
                        } else {
                            messageCallback0(message);
                        }
                    } else {
                        this.doFailureCallback(resultSet);
                    }
                }.bind(this, messageCallback);
                
                this.execGetStatusMessage(successCallback);
            },
            
            /*===================================================*/
            execAsyncPercentUntilDone : function (jobSuccessCallback, statusBarCallback) {
                
                this.execGetPercentCompleteInt(this.execAsyncPercentCallback.bind( this,
                                                                                0,
                                                                                50,
                                                                                jobSuccessCallback,
                                                                                statusBarCallback
                                                                                ) );
            },
            
            /*
             * execAsync chain's percent complete loop
             * @private
             */
            execAsyncPercentCallback : function (lastPercent, timeout, jobSuccessCallback, statusBarCallback, thisPercent) {
                if (thisPercent > 99) {
                    
                    statusBarCallback(100);
                    this.execGetStatusBoolean(this.execAsyncStatusCallback.bind(this, jobSuccessCallback));

                } else {

                    statusBarCallback(lastPercent);

                    // if percent just changed, reset timeout 
                    var thisTimeout = (thisPercent == lastPercent) ? timeout : 50;
                    // next timeout should be 1.5 longer until we hit 5000
                    var nextTimeout = Math.min(Math.floor(thisTimeout * 1.5), 5000);

                    setTimeout( this.execGetPercentCompleteInt.bind(this, 
                                                                    this.execAsyncPercentCallback.bind( this, 
                                                                                                        thisPercent, 
                                                                                                        nextTimeout,
                                                                                                        jobSuccessCallback,
                                                                                                        statusBarCallback)
                                                                 ),
                                thisTimeout
                              );
                } 
            },
            
            /*
             * execAsync chain's status callback
             * @private
             */
            execAsyncStatusCallback : function (jobSuccessCallback, boolSuccess) {
                        
                if (boolSuccess) {
                    jobSuccessCallback();
                    
                } else {
                    // failed: get the message
                    this.execJobStatusMessageString(this.execAsyncMessageCallback.bind(this));
                }
            },
            
            execAsyncMessageCallback : function (statusMessage) {
                if (typeof this.optFailureCallback == "undefined") {
                    ModalIidx.alert(statusMessage);
                } else {
                    this.optFailureCallback(statusMessage);
                }
            },
                
            // Add a header and run failure callback
            // or override it with another failure callback
            doFailureCallback : function (resultSet, optHeader) {
                var html = (typeof optHeader == "undefined" || optHeader == null) ? "" : "<b>" + optHeader + "</b><hr>";
                html += resultSet.getFailureHtml();
                
                if (typeof this.optFailureCallback == "undefined") {
                    ModalIidx.alert("Status Service Failure", html);
                } else {
                    this.optFailureCallback(html);
                }
            },
                
            /*===================================================*/
            
			getFailedResultHtml : function (resultSet) {
				return resultSet.getGeneralResultHtml();
			},
			
			getSucceededPercentComplete : function (resultSet) {
				return resultSet.getSimpleResultField("percentComplete");
			},
			
			getStatusIsSuccess : function (resultSet) {
				return (resultSet.getSimpleResultField("status") == "Success");
			},
			
			getStatusMessageString : function (resultSet) {
				return resultSet.getSimpleResultField("statusMessage");
			},
                
		};
	
		return MsiClientStatus;            // return the constructor
	}
);