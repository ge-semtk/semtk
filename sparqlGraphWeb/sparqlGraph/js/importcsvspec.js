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
 *  ImportCsvSpec - the data structure specifying how to import data
 *
 */

var gGlobalFromElsewhere;


define([	// properly require.config'ed
         	'jquery',
         	'jquery-csv',
		
         	// shimmed
    		//'logconfig',
			
		],

	function($) {
	
	    //============ local object ImportColumn =============
		var ImportColumn = function (name, type) {
			this.name = name;
			this.type = type;
		};
		
		ImportColumn.prototype = {
				
		};
		
		//============ local object ImportText =============
		var ImportText = function (string) {
			this.string = string;
		};
		
		ImportText.prototype = {
				
		};
		
		
		
	
		//=========== public object  ImportCsvSpec =============
		var ImportCsvSpec = function() {
		    this.csvLists = [];  // csv file defining columns
		    this.columns = [];   // array of ImportColumn 
		    
		   
		};
		
		ImportCsvSpec.prototype = {
			
			
			getColNames : function () {
				var ret = [];
				for (var i=0; i < this.columns.length; i++) {
					ret.push(this.columns[i].name);
				}
				return ret;
			},
			
			loadCsvContents : function (fileContentStr) {
				
				// load in the first line
				this.csvLists = $.csv.toArrays(fileContentStr); 
				
				// loop through the columns
				var headerRow = this.csvLists[0];
				for (var col=0; col < headerRow.length; col ++) {
					// create a new column
					// search for too low, too high, quotes (x22 and x27)
					if (headerRow[col].search(/[\x00-\x1F\x7F-\xFF\x22\x27]/) > -1) {
						this.columns = [];
						return ("Illegal character in column header: " + headerRow[col]);
					} 
					
					var column = new ImportColumn(headerRow[col], 
												  this.calcCsvColumnTypes(col));
					this.columns.push(column);
					
				}
				// return a message if there is an error
				return null;
			},
			
			calcCsvColumnTypes : function (col) {
				// sets all column types to string
				return "string";
				
				// for (var row=0; row < this.csvLists.length; row ++) {
				//	int val = this.csvLists[row][col];
				// }
			},
			
			loadCsvFileAsync : function (csvFile, callback) {
    			var reader = new FileReader();
    			reader.onload = function(event) {
    				
    				var contents = event.target.result;
    				
    				// handle MAC line returns
    				contents = contents.replace(/\r([^\n])/g, "\n$1").replace(/\r$/g, "\n");
    			    
    			    var msg = this.loadCsvContents(contents);
    			    
    				 // print error if any, else use the new importSpec
            		if (msg) {
            			kdlLogAndAlert(msg);
            		} else {
            			callback();
            		}
    			}.bind(this);

    			reader.onerror = function(event) {
    				kdlLogAndAlert("CSV file could not be read! Code " + event.target.error.code);
    			};

    			reader.readAsText(csvFile);    
    			
		    },
		    
		};
		
		return ImportCsvSpec;            // return the constructor
	}
	
);
