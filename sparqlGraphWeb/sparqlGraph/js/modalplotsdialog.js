/**
 ** Copyright 2021 General Electric Company
 **
 ** Authors:  Paul Cuddihy
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
            'sparqlgraph/js/sparqlgraphjson',
        	'jquery',

			// shimmed
            'sparqlgraph/js/belmont'
		],
	function(IIDXHelper, ModalIidx, SparqlGraphJson, $) {

		var ModalPlotsDialog = function(ngsClient, sgJson, tableResults, successCallback) {

            this.ngsClient = ngsClient;
			this.sgJson = sgJson.deepCopy();
            this.tableResults = tableResults;
			this.callback = successCallback;

            this.m = null;
            this.select = null;
            this.displayIndex = -1;
            this.text = null;

            this.addModalElemCols = null;
            this.addModalElemName = null;
            this.addModalElemType = null;
		};

		ModalPlotsDialog.prototype = {

			submit : function () {
                var handler = this.sgJson.getPlotSpecsHandler();
                handler.setDefaultIndex(this.displayIndex);
                this.callback(handler);
			},

            show : function (index) {
                var dom = document.createElement("div");
                var span = document.createElement("span");
                dom.appendChild(span);

                // select (placeholder)
				var select = IIDXHelper.createSelect("ModelPlotsDialog.select", [], [], false, "input-xlarge");
                select.onchange = this.selectChanged.bind(this);
                select.style.margin = "0px";
                this.select = select;
                span.appendChild(select);

                // add button
                span.appendChild(document.createTextNode(" "));
                span.appendChild(IIDXHelper.createIconButton("icon-plus", this.addButtonCallback.bind(this), undefined, undefined, "Add"));

                // delete button
                span.appendChild(document.createTextNode(" "));
                span.appendChild(IIDXHelper.createIconButton("icon-remove", this.removeCallback.bind(this), undefined, undefined, "Delete"));

                // reset button
                span.appendChild(document.createTextNode(" "));
                span.appendChild(IIDXHelper.createIconButton("icon-refresh", this.resetCallback.bind(this), undefined, undefined, "Reset"));

                // info button
                span.appendChild(document.createTextNode(" "));
                var msg =  "JSON docs are at <a href='https://plotly.com/javascript/#basic-charts' target='_blank'>plotly.com/javascript</a><br><br>" +
                            "SemTK will replace values into the JSON.  See the wiki <a href='https://github.com/ge-semtk/semtk/wiki/Plotting' target='_blank'>Plotting page</a>";
                span.appendChild(ModalIidx.createInfoButton(msg, undefined, true));


                var text = document.createElement("textarea");
                text.classList.add("input-xlarge");
                text.rows=25;
                text.style.width="99%";
                this.text = text;
                dom.appendChild(text);

                if (index == -1 && this.sgJson.getPlotSpecsHandler().getNumPlots() > 0) {
                    this.displayIndex = 0;
                } else {
                    this.displayIndex = index;
                }

				this.m =  ModalIidx.okCancel("Edit Plots", dom, this.submit.bind(this), undefined, undefined, this.validateAndStorePlot.bind(this));

                this.updateAll(true);
            },

            addButtonCallback : function() {
                if ( ! this.validateAndStoreWithDialog() ) {
                    return;
                }

                var div = document.createElement("div");
                var form = IIDXHelper.buildHorizontalForm();
                div.appendChild(form);
                var fieldset = IIDXHelper.addFieldset(form)

                var ng = new SemanticNodeGroup();
                this.sgJson.getNodeGroup(ng)
                var cols = ng.getReturnedSparqlIDs();
                var colsSel = cols.slice(0,2);
                this.addModalElemCols = IIDXHelper.createSelect("mpd.add.colselect", cols, colsSel, true, "input-large");
                this.addModalElemCols.size=3;

                this.addModalElemName = IIDXHelper.createTextInput("mpd.add.name");
                this.addModalElemType = IIDXHelper.createSelect("mpd.add.select", ["bar", "scatter"], ["bar"], false, "input-large");
                fieldset.appendChild(IIDXHelper.buildControlGroup("Name: ", this.addModalElemName));
                fieldset.appendChild(IIDXHelper.buildControlGroup("Type: ", this.addModalElemType));
                fieldset.appendChild(IIDXHelper.buildControlGroup("Columns: ", this.addModalElemCols));

                var m = new ModalIidx("ModalIidxOkCancel-AddPlot");
                m.showOKCancel("Add Plot", div,  this.addCallbackValidate.bind(this), this.addCallbackCallNgs.bind(this), this.unstack.bind(this));
                this.stack();
            },

            resetCallback : function() {
                this.text.value = JSON.stringify(this.sgJson.getPlotSpecsHandler().getPlotter(this.displayIndex).getSpec(), null, 4);
            },

            // disable buttons when another dialog is stacked over
            stack : function () {
                this.m.disableButtons();
            },

            unstack : function () {
                this.m.enableButtons();
            },

            addCallbackValidate : function() {
                var name = this.addModalElemName.value;
                var cols = IIDXHelper.getSelectValues(this.addModalElemCols);

                if (name.length < 1) {
                    return "Name can not be null";

                } else if(this.sgJson.getPlotSpecsHandler().getNames().indexOf(name) > -1) {
                    return "Name is already in use";

                } else if (cols.length == 0) {
                    return "1 or more columns should be selected";

                } else {
                    return null;
                }
            },

            addCallbackCallNgs : function() {
                this.unstack();

                var name = this.addModalElemName.value;
                var graphType = this.addModalElemType.value;
                var cols = IIDXHelper.getSelectValues(this.addModalElemCols);

                this.ngsClient.execAsyncAddSamplePlot(this.sgJson, cols, graphType, name, "plotly", this.addCallbackNgsCallback.bind(this), ModalIidx.alert.bind("Nodegroup service failure"));
            },

            addCallbackNgsCallback : function(sgJson) {
                this.sgJson = sgJson;

                // set default plot to last one (newly added)
                this.displayIndex = this.sgJson.getPlotSpecsHandler().getNumPlots() - 1;
                this.updateAll(true);
            },


            // remove button callback
            removeCallback : function() {
                if (this.displayIndex > -1) {
                    var handler = this.sgJson.getPlotSpecsHandler();
                    handler.delPlotter(this.displayIndex);
                    this.sgJson.setPlotSpecsHandler(handler);

                    this.displayIndex -= 1;
                    this.updateAll(true);
                }
            },

            selectChanged : function () {
                if ( ! this.validateAndStoreWithDialog() ) {
                    this.select.selectedIndex = this.displayIndex;
                    return;
                } else {
                    this.displayIndex = this.select.selectedIndex;
                    this.updateAll(false);
                }
            },

            //  PRE:  json on screen is stored away
            //        this.displayIndex is desired display
            //
            //  dataChangedFlag - rebuild select
            //
            // using this.displayIndex:
            //  sets select.selectedIndex
            //  displays json
            updateAll : function (dataChangedFlag) {

                if (dataChangedFlag) {
                    // rebuild the select based on this.sgJson.plotSpecs
                    IIDXHelper.removeAllOptions(this.select);
                    var handler = this.sgJson.getPlotSpecsHandler();
                    var names = handler.getNames();
                    var defaultPlotter = handler.getDefaultPlotter();
                    var defaultName = defaultPlotter != null ? [defaultPlotter.getName()] : [];
                    IIDXHelper.addOptions(this.select, names, defaultName);
                }

                // success: show possibly new json
                this.select.selectedIndex = this.displayIndex;
                if (this.displayIndex > -1) {
                    this.text.value = JSON.stringify(this.sgJson.getPlotSpecsHandler().getPlotter(this.displayIndex).getSpec(), null, 4);
                } else {
                    this.text.value = "";
                }

            },

            // success : return true
            // error : dialog and return false
            validateAndStoreWithDialog : function () {
                var err = this.validateAndStorePlot()
                if (err != null) {
                    ModalIidx.alert("Invalid Json", err + "<br><br>Fix JSON first.");
                    return false;
                } else  {
                    return true;
                }
            },

            // validates json and stores it in this.sgJson if it's ok
            // otherwise return error
            validateAndStorePlot : function() {
                if (this.displayIndex > -1) {
                    var json;
                    try {
                        json = JSON.parse(this.text.value);
                    } catch (err) {
                        console.log(err);
                        return err.toString();
                    }

                    // put json into plot spec handler, keeping this.sgJson holding everything
                    var handler = this.sgJson.getPlotSpecsHandler();
                    var plotter = handler.getPlotter(this.displayIndex);
                    plotter.setSpec(json);
                    handler.setPlotter(this.displayIndex, plotter);
                    this.sgJson.setPlotSpecsHandler(handler);
                    return null;
                }
            },

		};

		return ModalPlotsDialog;            // return the constructor
	}
);
