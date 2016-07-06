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
	
		
		var MsiClientDispatcher = function (serviceURL, optFailureCallback, optTimeout) {
			
			this.msi = new MicroServiceInterface(serviceURL);
			this.optFailureCallback = optFailureCallback;
			this.optTimeout = optTimeout;
			
		};
		
		
		MsiClientDispatcher.prototype = {
				
			
			execQueryFromNodegroup : function (conn, nodegroup, constraintSet, successCallback) {
				var nodeGroupString = JSON.stringify ({
					"sparqlConn" : conn.toJson(),
					"sNodeGroup" : nodegroup.toJson(),
				}).replace(/"/g, '\\"');
				
				var constraintSetStr = constraintSet ? JSON.stringify(constraintSet.toJson()).replace(/"/g, '\\"') : "";
				
				var myData = '{  "jsonRenderedNodeGroup" : "' + nodeGroupString + '",' +
				             '    "constraintSet" : "'        + constraintSetStr + '" }';
				
				this.msi.postToEndpoint("queryFromNodeGroup", myData, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},
			
			execGetConstraintInfo : function (conn, nodegroup, successCallback) {
				var nodeGroupString = JSON.stringify ({
					"sparqlConn" : conn.toJson(),
					"sNodeGroup" : nodegroup.toJson(),
				}).replace(/"/g, '\\"');
				
				var myData = '{  "jsonRenderedNodeGroup" : "' + nodeGroupString +  '" }';
				
				this.msi.postToEndpoint("getConstraintInfo", myData, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},
			
			getQueryFailedResultHtml : function (resultSet) {
				return resultSet.getGeneralResultHtml();
			},
			
			getQuerySucceededJobId : function (resultSet) {
				// may return null
				return resultSet.getSimpleResultField("requestID");
			},
		};
	
		return MsiClientDispatcher;            // return the constructor
	}
);