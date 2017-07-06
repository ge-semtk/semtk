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
 *  
 *  Call the Sparql Server and execute SPARQL
 *  
 *  SAMPLE CODE:
 *  See fusekiserverinterface.html
 *  
 *  IMPORTANT NOTES:
 *  (1) This code ignores some nice functionality in the return JSON and does some other sub-optimal things
 *      BECAUSE IT MIMICS QueryServerInterface.
 *      SparqlServerInterface and QueryServerInterface need to be interchangeable.
 *  (2) There is a SERIOUS BUG in JSONP which causes it to fail silently on a SPARQL error...or any other error.
 *  	It will time out with a vague message.   Setting the timeout is tricky.   It is in ExecuteQuery()
 *      I can't find a better way to get around CORS issues than JSONP, so at the moment we're stuck with the bug.
 *
 *  NEEDS THESE:
 	<script type="text/javascript" src="../jquery/jquery.js"></script>
 	<script type="text/javascript" src="../jquery/jquery.jsonp-1.0.4.min.js"></script>
 */


/*
 * SparqlServerInterface
 * 
 * This needs to be interchangeable with QueryServerInterface
 */

// TODO: 
//    optResultsType should be moved down to the queries, not the SparqlServerInterface constructor
//    different formats should return different results object types
//
SparqlServerInterface.FUSEKI_SERVER = "fuseki";
SparqlServerInterface.VIRTUOSO_SERVER = "virtuoso";

SparqlServerInterface.TABLE_RESULTS = 0;
SparqlServerInterface.GRAPH_RESULTS = 1;

function SparqlServerInterface(serverType, serverURL, dataset) {
	
	// give base URL for the fuseki and the dataset parameter.
	this.serverType = serverType;
	this.serverURL = serverURL;
	this.dataset = dataset;
	
	// Fuseki and Virtuoso put the /dataset and /sparql in different orders in the URL!
	if (serverType == SparqlServerInterface.FUSEKI_SERVER) {
		this.queryURL = encodeURI(this.serverURL) +"/" +  encodeURIComponent(this.dataset) + "/sparql?output=json";
		
	} else if (serverType == SparqlServerInterface.VIRTUOSO_SERVER) {
		this.queryURL = encodeURI(this.serverURL) + "/sparql?default-graph-uri=" + encodeURIComponent(dataset);
	}
};

SparqlServerInterface.prototype = {
	equals : function (other) {
		return(other.serverType == this.serverType && other.serverURL == this.serverURL && other.dataset == this.dataset);
	},
	
	executeQuery : function(sparql, callback, optResultsType) {
		return this.executeQueryAJAXPost(sparql, callback, undefined, this.getResultsType(optResultsType));
	//	return this.executeQueryJSONP(sparql, callback, undefined, this.getResultsType(optResultsType));
	},
	
	executeAndParse : function(sparql, callbackQSResult, optResultsType) {
		// Just for backwards compatibility and compatibility with QueryServerInterface
		
		// use the instance of the KDL Easy Logger, eLogger to log the results.
		try{
			var detailsArr = [];
      		var d1 = new KDLDetailsPair("Query", sparql);
	       	detailsArr.push(d1);

			eLogger.logEvent("SPARQL query fired", detailsArr, null);
		}
		catch(e){
			console.log("the logging failed");
		}
		
		
		return this.executeQuery(sparql, callbackQSResult, this.getResultsType(optResultsType));
	},
	
    executeAndParseToSuccess(sparql, successQSResCallback, failureCallback, optResultsType) {
        
        var successCallback = function(qsResCallback, failCallback, qsResult) {
            if (qsResult.isSuccess()) {
                qsResCallback(qsResult);
            } else {
                failCallback(qsResult.getStatusMessage());
            }
        }.bind(this, successQSResCallback, failureCallback);
        
        this.executeAndParse(sparql, successCallback, optResultsType);
    },
    
	executeAndParseGet : function(sparql, callbackQSResult, optResultsType) {
		// Just for backwards compatibility and compatibility with QueryServerInterface
		
		// use the instance of the KDL Easy Logger, eLogger to log the results.
		try{
			var detailsArr = [];
      		var d1 = new KDLDetailsPair("Query", sparql);
	       	detailsArr.push(d1);

			eLogger.logEvent("SPARQL query fired", detailsArr, null);
		}
		catch(e){
			console.log("the logging failed");
		}
		
		
		//return this.executeQuery(sparql, callbackQSResult, this.getResultsType(optResultsType));
		return this.executeQueryJSONP(sparql,  callbackQSResult, undefined, this.getResultsType(optResultsType));
	},
	
	getServerType : function() {
		return this.serverType;
	}, 
	
	getServerURL : function() {
		return this.serverURL;
	},
	
	getDataset : function() {
		return this.dataset;
	},
	
	setServerType : function(x) {
		this.serverType = x;
	}, 
	
	setServerURL : function(x) {
		return this.serverURL = x;
	},
	
	setDataset : function(x) {
		return this.dataset = x;
	},
	getResultsType : function(optResultsType) {
		return (typeof optResultsType === 'undefined') ? SparqlServerInterface.TABLE_RESULTS : optResultsType;
	},
	
	getFormatValue : function(optResultsType) {
		var resultsType = this.getResultsType(optResultsType);

		var format = "";
		if (resultsType === SparqlServerInterface.TABLE_RESULTS) {
			format = "application/sparql-results+json";
		} else {   // GRAPH_RESULTS
			format = "application/x-json+ld";
		}
		return format;
	}, 
	
	getFormatURIParam : function(optResultsType) {
			return "&format=" + encodeURIComponent(this.getFormatValue(optResultsType));
	},
	
	executeQueryXMLHTTP : function(sparql, callback, optTimeout, optResultsType) {
		// ************** this version fails on penobscot due to CORS error of some kind. ****************************

		// SIMPLEST: execute some SPARQL and then callback(responseText)
		// This is really PRIVATE, but you can call it as a light-weight afdlternative if you'd like.
		var timeout = (typeof optTimeout === 'undefined') ? 15000 : optTimeout;
		var resultsType = this.getResultsType(optResultsType);
		var url = encodeURI(this.queryURL) + this.getFormatURIParam(optResultsType) + "&query=" + encodeURIComponent(sparql);
		
		var xhr = new XMLHttpRequest();
		xhr.open("GET", url, true);
		xhr.timeout = timeout;
		xhr.ontimeout = function () {
			res = new SparqlServerResult(null, "timeout", "Query timed out after " + String(timeout/1000) + " sec:\n\n" + sparql);
    		callback(res);
		}; 
		// Response handlers.
		xhr.onload = function() {
			var text = xhr.responseText;
			var title = getTitle(text);
		};

		xhr.onreadystatechange = function() {
			if (xhr.readyState === 4) {   // if request is complete
				if (xhr.status === 200) {  // if status is success
					var res = null;
		        	if (resultsType === SparqlServerInterface.TABLE_RESULTS) {
		        		res = new SparqlServerResult(data, "success", null);
		        	} else {
		        		res = new SparqlServerGraphResult(data, "success", null);
		        	}
		        	callback(res);
				} else {
					var res = new SparqlServerResult(null, "error", "Query failed with status: " + xhr.status + "\nServer response: \n" + xhr.responseText);
	        		callback(res);
				}
			} 
		};
		

		xhr.send();
		
	},
	

	executeQueryAJAXGet : function(sparql, callback, optTimeout, optResultsType) {
		// ************** ajax version does not call error() callback until the timeout ****************/

		// SIMPLEST: execute some SPARQL and then callback(responseText)
		// This is really PRIVATE, but you can call it as a light-weight afdlternative if you'd like.
		var timeout = (typeof optTimeout === 'undefined') ? 15000 : optTimeout;
		var resultsType = this.getResultsType(optResultsType);
		// The "callback=?" on the end is what makes this JSONP
		var url = this.queryURL + this.getFormatURIParam(optResultsType) + "&query=" + encodeURIComponent(sparql) + "&callback=?";

		$.ajax({
	        url      : url,
	        dataType : "jsonp",
	        timeout  : timeout,
	        statusCode: {
	        	404: function() {
	        		alert( "Status code 404");   // never happens
	        	}
	        },
	        success  : function(data, status){
	        	var res = null;
	        	if (resultsType === SparqlServerInterface.TABLE_RESULTS) {
	        		res = new SparqlServerResult(data, "success", null);
	        	} else {
	        		res = new SparqlServerGraphResult(data, "success", null);
	        	}
	        	callback(res);
	        },
	        error    : function(XHR, textStatus, errorThrown) {
	        	var res = new SparqlServerResult(null, "error", "Query failed with status: " + textStatus + "\nServer response: \n" + errorThrown);
        		callback(res);
	        },

	    });
	},

	executeQueryAJAX : function(sparql, callback, optTimeout, optResultsType) {
		return this.executeQueryAJAXPost(sparql, callback, optTimeout, optResultsType);
	
	},

	executeQueryAJAXPost : function(sparql, callback, optTimeout, optResultsType) {
			// ************** ajax version does not call error() callback until the timeout ****************/
	
			// SIMPLEST: execute some SPARQL and then callback(responseText)
			// This is really PRIVATE, but you can call it as a light-weight afdlternative if you'd like.
			var timeout = (typeof optTimeout === 'undefined') ? 15000 : optTimeout;
			var resultsType = this.getResultsType(optResultsType);
			var url = this.serverURL +  "/sparql";
			var me = this;
			
			$.ajax({
		        url      : url,
		        type	 : "POST",
		        dataType : "json",
		        data	 : { "query" : sparql, "default-graph-uri" : me.dataset, "format" :  me.getFormatValue(resultsType)},
		        timeout  : timeout,
		        statusCode: {
		        	404: function() {
		        		alert( "Status code 404");   // never happens
		        	}
		        },
		        success  : function(data, status){
		        	var res = null;
		        	   	if (resultsType === SparqlServerInterface.TABLE_RESULTS) {
		        		res = new SparqlServerResult(data, "success", null);
		        	} else {
		        		res = new SparqlServerGraphResult(data, "success", null);
		        	}
		        	callback(res);
		        },
		        error    : function(XHR, textStatus, errorThrown) {
		        	var res = new SparqlServerResult(null, "error", "Query failed with status: " + textStatus + "\nServer response: \n" + errorThrown);
	        		callback(res);
		        },
	
		    });
		},
		
	executeQueryJSONP : function(sparql, callback, optTimeout, optResultsType) {
//************** there seems to be a size limit (8K?) on the response, with no error message.  Consider using AJAX when expecting large responses ****************/  
		/* when used with iids, this needs requre.js magic */
		
		/* can not be synchronous */

		// SIMPLEST: execute some SPARQL and then callback(responseText)
		// This is really PRIVATE, but you can call it as a light-weight afdlternative if you'd like.
		var timeout = (typeof optTimeout === 'undefined') ? 30000 : optTimeout;
		var resultsType = this.getResultsType(optResultsType);
		var format = "";
		
		if (resultsType === SparqlServerInterface.TABLE_RESULTS) {
			format = "application/sparql-results+json";
		} else {   // GRAPH_RESULTS
			format = "application/x-json+ld";
		}
		
		// The "callback=?" on the end is what makes this JSONP
		// Timeout:  tell the virtuoso server to timeout a half second after the jsonp call times out
		//           fuseki will ignore it
		//           virtuoso will take it
		var url = this.queryURL + "&query=" + encodeURIComponent(sparql) + this.getFormatURIParam(optResultsType) + "&timeout=" + String(timeout + 500) + "&callback=?";
        //alert(url);
		$.jsonp({
	        url      : url,
	        dataType : "jsonp",
	        timeout  : timeout,
	        success  : function(data, status){
	        	console.log("status: " + status);
	        	var res = null;
	        	if (resultsType === SparqlServerInterface.TABLE_RESULTS) {
	        		res = new SparqlServerResult(data, "success", null);
	        	} else {
	        		res = new SparqlServerGraphResult(data, "success", null);
	        	}
	        	callback(res);
	        },
	        error    : function(xopt, textStatus) {
	        	if (textStatus == "timeout") {
	        		var res = new SparqlServerResult(null, "timeout", "Query timed out after " + String(timeout/1000) + " sec:\n\n" + sparql);
	        		callback(res);
	        	}
	        	
	        	else if (textStatus == "error") {
	        		// re-run the query as AJAX with a short timeout
	        		// HOPEFULLY, this will result a correct error message
	        		// JSONP doesn't seem to be capable of finding the error message, (but it does great at detecting that one occurred)
	        		var a = $.ajax({
	        	        url     : url,
	        	        type    : "GET",
	        	        async   : false
	        	    });
	        		
	        		var msg;
	        		if (! a.hasOwnProperty('responseText')) msg = "Query failed with no response text.\nCheck for syntax errors, bad connections, or results too large.";
	        		else if (a.responseText == "") msg = "Query failed.  \nError string is empty (probably due to CORS violation).\nError may be available on your browser's console.";
	        		else msg = a.resonseText
	        		
	        		var res = new SparqlServerResult(null, "error", msg);
	        		callback(res);

	        	}   
	        	else {
	        		var res = new SparqlServerResult(null, textStatus, "Query failed.  Unexpected status: " + textStatus);
	        		callback(res);
	        	}
	        	
	        },
	    });
	},
	
	toString : function() {
		return 	this.serverType + " server on " + this.serverURL + ", dataset='" + this.dataset + "'";
		// Fuseki and Virtuoso put the 
	}
};
		   
/*
 * SparqlServerResult
 * 
 * This needs to be interchangeable with QueryServerResult
 */

function SparqlServerResult(resultJSON, statusWord, statusMessage) {
	//
	this.jsonObj = resultJSON;
	this.statusWord = statusWord;
	this.statusMessage = statusMessage;
	this.sTypeHash = {};
	this.sortAscendHash = {};
	this.sortDescendHash = {};
};

SparqlServerResult.prototype = {
	NAMESPACE_YES : 1,
	NAMESPACE_NO : 2,
	NAMESPACE_ONLY : 3,
	ESCAPE_HTML : 10,
	
	isSuccess : function() {
		return this.statusWord == "success";
	},
	
	getStatusCode : function() {
		// Hopefully, you get an "OK"
		return this.statusWord
	},

	getStatusMessage : function() {
		// if isSuccess() == false then this will return something useful
		return this.statusMessage;
	},
	
	getSparqlQuery : function() {
		
		return "SparqlServerResult.getSparqlQuery() is not implemented.";
	},
	
	getColumnNames : function() {
		// get an array of column names of the results
		
		if (this.hasOwnProperty('jsonObj') && this.jsonObj.hasOwnProperty('head') && this.jsonObj.head.hasOwnProperty('vars')) {
			return this.jsonObj.head.vars;
		} else {
			return null;
		}
	},
	
	getColumnName : function(colNum) {
		// get an array of column names of the results
		if (this.hasOwnProperty('jsonObj') && this.jsonObj.hasOwnProperty('head') && this.jsonObj.head.hasOwnProperty('vars') && 
			colNum > -1 && colNum < this.getColumnCount()) {
			
			return this.jsonObj.head.vars[colNum];

		} else {
			return null;
		}
	},
	
	getColumnByName : function(colName) {
		// get column number given a name
		if (this.hasOwnProperty('jsonObj') && this.jsonObj.hasOwnProperty('head') && this.jsonObj.head.hasOwnProperty('vars')) {
			for (var i=0; i < this.jsonObj.head.vars.length; i++) {
				if (this.jsonObj.head.vars[i] === colName) {
					return i;
				}
			}
		}
		return -1;
	},
	
	getColumnAsList : function(colName) {
		// get all the values in a named column as a list
		var c = this.getColumnByName(colName);
		var rowCount = this.getRowCount();
		var ret = [];
		
		for (var i=0; i < rowCount; i++) {
			ret.push( this.getRsData(i, c, this.NAMESPACE_YES) );
		}
		return ret;
	},
	
	getRowCount : function() {
		
		if (this.hasOwnProperty('jsonObj') && this.jsonObj.hasOwnProperty('results') && this.jsonObj.results.hasOwnProperty('bindings')) {
			return this.jsonObj.results.bindings.length;
		} else {
			return null;
		}
	},
	
	getColumnCount : function() {
		if (this.hasOwnProperty('jsonObj') && this.jsonObj.hasOwnProperty('head') && this.jsonObj.head.hasOwnProperty('vars')) {
			return this.jsonObj.head.vars.length;
		} else {
			return null;
		}
	},
	
	getRow : function(rowNum) {
		// get an array representing one column results
		if (this.jsonObj == null || rowNum < 0 || rowNum >= this.getRowCount()) {
			return null;
			
		} else {
			//NOT IMPLEMENTED
			return null;
		}
	},
	
	getResultData : function() {
		// get an array of rows
		//NOT IMPLEMENTED
		return null;
	},
	
	getRsData : function(rowNum, col, optNamespaceFlag) {
		// get a value STRING by row and column.  
		//             null if it doesn't exist.
		//
		// col - can be a column number or a column name   Names are more efficient.
		//
		// optNamespaceFlag is optional (default YES) tells whether to include namespaces
		//       this flag is simple and dumb, it just splits a string by "#" and considers it to be namespace#value
		// TODO should pass in OntologyInfo object and check whether strings are classes/ranges, and use that to strip namespace
		//
		// NOTE: original simple function backwards-compatible.   Used heavily.
		var nsFlag = (typeof optNamespaceFlag == 'undefined') ? this.NAMESPACE_YES : optNamespaceFlag;
		var val;
		
		// get the value
		try {
			if (isNaN(parseFloat(col))) {
				val = this.jsonObj.results.bindings[rowNum][col];
			} else {
				val = this.jsonObj.results.bindings[rowNum][this.getColumnName(col)];
			}
		} catch(err) {
			return null;
		};
		
		/* PEC Removed 10-29-2015
		 * This seems very dangerous.  It changes "0" into "", for example.
		 * https://dorey.github.io/JavaScript-Equality-Table/
		if (!val) {       
			return "";
			
		} else 
		*/
		// use this instead
		if (val == undefined) { return "";}

		if (nsFlag == this.NAMESPACE_YES) {
			return val.value;
		
		} else {
			if (val.type == "uri") {
					return val.value.split('#')[1];
			} else {
				return val.value; 
			}
		}
	},
	
	getRsValue : function(rowNum, col, optNamespaceFlag) {
	    // Get correctly typed value
		// col can be number (inefficient) or column name (efficient)
		//
		// All datatypes:
		//      (ret == null) - value is missing, or it is actually null
		//
		// Numbers:   
		//      isNaN(ret) - value is not a number
		//         Note that integers are silently truncated but how could that ever happen?
		//         Note: All numbers are parsed with javascript parseInt or parseFloat,
		//                so extra characters are ignored, but how could that ever happen?
		var nsFlag = (typeof optNamespaceFlag == 'undefined') ? this.NAMESPACE_YES : optNamespaceFlag;
		var val;
		
		// get the value
		try {
			if (isNaN(parseFloat(col))) {
				val = this.jsonObj.results.bindings[rowNum][col];
			} else {
				val = this.jsonObj.results.bindings[rowNum][this.getColumnName(col)];
			}
		} catch(err) {
			return null;
		};
		
		if (val == undefined) { return null;}
		
		// get type and datatype
		var type = val.type ? val.type : "";
		type = (type.indexOf("#") > -1) ? type.split("#")[1] : type;
		var datatype = val.datatype ? val.datatype : "";
		datatype = (datatype.indexOf("#") > -1) ? datatype.split("#")[1] : datatype;
		
		// Goal: eventually finish this to handle all data types in:
		// http://www.w3.org/TR/xmlschema-2/#built-in-datatypes
		
		if (type == "uri") {
			if (nsFlag == this.NAMESPACE_YES) {
				return val.value;
			} else {
				return val.value.split('#')[1];
			}
		
			// ---- numbers are lumped together until there's a reason to care ----
		} else if (datatype == "integer" || datatype == "int") {
			
			// silently truncates at decimal because this should never happen
			// or NaN
			return parseInt(Number(val.value));
			
		// ---- numbers are lumped together until there's a reason to care ----
		} else if (datatype == "decimal" || datatype == "float" || datatype == "double") {
			
			// return the number or NaN
			return parseFloat(Number(val.value));
		
		// ---- everything else ----
		} else {
			return val.value;
		}
	},
	
	// force column to have a particular sType in the datagrid (actually a datatable)
	// This overrides this.getColumnSType()
	// sType may be a made up new type name, and you can then apply a custom sort.
	setColumnSType : function (colName, sType) {
		this.sTypeHash[colName] = sType;
	},
	
	// set datagrid (actually a datatable) sorting functions for an sType
	setSTypeSortFunctions : function (sType, sortAscend, sortDescend) {
		this.sortAscendHash[sType] = sortAscend;
		this.sortDescendHash[sType] = sortDescend;
	},
	
	sort : function(colName) {
		if (this.hasOwnProperty('jsonObj') && this.jsonObj.hasOwnProperty('results') && this.jsonObj.results.hasOwnProperty('bindings')) {

			this.jsonObj.results.bindings = this.jsonObj.results.bindings.sort(function(a,b) {
				// a row with only an optional value that is "null" comes back from virtuoso as a totally empty row
				try {
				    if ( a[colName].value < b[colName].value )
				        return -1; 
				    if ( a[colName].value > b[colName].value ) 
				        return 1;  
				    return 0;
				} catch(err) {
					return 1;
				}
			} ); 
		}
	}, 
	
	transformColumn : function (colName, callback) {
		// for every value in given column
		// replace the value with callback(value)
		
		var rowLen =  this.jsonObj.results.bindings.length;
		for (var i=0; i < rowLen; i++) {
			// If this row has this colName, then transform
			if (this.jsonObj.results.bindings[i].hasOwnProperty(colName)) {
				this.jsonObj.results.bindings[i][colName].value = callback(this.jsonObj.results.bindings[i][colName].value);
			}
		}
	},
	
	
	addColumn : function (colName, colType, dataType, callback) {
		// colType : for jquery data grid
		// dataType : wc3 data type for sorting and other operations.  only text after # is actually used.
		// optSorter : sort function
		// add a column of given type to the results set
		// populate values by calling callback(res, rowNum)
		// callback should be prepared that all column fields are optional
		//     and returning null will result in the new column value missing for that row.
		
		this.jsonObj.head.vars.push(colName);
		
		var rowLen =  this.jsonObj.results.bindings.length;
		for (var i=0; i < rowLen; i++) {
			var x = {};
			x.value = callback(this, i);
			
			if (x.value !== null) {
				x.type = colType;
				x.datatype = dataType;
				
				this.jsonObj.results.bindings[i][colName] = x;
			}
		}
	},
	
	getResultsHTMLTable : function(attributes, optNamespaceFlag) {
		// return an HTML table containing results
		// optNamespaceFlag is optional (default YES) tells whether to include namespaces
		var nsFlag = optNamespaceFlag || this.NAMESPACE_YES;
		
		var rowCount = this.getRowCount();
		var colCount = this.getColumnCount();
		var resultStr = "";
		var val;
		
		resultStr += "<table " + attributes + ">";
		
		resultStr += "<tr>";
		for (var j=0; j < colCount; j++) {
			resultStr += "<th>" + this.getColumnName(j) + "</th>";
		}
		resultStr += "</tr>";
	
		for (var i=0; i < rowCount; i++) {
			resultStr += "<tr>";
			for (var j=0; j < colCount; j++) {
				// these results need to be htmlSafe
				val = this.getRsData(i,this.jsonObj.head.vars[j], nsFlag);
				resultStr += "<td>" + this.htmlSafe(val) + "</td>";
			}
			resultStr += "</tr>";
		}
		resultStr += "</table>";
		return resultStr;
	},
	
	getResultsCSV : function(optNamespaceFlag) {
		// return results as a long string that could be contents of a CSV file

		var nsFlag = (typeof optNamespaceFlag === 'undefined') ? this.NAMESPACE_YES : optNamespaceFlag;
		
		var rowCount = this.getRowCount();
		var colCount = this.getColumnCount();
		var resultStr = "";
		
		for (var j=0; j < colCount; j++) {
			resultStr += this.getColumnName(j);
			if (j + 1 < colCount)
				resultStr += ',';
		}
		resultStr += "\n";
	
		for (var i=0; i < rowCount; i++) {
			for (var j=0; j < colCount; j++) {
				// get data.  Note it is more efficient to get the column name here than to pass in j
				var strRaw = this.getRsData(i, this.jsonObj.head.vars[j], nsFlag);	
				
				// handle strings with commas embedded
			    if(strRaw.indexOf(",") >= 0 || strRaw.indexOf("\n") >= 0){  strRaw = '"' + strRaw + '"';}
				resultStr += strRaw;

				if (j + 1 < colCount)
					resultStr += ',';
			}
			resultStr += "\n";
		}
		return resultStr;
	},
	
	getColumnSType : function (colName) {
		// return jquery column sType of 'string', 'numeric', or 'date'
		// based on all of the columns .datatype fields which are https://www.w3.org primitive types
		//
		// If column contains multiple types or any other problem: return 'string'
		//
		// This turns out to be smarter than the datatable's default behavior.
		
		
		//handle override
		if (colName in this.sTypeHash) {
			return this.sTypeHash[colName];
		}
		
		try {
			var ret = null;
					
			for (var i=0; i < this.getRowCount(); i++) {
				// for rows that have a value for the target column
				if (this.jsonObj.results.bindings[i].hasOwnProperty(colName)) {
					var t = this.jsonObj.results.bindings[i][colName].datatype;
					t = t.slice(t.indexOf("#")+1, t.length);    // strip of anything up to "#" if there is a "#"
					
					// figure out the sType
					var st = 'string';
					
					if (t == "float" ||
						t == "double" ||
						t == "decimal" ||
						t == "integer") {
						st = 'numeric';
						
					} else if (t == "dateTime" || t == "date" || t == "time") {
						st = 'date';
					}
					
					// if first time, set sType.  Rest of times, make sure it didn't change.
					if (ret == null) { 
						ret = st;
					} else if (st !== ret) {
						return 'string';    // multiple types: return string
					}
				}
			}
			// if no type ever got set, return string, else return the type
			return (ret == null) ? 'string' : ret;
		}
		catch (e) {
			return 'string';  // error: return string
		}
		
	},
	
	getResultsDatagridAoColumns : function(filterFlag, optColHash) {
		// get column names in a format that datagrids like
		//    filterFlag is an iidx pass-through
		//    optColHash["col"]="display name" :  show only these cols
		
		var colHash = (typeof optColHash === 'undefined') ? null : optColHash;
		var ret = [];
		var colNames;
		var origNames;
		
		if (colHash == null) {
			// no hash: push all the column names
			colNames = this.getColumnNames();
			origNames = colNames.slice();
		} else {
			// if optColHash, then push the hash values as column names
			var legalColNames = this.getColumnNames();
			colNames = [];
			origNames = [];
			for (var colName in colHash) {
				if (legalColNames.indexOf(colName) > -1) {
					colNames.push(colHash[colName]);
					origNames.push(colName);
				}
			}
		}
		
		// build results
		for (var i=0; i < colNames.length; i++) {
			
			ret.push({sTitle: colNames[i], sType: this.getColumnSType(origNames[i]), filter: filterFlag });
			
		}
		return ret;
	},
	
	getResultsDatagridAaData : function(optSuperNsFlag, optColHash) {
		// get data in a format that datagrids like
		var nsFlag = (typeof optSuperNsFlag == 'undefined') ? this.NAMESPACE_NO : (optSuperNsFlag %  this.ESCAPE_HTML);
		var escapeHTMLFlag = (typeof optSuperNsFlag == 'undefined') ? false : (optSuperNsFlag >= this.ESCAPE_HTML);
		var colHash = (typeof optColHash === 'undefined') ? null : optColHash;
		var ret = [];
		var rowCount = this.getRowCount();
		var colNames = null;
		
		// get columns
		if (colHash == null) {
			// no hash: push all the column names
			colNames = this.getColumnNames();
		} else {
			// if optColHash, then push the hash values as column names
			var legalColNames = this.getColumnNames();
			colNames = [];
			for (var colName in colHash) {
				if (legalColNames.indexOf(colName) > -1) {
					colNames.push(colName);
				}
			}
		}
		
		// loop through rows and cols
		for (var i=0; i < rowCount; i++) {
			var row = [];
			var val;
			
			// no optColHash: push all columns
			for (var j=0; j < colNames.length; j++) {
				val = this.getRsValue(i, colNames[j], nsFlag);
				if (escapeHTMLFlag) {
					val = this.htmlSafe(val);
				}
				row.push(val);
			}
			
			ret.push(row);
		}
		
		return ret;
	},
	
	htmlSafe : function(str) {
	    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
	},
	
    putSparqlResultsDatagridInDiv : function (div, optFinishedCallback, nsFlag) {
        var myUnique = Math.floor(Math.random() * 100000).toString();
        this.getResultsInDatagridDiv(div, 
                                     "ssiDataTableName"+myUnique,
                                     "ssiDataTableId" + myUnique,
                                     "table table-bordered table-condensed",
                                     this.getRowCount() > 10,
                                     null,
                                     null,
                                     optFinishedCallback,
                                     nsFlag
                                    );
    },
    
    // Really old function with backwards compatibility
    // to some projects we don't know about
	getResultsInDatagridDiv : function(divElement, dataTableName, tableId, tableClass, gridFilterFlag, DEPRECATED, optColHash, optCallback, optSuperNsFlag) {
		// put the results into a table and put the table into a divElement.
		// make the whole thing IIDX datagrid-friendly
		//
		// PARAMS:
		//  divElement - an empty html div element where the table can be inserted
		//  dataTableName and tableId just need to be unique for this HTML
		//  optColHash is an optional list of column headers specifying the subset and order to display the columns.  optColHash["colName"] = "display name"
		//
		// NOTE:  needs require.js with IIDX dependencies in the require line down below to be defined
		
		// build callback that will download the table
		var nsFlag =      (typeof optSuperNsFlag == 'undefined') ? this.NAMESPACE_NO : (optSuperNsFlag %  this.ESCAPE_HTML);
		var superNsFlag = (typeof optSuperNsFlag == 'undefined') ? this.NAMESPACE_NO : (optSuperNsFlag);
		var callback = (typeof optCallback == 'undefined' || optCallback == null) ? function(){} : optCallback;
		
        var downloadCallback = function() {
            this.downloadFile(this.getResultsCSV(nsFlag), "table.csv");
        }.bind(this);

		var searchHTML = '<input type="text" id="table_filter" class="input-medium search-query" data-filter-table="' + dataTableName + '"><button class="btn btn-icon"><i class="icon-search"></i></button>';
		// build a button
		var buttonHTML = 	'<div class="btn-group" align="right" style="width:100%">' +
							'  <button class="btn dropdown-toggle" data-toggle="dropdown" id="btnDownloadCSV"><i class="icon-chevron-down"></i></button>' +
							'  <ul class="dropdown-menu pull-right">' +
							'    <li><a id="ssiDownloadCsvAnchor">Save table csv</a></li>' +
							'  </ul>' +
							'</div>';
		
		// build an empty table
		var tableHTML = "<table id='" + tableId + "' class='" + tableClass + "' data-table-name='" + dataTableName + "'></table>";

		// put them in the div
		var html = "";
		html = '<table align="right"><tr>';    // '<div align="right"><form class="form-inline"> ';
		html += '<td>' + searchHTML + '</td>';
		html += '<td>' + buttonHTML+ '</td>';
		html += "</tr></table><br><br>";
		html += tableHTML;
		divElement.innerHTML = html;
        document.getElementById("ssiDownloadCsvAnchor").onclick=downloadCallback;
		
		// define a variable since 'this' isn't legal inside the require js function / function 
		var tableStr = 'table[data-table-name="' + dataTableName + '"]';
		// add the data to the table and set it up as a datagrid
		var me = this;  // work-around require scoping
		
		
		var sortNumAsc = function(a,b) {}
		require( ['jquery', 'datagrids', 'col-reorder-amd'], function($) {
			
			// fix numeric sorts so 
			//   (1) ints and floats co-exist:  parseFloat handles ints, floats, strings
			//   (2) numbers and blanks co-exist  (zero > NaN)
			$.fn.dataTableExt.oSort['numeric-asc'] = function(a,b) {
				var x = parseFloat(a);   
				var y = parseFloat(b);
				return ((isNaN(x) || x < y) ? -1 : ((isNaN(y) || x > y) ? 1 : 0));
			};
			$.fn.dataTableExt.oSort['numeric-desc'] = function(a,b) {
				var x = parseFloat(a);
				var y = parseFloat(b);
				return ((isNaN(x) || x < y) ? 1 : ((isNaN(y) || x > y) ? -1 : 0));
			};

			// apply any special sorts
			for (var stype in me.sortAscendHash) {
				$.fn.dataTableExt.oSort[stype + '-asc'] = me.sortAscendHash[stype];
			}
			for (var stype in me.sortDescendHash) {
				$.fn.dataTableExt.oSort[stype + '-desc'] = me.sortDescendHash[stype];
			}

			// go
			$(function() {
				$(tableStr).iidsBasicDataGrid({
					'aoColumns':   me.getResultsDatagridAoColumns(gridFilterFlag, optColHash),
					'aaData':      me.getResultsDatagridAaData(superNsFlag, optColHash),
					//'sAjaxSource': '../assets/data/ao.browsers.json',
					'plugins': ['R'], //enable the col-reorder plugin (assumes 'col-reorder-amd' is on the page)
					'useFloater': false,
					'isResponsive': true
				});
				// avoid flash of unstyled content
				$(tableStr).css( { visibility: 'visible' } );
				callback();
			});
		});
	},
	
    downloadFile : function (data, filename) {
    	// A generic function to have the browser download a long string as file contents

    	// build an anchor and click on it
		$('<a>invisible</a>')
			.attr('id','downloadFile')
			.attr('href','data:text/csv;charset=utf8,' + encodeURIComponent(data))
			.attr('download', filename)
			.appendTo('body');
		$('#downloadFile').ready(function() {
			$('#downloadFile').get(0).click();
		});
		
		// remove the evidence
		var parent = document.getElementsByTagName("body")[0];
		var child = document.getElementById("downloadFile");
		parent.removeChild(child);
    },
	
	getRowsJSON : function(optNamespaceFlag) {
		// create a simple JSON object 
		// with one field called "rows" which is an array of sets of row name:val pairs
		// {"rows":[{"col1":"val", "col2":"val"},{"col1":"val", "col2":"val"},...]}
		
		var nsFlag = optNamespaceFlag || this.NAMESPACE_YES;
		
		var rowCount = this.getRowCount();
		var colCount = this.getColumnCount();
		var resultStr = "";
		
		resultStr += '{"rows":[';
	
		for (var i=0; i < rowCount; i++) {
			resultStr += "\n{";
			for (var j=0; j < colCount; j++) {
				resultStr += '"' + this.getColumnName(j) + '":';
				resultStr += '"' + this.getRsData(i,j, nsFlag) + '"';
				if (j + 1 < colCount)
					resultStr += ',';
			}
			resultStr += "}";
			if (i + 1 < rowCount)
				resultStr += ',';
		}
		resultStr += ']}';
		return JSON.parse(resultStr);
	},

	countColUniqueVals : function(colName) {
		// count number of unique values in a named column
		// -1 if colName is bad
		//  0 if there are no rows
		var col = this.getColumnNames().indexOf(colName);
		if (col < 0) {
			return -1;
		}
		var unique = [];
		var rowCount = this.getRowCount();
		for (var i=0; i < rowCount; i++) {
			var val = this.getRsData(i, col, this.NAMESPACE_YES);
			if (unique.indexOf(val) == -1) {
				unique.push(val);
			}
		}
		return unique.length;
	},
};

function SparqlServerGraphResult(resultJSON, statusWord, statusMessage) {
	//
	this.jsonObj = resultJSON;
	this.statusWord = statusWord;
	this.statusMessage = statusMessage;
	
};

SparqlServerGraphResult.prototype = {

	
	isSuccess : function() {
		return this.statusWord == "success";
	},
	
	getStatusCode : function() {
		// Hopefully, you get an "OK"
		return this.statusWord
	},

	getStatusMessage : function() {
		// if isSuccess() == false then this will return something useful
		return this.statusMessage;
	},
	
	getSparqlQuery : function() {
		
		return "SparqlServerResult.getSparqlQuery() is not implemented.";
	},
	
	getAtGraph : function () {
		if (this.hasOwnProperty('jsonObj') && this.jsonObj.hasOwnProperty('@graph')) {
			return this.jsonObj["@graph"];
			
		} else {
			return null;
		}
	},
};
	