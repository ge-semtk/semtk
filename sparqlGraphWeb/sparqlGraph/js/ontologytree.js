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
 * Attribute
 */
var Attribute = function(attName, attType) {
	this.name = attName;
	this.type = attType;
};

/*
 * OntologyTree
 */
var OntologyTree = function(dynaTree, optEnumFlag, optCollapseNamespaceList) {
	// EnumFlag : should enumerations appear
	// CollapseNamespaceList : in this namespace, only show sub-classes that are outside the namespace
    this.tree = dynaTree;
	this.HTMLOpenHighlight = "";
	this.specialClassHTML1 = "";
	this.specialClasses = [];
	this.oInfo = null;
	this.subsetURIs = null;
	
	this.enumFlag = (typeof optEnumFlag === 'undefined') ? true : optEnumFlag;
	this.collapseList = (typeof optCollapseNamespaceList === 'undefined') ? [] : optCollapseNamespaceList;
};

	
OntologyTree.prototype = {
	
	setSpecialClassFormat : function(htmlStart, htmlEnd) {
		// special classes have html tags before and after them in their title
		this.HTMLOpenHighlight = htmlStart;
		this.specialClassHTML1 = htmlEnd;
	},
	
	addSpecialClass : function(fullName) {
		this.specialClasses.push(fullName);
	},
	
	getSpecialClasses : function() {
		return this.specialClasses;
	},
	
	addSpecialClasses : function(nameList) {
		this.specialClasses = this.specialClasses.concat(nameList);
	},
	
	classIsSpecial : function (fullName) {
		return (this.specialClasses.indexOf(fullName) > -1);
	},
	
    //
    // delete old oInfo and put noOInfo into the tree
    //
    // preserve expanded/selected status of nodes
    //   optRenameFrom \
    //   optRenameTo   /  lists preserve status across name changes
    //
    update : function (newOInfo, optRenameFrom, optRenameTo) {
        var renameFrom = optRenameFrom ? optRenameFrom : [];
        var renameTo   = optRenameTo   ? optRenameTo   : [];
        var expandList = [];
        var selectList = [];

        // save expanded and selected nodes
        this.tree.getRoot().visit(function(elist, slist, node){
			if (node.bExpanded) {
                var i = renameFrom.indexOf(node.data.tooltip);
                var val = (i > -1) ? renameTo[i] : node.data.tooltip;
                elist.push(val);
			}
            if (node.bSelected) {
                var i = renameFrom.indexOf(node.data.tooltip);
                var val = (i > -1) ? renameTo[i] : node.data.tooltip;
                slist.push(val);
			}
		}.bind(this,expandList,selectList));
        
        // reset oInfo
        this.setOInfo(newOInfo);
        this.showAll();
        
        // restore expanded and selected nodes
        this.tree.getRoot().visit(function(elist, slist, node){
            node.expand(elist.indexOf(node.data.tooltip) > -1);
            node.select(slist.indexOf(node.data.tooltip) > -1);
		}.bind(this,expandList,selectList));
    },
    
    activateByValue : function (val) {
        this.tree.getRoot().visit(function(v, node) {
            if (node.data.value == v) {    
                node.activate();
            }
        }.bind(this, val))
    },
    
	setOInfo : function (ontInfo) {
		// associate an OntologyInfo		
		this.oInfo = ontInfo;
	},
	
	showAll : function() {
		
		// populate all info from the saved this.oInfo into the dynatree
		this.subsetURIs = null;
		
		var names = this.oInfo.getClassNames();
		this.tree.enableUpdate(false);
    	this.tree.getRoot().removeChildren();
    	
		
		for (var i=0; i < names.length; i++) {
			var c = this.oInfo.getClass(names[i]);
			if (	this.enumFlag || 
					c.getProperties().length > 0 || 
					this.oInfo.getDescendantProperties(c).length > 0 ||
					this.oInfo.getInheritedProperties(c).length > 0     ) {
				this.addOntClass(c);
			}
		}
		
		
		this.tree.getRoot().sortChildren(this.cmp, true);
    	this.tree.enableUpdate(true);
	},
	
	showSubset : function(names) {
		// add a list of classes to the tree
		this.subsetURIs = names;
		this.tree.enableUpdate(false);
		for (var i=0; i < names.length; i++) {
			this.addOntClass(this.oInfo.getClass(names[i]));
		}
		
		this.tree.getRoot().sortChildren(this.cmp, true);
		this.tree.enableUpdate(true);
	},
	
	
	addOntClass : function(ontClass, optOntPropList) {
		// Add a class to the tree.
		//     -> silently ignores request to add duplicate classes or properties
		// optOntPropList - if this OntologyProperty is not null, then add only this property
		
		// find the parent
		var className = ontClass.getNameStr();
		var classNameLocal = ontClass.getNameStr(true);
		var parentName;
		var parentNameLocal;
    	var parentNode;
    	var newNode;
    	var list = [];
    	
    	// do nothing if ontClass's namespace is in the collapseList
    	if (this.collapseList.indexOf(ontClass.getNamespaceStr()) > -1) {
    		return;
    	}
    	
    	// get class parent.  Keep walking up until null, or until namespace is not in this.collpaseList
    	var ontParents = this.oInfo.getClassParents(ontClass);
        
        // PEC TODO: ontologyTree needs to handle multiple inheritence
        var ontParent = ontParents.length > 0 ? ontParents[0] : null;
        
        if (ontParents.length > 1) {
            throw new Error("OntologyTree does not yet support classes with multiple parents: " + ontClass.getNameStr());
        }
        
    	while (ontParent && this.collapseList.indexOf(ontParent.getNamespaceStr()) > -1) {
    		ontParents = this.oInfo.getClassParents(ontParent);
            
            // PEC TODO: ontologyTree needs to handle multiple inheritence
            ontParent = ontParents.length > 0 ? ontParents[0] : null;
    	}
    	
    	if (ontParent) {
    		parentName =      ontParent.getNameStr();
    		parentNameLocal = ontParent.getNameStr(true);

    	} else {
    		// no parent: use the namespace string
    		parentName = ontClass.getNamespaceStr();
    		parentNameLocal = parentName.substring(parentName.lastIndexOf('/')+1);
        } 
    	
    	// Check if parent is in the tree
    	list = this.getNodesByURI(parentName);
    	if (list.length == 0) {
    		parentNode = null;
    	} else if (list.length > 1) {
    		throw "Internal error in OntologyTree.addOntClass(): found parent class more than once in the tree: " + parentName;
    	} else {
    		parentNode = list[0];
    	}
    		
    	// recursively add parents
        if (parentNode == null) { 
        	var parentClass = this.oInfo.getClass(parentName);
        	if (parentClass) {
        		// if only adding specific property (ies)
        		if (typeof optOntPropList !== 'undefined') {
        			// add just the parent class with no properties
        			parentNode = this.addOntClass(parentClass, []);
        		} else {
        			// normally: add the entire parent class
        			parentNode = this.addOntClass(parentClass);
        		}
        		
        		
        	// break the recursion when we get to the namespace node.  This one is not a class.
        	} else {
        		parentNode = this.addNamespace(parentName, parentNameLocal);
        	}
        }
        
        // add self if class doens't exist
    	list = this.getNodesByURI(className);
    	if (list.length == 0) {
    		newNode = null;
    	} else if (list.length > 1) {
    		throw "Internal error in OntologyTree.addOntClass(): found class more than once in the tree: " + className;
    	} else {
    		newNode = list[0];
    	}
        if (newNode == null) {
	        // add the node
        	var title = classNameLocal;
        	if (this.classIsSpecial(className)) {
        		title = this.HTMLOpenHighlight + title + this.specialClassHTML1;
        	}
        	
	        newNode = parentNode.addChild({
	    		title: title,
	    		//key: classKey,
	    		tooltip: className,
	    		isFolder: true,
	    	});
	        newNode.data.value = className;
		}
        
        // Check if we're adding a specific optOntProp, or defaulting to add all of them
        var props = null;
        if (typeof(optOntPropList) !== 'undefined') {
        	props = optOntPropList;
        } else {
        	props = this.oInfo.getInheritedProperties(ontClass);
        }
        
        this.addNodeProps(newNode, ontClass, props);
        
        return newNode;
	},

    addNamespace : function(fullName, localName) {
        var parentNode = this.tree.getRoot().addChild({
            title: localName,
            tooltip: fullName,
            isFolder: true});
        
        parentNode.data.value = fullName;
        
        return parentNode
    },
    
	addNodeProps : function(node, ontClass, propList) {
		// add propList to node if they are not already there
		
        var localProps = ontClass.getProperties();
        var title = "";
        for (var i=0; i < propList.length; i++) {
        	var propName = propList[i].getNameStr();
        	
        	// add property if it don't exist yet under this class
        	var nodeList = this.getNodesByURI(propName);
        	var propNode = null;
        	for (var j=0; j < nodeList.length; j++) {
        		// found existing node if both the uri and the parent are the same
        		if (nodeList[j].getParent() == node) {
        			propNode = nodeList[j];
        		}
        	}
        	
        	if (propNode == null ) {
        		// format local properties different than inherited
        		if (localProps.indexOf(propList[i]) > -1) {
        			title = "<i>" + propList[i].getNameStr(true) + ": " + propList[i].getRangeStr(true) + "</i>";

        		} else {
    	        	title = propList[i].getNameStr(true) + ": " + propList[i].getRangeStr(true);

        		}
	        	
	        	// highlight if range is a special class
	        	if (this.classIsSpecial(propList[i].getRangeStr(false))) {
	        		title = this.HTMLOpenHighlight + title + this.specialClassHTML1;
	        	}
	        	
	        	var child = node.addChild({
	        		title: title,
	        		//key: propName,
	        		tooltip: propName,
	        		isFolder: false
	        	});
	        	child.data.value = propName;
        	}
        }
		
	},
	
	// keys are now randomly generated
	// URI is stored in node.data.value
	getNodeByKey : function(keyName) {
		alert("getNodeByKey Deprecated 7/20/15 PEC");
		return this.tree.getNodeByKey(keyName);
	},
	nodeGetKeyname : function(node) {
		alert("nodeGetKeyname Deprecated 7/20/15 PEC");
		return node.data.key;
	},
	// ==================================
	getNodeByURI : function(val) {
		// with any inheritance, properties can appear in the ontologyTree more than once
		// later, we might add multiple inheritance, and then classes can also appear more than once
		// Use getNodesByURI() instead, and check the length of the return list.
		alert("Internal error: called deprecated function OntologyTree.getNodeByURI()");
	},
	
	getNodesByURI : function(uri) {
		// uri can be a class or property
		// find all tree nodes that match
		var ret = [];
		this.tree.getRoot().visit(function(node){
			if (node.data.value == uri) {
				ret.push(node);
			}
		});
		return ret;
	},
	
	nodeGetURI : function(node, optLocalFlag) {
		var localFlag = (typeof optLocalFlag === 'undefined') ? false : optLocalFlag;
		
		if (localFlag) {
			return new OntologyName(node.data.value).getLocalName();
		}
		else {
			return node.data.value;
		}
	},
	
    nodeIsClass : function(node) {
    	return this.oInfo.containsClass(this.nodeGetURI(node));
    },
    
    nodeIsProperty : function(node) {
    	return this.oInfo.containsProperty(this.nodeGetURI(node));
    },
    
    nodeIsNamespace : function(node) {
        return (this.node.value.indexOf('#') == -1);
    },
    
    nodeGetParent : function(node) {
    	return node.getParent();
    },

	find : function(pattern) {
		
		// There is a bug in dynatree that I can't quite nail down.
		// Running these four lines prevents it.
		// This is only necessary once, after classes are loaded
		// But it is so painless that I've kept the code cleaner by running it every time.
		this.tree.enableUpdate(false);
    	this.expandAll();
    	this.collapseAll();
    	this.tree.enableUpdate(true);
    	// ---------------------------------------
    	
		// opens and highlights all matches
		try {
			this.tree.getRoot().find(pattern);
		} catch (err) {
			alert("Attempt to avoid dynatree/jquery error failed.  \n To recover, reload your data and expand/collapse the tree.");
		}
	},	
    collapseAll : function() {
    	this.tree.getRoot().visit(function(node){node.expand(false);});
	},
	expandAll : function() {
		this.tree.getRoot().visit(function(node){node.expand(true);});
	},
    removeAll : function() {
    	this.tree.enableUpdate(false);
    	this.tree.getRoot().removeChildren();
    	this.tree.enableUpdate(true);
    },
    sortAll : function() {
    	this.tree.enableUpdate(false);
    	this.tree.getRoot().sortChildren(this.cmp, true);
    	this.tree.enableUpdate(true);
    },
    
    powerSearch : function(pattern) {
    	// perform a search that removes any non-matches from the dynatree
    	// any matching prop is added
    	// any matching class is added with all its props
    	
    	this.tree.enableUpdate(false);
    	this.removeAll();
    	
    	var names = (this.subsetURIs !== null) ? this.subsetURIs : this.oInfo.getClassNames();
		
		for (var i=0; i < names.length; i++) {
			var c = this.oInfo.getClass(names[i]);
			var plist;
			
			// get all properties if class matches, otherwise only matching properties
			if (c.powerMatch(pattern)) {
				plist = this.oInfo.getInheritedProperties(c);
			} else {
				plist = [];
				var props = this.oInfo.getInheritedProperties(c);
				
				for (var p=0; p < props.length; p++) {
					if (props[p].powerMatch(pattern)) {
						plist.push(props[p]);
					}
				}
			}
			
			// add to the tree
			for (var j=0; j < plist.length; j++) {
				this.addOntClass(c, [plist[j]]);
				this.expandAll();
			}
		}
		this.tree.enableUpdate(true);
		
		this.sortAll();
		// do the traditional "find" to get the high-lighting
		this.find(pattern);
		

    }, 
    
    cmp : function(a,b) {
    	if (a.hasChildren() && !b.hasChildren())
    		return 1;
    	else if (b.hasChildren() && !a.hasChildren())
    		return -1;
    	else if (a.data.value < b.data.value) 
    		return -1;
    	else if (a.data.value > b.data.value)
    		return 1;
    	else
    		return 0;
    },
    
};