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

			// shimmed
         	'sparqlgraph/dynatree-1.2.5/jquery.dynatree',
            'sparqlgraph/js/ontologytree',
            'sparqlgraph/js/belmont'

		],

    // TODO: this isn't leveraging VisJsHelper properly.  Code duplication.
	function(IIDXHelper, ModalIidx, MsiClientNodeGroupExec, MsiClientNodeGroupStore, MsiClientOntologyInfo, MsiClientStatus, MsiClientResults, MsiResultSet, OntologyInfo, SparqlGraphJson, VisJsHelper, $, vis) {


		//============ local object  ExploreTab =============
		var ReportTab = function(optionsDiv, reportDiv, g) {
            this.conn = null;
            this.optionsDiv = optionsDiv;
            this.reportDiv = reportDiv;

            this.initOptionsDiv();
            this.initReportDiv();

            this.ngExecClient = new MsiClientNodeGroupExec(g.service.nodeGroupExec.url);
            this.ngStoreClient =  new MsiClientNodeGroupStore(g.service.nodeGroupStore.url);
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


        .report-title {
         font-size: 30px;
         text-align: center;
         color: var(--main-dark);
        }

        .table-header {
         backgroundColor: red;
         fontWeight: bold;
        }

        .report-h1 {
         font-family: Georgia, serif;
         font-size: 24px;
         letter-spacing: 2.5px;
         border-bottom: 1px solid main-dark;
         }

        .report-h2 {
         color: #034f84;
         font-size: 18px;
         color: var(--main-medium);
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
        `.replaceAll("var(--main-dark)","#0c2333")
        .replaceAll("var(--main-light)","#91bbe5")
        .replaceAll("var(--main-medium)","#3376bc")
        .replaceAll("var(--main-gray)","#c3d5e8")
        .replaceAll("var(--error)","#c14d7c")
        .replaceAll("var(--warning)","#f4a82c")
        .replaceAll("var(--white)","#ffffff")
        ;

        // load this from triplestore
        ReportTab.RACK = {
            "title": "A sample report on RACK",
            "sections": [
                {
                    "header" : "Data statistics",
                    "sections" : [
                        {
                            "header" : "Class count",
                            "description" : "Number of instances of each exact class",
                            "special" : "class_count"
                        }
                    ]
                },
                {
                    "header" : "Show TESTS verifying REQUIREMENTS",
                    "description" : "This is a display section that doesn't test anything",
                    "sections" : [
                        {
                            "header" : "Histogram: Number of TESTS verifying REQUIREMENTS",
                            "description" : "This section simply demonstrates histograms.  It shows up whether it is needed or not.",
                            "nodegroup" : "report_count_test_verifies_req",
                            "plot" : "hist",
                        },
                        {
                            "header" : "Requirements sorted by number of tests verifying",
                            "description" : "This also always shows up.",
                            "nodegroup" : "report_count_test_verifies_req",
                        },
                    ]
                },
                {
                    "header" : "Search for TESTs that do not verify requirements",
                    "nodegroup" : "query Testcase without requirement",
                    "count" : true,
                    "ranges" : [
                        {
                            "lte" : 0,
                            "status" : "success",
                            "format" : "No test cases found which do not verify a requirement",
                        },
                        {
                            "gte" : 1,
                            "status" : "failure",
                            "format": "Found {0} test cases that do not verify a requirement",
                            "sections": [
                                {
                                    "header" : "TESTs that do not verify requirements",
                                    "nodegroup" : "query Testcase without requirement",
                                },
                            ]
                        }
                    ]
                },
                {
                    "header" : "TESTs  confirmed by a TEST_RESULT",
                    "description" : "All TESTs should be confirmed by at least one TEST_RESULT",
                    "nodegroup" : "report_test_without_result",
                    "count" : true,
                    // first range that matches will be used
                    "ranges" : [
                        {
                            "lte" : 0,
                            "status" : "success",
                            "format" : "No tests found without a confirming test result",
                        },
                        {
                            "gte" : 1,
                            "status" : "failure",
                            "format": "Found {0} tests that are not confirmed.",
                            "sections" : [
                                {
                                    "header" : "TESTs that are not confirmed by a TEST_RESULT",
                                    "nodegroup" : "report_test_without_result",
                                },
                            ]
                        },
                    ]
                },
            ]
        };

		ReportTab.prototype = {
            setConn : function(conn) {
                this.conn = conn;
            },

            // print a div
            //   https://stackoverflow.com/questions/2255291/print-the-contents-of-a-div


            initOptionsDiv : function() {
				this.optionsDiv.innerHTML = "";

				// baseURI
				var form = IIDXHelper.buildHorizontalForm();
                var select = IIDXHelper.createSelect("reportSelect", ["", "RACK"], [""]);
                select.onchange = this.drawReport.bind(this, select);

				var group = IIDXHelper.buildControlGroup("Report: ", select, "choose a report", "reportSelectHelp");
                form.appendChild(group);
				this.optionsDiv.appendChild(form);
            },

            initReportDiv : function() {
                this.reportDiv.innerHTML = "";


            },

            appendStyles : function() {
                var style = document.createElement("style");
                style.innerHTML = ReportTab.CSS;
                this.reportDiv.appendChild(style);
            },

            drawReport : function(select) {
                // TODO: always draws the rack report
                report = ReportTab.RACK;

                var reportName = select.options[select.selectedIndex].text;
                this.appendStyles();

                var p = IIDXHelper.createElement("p", report["title"], "report-title");
                this.reportDiv.appendChild(p);

                for (var section of report["sections"]) {
                    this.reportDiv.appendChild(this.generateSection(section, 1));
                }
            },

            generateSection : function(section, level) {

                var div = IIDXHelper.createElement("div", "", "report-div-level-" + level);

                // print header
                var header = section["header"] || ""
                let h = IIDXHelper.createElement("h" + level, header, "report-h" + level);
                div.appendChild(h);

                // description
                if (section["description"]) {
                    let p = IIDXHelper.createElement("p", section["description"], undefined);
                    div.appendChild(p);
                }

                try {
                    if (section["nodegroup"]) {
                        // create a blank div to cover for async behavior
                        let nodegroupDiv = IIDXHelper.createElement("div", "", undefined);
                        div.appendChild(nodegroupDiv);

                        if (section["plot"]) {
                            this.ngStoreClient.getNodeGroupByIdToJsonStr(section["nodegroup"], this.asyncPlotSection.bind(this, nodegroupDiv, section, level));
                        } else if (section["count"]) {

                        } else {

                        }

                    } else if (section["special"]) {

                    }

                    if (section["sections"]) {
                        for (subSection of section["sections"]) {
                            div.appendChild(this.generateSection(subSection, level+1));
                        }
                    }
                } catch (e) {
                    let errorDiv = IIDXHelper.createElement("div", "Error generating this section.", "report-error");
                    div.appendChild(errorDiv);
    				console.error(e);
    				return;
    			}

                return div;
            },

            asyncPlotSection : function(div, section, level, jsonStr) {
                var sgJson = new SparqlGraphJson().fromJson(jsonStr);
                var ng = sgJson.getNodeGroup();
                var conn = sgJson.getSparqlConn();
                var plotSpecHandler = sgJson.getPlotSpecsHandler();
                var plotter = plotSpecHandler.getPlotByName(section["plot"]);
                div.innerHTML = "Hope to add plot " + section["plot"];
            }

        }

		return ReportTab;            // return the constructor
	}

);
