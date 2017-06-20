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
//  This builds an HTML table mean for selecting (like a <select>).
//  It is a severe compromise due to the fact that IIDX dataTables / dataGrid
//  does not seem up to the task, and we don't want to import yet 
//  another version of jquery.   So it is hand-spun.
//
//  Uses .css from IIDX
//

define([	// properly require.config'ed   bootstrap-modal
        	'sparqlgraph/js/iidxhelper',
			// shimmed
			
		],

	function(IIDXHelper) {
	
		/*
         *  Params mimic jquery dataTable:
         *    cols - array of column names
         *    rows - array of row arrays.  Can have "" but not undefined.
         *    widths - array of column widths in %
         *    heightRows - height in rows
         *
         *  Cell widths:  are in percents.  Contents are truncated w/ ellipses
         *  heightRows is approximate
         */
		var SelectTable = function (rows, cols, widths, heightRows, multiFlag) {
			this.rows = rows;
            this.cols = cols;
            this.widths = widths;
            this.heightRows = heightRows;
            this.multiFlag = multiFlag;
            
            this.dom = null;
            this.headTab = null;
            this.rowsTab = null;
            this.filter = null;     // this.filter[12][1] = true if row 12 col 1 matches filter
            
            this.populateTable();
		};
		
		SelectTable.prototype = {
            getTableDom : function () {
                return this.dom;
            },
            
            getSelectedValues : function (colName) {
                var ret = [];
                var indices = this.getSelectedIndices();
                
                for (var i=0; i < indices.length; i++) {
                    ret.push(this.rows[indices[i]][this.getColumnByName(colName)]);
                }
                
                return ret;
            },
            
            getColumnByName : function (colName) {
                var match = colName.toLowerCase();
                
                for (var i=0; i < this.cols.length; i++) {
                    if (this.cols[i].toLowerCase() == match) {
                        return i;
                    }
                }
                return -1;
            },
            
            /*
             *
             */  
			populateTable : function () {
                this.filter = [];
                var row, cell;
                this.dom = document.createElement("div");
                
                // header table
                this.headTab = document.createElement("table");
                this.dom.appendChild(this.headTab);
                this.headTab.classList.add("table");
                this.headTab.classList.add("table-condensed");
                this.headTab.style.width="100%";
                this.headTab.style.marginBottom="0";
            
                // header
                var head = document.createElement("thead");
                var input;
                this.headTab.appendChild(head);
                
                row = document.createElement("tr");
                head.appendChild(row);
                
                // header
                for (var i=0; i < this.cols.length; i++) {
                    cell = document.createElement("th");
                    row.appendChild(cell);
                    cell.innerHTML = this.cols[i];
                    
                    // filter text box
                    cell.appendChild(document.createElement("br"));
                    input = document.createElement("input");
                    cell.appendChild(input);
                    input.type = "text";
                    input.classList.add("input-small");
                    input.placeholder=("filter...");
                    input.onkeyup=this.filterCallback.bind(this, input, i);
                    
                    // cell styles
                    cell.style.width = this.widths[i] + "%";
                    cell.style.whiteSpace = "nowrap";
                    cell.style.overflow = "hidden";
                    cell.style.textOverflow = "ellipsis";
                }
                
                // row div and # rows
                var rowDiv = document.createElement("div");
                this.dom.appendChild(rowDiv);
                rowDiv.style.height = this.heightRows * 3 + "ch";
                rowDiv.style.width = "100%";
                rowDiv.style.overflowY = "auto";
                
                this.rowsTab = document.createElement("table");
                rowDiv.appendChild(this.rowsTab);
                this.rowsTab.classList.add("table");
                this.rowsTab.classList.add("dataTable");
                this.rowsTab.classList.add("table-condensed");
                this.rowsTab.classList.add("table-bordered");
                this.rowsTab.style.width="100%";
                this.rowsTab.style.tableLayout="fixed";
                
                // body
                var body = document.createElement("tbody");
                this.rowsTab.appendChild(body);
                
                // loop through rows
                for (i=0; i < this.rows.length; i++) {
                    this.filter.push([]);
                    
                    // create row
                    row = document.createElement("tr");
                    row.classList.add("odd");
                    row.style.height - "1ch";
                    row.onclick = this.rowClick.bind(this);
                    body.appendChild(row);
                    
                    // loop through cols
                    for (var j=0; j < this.cols.length; j++) {
                        this.filter[i].push(true);
                        cell = document.createElement("td");
                        row.appendChild(cell);
                        cell.innerHTML = this.rows[i][j];
                        cell.style.width = this.widths[j] + "%";
                        cell.style.height = "1ch";
                        cell.style.whiteSpace = "nowrap";
                        cell.style.overflow = "hidden";
                        cell.style.textOverflow = "ellipsis";
                    }
                }
            },
            
            filterCallback : function(textInput, col) {
                var filter = textInput.value.toLowerCase();
                var last, curr;
                
                for (var i=0; i < this.rowsTab.rows.length; i++) {
                    // save last and set new filter
                    last = this.filter[i][col];
                    curr = (this.rows[i][col].toLowerCase().indexOf(filter) > -1);
                    this.filter[i][col] = curr;
                    
                    if (curr == last) {
                        
                    } else if (!curr) {
                        this.rowsTab.rows[i].style.display = "none";
                    } else {
                        // switched to false.  check all of them
                        this.rowsTab.rows[i].style.display = "";
                        for (var j=0; j < this.cols.length; j++) {
                            if (!this.filter[i][j]) {
                                this.rowsTab.rows[i].style.display = "none";
                                break;
                            }
                        }  
                    }
                }
            },
            
            rowClick : function (e) {
                // unselect others if not multiFlag
                if (! this.multiFlag) {
                    for (var i=0; i < this.rowsTab.rows.length; i++) {
                        if (this.rowsTab.rows[i] != e.target.parentElement) {
                            this.rowsTab.rows[i].classList.remove("row_selected");
                        }
                    }
                } 
                
                // toggle selected row
                if (e.target.parentElement.classList.contains("row_selected")) {
                    e.target.parentElement.classList.remove("row_selected");
                } else {
                    e.target.parentElement.classList.add("row_selected");
                }
            },
            
            /*
             * return the selected row index
             * or -1
             */
            getSelectedIndices : function () {
                var ret = [];
                for (var i=0; i < this.rowsTab.rows.length; i++) {
                    if (this.rowsTab.rows[i].classList.contains("row_selected")) {
                        ret.push(i);
                    }
                }
                return ret;
            }
        };
	
		return SelectTable;            // return the constructor
	}
);