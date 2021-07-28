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
            'sparqlgraph/js/msiclientnodegroupexec',
         	'sparqlgraph/js/msiclientquery',
            'sparqlgraph/js/msiclientontologyinfo',
            'sparqlgraph/js/msiclientresults',
            'sparqlgraph/js/msiclientstatus',
            'sparqlgraph/js/msiresultset',
         	'sparqlgraph/js/sparqlgraphjson',
         	'jquery',

			// shimmed
         	'sparqlgraph/js/belmont',
         	//'logconfig',
		],

	function(ModalIidx, IIDXHelper, SparqlGraphJson, MsiClientIngestion, MsiClientNodeGroupExec, MsiClientQuery, MsiClientOntologyInfo, MsiClientResults, MsiClientStatus, MsiResultSet, SparqlGraphJson, $) {

		/*
		 *    A column name or text or some item used to build a triple value
		 */
		var UploadTab = function (parentDiv, toolsDiv, miscDiv, reloadCallback, ingestionServiceURL, sparqlQueryServiceURL) {
			 this.parentDiv = parentDiv;
			 this.toolsDiv = toolsDiv;
             this.miscDiv = miscDiv;
			 this.reloadCallback = reloadCallback;
			 this.ingestionServiceURL = ingestionServiceURL;
			 this.sparqlQueryServiceURL = sparqlQueryServiceURL;
			 this.fileDiv = null;
			 this.button = null;

			 this.importJsonFile = null;
			 this.importNgName = null;
			 this.conn = null;
			 this.dataFile = null;

			 this.nodegroup = null;
             this.oInfo = null;
			 this.inputURI = null;
			 this.uploadFile = null;
             this.syncOwlFiles = [];

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
                    this.drawMiscTools();
					this.fillSelectChooseGraph();     // fill in nodegroup-specific the first time
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
                p.id = "uploadtab_importcsv_p";
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
                this.fillMiscTools();
				this.fillTools();
			},

			/**
			 * Fill in values in the import section
			 */
			fillBody : function () {
				var readyFlag = true;
				var msgTxt;
				var dropZone;
                var uploadP;

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
				if (this.importJsonFile == null) {
					msgTxt = "Drop Nodegroup Query Json";
					IIDXHelper.setDropzoneLabel(dropZone, msgTxt, IIDXHelper.DROPZONE_EMPTY);
					readyFlag = false;

				} else if (this.importJsonFile.hasOwnProperty(name)) {
					msgTxt = this.importJsonFile.name + " (" + Number(this.importJsonFile.size / 1024).toFixed(1) + " KB)";
					IIDXHelper.setDropzoneLabel(dropZone, msgTxt, IIDXHelper.DROPZONE_FULL);
				// display node group
				} else {
					msgTxt = "Nodegroup from: " + this.importNgName;
					IIDXHelper.setDropzoneLabel(dropZone, msgTxt, IIDXHelper.DROPZONE_FULL);
				}

                uploadP = document.getElementById("uploadtab_importcsv_p");
                if (this.importJsonConn == null) {
                    uploadP.innerHTML = "Import csv";
                } else {
                    var sei = this.importJsonConn.getDataInterface(0);
                    uploadP.innerHTML = "Import csv into: <b>" + sei.getServerURL() + " </b>graph:<b> " + sei.getGraph() + "</b>";
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

                // ===== data dictionary button row =====
                tr = document.createElement("tr");
                td1 = document.createElement("td");
				td1.id = "tdDataDict1";
                td1.innerHTML = "Generate data dictionary"
				tr.appendChild(td1);

                td2 = document.createElement("td");
				button = document.createElement("button");
				button.id = "butDataDict";
				button.classList.add("btn");
				button.innerHTML = "Download";
				button.onclick = this.generateDataDictionary.bind(this);
				td2.appendChild(button);
				tr.appendChild(td2);
				table.appendChild(tr);

				// ===== choose dataset model row =====
				tr = document.createElement("tr");

				td1 = document.createElement("td");
				td1.id = "tdChooseGraph1";
				tr.appendChild(td1);

				td2 = document.createElement("td");
				tr.appendChild(td2);
				select = document.createElement("select");
				select.id="selectChooseGraph";
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
				td1.id = "tdUploadFile1";
				td1.appendChild(IIDXHelper.createDropzone("icon-sitemap", "Drop OWL/TTL file", function(e) {return true;}, this.toolsDropOwlFile.bind(this)));
				tr.appendChild(td1);

				// button cell
				td2 = document.createElement("td");
				button = document.createElement("button");
				button.id = "butUploadFile";
				button.classList.add("btn");
				button.innerHTML = "Upload owl/ttl";
				button.onclick = this.toolsUploadFile.bind(this);
				td2.appendChild(button);
                td2.appendChild(IIDXHelper.createNbspText());
                td2.appendChild(ModalIidx.createInfoButton("Adds triples from an owl/rdf or ttl file to the selected graph."));
				tr.appendChild(td2);
				table.appendChild(tr);

				this.toolsDiv.appendChild(table);

				// status div
				div = document.createElement("div");
				div.id = "toolsStatusDiv";
				this.toolsDiv.appendChild(div);
			},

            /**
             * Draw the tools section
             */
            drawMiscTools : function() {
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

                // ===== sync owl row =====
                tr = document.createElement("tr");
                td1 = document.createElement("td");
                td1.id = "tdSyncOwl1";
                td1.appendChild(IIDXHelper.createDropzone("icon-sitemap", "Drop OWL file(s)", function(e) {return true;}, this.toolsDropSyncOwlFile.bind(this)));
                tr.appendChild(td1);

                // button cell
                td2 = document.createElement("td");
                button = document.createElement("button");
                button.id = "butSyncOwl";
                button.classList.add("btn");
                button.innerHTML = "Sync Owl";
                button.onclick = this.toolsSyncOwl.bind(this);
                td2.appendChild(button);
                td2.appendChild(IIDXHelper.createNbspText());
                td2.appendChild(ModalIidx.createInfoButton("Finds graph based on rdf:RDF xml:base in the rdf/owl file.<br>Clears the graph.<br>Uploads the owl file."));
                tr.appendChild(td2);
                table.appendChild(tr);

                this.miscDiv.appendChild(table);

            },
			// fill and reset the dataset select dropdown
			// Build a unique list of options.
			// Precede by "model" "data" or "both"
			// Set option value to a lookup code "m1" (model 1) or ("d0" data 0) etc.
			fillSelectChooseGraph : function() {
				var select = document.getElementById("selectChooseGraph");

				// clear all options
				while (select.options.length > 0) {
					select.remove(0);
				}

				if (this.conn != null) {
					var mCount = this.conn.getModelInterfaceCount();
					var dCount = this.conn.getDataInterfaceCount();
					var seis = [];
					var src = [];
					var vals = [];

					// loop through model sei's
					for (var i=0; i < mCount; i++) {
						var sei = this.conn.getModelInterface(i);

						// search already-added sei's
						var found = -1;
						for (var j=0; j < seis.length; j++) {
							if (seis[j].equals(sei)) {
								found = j;
								break;
							}
						}
						// if not found
						if (found == -1) {
							seis.push(sei);    // add sei
							src.push("model"); // string for output
							vals.push("m"+i);  // lookup code
						}
					}

                    // loop through importedGraphs
                    var importList = (this.oInfo != null) ? this.oInfo.getImportedGraphs() : [];
                    var defaultSei = this.conn.getModelInterface(0);

					for (var i=0; i < importList.length; i++) {
						var sei = new SparqlServerInterface(defaultSei.getServerType(), defaultSei.getServerURL(), importList[i]);

						// search already-added sei's
						var found = -1;
						for (var j=0; j < seis.length; j++) {
							if (seis[j].equals(sei)) {
								found = j;
								break;
							}
						}
						// if not found
						if (found == -1) {
							seis.push(sei);    // add sei
							src.push("model"); // string for output
							vals.push("m"+i);  // lookup code
						}
					}

					// loop through data sei's
					for (var i=0; i < dCount; i++) {
						var sei = this.conn.getDataInterface(i);

						// search already-added sei's
						var found = -1;
						for (var j=0; j < seis.length; j++) {
							if (seis[j].equals(sei)) {
								found = j;
								break;
							}
						}
						if (found == -1) {
							seis.push(sei);    // add sei
							src.push("data");  // string for output
							vals.push("d"+i);  // lookup code
						} else {
							src[found] = "both";
						}
					}

					// Only need "-- choose --" if there are 2 unequal SEI's
					if (seis.length > 1) {
							option = document.createElement("option");
							option.text = "-- choose --";
							option.value = "";               // empty value
							select.add(option);
					}

					// fill rest of options
					for (var i=0; i < seis.length; i++) {
						var sei = seis[i];

						option = document.createElement("option");
						option.text = src[i] + ": " + sei.getGraph();      // could add sei.getServerURL()
						option.value = vals[i];
						select.add(option);
					}

				} else {
					// no connection to choose
					option = document.createElement("option");
					option.text = " ";
					option.value = "";               // empty value
					select.add(option);
					select.selectedIndex = "0";
				}

				select.selectedIndex = "0";
			},

			/**
			 * fillTools for just "clear prefix" so that it can also be used as oninput() callback for the input
			 */
			fillClearPrefix : function () {
				var connFlag = (this.getSelectedSei() != null);
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
				var connFlag = (this.getSelectedSei() != null);

				// choose dataset
				document.getElementById("tdChooseGraph1").innerHTML = "<b>Server: </b>" + this.getGraphServerUrl() + "<br>" +
																		"<b>Graph: </b>" + this.getGraphGraph();

				// clear graph
				document.getElementById("butClearGraph").disabled = (! connFlag);

				// clear prefix
				this.fillClearPrefix();

				// upload owl
				var dropzone = document.getElementById("tdUploadFile1").childNodes[0];
				var msgTxt = "";
				if (this.uploadFile !== null) {
					document.getElementById("butUploadFile").disabled = false;
					msgTxt = this.uploadFile.name + " (" + Number(this.uploadFile.size / 1024).toFixed(1) + " KB)";
					IIDXHelper.setDropzoneLabel(dropzone, msgTxt, IIDXHelper.DROPZONE_FULL);

				} else {
					document.getElementById("butUploadFile").disabled = true;
					msgTxt = "Drop OWL/TTL file";
					IIDXHelper.setDropzoneLabel(dropzone, msgTxt, IIDXHelper.DROPZONE_EMPTY);
				}

				// status div
				var statusDiv = document.getElementById("toolsStatusDiv");
				if (!connFlag) {
					// warn there's no connection
					statusDiv.classList.add("alert");
					statusDiv.innerHTML = "No valid endpoint/graph is chosen.";

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
			 * Fill in values in the tools section
			 */
			fillMiscTools : function () {

				// sync owl
				var dropzone = document.getElementById("tdSyncOwl1").childNodes[0];
                var syncButton = document.getElementById("butSyncOwl");
				var msgTxt = "";
				if (this.syncOwlFiles.length > 0) {
					syncButton.disabled = false;
                    dropzone.disabled = false;
                    msgTxt = "";
                    for (var i=0; i < this.syncOwlFiles.length; i++) {
                        msgTxt += " " + this.syncOwlFiles[i].name;
                        if (i == 3) {
                            msgTxt += "...";
                            break;
                        }
                    }
					IIDXHelper.setDropzoneLabel(dropzone, msgTxt, IIDXHelper.DROPZONE_FULL);

				} else if (this.conn != null) {
                    syncButton.disabled = true;
                    dropzone.disabled = false;
					document.getElementById("butSyncOwl").disabled = true;
					msgTxt = "Drop OWL file";
					IIDXHelper.setDropzoneLabel(dropzone, msgTxt, IIDXHelper.DROPZONE_EMPTY);

				} else {
                    syncButton.disabled = true;
                    dropzone.disabled = true;
					document.getElementById("butSyncOwl").disabled = true;
					msgTxt = "<no connection loaded>";
					IIDXHelper.setDropzoneLabel(dropzone, msgTxt, IIDXHelper.DROPZONE_EMPTY);
				}
            },

            generateDataDictionary : function() {
                var successCallback = function(res) {
                    if (! res.isSuccess()) {
                        ModalIidx.alert("OInfo service failure", res.getFailureHtml());
                    } else {
                        res.tableDownloadCsv();
                        ModalIidx.alert("Success", "Data dictionary CSV has been downloaded.");
                    }
                }

                var client = new MsiClientOntologyInfo(g.service.ontologyInfo.url);
                client.execGetDataDictionary(this.conn, successCallback);
            },

			// get SparqlEndpointInterface or null
			getSelectedSei : function () {
				var val = document.getElementById("selectChooseGraph").value;
				if (val == "" || this.conn == null) {
					return null;
				} else if (val.charAt(0) == "m") {
					return this.conn.getModelInterface(parseInt(val.substring(1)));
				} else if (val.charAt(0) == "d") {
					return this.conn.getDataInterface(parseInt(val.substring(1)));
				}
			},

			/**
			 * Get server URL for the selected connection (data or model)
			 */
			getGraphServerUrl : function () {
				var sei = this.getSelectedSei();

				if (sei == null) {
					return "";
				} else {
					return sei.getServerURL();
				}
			},

			/**
			 * Get dataset for the selected connection (data or model)
			 */
			getGraphGraph : function () {
				var sei = this.getSelectedSei();

				if (sei == null) {
					return "";
				} else {
					return sei.getGraph();
				}
			},

			/**
			 * Is the model dataset selecte   OR  data is selected but it is identical
			 */
			isModelGraphSelected : function () {
				var sei = this.getSelectedSei();
				if (this.conn != null) {
					for (var i=0; i < this.conn.getModelInterfaceCount(); i++) {
						if (this.conn.getModelInterface(i).equals(sei)) {
							return true;
						}
					}
				}
				return false;
			},

			/**
			 * Clear prefix button
			 */
			toolsClearPrefix : function () {

				var prefix = document.getElementById("inputClearPrefix").value;
				var targetHTML = "<b>Server: </b>" + this.getGraphServerUrl() + "<br>";
				targetHTML += "<b>Graph: </b> "+ this.getGraphGraph()+ "<br>";
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
						if (this.isModelGraphSelected()) {
							this.modelChangedFlag = true;
							this.draw();
						}
					}

					this.fillAll();
					IIDXHelper.progressBarRemove(this.progressDiv);
				};

				IIDXHelper.progressBarCreate(this.progressDiv, "progress-danger progress-striped active");
				IIDXHelper.progressBarSetPercent(this.progressDiv, 50);

				var mq = new MsiClientQuery(this.sparqlQueryServiceURL, this.getSelectedSei(), this.msiFailureCallback.bind(this));
	    		mq.execClearPrefix(prefix, successCallback.bind(this, mq));

			},

			/**
			 * Owl drop callback
			 */
			toolsDropOwlFile : function (ev) {
				this.uploadFile = null;

				if (ev.dataTransfer.files.length != 1) {
					this.logAndAlert("Error dropping OWL file", "Only single file drops are supported.");
				} else if (ev.dataTransfer.files[0].name.slice(-4).toLowerCase() != ".owl" && ev.dataTransfer.files[0].name.slice(-4).toLowerCase() != ".ttl") {
					this.logAndAlert("Error dropping OWL file", "Only .owl and .ttl file drops are supported.");
				} else {
					this.readSingleOwlTtlAsync(ev.dataTransfer.files[0]);
				}
			},

            /**
             * SyncOwl drop callback
             */
            toolsDropSyncOwlFile : function (ev) {
                this.syncOwlFiles = [];
                var files = ev.dataTransfer.files;

                readNext = function(index) {
                    // done
                    if (index >= files.length) {
                        this.fillAll();
                        return;
                    }

                    // error
                    var file = files[index];
                    if (file.name.slice(-4) != ".owl" && file.name.slice(-4) != ".ttl") {
                        this.logAndAlert("Error dropping data file", "Only .owl and .ttl are supported: " + file.name);
                        this.fillAll();
                        return;
                    }

                    // normal
                    var reader = new FileReader();
                    reader.onload = function(f) {
                        console.log(file.name);
                        this.syncOwlFiles.push(file);
                        // chain next read
                        readNext(index+1);
                    }.bind(this);
                    reader.readAsText(file);
                }.bind(this);

                readNext(0);
            },

			/**
			 * Upload owl button callback
			 */
			toolsUploadFile : function() {
				kdlLogEvent("SG: import upload file", "filename", this.uploadFile);

				var successCallback = function (mq, resultSet) {
					IIDXHelper.progressBarSetPercent(this.progressDiv, 100);

					if (! resultSet.isSuccess()) {
						this.logAndAlert("'Upload File' Service failed", mq.getDropGraphResultHtml(resultSet));
					} else {

						ModalIidx.alert("Success", mq.getSuccessMessageHTML(resultSet));

						// set modelChangedFlag
						if (this.isModelGraphSelected()) {
							this.modelChangedFlag = true;
							this.draw();
						}
					}
					IIDXHelper.progressBarRemove(this.progressDiv);
					this.fillAll();
				};

				IIDXHelper.progressBarCreate(this.progressDiv, "progress-success progress-striped active");
				IIDXHelper.progressBarSetPercent(this.progressDiv, 50);

                if (this.getSelectedSei() == null) {
                    ModalIidx.alert("No endpoint", "No endpoint/graph specified.<br>Don't know where to upload the file.");
                } else {
                    var mq = new MsiClientQuery(this.sparqlQueryServiceURL, this.getSelectedSei(), this.msiFailureCallback.bind(this));

                    if (this.uploadFile.name.slice(-4).toLowerCase() == ".owl") {
                        mq.execUploadOwl(this.uploadFile, successCallback.bind(this, mq));
                    } else {
                        mq.execUploadTurtle(this.uploadFile, successCallback.bind(this, mq));
                    }
                }

			},

            /**
			 * Upload owl button callback
			 */
			toolsSyncOwl : function() {
				kdlLogEvent("SG: import upload owl");

				var successCallback = function (mq, index, resultSet) {

					if (! resultSet.isSuccess()) {
						this.logAndAlert("'Sync owl' Service failed", mq.getDropGraphResultHtml(resultSet));
                        // finish
                        IIDXHelper.progressBarRemove(this.progressDiv);
    					this.fillAll();

					} else {

                        var percentEach = 100 / this.syncOwlFiles.length;
                        var newIndex = index + 1;
                        IIDXHelper.progressBarSetPercent(this.progressDiv, percentEach * (newIndex));

                        if (newIndex < this.syncOwlFiles.length) {

                            IIDXHelper.progressBarSetPercent(this.progressDiv, percentEach * (newIndex + 0.5));
                            mq.execSyncOwl(this.syncOwlFiles[newIndex], successCallback.bind(this, mq, newIndex));
                        } else {
                            // finish
                            IIDXHelper.progressBarRemove(this.progressDiv);
        					this.fillAll();
                        }
					}

				};

				IIDXHelper.progressBarCreate(this.progressDiv, "progress-success progress-striped active");


                if (this.conn == null) {
                    ModalIidx.alert("No endpoint", "No connection is loaded. <br>Don't know where to find the triplestore.");
                } else {
                    var mq = new MsiClientQuery(this.sparqlQueryServiceURL, this.conn.getDefaultQueryInterface(), this.msiFailureCallback.bind(this));
                    var percentEach = 100 / this.syncOwlFiles.length;
                    IIDXHelper.progressBarSetPercent(this.progressDiv, percentEach * 0.5);
                    mq.execSyncOwl(this.syncOwlFiles[0], successCallback.bind(this, mq, 0));
                }

			},
			/**
			 * Clear graph button callback
			 */
			toolsClearGraph : function () {

				if (this.getGraphServerUrl() === "") {
					this.logAndAlert("Error", "No graph specified.  Load a nodegroup or drop a JSON file.");

					return;
				}
				var targetHTML = "<b>Server: </b>" + this.getGraphServerUrl() + "<br><b>Graph: </b>" + this.getGraphGraph();
				ModalIidx.okCancel("Confirm",
						           "Are you sure you want to clear the graph: <br><br>" + targetHTML,
						            this.toolsClearGraphOK.bind(this));
			},

			/**
			 * Clear graph button callback, after confirmation dialog
			 */
			toolsClearGraphOK : function () {
				kdlLogEvent("SG: import clear graph");

				var successCallback = function (resultSet) {
					IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
                    ModalIidx.alert("Success", resultSet.tableGetRows()[0][0], true);

					// set modelChangedFlag
					if (this.isModelGraphSelected()) {
						this.modelChangedFlag = true;
						this.draw();
					}

					this.fillAll();
					IIDXHelper.progressBarRemove(this.progressDiv);
				}.bind(this);

				IIDXHelper.progressBarCreate(this.progressDiv, "progress-danger progress-striped active");
				IIDXHelper.progressBarSetPercent(this.progressDiv, 50);

                var checkForCancel = function() { return false; };
                var statusCallback = function() {};
                // Run nodegroup via Node Group Exec Svc
                var jsonCallback = MsiClientNodeGroupExec.buildFullJsonCallback(successCallback,
                                                                                 this.msiFailureCallback.bind(this),
                                                                                 statusCallback,
                                                                                 checkForCancel,
                                                                                 g.service.status.url,
                                                                                 g.service.results.url);
                var execClient = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url, g.shortTimeoutMsec);


                execClient.execAsyncDispatchClearGraph(this.getSelectedSei(), jsonCallback, this.msiFailureCallback.bind(this));
			},

			// shut off all stuff during an oper
			// note: opposite of disableAll() is .fillAll()
			disableAll : function () {
				this.button.disabled = true;
				document.getElementById("butClearGraph").disabled = true;
				document.getElementById("butClearPrefix").disabled = true;
				document.getElementById("butUploadFile").disabled = true;
			},

			/**
			 * Generic callback for microservice call failures
			 */
			msiFailureCallback : function (msg, optAllowHTML) {
				// optAllowHtml default is true.
				var htmlFlag = (typeof optAllowHtml === "undefined" || optAllowHtml) ? ModalIidx.HTML_ALLOW : ModalIidx.HTML_SAFE;
				ModalIidx.alert("Microservice Failure", msg, htmlFlag);
				kdlLogEvent("SG Microservice Failure", "message", msg);
				IIDXHelper.progressBarRemove(this.progressDiv);
				this.fillAll();
			},

			/**
			 * Clear the nodegroup
			 */
			clearNodeGroup : function () {
				this.importJsonFile = null;
				this.importNgName = "";
                this.importJsonConn = null;
				this.conn = null;
				this.nodegroup = null;
				this.mappingTab = null;
				this.draw();
			},

			/**
			 * Set up the connection and nodegroup
			 */
			setNodeGroup : function (conn, nodegroup, oInfo, mappingTab, oInfoLoadTime) {

				if (mappingTab !== null && nodegroup !== null && conn !== null) {
					// set json, data, and suggested Prefix
					var sgjson = new SparqlGraphJson(conn, nodegroup, mappingTab.getImportSpec());
		    		this.importJsonFile = new Blob( [sgjson.stringify()], { type: "text/css"});
                    this.importJsonConn = conn;

		    		//only change this when setDataFile is called.
		    		//this.dataFile = mappingTab.getCsvFile();
		    		this.suggestedPrefix = mappingTab.getBaseURI();
				} else {
					this.importJsonFile = null;
                    this.importJsonConn = null;
					//this.dataFile = null;
					this.suggestedPrefix = null;
				}

				if (conn !== null) {
		    		this.importNgName = conn.name;
		    		this.conn = conn;
				} else {
					this.importNgName = "";
					this.conn = null;
				}

	    		this.nodegroup = nodegroup;
                this.oInfo = oInfo;

	    		// was a new oInfo loaded?
	    		if (oInfoLoadTime !== this.oInfoLoadTime) {
	    			this.modelChangedFlag = false;
	    			this.oInfoLoadTime = oInfoLoadTime;
	    		}
	    		this.draw();
	    		this.fillSelectChooseGraph();
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
				this.importJsonFile = null;
                this.importJsonConn = null;
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
			readSingleOwlTtlAsync : function (f) {
				//Retrieve the first (and only!) File from the FileList object

				if (f) {
					this.disableAll();
					IIDXHelper.progressBarCreate(this.progressDiv, "progress-success progress-striped active");
					IIDXHelper.progressBarSetPercent(this.progressDiv, 50);

					var r = new FileReader();
					r.onload = function(e) {
						kdlLogEvent("SG: Import Read Owl/Ttl", "filename", f.name);

						this.uploadFile = f;

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
             * Read an owl file
             */
            readSyncOwlAsync : function (f) {
                //Retrieve the first (and only!) File from the FileList object

                if (f) {
                    this.disableAll();
                    IIDXHelper.progressBarCreate(this.progressDiv, "progress-success progress-striped active");
                    IIDXHelper.progressBarSetPercent(this.progressDiv, 50);

                    var r = new FileReader();
                    r.onload = function(e) {
                        kdlLogEvent("SG: Sync Read Owl", "filename", f.name);

                        this.syncOwlFile = f;

                        this.progressDone();

                    }.bind(this);

                    try {
                        r.readAsText(f);
                    } catch (e) {
                        logAndAlert("Error loading OWL file", err.message);

                        this.progressDone();
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

						this.progressDone();
					}.bind(this);

					try {
						r.readAsText(f);
					} catch (e) {
						logAndAlert("Error loading data file", err.message);

						this.progressDone();
					}

				} else {
					alert("Failed to load file");
				}
			},

            progressDone : function() {
                IIDXHelper.progressBarSetPercent(this.progressDiv, 100);
                IIDXHelper.progressBarRemove(this.progressDiv);
                this.fillAll();
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

						var sgjson = new SparqlGraphJson();
						try {
			    			sgjson.parse(r.result);

                            this.importJsonFile = f;
    						this.importNgName = f.name;

			    			this.importJsonConn = sgjson.getSparqlConn();
                            if (this.conn && ! this.conn.equals(this.importJsonConn, true)) {
                                ModalIidx.choose("New Connection",
                                                 "Nodegroup json contains a different SPARQL connection<br><br>Which one do you want to use?",
                                                 ["Keep Current",                            "From file"],
                                                 [this.swapJsonFileConn.bind(this, sgjson),   function(){}]
                                                 );
                            }
                            //HERE compare jsonConn to this.conn and decide whether to swap
                            //     display the jsonConn.
			    		} catch (e) {
			    			this.importJsonFile = null;
			    			this.importNgName = null;
			    			this.logAndAlert("Error", "Error parsing the JSON query file: \n" + e);
			    		}

			    		this.progressDone();
					}.bind(this);

					try {
						r.readAsText(f);
					} catch (e) {
						logAndAlert("Error loading data file", err.message);
						this.progressDone();
					}
				} else {
					alert("Failed to load file");
				}
			},

            // inject this.conn into this.importJsonFile
            swapJsonFileConn : function(jsonFileSgjson) {
                this.importJsonConn = this.conn;
                jsonFileSgjson.setSparqlConn(this.conn);
                this.importJsonFile = new Blob( [jsonFileSgjson.stringify()], { type: "text/css"});
                this.fillAll();
            },

			/**
			 * Import button callback
			 */
			callFromCsvFile : function (e) {

			    kdlLogEvent("Import Service Call");
			    e.preventDefault();

                var gotFailureTable = function(tableResults) {
                    if (!tableResults.isSuccess()) {
                        gotFailureMessage.bind(this)(tableResults.getRationaleHtml());

                    } else {
                        gotFailureMessage.bind(this)(tableResults.tableGetHtml());

                        // check for an error table and download
    					var csv = tableResults.tableGetCsv();
    					if (csv) {
    						IIDXHelper.downloadFile(csv, "error_report.csv", "text/csv;charset=utf8");
    					}
                    }
                };

                var gotSuccessStatusMessage = function(message) {
                    ModalIidx.alert("Import successful", message);
					IIDXHelper.progressBarRemove(this.progressDiv);
					this.fillAll();
                };

                var gotFailureMessage = function(message) {
                    ModalIidx.alert("Import failed", message);
					IIDXHelper.progressBarRemove(this.progressDiv);
					this.fillAll();
                };

                var gotSuccessCallback = function(statusClient, resultsClient, successFlag) {
                    if (successFlag) {
                        statusClient.execJobStatusMessageString(gotSuccessStatusMessage.bind(this));
                    } else {
                        resultsClient.execGetTableResultsJson(1000, gotFailureTable.bind(this));   // PEC HERE, need csv for download, html for screen
                    }
                };

                var getSuccessCallback = function(statusClient, resultsClient) {
                    statusClient.execGetStatusBoolean(gotSuccessCallback.bind(this, statusClient, resultsClient));
                };

                var waitingCallback = function(statusClient, resultsClient, message, percent) {
                    IIDXHelper.progressBarSetPercent(this.progressDiv, percent, message);
                    if (percent == 100) {
                        getSuccessCallback.bind(this)(statusClient, resultsClient);
                    } else {
                        statusClient.execWaitForPercentOrMsecInt(Math.min(percent + 10, 100), 1000, waitingCallback.bind(this, statusClient, resultsClient));

                    }
                };

			    var successCallback = function (client, resultSet) {
                    if (resultSet.isSuccess()) {
                        var jobId = resultSet.getSimpleResultField("JobId");

                        var statusClient = new MsiClientStatus(g.service.status.url, jobId, this.msiFailureCallback.bind(this));
                        var resultsClient = new MsiClientResults(g.service.results.url, jobId, this.msiFailureCallback.bind(this));

                        waitingCallback.bind(this)(statusClient, resultsClient, 1);
                    } else {
                        gotFailureMessage.bind(this)(resultSet.getRationaleHtml());
                    }
                };

                /*
                // PEC here
                // left over code
					IIDXHelper.progressBarSetPercent(this.progressDiv, 100);

			    	// figure out if dialog should say "success" or "failure"
					var title = resultSet.isSuccess() ? "Success" : "Failure";

					// get the generic HTML results
					var html = client.getFromCsvResultHtml(resultSet)

					// check for an error table and download
					var csv = client.getFromCsvErrorTable(resultSet);
					if (csv) {
						html += "<h3><b>CSV containing failed rows is being downloaded.</b>";
						IIDXHelper.downloadFile(csv, "error_report.csv", "text/csv;charset=utf8");
					}

					// display to user
                    kdlLogEvent("Import Complete",
                                "recordsProcessed",    resultSet.getRecordProcessResultField(MsiClientIngestion.RECORDS_PROCESSED) || 0,
                                "failuresEncountered", resultSet.getRecordProcessResultField(MsiClientIngestion.FAILURE_ROWS) || 0  );
					ModalIidx.alert(title, html);
					IIDXHelper.progressBarRemove(this.progressDiv);
					this.fillAll();
				};
                */

				IIDXHelper.progressBarCreate(this.progressDiv, "progress-success progress-striped active");
				IIDXHelper.progressBarSetPercent(this.progressDiv, 1);

			    var client = new MsiClientIngestion(this.ingestionServiceURL, this.msiFailureCallback.bind(this));
				kdlLogEvent("SG: import fromCsvFilePrecheck");
			    client.execFromCsvFilePrecheckAsync(this.importJsonFile, this.dataFile, successCallback.bind(this, client));


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
