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
		 *  callback(OrderElement[])
		 */
		var ModalOrderByDialog= function (sparqlIDs, orderElems, callback) {
            // copy sparqlIDs
            this.sparqlIDs = sparqlIDs.slice();
            
            // copy orderElems
            this.orderElems = [];
            for (var i=0; i < orderElems.length; i++) {
                this.orderElems.push(orderElems[i].deepCopy());
            }
            
            this.callback = callback;
            
            this.div = null;
            this.selTable = null;
            this.selTableDiv = null;
		};
		
		
		ModalOrderByDialog.prototype = {
            
            
            validateCallback : function() {
				if (false) {
					return "Something is wrong";
				} else {
					return null;
				}
			},
			
			okCallback : function() {
                this.callback(this.orderElems);
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
                
                // remove sparqlID's in use in other orderElems
                for (var i=0; i < this.orderElems.length; i++) {
                    if (this.orderElems[i].getSparqlID() != mySparqlID) {
                        var otherID = this.orderElems[i].getSparqlID();
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
                this.updateOrderElems();
                this.updateSelTable();
            },
            
            buildTableRow : function (mySparqlID, myFunc) {
                var sel0 = this.createSparqlIDSelect(mySparqlID);
                sel0.onchange = this.updateRoundTrip.bind(this);

                var sel1 = this.createFuncSelect(myFunc);
                sel1.onchange = this.updateOrderElems.bind(this);

                return [sel0, sel1];
            },
            
            /**
              * Create new this.selTable from this.orderElems
              * updates this.selTable and this.selTableDiv.innerHTML
              */
            updateSelTable : function() {
                var multiFlag = false;
                var colList = ["SPARQL ID", "Order Function"];
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
                
                // if orderElems is empty, create a blank OrderElement
                if (this.orderElems.length == 0) {
                    rows.push(this.buildTableRow("", ""));
                }
                
                // add 'normal' rows
                for (var i=0; i < this.orderElems.length; i++) {
                    
                    rows.push(this.buildTableRow(   this.orderElems[i].getSparqlID(),
                                                    this.orderElems[i].getFunc()      ));
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
             * Create new this.orderElems based on the this.selTable
             */
            updateOrderElems : function() {
                this.orderElems = [];
                
                for (var i=0; i < this.selTable.getNumRows(); i++) {
                    var sparqlID = this.selTable.getCellDom(i,0).getElementsByTagName("select")[0].value;
                    
                    // skip blank rows
                    if (sparqlID != "") {
                        var func     = this.selTable.getCellDom(i,1).getElementsByTagName("select")[0].value;
                        var oe = new OrderElement(sparqlID, func);
                        this.orderElems.push(oe);
                    }
                }
            },
    
            callbackPlus : function() {
                this.orderElems.push(new OrderElement("", ""));
                this.updateSelTable();
            },
            
            callbackRemove : function() {
                var rows = this.selTable.getSelectedIndices();
                if (rows.length == 0) {
                    return;
                }
                this.orderElems.splice(rows[0],1);
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
                this.orderElems.splice(to, 0, this.orderElems.splice(from, 1)[0]);
                this.updateSelTable();
            },
            
            callbackDown : function() {
                var rows = this.selTable.getSelectedIndices();
                if (rows.length == 0) {
                    return;
                }
                var from = rows[0];
                var to = from + 1;
                if (to == this.orderElems.length) return;
                this.orderElems.splice(to, 0, this.orderElems.splice(from, 1)[0]);
                this.updateSelTable();
            },
            
            buildButtonDiv : function() {
                var div = document.createElement("div");
                div.style.align="right";
                div.style.marginBottom = "0.5ch";
                var but = null;
                
                but = IIDXHelper.createIconButton("icon-plus", this.callbackPlus.bind(this));
                div.appendChild(but);
                
                but = IIDXHelper.createIconButton("icon-remove", this.callbackRemove.bind(this));
                div.appendChild(but);
                
                but = IIDXHelper.createIconButton("icon-sort-up", this.callbackUp.bind(this));
                div.appendChild(but);
                
                but = IIDXHelper.createIconButton("icon-sort-down", this.callbackDown.bind(this));
                div.appendChild(but);
                
                return div;
            },
            
            /**
              *  Call nodegroup store to get all nodegroups
              *  Then launch generic dialog with title and callback linked to "OK"
              *  callback(id)
              */
            launchOrderByDialog : function () {
                
                if (this.sparqlIDs.length == 0) {
                    ModalIidx.alert("Order by error", "<b>Query has no return values.</b><br>Can not create an ORDER BY clause.");
                    return;
                }
                
                this.div = document.createElement("div");
                this.div.appendChild(this.buildButtonDiv());
                this.selTableDiv = document.createElement("div");
                this.div.appendChild(this.selTableDiv);
                
                this.updateSelTable();
            
                // launch the modal
                var m = new ModalIidx();
                m.showOKCancel(	
                                "Order By",
                                this.div,
                                this.validateCallback.bind(this),
                                this.okCallback.bind(this),
                                this.cancelCallback.bind(this),
                                "OK",
                                40); 
                    
            },
            
            
		};
	
		return ModalOrderByDialog;
	}
);