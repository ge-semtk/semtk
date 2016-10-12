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

// This file is not a typical javascript object, but a script
// that needs to be loaded by the HTML.
// Its other half must also be loaded byt the HTML:  sparqlform.js

/**
 *   This file is meant to be CONSTANT across different custom forms.
 *   Put local functions in customconfig.js.
 */
require([	'sparqlgraph/js/sparqlform',
         	'sparqlgraph/js/modaliidx',
         	'sparqlgraph/js/htmlformgroup',  
         	'sparqlgraph/js/msiclientquery',
         	
         	'local/sparqlformconfig',
         	
         	'jquery',
		
			// rest are shimmed
         	
         	
		],

	function(SparqlForm, ModalIidx, HtmlFormGroup, MsiClientQuery, Config, $) {

		var g = null;
		
		// ****   Start on load proceedure ****
	
		onLoadCustom = function () {
			
			// set "g" before calling anything
			require(['custom/customconfig'],
				function(Global) {
					g = Global;
					var fullURL = window.location.toString();
					var customURL = fullURL.substring(0, fullURL.lastIndexOf("/") + 1) + gCustom + "/customDiv.html";
					kdlLogEvent("Custom: Page Load");
					
					// this is a little funky, but jquery .load() generates a "forbidden 403" in Chrome.
					$.ajax({
						  // will not work for jsonp or cross domain requests, will force success to run before
						  // other code
						  async: false,
						  url: customURL,
						  dataType: "html",
						  success: function(data) {
							  document.getElementById("customDiv").innerHTML = data;
						  },
						  error: function() {
					    	  alert("Bad 'form' parameter on URL.  Can't find file: " + customURL);
						  }
						});
					
					setupFileDrop();
					
					gConnSetup();   // sparqlform.js
					
					refreshHtmlFormGroup();
			});
			
			
		};
		
		refreshHtmlFormGroup = function (optValHash) {
			var valHash = (typeof optValHash === "undefined") ? undefined: optValHash;
			
			clearResults();
			
			formConstraintsNew();
			formConstraintsInit();
			
			var queryClient = new MsiClientQuery(Config.services.query.url, gConn.getDataInterface());
			var getFlag = false;
			gHtmlFormGroup = new HtmlFormGroup(this.document, queryClient, g.conn.domain, g.fields, g.query, setStatus, alertUser, beforeUpdatingCallback, doneUpdatingCallback,  valHash, getFlag);
			
		};
		
		htmlFormGroupLoadCallback = function () {
			// after initial load of formgroup and refreshes.
			
			
			doneUpdatingCallback();
			gHtmlFormGroup.doneUpdatingCallback = doneUpdatingCallback;
		};
		
		runButtonCallback = function() {
			if (g.preQueryCheck()) {
				// this is a sparqlformlocal.js function
				
				if (formConstraintsValidate() > 0) {
					alertUser("Fix highlighted constraint errors before running query.", "Constraint Errors");
				} else {
					doQuery(gConn, gHtmlFormGroup.getNodegroup(), gFormConstraint);
				}
			}
		};
		
		clearButtonCallback = function() {
			g.clearButtonCallback();
			refreshHtmlFormGroup();		
		};
		
		beforeUpdatingCallback = function() {
			setStatus("Updating choices...");
			btnFormClear.disabled = true;
			btnFormExecute.disabled = true;
			g.beforeUpdatingCallback();
		};
		
		doneUpdatingCallback = function() {
			gNodeGroup = gHtmlFormGroup.getNodegroup();
			formConstraintsUpdateTable();
			
			setStatus("");
			btnFormClear.disabled = false;
			btnFormExecute.disabled = false;
			g.doneUpdatingCallback();
		};
		
		downloadValues = function() {
			var j = gHtmlFormGroup.getValueHash();
			downloadFile(JSON.stringify(j), gCustom + ".json");
			kdlLogEvent("Download values");
		};
		
		uploadValues = function() {
			// get a file and upload values from it
			var fileInput = document.getElementById("fileInput");
			fileInput.addEventListener('change', uploadValuesLoadEvt, false);
			fileInput.click();
		};
		
		uploadValuesFile = function(file) {
			// reads json file, clears form, loads new one
			var r = new FileReader();

			r.onload = function() {
				var payload;
				try {
					payload = JSON.parse(r.result);
				} catch (e) {
					alertUser("Error parsing the JSON values file: \n" + e);
					return;
				}

				gHtmlFormGroup.setValuesFromHash(payload);

			};
			r.readAsText(file);

		};

		uploadValuesLoadEvt = function(evt) {
			// fileInput callback to load values
			uploadValuesFile(evt.target.files[0]);
		};
		
		uploadValuesDropEvt = function(evt) {
			noOpHandler(evt);
			var files = evt.dataTransfer.files;
			uploadValuesFile(files[0]);
		};
		
		noOpHandler = function(evt) {
			evt.stopPropagation();
			evt.preventDefault();
		};
		
		setupFileDrop = function() {
			// Paul's html5 voodoo:  to allow json files to be dropped
			var dropbox = document.getElementById("pageContainer");
			dropbox.addEventListener("dragenter", noOpHandler, false);
			dropbox.addEventListener("dragexit", noOpHandler, false);
			dropbox.addEventListener("dragover", noOpHandler, false);
			dropbox.addEventListener("drop", uploadValuesDropEvt, false);
		};
	}

);