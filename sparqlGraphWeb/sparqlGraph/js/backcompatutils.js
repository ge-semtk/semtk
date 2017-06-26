
/**
 ** Copyright 2016 General Electric Company
 **
 ** Authors:  Paul E Cuddihy, Justin McHugh
 ** Test change 6/26/2017 9:56am EDT
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
 * BackwardCompatibleUtils
 * 
 * PROBLEM:  legacy javascript includes many original files without require.js enabled.
 *           New functionality needs some of these files to know about each other.
 *           There is no way to enforce this without breaking backwards compatibility.
 *           
 * SOLUTION: this file contains static functions that SHOULD be in another file
 *           if that file were require.js enabled.   Call if from somewhere that IS require.js enabled.
 *           
 *           require(['sparqlgraph/js/backcompatutils'
 *   	         ], function (BackCompatUtil) {
 *   
 * 			          BackCompatUtil.test();
 *  		 });
 *  
 *           Each function should be commented with its actual correct location.  
 *           On the Java side, it should live there.
 */
define([// properly require.config'ed
        'sparqlgraph/js/msiclientquery',
        'jquery',
         	
		// shimmed
		'sparqlgraph/js/ontologyinfo',	
		],

	function (MsiClientQuery, $) {
	
		//============ main object  ImportSpec =============
		var BackwardCompatibleUtil = function () {
		};
		
		BackwardCompatibleUtil.prototype = {
			
			test : function () {
				alert("worked");
			},
			
			/**
			 * if not for javascript backwards compatibility, this is an   >> OntologyInfo <<  function

			 * Load a SparqlConnection (with potentially multiple models) into an OntologyInfo
			 * 
			 * queryServiceUrl: if not null or "", then using MsiClientQuery() microservice client.
			 * 
			 * Presumes this is a new OntologyInfo().  (empty)
			 * 
			 */
			loadSparqlConnection : function(oInfo, conn, queryServiceUrl, statusCallback, successCallback, failureCallback, optRecursionIndex) {
		    	
		    	
		    	// first (non-private) call
		    	if (typeof optRecursionIndex == "undefined") {
		  
		    		this.loadSparqlConnection(oInfo, conn, queryServiceUrl, statusCallback, successCallback, failureCallback, 0);
		    	
		    	// break recursion
		    	} else if (optRecursionIndex >= conn.getModelInterfaceCount()) {
		    		successCallback();
		    	
		    	// normal recursive call of next model
		    	} else {
		    		var i = optRecursionIndex;
		    		var queryClientOrSei = null;
		    		
		    		if (queryServiceUrl == null || queryServiceUrl == "") {
		    			queryClientOrSei = conn.getModelInterface(i);
		    		} else {
		    			queryClientOrSei = new MsiClientQuery(queryServiceUrl, conn.getModelInterface(i));
		    		}
		    		
		    		// load model i.   On success, call me again with next model number i+1
		    		oInfo.load(	conn.getDomain(), 
		    					queryClientOrSei, 
		    					statusCallback, 
		    					this.loadSparqlConnection.bind(this, oInfo, conn, queryServiceUrl, statusCallback, successCallback, failureCallback, i+1), 
		    					failureCallback
		    				  	);	
		    	}
		    },
        };
		
		// return an instance, NOT the class.
		var util = new BackwardCompatibleUtil();
        return util;
	}
);