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
	
		
		var MsiClientResults = function (serviceURL, jobId, optFailureCallback, optTimeout) {
			
			this.msi = new MicroServiceInterface(serviceURL);
			this.jobId = jobId;
			this.optFailureCallback = optFailureCallback;
			this.optTimeout = optTimeout;
			
		};
		
		
		MsiClientResults.prototype = {
				
			getJobIdDataStr : function () {
				return JSON.stringify ({
					"jobId" : this.jobId,
				});	
			},
            
            getJobIdData : function () {
				return {
					"jobId" : this.jobId,
				};	
			},
			
			execGetResults : function (successCallback) {			
				this.msi.postToEndpoint("getResults", this.getJobIdDataStr(), "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},
			
            execGetTableResultsJson : function (maxRows, successCallback) {
                var data = this.getJobIdData();
                
                if (maxRows != null && maxRows > 0) {
                    data.maxRows = maxRows;
                }
                    
                this.msi.postToEndpoint("getTableResultsJson", JSON.stringify(data), "application/json", successCallback, this.optFailureCallback, this.optTimeout);

            },
            
            /* downloadFlag won't work in Ajax */
             execGetTableResultsCsv : function (maxRows, downloadFlag, successCallback) {
                var data = this.getJobIdData();
                
                if (maxRows != null && maxRows > 0) {
                    data.maxRows = maxRows;
                }
                
                if (downloadFlag) {
                    data.appendDownloadHeaders = true;
                }
                    
                this.msi.postToEndpoint("getTableResultsCsv", SON.stringify(data), "application/json", successCallback, this.optFailureCallback, this.optTimeout);

            },
            
            /*
             * tableResCallback(tableResults)
             */
            execGetTableResultsJsonTableRes : function (maxRows, tableResCallback) {
                
                var successCallback = function(tableCallback0, resultSet) {
                        
                    if (resultSet.isSuccess()) {
                        var table = resultSet.getTable();

                        if ( table == null) {
                            this.doFailureCallback(resultSet, 
                                                   "Results service getTableResultsJson did not return a table.",
                                                   );
                        } else {
                            tableCallback0(resultSet);
                        }
                    } else {
                        this.doFailureCallback(resultSet);
                    }
                }.bind(this, tableResCallback);
                
                this.execGetTableResultsJson(maxRows, successCallback);
            },
            
            /*
             * return a URL where CSV file can be found
             */
            getTableResultsCsvDownloadUrl : function (optMaxRows) {
                var ret =  this.msi.url + "getTableResultsCsvForWebClient?jobId=" + this.jobId;
                
                if (typeof optMaxRows != "undefined") {
                    ret += "&maxRows=" + optMaxRows;
                }
                return ret;
            },
            
            
			getFailedResultHtml : function (resultSet) {
				return resultSet.getGeneralResultHtml();
			},
            
            // Add a header and run failure callback
            // or override it with another failure callback
            doFailureCallback : function (resultSet, optHeader) {
                var html = (typeof optHeader == "undefined" || optHeader == null) ? "" : "<b>" + optHeader + "</b><hr>";
                html += resultSet.getFailureHtml();
                
                if (typeof this.optFailureCallback == "undefined") {
                    ModalIidx.alert("Results Service Failure", html);
                } else {
                    this.optFailureCallback(html);
                }
            },
            
            /* soon to be deprecated */
			getResultsSampleUrl : function (resultSet) {
				return resultSet.getSimpleResultField("sampleURL");
			},
			
			getResultsFullUrl : function (resultSet) {
				return resultSet.getSimpleResultField("fullURL");
			},
            
			
		};
	
		return MsiClientResults;            // return the constructor
	}
);