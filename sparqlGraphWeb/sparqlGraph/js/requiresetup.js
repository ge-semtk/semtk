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

// SAMPLE USAGE:
//
//	<script type="text/javascript" src="../sparqlGraph/js/requiresetup.js"></script>   <-- find this file relative to your app
//
//	<script>
//	    requireConfigSparqlgraph(
//			"../sparqlGraph",                                <-- path of sparqlGraph relative to the baseURL
//
//			{                                                <--------- standard require.js config object ----------
//				baseUrl : '../iidx-2.0.3',                   <-- base URL for your require (usually iidx relative to your project)
//				paths : {
//					"somecode" : "../somecode",          <-- anything else you need (relative to baseUrl)
//			    }
//	        });
//	</script>

var requireConfigSparqlgraph = function(pathRelativeToBase, config) {
	// attach needed paths and shims to sparqlgraph
	var p = pathRelativeToBase;

	if (! config.hasOwnProperty("paths")) { config.paths = {}; }
	if (! config.hasOwnProperty("shim")) { config.shim = {}; }

	config.paths["jsonp"] = p + "/jquery/jquery.jsonp-2.3.0.min";
	config.paths["sparqlgraph"] = p;

	// shims

	config.shim['ge-bootstrap'] = {
			deps : [ 'jquery' ]
	}
	config.shim['bootstrap/bootstrap-modal'] = {
			deps : [ 'jquery' ]
	}
	config.shim['bootstrap/bootstrap-datepicker'] = {
			deps : [ 'jquery' ]
	}
	config.shim['bootstrap/bootstrap-tooltip'] = {
			deps : [ 'jquery' ]
	}
	config.shim['bootstrap/bootstrap-transition'] = {
			deps : [ 'jquery' ]
	}
	config.shim['bootstrap/bootstrap-popover'] = {
			deps : [ 'bootstrap/bootstrap-tooltip',
			         'jquery' ]
	}
	//
	config.shim['jsonp'] =  {
			deps : [ 'jquery' ]
		};
	config.shim['sparqlgraph/dynatree-1.2.5/jquery.dynatree'] =  {
			deps : [ 'sparqlgraph/jquery/jquery-ui-1.10.4.min',
					'sparqlgraph/jquery/jquery.cookie' ]
		};
	config.shim['sparqlgraph/jquery/jquery-ui-1.10.4.min'] =  {
			deps : [ 'bootstrap/bootstrap-popover' ]
		};



    // special logic to include sparql generation for legacy
    // set gVERSION to 2 or more before requiresetup.js is included to stop this.
    //if (typeof gVERSION == "undefined" || gVERSION < 2) {
    //    config.shim['sparqlgraph/js/belmont'].deps.push('sparqlgraph/js/belmont_v1');
    //};

	config.shim['sparqlgraph/js/htmlform'] = {
		deps: [
		          'sparqlgraph/js/sparqlconnection',
		          'sparqlgraph/js/ontologyinfo',
		          'sparqlgraph/js/belmont',
		      ]
		};
	config.shim['sparqlgraph/js/sparqlconnection'] =  {
			deps : [ 'sparqlgraph/js/sparqlserverinterface' ],
			exports: 'SparqlConnection',
		};
	config.shim['sparqlgraph/js/sparqlserverinterface'] =  {
			deps : [ 'jquery', 'jsonp' ]
		};
	config.shim['sparqlgraph/js/ontologyinfo'] = {
			deps : [ 'sparqlgraph/js/sparqlconnection',
			         'sparqlgraph/js/msiclientquery',
			       ],
			exports: 'OntologyInfo',
		};
    config.shim['sparqlgraph/js/nodegrouprenderer'] = {
			deps : [ 'sparqlgraph/js/cookiemanager'
			       ]
		};
	config.shim['sparqlgraph/js/sparqlgraph'] = {
			deps : [ 'sparqlgraph/js/cookiemanager'
			       ]
		};

	config.waitSeconds = 45;

	require.config(config);

	requirejs.onError = function (err) {
	    if (err.requireType === 'timeout') {
	        alert("Require.js page loading time-out. (Try reloading the page)  \nError: "+err);
	    }
	    else {
	    	console.log(err.stack);
	        throw err;
	    }
	};
};
