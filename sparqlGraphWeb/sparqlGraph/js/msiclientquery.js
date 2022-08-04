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


		var MsiClientQuery = function (serviceURL, ssinterface, optFailureCallback, optTimeout) {
			// optFailureCallback(html)
			this.optFailureCallback = optFailureCallback;
			this.optTimeout = optTimeout;
			this.ssinterface = ssinterface;
			this.msi = new MicroServiceInterface(serviceURL);

			this.data = {};
			this.data.serverAndPort = ssinterface.serverURL;
			this.data.serverType = ssinterface.serverType;
			this.data.graph = ssinterface.graph;
			this.data.user = "dba";
			this.data.password = "dba";

		};


		MsiClientQuery.prototype = {

			execQuery : function (query, successCallback, optReturnType) {
				// BUG: this inexplicably has been "returnType", which doesn't work with the query service
				this.data.resultType = typeof optReturnType === "undefined" ? "TABLE" : optReturnType;;

				this.data.query = query;
				var myData = JSON.stringify(this.data);
				delete this.data.query;
				delete this.data.resultType;

				this.msi.postToEndpoint("query", myData, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

			execAuthQuery : function (query, successCallback, optReturnType) {
				// BUG: this inexplicably has been "returnType", which doesn't work with the query service
				this.data.resultType = typeof optReturnType === "undefined" ? "TABLE" : optReturnType;;

				this.data.query = query;
				var myData = JSON.stringify(this.data);
				delete this.data.query;
				delete this.data.resultType;

				this.msi.postToEndpoint("queryAuth", myData, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

			execSelectGraphNames : function (successCallback) {
				var myData = JSON.stringify(this.data);
				this.msi.postToEndpoint("selectGraphNames", myData, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},
			
			execDropGraph : function (successCallback) {
				var myData = JSON.stringify(this.data);
				this.msi.postToEndpoint("dropGraph", myData, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

			execClearAll : function (successCallback) {
				var myData = JSON.stringify(this.data);
				this.msi.postToEndpoint("clearAll", myData, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

			execClearPrefix : function (prefix, successCallback) {
				this.data.prefix = prefix;
				var myData = JSON.stringify(this.data);
				delete this.data.prefix;

				this.msi.postToEndpoint("clearPrefix", myData, "application/json", successCallback, this.optFailureCallback, this.optTimeout);
			},

			execUploadOwl : function (owlFile, successCallback) {
				var formdata = new FormData();
				formdata.append("serverAndPort", this.data.serverAndPort);
				formdata.append("serverType",    this.data.serverType);
				formdata.append("dataset",       this.data.graph);
				formdata.append("user",          this.data.user);
				formdata.append("password",      this.data.password);
				formdata.append("owlFile",       owlFile);

				this.msi.postToEndpoint("uploadOwl", formdata, MicroServiceInterface.FORM_CONTENT, successCallback, this.optFailureCallback, this.optTimeout);
			},

            execUploadTurtle : function (ttlFile, successCallback) {
				var formdata = new FormData();
				formdata.append("serverAndPort", this.data.serverAndPort);
				formdata.append("serverType",    this.data.serverType);
				formdata.append("graph",       this.data.graph);
				formdata.append("user",          this.data.user);
				formdata.append("password",      this.data.password);
				formdata.append("ttlFile",        ttlFile);

				this.msi.postToEndpoint("uploadTurtle", formdata, MicroServiceInterface.FORM_CONTENT, successCallback, this.optFailureCallback, this.optTimeout);
			},

            execSyncOwl : function (owlFile, successCallback) {
				var formdata = new FormData();
				formdata.append("serverAndPort", this.data.serverAndPort);
				formdata.append("serverType",    this.data.serverType);
				formdata.append("user",          this.data.user);
				formdata.append("password",      this.data.password);
				formdata.append("owlFile",       owlFile);

				this.msi.postToEndpoint("syncOwl", formdata, MicroServiceInterface.FORM_CONTENT, successCallback, this.optFailureCallback, this.optTimeout);
			},

			// get the success message
			getSuccessMessageHTML : function (resultSet) {
				var ret =  resultSet.getSimpleResultField("@message");
				if (ret === null) {
					ret = "Operation successful.";
				}
				// un-HTML it
				ret = ret.replace(/</g, '&lt;');
				ret = ret.replace(/>/g, '&gt;');

				return ret;
			},

			getDropGraphResultHtml : function (resultSet) {
				return resultSet.getGeneralResultHtml();
			},

			/*
			 *  For compatibility with the "old" sparqlgraph sparqlserverinterface, etc.
			 */

			executeAndParse : function (sparql, callback) {
				// callback overrides constructor successCallback.
				//          callback(msiresultset) is called no matter what happens
				//          so msiresultset must behave the same as sparqlServerResult
				//             isSuccess()
				//             getRowCount()
				//             getRsData(row, col, nsFlag)
				//             getStatusMessage()

				this.data.resultType = "TABLE";
				this.data.query = sparql;
				var myData = JSON.stringify(this.data);
				delete this.data.query;
				delete this.data.resultType;

				// TODO: second callback is wrong because parameter for failing is different
				this.msi.postToEndpoint("query", myData, "application/json", callback, this.optFailureCallback, this.optTimeout);
			},


		};

		return MsiClientQuery;            // return the constructor
	}
);
