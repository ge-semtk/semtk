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
        	'bootstrap/bootstrap-transition',
        	'sparqlgraph/jquery/jquery-ui-1.10.4.min'
			
		],
	function(IIDXHelper, ModalIidx, $) {
	
		var ModalLinkDialog = function(item, sourceSNode, targetSNode, nodegroup, callback, data) {
			// callback(snode, item, data, optionalFlag, deleteFlag)
			// data = {draculaLabel: label}
			
			this.item = item;
			this.sourceSNode = sourceSNode;
			this.targetSNode = targetSNode;
			this.nodegroup = nodegroup;
			this.callback = callback;
			this.data = data;
		};
	
		ModalLinkDialog.prototype = {
				
			
			clear : function () {	
				document.getElementById("ModalLinkDialog.optionalCheck").checked = false;
				document.getElementById("ModalLinkDialog.deleteCheck").checked = false;
			},
			
			submit : function () {		
				
				// return a list containing just the text field
				this.callback(	this.sourceSNode, 
								this.item,
								this.targetSNode,
								this.data,
								document.getElementById("ModalLinkDialog.optionalCheck").checked,
								document.getElementById("ModalLinkDialog.deleteCheck").checked
							  );
			},
			
			
			show : function () {
				var dom = document.createElement("fieldset");
				dom.id = "ModalLinkDialogdom";
				var title = this.sourceSNode.getSparqlID() + "-- " + this.item.getKeyName() + " --" + this.targetSNode.getSparqlID();
				
				
				// optional checkbox
				
				optionalCheck = IIDXHelper.createVAlignedCheckbox();
				optionalCheck.id = "ModalLinkDialog.optionalCheck";
				optionalCheck.checked = false;    // PEC TODO
				
				deleteCheck = IIDXHelper.createVAlignedCheckbox();
				deleteCheck.id = "ModalLinkDialog.deleteCheck";
				deleteCheck.checked = false;			

				// normal operation: put optional check into the top table
				dom.appendChild(optionalCheck)
				dom.appendChild(document.createTextNode(" optional"));
				dom.appendChild(document.createElement("br"));
				dom.appendChild(deleteCheck)
				dom.appendChild(document.createTextNode(" delete this link"));
				
				// compute a width
				var width = title.length * 1.3;
				width = Math.max(30, width);
				width = Math.floor(width);
				
				ModalIidx.clearCancelSubmit(title, dom, this.clear.bind(this), this.submit.bind(this), "OK", width.toString() + "ch");   
				
			},
			
		};
		
		return ModalLinkDialog;            // return the constructor
	}
);
