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

SparqlUtil.isValidURI = function(value) {
  // from https://stackoverflow.com/questions/8667070/javascript-regular-expression-to-validate-url
  // but I added random protocol [a-zA-Z]+ after ftp
  return /^(?:(?:(?:https?|ftp|[a-zA-Z]+):)?\/\/)(?:\S+(?::\S*)?@)?(?:(?!(?:10|127)(?:\.\d{1,3}){3})(?!(?:169\.254|192\.168)(?:\.\d{1,3}){2})(?!172\.(?:1[6-9]|2\d|3[0-1])(?:\.\d{1,3}){2})(?:[1-9]\d?|1\d\d|2[01]\d|22[0-3])(?:\.(?:1?\d{1,2}|2[0-4]\d|25[0-5])){2}(?:\.(?:[1-9]\d?|1\d\d|2[0-4]\d|25[0-4]))|(?:(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)(?:\.(?:[a-z\u00a1-\uffff0-9]-*)*[a-z\u00a1-\uffff0-9]+)*(?:\.(?:[a-z\u00a1-\uffff]{2,})))(?::\d{2,5})?(?:[/?#]\S*)?$/i.test(value);
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

	// buildValueConstraint : function(item, valList) {
    //       if needed use a service call  1/7/2022 -Paul
    // }

    // TODO: this should be a call to the service layer
    buildFilterInConstraint : function(item, valList) {
		// build a value constraint for an "item" (see item interface comment)
		var ret = "";

        if (item.getSparqlID() == "") {
            throw new Error("Internal: trying to build FILTER IN constraint for property with empty sparql ID");
        }
		if (valList.length > 0) {
			ret = "FILTER ( " + item.getSparqlID() + " IN (";
            ret += this.buildRDF11Literal(item, valList[0]);
			for (var i=1; i < valList.length; i++) {
                ret += ", " + this.buildRDF11Literal(item, valList[i]);
			}

			ret += " ) ) ";
		}
		return ret;
	},

    buildRDF11Literal : function(item, val) {
        var itemType = item.getValueTypes()[0];

        if (itemType == "string") {
            return "'" + val + "'";
            // Removed backwards compatibility to Virtuoso ^^String
            // return "'" + val + "', '" + val + "'^^" + SemanticNodeGroup.XMLSCHEMA_PREFIX + itemType;
        } else if (itemType == "int" || itemType == "long" || itemType == "float" || itemType == "boolean") {
            return val;
        } else if (itemType == "uri") {
            return '<' + val + '>';
        } else {
            ret += " '" + val + "'^^" + SemanticNodeGroup.XMLSCHEMA_PREFIX + itemType;
        }
    },

	buildFilterConstraint : function(item, op, val) {
		// generates a FILTER constraint
		// item:  Does basic checking on types, passing through unknown types with an unlikely best guess
		// op:    Operator
		//        Defaults to '='
		// val:   Minimum attempt to cast val to a number or integer when appropriate
		//        If val == null then use a sample legal value

		var v = val;
		var itemType = item.getValueTypes()[0];

		if (itemType == "string") {
			if (!v) v = "example";
			if (!op) op = "regex";
			if (op !== 'regex') alert("Internal error buildFilterConstraint(): string FILTER op must be 'regex'");
			ret = "FILTER regex(" + item.getSparqlID() + ', "' + v + '")';

		} else if (itemType == "int" || itemType == "long") {
			if (!v) v = 1;
			if (!op) op = "=";

			ret = 'FILTER (' + item.getSparqlID() + ' ' + op + " " + Math.floor(v) + ")";

		} else if (itemType == "float") {
			if (!v) v = 1.5;
			if (!op) op = "=";

			ret = 'FILTER (' + item.getSparqlID() + ' ' + op + " " + Number(v) + ")";

		} else if (itemType == "boolean") {
			if (!v) v = true;
			if (!op) op = "=";

			ret = 'FILTER (' + item.getSparqlID() + ' ' + op + " " + v + ")";

		} else if (itemType == "uri") {
            if (!v) v = "?other";
			if (!op) op = "!=";

            ret = 'FILTER (' + item.getSparqlID() + ' ' + op + " <" + v + ">)";

		} else if (itemType == "date") {
			if (!v) v = '1/21/2003';
			if (!op) op = "=";

			ret = 'FILTER (' + item.getSparqlID() + ' ' + op + " '" + v + "'^^" + SemanticNodeGroup.XMLSCHEMA_PREFIX + "date)";

		} else if (itemType == "dateTime") {
			if (!v) v = '2011-12-03T10:15:30';
			if (!op) op = "=";

			ret = 'FILTER (' + item.getSparqlID() + ' ' + op + " '" + v + "'^^"+ SemanticNodeGroup.XMLSCHEMA_PREFIX + "dateTime)";

		} else {
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
var NodeItem = function(uri, rangeList, jObj, nodeGroup) { // used for
																// keeping track
																// of details
																// relating two
																// semantic
																// nodes
	if (jObj) {
		this.fromJson(jObj, nodeGroup);
	} else {
		this.nodes = [];    // the semantic nodes, if one exists currently,
							// represented by this NodeItem.
		this.optionalMinus = [];
        this.qualifiers = [];
		this.rangeUris = {};
		for (var r of rangeList) {
			this.rangeUris[r] = 1;
		}
		this.connectedBy = ''; // the connection link, such as "hasSessionCode"
		this.connected = false; // toggled if a connection between this node and
								// the Semantic node owning the NodeItem list are linked.
		this.uriConnectBy = uri;
		this.deletionFlags = [];
	}
};

//deprecating
NodeItem.OPTIONAL_FALSE = 0;
NodeItem.OPTIONAL_TRUE = 1;       // everything "downstream" of this nodeItem is optional
NodeItem.OPTIONAL_REVERSE = -1;   // everything "upstream"   of this nodeItem is optional
NodeItem.MINUS_TRUE = 2;       // everything "downstream" of this nodeItem is optional
NodeItem.MINUS_REVERSE = -2;   // everything "upstream"   of this nodeItem is optional


// the functions used by the node item to keep things in order
NodeItem.prototype = {
	toJson : function() {
		// return a JSON object of things needed to serialize
		var ret = {
			SnodeSparqlIDs : [],
			OptionalMinus : [],
            Qualifiers : [],
			DeletionMarkers : [],
			range : Object.keys(this.rangeUris),
			ConnectBy : this.connectedBy,
			Connected : this.connected,
			UriConnectBy : this.uriConnectBy,

		};
		for (var i=0; i < this.nodes.length; i++) {
			ret.SnodeSparqlIDs.push(this.nodes[i].getSparqlID());
			ret.OptionalMinus.push(this.optionalMinus[i]);
            ret.Qualifiers.push(this.qualifiers[i]);
			ret.DeletionMarkers.push(this.deletionFlags[i]);
		}

		return ret;
	},
	fromJson : function(jObj, nodeGroup) {
		// presumes that SNode references have already been created
		this.nodes = [];
		for (var i = 0; i < jObj.SnodeSparqlIDs.length; i++) {
			var snode = nodeGroup.getNodeBySparqlID(jObj.SnodeSparqlIDs[i]);
			if (!snode) {
                throw new Error("Error reading JSON in NodeItem.fromJson.  Can't find node: " + jObj.SnodeSparqlIDs[i]);
			}
			this.nodes.push(snode);
		}


		this.optionalMinus = [];
		if (jObj.hasOwnProperty("OptionalMinus")) {
            // version 9
			for (var i=0; i < jObj.OptionalMinus.length; i++) {
				this.optionalMinus.push(jObj.OptionalMinus[i]);
			}
		} else if (jObj.hasOwnProperty("SnodeOptionals")) {
            // version 3:  SnodeOptionals
			for (var i=0; i < jObj.SnodeOptionals.length; i++) {
				this.optionalMinus.push(jObj.SnodeOptionals[i]);
			}
		} else{
            // older versions
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
				this.optionalMinus.push(opt);
			}
		}

        this.qualifiers = [];
		if (jObj.hasOwnProperty("Qualifiers")) {
            // version 9
			for (var i=0; i < jObj.Qualifiers.length; i++) {
				this.qualifiers.push(jObj.Qualifiers[i]);
			}
		} else {
			// backward compatibility
			for (var i=0; i < jObj.SnodeSparqlIDs.length; i++) {
				this.qualifiers.push("");
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
		
		this.rangeUris = {};
		if (jObj.hasOwnProperty("UriValueType")) {
			// older version
			this.rangeUris[jObj.UriValueType] = 1;
		} else {
			for (var i=0; i < jObj.range.length; i++) {
				this.rangeUris[jObj.range[i]] = 1;
			}
		}
		this.connectedBy = jObj.ConnectBy;
		this.connected = jObj.Connected;
		this.uriConnectBy = jObj.UriConnectBy;
	},

    /*
        Fill in this property with all the non-model values of another property
     */
    merge : function(other, target) {
        var i = other.nodes.indexOf(target);
        if (i == -1) throw "Can't find connection to " + target.getSparqlID() + " in " + this.getKeyName();

        this.nodes.push(target);
		this.optionalMinus.push(other.optionalMinus[i]);
        this.qualifiers.push(other.qualifiers[i]);
        this.deletionFlags.push(other.deletionFlags[i]);

		this.connected = true;

    },

    getIsReturned : function() {
        return false;
    },

    hasConstraints : function() {
		return false;
	},

    getRangeUris : function() {
		return Object.keys(this.rangeUris);
	},
	
	setRangeUris : function(rangeList) {
		for (var r of rangeSet) {
			this.rangeUris[r] = 1;
		}
	},
	
	setConnected : function(connect) {
		this.connected = connect;
	},
	setConnectBy : function(strConnName) {
		this.connectedBy = strConnName;
	},
	setUriConnectBy : function(strConnName) {
		this.uriConnectBy = strConnName;
	},

    // deprecated
    setSNodeOptional: function(snode, optional) {
        this.setOptionalMinus(snode, optional);
    },

	setOptionalMinus: function(snode, optionalMinus) {
		for (var i=0; i < this.nodes.length; i++) {
			if (this.nodes[i] == snode) {
				this.optionalMinus[i] = optionalMinus;
				return;
			}
		}
		throw new Error("NodeItem can't find link to semantic node");
	},

    // deprecated
    getSNodeOptional: function(snode) {
        return this.getOptionalMinus(snode);
    },

	getOptionalMinus: function(snode) {
		for (var i=0; i < this.nodes.length; i++) {
			if (this.nodes[i] == snode) {
				return this.optionalMinus[i] ;
			}
		}
		throw new Error("NodeItem can't find link to semantic node");
	},

    setQualifier: function(snode, qual) {
		for (var i=0; i < this.nodes.length; i++) {
			if (this.nodes[i] == snode) {
				this.qualifiers[i] = qual;
				return;
			}
		}
		throw new Error("NodeItem can't find link to semantic node");
	},

    getQualifier: function(snode) {
		for (var i=0; i < this.nodes.length; i++) {
			if (this.nodes[i] == snode) {
				return this.qualifiers[i] ;
			}
		}
		throw new Error("NodeItem can't find link to semantic node");
	},

	allOptionalReverse : function() {
		// Does node item make it's owner node optional
		//  1) connects to something
		//  2) all optional reverse
		if (this.optionalMinus.length == 0) {
			return false;
		}
		for (var i=0; i < this.optionalMinus.length; i++) {
			if (this.optionalMinus[i] != NodeItem.OPTIONAL_REVERSE) {
				return false;
			}
		}
		return true;
	},

	setSnodeDeletionMarker : function (snode, toDelete){
		for (var i=0; i < this.nodes.length; i++) {
			if (this.nodes[i] == snode) {
				this.deletionFlags[i] = toDelete;
				return;
			}
		}
		throw new Error("NodeItem can't find link to semantic node");
	},

	getSnodeDeletionMarker : function (snode){
		for (var i=0; i < this.nodes.length; i++) {
			if (this.nodes[i] == snode) {
				return this.deletionFlags[i] ;
			}
		}
		throw new Error("NodeItem can't find link to semantic node");
	},

	getConnectsTo : function (snode) {
		return (this.nodes.indexOf(snode) > -1);
	},

	pushSNode : function(snode, optOptional, optDeletionFlag, optQualifier){
		this.nodes.push(snode);
		this.optionalMinus.push((optOptional === undefined)     ? NodeItem.OPTIONAL_FALSE : optOptional );
		this.deletionFlags.push( (optDeletionFlag === undefined) ? false                   : optDeletionFlag);
        this.qualifiers.push( (optQualifier === undefined) ? ""                   : optQualifier);
	},

	// get values used by the NodeItem
	getKeyName : function() {
		return new OntologyName(this.uriConnectBy).getLocalName();
	},
    getValueTypes : function() {
        return [this.valueType];   // NodeItem js is still singular valueType
    },

	getConnected : function() {
		return this.connected;
	},
	getConnectBy : function() {
		return this.connectedBy;
	},
	getSNodes : function() {
		return this.nodes;
	},
    // item function
    getURI : function() {
        return this.getURIConnectBy();
    },
	getURIConnectBy : function() {
		return this.uriConnectBy;
	},

	removeSNode : function(nd) {
		// iterate over the nodes, remove the one that makes no sense.
		// console.log("calling transitive node group removal from " +
		// this.Keyname);
		for (var i = 0; i < this.nodes.length; i++) {
			if (this.nodes[i] == nd) {
				// console.log("removing " + this.nodes[i].getSparqlID);
				this.nodes.splice(i, 1);
				this.optionalMinus.splice(i,1);
                this.qualifiers.splice(i,1);
				this.deletionFlags.splice(i, 1);
			}
		}
		if (this.nodes.length === 0)
		{
			this.connected = false;
		}

	},

	getSnodesWithDeletionFlagsEnabledOnThisNodeItem :function () {
		// iterate over the nodes and return the ones that have a
		// deletion set to true.
		var retval = [];

		for( var i = 0; i < this.nodes.length; i++){
			var currNode = this.nodes[i];
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

    getItemUri: function() {
        return this.uriConnectBy;
    },
    getRangeDisplayString : function() {
		var shortList = [];
		for (var r of Object.keys(this.rangeUris)) {
			shortList.push(new OntologyName(r).getLocalName());
		}
		return shortList.join(",");
	}
};

/* the property item */
// valType      - XSD style string, e.g. "int"
// rangeU - full uri may be http://owl/whatever#int  or a local datatype http://myOntology#MyDataType
// domainURI - the property URI
// jObj - if not null then use json instead of any of these other params
var PropertyItem = function(valTypes, rangeURI, uriRelationship, jObj) {

	if (jObj) {
		this.fromJson(jObj);
	} else {
		this.valueTypes = valTypes;
		this.rangeURI = rangeURI;
		this.uriRelationship = uriRelationship;
		this.Constraints = ''; // the constraints are represented as a str and
								// will be used in the
		// in the sparql generation.
		this.fullURIName = '';
		this.SparqlID = '';
		this.isReturned = false;
        this.functions = [];
        this.binding = null;
        this.isBindingReturned = false;
		this.optMinus = 0;
		this.isRuntimeConstrained = false;
		this.instanceValues = [];
		this.isMarkedForDeletion = false;
	}
};

PropertyItem.OPT_MINUS_NONE = 0;
PropertyItem.OPT_MINUS_OPTIONAL = 1;
PropertyItem.OPT_MINUS_MINUS = 2;
PropertyItem.OPT_MINUS_EXIST = 3;

// the functions used by the property item to keep its stuff in order.
PropertyItem.prototype = {
	toJson : function() {
		// return a JSON object of things needed to serialize
		var ret = {
			valueTypes : this.valueTypes,
			rangeURI : this.rangeURI,
			UriRelationship : this.uriRelationship,
			Constraints : this.Constraints,
			SparqlID : this.SparqlID,
			isReturned : this.isReturned,
            optMinus : this.getOptMinus(),
			isRuntimeConstrained : this.getIsRuntimeConstrained(),
			instanceValues : this.instanceValues,
			isMarkedForDeletion : this.isMarkedForDeletion,
		};

        if (this.binding) {
            ret.binding = this.binding;
            ret.isBindingReturned = this.isBindingReturned;
        }

        if (this.functions.length > 0) {
            ret.functions = this.functions;
        }
		return ret;
	},
	fromJson : function(jObj) {
		// presumes SparqlID's in jObj are already reconciled with the nodeGroup
		this.valueTypes = jObj.valueTypes || [jObj.ValueType];   // support version<17 "ValueType"
		this.rangeURI = jObj.rangeURI || jObj.relationship;      // support version<17 "relationship"
		this.uriRelationship = jObj.domainURI || jObj.UriRelationship; // support version<17 "UriRelationship"
		this.Constraints = jObj.Constraints;
		this.SparqlID = jObj.SparqlID;
		this.isReturned = jObj.isReturned;
        // backwards compatible isOptional
		this.optMinus = PropertyItem.OPT_MINUS_NONE;
       	if (jObj.hasOwnProperty("isOptional")) {
       		this.optMinus =  (jObj.isOptional) ? PropertyItem.OPT_MINUS_OPTIONAL : PropertyItem.OPT_MINUS_NONE;
       	} else if (jObj.hasOwnProperty("optMinus")) {
       		this.optMinus = jObj.optMinus;
       	}
        // optional things
        if (jObj.hasOwnProperty("binding")) {
            this.binding = jObj.binding;
            this.isBindingReturned = jObj.isBindingReturned;
        } else {
            this.binding = null;
            this.isBindingReturned = false;
        }
        // optional things
        if (jObj.hasOwnProperty("functions")) {
            this.functions = jObj.functions;
        } else {
            this.functions = [];
        }

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
	buildFilterInConstraint : function(valueList) {
		//  build but don't set  a value constraint from a list of values (basic types or fully qualified URIs)
		f = new SparqlFormatter();
		return f.buildFilterInConstraint(this, valueList);
	},

	// set the values of items in the propertyItem
	setKeyName : function(keyname) {
		// deprecated
	},
	// set the insertion value. really, we should check the correct value type as we go.
	addInstanceValue : function(inval){
		this.instanceValues.push(inval);
	},

    getFunctions : function() {
        return this.functions;
    },
    setFunctions : function(list) {
        this.functions = list;
    },

    getPossibleFunctionSparqlIDs : function() {
        ret = [];
        for (var f of SemanticNodeGroup.FUNCTION_LIST) {
            ret.push(this.getFunctionSparqlID(SemanticNodeGroup.getFunctionName(f)));
        }
        return ret;
    },

    getFunctionSparqlIDs : function() {
        ret = [];
        for (var f of this.functions) {
            ret.push(this.getFunctionSparqlID(f));
        }
        return ret;
    },

    getFunctionSparqlID : function(f) {
        return this.getBindingOrSparqlID() + "_" + f;
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
    isUsed : function() {
        return (
            this.hasAnyReturn() ||
            this.hasConstraints() ||
            this.instanceValues.length > 0 ||
            this.isRuntimeConstrained ||
            this.isMarkedForDeletion ||
            this.getOptMinusIsUsed())
    },
	clearInstanceValues : function() {
		this.instanceValues = [];
	},

	setValueTypes : function(types) {
		this.valueTypes = types;
	},
	setSparqlID : function(id) {
		if (this.SparqlID != null && this.hasConstraints()) {
			this.Constraints = this.Constraints.replace(new RegExp('\\'+this.SparqlID+'\\b', 'g'), id);
		}
        if (id != null && id != "" && ! id.startsWith("?")) {
            this.SparqlID = "?" + id;
        } else {
            this.SparqlID = id;
        }

	},
	setConstraints : function(con) {
		var f = new SparqlFormatter();

		this.Constraints = f.tagSparqlID(con, this.SparqlID);
	},

    setRangeURI : function(rel) {
		this.rangeURI = rel;
	},

	setIsReturned : function(val) {
        if (val == true && this.SparqlID == "") {
            throw new Error("Internal: trying to return a property whose sparqlID is empty.");
        }
		this.isReturned = val;
	},
    setIsBindingReturned : function(val) {
		this.isBindingReturned = val;
	},

    // deprecated form of optMinus
	setIsOptional : function(bool) {
		this.optMinus = bool ? PropertyItem.OPT_MINUS_OPTIONAL : PropertyItem.OPT_MINUS_NONE;
	},

    setOptMinus : function(optMin) {
        this.optMinus = optMin;
    },

	setIsRuntimeConstrained : function(bool) {
		this.isRuntimeConstrained = bool;
	},

	// return values from the propertyItem

    hasAnyReturn : function() {
		return this.isReturned || this.isTypeReturned || this.isBindingReturned || this.functions.length > 0;
	},
    getBinding : function() {
        return this.binding;
    },

    getIsBindingReturned : function() {
        return (this.isBindingReturned && this.binding != null && this.binding != "");
	},
	getSparqlID : function() {
		return this.SparqlID;
	},
    getBindingOrSparqlID : function() {
        if (this.binding != null) {
            return this.binding;
        } else {
            return this.getSparqlID();
        }
    },
    getTypeSparqlID : function() {
		return "type_" + this.SparqlID;
	},

	getKeyName : function() {
		// return the name of the property
		return new OntologyName(this.uriRelationship).getLocalName();;
	},
    getRangeURI : function() {
        return this.rangeURI;
    },

    // item function:  three different names?  sorry.
    getURI : function() {
        return this.uriRelationship;
    },
	getUriRelationship : function() {
		return this.uriRelationship;
	},
	// named for "item"
    getItemUri: function() {
        return this.uriRelationship;
    },
	getValueTypes : function() {
		// return the type, as determined by the ontology
		return this.valueTypes;
	},
	getIsReturned : function() {
		return this.isReturned;
	},
    getIsTypeReturned : function () {
        return false;
    },

	getIsOptional : function() {
		return this.optMinus == PropertyItem.OPT_MINUS_OPTIONAL;
	},
    getOptMinus : function() {
        return this.optMinus;
    },
    // can optMinus alone make the property .isUsed() == true
    getOptMinusIsUsed : function() {
        return this.optMinus == PropertyItem.OPT_MINUS_MINUS || this.optMinus == PropertyItem.OPT_MINUS_EXIST;
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

	getItemType : function () {
		return "PropertyItem";
	},

	setIsMarkedForDeletion : function(markToSet) {
		this.isMarkedForDeletion = markToSet;
	},

	getIsMarkedForDeletion : function(){
		return this.isMarkedForDeletion;
	},
	
	// short names of Range.  For NodeItem and PropItem
	getRangeDisplayString : function() {
		return this.getValueTypes().join(",");
	}
};

/* to set nodes */
var setNode = function(SNode) {
    // deleted
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
var SemanticNode = function(nome, plist, nlist, fullName, subClassNamesUNUSED,
		nodeGroup, jObj) {

	if (jObj) {
		this.fromJson(jObj, nodeGroup);
	} else {
		this.propList = plist.slice(); // a list of properties
		this.nodeList = nlist.slice(); // a list of the nodes that this can
										// link to.
		this.fullURIName = fullName; // full name of the class
													// possible subclasses
		this.SparqlID = new SparqlFormatter().genSparqlID(
                nome,
				nodeGroup.getAllVariableNamesHash()); // always has SparqlID since it is
											// always included in the query
		this.isReturned = false;
        this.isTypeReturned = false;
        this.isConstructed = true;
        this.functions = [];
        this.binding = null;
        this.isBindingReturned = false;
		this.isRuntimeConstrained = false;
		this.valueConstraint = "";
		this.instanceValue = null;
		this.deletionMode = NodeDeletionTypes.NO_DELETE;
	}

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

	toJson : function(optDeflateFlag, optDontDeflatePropItems) {
		var deflateFlag = (optDeflateFlag === undefined) ? false : optDeflateFlag;
		var dontDeflatePropItems = (optDontDeflatePropItems === undefined) ? [] : optDontDeflatePropItems;

		// return a JSON object of things needed to serialize
		var ret = {
			propList : [],
			nodeList : [],
			fullURIName : this.fullURIName,
			SparqlID : this.SparqlID,
			isReturned : this.isReturned,
			isRuntimeConstrained : this.isRuntimeConstrained,
			valueConstraint : this.valueConstraint,
			instanceValue : this.instanceValue,
			deletionMode :  getNodeDeletionTypeName(this.deletionMode),
		};

        // optional things
        if (this.isTypeReturned) {
            ret.isTypeReturned = true;
        }

        if (this.isConstructed == false) {
            ret.isConstructed = false;
        }
        if (this.functions.length > 0) {
            ret.functions = this.functions;
        }

        if (this.binding) {
            ret.binding = this.binding;
            ret.isBindingReturned = this.isBindingReturned;
        }

		// add properties
		for (var i = 0; i < this.propList.length; i++) {
			var p = this.propList[i];
			// if deflateFlag, then only add property if returned or constrained
			if (deflateFlag == false || p.isUsed() || dontDeflatePropItems.indexOf(p) > -1) {
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

	fromJson : function(jObj, nodeGroup, optOInfo_GONE) {

		if (optOInfo_GONE !== undefined)
            throw "SemTK version error.  SemanticNode.fromJson no longer inflatesAndValidates based on oInfo.  Refresh your browser.  Check your versions."

		// presumes SparqlID's are reconciled already
		// presumes that SNodes pointed to by NodeItems already exist
		this.propList = [], this.nodeList = [];
		this.fullURIName = jObj.fullURIName;
		this.SparqlID = jObj.SparqlID;
		this.isReturned = jObj.isReturned;
        this.isConstructed = jObj.hasOwnProperty("isConstructed") ? jObj.isConstructed : true;
		this.isRuntimeConstrained = jObj.hasOwnProperty("isRuntimeConstrained") ? jObj.isRuntimeConstrained : false;
		this.valueConstraint = jObj.valueConstraint;
		this.instanceValue = jObj.instanceValue;
		this.deletionMode = jObj.hasOwnProperty("deletionMode") ? getNodeDeletionTypeByName(jObj.deletionMode) : NodeDeletionTypes.NO_DELETE;

        // optional things
        if (jObj.hasOwnProperty("isTypeReturned")) {
            this.isTypeReturned = jObj.isTypeReturned;
        } else {
            this.isTypeReturned = false;
        }

        if (jObj.hasOwnProperty("functions")) {
            this.functions = jObj.functions;
        } else {
            this.functions = [];
        }

        if (jObj.hasOwnProperty("binding")) {
            this.binding = jObj.binding;
            this.isBindingReturned = jObj.isBindingReturned;
        } else {
            this.binding = null;
            this.isBindingReturned = false;
        }

		// load JSON properties as-is
		for (var i = 0; i < jObj.propList.length; i++) {
			var p = new PropertyItem(null, null, null, jObj.propList[i]);
			this.propList.push(p);
		}

		// (val) Temporarily commenting this out: it assumes that `the nodes
		// referenced in the `nodeList` are readily available, but this was only
		// true when 1. the nodegroups could not have cycles and 2. the
		// `sNodeList` was topologically sorted.  Since we are breaking those
		// assumptions, we can no longer start creating `NodeItem` instances
		// until all `SemanticNode` instances have been created. Only then will
		// a second pass add the `NodeItem`s, see
		// `SemanticNodeGroup.prototype.addJson` for details.

		// for (var i = 0; i < jObj.nodeList.length; i++) {
		// 	var n = new NodeItem(null, [], jObj.nodeList[i], nodeGroup);
		// 	this.nodeList.push(n);
		// }

	},

	setInstanceValue : function(inval){
		this.instanceValue = inval;
	},
	getInstanceValue : function() {
		return this.instanceValue;
	},
    getFunctions : function() {
        return this.functions;
    },
    setFunctions : function(list) {
        this.functions = list;
    },

    getPossibleFunctionSparqlIDs : function() {
        ret = [];
        for (var f of SemanticNodeGroup.FUNCTION_LIST) {
            ret.push(this.getFunctionSparqlID(SemanticNodeGroup.getFunctionName(f)));
        }
        return ret;
    },

    getFunctionSparqlIDs : function() {
        ret = [];
        for (var fname of this.functions) {
            ret.push(this.getFunctionSparqlID(fname));
        }
        return ret;
    },

    getFunctionSparqlID : function(fname) {
        return this.getBindingOrSparqlID() + "_" + fname;
    },

	buildFilterConstraint : function(op, val) {
		// build but don't set a filter constraint from op and value
		f = new SparqlFormatter();
		return f.buildFilterConstraint(this, op, val);
	},
	buildFilterInConstraint : function(valueList) {
		//  build but don't set  a value constraint from a list of values (basic types or fully qualified URIs)
		f = new SparqlFormatter();
		return f.buildFilterInConstraint(this, valueList);
	},

	setSparqlID : function(id) {
		if (this.SparqlID != null && this.hasConstraints()) {

			this.valueConstraint = this.valueConstraint.replace(new RegExp('\\'+this.SparqlID+'\\b', 'g'), id);
		}
        if (id != null && id != "" && ! id.startsWith("?")) {
            this.SparqlID = "?" + id;
        } else {
            this.SparqlID = id;
        }
	},

	getSparqlID : function() {
		return this.SparqlID;
	},

    getBindingOrSparqlID : function() {
		if (this.binding != null) {
			return this.binding;
		} else {
			return this.getSparqlID();
		}
	},

    getTypeSparqlID : function() {
        return this.SparqlID + "_type";
    },

	setIsReturned : function(val) {
		this.isReturned = val;
	},
    setIsBindingReturned : function(val) {
		this.isBindingReturned = val;
	},
    setBinding : function (binding) {
		if (binding == null) {
			this.binding = null;
		} else {
			this.binding = binding.startsWith("?") ? binding : "?" + binding;
		}
	},
    setIsTypeReturned : function(val) {
		this.isTypeReturned = val;
	},
	setIsRuntimeConstrained : function(val) {
		this.isRuntimeConstrained = val;
	},
    setIsConstructed : function(val) {
        this.isConstructed = val;
    },

	setValueConstraint : function(c) {
		this.valueConstraint = c;
	},
    hasAnyReturn : function() {
		return this.isReturned || this.isTypeReturned || this.isBindingReturned || this.functions.length > 0;
	},
    getBinding : function() {
        return this.binding;
    },

    getIsBindingReturned : function() {
        return (this.isBindingReturned && this.binding != null && this.binding != "");
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
	getValueTypes : function() {
		// make this look like a propertyItem.
		return ["uri"];
	},
	getIsReturned : function() {
		return this.isReturned;
	},
    getIsTypeReturned : function() {
		return this.isTypeReturned;
	},
	getIsRuntimeConstrained : function() {
		return this.isRuntimeConstrained;
	},
    getIsConstructed : function() {
        return this.isConstructed;
    },
	isUsed : function (optInstanceOnly) {
		var instanceOnly = (optInstanceOnly === undefined) ? false : optInstanceOnly;

		if (instanceOnly) {
			if (this.instanceValue != null) return true;

			for (var i = 0; i < this.propList.length; i++) {
				if (this.propList[i].instanceValues.length > 0) {
					return true;
				}
			}
		} else {
			// does this node or its properties have any constrants or isReturned() or isTypeReturned()
			if (this.hasAnyReturn() || this.hasConstraints() || this.instanceValue != null || this.isRuntimeConstrained || this.deletionMode != NodeDeletionTypes.NO_DELETE ) {
                return true;
            }

			for (var i = 0; i < this.propList.length; i++) {
				if (this.propList[i].isUsed()) {
					return true;
				}
			}
		}
		return false;
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
			if (this.propList[s].hasAnyReturn()) {
				retprops.push(this.propList[s]);
			}
		}
		return retprops;
	},

    getUsedPropertyItems : function() {
		var retprops = [];
		var t = this.propList.length;
		for (var s = 0; s < t; s++) {
			if (this.propList[s].isUsed()) {
				retprops.push(this.propList[s]);
			}
		}
		return retprops;
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

    // item function
	getURI : function(optLocalFlag) {
		var localFlag = (optLocalFlag === undefined) ? false : optLocalFlag;

		if (localFlag) return new OntologyName(this.fullURIName).getLocalName();
		else           return this.fullURIName;
	},

    changeURI : function(newURI) {
        this.fullURIName = newURI;
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

	getConnectingNodeItems : function(otherSNode) {
		// get the node items that connects this snode to otherSNode, or []
		ret = [];
		for (var i=0; i < this.nodeList.length; i++) {
			var nItem = this.nodeList[i];
			if (nItem.getConnected()) {
				// check all of the nodes that may be connected to that locus.
				var nodeList = this.nodeList[i].getSNodes(); // get the nodes this item connects to

				for (var d = 0; d < nodeList.length; d++) {
					if (nodeList[d].getSparqlID() == otherSNode.getSparqlID()) {
						ret.push(nItem);
					}
				}
			}
		}
		return ret;
	},

	/**
		Get all nodes connected by nodeItem (object properties) facing outward
		
		This includes self and duplicates
	 */
	getConnectedNodes : function() {
		
		var retval = [];
	   	for (var nItem of this.nodeList) {
            if (nItem.getConnected()) {
                for (var snode of nItem.getSNodes()) {
                    if (retval.indexOf(snode) == -1) {
                        retval.push(snode);
                    }
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
			if (this.propList[i].getURI() == uriRel) {
				return this.propList[i];
			}
		}
		return null;
	},
    // Danger: deprecate this. It is "legal" for two nodeItems to share keyname
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
    // TODO: could be indeterminate with UNION queries or anywhere binding is shared
    getItemBySparqlID : function(sparqlID) {
        // check self
        if (this.getSparqlID() == sparqlID || this.getTypeSparqlID() == sparqlID || this.getBinding() == sparqlID) {
            return this;
        }

        // only property items have sparqlID's.
		for (var i = 0; i < this.propList.length; i++) {
			if (this.propList[i].getSparqlID() == sparqlID || this.propList[i].getBinding() == sparqlID) {
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

	ownsNodeItem : function (nodeItem) {
		return this.nodeList.indexOf(nodeItem) > -1;
	},

    // deprecated bad name
    getPropList : function() {
		return this.propList;
	},

    getPropertyItems : function() {
		return this.propList;
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

	addSubclassProperty : function (valTypes, rangeUri, uriRelation ) {
		// force-add a subclass property.   So it must be optional.
		prop = new PropertyItem(valTypes, rangeUri, uriRelation);
		prop.optMinus = PropertyItem.OPT_MINUS_OPTIONAL;
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

    getAreEdgesMarkedForDeletion : function() {
        return this.deletionMode != NodeDeletionTypes.NO_DELETE &&
               this.deletionMode != NodeDeletionTypes.TYPE_INFO_ONLY;
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

var OrderElement = function(sparqlID, func) {
    this.sparqlID = (sparqlID === undefined) ? "" : sparqlID;
    this.func = (func === undefined) ? "" : func;
    this.fixID();
};

OrderElement.prototype = {
    fromJson : function (jObj) {
        // support reading old inconsistent capitalization
		if (jObj.hasOwnProperty("sparqlID")) {
            this.sparqlID = jObj.sparqlID;
        } else {
            this.sparqlID = jObj.SparqlID;
        }

        if (jObj.hasOwnProperty("func")){
            this.func = jObj.func;
        } else {
            this.func = "";
        }
        this.fixID();
    },

    fixID : function () {
        if (this.sparqlID != "" && this.sparqlID[0] != "?") {
            this.sparqlID = "?" + this.sparqlID;
        }
    },

    getSparqlID : function() {
        return this.sparqlID;
    },

    setSparqlID : function(id) {
        this.sparqlID = id;
        this.fixID();
    },

    getFunc : function() {
        return this.func;
    },

    setFunc : function(f) {
        this.func = f;
    },

    toJson : function() {
        var jObj = {};
        jObj.sparqlID = this.sparqlID;
        if (this.func != "") {
            jObj.func = this.func;
        }
        return jObj;
    },

    toSparql : function() {
        if (this.func != "") {
            return this.func + "(" + this.sparqlID + ")";
        } else {
            return this.sparqlID;
        }
    },

    deepCopy : function() {
        var o = new OrderElement();
        o.fromJson(this.toJson());
        return o;
    }
};

var UnionMembership = function() {
    this.idToListHash = {};
};

/* the semantic node group */
var SemanticNodeGroup = function() {
	this.SNodeList = [];
    this.limit = 0;
    this.offset = 0;
    this.orderBy = [];
    this.groupBy = [];
    this.queryType = null;
    this.returnTypeOverride = null;
    this.columnOrder = [];

	this.sparqlNameHash = {};

	this.conn = null;       // optional relevant SparqlConnection.
	                        // this will need to be non-optional and expanded in the upcoming versions.

	this.prefixHash = {};
	this.prefixNumberStart = 0;

    this.unionHash = {};   // list of unionValStrings specifying the branch points that define the union
                           // this unionhash.int_id = [unionValStr, unionValStr]
    this.tmpUnionMembersHash = {};  //  list of unions for each item that has any.  Deepest first.  So zeroth is most important.
                                    // hash[memberValStr] = [deepest, nextdeep, ...]
                                    // temporarily built and maintained on the javascript side
                                    // This is difficult to maintain during nodegroup editing,
                                    // so it is re-generated when needed.
    this.tmpUnionParentHash = {};   // same key as tmpUnionMembersHash
                                    // value is a hash of unionKey->parentValStr
                                    // where parent is one of the items in tmpUnionMemberHash[key]
};

SemanticNodeGroup.JSON_VERSION = 20;
// version 20 - ingest with subtypes
// version 19 - fixed buggy wrong name propertyItem.domainURI back to old propertyItem.UriRelationship
// version 18 - complex ranges
// version 17 - propertyItem domainURI, rangeURI, valueTypes (plural)
// version 16 - columnOrder
// version 15 - node.isConstructed  ng.queryType ng.returnTypeOverride
// version 14 - added functions (optional)
// version 13 - removed node.NodeName
// version 12 - unionHash
// version 11 - import spec has dataValidator
// version 10 - (accidentally wasted in a push)
// version 9 - minus links
// version 8 - fixes to order by
// version 7 - offset, order by
// version 6 - limit
// version 5 - top-secret undocumented version
// version 4 - multiple connections
// version 3 - SNodeOptionals


SemanticNodeGroup.XMLSCHEMA_PREFIX = "XMLSchema:";
SemanticNodeGroup.XMLSCHEMA_FULL = "http://www.w3.org/2001/XMLSchema#";
SemanticNodeGroup.INSERT_PREFIX = "generateSparqlInsert:";
SemanticNodeGroup.INSERT_FULL = "belmont/generateSparqlInsert#";

SemanticNodeGroup.NULL_TARGET = "Nu11T@rG3t";

// Java AutoGeneratedQueryTypes
SemanticNodeGroup.QT_DISTINCT = "SELECT_DISTINCT";
SemanticNodeGroup.QT_CONSTRAINT = "FILTER_CONSTRAINT";
SemanticNodeGroup.QT_COUNT = "COUNT";
SemanticNodeGroup.QT_CONSTRUCT = "CONSTRUCT";
SemanticNodeGroup.QT_ASK = "ASK";
SemanticNodeGroup.QT_DELETE = "DELETE";

SemanticNodeGroup.RT_TABLE = "TABLE";
SemanticNodeGroup.RT_JSONLD = "GRAPH_JSONLD";
SemanticNodeGroup.RT_CONFIRM = "CONFIRM";
SemanticNodeGroup.RT_OWLRDF = "RDF";
SemanticNodeGroup.RT_HTML = "HTML";
SemanticNodeGroup.RT_NTRIPLES = "N_TRIPLES";

SemanticNodeGroup.FUNCTION_MIN = 0;
SemanticNodeGroup.FUNCTION_MAX = 1;
SemanticNodeGroup.FUNCTION_COUNT = 2;
SemanticNodeGroup.FUNCTION_AVG = 3;
SemanticNodeGroup.FUNCTION_SUM = 4;
SemanticNodeGroup.FUNCTION_SAMPLE = 5;
SemanticNodeGroup.FUNCTION_GROUP_CONCAT = 6;

SemanticNodeGroup.FUNCTION_LIST = [
    SemanticNodeGroup.FUNCTION_MIN,
    SemanticNodeGroup.FUNCTION_MAX,
    SemanticNodeGroup.FUNCTION_COUNT,
    SemanticNodeGroup.FUNCTION_AVG,
    SemanticNodeGroup.FUNCTION_SUM,
    SemanticNodeGroup.FUNCTION_SAMPLE,
    SemanticNodeGroup.FUNCTION_GROUP_CONCAT,
];

SemanticNodeGroup.getFunctionName = function(f) {
    if (f == SemanticNodeGroup.FUNCTION_MIN) return "MIN";
    else if (f == SemanticNodeGroup.FUNCTION_MAX) return "MAX";
    else if (f == SemanticNodeGroup.FUNCTION_COUNT) return "COUNT";
    else if (f == SemanticNodeGroup.FUNCTION_AVG) return "AVG";
    else if (f == SemanticNodeGroup.FUNCTION_SUM) return "SUM";
    else if (f == SemanticNodeGroup.FUNCTION_SAMPLE) return "SAMPLE";
    else if (f == SemanticNodeGroup.FUNCTION_GROUP_CONCAT) return "GROUP_CONCAT";
    else return null;
};

SemanticNodeGroup.prototype = {


	toJson : function(optDeflateFlag, optDontDeflatePropItems) {
		var deflateFlag = (optDeflateFlag === undefined) ? false : optDeflateFlag;
		var dontDeflatePropItems = (optDontDeflatePropItems === undefined) ? [] : optDontDeflatePropItems;

		// get list in order such that linked nodes always preceed the node that
		// links to them
		var snList = this.getOrderedNodeList().reverse();

		var ret = {
			version : SemanticNodeGroup.JSON_VERSION,
            limit : this.limit,
            offset : this.offset,
			sNodeList : [],
            orderBy : [],
            groupBy : [],
            unionHash : {}
		};

        // optional things
        if (this.queryType != null && this.queryType != SemanticNodeGroup.QT_DISTINCT) {
            ret.queryType = this.queryType;
        }
        if (this.returnTypeOverride) {
            ret.returnTypeOverride = this.returnTypeOverride;
        }
        if (this.columnOrder != null && this.columnOrder != []) {
            this.removeInvalidColumnOrder();
            ret.columnOrder = this.columnOrder;
        }

		// add json snodes to sNodeList
		for (var i = 0; i < snList.length; i++) {
			ret.sNodeList.push(snList[i].toJson(deflateFlag, dontDeflatePropItems));
		}

        // orderBy
        for (var i = 0; i < this.orderBy.length; i++) {
            ret.orderBy.push(this.orderBy[i].toJson());
        }

        // groupBy
        if (this.groupBy.length > 0) {
            ret.groupBy = [];
            for (s of this.groupBy) {
                ret.groupBy.push(s);
            }
        }
        // unionHash
        for (var k in this.unionHash) {
            ret.unionHash[k.toString()] = this.unionHash[k].slice();
        }

		return ret;
	},

	addJson : function(jObj) {

		if (jObj.version > SemanticNodeGroup.JSON_VERSION) {
			throw new Error("\nJavascript is old. Browser may need a hard refresh. https://en.wikipedia.org/wiki/Wikipedia:Bypass_your_cache \nbelmont.js supports version " + SemanticNodeGroup.JSON_VERSION + " but nodegroup json is version " + jObj.version);
		}

        if (jObj.hasOwnProperty("limit")) {
            this.limit = jObj.limit;
        }

        if (jObj.hasOwnProperty("offset")) {
            this.offset = jObj.offset;
        }

        if (jObj.hasOwnProperty("queryType")) {
            this.queryType = jObj.queryType;
        }

        if (jObj.hasOwnProperty("returnTypeOverride")) {
            this.returnTypeOverride = jObj.returnTypeOverride;
        }

        if (jObj.hasOwnProperty("columnOrder")) {
            this.columnOrder = jObj.columnOrder;
        }

		for (var i = 0; i < jObj.sNodeList.length; i++) {
		}

		// (val) We temporarily store the semantic nodes by index, just so that
		// the second pass can easily re-access them by index.
		let snodes = []

		// loop through SNodes in the json
		for (var i = 0; i < jObj.sNodeList.length; i++) {
			snodes[i] = new SemanticNode(null, null, null, null, null, this,
					jObj.sNodeList[i]);

			// add the node without messing with any connections...they are
			// already there.
			this.addOneNode(snodes[i], null, null, null, false);
		}

		// (val) In this second pass, we can now create `NodeItem`s since the
		// `SemanticNode`s they will refer to have all been created.
		for (var i = 0; i < jObj.sNodeList.length; i++) {
			const jObjSNode = jObj.sNodeList[i]
			const snode = snodes[i]
			for (var j = 0; j < jObjSNode.nodeList.length; j++) {
				var n = new NodeItem(null, [], jObjSNode.nodeList[j], this);
				snode.nodeList.push(n);
			}
		}

        if (jObj.hasOwnProperty("orderBy")) {
            for (var i=0; i < jObj.orderBy.length; i++) {
                var j = jObj.orderBy[i];
                var o = new OrderElement();
                o.fromJson(j);
                this.appendOrderBy(o);
            }
        }

        if (jObj.hasOwnProperty("groupBy")) {
            for (var i=0; i < jObj.groupBy.length; i++) {
                var j = jObj.groupBy[i];
                this.appendGroupBy(j);
            }
        }
        this.validateOrderBy();
        this.validateGroupBy();

        // unionHash
        this.unionHash = {};
        if (jObj.version >= 12) {
            for (var k in jObj.unionHash) {
                this.unionHash[parseInt(k)] = jObj.unionHash[k].slice();
            }
		}
	},

	deepCopy : function() {
		// use the json functionality to implement deepCopy
		// maybe less efficient but keeps avoids double implementation - Paul
		var ret = new SemanticNodeGroup();
		ret.addJson(this.toJson());

		// connection
        var conn = null;
        if (this.conn != null) {
		  var conn = new SparqlConnection();
		  conn.fromJson(this.conn.toJson());
        }
		ret.setSparqlConnection(conn);

		return ret;
	},

    // Build keystring for a union item.  One of:
    //  snode
    //  snode propItem
    //  snode nodeItem target rev_flag

    buildUnionValueStr : function(snode, optItem, optTarget, optReverse_flag) {
        if (optTarget !== undefined) {
            // nodeItem
            return snode.getSparqlID() + "|" + optItem.getURI() + "|" + optTarget.getSparqlID() + "|" + String(optReverse_flag);

        } else if (optItem !== undefined) {
            // propItem
            return snode.getSparqlID() + "|" + optItem.getURI();

        } else {
            // node
            return snode.getSparqlID();
        }
    },

    buildPropItemStr : function(prop) {
        return this.buildItemStr(this.getPropertyItemParentSNode(prop), prop);
    },

    buildItemStr : function(snode, optItem, optTarget) {
        if (optItem && optItem.getItemType() == "NodeItem") {
            if (optTarget) {
                return snode.getSparqlID() + "|" + optItem.getURI() + "|" + optTarget.getSparqlID();
            } else {
                return snode.getSparqlID() + "|" + optItem.getURI() + "|" + SemanticNodeGroup.NULL_TARGET;
            }
        } else {
            return this.buildUnionValueStr(snode, optItem);
        }
    },

    // query type, default to SELECT DISTINCT
    getQueryType : function() {
        return this.queryType ? this.queryType : SemanticNodeGroup.QT_DISTINCT;
    },

    setQueryType : function(qt) {
        this.queryType = qt;
    },

    getReturnTypeOverride : function() {
        return this.returnTypeOverride;
    },

    setReturnTypeOverride : function(rt) {
        this.returnTypeOverride = rt;
    },

    getColumnOrder : function() {
        return this.columnOrder;
    },

    setColumnOrder : function(strList) {
        this.columnOrder = strList;
    },

    // get text for menu
    getUnionValueStrName : function (str) {
        var entry = str.split("|");
        if (entry.length == 1) {
            return entry[0];
        } else {
            return (new OntologyName(entry[1])).getLocalName();
        }
    },

    // get [snode, node_item, target, reverse_flag]
    //     [snode, node_item, target]
    //     [snode, prop_item]
    //     [snode]
    //
    //  node_item target can be null if non-union-related item string
    getEntryTuple : function(str) {
        var entry = str.split("|");
        var snode = this.getNodeBySparqlID(entry[0]);
        if (entry.length == 1) {
            return [snode];
        } else if (entry.length == 2) {
            return [snode, snode.getPropertyByURIRelation(entry[1])];
        } else if (entry.length == 3) {
            return [snode,
                    snode.getNodeItemByURIConnectBy(entry[1]),
                    entry[2] == SemanticNodeGroup.NULL_TARGET ? null : this.getNodeBySparqlID(entry[2])];
        } else {
            return [snode,
                    snode.getNodeItemByURIConnectBy(entry[1]),
                    entry[2] == SemanticNodeGroup.NULL_TARGET ? null : this.getNodeBySparqlID(entry[2]),
                    entry[3] == "true"];
        }
    },


    // get an id for a new union
    newUnion : function () {
        var ret = 1;
        while (this.unionHash.hasOwnProperty(ret)) {
            ret += 1;
        }
        this.unionHash[ret]=[];
        return ret;
    },

    rmUnion : function(id) {
        delete this.unionHash[id];
    },


    getUnionNameStr : function(id) {
        var names = [];
        if (id == null) {
            return "Union: unnamed";
        }

        for (var str of this.unionHash[id]) {
            names.push(this.getUnionValueStrName(str))
        }
        return "Union: " + names.join(",")
    },

    // rm item from unionHash
    // silent if item is not in unionHash
    rmFromUnions : function(snode, item, optTarget) {
        var lookup1 = undefined;
        var lookup2 = "-";

        if (optTarget === undefined) {
            lookup1 = this.buildUnionValueStr(snode, item);
        } else {
            lookup1 = this.buildUnionValueStr(snode, item, optTarget, true);
            lookup2 = this.buildUnionValueStr(snode, item, optTarget, false);
        }

        for (var key in this.unionHash) {
            for (var i=0; i < this.unionHash[key].length; i++) {

                if (this.unionHash[key][i] == lookup1 || this.unionHash[key][i] == lookup2) {
                    this.unionHash[key].splice(i,1);
                    if (this.unionHash[key].length == 0) {
                        this.rmUnion(key);
                    }
                    return;
                }
            }
        }
    },

    // add item to union
    // params:
    //      id :
    //      item, optIndex : don't belong to any union yet.
    addToUnion : function(id, snode, item, optTarget, optReverseFlag) {
        if (!this.unionHash.hasOwnProperty(id)) {
            this.unionHash[id] = [];
        }
        this.unionHash[id].push(this.buildUnionValueStr(snode, item, optTarget, optReverseFlag));
    },

    getAllUnionKeys : function() {
        return Object.keys(this.unionHash);
    },

    // get union keys of snode and its props and nodeitems
    getUnionKeyList : function(snode) {
        var ret = [];
        u = this.getUnionKey(snode);
        if (u) {
            ret.push(u);
        }
        for (var p of snode.getPropertyItems()) {
            u = this.getUnionKey(snode, p);
            if (u) {
                ret.push(u);
            }
        }
        for (var n of snode.getNodeList()) {
            for (var t of n.getSNodes()) {
                u = this.getUnionKey(snode, n, t);
                if (u) {
                    ret.push(Math.abs(u));
                }
            }
        }
        return ret;
    },

    // get union key for this item
    //     negative if nodeitem with reverse_flag == true
    //     null if none
    getUnionKey : function (snode, optItem, optTarget) {
        var lookup1 = "@";
        var lookup2 = "@";
        if (optTarget === undefined) {
            lookup1 = this.buildUnionValueStr(snode, optItem);
        } else {
            lookup1 = this.buildUnionValueStr(snode, optItem, optTarget, false);
            lookup2 = this.buildUnionValueStr(snode, optItem, optTarget, true);
        }
        for (var key in this.unionHash) {
            for (var valStr of this.unionHash[key]) {
                if (valStr == lookup1) {
                    return key;
                } else if (valStr == lookup2) {
                    return -key;
                }
            }
        }
        return null;
    },

    addtoUnionMembershipHashes : function(key, parentEntryStr, snode, optItem, optTarget) {
        var entryStr = this.buildItemStr(snode, optItem, optTarget);

        if (this.tmpUnionMembersHash[entryStr] == undefined) {
            this.tmpUnionMembersHash[entryStr] = [];
        }
        this.tmpUnionMembersHash[entryStr].push(key);

        if (this.tmpUnionParentHash[entryStr] == undefined) {
            this.tmpUnionParentHash[entryStr] = {};
        }
        this.tmpUnionParentHash[entryStr][key] = parentEntryStr;
    },

    // expensive operation calculates all union memberships
    updateUnionMemberships  : function() {

        this.tmpUnionMembersHash = {};  // hash entry str (3 tuple, not 4) to a list of unions
        this.tmpUnionParentHash = {};   // hash same entry str to a hash:   hash unionKey to parentValStr

        // loop through all unionHash entries
        for (var unionKey in this.unionHash) {
            // for each parent item in the un
            for (var entryStr of this.unionHash[unionKey]) {
                var entry = this.getEntryTuple(entryStr);
                if (entry.length == 2) {
                    // propertyItem: easy add
                    this.addtoUnionMembershipHashes(unionKey, entryStr, entry[0], entry[1]);
                } else {
                    // get list of nodes in the Union
                    var subgraphNodeList;
                    if (entry.length == 1) {
                        subgraphNodeList = this.getSubGraph(entry[0], []);
                    } else {
                        // nodeItems

                        // get subgraph to add
                        if (entry[3] == false) {
                            this.addtoUnionMembershipHashes(unionKey, entryStr, entry[0], entry[1], entry[2]);
                            subgraphNodeList = this.getSubGraph(entry[2], [entry[0]]);
                        } else {
                            subgraphNodeList = this.getSubGraph(entry[0], [entry[2]]);
                        }
                    }

                    // add the nodes
                    for (var subgraphNode of subgraphNodeList) {
                        // add the node
                        this.addtoUnionMembershipHashes(unionKey, entryStr, subgraphNode);

                        // add its props
                        for (var prop of subgraphNode.getUsedPropertyItems()) {
                            this.addtoUnionMembershipHashes(unionKey, entryStr, subgraphNode, prop);
                        }

                        // add its connected nodeItems
                        var nodeItemList = subgraphNode.getNodeList();
                        for (var nodeItem of nodeItemList) {
                            var targetSNodes = nodeItem.getSNodes();
                            for (var target of targetSNodes) {
                                // - don't need membershipList, collapse it below (in this function)
                                // - fix getUnionMembership  (document that "boss" is also a member)
                                // - fix get LegalUnions
                                this.addtoUnionMembershipHashes(unionKey, entryStr, subgraphNode, nodeItem, target);
                            }
                        }
                    }
                }
            }
        }

        var deepestFirst = function(union0, union1) {
            return this.getUnionDepth(union1) - this.getUnionDepth(union0);
        }.bind(this);

        // rearrange so entry [0] is the parent and grandparents are later
        for (var keyStr in this.tmpUnionMembersHash) {
            this.tmpUnionMembersHash[keyStr].sort(deepestFirst);
        }
    },

    getUnionDepth : function(unionKey) {
        var firstEntry = this.getEntryTuple(this.unionHash[unionKey][0]);
        return this.tmpUnionMembersHash[this.buildItemStr(firstEntry[0], firstEntry[1], firstEntry[2])].length;
    },

    getUnionMembershipList : function(snode, optItem, optTarget) {
        var keyVal = this.buildItemStr(snode, optItem, optTarget);
        return this.tmpUnionMembersHash[keyVal] || [];
    },
    // Get the most deeply nested union to which this item belongs, or null
    //
    // call updateUnionMemberships() first.
    // then call this multiple times with no intervening nodegroup edits
    getUnionMembership : function(snode, optItem, optTarget) {
        var memberOfList = this.getUnionMembershipList(snode, optItem, optTarget);

        return memberOfList[0] || null;

    },

    getUnionParentStr : function(snode, optProp) {
        var unionKey = this.getUnionMembership(snode, optProp);
		if (unionKey == null) {
			return null;
		} else {
            var keyStr = this.buildItemStr(snode, optProp);
			return this.tmpUnionParentHash[keyStr][unionKey];
		}
    },

    // Get list of union keys which this item could reasonably join
    //
    // call updateUnionMemberships() first.
    getLegalUnions : function(snode, optItem, optTarget, optReverse) {
        var ret = [];

        // (remember that membership lists are sorted closest to furthest)
        // get membership list.  Remove unionKey, if any
        // result: all unions item is a member of but NOT a key member
        var membershipList = this.getUnionMembershipList(snode, optItem, optTarget);
        var key = this.getUnionKey(snode, optItem, optTarget);
        key = (key == null) ? null : Math.abs(key);
        if (membershipList[0] == key) {
            membershipList = membershipList.slice().splice(1);
        }
        var membershipStr = JSON.stringify(membershipList);

        // search first member of each union
        for (var unionKey in this.unionHash) {
            var firstMemberStr = this.unionHash[unionKey][0];
            var firstMemberEntry = this.getEntryTuple(firstMemberStr);
            var firstMemberMembership = this.getUnionMembershipList(firstMemberEntry[0], firstMemberEntry[1], firstMemberEntry[2]);
            // first member's membership[0] is always its union key.  Remove it.
            firstMemberMembership = firstMemberMembership.slice().splice(1)  // make a copy so we don't break the hash

            // add to ret union non-key memberships match this union's first member's non-key memberships.
            var firstMembershipStr = JSON.stringify(firstMemberMembership);
            if (firstMembershipStr == membershipStr) {
                ret.push(parseInt(unionKey));
            }
        }

        // remove illegally connected unions for snodes or nodeitems
        if (optItem == undefined || optTarget != undefined) {
            var startNode;
            var stopNodes;

            if (optItem == undefined) {
                // snode:  anything connected is illegal
                startNode = snode;
                stopNodes = [];
            } else {
                // nodeItem:  anything downstream is illegal
                if (optReverse) {
                    startNode = snode;
                    stopNodes = [optTarget];
                } else {
                    startNode = optTarget;
                    stopNodes = [snode];
                }
            }
            subgraph = this.getSubGraph(startNode, stopNodes);

            // find all illegal union keys to remove
            var illegals = [];
            for (var nd of subgraph) {
                illegals = illegals.concat(this.getUnionKeyList(nd));
            }

            // remove illegals
            for (var ill of illegals) {
                // don't remove key.  It slips in if reverseFlag==true.
                if (ill != key) {
                    var idx = ret.indexOf(ill);
                    if (idx > -1) {
                        ret.splice(idx,1);
                    }
                }
            }
        }

        return ret;
    },

    /*
        Move all the non-model-related content of prop into
        an existing prop newURI.
        Remove prop from it's parent.
     */
    mergeNodeItem : function(nodeItem, target, mergeIntoURI) {
        var snode = this.getNodeItemParentSNode(nodeItem);
        var mergeIntoNItem = snode.getNodeItemByURIConnectBy(mergeIntoURI);
        if (mergeIntoNItem == null) throw "Can't find node item edge to merge into: " + mergeIntoURI;

        mergeIntoNItem.merge(nodeItem, target);

        // here: todo doesn't merge incorrect range.
        //    1 - how did inflate and validate leave things?
        //    2 - shouldn't we do this in java
        //    3 - needs oinfo?
        //    4 - should also delete the nodeItem if it is incorrect and no longer connected
        //    5 - repeat for mergeProperty (pls "mergePropItem") and mergeIntoURI ("mergeSNode")

        //snode.rmPropItem(prop);

        return mergeIntoNItem;
    },
    /**
	 * Get all variable names in the nodegroup except those in same union and different parent as targetItem
     * and not my object's own name
	 */
    getAllVariableNames : function (optTargetSNode, optTargetPItem) {
        return Object.keys(this.getAllVariableNamesHash());
    },

    getAllVariableNamesHash : function (optTargetSNode, optTargetPItem) {
		this.updateUnionMemberships();

        var targetUnion = (optTargetSNode == undefined) ? null : this.getUnionMembership(optTargetSNode, optTargetPItem);
        var targetParentStr = (optTargetSNode == undefined) ? null : this.getUnionParentStr(optTargetSNode, optTargetPItem);

        var ret = {};

		for (var snode of this.SNodeList) {
			var snodeUnion = this.getUnionMembership(snode);
			var snodeParentStr = this.getUnionParentStr(snode);
            // skipping self..
            if (optTargetPItem != undefined || snode != optTargetSNode) {
    			// if different union or no union or same union parent
    			if (snodeUnion != targetUnion || snodeParentStr == null || targetParentStr == snodeParentStr) {
    				ret[snode.getSparqlID()] = 1;
    				if (snode.getBinding() != null) {
    					ret[snode.getBinding()] = 1;
    				}
    				if (snode.getIsTypeReturned()) {
    					ret[snode.getTypeSparqlID()] = 1;
    				}

                    for (var id of snode.getFunctionSparqlIDs()) {
                        ret[id] = 1;
                    }
    			}
            }

			for (var prop of snode.getPropertyItems()) {
                // skipping self
                if (prop != optTargetPItem) {
    				var propUnion = this.getUnionMembership(snode, prop);
    				var propParentStr = this.getUnionParentStr(snode, prop);
    				// if different union or no union or same union parent
    				if (propUnion != targetUnion || propParentStr == null || targetParentStr == propParentStr) {
    					if (prop.getSparqlID() != null && prop.getSparqlID() != "") {
    						ret[prop.getSparqlID()] = 1;
    					}
    					if (prop.getBinding() != null) {
    						ret[prop.getBinding()] = 1;
    					}
                        for (var id of prop.getFunctionSparqlIDs()) {
                            ret[id] = 1;
                        }
    				}
                }
			}
		}
		return ret;
	},


    /*
    ** Set isReturned, or isTypeReturned, or setIsBindingReturned
    ** for anything in the nodegroup that matches varname
    */
    setIsReturned : function (varname, bool) {
        for(var snode of this.SNodeList) {
            if (snode.getSparqlID() == varname) {
                snode.setIsReturned(bool);
            }
            if (snode.getTypeSparqlID() == varname) {
                snode.setIsTypeReturned(bool);
            }
            if (snode.getBinding() == varname) {
                snode.setIsBindingReturned(bool);
            }

            for (var pItem of snode.propList) {
                if (pItem.getSparqlID() == varname) {
                    pItem.setIsReturned(bool);
                }
                if (pItem.getBinding() == varname) {
                    pItem.setIsBindingReturned(bool);
                }
			}
        }
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
				this.addToPrefixHash(pi.getURI());
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

    setLimit : function (l) {
        if (typeof l !== "number" || isNaN(l)) {
            this.limit = 0;
        }
        this.limit = l;
    },

    getLimit : function () {
        return this.limit;
    },

    setOffset : function (l) {
        if (typeof l !== "number" || isNaN(l)) {
            this.offset = 0;
        }
        this.offset = l;
    },

    getOffset : function () {
        return this.offset;
    },

    getOrderBy : function () {
        return this.orderBy;
    },

    setOrderBy : function (orderElemList) {
        this.orderBy = orderElemList;
    },

    clearOrderBy : function () {
        this.orderBy = [];
    },

    appendOrderBy : function (oElem) {
        this.orderBy.push(oElem);
    },

    // remove any invalid ORDER BY
    removeInvalidOrderBy : function() {
        var keep = [];
        var returnedSparqlIDs = this.getReturnedSparqlIDs();
        for (var i=0; i < this.orderBy.length; i++) {
            var e = this.orderBy[i];
            if (returnedSparqlIDs.indexOf(e.getSparqlID()) != -1) {
                keep.push(e);
            }
        }

        this.orderBy = keep;
    },

    updateOrderBy : function(oldID, newID) {

        for (var o of this.orderBy) {
            if (o.getSparqlID() == oldID) {
                o.setSparqlID(newID);
            }
        }
    },

    validateOrderBy: function() {
        var ids = this.getReturnedSparqlIDs();
        for (var i=0; i < this.orderBy.length; i++) {
            var e = this.orderBy[i];
            if (ids.indexOf(e.getSparqlID()) == -1) {
                throw new Error("Invalid SparqlID in ORDER BY : " + e.getSparqlID());
            }
        }
    },

    /**
     * Set orderBy to every returned item.
     * (To ensure a deterministic return order for OFFSET)
     * @throws Exception
     */
    orderByAll : function() {
        this.clearOrderBy();
        var rList = this.getReturnedItems();
        for (var i=0; i < rList.length; i++) {
            r = rList[i];
            this.appendOrderBy(new OrderElement(r.getSparqlID(), ""));
        }
    },

    getGroupBy : function () {
        return this.groupBy;
    },

    setGroupBy : function (strList) {
        this.groupBy = strList;
    },

    clearGroupBy : function () {
        this.groupBy = [];
    },

    appendGroupBy : function (oElem) {
        this.groupBy.push(oElem);
    },

    // remove invalid GROUP BY
    // optionally update one name
    removeInvalidGroupBy : function() {
        var keep = [];
        var returnedSparqlIDs = this.getReturnedSparqlIDs(true);
        for (var i=0; i < this.groupBy.length; i++) {
            var s = this.groupBy[i];
            if (returnedSparqlIDs.indexOf(s) != -1) {
                keep.push(s);
            }
        }

        this.groupBy = keep;
    },

    removeInvalidColumnOrder : function() {
        var keep = [];
        var returnedSparqlIDs = this.getReturnedSparqlIDs();
        for (var i=0; i < this.columnOrder.length; i++) {
            var s = this.columnOrder[i];
            if (returnedSparqlIDs.indexOf(s) != -1 && keep.indexOf(s) == -1) {
                keep.push(s);
            }
        }

        this.columnOrder = keep;
    },

    updateGroupBy : function(oldID, newID) {
        for (var i=0; i < this.groupBy.length; i++) {
            if (this.groupBy[i] == oldID) {
                this.groupBy[i] = newID;
            }
        }
    },
    validateGroupBy: function() {
        var returnedSparqlIDs = this.getReturnedSparqlIDs(true);
        for (var i=0; i < this.groupBy.length; i++) {
            var s = this.groupBy[i];
            if (returnedSparqlIDs.indexOf(s) == -1) {
                throw new Error("Invalid SparqlID in GROUP BY : " + s);
            }
        }
    },
    setOffset : function(offset) {
        this.offset = offset;
    },

	setCanvasOInfo : function (oInfo) {
            // deleted
	},

	setAsyncPropEditor : function (func) {
			// deleted
	},
	setAsyncNodeEditor : function (func) {
			// deleted
	},
	setAsyncLinkBuilder : function (func) {
			// deleted
	},
	setAsyncLinkEditor : function (func) {
			// deleted
	},
	setAsyncSNodeEditor : function (func) {
			// deleted
	},
    setAsyncSNodeRemover : function (func) {
        	// deleted
    },

	setSparqlConnection : function (sparqlConn) {
		this.conn = sparqlConn;
	},

	getNodeCount : function() {
		return this.SNodeList.length;
	},
	drawNodes : function() {
		// deleted
	},

    // render all unused nodes collapsed
    renderUnusedNodesCollapsed : function() {
        	// deleted
    },

    renderNodeCollapsed : function(snode) {
        	// deleted
    },

    renderNodeUncollapsed : function(snode) {
        	// deleted
    },

	reserveNodeSparqlIDs : function(snode, legalizeSparqlIDs=true) {
		// reserve all of a snode's sparqlID's
		// optLegalizeSparqlIDs: change them if they are already in use.
        // Presumes node is not yet in the nodeGroup
		var id;
		var f = new SparqlFormatter();

        // get names of everything else already in the nodegroup.
        var nameHash = this.getAllVariableNamesHash();

		// sparqlID
		id = snode.getSparqlID();
		if (legalizeSparqlIDs && id in nameHash) {
			id = f.genSparqlID(id, nameHash);
			snode.setSparqlID(id);
		}

		// all the properties
		var props = snode.getReturnedPropertyItems();
		for (var i = 0; i < props.length; i++) {
			id = props[i].getSparqlID();
			if (legalizeSparqlIDs && id in nameHash) {
				id = f.genSparqlID(id, nameHash);
				props[i].setSparqlID(id);
			}
		}
	},

	addOneNode : function(newNode, existingNode, linkFromNewUri, linkToNewUri, legalizeSparqlIDs=true) {
		// add a newNode, possibly with a connection either from it or to it
		// newNode is a SemanticNode. Link it to existingNodeUri
		// if existingNode != null, then either linkFromNewUri or linkToNewUri
		// should also be non-null
		//
		// return the nodeItem (or null if there was an error)
		this.reserveNodeSparqlIDs(newNode, legalizeSparqlIDs);

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
		// path start class is the new one, path ENDS at existing nodegroup
		// return the node corresponding to the path's startClass. (i.e. the one
		// the user is adding.)
		// reverseFlag:  force connection in the opposite direction (path STARTS at nodegroup)
		var reverseFlag = (optReverseFlag === undefined) ? false : optReverseFlag;

		// optionalFlag:  if set, the first link will be optional
		var optionalFlag = (optOptionalFlag  === undefined) ? false : optOptionalFlag;

		// add the first class in the path
		var retNode = this.addNode(path.getStartClassName(), oInfo);
		var lastNode = reverseFlag ? anchorNode : retNode;
		var node0;
		var node1;
		var pathLen = path.getLength();
        var collapseSNodes = [];




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
				collapseSNodes.push(node1);
			// else this hop in path is class0--hasX-->lastAdded
			} else {
				node0 = this.returnBelmontSemanticNode(class0Uri, oInfo);
				this.addOneNode(node0, lastNode, attUri, null);
				lastNode = node0;
                collapseSNodes.push(node0);
			}
		}

		// link the last two nodes, which by now already exist
		var class1Uri = path.getClass1Name(pathLen - 1);
		var attUri = path.getAttributeName(pathLen - 1);
		var nodeItem;
        var finalNode = reverseFlag ? retNode : anchorNode;

		if (finalNode.getURI() == class1Uri) {
            // normal link from last node to anchor node,
			// When last connection URI matches and reverseFlag isn't set
			var opt = optionalFlag ? NodeItem.OPTIONAL_REVERSE : NodeItem.OPTIONAL_FALSE;
			nodeItem = lastNode.setConnection(finalNode, attUri, opt);

		} else {
            // reverse connection
			// either reverseFlag, or normal direction URI wasn't correct so presume backwards will work
			// throw exception if URI doesn't work
			var opt = optionalFlag ? NodeItem.OPTIONAL_TRUE : NodeItem.OPTIONAL_FALSE;
			var nodeItem = finalNode.setConnection(lastNode, attUri, opt);

		}

        this.drawNodes();
        for (var i=0; i < collapseSNodes.length; i++) {
            this.renderNodeCollapsed(collapseSNodes[i]);
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
			var propNameFull = props[i].getName().getFullName();
			var oRange = props[i].getRange(oClass, oInfo);

			if (oRange.isComplex() || oInfo.containsClass(oRange.getSimpleUri())) {
				// range is a class: create node item
				var p = new NodeItem(propNameFull, oRange.getUriList());
				belnodes.push(p);

			} else if (oInfo.containsDatatype(oRange.getSimpleUri())) {
				// range is a datatype
				var p = new PropertyItem(oInfo.getDatatype(oRange.getSimpleUri()).getEquivalentXSDTypes(), oRange.getSimpleUri(), propNameFull);
				belprops.push(p);
				
			} else {
				// range is a raw OWL data type
				var typeList = oInfo.getPropertyRangeXSDTypes(oRange.getSimpleUri());
				var p = new PropertyItem(typeList, oRange.getSimpleUri(), propNameFull);
				belprops.push(p);
			}
		}

		return new SemanticNode(nome, belprops, belnodes, fullNome, oInfo
				.getSubclassNames(fullNome), this);
	},

	getSNodeList : function() {
		return this.SNodeList;
	},

    getSNodeSparqlIDs : function() {
        var ret = [];
        for (var snode of this.SNodeList) {
            ret.push(snode.getSparqlID());
        }
        return ret;
    },

    getNode : function(i) {
        return this.SNodeList[i];
    },

    getNodeCount : function(i) {
        return this.SNodeList.length;
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

	/**
		Recursively walk down object props / nodeItems and find all subNodes
		Handles circuits.
	 */
	getSubNodes : function(topNode, optStopList) {
		var stopList = optStopList || [topNode];
		var ret = [];
		var nextLevel = [];

		// breadth first to match legacy behavior 
		for (var n of topNode.getConnectedNodes()) {
			if (stopList.indexOf(n) == -1 && ret.indexOf(n) == -1) {
				ret.push(n);
				nextLevel.push(n);
			}
		}
		
		for (var n of nextLevel) {
			// to make depth-first, get rid of nextLevel and combine into loop above
			ret = ret.concat(this.getSubNodes(n, ret.concat(stopList)));
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
			//console.log("Danger in belmont.js getOrderedNodeList(): No head nodes found.  Graph is totally circular.");
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

					for (var c=0; c < nodeItem.nodes.length; c++) {
						// increment hash[sparqlID] for each incoming link
						linkHash[nodeItem.nodes[c].getSparqlID()] += 1;
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
			if (nodeItems[j].getOptionalMinus(sNode) != NodeItem.OPTIONAL_REVERSE) {
				for (var rangeUri of nodeItems[j].getRangeUris()) {
					if (ret.indexOf(rangeUri) < 0) {
						ret.push(rangeUri);
					}
				}
			}
		}

		return ret;
	},

    getPropertyItemParentSNode : function(propItem) {
        for (var i=0; i < this.SNodeList.length; i++) {
            if (this.SNodeList[i].propList.indexOf(propItem) > -1) {
                return this.SNodeList[i];
            }
        }
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

    // get all incoming nodeItems that point here AND
    //         outgoing nodeItems that are connected to something
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

	getNodeItemsBetween : function(sNode1, sNode2) {
		// return a list of node items between the two nodes
		// Ahead of the curve: supports multiple links between snodes
		var ret = [];

		for (var i=0; i < sNode1.nodeList.length; i++) {
			if (sNode1.nodeList[i].nodes.indexOf(sNode2) > -1) {
				ret.push(sNode1.nodeList[i]);
			}
		}

		for (var i=0; i < sNode2.nodeList.length; i++) {
			if (sNode2.nodeList[i].nodes.indexOf(sNode1) > -1) {
				ret.push(sNode2.nodeList[i]);
			}
		}

		return ret;
	},

	getSubGraph : function(startSNode, stopList) {
		// get all the nodes connected recursively through all connections
		// except stopList.
		var ret = [];
		var work = [startSNode];

		while (work.length > 0) {
			var node = work.pop()
			if (stopList.indexOf(node) === -1 && ret.indexOf(node) === -1) {
				ret.push(node);

				var conn = this.getAllConnectedNodes(startSNode);
				for (var i=0; i < conn.length; i++) {
					work.push(conn[i])
				}
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
			var nonOptRet = (snode.getIsReturned() || snode.getIsBindingReturned() || snode.getIsTypeReturned) ? 1 : 0;
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
						if (snode.ownsNodeItem(nodeItem) && nodeItem.getOptionalMinus(otherSnode) == NodeItem.OPTIONAL_FALSE) {
							nodeItem.setOptionalMinus(otherSnode, NodeItem.OPTIONAL_REVERSE);
						}

						if (otherSnode.ownsNodeItem(nodeItem) && nodeItem.getOptionalMinus(snode) == NodeItem.OPTIONAL_FALSE) {
							nodeItem.setOptionalMinus(snode, NodeItem.OPTIONAL_TRUE);
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
				var nonOptReturnCount = (snode.getIsReturned() || snode.getIsBindingReturned() || snode.getIsTypeReturned()) ? 1 : 0;
				var optPropCount = 0;
				var retItems = snode.getReturnedPropertyItems();
				for (var p=0; p < retItems.length; p++) {
					var pItem = retItems[p];
					if (! pItem.getIsOptional()) {
						nonOptReturnCount++;
					} else if (pItem.getIsReturned() || pItem.getIsBindingReturned()) {
						optPropCount++;
					}
				}

				// sort all connected node items by their optional status: none, in, out
				var normItems = [];
				var optOutItems = [];
				var optInItems= [];
                var optOutMinusCount = 0;
				var optOutOptionalCount = 0;

				// outgoing nodes
				var nItems = snode.getNodeList();
				for (var n=0; n < nItems.length; n++) {
					var nItem = nItems[n];
					if (nItem.getConnected()) {
						var targets = nItem.getSNodes();
						for (var t=0; t < targets.length; t++) {
							var target = targets[t];
							var opt = nItem.getOptionalMinus(target);

							if (opt == NodeItem.OPTIONAL_FALSE) {
								normItems.push([nItem, target]);

							} else if (opt == NodeItem.OPTIONAL_TRUE) {
								optOutItems.push([nItem, target]);
                                optOutOptionalCount += 1;
                            } else if (opt == NodeItem.MINUS_TRUE) {
                                optOutItems.push([nItem, target]);
                                optOutMinusCount += 1;
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

					var opt = nItem.getOptionalMinus(snode);

					if (opt == NodeItem.OPTIONAL_FALSE) {
						normItems.push([nItem, snode]);

					} else if (opt == NodeItem.OPTIONAL_REVERSE || opt == NodeItem.MINUS_REVERSE) {
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

                    // also can't do anything if there's a mix of OPTIONAL and MINUS outgoing
                    if (optOutOptionalCount == 0 || optOutMinusCount == 0) {
    					// set the single normal nodeItem to incoming optional
    					var nItem = normItems[0][0];
    					var target = normItems[0][1];
    					if (target != snode) {
    						nItem.setOptionalMinus(target, NodeItem.OPTIONAL_REVERSE);
    					} else {
    						nItem.setOptionalMinus(target, NodeItem.OPTIONAL_TRUE);
    					}

    					// if there is only one outgoing optional, than it can be set to non-optional for performance
    					if (optOutItems.length == 1 && optPropCount == 0) {
    						var oItem = optOutItems[0][0];
    						var oTarget = optOutItems[0][1];
    						oItem.setOptionalMinus(oTarget, NodeItem.OPTIONAL_FALSE);
    					}

    					changedFlag = true;
                    }
				}
			}
		}
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

    getNodeByBindingOrSparqlID : function(id) {
		// gets first node by uri, or null if it doesn't exist
		for (var i = 0; i < this.SNodeList.length; i++) {
			if (this.SNodeList[i].getBindingOrSparqlID() == id) {
				return this.SNodeList[i];
			}
		}
		return null;
	},

    getReturnedItems : function () {
        var ret = [];

        var nList = this.getOrderedNodeList();
        for(var i=0; i < nList.length; i++) {
            var n = nList[i];
            // check if node URI is returned
            if (n.hasAnyReturn()) {
                ret.push(n);
            }

            ret = ret.concat(n.getReturnedPropertyItems());
        }
        return ret;

    },

    getTypeReturnedItems : function () {
        var ret = [];

        var nList = this.getOrderedNodeList();
        for(var i=0; i < nList.length; i++) {
            var n = nList[i];
            if (n.getIsTypeReturned()) {
                ret.push(n);
            }
        }
        return ret;
    },

    //
    // includes types and bindings that are returned
    //
    getReturnedSparqlIDs : function(optSkipFuncsFlag) {
        var items = this.getReturnedItems();
        var retHash = {};
        for (var i=0; i < items.length; i++) {
            if (items[i].getIsReturned()) {
                retHash[items[i].getSparqlID()] = 1;
            }
            if (items[i].getIsBindingReturned()) {
                retHash[items[i].getBinding()] = 1;
            }
            if (items[i].getIsTypeReturned()) {
                retHash[items[i].getTypeSparqlID()] = 1;
            }
            if (! optSkipFuncsFlag) {
                for (var id of items[i].getFunctionSparqlIDs()) {
                    retHash[id] = 1;
                }
            }
        }

        return Object.keys(retHash);
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
        // WARNING: behavior undefined for requestID null or ""
        var oldID = obj.getSparqlID();
        var oldTypeID = obj.getTypeSparqlID();


        // return if non-event
        if (requestID == oldID) {
            return;
        }

        // build nameHash
        obj.setSparqlID("?Throw___Away_");
        var nameHash = {};
        if (obj instanceof PropertyItem) {
            nameHash = this.getAllVariableNamesHash(this.getPropertyItemParentSNode(obj), obj);
        } else {
            nameHash = this.getAllVariableNamesHash(obj);
        }

        // set new ID if it isn't blank
        var newID = "";
        if (requestID != "") {
            var oldPossibleFuncIDs = obj.getPossibleFunctionSparqlIDs();

            newID = new SparqlFormatter().genSparqlID(requestID, nameHash);
		    obj.setSparqlID(newID);

            // update ORDER BY and GROUP BY
            var newPossibleFuncIDs = obj.getPossibleFunctionSparqlIDs();
            this.updateOrderBy(oldID, newID);
            this.updateGroupBy(oldID, newID);
            for (var i=0; i < oldPossibleFuncIDs.length; i++) {
                this.updateOrderBy(oldPossibleFuncIDs[i], newPossibleFuncIDs[i]);
                this.updateGroupBy(oldPossibleFuncIDs[i], newPossibleFuncIDs[i]);
            }

        }

		return newID;
	},

    /**
      *  set binding
      *    newID = new id or null
     **/
    setBinding : function (obj, newID) {
        var oldID = obj.getBindingOrSparqlID();
        var oldPossibleFuncIDs = obj.getPossibleFunctionSparqlIDs();

        if (newID == null) {
            obj.binding = null;
            newID = obj.getBindingOrSparqlID();
        } else {
            obj.binding = newID.startsWith("?") ? newID : "?" + newID;
        }

        // update ORDER BY and GROUP BY
        var newPossibleFuncIDs = obj.getPossibleFunctionSparqlIDs();
        this.updateOrderBy(oldID, newID);
        this.updateGroupBy(oldID, newID);
        for (var i=0; i < oldPossibleFuncIDs.length; i++) {
            this.updateOrderBy(oldPossibleFuncIDs[i], newPossibleFuncIDs[i]);
            this.updateGroupBy(oldPossibleFuncIDs[i], newPossibleFuncIDs[i]);
        }
    },

	removeTaggedNodes : function() {

		for (var i = 0; i < this.SNodeList.length; i++) {
            var snode = this.SNodeList[i];
			if (this.SNodeList[i].getRemovalTag()) {

				// remove the current sNode from all links.
				for (var k = 0; k < this.SNodeList.length; k++) {
                    var ngNode = this.SNodeList[k];
                    for (var n = 0; n < ngNode.nodeList.length; n++) {
                        this.rmFromUnions(ngNode, ngNode.nodeList[n], snode);
                        ngNode.nodeList[n].removeSNode(snode);
                    }
				}

                // remove nodeItems from unionHash
                for (var nItem of snode.nodeList) {
                    for (var target of nItem.nodes) {
                        this.rmFromUnions(snode, nItem, target);
                    }
                }

                // remove propItems from unionHash
                for (var pItem of snode.propList) {
                    this.rmFromUnions(snode, pItem);
                }

                // remove the snode from unionHash
                this.rmFromUnions(snode);

				// remove the sNode from the nodeGroup
				this.SNodeList.splice(i, 1);
			}
		}

	},

    removeLink : function(nodeItem, targetSNode) {
        this.rmFromUnions(this.getNodeItemParentSNode(nodeItem), nodeItem, targetSNode);
		nodeItem.removeSNode(targetSNode);
	},

	clear : function() {
        console.log("SemanticNodeGroup.clear() is deprecated.")
		this.SNodeList = [];
		this.limit = 0;
        this.offset = 0;
        this.orderBy = [];
        this.groupBy = [];
        this.columnOrder = [];

        this.unionHash = {};

		this.conn = null;
        this.sparqlNameHash = {};

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

        this.removeInvalidOrderBy();
        this.removeInvalidGroupBy();
        this.removeInvalidColumnOrder();

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
		var instanceOnly = (optInstanceOnly === undefined) ? false : optInstanceOnly;

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
		var instanceOnly = (optInstanceOnly === undefined) ? false : optInstanceOnly;

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

	setConnectingNodeItemsOptional : function(snode, val) {
		// set setSNodeOptional(val) on every nodeItem pointing to this snode

		var nItemList = this.getConnectingNodeItems(snode);
		for (var i=0; i < nItemList.length; i++) {
			nItemList[i].setSNodeOptional(snode, val);
		}
	},

	addSubclassPropertyByURI : function(snode, propURI, oInfo) {
		// add a subclass's property to this sNode
		// silently return NULL the keyname isn't found

		var oProp = oInfo.findSubclassProperty(snode.getURI(), propURI );
		if (oProp == null) {
			return null;
		}
		
		var oClass = oInfo.getClass(snode.getURI());
		var oRange = oProp.getRange(oClass, oInfo);
		var prop = snode.addSubclassProperty(oInfo.getPropertyRangeXSDTypes(oRange.getSimpleUri()),
				                             oRange.getSimpleUri(),
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

    // Exists beyond V1 for the GUI to edit
    generateOrderByClause : function () {
		this.validateOrderBy();

		if (this.orderBy.length > 0) {
			var ret = "ORDER BY";
			for (var i=0; i < this.orderBy.length; i++) {
				ret += " " + this.orderBy[i].toSparql();
			}
			return ret;
		} else {
			return "";
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
