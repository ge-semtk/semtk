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
 *  Paul Cuddihy  (Justin McHugh, Ravi Palla)
 *  Knowledge Discovery Lab.   GE Research Niskayuna.
 *  (c) 2014
 *  
 *  Call the SADL Query Server and execute SPARQL
 *  
 *  SAMPLE CODE:
 *  See queryserverinterface.html
 *
 */


/*
 * QueryServerInterface
 */
function QueryServerInterface(serverURL, ksURL, service) {
	// give base URL for the queryserver and the "serviceName" parameter.
	this.serverURL = serverURL;
	this.ksURL = ksURL;
	this.service = service;
};

QueryServerInterface.prototype = {
	equals : function (other) {
		return(other.serverURL = this.serverURL && other.ksURL == this.ksURL && other.service == this.service);
	},	
	
	executeQuery : function(sparql, callback) {
		// SIMPLEST: execute some SPARQL and then callback(responseText)
		// This is really PRIVATE, but you can call it as a light-weight alternative if you'd like.
		this.results = null;
		this.post_to_url(this.serverURL + '/execute-query', 
						{   ksUrlId: '33',
							ksUrl: this.ksURL,
							serviceName: this.service,
							file: '',
							formatType: 'RDF/XML',
							csvfile: '',
							csvHeaders: '1',
							csvTemplate: '-',
							savedQueryId: '-',
							queryType: 'Sparql',
							query: sparql,
							queryName: '',             
							showNamespaces: '1'        // always get namespaces.  QueryServerResult knows how to strip them.
						},
						callback);
	},
	
	executeAndParse : function(sparql, callbackQSResult) {
		// Execute a query, then parse and hold on to the results.  Returns boolean true if successful.
		// Then you may call all the "get" methods below.
		
		var localCallback = function(resultTxt) {
			callbackQSResult(new QueryServerResult(resultTxt));
		};
		
		this.executeQuery(sparql, localCallback);
	},
	
	post_to_url : function(url, params, callbackText) {
		// PRIVATE
		var xhr = new XMLHttpRequest();
		
		xhr.onload = function() {
			callbackText(xhr.responseText);
		};
		var boundary='01-B0UndARY-92';
		
		
		var sendit = '--' + boundary;
		var newline = '\r\n';
	
		for (var key in params) {
	
			sendit = sendit + newline;
			sendit = sendit + 'Content-Disposition: form-data; name="' + key + '"' + newline + newline;
			sendit = sendit + params[key] + newline;
			sendit = sendit + '--' + boundary ;
		}
		sendit = sendit + '--' + newline;
		
		xhr.open("POST", url, true);
		xhr.setRequestHeader("Content-Type","multipart/form-data; boundary="+boundary);
		xhr.setRequestHeader("Accept","application/json, text/javascript, */*; q=0.01");
		xhr.setRequestHeader("X-Requested-Witht","XMLHttpRequest");
		xhr.send(sendit);
	},
};
		   
/*
 * QueryServerResult
 */

function QueryServerResult(resultTxt) {
	// give base URL for the queryserver and the "serviceName" parameter.
	if (typeof(resultTxt) ==='string') {
		this.jsonObj = JSON.parse(resultTxt);
	} else {
		alert(typeof(resultTxt));
	};
};

QueryServerResult.prototype = {
	NAMESPACE_YES : 1,
	NAMESPACE_NO : 2,
	NAMESPACE_ONLY : 3,
	
	isSuccess : function() {
		return (!(this.jsonObj.statusCode < "OK") && !(this.jsonObj.statusCode > "OK"));
	},
	
	getStatusCode : function() {
		// Hopefully, you get an "OK"
		if (this.jsonObj == null) {
			return null;
			
		} else {
			return this.jsonObj.statusCode;
		}
	},

	getStatusMessage : function() {
		// if getStatusCode() is not "OK", then you'll want to see why.
		if (this.jsonObj == null) {
			return null;
			
		} else {
			return this.jsonObj.statusMessage;
		}
	},
	
	getSparqlQuery : function() {
		// Not sure why you need this.  Just here for the sake of completion
		if (this.jsonObj == null) {
			return null;
			
		} else {
			return this.jsonObj.sparqlQuery;
		}
	},
	
	getColumnNames : function() {
		// get an array of column names of the results
		if (this.jsonObj == null) {
			return null;
			
		} else {
			return this.jsonObj.resultSet.columnNames;
		}
	},
	
	getColumnName : function(colNum) {
		// get an array of column names of the results
		if (this.jsonObj == null || colNum < 0 || colNum >= this.jsonObj.resultSet.columnCount) {
			return null;
			
		} else {
			return this.jsonObj.resultSet.columnNames[colNum];
		}
	},
	
	getRowCount : function() {
		// get number of rows in results
		if (this.jsonObj == null) {
			return null;
			
		} else {
			return this.jsonObj.resultSet.rowCount;
		}
	},
	
	getColumnCount : function() {
		// get number of columns in results
		if (this.jsonObj == null) {
			return null;
			
		} else {
			return this.jsonObj.resultSet.columnCount;
		}
	},
	
	getRow : function(rowNum) {
		// get an array representing one column results
		if (this.jsonObj == null || rowNum < 0 || rowNum >= this.jsonObj.resultSet.rowCount) {
			return null;
			
		} else {
			return this.jsonObj.resultSet.data[rowNum];
		}
	},
	
	getResultData : function() {
		// get an array of rows
		return this.jsonObj.resultSet.data;
	},
	
	getRsData : function(rowNum, colNum, optNamespaceFlag) {
		// get a value by row and column.  null if either is out of bounds.
		// optNamespaceFlag is optional (default YES) tells whether to include namespaces
		//       this flag is simple and dumb, it just splits a string by "#" and considers it to be namespace#value
		// TODO should pass in OntologyInfo object and check whether strings are classes/ranges, and use that to strip namespace
		var nsFlag = optNamespaceFlag || this.NAMESPACE_YES;
		var val;
		
		// get the value
		if (this.jsonObj == null || rowNum < 0 || rowNum >= this.jsonObj.resultSet.rowCount || colNum < 0 || colNum >= this.jsonObj.resultSet.colCount) {
			return null;
		
		} else {
			val = this.jsonObj.resultSet.data[rowNum][colNum];
		}
		
		
		if (typeof val != "string") {
			return val;
		
		} else if (val.indexOf('#') == -1) {
			return val;
		
		} else if (nsFlag == this.NAMESPACE_YES) {
			return val;
		
		} else if (nsFlag == this.NAMESPACE_NO) {
			return val.split('#')[1];
			
		} else {    // NAMESPACE_ONLY
			return val.split('#')[0];
		}
	},
	
	getResultsHTMLTable : function(attributes, optNamespaceFlag) {
		// return an HTML table containing results
		// optNamespaceFlag is optional (default YES) tells whether to include namespaces
		var nsFlag = optNamespaceFlag || this.NAMESPACE_YES;
		
		var rowCount = this.getRowCount();
		var colCount = this.getColumnCount();
		var resultStr = "";
		
		resultStr += "<table " + attributes + ">";
		
		resultStr += "<tr>";
		for (var j=0; j < colCount; j++) {
			resultStr += "<th>" + this.getColumnName(j) + "</th>";
		}
		resultStr += "</tr>";
	
		for (var i=0; i < rowCount; i++) {
			resultStr += "<tr>";
			for (var j=0; j < colCount; j++) {
				resultStr += "<td>" + this.getRsData(i,j, nsFlag) + "</td>";
			}
			resultStr += "</tr>";
		}
		resultStr += "</table>";
		return resultStr;
	},
	
	getResultsCSV : function(optNamespaceFlag) {
		
		var nsFlag = optNamespaceFlag || this.NAMESPACE_YES;
		
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
				resultStr += this.getRsData(i,j, nsFlag);
				if (j + 1 < colCount)
					resultStr += ',';
			}
			resultStr += "\n";
		}
		return resultStr;
	},	
};

