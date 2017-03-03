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
	
		
		var MsiClientOntologyInfo = function (serviceURL, jobId, optFailureCallback, optTimeout) {
			
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
			
			execGetPercentComplete : function (successCallback) {			
				
				this.msi.postToEndpoint("getPercentComplete", this.getJobIdData(), "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},
			
			execGetStatus : function (successCallback) {
				
				this.msi.postToEndpoint("getStatus", this.getJobIdData(), "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},
			
			execGetStatusMessage : function (successCallback) {
				
				this.msi.postToEndpoint("getStatusMessage", this.getJobIdData(), "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},
			
			execRetrieveDetailedOntologyInfo : function (dataset, domain, servertype, url) {
				var myData = JSON.stringify ({
					"dataset" : this.jobId,
					"domain" : timeoutMsec,
					"serverType" : percent,
					"url" : url,
				});				
				
				this.msi.postToEndpoint("getDetailedOntologyInfo", myData, "application/json", successCallback, this.optFailureCallback, timeoutMsec + 5000);
			},
			
			getRetrieveDetailedOntologyInfoSucceeded : function (resultSet) {
				// may return null
				return resultSet.getSimpleResultField("ontologyInfo");
			},
			getQueryFailedResultHtml : function (resultSet) {
				return resultSet.getGeneralResultHtml();
			},
		};
	
		return MsiClientStatus;            // return the constructor
	}
);