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
            generateAsk : function (nodegroup, successCallback)       { return this.execNodegroupOnly("generateAsk",       nodegroup, successCallback); },
            generateConstruct : function (nodegroup, successCallback) { return this.execNodegroupOnly("generateConstruct", nodegroup, successCallback); },
            generateCountAll : function (nodegroup, successCallback)  { return this.execNodegroupOnly("generateCountAll",  nodegroup, successCallback); },
            generateDelete : function (nodegroup, successCallback)    { return this.execNodegroupOnly("generateDelete",    nodegroup, successCallback); },
            generateSelect : function (nodegroup, successCallback)    { return this.execNodegroupOnly("generateSelect",    nodegroup, successCallback); },

            getSuccessSparql : function (resultSet) {
				return resultSet.getSimpleResultField("SparqlQuery");
			},
            
            getFailedResultHtml : function (resultSet) {
				return resultSet.getGeneralResultHtml();
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
			
		};
	
		return MsiClientNodeGroupService;            // return the constructor
	}
);