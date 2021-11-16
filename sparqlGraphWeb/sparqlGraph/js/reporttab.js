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
	function(IIDXHelper, ModalIidx, MsiClientNodeGroupExec, MsiClientNodeGroupStore, MsiClientOntologyInfo, MsiClientStatus, MsiClientResults, MsiResultSet, OntologyInfo, SparqlGraphJson, VisJsHelper, $, vis, JSONEditor) {


		//============ local object  ExploreTab =============
		var ReportTab = function(toolForm, optionsDiv, editorDiv, reportDiv, g) {
            this.conn = null;
            this.toolForm = toolForm;
            this.optionsDiv = optionsDiv;

            editorDiv.innerHTML = "";
            var div = document.createElement("div");
            div.id="editWrapper";
            div.style.paddingBottom="10em";  // leave space for menus to overflow
            editorDiv.appendChild(div);
            this.editorDiv = div;
            this.reportDiv = reportDiv;
            this.g = g;
            this.editor = null;

            this.initToolForm();
            this.initOptionsDiv();
            this.initEditorDiv();
            this.initReportDiv();

            this.reportJSON = null;
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
                this.initToolForm();
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

                // left (empty)
                var td = document.createElement("td");
                td.align="right";
                tr.appendChild(td);

                // center
                td = document.createElement("td");
                td.align="center";
                tr.appendChild(td);

                var span = IIDXHelper.createElement("span", "");
                span.id = "reportConnSpan";
                td.appendChild(span);
                this.updateToolForm();

                // right (empty)
                td = document.createElement("td");
                td.align="right";
                tr.appendChild(td);
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

                // run button cell
                var td2 = document.createElement("td");
                var button = document.createElement("button");
                button.id = "butSyncOwl";
                button.classList.add("btn");
                button.innerHTML = "Run";
                button.onclick = this.runReport.bind(this);
                td2.appendChild(IIDXHelper.createNbspText());
                td2.appendChild(button);
                td2.appendChild(IIDXHelper.createNbspText());
                tr.appendChild(td2);

                // run button cell
                var td3 = document.createElement("td");
                td3.align="right";
                td3.style.width = "20em";
                var span = document.createElement("span");
                span.id = "valid_indicator";
                td3.appendChild(span);
                tr.appendChild(td3);

                table.appendChild(tr);
                this.optionsDiv.appendChild(table);
                return;
            },

            dropReportJSON : function (ev, label) {
                this.reportJSON = [];
                var files = ev.dataTransfer.files;

                readNext = function(index) {
                    // done
                    if (index == 1) {
                        return;
                    }

                    // error
                    var file = files[index];
                    if (file.name.slice(-5) != ".json" && file.name.slice(-5) != ".JSON") {
                        this.logAndAlert("Error dropping data file", "Only .json is supported: " + file.name);
                        return;
                    }

                    // normal
                    var reader = new FileReader();
                    reader.onload = function(f) {
                        console.log(file.name);
                        this.reportJSON.push(file);
                        label.data = file.name;
                        file.text().then(
                            function(jsonStr) {
                                this.editor.setValue(JSON.parse(jsonStr));
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

                // download button cell
                right.appendChild(IIDXHelper.createButton("Download", this.downloadReport.bind(this), ["btn", "btn-primary"]));
                right.appendChild(IIDXHelper.createNbspText());

                // json editor info button
                right.appendChild(IIDXHelper.createIconButton("icon-info-sign",
                                    function() {
                                        window.open('https://github.com/json-editor/json-editor', "_blank","location=yes");
                                    },
                                    ["icon-white", "btn-small", "btn-info"],
                                    undefined,
                                    "JSONEditor",
                                    "Built with JSONEditor"
                                ));



                var options = {
                    ajax: true,
                    display_required_only: true,
                    schema : { $ref: "../json/report-schema.json" },
                    theme : "barebones",
                    iconlib: "fontawesome5"
                };
                this.editor = new JSONEditor.JSONEditor(this.editorDiv, options);
                this.initValidator();

            },

            initValidator : function() {
                // Hook up the validation indicator to update its
                // status whenever the editor changes
                this.editor.on('change',function() {
                    // Get an array of errors from the validator
                    var errors = this.editor.validate();
                    var indicator = document.getElementById('valid_indicator');

                    // Not valid
                    if(errors.length) {
                        var json = this.editor.getValue();
                        if (Object.keys(json).length == 1 && json.title == "") {
                            indicator.className = 'reports-valid';
                            indicator.textContent = 'JSON is empty';
                        } else {
                            indicator.className = 'reports-invalid';
                            indicator.textContent = 'JSON is invalid';
                        }
                    }
                    // Valid
                    else {
                        indicator.className = 'reports-valid';
                        indicator.textContent = 'JSON is valid';
                    }
                }.bind(this));
            },

            downloadReport : function() {
                var json = this.editor.getValue();
                var jsonStr = JSON.stringify(json, null, 4);
                var title = json.title ? (json.title.replaceAll(" ", "_") + ".json") : "report.json";
                IIDXHelper.downloadFile(jsonStr, title, "text/json");
            },

            runReport : function() {
                var json = this.editor.getValue();

                if (Object.keys(json).length == 1 && json.title == "") {
                    ModalIidx.alert("Error", "Report JSON is empty");
                } else if (this.editor.validate().length > 0) {
                    ModalIidx.alert("Error", "Report JSON is not valid");
                } else {
                    this.drawReport(json);
                }
            },

            initReportDiv : function() {
                this.reportDiv.innerHTML = "";

            },

            appendStyles : function() {
                var style = document.createElement("style");
                style.innerHTML = ReportTab.CSS;
                this.reportDiv.appendChild(style);
            },

            drawReport : function(report) {

                this.initReportDiv();
                this.appendStyles();

                var p = IIDXHelper.createElement("p", report["title"], "report-title");
                this.reportDiv.appendChild(p);

                if (report["description"]) {
                    p = IIDXHelper.createElement("p", report["description"], undefined);
                    this.reportDiv.appendChild(p);
                }

                for (var section of report["sections"]) {
                    this.reportDiv.appendChild(this.generateSection(section, 1));
                }
            },

            generateSection : function(section, level) {

                var div = IIDXHelper.createElement("div", "", "report-div-level-" + level);

                // required: header
                var header = section["header"] || "<No Header Specified>"
                let h = IIDXHelper.createElement("h" + level, header, "report-h" + level);
                div.appendChild(h);

                // description
                if (section["description"] != undefined) {
                    let p = IIDXHelper.createElement("p", section["description"], undefined);
                    div.appendChild(p);
                }

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
                                                                                                this.checkForCallback.bind(this, spDiv));
                            client.execGetPredicateStats(this.conn, jsonBlobCallback);
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
                        ngStoreClient.getNodeGroupByIdToJsonStr(nodegroup, this.plotGetNgCallback.bind(this, ngDiv, plotId, level));

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
                                                                                         this.checkForCallback.bind(this, ngDiv),
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
                                                                                         this.checkForCallback.bind(this, ngDiv),
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
                                                                                         this.checkForCallback.bind(this, ngDiv),
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
                                                                                 this.checkForCallback.bind(this.div),
                                                                                 this.g.service.status.url,
                                                                                 this.g.service.results.url);
                ngExecClient.execAsyncDispatchSelectFromNodeGroup(ng, this.conn, null, null, jsonCallback, this.failureCallback.bind(this, div));
            },

            plotGetTableCallback : function(div, plotter, tableRes) {
                div.innerHTML="";
                plotter.addPlotToDiv(div, tableRes);
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

                var tableElem = IIDXHelper.buildDatagridInDiv( div,
    								           function() { return [{sTitle: "class", sType: "string", filter: true},
                                                                    {sTitle: "count", sType: "numeric", filter: true}]; },
								               function() { return rows; },
	                                           undefined,
                                               [[1,'desc']]
                                           );

                this.fixTableStyle(div, tableElem);
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

            tableGetTableCallback : function(div, tableRes) {
                div.innerHTML="";
                var tableElem = tableRes.putTableResultsDatagridInDiv(div, undefined, []);
                this.fixTableStyle(div, tableElem);
            },

            graphGetGraphCallback : function(div, res) {

                if (! res.isJsonLdResults()) {
                    div.innerHTML =  "<b>Error:</b> Results returned from service are not JSON-LD";
                    return;
                }
                div.innerHTML="";  // clear the spinner

                // canvas
                var canvasDiv = document.createElement("div");
                canvasDiv.style.width="100%";
                canvasDiv.style.height="650px";
                canvasDiv.style.margin="1ch";
                div.appendChild(canvasDiv);

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
                div.appendChild(table);

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

                network.startSimulation();
                setTimeout(function(network){ network.fit(); }.bind(this, network), 2500);
            },

            fixTableStyle : function(div, tableElem) {
                tableElem.classList.add("report-table");
                tableElem.style.width="100%";
                div.appendChild(document.createElement("br"));
                div.appendChild(document.createElement("br"));
            },

            statusCallback : function(div, eOrMsg) {
            },
            checkForCallback : function(div, eOrMsg) {
                return false;
            },

            failureCallback : function(div, eOrMsg) {
                var msg = eOrMsg.toString();
                let errorDiv = IIDXHelper.createElement("div", "Error generating this section.", "report-error");

                var summary = null;
                if (msg.indexOf("special.id") > -1) {
                    summary = "Unknown special section: " + msg.split(":")[2];
                } else if (msg.indexOf("Could not find nodegroup with id")) {
                    let lines = msg.split('<br>');
                    for (let l of lines) {
                        if (l.indexOf("rationale") > -1) {
                            summary = l.split('\\n')[0];
                            if (summary.indexOf("Exception") > -1) {
                                summary = summary.split("Exception")[1];
                            }
                        }
                    }
                }
                if (summary != null) {
                    let errorP = IIDXHelper.createElement("p", summary, undefined);
                    errorDiv.appendChild(errorP);
                }

                div.innerHTML = "";
                div.appendChild(errorDiv);
                console.error(eOrMsg);
            },

        }

		return ReportTab;            // return the constructor
	}

);
