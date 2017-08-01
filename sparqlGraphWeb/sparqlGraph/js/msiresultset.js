/**
 ** Copyright 2016-17 General Electric Company
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

define([	// properly require.config'ed   bootstrap-modal
        	'sparqlgraph/js/iidxhelper',
			// shimmed
			
		],

	function(IIDXHelper) {
	
		
		var MsiResultSet = function (serviceURL, xhr) {
			this.serviceURL = serviceURL;
			this.xhr = xhr;
			
			this.localUriFlag = false;
			this.escapeHtmlFlag = false;
		};
		
		
		MsiResultSet.prototype = {
			NAMESPACE_YES : 1,
            NAMESPACE_NO : 2,
            NAMESPACE_ONLY : 3,	
            
			isSuccess : function () {
				return this.xhr && this.xhr.status && (JSON.stringify(this.xhr.status).indexOf("success") == 1);
			},
			
			isRecordProcessResults : function () {
				return this.xhr.hasOwnProperty("recordProcessResults");
			},
			
			isSimpleResults : function () {
				return this.xhr.hasOwnProperty("simpleresults");
			},
			
			isTableResults : function () {
				return this.xhr.hasOwnProperty("table");
			},
			
			getColumnName : function (x) {
				return this.getTable().col_names[x];
			},
			
			getColumnNumber : function(name) {
				return this.getTable().col_names.indexOf(name);
			},
			
			getGeneralResultHtml : function () { 
				// build GeneralResultSet html
				
				var html =  "<b>" + this.serviceURL + "</b>";
				
				// always has status
				html += "<br><b>status:</b> " + this.xhr.status;
				
				// don't repeat the message on "success"
				if (status != "success") {
					html += "<br><b>message: </b> " +  IIDXHelper.htmlSafe(this.xhr.message);
				}
				
				// may have rationale regardless of status
				if (this.xhr.hasOwnProperty("rationale")) {
					html += "<br><b>rationale: </b> " +  IIDXHelper.htmlSafe(this.xhr.rationale).replace(/[\n]/, "<br>");
				}
				
				return html;
			},
			
            /*
             * If simple results, build html out of just those fields
             * otherwise, do general results html
             *
             */
            getSimpleResultsHtml : function () {
                
                // A simpleResultSet with no fields in it is generated
                // by the java without "simpleresults".
                if (! this.xhr.hasOwnProperty("simpleresults")) {
                    return this.getGeneralResultHtml();
                }
                
                // put message if there is one
                var html = (this.xhr.hasOwnProperty("@message")) ? "<p><b>message: </b>" + IIDXHelper.htmlSafe(this.xhr.simpleresults["@message"]) + "<p>" : "";
                
                // add any other fields
                for (var key in this.xhr.simpleresults) {
                    if (key != "@message") {
                        html += "<b>" + IIDXHelper.htmlSafe(key) + ": </b>" + IIDXHelper.htmlSafe(this.xhr.simpleresults[key]) + "<br>";
                    }
                }
                
                // remove last <br>
                if (html.slice(-4) == "<br>") {
                    html = html.slice(0, -4);
                }
                
                return html;
            },
            
            /*
             *   Show custom summary of a bad xhr
             *   optTitle should be of the form: "did not return a table".  Default is "failed"
             */
            getFailureHtml : function (optTitle) {
                var title = (typeof optTitle == "undefined" || optTitle == null) ? "failed" :  optTitle;
                
                var urlParts = this.serviceURL.split('/');
                var endpoint = urlParts.pop();
                var service = urlParts.pop();
                
                // service info
                var html = "<b>Service &quot;" + service + "&quot; " + title + "</b><br>";
                html += "<b>endpoint: </b>" + endpoint + " <br>";
                html += "<b>full URL: </b>" + this.serviceURL + " <br>";
                html += "<hr>";
                html += "<b>xhr contents: </b><br>";
                
                // loop through xhr keys
                for (var key in this.xhr) {
                    if (typeof this.xhr[key] == "function") continue;
                    
                    html += "<b>&nbsp;&nbsp;" + IIDXHelper.htmlSafe(key) + ": </b>";
                
                    var val = JSON.stringify(this.xhr[key]);
                    if (val.length > 32) {
                        val = val.slice(0,32)+"...";
                    }
                    html += IIDXHelper.htmlSafe( val ) + "<br>";
                    
                    // do one layer lower keys if it's an object (not an array)
                    if (typeof this.xhr[key] == "object" && ! Array.isArray(this.xhr[key])) {
                        for (var key1 in this.xhr[key]) {
                            html += "<b>&nbsp;&nbsp;&nbsp;&nbsp; - " + IIDXHelper.htmlSafe(key + "." + key1) + ": </b>";
                            var val = JSON.stringify(this.xhr[key][key1]);
                            if (val.length > 32) {
                                val = val.slice(0,32)+"...";
                            }
                            html += IIDXHelper.htmlSafe( val ) + "<br>";
                        }
                    }
                    
                    // stop if it's too big
                    if (html.length > 2048) {
                        html += "...<br>";
                        break;
                    }
                }           
                
                return html;
            },
            
			getRecordProcessResultHtml : function () { 
				// build html out of record process results
				
				
				if (! this.isRecordProcessResults()) {
					return "<b>Error:</b> Results returned from service are not RecordProcessResults";
				}
				
				var fullFlag =  false;
				
				var html = this.getGeneralResultHtml();

				var rpr = this.xhr.recordProcessResults;
				
				
				html += "<br><b>failures encountered:</b> " + rpr.failuresEncountered;
				html += "<br><b>records processed:</b> " + rpr.recordsProcessed;
				
				// error table
				if (rpr.hasOwnProperty("errorTable")) {
					var tab = rpr.errorTable;
					html += "<br><table border='1'>";
					
					var startCol = fullFlag ? 0 :  tab.col_names.length - 2;
					
					// column names
					if (tab.hasOwnProperty("col_names")) {
						html += "<tr>";
						for (var i=startCol; i < tab.col_names.length; i++) {
							html += "<th>" + IIDXHelper.htmlSafe(tab.col_names[i]) + "</th>";
						}
						html += "</tr>";
					}
					
					// column types
					if (fullFlag) {
						if (tab.hasOwnProperty("col_type")) {
							html += "<tr>";
							for (var i=startCol; i < tab.col_type.length; i++) {
								html += "<th>" + IIDXHelper.htmlSafe(tab.col_type[i]) + "</th>";
							}
							html += "</tr>";
						}
					}
					
					// rows
					if (tab.hasOwnProperty("rows")) {
						for (var i=0; i < tab.rows.length; i++) {
							html += "<tr>";
							for (var j=startCol; j < tab.rows[i].length; j++) {
								html += "<td>" + IIDXHelper.htmlSafe(tab.rows[i][j]) + "</td>";
							}
							html += "</tr>";
						}
					}
					
					html += "</table>";
					
				}
				
				return html;
			},
			
			getRecordProcessResultErrorCsv : function (xhr) {
				// get the error table or null if it can't be found for any reason
				
				var ret = "";
				
				// error table
				if (this.xhr.hasOwnProperty("recordProcessResults") && this.xhr.recordProcessResults.hasOwnProperty("errorTable")) {
					var tab = this.xhr.recordProcessResults.errorTable;
					
					// column names
					if (tab.hasOwnProperty("col_names")) {
						for (var i=0; i < tab.col_names.length; i++) {
							ret += tab.col_names[i] + ",";
						}
						ret = ret.slice(0, -1);  // remove last comma
						ret += "\n";
					}
					
					// rows
					if (tab.hasOwnProperty("rows")) {
						for (var i=0; i < tab.rows.length; i++) {
							for (var j=0; j < tab.rows[i].length; j++) {
								ret += tab.rows[i][j] + ",";
							}
							ret = ret.slice(0, -1);  // remove last comma
							ret += "\n";
						}
					}
				}
				
				return (ret.length > 0) ? ret : null;
			},
            
            getRecordProcessResultField : function (field) {
                if (this.isRecordProcessResults() && this.xhr.recordProcessResults.hasOwnProperty(field)) {
                    return this.xhr.recordProcessResults[field];
                } else {
					return null;
				}
            },
			
			getSimpleResultField : function (field) {
				// return a field or null if it can't be found
				
				if (this.isSimpleResults()  && this.xhr.simpleresults.hasOwnProperty(field)) {
					return this.xhr.simpleresults[field];
				} else {
					return null;
				}
			},
			
            getGeneralField : function (field) {
                // return a top-level field, or null if it doesn't exist
                return (this.xhr.hasOwnProperty(field)) ? this.xhr[field] : null;
            },
            
			getTable : function () {
				// efficient helper function with no checks
				return this.xhr.table["@table"];
			},
			
			setLocalUriFlag : function (val) {
				this.localUriFlag = val;
			},
			setEscapeHtmlFlag : function (val) {
				this.escapeHtmlFlag = val;
			},
			
			tableGetCols : function () {
				var ret = [];
				var colNames;
				var typeHash = {};
				var table = this.getTable();
				
				// set up typeHash:   map types to values that jquery datatable understands:  string, numeric, date
				for (var i=0; i < table.col_names.length; i++) {
					
					// get lowercase type after any '#'
					var t = table.col_type[i].toLowerCase();
					if (t.indexOf("#") > -1) {
						t = t.split("#")[1];
					}
					
					// default to string
					var st = 'string';
					
					// check for numbers and dates
					if (t == "float" || t == "double" || t == "decimal" || t == "integer") {
						st = 'numeric';
						
					} else if (t == "dateTime" || t == "date" || t == "time") {
						st = 'date';
					}
					
					typeHash[table.col_names[i]] = st;
				}
				
				colNames = table.col_names;
				
				var filterFlag = (table.row_count > 10);
				// build results
				for (var i=0; i < colNames.length; i++) {
					ret.push({sTitle: colNames[i], sType: typeHash[colNames[i]], filter: filterFlag });
				}
				return ret;
			},
			
			tableGetRows : function () {
				var table = this.getTable();
				
				// which columns are URIs
				var uriCols = [];
				for (var i=0; i < table.col_type.length; i++) {
					if (table.col_type[i].toLowerCase().indexOf("uri") > -1) {
						uriCols.push(i);
					}
				}
				
				// make a copy of the rows (in case we want to change it)
				var row = [];
				var rows = [];
				for (var i=0; i < table.rows.length; i++) {
					row = table.rows[i].slice();
					
					// change URI's to local names
					if (this.localUriFlag) {
						for (var j=0; j < uriCols.length; j++) {
							row[uriCols[j]] = new OntologyName(row[uriCols[j]]).getLocalName();
						}
					}
					
					// escape HTML
					if (this.escapeHtmlFlag) {
						for (var j=0; j < uriCols.length; j++) {
							row[uriCols[j]] = IIDXHelper.htmlSafe(row[uriCols[j]]);
						}
					}
					
					// change undefined to empty strings so the datagrid doesn't crash
					for (var j=0; j < row.length; j++) {
						if (row[j] === undefined) {
							row[j] = "";
						}
					}
					rows.push(row);
				}
				
				return rows;
			},
			
            tableGetNamedRows : function (colNameList, optUndefinedVal, optSortFlag) {
                // get col numbers
                var colNums = [];
                for (var i=0; i < colNameList.length; i++) {
                    colNums.push(this.getColumnNumber(colNameList[i]));
                }
                
                var newRows = [];
                var newRow = [];
                var val = "";
                var rows = this.tableGetRows();
                
                for (var i=0; i < rows.length; i++) {
                    newRow = [];
                    for (j=0; j < colNameList.length; j++) {
                        val = rows[i][colNums[j]];
                        if (val == undefined && typeof optUndefinedVal != "undefined") {
                            newRow.push(optUndefinedVal);
                        } else {
                            newRow.push(val);
                        }
                    }
                    newRows.push(newRow);
                }
                
                // sort by left to right column string if optSortFlag
                if (typeof optSortFlag != "undefined" && optSortFlag) {
                    newRows = newRows.sort(function(a,b) {
                        try {
                            for (var col=0; col<a.length; col++) {
                                if ( a[col] < b[col] ) {
                                    return -1; 
                                }
                                else if ( a[col] > b[col] ) {
                                    return 1;  
                                }
                            }
                            return 0;
                        } catch(err) {
                            return 1;
                        }
                    } ); 
                }
                
                return newRows;
            },
            
			tableGetCsv : function () {
				var table = this.getTable();
				// translate into a csv string
				var csv = "";
				csv +=  table.col_names.join() + "\n";
				var rows = table.rows;
				for (var i=0; i < rows.length; i++) {
					var row = table.rows[i];
					var formatted_row = [];
					for (var j=0; j < row.length; j++) {
						if (row[j].indexOf(",") > -1 || row[j].indexOf("\n") > -1 || row[j].indexOf("\r") > -1) {
							formatted_row.push('"' + row[j] + '"');
						} else {
							formatted_row.push(row[j]);
						}
					}
					csv +=  formatted_row.join() + "\n";
				}
				return csv;
			},
			
			tableDownloadCsv : function () {
				IIDXHelper.downloadFile(this.tableGetCsv(), "table.csv", "text/csv;charset=utf8");
			},
            
			sort : function(optColName) {
				
				var col = (typeof optColName != "undefined") ? this.getTable().col_names.indexOf(colName) : 0;

				this.xhr.table["@table"].rows = this.xhr.table["@table"].rows.sort(function(a,b) {
					// a row with only an optional value that is "null" comes back from virtuoso as a totally empty row
					try {
					    if ( a[col] < b[col] )
					        return -1; 
					    if ( a[col] > b[col] ) 
					        return 1;  
					    return 0;
					} catch(err) {
						return 1;
					}
				} ); 
				
			}, 
            
            
			
			/**
			 * build an html iidx datagrid and add it to the div.
			 * return the datagrid table element.
			 */
			putTableResultsDatagridInDiv : function (div, headerHtml, optFinishedCallback, optMenuList, optMenuCallbackList) {
				
				if (! this.isTableResults()) {
					div.innerHTML =  "<b>Error:</b> Results returned from service are not TableResults";
					return;
				}
				
				return IIDXHelper.buildDatagridInDiv( div, 
													  headerHtml,
								                      this.tableGetCols.bind(this), 
								                      this.tableGetRows.bind(this), 
								                      typeof optMenuList == "undefined" ? ["Save table csv"] : optMenuList, 
								                      typeof optMenuCallbackList == "undefined" ? [this.tableDownloadCsv.bind(this)] : optMenuCallbackList, 
								                      optFinishedCallback);
			},
            
            /**
			 * build an html iidx datagrid and add it to the div.
			 * return the datagrid table element.
			 */
			putTableSelectDatagridInDiv : function (div, optFinishedCallback) {
				
				if (! this.isTableResults()) {
					div.innerHTML =  "<b>Error:</b> Results returned from service are not TableResults";
					return;
				}
				
				return IIDXHelper.buildSelectDatagridInDiv( div, 
								                      this.tableGetCols.bind(this), 
								                      this.tableGetRows.bind(this), 
								                      optFinishedCallback);
			},
			
            // get named column's values as the raw strings
			getStringResultsColumn : function(name) {
				return this.getColumnStrings(this.getColumnNumber(name));
			},
			
			/**
			 * Return list of value strings for a given column.
			 * null if column number is invalid.
			 */
			getColumnStrings : function (col) {
				var cols = this.getColumnCount();
				if (col < 0 || col >= cols) { return null; }
				
				var rows = this.getRowCount();
				var table = this.getTable();
				var ret = [];
				for (var i=0; i < rows; i++) {
					ret.push( (table.rows[i][col] != undefined) ? table.rows[i][col] : "");
				}
				return ret;
			},
			
			//  COMPATIBLE with sparqlServerResult:
			//             isSuccess() - above
			//             getRowCount()
			//             getRsData(row, col, nsFlag)
			//             getStatusMessage()
			
			getRowCount : function() {
				// sparqlServerResult compatibility
				var table = this.getTable();
				
				if (! this.isTableResults()) {
					alert("Service did not return table results.");
				} else {
					return table.rows.length;
				}
			},
			
			getColumnCount : function () {
				
				return this.getTable().col_names.length;
			},
			
			getStatusMessage : function () {
				// don't repeat the message on "success"
				var text = "";
				
				text +=  this.xhr.message;
				
				// may have rationale regardless of status
				if (this.xhr.hasOwnProperty("rationale")) {
					text += "\n";
					text += "rationale: " +  this.xhr.rationale;
				}
				return text;
			},
			
			getRsData : function (row, col, optNsFlag) {
				// convert the value and optionally strip namespace of URIs
				var table = this.getTable();
				
				var stripNsFlag = (typeof optNsFlag !== 'undefined' && optNsFlag === 2);
				
				// get the value
				
				var val = table.rows[row][col];
			
				if (val == undefined) { return null;}
				
				var t = table.col_type[col].toLowerCase();
				if (t.indexOf("#") > -1) {
					t = t.split("#")[1];
				}
				
				if (t.indexOf("uri") > -1 && stripNsFlag) {
						return val.split('#')[1];
				
				} else if (t == "integer" || t == "int") {
					
					// silently truncates at decimal because this should never happen
					// or NaN
					return parseInt(Number(val));
					
				} else if (t == "decimal" || t == "float" || t == "double") {
					
					// return the number or NaN
					return parseFloat(Number(val));
				
				} else {
					return val;
				}
			},
			
			/**
			 * Append another result set
			 * @param otherResults - MsiResultSet
			 * @throws error if otherResults has new column names
			 */
			appendResults : function (otherResults) {
				var colMap = [];
				
				// build colMap
				for (var i=0; i < otherResults.getColumnCount(); i++) {
					var map = -1;
					for (var j=0; j < this.getColumnCount(); j++) {
						if (this.getColumnName(j) === otherResults.getColumnName(i)) {
							map = j;
							break;
						}
					}
					if (map == -1) {
						throw "MsiResultsSet.appendResults() new data has column that isn't in this resultSet: " + other.getColumnName(i);
					} else {
						colMap.push(map);
					}
				}
				
				// create template row
				var table = this.getTable();
				var other = otherResults.getTable();
				var template = [];
				for (var i=0; i < this.getColumnCount(); i++) {
					template.push("");
				}
				
				// append: loop through new rows
				var len = other.rows.length;
				for (var i=0; i < len; i++) {
					// copy the template
					var row = template.slice();
					// insert new values into correct position in the template row
					for (var j=0; j < other.rows[i].length; j++) {
						row[colMap[j]] = other.rows[i][j];
					}
					// push the new row
					table.rows.push(row);
					table.row_count += 1;
				}
			}
		};
	
		return MsiResultSet;            // return the constructor
	}
);