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

var ModalConstraintDialog = function(document, varName) {
	// strangely, "varName" is the name of the variable holding this dialog in your HTML
	// "divId" is the id of a <div> somewhere (anywhere) in your HTML that is otherwise unused
	// sorry.  We all had to learn javascript at some time.  No resources to fix this now.
	this.document = document;
	this.div = document.getElementById("modaldialog");
	this.varName = varName;
	this.callback = null;
	this.item = null;
	this.hide();
	this.sparql = "";
	this.qsinterface = null;
};

var MCD_FIELD_SELECT = 0;
var MCD_FIELD_TEXT = 1;


/* ************************************  List Dialog ************************************ */
ModalConstraintDialog.prototype = {
		
	hide : function () {
		this.div.style.visibility = "hidden" ;
	},
	
	show : function () {
		this.div.style.visibility = "visible";
	},
		
	selectChanged : function () {
		// build the text field (field 1) using the selected fields in the <select> (field 0)
		var select = this.getFieldElement(MCD_FIELD_SELECT);
		var opt;
		var valList = [];
		
		for (var i=0; i<select.length;i++) {
			opt = select[i];
			// if option is selected and value is not null then use it
			// note: javascript and html team up to change null to "null"
			if (opt.selected && opt.value != null && opt.value != "null") {
				// PEC TODO: separator is currently hard-coded " "
				valList.push(opt.value);
			}
		}
		
		this.setFieldValue(MCD_FIELD_TEXT, this.item.buildValueConstraint(valList));
	},
	
	cancel : function () {		
		this.hide();
	},
	
	clear : function () {		
		var select = this.getFieldElement(MCD_FIELD_SELECT);
		for (var i=0; i<select.length;i++) {
			select[i].selected = false;
		}
		this.setFieldValue(MCD_FIELD_TEXT, "");
	},
	
	submit : function () {		
		this.hide();
		
		// return a list containing just the text field
		this.callback([this.getFieldValue(MCD_FIELD_TEXT)]);
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
	
		this.setRunningQuery(true);
		this.qsinterface.executeAndParse(this.sparql, this.queryCallback.bind(this));
	},
	
	queryCallback : function (qsResult) {
		
		this.setRunningQuery(false);

		if (!qsResult.isSuccess()) {
			alert("Error retrieving possible values from the SPARQL server\n\n" + qsResult.getStatusMessage()); 
			
		} else if (qsResult.getRowCount() < 1) {
			alert("No possible values exist for " + this.item.getSparqlID() + "\n\nOther constraints may be too tight.\nOr no instance data exists.");
		
		} else {
			
			// get limit from the query
			var limitPat = /LIMIT ([0-9]+)/m;
			var limitArr = limitPat.exec(this.sparql);
			if (!limitArr || limitArr.length != 2) {
				alert("Assertion Failed: ModalConstraintDialog.queryCallback 001: query has not LIMIT statement.");
			}
			var limit=limitArr[1];
			
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
			if (qsResult.getRowCount() == limit) {
				this.setStatusAlert("Too many possible values.  A random subset of " + limit + " are shown. Consider the Regex option."); 
				element[limit] = {name: "", val: ""};
				element[limit].name = "...";
				element[limit].val = null;
			};
			
			this.fillSelect(element);
		};
	},
	
	filter : function() {
		var newConstraint = this.item.buildFilterConstraint(null, null);
		
		this.setFieldValue(MCD_FIELD_TEXT, newConstraint);
	},
	

	fillSelect : function (element) {
		// element should be an array of items with ".name" and ".val" fields
		// populate the SELECT with given parallel arrays of names and values
		var select = this.getFieldElement(MCD_FIELD_SELECT);
		var textVal = this.getFieldValue(MCD_FIELD_TEXT);
		
		select.options.length = 0;
		
		for (var i=0; i < element.length; i++) {
			var el = this.document.createElement("option");
			// fill the element
			
			el.textContent = element[i].name; 
			 
			el.value = element[i].val;
			
			// check to see if it should be selected
			if (textVal.indexOf(element[i].val) > -1) {
				el.selected = true;
			}
			
			// add element
			select.appendChild(el);
		}
		//this.selectChanged();   // build the text VALUE constraint
	},
	
	constraintDialog : function (item, sparql, qsinterface, callback, autoSuggestFlag) {
		// 
		// query - query should get choices and have a big limit.  Don't ORDER BY, because sparql is too slow
		// autoSuggestFlag - instead of a "suggest" button, perform suggestion query automatically
		this.item = item;
		var defaultResult = item.getConstraints();
		
		this.callback = callback;
		this.sparql = sparql;            // query to get choices.  needs a LIMIT <maxChoices> clause
		this.qsinterface = qsinterface;
		var html = '';
		var style = '';
	
		html += '<div id="modaldialog_div" style="width: 50%;">';
		html += '	<form class="form" action="javascript:' + this.varName + '.submit()">\n';
		html += '	<fieldset>\n';
		html += '		<legend>Constrain ' + this.item.getSparqlID().slice(1) + '</legend>\n';
		html += '		<select multiple="multiple" id="' + this.getFieldId(MCD_FIELD_SELECT) + '" size="10" onchange=javascript:' + this.varName + '.selectChanged() style="width:100%;"></select>\n';
			
		html += '		<div class="form-actions" align="left">\n';
		
		if (autoSuggestFlag) 
			style = ' style="display:none;"';   // hide the "Suggest" button if suggestions have already been generated
		else
			style = '';
		html += '			<button class="btn btn" id="btnSuggest" type="button" onclick="' + this.varName + '.query();"' + style + '>Suggest Values</button>\n';
		
		html += '			<button class="btn btn" id="btnRegex"   type="button" onclick="' + this.varName + '.filter();">Regex Template</button>\n';
		html += '		</div>\n';

		html += '		Constraint<br>';
		html += '		<textarea rows="3" class="input-xlarge" id="' + this.getFieldId(MCD_FIELD_TEXT) + '" value="" style="width:100%; max-width:100%;"></textarea>';
		html += '	</fieldset>\n';
		
		html += '       <div id="mcdstatus" align="left"></div>';
		
		html += '	<div class="form-actions" align="right">\n';
		html += '		<button class="btn btn"         type="button" onclick="' + this.varName + '.clear();">Clear</button>\n';
		html += '		<button class="btn btn-danger"  type="button" onclick="' + this.varName + '.cancel();">Cancel</button>\n';
		html += '		<button class="btn btn-primary" type="submit">Submit</button>\n';
		html += '	</div>\n';
		html += '	</form>\n';

		html += '</div>\n';
		
		this.div.innerHTML = html;
		this.show();
		this.setFieldValue(MCD_FIELD_TEXT, defaultResult);
		if (autoSuggestFlag) {
			this.query();
		}
	},
	
	// PEC TODO:  these should be inherited from modalDialog, I think.
	getFieldId : function(n) {
		return "modalDialogField_" + n;
	},
	
	getFieldValue : function(n) {
		return this.document.getElementById(this.getFieldId(n)).value;
	},
	
	getFieldElement : function(n) {
		return this.document.getElementById(this.getFieldId(n));
	},
	
	setFieldValue : function(n, val) {
		this.document.getElementById(this.getFieldId(n)).value = val;
	},
};
