/**
 ** Copyright 2016-2018 General Electric Company
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


define([	// properly require.config'ed
    'sparqlgraph/js/modaliidx',
    'sparqlgraph/js/iidxhelper',
    'sparqlgraph/js/msiclientquery',
    'sparqlgraph/js/sparqlconnection',
    'jquery',

    // shimmed
    'sparqlgraph/js/belmont',
    'sparqlgraph/js/sparqlserverinterface'
    
    ],

	function(ModalIidx, IIDXHelper, MsiClientQuery, SparqlConnection, $) {

	
		var ModalConnWizardDialog= function (conn, serverTypeList, queryServiceURL, connCallback) {
            this.conn = conn;
            this.serverTypeList = serverTypeList;
            this.queryServiceURL = queryServiceURL;
            this.connCallback = connCallback;
            
            this.inputName = null;
            this.inputServerUrl = null;
            this.inputServerType = null;
            this.selectGraphModel = null;
            this.selectGraphDataIngest = null;
            this.selectGraphDataOther = null;
            
            this.grpName = null;
            this.grpServerUrl = null;
            this.grpServerType = null;
            this.grpGraphModel = null;
            this.grpGraphDataIngest = null;
            this.grpGraphDataOther = null;
		};


		ModalConnWizardDialog.prototype = {


			// Dual use:
			//   1. call after every change to highlight fields with errors
			//   2. return a message for the ModalIIDX dialog validation call
            validateCallback : function() {
				var msgHtml = "";
				// name
				if (this.inputName.value == "") {
					msgHtml += "<li>Name must not be empty</li>";
					IIDXHelper.changeControlGroupHelpText(this.grpName, "Name must not be empty", "warning");
				} else {
					IIDXHelper.changeControlGroupHelpText(this.grpName, "", "");
				}
				
				// no graphs means the server URL and Type have some issues
				if (this.selectGraphModel.options.length == 0) {
					msgHtml += "<li>Server URL and/or type are not correct</li>";
					IIDXHelper.changeControlGroupHelpText(this.grpServerUrl, "e.g. http://localhost:3030/MYDATASET", "warning");
					IIDXHelper.changeControlGroupHelpText(this.grpServerType, "Invalid triplestore connection", "warning");
				} else {
					IIDXHelper.changeControlGroupHelpText(this.grpServerUrl, "", "");
					IIDXHelper.changeControlGroupHelpText(this.grpServerType, "", "");
				}
				
				// model graph
				if (IIDXHelper.getSelectValues(this.selectGraphModel).length == 0) {
					msgHtml += "<li>Select at least one model graph</li>";
					IIDXHelper.changeControlGroupHelpText(this.grpGraphModel, "Select at least one", "warning");
				} else {
					IIDXHelper.changeControlGroupHelpText(this.grpGraphModel, "", "");
				}
				
				// main data graph
				if (IIDXHelper.getSelectValues(this.selectGraphDataIngest).length == 0) {
					msgHtml += "<li>Select one main data graph</li>";
					IIDXHelper.changeControlGroupHelpText(this.grpGraphDataIngest, "Select exactly one", "warning");
				} else {
					IIDXHelper.changeControlGroupHelpText(this.grpGraphDataIngest, "", "");
				}
				
				return msgHtml != "" ? "Fix connection errors before continuing." : null;
			},

			okCallback : function() {
				// valdation callback has made sure everything is correct
				// perform the callback
				
				var newConn = new SparqlConnection();
				newConn.setName(this.inputName.value);
				
				// model endpoint
                for (var graph of IIDXHelper.getSelectValues(this.selectGraphModel)) {
					newConn.addModelInterface(this.inputServerType.value, this.inputServerUrl.value, graph);
				}
				
				// data ingest endpoint
				var dataGraph0 = IIDXHelper.getSelectValues(this.selectGraphDataIngest)[0];
				newConn.addDataInterface(this.inputServerType.value, this.inputServerUrl.value, dataGraph0);
				
				// other data endpoints
				for (var graph of IIDXHelper.getSelectValues(this.selectGraphDataOther)) {
					if (graph != dataGraph0) {
						newConn.addDataInterface(this.inputServerType.value, this.inputServerUrl.value, graph);
					}
				}
				
				// the callback
				this.connCallback(newConn);
			},

			cancelCallback : function() {

            },
            
            serverChangedCallback : function() {
				var queryClient = new MsiClientQuery(this.queryServiceURL, new SparqlServerInterface(this.inputServerType.value, this.inputServerUrl.value, "http://any#graph"));
				queryClient.execQuery("SELECT ?g WHERE { GRAPH ?g { }}", this.graphQueryCallback.bind(this));
			},
			
			graphQueryCallback : function(results) {
				if (!results.isSuccess()) {
					// Let's not display the dialog
					// ModalIidx.alert("Error retrieving graphs", "Server URL and/or type are likely invalid.<hr><b>Message:</b><br>" + results.getStatusMessage());
					
					for (var s of [this.selectGraphModel, this.selectGraphDataIngest, this.selectGraphDataOther]) {
						IIDXHelper.removeAllOptions(s);
					}
					
				} else {
					var graphs = results.getColumnStrings(0).sort();
					for (var s of [this.selectGraphModel, this.selectGraphDataIngest, this.selectGraphDataOther]) {
						IIDXHelper.removeAllOptions(s);
						IIDXHelper.addOptions(s, graphs, []);
					}
				}
				
				this.validateCallback();   
			},

            
            /**
              *  Call query to get all graphs
              *  Then launch dialog
              */
            launch : function () {
				this.div = document.createElement("div");
				var form = IIDXHelper.buildHorizontalForm();
				this.div.appendChild(form);
				
				var sei = (this.conn != null && this.conn.getModelInterfaceCount() > 0) ? this.conn.getModelInterface(0) : null;
				
    			var fieldset = IIDXHelper.addFieldset(form)
    			this.inputName = IIDXHelper.createTextInput("mcwName");
    			this.inputName.onchange = this.validateCallback.bind(this);
    			this.grpName = IIDXHelper.buildControlGroup("Name: ", this.inputName);
    			fieldset.appendChild(this.grpName);
    			
    			this.inputServerUrl = IIDXHelper.createTextInput("mcwServer");
    			this.inputServerUrl.value = (sei != null) ? sei.getServerURL() : "";
    			this.inputServerUrl.onchange = this.serverChangedCallback.bind(this);
    			this.grpServerUrl = IIDXHelper.buildControlGroup("Server URL: ", this.inputServerUrl);
    			fieldset.appendChild(this.grpServerUrl);
    			
    			this.inputServerType = IIDXHelper.createSelect("mcwType", this.serverTypeList, []);
    			this.inputServerType.onchange = this.serverChangedCallback.bind(this);
    			IIDXHelper.selectFirstMatchingText(this.inputServerType, sei != null ? sei.getServerType() : "fuseki");
    			this.grpServerType = IIDXHelper.buildControlGroup("Type: ", this.inputServerType);
    			fieldset.appendChild(this.grpServerType);
    			
    			var table = document.createElement("table");
    			table.width="100%";
    			this.div.appendChild(table);
    			var tbody = document.createElement("tbody");
    			table.appendChild(tbody);
    			var tr = document.createElement("tr");
    			tbody.appendChild(tr);
    			var col1 = document.createElement("td");
    			col1.width="33%";
    			tr.appendChild(col1);
    			this.selectGraphModel = IIDXHelper.createSelect("mcwSelectModel", [], [], true);
    			this.selectGraphModel.onchange = this.validateCallback.bind(this);
    			this.selectGraphModel.size = 8;
    			this.selectGraphModel.style.width="100%";
    			this.selectGraphModel.style.overflow="auto";
    			this.grpGraphModel = IIDXHelper.buildControlGroup("Model graph(s):", this.selectGraphModel);
    			col1.appendChild(this.grpGraphModel);
    			
    			var col2 = document.createElement("td");
    			col2.width="33%";
    			tr.appendChild(col2);
    			this.selectGraphDataIngest = IIDXHelper.createSelect("mcwSelectDataIngest", [], [], false);
    			this.selectGraphDataIngest.onchange = this.validateCallback.bind(this);
    			this.selectGraphDataIngest.size = 8;
    			this.selectGraphDataIngest.style.width="100%";
    			this.selectGraphDataIngest.style.overflow="auto";
    			this.grpGraphDataIngest = IIDXHelper.buildControlGroup("Main data graph:", this.selectGraphDataIngest);
    			col2.appendChild(this.grpGraphDataIngest);
    			
    			var col3 = document.createElement("td");
    			tr.appendChild(col3);
    			col3.width="33%";
    			this.selectGraphDataOther = IIDXHelper.createSelect("mcwSelectDataOther", [], [], true);
    			this.selectGraphDataOther.onchange = this.validateCallback.bind(this);
    			this.selectGraphDataOther.size = 8
    			this.selectGraphDataOther.style.width="100%";
    			this.selectGraphDataOther.style.overflow="auto";
    			this.grpGraphDataOther = IIDXHelper.buildControlGroup("Additional data graph(s):", this.selectGraphDataOther);
    			col3.appendChild(this.grpGraphDataOther);
			
				if (sei != null) {
					this.serverChangedCallback();
				}
                // launch the modal
                var m = new ModalIidx();
                m.showOKCancel(
                                "Build a Connection",
                                this.div,
                                this.validateCallback.bind(this),
                                this.okCallback.bind(this),
                                this.cancelCallback.bind(this),
                                "OK",
                                65);
                                
                this.validateCallback();

            },


		};

		return ModalConnWizardDialog;
	}
);
