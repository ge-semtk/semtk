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



define([	// properly require.config'ed
         	'sparqlgraph/js/modaliidx',
         	'sparqlgraph/js/iidxhelper',
         	'sparqlgraph/js/sparqlgraphjson',
         	'sparqlgraph/js/msiclientingestion',
         	'sparqlgraph/js/msiclientquery',
         	'sparqlgraph/js/sparqlgraphjson',
         	'jquery',

			// shimmed
         	'sparqlgraph/js/belmont',
         	//'logconfig',
		],

	function(ModalIidx, IIDXHelper, SparqlGraphJson, MsiClientIngestion, MsiClientQuery, SparqlGraphJson, $) {
	
		/*
		 *    A column name or text or some item used to build a triple value
		 */
		var UploadTab = function (parentDiv, toolsDiv, reloadCallback, ingestionServiceURL, sparqlQueryServiceURL) {
			 this.parentDiv = parentDiv;
			 this.toolsDiv = toolsDiv;
			 this.reloadCallback = reloadCallback;
			 this.ingestionServiceURL = ingestionServiceURL;
			 this.sparqlQueryServiceURL = sparqlQueryServiceURL;
			 this.fileDiv = null;
			 this.button = null;
			 
			 this.jsonFile = null;
			 this.nodeGroupName = null;   
			 this.conn = null;
			 this.dataFile = null;
	
			 this.nodegroup = null;
			 this.inputURI = null;
			 this.owlFile = null;
			 
			 this.progressDiv = null;
			 
			 this.modelChangedFlag = false;
			 this.oInfoLoadTime = "";
			 
			 this.suggestedPrefix = null;
		};
		
		UploadTab.VAR = "var";
		
		UploadTab.prototype = {
			
			/**
			 * Draw if needed.  Fill in all values.
			 */
			draw : function () {
				if (this.toolsDiv.innerHTML.indexOf("<") == -1) {
					this.drawTools();
				}
				
				// draw body only if it's never been done before
				if (this.parentDiv.innerHTML.indexOf("<") == -1) {
					this.drawBody();
				}
				this.fillAll();
			},
			
			/**
			 * Draw the import section
			 */
			drawBody : function () {
				
				this.parentDiv.innerHTML = "";
				
				// title
				var p = document.createElement("p");
				p.innerHTML = "Import CSV into a nodegroup's data endpoint:";
				this.parentDiv.appendChild(p);
				
				// table
				var table = document.createElement("table");
				table.classList.add("table");
				
				// set column widths
				col = document.createElement("col");
				col.style.width = "50%";
				table.appendChild(col);
				
				col = document.createElement("col");
				col.style.width = "50%";
				table.appendChild(col);
				
				var tr;
				var td;
				
				// --- json row ---
				tr = document.createElement("tr");
				
				// left: json dropzone
				td = document.createElement("td");
				var jsonDropElem = IIDXHelper.createDropzone( 	"icon-cogs",
																"Drop Nodegroup Query Json",
																this.isDroppableJson.bind(this),
																this.ondropJson.bind(this));
				jsonDropElem.id = "jsonDropZone";
				td.appendChild(jsonDropElem);
				tr.appendChild(td);
				
				// right cell: csv dropzone
				td = document.createElement("td");
				
				td = document.createElement("td");
				var fileDropElem = IIDXHelper.createDropzone( 	"icon-table",
																"Drop CSV file",
																this.isDroppableFile.bind(this),
																this.ondropData.bind(this));
				fileDropElem.id = "fileDropZone";
				td.appendChild(fileDropElem);
				
				tr.appendChild(td);
				table.appendChild(tr);
				
				
				
				// put table on parentDiv
				this.parentDiv.appendChild(table);
				
				var buttonDiv = document.createElement("div");
				buttonDiv.classList.add("form-actions");
				buttonDiv.align = "right";
				buttonDiv.style = "padding-top: 0.5em; padding-bottom: 0.5em; margin-top: 0px; margin-bottom: 0px;";
				
				// --- button ---
				this.button = document.createElement("button");
				this.button.classList.add("btn");
				this.button.classList.add("btn-primary");
				this.button.innerHTML = "Import";
				this.button.onclick = function (e) {e.preventDefault(); this.callFromCsvFile(e); return false; }.bind(this);
				buttonDiv.appendChild(this.button);
				
				this.parentDiv.appendChild(buttonDiv);
				
				this.parentDiv.appendChild(document.createElement("br"));
				this.progressDiv = document.createElement("div");
				this.parentDiv.appendChild(this.progressDiv);
				
				// try to stop the default Firefox drop
				this.parentDiv.ondragover = function (e) { e.stopPropagation(); e.preventDefault();};
				this.parentDiv.ondrop     = function (e) { e.stopPropagation(); e.preventDefault();};
			},
		
			
			fillAll : function () {
				this.fillBody();
				this.fillTools();
			},
			
			/**
			 * Fill in values in the import section
			 */
			fillBody : function () {
				var readyFlag = true;
				var msgTxt;
				var dropZone;
				
				// csv file
				dropZone = document.getElementById("fileDropZone");
				if (this.dataFile) {
					msgTxt = this.dataFile.name + " (" + Number(this.dataFile.size / 1024).toFixed(1) + " KB)";
					IIDXHelper.setDropzoneLabel(dropZone, msgTxt, IIDXHelper.DROPZONE_FULL);
				} else {
					msgTxt = "Drop CSV file";
					IIDXHelper.setDropzoneLabel(dropZone, msgTxt, IIDXHelper.DROPZONE_EMPTY);
					readyFlag = false;
				}
				
				
				// nodegroup json
				dropZone = document.getElementById("jsonDropZone");
				if (this.jsonFile == null) {
					msgTxt = "Drop Nodegroup Query Json";
					IIDXHelper.setDropzoneLabel(dropZone, msgTxt, IIDXHelper.DROPZONE_EMPTY);

					readyFlag = false;
				
				} else if (this.jsonFile.hasOwnProperty(name)) {
					msgTxt = this.jsonFile.name + " (" + Number(this.jsonFile.size / 1024).toFixed(1) + " KB)";
					IIDXHelper.setDropzoneLabel(dropZone, msgTxt, IIDXHelper.DROPZONE_FULL);
					
				// display node group
				} else {
					msgTxt = "Nodegroup from: " + this.nodeGroupName;
					IIDXHelper.setDropzoneLabel(dropZone, msgTxt, IIDXHelper.DROPZONE_FULL);
				}
				
				this.button.disabled = (! readyFlag);
				
			},
			
			/**
			 * Draw the tools section
			 */
			drawTools : function() {
				// --- button ---
				var table = document.createElement("table");
				table.classList.add("table");
				//table.border = "1";
				var tr;
				var td1;
				var td2;
				var col;
				var button;
				var select;
				var option;
				var input;
				var controlGroupDiv;
				var div;
				
				// set column widths
				col = document.createElement("col");
				col.style.width = "50%";
				table.appendChild(col);
				
				col = document.createElement("col");
				col.style.width = "50%";
				table.appendChild(col);
				
				
				// ===== clear model row =====
				tr = document.createElement("tr");
				
				td1 = document.createElement("td");
				td1.id = "tdChooseDataset1";
				tr.appendChild(td1);
				
				td2 = document.createElement("td");
				tr.appendChild(td2);
				select = document.createElement("select");
				select.id="selectChooseDataset";
				option = document.createElement("option");
				option.text = "Model Dataset";
				option.value = "model";
				select.add(option);
				option = document.createElement("option");
				option.text = "Instance Dataset";
				option.value = "data";
				select.add(option);
				td2.appendChild(select);
				select.onchange = this.fillTools.bind(this);
				table.appendChild(tr);
				
				
				// ===== clear data row =====
				tr = document.createElement("tr");
				
				td1 = document.createElement("td");
				td1.id = "tdClearGraph1";
				tr.appendChild(td1);
				
				// button cell
				td2 = document.createElement("td");
				button = document.createElement("button");
				button.id = "butClearGraph";
				button.classList.add("btn");
				button.innerHTML = "Clear Graph";
				button.onclick = this.toolsClearGraph.bind(this);
				td2.appendChild(button);
				tr.appendChild(td2);
				table.appendChild(tr);
				
				// ===== clear prefix row =====
				tr = document.createElement("tr");
				td1 = document.createElement("td");
				td1.id = "tdClearPrefix1";
				
				td1.appendChild(document.createTextNode("URI prefix: "));
				td1.appendChild(document.createElement("br"));
				input = document.createElement("input");
				input.id = "inputClearPrefix";
				input.type = "text";
				input.classList.add("input-xlarge");
				input.style = "width: 100%; padding-left: 0px; padding-right: 0px; ";
				input.value = "";
				input.oninput = this.fillClearPrefix.bind(this);
				td1.appendChild(input);
				tr.appendChild(td1);
				
				// button cell
				td2 = document.createElement("td");
				button = document.createElement("button");
				button.id = "butClearPrefix";
				button.classList.add("btn");
				button.innerHTML = "Clear Prefix";
				button.onclick = this.toolsClearPrefix.bind(this);
				td2.appendChild(button);
				tr.appendChild(td2);
				table.appendChild(tr);
				
				// ===== upload owl row ===== 
				tr = document.createElement("tr");
				td1 = document.createElement("td");
				td1.id = "tdUploadOwl1";
				td1.appendChild(IIDXHelper.createDropzone("icon-sitemap", "Drop OWL file", function(e) {return true;}, this.toolsDropOwlFile.bind(this)));
				tr.appendChild(td1);
				
				// button cell
				td2 = document.createElement("td");
				button = document.createElement("button");
				button.id = "butUploadOwl";
				button.classList.add("btn");
				button.innerHTML = "Upload Owl";
				button.onclick = this.toolsUploadOwl.bind(this);
				td2.appendChild(button);
				tr.appendChild(td2);
				table.appendChild(tr);		
				
				this.toolsDiv.appendChild(table);
				
				// status div
				div = document.createElement("div");
				div.id = "toolsStatusDiv";
				this.toolsDiv.appendChild(div);
			},
			
			/**
			 * fillTools for just "clear prefix" so that it can also be used as oninput() callback for the input
			 */
			fillClearPrefix : function () {
				var connFlag = (this.conn != null);
				var prefix = document.getElementById("inputClearPrefix").value.trim();
				
				// weird: revert any empty prefix to the suggestion
				if (prefix.length < 1) {
					if (this.suggestedPrefix != null) {
						document.getElementById("inputClearPrefix").value = this.suggestedPrefix.trim();
						prefix = this.suggestedPrefix.trim();
					}
				}
				
				// enable or disable button
				if (connFlag && prefix.length > 0) {
					document.getElementById("butClearPrefix").disabled = false;
				} else {
					document.getElementById("butClearPrefix").disabled = true;
				}
			},
			
			/**
			 * Fill in values in the tools section
			 */
			fillTools : function () {
				var connFlag = (this.conn != null);
				
				// choose dataset
				document.getElementById("tdChooseDataset1").innerHTML = "<b>Server: </b>" + this.getGraphServerUrl() + "<br>" +
																		"<b>Dataset: </b>" + this.getGraphDataset();
					
				// clear graph
				document.getElementById("butClearGraph").disabled = (! connFlag);
				
				// clear prefix
				this.fillClearPrefix();
				
				// upload owl
				var dropzone = document.getElementById("tdUploadOwl1").childNodes[0];
				var msgTxt = "";
				if (this.owlFile !== null) {
					document.getElementById("butUploadOwl").disabled = false;
					msgTxt = this.owlFile.name + " (" + Number(this.owlFile.size / 1024).toFixed(1) + " KB)";
					IIDXHelper.setDropzoneLabel(dropzone, msgTxt, IIDXHelper.DROPZONE_FULL);
					
				} else {
					document.getElementById("butUploadOwl").disabled = true;
					msgTxt = "Drop OWL file";
					IIDXHelper.setDropzoneLabel(dropzone, msgTxt, IIDXHelper.DROPZONE_EMPTY);
				}
				
				// status div
				var statusDiv = document.getElementById("toolsStatusDiv");
				if (!connFlag) {
					// warn there's no connection
					statusDiv.classList.add("alert");
					statusDiv.innerHTML = "<strong>No nodegroup is loaded: </strong><br>Use file->load or drag a JSON nodegroup onto the query tab.";
					
				} else if (this.modelChangedFlag) {
					// warn that model has changed
					
					// table
					var table = document.createElement("table");
					table.style.width="100%";
					var tr = document.createElement("tr");
					var td1 = document.createElement("td");
					var td2 = document.createElement("td");
					tr.appendChild(td1);
					tr.appendChild(td2);
					table.appendChild(tr);
					
					// left cell
					td1.innerHTML = "<strong>Model dataset has been changed: </strong><br>Reloading will discard any unsaved nodegroup and mapping. ";

					// right cell
					var button = document.createElement("button");
					button.classList.add("btn");
					button.innerHTML = '<i class="icon-retweet"></i> Reload';
					button.onclick = function(e) { this.reloadCallback(this.conn); }.bind(this);
					
					var buttonDiv = document.createElement("div");
					buttonDiv.align = "right"
					buttonDiv.appendChild(button);
					td2.appendChild(buttonDiv);
					
					// status Div
					statusDiv.classList.add("alert");
					statusDiv.classList.add("alert-error");
					statusDiv.innerHTML = "";
					statusDiv.appendChild(table);
					
				} else {
					// remove all warnings
					statusDiv.innerHTML = "";
					statusDiv.classList.remove("alert");
					statusDiv.classList.remove("alert-error");

				}
			},
			
			/**
			 * Get server URL for the selected connection (data or model)
			 */
			getGraphServerUrl : function () {
				var ret = "";
				
				if (this.conn == null) {
					ret = "";
				} else if (document.getElementById("selectChooseDataset").value === "data") {
					ret = this.conn.dataServerUrl;
				} else {
					ret = this.conn.ontologyServerUrl;
				}
				return ret;
			},
			
			/**
			 * Get dataset for the selected connection (data or model)
			 */
			getGraphDataset : function () {
				var ret = "";
				
				if (this.conn == null) {
					ret = "";
				} else if (document.getElementById("selectChooseDataset").value === "data") {
					ret = this.conn.dataSourceDataset;
				} else {
					ret = this.conn.ontologySourceDataset;
				}
				return ret;
			},
			
			/**
			 * Is the model dataset selecte   OR  data is selected but it is identical
			 */
			isModelDatasetSelected : function () {
				// "model" is selected
				if ( document.getElementById("selectChooseDataset").value === "model" ) {
					return true;
				// "model" and "data" are actually the same
				} else if (this.conn.dataServerUrl === this.conn.ontologyServerUrl && this.conn.dataSourceDataset === this.conn.ontologySourceDataset) {
					return true;
				} else {
					return false;
				}
			},
			
			/**
			 * Get connection server interface for the selected connection (data or model)
			 */
			getGraphInterface : function () {
				var ret = null;
				if (this.conn == null) {
					ret = null;
				} else if (document.getElementById("selectChooseDataset").value === "data") {
					ret = this.conn.getDataInterface();
				} else {
					ret = this.conn.getOntologyInterface();
				}
				return ret;
			},
			
			/**
			 * Clear prefix button
			 */
			toolsClearPrefix : function () {
				
				var prefix = document.getElementById("inputClearPrefix").value;
				var targetHTML = "<b>Server: </b>" + this.getGraphServerUrl() + "<br>";
				targetHTML += "<b>Dataset: </b> "+ this.getGraphDataset()+ "<br>";
				targetHTML += "<b>URI Prefix:</b>" + prefix;
				ModalIidx.okCancel("Confirm", 
						           "Are you sure you want to clear these triples: <br><br>" + targetHTML, 
						            this.toolsClearPrefixOK.bind(this));
			},
			
			/**
			 * Clear prefix button after confirmation dialog
			 */
			toolsClearPrefixOK : function () {
				var prefix = document.getElementById("inputClearPrefix").value;

				kdlLogEvent("SG: import clear prefix", "prefix", prefix);
				
				var successCallback = function (mq, resultSet) { 
					IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
					
					if (! resultSet.isSuccess()) {
						this.logAndAlert("'Clear Prefix' Service failed", mq.getDropGraphResultHtml(resultSet));
					} else {
						
						ModalIidx.alert("Success", mq.getSuccessMessageHTML(resultSet));

						// set modelChangedFlag
						if (this.isModelDatasetSelected()) {
							this.modelChangedFlag = true;
							this.draw();
						}
					}
					
					this.fillAll();
					IIDXHelper.progressBarRemove(this.progressDiv);
				};
				
				IIDXHelper.progressBarCreate(this.progressDiv, "progress-danger progress-striped active");
				IIDXHelper.progressBarSetPercent(this.progressDiv, 50);
				
				var mq = new MsiClientQuery(this.sparqlQueryServiceURL, this.getGraphInterface(), this.msiFailureCallback.bind(this));
	    		mq.execClearPrefix(prefix, successCallback.bind(this, mq));
				
			},
			
			/**
			 * Owl drop callback
			 */
			toolsDropOwlFile : function (ev) {
				this.owlFile = null;
				
				if (ev.dataTransfer.files.length != 1) {
					this.logAndAlert("Error dropping OWL file", "Only single file drops are supported.");
				} else if (ev.dataTransfer.files[0].name.slice(-4).toLowerCase() != ".owl") {
					this.logAndAlert("Error dropping OWL file", "Only OWL file drops are supported.");
				} else {
					this.readSingleOwlAsync(ev.dataTransfer.files[0]);
				}
			},
			
			/**
			 * Upload owl button callback
			 */
			toolsUploadOwl : function() {
				kdlLogEvent("SG: import upload owl", "filename", this.owlFile);     
				
				var successCallback = function (mq, resultSet) { 
					IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
					
					if (! resultSet.isSuccess()) {
						this.logAndAlert("'Upload owl' Service failed", mq.getDropGraphResultHtml(resultSet));
					} else {
						
						ModalIidx.alert("Success", mq.getSuccessMessageHTML(resultSet));
						
						// set modelChangedFlag
						if (this.isModelDatasetSelected()) {
							this.modelChangedFlag = true;
							this.draw();
						}
					}
					IIDXHelper.progressBarRemove(this.progressDiv);
					this.fillAll();
				};
				
				IIDXHelper.progressBarCreate(this.progressDiv, "progress-success progress-striped active");
				IIDXHelper.progressBarSetPercent(this.progressDiv, 50);
				
				var mq = new MsiClientQuery(this.sparqlQueryServiceURL, this.getGraphInterface(), this.msiFailureCallback.bind(this));
	    		mq.execUploadOwl(this.owlFile, successCallback.bind(this, mq));
			},
			
			/**
			 * Clear graph button callback
			 */
			toolsClearGraph : function () {
				
				if (this.getGraphServerUrl() === "") {
					this.logAndAlert("Error", "No graph specified.  Load a nodegroup or drop a JSON file.");

					return;
				}
				var targetHTML = "<b>Server: </b>" + this.getGraphServerUrl() + "<br><b>Dataset: </b>" + this.getGraphDataset();
				ModalIidx.okCancel("Confirm", 
						           "Are you sure you want to clear the graph: <br><br>" + targetHTML, 
						            this.toolsClearGraphOK.bind(this));
			},
			
			/**
			 * Clear graph button callback, after confirmation dialog
			 */
			toolsClearGraphOK : function () {
				kdlLogEvent("SG: import clear graph");
				
				var successCallback = function (mq, resultSet) { 
					IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
					
					if (! resultSet.isSuccess()) {
						this.logAndAlert("'Clear All' Service failed", mq.getDropGraphResultHtml(resultSet));
					} else {
						
						ModalIidx.alert("Success", mq.getSuccessMessageHTML(resultSet));
						
						// set modelChangedFlag
						if (this.isModelDatasetSelected()) {
							this.modelChangedFlag = true;
							this.draw();
						}
					}
					
					this.fillAll();
					IIDXHelper.progressBarRemove(this.progressDiv);
				};
				
				IIDXHelper.progressBarCreate(this.progressDiv, "progress-danger progress-striped active");
				IIDXHelper.progressBarSetPercent(this.progressDiv, 50);
				
				var mq = new MsiClientQuery(this.sparqlQueryServiceURL, this.getGraphInterface(), this.msiFailureCallback.bind(this));
	    		mq.execClearAll(successCallback.bind(this, mq));
			},
			
			// shut off all stuff during an oper
			// note: opposite of disableAll() is .fillAll()
			disableAll : function () {
				this.button.disabled = true;
				document.getElementById("butClearGraph").disabled = true;
				document.getElementById("butClearPrefix").disabled = true;
				document.getElementById("butUploadOwl").disabled = true;
			},
			
			/**
			 * Generic callback for microservice call failures
			 */
			msiFailureCallback : function (msg, optAllowHtml) {
				// optAllowHtml default is false.  If you know msg is html: make it true.
				var htmlFlag = (typeof optAllowHtml === "undefined" || !optAllowHtml) ? ModalIidx.HTML_SAFE : ModalIidx.HTML_ALLOW;
				ModalIidx.alert("Microservice Failure", msg, htmlFlag);
				kdlLogEvent("SG Microservice Failure", "message", msg);
				IIDXHelper.progressBarRemove(this.progressDiv);
				this.fillAll();
			},
			
			/**
			 * Clear the nodegroup
			 */
			clearNodeGroup : function () {
				this.jsonFile = null;
				this.nodeGroupName = "";
				this.conn = null;
				this.nodegroup = null;
				this.importTab = null;
				this.draw();
			},
			
			/**
			 * Set up the connection and nodegroup
			 */
			setNodeGroup : function (conn, nodegroup, importTab, oInfoLoadTime) {
				
				if (importTab !== null && nodegroup !== null && conn !== null) {
					// set json, data, and suggested Prefix 
					var j = new SparqlGraphJson(conn, nodegroup, importTab);
		    		this.jsonFile = new Blob( [j.stringify()], { type: "text/css"});
		    		
		    		//only change this when setDataFile is called.
		    		//this.dataFile = importTab.getCsvFile();
		    		this.suggestedPrefix = importTab.getBaseURI();
				} else {
					this.jsonFile = null;
					//this.dataFile = null;
					this.suggestedPrefix = null;
				}
				
				if (conn !== null) {
		    		this.nodeGroupName = conn.name;
		    		this.conn = conn;
				} else {
					this.nodeGroupName = "";
					this.conn = null;
				}

	    		this.nodegroup = nodegroup;
	    		
	    		// was a new oInfo loaded?
	    		if (oInfoLoadTime !== this.oInfoLoadTime) {
	    			this.modelChangedFlag = false;
	    			this.oInfoLoadTime = oInfoLoadTime;
	    		}
	    		this.draw();
			},
			
			clearDataFile : function () {
				this.dataFile = null;
			},
			
			setDataFile : function (f) {
				this.dataFile = f;
			},
			
			/**
			 * Is file droppable on data (CSV) dropzone
			 */
			isDroppableFile : function (ev) {
				return true;  // I can't figure out how to do this properly.  -Paul
			},
			
			/**
			 * is file droppable on json nodegroup dropzone
			 */
			isDroppableJson : function (ev) {
				return true;  // I can't figure out how to do this properly.  -Paul
			},
			
			/**
			 * Drop datafile callback
			 */
			ondropData : function(ev, label) {
				this.dataFile = null;
				label.data = "Drop CSV File";
				
				if (ev.dataTransfer.files.length != 1) {
					this.logAndAlert("Error dropping data file", "Only single file drops are supported.");
				} else if (ev.dataTransfer.files[0].name.slice(-4).toLowerCase() != ".csv") {
					this.logAndAlert("Error dropping data file", "Only CSV file drops are supported.");
				} else {
					this.readSingleFileAsync(ev.dataTransfer.files[0]);
					kdlLogEvent("SG: import drop csv", "file", ev.dataTransfer.files[0]);
					label.data = ev.dataTransfer.files[0].name;
				}
			},
			
			/**
			 * Drop json nodegroup callback
			 */
			ondropJson : function(ev, label) {
				this.jsonFile = null;
				label.data = "Drop Nodegroup Query Json";

				if (ev.dataTransfer.files.length != 1) {
					this.logAndAlert("Error dropping nodegroup JSON", "Only single file drops are supported.");
				} else if (ev.dataTransfer.files[0].name.slice(-5).toLowerCase() != ".json") {
					this.logAndAlert("Error dropping nodegroup JSON", "Only JSON file drops are supported.");
				} else {
					this.readSingleJsonAsync(ev.dataTransfer.files[0]);
					kdlLogEvent("SG: import drop json", "file", ev.dataTransfer.files[0]);
					label.data = ev.dataTransfer.files[0].name;
				}
			},
			
			
			/** 
			 * Read an owl file 
			 */
			readSingleOwlAsync : function (f) {
				//Retrieve the first (and only!) File from the FileList object

				if (f) {
					this.disableAll();
					IIDXHelper.progressBarCreate(this.progressDiv, "progress-success progress-striped active");
					IIDXHelper.progressBarSetPercent(this.progressDiv, 50);
					
					var r = new FileReader();
					r.onload = function(e) { 
						kdlLogEvent("SG: Import Read Owl", "filename", f.name);
						this.owlFile = f;
						
						IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
						IIDXHelper.progressBarRemove(this.progressDiv);
						this.fillAll();
						
					}.bind(this);
					
					try {
						r.readAsText(f);
					} catch (e) {
						logAndAlert("Error loading OWL file", err.message);
						
						IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
						IIDXHelper.progressBarRemove(this.progressDiv);
						this.fillAll();
					}
				} else { 
					alert("Failed to load file");
				}
			},
			
			/** 
			 * Read a data (CSV) file 
			 */
			readSingleFileAsync : function (f) {
				//Retrieve the first (and only!) File from the FileList object

				if (f) {
					this.disableAll();
					IIDXHelper.progressBarCreate(this.progressDiv, "progress-success progress-striped active");
					IIDXHelper.progressBarSetPercent(this.progressDiv, 50);
					
					var r = new FileReader();
					r.onload = function(e) { 
						kdlLogEvent("Import Service Load Csv", "filename", f.name);
						this.dataFile = f;
						
						IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
						IIDXHelper.progressBarRemove(this.progressDiv);
						this.fillAll();
					}.bind(this);
					
					try {
						r.readAsText(f);
					} catch (e) {
						logAndAlert("Error loading data file", err.message);
						
						IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
						IIDXHelper.progressBarRemove(this.progressDiv);
						this.fillAll();
					}
					
				} else { 
					alert("Failed to load file");
				}
			},
			  
			
			/** 
			 * Read a nodegroup (JSON) file 
			 */
			readSingleJsonAsync : function (f) {
				//Retrieve the first (and only!) File from the FileList object

				if (f) {
					this.disableAll();
					IIDXHelper.progressBarCreate(this.progressDiv, "progress-success progress-striped active");
					IIDXHelper.progressBarSetPercent(this.progressDiv, 50);
					
					var r = new FileReader();
					r.onload = function(e) { 
						kdlLogEvent("Import Service Load Json", "filename", f.name);
						
			    		this.jsonFile = f;
						this.nodeGroupName = f.name;
						
						var jsonifier = new SparqlGraphJson();
						try {
			    			jsonifier.parse(r.result);
			    			this.conn = jsonifier.getSparqlConn();
			    			
			    		} catch (e) {
			    			this.jsonFile = null;
			    			this.nodeGroupName = null;
			    			this.conn = null;
			    			this.logAndAlert("Error", "Error parsing the JSON query file: \n" + e);
			    		}
			    		
			    		IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
						IIDXHelper.progressBarRemove(this.progressDiv);
						this.fillAll();	 
					}.bind(this);
					
					try {
						r.readAsText(f);
					} catch (e) {
						logAndAlert("Error loading data file", err.message);
						IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
						IIDXHelper.progressBarRemove(this.progressDiv);
						this.fillAll();
					}
				} else { 
					alert("Failed to load file");
				}
			},
			
			/** 
			 * Import button callback
			 */
			callFromCsvFile : function (e) {
				
			    kdlLogEvent("Import Service Call");
			    e.preventDefault();
			    
			    var successCallback = function (client, resultSet) { 
					IIDXHelper.progressBarSetPercent(this.progressDiv, 100);

			    	// figure out if dialog should say "success" or "failure"
					var title = resultSet.isSuccess() ? "Success" : "Failure";
					
					// get the generic HTML results
					var html = client.getFromCsvResultHtml(resultSet)
					
					// check for an error table and download
					var csv = client.getFromCsvErrorTable(resultSet);
					if (csv) {
						html += "<h3><b>CSV containing failed rows is being downloaded.</b>";
						IIDXHelper.downloadFile(csv, "error_report.csv");
					}
					
					// display to user
					this.logAndAlert(title, html); 
					IIDXHelper.progressBarRemove(this.progressDiv);
					this.fillAll();
				};
				
				IIDXHelper.progressBarCreate(this.progressDiv, "progress-success progress-striped active");
				IIDXHelper.progressBarSetPercent(this.progressDiv, 50);
				
			    var client = new MsiClientIngestion(this.ingestionServiceURL, this.msiFailureCallback.bind(this));
				kdlLogEvent("SG: import fromCsvFilePrecheck");
			    client.execFromCsvFilePrecheck(this.jsonFile, this.dataFile, successCallback.bind(this, client));	
			    
				
			},
			
			/** 
			 * Log and ... something else ... I forgot...
			 * Just aiming for a comment longer than the function
			 */
			logAndAlert : function (title, msg) {
		    	kdlLogEvent("SG: alert", "message", title + ": " + msg);
		    	ModalIidx.alert(title, msg);
		    },
			
		};
	
		return UploadTab;            
	}
);