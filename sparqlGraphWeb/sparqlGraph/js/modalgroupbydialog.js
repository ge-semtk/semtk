/**
 ** Copyright 2016-2018 General Electric Company
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
    'sparqlgraph/js/selecttable',
    'jquery',

    // shimmed
    'sparqlgraph/js/belmont',
    ],

	function(ModalIidx, IIDXHelper, SelectTable, $) {

		/**
		 *  callback(String[])
		 */
		var ModalGroupByDialog= function (sparqlIDs, groupIds, callback) {
            // copy sparqlIDs
            this.sparqlIDs = sparqlIDs.slice();

            // copy groupIds
            this.groupIds = [];
            for (var id of groupIds) {
                this.groupIds.push(id);
            }

            this.callback = callback;

            this.div = null;
            this.selTable = null;
            this.selTableDiv = null;
		};


		ModalGroupByDialog.prototype = {


            validateCallback : function() {
				if (false) {
					return "Something is wrong";
				} else {
					return null;
				}
			},

			okCallback : function() {
                this.updateGroupElems(true);
                this.callback(this.groupIds);
			},

			cancelCallback : function() {

            },

            createFuncSelect(myFunc) {
                var multiFlag = false;
                var id = null;
                var sel = IIDXHelper.createSelect(id,
                                                  ["", "desc"],
                                                  myFunc.toLowerCase(),
                                                  multiFlag,
                                                  "input-small");
                sel.style.margin="0px 0px 0px 0px";
                return sel;
            },

            createSparqlIDSelect(mySparqlID) {
                var multiFlag = false;
                var id = null;
                var sparqlIDList = [""].concat(this.sparqlIDs);

                // remove sparqlID's in use in other groupIds
                for (var i=0; i < this.groupIds.length; i++) {
                    if (this.groupIds[i] != mySparqlID) {
                        var otherID = this.groupIds[i];
                        var pos = sparqlIDList.indexOf(otherID);
                        if (otherID != "" && pos > -1) {
                            sparqlIDList.splice(pos,1);
                        }
                    }
                }

                // create the select
                var sel = IIDXHelper.createSelect(id,
                                                  sparqlIDList,
                                                  mySparqlID,
                                                  multiFlag,
                                                  "input-xlarge");
                sel.style.margin="0px 0px 0px 0px";
                return sel;
            },

            /*
             * Perform round trip so all selects can be enabled/disabled
             * with currently used sparql ID's, etc.
             */
            updateRoundTrip : function() {
                this.updateGroupElems();
                this.updateSelTable();
            },

            buildTableRow : function (mySparqlID) {
                var sel0 = this.createSparqlIDSelect(mySparqlID);
                sel0.onchange = this.updateRoundTrip.bind(this);

                return [sel0];
            },

            /**
              * Create new this.selTable from this.groupIds
              * updates this.selTable and this.selTableDiv.innerHTML
              */
            updateSelTable : function() {
                var multiFlag = false;
                var colList = ["SPARQL ID"];
                var undefVal = "";
                var filterFlag = false;

                var widthList = [75, 25];

                // snag selected sparqlID off previous table
                var selectedID = null;
                if (this.selTable != null) {
                    var domList = this.selTable.getSelectedValues("SPARQL ID");
                    if (domList.length > 0) {
                        selectedID = domList[0].value;
                    }
                }

                // build rows
                var rows = [];

                // add 'normal' rows
                for (var i=0; i < this.groupIds.length; i++) {

                    rows.push(this.buildTableRow(   this.groupIds[i]));
                }

                // build table
                this.selTable = new SelectTable(rows,
                                                colList,
                                                widthList,
                                                0,
                                                multiFlag,
                                                filterFlag);

                // select old row
                if (selectedID != null) {
                    var rList = this.selTable.findRow(
                                    function(val, r) {
                                        return (r[0].getElementsByTagName("select")[0].value == val);
                                    }.bind(this, selectedID) );
                    if (rList.length > 0) {
                        this.selTable.selectRow(rList[0]);
                    }
                }

                this.selTableDiv.innerHTML = "";
                this.selTableDiv.appendChild(this.selTable.getTableDom());
            },

            /*
             * Create new this.groupIds based on the this.selTable
             *
             * optRmBlanksFlag - if true, remove any blank rows (used for final "OK")
             */
            updateGroupElems : function(optRmBlanksFlag) {
                var rmBlanksFlag = typeof optRmBlanksFlag !== "undefined" ? optRmBlanksFlag : false;
                this.groupIds = [];

                for (var i=0; i < this.selTable.getNumRows(); i++) {
                    var id = this.selTable.getCellDom(i,0).getElementsByTagName("select")[0].value;

                    // if we're not removing blanks or row isn't blank
                    if (!rmBlanksFlag || id !== "")  {
                        this.groupIds.push(id);
                    }
                }
            },

            callbackSelectAll : function() {
                this.groupIds = this.sparqlIDs.slice();
                this.updateSelTable();
            },

            callbackPlus : function() {
                this.groupIds.push("");
                this.updateSelTable();
            },

            callbackRemove : function() {
                var rows = this.selTable.getSelectedIndices();
                if (rows.length == 0) {
                    return;
                }
                this.groupIds.splice(rows[0],1);
                this.updateSelTable();
            },

            callbackUp : function() {
                var rows = this.selTable.getSelectedIndices();
                if (rows.length == 0) {
                    return;
                }
                var from = rows[0];
                var to = from - 1;
                if (to == -1) return;
                this.groupIds.splice(to, 0, this.groupIds.splice(from, 1)[0]);
                this.updateSelTable();
            },

            callbackDown : function() {
                var rows = this.selTable.getSelectedIndices();
                if (rows.length == 0) {
                    return;
                }
                var from = rows[0];
                var to = from + 1;
                if (to == this.groupIds.length) return;
                this.groupIds.splice(to, 0, this.groupIds.splice(from, 1)[0]);
                this.updateSelTable();
            },

            buildButtonDiv : function() {
                var div = document.createElement("div");
                div.style.align="right";
                div.style.marginBottom = "0.5ch";
                var but = null;

                but = IIDXHelper.createIconButton("icon-plus", this.callbackPlus.bind(this), undefined, undefined, undefined, "Add row");
                div.appendChild(but);

                but = IIDXHelper.createIconButton("icon-remove", this.callbackRemove.bind(this), undefined, undefined, undefined, "Remove row");
                div.appendChild(document.createTextNode(" "));
                div.appendChild(but);

                but = IIDXHelper.createIconButton("icon-sort-up", this.callbackUp.bind(this), undefined, undefined, undefined, "Move up");
                div.appendChild(document.createTextNode(" "));
                div.appendChild(but);

                but = IIDXHelper.createIconButton("icon-sort-down", this.callbackDown.bind(this), undefined, undefined, undefined, "Move down");
                div.appendChild(document.createTextNode(" "));
                div.appendChild(but);

                but = IIDXHelper.createIconButton("icon-plus-sign-alt", this.callbackSelectAll.bind(this), undefined, undefined, "Add all", undefined);
                div.appendChild(document.createTextNode(" "));
                div.appendChild(but);


                return div;
            },

            /**
              *  Call nodegroup store to get all nodegroups
              *  Then launch generic dialog with title and callback linked to "OK"
              *  callback(id)
              */
            launch : function () {

                if (this.sparqlIDs.length == 0) {
                    ModalIidx.alert("Group by error", "<b>Query has no return values.</b><br>Can not create an GROUP BY clause.");
                    return;
                }

                this.div = document.createElement("div");
                this.div.appendChild(this.buildButtonDiv());
                this.selTableDiv = document.createElement("div");
                this.div.appendChild(this.selTableDiv);

                // if groupIds is empty, create a blank GroupElement
                if (this.groupIds.length == 0) {
                    this.groupIds = [""];
                }

                this.updateSelTable();

                // launch the modal
                var m = new ModalIidx();
                m.showOKCancel(
                                "Group By",
                                this.div,
                                this.validateCallback.bind(this),
                                this.okCallback.bind(this),
                                this.cancelCallback.bind(this),
                                "OK",
                                40);

            },


		};

		return ModalGroupByDialog;
	}
);
