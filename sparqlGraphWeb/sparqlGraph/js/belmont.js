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

var SparqlUtil = function() {
	// only static functions
};

SparqlUtil.guid = function () {
  // from http://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript

  function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
      .toString(16)
      .substring(1);
  }
  return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
    s4() + '-' + s4() + s4() + s4();
};

var SparqlFormatter = function() {
	this.PAT_ILLEGAL_ID_CHAR = "[^A-Za-z_0-9]";
	this.ID_TAG = "%id";
};

SparqlFormatter.prototype = {
	legalizeSparqlID : function(suggestion) {
		// replace illegal characters with "_"
		var re = new RegExp(this.PAT_ILLEGAL_ID_CHAR, "g");
		var ret = suggestion.replace(re, "_");

		// make sure it starts with "?"
		if (ret.indexOf("?") != 0)
			ret = "?" + ret;

		return ret;
	},
	legalizePrefixName : function(suggestion) {
		// replace illegal characters with "_"
		
		var re = new RegExp(this.PAT_ILLEGAL_ID_CHAR, "g");
		var ret = suggestion.replace(re, "_");

		// make sure first character is a letter
		if (ret.match(/^[a-zA-Z]/) == null) {
			ret = "a" + ret;
		}
		return ret;
	},
	
	sparqlSafe : function(str) {
		// return a version of a string that can be used as a string in SPARQL
		
	    return String(str).replace(/"/g, '\\"').replace(/\n/g, '\\n').replace(/\r/g, '\\n');
	},
	
	tagSparqlID : function(sparql, id) {
		var ret = sparql;  
		
		if (ret !== "" && id !== "") {
			// search for id with a illegal char afterwards, so we don't match part
			// of a longer id
			var re = new RegExp("(\\" + id + ")(" + this.PAT_ILLEGAL_ID_CHAR + ")", "g");
			ret = ret.replace(re, this.ID_TAG + "$2");
		}
		return ret;
	},

	untagSparqlID : function(str, id) {
		var ret = str;
		var re = new RegExp(this.ID_TAG, "g");
		ret = ret.replace(re, id);
		return ret;
	},
	genSparqlID : function(suggestion, optionalReservedNameHash) {
		// counter with a suggestion that is legal and has frequent attribute
		// prefixes removed.
		// if optionalReservedNameHash: make sure returned name is not already
		// in optionalReservedNameHash
		// Return name is not reserved.  It is still a s

		// first catch any garbage and return ""
		if (suggestion == "" || suggestion == "?") {
			return "";
		}

		var ret = suggestion;

		// take off the '?' if any. It will be added later
		if (ret[0] == '?') {
			ret = ret.slice(1);
		}

		// remove known prefixes
		if (ret.indexOf("has") == 0)
			ret = ret.slice(3);
		else if (ret.indexOf("is") == 0)
			ret = ret.slice(2);

		// now remove leading underscore
		if (ret.indexOf("_") == 0)
			ret = ret.slice(1);

		// add question mark and replace illegal characters with "_"
		ret = this.legalizeSparqlID(ret);

		// find a name that isn't in the optionalReservedNameHash
		if (optionalReservedNameHash && ret in optionalReservedNameHash) {
			var i = 0;
			// strip of any existing _Number at the end
			ret = ret.replace(/_\d+$/, "");

			// search for a number that makes ret_Number legal
			while ((ret + "_" + i.toString()) in optionalReservedNameHash) {
				i++;
			}
			ret = ret + "_" + i.toString();
		}

		return ret;
	},

	prefixQuery : function(query) {
		console.log("Using DEPRECATED function sparqlFormatter.prefixQuery()");
		// look for URI's in a query and build prefixes
		// return an equivalent query that is easier to read
		var n;
		var prefixes = "";
		var query1 = "";

		// shuffle the query lines into two piles: prefixes & query1
		// depending on whether or not the line contains "prefix" (also keeps
		// "Error" lines in prefixes)
		// this will move prefixes to the front of the query and stop them from
		// being re-prefixed
		var lines = query.split('\n');
		for (var i = 0; i < lines.length; i++) {
			if (lines[i].match(/^prefix |^#Error/)) {
				prefixes += lines[i] + '\n';
			} else {
				query1 += lines[i] + '\n';
			}
		}

		// search for <prefix0/prefix1#val>
		var re = new RegExp("<(([^#<]+)\/([-_A-Za-z0-9]+))#([^<>]+)>");
		while ((n = query1.match(re)) != null) {
			var prefixAll = n[1];
			var prefix0 = n[2];
			var prefix1 = n[3];
			var prefixName = this.legalizePrefixName(n[3]);

			prefixes = prefixes + "prefix " + prefixName + ":<" + prefix0 + "/"
					+ prefix1 + "#>\n";

			var re2 = new RegExp("<" + prefixAll + "#([^>]+)>", "g");
			var replacement = prefixName + ":$1";
			var last = query1;
			query1 = query1.replace(re2, replacement);
			
			if (last == query1) {
				console.log("internal error in prefixQuery.  Failed to replace string in query.  \nString:" + re2 + "\nQuery: " + last);
				break;
			}
		}
		return prefixes + '\n' + query1;
	},
	
	buildValueConstraint : function(item, valList) {
		// build a value constraint for an "item" (see item interface comment)
		var ret = "";
		if (valList.length > 0) {
			ret = "VALUES " + item.getSparqlID() + " {";
			
			for (var i=0; i < valList.length; i++) {
				if (item.getValueType() == "uri") {
					ret += " <" + valList[i] + ">";
				} else {
					ret += " '" + valList[i] + "'^^" + SemanticNodeGroup.XMLSCHEMA_PREFIX + item.getValueType();
				}
			}
			
			ret += " } ";
		}
		return ret;
	},
	
	buildFilterConstraint : function(item, op, val) {
		// generates a FILTER constraint
		// item:  Does basic checking on types, passing through unknown types with an unlikely best guess
		// op:    Operator
		//        Defaults to '='
		// val:   Minimum attempt to cast val to a number or integer when appropriate
		//        If val == null then use a sample legal value

		var v = val;
		var itemType = item.getValueType();
		
		if (itemType == "string") {
			if (!v) v = "example";
			if (!op) op = "regex";
			if (op !== 'regex') alert("Internal error buildFilterConstraint(): string FILTER op must be 'regex'");
			ret = "FILTER regex(" + item.getSparqlID() + ', "' + v + '")';
			
		} else if (itemType == "int" || itemType == "long") {
			if (!v) v = 1;
			if (!op) op = "=";

			ret = 'FILTER (' + item.getSparqlID() + ' ' + op + " " + Math.floor(v) + ")";
		}
		else if (itemType == "float") {
			if (!v) v = 1.5;
			if (!op) op = "=";

			ret = 'FILTER (' + item.getSparqlID() + ' ' + op + " " + Number(v) + ")";
		}
		else if (itemType == "uri") {
			alert("Internal error in modalconstraintdialog.filter(): " + item.getSparqlID() + " is a URI.\nUse Value constraint instead of filter constraint.");
			return;
			
		} else if (itemType == "date") {
			if (!v) v = '1/21/2003';
			if (!op) op = "=";

			ret = 'FILTER (' + item.getSparqlID() + ' ' + op + " '" + v + SemanticNodeGroup.XMLSCHEMA_PREFIX + "date)";
			
		} else if (itemType == "dateTime") {
			if (!v) v = '2011-12-03T10:15:30';
			if (!op) op = "=";

			ret = 'FILTER (' + item.getSparqlID() + ' ' + op + " '" + v + SemanticNodeGroup.XMLSCHEMA_PREFIX + "dateTime)";
			
		} else{
			if (!v) v = 'something';
			if (!op) op = "=";

			ret = 'FILTER (' + item.getSparqlID() + ' ' + op + " " + v + ")";
		}
		return ret;
	},
	
	combineFilterConstraints : function(filterStrList, op) {
		// combine a bunch of filter constraints with AND or OR
		// op should be "and" or "or"
		if (filterStrList.length == 0) {
			return "";
		}
		else if (filterStrList.length == 1) {
			return filterStrList[0];
		}
		else {
			if (op.search(/and/i) > -1) oper = " && ";
			else if (op.search(/or/i) > -1) oper = " || ";
			else {
				alert("Internal error in SparqlFormatter(): illegal op param should be 'and' or 'or': " + op);
				return;
			}
			var ret = ' FILTER ( ' + filterStrList[0].replace("FILTER", "");
			for (var i=1; i < filterStrList.length; i++) {
				ret += oper + filterStrList[i].replace("FILTER", "");
			}
			ret += ')';
			return ret;
		}
	},
};

/* the node item */
var NodeItem = function(nome, val, uriVal, jObj, nodeGroup) { // used for
																// keeping track
																// of details
																// relating two
																// semantic
																// nodes
	if (jObj) {
		this.fromJson(jObj, nodeGroup);
	} else {
		this.SNodes = [];    // the semantic nodes, if one exists currently,
							// represented by this NodeItem.
		this.SNodeOptionals = [];
		this.KeyName = nome; // the name used to identify this node item
		this.ValueType = val; // the type given for the linked (linkable?) node
								
		this.UriValueType = uriVal; // full name of val
		this.ConnectBy = ''; // the connection link, such as "hasSessionCode"
		this.Connected = false; // toggled if a connection between this node and
								// the Semantic node owning the NodeItem list are linked.
		this.UriConnectBy = '';
		this.deletionFlags = [];
	}
};

//deprecating
NodeItem.OPTIONAL_FALSE = 0;
NodeItem.OPTIONAL_TRUE = 1;       // everything "downstream" of this nodeItem is optional
NodeItem.OPTIONAL_REVERSE = -1;   // everything "upstream"   of this nodeItem is optional

// the functions used by the node item to keep things in order
NodeItem.prototype = {
	toJson : function() {
		// return a JSON object of things needed to serialize
		var ret = {
			SnodeSparqlIDs : [],
			SnodeOptionals : [],
			DeletionMarkers : [],
			KeyName : this.KeyName,
			ValueType : this.ValueType,
			UriValueType : this.UriValueType,
			ConnectBy : this.ConnectBy,
			Connected : this.Connected,
			UriConnectBy : this.UriConnectBy,
			
		};
		for (var i=0; i < this.SNodes.length; i++) {
			ret.SnodeSparqlIDs.push(this.SNodes[i].getSparqlID());
			ret.SnodeOptionals.push(this.SNodeOptionals[i]);
			ret.DeletionMarkers.push(this.deletionFlags[i]);
		}
		
		return ret;
	},
	fromJson : function(jObj, nodeGroup) {
		// presumes that SNode references have already been created
		this.SNodes = [];
		for (var i = 0; i < jObj.SnodeSparqlIDs.length; i++) {
			var snode = nodeGroup.getNodeBySparqlID(jObj.SnodeSparqlIDs[i]);
			if (!snode) {
				alert("Assertion Failed in NodeItem.fromJson: "
						+ jObj.SnodeSparqlIDs[i]);
			}
			this.SNodes.push(snode);
		}
		
		// version 3:  SnodeOptionals
		this.SNodeOptionals = [];
		if (jObj.hasOwnProperty("SnodeOptionals")) {
			for (var i=0; i < jObj.SnodeOptionals.length; i++) {
				this.SNodeOptionals.push(jObj.SnodeOptionals[i]);
			}
		} else {
			var opt = NodeItem.OPTIONAL_FALSE;

			// backward compatibility
			if (jObj.hasOwnProperty("isOptional")) {
				var isOpt = jObj.isOptional;
				if (isOpt == true || isOpt == NodeItem.OPTIONAL_TRUE) { 
					opt = NodeItem.OPTIONAL_TRUE; 
				} else if (isOpt == NodeItem.OPTIONAL_REVERSE) { 
					opt = NodeItem.OPTIONAL_REVERSE; 
				}
			}
		
			for (var i=0; i < jObj.SnodeSparqlIDs.length; i++) {
				this.SNodeOptionals.push(opt);
			}
		}
		
		// version 5: add in the deletion flags
		this.deletionFlags = [];
		if(jObj.hasOwnProperty("DeletionMarkers")) {
			for (var i=0; i < jObj.DeletionMarkers.length; i++) {
				this.deletionFlags.push(jObj.DeletionMarkers[i]);
			}
		} else {
			// backward compatibility		
			for (var i=0; i < jObj.SnodeSparqlIDs.length; i++) {
				this.deletionFlags.push(false);
			}
		}
		
		this.KeyName = jObj.KeyName;
		this.ValueType = jObj.ValueType;
		this.UriValueType = jObj.UriValueType;
		this.ConnectBy = jObj.ConnectBy;
		this.Connected = jObj.Connected;
		this.UriConnectBy = jObj.UriConnectBy;
	},
	// set values used by the NodeItem.
	setKeyName : function(strName) {
		this.KeyName = strName;
	},
	setValueType : function(strValType) {
		this.ValueType = strValType;
	},
	setUriValueType : function(strUriValType) {
		this.UriValueType = strUriValType;
	},
	setConnected : function(connect) {
		this.Connected = connect;
	},
	setConnectBy : function(strConnName) {
		this.ConnectBy = strConnName;
	},
	setUriConnectBy : function(strConnName) {
		this.UriConnectBy = strConnName;
	},
	
	setSNodeOptional(snode, optional) {
		for (var i=0; i < this.SNodes.length; i++) {
			if (this.SNodes[i] == snode) {
				this.SNodeOptionals[i] = optional;
				return;
			}
		}
		throw new Error("NodeItem can't find link to semantic node");
	},
	
	getSNodeOptional(snode) {
		for (var i=0; i < this.SNodes.length; i++) {
			if (this.SNodes[i] == snode) {
				return this.SNodeOptionals[i] ;
			}
		}
		throw new Error("NodeItem can't find link to semantic node");
	},
	
	allOptionalReverse : function() {
		// Does node item make it's owner node optional
		//  1) connects to something
		//  2) all optional reverse
		if (this.SNodeOptionals.length == 0) {
			return false;
		}
		for (var i=0; i < this.SNodeOptionals.length; i++) {
			if (this.SNodeOptionals[i] != NodeItem.OPTIONAL_REVERSE) {
				return false;
			}
		}
		return true;
	},
	
	setSnodeDeletionMarker : function (snode, toDelete){
		for (var i=0; i < this.SNodes.length; i++) {
			if (this.SNodes[i] == snode) {
				this.deletionFlags[i] = toDelete;
				return;
			}
		}
		throw new Error("NodeItem can't find link to semantic node");
	},
	
	getSnodeDeletionMarker : function (snode){
		for (var i=0; i < this.SNodes.length; i++) {
			if (this.SNodes[i] == snode) {
				return this.deletionFlags[i] ;
			}
		}
		throw new Error("NodeItem can't find link to semantic node");
	},
	
	getConnectsTo : function (snode) {
		return (this.SNodes.indexOf(snode) > -1);
	},
	
	pushSNode : function(snode, optOptional, optDeletionFlag){
		this.SNodes.push(snode);
		this.SNodeOptionals.push((typeof optOptional === "undefined")     ? NodeItem.OPTIONAL_FALSE : optOptional );
		this.deletionFlags.push( (typeof optDeletionFlag === "undefined") ? false                   : optDeletionFlag);
	},

	// get values used by the NodeItem
	getKeyName : function() {
		return this.KeyName;
	},
	getValueType : function() {
		return this.ValueType;
	},
	getUriValueType : function() {
		return this.UriValueType;
	},
	getConnected : function() {
		return this.Connected;
	},
	getConnectBy : function() {
		return this.ConnectBy;
	},
	getSNodes : function() {
		return this.SNodes;
	},
	getURIConnectBy : function() {
		return this.UriConnectBy;
	},

	getDisplayOptions : function() {
		var bitmap = 0;
		return bitmap;
	},
	removeSNode : function(nd) {
		// iterate over the nodes, remove the one that makes no sense.
		// console.log("calling transitive node group removal from " +
		// this.Keyname);
		for (var i = 0; i < this.SNodes.length; i++) {
			if (this.SNodes[i] == nd) {
				// console.log("removing " + this.SNodes[i].getSparqlID);
				this.SNodes.splice(i, 1);
				this.SNodeOptionals.splice(i,1);
				this.deletionFlags.splice(i, 1);
			}
		}
		if (this.SNodes.length === 0) 
		{
			this.Connected = false;
		}

	},
	
	getSnodesWithDeletionFlagsEnabledOnThisNodeItem :function () {
		// iterate over the nodes and return the ones that have a
		// deletion set to true.
		var retval = [];
		
		for( var i = 0; i < this.SNodes.length; i++){
			var currNode = this.SNodes[i];
			if(this.deletionFlags[i]){
				retval.push(currNode);
			}
		}
		
		// ship it back
		return retval;
	},
	
	getItemType : function () {
		return "NodeItem";
	},
};

/* the property item */
var PropertyItem = function(keyname, valType, relationship, UriRelationship, jObj) { 
	
	if (jObj) {
		this.fromJson(jObj);
	} else {
		this.KeyName = keyname; // the name used to identify the property
		this.ValueType = valType; // the type of the value associated with
								// property in the ontology.
		this.relationship = relationship;
		this.UriRelationship = UriRelationship;
		this.Constraints = ''; // the constraints are represented as a str and
								// will be used in the
		// in the sparql generation.
		this.fullURIName = '';
		this.SparqlID = '';
		this.isReturned = false;
		this.isOptional = false;
		this.isRuntimeConstrained = false;
		this.instanceValues = [];
		this.isMarkedForDeletion = false;
	}
};
// the functions used by the property item to keep its stuff in order.
PropertyItem.prototype = {
	toJson : function() {
		// return a JSON object of things needed to serialize
		var ret = {
			KeyName : this.KeyName,
			ValueType : this.ValueType,
			relationship : this.relationship,
			UriRelationship : this.UriRelationship,
			Constraints : this.Constraints,
			fullURIName : this.fullURIName,
			SparqlID : this.SparqlID,
			isReturned : this.isReturned,
			isOptional : this.getIsOptional(),
			isRuntimeConstrained : this.getIsRuntimeConstrained(),
			instanceValues : this.instanceValues,
			isMarkedForDeletion : this.isMarkedForDeletion,
		};
		return ret;
	},
	fromJson : function(jObj) {
		// presumes SparqlID's in jObj are already reconciled with the nodeGroup
		this.KeyName = jObj.KeyName;
		this.ValueType = jObj.ValueType;
		this.relationship = jObj.relationship;
		this.UriRelationship = jObj.UriRelationship;
		this.Constraints = jObj.Constraints;
		this.fullURIName = jObj.fullURIName;
		this.SparqlID = jObj.SparqlID;
		this.isReturned = jObj.isReturned;
		this.isOptional = jObj.isOptional;
		this.isRuntimeConstrained = jObj.hasOwnProperty("isRuntimeConstrained") ? jObj.isRuntimeConstrained : false;
       
        
        if(jObj.instanceValues){
        	 this.instanceValues = jObj.instanceValues;
        }
        else{
        	this.instanceValues = [];
        }
        
        // check for the existance of the isMarkedForDeletion. if it exists, set it.
        this.isMarkedForDeletion = jObj.hasOwnProperty("isMarkedForDeletion") ? jObj.isMarkedForDeletion : false;
         
        
	},
	buildFilterConstraint : function(op, val) {
		//  build but don't set  a filter constraint from op and value
		f = new SparqlFormatter();
		return f.buildFilterConstraint(this, op, val);
	},
	buildValueConstraint : function(valueList) {
		//  build but don't set  a value constraint from a list of values (basic types or fully qualified URIs)
		f = new SparqlFormatter();
		return f.buildValueConstraint(this, valueList);
	},
	// set the values of items in the propertyItem
	setKeyName : function(keyname) {
		this.KeyName = keyname;
	},
	// set the insertion value. really, we should check the correct value type as we go.
	addInstanceValue : function(inval){
		this.instanceValues.push(inval);
	},
	getInstanceValues : function() {
		return this.instanceValues;
	},
	getInstanceValueAtIndex : function(index) {
		if(index > this.instanceValues.length){
			return null;
		}
		if(index < 0 ) { return null;}
		return this.instanceValues[index];
	}, 
	clearInstanceValues : function() {
		this.instanceValues = [];
	},
	setfullURIName : function(nome) {
		this.fullURIName = nome;
	},
	setValueType : function(typ) {
		this.ValueType = typ;
	},
	setSparqlID : function(id) {
		if (this.SparqlID != null && this.constraints != null) {
			this.constraints = this.constraints.replace(new RegExp('\\'+this.SparqlID+'\\b', 'g'), id);    
		}
		this.SparqlID = id;
	},
	setConstraints : function(con) {
		var f = new SparqlFormatter();

		this.Constraints = f.tagSparqlID(con, this.SparqlID);
	},
	setRelation : function(rel) {
		this.relationship = rel;
	},
	setIsReturned : function(val) {
		this.isReturned = val;
	},
	setIsOptional : function(bool) {
		this.isOptional = bool;
	},
	setIsRuntimeConstrained : function(bool) {
		this.isRuntimeConstrained = bool;
	},
	// return values from the propertyItem
	getfullURIName : function() {
		// return the name of the property
		return this.fullURIName;
	},
	getSparqlID : function() {
		return this.SparqlID;
	},

	getKeyName : function() {
		// return the name of the property
		return this.KeyName;
	},
	getRelation : function() {
		return this.relationship;
	},
	getUriRelation : function() {
		return this.UriRelationship;
	},
	getValueType : function() {
		// return the type, as determined by the ontology
		return this.ValueType;
	},
	getIsReturned : function() {
		return this.isReturned;
	},
	getIsOptional : function() {
		return this.isOptional;
	},
	getIsRuntimeConstrained : function() {
		// boolean for PropertyItems
		return this.isRuntimeConstrained;
	},
	getConstraints : function() {
		// return the string representing the constraints.
		var f = new SparqlFormatter();

		return f.untagSparqlID(this.Constraints, this.SparqlID);
	},
	hasConstraints : function() {
		// more efficent if you're only checking whether constraints exist.
		// Verbose though.
		if (this.Constraints) {
			return true;
		} else {
			return false;
		}
	},
	getDisplayOptions : function() {
		// give dracula_graph a list of binary display options
		var bitmap = 0;
		if (this.getIsReturned())
			bitmap += 1;
		if (this.hasConstraints())
			bitmap += 2;
		return bitmap;
	},
	
	setAndReserveSparqlID : function(retName) {
		// callback from setting the return name
		// caller must check that retName is legal and work it out with the GUI if
		// not.

		var curName = this.getSparqlID(); // current name w/o the "?"

		// if user entered an empty name, they don't want it returned anymore
		if (retName == "") {
			freeUnusedSparqlID(this);

			// else user entered a real name
		} else {
			// if name changed: free old one and grab new one
			if (retName != curName) {
				gNodeGroup.freeSparqlID(curName);
				gNodeGroup.reserveSparqlID(retName);
				this.setSparqlID(retName);
			}

		}
	},
	
	getItemType : function () {
		return "PropertyItem";
	},
	
	setIsMarkedForDeletion : function(markToSet) {
		this.isMarkedForDeletion = markToSet;
	},
	
	getIsMarkedForDeletion : function(){
		return this.isMarkedForDeletion;
	}
};

var freeUnusedSparqlID = function(item) {
	// set SparqlID to "" only if it is not returned or constrained
	// item could be SNode or PropItem
	var id = item.getSparqlID();
	if (id != "" && item.getIsReturned() == false
			&& item.hasConstraints() == false) {
		gNodeGroup.freeSparqlID(id);
		item.setSparqlID("");
	}
};


/* to set nodes */
var setNode = function(SNode) { // set up the node itself. this includes the
								// creation of the node via dracula
// var node = new Graph.Node(SNode.NodeName);
	var node = new Graph.Node(SNode.NodeName, SNode.getSparqlID());
	node.setPropLabels(SNode.propList);
	node.setNodeLabels(SNode.nodeList);
	node.setParent(SNode);

	return node;
};
/* we need an intermediate to the arrow generation */
var edgeIntermediate = function(source, target, relation) {
	this.src = source;
	this.tgt = target;
	this.rel = relation;
};

edgeIntermediate.prototype = {
	getSrc : function() {
		return this.src;
	},
	getTgt : function() {
		return this.tgt;
	},
	getRel : function() {	
		return this.rel;
	}
};

/* the semantic node */
var SemanticNode = function(nome, plist, nlist, fullName, subClassNames,
		nodeGroup, jObj, optInflateOInfo) {
	
	var inflateOInfo = (typeof optInflateOInfo === "undefined") ? null : optInflateOInfo;

	if (jObj) {
		this.fromJson(jObj, nodeGroup, inflateOInfo);
	} else {
		this.propList = plist.slice(); // a list of properties
		this.nodeList = nlist.slice(); // a list of the nodes that this can
										// link to.
		this.NodeName = nome; // a name to be used for the node.
		this.fullURIName = fullName; // full name of the class
		this.subClassNames = subClassNames.slice(); // full names of all
													// possible subclasses
		this.SparqlID = new SparqlFormatter().genSparqlID(nome,
				nodeGroup.sparqlNameHash); // always has SparqlID since it is
											// always included in the query
		this.isReturned = false;
		this.isRuntimeConstrained = false;
		this.valueConstraint = "";
		this.instanceValue = null;
		this.deletionMode = NodeDeletionTypes.NO_DELETE; 
	}
	this.node = setNode(this); // the dracula node used in this Semantic Node.
								// this is the thing that gets drawn
	this.removalTag = false;
	this.nodeGrp = nodeGroup; // a reference to the node group itself. this
								// will be used for deletions
	
	
};


// PEC  I don't know how to do "Interface" in javascript
// but there is an "interface" referred to as "item"
//
// Which strangely, has members:
//     SemanticNode
//     PropertyItem
//
// Methods:
//     getConstraints
//     getValueType
//  
//     hasConstraints
//

// the functions used by the SemanticNode in order to keep things in order.
SemanticNode.prototype = {

	toJson : function(optDeflateFlag, optMappedPropItems) {
		var deflateFlag = (typeof optDeflateFlag === "undefined") ? false : optDeflateFlag;
		var mappedPropItems = (typeof optMappedPropItems === "undefined") ? [] : optMappedPropItems;

		// return a JSON object of things needed to serialize
		var ret = {
			propList : [],
			nodeList : [],
			NodeName : this.NodeName,
			fullURIName : this.fullURIName,
			subClassNames : this.subClassNames.slice(),
			SparqlID : this.SparqlID,
			isReturned : this.isReturned,
			isRuntimeConstrained : this.isRuntimeConstrained,
			valueConstraint : this.valueConstraint,
			instanceValue : this.instanceValue,
			deletionMode :  getNodeDeletionTypeName(this.deletionMode),
		};
		
		// add properties
		for (var i = 0; i < this.propList.length; i++) {
			var p = this.propList[i];
			// if deflateFlag, then only add property if returned or constrained
			if (deflateFlag == false || p.getIsReturned() || p.getConstraints() != "" || mappedPropItems.indexOf(p) > -1) {
				ret.propList.push(p.toJson());
			}
		}
		
		// add nodes
		for (var i = 0; i < this.nodeList.length; i++) {
			// if we're deflating, only add connected nodes
			if (deflateFlag == false || this.nodeList[i].getConnected()) {
				ret.nodeList.push(this.nodeList[i].toJson());
			}
		}
		
		return ret;
	},
	
	fromJson : function(jObj, nodeGroup, optInflateOInfo) {
		
		var inflateOInfo = (typeof optInflateOInfo === "undefined") ? null : optInflateOInfo;

		// presumes SparqlID's are reconciled already
		// presumes that SNodes pointed to by NodeItems already exist
		this.propList = [], this.nodeList = [];
		this.NodeName = jObj.NodeName;
		this.fullURIName = jObj.fullURIName;
		this.subClassNames = jObj.subClassNames.slice();
		this.SparqlID = jObj.SparqlID;
		this.isReturned = jObj.isReturned;
		this.isRuntimeConstrained = jObj.hasOwnProperty("isRuntimeConstrained") ? jObj.isRuntimeConstrained : false;
		this.valueConstraint = jObj.valueConstraint;
		this.instanceValue = jObj.instanceValue;	
		this.deletionMode = jObj.hasOwnProperty("deletionMode") ? getNodeDeletionTypeByName(jObj.deletionMode) : NodeDeletionTypes.NO_DELETE;
		
		// load JSON properties as-is
		for (var i = 0; i < jObj.propList.length; i++) {
			var p = new PropertyItem(null, null, null, null, jObj.propList[i]);
			this.propList.push(p);
		}
	
		for (var i = 0; i < jObj.nodeList.length; i++) {
			var n = new NodeItem(null, null, null, jObj.nodeList[i], nodeGroup);
			this.nodeList.push(n);
		}
		
		if (inflateOInfo != null) {
			this.inflateAndValidate(inflateOInfo);
		}
		
	},
	
	/*
	 * Expand props to full set of properties for this classURI in oInfo 
	 * and validates all props
	 * Throws errors that user interface really wants.
	 */
	inflateAndValidate : function(oInfo) {
		var newProps = [];
		var newNodes = [];

		
		// build hash of suggested properties for this class
		var propItemHash = {};
		for (var p=0; p < this.propList.length; p++) {
			propItemHash[this.propList[p].getUriRelation()] = this.propList[p];
		}
		
		// build hash of suggested nodes for this class
		var nodeItemHash = {};
		for (var n=0; n < this.nodeList.length; n++) {
			nodeItemHash[this.nodeList[n].getKeyName()] = this.nodeList[n];
		}
		
		// get oInfo's version of the property list
		var ontClass = oInfo.getClass(this.fullURIName);
		if (ontClass == null) {
			throw "Class does not exist in the model: " + this.fullURIName;
		}
		var ontProps = oInfo.getInheritedProperties(ontClass);
		
		// loop through oInfo's version	
		for (var i=0; i < ontProps.length; i++) {
			var oProp = ontProps[i];
			var oPropURI = oProp.getNameStr();
			var oPropKeyname = oProp.getNameStr(true);
			
			// if ontology property is one of the prop parameters, then check it over
			if (oPropURI in propItemHash) {
				
				// has range changed
				var propItem = propItemHash[oPropURI];
				if (propItem.getRelation() != oProp.getRangeStr()) {
					throw this.getSparqlID() + " property " + oPropURI + " range of " + propItem.getRelation() + " doesn't match model range of " + oProp.getRangeStr();
				}
				
				// all is ok: add the propItem
				newProps.push(propItem);
				
				delete propItemHash[oPropURI];
				
			// else ontology property wasn't passed in.  AND its range is outside the model (it's a Property)  
		    // Inflate (create) it.
			} else if (!oInfo.containsClass(oProp.getRangeStr())) {
				
				var propItem = new PropertyItem(oProp.getNameStr(true), 
												oProp.getRangeStr(true),
												oProp.getRangeStr(false),
												oProp.getNameStr(false)
												);
				newProps.push(propItem);
				
		    // node, in hash
			} else if (oPropKeyname in nodeItemHash) {
				
				// regardless of connection, check range
				var nodeItem = nodeItemHash[oPropKeyname];
				var nRangeStr = nodeItem.getUriValueType();
				var nRangeAbbr = nodeItem.getValueType();
				if (nRangeStr != oProp.getRangeStr()) {
					throw this.getSparqlID() + " Node property " + oPropURI + " range of " + nRangeStr+ " doesn't match model range of " + oProp.getRangeStr();
				}
				if (nRangeAbbr != oProp.getRangeStr(true)) {
					throw this.getSparqlID() + " Node property " + oPropURI + " range abbreviation of " + nRangeAbbr + " doesn't match model range of " + oProp.getRangeStr(true);
				}
				
				// if connected 
				if (nodeItem.getConnected()) {
					
					// check full domain
					var nDomainStr = nodeItem.getURIConnectBy();
					if (nDomainStr != oProp.getNameStr()) {
						throw this.getSparqlID() + " Node property " + oPropURI + " domain of " + nDomainStr + " doesn't match model domain of " + oProp.getNameStr();
					}
					
					// check all connected snode classes
					var nRangeClass = oInfo.getClass(nRangeStr);
					
					var snodeList = nodeItem.getSNodes();
					for (var j=0; j < snodeList.length; j++) {
						var snodeURI = snodeList[j].getURI();
						var snodeClass = oInfo.getClass(snodeURI);
						
						if (snodeClass == null) {
							throw this.getSparqlID() + " Node property " + oPropURI + " is connected to node with class " + snodeURI + " which can't be found in model";
						}
						
						if (!oInfo.classIsA(snodeClass, nRangeClass)) {
							throw this.getSparqlID() + " Node property " + oPropURI + " is connected to node with class " + snodeURI + " which is not a type of " + nRangeStr + " in model";

						}
					}
				}
				// all is ok: add the propItem
				newNodes.push(nodeItem);
				
				delete nodeItemHash[oPropKeyname];
				
			// new node
			} else {
				var nodeItem = new NodeItem(oProp.getNameStr(true), 
											oProp.getRangeStr(true),
											oProp.getRangeStr(false)
											);
				newNodes.push(nodeItem);
			}
		}	
		
		if (Object.keys(propItemHash).length > 0) {
			throw this.getSparqlID() + " Property does not exist in the model: " + Object.keys(propItemHash);
		}
		if (Object.keys(nodeItemHash).length > 0) {
			throw this.getSparqlID() + " Node property does not exist in the model: " + Object.keys(nodeItemHash);
		}
		
		this.propList = newProps;
		this.nodeList = newNodes;
	},
	
	setInstanceValue : function(inval){
		this.instanceValue = inval;
	},
	getInstanceValue : function() {
		return this.instanceValue;
	},
	removeFromNodeList : function(nd) {
		// remove any links in this SNode's nodeList which point to nd
		for (var i = 0; i < this.nodeList.length; i++) {
			this.nodeList[i].removeSNode(nd);
		}
	},
	removeLink : function(nodeItem, targetSNode) {
		
		nodeItem.removeSNode(targetSNode);
		this.nodeGrp.graph.removeEdge(this.node, targetSNode.node);
	},
	
	buildFilterConstraint : function(op, val) {
		// build but don't set a filter constraint from op and value
		f = new SparqlFormatter();
		return f.buildFilterConstraint(this, op, val);
	},
	buildValueConstraint : function(valueList) {
		//  build but don't set  a value constraint from a list of values (basic types or fully qualified URIs)
		f = new SparqlFormatter();
		return f.buildValueConstraint(this, valueList);
	},
	setSparqlID : function(id) {
		if (this.SparqlID != null && this.constraints != null) {
			this.constraints = this.constraints.replace(new RegExp('\\'+this.SparqlID+'\\b', 'g'), id);    
		}
		this.SparqlID = id;
	},
	getSparqlID : function() {
		return this.SparqlID;
	},
	setIsReturned : function(val) {
		this.isReturned = val;
	},
	setIsRuntimeConstrained : function(val) {
		this.isRuntimeConstrained = val;
	},
	setValueConstraint : function(c) {
		this.valueConstraint = c;
	},
	getValueConstraint : function() {
		return this.valueConstraint;
	},
	getConstraints : function() {
		// makes this look like a propertyItem and its constraint
		return this.valueConstraint;
	},
	setConstraints : function(c) {
		this.valueConstraint = c;
	},
	getValueType : function() {
		// make this look like a propertyItem. Type is always "uri"
		return "uri";
	},
	getIsReturned : function() {
		return this.isReturned;
	},
	getIsRuntimeConstrained : function() {
		return this.isRuntimeConstrained;
	},
	setNodeName : function(nome) {
		this.NodeName = nome;
	},
	getNode : function() {
		return this.node;
	},
	getPropsForSparql : function(forceRet, queryType) {
		// return properties needed for a SPARQLquery

		// forceRet can be empty or a propItem to return regardless of whether
		// it is returned
		
		// queryType is going to be needed for the deletes. we need values labeled for deletion that may not have any 
		// other meaningful features about them. this support will be added later.
		
		var retprops = [];
		var t = this.propList.length;
		for (var s = 0; s < t; s++) {
			if (this.propList[s].getIsReturned()
					|| this.propList[s].getConstraints() != ''
					|| this.propList[s] == forceRet) {
				retprops.push(this.propList[s]);
			}
		}

		return retprops;
	},
	
	isUsed : function (optInstanceOnly) {
		var instanceOnly = (typeof optInstanceOnly === "undefined") ? false : optInstanceOnly;

		if (instanceOnly) {
			if (this.instanceValue != null) return true;
			
			for (var i = 0; i < this.propList.length; i++) {
				if (this.propList[i].instanceValues.length > 0) {
					return true;
				}
			}
		} else {
			// does this node or its properties have any constrants or isReturned()
			if (this.getIsReturned() || this.hasConstraints() || this.instanceValue != null) return true;
			
			for (var i = 0; i < this.propList.length; i++) {
				if (this.propList[i].getIsReturned() || this.propList[i].hasConstraints() || this.propList[i].instanceValues.length > 0) {
					return true;
				}
			}
		}
		return false;
	},
	
	countReturns : function () {
		var ret = this.getIsReturned() ? 1 : 0;
		
		for (var i = 0; i < this.propList.length; i++) {
			ret += (this.propList[i].getIsReturned() ? 1 : 0);
		}
		
		return ret;
	},
	
	getConstrainedPropertyItems: function () {
        var retprops = [];
        var t = this.propList.length;
        for (var s = 0; s < t; s++) {
            if (this.propList[s].getConstraints() != "") {
                retprops.push(this.propList[s]);
            }
        }
        return retprops;
    },

	getReturnedPropertyItems : function() {
		var retprops = [];
		var t = this.propList.length;
		for (var s = 0; s < t; s++) {
			if (this.propList[s].getIsReturned()) {
				retprops.push(this.propList[s]);
			}
		}
		return retprops;
	},
	
	getReturnedCount : function() {
		var ret = 0;
		if (this.getIsReturned()) {
			ret += 1;
		}
		for (var i=0; i < this.propList.length; i++) {
			if (this.propList[i].getIsReturned()) {
				ret += 1;
			}
		}
		return ret;
	},
    getConstrainedPropertyItems: function () {
        var retprops = [];
        var t = this.propList.length;
        for (var s = 0; s < t; s++) {
            if (this.propList[s].getConstraints() != "") {
                retprops.push(this.propList[s]);
            }
        }
        return retprops;
    },
	getReturnedPropsFullURI : function() {
		var retprops = [];
		var t = this.propList.length;
		for (var s = 0; s < t; s++) {
			if (this.propList[s].getIsReturned()) {

				retprops.push("<" + this.propList[s].getfullURIName() + ">");
			}
		}

		return retprops;
	},
	getReturnedPropsReturnNames : function() {
		var retprops = [];
		var t = this.propList.length;
		for (var s = 0; s < t; s++) {
			if (this.propList[s].getIsReturned()) {
				retprops.push(this.propList[s].getSparqlID());
			}
		}

		return retprops;
	},

	getSparqlIDList : function() {
		// list all sparqlID's in use by this node
		var ret = [];
		ret.push(this.getSparqlID());
		for (var i = 0; i < this.propList.length; i++) {
			var s = this.propList[i].getSparqlID();
			if (s != null && s != "") {
				ret.push(s);
			}
		}
		return ret;
	},

	getURI : function(optLocalFlag) {
		var localFlag = (typeof(optLocalFlag) == 'undefined') ? false : optLocalFlag;
		
		if (localFlag) return new OntologyName(this.fullURIName).getLocalName();
		else           return this.fullURIName;
	},
	
	getConnections : function(connList) {
		// return a list of connections as edges.
		var k = this.nodeList.length;
		var ret = [];

		for (var i = 0; i < k; i++) {
			var nd = this.nodeList[i];
			if (nd.getConnected()) {
				// changing to pass node, not ID
				// now that we support N number connections per locus, this will
				// have to be done as a loop.

				var nodeList = this.nodeList[i].getSNodes(); // get the nodes
															// connecting to
															// this locus
				for (var d = 0; d < nodeList.length; d++) {

					var edge = new edgeIntermediate(this, nodeList[d],
							this.nodeList[i].getConnectBy());
					// console.log("node item returned edge: " + this.SparqlID +
					// " --> " + this.nodeList[i].getConnectBy() + " --> " +
					// nodeConn[d].getSparqlID() );
					ret.push(edge);
				}
			}

		}
		return ret;
	},
	
	hasConstraints : function() {
		if (this.valueConstraint != "") {
			return true;
		} else {
			return false;
		}
	},
	
	checkConnectedTo : function(other) {
		// does this node connect to other

		for (var i = 0; i < this.nodeList.length; i++) {
			nd = this.nodeList[i];
			if (nd.getConnected()) {
				var nodeList = this.nodeList[i].getSNodes();
				for (var d = 0; d < nodeList.length; d++) {
					if (nodeList[d].getSparqlID() == other.getSparqlID()) {
						return true;
					}
				}
			}
		}
		return false;
	},

	getConnectingNodeItems : function(other) {
		// get the node items that connects this snode to other, or []
		ret = [];
		for (var i=0; i < this.nodeList.length; i++) {
			var nItem = this.nodeList[i];
			if (nItem.getConnected()) {
				// check all of the nodes that may be connected to that locus.
				var nodeList = this.nodeList[i].getSNodes(); // get the nodes this item connects to
															
				for (var d = 0; d < nodeList.length; d++) {
					if (nodeList[d].getSparqlID() == other.getSparqlID()) {
						ret.push(nItem);
					}
				}
			}
		}
		return ret;
	},

	getConnectedNodesForSparql : function() {
		// return a list of connections as edges.
		var k = this.nodeList.length;
		var retval = [];

		for (var i = 0; i < k; i++) {
			nd = this.nodeList[i];
			if (nd.getConnected()) {
				var nodeConn = this.nodeList[i].getSNodes(); // get the nodes
															// connecting to
															// this locus
				for (var d = 0; d < nodeConn.length; d++) {
					var nxt = [ nd.getURIConnectBy(), nodeConn[d].getSparqlID() ];
					retval.push(nxt);
				}
			}
		}
		return retval;
	},

	getConnectedNodes : function() {
		// return a list of connections as SNodes.
		var k = this.nodeList.length;
		var retval = [];

		for (var i = 0; i < k; i++) {
			nd = this.nodeList[i];
			if (nd.getConnected()) {
				var nodeConn = this.nodeList[i].getSNodes(); // get the nodes
															// connecting to
															// this locus
				for (var d = 0; d < nodeConn.length; d++) {
					retval.push(nodeConn[d]);
				}
			}
		}
		return retval;
	},

	setConnection : function(otherNode, connectionUri, optOptional) {
		// set the connection between the desired nodes.

		connectionLocal = new OntologyName(connectionUri).getLocalName();
		
		for (var i = 0; i < this.nodeList.length; i++) {
			var nItem = this.nodeList[i];

			if (nItem.getKeyName() == connectionLocal) {
				nItem.setConnected(true);
				nItem.setConnectBy(connectionLocal);
				nItem.setUriConnectBy(connectionUri);

				nItem.pushSNode(otherNode, optOptional);
				return nItem;
			}
		}
		throw new Error("Internal error in SemanticNode.setConnection().  Couldn't find node item connection: " + this.getSparqlID() + "->" + connectionUri);
	},
	setPList : function(lst) {
		this.propList = lst;
		this.node.setPropLabels(this.propList);
	},
	setNList : function(lst) {
		this.nodeList = lst;
		this.node.setNodeLabels(this.nodeList);
		// console.log("set node list for " + this.NodeName + ". the list size
		// was " + lst.length);
	},
	getPropertyItem : function(i) {
		return this.propList[i];
	},
	getPropertyByKeyname : function(keyname) {
		for (var i = 0; i < this.propList.length; i++) {
			if (this.propList[i].getKeyName() == keyname) {
				return this.propList[i];
			}
		}
		return null;
	},
	getPropertyByURIRelation : function(uriRel) {
		for (var i = 0; i < this.propList.length; i++) {
			if (this.propList[i].getUriRelation() == uriRel) {
				return this.propList[i];
			}
		}
		return null;
	},
	getNodeItemByKeyname : function(keyname) {
		for (var i = 0; i < this.nodeList.length; i++) {
			if (this.nodeList[i].getKeyName() == keyname) {
				return this.nodeList[i];
			}
		}
		return null;
	},
	getNodeItemByURIConnectBy : function(URIConnectedBy) {
		for (var i = 0; i < this.nodeList.length; i++) {
			if (this.nodeList[i].getURIConnectBy() == URIConnectedBy) {
				return this.nodeList[i];
			}
		}
		return null;
	},
    
    // look anywhere for something with sparqlID
    getItemBySparqlID : function(sparqlID) {
        // check self
        if (this.getSparqlID() == sparqlID) {
            return this;
        }
        // only property items have sparqlID's.
		for (var i = 0; i < this.propList.length; i++) {
			if (this.propList[i].getSparqlID() == sparqlID) {
				return this.propList[i];
			}
		}
		return null;
    },
    
	getNodeItem : function(i) {
		return this.nodeList[i];
	},
	getNodeList : function() {
		return this.nodeList;
	},
	getNodeName : function() {
		return this.NodeName;
	},
	ownsNodeItem : function (nodeItem) {
		return this.nodeList.indexOf(nodeItem) > -1;
	},

	callAsyncPropEditor : function (propKeyname, draculaLabel) {
		var propItem = this.getPropertyByKeyname(propKeyname);
		this.nodeGrp.asyncPropEditor(propItem, draculaLabel);
	},
	callAsyncSNodeEditor : function (draculaLabel) {
		this.nodeGrp.asyncSNodeEditor(this, draculaLabel);
	},
    callAsyncSNodeRemover : function () {
        this.nodeGrp.asyncSNodeRemover(this);
    },
	callAsyncNodeEditor : function (nodeKeyname, draculaLabel) {
		var nodeItem = this.getNodeItemByKeyname(nodeKeyname);
		this.nodeGrp.asyncNodeEditor(nodeItem, draculaLabel);
	},
	callAsyncLinkBuilder : function(nItemIndex) {
		var nItem = this.nodeList[nItemIndex];
		this.nodeGrp.asyncLinkBuilder(this, nItem);
	},
	callAsyncLinkEditor : function(nodeKeyname, targetSNode, edge) {
		var nItem = this.getNodeItemByKeyname(nodeKeyname);
		this.nodeGrp.asyncLinkEditor(this, nItem, targetSNode, edge);
	},

	toggleReturnType : function(lt) {
		// synchronous since we're only using alert()
		if (this.isReturned) {
			alert("Name will no longer be returned");
			this.setIsReturned(false);
		} else {
			alert("Instance name will be returned in query as: "
					+ this.getSparqlID());
			this.setIsReturned(true);
		}

		displayLabelOptions(lt, this.getDisplayOptions());
	},
	removeFromNodeGroup : function(recurse) {
		// simply call the deletion method of the node group itself.
		// pass in the current node and the recursion value.
		// console.log("called removal. reached node.");
		this.nodeGrp.deleteNode(this, recurse);
	},

	setRemovalTag : function() {
		this.removalTag = true;
	},
	getRemovalTag : function() {
		return this.removalTag;
	},
	getDisplayOptions : function() {
		var bitmap = 0;
		if (this.getIsReturned())
			bitmap += 1;
		if (this.hasConstraints())
			bitmap += 2;
		return bitmap;
	},
	getEdgeDisplayOptions : function(nodeKeyname, targetSNode) {
		var nItem = this.getNodeItemByKeyname(nodeKeyname);
		var opt = nItem.getSNodeOptional(targetSNode);
		
		if (opt == 0) { return 0;}
		else if (opt == 1) { return 1;}
		else if (opt == -1) { return 2;}

	},
	
	// TODO: Justin plumb in additional details about where
	//       these properties came from
	addNonDomainProperty : function (keyname, valType, relation, uriRelation ) {
		// force-add a property that isn't in the domain
		prop = new PropertyItem(keyname, valType, relation, uriRelation);
		this.propList.push(prop);
		return prop;
	},
	
	addSubclassProperty : function (keyname, valType, relation, uriRelation ) {
		// force-add a subclass property.   So it must be optional.
		prop = new PropertyItem(keyname, valType, relation, uriRelation);
		prop.isOptional = true;
		this.propList.push(prop);
		return prop;
	},

	// returns the numeric constants
	getDeletionMode : function () {
		return this.deletionMode;
	},
	
	// uses the numeric constants
	setDeletionMode : function (nodeDeletionType) {
		this.deletionMode = nodeDeletionType;
	},
	
	getItemType : function () {
		return "SemanticNode";
	},
};

/* Node Deletion types */
var NodeDeletionTypes = {
	NO_DELETE : 0,
	TYPE_INFO_ONLY : 1,
	FULL_DELETE : 2,
	LIMITED_TO_NODEGROUP : 3,
	LIMITED_TO_MODEL : 4,
};

var getNodeDeletionTypeName = function(delVal){
	
	if(delVal === NodeDeletionTypes.NO_DELETE)				{ return "NO_DELETE";}
	if(delVal === NodeDeletionTypes.TYPE_INFO_ONLY)			{ return "TYPE_INFO_ONLY";}
	if(delVal === NodeDeletionTypes.FULL_DELETE)		    	{ return "FULL_DELETE";}
	if(delVal === NodeDeletionTypes.LIMITED_TO_NODEGROUP)	{ return "LIMITED_TO_NODEGROUP";}
	if(delVal === NodeDeletionTypes.LIMITED_TO_MODEL)		{ return "LIMITED_TO_MODEL";}
	
	// did not find it.
	throw new Error("No Deletion Type exists for " + delVal);
};

var getNodeDeletionTypeByName = function(delVal){
	
	if(delVal === "NO_DELETE")				{ return 0;}
	if(delVal === "TYPE_INFO_ONLY")			{ return 1;}
	if(delVal === "FULL_DELETE")  			{ return 2;}
	if(delVal === "LIMITED_TO_NODEGROUP")	{ return 3;}
	if(delVal === "LIMITED_TO_MODEL")		{ return 4;}
	
	// did not find it.
	throw new Error("No Deletion Type exists for " + delVal);
};

/* the semantic node group */
var SemanticNodeGroup = function(width, height, divName) {
    var drawFlag = (typeof width !== "undefined");
    
	this.SNodeList = [];
    this.limit = 0;
    if (drawFlag) {
        this.graph = new Graph();
        this.layouter = new Graph.Layout.Spring(this.graph, width, height);
        this.renderer = new Graph.Renderer.Raphael(divName, this.graph, width,
                height);
        this.rangeSetter = '';
        this.isRangeSetterAsync = false;
        this.returnNameSetter = '';
        this.isReturnNameSetterAsync = false;
        this.asyncPropEditor = function(){alert("Internal error: SemanticNodeGroup asyncPropEditor function is not defined.")};
        this.asyncSNodeEditor = function(){alert("Internal error: SemanticNodeGroup asyncSNodeEditor function is not defined.")};
        this.asuncSNodeRemover = function() {};  // 
        this.asyncNodeEditor = function(){alert("Internal error: SemanticNodeGroup asyncNodeEditor function is not defined.")};
        this.asyncLinkBuilder = function(){alert("Internal error: SemanticNodeGroup asyncLinkBuilder function is not defined.")};
        this.asyncLinkEditor = function(){alert("Internal error: SemanticNodeGroup asyncLinkEditor function is not defined.")};


        this.height = height;
        this.width = width;
        this.divName = divName;
        this.drawable = true;    // when I make copies and delete stuff, drawing fails
	                         // So I set this flag to false and skip drawing.
	                         // JUSTIN / PAUL TODO.  Untangle this mess.
    } else {
        this.drawable = false;
    }
    
	this.sparqlNameHash = {};
	
	this.conn = null;       // optional relevant SparqlConnection.
	                        // this will need to be non-optional and expanded in the upcoming versions.
	
	this.prefixHash = {};
	this.prefixNumberStart = 0;
	
	this.canvasOInfo = null;    // DEPRECATED
	                            // this is a late addition used by nothing except callbacks on the canvas which add nodes.
    							// that's why it has a funny name.
    							// Only sparqlgraph.js calls this after loading a new oInfo.
    							// All other code passes in an oInfo when needed.
	                            // This feels dirty, and was done only after many other retro-fits were tried and failed.
	                            // Next best thing:  all code anywhere should keep a node group's oInfo up to date.
	                            //                   then all the oInfo params in functions could be removed.
	                            //                   thus breaking legacy code outside semTK.
};

SemanticNodeGroup.QUERY_DISTINCT = 0;
SemanticNodeGroup.QUERY_CONSTRAINT = 1;
SemanticNodeGroup.QUERY_COUNT = 2;
SemanticNodeGroup.QUERY_CONSTRUCT = 3;
SemanticNodeGroup.QUERY_CONSTRUCT_WHERE = 4;
SemanticNodeGroup.QUERY_DELETE_WHERE = 5;

SemanticNodeGroup.JSON_VERSION = 6;
// version 6 - limit
// version 5 - top-secret undocumented version 
// version 4 - multiple connections
// version 3 - SNodeOptionals


SemanticNodeGroup.XMLSCHEMA_PREFIX = "XMLSchema:";
SemanticNodeGroup.XMLSCHEMA_FULL = "http://www.w3.org/2001/XMLSchema#";
SemanticNodeGroup.INSERT_PREFIX = "generateSparqlInsert:";
SemanticNodeGroup.INSERT_FULL = "belmont/generateSparqlInsert#";

SemanticNodeGroup.prototype = {
		
	
	toJson : function(optDeflateFlag, optMappedPropItems) {
		var deflateFlag = (typeof optDeflateFlag === "undefined") ? false : optDeflateFlag;
		var mappedPropItems = (typeof optMappedPropItems === "undefined") ? [] : optMappedPropItems;

		// get list in order such that linked nodes always preceed the node that
		// links to them
		var snList = this.getOrderedNodeList().reverse();

		var ret = {
			version : SemanticNodeGroup.JSON_VERSION,
            limit : this.limit,
			sNodeList : [],
		};
		// add json snodes to sNodeList
		for (var i = 0; i < snList.length; i++) {
			ret.sNodeList.push(snList[i].toJson(deflateFlag, mappedPropItems));
		}
		return ret;
	},
	
	addJson : function(jObj, optInflateOInfo) {
		var inflateOInfo = (typeof optInflateOInfo === "undefined") ? null : optInflateOInfo;
		
		if (jObj.version > SemanticNodeGroup.JSON_VERSION) {
			throw new Error("SemanticNodeGroup.addJson only recognizes nodegroup json up to version " + SemanticNodeGroup.JSON_VERSION + " but file is version " + jObj.version);
		}
        
        if (jObj.hasOwnProperty("limit")) {
            this.limit = jObj.limit;
        }
		// clean up name collisions while still json
		this.resolveSparqlIdCollisionsJson(jObj);

		// loop through SNodes in the json
		for (var i = 0; i < jObj.sNodeList.length; i++) {
			var newNode = new SemanticNode(null, null, null, null, null, this,
					jObj.sNodeList[i], inflateOInfo);

			// add the node without messing with any connections...they are
			// already there.
			this.addOneNode(newNode, null, null, null);
		}
	},
	fromConstructJson : function(jsonGraph, oInfo){
		// create a hash of the nodes to be manipulated
		var semanticNodeHash = {};
		if (! jsonGraph) { return; }
		
		// loop through top-level
		for( var i = 0; i < jsonGraph.length; i += 1) {
			// create a semantic node for this entry...
			var obj = jsonGraph[i];

			var classUriList = obj["@type"];
			// TODO: there may be more than one class uri returned. right now, we are only handling the first. 
			var classUri_tuple = classUriList[0];

			// uri for the instance:
			var instanceURI = obj["@id"];


			var classUri = classUri_tuple["@id"];
			var nome = new OntologyName(classUri).getLocalName();
			// create a new node based on the return. 
			var currSNode = new SemanticNode(nome, [], [], classUri, oInfo.getSubclassNames(classUri), this);
			this.addOneNode(currSNode, null, null, null);
			currSNode.instanceValue = instanceURI;
			semanticNodeHash[instanceURI] = currSNode;

			// loop through the other properties and get the values that matter.
			var propertyList = [];		// store the properties we are using later

			for(var itemUri in obj)
			{
				// ignore the Uri and Type info
				if(itemUri === "@type" || itemUri === "@id") { continue; }

				// process other values.

				var valueList = obj[itemUri];
				var createdProp = false;
				var currPropertyItem = null;
				for( var k = 0; k < valueList.length; k += 1){
					// check that this is a "property" -- basically that the value is a primitive. 
					if(!valueList[k].hasOwnProperty("@type") ){ continue; } // not a primitive

					if(!createdProp){
						var propRangeFull = valueList[k]["@type"];
						var propNameFull = itemUri;
						var propRangeLocal = new OntologyName(propRangeFull).getLocalName();
						var propNameLocal = new OntologyName(propNameFull).getLocalName();

						currPropertyItem = new PropertyItem(propNameLocal, propRangeLocal, propRangeFull, propNameFull);
						createdProp = true;
						// thanks Jenny for pointing out this bug, explained below. 
						propertyList.push(currPropertyItem);
						currPropertyItem.setIsReturned(true);

					}
					var instanceVal = valueList[k]["@value"];
					currPropertyItem.addInstanceValue(instanceVal);
					// following two lines moved into !createdProp conditional after jenny pointed out they are 
					// likely displaced 
//					propertyList.push(currPropertyItem);
//					currPropertyItem.setIsReturned(true);
				}

			}
			// add this to the node we made.
			currSNode.setPList(propertyList);	

		}
		// add nodeItems. strangely, this can only be done after all the nodes are created. 
		// i hope this explains the node hash
		for( var i = 0; i < jsonGraph.length; i += 1) {
			var obj = jsonGraph[i];

			var nodeList = [];

			for(var itemUri in obj)
			{
				var currNodeUri = obj["@id"];
				currSNode = semanticNodeHash[currNodeUri];
				// ignore the Uri and Type info
				if(itemUri === "@type" || itemUri === "@id") { continue; }

				// process other values.

				var valueList = obj[itemUri];
				var createdNode = false;
				var currNodeItem = null;
				for( var k = 0; k < valueList.length; k += 1){
					// check that this is a "node" -- basically that the value is a uri. 
					if(!valueList[k].hasOwnProperty("@type") ){ 
						var linkedUriForType = valueList[k]["@id"];
						var linkedSNode = semanticNodeHash[linkedUriForType];

						if(!createdNode) {
							var localName = new OntologyName(itemUri).getLocalName();
							var fullRange = linkedSNode.getURI(false);
							var localRange = linkedSNode.getURI(true);
							currNodeItem = new NodeItem(localName, localRange, fullRange);
							// do not repeat this step.
							createdNode = true;
							currNodeItem.setConnected(true);
							currNodeItem.setConnectBy(localRange);
							currNodeItem.setUriConnectBy(itemUri);
							nodeList.push(currNodeItem);
						}	
						// add the link to the semanticNode itself

						currNodeItem.pushSNode(linkedSNode);

					}
				}

			}
			// add this to the node we made.
			currSNode.setNList(nodeList);	

			if (nodeList.length === 0 && currSNode.instanceValue) {
				currSNode.setIsReturned(true);
			}

		}

	},
	deepCopy : function() {
		// use the json functionality to implement deepCopy
		// maybe less efficient but keeps avoids double implementation - Paul
		var ret = new SemanticNodeGroup();
		ret.addJson(this.toJson());
		
		// connection
		var conn = new SparqlConnection();
		conn.fromJson(this.conn.toJson());
		ret.setSparqlConnection(conn);
		
		ret.drawable = false;     // TODO: automatically make it illegal to draw a copy to get around raphael draw bugs.
		return ret;
	},

	getPrefixedUri : function (originalUri) {
		var retval = "";
		if(originalUri == null ){
			throw new Error("prefixed URI " + originalUri + " does not seem to contain a proper prefix.");
		}
		else if(originalUri.indexOf("#") == -1 ){
			return originalUri;
		}
		else{
			// get the chunks and build the prefixed string.
			var chunks = originalUri.split("#");
			var pre = this.prefixHash[chunks[0]];
			
			if(chunks.length > 1 ){
				retval = pre + ":" + chunks[1];
			}
			else{
				retval = pre + ":";
			}
		}
		
		return retval;
	},
	
	addToPrefixHash : function (prefixedUri){
		// from the incoming string, remove the local fragment and then try to add the rest to the prefix hash.
		if(prefixedUri == null){ return; }
		if(prefixedUri.indexOf("#") == -1){ return; }
		
		var chunks = prefixedUri.split("#");
		
		// found a new prefix
		if(! (chunks[0] in this.prefixHash)) {
			// create a new prefix name
			var fragments = chunks[0].split("/");
			var newPrefixName = fragments[fragments.length - 1];
			
			// === Object.values() apparently isn't widely supported  ===
			// === so populate prefixVals = Object.values(prefixHash) ===
			var prefixVals = [];
			for (var k in this.prefixHash) {
				prefixVals.push(this.prefixHash[k]);
			}
			//============================================================
			
			// make sure prefix starts with a number
			f = new SparqlFormatter();
			newPrefixName = f.legalizePrefixName(newPrefixName);
			
			// make sure new prefix name is unique
			if (prefixVals.indexOf(newPrefixName) > -1) {
				var i=0;
				while (prefixVals.indexOf(newPrefixName + "_" + i) > -1) {
					i++;
				}
				newPrefixName = newPrefixName + "_" + i;
			}
			
			//String newPrefixName = "pre_" + this.prefixNumberStart;
			this.prefixNumberStart += 1;  // also obsolete I think
			
			this.prefixHash[chunks[0]] = newPrefixName;
			
			//System.err.println("adding prefix: " + newPrefixName + " with key " + chunks[0] + " from input " + prefixedUri);
		}
	},
	
	rebuildPrefixHash : function (startingMap){
		
		this.prefixHash = {};		// x out the old map.
		
		this.prefixHash = startingMap;							// replace it.
		this.prefixNumberStart = Object.keys(startingMap).length;
		this.addAllToPrefixHash();
		
	},
	
	addAllToPrefixHash : function (){
		
		this.addToPrefixHash(SemanticNodeGroup.XMLSCHEMA_FULL);
		
		for(var i=0; i < this.SNodeList.length; i++){
			var n = this.SNodeList[i];
			if(n.getInstanceValue() != null && n.getInstanceValue().indexOf("#") > -1){
				this.addToPrefixHash(n.getInstanceValue());
			}			
			// add the prefix for each node.
			this.addToPrefixHash(n.getURI());
			// add the URIs for the properties as well:
			
			for (var p=0; p < n.propList.length; p++) {
				var pi = n.getPropertyItem(p);
				this.addToPrefixHash(pi.getUriRelation());
			}
			// add the URIs for the node items
			for (var q=0; q < n.nodeList.length; q++) {
				var ni = n.getNodeItem(q);
				this.addToPrefixHash(ni.getURIConnectBy());
			}
		}

	},
	
	getPrefixHash : function() {
		
		if(this.prefixHash != null && Object.keys(this.prefixHash).length != 0){
			return this.prefixHash;
		}
		else{
			this.buildPrefixHash();		// create something to send.
			return this.prefixHash;
		}
		
	},
	
	buildPrefixHash : function() {
		
		if(Object.keys(this.prefixHash).length != 0){
			return;
		}
		else{
		// if possible, add the default prefix and user prefix:
			this.addAllToPrefixHash();
		}
	},
	
	resolveSparqlIdCollisionsJson : function(jObj) {
		// loop through a json object and resolve any SparqlID name collisions
		// with this node group.

		if (this.sparqlNameHash.length == 0) {
			return;
		}

		// set up formatter, temporary name hash, and hash of changed names
		var f = new SparqlFormatter();
		var tmpNameHash = {};
		for ( var key in this.sparqlNameHash) {
			tmpNameHash[key] = this.sparqlNameHash[key];
		}
		var changedHash = {};

		// loop through JSON SNodes
		for (var i = 0; i < jObj.sNodeList.length; i++) {
			// check snode sparqlId
			var jnode = jObj.sNodeList[i];

			this.helperUpdateId(jnode, "SparqlID", f, changedHash, tmpNameHash);

			// loop through property items
			for (var j = 0; j < jnode.propList.length; j++) {
				var jprop = jnode.propList[j];
				this.helperUpdateId(jprop, "SparqlID", f, changedHash,
						tmpNameHash);
			}

			// loop through node items
			for (var j = 0; j < jnode.nodeList.length; j++) {
				var jnitem = jnode.nodeList[j];

				for (var k = 0; k < jnitem.SnodeSparqlIDs.length; k++) {
					this.helperUpdateId(jnitem.SnodeSparqlIDs, k, f,
							changedHash, tmpNameHash);
				}
			}
		}
	},
	helperUpdateId : function(obj, idx, fmt, changedHash, tmpNameHash) {
		// helper function applies changedHash to obj.fieldname
		// obj[idx] will be acted upon. In javascript: obj.idx is equivalent to
		// obj[idx]
		// so idx can be an array index OR a field name

		var id = obj[idx];
		if (id == "")
			return;

		// if possible: replace id with one in changedHash
		if (id in changedHash) {
			obj[idx] = changedHash[id];

			// else check that id is legal and unique
		} else {
			var newId = fmt.genSparqlID(id, tmpNameHash);

			// if needed, change the id and record in all three places
			if (newId != id) {
				changedHash[id] = newId;
				obj[idx] = newId;
				tmpNameHash[newId] = 1;
			}
		}
	},
    
    setLimit : function (l) {
        this.limit = l;
    },
    
    getLimit : function () {
        return this.limit;
    },
    
	// DEPRECATED
	setCanvasOInfo : function (oInfo) {
		this.canvasOInfo = oInfo;
	},
	
	setAsyncPropEditor : function (func) {
		// func(propertyItem) will edit the property (e.g. constraints, sparqlID, optional)
		this.asyncPropEditor = func;
	},
	setAsyncNodeEditor : function (func) {
		// func(nodeItem) will edit the property 
		this.asyncNodeEditor = func;
	},
	setAsyncLinkBuilder : function (func) {
		// func(nodeItem) will edit the property 
		this.asyncLinkBuilder = func;
	},
	setAsyncLinkEditor : function (func) {
		// func(nodeItem) will edit the property 
		this.asyncLinkEditor = func;
	},
	setAsyncSNodeEditor : function (func) {
		// func(propertyItem) will edit the property (e.g. constraints, sparqlID, optional)
		this.asyncSNodeEditor = func;
	},
    setAsyncSNodeRemover : function (func) {
        this.asyncSNodeRemover = func;
    },
	
	setSparqlConnection : function (sparqlConn) {
		this.conn = sparqlConn;
	},
	
	getNodeCount : function() {
		return this.SNodeList.length;
	},
	drawNodes : function() {
		if (! this.drawable) return;
		
		// draws the Dracula nodes in the list, in no particular order.
		var t = this.SNodeList.length;
		var connectionList = [];
		for (var l = 0; l < t; l++) {
			// draw each node.
			this.graph.addExistingNode(this.SNodeList[l].getNode());
			connectionList = connectionList.concat(this.SNodeList[l].getConnections());
		}

		var t = connectionList.length;
		for (var i = 0; i < t; i++) {
			this.graph.addEdge(connectionList[i].getSrc(), connectionList[i]
					.getTgt(), {
				directed : true,
				label : connectionList[i].getRel()
			});
		}

		//this.layouter.layout();    //don't layout every time.  It doesn't work that well.
		this.renderer.draw();

	},

	reserveNodeSparqlIDs : function(snode) {
		// reserve all of a node's sparqlID's
		// changing them if they are already in use.
		var id;
		var f = new SparqlFormatter();

		// sparqlID
		id = snode.getSparqlID();
		if (id in this.sparqlNameHash) {
			id = f.genSparqlID(id, this.sparqlNameHash);
			snode.setSparqlID(id);
		}
		this.reserveSparqlID(id);

		// all the properties
		var props = snode.getReturnedPropertyItems();
		for (var i = 0; i < props.length; i++) {
			id = props[i].getSparqlID();
			if (id in this.sparqlNameHash) {
				id = f.genSparqlID(id, this.sparqlNameHash);
				props[i].setSparqlID(id);
			}
			this.reserveSparqlID(id);
		}
	},

	addOneNode : function(newNode, existingNode, linkFromNewUri, linkToNewUri) {
		// add a newNode, possibly with a connection either from it or to it
		// newNode is a SemanticNode. Link it to existingNodeUri
		// if existingNode != null, then either linkFromNewUri or linkToNewUri
		// should also be non-null
		//
		// return the nodeItem (or null if there was an error)

		this.reserveNodeSparqlIDs(newNode);

		this.SNodeList.push(newNode);

		// create the link to or from existingNode, if there is one
		if (linkFromNewUri) {
			return newNode.setConnection(existingNode, linkFromNewUri);
		} else if (linkToNewUri) {
			return existingNode.setConnection(newNode, linkToNewUri);
		} else {
			// no existing node to link to.
			// presume / hope the canvas is empty
			return null;
		}

	},

	addNode : function(classUri, oInfo) {
		// adds a node without making any connections

		var node = this.returnBelmontSemanticNode(classUri, oInfo);
		this.addOneNode(node, null, null, null);
		return node;
	},

	addPath : function(path, anchorNode, oInfo, optReverseFlag, optOptionalFlag) {
		// Adds a path to the canvas.
		// path start class is the new one
		// path end class already exists
		// return the node corresponding to the path's startClass. (i.e. the one
		// the user is adding.)
		
		// reverseFlag:  in diabolic case where path is one triple that starts and ends on same class
		//               if reverseFlag, then connect
		var reverseFlag = (typeof optReverseFlag === 'undefined') ? false : optReverseFlag;
		
		// optionalFlag:  if set, the first link will be optional
		var optionalFlag = (typeof optOptionalFlag  === "undefined") ? false : optOptionalFlag;
		
		// add the first class in the path
		var retNode = this.addNode(path.getStartClassName(), oInfo);
		var lastNode = retNode;
		var node0;
		var node1;
		var pathLen = path.getLength();
		// loop through path but not the last one
		for (var i = 0; i < pathLen - 1; i++) {
			var class0Uri = path.getClass0Name(i);
			var attUri = path.getAttributeName(i);
			var class1Uri = path.getClass1Name(i);

			// if this hop in path is  lastAdded--hasX-->class1
			if (class0Uri == lastNode.getURI()) {
				node1 = this.returnBelmontSemanticNode(class1Uri, oInfo);
				this.addOneNode(node1, lastNode, null, attUri);
				lastNode = node1;
				
			// else this hop in path is class0--hasX-->lastAdded
			} else {
				node0 = this.returnBelmontSemanticNode(class0Uri, oInfo);
				this.addOneNode(node0, lastNode, attUri, null);
				lastNode = node0;
			}
		}

		// link the last two nodes, which by now already exist
		var class0Uri = path.getClass0Name(pathLen - 1);
		var class1Uri = path.getClass1Name(pathLen - 1);
		var attUri = path.getAttributeName(pathLen - 1);
		var nodeItem;

		
		if (class0Uri === class1Uri && reverseFlag ) {
			// link diabolical case from anchor node to last node in path
			var opt = optionalFlag ? NodeItem.OPTIONAL_REVERSE : NodeItem.OPTIONAL_FALSE;
			nodeItem = anchorNode.setConnection(lastNode, attUri, opt);
					
		} else if (anchorNode.getURI() == class1Uri) {
			// normal link from last node to anchor node
			var opt = optionalFlag ? NodeItem.OPTIONAL_REVERSE : NodeItem.OPTIONAL_FALSE;
			nodeItem = lastNode.setConnection(anchorNode, attUri, opt);
			
		} else {
			// normal link from anchor node to last node
			var opt = optionalFlag ? NodeItem.OPTIONAL_TRUE : NodeItem.OPTIONAL_FALSE;
			var nodeItem = anchorNode.setConnection(lastNode, attUri, opt);
			
		}
		return retNode;

	},

	returnBelmontSemanticNode : function(classUri, oInfo) {
		// return a belmont semantic node represented by the class passed from
		// oInfo.
		// PAUL NOTE: this used to be in graphGlue.js
		// But there is no value in keeping oInfo and belmont separate, and
		// combining is elegant.
		var oClass = oInfo.getClass(classUri);
		belprops = [];
		belnodes = [];

		// set the value for the node name:
		var nome = oClass.getNameStr(true);
		var fullNome = oClass.getNameStr(false);

		props = oInfo.getInheritedProperties(oClass);

		// get a list of the properties not repesenting other nodes.
		for (var i = 0; i < props.length; i++) {
			var propNameLocal = props[i].getName().getLocalName();
			var propNameFull = props[i].getName().getFullName();
			var propRangeNameLocal = props[i].getRange().getLocalName();
			var propRangeNameFull = props[i].getRange().getFullName();

			// is the range a class ?
			if (oInfo.containsClass(propRangeNameFull)) {
				var p = new NodeItem(propNameLocal, propRangeNameLocal,
						propRangeNameFull);
				belnodes.push(p);

			}
			// range is string, int, etc.
			else {

				// create a new belmont property object and add it to the list.
				var p = new PropertyItem(propNameLocal, propRangeNameLocal,
						propRangeNameFull, propNameFull);
				belprops.push(p);
			}
		}

		return new SemanticNode(nome, belprops, belnodes, fullNome, oInfo
				.getSubclassNames(fullNome), this);
	},

	getSNodeList : function() {
		return this.SNodeList;
	},
	
	getAllNodeItems : function() {
		ret = [];
		for (var i=0; i < this.SNodeList.length; i++) {
			for (var j=0; j < this.SNodeList[i].nodeList.length; j++) {
				ret.push(this.SNodeList[i].nodeList[j]);
			}
		}
		return ret;
	},
	
	getSubNodes : function(topNode) {
		// recursive function returns topNode and all it's sub-nodes in a top
		// down breadth-first search
		var ret = [];

		var conn = topNode.getConnectedNodes();
		ret = ret.concat(conn);

		for (var i = 0; i < conn.length; i++) {
			var subs = this.getSubNodes(conn[i]);
			ret = ret.concat(subs);
		}
		return ret;
	},

	getHeadNodes : function () {
		// get nodes with no incoming connections
		// SparqlGraph should evolve to the point where these are not guaranteed to exist.
		var nodeCount = this.SNodeList.length;
		var ret = [];
		
		// find head nodes
		for (var i = 0; i < nodeCount; i++) {
			var connCount = 0;
			for (var j = 0; j < nodeCount; j++) {
				if (this.SNodeList[j].checkConnectedTo(this.SNodeList[i])) {
					connCount++;
					break;
				}
			}

			if (connCount == 0) {
				ret.push(this.SNodeList[i]);
			}
		}

		// catch the problem for the day we allow circular graphs
		if (nodeCount > 0 && ret.length == 0) {
			ret.push(this.SNodeList[0]);
			console.log("Danger in belmont.js getOrderedNodeList(): No head nodes found.  Graph is totally circular.");
		}
		
		return ret;
	},
	
	getNextHeadNode : function (skipNodes) {
		
		// peacefully return null if there are no nodes left
		if (skipNodes.length == this.SNodeList.length) {
			return null;
		}
		
		var optHash = this.calcOptionalHash(skipNodes);
		var linkHash = this.calcIncomingLinkHash(skipNodes);
		
		var retID = null;
		var minLinks;
		
		// both hashes have same keys: loop through valid snode SparqlID's
		for (var id in optHash) {
			// find nodes that are not optional
			if (optHash[id] == 0) {
				// choose node with lowest number of incoming links
				if (retID == null || linkHash[id] < minLinks) {
					retID = id;
					minLinks = linkHash[id];
					// be efficient
					if (minLinks == 0) { break; }
				}
			}
		}
		
		// throw an error if no nodes have optHash == 0
		if (retID == null) {
			alert("Internal error in belmont.js getHeadNextHeadNode(): No head nodes found. Probable cause: no non-optional semantic nodes.");
			throw "Internal error.";
		}
		
		return this.getNodeBySparqlID(retID);
	},
	
	calcIncomingLinkHash : function (skipNodes) {
		// so linkHash[snode.getSparqlID()] == count of incoming nodeItem links
		
		var linkHash = {};
		
		// initialize hash
		for (var i=0; i < this.SNodeList.length; i++) {
			var snode = this.SNodeList[i];
			if (skipNodes.indexOf(snode) == -1) {
				linkHash[snode.getSparqlID()] = 0;
			}
		}
		
		// loop through all snodes
		for (var i=0; i < this.SNodeList.length; i++) {
			var snode = this.SNodeList[i];
			
			if (skipNodes.indexOf(snode) == -1) {
				
				// loop through all nodeItems
				for (var n=0; n < snode.nodeList.length; n++) {
					var nodeItem = snode.nodeList[n];
					
					for (var c=0; c < nodeItem.SNodes.length; c++) {
						// increment hash[sparqlID] for each incoming link
						linkHash[nodeItem.SNodes[c].getSparqlID()] += 1;
					}
				}
			}
		}
		return linkHash;
	},
	
	calcOptionalHash : function(skipNodes) {
		
		// ---- set optHash ----
		// so optHash[snode.getSparqlID()] == count of nodeItems indicating this node is optional
		
		var optHash = {};
		// initialize optHash
		for (var i=0; i < this.SNodeList.length; i++) {
			var snode = this.SNodeList[i];
			if (skipNodes.indexOf(snode) == -1) {
				optHash[snode.getSparqlID()] = 0;
			}
		}
		
		// loop through all snodes
		for (var i=0; i < this.SNodeList.length; i++) {
			var snode = this.SNodeList[i];
			
			if (skipNodes.indexOf(snode) == -1) {
				// loop through all nodeItems
				for (var n=0; n < snode.nodeList.length; n++) {
					var nodeItem = snode.nodeList[n];
					
					// loop through all connectedSNodes
					var connSNodes = nodeItem.getSNodes();
					for (c=0; c < connSNodes.length; c++) {
						var targetSNode = connSNodes[c];
						// if found an optional nodeItem
						var opt = nodeItem.getSNodeOptional(targetSNode);
						
						var subGraph = [];
						// get subGraph(s) on the optional side of the nodeItem
						if (opt == NodeItem.OPTIONAL_TRUE) {
							subGraph = subGraph.concat(this.getSubGraph(targetSNode, [snode]));
							
						} else if (opt == NodeItem.OPTIONAL_REVERSE) {
							subGraph = subGraph.concat(this.getSubGraph(snode, [targetSNode]));
						}
						
						// increment every node on the optional side of the nodeItem
						for (var k=0; k < subGraph.length; k++) {
							optHash[subGraph[k].getSparqlID()] += 1;
						}
					}
				}
			}
		}
		
		return optHash;
	},
	
	getOrderedNodeList : function() {
		var ret = [];
		var headNodes = [];

		headNodes = this.getHeadNodes();
		
		// loop through headNodes
		for (var i = 0; i < headNodes.length; i++) {
			ret.push(headNodes[i]);
			ret = ret.concat(this.getSubNodes(headNodes[i]));
		}

		// remove duplicates from ret
		ret2 = [];
		for (var i = 0; i < ret.length; i++) {
			// push only the last one onto the return list
			if (ret.lastIndexOf(ret[i]) == i) {
				ret2.push(ret[i]);
			}
		}
		return ret2;

	},

	getConnectedRange : function(sNode) {
		// get a list of classes that this class is constrained to by it's
		// parent(s)' attribute ranges
		// e.g. if no parents, then []
		// if one parent hasSon SonClass connects to sNode, then [SonClass]
		// if multiple parents then list..
		var ret = [];
		
		var nodeItems = this.getConnectingNodeItems(sNode);
		for (j=0; j < nodeItems.length; j++) {
			if (nodeItems[j].getSNodeOptional(sNode) != NodeItem.OPTIONAL_REVERSE) {
				var uriValType = nodeItems[j].getUriValueType();
				if (ret.indexOf(uriValType) < 0)
					ret.push(uriValType);
			}
		}
		
		return ret;
	},

	getConnectingNodes : function(sNode) {
		// get semantic nodes with a nodeItem pointing to sNode
		var ret = [];
		for (var i = 0; i < this.SNodeList.length; i++) {
			if (this.SNodeList[i].getConnectingNodeItems(sNode).length > 0) {
				ret.push(this.SNodeList[i]);
			}
		}
		return ret;
	},
	
	getConnectingNodeItems : function(sNode) {
		// get any nodeItem in the nodeGroup that points to sNode
		var ret = [];
		// for every node
		for (var i=0; i < this.SNodeList.length; i++) {
			// get connections to sNode and push them to ret
			var nodeItems = this.SNodeList[i].getConnectingNodeItems(sNode);
			for (var j=0; j < nodeItems.length; j++) {
				ret.push(nodeItems[j]);
			}
		}
		return ret;
	},
	
	getAllConnectedNodeItems : function(sNode) {
		var ret = [];
		
		// SNode knows who it points too
		ret = ret.concat(sNode.nodeList);
		
		// nodegroup knows which nodes point to startSNode
		ret = ret.concat(this.getConnectingNodeItems(sNode));

		return ret;
	},
	
	getAllConnectedConnectedNodeItems : function(sNode) {
		// get the connectedNodeItems that are actually in use
		var ret = [];
		var temp = this.getAllConnectedNodeItems(sNode);
		for (var i=0; i < temp.length; i++) {
			if (temp[i].getConnected()) {
				ret.push(temp[i]);
			}
		}
		return ret;
	},
	
	getNodeItemParentSNode : function(nodeItem) {
		for (var i=0; i < this.SNodeList.length; i++) {
			if (this.SNodeList[i].nodeList.indexOf(nodeItem) > -1) {
				return this.SNodeList[i];
			}
		}
	},
	
	getAllConnectedNodes : function(sNode) {
		// get any node with connection TO or FROM this sNode
		var ret = [];
		
		// startSNode knows who it points too
		ret = ret.concat(sNode.getConnectedNodes());
		
		// nodegroup knows which nodes point to startSNode
		ret = ret.concat(this.getConnectingNodes(sNode));

		return ret;
	},
	
	getNodeItemsBetween(sNode1, sNode2) {
		// return a list of node items between the two nodes
		// Ahead of the curve: supports multiple links between snodes
		var ret = [];
		
		for (var i=0; i < sNode1.nodeList.length; i++) {
			if (sNode1.nodeList[i].SNodes.indexOf(sNode2) > -1) {
				ret.push(sNode1.nodeList[i]);
			}
		}
		
		for (var i=0; i < sNode2.nodeList.length; i++) {
			if (sNode2.nodeList[i].SNodes.indexOf(sNode1) > -1) {
				ret.push(sNode2.nodeList[i]);
			}
		}
		
		return ret;
	},
	
	getSubGraph : function(startSNode, stopList) {
		// get all the nodes connected recursively through all connections
		// except stopList.
		var ret = [startSNode];
		var conn = this.getAllConnectedNodes(startSNode);

		for (var i=0; i < conn.length; i++) {
			if (stopList.indexOf(conn[i]) === -1 && ret.indexOf(conn[i]) === -1) {
				ret = ret.concat(this.getSubGraph(conn[i], ret));
			}
		}
		return ret;
	},
    
	expandOptionalSubgraphs : function() {
		// Find nodes with only optional returns
		// and add incoming optional nodeItem so that entire snode is optional
		// then move the optional nodeItem outward until some non-optional return is found
		// this way the "whole chain" becomes optional.
		// Leave original optionals in place
		
		// For nodes with only one non-optional connection, and optional properties
		// make the node connection optional too
		for (var i = 0; i < this.SNodeList.length; i++) {
			var snode = this.SNodeList[i];
			
			// count optional and non-optional returns properties
			var optRet = 0;
			var nonOptRet = snode.getIsReturned() ? 1 : 0;
			var retProps = snode.getReturnedPropertyItems();
			for (var j=0; j < retProps.length; j++) {
				var prop = retProps[j];
				if (prop.getIsOptional()) {
					optRet += 1;
				} else {
					nonOptRet += 1;
				}
			}
			
			// if all returned props are optional
			if (optRet > 0 && nonOptRet == 0) {		
				var connectedSnodes = this.getAllConnectedNodes(snode);
				
				// if there's only one snode connected
				if (connectedSnodes.length == 1) {
					var otherSnode = connectedSnodes[0];
					var nodeItems = this.getNodeItemsBetween(snode, otherSnode);
					
					// if it's only connected once between snode and otherSnode 
					// and connection is non-optional
					// then make it optional
					if (nodeItems.length == 1) {
						var nodeItem = nodeItems[0];
						if (snode.ownsNodeItem(nodeItem) && nodeItem.getSNodeOptional(otherSnode) == NodeItem.OPTIONAL_FALSE) {
							nodeItem.setSNodeOptional(otherSnode, NodeItem.OPTIONAL_REVERSE);
						}
						
						if (otherSnode.ownsNodeItem(nodeItem) && nodeItem.getSNodeOptional(snode) == NodeItem.OPTIONAL_FALSE) {
							nodeItem.setSNodeOptional(snode, NodeItem.OPTIONAL_TRUE);
						}
					} 
				}
			}
		}
		
		// now move optional nodeItems as far away from subgraph leafs as possible
		var changedFlag = true;
		while (changedFlag) {
			changedFlag = false;
			
			// loop through all snodes
			for (var i = 0; i < this.SNodeList.length; i++) {
				var snode = this.SNodeList[i];
				
				// count non-optional returns and optional properties
				var nonOptReturnCount = snode.getIsReturned() ? 1 : 0;
				var optPropCount = 0;
				var retItems = snode.getReturnedPropertyItems();
				for (var p=0; p < retItems.length; p++) {
					var pItem = retItems[p];
					if (! pItem.getIsOptional()) {
						nonOptReturnCount++;
					} else if (pItem.getIsReturned()) {
						optPropCount++;
					}
				}
				
				// sort all connected node items by their optional status: none, in, out
				var normItems = [];
				var optOutItems = [];
				var optInItems= [];
				
				// outgoing nodes
				var nItems = snode.getNodeList();
				for (var n=0; n < nItems.length; n++) {
					var nItem = nItems[n];
					if (nItem.getConnected()) {
						var targets = nItem.getSNodes();
						for (var t=0; t < targets.length; t++) {
							var target = targets[t];
							var opt = nItem.getSNodeOptional(target);
							
							if (opt == NodeItem.OPTIONAL_FALSE) {
								normItems.push([nItem, target]);
								
							} else if (opt == NodeItem.OPTIONAL_TRUE) {
								optOutItems.push([nItem, target]);
									
							} else {// OPTIONAL_REVERSE
								optInItems.push([nItem, target]); 
							}
						}
					}
				}
				
				// incoming nodes
				var nItems = this.getConnectingNodeItems(snode); 
				for (var n=0; n < nItems.length; n++) {
					var nItem = nItems[n];
					
					var opt = nItem.getSNodeOptional(snode);
					
					if (opt == NodeItem.OPTIONAL_FALSE) {
						normItems.push([nItem, snode]);
	
					} else if (opt == NodeItem.OPTIONAL_REVERSE) {
						optOutItems.push([nItem, snode]);
							
					} else {// OPTIONAL_TRUE
						optInItems.push([nItem, snode]); 
					}
					
				}
				
				// if nothing is returned AND
				//  one normal connection AND
				//  >= 1 optional outward connections AND
				// no optional in connections AND
				if (nonOptReturnCount == 0 && normItems.length == 1 && optOutItems.length >= 1 && optInItems.length == 0) {
				
					// set the single normal nodeItem to incoming optional
					var nItem = normItems[0][0];
					var target = normItems[0][1];
					if (target != snode) {
						nItem.setSNodeOptional(target, NodeItem.OPTIONAL_REVERSE);
					} else {
						nItem.setSNodeOptional(target, NodeItem.OPTIONAL_TRUE);
					}
					
					// if there is only one outgoing optional, than it can be set to non-optional for performance
					if (optOutItems.length == 1 && optPropCount == 0) {
						var oItem = optOutItems[0][0];
						var oTarget = optOutItems[0][1];
						oItem.setSNodeOptional(oTarget, NodeItem.OPTIONAL_FALSE);
					}
					
					changedFlag = true;
				}
			}
		}
	},
	
	alreadyExists : function(SNode) {
		var retval = false;
		var namedSNode = SNode.getNodeName();
		var t = this.SNodeList.length;
		for (var l = 0; l < t; l++) {
			if (this.SNodeList[l].getNodeName() == namedSNode) {
				retval = true;
				break;
			}
		}
		return retval;
	},

	getNodesByURI : function(uri) {
		// get all nodes with the given uri
		var ret = [];

		for (var i = 0; i < this.SNodeList.length; i++) {
			if (this.SNodeList[i].getURI() == uri) {
				ret.push(this.SNodeList[i]);
			}
		}
		return ret;
	},
	
	getNodesBySuperclassURI : function(uri, oInfo) {
		// get all nodes with the given uri
		var ret = [];

		// get all subclasses
		var classes = [uri].concat(oInfo.getSubclassNames(uri));
		
		// for each class / sub-class
		for (var i=0; i < classes.length; i++) {
			// get all nodes
			var c = this.getNodesByURI(classes[i]);
			// push node if it isn't already in ret
			for (var j=0; j < c.length; j++) {
				if (ret.indexOf(c[j]) == -1) {
					ret.push(c[j]);
				}
			}
		}
		
		return ret;
	},
	
	getNodeBySparqlID : function(id) {
		// gets first node by uri, or null if it doesn't exist
		for (var i = 0; i < this.SNodeList.length; i++) {
			if (this.SNodeList[i].getSparqlID() == id) {
				return this.SNodeList[i];
			}
		}
		return null;
	},
    
    getItemBySparqlID : function(id) {
        var item = null;
		// search every SemanticNode for the sparqlID
		for (var i = 0; i < this.SNodeList.length; i++) {
			item = this.SNodeList[i].getItemBySparqlID(id);
            if (item != null) {
                return item;
            }
		}
		return null;
	},
    
	// unused ?
	getArrayOfURINames : function() {
		var retval = [];
		var t = this.SNodeList.length;
		for (var l = 0; l < t; l++) {
			// output the name
			retval.push(this.SNodeList[l].getURI());
			// alert(this.SNodeList[l].getURI());
		}
		return retval;

	},

	changeSparqlID : function(obj, requestID) {
		// API call for any object with get/setSparqlID:
		// set an object's sparqlID, making sure it is legal, unique, nameHash,
		// etc...
		// return the new id, which may be slightly different than the requested
		// id.

		this.freeSparqlID(obj.getSparqlID());
		var newID = new SparqlFormatter().genSparqlID(requestID,
				this.sparqlNameHash);
		this.reserveSparqlID(newID);
		obj.setSparqlID(newID);
		return newID;
	},

	reserveSparqlID : function(id) {
		if (id != null && id != "") {
			this.sparqlNameHash[id] = 1;
		}
	},

	freeSparqlID : function(id) {
		// alert("retiring " + id);
		if (id != null && id != "") {
			delete this.sparqlNameHash[id];
		}
	},

	removeTaggedNodes : function() {

		for (var i = 0; i < this.SNodeList.length; i++) {
			if (this.SNodeList[i].getRemovalTag()) {
				// remove the current sNode from all links.
				for (var k = 0; k < this.SNodeList.length; k++) {
					this.SNodeList[k].removeFromNodeList(this.SNodeList[i]);
				}
				// remove the node from the graph
                if (this.drawable) {
                    this.graph.removeNode(this.SNodeList[i].node.id);
                }
				
				// remove the sNode from the nodeGroup
				this.SNodeList[i].node.hide();
				this.SNodeList.splice(i, 1);
				
			}
		}
		// console.log("NODES STILL REMAINING AFTER REMOVAL:");
		for (var i = 0; i < this.SNodeList.length; i++) {
			// console.log("node: " + this.SNodeList[i].getSparqlID() );
		}

	},

	clear : function() {
		// remove all nodes and redraw
		for (var k = 0; k < this.SNodeList.length; k++) {
			this.SNodeList[k].node.hide();
		}

		this.SNodeList = [];
		// this.graph = new Graph();
		// this.renderer = new Graph.Renderer.Raphael(this.divName, this.graph,
		// this.width, this.height);
		this.graph = new Graph();
		this.layouter = new Graph.Layout.Spring(this.graph, this.width, this.height);
		this.renderer = new Graph.Renderer.Raphael(this.divName, this.graph, this.width, this.height);
		this.sparqlNameHash = {};
		this.conn = null;

	},
	

	deleteNode : function(nd, recurse) {
		// delete a given node (nd), usually at its request.
		// if "recurse" is true, remove all the child nodes that connect to this
		// one, recursively.
		var sparqlIDsToRemove = [];
		var nodesToRemove = [];

		// console.log("called removal. reached node group.");

		// add the requested node
		nodesToRemove.push(nd);

		// if appropriate, get the children recursively.
		if (recurse) {
			var tempVal = this.getSubNodes(nd);
			nodesToRemove = nodesToRemove.concat(tempVal);
		} else {
			// do nothing extra at all.
		}
		// remove the nodes, as many as needed.
		for (var j = 0; j < nodesToRemove.length; j++) {
			// console.log("set " + nodesToRemove[j].getSparqlID() + " to
			// null.");
			var k = nodesToRemove[j].getSparqlIDList();
			sparqlIDsToRemove = sparqlIDsToRemove.concat(k);
			nodesToRemove[j].setRemovalTag();
		}

		// pull the nulls from the list
		this.removeTaggedNodes();

		// free sparqlIDs
		for (var i = 0; i < sparqlIDsToRemove.length; i++) {
			this.freeSparqlID(sparqlIDsToRemove[i]);
		}

	},

	
	pruneUnusedSubGraph : function(snode, optInstanceOnly) {
		// deletes a node if 
		//       - it contains no returns or constraints
		//       - it has one or less subGraphs hanging off it that contain no returns or constraints
		// also deletes those subGraphs with no returns or constraints.
		// This means it could delete the entire node group if there are not contraints or returns set.
		//
		// If node or multiple subgraphs have returns and constraints: does nothing and returns false
		// If anything was deleted: return true
		var instanceOnly = (typeof optInstanceOnly === "undefined") ? false : optInstanceOnly;
		
		if (! snode.isUsed(instanceOnly)) {
			var subNodes = this.getAllConnectedNodes(snode);
			var subGraphs = [];
			var needSubTree = [];
			var needSubTreeCount = 0;
			
			// build a subGraph for every connection
			for (var i=0; i<subNodes.length; i++) {
				subGraphs[i] = this.getSubGraph(subNodes[i], [snode]);
				needSubTree[i] = 0;
				// check to see if the subGraph contains any constraints or returns
				for (var j=0; j < subGraphs[i].length; j++) {
					if (subGraphs[i][j].isUsed(instanceOnly)) {
						needSubTree[i] = 1;
						needSubTreeCount += 1;
						break;
					}
				}
				if (needSubTreeCount > 1) break;
			}
			
			// if only one subGraph has nodes that are constrained or returned
			if (needSubTreeCount < 2) {
				
				// delete any subGraph with no returned or constrained nodes
				for (var i=0; i < subGraphs.length; i++) {
					if (!needSubTree[i]) {
						for (var j=0; j < subGraphs[i].length; j++) {
							this.deleteNode(subGraphs[i][j], false);
						}
					}
				}
				
				// recursively walk up the 'needed' subtree
				// pruning off any unUsed nodes and subGraphs
				var connList = this.getAllConnectedNodes(snode);
				this.deleteNode(snode, false);
				for (var i=0; i < connList.length; i++) {
					this.pruneUnusedSubGraph(connList[i], optInstanceOnly);
				}
				
				return true;
			}
		}
		return false;
	},
	
	pruneAllUnused : function(optInstanceOnly) {
		var instanceOnly = (typeof optInstanceOnly === "undefined") ? false : optInstanceOnly;

		// prune all unused subgraphs
		
		var pruned = [];        // sparqlID's of nodes already pruned
		var prunedSomething = true;
		
		// continue until every node has been pruned
		while (prunedSomething) {
			// list is different each time, so start over to find first unpruned node
			prunedSomething = false;
			for (var i=0; i < this.SNodeList.length; i++) {
				if (pruned.indexOf(this.SNodeList[i].getSparqlID()) == -1) {
					pruned.push(this.SNodeList[i].getSparqlID());
					this.pruneUnusedSubGraph(this.SNodeList[i], instanceOnly);
					prunedSomething = true;
					break;   // go back to "while" since this.SNodeList is now changed
				}
			}
		}
	},
	
	getOrAddNode : function(classURI, oInfo, domain, optSuperclassFlag, optOptionalFlag) {
		// return first (randomly selected) node with this URI
		// if none exist then create one and add it using the shortest path (see addClassFirstPath)
		// if superclassFlag, then any subclass of classURI "counts"
		// if optOptionalFlag: ONLY if node is added, change first nodeItem connection in path's isOptional to true
		
		// if gNodeGroup is empty: simple add
		var sNode;
		var scFlag =       (typeof optSuperclassFlag === "undefined") ? false : optSuperclassFlag;
		var optionalFlag = (typeof optOptionalFlag   === "undefined") ? false : optOptionalFlag;
		
		if (this.getNodeCount() === 0) {
			sNode = this.addNode(classURI, oInfo);
			
		} else {
			// if node already exists, return first one
			var sNodes; 
			
			// if superclassFlag, then any subclass of classURI "counts"
			if (scFlag) {
				sNodes = this.getNodesBySuperclassURI(classURI, oInfo);
			// otherwise find nodes with exact classURI
			} else {
				sNodes = this.getNodesByURI(classURI);
			}
			
			if (sNodes.length > 0) {
				sNode = sNodes[0];
			} else {
				sNode = this.addClassFirstPath(classURI, oInfo, domain, optOptionalFlag);
			}
		}
		return sNode;
	},
	
	setConnectingNodeItemsOptional : function(snode, val) {
		// set setSNodeOptional(val) on every nodeItem pointing to this snode
		
		var nItemList = this.getConnectingNodeItems(snode);
		for (var i=0; i < nItemList.length; i++) {
			nItemList[i].setSNodeOptional(snode, val);
		}
	},
	
	addClassFirstPath : function(classURI, oInfo, domain, optOptionalFlag) {
		// attach a classURI using the first path found.
		// Error if less than one path is found.
		// return the new node
		// return null if there are no paths

		// get first path from classURI to this nodeGroup
		var paths = oInfo.findAllPaths(classURI, this.getArrayOfURINames(), domain);
		if (paths.length === 0) {
			return null;
		}
		var path = paths[0];
		
		// get first node matching anchor of first path
		var nlist = this.getNodesByURI(path.getAnchorClassName());
		
		// add sNode
		var sNode = this.addPath(path, nlist[0], oInfo, false, optOptionalFlag);

		return sNode;
	},
	
	addSubclassPropertyByURI : function(snode, propURI, oInfo) {
		// add a subclass's property to this sNode
		// silently return NULL the keyname isn't found
		
		var oProp = oInfo.findSubclassProperty(snode.getURI(), propURI );
		if (oProp == null) {
			return null;
		}
		
		
		var prop = snode.addSubclassProperty(oProp.getName().getLocalName(),
				                             oProp.getRange().getLocalName(),
				                             oProp.getRange().getFullName(),
				                             oProp.getName().getFullName()
				                             );
		return prop;
	},
	
	getSingleConnectedNodeItem : function(snode) {
		// GUI helper function: 
		// find single connected nodeItem (either direction) with single connected SNode
		// otherwise null.
		
		var nItems = this.getAllConnectedConnectedNodeItems(snode);
		// one nItem && one SNode
		if (nItems.length == 1 && nItems[0].getSNodes().length == 1) {
			return nItems[0];
		} else {
			return null;
		}
		
	},
	
	//--- move start ----
    getBelmontV1 : function () {
        try {
            return new BelmontV1(this);
        } catch (e) {
            throw new Error ("BelmontV1 is not included in this VERSION >= 2.");
        }
    },
    
    generateSparqlPrefix : function(){
        var v1 = this.getBelmontV1();
        return v1.generateSparqlPrefix();
	},
    
	tabIndent : function (tab) {
		var v1 = this.getBelmontV1();
        return v1.tabIndent(tab);
	},
    
	tabOutdent : function (tab) {
        var v1 = this.getBelmontV1();
        return v1.tabOutdent(tab);
	},

	generateSparqlConstruct : function() {   
        var v1 = this.getBelmontV1();
        return v1.generateSparqlConstruct();
	},

	generateSparqlInsert : function() {
        var v1 = this.getBelmontV1();
        return v1.generateSparqlInsert();
	},
	
	generateSparqlDelete : function(postFixString, oInfo) {
        var v1 = this.getBelmontV1();
        return v1.generateSparqlDelete(postFixString, oInfo);
	},
	
	getDeletionLeader : function(postFixString, oInfo) {
        var v1 = this.getBelmontV1();
        return v1.generateSparqlDelete(postFixString, oInfo);
	},
	
	generateNodeDeletionSparql : function(nodeInScope) {
        var v1 = this.getBelmontV1();
        return v1.generateNodeDeletionSparql(nodeInScope);
	},
	getDeletionWhereBody : function (postFixString, oInfo) {
        var v1 = this.getBelmontV1();
        return v1.generateNodeDeletionSparql(postFixString, oInfo);
	},
	
	generateSparql : function(queryType, optionalFlag, optLimitOverride, optTargetObj, optKeepTargetConstraints) {
        var v1 = this.getBelmontV1();
        return v1.generateSparql(queryType, optionalFlag, optLimitOverride, optTargetObj, optKeepTargetConstraints);
	},
	
    /**
     *  optLimitOverride - if > -1 then override this.limit
     */
    generateLimitClause : function (optLimitOverride) {
        var v1 = this.getBelmontV1();
        return v1.generateLimitClause(optLimitOverride);
    },
        
	/**
	 * Very simple FROM clause logic
	 * Generates FROM clause if this.conn has
	 *     - exactly 1 serverURL
	 *     - more than one datasets (graphs)
	 */
	generateSparqlFromClause : function (tab) {
		var v1 = this.getBelmontV1();      
        return v1.generateSparqlFromClause(tab);
	},
	
	generateSparqlSubgraphClauses : function (queryType, snode, skipNodeItem, skipNodeTarget, targetObj, doneNodes, tab) {
		var v1 = this.getBelmontV1();
        return v1.generateSparqlSubgraphClauses(queryType, snode, skipNodeItem, skipNodeTarget, targetObj, doneNodes, tab);
	},
	
	generateSparqlTypeClause : function(node, TAB) {
		var v1 = this.getBelmontV1();
        return v1.generateSparqlTypeClause(node, TAB);
	},
	
    //----- move end -----
};