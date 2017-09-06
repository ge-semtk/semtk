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
    this.propHash = {};         // propHash[propURI] = oProp object
    this.subclassHash = {};     // for each class list of sub-classes   (NOTE: super-classes are stored in the class itsself)
    this.enumHash = {};         // this.enumHash[className] = [ full-uri-of-enum-val, full-uri-of-enum_val2... ]
    
    this.maxPathLen = 50;
    this.pathWarnings = [];
    this.connHash = {};         // calculated as needed. connHash[class] = [all single-hop paths to or from other classes]
    
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

OntologyInfo.prototype = {
	
	addClass : function(ontClass) {
		var classNameStr = ontClass.getNameStr();
		this.connHash = {};
		
		// silently overwrites if it already exists
		this.classHash[classNameStr] = ontClass;
		
		// store subClasses
		var supClassName = ontClass.getParentNameStr();
		if (!(supClassName in this.subclassHash)) {
			this.subclassHash[supClassName] = [classNameStr];
		} else {
			this.subclassHash[supClassName].push(classNameStr);
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
				rangeClasses.concat(this.getSubclassNames(rangeClasses[0]));
				
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
		return this.classHash.length;
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
	
	getClassParent : function (ontClass) {
		// returns parent as class object, or undefined
		return this.getClass(ontClass.getParentNameStr());
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
	
	getSuperclassNames : function (classNameStr) {
		// walk up list of all superclasses
		var ret = [];
		var cname = classNameStr;
		
		// PEC TODO: could eventually walk right out of the domain.
		// but there is currently no mechanism to store domain(s).
		// Hope that parents don't actually point out of the domain.
		while ((cname = this.classHash[cname].getParentNameStr()) != "" ) {
			ret.push(cname);
		}
		return ret;
	},
	
	getPropNames : function() {
		// returns an array of all known properties
		return Object.keys(this.propHash);
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
		return (propNameStr in this.propHash);
	},
	
	getInheritedProperties : function(ontClass) {
		// return an array of OntologyProperties representing all inherited properties
		// error if any class names do not exist
		
		var ret = [];
		var dup = {};
			
		var cName = ontClass.getNameStr();
		var oClass = ontClass;
		while (cName != "") {
			
			var p = oClass.getProperties();
		
			for (var i=0; i < p.length; i++) {
				// push class if it isn't already on the list
				var key = p[i].getNameStr() + ":" + p[i].getRangeStr();
				if (!(key in dup)) {
					ret.push(p[i]);
					dup[key] = 1;
				}
			}
			
			// get name of parent and look up object
			cName = oClass.getParentNameStr();
			if (cName != "") {
				var oClass = this.getClass(cName);
				if (oClass == null)
					alert("ERROR: OntologyInfo.getInheritedProperties(): bad class name:" + cName);
			}
		}
		
		// properties are sorted by subclass as we walked up the tree
		// the following code makes them alphabetical...just to show off maybe
		ret.sort( function(a,b) {if (a.getNameStr() < b.getNameStr())  return -1; else if (a.getNameStr() > b.getNameStr())  return 1; else return 0;} );
		
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
		// load queryResult
		for (var i=0; i < subClassList.length; i++) {
			var x = new OntologyClass(subClassList[i], superClassList[i]);
			this.addClass(x);
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
			var prop = new OntologyProperty(propList[i], rangeList[i]);
			var c = this.classHash[classList[i]];
			c.addProperty(prop);
			this.propHash[propList[i]] = prop;
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
		// is class1 a class2 ?    Recursive check.
		var c = class1;
		
		if (!class1 || !class2) return false;
		
		do {
			if (c.equals(class2)) return true;
			c = this.getClassParent(c);
		} while (c);
		
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
			
			var sparql = this.getEnumQuery(this.asyncDomain);
    		this.asyncStatusCallback("Loading ontology (4/4: enums)");
    		this.executeQuery(sparql, this.doLoad4.bind(this));
		}	
    },
    
    doLoad4 : function  (qsResult) {
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
            "topLevelClassList" : [],
            "subClassSuperClassList" : [],
            "classPropertyRangeList" : [],
            "classEnumValList" : [],
            "prefixes" : {}
        };
        
        var prefixToIntHash = {};

        // topLevelClassList and subClassSuperClassList
        for (var c in this.classHash) {
            var parent = this.classHash[c].getParentNameStr();
            var name = this.classHash[c].getNameStr();
            if (parent == "") {
                json.topLevelClassList.push(this.prefixURI(name, prefixToIntHash));
            } else {
                json.subClassSuperClassList.push([this.prefixURI(name, prefixToIntHash), 
                                                  this.prefixURI(parent, prefixToIntHash) ]);
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
        
    },
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
 * OntologyClass
 */
var OntologyClass = function(name, parentName) {
    this.name = new OntologyName(name);
    this.parentName = new OntologyName(parentName);
    this.properties = [];
};

OntologyClass.prototype = {
	getNameStr : function(stripNsFlag) {
		if (stripNsFlag) 
			return this.name.getLocalName();
		else
			return this.name.getFullName();
	},
	
	getParentNameStr : function(stripNsFlag) {
		if (stripNsFlag) 
			return this.parentName.getLocalName();
		else
			return this.parentName.getFullName();
	},
	
	getNamespaceStr : function() {
		return this.name.getNamespace();
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
		return 
	},
	
	getPropertyByKeyname : function(keyName) {
		// find first matching property
		for (var i=0; i < this.properties.length; i++) {
			if (this.properties[i].getNameStr().endsWith('#' + keyName)) {
				return this.properties[i];
			}
		}
		return null
	},
	
	addProperty : function(ontProperty) {
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
    
    
    
};

/*
 * OntologyProperty
 */
var OntologyProperty = function(name, range) {
    this.name = new OntologyName(name);
    this.range = new OntologyRange(range);

};

OntologyProperty.prototype = {
	getName : function() {
		return this.name;
	},
	
	getRange : function() {
		return this.range;
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
	}
};

/*
 * OntologyRange
 */
var OntologyRange = function(fullname) {
    this.name = fullname;

};
OntologyRange.prototype = {
		
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
