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

        MsiClientNodeGroupStore.TYPE_NODEGROUP = "PrefabNodeGroup";
        MsiClientNodeGroupStore.TYPE_REPORT = "Report";

		MsiClientNodeGroupStore.prototype = {
			deleteStoredItem : function (id, itemType, successCallback) {
				var data = JSON.stringify ({ "id": id, "itemType" : itemType });
				this.msi.postToEndpoint("deleteStoredItem", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

			getStoredItemById : function (id, itemType, successCallback) {
				var data = JSON.stringify ({ "id": id, "itemType" : itemType });
				this.msi.postToEndpoint("getStoredItemById", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

            getStoredItemByIdToStr : function (id, itemType, jsonStrCallback) {
                var successCallback = function(jsCallback, ngId, resultSet) {
                    if (! resultSet.isSuccess()) {
                        this.msi.userFailureCallback(resultSet.getGeneralResultHtml());
                    } else {
                        var nodegroupArr = resultSet.getStringResultsColumn("item");

                        if (nodegroupArr.length < 1) {
                             this.msi.userFailureCallback("<b>Failure retrieving nodegroup.</b><br>Nodegroup was not found: " + ngId);
                        } else {
                            jsCallback(nodegroupArr[0], id);
                        }
                    }
                }.bind(this, jsonStrCallback, id);

                this.getStoredItemById(id, itemType, successCallback);
            },

            getNodeGroupList : function (successCallback) {
				var data = JSON.stringify ({});
				this.msi.postToEndpoint("getNodeGroupList", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

            getStoredItemsMetadata : function (itemType, successCallback) {
				var data = JSON.stringify ({"itemType": itemType});
				this.msi.postToEndpoint("getStoredItemsMetadata", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

            getNodeGroupRuntimeConstraints : function (id, successCallback) {
				var data = JSON.stringify ({ "id": id });
				this.msi.postToEndpoint("getNodeGroupRuntimeConstraints", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

			storeItem : function (itemStr, creator, name, comments, itemType, successCallback) {
				var data = JSON.stringify ({
                      "creator" : creator,
					  "comments": comments,
					  "item" : itemStr,
					  "name": name,
                      "itemType" : itemType
					});

				this.msi.postToEndpoint("storeItem", data, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

		};

		return MsiClientNodeGroupStore;            // return the constructor
	}
);
