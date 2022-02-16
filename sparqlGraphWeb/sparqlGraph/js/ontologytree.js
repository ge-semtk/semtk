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

    // ----- options for all dynatrees (sparqlGraph, sparqlForm, ExploreTab) ------

    // don't allow closing of non-folders (sub-properties)
    this.tree.options.onQueryExpand = function(x, node) {
          return node.data.isFolder;
    };
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
            var tooltipUri=node.data.tooltip.split("\n")[0];
			if (node.bExpanded) {
                var i = renameFrom.indexOf(tooltipUri);
                var val = (i > -1) ? renameTo[i] : tooltipUri;
                elist.push(val);
			}
            if (node.bSelected) {
                var i = renameFrom.indexOf(tooltipUri);
                var val = (i > -1) ? renameTo[i] : tooltipUri;
                slist.push(val);
			}
		}.bind(this,expandList,selectList));

        // reset oInfo
        this.setOInfo(newOInfo);
        this.showAll();

        // restore expanded and selected nodes
        this.tree.getRoot().visit(function(elist, slist, node){
            var tooltipUri=node.data.tooltip.split("\n")[0];
            node.expand(elist.indexOf(tooltipUri) > -1);
            node.select(slist.indexOf(tooltipUri) > -1);
		}.bind(this,expandList,selectList));
    },

    activateByValue : function (val) {
        this.tree.getRoot().visit(function(v, node) {
            if (node.data.value == v) {
                node.activate();
            }
        }.bind(this, val))
    },

    activateByPropertyPair : function (pair) {
        var domainURI = pair[0];
        var propURI = pair[1];
        this.tree.getRoot().visit(function(dURI, pURI, node) {
            if (node.data.value == pURI) {
                if (node.getParent() && node.getParent().data.value == dURI) {
                    node.activate();
                }
            }
        }.bind(this, domainURI, propURI));
    },

    // Property pair will be:
    // namespace: null
    // node:  [uri]
    // prop:  [class uri, prop uri]
    getPropertyPair : function (node) {
        if (this.nodeIsClass(node)) {
            return [node.data.value];
        } else if (this.nodeIsProperty(node)) {
            var parentClassNode = node;
            do {
                parentClassNode = parentClassNode.getParent();
            } while (! this.nodeIsClass(parentClassNode));

            return [parentClassNode.data.value, node.data.value];
        } else {
            return null;
        }
    },

    getPropertyPairsFamily : function (node) {
        var ret = [];
        var pair = this.getPropertyPair(node);
        if (pair) {
            ret.push(pair);
        }

        node.visit(function(ret, child){
			ret.push(this.getPropertyPair(child));
		}.bind(this, ret));

        return ret;
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


		this.tree.getRoot().sortChildren(this.cmp.bind(this), true);
    	this.tree.enableUpdate(true);
	},

	showSubset : function(names) {
		// add a list of classes to the tree
		this.subsetURIs = names;
		this.tree.enableUpdate(false);
		for (var i=0; i < names.length; i++) {
			this.addOntClass(this.oInfo.getClass(names[i]));
		}

		this.tree.getRoot().sortChildren(this.cmp.bind(this), true);
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
    	var parentNodes = [];
    	var newNode;
    	var list = [];
        var retNodes = [];

    	// do nothing if ontClass's namespace is in the collapseList
    	if (this.collapseList.indexOf(ontClass.getNamespaceStr()) > -1) {
    		return;
    	}

    	// get class parents
    	var ontParents = this.getUncollapsedParents(ontClass);

        // does ontClass have any parents in its namespace
        var parentInNamespaceFlag = false;
        for (var i=0; i < ontParents.length && !parentInNamespaceFlag; i++) {
            if (ontParents[i].getNamespaceStr() == ontClass.getNamespaceStr()) {
                parentInNamespaceFlag = true;
            }
        }

        // add namespace as parentNode if it has not parent in it's own namespace
        if (! parentInNamespaceFlag) {
            parentName = ontClass.getNamespaceStr();
    		parentNameLocal = parentName.substring(parentName.lastIndexOf('/')+1);

            var nsNodes = this.getNodesByURI(ontClass.getNamespaceStr());
            if (nsNodes.length == 0) {
                // create non-existent namespace parent
                parentNodes.push(this.addNamespace(parentName, parentNameLocal));
            }
            else {
                // add existing namespace parent
                parentNodes = parentNodes.concat(nsNodes);
            }
        }

        // for each parent class, collect all nodes into ParentNodes
        for (var i=0; i < ontParents.length; i++) {

    		parentName =      ontParents[i].getNameStr();
    		parentNameLocal = ontParents[i].getNameStr(true);

        	// Check if parent is in the tree
        	var parentNodeList = this.getNodesByURI(parentName);
        	if (parentNodeList.length == 0) {

                var parentClass = this.oInfo.getClass(parentName);
            	if (parentClass) {
            		// if only adding specific property (ies)
            		if (typeof optOntPropList !== 'undefined') {
            			// add just the parent class with no properties
            			parentNodes = parentNodes.concat(this.addOntClass(parentClass, []));
            		} else {
            			// normally: add the entire parent class
            			parentNodes = parentNodes.concat(this.addOntClass(parentClass));
            		}

            	// break the recursion when we get to the namespace node.  This one is not a class.
            	} else {
            		parentNodes.push(this.addNamespace(parentName, parentNameLocal));
            	}
        	} else {
                parentNodes = parentNodes.concat(parentNodeList);
            }
        }

        // find or create a new node under each parentNode
        for (var i=0; i < parentNodes.length; i++) {
            list = this.getNodesByURIAndParent(className, parentNodes[i].data.value);
            if (list.length == 0) {
        		newNode = null;
        	} else if (list.length > 1) {
        		throw "Internal error in OntologyTree.addOntClass(): found class more than once under same parent: " + className + " under " + parentNodes[i].data.value;
        	} else {
        		newNode = list[0];
        	}

            // add node if needed
            if (newNode == null) {
    	        // add the node
            	var title = classNameLocal;
            	var uris = OntologyRange.parseDisplayString(className);
            	var specialFlag = false;
            	for (var u of uris) {
					if (this.classIsSpecial(u)) {
						specialFlag = true;
						break;
					}
				}
            	if (specialFlag) {
            		title = this.HTMLOpenHighlight + title + this.specialClassHTML1;
            	}

                // due to update() function, tooltip.split('\n')[0] must equal full URI
                var tipText = [className].concat(ontClass.getAnnotationLabels()).concat(ontClass.getAnnotationComments()).join('\n');

    	        newNode = parentNodes[i].addChild({
    	    		title: title,
    	    		//key: classKey,
    	    		tooltip: tipText,
    	    		isFolder: true,
    	    	});
    	        newNode.data.value = className;
    		}

            // Add props
            // Check if we're adding a specific optOntProp, or defaulting to add all of them
            var props = null;
            if (typeof(optOntPropList) !== 'undefined') {
            	props = optOntPropList;
            } else {
            	props = this.oInfo.getInheritedProperties(ontClass);
            }

            this.addNodeProps(newNode, ontClass, props);
            retNodes.push(newNode);
        }

        return retNodes;
	},

    getUncollapsedParents : function(oClass) {
        // get class parents
    	var ontParentList = this.oInfo.getClassParents(oClass);
        var ret = [];

        // loop through all parents
        for (var i=0; i < ontParentList.length; i++) {
            var parent = ontParentList[i];
            var parentFullName = parent.getNameStr();
            var parentNamespace = parent.getNamespaceStr();

            // if parent is not already in ret
            if (ret.filter(function(oClass) {return oClass.getNameStr() == parentFullName; }).length == 0) {

                // if it's in the collapseList, then uncollapse
                if (this.collapseList.indexOf(parentNamespace) > -1) {
                    ret = ret.concat(this.getUncollapsedParents(parent));
                } else {
                    ret.push(parent);
                }
            }
        }

    	return ret;
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

        // build topLevelProps:  all propList which are NOT subprops of other props in propList
        //       subProps:       all propList which ARE subprops of other props in propList
        var topLevelProps = propList.slice();
        var otherLevelProps = [];
        for (var p of propList) {
            
            var subProps = this.oInfo.getSubProperties(p);
            for (var sub of subProps) {
                var i = topLevelProps.indexOf(sub);
                
                if (i > -1) {
                    // found that sub is a subprop of p
                    // remove s from topLevelProps
                    // add s to otherLevelProps
                    topLevelProps.splice(i, 1);
                    otherLevelProps.push(sub);
                }
            }
        }

        var title = "";
        for (var prop of topLevelProps) {
        	var propName = prop.getNameStr();

        	// add property if it don't exist yet under this class
        	var nodeList = this.getNodesByURI(propName);
        	var propNode = null;
        	for (var j=0; j < nodeList.length; j++) {
        		// found existing node if both the uri and the parent are the same
        		if (nodeList[j].getParent() == node) {
        			propNode = nodeList[j];
        		}
        	}

            // if prop is not already added, then add it
        	if (propNode == null) {
                var newNode = this.addPropToNode(node, prop, ontClass);
                // otherLevelProps are sub to SOME property, maybe this one, maybe not
                this.recursivelyAddSubpropsIn(newNode, prop, ontClass, otherLevelProps);
        	}
        }

	},

    recursivelyAddSubpropsIn : function(node, prop, ontClass, possibleSubProps) {
        // Recursively add any members of possibleSubProps that are actually subProps of prop
        //
        // node - a property node to add subProps
        // prop - the prop shown in node
        // ontClass - the parent Class
        // possibleSubProps - list of possible subProps

        var subProps = this.oInfo.getSubProperties(prop);
        for (var sub of possibleSubProps) {
            if (subProps.indexOf(sub) > -1) {
                // sub is a valid subProp
                var newNode = this.addPropToNode(node, sub, ontClass);
                this.recursivelyAddSubpropsIn(newNode, sub, ontClass, possibleSubProps);
            }
        }
    },

    addPropToNode : function(node, ontProp, ontClass) {
        // format local properties different than inherited
        var inheritedFlag = (ontClass.getProperties().indexOf(ontProp) == -1)
       
        var oRange = ontProp.getRange(ontClass, this.oInfo);
        if (inheritedFlag) {
            title = ontProp.getNameStr(true) + ": " + oRange.getDisplayString(true);
        } else {
            title = "<i>" + ontProp.getNameStr(true) + ": " + oRange.getDisplayString(true) + "</i>";
        }

        // highlight if range is a special class
        if (this.classIsSpecial(oRange.getDisplayString(false))) {
            title = this.HTMLOpenHighlight + title + this.specialClassHTML1;
        }

        // add
        var propName = ontProp.getNameStr();
        // due to update() function, tooltip.split('\n')[0] must equal full URI
        var tipText = [propName].concat(ontProp.getAnnotationLabels()).concat(ontProp.getAnnotationComments()).join('\n');
        var child = node.addChild({
            title: title,
            //key: propName,
            tooltip: tipText,
            isFolder: false,
            expand: true
        });
        child.data.value = propName;
        return child;
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

    selectAll : function(flag) {
        this.tree.getRoot().visit(function(node){
            node.select(flag);
        });
    },

    setAllSelectable : function(flag) {
        this.tree.getRoot().visit(function(node){
            node.data.unselectable = !flag;
        });
    },

    selectNodesByURI : function(uri, flag) {
        this.tree.getRoot().visit(function(node){
			if (node.data.value == uri) {
				node.select(flag);
			}
		});
    },

    selectIdenticalNodes : function (node, flag) {
        var nodeURI = node.data.value;
        var parentURI = node.getParent() ? node.getParent().data.value : "";

        this.tree.getRoot().visit(function(n){
			if (n != node && n.data.value == nodeURI) {
                if (this.nodeIsClass(n)) {
                    // classes match automatically
				    n.select(flag);
                } else if (this.nodeIsProperty(n) && n.getParent().data.value == parentURI) {
                    // properties must share parent
                    n.select(flag);
                }
			}
		}.bind(this));
    },

    getSelectedClassNames : function() {
        var selected = this.tree.getSelectedNodes();
        var ret = [];
        // add if selected and not found already
        for (var node of selected) {
            if (this.nodeIsClass(node)) {
                var name = this.nodeGetURI(node);
                if (ret.indexOf(name) == -1) {
                    ret.push(name);
                }
            }
        }
		return ret;
    },

    getSelectedPropertyNames : function() {
        var selected = this.tree.getSelectedNodes();
        var ret = [];
        // add if selected and not found already
        for (var node of selected) {
            if (this.nodeIsProperty(node)) {
                var name = this.nodeGetURI(node);
                if (ret.indexOf(name) == -1) {
                    ret.push(name);
                }
            }
        }
		return ret;
    },

    // list of [[domainURI, propURI],  ]
    //
    getSelectedPropertyPairs : function() {
        var selected = this.tree.getSelectedNodes();
        var ret = [];
        var repeatHash = {};
        // add if selected and not found already
        for (var node of selected) {
            if (this.nodeIsProperty(node)) {
                var propURI = this.nodeGetURI(node);
                var domainURI = this.nodeGetURI(node.getParent());
                var hash = domainURI + ":" + propURI;
                if (! (hash in repeatHash)) {
                    ret.push([domainURI, propURI]);
                    repeatHash[hash] = 1;
                }
            }
        }
		return ret;
    },

    getNodesByURIAndParent: function(uri, parentUri) {
		// uri can be a class or property
		// find all tree nodes that match
		var ret = [];
		this.tree.getRoot().visit(function(node){
			if (node.data.value == uri && node.getParent().data.value == parentUri) {
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
        return (node.value.indexOf('#') == -1);
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

    // like find, but changes color instead of selecting
    // sorry about the in-code formatting
    search : function(pattern) {

        // collapse everything
		this.tree.enableUpdate(false);
    	this.expandAll();
    	this.collapseAll();
    	this.tree.enableUpdate(true);
    	// ---------------------------------------

        var lowPattern = pattern.toLowerCase();
        var tag0 = "<font color=green>";
        var tag1 = "</font>";

        mysearch=function(node) {
            // remove tags if any
            if (node.data.title.indexOf(tag0) == 0) {
                node.data.title = node.data.title.slice(tag0.length, -tag1.length);
                node.setTitle(node.data.title);
            }
            // if match
            if (pattern.length > 0 && node.data.title.toLowerCase().indexOf(lowPattern) > -1) {
                // add tags
                node.data.title = tag0 + node.data.title + tag1;
                node.setTitle(node.data.title);
                // expand
                for (p = node.getParent(); p != null; p = p.getParent()) {
                    p.expand();
                }
            }
        }

		this.tree.getRoot().visit(mysearch);

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
    	this.tree.getRoot().sortChildren(this.cmp.bind(this), true);
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
        var aIsClass = this.oInfo.containsClass(a.data.value);
        var bIsClass = this.oInfo.containsClass(b.data.value);
    	if (aIsClass && !bIsClass)
    		return 1;
    	else if (bIsClass && !aIsClass)
    		return -1;
    	else if (a.data.value < b.data.value)
    		return -1;
    	else if (a.data.value > b.data.value)
    		return 1;
    	else
    		return 0;
    },

};
