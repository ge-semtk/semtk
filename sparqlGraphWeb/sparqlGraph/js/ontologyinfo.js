
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

/*
 * This holds a local copy of model information returned from a SPARQL connection.
 * Allows local path finding and listing of classes and attributes, etc.
 *
 * Version 2.   Multi directional breadth first path finding.
 *
 */



/*
 * OntologyAnnotation
 * In Java, this is a superclass of OntologyClass and OntologyProperty
 * but in js it a property
 */
var OntologyAnnotation = function() {
    this.comments = [];
    this.labels = [];
};

OntologyAnnotation.prototype = {
    /**
	 * Add comment, silently ignoring duplicates, nulls, isEmpty
	 * @param comment - may be undefined, null, empty
	 */
	addAnnotationComment : function(comment) {
        if (comment) {
            var trimmed = comment.trim();
            if (trimmed.length) {
                this.comments.push(trimmed);
            }
        }
	},

    clearAnnotationComments : function() {
        this.comments = [];
    },

	/**
	 * Add label, silently ignoring duplicates, nulls, isEmpty
	 * @param label
	 */
	addAnnotationLabel : function(label) {
		if (label) {
            var trimmed = label.trim();
            if (trimmed.length) {
                this.labels.push(trimmed);
            }
        }
	},

    clearAnnotationLabels : function() {
        this.labels = [];
    },

	getAnnotationComments : function() {
		return this.comments;
	},

	getAnnotationLabels : function() {
		return this.labels;
	},

	/**
	 * Return ; separated list of comments, or ""
	 * @return
	 */
	getAnnotationCommentsString : function() {
		return this.comments.join(" ; ");
	},

	/**
	 * Return ; separated list of comments, or ""
	 * @return
	 */
	getAnnotationLabelsString : function() {
		return this.labels.join(" ; ");
	},

	/**
     * Generate <rdfs:label> and <rdfs:comment> lines for an element
     * @param elem
     * @return
     */
    generateAnnotationRdf : function(tab) {
    	var ret = "";

    	for (var i=0; i < this.comments.length; i++) {
            ret += tab + "<rdfs:comment>" + this.comments[i] + "</rdfs:comment>\n";
    	}

    	for (var i=0; i < this.labels.length; i++) {
            ret += tab + "<rdfs:label>" + this.labels[i] + "</rdfs:label>\n";
    	}

    	return ret;
    },

    generateAnnotationsSADL : function() {
    	var ret = "";

    	for (var i=0; i < this.comments.length; i++) {
            ret += '(note "' + this.comments[i] + '\")';
    	}

    	for (var i=0; i < this.labels.length; i++) {
            ret += '(alias "' + this.labels[i] + '\")';
    	}

    	return ret;
    },
};
/*
 * OntologyName
 */
var OntologyName = function(fullname) {
    this.name = fullname;

};
OntologyName.prototype = {

	getLocalName : function() {
		return this.name.split('#')[1];
	},
	getNamespace : function() {
		return this.name.split('#')[0];
	},
	getFullName : function() {
		return this.name;
	},
	equals : function(other) {
		return (this.name == other.name);
	},
	isInDomain : function(domain) {
		// does my name start with this domain
		//i = this.name.indexOf(domain);
		//return (i == 0);

		var m = this.name.match("^"+domain);
		return (m != null);
	},
};
/*
 * OntologyClass
 */
var OntologyClass = function(name, parentName) {
    // parentName can be ""
    this.name = new OntologyName(name);
    this.parentNames = [];
    this.properties = [];
    this.annotation = new OntologyAnnotation();

    if (parentName) {
        this.parentNames.push(new OntologyName(parentName));
    }
};

OntologyClass.prototype = {
    // In java, sub-classing takes care of this
    addAnnotationComment :        function(c) { return this.annotation.addAnnotationComment(c); },
    clearAnnotationComments:      function()  { return this.annotation.clearAnnotationComments(); },
    addAnnotationLabel :          function(l) { return this.annotation.addAnnotationLabel(l); },
    clearAnnotationLabels:        function()  { return this.annotation.clearAnnotationLabels(); },
    getAnnotationComments :       function()  { return this.annotation.getAnnotationComments(); },
    getAnnotationLabels :         function()  { return this.annotation.getAnnotationLabels(); },
    getAnnotationCommentsString : function()  { return this.annotation.getAnnotationCommentsString(); },
    getAnnotationLabelsString :   function()  { return this.annotation.getAnnotationLabelsString(); },
    generateAnnotationRdf :       function(d) { return this.annotation.generateAnnotationRdf(d); },
    generateAnnotationsSADL :     function()  { return this.annotation.generateAnnotationsSADL(); },

    setName : function (nameStr) {
        this.name = new OntologyName(nameStr);
    },

    getName : function () {
        return this.name;
    },

	getNameStr : function(stripNsFlag) {
		if (stripNsFlag)
			return this.name.getLocalName();
		else
			return this.name.getFullName();
	},

    getLocalName : function() {
		return this.name.getLocalName();
	},

    getNamespaceStr : function() {
		return this.name.getNamespace();
	},

	getParentNameStrings : function(stripNsFlag) {
        var ret = [];
        for (var i=0; i < this.parentNames.length; i++) {
            if (stripNsFlag) {
                ret.push( this.parentNames[i].getLocalName() );
            } else {
                ret.push( this.parentNames[i].getFullName() );
            }
        }
		return ret;
	},

	getProperties : function() {
		return this.properties;
	},

	getProperty : function(propName) {
		for (var i=0; i < this.properties.length; i++) {
			if (this.properties[i].getNameStr() == propName) {
				return this.properties[i];
			}
		}
		return ;
	},

	getPropertyByKeyname : function(keyName) {
		// find first matching property
		for (var i=0; i < this.properties.length; i++) {
			if (this.properties[i].getNameStr().endsWith('#' + keyName)) {
				return this.properties[i];
			}
		}
		return null;
	},

    addParentName : function(name) {
        this.parentNames.push(new OntologyName(name));
    },

	addProperty : function(ontProperty) {
        // insert property alphabetically
        var name = ontProperty.getNameStr(true);
        for (var i=0; i < this.properties.length; i++) {
            if (this.properties[i].getNameStr(true) > name) {
                this.properties.splice(i,0,ontProperty);
                return;
            }
        }
        // else put it on the end
        this.properties.push(ontProperty);
	},

	equals : function(other) {
		return this.name.equals(other.name);
	},

	powerMatch : function(pattern) {
		// match against class name
		// Case-insensitive
		var pat = pattern.toLowerCase();

		return (this.getNameStr(true).toLowerCase().indexOf(pat) > -1);
	},

	powerMatchProps : function(pattern) {
		// match against name and all of the property names and ranges
		// Case-insensitive
		var pat = pattern.toLowerCase();

		var ret = [];
		var prop = this.getProperties();
		for (var i=0; i < prop.length; i++) {
			if (prop[i].getPowerMatchString().toLowerCase().indexOf(pat) > -1) {
				ret.push(prop[i]);
			}
		}

		return ret;
	},

    renameParent : function(oldURI, newURI) {
        for (var i=0; i < this.parentNames.length; i++) {
            var oName = this.parentNames[i];
            if (oName.getFullName() == oldURI) {
                this.parentNames[i] = new OntologyName(newURI);
            }
        }
    },
    
    
    /* =========== Edit functions =============== */

    //
    // delete parent or property if it belongs to namespace
    //
    deleteNamespace : function (namespace) {

        // parentNames
        for (var i=this.parentNames.length-1; i >= 0; i--) {
            if (this.parentNames[i].getNamespace() == namespace) {
                this.parentNames.slice(i,1);
            }
        }

        // properties
        for (var i=this.properties.length-1; i >= 0; i--) {
            if (this.properties[i].getNamespace() == namespace) {
                this.properties.slice(i,1);
            }
        }
    },

    renameNamespace : function (oldName, newName) {

        // usage:  uri = updateURI(uri)
        var updateURI = function(oName, nName, uri) {
           return nName + uri.slice(oName.length);
        }.bind(this, oldName, newName);

        if (this.name.getNamespace() == oldName) {
            this.name = new OntologyName(updateURI(this.name.getFullName()));
        }

         // parentNames
        for (var i=this.parentNames.length-1; i >= 0; i--) {
            if (this.parentNames[i].getNamespace() == oldName) {
                this.parentNames[i] = new OntologyName(updateURI(this.parentNames[i].getFullName()));
            }
        }

        // properties
        for (var i=this.properties.length-1; i >= 0; i--) {
            if (this.properties[i].getNamespace() == oldName) {
                this.properties[i].setName(updateURI(this.properties[i].getName()));
            }
        }
    },
};
/*
 * OntologyInfo
 */
var OntologyInfo = function(optJson) {
    this.classHash = {};        // classHash[classURI] = oClass object
    this.datatypeHash = {};     // datatypeHash[URI] = oDatatype object
    this.propertyHash = {};         // propertyHash[propURI] = oProp object
    this.subclassHash = {};     // for each class list of sub-classes names  (NOTE: super-classes are stored in the class itsself)
    this.enumHash = {};         // this.enumHash[className] = [ full-uri-of-enum-val, full-uri-of-enum_val2... ]
    this.subPropHash = {};      // this.subPropHash[propURI] = [ full-uri-of-subprop, full-uri-of-subprop...]

    this.maxPathLen = 50;
    this.pathWarnings = [];

    this.connHash = {};           // calculated as needed. connHash[class] = [all single-hop paths to or from other classes]
    this.prefixHash = null;       // calculated as needed: prefixHash[longName]=shortName

    this.getFlag = false;

    this.loadWarnings = [];
    this.importedGraphs = [];

    this.asyncDomain = null;
    this.asyncSei = null;
    this.asyncStatusCallback = null;
    this.asyncSuccessCallback = null;
    this.asyncFailureCallback = null;

    if (typeof optJson != "undefined") {
        this.addJson(optJson);
    }
};
// version 4 adds datatypeList
OntologyInfo.JSON_VERSION = 5;

OntologyInfo.ORPHAN_STR = "%orphan%";
OntologyInfo.ORPHAN_CLASS = new OntologyClass(OntologyInfo.ORPHAN_STR);

OntologyInfo.localizeNamespace = function(fullNamespace) {
   return fullNamespace.substring(fullNamespace.lastIndexOf('/')+1);
};

OntologyInfo.getSadlRangeList = function() {
   return ["http://www.w3.org/2001/XMLSchema#int",
           "http://www.w3.org/2001/XMLSchema#string",
           "http://www.w3.org/2001/XMLSchema#dateTime"];
};

OntologyInfo.getConstructOntologyQuery = function(conn) {
	var from = "";
	for (var i=0; i < conn.getModelInterfaceCount(); i++) {
		from = from + " FROM <" + conn.getModelInterface(i).getGraph(true) + "> ";
	}
	
	return "CONSTRUCT { ?s ?p ?o } " + from + " WHERE { ?s ?p ?o }" ;
};

OntologyInfo.prototype = {

    /*
     * create a hash of human-readable uri prefixes
     *
     */
	calcPrefixHash : function() {

        this.prefixHash = {};

        // preload standard prefixes
        // http://eo.dbpedia.org/sparql?nsdecl
        this.prefixHash["http://www.w3.org/1999/02/22-rdf-syntax-ns"] = "rdf";
        this.prefixHash["http://www.w3.org/2000/01/rdf-schema"]       = "rdfs";
        this.prefixHash["http://www.w3.org/2001/XMLSchema"]           = "xsd";

        // get every URI in the OntologyInfo
        uriList = [];
        uriList = uriList.concat(Object.keys(this.classHash));
        uriList = uriList.concat(Object.keys(this.propertyHash));
        uriList = uriList.concat(Object.keys(this.enumHash));
        
        // get every range URI
        for (var p in this.propertyHash) {
			for (var uri of p.getAllRangeUris()) {
				uriList = uriList.concat(this.propertyHash[p].getRange(uri, this).getUriList());
			}
        }
        
        // uniquify
        uriList = Array.from(new Set(uriList));

        for (var i=0; i < uriList.length; i++) {
            var tok = uriList[i].split("#");
            // if there is a #
            if (tok.length == 2) {
                // if prefix isn't known yet
                var longName = tok[0];
                var shortName = longName.substring(longName.lastIndexOf('/')+1);

                if (! (longName in this.prefixHash)) {

                    // append a number until the shortname is unique
                    // while Object.values(this.prefixHash) contains shortName
                    // (browser compatibility issue creates unreadable code)
                    var origName = shortName;
                    var suffix = 0;
                    while (Object.keys(this.prefixHash).map(function(k){return this.prefixHash[k];}.bind(this)).indexOf(shortName) > -1) {
                        suffix += 1;
                        shortName = origName + str(suffix);
                    }

                    this.prefixHash[longName] = shortName;
                }
            }
        }
    },

    // TODO: this should be renamed and static
    getPrefix : function(namespace) {
        if (! this.prefixHash) this.calcPrefixHash();

        return this.prefixHash[namespace];
    },

    /*
     * split an OntologyName into [shortPrefix, localName]
     */
    // TODO: this should be an OntologyName method
    splitName : function(oName) {
        return [this.getPrefix(oName.getNamespace()),
                oName.getLocalName()];
    },

    // TODO: this should be an OntologyName method
    getPrefixedName : function(oName) {
        return this.splitName(oName).join(":");
    },

	addClass : function(ontClass) {
		var classNameStr = ontClass.getNameStr();
		this.connHash = {};

		// silently overwrites if it already exists
		this.classHash[classNameStr] = ontClass;

		// store subClasses
		var supClassNames = ontClass.getParentNameStrings();
        for (var i=0; i < supClassNames.length; i++) {

            if (!(supClassNames[i] in this.subclassHash)) {
                this.subclassHash[supClassNames[i]] = [];
            }

            this.subclassHash[supClassNames[i]].push(classNameStr);
        }

	},

    addDatatype : function (oDatatype) {
		var nameStr = oDatatype.getNameStr();

		if (this.containsDatatype(nameStr)) {
			throw new Error("Internal error: datatype already exists in ontology.  Cannot re-add it: " + nameStr);
		}
		this.datatypeHash[nameStr] = oDatatype;
	},

	setGetFlag : function() {
		this.getFlag = true;
	},

	getDomainRangeRoots : function () {
		// get names of all classes who are not in the range of any other classes

		// start with all classes
		var ret = Object.keys(this.classHash);

		// loop through all classes
		for (var className in this.classHash) {
			var classVal = this.classHash[className];
			// loop through exact properties, (inherited props will appear in loop eventually)
			var props = classVal.getProperties();
			for (var i=0; i < props.length; i++) {
				var prop = props[i];
				// find range and all subclasses
				var rangeClasses = [prop.getAllRangeUris()];
				rangeClasses = rangeClasses.concat(this.getSubclassNames(rangeClasses[0]));

				// remove range and its subclasses from ret
				for (var j=0; j < rangeClasses.length; j++) {
					var index = ret.indexOf(rangeClasses[j]);
					if (index > -1) {
						ret.splice(index, 1);
					}
				}
			}
		}
		return ret;
	},

	getNumClasses : function () {
		return Object.keys(this.classHash).length;
	},

	getClass : function(classNameStr) {
		// inefficient but great for debugging
		//if (!this.containsClass(classNameStr))
		//	alert("OntologyInfo.getClass() is undefined for class name: " + classNameStr);

		// returns undefined if class does not exist
		return this.classHash[classNameStr];
	},

    // get named datatypes.
    // in owl  ?x a rdfs:Datatype   with  ?x owl:onDatatype ?equivType
    //    and ?equivType is an owl datatype like "owl#int"
    getDatatype : function(nameStr) {
		return this.datatypeHash[nameStr];
	},

    getPropertyRangeXSDTypes : function(rangeURI) {
        if (this.containsDatatype(rangeURI)) {
            return this.getDatatype(rangeURI).getEquivalentXSDTypes();
        } else {
            if (rangeURI.indexOf("#") > -1) {
                return [(new OntologyName(rangeURI)).getLocalName()];
            } else {
                return ["uri"];
            }
        }
    },

	getClassNames : function() {
		// returns an array of all known classes
		return Object.keys(this.classHash);
	},

    // get list of pairs [ [domainURI, propURI],   ]
    getPropertyPairs : function() {
        var classNameList = this.getClassNames();
        var ret = [];
        for (var cName of classNameList) {
            var propList = this.getInheritedProperties(this.getClass(cName));
            for (var prop of propList) {
                ret.push([cName, prop.getNameStr()]);
            }
        }
        return ret;
    },

    //
    // Get direct parents (not recursive)
    //
	getClassParents : function (ontClass) {
        var ret = [];
		var names = ontClass.getParentNameStrings();
        for (var i=0; i < names.length; i++) {
            ret.push(this.getClass(names[i]));
        }
		return ret;
	},
	
	getClassAncestorNames : function(oClass) {
		var ret = []
		var first = 0;
		ret = oClass.getParentNameStrings(false);
		var len = ret.length;
		
		while (len > first) {
			
			for (var i = first; i < len; i++) {
				var parents = this.getClass(ret[i]).getParentNameStrings(false);
				for (var p of parents) {
					if (ret.indexOf(p) == -1) {
						ret.push(p);
					}
				}
			}
			first = len;
			len = ret.length;
		}
		return ret;
	},

    // Recursively find all sub-classes
	getSubclassNames : function (classNameStr) {
		var ret = [];

		// get list of subclasses (or undefined)
		var subclassList = this.subclassHash[classNameStr];

		if (subclassList) {
			// recursively add subclass names
			for (var i=0; i < subclassList.length; i++ ) {
				ret.push(subclassList[i]);
				ret = ret.concat(this.getSubclassNames(subclassList[i]));
			}
		}
		return ret;
	},

	getSuperclassNames : function (subclassName) {
		// walk up list of all superclasses
		var ret = [];
		var superclasses = [];

        // find each parent
        var parentNames = this.classHash[subclassName].getParentNameStrings(false);
        for (var i=0; i < parentNames.length; i++) {
            var currParentName = parentNames[i];
            ret.push(currParentName);

            if (currParentName in this.classHash) {
                superclasses.push(this.classHash[currParentName]);
            } else {
                throw new Error("Class " + subclassName + "'s superclass " + currParentName + " is not found in the ontology.");
            }
        }

        // recursively add parents of parents
        for (var i=0; i < superclasses.length; i++) {
            var currParentClass = superclasses[i];
            ret = ret.concat(this.getSuperclassNames(currParentClass.getNameStr(false)));
        }
		return ret;
	},

    getImportedGraphs : function() {
        return this.importedGraphs;
    },

    getLoadWarnings : function() {
        return this.loadWarnings;
    },

	getPropNames : function() {
		// returns an array of all known properties
		return Object.keys(this.propertyHash);
	},

    getEnumNames : function() {
        // returns an array of all known enumeration values

        var retHash = {};

        for (var k in this.enumHash) {
            var eList = this.enumHash[k];
            for (var i=0; i < eList.length; i++) {
                retHash[eList[i]] = 1;
            }
        }
        return Object.keys(retHash);
    },

    /*
     * return a list of all namespace strings
     */
    getNamespaceNames : function () {
        var retHash = {};
        var names = this.getClassNames().concat(this.getPropNames()).concat(this.getEnumNames());

        for (var i=0; i < names.length; i++) {
            var oName = new OntologyName(names[i]);
            retHash[oName.getNamespace()] = 1;
        }

        return Object.keys(retHash);
    },

    /*
     * Return list of URIs
     */
    getClassEnumList : function (oClass) {
        var ret = [];

        if (oClass.getNameStr() in this.enumHash) {
            var eList = this.enumHash[oClass.getNameStr()];
            for (var i=0; i < eList.length; i++) {
                ret.push(eList[i]);
            }
        }

        return ret;
    },

	isDataProperty : function(oProp, oDomain) {
		return !this.isObjectProperty(oProp, oDomain);
	},

	isObjectProperty(oProp, oDomain) {
		var oRange = oProp.getRange(oDomain, this);
		if (oRange.isComplex()) {
			for (uri of oRange.getUriList()) {
				// check first one, presuming mixture is illegal as it seems to be in SADL
				return (uri in this.classHash);
			}
		} else {
			// return false for Datatype or owl:data
			return (oRange.getSimpleUri() in this.classHash);
		}
		return false;
	},

    uriIsKnown : function(uri) {
        return (    this.getPropNames().indexOf(uri) > -1   ||
                    this.getClassNames().indexOf(uri) > -1  ||
                    this.getEnumNames().indexOf(uri) > -1
               );
    },

	containsClass : function(classNameStr) {
		// returns an array of all known classes
		return (classNameStr in this.classHash);
	},

    containsDatatype : function(nameStr) {
		// returns an array of all known classes
		return (nameStr in this.datatypeHash);
	},

	containsProperty : function(propertyUri) {
		// does this contain given property
		return (propertyUri in this.propertyHash);
	},
	
	getProperty : function(propertyUri) {
		return this.propertyHash[propertyUri];
	},

	getInheritedProperties : function(ontClass, skipSelfFlag) {
		// return an array of OntologyProperties representing all inherited properties
		// error if any class names do not exist

		var ret = [];
		var dup = {};

        // get full list of classes
        var cNames = skipSelfFlag ? [] : [ontClass.getNameStr()];
		cNames = cNames.concat(this.getSuperclassNames(ontClass.getNameStr()));

        // for each class
		for (var i=0; i < cNames.length; i++) {

			var p = this.classHash[cNames[i]].getProperties();

            // save each prop if it's new
			for (var j=0; j < p.length; j++) {
				if (ret.indexOf(p[j]) == -1) {
					ret.push(p[j]);
                }
			}
		}

		// properties are sorted by subclass as we walked up the tree
		ret.sort( function(a,b) {
                                    var aa = a.getNameStr(true);
                                    var bb = b.getNameStr(true);
                                    if (aa < bb)  return -1;
                                    else if (aa > bb)  return 1;
                                    else return 0;
                                } );

		return ret;
	},

	getInheritedPropertyByKeyname : function(ontClass, propName) {
		var props = this.getInheritedProperties(ontClass);
		for (var i=0; i < props.length; i++) {
			if (props[i].getNameStr(true) == propName) {
				return props[i];
			}
		}
		return null;
	},

	getDescendantProperties : function(ontClass) {
		// return properties in subclasses

		var ret = [];
		var dup = {};

		// get subclasses and loop through them
		var subclassNames = this.getSubclassNames(ontClass.getNameStr());

		for (var i=0; i < subclassNames.length; i++) {
			var subClass = this.getClass(subclassNames[i]);
			var props = subClass.getProperties();

			// loop through properties
			for (var j=0; j < props.length; j++) {
				// push class if it isn't already on the list
				var key = props[j].getNameStr() + ":" + props[j].getDisplayString(false);
				if (!(key in dup)) {
					ret.push(props[j]);
					dup[key] = 1;
				}
			}
		}

		// the following code makes them alphabetical...just to show off maybe
		ret.sort( function(a,b) {if (a.getNameStr() < b.getNameStr())  return -1; else if (a.getNameStr() > b.getNameStr())  return 1; else return 0;} );

		return ret;
	},

	

	loadSuperSubClasses : function(subClassList, superClassList) {
        var tempClasses = {};
        for (var i=0; i < subClassList.length; i++) {
            // check for the existence of the current class.
			if( ! (subClassList[i] in tempClasses)) {
				var c = new OntologyClass(subClassList[i], null);
				tempClasses[subClassList[i]] = c;
			}
			// get the current class and add the parent.
			var c = tempClasses[subClassList[i]];
			c.addParentName(superClassList[i]);
        }

		// call addClass() on the temp list.
		for(keyName in tempClasses) {
			var oClass = tempClasses[keyName];
			this.addClass(oClass);
		}
	},

    getBreadthFirstSuperProps : function(subPropName) {
		var ret = [];
		var prevLen = 0;
		this.addSuperProps(ret, subPropName);
		while (ret.length > prevLen) {
			var len = ret.length;
			for (var i = prevLen; i < len; i++) {
				this.addSuperProps(ret, ret[i]);
			}
			prevLen = len;
		}
		return ret;
		
	},
	
	addSuperProps : function(theList, subPropName) {
		for (var k of Object.keys(this.subPropHash)) {
			if (this.subPropHash[subPropName] && theList.indexOf(k) == -1) {
				theList.push(k);
			}
		}
	},
	
    loadSuperSubProperties : function(subPropNames, superPropNames) {
        
        // pass 1: set up this.subPropHash and make sure subProp exists (for old versions)
        for (var i = 0; i < subPropNames.length; i++) {
            // make sure subpropHash entry exists
			if (this.subPropHash[superPropNames[i]] == undefined) {
				this.subPropHash[superPropNames[i]] = [];
			}
			
			// add to subpropHash
			var subList = this.subPropHash[superPropNames[i]];
			if (subList.indexOf(subPropNames[i]) == -1) {
				subList.push(subPropNames[i]);
			}
            
            // find existing subProp: has domain so loadProperties got it
			var oSubProp = this.propertyHash[subPropNames[i]];
			
			if (! oSubProp ) {
				// found a new sub property, copy it's parent completely
				// since it was never given a domain or range of its own during loadProperties()
				// only happens in older versions
				oSubProp = new OntologyProperty(subPropNames[i], oSuperProp);
				this.propertyHash[subPropNames[i]] = oSubProp;
				for (var domain of oSubProp.getRangeDomains()) {
					if (domain != OntologyInfo.ORPHAN_STR) {
						this.getClass(domain).addProperty(oSubProp);
					}
				}
			}
			
		}
		
		// pass 2
		for (var subPropName of subPropNames) {
			var oSubProp = this.getProperty(subPropName);
		
			for (var superPropName of this.getBreadthFirstSuperProps(subPropName)) {
				var oSuperProp = this.getProperty(superPropName);
				this.mergeParentRange(oSubProp, oSuperProp);
			}
        }
	},

	mergeParentRange : function (oSubProp, oSuperProp) {
		var myOrphanRange = oSubProp.getExactRange(OntologyInfo.ORPHAN_STR);
		var supOrphanRange = oSuperProp.getExactRange(OntologyInfo.ORPHAN_STR);
		
		// if I have a range with domain ORPHAN, copy the range to every domain that oSuperProp has
		if (myOrphanRange != null) {
			oSubProp.removeRange(OntologyInfo.ORPHAN_STR);
			for (supDomain of oSuperProp.getRangeDomains()) {
				oSubProp.putRange(supDomain, myOrphanRange.deepCopy());
				if (supDomain != OntologyInfo.ORPHAN_STR) {
					this.getClass(supDomain).addProperty(oSubProp);
				}
			}
		}
		
		// now check the ranges to see if they contain ORPHAN
		for (myDomain of oSubProp.getRangeDomains()) {
			var oMyRange = oSubProp.getExactRange(myDomain);
			if (oMyRange.containsUri(OntologyInfo.ORPHAN_STR)) {
				// range contains "orphan"
				var oSupRange = null;
				if (myDomain == OntologyInfo.ORPHAN_STR) {
					// use the superProps orphan range if it has one
					oSupRange = supOrphanRange;
				} else {
					try {
						// try to get range from superProp via domain incl inheritence (might throw exception that there isn't one)
						oSupRange = oSuperProp.getRange(this.getClass(myDomain), this);
					} catch (e) {
						// superProp doesn't have the range.
					}
				}
				if (oSupRange != null) {
					// found something: replace it in
					oSubProp.putRange(myDomain, oSupRange.deepCopy());
				}
			}
		}
	},

	loadTopLevelClasses : function(classList) {
		// load from rows of [class]
		for (var i=0; i < classList.length; i++) {
			var x = new OntologyClass(classList[i], "");
			this.addClass(x);
		}
	},

    loadDatatypes : function(dataTypeList, equivTypeList, restrictPredList, restrictObjList) {
		// load from rows of [class]
		for (var i=0; i < dataTypeList.length; i++) {
			var dt;
			if (this.containsClass(dataTypeList[i])) {
				throw new Error("Error loading ontology.  Datatype URI is already a class: " + dataTypeList[i]);
            
            } else if (this.containsDatatype(dataTypeList[i])) {
                // already exists: add additional equivalent types
                dt = this.getDatatype(dataTypeList[i]);
				dt.addEquivalentType(equivTypeList[i]);
				
            } else {
				
    			dt = new OntologyDatatype(dataTypeList[i], equivTypeList[i]);
    			this.addDatatype(dt);
            }
            
            if (restrictPredList != null && restrictPredList[i] != "") {
				dt.addRestriction(restrictPredList[i], restrictObjList[i]);
			}
		}
	},

	findSubclassProperty : function (className, propName) {
		// return property from a subclass of className, or NULL
		var scNames = this.getSubclassNames(className);

		// loop through subclasses
		for (var i=0; i < scNames.length; i++) {
			var subClass = this.getClass(scNames[i]);
			var props = subClass.getProperties();

			// loop through subclass properties
			for (var j=0; j < props.length; j++) {
				if (props[j].getName().getFullName() == propName) {
					return props[j];
				}
			}
		}
		return null;
	},

	

	loadProperties : function(classList, propertyList, rangeList) {
		// load from rows of [class, property, range]
		for (var i=0; i < classList.length; i++) {
			
			// Skip rows with no class  IFF the same property appears somewhere else with a class and same range
			// (aka forgive the loadPropertiesQuery)
			var skip = false;
			if (classList[i] ==  "") {
				for (var j=0; j < propertyList.length; j++) {
					if (i != j && propertyList[i] == propertyList[j] && rangeList[i] == rangeList[j]) {
						skip = true;
					}
				}
			}
			if (skip) continue;
			
			var oProp = null;
			var oClass = this.getClass(classList[i]);
			
			// if domain or range are empty, use OrphanClass
			// this will be resolved in loadSuperSubProperties
			if (oClass == null) oClass = OntologyInfo.ORPHAN_CLASS;
			var rangeStr = rangeList[i] == "" ? OntologyInfo.ORPHAN_STR : rangeList[i];
			var domainStr = classList[i] == "" ? OntologyInfo.ORPHAN_STR : classList[i];
			
			if (Object.keys(this.propertyHash).indexOf(propertyList[i]) > -1) {
				// get prop from propertyHash
				oProp = this.propertyHash[propertyList[i]];
				oProp.addRange(oClass, rangeStr);
			} else {
				// property doesn't exist: create it
				oProp = new OntologyProperty(propertyList[i], domainStr, rangeStr);
				this.propertyHash[propertyList[i]] = oProp;
			}

			if (domainStr != OntologyInfo.ORPHAN_STR) {
				// Add property to the class
				var c = this.classHash[classList[i]];
				if (c == null){
					throw new Error("Cannot find class " + classList[i] + " in the ontology");
				}			
				c.addProperty(oProp);
			}			
		}
	},

	

	loadEnums : function(classList, valList) {
		// load from rows of [class, property, range]
		for (var i=0; i < classList.length; i++) {
			var className = classList[i];
			var enumVal = valList[i];

			// add to enumHash list for this class, or start a new entry
			if (className in this.enumHash) {
				this.enumHash[classList[i]].push(valList[i]);
			} else {
				this.enumHash[classList[i]] = [ valList[i] ];
			}
		}
	},

   

	loadAnnotationLabels : function(elemList, labelList) {

		for (var i=0; i < elemList.length; i++) {

			// find the element: class or property
			var e = this.classHash[elemList[i]];
			if (e == null) {
				e = this.propertyHash[elemList[i]];
			}
			if (e == null)  {
				throw new Error("Error in ontology: cannot find element to attach annotation: " + elemList[i]);
			}

			// add the annotations (empties and duplicates are handled downstream)
			e.addAnnotationLabel(labelList[i]);
		}
	},

	
	loadAnnotationComments : function(elemList, commentList) {
		for (var i=0; i < elemList.length; i++) {

			// find the element: class or property
			var e = this.classHash[elemList[i]];
			if (e == null) {
				e = this.propertyHash[elemList[i]];
			}
			if (e == null)  {
				throw new Error("Error in ontology: cannot find element " + elemList[i] + " in the ontology");
			}

			// add the annotations (empties and duplicates are handled downstream)
			e.addAnnotationComment(commentList[i]);
		}
	},

	classIsEnum : function (classURI) {
		return classURI in this.enumHash;
	},

	getMatchingEnum : function (classURI, enumStr) {
		// return full URI of enumStr matching an enumerated value for classURI
		// "match" means enumStr is an exact match or the local fragment
		// returns NULL if classURI is not enumerated or if none of its enum values end in enumStr

		if (classURI in this.enumHash) {
			var enumList = this.enumHash[classURI];
			for (var i=0; i < enumList.length; i++) {
				if (enumList[i] == enumStr || enumList[i].endsWith("#" + enumStr)) {
					return enumList[i];
				}
			}
		}
		return null;
	},

    // findAllPaths : function
    // has been moved to the service layer via MsiClientNodeGroupService

	classIsA : function (class1, class2) {


		if (!class1 || !class2) return false;

        if (class1.equals(class2)) return true;

        var allParents = this.getClassParents(class1);
		// recursively check class1 parents
		for (var i=0; i < allParents.length; i++) {
			if (this.classIsA(allParents[i], class2)) {
                return true;
            }
		}

		return false;
	},

	classIsInRange : function (class1, range1) {
		// at the moment a range is just a single class.
		return this.classIsA(class1, this.getClass(range1.getFullName()));

	},

	loadAsync : function(conn, statusCallback, successCallback, failureCallback) {
		// loadAsync() is for backwards compatibility.
		// Please use load() instead.
		console.log("OntologyInfo.loadAsync(): called DEPRECATED function.  Use load()");
		this.load(conn.getDomain(), conn.getModelInterface(0), statusCallback, successCallback, failureCallback);

    },

    loadSparqlConnection : function(conn, queryServiceUrl, statusCallback, successCallback, failureCallback, optRecursionIndex) {
    	throw new Error("This is implemented in backcompatutils.js");

    },

    loadFromService : function (oInfoClient, conn, statusCallback, successCallback, failureCallback) {
        statusCallback("retrieving ontology");

        var success = function(statusCbk, successCbk, failureCbk, msiRes) {
            statusCbk("parsing ontology");
            if (!msiRes.isSuccess()) {
                failureCbk(msiRes.getFailureHtml());
            } else {
                this.addJson(msiRes.getSimpleResultField("ontologyInfo"));
                statusCbk("");
                successCbk();
            }
        }.bind(this, statusCallback, successCallback, failureCallback);

        oInfoClient.execGetOntologyInfoJson(conn, success);
    },

    load : function (domain, interfaceOrClient, statusCallback, successCallback, failureCallback) {
    	throw "ontologyinfo.js loadVirtuosoOnlyDeprecated is no longer supported.  Use loadFromService() instead.";
    },

    // numeric prefix for efficient JSON
    prefixURI : function (uri, prefixToIntHash) {
        var tok = uri.split("#");
        // if there is a #
        if (tok.length == 2) {
            // add to prefixToIntHash if missing
            if (! (tok[0] in prefixToIntHash)) {
                prefixToIntHash[tok[0]] = Object.keys(prefixToIntHash).length;
            }

            return prefixToIntHash[tok[0]] + ":" + tok[1];
        } else {
            return uri;
        }
    },

    unPrefixURI : function (uri, intToPrefixHash) {
        var tok = uri.split(":");
        if (tok.length == 2 && tok[0] in intToPrefixHash) {
            return intToPrefixHash[tok[0]] + "#" + tok[1];
        } else {
            return uri;
        }
    },

    /*
     * Build a json that mimics the returns from the load queries
     */
    toJson : function () {
        var json = {
            "version" : OntologyInfo.JSON_VERSION,
            "topLevelClassList" : [],
            "datatypeList" : [],
            "subClassSuperClassList" : [],
            "classPropertyRangeList" : [],
            "subSuperPropList" : [],
            "classEnumValList" : [],
            "annotationLabelList" : [],
            "annotationCommentList" : [],
            "prefixes" : {},
            "importedGraphsList" : [],
            "loadWarningsList" : [],
        };

        var prefixToIntHash = {};

        // topLevelClassList and subClassSuperClassList
        for (var c in this.classHash) {
            var parents = this.classHash[c].getParentNameStrings();
            var name = this.classHash[c].getNameStr();
            if (parents.length == 0) {
                json.topLevelClassList.push(this.prefixURI(name, prefixToIntHash));
            } else {
                for (var i=0; i < parents.length; i++) {
                    json.subClassSuperClassList.push([this.prefixURI(name, prefixToIntHash),
                                                      this.prefixURI(parents[i], prefixToIntHash) ]);
                }
            }
        }

        // datatypeList
        for (var name in this.datatypeHash) {
			var rows = this.generateDatatypeRows(this.datatypeHash[name]);
			for (var r of rows) {
				json.datatypeList.push(r);
			}
        }

        for (var superProp in this.subPropHash) {
            for (var subProp of this.subPropHash[superProp]) {
                json.subSuperPropList.push([subProp, superProp]);
            }
        }

        // classPropertyRangeList
        for (var oProp of Object.values(this.propertyHash)) {
			var propName = oProp.getNameStr();
			for (var domain of oProp.getRangeDomains()) {
				var oClass = this.getClass(domain);
				for (var rangeUri of oProp.getRange(oClass, this).getUriList()) {
					 json.classPropertyRangeList.push(
						[this.prefixURI(domain, prefixToIntHash),
                         this.prefixURI(propName, prefixToIntHash),
                         this.prefixURI(rangeUri, prefixToIntHash) ]);
				}
			}
        }

        // classEnumValList
        for (var c in this.enumHash) {
            var valList = this.enumHash[c];
            for (var i=0; i < valList.length; i++) {
                var v = valList[i];
                json.classEnumValList.push([this.prefixURI(c, prefixToIntHash),
                                            this.prefixURI(v, prefixToIntHash)]);
            }
        }

        // annotation Lists: classes
        for (var c in this.classHash) {
            var commentList = this.classHash[c].getAnnotationComments();
        	for (var i=0; i < commentList.length; i++) {
                json.annotationCommentList.push([this.prefixURI(c, prefixToIntHash), commentList[i]]);
        	}

        	labelList = this.classHash[c].getAnnotationLabels();
        	for (var i=0; i < labelList.length; i++) {
                json.annotationLabelList.push([this.prefixURI(c, prefixToIntHash), labelList[i]]);
        	}
        }

        // annotation Lists: properties
        for (var p in this.propertyHash) {
            var commentList = this.propertyHash[p].getAnnotationComments();
        	for (var i=0; i < commentList.length; i++) {
                json.annotationCommentList.push([this.prefixURI(p, prefixToIntHash), commentList[i]]);
        	}

        	labelList = this.propertyHash[p].getAnnotationLabels();
        	for (var i=0; i < labelList.length; i++) {
                json.annotationLabelList.push([this.prefixURI(p, prefixToIntHash), labelList[i]]);
        	}
        }

        // prefixes: reverse the hash so its intToPrefix
        for (var p in prefixToIntHash) {
            json.prefixes[prefixToIntHash[p]] = p;
        }

        for (var s in this.importedGraphs) {
            json.importedGraphsList.push(s);
        }

        for (var s in this.loadWarnings) {
            json.loadWarningsList.push(s);
        }

        return json;
    },
    
	generateDatatypeRows: function(dt) {
		var ret = [];
		
		// add a row for each type, with empty restriction columns
		for (var typeStr of dt.getEquivalentTypes()) {
			var row = [];
			row.push(dt.getNameStr(false));
			row.push(typeStr);
			ret.push(row);
		}
		
		// add restrictions columns
		var r = 0;
		for (var restrict of dt.getRestrictions()) {
			// grab the next available row 
			// duplicating the last row if there aren't enough rows from xsdTypes to hold all restrictions
			var row;
			if (r >= ret.size()) {
				row = [];
				var last = ret[ret.length-1];
				for (var i=0; i < 2; i++) {
					row.push(last[i]);
				}
				ret.push(row);
			} else {
				row = ret[r];
			}
			
			// add the restriction to the row
			row.push(restrict.getPredicate());
			row.push(restrict.getObject());
			r++;
		}
		
		// if fewer restrictions than types, round out all rows with ""
		while (r < ret.length) {
			ret[r].push("");
			ret[r].push("");
			r++;
		}
		
		return ret;  
	},
	
    /*
     * adds json to OntologyInfo
     *
     * Normally, use new OntologyInfo(json)
     */
    addJson : function (json) {
        var version = 0;
        if (json.hasOwnProperty("version")) {
            version = json.version;
        }
        if (version > OntologyInfo.JSON_VERSION) {
            throw new Error("Can't read OntologyInfo.  Services returned v" + version + " but SPARQLgraph js is older v" + OntologyInfo.JSON_VERSION);
        }
        /*
         * return one columns of data as a list
         * unPrefix all values
         */
        var getColumn = function(hash, data, colNum) {
            return data.map( function(h, c, row) { return this.unPrefixURI(row[c], h); }.bind(this, hash, colNum) );
        }.bind(this, json.prefixes);

        // unhash topLevelClasses
        var topLevelClassList = [];
        for (var i=0; i < json.topLevelClassList.length; i++) {
            topLevelClassList.push(this.unPrefixURI(json.topLevelClassList[i], json.prefixes));
        }

        this.loadTopLevelClasses(topLevelClassList);
        
        if (json.hasOwnProperty("datatypeList") && json["datatypeList"].length > 0) {
			if (json.datatypeList[0].length == 4) {
				this.loadDatatypes(getColumn(json.datatypeList, 0),
	                    	       getColumn(json.datatypeList, 1),
	                    	       getColumn(json.datatypeList, 2),
	                    	       getColumn(json.datatypeList, 3)
	                     	  	);
			} else {
				// older version only had 2 columns
	        	this.loadDatatypes(getColumn(json.datatypeList, 0),
	                    	       getColumn(json.datatypeList, 1)
	                     	  	);
	        }
	    }
        this.loadSuperSubClasses(getColumn(json.subClassSuperClassList, 0),
                                 getColumn(json.subClassSuperClassList, 1)
                             );
        this.loadProperties(getColumn(json.classPropertyRangeList, 0),
                            getColumn(json.classPropertyRangeList, 1),
                            getColumn(json.classPropertyRangeList, 2)
                            );

        if (json.hasOwnProperty("subSuperPropList")) {
            this.loadSuperSubProperties(getColumn(json.subSuperPropList, 0),
                                        getColumn(json.subSuperPropList, 1)
                                    );
        }

        this.loadEnums(getColumn(json.classEnumValList, 0),
                       getColumn(json.classEnumValList, 1)
                      );

        if (json.hasOwnProperty("annotationLabelList")) {
            this.loadAnnotationLabels(getColumn(json.annotationLabelList, 0),
                                      getColumn(json.annotationLabelList, 1));
        }

        if (json.hasOwnProperty("annotationCommentList")) {
            this.loadAnnotationComments(getColumn(json.annotationCommentList, 0),
                                      getColumn(json.annotationCommentList, 1));
        }

        if (json.hasOwnProperty("importedGraphsList")) {
            this.importedGraphs =  this.importedGraphs.concat(json.importedGraphsList);
        }

        if (json.hasOwnProperty("loadWarningsList")) {
            this.loadWarnings = this.loadWarnings.concat(json.loadWarningsList);
        }

    },

    // returns a list, possibly []
    getSubProperties : function(superProp) {
        var superPropUri = superProp.getNameStr();
        var ret = [];

        if (this.subPropHash[superPropUri]) {
            for (var uri of this.subPropHash[superPropUri]) {
                ret.push(this.propertyHash[uri]);
            }
        }
        return ret;
    },

    /* =========== Edit functions =============== */

    clearTempHashes : function () {
        this.connHash = {};
        this.prefixHash = null;
    },

    //
    // Delete all traces of a namespace from each hash
    // and from any classes
    //
    deleteNamespace : function (namespace) {
        var prefix = namespace + "#";
        this.clearTempHashes();

        // classHash
        for (var c in this.classHash) {
            if (c.startsWith(prefix)) {
                delete this.classHash[c];
            } else {
                this.classHash[c].deleteNamespace(namespace);
            }
        }

        // propertyHash
        for (var p in this.propertyHash) {
             if (p.startsWith(prefix)) {
                delete this.propertyHash[p];
            }
        }

        // subclassHash
        for (var c in this.subclassHash) {
            if (c.startsWith(prefix)) {
                // delete whole list
                delete this.subclassHash[c];
            } else {
                // delete only list values in namespace
                for (var i=this.subclassHash[c].length-1; i >= 0; i--) {
                    if (this.subclassHash[c][i].startsWith(prefix)) {
                        this.subclassHash[c].slice(i,1);
                    }
                }
            }
        }

        // enumHash
        for (var e in this.enumHash) {
            if (e.startsWith(prefix)) {
                // delete whole list
                delete this.enumHash[e];
            } else {
                // delete only list values in namespace
                for (var i=this.enumHash[e].length-1; i >= 0; i--) {
                    if (this.enumHash[e][i].startsWith(prefix)) {
                        this.enumHash[e].slice(i,1);
                    }
                }
            }
        }
    },

    //
    // Delete all traces of a namespace from each hash
    // and from any classes
    //
    renameNamespace : function (oldName, newName) {
        var oldPrefix = oldName + "#";
        this.clearTempHashes();

        // usage:  uri = updateURI(uri)
        var updateURI = function(oName, nName, uri) {
           return nName + uri.slice(oName.length);
        }.bind(this, oldName, newName);

        // classHash
        for (var c in this.classHash) {
            // handle the class innards
            this.classHash[c].renameNamespace(oldName, newName);

            if (c.startsWith(oldPrefix)) {
                // rename hash key
                this.classHash[updateURI(c)] = this.classHash[c];
                delete this.classHash[c];
            }
        }

        // propertyHash
        for (var p in this.propertyHash) {
             if (p.startsWith(oldPrefix)) {
                // rename hash key
                this.propertyHash[updateURI(p)] = this.classHash[p];
                delete this.propertyHash[p];
            }
        }

        // subclassHash
        for (var c in this.subclassHash) {
            if (c.startsWith(oldPrefix)) {
                // rename hash key
                this.subclassHash[updateURI(c)] = this.subclassHash[c];
                delete this.subclassHash[c];
            } else {
                // rename only list values in namespace
                for (var i=this.subclassHash[c].length-1; i >= 0; i--) {
                    if (this.subclassHash[c][i].startsWith(oldPrefix)) {
                        this.subclassHash[c][i] = updateURI(this.subclassHash[c][i]);
                    }
                }
            }
        }

        // enumHash
        for (var e in this.enumHash) {
            if (e.startsWith(oldPrefix)) {
                // rename hash key
                this.enumHash[updateURI(e)] = this.enumHash[e];
                delete this.enumHash[e];
            } else {
                // delete only list values in namespace
                for (var i=this.enumHash[e].length-1; i >= 0; i--) {
                    if (this.enumHash[e][i].startsWith(oldPrefix)) {
                        this.enumHash[e][i] = updateURI(this.enumHash[e][i]);
                    }
                }
            }
        }
    },

    /*
     * Change name of a class
     * @param oClass {OntologyClass} - a class from this oInfo
     * @param newName {String} - new name already checked to be legalURI and !uriIsKnown()
     */
    editClassName : function(oClass, newURI) {
        var oldURI = oClass.getNameStr(false);

        if (! SparqlUtil.isValidURI(newURI)) {
            throw new Error("Invalid URL: " + newURI);

        } else if (this.uriIsKnown(newURI)) {
            throw new Error("URI is already in use: " + newURI);

        } else if (newURI != oldURI) {

            oClass.setName(newURI);

            // update classHash
            this.classHash[newURI] = oClass;
            delete this.classHash[oldURI];

            // update subclassHash keys
            if (oldURI in this.subclassHash) {
                this.subclassHash[newURI] = this.subclassHash[oldURI];
                delete this.subclassHash[oldURI];
            }

            // update subclassHash contents
            for (var key in this.subclassHash) {
                var i = this.subclassHash[key].indexOf(oldURI);
                if (i > -1) {
                    this.subclassHash[key][i] = newURI;
                }
            }

            // update enumHash
            if (oldURI in this.enumHash) {
                this.enumHash[newURI] = this.enumHash[oldURI];
                delete this.enumHash[oldURI];
            }

            // update enumHash contents
            for (key in this.enumHash) {
                var i = this.enumHash[key].indexOf(oldURI);
                if (i > -1) {
                    this.enumHash[key][i] = newURI;
                }
            }

            // upadate superclasses
            for (var key in this.classHash) {
                this.classHash[key].renameParent(oldURI, newURI);
            }

            // clear connHash
            this.connHash = {};

            return oClass;
        }
    },

    /*
     * Change name of a class
     * @param oClass {OntologyClass} - a class from this oInfo
     * @param newName {String} - new name already checked to be legalURI and !uriIsKnown()
     */
    deleteClass : function(oClass) {
        var oldURI = oClass.getNameStr(false);


        // update classHash
        delete this.classHash[oldURI];

        // update subclassHash keys
        if (oldURI in this.subclassHash) {
            delete this.subclassHash[oldURI];
        }

        // update subclassHash contents
        for (var key in this.subclassHash) {
            var i = this.subclassHash[key].indexOf(oldURI);
            if (i > -1) {
                splice(this.subclassHash[key], i, 1);
            }
        }

        // update enumHash
        if (oldURI in this.enumHash) {
            delete this.enumHash[oldURI];
        }

        // update enumHash contents
        for (key in this.enumHash) {
            var i = this.enumHash[key].indexOf(oldURI);
            if (i > -1) {
                splice(this.enumHash[key], i, 1);
            }
        }

        // clear connHash
        this.connHash = {};
    }
};

/*
 * OntologyPath
 */
var OntologyPath = function(className) {
	// list of triple name lists [[ class, att, class], [class, att, class]...]
	this.tripleList = [];
	this.startClassName = className;
	this.endClassName = className;

	// hash of all known classes
	this.classHash = {};
	this.classHash[className] = 1;
};

OntologyPath.fromJson = function (jObj) {
    var ret = new OntologyPath(jObj["startClassName"]);
    for (var j of jObj["triples"]) {
        ret.addTriple(j["s"], j["p"], j["o"]);
    }
    return ret;
};

OntologyPath.prototype = {

	addTriple : function(className0, attName, className1) {
		// PEC TODO:  Fails when className0 == className1
		//            Presumes "forward" link direction.
		//            There's no way to make it go in the other direction.
		//            This might only matter for paths with only one hop  [ Man hasDad Man ]
		//              because that's the only time we care about new vs. existing sparqlID's

		// add whichever class is new to the hash and endpoint
		if (className0 == this.endClassName) {
			this.classHash[className1] = 1;
			this.endClassName = className1;

		} else if (className1 == this.endClassName) {
			this.classHash[className0] = 1;
			this.endClassName = className0;

		} else {
			alert("OntologyPath.addTriple(): Error adding triple to path.  It isn't connected.\n"+
				   "  Triple is: "+className0+","+attName+"," + className1 + "\n" +
				   "  Path is: " + this.debugString());
		}

		// push triple onto end of path
		this.tripleList.push([className0, attName, className1]);
	},

	addTriples : function(tripleList) {
		for (var i=0; i < tripleList.length; i++) {
			this.addTriple(tripleList[i][0], tripleList[i][1], tripleList[i][2]);
		}
	},

	asList : function() {
		// return list of triples.  Each triple is a list of three string names [class, att, class]
		return this.tripleList;
	},

	asOldFashionedList : function() {
		// create backwards compatible list if possible.  Otherwise alert and return []
		var ret = [];

		// push first triple
		if (this.tripleList.length > 0) {
			ret.push(this.tripleList[0][0]);
			ret.push(this.tripleList[0][1]);
			ret.push(this.tripleList[0][2]);
		}

		// push the rest skipping the first value presuming it is a duplicate of the last value in the prev triple
		// if it is not a duplicate then alert and fail.
		for (var i=1; i < this.tripleList.length; i++) {
			if (this.tripleList[i][0] != this.tripleList[i-1][2]) {
				alert("Path can't be converted to old format: \n\n" + this.debugString());
				return [];
			}
			ret.push(this.tripleList[i][1]);
			ret.push(this.tripleList[i][2]);
		}
		return ret;

	},

	getClass0Name : function(tripleIndex) {
		return this.tripleList[tripleIndex][0];
	},

	getClass1Name : function(tripleIndex) {
		return this.tripleList[tripleIndex][2];
	},

	getStartClassName: function() {
		return this.startClassName;
	},

	// for paul
	getEndClassName : function () {
		return this.endClassName;
	},

	// for justin
	getAnchorClassName : function () {
		return this.endClassName;
	},

	getAttributeName : function(tripleIndex) {
		return this.tripleList[tripleIndex][1];
	},

	getTriple : function(tripleIndex) {
		// return a copy of the given triple
		return [this.tripleList[tripleIndex][0],
		        this.tripleList[tripleIndex][1],
		        this.tripleList[tripleIndex][2]
		       ];
	},

	getLength : function () {
		return this.tripleList.length;
	},

	containsClass : function(className) {
		return className in this.classHash;
	},

	isSingleLoop : function () {
		// detect exceptional path such as  Man -hasSon-> Man
		// It's the only case where start and end class don't help determine the direction of the path.
		return (this.tripleList.length == 1 && this.tripleList[0][0] == this.tripleList[0][2]);
	},

	deepCopy : function() {
		ret = new OntologyPath(this.getStartClassName());

		for (var i=0; i < this.tripleList.length; i++) {
			ret.addTriple(this.tripleList[i][0], this.tripleList[i][1], this.tripleList[i][2]);
		}
		return ret;
	},

	debugString : function() {
		ret = "(OntologyPath from " + this.getStartClassName() + " to " + this.getEndClassName() + ") \n [";
		for (var i=0; i < this.tripleList.length; i++) {
			var t = this.tripleList[i];
			ret = ret + "   [" + new OntologyName(t[0]).getLocalName() + ", " + new OntologyName(t[1]).getLocalName() + ", " + new OntologyName(t[2]).getLocalName() + "],\n  ";
		}
		ret = ret + "]";
		return ret;
	},

	asString : function() {
		// generate a one-line string for the user to choose or select paths
		ret = "";
		for (var i=0; i < this.tripleList.length; i++) {

			var from =  new OntologyName(this.tripleList[i][0]).getLocalName();
			var via =  new OntologyName(this.tripleList[i][1]).getLocalName();
			var to =  new OntologyName(this.tripleList[i][2]).getLocalName();

			// Always show first class and attribute
			ret += from + "." + via + " ";

			// If "to" does not equal first class in next triple then put it in too
			if (i == this.tripleList.length - 1 || to != this.tripleList[i+1][2].getLocalName()) {
				ret += to + " ";
			}
		}

		return ret;
	},

    genPathString : function(anchorNode, reverseFlag) {
        var str = anchorNode.getSparqlID() + ": ";

        // is path connecting at start instead of "normal" end
        var startAtNew = !reverseFlag || this.getEndClassName() != anchorNode.getURI();

        var firstName = new OntologyName(this.getStartClassName()).getLocalName();
        if (startAtNew) {
            str += firstName + "_NEW";
        } else {
            str += anchorNode.getBindingOrSparqlID();
        }
        var lastNameSoFar = firstName;

        for (var i=0; i < this.getLength(); i++) {
            var class0 = new OntologyName(this.getClass0Name(i)).getLocalName();
            var att = new OntologyName(this.getAttributeName(i)).getLocalName();
            var class1 = new OntologyName(this.getClass1Name(i)).getLocalName();
            var sub0 = "";
            var sub1 = "";

            // SUB-stitute in binding/sparqlID of node if needed at end of path
            if (i == this.getLength() - 1 && startAtNew) {
                if (class0 == lastNameSoFar) {
                    sub1 = anchorNode.getBindingOrSparqlID();
                } else {
                    sub0 = anchorNode.getBindingOrSparqlID();
                }
            }

            if ( class0 == lastNameSoFar ) {
                str += "-" + att + "->";
                str += sub1 ? sub1 : class1;
                lastNameSoFar = class1;
            } else {
                str += "<-" + att + "-";
                str += sub0 ? sub0 : class0;
                lastNameSoFar = class0;
            }
        }
        if (! startAtNew) str += "_NEW";


        return str;
    },

};



/*
 * OntologyRestriction
 * (Only enough code to store them in oinfo)
 */

 var OntologyRestriction = function(pred, obj) {
     this.predicate = pred;
     this.object = obj;
 };

 OntologyRestriction.prototype = {

     getUniqueKey : function () {
         return this.predicate + "," + this.object;
     },
 };

/*
 * OntologyDatatype
 */
var OntologyDatatype = function(name, equivalentType) {
    // parentName can be ""
    this.name = new OntologyName(name);
    this.xsdTypes = [];
    this.strTypes = [];
    this.addEquivalentType(equivalentType);
    this.annotation = new OntologyAnnotation();
    this.restrictions = {};
};

OntologyDatatype.prototype = {
    // In java, sub-classing takes care of this
    addAnnotationComment :        function(c) { return this.annotation.addAnnotationComment(c); },
    clearAnnotationComments:      function()  { return this.annotation.clearAnnotationComments(); },
    addAnnotationLabel :          function(l) { return this.annotation.addAnnotationLabel(l); },
    clearAnnotationLabels:        function()  { return this.annotation.clearAnnotationLabels(); },
    getAnnotationComments :       function()  { return this.annotation.getAnnotationComments(); },
    getAnnotationLabels :         function()  { return this.annotation.getAnnotationLabels(); },
    getAnnotationCommentsString : function()  { return this.annotation.getAnnotationCommentsString(); },
    getAnnotationLabelsString :   function()  { return this.annotation.getAnnotationLabelsString(); },
    generateAnnotationRdf :       function(d) { return this.annotation.generateAnnotationRdf(d); },
    generateAnnotationsSADL :     function()  { return this.annotation.generateAnnotationsSADL(); },

    getName : function () {
        return this.name;
    },

    getNameStr : function(stripNsFlag) {
		if (stripNsFlag)
			return this.name.getLocalName();
		else
			return this.name.getFullName();
	},

    getEquivalentTypes : function() {
        return this.strTypes;
    },

    getEquivalentXSDTypes : function() {
        return this.xsdTypes;
    },

    /*
     * Adds equivalentType.  No-op if type already exists
     */
    addEquivalentType : function(fullURI) {
        if (this.strTypes.indexOf(fullURI) == -1) {
            this.strTypes.push(fullURI);
            this.xsdTypes.push((new OntologyName(fullURI)).getLocalName());
        }
    },

    addRestriction : function(pred, obj) {
        var restriction = new OntologyRestriction(pred, obj);
        var key = restriction.getUniqueKey();
        if (! key in this.restrictions) {
            this.restrictions[key] = restriction;
        }
    },
    
    getRestrictions : function() {
	 	var ret = [];
	 	for (var key in this.restrictions) {
			ret.push(this.restrictions[key]);
		}
		return ret;
	}


};


/*
 * OntologyProperty
 */
var OntologyProperty = function(name, domainStr, rangeStr, optToCopy) {
	this.name = new OntologyName(name);
	if (optToCopy) {
		for (var key in optToCopy.rangeHash ) {
			this.rangeHash[key] = optToCopy.rangeHash[key].deepCopy();
		}
	} else {
	    this.rangeHash = {};
	    this.rangeHash[domainStr] = new OntologyRange(rangeStr);
	    this.annotation = new OntologyAnnotation();
	}
};

OntologyProperty.prototype = {
    // In java, sub-classing takes care of this
    addAnnotationComment :        function(c) { return this.annotation.addAnnotationComment(c); },
    clearAnnotationComments:      function()  { return this.annotation.clearAnnotationComments(); },
    addAnnotationLabel :          function(l) { return this.annotation.addAnnotationLabel(l); },
    clearAnnotationLabels:        function()  { return this.annotation.clearAnnotationLabels(); },
    getAnnotationComments :       function()  { return this.annotation.getAnnotationComments(); },
    getAnnotationLabels :         function()  { return this.annotation.getAnnotationLabels(); },
    getAnnotationCommentsString : function()  { return this.annotation.getAnnotationCommentsString(); },
    getAnnotationLabelsString :   function()  { return this.annotation.getAnnotationLabelsString(); },
    generateAnnotationRdf :       function(d) { return this.annotation.generateAnnotationRdf(d); },
    generateAnnotationsSADL :     function()  { return this.annotation.generateAnnotationsSADL(); },

	getName : function() {
		return this.name;
	},

    setName : function(n) {
        this.name = n;
    },
	
	getRange : function(domain, oInfo) {
		var ret = this.rangeHash[domain.getNameStr(false)];
		if (ret != null) {
			return ret;
		} else if (domain != null) {
			// try parents breadth-first
			for (var c of oInfo.getClassAncestorNames(domain)) {
				ret = this.rangeHash[c];
				if (ret != null) {
					return ret;
				}
			}
		} 
		
		throw new Error("Can not find range of property: " + this.getNameStr(false) + " for domain: " + domain.getNameStr(false));
	},
	
	getRangeDomains : function() {
		return Object.keys(this.rangeHash);
	},
	
	setRange : function(domain, range) {
		this.rangeHash[domain.getNameStr(false)] = range;
	},
	
	removeRange : function(domain) {
		if (typeof domain == 'string') {
			delete this.rangeHash[domain];
		} else {
			delete this.rangeHash[domain.getNameStr(false)];
		}
	},
	
	removeFromRange : function(domainStr, uri) {
		var oRange = this.rangeHash[domainStr];
		oRange.removeUri(uri);
	},
	
	addRange : function(domain, rangeUri) {
		var domainUri = domain.getNameStr(false);
		if (Object.keys(this.rangeHash).indexOf(domainUri) > -1) {
			this.rangeHash[domainUri].addRange(rangeUri);
		} else {
			this.rangeHash[domainUri] = new OntologyRange(rangeUri);
		}
	},
	
	getExactRange : function(domain) {
		return this.rangeHash[domain]
	},

    getLocalName : function() {
		return this.name.getLocalName();
	},
	getNamespace : function() {
		return this.name.getNamespace();this.getNameStr(true).toLowerCase().indexOf(pat) > -1
	},

	getNameStr : function(stripNsFlag) {
		if (stripNsFlag)
			return this.name.getLocalName();
		else
			return this.name.getFullName();
	},
	
	getAllRangeUris : function() {
		ret = [];
		for (var domainStr in this.rangeHash) {
			ret = ret.concat(this.rangeHash[domainStr].getUriList());
		}
		// uniquify
		return Array.from(new Set(ret));
	},

	// get name and all ranges joined by " "
	getPowerMatchString : function() {
		var retList = [this.getNameStr(true)];
		retList = retList.concat(this.getAllRangeUris());
		
		return retList.join(" ");
	},
	
	powerMatch : function(pattern) {
		// match against the property names and ranges
		// Case-insensitive
		var pat = pattern.toLowerCase();

		var rangeUris = [];
		for (var d of Object.keys(this.rangeHash)) {
			rangeUris = rangeUris.concat(this.rangeHash[d].getUriList());
		}
		var rangeUriString = rangeUris.join(",");
		
		return this.getPowerMatchString().toLowerCase().indexOf(pat) > -1 ;
	}
};

/*
 * OntologyRange
 */
var CLASS = "http://www.w3.org/2002/07/owl#Class";
var OntologyRange = function(fullname) {
	var className = (fullname == null || fullname.trim() == "" ? CLASS : fullname);
	
	// uris is a hash with values of 1 to efficiently avoid duplicates.
	// add:  this.uri[className] = 1
	// check: this.uri[className] != undefined
	this.uris = {};
    this.uris[className] = 1;
};

OntologyRange.parseDisplayString = function(str) {
	ret = [];
	if (str.indexOf("{") == -1) {
		ret.push(str);
	} else {
		var str1 = str.replaceAll("{", "").replaceAll("}", "").trim();
		for (var uri of str1.split(" or ")) {
			ret.add(uri);
		}
	}
	return ret;
};

OntologyRange.prototype = {
	
	deepCopy : function () {
		var copy = null;
		for (var name in this.uris) {
			if (copy == null) {
				copy = new OntologyRange(name);
			} else {
				copy.addRange(name);
			}
		}
		return copy;
	},
	
	addRange : function(name) {
		this.uris[name] = 1;
	},
	
	containsDefaultClass : function() {
		return this.uris[CLASS] != undefined;
	},
	
	isComplex : function() {
		return Object.keys(this.uris).length > 1;
	},
	
	getUriList : function() {
		return Object.keys(this.uris);
	},
	
	containsUri : function(rangeURI) {
		return this.uris[rangeURI] != undefined;
	},
	
	removeUri : function(rangeURI) {
		delete this.uris[rangeURI];
	},
	
	equalsUri : function(rangeURI) {
		return !this.isComplex() && this[rangeURI] != undefined;
	},
	
	getSimpleUri : function() {
		if (this.isComplex()) {
			throw new Error("Internal error: can not get simple name of a complex range");
		}
		for (var ret in this.uris) {
			return ret;
		}
		return "";
	},
	
	getDisplayString : function(abbreviate) {
		return OntologyRange.getDisplayString(Object.keys(this.uris), abbreviate);
	},

};

OntologyRange.getDisplayString = function(uris, abbreviate) {
		if (abbreviate) {
			var modified = [];
			for (var uri of uris) {
				modified.push(new OntologyName(uri).getLocalName());
			}
			if (modified.length == 1) {
				return modified.join("");
			} else {
				return "{" + modified.join(" or ") + "}";
			}
		} else {
			if (uris.length == 1) {
				return uris.join("");
			} else {
				return "{" + uris.join(" or ") + "}";
			}
		}
	}
