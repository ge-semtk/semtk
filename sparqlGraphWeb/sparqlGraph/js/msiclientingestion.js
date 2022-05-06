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


		var MsiClientIngestion = function (serviceURL, optFailureCallback, optTimeout) {

			this.msi = new MicroServiceInterface(serviceURL);
			this.optFailureCallback = optFailureCallback;
			this.optTimeout = optTimeout;
		};

        MsiClientIngestion.RECORDS_PROCESSED = "recordsProcessed";
        MsiClientIngestion.FAILURE_ROWS = "failuresEncountered"


		MsiClientIngestion.prototype = {
			execGetClassTemplate(uri, conn, idRegex, successJsonCallback) {
				var data = {}
				data.connection = JSON.stringify(conn.toJson());
				data.classURI = uri;
				if (idRegex && idRegex.length > 0) {
					data.idRegex = idRegex;
				}
				callback = function(msiRes) {
					successJsonCallback(msiRes.getSimpleResultField("sgjson"));
				}
				this.msi.postAndCheckSuccess("getClassTemplateAndCsv", JSON.stringify(data), "application/json", callback);

			},

			execFromCsvFilePrecheck : function (jsonFile, dataFile, successCallback) {
				var formdata = new FormData();
				formdata.append("template", jsonFile);
				formdata.append("data", dataFile);
				formdata.append("time", new Date());

				this.msi.postToEndpoint("fromCsvFilePrecheck", formdata, false, successCallback, this.optFailureCallback, this.optTimeout);
			},

            execFromCsvFilePrecheckAsync : function (jsonFile, dataFile, successCallback) {
				var formdata = new FormData();
				formdata.append("template", jsonFile);
				formdata.append("data", dataFile);
				formdata.append("time", new Date());
				this.msi.postToEndpoint("fromCsvFilePrecheckAsync", formdata, false, successCallback, this.optFailureCallback, this.optTimeout);
			},

			getFromCsvResultHtml : function (resultSet) {
				// call can return RecordProcessResults or GeneralResults
				if (resultSet.isRecordProcessResults()) {
					return resultSet.getRecordProcessResultHtml();

				} else {
					return resultSet.getGeneralResultHtml();
				}
			},

			getFromCsvErrorTable: function (resultSet) {
				// download the error table, or null
				return resultSet.getRecordProcessResultErrorCsv()
			},

		};

		return MsiClientIngestion;            // return the constructor
	}
);
