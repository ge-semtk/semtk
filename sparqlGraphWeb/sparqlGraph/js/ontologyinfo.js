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
 * OntologyInfo
 */
var OntologyInfo = function(optJson) {
    this.classHash = {};        // classHash[classURI] = oClass object
    this.propertyHash = {};         // propertyHash[propURI] = oProp object
    this.subclassHash = {};     // for each class list of sub-classes names  (NOTE: super-classes are stored in the class itsself)
    this.enumHash = {};         // this.enumHash[className] = [ full-uri-of-enum-val, full-uri-of-enum_val2... ]
    
    this.maxPathLen = 50;
    this.pathWarnings = [];
    
    this.connHash = {};           // calculated as needed. connHash[class] = [all single-hop paths to or from other classes]
    this.prefixHash = null;       // calculated as needed: prefixHash[longName]=shortName
    
    this.getFlag = false;
   
    this.asyncDomain = null;
    this.asyncSei = null;
    this.asyncStatusCallback = null;
    this.asyncSuccessCallback = null;
    this.asyncFailureCallback = null;
    
    if (typeof optJson != "undefined") {
        this.addJson(optJson);
    }
};

OntologyInfo.JSON_VERSION = 2;

OntologyInfo.localizeNamespace = function(fullNamespace) {
   return fullNamespace.substring(fullNamespace.lastIndexOf('/')+1);
};

OntologyInfo.getSadlRangeList = function() {
   return ["http://www.w3.org/2001/XMLSchema#int",
           "http://www.w3.org/2001/XMLSchema#string",
           "http://www.w3.org/2001/XMLSchema#dateTime"];
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
        for (var p in this.propertyHash) {
            uriList.push(this.propertyHash[p].getRangeStr());
        }
        
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
		var supClassNames = ontClass.getParentNameStrs();
        for (var i=0; i < supClassNames.length; i++) {
            
            if (!(supClassNames[i] in this.subclassHash)) {
                this.subclassHash[supClassNames[i]] = [];
            } 
            
            this.subclassHash[supClassNames[i]].push(classNameStr);
        }
		
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
				var rangeClasses = [prop.getRangeStr()];
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
	
	getClassNames : function() {
		// returns an array of all known classes
		return Object.keys(this.classHash);
	},
	
	getClassParents : function (ontClass) {
        var ret = [];
		var names = ontClass.getParentNameStrs();
        for (var i=0; i < names.length; i++) {
            ret.push(this.getClass(names[i]));
        }
		return ret;
	},
	
	getSubclassNames : function (classNameStr) {
		// recursively find all subclasses
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
        
        var parentNames = this.classHash[subclassName].getParentNameStrs(false);
        for (var i=0; i < parentNames.length; i++) {
            var currParentName = parentNames[i];
            ret.push(currParentName);
            superclasses.push(this.classHash[currParentName]);
        }
        
        for (var i=0; i < superclasses.length; i++) {
            var currParentClass = superclasses[i];
            ret = ret.concat(this.getSuperclassNames(currParentClass.getNameStr(false)));
        }
		return ret;
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
    
    uriIsKnown : function(uri) {
        return (    this.getPropNames().indexOf(uri) > -1   ||
                    this.getClassNames().indexOf(uri) > -1  ||
                    this.getEnumNames().indexOf(uri) > -1   
               );
    },
	
	getConnList : function(classNameStr) {
		// return or calculate all legal one-hop path connections to and from a class
		
		if (!(classNameStr in this.connHash)) {
			var ret = [];
			// var prop; // bug removed when I ported to Java
			var path;
			if (! (classNameStr in this.classHash)) {
				alert("Internal error in OntologyInfo.getConnList(): class name is not in the ontology: " + classNameStr);
			}
			var classVal = this.classHash[classNameStr];
			var foundHash = {};     // hash of path.asString()     PEC TODO FAILS when Man-hasSon->Man hashes same as Man<-hasSon-Man
			var hashStr = '';
			
			
			//--- calculate HasA:   exact range classes for all inherited properties
			var props = this.getInheritedProperties(classVal);
			for (var i=0; i < props.length; i++) {
				var prop = props[i];
				var rangeClassName = prop.getRangeStr();
				
				// if the range class in this domain
				if (this.containsClass(rangeClassName)) {
					
					// Exact match:  class -> hasA -> rangeClassName
					path = new OntologyPath(classNameStr);
					path.addTriple(classNameStr, prop.getNameStr(), rangeClassName);
					hashStr = path.asString();
					if (! (hashStr in foundHash)) {
						ret.push(path);
						foundHash[hashStr] = 1;
					}
				
					// Sub-classes:  class -> hasA -> subclass(rangeClassName)
					var rangeSubNames = this.getSubclassNames(rangeClassName);
					for (var j=0; j < rangeSubNames.length; j++) {
						if (this.containsClass(rangeSubNames[j])) {
							path = new OntologyPath(classNameStr);
							path.addTriple(classNameStr, prop.getNameStr(), rangeSubNames[j]);
							hashStr = path.asString();
							if (! (hashStr in foundHash)) {
								ret.push(path);
								foundHash[hashStr] = 1;
							}
						}
					}
				}
			}
			
			//--- calculate HadBy: class which HasA classNameStr
			
			// store all superclasses of target class
			var supList = this.getSuperclassNames(classNameStr);
			
			// loop through every single class in oInfo
			for (var cname in this.classHash) {
				
				// loop through every property
				// Issue 50 : fixed this to get inherited properties
				// var cprops = this.classHash[cname].getProperties();
				var cprops = this.getInheritedProperties(this.classHash[cname]);
				
				for (var i=0; i < cprops.length; i++) {
					var prop = cprops[i];
					var rangeClassStr = prop.getRangeStr();
					
					// HadBy:  cName -> hasA -> class
					if (rangeClassStr == classNameStr) {
						path = new OntologyPath(classNameStr);
						path.addTriple(cname, prop.getNameStr(), classNameStr);
						hashStr = path.asString();
						if (! (hashStr in foundHash)) {
							ret.push(path);
							foundHash[hashStr] = 1;
						}
					}
					
					// IsA + HadBy:   cName -> hasA -> superClass(class)
					for (var j = 0; j < supList.length; j++) {
						if (rangeClassStr == supList[j]) {
							path = new OntologyPath(classNameStr);
							path.addTriple(cname, prop.getNameStr(), classNameStr);
							hashStr = path.asString();
							if (! (hashStr in foundHash)) {
								ret.push(path);
								foundHash[hashStr] = 1;
							}
						}
					}
				}
			}
			this.connHash[classNameStr] = ret;
		}
		
		return this.connHash[classNameStr];
	},
	
	containsClass : function(classNameStr) {
		// returns an array of all known classes
		return (classNameStr in this.classHash);
	},
	
	containsProperty : function(propNameStr) {
		// does this contain given property
		return (propNameStr in this.propertyHash);
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
	
	getInheritedPropertyByKeyname(ontClass, propName) {
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
				var key = props[j].getNameStr() + ":" + props[j].getRangeStr();
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
	
	getSuperSubClassQuery : function (domain) {
		// returns a cheesy query that gives
		// domain - e.g. caterham.ge.com
		
		// PEC TODO: domain regex does not match the isInDomain() method logic
		return 'PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>    \
				PREFIX owl: <http://www.w3.org/2002/07/owl#>   \
				PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>              \
				PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>         \
        \
				select distinct ?x ?y  where {         \
				?x rdf:type owl:Class .  \
		        ?y rdf:type owl:Class .  \
				?x rdfs:subClassOf ?y  				  \
				  filter regex(str(?x),"^' + domain + '")              \
				  filter regex(str(?y),"^' + domain + '")              \
				  filter (?x != ?y).           						  \
				}    \
			';
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
	
	getTopLevelClassQuery : function (domain) {
		// returns a cheesy query that gives
		// domain - e.g. caterham.ge.com
		return 'PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>                \
		PREFIX owl: <http://www.w3.org/2002/07/owl#> 	    \
			PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>     \
			PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>                \
        \
			select distinct ?Class  {  									  \
			?Class rdf:type owl:Class filter regex(str(?Class),"^' + domain + '") .        \
			MINUS 													  \
			{ ?Sup rdf:type owl:Class.  \
			  ?Class rdfs:subClassOf ?Sup         								       \
			    filter regex(str(?Sup),"^' + domain + '")    	 				           \
			    filter (?Class != ?Sup).}   								  \
			}                \
			';
	},
	
	loadTopLevelClasses : function(classList) {
		// load from rows of [class]
		for (var i=0; i < classList.length; i++) {
			var x = new OntologyClass(classList[i], "");
			this.addClass(x);
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
	
	getLoadPropertiesQuery : function (domain) {
		// returns a cheesy query that gives
		// domain - e.g. caterham.ge.com
		return 'PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>              \
		PREFIX owl: <http://www.w3.org/2002/07/owl#>              \
			PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>              \
			PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>              \
			PREFIX  list: <http://jena.hpl.hp.com/ARQ/list#>              \
        \
			select distinct ?Class ?Property ?Range {              \
			{ \
			    ?Class rdf:type owl:Class. \
			    ?Property rdfs:domain ?Class filter regex(str(?Class),"^' + domain + '").              \
				?Property rdfs:range ?Range filter (regex(str(?Range),"^' + domain + '") || regex(str(?Range),"XML")).              \
			} UNION {             \
				?Class rdf:type owl:Class. \
				?Property rdfs:domain ?x.              \
				?x owl:unionOf ?y.              \
				?y rdf:rest* ?Rest0. ?Rest0 rdf:first ?Class filter regex(str(?Class),"^' + domain + '").              \
				?Property rdfs:range ?Range filter (regex(str(?Range),"^' + domain + '") || regex(str(?Range),"XML")).             \
			} UNION {             \
				?Class rdf:type owl:Class. \
				?Property rdfs:domain ?Class filter regex(str(?Class),"^' + domain + '").              \
				?Property rdfs:range ?x.              \
				?x owl:unionOf ?y.              \
				?y rdf:rest* ?Rest1. ?Rest1 rdf:first ?Range  filter (regex(str(?Range),"^' + domain + '") || regex(str(?Range),"XML")).              \
			} UNION {             \
				?Class rdf:type owl:Class. \
				?Property rdfs:domain ?x.              \
				?x owl:unionOf ?y.              \
				?y rdf:rest* ?Rest2. ?Rest2 rdf:first ?Class filter regex(str(?Class),"^' + domain + '").              \
				?Property rdfs:range ?x1.              \
				?x1 owl:unionOf ?y1.              \
				?y1 rdf:rest* ?Rest3. ?Rest3 rdf:first ?Range filter (regex(str(?Range),"^' + domain + '") || regex(str(?Range),"XML")).              \
			} UNION {             \
				?Class rdf:type owl:Class. \
				?Class rdfs:subClassOf ?x filter regex(str(?Class),"^' + domain + '").              \
				?x rdf:type owl:Restriction. ?x owl:onProperty ?Property.              \
				?x owl:onClass ?Range filter (regex(str(?Range),"^' + domain + '") || regex(str(?Range),"XML")).              \
			} UNION {             \
				?Class rdf:type owl:Class. \
				?Class rdfs:subClassOf ?x filter regex(str(?Class),"^' + domain + '").              \
				?x rdf:type owl:Restriction. ?x owl:onProperty ?Property. ?x owl:onClass ?y.              \
				?y owl:unionOf ?z. \
				?z rdf:rest* ?Rest4. ?Rest4 rdf:first ?Range filter (regex(str(?Range),"^' + domain + '") || regex(str(?Range),"XML")).              \
			} UNION {             \
				?Class rdf:type owl:Class. \
				?x1 owl:unionOf ?x2. \
				?x2 rdf:rest* ?Rest5. ?Rest5 rdf:first ?Class filter regex(str(?Class),"^' + domain + '").              \
				?x1 rdfs:subClassOf ?x .              \
				?x rdf:type owl:Restriction. ?x owl:onProperty ?Property. ?x owl:onClass ?y.              \
				?y owl:unionOf ?z. \
				?z rdf:rest* ?Rest6. ?Rest6 rdf:first ?Range filter (regex(str(?Range),"^' + domain + '") || regex(str(?Range),"XML")).        \               \
			} UNION {              \
				?Class rdf:type owl:Class. \
				?Class rdfs:subClassOf ?x filter regex(str(?Class),"^' + domain + '").              \
				?x rdf:type owl:Restriction. ?x owl:onProperty ?Property.              \
				?x owl:someValuesFrom ?Range filter (regex(str(?Range),"^' + domain + '") || regex(str(?Range),"XML")).              \
			} UNION {              \
				?Class rdf:type owl:Class. \
				?Class rdfs:subClassOf ?x filter regex(str(?Class),"^' + domain + '").              \
				?x rdf:type owl:Restriction. ?x owl:onProperty ?Property.              \
				?x owl:allValuesFrom ?Range filter (regex(str(?Range),"^' + domain + '") || regex(str(?Range),"XML")). \
			}              \
		}';
	},
	
	loadProperties : function(classList, propList, rangeList) {
		// load from rows of [class, property, range]
		for (var i=0; i < classList.length; i++) {
            var prop = null;
            
            // does property already exist
			if (propList[i] in this.propertyHash) {
				prop = this.propertyHash[propList[i]];
				
				// if property exists, make sure range is the same
				if (prop.getRangeStr() != rangeList[i]) {
					throw new Error("SemTk doesn't handle complex ranges.\nClass" + classList[i] +
                                    "property domain " + propList[i] +
                                    "\nrange 1: " + prop.getRangeStr() +
                                    "\nrange 2: " + rangeList[i]
								    );
				}
			} else {
				// if property doesn't exist, create it
				prop = new OntologyProperty(propList[i], rangeList[i]);
			}
			var c = this.classHash[classList[i]];
			c.addProperty(prop);
			this.propertyHash[propList[i]] = prop;
		}
	},
	
	getEnumQuery : function (domain) {
		// return every ?t ?e where 
		//     ?t is a class that "must be one of" ?e
		return  'select ?Class ?EnumVal where { \n' +
				'  ?Class <http://www.w3.org/2002/07/owl#equivalentClass> ?ec filter regex(str(?Class),"^' + domain + '").' +
				'  ?ec <http://www.w3.org/2002/07/owl#oneOf> ?c . \n' +
				'  ?c <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest>*/<http://www.w3.org/1999/02/22-rdf-syntax-ns#first> ?EnumVal. \n' +
				'}';
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
	
    getAnnotationLabelsQuery : function(domain) {
		// This query will be sub-optimal if there are multiple labels and comments for many elements
		// because every combination will be returned
		//
		// But in the ususal case where each element has zero or 1 labels and comments
		// It is more efficient to get them in a single query with each element URI only transmitted once.
		return "prefix owl:<http://www.w3.org/2002/07/owl#>\n" + 
				"prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" + 
				"\n" + 
				"select distinct ?Elem ?Label where {\n" + 
				" ?Elem a ?p.\r\n" + 
				" filter regex(str(?Elem),'^" + domain + "'). " + 
				" VALUES ?p {owl:Class owl:DatatypeProperty owl:ObjectProperty}.\n" + 
				"    optional { ?Elem rdfs:label ?Label. }\n" + 
				"}";
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
    
	getAnnotationCommentsQuery : function(domain) {
		// This query will be sub-optimal if there are multiple labels and comments for many elements
		// because every combination will be returned
		//
		// But in the ususal case where each element has zero or 1 labels and comments
		// It is more efficient to get them in a single query with each element URI only transmitted once.
		return "prefix owl:<http://www.w3.org/2002/07/owl#>\n" + 
				"prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" + 
				"\n" + 
				"select distinct ?Elem ?Comment where {\n" + 
				" ?Elem a ?p.\r\n" + 
				" filter regex(str(?Elem),'^" + domain + "'). " + 
				" VALUES ?p {owl:Class owl:DatatypeProperty owl:ObjectProperty}.\n" + 
				"    optional { ?Elem rdfs:comment ?Comment. }\n" +
				"}";
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
	
	findAllPaths : function(fromClassName, targetClassNames, domain) {
		//   A form of A* path finding algorithm
		//   See getConnList() for the types of connections that are allowed
		//   Returns a list shortest to longest:  [path0, path1, path2...]
		//        pathX.getStartClassName() == fromClassName
		//        pathX.getEndClassName() == member of targetClassNames
		//        pathX.asList() returns list of triple lists [[className0, att, className1], [className1, attName, className2]...]
		var t0 = (new Date()).getTime();
		this.pathWarnings = []; 
		var waitingList = [new OntologyPath(fromClassName)];
		var ret = [];
		var targetHash = {};              // hash of all possible ending classes:  targetHash[className] = 1
		
		var LENGTH_RANGE = 2;     // search only for paths this much longer than the shortest one found
		var SEARCH_TIME_MSEC = 5000;
		var LONGEST_PATH = 10;
		var CONSOLE_LOG = false;
		
		/* ISSUE 50:  but perhaps these should be handled by a caller, not by this method
		 * Pathfinding should also find paths
 		 *   A) from the new class to a superclass of an existing class
         *   B) from an existing class to a superclass of the new class.
		 */
		
		// return if there is no endpoint
		if (targetClassNames.length < 1) return [];
		
		// PEC CONFUSED: why is this here?  Process->hasNext->Process can't get past here.
		// return if this special case has no possible solution
		//if (targetClassNames.length == 1 && targetClassNames[0] == fromClassName) return [];
		
		// set up targetHash[targetClass] = 1
		for (var i=0; i < targetClassNames.length; i++) {
			targetHash[targetClassNames[i]] = 1;
		}
		
				
		// STOP CRITERIA A: search as long as there is a waiting list 
		while (waitingList.length > 0) {
			// pull one off waiting list
			var item = waitingList.shift();
			var waitClass = item.getEndClassName();
			var waitPath = item;
			
			// STOP CRITERIA B:  Also stop searching if:
			//    this final path (with 1 added connection) will be longer than the first (shortest) already found path
			if (ret.length > 0 && 
				(waitPath.getLength() + 1  > ret[0].getLength() + LENGTH_RANGE)) {
				break;
			} 

			// STOP CRITERIA C: stop if path is too long
			if (waitPath.getLength() > LONGEST_PATH) {
				break;
			}
			
			// STOP CRITERIA D: too much time spent searching
			var tt = (new Date()).getTime();
			// PEC TODO: false && turns it off for debugging
			if ( tt - t0 > SEARCH_TIME_MSEC) {
				alert("Note: Path-finding timing out.  Search incomplete.");
				break;
			}
			
			// get all one hop connections and loop through them
			var conn = this.getConnList(waitClass); 
			for (var i=0; i < conn.length; i++) {
				
				//  each connection is a path with only one node (the 0th)
				//  grab the name of the newly found class
				var newClass = "";
				var newPath = null;
				var loopFlag = false;
				
				// if the newfound class is pointed to by an attribute of one on the wait list
				if (conn[i].getStartClassName() == waitClass) {
					newClass = conn[i].getEndClassName();
					
				} else {
					newClass = conn[i].getStartClassName();
				}
				
				// check for loops in the path before adding the class
				if (waitPath.containsClass(newClass)) {
					loopFlag = true;
				} 
				
				// build the new path
				var t = conn[i].getTriple(0);
				newPath = waitPath.deepCopy();
				newPath.addTriple(t[0], t[1], t[2]);
				
				// if path leads anywhere in domain, store it
				var name = new OntologyName(newClass);
				if (name.isInDomain(domain)) {
					
					// if path leads to a target, push onto the ret list
					if (newClass in targetHash) {
						ret.push(newPath);
						if (CONSOLE_LOG) console.log(">>>found path " + newPath.debugString());
						
					// PEC CONFUSED: this used to happen every time without any "else" or "else if"
					
					// if path doens't lead to target, add to waiting list
					// But if it is a loop (that didn't end at the targetHash) then stop
					}  else if (loopFlag == false){
					    // try extending already-found paths
						waitingList.push(newPath);
						if (CONSOLE_LOG) console.log("searching " + newPath.debugString());
					}
					
				}
				
			}
		}
		
		if (CONSOLE_LOG) {
			console.log("These are the paths I found:");
			for (var i=0; i < ret.length; i++) {
				console.log(ret[i].debugString());
			}
			
			var t1 = (new Date()).getTime();
			console.log("findAllPaths time is: " + (t1-t0) + " msec");
		}
		return ret;
	},
	
	
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
	
	addPathWarning : function (txt) {
		// add a path warning if it is new
		if (this.pathWarnings.indexOf(txt) < 0) {
			this.pathWarnings.push(txt);
		}
	},
	
	hasPathWarnings : function () {
		return (this.pathWarnings.length > 0);
	},
	
	getPathWarnings : function () {
		ret = "";
		for (var i=0; i < this.pathWarnings.length; i++) {
			ret += this.pathWarnings[i] + "\n";
		}
		return ret;
	},
	
	clearPathWarnings : function() { 
		this.pathWarnings = []; 
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
    
    load : function (domain, interfaceOrClient, statusCallback, successCallback, failureCallback) {
    	// interfaceOrClient: sparqlServerInterface or MsiClientQuery
    	// 
    	this.asyncDomain = domain;
		this.asyncSei = interfaceOrClient;
		this.asyncStatusCallback = statusCallback;
		this.asyncSuccessCallback = successCallback;
		this.asyncFailureCallback = failureCallback;
		
    	// Need to initialize this object intelligently:  some server address
    	var sparql = this.getSuperSubClassQuery(this.asyncDomain);
    	statusCallback("Loading ontology (1/4: sub-classes)");
    	this.executeQuery(sparql, this.doLoad1.bind(this));
    },
    
    doLoad1 : function (qsResult) {
    	// if loading is still successful, load classes, run topLevelClass query, callback: doLoad2()
    	
    	if (!qsResult.isSuccess()) {
    		this.asyncFailureCallback("First connection to sparql endpoint failed.\nSuper-subclass query:\n\n" + qsResult.getStatusMessage());
			
    	} else {
            // Note: can load nothing if ontology is flat
    		this.loadSuperSubClasses(qsResult.getStringResultsColumn("x"), 
                                     qsResult.getStringResultsColumn("y"));
    		    	
    		var sparql = this.getTopLevelClassQuery(this.asyncDomain);
    		this.asyncStatusCallback("Loading ontology (2/4: top-level classes)");
        	
    		this.executeQuery(sparql, this.doLoad2.bind(this));
    	};
    },
    
    doLoad2 : function (qsResult) {
    	// if loading is still successful, load classes, run loadProperties query, callback: doLoad2()
    	
    	if (!qsResult.isSuccess()) {
    		this.asyncFailureCallback("Top-level class query:\n\n " + qsResult.getStatusMessage());

		} else {
			this.loadTopLevelClasses(qsResult.getStringResultsColumn("Class"));
    		    	
    		var sparql = this.getLoadPropertiesQuery(this.asyncDomain);
    		this.asyncStatusCallback("Loading ontology (3/4: properties)");
    		this.executeQuery(sparql, this.doLoad3.bind(this));
    	};
    },
    
    doLoad3 : function  (qsResult) {
    	// last in callback chain.  
    	if (!qsResult.isSuccess()) {
    		this.asyncFailureCallback("Class properties query:\n\n" + qsResult.getStatusMessage());
			
		} else {
			this.loadProperties(qsResult.getStringResultsColumn("Class"),
                                qsResult.getStringResultsColumn("Property"),
                                qsResult.getStringResultsColumn("Range") );
			
			var sparql = this.getAnnotationCommentsQuery(this.asyncDomain);
    		this.asyncStatusCallback("Loading ontology (4/6: comments)");
    		this.executeQuery(sparql, this.doLoad4.bind(this));
		}	
    },
    
    doLoad4 : function  (qsResult) {
    	// last in callback chain.  
    	if (!qsResult.isSuccess()) {
    		this.asyncFailureCallback("Annotation comments query:\n\n" + qsResult.getStatusMessage());
			
		} else {
			this.loadAnnotationComments(qsResult.getStringResultsColumn("Elem"),
                                        qsResult.getStringResultsColumn("Comment"));
			
			var sparql = this.getAnnotationLabelsQuery(this.asyncDomain);
    		this.asyncStatusCallback("Loading ontology (5/6: labels)");
    		this.executeQuery(sparql, this.doLoad5.bind(this));
		}	
    },
    
    doLoad5 : function  (qsResult) {
    	// last in callback chain.  
    	if (!qsResult.isSuccess()) {
    		this.asyncFailureCallback("Annotation labels query:\n\n" + qsResult.getStatusMessage());
			
		} else {
			this.loadAnnotationLabels(qsResult.getStringResultsColumn("Elem"),
                                      qsResult.getStringResultsColumn("Label") );
			
			var sparql = this.getEnumQuery(this.asyncDomain);
    		this.asyncStatusCallback("Loading ontology (6/6: enums)");
    		this.executeQuery(sparql, this.doLoad6.bind(this));
		}	
    },
    
    doLoad6 : function  (qsResult) {
    	// last in callback chain.  
    	if (!qsResult.isSuccess()) {
    		this.asyncFailureCallback("Enums query:\n\n" + qsResult.getStatusMessage());
   
		} else {
			this.loadEnums(qsResult.getStringResultsColumn("Class"),
                           qsResult.getStringResultsColumn("EnumVal"));
			this.asyncStatusCallback("");
			this.asyncSuccessCallback();
		}	
    },
    
    executeQuery : function (sparql, callback) {
    	// some kind of Justin / Kareem hack
    	// will die if using query server.  fix please.
	    if (this.getFlag) {
	    	this.asyncSei.executeAndParseGet(sparql, callback);
		} else {
			this.asyncSei.executeAndParse(sparql, callback);
		}
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
            "subClassSuperClassList" : [],
            "classPropertyRangeList" : [],
            "classEnumValList" : [],
            "annotationLabelList" : [],
            "annotationCommentList" : [],
            "prefixes" : {}
        };
        
        var prefixToIntHash = {};

        // topLevelClassList and subClassSuperClassList
        for (var c in this.classHash) {
            var parents = this.classHash[c].getParentNameStrs();
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
        
        // classPropertyRangeList
        for (var c in this.classHash) {
            var propList = this.classHash[c].getProperties();
            for (var i=0; i < propList.length; i++) {
                var oProp = propList[i];
                json.classPropertyRangeList.push([this.prefixURI(c, prefixToIntHash), 
                                                  this.prefixURI(oProp.getNameStr(), prefixToIntHash),
                                                  this.prefixURI(oProp.getRangeStr(), prefixToIntHash) ]);
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
        
        return json;
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
            throw new Error("Can't decode OntologyInfo JSON with newer version > " + OntologyInfo.JSON_VERSION + " found: " + version);
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
        this.loadSuperSubClasses(getColumn(json.subClassSuperClassList, 0),
                                 getColumn(json.subClassSuperClassList, 1)     
                                );
        this.loadProperties(getColumn(json.classPropertyRangeList, 0),
                            getColumn(json.classPropertyRangeList, 1),
                            getColumn(json.classPropertyRangeList, 2)
                            );
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
    
    genPathString : function(anchorNode, singleLoopFlag) {
        var str = anchorNode.getSparqlID() + ": ";

        // handle diabolical case
        if (singleLoopFlag) {
            cl = new OntologyName(this.getClass0Name(0)).getLocalName();
            var att = new OntologyName(this.getAttributeName(0)).getLocalName();
            str += anchorNode.getSparqlID() + "-" + att + "->" + cl + "_NEW";
        }
        else {
            var first = new OntologyName(this.getStartClassName()).getLocalName();
            str += first;
            if (first != anchorNode.getURI(true)) str += "_NEW";
            var last = first;

            for (var i=0; i < this.getLength(); i++) {
                var class0 = new OntologyName(this.getClass0Name(i)).getLocalName();
                var att = new OntologyName(this.getAttributeName(i)).getLocalName();
                var class1 = new OntologyName(this.getClass1Name(i)).getLocalName();
                var sub0 = "";
                var sub1 = "";

                // mark connecting node on last hop of this
                if (i == this.getLength() - 1) {
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
    },
		
};

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
	
	getParentNameStrs : function(stripNsFlag) {
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
			if (prop[i].getNameStr().toLowerCase().indexOf(pat) > -1 || 
				prop[i].getRangeStr().toLowerCase().indexOf(pat) > -1) {
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
 * OntologyProperty
 */
var OntologyProperty = function(name, range) {
    this.name = new OntologyName(name);
    this.range = new OntologyRange(range);
    this.annotation = new OntologyAnnotation();
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
	
	getRange : function() {
		return this.range;
	},
	
    getLocalName : function() {
		return this.name.getLocalName();
	},
	getNamespace : function() {
		return this.name.getNamespace();
	},
    
	getNameStr : function(stripNsFlag) {
		if (stripNsFlag) 
			return this.name.getLocalName();
		else
			return this.name.getFullName();
	},
	
	getRangeStr : function(stripNsFlag) {
		if (stripNsFlag) 
			return this.range.getLocalName();
		else
			return this.range.getFullName();
	},
	
	powerMatch : function(pattern) {
		// match against the property names and ranges
		// Case-insensitive
		var pat = pattern.toLowerCase();
		
		return (this.getNameStr(true).toLowerCase().indexOf(pat) > -1 || 
				this.getRangeStr(true).toLowerCase().indexOf(pat) > -1);
	}
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
 * OntologyRange
 */
var OntologyRange = function(fullname) {
    this.name = fullname;

};
OntologyRange.prototype = {
		
    getName : function() {
        return new OntologyName(this.name);
    },
	getLocalName : function() {
		return this.name.split('#')[1];
	},
	getNamespace : function() {
		return this.name.split('#')[0];
	},
	getFullName : function() {
		return this.name;
	},
	isInDomain : function(domain) {
		// does my name start with this domain
		//i = this.name.indexOf(domain);
		//return (i == 0);
		var m = this.name.match("^"+domain);
		return (m != null);
	},
	
};
