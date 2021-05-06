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
            'sparqlgraph/js/importspec',
            'sparqlgraph/js/importmapping',
        	'sparqlgraph/js/modaliidx',
            'sparqlgraph/js/msiclientnodegroupservice',
        	'sparqlgraph/js/sparqlconnection',
            'sparqlgraph/js/sparqlgraphjson',
        	'jquery',

			// shimmed
        	'sparqlgraph/js/belmont'
		],
	function(IIDXHelper, ImportSpec, ImportMapping, ModalIidx, MsiClientNodeGroupService, SparqlConnection, SparqlGraphJson, $) {

		var ModalImportSpecWizardDialog = function(importSpec, conn, nodegroup, ngClient, iSpecJsonCallback) {
			this.importSpec = importSpec;
			this.conn = conn;
			this.nodegroup = nodegroup;
			this.ngClient = ngClient;
			this.iSpecJsonCallback = iSpecJsonCallback;

            this.actionSelect = null;
            this.lookupModeSelect = null;
            this.lookupRegexText = null;
            this.messageDiv = null;

            this.testSelect = null;
		};


		ModalImportSpecWizardDialog.prototype = {

			submit : function () {

                // only submit if something useful is filled in
                if (this.actionSelect.value == "Build from nodegroup" || this.lookupModeSelect.disabled == false) {

                    var successCallback = function(sgjson) {
                        this.iSpecJsonCallback(sgjson.getImportSpecJson());
                    }.bind(this);

                    this.ngClient.execAsyncSetImportSpecFromReturns(
                        new SparqlGraphJson(this.conn, this.nodegroup, this.importSpec, false),
                        this.actionSelect.value.trim(),
                        this.lookupRegexText.value.trim(),
                        this.lookupModeSelect.value.trim(),
                        successCallback,
                        ModalIidx.alert.bind(this, "Nodegroup service failure")
                    )
                }
			},

            // update the select after other inputs on the screen have changed
            // call with initUnionText to force union, otherwise null rebuilds list and keeps value
            updateAll : function () {
                this.lookupModeSelect.disabled =  (this.lookupRegexText.value.trim().length == 0);

                // *********** end form ***********
                if (this.importSpec && this.importSpec.getSortedColumns().length > 0) {
                    if (this.actionSelect.value == " ") {
                        if (this.lookupModeSelect.disabled) {
                            this.messageDiv.innerHTML = "";
                        } else {
                            this.messageDiv.innerHTML = "<center><span class='label label-info'>This will overwrite the existing URI lookups.</span></center>";
                        }
                    } else {
                        this.messageDiv.innerHTML = "<center><span class='label label-info'>This will overwrite the existing import mapping and CSV.</span></center>";
                    }
                }
            },

			show : function () {
				var dom = document.createElement("fieldset");
				dom.id = "ModalImportSpecWizardDialogDom";
				var title = "Import Mapping Wizard";

				// ********  horizontal form with fieldset *********
				var form = IIDXHelper.buildHorizontalForm();
				dom.appendChild(form);
				var fieldset = IIDXHelper.addFieldset(form);

				// Columns select
                var selectList = [" ", "Build from nodegroup"];
                var selected = ["Build from nodegroup"];

				this.actionSelect = IIDXHelper.createSelect("ispecWizard.actionSelect", selectList, selected, false, "input-large");
                this.actionSelect.onchange = this.updateAll.bind(this);
                var span = document.createElement("span");
                span.appendChild(this.actionSelect);
                span.appendChild(IIDXHelper.createNbspText());
                span.appendChild(ModalIidx.createInfoButton("<b>Build from nodegroup</b> - Creates new CSV columns and mappings.  Each nodegroup return is used as a CSV column, and that column is mapped to the returned item.", undefined, true));
                fieldset.appendChild(IIDXHelper.buildControlGroup("Columns: ", span));

                // Lookup regex text
                this.lookupRegexText = IIDXHelper.createTextInput("ispecWizard.lookupRegexText", "input-large");
                this.lookupRegexText.onchange = this.updateAll.bind(this);
                span = document.createElement("span");
                span.appendChild(this.lookupRegexText);
                span.appendChild(IIDXHelper.createNbspText());
                span.appendChild(ModalIidx.createInfoButton("<b>Adds URILookups</b>  Any property bound to a SPARQL variable names matching this regex will be set to look up its parent node.", undefined, true));
                fieldset.appendChild(IIDXHelper.buildControlGroup("Lookup Regex: ", span));

                // Lookup mode select
                selectList = ImportMapping.LOOKUP_MODE_LIST;
                selected = [ImportMapping.LOOKUP_MODE_CREATE];
                selected.disabled = true;

                this.lookupModeSelect = IIDXHelper.createSelect("ispecWizard.lookupModeSelect", selectList, selected, false, "input-large");
                this.lookupModeSelect.onchange = this.updateAll.bind(this);
                span = document.createElement("span");
                span.appendChild(this.lookupModeSelect);
                span.appendChild(IIDXHelper.createNbspText());
                span.appendChild(ModalIidx.createInfoButton("This lookup mode will be used for auto-generated URI lookups.", undefined, true));
                fieldset.appendChild(IIDXHelper.buildControlGroup("Lookup Mode: ", span));

                // messageDiv form div
                this.messageDiv = document.createElement("div");
                dom.appendChild(this.messageDiv);

				ModalIidx.okCancel(title, dom, this.submit.bind(this));
                this.updateAll();
			},

		};

		return ModalImportSpecWizardDialog;            // return the constructor
	}
);
