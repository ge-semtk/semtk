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


/**
 * 
 *  Loads the semtk api and all its dependencies.
 *  Creates global: semtk
 * 
 *  Requires: that it is called from an html document with a <head> tag in it.
 *  
 *  Your html should look like this;
 *  
 *    <script>	
 *		doneLoading = function() {
 *          alert("doing whatever now that semtk is loaded");
 *          semtk.doWhatever();
 *      }
 *      
 *    </script>
 *    
 *    <script>	
 *      SEMTK_ERROR_CALLBACK = errorCallback; // error function (messageString)
 *		SEMTK_LOAD_CALLBACK = doneLoading;    // ready function.  Uses the global: semtk
 *		SEMTK_LOAD_PATH = "..";               // relative URL path to folder containing iidx-oss and sparqlGraph
 *    </script>
 *
 *	  <script src="../sparqlGraph/js/semtk_api_loader.js"></script>
 * 
 * 
 */



var semtk = null;

var IIDX_PATH =        (typeof SEMTK_LOAD_PATH !== "undefined")  ? SEMTK_LOAD_PATH + "/iidx-oss"    : "../iidx-oss";
var SPARQLGRAPH_PATH = (typeof SEMTK_LOAD_PATH !== "undefined")  ? SEMTK_LOAD_PATH + "/sparqlGraph" : "../sparqlGraph";
var LOAD_CALLBACK =    (typeof SEMTK_LOAD_CALLBACK !== "undefined")   ? SEMTK_LOAD_CALLBACK  : function() { alert("SemTk loaded.\nNo SEMTK_LOAD_CALLBACK() is defined");};
var ERROR_CALLBACK =   (typeof SEMTK_ERROR_CALLBACK !== "undefined")  ? SEMTK_ERROR_CALLBACK : function(msg) { alert("SemTk error: " + msg);};

loadScript = function(url, callback)
{
    // Adding the script tag to the head as suggested before
    var head = document.getElementsByTagName('head')[0];
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = url;

    // Then bind the event to the callback function.
    // There are several events for cross browser compatibility.
    script.onreadystatechange = callback;
    script.onload = callback;

    // Fire the loading
    head.appendChild(script);
}

load3 = function() {
	requireConfigSparqlgraph( 
		"../sparqlGraph",
		{
			baseUrl : IIDX_PATH,
			paths : {
			    "jquery" :      "../sparqlGraph/jquery/jquery",

			}
		}
	);
	
	require([ 'sparqlgraph/js/semtk_api' ], function(SemtkAPI) {
		
		// load the semtk  (a global for this script)
		semtk = new SemtkAPI(ERROR_CALLBACK);
		
		// tell app we're done
		LOAD_CALLBACK();
	});
}

load2 = function() {
	loadScript(IIDX_PATH + "/js/require.config.js", load3);
}

load1 = function() {
	loadScript(SPARQLGRAPH_PATH + "/js/requiresetup.js", load2);
}

load0 = function() {
	loadScript(IIDX_PATH + "/components/requirejs/require.js", load1);
}

load0();