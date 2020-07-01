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
    'sparqlgraph/js/columnvalidator',
    'sparqlgraph/js/datavalidator',
    'sparqlgraph/js/modaliidx',
    'sparqlgraph/js/iidxhelper',
    'sparqlgraph/js/selecttable',
    'jquery',

    // shimmed
    'sparqlgraph/js/belmont',
    ],

	function(ColumnValidator, DataValidator, ModalIidx, IIDXHelper, SelectTable, $) {


		var ModalValidateColumnDialog= function (cv, colName, callback) {

            // get column validator
            if (cv == null) {
                cv = new ColumnValidator(colName);
            }

            this.colValidator = cv.deepCopy();
            this.colName = colName;
            this.callback = callback;    // callback(colValidator)

            this.div = null;
            this.selTable = null;
            this.selTableDiv = null;
		};


		ModalValidateColumnDialog.prototype = {


            validateCallback : function() {
				return this.colValidator.getAllErrorsHTML();
			},

			okCallback : function() {
                this.callback(this.colValidator);
			},

			cancelCallback : function() {

            },

            createOperationsSelect : function(opAlias){
                var multiFlag = false;
                var id = null;
                // get list of opAliass [{ name:x, alias:x, avail:x, defaultVal: x}]
                var validOps = this.colValidator.getValidOperations();
                var ops = [];
                var disabledList = [];
                for (var o of validOps) {
                    ops.push([o.alias, o.name]);
                    if (o.alias != opAlias && o.avail) {
                        disabledList.push(o.alias);
                    }
                }

                var sel = IIDXHelper.createSelect(id,
                                                  ops,
                                                  [opAlias],
                                                  multiFlag,
                                                  "input-medium",
                                                  disabledList);
                sel.required = true;
                sel.style.margin="0px 0px 0px 0px";
                sel.onchange = this.tableChanged.bind(this);
                return sel;
            },

            createParamInput : function (opAlias, paramStr) {
                if (this.colValidator.isOpEnum(opAlias)) {
                    input = IIDXHelper.createSelect(null, this.colValidator.getEnumLegalValues(opAlias), [paramStr], false, "", []);
                } else {
                    input = IIDXHelper.createTextInput();
                    input.value = paramStr;
                }
                input.style.margin = "0";
                input.onchange = this.tableChanged.bind(this);
                return input;
            },

            updateColValidator : function() {
                // update the column validator from the screen
                this.colValidator = new ColumnValidator(this.colName);
                for (var r=0; r < this.selTable.getNumRows(); r++) {
                    var opAlias = this.getTableRowOpAlias(r);
                    var paramStr = this.getTableRowParamStr(r);
                    this.colValidator.setValidator(opAlias, paramStr);
                }
            },

            // update this.colValidator
            // fix all the disabledOptions on the selects
            // error check all the params
            tableChanged: function() {

                this.updateColValidator();
                this.rebuildTable();

            },

            // presumes selTable has entries for Everything in colValidator
            // rebuilds the whole thing based on types and errors etc.
            //
            // SIDE-EFFECT: changes illegal enumerated values to something legal
            rebuildTable : function () {
                // table should have the right number of rows and columns already
                // flag any errors
                // disable any invalid select options
                for (var r=0; r < this.selTable.getNumRows(); r++) {

                    // swap in a new select with the right things disabled
                    var oldSel = this.selTable.getCellDom(r,0).firstChild;
                    var opAlias = this.getTableRowOpAlias(r);

                    var newSel = this.createOperationsSelect(opAlias);
                    oldSel.parentNode.replaceChild(newSel, oldSel);

                    var oldInput = this.selTable.getCellDom(r,1).firstChild;
                    var paramStr = this.getTableRowParamStr(r);

                    // if parameter is suddenly an illegal enumeration, set it to the default
                    if (this.colValidator.isOpEnum(opAlias) && this.colValidator.errorCheckValueStr(opAlias, paramStr) != null) {
                        paramStr = String(this.colValidator.getEnumLegalValues(opAlias)[0]);
                        this.colValidator.setValidator(opAlias, paramStr);
                    }

                    var newInput = this.createParamInput(opAlias, paramStr)

                    // set "error" on parameter div
                    if (this.colValidator.errorCheckValueStr(opAlias, paramStr) != null) {
                        newInput.style.color = "red";
                    }
                    oldInput.parentNode.replaceChild(newInput, oldInput);
                }
            },

            // build a table row  DOMs with dummy select and no errors shown in the param
            buildTableRow : function (opAlias, param) {

                // create a dummy select, it will be fixed by tableChanged()
                var dummySel = IIDXHelper.createSelect(null, [opAlias], [opAlias], false, "", []);

                var dummyInput = IIDXHelper.createTextInput();
                dummyInput.value = String(param);

                return [dummySel, dummyInput];
            },

            /**
              * Create new this.selTable from this.colValidator
              * updates this.selTable and this.selTableDiv.innerHTML
              */
            updateTableFromColValidator : function() {
                var multiFlag = false;
                var colList = ["validator", "parameter"];
                var undefVal = "";
                var filterFlag = false;

                // snag selected sparqlID off previous table
                var selectedID = null;
                if (this.selTable != null) {
                    var domList = this.selTable.getSelectedValues("validator");
                    if (domList.length > 0) {
                        selectedID = domList[0].value;
                    }
                }

                // build rows
                var rows = [];

                // add 'normal' rows
                for (var validator of this.colValidator.getValues()) {

                    rows.push(this.buildTableRow(   validator.alias,
                                                    validator.value      ));
                }

                // build table
                this.selTable = new SelectTable(rows,
                                                colList,
                                                [33,67],
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

                this.tableChanged();
            },

            // keep colValidator up to date
            // add a table row
            callbackPlus : function() {
                var opAlias = this.colValidator.getFirstUnusedOperation();
                if (opAlias == null) {
                    ModalIidx.alert("Error", "All validations are already in use.");
                } else {
                    var param = this.colValidator.getDefaultValue(opAlias.key);
                    var rowDoms = this.buildTableRow(opAlias.alias, param);
                    this.selTable.addRow(rowDoms);

                    this.colValidator.setValidator(opAlias.key, param);
                    this.tableChanged();
                }
            },

            // keep colValidator up to date
            // delete a table row
            callbackRemove : function() {
                var rows = this.selTable.getSelectedIndices();
                if (rows.length == 0) {
                    return;
                }
                var selectedIndex = rows[0];
                var alias = this.getTableRowOpAlias(selectedIndex);
                // clear validator from colValidator
                this.colValidator.clearValidator(alias);
                // clear row from table
                this.selTable.removeRow(selectedIndex);
                this.tableChanged();
            },

            getTableRowOpAlias : function (row) {
                var sel = this.selTable.getCellDom(row, 0).firstChild;
                return sel.selectedOptions[0].text;
            },

            getTableRowParamStr : function (row) {
                var inputDom =  this.selTable.getCellDom(row,1).firstChild;
                if (inputDom.type == "text") {
                    return inputDom.value;
                } else if (inputDom.type.startsWith("sel")) {
                    return inputDom.selectedOptions[0].text;
                }
            },

            buildButtonTable : function() {
                var table = document.createElement("table");
                table.style.marginBottom = "0.5ch";
                table.style.width = "100%";
                var tr = document.createElement("tr");
                table.appendChild(tr);
                var td1 = document.createElement("td");
                td1.align="left";
                tr.appendChild(td1);
                var td2 = document.createElement("td");
                td2.align="right";
                tr.appendChild(td2);

                var but = null;

                but = IIDXHelper.createIconButton("icon-plus", this.callbackPlus.bind(this));
                td1.appendChild(but);

                but = IIDXHelper.createIconButton("icon-remove", this.callbackRemove.bind(this));
                td1.appendChild(but);

                but = ModalIidx.createWikiButton("Ingesting-CSV-Data#data-validation");
                td2.appendChild(but);
                return table;
            },

            /**
              *  Call nodegroup store to get all nodegroups
              *  Then launch generic dialog with title and callback linked to "OK"
              *  callback(id)
              */
            launch : function () {


                this.div = document.createElement("div");
                this.div.appendChild(this.buildButtonTable());
                this.selTableDiv = document.createElement("div");
                this.div.appendChild(this.selTableDiv);



                this.updateTableFromColValidator();

                // launch the modal
                var m = new ModalIidx();
                m.showOKCancel(
                                "Validation rules for column: " + this.colName,
                                this.div,
                                this.validateCallback.bind(this),
                                this.okCallback.bind(this),
                                this.cancelCallback.bind(this),
                                "OK",
                                40);

            },


		};

		return ModalValidateColumnDialog;
	}
);
