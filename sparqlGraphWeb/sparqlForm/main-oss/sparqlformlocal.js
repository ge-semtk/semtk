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

// This file is not a typical javascript object, but a script
// that needs to be loaded by the HTML.
// Its other half must also be loaded by the HTML:  sparqlform.js

var gQueryClient = null;
var gAvoidQueryMicroserviceFlag = false;
var gLoadDialog = null;
var gQueryResults = null;

require([	'local/sparqlformconfig',
         	
         	'sparqlgraph/js/msiclientquery',
         	'sparqlgraph/js/msiresultset',
         	'sparqlgraph/js/modaliidx',
         	
         	'jquery',
		
			// rest are shimmed
			'sparqlgraph/js/sparqlconnection', 
			'sparqlgraph/js/belmont',
			'sparqlgraph/js/cookiemanager',
			'sparqlgraph/js/ontologyinfo',
			'sparqlgraph/js/ontologytree',
			'sparqlgraph/js/modalconstraintdialog',
			'sparqlgraph/js/modaldialog',
			'sparqlgraph/js/modalloaddialog',

		],

	function(Config, MsiClientQuery, MsiResultSet, ModalIidx, $) {
	

		//----- e d c ------/
		getOntTriggerClassNames = function() {
			return [];
		};
		
		isOntTriggerClassName = function(uri) {
			return false;
		};
		
		removeTrigger = function(nodegroup) {
		};
		
		//----- Constraints -----/
		formConstraintsNew = function() {
			gFormConstraint = null;
		};
		
		formConstraintsInit = function() {
		};
		
		formConstraintsUpdateTable = function() {
		};

		formConstraintsValidate = function () {
			return 0;
		};
		
		formConstraintsGet = function () {
			return null;
		};
		
		formConstraintJson = function () {
			return {};
		};
		
		formConstraintSetFromJson = function(json) {
		};
		
		//----- Constraint (as in Belmont Constraint) dialog -----//
		
		launchConstraintDialog = function(item, sparql, textId, failureCallback) {
			
			var clientOrInterface;
			
			if (gAvoidQueryMicroserviceFlag) {
				clientOrInterface = gConn.getDataInterface();
			}
			else {
				clientOrInterface = new MsiClientQuery(Config.services.query.url, gConn.getDataInterface(), failureCallback);
			}
			
			gConstraintDialog.constraintDialog(item, sparql, clientOrInterface, filterDialogCallback.bind(window, textId, item), true);
		}
		
		doAbout = function() {
			ModalIidx.alert("About SparqlForm", Config.help.aboutHtml + "<br>" + Config.help.legalNoticeHtml);
		};
		
		doTest = function() {
			ModalIidx.alert("Testing", "No tests to run.");
		};
		
		//----- Query -----/
		
		doQuery = function() {
			// User hit the "run" button
			setStatusProgressBar("Running query", 0);

			kdlLogEvent("SF: Query");
			
			var query = gNodeGroup.generateSparql(SemanticNodeGroup.QUERY_DISTINCT, false, 0);
			if (gAvoidQueryMicroserviceFlag) {
				/* Old non-microservice code */
				gConn.getDataInterface().executeAndParse(query, doQueryCallback);
		    	
			} else {
				gQueryClient.executeAndParse(query, doQueryCallback);
			}
			
			setStatusProgressBar("Running query", 50);
		};
 
		
		doQueryCallback = function (results) {
			setStatusProgressBar("Running query", 100);

			if (results.isSuccess()) {
				var gridDiv = document.getElementById("gridDiv");
				var localUriFlag = true;
				
				if (gAvoidQueryMicroserviceFlag) {
					/* old non-microservice */
					results.getResultsInDatagridDiv(	gridDiv, 
														"resultsTableName",
														"resultsTableId",
														"table table-bordered table-condensed", 
														results.getRowCount() > 10, // filter flag
														"gQueryResults",
														null,
														null,
														localUriFlag);
				} 
				else {
				    // query microservice results grid functionality
					results.setLocalUriFlag(localUriFlag);
					results.setEscapeHtmlFlag(true);
					results.putTableResultsDatagridInDiv(gridDiv, "");
				}
				gQueryResults = results;
				guiEndQuery();
			} else {
				guiEndQuery(results.getStatusMessage());
			}
		};
		
		//-----  query directly -----//

		var AQM_COOKIE = "avoidMs";
		doAvoidQueryMicroservice = function(flag) {
			gAvoidQueryMicroserviceFlag = flag;
			var cookies = new CookieManager(document);
			cookies.setCookie(AQM_COOKIE, gAvoidQueryMicroserviceFlag ? "t" : "f");
		};
		
		initAvoidQueryMicroservice = function() {
			var cookies = new CookieManager(document);
			var flag = cookies.getCookie(AQM_COOKIE);
			gAvoidQueryMicroserviceFlag = flag && flag == "t";
			document.getElementById("chkboxAvoidMicroSvc").checked = gAvoidQueryMicroserviceFlag;
		};
		
		//-----  on load async callback chain functions -----//
		doFileLoad = function() {
			gLoadDialog.loadDialog(gConn, loadSuccess0);
		};
		
		loadSuccess0 = function(connProfile, optCallback) {
	    	// Callback from the load dialog
	    	var callback = (typeof optCallback === "undefined") ? function(){} : optCallback;
	    		
	    	// Clean out existing GUI    		
    		initForm(); 
			clearResults();
			enableButton('btnFormExecute'); // browser sometimes caches this disabled if user reloaded while a query is running.  wow.
			gConstraintDialog = new ModalConstraintDialog(document, "gConstraintDialog");
			gOInfo = new OntologyInfo();
    		
	    	// Get connection info from dialog return value
	    	gConn = connProfile;   // instead of gConnSetup()
	    	gQueryClient =       new MsiClientQuery(Config.services.query.url, gConn.getDataInterface());
	    	var ontQueryClient = new MsiClientQuery(Config.services.query.url, gConn.getOntologyInterface(), alertUser);
	    	
	    	kdlLogEvent("SF Loading", "connection", gConn.toString());
    		
	    	
    		if (gAvoidQueryMicroserviceFlag) {
    			gOInfo.loadAsync(gConn, setStatus, function(){ loadSuccess2(); callback();}, loadFailure);
    			
    		} else {
		    	// load ontology via microservice
				gOInfo.load(gConn.getDomain(), ontQueryClient, setStatus, function(){ loadSuccess2(); callback();}, loadFailure);
    		}
	  
	    };
	
		loadSuccess2 = function() {
			document.getElementById("titleSpan").innerHTML = "SparqlForm: "
				+ gConn.name
				+ "<br><small><i>powered by GRC Knowledge Discovery Lab</i></small>";
			
			kdlLogEvent("SF: Load Success", "endpoint", gConn.name);
			loadDone();
		};
	
		loadFailure = function(msg) {
			alertUser("failure: " + msg);
			gOInfo = new OntologyInfo();
			kdlLogEvent("SF: Load Failure", "endpoint", gConn.name);
			loadDone();
		};
		
		loadDone = function() {
			// after succesfully loading the ontology
			setStatus("");
			initNodeGroup();
			initOTree();
		};
		
		logAndAlert = function (msgHtml, optTitle) {
	    	var title = typeof optTitle === "undefined" ? "Alert" : optTitle
	    	kdlLogEvent("SF: alert", "message", msgHtml);
	    	ModalIidx.alert(title, msgHtml);
	    };
	
		// ****   Start on load proceedure ****
	
		onLoad = function () {
			// check for firefox
			var is_firefox = navigator.userAgent.toLowerCase().indexOf('firefox') > -1;
			if (!is_firefox) {
				alertUser("Only FireFox is currently supported.");
				return;
			}
			
			initAvoidQueryMicroservice();
			
			// globals
			gModalDialog = new ModalDialog(document, "gModalDialog");
			gLoadDialog = new ModalLoadDialog(document, "gLoadDialog");
	
			// load last connection
			var conn = gLoadDialog.getLastConnectionInvisibly();
			if (conn) {
				loadSuccess0(conn);
			}
			
			// PEC TODO: load cookie or launch dialog
			setStatus("");
		};

	}

);