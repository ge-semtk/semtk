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

			// shimmed
         	'sparqlgraph/js/belmont',
           // 'logconfig',
		],

	function(ModalIidx, IIDXHelper) {
	
		/*
		 *    A column name or text or some item used to build a triple value
		 */
		var ImportServiceModal = function (queryStr) {
			 this.queryStr = queryStr;
			 this.csvContents = null;
		};
		
		ImportServiceModal.VAR = "var";
		
		ImportServiceModal.prototype = {
			buildBody : function () {
				
				this.body = document.createElement("div");
				
				this.body.appendChild(this.createCsvDropzone());
				
				var button = document.createElement("button");
				button.classList.add("btn");
				button.innerHTML = "Go";
				button.onclick = function (x) {this.callService();}.bind(this);
				this.body.appendChild(button);
			},
			
			fillBody : function () {
				
				
			},
			
			createCsvDropzone : function () {
				
				var elem = document.createElement("icon");
				elem.id = "ismCsvDropzone";
				elem.className = "icon-table";
				elem.style = "font-size: 3em; color: gray";
				
				var div = document.createElement("div");
				div.className = "label-inverse";
				var center = document.createElement("center");
				div.appendChild(center);
									
				center.appendChild(elem);
				center.appendChild(document.createElement("br"));
				center.appendChild(document.createTextNode("Drop CSV Here"));
				
				// dragover changes color.  dropping or dragging out restores color.
				
				div.ondrop =  		function (ev) { 
										
										if (ev.dataTransfer.files.length != 1) {
											alertUser("Only single file drops are supported.");
										} else if (ev.dataTransfer.files[0].name.slice(-4).toLowerCase() != ".csv") {
											alertUser("Only CSV file drops are supported.");
										} else {
											this.readSingleFileAsync(ev.dataTransfer.files[0]);
										}
										ev.preventDefault();
										ev.stopPropagation();
							   		}.bind(this);
							   		

				div.ondragover = 	function (ev) { 
										if (ev.target.nodeType == 1) {
											if (this.isDroppableFile(ev)) {
												center.style.color = "blue"; 
												elem.style.color = "blue"; 
												ev.preventDefault();
											}
								   		    ev.preventDefault();
								   		    ev.stopPropagation();
								   		 }
									}.bind(this);
									
				div.ondragleave = 	function (ev) { 
					
										  //if (ev.target.nodeType == 1) {
										  //  ev.target.className = '';
										  //}
										center.style.color = "gray";
										elem.style.color = "gray";

									};
				return div;
			},
			
			isDroppableFile : function (ev) {
				return true;
			},
			
			readSingleFileAsync : function (f) {
				//Retrieve the first (and only!) File from the FileList object

				if (f) {
					var r = new FileReader();
					r.onload = function(e) { 
						kdlLogEvent("Import Service Load Csv", "filename", f.name);
						this.csvContents = e.target.result;
						alert( "Got the file.n" 
								+"name: " + f.name + "\n"
								+"type: " + f.type + "\n"
								+"size: " + f.size + " bytes\n"
								+ "starts with: " + this.csvContents.substr(1, 64)
						);  
					}.bind(this);
					r.readAsText(f);
				} else { 
					alert("Failed to load file");
				}
			},
			  
			callService : function () {
			    kdlLogEvent("Import Service Call");
				var url = 'http://nsk1200018594c.logon.ds.ge.com:8080/ingestion/fromCsv';
				var th = this;
				
				$.ajax({
					url: url,
					type: 'POST',
					data: { 
						"template" : th.queryStr, 
						"data" : th.csvContents, 
					},
					success: function (x) {
						alert("success");
					},
					failure: function (x) {
						alert("failure");
					},
				});
			},
			
			validate : function() {
					return null;
			},
			
			okCallback : function(userCallback) {
				kdlLogEvent("Import Service Modal Close");
			},
			
			cancelCallback : function () {

			},
				
			show : function (userCallback) {
				kdlLogEvent("Import Service Modal Open");

				this.buildBody();
				
				// launch
				var m = new ModalIidx(document, "serviceModal");
				m.showOK(	"Upload CSV", 
							this.body, 
							userCallback);
				
				kdlLogEvent("Launch Injest Service Modal");
				this.updateAll();
			},
			
			updateAll : function () {
				
				this.fillBody();
			},
			
		};
	
		return ImportServiceModal;            
	}
);