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

//
// >>>>>>   NOT USED IN SPARQLGRAPH 2   <<<<<
//

/*
 *  A simple HTML modal dialog.    DEPRECATED in favor of ModalIidx
 *
 *
 * In your HTML, you need:
 * 		1) the stylesheet
 * 			<link rel="stylesheet" type="text/css" href="../css/modaldialog.css" />
 * 
 * 		2) an empty div named "modaldialog"
 * 			<div id="modaldialog"></div>
 */

var ModalDialog = function(document, varName) {
	/*
	 *   Deprecated, but used in several historical places.
	 *   Please use ModalIidx instead.
	 */
	
	// strangely, "varName" is the name of the variable holding this dialog in your HTML
	// sorry, I didn't know javascript when I wrote this.  -Paul
	this.document = document;
	this.div = document.getElementById("modaldialog");
	this.varName = varName;
	this.callback = null;
	this.numFields = 0;
	this.hide();
	this.valArray = null;
	
};

var LIST_DIALOG_SELECT = 0;
var LIST_DIALOG_TEXT = 1;

ModalDialog.prototype = {
	
	hide : function () {
		this.div.style.visibility = "hidden" ;
	},
	
	show : function () {
		this.div.style.visibility = "visible";
	},
	
	alertMoreLess : function () {
		var div = this.document.getElementById("alertMoreInfoDiv");
		if (div.innerHTML === "") {
			div.innerHTML = "<hr>" + this.moreInfo + "<hr>";
			this.document.getElementById("butAlertMoreLess").innerHTML = "Less";
		} else {
			div.innerHTML = "";
			this.document.getElementById("butAlertMoreLess").innerHTML = "More";
		}

	},
	
	alertDialog : function(title, message, moreInfo, optWidthInclUnits) {
		var widthInclUnits = (typeof optWidthInclUnits == "undefined") ? "60ch" : optWidthInclUnits;
		// Basic error message
		// if moreInfo != null, then there is a "more" button that will show the expanded message
		this.moreInfo = moreInfo;
		var html = '';
		html += '<div id="modaldialog_div" style="width:' + widthInclUnits + ';">';
		html += '<h2>' + title + '</h2>';
		html += message;
		
		if (this.moreInfo !== null) {
			html += '<div align="right">\n';
			html += '<button class="btn btn"         type="button" id="butAlertMoreLess" onclick="' + this.varName + '.alertMoreLess();">More</button>\n';
			html += '</div>\n';
			html += '<div id="alertMoreInfoDiv"></div>\n';
		}
		
		html += '<div class="form-actions" align="center" action="javascript:' + this.varName + '.hide()">\n';
		html += '<button class="btn btn-primary" onclick="' + this.varName + '.hide();">OK</button>\n';
		html += '</div>\n';
		this.div.innerHTML = html;
		this.show();
	},
	
	//==== test field dialog ====//
	textFieldSubmit : function () {
		var ret = [];
		
		// hide
		this.hide();
		
		// build callback param
		for (var i = 0; i < this.numFields; i++) {
			ret.push(this.getFieldValue(i));
		}
		
		// callback
		this.callback(ret);
	},
	
	textDialogCancel : function () {		
		this.hide();
	},
	
	textDialogClear : function () {		
		this.setFieldValue(0,"")
	},
	
	textFieldDialog : function (title, buttonLabel, nameArray, valArray, callback, width) {
		// A dialog full of text fields.   
		// nameArray controls the number of fields and their labels
		// valArray can have default values.  Or it can be empty or shorter than nameArray.
		// callback receives the parameter valArray
		// TODO width
		// width is optional and doesn't work very well
		
		this.numFields = nameArray.length;
		this.callback = callback;
		
		var html = '';
		
		var w = width ? width : 80;   // width defaults to 40
	
		html += '<div id="modaldialog_div" style="width:' + (w + 10) + 'ch;">';
		html += '<form class="form-horizontal" action="javascript:' + this.varName + '.textFieldSubmit()">\n';
		html += '<fieldset>\n';
		html += '<legend>' + title + '</legend>\n';

		for (var i=0; i < nameArray.length; i++) {
			html += '<div class="control-group">\n';
			html += '\t<label class="control-label">' + nameArray[i] +'</label>\n';
			html += '\t<div class="controls">\n';
			html += '\t\t<input type="text" class="input-xlarge" id="' + this.getFieldId(i) + '" ';
			
			if (i < valArray.length) {
				html += 'value="' + valArray[i] + '"';
			}
			html += '>\n';
			html += '\t</div>\n';
			html += '</div>\n';
		}
		html += '<br><br>';
		html += '<button class="btn btn"        type="button" onclick="' + this.varName + '.textDialogClear();">Clear</button>\n';
		html += '<button class="btn btn-danger" type="button" onclick="' + this.varName + '.textDialogCancel();">Cancel</button>\n';
		html += '<button class="btn btn-primary" type="submit">' + buttonLabel + '</button>\n';
		html += '</fieldset>';
		html += '</form>\n';
		html += '</div>\n';

		this.div.innerHTML = html;
		this.show();
	},
	
	/* ************************************  List Dialog ************************************ */
	
	listDialogSelectChanged : function () {
		
	},
	
	listDialogCancel : function () {		
		this.hide();
	},
	
	listDialogClear : function () {		
		var select = this.getFieldElement(LIST_DIALOG_SELECT);
		for (var i=0; i<select.length;i++) {
			select[i].selected = false;
		}
	},
	
	listDialogSubmit : function () {		
		this.hide();
		var selIdx = -1;
		var select = this.getFieldElement(LIST_DIALOG_SELECT);
		for (var i=0; i<select.length;i++) {
			if (select[i].selected) {
				selIdx = i;
			}
		}
		if (selIdx > -1) {
			// return a list containing just the text field
			this.callback(this.valArray[selIdx]);
		} 
	},
	
	listDialog : function (title, buttonLabel, nameArray, valArray, defaultIndex, callback, width) {
		// A dialog full of text fields.   
		// nameArray controls the number of fields and their labels
		// valArray can have default values.  Or it can be empty or shorter than nameArray.
		// callback receives the parameter valArray
		// 
		// width can be:
		//      optional - 70%
		//      number - characters
		//      string - the actual value of width style
		//
		// Things that should be parameterized:
		//      size = 6
		//      select multiple
		
		this.numFields = nameArray.length;
		this.callback = callback;
		this.valArray = valArray;
		
		var selected = null;
		var html = '';
		
		if (typeof width === "undefined") { width = "70%"; }
		else if (typeof width == "number") {width = width + "ch";}
		
	
		html += '<div id="modaldialog_div" style="width:' + width + '";>';
		html += '<form class="form-horizontal" action="javascript:' + this.varName + '.listDialogSubmit()">\n';
		html += '<fieldset>\n';
		html += '<legend>' + title + '</legend>\n';
		// PEC TODO:  width is hardcoded
		// PEC TODO: overflow:auto  does not work
		html += '<select id="' + this.getFieldId(0) + '" size="6" onchange=javascript:' + this.varName + '.listDialogSelectChanged() style="width:90%; overflow-x:auto;">\n';
		
		// populate the <select multiple>
		for (var i=0; i < nameArray.length; i++) {
			if (defaultIndex == i) {
				selected = "SELECTED";
			} else {
				selected = "        ";
			}
			html += '\t<option value="' + valArray[i] + '" ' + selected + ' >' + nameArray[i] +'\n';
		}
		html += '</select>';
		
		html += '<br>';
		
		html += '<button class="btn btn"         type="button" onclick="' + this.varName + '.listDialogClear();">Clear</button>\n';
		html += '<button class="btn btn-danger" type="button" onclick="' + this.varName + '.listDialogCancel();">Cancel</button>\n';
		html += '<button class="btn btn-primary" type="submit">' + buttonLabel + '</button>\n';
		
		html += '</fieldset>';
		html += '</form>\n';
		html += '</div>\n';

		this.div.innerHTML = html;
		this.show();
	},
	
	/* ************************************  Cookies Dialog ************************************ */
	
	cookiesDialog : function (title, buttonLabel, cookieMgr, defaultIndexCookie, nameCookie, cookieNameArray, displayNameArray, callback, width) {
		// complicated dialog of text strings with sets of options stored in cookies
		//
		// defaultIndexCookie - name of cookie that has default index for indexed cookie
		// nameCookie - name of indexed cookie that holds the name of each indexed set
		// cookieNameArray - array of the other names of indexed cookies
		// displayNameArray - parallel array of display names for each indexed cookie
		
		this.numFields = displayNameArray.length;
		this.callback = callback;
		
		var html = '';
		
		var w = width ? width : 40;   // width defaults to 40
		var curIndex = cookieMgr.getCookie(defaultIndexCookie);
		
		html += '<div id="modaldialog_div" style="width:' + (w + 10) + 'ch;">';
		html += title + '<br><br>\n';
		
		// PEC TODO: insert dropdown list of all the names of possible sets
		//           need callback to select a set
		//           need callbacks to delete or add a new set
		//           need the normal callback
		html += '<form action="javascript:' + this.varName + '.textFieldSubmit()"><table>\n';
		
		for (var i=0; i < displayNameArray.length; i++) {
			html += '<tr>';
			html += '<td>' + displayNameArray[i] +'</td>';
			html += '<td><input type="text" class="input-xlarge" id="' + this.getFieldId(i) + '" ';
			if (i < valArray.length) {
				html += 'value="' + cookieMgr.getIndexedCookie(nameArray[i], curIndex) + '"';
			}
			html += 'size="' + w + '"></td></tr>\n';
		}
	    
		html += '</table>\n';
		html += '<br><button class="btn btn-primary" type="submit">' + buttonLabel + '</button>\n';
		html += '</form>\n';
		html += '</div>\n';
	
		this.div.innerHTML = html;
		this.show();
	},
	
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