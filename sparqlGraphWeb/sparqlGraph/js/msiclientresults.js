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
	
		
		var MsiClientResults = function (serviceURL, jobId, optFailureCallback, optTimeout) {
			
			this.msi = new MicroServiceInterface(serviceURL);
			this.jobId = jobId;
			this.optFailureCallback = optFailureCallback;
			this.optTimeout = optTimeout;
			
		};
		
		
		MsiClientResults.prototype = {
				
			getJobIdData : function () {
				return JSON.stringify ({
					"jobId" : this.jobId,
				});	
			},
			
			execGetResults : function (successCallback) {			
				this.msi.postToEndpoint("getResults", this.getJobIdData(), "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},
			
			getResultsSampleUrl : function (resultSet) {
				return resultSet.getSimpleResultField("sampleURL");
			},
			
			getResultsFullUrl : function (resultSet) {
				return resultSet.getSimpleResultField("fullURL");
			},
			
			getFailedResultHtml : function (resultSet) {
				return resultSet.getGeneralResultHtml();
			},
		};
	
		return MsiClientResults;            // return the constructor
	}
);