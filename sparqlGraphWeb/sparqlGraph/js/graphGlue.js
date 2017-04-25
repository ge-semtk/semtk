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


//global from sparqlGraph.html
var globalModalDialogue = null;      
var gConn;
var gAvoidQueryMicroserviceFlag;


//-----  Intermediate "glue" callbacks change dialog callbacks to Belmont callbacks and hold onto some params in globals


var returnPropertiesAsString = function(oClass){
	var strVal = '';
	
	props = oClass.getProperties();
	var k = props.length;
	for(var i = 0; i < k; i++){
		strVal = strVal + ":" + props[i].getName().getLocalName() + ", " + props[i].getRange().getLocalName();
		}
	return strVal;
};

var genPathString = function(path, anchorNode, singleLoopFlag) {
	var str = anchorNode.getSparqlID() + ": ";
	
	// handle diabolical case
	if (singleLoopFlag) {
		cl = new OntologyName(path.getClass0Name(0)).getLocalName();
		var att = new OntologyName(path.getAttributeName(0)).getLocalName();
		str += anchorNode.getSparqlID() + "-" + att + "->" + cl + "_NEW";
	}
	else {
		var first = new OntologyName(path.getStartClassName()).getLocalName();
		str += first;
		if (first != anchorNode.getURI(true)) str += "_NEW";
		var last = first;
		
		for (var i=0; i < path.getLength(); i++) {
			var class0 = new OntologyName(path.getClass0Name(i)).getLocalName();
			var att = new OntologyName(path.getAttributeName(i)).getLocalName();
			var class1 = new OntologyName(path.getClass1Name(i)).getLocalName();
			var sub0 = "";
			var sub1 = "";
			
			// mark connecting node on last hop of path
			if (i == path.getLength() - 1) {
				if (class0 == last) {
					sub0 = anchorNode.getSparqlID();
				} else {
					sub1 = anchorNode.getSparqlID();
				}
			}
			
			if ( class0 == last ) {
				str += "-" + att + "->";
				str += sub1 ? sub1 : class1;
				last = class1;
			} else {
				str += "<-" + att + "-";
				str += sub0 ? sub0 : class0;
				last = class0;
			}
		}
		if (last != anchorNode.getURI(true)) str += "_NEW";
	}
	
	return str;
};


