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

			// shimmed

		],

	function(MicroServiceInterface, MsiResultSet) {


		var MsiClientNodeGroupStore = function (serviceURL, optFailureCallback, optTimeout) {
			// optFailureCallback(html)
			this.optFailureCallback = optFailureCallback;
			this.optTimeout = optTimeout;
			this.msi = new MicroServiceInterface(serviceURL);
		};


		MsiClientNodeGroupStore.prototype = {
			deleteStoredNodeGroup : function (id, successCallback) {
				var data = JSON.stringify ({ "id": id });
				this.msi.postToEndpoint("deleteStoredNodeGroup", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

			getNodeGroupById : function (id, successCallback) {
				var data = JSON.stringify ({ "id": id });
				this.msi.postToEndpoint("getNodeGroupById", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

            getNodeGroupByIdToJsonStr : function (id, jsonStrCallback) {
                var successCallback = function(jsCallback, ngId, resultSet) {
                    if (! resultSet.isSuccess()) {
                        this.msi.userFailureCallback(resultSet.getGeneralResultHtml());
                    } else {
                        var nodegroupArr = resultSet.getStringResultsColumn("NodeGroup");

                        if (nodegroupArr.length < 1) {
                             this.msi.userFailureCallback("<b>Failure retrieving nodegroup.</b><br>Nodegroup was not found: " + ngId);
                        } else {
                            jsCallback(nodegroupArr[0], id);
                        }
                    }
                }.bind(this, jsonStrCallback, id);

                this.getNodeGroupById(id, successCallback);
            },

			getNodeGroupList : function (successCallback) {
				var data = JSON.stringify ({});
				this.msi.postToEndpoint("getNodeGroupList", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

            getNodeGroupList : function (successCallback) {
				var data = JSON.stringify ({});
				this.msi.postToEndpoint("getNodeGroupList", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

			getNodeGroupMetadata : function (successCallback) {
				var data = JSON.stringify ({});
				this.msi.postToEndpoint("getNodeGroupMetadata", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

            getNodeGroupRuntimeConstraints : function (id, successCallback) {
				var data = JSON.stringify ({ "id": id });
				this.msi.postToEndpoint("getNodeGroupRuntimeConstraints", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

			storeNodeGroup : function (sgJson, creator, name, comments, successCallback) {
				var data = JSON.stringify ({
                      "creator" : creator,
					  "comments": comments,
					  "jsonRenderedNodeGroup": JSON.stringify(sgJson.toJson()),
					  "name": name
					});
				this.msi.postToEndpoint("storeNodeGroup", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

		};

		return MsiClientNodeGroupStore;            // return the constructor
	}
);
