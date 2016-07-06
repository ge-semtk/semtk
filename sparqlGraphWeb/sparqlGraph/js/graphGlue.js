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

// not-quite-asynchronous-enough cache of stuff needed for callbacks
var globalConstraintCallback = null;
var globalObjtoSet = null;
var globalDraculaLabel = null;

var globalReturnNameCallback = null;
var globalObjtoSetRetName = null;

//global from sparqlGraph.html
var globalModalDialogue = null;      
var gConn;
var gAvoidQueryMicroserviceFlag;



//-----  Intermediate "glue" callbacks change dialog callbacks to Belmont callbacks and hold onto some params in globals
var attributeNameCallback = function (arr) {
	console.log("setting constraints in callback");
	globalConstraintCallback(globalObjtoSet, arr[0], globalDraculaLabel);
};

var attributeConstraintCallback = function (arr) {
	
	console.log("setting return name in callback");
	var retName;
	
	// if return name is not ""
	if (arr[0].length > 0) {
		//  make sure it has "?" 
		if (arr[0][0] == "?") {
			retName = arr[0];
		} else {
			retName = "?" + arr[0];
		}
		
		// if it is a new name
		if (retName != globalObjtoSetRetName.getSparqlID()) {
			var f = new SparqlFormatter();
			// make sure new name is legal
			var newName = f.genSparqlID(retName, gNodeGroup.sparqlNameHash);
			if (newName != retName) {
				alert("Using " + newName + " instead.");
			}
			retName = newName;
		}
	} else {
		retName = "";
	}
	
	globalReturnNameCallback(globalObjtoSetRetName, retName, globalDraculaLabel);
};

// PEC TODO: not called.  Do you really care if we occasionally reserve a SparqlID?
var attributeConstraintClearCallback = function() {
	// undo any sparqlID generation that might have happened in RangeDialog and isn't needed since user hit "Clear"
	freeUnusedSparqlID(globalObjtoSetRetName);
};

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

var dropCallback = function(val) {
	// PEC TODO: list
	//    ignores the anchorNode
	//    dialog allows multi-select
	var path = val[0];
	var anchorNode = val[1];
	var singleLoopFlag = val[2];
	gNodeGroup.addPath(path, anchorNode, gOInfo, singleLoopFlag);
	gNodeGroup.drawNodes();
  	guiGraphNonEmpty();
};



var rangeDialogue = function(callbackfunct, objToSetConstraintsFor, draculaLabel){
	require(['sparqlgraph/js/msiclientquery',
	         'jquery', 
	         'jsonp'], function(MsiClientQuery) {
		
		// objToSetconstraintsFor can be an PropertyItem or a SemanticNode
		// save the callback and the object in globals
		globalConstraintCallback = callbackfunct;
		globalObjtoSet = objToSetConstraintsFor;
		globalDraculaLabel = draculaLabel;
		
		// suggest default return name
		// PropItems may not have a SparqlID, so generate one if it empty
			
		var id = objToSetConstraintsFor.getSparqlID();
		if (id == null || id == "") {
			f = new SparqlFormatter();
			id = f.genSparqlID(objToSetConstraintsFor.getKeyName(), gNodeGroup.sparqlNameHash);
			
			// give the obj its sparqlID so that the query will work
			objToSetConstraintsFor.setSparqlID(id);
			gNodeGroup.reserveSparqlID(id);
		}
	
		var sparql = gNodeGroup.generateSparql(SemanticNodeGroup.QUERY_CONSTRAINT, false, 100, objToSetConstraintsFor);
		
		// get query interface
		var dataInterface = null;
		if (gAvoidQueryMicroserviceFlag) {
			// query directly
			dataInterface= gConn.getDataInterface();
		} else {
			// query microservice
			dataInterface = new MsiClientQuery(g.service.sparqlQuery.url, gConn.getDataInterface(), logAndAlert);
		}
		
		// launch the dialog
		gConstraintDialog.constraintDialog(objToSetConstraintsFor, sparql, dataInterface, attributeNameCallback);
	});
};



var returnNameDialogue = function (propName, currentReturnName, callbackfunct, objToSetReturnNameFor, draculaLabel){
	globalReturnNameCallback = callbackfunct;
	globalObjtoSetRetName = objToSetReturnNameFor;    //SemanticNode or PropertyItem
	globalDraculaLabel = draculaLabel;

	if (currentReturnName == null || currentReturnName == "") {
		f = new SparqlFormatter();
		currentReturnName = f.genSparqlID(propName, gNodeGroup.sparqlNameHash);
	}
	globalModalDialogue.textFieldDialog("Enter return name for " + propName, "Submit", ["return name"], [currentReturnName.slice(1)], attributeConstraintCallback, 52);
	
	
};
