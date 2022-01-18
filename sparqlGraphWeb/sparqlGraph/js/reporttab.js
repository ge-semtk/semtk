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
 *  ReportTab
 */

define([	// properly require.config'ed
         	'sparqlgraph/js/iidxhelper',
            'sparqlgraph/js/modaliidx',
            'sparqlgraph/js/modalstoredialog',
            'sparqlgraph/js/msiclientnodegroupexec',
            'sparqlgraph/js/msiclientnodegroupstore',
            'sparqlgraph/js/msiclientontologyinfo',
            'sparqlgraph/js/msiclientstatus',
            'sparqlgraph/js/msiclientresults',
            'sparqlgraph/js/msiresultset',
            'sparqlgraph/js/ontologyinfo',
            'sparqlgraph/js/sparqlgraphjson',
            'sparqlgraph/js/visjshelper',

         	'jquery',
            'visjs/vis.min',
            'jsoneditor',

			// shimmed
         	'sparqlgraph/dynatree-1.2.5/jquery.dynatree',
            'sparqlgraph/js/ontologytree',
            'sparqlgraph/js/belmont',


		],

    // TODO: this isn't leveraging VisJsHelper properly.  Code duplication.
	function(IIDXHelper, ModalIidx, ModalStoreDialog, MsiClientNodeGroupExec, MsiClientNodeGroupStore, MsiClientOntologyInfo, MsiClientStatus, MsiClientResults, MsiResultSet, OntologyInfo, SparqlGraphJson, VisJsHelper, $, vis, JSONEditor) {


		//============ local object  ExploreTab =============
		var ReportTab = function(toolForm, optionsDiv, editorDiv, reportDiv, g, initialUser, setUser) {
            this.conn = null;
            this.toolForm = toolForm;
            this.optionsDiv = optionsDiv;
            this.setUser = setUser;
            this.storeDialog = new ModalStoreDialog(initialUser || "", g.service.nodeGroupStore.url, MsiClientNodeGroupStore.TYPE_REPORT);

            editorDiv.innerHTML = "";
            var div = document.createElement("div");
            div.id="editWrapper";
            div.style.paddingBottom="4em";  // leave space for menus to overflow
            editorDiv.appendChild(div);
            this.editorDiv = div;
            this.reportDiv = reportDiv;
            this.g = g;
            this.editor = null;

            this.initToolForm();
            this.initOptionsDiv();
            this.initEditorDiv();
            this.initReportDiv();

            this.threadCounter = 0;   // how many threads are running

            // some elements will have two versions, one each list
            this.staticElements = [];
            this.dynamicElements = [];

            this.downloadReportButton = null;
            this.statusDiv = null;

        };

        ReportTab.CSS = `
        /* colors    */
        /* https://digitalsynopsis.com/design/color-combinations-palettes-schemes/ */

        // decide where to load this
        // These do not work in javascript.  See replaceAll() duplication below
        :root {
          --main-dark: #0c2333;
          --main-light: #91bbe5;
          --main-medium: #3376bc;
          --main-gray: #c3d5e8;
          --error: #c14d7c;
          --warning: #f4a82c;
          --white: #ffffff;
        }

        .page-title {
         font-size: 30px;
         text-align: center;
         color: var(--main-dark);
        }

        .header-div {
         color: var(--main-dark);
         background-color: var(--main-light);
         border-top: 2px solid var(--main-dark);
         border-bottom: 2px solid var(--main-dark);
         padding-left: 3em;
         padding-right: 3em;
         padding-bottom: 1em;
         padding-bottom: 1em;
         margin-bottom: 2em;
        }

        .page-div {
         background-color: var(--white);
        }

        .report-div {
         margin-left: 3em;
         margin-right: 3em;
         }

        .report-table {
            padding-left: 2em;
            padding-right: 2em;
        }
        .report-table th {
          background-color: var(--main-dark);
          font-weight: bold;
          color: var(--white);
        }
        .report-table td {
          background-color: white;
          border: 1px solid var(--main-medium);
        }

        .report-title {
         font-size: 30px;
         text-align: center;
         color: var(--main-dark);
         counter-reset:h1counter;
        }

        .table-header {
         backgroundColor: red;
         fontWeight: bold;
        }

        .report-h1:before
        {
            counter-increment: h1counter;
            content: counter(h1counter) ". ";
            font-weight:bold;
        }
        .report-h2:before
        {
            counter-increment: h2counter;
            content:counter(h1counter) "." counter(h2counter) ". ";
        }
        .report-h3:before
        {
            counter-increment: h3counter;
            content:counter(h1counter) "." counter(h2counter) "." counter(h3counter) ". ";
        }
        .report-h1 {
         font-family: Georgia, serif;
         font-size: 24px;
         letter-spacing: 2.5px;
         border-bottom: 1px solid main-dark;
         counter-reset:h2counter;
        }

        .report-h2 {
         color: #034f84;
         font-size: 18px;
         color: var(--main-medium);
         counter-reset:h3counter;
        }

        .report-h3 {
         font-size: 14px;
        }

        .report-h4 {
         font-size: 12px;
        }

        .report-div-level-1 {
          margin-bottom: 2em;
          margin-top: 2em;
        }

        .report-div-level-2 {
          border: 2px solid var(--main-medium);
          border-radius: 1em;
          margin-left: 2em;
          margin-bottom: 1em;
          margin-top: 1em;
          padding: 1em;
          box-shadow: 10px 10px var(--main-gray);

        }

        .failure-icon {
          font-size: 18px;
        }

        .success-icon {
          font-size: 18px;
        }

        .report-desc-div {
            padding-top: 1em;
            padding-bottom: 1em;
            padding: 2em;
        }

        .report-desc-div p, list {
            padding-top: 1em;
        }

        .report-error {
          color: var(--error);
          text-align: center;
          font-weight: bold;
          border: 2px solid var(--error);
        }

        .report-wait-spinner {
          border: 1em solid #f3f3f3;
          border-radius: 50%;
          border-top: 1em solid #3498db;
          width: 10em;
          height: 10em;
          margin-left: 40%;
          -webkit-animation: spin 2s linear infinite; /* Safari */
          animation: spin 2s linear infinite;
        }
        `.replaceAll("var(--main-dark)","#0c2333")
        .replaceAll("var(--main-light)","#91bbe5")
        .replaceAll("var(--main-medium)","#3376bc")
        .replaceAll("var(--main-gray)","#c3d5e8")
        .replaceAll("var(--error)","#c14d7c")
        .replaceAll("var(--warning)","#f4a82c")
        .replaceAll("var(--white)","#ffffff")
        ;

        // load this from triplestore

        // built and verified at  https://json-editor.github.io/json-editor/

		ReportTab.prototype = {
            setConn : function(conn) {
                this.conn = conn;
                this.initReportDiv();
                this.updateToolForm();
            },

            updateToolForm : function() {
                var span = document.getElementById("reportConnSpan");
                var connHTML = (this.conn != null && this.conn.getName() != null) ? ("<b>Conn: </b>" + this.conn.getName()) : "";
                span.innerHTML = connHTML;
            },


            initToolForm : function() {
                var table = document.createElement("table");
                table.classList = [];
                table.style.width="100%";
                var tr = document.createElement("tr");
                table.appendChild(tr);
                this.toolForm.appendChild(table);

                // left
                var td = document.createElement("td");
                td.style.width="35%";
                td.align="right";
                tr.appendChild(td);

                // center
                td = document.createElement("td");
                td.style.width="30%";
                td.align="center";
                tr.appendChild(td);

                var span = IIDXHelper.createElement("span", "");
                span.id = "reportConnSpan";
                td.appendChild(span);

                // right
                td = document.createElement("td");
                td.align="right";
                td.style.width="35%";
                tr.appendChild(td);

                td.appendChild(this.buildButtonToolbar());

                this.updateToolForm();
            },

            // print a div
            //   https://stackoverflow.com/questions/2255291/print-the-contents-of-a-div


            initOptionsDiv : function() {
				this.optionsDiv.innerHTML = "";

                var table = IIDXHelper.createElement("table", "");
                table.style.width = "100%";
                var tr = document.createElement("tr");

                // ===== drop json cell =====
                var td1 = document.createElement("td");
                td1.id = "tdSyncOwl1";
                td1.style.width="25%";
                td1.appendChild(IIDXHelper.createDropzone("icon-tasks", "Drop Report JSON", function(e) {return true;}, this.dropReportJSON.bind(this)));
                tr.appendChild(td1);

                // validator cell
                var td3 = document.createElement("td");
                td3.style.width = "50%";
                td3.align="center";
                var span = document.createElement("span");
                span.id = "valid_indicator";
                td3.appendChild(span);
                tr.appendChild(td3);

                // info button cell
                var td4 = document.createElement("td");
                td4.align = "right";
                td4.style.width = "25%";
                td4.appendChild(IIDXHelper.createIconButton("icon-play", this.doRunReport.bind(this), ["btn", "btn-primary"], undefined, "Run", "Generate the report"));

                tr.appendChild(td4);

                table.appendChild(tr);
                this.optionsDiv.appendChild(table);
                return;
            },

            buildButtonToolbar : function() {
                //var toolbar = IIDXHelper.createElement("div", "", "btn-toolbar");
                //toolbar.style.marginTop="0px";
                //var group1 = IIDXHelper.createElement("div", "", "btn-group");
                //toolbar.appendChild(group1);
                var group1 = document.createElement("span");
                group1.appendChild(IIDXHelper.createIconButton("icon-remove-circle", this.doClear.bind(this), ["btn"], undefined, "Clear", "Clear the report"));
                IIDXHelper.appendSpace(group1);
                group1.appendChild(IIDXHelper.createIconButton("icon-cloud", this.doOpenStore.bind(this), ["btn"], undefined, "Open store", "Open cloud storage"));
                IIDXHelper.appendSpace(group1);
                group1.appendChild(IIDXHelper.createIconButton("icon-cloud-upload", this.doSaveToStore.bind(this), ["btn"], undefined, "Save", "Save to cloud storage"));
                IIDXHelper.appendSpace(group1);
                group1.appendChild(IIDXHelper.createIconButton("icon-save", this.doDownloadReport.bind(this), ["btn"], undefined, "Download", "Download report definition JSON"));
                IIDXHelper.appendSpace(group1);
                group1.appendChild(IIDXHelper.createIconButton("icon-info-sign",
                                    function() {
                                        window.open('https://github.com/json-editor/json-editor', "_blank","location=yes");
                                    },
                                    ["icon-white", "btn", "btn-info"],
                                    undefined,
                                    "JSONEditor",
                                    "Built with JSONEditor"
                                ));return group1;
                //return toolbar;
            },

            // check for changes then dropReportJSONOK
            dropReportJSON : function (ev, label) {
                var file = ev.dataTransfer.files[0];
                var okCallback = function(f, l) {
                    this.dropReportJSONOK(f, l);
                }.bind(this, file, label);

                if (this.reportChanged()) {
                    ModalIidx.okCancel("Overwrite warning", "Report has been edited.  <br><br>Discard changes and continue?", okCallback, "Discard", function(){}, undefined, "btn-danger");
                } else {
                    okCallback();
                }
            },

            // drop a file w/o checking
            dropReportJSONOK : function(file, label) {
                var reportJSON = [];

                readNext = function(index) {
                    // done
                    if (index == 1) {
                        return;
                    }

                    // error
                    if (file.name.slice(-5) != ".json" && file.name.slice(-5) != ".JSON") {
                        this.logAndAlert("Error dropping data file", "Only .json is supported: " + file.name);
                        return;
                    }

                    // normal
                    var reader = new FileReader();
                    reader.onload = function(f) {
                        console.log(file.name);
                        reportJSON.push(file);
                        label.data = file.name;
                        file.text().then(
                            function(jsonStr) {
                                this.setReport(jsonStr);
                            }.bind(this));
                        // chain next read
                        readNext(1);
                    }.bind(this);
                    reader.readAsText(file);
                }.bind(this);
                readNext(0);
            },

            initEditorDiv : function () {
                // make sure this stylesheet is the last (closest/overriding) sheet
                var link1 = document.createElement("link");
                link1.rel='stylesheet';
                link1.type='text/css';
                link1.href = '../css/json-editor.css';
                this.editorDiv.appendChild(link1);

                var link2 = document.createElement("link");
                link2.rel='stylesheet';
                link2.type='text/css';
                link2.href = 'https://use.fontawesome.com/releases/v5.12.1/css/all.css';
                this.editorDiv.appendChild(link2);

                var table = document.createElement("table");
                this.editorDiv.appendChild(table);
                table.style.width="100%";

                var tr = document.createElement("tr");
                table.appendChild(tr);
                var left = document.createElement("td");
                tr.appendChild(left);
                var middle = document.createElement("td");
                middle.align = "center";
                tr.appendChild(middle);

                var right = document.createElement("td");
                right.align = "right";
                tr.appendChild(right);

                // right is now empty

                var options = {
                    ajax: true,
                    display_required_only: true,
                    schema : { $ref: "../json/report-schema.json" },
                    theme : "barebones",
                    iconlib: "fontawesome5"
                };
                this.editor = new JSONEditor.JSONEditor(this.editorDiv, options);
                this.initValidator();

                this.editor.on('ready',function() {
                    // Create buttom and insert it after the root header
                    var button = this.editor.root.getButton('Expand All','expand','Expand All');
                    button.classList.add("json-editor-btntype-expand");
                    button.value = '0';
                    var button_holder = this.editor.root.theme.getHeaderButtonHolder();
                    button_holder.appendChild(button);
                    this.editor.root.header.parentNode.insertBefore(button_holder, this.editor.root.header.nextSibling);

                    button.onclick = this.clickExpandCollapse.bind(this, button);
                }.bind(this));
            },

            //
            // click the expand collapse button
            //
            clickExpandCollapse: function(thisButton, e) {
                e.preventDefault();
                e.stopPropagation();

                // Toggle the value on the button
                thisButton.value = thisButton.value == '1' ? '0' : '1';

                // Change the text/icon on the button
                if (thisButton.value == '1') {
                  // Expand
                  this.editor.root.setButtonText(thisButton,'Collapse All','collapse','Collapse All');
                }
                else {
                  // Collapse
                  this.editor.root.setButtonText(thisButton,'Expand All','expand','Expand All');
                }
                this.expandOrCollapseAll(thisButton.value == '1');

            },


            //
            // set the report
            setReport : function(reportJsonStr) {
                var json = JSON.parse(reportJsonStr);
                this.editor.setValue(JSON.parse(reportJsonStr));
                this.storeDialog.suggestId(json.title || "");
                this.expandOrCollapseAll(false);
                this.setReportAsUnchanged();
            },

            //
            // mark the existing report as unchanged
            setReportAsUnchanged : function() {
                var curReportStr = JSON.stringify(this.editor.getValue());
                this.lastSetReportStr = curReportStr;
            },

            //
            // has report been edited with the editor
            reportChanged : function() {
                var curReportStr = JSON.stringify(this.editor.getValue());
                return (this.lastSetReportStr != undefined && curReportStr != this.lastSetReportStr);
            },

            expandOrCollapseAll : function(expandFlag) {
                var exempt = ["root", "root.sections"];

                // Loop through all editors
                for (var key in this.editor.editors) {
                    if (exempt.indexOf(key) == -1) {
                        var ed = this.editor.editors[key];
                        this.expandOrCollapse(ed, expandFlag);
                    }
                }
            },

            expandOrCollapse : function(ed, expandFlag) {
                try {
                    if (['array', 'object'].indexOf(ed.schema.type) !== -1 && ed.editor_holder) {
                        if (expandFlag) {
                            // Expand
                            ed.editor_holder.style.display = '';
                            ed.collapsed = false;
                            if (ed.toggle_button) {
                                ed.setButtonText(ed.toggle_button,'','collapse',ed.translate('button_collapse'));
                            }
                        }
                        else {
                            // Collapse
                            ed.editor_holder.style.display = 'none';
                            ed.collapsed = true;
                            if (ed.toggle_button) {
                                ed.setButtonText(ed.toggle_button,'','expand',ed.translate('button_expand'));
                            }
                        }
                    }
                } catch (err) {
                    // Json is so bad everthing is fubar
                }
            },

            initValidator : function() {
                // Hook up the validation indicator to update its
                // status whenever the editor changes
                this.editor.on('change',function() {
                    // Get an array of errors from the validator
                    var errors = this.editor.validate();
                    var indicator = document.getElementById('valid_indicator');
                    var json = this.editor.getValue();

                    if(errors.length) {
                        // Not valid
                        if (Object.keys(json).length == 1 && json.title == "") {
                            indicator.className = 'reports-valid';
                            indicator.textContent = 'Report JSON is empty';
                        } else {
                            indicator.className = 'reports-invalid';
                            indicator.textContent = 'Report JSON is invalid';
                        }
                    }
                    else {
                        // Valid
                        indicator.className = 'reports-valid';
                        indicator.textContent = 'Report: ' + json.title;
                    }
                }.bind(this));
            },

            doSaveToStore : function() {
                try {
                    var reportJson = this.editor.getValue();
                    if (! this.storeDialog.getSuggestedId() && reportJson.title) {
                        this.storeDialog.suggestId(reportJson.title);
                    }
                    this.storeDialog.launchStoreDialog(JSON.stringify(reportJson), this.setReportAsUnchanged.bind(this));
                } catch (err) {
                    console.log(err.stack);
                    alert(err);   // need to make it to "return false" so page doesn't reload
                }
                return false;
            },

            doOpenStore : function() {
                try {
                    if (this.reportChanged()) {
                        ModalIidx.okCancel("Overwrite warning", "Report has been edited.  <br><br>Discard changes and continue?", this.doOpenStoreOk.bind(this), "Discard", function(){}, undefined, "btn-danger");
                    } else {
                        this.doOpenStoreOk();
                    }
                } catch (err) {
                    console.log(err.stack);
                    alert(err);   // need to make it to "return false" so page doesn't reload
                }
                return false;
            },

            doOpenStoreOk : function() {
                this.storeDialog.launchRetrieveDialog(this.setReport.bind(this));
            },

            doDownloadReport : function() {
                try {
                    var json = this.editor.getValue();
                    var jsonStr = JSON.stringify(json, null, 4);
                    var title = json.title ? (json.title.replaceAll(" ", "_") + ".json") : "report.json";
                    IIDXHelper.downloadFile(jsonStr, title, "text/json");
                    this.setReportAsUnchanged();
                } catch (err) {
                    console.log(err.stack);
                    alert(err);   // need to make it to "return false" so page doesn't reload
                }
                return false;
            },

            doRunReport : function() {
                var json = this.editor.getValue();

                if (Object.keys(json).length == 1 && json.title == "") {
                    ModalIidx.alert("Error", "Report JSON is empty");
                } else if (this.editor.validate().length > 0) {
                    ModalIidx.alert("Error", "Report JSON is not valid");
                } else {
                    try {
                        this.drawReport(json);
                    } catch (err) {
                        console.log(err.stack);
                        alert(err); // need to make it to "return false" so page doesn't reload
                    }
                }
                return false;
            },

            doClear : function() {
                try {
                    this.setReport("{\"title\": \"\"}");
                } catch (err) {
                    console.log(err.stack);
                    alert(err); // need to make it to "return false" so page doesn't reload
                }
                return false;
            },

            initReportDiv : function() {
                this.reportDiv.innerHTML = "";
            },

            doDownloadResults : function() {
                for (var e of this.staticElements) {
                    e.style.display=null;
                }
                for (var e of this.dynamicElements) {
                    e.style.display="none";
                }

                IIDXHelper.downloadFile(this.reportDiv.innerHTML, "report.html", "text/html");

                for (var e of this.staticElements) {
                    e.style.display="none";
                }
                for (var e of this.dynamicElements) {
                    e.style.display=null;
                }
            },

            addReportButtons : function() {
                var table = document.createElement("table");
                table.classList = [];
                table.style.width="100%";
                var tr = document.createElement("tr");
                table.appendChild(tr);
                this.toolForm.appendChild(table);

                // left
                td = document.createElement("td");
                tr.appendChild(td);
                // unused
                this.statusDiv = document.createElement("div");
                this.statusDiv.style.width="100%";
                this.statusDiv.id = "rt_statusDiv";
                td.appendChild(this.statusDiv);
                td.align="left";

                // center

                // right
                td = document.createElement("td");
                td.align="right";
                td.style.width="35%";
                tr.appendChild(td);

                this.downloadReportButton = IIDXHelper.createIconButton("icon-save", this.doDownloadResults.bind(this), ["btn", "btn-primary"], undefined, "Download", "Download report results html");
                this.downloadReportButton.disabled = true;
                td.appendChild(this.downloadReportButton);

                this.reportDiv.appendChild(table);

                this.dynamicElements.push(table);
            },

            appendStyles : function() {
                var style = document.createElement("style");
                style.innerHTML = ReportTab.CSS;
                this.reportDiv.appendChild(style);
            },

            drawReport : function(report) {

                this.initReportDiv();
                this.appendStyles();

                this.addReportButtons();

                var p = IIDXHelper.createElement("p", report["title"], "report-title");
                this.reportDiv.appendChild(p);

                this.addDescription(this.reportDiv, report["description"]);

                this.sectionThreadsReset()
                if (report.hasOwnProperty("sections")) {
                    for (var section of report["sections"]) {
                        this.reportDiv.appendChild(this.generateSection(section, 1));
                    }
                }
            },

            // call this before starting to draw a report
            sectionThreadsReset : function() {
                this.threadCounter = 0;
            },

            // section callback should call this to free up a thread
            sectionThreadNew : function() {
                this.threadCounter += 1;
                if (this.threadCounter == 1) {
                    document.body.style.cursor='wait';

                    this.downloadReportButton.disabled = true;
                }
            },

            // section callback should call this to free up a thread
            sectionThreadDone : function() {
                this.threadCounter -= 1;

                if (this.threadCounter == 0) {
                    document.body.style.cursor='default';
                    this.downloadReportButton.disabled = null;
                }
            },

            generateSection : function(section, level, optDivWaiting) {

                // -------- control threading --------
                // not positive this is needed, but seems bad to send dozens of large queries to fuseki
                // need to make sure every callback calls sectionThreadDone() or else this will go wrong

                // create div first time only
                var div = optDivWaiting ? optDivWaiting : IIDXHelper.createElement("div", "", "report-div-level-" + level);

                if(this.threadCounter >= 3) {
                    //wait then try again
                    setTimeout(this.generateSection.bind(this,section,level,div), 500);
                    //return an empty div
                    // when thread is available it will get it's header, probably a spinner, then results
                    return div;
                }
                this.sectionThreadNew();
                // -------- end control threading --------

                // required: header
                var header = section["header"] || "<No Header Specified>"
                let h = IIDXHelper.createElement("h" + level, header, "report-h" + level);
                div.appendChild(h);

                this.addDescription(div, section["description"]);

                if (section["special"] != undefined) {
                    var id = section["special"]["id"]

                    let spDiv = IIDXHelper.createElement("div", "", undefined);
                    spDiv.appendChild(IIDXHelper.createElement("div", "", "report-wait-spinner"));
                    div.appendChild(spDiv);

                    try {
                        if (! id ) throw new Error("Report section['special'] is missing field 'id'");

                        if (id == "class_count") {
                            var client = new MsiClientOntologyInfo(this.g.service.ontologyInfo.url, this.failureCallback.bind(this, spDiv));
                            var jsonBlobCallback = MsiClientOntologyInfo.buildPredicateStatsCallback(this.specialClassCountCallback.bind(this, spDiv),
                                                                                                this.failureCallback.bind(this, spDiv),
                                                                                                this.statusCallback.bind(this, spDiv),
                                                                                                this.checkForCancelCallback.bind(this, spDiv));
                            client.execGetPredicateStats(this.conn, jsonBlobCallback);

                        } else if (id == "cardinality") {
                            var client = new MsiClientOntologyInfo(this.g.service.ontologyInfo.url, this.failureCallback.bind(this, spDiv));
                            var tableCallback = MsiClientOntologyInfo.buildCardinalityViolationsCallback(this.cardinalityGetTableCallback.bind(this, spDiv),
                                                                                                this.failureCallback.bind(this, spDiv),
                                                                                                this.statusCallback.bind(this, spDiv),
                                                                                                this.checkForCancelCallback.bind(this, spDiv));
                            client.execGetCardinalityViolations(this.conn, tableCallback);
                        } else {
                            throw new Error("Report 'special.id' has unknown value value: " + id);
                        }

                    } catch (e) {
                        this.failureCallback(spDiv, e)
        			}

                } else if (section["plot"] != undefined) {
                    var nodegroup = section["plot"]["nodegroup"];
                    var plotId = section["plot"]["plotname"];

                    // create a blank div to cover for async behavior
                    let ngDiv = IIDXHelper.createElement("div", "", undefined);
                    ngDiv.appendChild(IIDXHelper.createElement("div", "", "report-wait-spinner"));
                    div.appendChild(ngDiv);

                    try {
                        if (!nodegroup) throw new Error("Report section['plot'] is missing field 'nodegroup'");
                        if (! plotId) throw new Error("Report section['plot'] is missing field 'plotname'");

                        var ngStoreClient = new MsiClientNodeGroupStore(g.service.nodeGroupStore.url);
                        ngStoreClient.getStoredItemByIdToStr(nodegroup, MsiClientNodeGroupStore.TYPE_NODEGROUP, this.plotGetNgCallback.bind(this, ngDiv, plotId, level));

                    } catch (e) {
                        this.failureCallback(ngDiv, e)
                    }

                } else if (section["table"] != undefined) {
                    var nodegroup = section["table"]["nodegroup"];

                    let ngDiv = IIDXHelper.createElement("div", "", undefined);
                    ngDiv.appendChild(IIDXHelper.createElement("div", "", "report-wait-spinner"));
                    div.appendChild(ngDiv);

                    try {
                        if (! nodegroup) throw new Error("Report section['table'] is missing field 'nodegroup'");

                        var ngExecClient = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url);
                        var tableCallback = MsiClientNodeGroupExec.buildFullJsonCallback(this.tableGetTableCallback.bind(this, ngDiv),
                                                                                         this.failureCallback.bind(this, ngDiv),
                                                                                         this.statusCallback.bind(this, ngDiv),
                                                                                         this.checkForCancelCallback.bind(this, ngDiv),
                                                                                         this.g.service.status.url,
                                                                                         this.g.service.results.url);
                        ngExecClient.execAsyncDispatchSelectById(nodegroup, this.conn, null, null, tableCallback, this.failureCallback.bind(this, ngDiv));
                    } catch (e) {
                        this.failureCallback(ngDiv, e);
                    }

                } else if (section["count"] != undefined) {
                    var nodegroup = section["count"]["nodegroup"];

                    let ngDiv = IIDXHelper.createElement("div", "", undefined);
                    ngDiv.appendChild(IIDXHelper.createElement("div", "", "report-wait-spinner"));
                    div.appendChild(ngDiv);

                    try {
                        if (!nodegroup) throw new Error("Report section['count'] is missing field 'nodegroup'");

                        var ngExecClient = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url);
                        var countCallback = MsiClientNodeGroupExec.buildFullJsonCallback(this.countGetTableCallback.bind(this, ngDiv, section["count"], level),
                                                                                         this.failureCallback.bind(this, ngDiv),
                                                                                         this.statusCallback.bind(this, ngDiv),
                                                                                         this.checkForCancelCallback.bind(this, ngDiv),
                                                                                         this.g.service.status.url,
                                                                                         this.g.service.results.url);
                        ngExecClient.execAsyncDispatchCountById(nodegroup, this.conn, null, null, countCallback, this.failureCallback.bind(this, ngDiv));

                    } catch (e) {
                        this.failureCallback(div, e)
        			}
                } else if (section["graph"] != undefined) {
                    var nodegroup = section["graph"]["nodegroup"];

                    let ngDiv = IIDXHelper.createElement("div", "", undefined);
                    ngDiv.appendChild(IIDXHelper.createElement("div", "", "report-wait-spinner"));
                    div.appendChild(ngDiv);

                    try {
                        if (! nodegroup) throw new Error("Report section['graph'] is missing field 'nodegroup'");

                        var ngExecClient = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url);
                        var graphCallback = MsiClientNodeGroupExec.buildJsonLdCallback(this.graphGetGraphCallback.bind(this, ngDiv),
                                                                                         this.failureCallback.bind(this, ngDiv),
                                                                                         this.statusCallback.bind(this, ngDiv),
                                                                                         this.checkForCancelCallback.bind(this, ngDiv),
                                                                                         this.g.service.status.url,
                                                                                         this.g.service.results.url);
                        ngExecClient.execAsyncDispatchConstructById(nodegroup, this.conn, null, null, graphCallback, this.failureCallback.bind(this, ngDiv));
                    } catch (e) {
                        this.failureCallback(ngDiv, e);
                    }

                }

                if (section["sections"] != undefined) {
                    for (subSection of section["sections"]) {
                        div.appendChild(this.generateSection(subSection, level+1));
                    }
                }

                return div;
            },

            addDescription : function (div, optDesc) {
                if (optDesc) {
                    var descDiv = IIDXHelper.createElement("div", optDesc, "report-desc-div");
                    div.appendChild(descDiv);
                }
            },

            plotGetNgCallback : function(div, plotId, level, jsonStr) {
                var sgJson = new SparqlGraphJson();
                sgJson.parse(jsonStr);
                var ng = new SemanticNodeGroup();
                sgJson.getNodeGroup(ng);
                var plotSpecHandler = sgJson.getPlotSpecsHandler();
                var plotter = plotSpecHandler.getPlotterByName(plotId);

                var ngExecClient = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url);
                var jsonCallback = MsiClientNodeGroupExec.buildFullJsonCallback(this.plotGetTableCallback.bind(this, div, plotter),
                                                                                 this.failureCallback.bind(this, div),
                                                                                 this.statusCallback.bind(this, div),
                                                                                 this.checkForCancelCallback.bind(this.div),
                                                                                 this.g.service.status.url,
                                                                                 this.g.service.results.url);
                ngExecClient.execAsyncDispatchSelectFromNodeGroup(ng, this.conn, null, null, jsonCallback, this.failureCallback.bind(this, div));
            },

            plotGetTableCallback : function(div, plotter, tableRes) {
                div.innerHTML="";

                // set up two divs: dynamic and static
                var dynamicDiv = document.createElement("div");
                div.appendChild(dynamicDiv);
                var staticDiv = document.createElement("div");
                div.appendChild(staticDiv);
                var staticImage = document.createElement("img");
                staticDiv.appendChild(staticImage);
                this.saveDynamicStaticPair(dynamicDiv, staticDiv);

                plotter.addPlotToDiv(dynamicDiv, tableRes, staticImage);
                this.sectionThreadDone();
            },

            countGetTableCallback : function(div, countJson, level, tableRes) {
                div.innerHTML="";
                var count = tableRes.getTable().rows[0][0];

                var rangeFlag = false;
                if (countJson["ranges"] != undefined) {

                    for (var this_range of countJson["ranges"]) {
                        var match = true;

                        if (this_range["gte"] != undefined && count < this_range["gte"]) {
                            match = false;
                        }
                        if (this_range["lte"] != undefined && count > this_range["lte"]) {
                            match = false;
                        }
                        if (match) {
                            rangeFlag = true;

                            // print status if it is there
                            if (this_range["status"] != undefined) {
                                if (this_range["status"] == "success") {
                                    div.appendChild(IIDXHelper.createElement("span", "\u2705 ", className="success-icon"));
                                } else if (this_range["status"] == "failure") {
                                    div.appendChild(IIDXHelper.createElement("span", "\u26d4 ", className="failure-icon"));
                                }
                            }

                            // use format to print count, or just a simple one if none
                            var fmt = this_range["format"] || "count: {0}";
                            div.appendChild(IIDXHelper.createElement("span", this.format(fmt, count)));
                            this.sectionThreadDone();

                           // do sections
                           if (this_range["sections"] != undefined) {
                               for (subSection of this_range["sections"]) {
                                   div.appendChild(this.generateSection(subSection, level+1));
                               }
                           }
                        }
                    }
                }

                // no ranges matched, just print the count
                if (!rangeFlag) {
                    div.appendChild(IIDXHelper.createElement("span", this.format("count: {0}", count)));
                }
            },

            specialClassCountCallback : function(div, res) {
                div.innerHTML = "";
                var blob = res.xhr;

                var rows = [];
                for (var key in blob.exactTab) {
                    var jObj = JSON.parse(key);

                    // only visualize one-hops
                    if (jObj.triples.length == 1) {
                        var count = blob.exactTab[key];
                        var oSubjectClass = new OntologyName(jObj.triples[0].s);
                        var oPredicate = new OntologyName(jObj.triples[0].p);

                        // skipping Type since w already have oSubjectClass
                        if ( jObj.triples[0].p.endsWith("type")) {
                            rows.push([jObj.triples[0].s, count]);
                        }
                    }
                }

                var tableElem = this.addTable( div,
    								           [{sTitle: "class", sType: "string", filter: true},
                                                {sTitle: "count", sType: "numeric", filter: true}],
								               rows,
                                               1,
                                               'desc'
                                           );

                this.sectionThreadDone();

            },

            // do sprintf that only knows {0}
            format : function(fmt, val) {
                var split = fmt.split("{0}");
                if (split.length == 1) {
                    return split[0];
                } else {
                    return split[0] + val.toString() + split[1];
                }
            },

            // designate a pair of elements one for display interactively (dynamic)
            // and one for download (static)
            saveDynamicStaticPair : function(dynamicElem, staticElem) {
                this.dynamicElements.push(dynamicElem);
                staticElem.style.display="none";
                this.staticElements.push(staticElem);
            },

            tableGetTableCallback : function(div, tableRes) {
                div.innerHTML="";
                this.addTableResult(div, tableRes);
                this.sectionThreadDone();
            },

            // add tableRes to a div
            addTableResult : function(div, tableRes) {
                var tableElem = tableRes.putTableResultsDatagridInDiv(div, undefined, []);
                this.fixDynamicDatagrid(div, tableElem);

                var staticTable = tableRes.tableGetElem();
                this.fixStaticTable(staticTable);

                this.saveDynamicStaticPair(tableElem.parentElement, staticTable);
                div.appendChild(staticTable);
            },

            // here : buildDatagridInDiv needs to call this instead
            addTable : function(div, headerJson, rows, optSortCol, optSortDesc) {
                // add the "regular" dynamic table
                var sortOrder = optSortCol ? (optSortDesc ? [[optSortCol, 'desc']] : [[optSortCol]]) : undefined;
                var tableElem = IIDXHelper.buildDatagridInDiv( div,
                                               function() { return headerJson; },
                                               function() { return rows; },
                                               undefined,
                                               sortOrder
                                           );

                this.fixDynamicDatagrid(div, tableElem);

                // add a static table for download
                var staticTable = IIDXHelper.buildTableElem(
                                               headerJson,
                                               rows,
                                               optSortCol,
                                               optSortDesc
                                           );
                this.fixStaticTable(staticTable);

                this.saveDynamicStaticPair(tableElem.parentElement, staticTable);

                div.appendChild(staticTable);
            },

            cardinalityGetTableCallback : function(div, tableRes) {
                div.innerHTML="";

                var descDiv = IIDXHelper.createElement("div", "", className="report-desc-div");
                div.appendChild(descDiv);

                if (tableRes.getRowCount() == 0) {
                    // print success and no table
                    descDiv.appendChild(IIDXHelper.createElement("span", "\u2705 ", className="success-icon"));
                    descDiv.appendChild(IIDXHelper.createElement("span", "No cardinality violations were found."));
                } else {
                    // print failure
                    descDiv.appendChild(IIDXHelper.createElement("span", "\u26d4 ", className="failure-icon"));
                    descDiv.appendChild(IIDXHelper.createElement("span", "Cardinality violations were found."));
                    descDiv.appendChild(document.createElement("br"));

                    if (tableRes.getRowCount() == 5000) {
                        descDiv.appendChild(IIDXHelper.createElement("span", "\u26d4 ", className="failure-icon"));
                        descDiv.appendChild(IIDXHelper.createElement("span", "Only the first 5000 violations are shown."));
                        descDiv.appendChild(document.createElement("br"));
                    }

                    // add a table description
                    descDiv.appendChild(IIDXHelper.createElement("p",
                        "This table shows one line for each violation. <list>" +
                        "<li><b>class</b> - the class with the restricted property</li>" +
                        "<li><b>property</b> - the property being restricted</li>" +
                        "<li><b>restriction</b> - type of restriction</li>" +
                        "<li><b>limit</b> - cardinality limit declared in the model and violated in the data</li>" +
                        "<li><b>subject</b> - the instance of the class which violates the restriction</li>" +
                        "<li><b>class</b> - the actual cardinality of <b>property</b> for this <b>subject</b></li>" +
                        "</list>"
                    ));

                    // print the table
                    this.addTableResult(div, tableRes);
                    this.sectionThreadDone();

                }
            },

            graphGetGraphCallback : function(div, res) {

                if (! res.isJsonLdResults()) {
                    div.innerHTML =  "<b>Error:</b> Results returned from service are not JSON-LD";
                    return;
                }
                div.innerHTML="";  // clear the spinner

                // set up two divs: dynamic and static
                var dynamicDiv = document.createElement("div");
                div.appendChild(dynamicDiv);
                var staticDiv = document.createElement("div");
                div.appendChild(staticDiv);
                var staticImage = document.createElement("img");
                staticDiv.appendChild(staticImage);
                this.saveDynamicStaticPair(dynamicDiv, staticDiv);

                // canvas
                var canvasDiv = document.createElement("div");
                canvasDiv.style.width="100%";
                canvasDiv.style.height="650px";
                canvasDiv.style.margin="1ch";
                dynamicDiv.appendChild(canvasDiv);

                // add network config
                var configDiv = document.createElement("div");
                var showConfig = function() {
                    VisJsHelper.showConfigDialog(configDiv, function(){});
                    return false;
                }.bind(this);

                but = IIDXHelper.createIconButton("icon-magnet", showConfig, undefined, undefined, undefined, "Network physics");
                table = document.createElement("table");
                table.width="100%";
                tr = document.createElement("tr");
                td = document.createElement("td");
                td.align="center";
                table.appendChild(tr);
                tr.appendChild(td);
                td.appendChild(document.createTextNode("Network physics: "));
                td.appendChild(but);
                dynamicDiv.appendChild(table);

                // setup empty network
                var nodeDict = {};   // dictionary of nodes with @id as the key
                var edgeList = [];   // "normal" list of edges

                var options = VisJsHelper.getDefaultOptions(configDiv);
                var network = new vis.Network(canvasDiv, {nodes: Object.values(nodeDict), edges: edgeList }, options);

                // add data
                var jsonLd = res.getGraphResultsJsonArr(true, true, true);
                for (var i=0; i < jsonLd.length; i++) {
                    VisJsHelper.addJsonLdObject(jsonLd[i], nodeDict, edgeList);
                    if (i % 200 == 0) {
                        network.body.data.nodes.update(Object.values(nodeDict));
                        network.body.data.edges.update(edgeList);
                    }
                }
                network.body.data.nodes.update(Object.values(nodeDict));
                network.body.data.edges.update(edgeList);

                this.sectionThreadDone();

                network.startSimulation();

                // give 2.5 seconds to layout, then fit
                setTimeout(function(network){ network.fit(); }.bind(this, network), 2500);

                // create the static image for download
                network.on("afterDrawing", function (ctx) {
                    var dataURL = ctx.canvas.toDataURL();
                    staticImage.src = dataURL;
                  });
            },

            // make tweaks needed for datagrid display
            fixDynamicDatagrid : function(div, tableElem) {
                tableElem.classList.add("report-table");
                tableElem.style.width="100%";
                div.appendChild(document.createElement("br"));
                div.appendChild(document.createElement("br"));
            },

            // make tweaks needed for static tables
            fixStaticTable : function(tableElem) {
                var STATIC_TABLE_TRUNCATE_ROWS = 500;

                IIDXHelper.truncateTableRows(tableElem, STATIC_TABLE_TRUNCATE_ROWS)
                tableElem.classList.add("table");
                tableElem.classList.add("report-table");
            },

            statusCallback : function(div, eOrMsg) {
            },
            checkForCancelCallback : function(div, eOrMsg) {
                return false;
            },

            failureCallback : function(div, eOrMsg) {
                var msg = eOrMsg.toString();
                let errorDiv = IIDXHelper.createElement("div", "Error generating this section.", "report-error");

                var summary = null;
                if (msg.indexOf("special.id") > -1) {
                    summary = "Unknown special section: " + msg.split(":")[2];
                } else if (msg.indexOf("Could not find nodegroup with id") > -1) {
                    // give a simple message for this common & likely problem
                    let lines = msg.split('<br>');
                    for (let l of lines) {
                        if (l.indexOf("rationale") > -1) {
                            summary = l.split('\\n')[0];
                            if (summary.indexOf("Exception") > -1) {
                                summary = summary.split("Exception")[1];
                            }
                        }
                    }
                } else {
                    // unknown problem: for now display the whole this_range
                    summary = msg;
                }
                if (summary != null) {
                    let errorP = IIDXHelper.createElement("p", summary, undefined);
                    errorDiv.appendChild(errorP);
                }

                div.innerHTML = "";
                div.appendChild(errorDiv);
                console.error(eOrMsg);
                this.sectionThreadDone();

            },


        }

		return ReportTab;            // return the constructor
	}

);
