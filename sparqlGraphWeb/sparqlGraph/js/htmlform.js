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
	 *  Documentation on external controls.  In other words, choice elements outside the HTMLForm that might control it.
	 *       So the "external control", not in HtmlFormGroup, changes the value of some "internal control" which is part of HtmlFormGroup
	 *  
	 *  (1) Hide the internal control so that the user can mess with it.   works great:  <span style="display: none;">  ... </span>
	 *  (2) Register inside the preQueryCallback
	 *           - choicesSetValue(values_from_external_choice)
	 *                    * note this is the most popular option, but you could pull the values or do some other reconciling
	 *                    * but this normal function grabs the values of the external control, sets the internal before running a query
	 *                    * also note that any invalid choices are OK, they'll be returned by choiceGetValue(elem_internal) and choiceGetValue(elem_internal, true)
	 *  (3) Register inside the postQueryCallback
	 *           - choiceGetChoices(elem_internal) gets all legal choices
	 *           - choiceGetValue(elem_internal) gets all selected choices
	 *           - choiceGetValue(elem_internal, tru) gets all the selected choices that aren't actually valid right now given the rest of the query
	 *           
	 *  (4) To activate your choice (onchange or button press) call runChangedCallback(elem_internal).
	 *           - this will use (2) to fetch the values and (3) to set new ones.
	 */
	
	var HtmlFormGroup = function(document, g_conn, g_fields, g_query, statusFunc, alertFunc, preQueryCallback, postQueryCallback, doneUpdatingCallback, optDefaultHash, optGetFlag) {
		// set up the connection, load the form, call the callback
		//
		// preQueryCallback() - called before any query.  Good time to update any choice elements in the HtmlFormGroup, and disable buttons.
		// postQueryCallback(res) - after every change to choices, this is called with the results of the full query
		// doneUpdatingCallback() - called after the last query returns (can be after the postQueryCallback).  Good time to re-enable buttons.
		// optDefaultHash = {"choiceElementId": ["val1", "val2"],  
		//                   "choiceElementId2": ["text|val"] 
		//                  }
		//                    values may be "text|val".   If no colon, then the entire value is used as both text and value
	    //                    illegal values are ignored.
		// DOCUMENTATION:
		//     see UBL  formsetup.js
		//     older possibly out-of-date examples are in turbineeng/src/main/ui/trend-datastore/js/config.js
		
		// ============= variables ====================
	    this.conn = null;				// SPARQL connection
		this.oInfo = null;				// ontologyInfo
		this.nodeGroup = null;			// node group is where the query is built
		this.curField = null;           // current field (proxy for a parameter to callbacks)
		this.filterDict = {};           // this.filterDict[sparqlID][elementId] = sparql filter text
		this.document = document;
		
		this.query = null;
		this.fields = null;
		this.preQueryCallback = preQueryCallback;
		this.postQueryCallback = postQueryCallback;
		this.doneUpdatingCallback = doneUpdatingCallback;
		this.defaultHash = (typeof optDefaultHash === 'undefined' ? null : optDefaultHash);
		this.getFlag = (typeof optGetFlag === 'undefined' ? false : optGetFlag);

		this.outstandingQueries = 0;
		
		// now add all the fields, set up the query, establish the sparql connection
		this.setFields(g_fields);
		this.setQuery(g_query);
		this.setConn(g_conn);
		
		// Get oInfo, then continue formGroup initialization in initFormCallback
		this.oInfo = new OntologyInfo();
		if (this.getFlag) { this.oInfo.setGetFlag();}
		this.oInfo.loadAsync(this.conn, statusFunc, 
							(function(){ this.initFormCallback(); }).bind(this), 
							alertFunc); 
		
	};
	
	HtmlFormGroup.QUERY_FULL = 0;
	HtmlFormGroup.QUERY_CONSTRAINT = 1;

	
	HtmlFormGroup.prototype = {
		// ----  START EMERGENCY STUBBED IN SECTION -----
		
		setConn : function(g_conn) {
			// Establish Sparql Connection in this.conn, using g		
			this.conn = new SparqlConnection();
			this.conn.name = g_conn.name;
			this.conn.serverType = g_conn.serverType;
	
			this.conn.dataServerUrl = g_conn.serverURL;
			this.conn.dataKsServerURL = ""; // fuseki will not have this
			this.conn.dataSourceDataset = g_conn.dataset;
	
			this.conn.ontologyServerUrl = g_conn.serverURL;
			this.conn.ontologyKsServerURL = ""; // fuseki will not have this
			this.conn.ontologySourceDataset = g_conn.dataset;
	
			this.conn.domain = g_conn.domain;
			this.conn.build();
		},	
		
		setFields : function(g_fields) {
			this.fields = g_fields;
			this.initConfig();
		},
		setQuery : function(g_query) {
			this.query = g_query;
		},

		initFormCallback : function() {
			
			this.setCallbacks();
			this.enableDisableFields();
			this.initFixedSelects();
			this.buildNodeGroup();
			
			// put in any default values
			this.setFormDefaults();
			// read defaults out of form
			this.readFormConstraints();
			// update the form from the datastore, retaining the defaults:  async
			this.updateAllChoices();
			// run the count query: async
			this.runCountQuery();
			// run the actual data query and call the user's callback
			this.runQuery();
		},
		
		setFormDefaults : function() {
			// apply this.defaultHash to the choices
			// MUST be called while form elements are still empty...BEFORE first populated from queries
			
			
			if (this.defaultHash === null) {return;}
			
			// loop through the defaultHash
			for (var id in this.defaultHash) {
				var element = this.document.getElementById(id);
				var val = this.defaultHash[id];
				
				// for selects, add option(s) and select them
				if (element.nodeName === "SELECT" || element.nodeName === "DIV") {
					if (Array.isArray(val)) {
						for (var i=0; i < val.length; i++) {
							var splitVal = val[i].split("|");
							this.choiceAddOption(element, 
												 splitVal[0], 
												 (splitVal.length > 1) ? splitVal[1] : splitVal[0], 
												 true);
						}
					} else {
						var splitVal = val.split("|");
						this.choiceAddOption(element, 
								 splitVal[0], 
								 (splitVal.length > 1) ? splitVal[2] : splitval[1], 
								 true);
					}
					
				// for non-selects, just set the value
				} else {
					element.value = val;
				}
				this.fieldChanged(id, false);
			}
			
		},
		
		// execute the query I've been building & constraining
		// PEC TODO: callback should be this.postQueryCallback, and NOT a parameter
		runQuery : function() {
			
			var tmpNodeGroup = this.nodeGroup.deepCopy();
			this.addQueryReturns(tmpNodeGroup);   
			this.pruneNodeGroup(tmpNodeGroup);
			
			var sparql = tmpNodeGroup.generateSparql(SemanticNodeGroup.QUERY_DISTINCT, false, 0, null);
			this.registerQueryCall();
			if(this.getFlag){
				this.conn.getDataInterface().executeAndParseGet(sparql,  (function(res){this.runQueryCallback(res);}).bind(this)  );
			}
			else{
				this.conn.getDataInterface().executeAndParse(sparql,  (function(res){this.runQueryCallback(res);}).bind(this)  );
				
			}
		},
		
		runQueryCallback : function (res) {
			this.postQueryCallback(res);
			this.registerQueryReturn();
		},
		
		setCallbacks : function() {
			// set up callbacks on all choice elements
			
			for (var i = 0; i < this.fields.length; i++) {
				var element = this.document.getElementById(this.fields[i].elementId);
				
				var hform = this;
				var id = element.id;
				
				
				if (element.nodeName === "DIV") { 

					// no callbacks for "DIV":  GUI designer is responsible for calling runChangedCallback(div_element)
					
					//element.addEventListener("blur", function() { alert("onblurC"); }, true);
				} else {
					element.onchange = new Function('this.fieldChanged("' + id + '", true); return true;').bind(this);
				}
				
			}
			
		},
		
		runChangedCallback : function (element) {
			
		// call this after programatically changing a choice element's value
			if (element.nodeName === "DIV") { 
				this.fieldChanged(element.id, true);
			} else {
				element.onchange();
			}
		},
		
		// PEC TODO
		//   - getElementbyId needs error checking
		//   - count-div  and exact message are hard-coded
		//   - getOrAddURI fails spectacularly silently when URI is bad
		
		// ----  END EMERGENCY STUBBED IN SECTION -----
		
		enableDisableFields : function() {
			// enable or disable any form elements that depend upon other elements' value
			for (var i = 0; i < this.fields.length; i++) {
				var f = this.fields[i];
				var element = this.document.getElementById(f.elementId);
				
				// if element depends on others for its enable/disable
				if (f.hasOwnProperty("enablingElementId") && f.hasOwnProperty("enablingElementValues")) {
					var enablingElement = this.document.getElementById(f.enablingElementId);
					
					
					// if value is in the list then enable
					if (f.enablingElementValues.indexOf(enablingElement.value) > -1) {
						element.disabled = false;
					} else {
						this.choiceReset(element);
						element.disabled = true;
					}
				} else {
					// not dependent on others, so always enable it
					// this will clear out the browsers memory if user left the page in a wierd state last time
					element.disabled = false;
				}
			}
		},
		
		
		// parse sparqlClasses from config, and build a nodeGroup from it
		buildNodeGroup : function () {
			// Build this.nodeGroup based on "g" in config.js, and the values of html elements
			// The extra "returns" values are not added until later
			// when we decide whether we want "constraintReturns" or "returns"
			
			this.nodeGroup = new SemanticNodeGroup(1000, 700, 'canvas');
			
			// for each sparqlClasses (defined in config.js)
			for (var i = 0; i < this.fields.length; i++) {
				var f = this.fields[i];
				var element = this.document.getElementById(f.elementId);
				
				// skip if element is disabled
				if (element.disabled) continue;
				
				// skip if it has "dependsOnURI" and that URI isn't in the nodegroup yet
				if (f.hasOwnProperty("dependsOnURI")) {
					var found = false;
					for (var j=0; j < f.dependsOnURI.length; j++) {
						if (this.nodeGroup.getNodesByURI(f.dependsOnURI[j], this.oInfo).length > 0) {
							found = true;
							break;
						}
					}
					if (! found) continue;    // skip to next fields[i]
				}
				
				var uri = this.getFieldClassURI(f);
				var snode = null;
				
				
				// check for bad uri
				if (! this.oInfo.containsClass(uri)) {
					alert("HtmlForm.buildNodeGroup() Internal error: class does not exist in ontology info: " + uri);
					throw("internal error in htmlform.js");
				}
				
				// path can only be optional if every field with this classURI is "" so there well be no constraints
				var pathOptionalFlag = false;
				if (f.hasOwnProperty("optional") &&  this.fieldsAllEmpty(this.getFieldsByClassURI(f.classURI))) {
					pathOptionalFlag = true;
				}
				
				// find or add the snode
				snode = this.nodeGroup.getOrAddNode(uri, this.oInfo, this.conn.getDomain(), true, pathOptionalFlag); // superClassFlag=true				
				
				//**** Check that return props exist, and have the right name ****//
				if (f.hasOwnProperty("prop")) {
					// return uri->keyName as "sparqlID"
					var keyName = f.prop.keyName
					
					// look up the property
					var prop = snode.getPropertyByKeyname(keyName);
					
					// if it isn't found, try nonDomain property
					if (!prop) {
						
						if (! f.prop.hasOwnProperty("nonDomainType")) {
							alert("Internal error in buildNodeGroup(): " + f.name + " property " + keyName + "isn't found and doesn't have nonDomainType property.");
							throw "internal error";
						} 
						
						if (! f.prop.hasOwnProperty("nonDomainRelationURI") ){
							alert("Internal error in buildNodeGroup(): " + f.name + " property " + keyName + "isn't found and doesn't have nonDomainRelationURI property.");
							throw "internal error";
						}  
						
						prop = snode.addNonDomainProperty(	keyName,
															f.prop.nonDomainType.split('#')[1],
															f.prop.nonDomainType,
															f.prop.nonDomainRelationURI
														 );
					}
					
					// change sparqlID if it is wrong
					var suggestedID = f.prop.sparqlID
					if (prop.getSparqlID() !== suggestedID) {
						var actualID = this.nodeGroup.changeSparqlID(prop, suggestedID);
						if (actualID !== suggestedID) {
							alert("Internal error: tried to build sparqlID return '" + suggestedID + "' but got '" + actualID + "'.");
							return null;
						}
					}
				
				} else if (f.hasOwnProperty("node")) {
					
					// change sparqlID if it is wrong
					var suggestedID = f.node.sparqlID
					if (snode.getSparqlID() !== suggestedID) {
						var actualID = this.nodeGroup.changeSparqlID(snode, suggestedID);
						if (actualID !== suggestedID) {
							alert("Internal error: tried to build sparqlID return '" + suggestedID + "' but got '" + actualID + "'.");
							return null;
						}
					}
					
				}
			}
			
			return(this.nodeGroup);
		},
		
		addQueryReturns : function (nodeGroup, optSkipConstraintFlag) {
			// Add in the query.returns
			// optSkipConstraintFlag of QUERY_CONSTRAINT determines that skipForConstraint=true fields are skipped.
			
			var skipConstraintFlag = (typeof optSkipConstraintFlag !== 'undefined') && (optSkipConstraintFlag === HtmlFormGroup.QUERY_CONSTRAINT);
			var retlist = this.query.returns;
			
			// for each query.returns
			for (var i = 0; i < retlist.length; i++) {
				// skip entire URI if requested
				if (skipConstraintFlag && 
						retlist[i].hasOwnProperty("skipForConstraint") && retlist[i].skipForConstraint == true) {
					continue;
				}
				
				// skip if it has "dependsOnURI" and that URI isn't in the nodegroup yet
				if (retlist[i].hasOwnProperty("dependsOnURI")) {
					var found = false;
					for (var j=0; j < retlist[i].dependsOnURI.length; j++) {
						if (this.nodeGroup.getNodesByURI(retlist[i].dependsOnURI[j], this.oInfo).length > 0) {
							found = true;
							break;
						}
					}
					if (! found) continue;
				}
				
				var uri = retlist[i].classURI;
				var snode = null;
				
				// add the node if needed
				var optFlag = retlist[i].hasOwnProperty("optional");
				snode = nodeGroup.getOrAddNode(uri, this.oInfo, this.conn.getDomain(), true, optFlag);   // superClassFlag=true
				
				//**** Return the class if requested ****//
				if (retlist[i].hasOwnProperty("node")) {
					
					// check skipForConstraint
					if (skipConstraintFlag && 
							retlist[i].node.hasOwnProperty("skipForConstraint") && retlist[i].node.skipForConstraint == true) {
						; // skip
					}
					// make sure it has a sparqlID
					else if (retlist[i].node.hasOwnProperty("sparqlID")) {
	
						// change sparql id if it is wrong
						var targetID = retlist[i].node.sparqlID;
						if (snode.getSparqlID() !== targetID) {
							var newID = nodeGroup.changeSparqlID(snode, targetID);
							if (newID !== targetID) {
								alert("Internal error in addQueryReturns(): tried to build sparqlID return '" + targetID + "' but got '" + newID + "'.");
								return null;
							}
						}
						// set returned: no harm if it is already returned
						snode.setIsReturned(true);
						
					}
				}
				//**** Set properties to return ****//
				if (retlist[i].hasOwnProperty("props")) {
					for (var j = 0; j < retlist[i].props.length; j++) {
						
						// check skipForConstraint
						if (skipConstraintFlag && 
								retlist[i].props[j].hasOwnProperty("skipForConstraint") && retlist[i].props[j].skipForConstraint == true) {
							continue; // skip
						}
						
						// return uri->keyName as "sparqlID"
						var keyName = retlist[i].props[j].keyName
						var targetID = retlist[i].props[j].sparqlID
						
						// look up the property
						var prop = snode.getPropertyByKeyname(keyName);
						
						if (!prop) {
							
							// try to add as subclass property
							if (retlist[i].props[j].hasOwnProperty("subclassPropURI")) {
								prop = nodeGroup.addSubclassPropertyByURI(snode, retlist[i].props[j].subclassPropURI, this.oInfo);
								
								if (!prop) {
									alert("Internal error in addQueryReturns(): can't find subclass property '" + retlist[i].props[j].subclassPropURI + "' of '" + retlist[0].classURI + "'");
									return null;
								}
								
							} else {
								alert("Internal error in addQueryReturns(): can't find property '" + keyName + "' of '" + retlist[0].classURI + "'");
								return null;
							}
						}
						
						// change sparqlID if it is wrong
						if (prop.getSparqlID() !== targetID) {
							var newID = nodeGroup.changeSparqlID(prop, targetID);
							if (newID !== targetID) {
								alert("Internal error in addQueryReturns(): tried to build sparqlID return '" + targetID + "' but got '" + newID + "'.");
								return null;
							}
						}
						
						// set returned: no harm if it is already returned
						prop.setIsReturned(true);
						
						// set a constraint if one is listed
						if (retlist[i].props[j].hasOwnProperty("constraint")) {
							var constraint = retlist[i].props[j].constraint;
							
							if (prop.hasConstraints()) {
								constraint = prop.getConstraints() + ". " + constraint;
							}
							prop.setConstraints(constraint);
							
						}
						
						// make optional only if there are no constraints 
						if (optFlag && ! prop.hasConstraints()) {
							if (optFlag) prop.setIsOptional(true);
						}
					}
				}
			}
			
			return nodeGroup;
		},
		
		initConfig : function () {
			// pre setup on the config variable g
			for (var i=0; i < this.fields.length; i++) {
				var f = this.fields[i];
				
				// if there might be multiple filters on the same sparqlID, then set up a blank dictionary entry
				// this.filterDict[sparqlID][elementId]
				if (f.hasOwnProperty("constraintType") && f.constraintType != "value" && f.constraintType != "class") {
					
					var sparqlID = f.prop.sparqlID;
					// create this.filterDict[sparqlID] if needed
					if (! (sparqlID in this.filterDict) ) {
						this.filterDict[sparqlID] = {};
					}
					// overwrite this.filterDict[sparqlID][elementID])
					this.filterDict[sparqlID][f.elementId] = "";
				}
			}
		},
		
		initFixedSelects : function () {
			for (var i=0; i < this.fields.length; i++) {
				var f = this.fields[i];
				
				// skip if no elementId 
				if (! f.hasOwnProperty("elementId")) continue;
				
				// skip if element is disabled
				var element = this.document.getElementById(f.elementId);
				if (element.disabled) continue;
				
				if (f.hasOwnProperty("choiceMode")) {
					if (f.choiceMode == "fixed") {
						this.choiceRemoveAllChoices(element);
					    
						// add choices if there are any
						if (f.hasOwnProperty("choices")) {
							for (var c=0; c < f.choices.length; c++) {
								this.choiceAddOption(element, f.choices[c].text,
										                      f.choices[c].value,
										                      f.choices[c].hasOwnProperty("selected"));
							}
						}
					}
				}
			}
		},
		
		// initialize the form - iterathe through all fields, populating dropdowns as appropriate
		updateAllChoices : function () {
			// look through this.fields for those with an "choiceMode"
			//     - independent: run a query with just that class and prop.  Fill dropdown.

			
			
			for (var i=0; i < this.fields.length; i++) {
				var f = this.fields[i];
				
				// skip if no elementId 
				if (! f.hasOwnProperty("elementId")) continue;
				
				// skip if element is disabled
				var element = this.document.getElementById(f.elementId);
				if (element.disabled) continue;
				
				// initialize choices
				if (f.hasOwnProperty("choiceMode")) {
					
					// do "independent" choiceMode
					if (f.choiceMode == "independent") {
						
						// "independents" are only populated once. 
						if (element.length == 0) {
							this.choiceBusy(element);
							this.runIndependentElemValuesQuery(element);
							
						}
					}
					else if (f.choiceMode == "fixed") {

						// "fixed" are only populated once during initFixedSelects()
						
					}
					else if (f.choiceMode == "constraints") {
						this.choiceBusy(element);
						this.runConstraintElemValuesQuery(element);
						
					}
					else if (f.choiceMode == "validate") {
					   	// do nothing
					}
				}
			}
		},
		
		runIndependentElemValuesQuery : function (element) {
			var f = this.getFieldByElementId(element.id);
			
			// build the nodegroup
			var iNodeGroup = new SemanticNodeGroup(1000, 700, 'canvas');
			this.addQueryReturns(iNodeGroup, HtmlFormGroup.QUERY_CONSTRAINT);  

			var uri = this.getFieldClassURI(f);
			var snode = iNodeGroup.addNode(uri, this.oInfo);
			var item = null;
			
			// find the right return item
			if (f.hasOwnProperty("prop")) {
				var keyName = f.prop.keyName;
				item = snode.getPropertyByKeyname(keyName);
			} else {
				item = snode;
			}
			item.setIsReturned(true);
			
			// set the column name to the html elementId (this is how we can differentiate among multiple similar asynchronous requests)
			// Dashes are illegal in SparqlID, so make underscore.
			var suggestedID = "?" + f.elementId.replace(/-/g,'_');
			var actualID = iNodeGroup.changeSparqlID(item, suggestedID);
			if (actualID != suggestedID) {
				alert("Internal error in updateAllChoices(): can't set sparqlID to: " + suggestedID + ". Got: " + actualID);
				return null;
			}
			
			// build and run the query
			this.pruneNodeGroup(iNodeGroup);
			console.log("Generating independent choices query:");
			var sparql = iNodeGroup.generateSparql(SemanticNodeGroup.QUERY_DISTINCT, false, 0);
			
			this.registerQueryCall();
			if(this.getFlag){
				this.conn.getDataInterface().executeAndParseGet(sparql, (function(res){this.callbackSetChoices(res);}).bind(this));
			}
			else{
				this.conn.getDataInterface().executeAndParse(sparql, (function(res){this.callbackSetChoices(res);}).bind(this));
					
			}
		}, 
		
		runConstraintElemValuesQuery : function (element) {
			var f = this.getFieldByElementId(element.id);
			var tmpNodeGroup = this.nodeGroup.deepCopy();
			this.addQueryReturns(tmpNodeGroup, HtmlFormGroup.QUERY_CONSTRAINT);  

			var item = this.getFieldsConstrainItem(f, tmpNodeGroup);
			item.setIsReturned(true);
			
			// set the column name to the html elementId (this is how we can differentiate among multiple similar asynchronous requests)
			// Dashes are illegal in SparqlID, so make underscore.
			var suggestedID = "?" + f.elementId.replace(/-/g,'_');
			var actualID = tmpNodeGroup.changeSparqlID(item, suggestedID);
			if (actualID != suggestedID) {
				alert("Internal error in updateAllChoices(): can't set sparqlID to: " + suggestedID + ". Got: " + actualID);
				return null;
			}
			
			// PEC TODO, there needs to be a non-zero LIMIT on this query and a check and a warning?
			this.pruneNodeGroup(tmpNodeGroup);
			console.log("Generating constrained choices query:");
			var sparql = tmpNodeGroup.generateSparql(SemanticNodeGroup.QUERY_CONSTRAINT, false, 0, item);
			
			this.registerQueryCall();
			
			if(this.getFlag){
				this.conn.getDataInterface().executeAndParseGet(sparql, (function(res){this.callbackSetChoices(res);}).bind(this));
							}
			else{
				this.conn.getDataInterface().executeAndParse(sparql, (function(res){this.callbackSetChoices(res);}).bind(this));
									
			}
			
		},
		
		readFormConstraints : function () {
			// call all the field callbacks so that constraints are added to the query
			// don't call field callbacks that change the snodes in the query...only constraints
			
			
			for (var i=0; i < this.fields.length; i++) {
				var f = this.fields[i];
				
				var element = this.document.getElementById(f.elementId);
				if (element.disabled) continue;
				
				this.readFormConstraint(f);
				
			}
		},
		
		 registerQueryCall : function()  {
			 // increment count of outstanding queries
			this.outstandingQueries += 1;
		},

		registerQueryReturn : function() {
			// decrement count of outstanding queries
			// if count is now zero, run the doneUpdatingCallback()
			//
			// To ensure that you're actually done updating, this should be called
			// at the very end of a callback
			
			this.outstandingQueries -= 1;
		
			if (this.outstandingQueries == 0) {
				this.doneUpdatingCallback();
			}
		},
		
		// =========== choice functions:  generic functions run on choice elements
		choiceBusy : function(element) {
			element.disabled = true;
			element.classList.add("focused");
		},
		
		choiceUnBusy : function(element) {
			element.disabled = false;
			element.classList.remove("focused");
		},
		
		choiceRemoveAllChoices : function(element) {
			// remove any options
			
			if (element.nodeName === "SELECT") {
			    for(var j=element.options.length-1; j>=0; j--)
			    {
			    	element.remove(j);
			    }
			} else if (element.nodeName === "DIV") {
				element.innerHTML = "";
			}
		},
		
		choiceReset : function(element) {
			// set and element to blank
			
			if (element.nodeName === "SELECT") {
				// multiple: choose -1
				if (element.multiple) {
					element.selectedIndex = -1;
					
				// single selects: choose the first one
				} else {
					element.selectedIndex = 0;
				}
			
			} else if (element.nodeName === "DIV") {
				
				 var checks = document.querySelectorAll('#' + element.id + ' input[type="checkbox"]');
				 
				 for(var i =0; i< checks.length; i++) {
					checks[i].checked = false;
				 }
			} else {
				element.value = "";
			}
		},
		
		choiceResetList : function(eList) {
			// reset elements, updateing the query
			
			for (var i=0; i < eList.length; i++) {
				var e = this.document.getElementById(eList[i]);
				if (! e.disabled) {
					this.choiceReset(e);
					this.fieldChanged(eList[i], false);
				}
			}
			
		},
		
		choiceGetValue : function(element, optDisabledOnlyFlag) {
			// return the choice element's value as a list
			// if optDisabledOnlyFlag: return only disabled values
			var ret;
			var disabledOnlyFlag = (typeof optDisabledOnlyFlag == "undefined") ? false : optDisabledOnlyFlag;
			
			// for selects...
			if (element.nodeName === "SELECT") {
				// multiple selects [text, text, text, ...]
				if (element.multiple) {
					ret = [];
					for (var i=0; i < element.length; i++) {
					    var opt = element.options[i];
					    if (opt.selected && (!disabledOnlyFlag || opt.disabled)) {
					      ret.push(opt.value);
					    }
					  }
					
				// single selects: choose the first one
				} else {
					ret = element.value;
				}
				
			} else if (element.nodeName === "DIV") {
				
				 var checks = document.querySelectorAll('#' + element.id + ' input[type="checkbox"]');
				 ret = [];
				 for(var i =0; i< checks.length; i++) {
					if (checks[i].checked  && (!disabledOnlyFlag || checks[i].disabled)) {
						ret.push(checks[i].value);
					}
				 }
				 
			} else {
				ret = [element.value];
			}
			return ret;
		},
		
		choiceGetChoices : function(element) {
			// return all possible values from a choice element
			// Always returns a list.
			var ret = [];
			
			// for selects...
			if (element.nodeName === "SELECT") {
				// multiple selects [text, text, text, ...]
				for (var i=0; i < element.length; i++) {
				    ret.push(element.options[i].value);
				 }
				
			} else if (element.nodeName === "DIV") {
				
				 var checks = document.querySelectorAll('#' + element.id + ' input[type="checkbox"]');
				 ret = [];
				 for(var i =0; i< checks.length; i++) {
						ret.push(checks[i].value);
				 }
				
			} else {
				ret.push(element.value);
			}
			return ret;
		},
		
		choiceSetValue : function(element, valList) {
			// set the choice element's value
			// val can be "one" or ["list", "of", "things"]
			// Where possible, both option.text and option.value are set to val
			//
			// When you're done, you probably want to call element.onchange()
			//
			// val type depends on element type:  see choiceGetValue()
			//
			// If a value is not in the element, it is added as DISABLED 
			//
			var disable_choices = [];
			if (element.nodeName === "SELECT") {

				// check for multiple select mis-match
				if (valList.length > 1 && !element.multiple) {
					throw "Internal in htmlForm.choiceSetValue: " + element.id + " is not a multi-select so value can't be set to " + valList;
				}
				
				disable_choices = valList.slice();
				// select any values in valList
				// and set element.value to the last one
				for (var i=0; i < element.length; i++) {
				    var opt = element.options[i];
				    var pos = valList.indexOf(opt.value);
				    if (pos > -1) {
				      	opt.selected = true;
				      	opt.disabled = false;
				      	
				      	disable_choices.splice(disable_choices.indexOf(opt.value), 1);      // remove from disable_choices
				    } else {
				    	opt.selected = false;
				    }
				 }
				
			} else if (element.nodeName === "DIV") {
				
				 var checks = document.querySelectorAll('#' + element.id + ' input[type="checkbox"]');
				 disable_choices = valList.slice();
				 
				 for(var i=0; i< checks.length; i++) {
					 var chk = checks[i];
					 var pos = valList.indexOf(chk.value);
					 if (pos > -1) {
						 chk.checked = true;
						 chk.disabled = false;
						 
					     disable_choices.splice(disable_choices.indexOf(chk.value), 1);      // remove from disable_choices

					 } else {
						 chk.checked = false;
					 }
				 }
				
			// non selects
			} else {
				element.value = val;
			}
		
			// add any values that weren't in the select as selected/disabled
			for (var i=0; i < disable_choices.length; i++) {
				var text = this.getOptionText(element, disable_choices[i]);
				this.choiceAddOption(element, text, disable_choices[i], true, true);
			}
			
			return ret;
		},
		
		choiceAddOption : function(element, text, value, selected, optDisabled) {
			var disabled = (typeof optDisabled === 'undefined') ? false : optDisabled;
			// add an option to a select element
			var SPACE = "\u00a0";
			
			if (element.nodeName == "SELECT") {
				var option = this.document.createElement("option");
				option.text = text;
				option.value = value;
				if (selected) {option.selected = selected;}
				option.disabled = disabled;
				element.add(option);
			
			} else if (element.nodeName === "DIV") {
				var checkbox = this.document.createElement("input");
				checkbox.type = "checkbox";
				checkbox.value = value;
				checkbox.checked = selected;
				checkbox.disabled = disabled;
				element.appendChild(document.createTextNode(SPACE));
				element.appendChild(checkbox);
				element.appendChild(document.createTextNode(SPACE));
				element.appendChild(this.document.createTextNode(text));
				element.appendChild(this.document.createElement("br"));
				 
			} else {
					alert("Internal error in htmlform.js:choiceAddOption(): element is not a select: id=" + element.id);
					throw "Internal error";
			}
		},
		
		choiceIsMultiple : function(element) {
			if (element.nodeName === "SELECT" && element.multiple) {return true;}
			if (element.nodeName === "DIV") { return true;}
			
			return false;
		},
		
		choiceLegalValSubset : function(element, valList) {
			alert("HtmlForm.choiceLegalValSubset() is DEPRECATED");
			// return subset of valList that is still a legal option in a MULTIPLE element
			var ret = [];
			if (element.nodeName == "SELECT") {
				for (var i=0; i < element.length; i++) {
				    var opt = element.options[i];
				    if (valList.indexOf(opt.value) > -1) {
				      	ret.push(opt.value);
				    }
				}
				
			}  else if (element.nodeName === "DIV") {
				var checks = document.querySelectorAll('#' + element.id + ' input[type="checkbox"]');
				 
				for(var i =0; i< checks.length; i++) {
					if (valList.indexOf(checks[i].value) > -1) {
				      	ret.push(checks[i].value);
				    }
				}
				 
			} else {
				throw "Internal error in htmlform.js:choiceLegalValSubset(): element is not multi-selectable: id=" + element.id;
			}
			return ret;
		},
		
		choiceIsEmpty : function(element) {
			return element.value == "";
		},
		
		//================================== END choice ============================
		
		
		callbackSetChoices : function(res) {
			// initialize a field (e.g. populate a dropdown) based on return from a query		
			

			// check for wonky errors from server.
			if (res.getColumnCount() === 0) {
				alert("Internal error in htmlform.callbackSetChoices(): Bad result from server.  No columns.");
				this.choiceUnBusy(element);
				console.log("-----------" + new Date() + "------------\n" +
					    "callbackSetChoices(): No columns returned for" + ((res.getColumnCount() > 0) ? res.getColumnName(0).replace(/_/g,'-') : "<unknown>") + "\n" +
					    JSON.stringify(res.jsonObj));
				this.registerQueryReturn();
				return;
			}
			
			// check results of this link in the chain
			if (res != null && !res.isSuccess()) {
				alert("Internal error: field-initialization query failed for: " + this.fields[this.curField].name);
				this.choiceUnBusy(element);
				console.log("-----------" + new Date() + "------------\n" +
					    "callbackSetChoices(): unSuccessful return for " + ((res.getColumnCount() > 0) ? res.getColumnName(0).replace(/_/g,'-') : "<unknown>") + "\n" +
					    JSON.stringify(res.jsonObj));
				this.registerQueryReturn();
				return null;
			}
			
			// log empty results
			if (res.getRowCount() == 0) {
				//alert("DEBUG: Zero choices returned for: " + res.getColumnNames());
				console.log("-----------" + new Date() + "------------\n" +
						    "Zero returns (could be OK or a server error) for " + ((res.getColumnCount() > 0) ? res.getColumnName(0).replace(/_/g,'-') : "<unknown>") + "\n" +
						    JSON.stringify(res.jsonObj));
			}
			

			
			// pull element name out of column header
			var elementId = res.getColumnName(0).replace(/_/g,'-');
			console.log("CallbackSetChoices(): " + elementId);
			var element = this.document.getElementById(elementId);
			if (! element) {
				alert("Internal error in callbackSetChoices(): Can't find html element: " + elementId);
				this.choiceUnBusy(element);
				this.registerQueryReturn();
				return null;
			}
			         
			// save current value and text
			var prev_val = this.choiceGetValue(element);
			
			// remove everything in the element
			this.choiceRemoveAllChoices(element);
			
			// add blank element to non-multiples
			if (! this.choiceIsMultiple(element) ) {
				this.choiceAddOption(element, "", "", false);
			}
			
			// add all the return values
			res.sort(res.getColumnName(0));
			for (var i=0; i < res.getRowCount(); i++) {
				var val = res.getRsData(i, 0, SparqlServerResult.prototype.NAMESPACE_YES);     // full name
				var txt = this.getOptionText(element, val);
				if (txt === null || txt === "") continue;   // skip blanks in query return, or values not in choiceMap
				this.choiceAddOption(element, txt, val, false);
			}
						
			this.choiceSetValue(element, prev_val);
			this.choiceUnBusy(element);
			this.registerQueryReturn();
		},
		
		getOptionText : function(element, value) {
			// get text for a value.
			//      (1) if element has a choiceMap, then look it up
			//                if it doesnt exist, return null
			//      (2) otherwise, if value contains '#'
			//                return value.split('#')[1]
			//
			var f = this.getFieldByElementId(element.id);
			if (f.hasOwnProperty("choiceMap")) {
				for (var i=0; i < f.choiceMap.length; i++) {
					if (f.choiceMap[i].value == value) {
						return f.choiceMap[i].text;
					}
				}
				return null;
			} else {
				if (value.indexOf('#') > -1) {
					return value.split('#')[1];
				} else {
					return value;
				}
			}
		},
		
		readFormConstraint : function(f) {
			// read the element and set the query constraints
			var element = this.document.getElementById(f.elementId);
			
			// set value constraints
			if (f.constraintType == "value") {
				var item = null;
				
				item = this.getFieldsConstrainItem(f, this.nodeGroup);
				if (! item ) {
					alert("Internal error in readFormConstraint(): can't find constraint item for: " + elementId);
					return null;
				}
				
				var valList = this.choiceGetValue(element);
				
				if (valList.length > 0) {					
					// build value constraint with a single value
					var constraintTxt = item.buildValueConstraint(valList);
					item.setConstraints(constraintTxt);
				} else {
					item.setConstraints("");
				}
			
			// if a class has changed, rebuild the whole query
			} else if (f.constraintType == "class") {
				// rebuilding the query took care of everything.  No constraints.
				true;
				
			// set filter constraints.
			} else {
				// op is in constraintType
				var op = f.constraintType;
				var fmt = new SparqlFormatter();
				var sparqlID = f.prop.sparqlID;
				
				var item = this.getFieldsConstrainItem(f, this.nodeGroup);
				if (! item ) {
					alert("Internal error in readFormConstraint(): can't find constraint item for: " + elementId);
					return null;
				}
				
				// build a filter constriant from element.value, or clear it out if the element is empty
				if (element.value && element.value != "") {
					this.filterDict[sparqlID][f.elementId] = item.buildFilterConstraint(op, element.value);
				} else {
					this.filterDict[sparqlID][f.elementId] = "";
				}
				
				// loop through elements that have filters for this sparqlID and put filters into a list
				// This is done every time (inefficient) because we don't know if this is the last element referencing this sparqlID
				var filterList = [];
				for (var e in this.filterDict[sparqlID]) {
					var c = this.filterDict[sparqlID][e];
					if (c != "") {
						filterList.push(c);
					}
				}
				
				// combine all the FILTERs we've found into one constraint and set it
				var constraint = fmt.combineFilterConstraints(filterList, "AND");
				item.setConstraints(constraint);  
				
			}
		},
		
		fieldChanged : function(elementId, optUpdateCountFlag) {
			// this is the callback for choice fields
			// It should also be called any time a choice is changed by code.
			//
			// sets proper filter constraints
			// if requested, runs the query and updates the count
			var updateCountFlag = (typeof optUpdateCountFlag === 'undefined') ? true : optUpdateCountFlag;
			var element = this.document.getElementById(elementId);
			var f = this.getFieldByElementId(elementId); // get the field from config
			
			
			// call the preQueryCallback before doing anything
			if (this.preQueryCallback) {
				this.preQueryCallback();
			}
			
			if (! f.hasOwnProperty("constraintType")) {
				alert("Config error found in getFieldsConstrainItem.  Field has no constraintType: " + f.name);
				return null;
			}
			
			// if element automatically clears others when it changes, do that now
			if (f.hasOwnProperty("clearsOnChange")) {
				this.choiceResetList(f.clearsOnChange);
			}
			
			this.enableDisableFields(element, f, updateCountFlag);
			this.buildNodeGroup();
			this.readFormConstraints();
			
			
			if (updateCountFlag) {
				// validate those elements that need post-validation
				if (f.hasOwnProperty("choiceMode") && f.choiceMode == "validate") {
					this.runCountQueryValidateElement(element);
					
					// note that in this case, the user's callback
					// can't be run because runCountQueryValidateElement is async
					// So, that function's callback will call the user's callback.
					
				} else if (f.hasOwnProperty("choiceMode") && f.choiceMode == "fixed") {
					this.runCountQueryValidateOthers(element);

				} else {
				
					this.updateAllChoices();
					
					this.runQuery();
					
					this.runCountQuery();
				}
			}	
		},
		
		callbackCountValidateQuery : function(res) {
			// check the count of a change that requres post-validation
			// either accept or reject
			// then updateAll others and display
			
			if (res != null && !res.isSuccess()) {
				alert("Internal error: count query failed: " + res.getStatusMessage());
				this.registerQueryReturn();
				return null;
			}
			
			var count = res.getRsData(0, 0,SparqlServerResult.prototype.NAMESPACE_NO);
			if (count == 0) {
				var elementId = res.getColumnName(0).replace(/_/g,'-');
				var f = this.getFieldByElementId(elementId);
				var element = this.document.getElementById(elementId);
				
				alert("No results match " + f.name + " of " + element.value + ".\nResetting field.");
				
				// Recovery
				this.choiceReset(element);                     // reset element
				this.fieldChanged(elementId, true);    // retry fieldChanged and hope for no infinite loop
				
			} else {
				
				// change is ok:  update others and display the count
				this.updateAllChoices();
				this.displayCount(count);
			}
			
			this.runQuery();
			this.registerQueryReturn();
		},
		

		runCountQueryValidateElement : function(element) {
			// run count query when element has just changed.
			// if count comes back zero, we'll want to alert and reset element
			
			var tmpNodeGroup = this.nodeGroup.deepCopy();
			
			// generate sparql
			this.addQueryReturns(tmpNodeGroup);   
			this.pruneNodeGroup(tmpNodeGroup);
			var sparql = tmpNodeGroup.generateSparql(SemanticNodeGroup.QUERY_COUNT, false, 0, null);
			
			// PEC TODO: very crude change of column header to pass element.id along to callback
			var suggestedID = "?" + element.id.replace(/-/g,'_');
			sparql = sparql.replace("?count", suggestedID);
			
			HtmlFormGroup.cbThis = this;    // PEC TODO async danger
			
			this.registerQueryCall();
			if(this.getFlag){
				this.conn.getDataInterface().executeAndParseGet(sparql, (function(res){this.callbackCountValidateQuery(res);}).bind(this) );
			}
			else{
				this.conn.getDataInterface().executeAndParse(sparql, (function(res){this.callbackCountValidateQuery(res);}).bind(this) );
					
			}
		},
		
		callbackCountValidateOthers : function(res) {
			// check the count of a change that requres post-validation
			// if zero, reset all other elements
			
			if (res != null && !res.isSuccess()) {
				alert("Internal error: count query failed: " + res.getStatusMessage());
				this.registerQueryReturn();
				return null;
			}
			
			var count = res.getRsData(0, 0,SparqlServerResult.prototype.NAMESPACE_NO);
			if (count == 0) {
				var elementId = res.getColumnName(0).replace(/_/g,'-');
				var f = this.getFieldByElementId(elementId);
				var element = this.document.getElementById(elementId);
				
				// reset all other elements 
				for (var i=0; i < this.fields.length; i++) {
					if (this.fields[i].hasOwnProperty("elementId") && this.fields[i].elementId != elementId) {
						var other = this.document.getElementById(this.fields[i].elementId);
						this.choiceReset(other);
					}        
				}
				this.fieldChanged(elementId, true);    // retry fieldChanged and hope for no infinite loop
				
			} else {
				
				// change is ok:  update others and display the count
				this.updateAllChoices();
				this.displayCount(count);
			}
			
			this.runQuery();
			this.registerQueryReturn();
		},
		
		runCountQueryValidateOthers : function(element) {
			// run count query when element has just changed.
			// if count comes back zero, we'll want to reset all other elements
			
			var tmpNodeGroup = this.nodeGroup.deepCopy();
			
			// generate sparql
			this.addQueryReturns(tmpNodeGroup);   
			this.pruneNodeGroup(tmpNodeGroup);
			var sparql = tmpNodeGroup.generateSparql(SemanticNodeGroup.QUERY_COUNT, false, 0, null);
			
			// PEC TODO: very crude change of column header to pass element.id along to callback
			var suggestedID = "?" + element.id.replace(/-/g,'_');
			sparql = sparql.replace("?count", suggestedID);
			
			this.registerQueryCall();
			if(this.getFlag){
				this.conn.getDataInterface().executeAndParseGet(sparql, (function(res){this.callbackCountValidateOthers(res);}).bind(this));
			}
			else{
				this.conn.getDataInterface().executeAndParse(sparql, (function(res){this.callbackCountValidateOthers(res);}).bind(this));
					
			}
		},
		
		// if count query succeeded, display the count.  if failed, display error.
		callbackCountQuery : function(res) {
			
			// check results of this link in the chain
			if (res != null && !res.isSuccess()) {
				alert("Internal error: count query failed: " + res.getStatusMessage());
				this.registerQueryReturn();
				return null;
			}
			var count = res.getRsData(0, 0,SparqlServerResult.prototype.NAMESPACE_NO);
			this.displayCount(count);
			this.registerQueryReturn();
		},
		
		// display the count 
		displayCount : function(count) {
			this.document.getElementById("count-div").innerHTML = count;
		},
		
		// count the number of files matching the current constraints
		runCountQuery : function() {
			this.displayCount("***");
			
			var tmpNodeGroup = this.nodeGroup.deepCopy();
			this.addQueryReturns(tmpNodeGroup);   
			this.pruneNodeGroup(tmpNodeGroup);
			var sparql = tmpNodeGroup.generateSparql(SemanticNodeGroup.QUERY_COUNT, false, 0, null);

			this.registerQueryCall();
			if(this.getFlag){
				this.conn.getDataInterface().executeAndParseGet(sparql, (function(res){this.callbackCountQuery(res);}).bind(this));
			}
			else{
				this.conn.getDataInterface().executeAndParse(sparql, (function(res){this.callbackCountQuery(res);}).bind(this));
						
			}
		},
		
		pruneNodeGroup : function(nodeGroup) {
			// take off any unused nodes unless they are in this.query.dontPrune
			
			// loop through all dontPrune:  save the returned state, then set it to True so it won't be pruned
			var saveRet = [];
			for (var i=0; i < this.query.dontPrune.length; i++) {
				var uri = this.query.dontPrune[i];
				var snode = this.getOneNodeBySuperclassURI(nodeGroup, uri, this.oInfo, "pruneNodeGroup():A");
				saveRet[i] = snode.getIsReturned();
				snode.setIsReturned(true);
			}
			
			nodeGroup.pruneAllUnused();
			
			// loop through all dontPrune:  re-set isReturned to its original state
			var saveRet = [];
			for (var i=0; i < this.query.dontPrune.length; i++) {
				var uri = this.query.dontPrune[i];
				var snode = this.getOneNodeBySuperclassURI(nodeGroup, uri, this.oInfo, "pruneNodeGroup():B");
				snode.setIsReturned(saveRet[i]);
			}
		},
		
		fieldsAllEmpty : function (fieldList) {
			for (var i=0; i < fieldList.length; i++) {
				var element = this.document.getElementById(fieldList[i].elementId);
				if (! this.choiceIsEmpty(element)) {
					return false;
				}
			}
			return true;
		},
		
		getFieldByElementId : function(elementId) {
			// get field given elementId
			for (var i=0; i < this.fields.length; i++) {
				if (this.fields[i].hasOwnProperty("elementId") && this.fields[i].elementId == elementId) {
					return this.fields[i];
				}
			}
			alert("Internal error in getFieldByElementId: elementID is missing from config: " + elementId);
			return null;
		},
		
		getFieldsByClassURI : function(uri) {
			// get fields given classURI
			var ret = [];
			for (var i=0; i < this.fields.length; i++) {
				if (this.fields[i].hasOwnProperty("classURI") && this.fields[i].classURI == uri) {
					ret.push(this.fields[i]);
				}
			}
			return ret;
		},
		
		getFieldClassURI : function (f) {
			if (! f.hasOwnProperty("classURI")) {
				alert("Internal error in getFieldClassURI(): object has no classURI");
				return null;
			}
			// get the uri
			var uri = f.classURI;
			
			// if "elementValue", then get the classURI from the value of an element
			if (uri.indexOf("elementValue:") > -1) {
				var field = uri.split(":");
				var elementId = field[1];
				
				var element = this.document.getElementById(elementId);
				if (!element) {
					alert("Internal error in getFieldClassURI(): illegal field classURI: " + uri + ".  Can't find html element: " + elementId);
					return null;
				}
				uri = element.value;
			} 

			return uri;
		},
		
		getFieldsConstrainItem : function(f, nodeGroup) {
			// retrieve from nodeGroup the snode or snode.property item that is constrained by a member of this.fields
			// or null
			
			var uri = this.getFieldClassURI(f);
			var item = null;
			
			// find property item
			if (f.hasOwnProperty("prop")) {
				var snode = this.getOneNodeBySuperclassURI(nodeGroup, uri, this.oInfo, "getFieldsConstrainItem()");
				item = snode.getPropertyByKeyname(f.prop.keyName);
			
			// find node item
			} else if (f.hasOwnProperty("node")) {
				item = nodeGroup.getNodeBySparqlID(f.node.sparqlID);
				
			} else if (f.hasOwnProperty("constraintType") && f.constraintType === "class") {
				var uri = this.getFieldClassURI(f);
				var snode = this.getOneNodeBySuperclassURI(nodeGroup, uri, this.oInfo, "getFieldsConstrainItem()");

				item = snode;
			}
			
			return item;
		},
		
		getOneNodeBySuperclassURI : function(nodeGroup, uri, oInfo, callerStr) {
			// get the one node whose superclass is uri.  Handle errors.
			var snodes = nodeGroup.getNodesBySuperclassURI(uri, oInfo);
			if (snodes.length != 1) {
				alert("Internal error in " + callerStr + ": Expecting 1 but found " + snodes.length + " node(s) with superclass URI=" + uri);
				throw "Internal error";
				return null;
			} else {
				return snodes[0];
			}
		},
		
		
	};	
		