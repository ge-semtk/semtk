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

/*
 *  A simple HTML modal dialog.
 *
 *
 * In your HTML, you need:
 * 		1) the stylesheet
 * 			<link rel="stylesheet" type="text/css" href="../css/modaldialog.css" />
 * 
 * 		2) an empty div named "modaldialog"
 * 			<div id="modaldialog"></div>
 */

define([	// properly require.config'ed
        	'sparqlgraph/js/iidxhelper',
        	'sparqlgraph/js/modaliidx',
        	'jquery',
        	
			// shimmed
        	'sparqlgraph/js/belmont',
        	'bootstrap/bootstrap-tooltip',
        	'bootstrap/bootstrap-transition'
			
		],
	function(IIDXHelper, ModalIidx, $) {
	
		var ModalItemDialog = function(item, nodegroup, clientOrInterface, callback, data, optGhostItem, optGhostNodegroup) {
			// callback(item, sparqlID, optionalFlag, constraintStr, data)
			//          where sparqlID and contraintStr could be ""
			// data.draculaLabel
			// data.textId
			//
			// GHOST ITEM and NODEGROUP:
			//  GhostNodegroup : copy of nodegroup with changes made (e.g. remove trigger class in sparqlForm)
			//  GhostItem : corresponding to item
			//  These will be used to generate the SPARQL
			this.item = item;
			this.nodegroup = nodegroup;
			this.clientOrInterface = clientOrInterface;
			this.callback = callback;
			this.data = data;
			
			this.limit = 300;
			this.lastSparqlID = "";
			
			this.ghostItem = typeof optGhostItem === "undefined" ? false : optGhostItem;
			this.ghostNodegroup = typeof optGhostNodegroup === "undefined" ? false : optGhostNodegroup;
			
			this.sparqlformFlag = false;
		};
		
		
		ModalItemDialog.SELECT = 0;
		ModalItemDialog.CONSTRAINT_TEXT = 1;
		ModalItemDialog.SPARQL_ID_TEXT = 2;
		ModalItemDialog.OPTIONAL = 3;
		ModalItemDialog.RETURN_CHECK = 4;
		
		ModalItemDialog.prototype = {
				
			selectChanged : function () {
				// build the text field using the selected fields in the <select> 
				
				var select = this.getFieldElement(ModalItemDialog.SELECT);
				var opt;
				var valList = [];
				
				for (var i=0; i < select.length;i++) {
					opt = select[i];
					// if option is selected and value is not null then use it
					// note: javascript and html team up to change null to "null"
					if (opt.selected && opt.value != null && opt.value != "null") {
						// PEC TODO: separator is currently hard-coded " "
						valList.push(opt.value);
					}
				}
				
				// swap in sparqlID
				var savedID = this.item.getSparqlID();
				this.item.setSparqlID(this.getSparqlIDText());
				
				// build new constraints
				this.setFieldValue(ModalItemDialog.CONSTRAINT_TEXT, this.item.buildValueConstraint(valList));
				
				// swap sparqlID back out
				this.item.setSparqlID(savedID);
			},
			
			clear : function () {	
				// clear button
				
				// uncheck "return"
				if (! this.sparqlformFlag) {
					var returnCheck = this.getFieldElement(ModalItemDialog.RETURN_CHECK);
					returnCheck.checked = false;
					this.returnCheckOnClick();   // handles any disabling fields
				}
				
				// uncheck "optional"
				var optionalCheck = this.getFieldElement(ModalItemDialog.OPTIONAL);
				optionalCheck.checked = false;		
				
				// unselect everything
				var select = this.getFieldElement(ModalItemDialog.SELECT);
				for (var i=0; i<select.length;i++) {
					select[i].selected = false;
				}
				
				// clear the constraint
				this.setFieldValue(ModalItemDialog.CONSTRAINT_TEXT, "");
				
				// note that we leave the sparqlID
			},
			
			submit : function () {		
				
				var sparqlIDElem = this.getFieldElement(ModalItemDialog.SPARQL_ID_TEXT);
				var sparqlID = this.getSparqlIDText();
				
				// return a list containing just the text field
				this.callback(	this.item, 
								this.getFieldElement(ModalItemDialog.RETURN_CHECK).checked ? sparqlID : "",
								this.getFieldElement(ModalItemDialog.OPTIONAL).checked,
								this.getFieldValue(ModalItemDialog.CONSTRAINT_TEXT),
								this.data
							  );
			},
			
			setStatus : function (msg) {
				document.getElementById("mcdstatus").innerHTML= "<font color='red'>" + msg + "</font>";
			},
			
			setStatusAlert : function (msg, severity) {
				// make status box an iidx alert
				// severity = "alert-success", "alert-info", "alert-error", or ""
				document.getElementById("mcdstatus").innerHTML =' <div class="alert ' + severity + '">' + msg + '</div>';
			},
			
			setRunningQuery : function (flag) {
				if (flag) {
					this.setStatus("Running query...");
			    	document.getElementById("btnSuggest").className = "btn disabled";
			    	document.getElementById("btnSuggest").disabled = true;
				} else {
					this.setStatus("");
			    	document.getElementById("btnSuggest").className = "btn";
			    	document.getElementById("btnSuggest").disabled = false;
				};
			},
			
			query : function () {
				
				// use ghosts if they exist
				var queryItem = this.ghostItem ? this.ghostItem : this.item;
				var queryNodegroup = this.ghostNodegroup ? this.ghostNodegroup : this.nodegroup;
				
				this.setRunningQuery(true);
				
				// assign temp sparqlID if needed
				var blankFlag = false;
				if (queryItem.getSparqlID() == "") {
					blankFlag = true;
					queryItem.setSparqlID(this.getSparqlIDText());
				}
				
				// generate query
				var sparql = queryNodegroup.generateSparql(SemanticNodeGroup.QUERY_CONSTRAINT, false, this.limit, queryItem);
				
				// reset blank sparqlID if needed
				if (blankFlag) {
					queryItem.setSparqlID("");
				}
				
				// run query
				this.clientOrInterface.executeAndParse(sparql, this.queryCallback.bind(this));
			},
			
			queryCallback : function (qsResult) {
				
				this.setRunningQuery(false);
		
				if (!qsResult.isSuccess()) {
					alert("Error retrieving possible values from the SPARQL server\n\n" + qsResult.getStatusMessage()); 
					
				} else if (qsResult.getRowCount() < 1) {
					alert("No possible values exist for " + this.item.getSparqlID() + "\n\nOther constraints may be too tight.\nOr no instance data exists.");
				
				} else {
					
					// get all the return values
					
					var element = [];
					
					for (var i=0; i <qsResult.getRowCount(); i++) {
						element[i] = {	name: qsResult.getRsData(i, 0, qsResult.NAMESPACE_NO), 
								      	val: qsResult.getRsData(i, 0, qsResult.NAMESPACE_YES)
								     };
						
						if (element[i].val == "" || element[i].name=="") {
							alert("Error: Got a null value returned from SPARQL server");
							return;
						};
					}
					
					element = element.sort(function(a,b){ 
												if (a.name < b.name) return -1;
												if (a.name > b.name) return 1;
												return 0;
											});				
					
					// insert "..." if we hit the limit
					if (qsResult.getRowCount() == this.limit) {
						this.setStatusAlert("Too many possible values.  A random subset of " + this.limit + " are shown. Consider the Regex option."); 
						element[this.limit] = {name: "", val: ""};
						element[this.limit].name = "...";
						element[this.limit].val = null;
					};
					
					this.fillSelect(element);
				};
			},
			
			getSparqlIDText : function() {
				// get sparqlID out of text box, ensure single leading '?', remove spaces
				// may return ""
				
				var ret = this.getFieldValue(ModalItemDialog.SPARQL_ID_TEXT).trim();
				while (ret[1] === '?') { 
					ret = ret.slice(1);
				} 
				if (ret.length > 0 && ret[0] !== '?') {
					ret = '?' + ret;
				}
				return ret;
			},
			
			buildRegex : function() {
				// swap in sparqlID
				var savedID = this.item.getSparqlID();

				this.item.setSparqlID(this.getSparqlIDText());
				
				//  build the regex, swap id back out
				var newConstraint = this.item.buildFilterConstraint(null, null);
				this.item.setSparqlID(savedID);
				
				this.setFieldValue(ModalItemDialog.CONSTRAINT_TEXT, newConstraint);
			},
			
			fillSelect : function (element) {
				// element should be an array of items with ".name" and ".val" fields
				// populate the SELECT with given parallel arrays of names and values
				var select = this.getFieldElement(ModalItemDialog.SELECT);
				var textVal = this.getFieldValue(ModalItemDialog.CONSTRAINT_TEXT);
				var valuesFlag = (textVal.search("VALUES ") > -1);
				select.options.length = 0;
				
				for (var i=0; i < element.length; i++) {
					var el = document.createElement("option");
					// fill the element
					
					el.textContent = element[i].name; 
					 
					el.value = element[i].val;
					
					// check to see if it should be selected
					if (valuesFlag) {
						var searchFor = "['<]" + IIDXHelper.regexSafe(element[i].val) + "['>]";
						if (textVal.search(searchFor) > -1) {
							el.selected = true;
						}
					}
					
					// add element
					select.appendChild(el);
				}
				
			},
			
			returnCheckOnClick : function() {
				
				// nothing to do any more
				
			},
			
			sparqlIDOnFocus : function () {
				this.lastSparqlID = this.getSparqlIDText();
			},
			
			sparqlIDOnFocusOut : function() {
				// keep sparqlID valid at all times
				
				var retName = this.getSparqlIDText();
				var f = new SparqlFormatter();
				
				// handle blank sparqlID
				if (retName === "") {
					retName = f.genSparqlID(this.item.getKeyName(), gNodeGroup.sparqlNameHash);
					ModalIidx.alert("Blank SparqlID Invalid", "Using " + retName + ".");
					
				// check legality of non-blank sparqlID
				} else {
		    		// for legality-checking: make sure retName has "?"
		    		if (retName[0][0] !== "?") {
		    			retName = "?" + retName;
		    		}
		    		
		    		// if it is a new name
		    		if (retName != this.item.getSparqlID()) {
		    			// make sure new name is legal
		    			var newName = f.genSparqlID(retName, gNodeGroup.sparqlNameHash);
		    			if (newName != retName) {
		    				ModalIidx.alert("SparqlID Invalid", "Using " + newName + " instead.");
		    			}
		    			retName = newName;
		    		} 
		    	}
				// set the new sparqlID (without the leading '?')
				this.setFieldValue(ModalItemDialog.SPARQL_ID_TEXT, retName.slice(1));
				
				this.updateConstraintSparqlID(this.lastSparqlID, retName);
			},
			
			updateConstraintSparqlID : function(oldID, newID) {
				var constraint = this.getFieldValue(ModalItemDialog.CONSTRAINT_TEXT);
				
				var delims0 = [' ', ',', '\\('];
				var delims1 = [' ', ',', '('];
				for (var i=0; i < delims0.length; i++) {
					constraint = constraint.replace(new RegExp('\\' + oldID + delims0[i], 'g'), newID + delims1[i]);
				}
				
				this.setFieldValue(ModalItemDialog.CONSTRAINT_TEXT, constraint);
				
			},
			
			
			
			show : function (optSparqlformFlag) {
				if (typeof optSparqlformFlag != "undefined") {
					this.sparqlformFlag = optSparqlformFlag;
				}
				 
				var dom = document.createElement("fieldset");
				var elem;
				var title = this.item.getSparqlID().slice(1); // old: this.item.getKeyName();
				
				
				// return button
				var returnCheck;
				var sparqlIDTxt;
				var optionalCheck;
				var table;
				var tr;
				var td;
				var form;
				
				// table for return items
				table = document.createElement("table");
				dom.appendChild(table);
				
				tr = document.createElement("tr");
				table.appendChild(tr);
				
				// row 1 col 1:  "Name: "
				td = document.createElement("td");
				td.style.verticalAlign = "top";
				tr.appendChild(td);
				elem = document.createElement("b");
				elem.appendChild(document.createTextNode("Name: " ) );
				td.appendChild(elem);
				
				// row 1 col 2   "text input box"
				td = document.createElement("td");
				tr.appendChild(td);
				
				// return input
				sparqlIDTxt = document.createElement("input");
				sparqlIDTxt.type = "text";
				sparqlIDTxt.style.margin = 0;
				sparqlIDTxt.id = this.getFieldID(ModalItemDialog.SPARQL_ID_TEXT);
				// get a legal sparqlID
				var sparqlID = this.item.getSparqlID();
				if (sparqlID === "") {
					var f = new SparqlFormatter();
					sparqlID = f.genSparqlID(this.item.getKeyName(), gNodeGroup.sparqlNameHash);
				}
				sparqlIDTxt.value = sparqlID.slice(1);
				sparqlIDTxt.onfocus    = this.sparqlIDOnFocus.bind(this);
				sparqlIDTxt.onfocusout = this.sparqlIDOnFocusOut.bind(this);
				sparqlIDTxt.style.disabled = this.sparqlformFlag;
				td.appendChild(sparqlIDTxt);
							
				// row 1 col 3  "returned checkbox"
				td = document.createElement("td");
				td.style.verticalAlign = "top";
				tr.appendChild(td);
				
				// return checkbox
				returnCheck = IIDXHelper.createVAlignedCheckbox();
				returnCheck.classList.add("btn");
				returnCheck.id = this.getFieldID(ModalItemDialog.RETURN_CHECK);
				returnCheck.checked = this.item.getIsReturned();
				returnCheck.onclick = this.returnCheckOnClick.bind(this);
				
				td.appendChild( document.createTextNode( '\u00A0\u00A0' ) );
				td.appendChild(returnCheck);
				td.appendChild(document.createTextNode(" return"));
				
				// row #2
				tr = document.createElement("tr");
				table.appendChild(tr);
				
				// first cell row 3 is empty
				td = document.createElement("td");
				tr.appendChild(td);
				
				// second cell row 3 is empty
				td = document.createElement("td");
				tr.appendChild(td);
				
				// third cell row 3
				td = document.createElement("td");
				td.style.verticalAlign = "top";
				tr.appendChild(td);
				
				// optional checkbox
				
				optionalCheck = IIDXHelper.createVAlignedCheckbox();
				optionalCheck.id = this.getFieldID(ModalItemDialog.OPTIONAL);
				// snode doesn't have isOptional
				if (this.item.getIsOptional) {
					optionalCheck.checked = this.item.getIsOptional();
				} else {
					optionalCheck.disabled = true;
				}
				optionalCheck.onclick = function () {
					var e = this.getFieldElement(ModalItemDialog.OPTIONAL);
					e.value = e.checked;  
				}.bind(this);
				
				td.appendChild( document.createTextNode( '\u00A0\u00A0' ) );

				// Top section is handled totally differently with sparqlformFlag
				if (this.sparqlformFlag) {
					// create a right-justified div just for optional
					var div = document.createElement("div");
					div.align = "right";
					
					// add tooltip
					optionalCheck.title = "Match rows missing this value. NOTE: still under development.";
					optionalCheck.setAttribute("rel", "tooltip");	
					
					// assemble
					div.appendChild(optionalCheck)
					div.appendChild(document.createTextNode(" optional"));
					dom.appendChild(div);
					
					// make top table invisible
					table.style.display = "none";
					
				} else {
					// normal operation: put optional check into the top table
					td.appendChild(optionalCheck)
					td.appendChild(document.createTextNode(" optional"));
				}
				
				// Constraint
				elem = document.createElement("b");
				elem.innerHTML = "Constraint";
				dom.appendChild(elem);
				dom.appendChild(document.createElement("br"));
				
				
				// Constraint text area
				elem = document.createElement("textarea");
				elem.rows = "3";
				elem.classList.add("input-xlarge");
				elem.id = this.getFieldID(ModalItemDialog.CONSTRAINT_TEXT);
				elem.value = this.item.getConstraints();
				elem.style.width = "100%";
				elem.style.maxWidth = "100%";
				elem.style.boxSizing = "border-box";
				dom.appendChild(elem);
				
				// value <select>
				elem = document.createElement("select");
				elem.multiple = true;
				elem.id = this.getFieldID(ModalItemDialog.SELECT);
				elem.size = "10";
				elem.onchange = this.selectChanged.bind(this);
				elem.style.width = "100%";
				dom.appendChild(elem);
				
				// ----- buttons div under the select -----
				var div = document.createElement("div");
				//div.classList.add("form-actions");
				div.style.textAlign = "left";
				dom.appendChild(div);
				
				
				// suggest values button
				elem = document.createElement("button");
				elem.classList.add("btn");
				elem.id = "btnSuggest";
				elem.type = "button";
				elem.onclick = this.query.bind(this);
				if (this.sparqlformFlag) {
					elem.style.display = "none";   // hide the "Suggest" button if suggestions have already been generated
				}
				elem.innerHTML = "Suggest Values";
				div.appendChild(elem);
				
				if (this.sparqlformFlag) {
					div.appendChild(document.createTextNode(" "));
				}
				
				// regex button
				elem = document.createElement("button");
				elem.classList.add("btn");
				elem.id = "btnRegex";
				elem.type = "button";
				elem.onclick = this.buildRegex.bind(this);
				elem.innerHTML = "Regex Template";
				div.appendChild(elem);
				// ----- done with buttons under select -----

				// status
				elem = document.createElement("div");
				elem.id = "mcdstatus";
				elem.style.textAlign = "left";
				dom.appendChild(elem);
				
				ModalIidx.clearCancelSubmit(title, dom, this.clear.bind(this), this.submit.bind(this));   
				
				if (this.sparqlformFlag) {
					this.query();
				} else {
					// Set returned if it looks like this dialog is totally empty
					if (this.item.getIsReturned() == false && this.item.getConstraints() == "") {
						returnCheck.checked = true;
						this.returnCheckOnClick();
					}
				}
				
				// tooltips
				$("#" + this.getFieldID(ModalItemDialog.OPTIONAL)).tooltip({placement: "left"});
			},
			
			// ------ manage unique id's ------
			// would be actually unique if there were GUID in the constructor.  overkill.
			getFieldID : function(n) {
				return "modalItemField_" + n;
			},
			
			getFieldValue : function(n) {
				return document.getElementById(this.getFieldID(n)).value;
			},
			
			getFieldElement : function(n) {
				return document.getElementById(this.getFieldID(n));
			},
			
			setFieldValue : function(n, val) {
				document.getElementById(this.getFieldID(n)).value = val;
			},
		};
		
		return ModalItemDialog;            // return the constructor
	}
);
