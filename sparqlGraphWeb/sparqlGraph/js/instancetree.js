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
 * InstanceTree
 */
var InstanceTree = function(dynaTree, optNoDuplicatesFlag) {
    this.tree = dynaTree;
    
    // no duplicates means name, val and parent's name,val can't be the same.
    // NOTE that this is a pretty sketchy definition of "duplicate"
    this.noDupFlag = (typeof optNoDuplicatesFlag === 'undefined') ? false : optNoDuplicatesFlag;
    this.itemHash = {};
};

InstanceTree.ROOT = "root";

InstanceTree.prototype = {
	
	addInstanceDataTable : function(ssiRes, parentTitleColName, parentValColName, childTitleColName, childValColName, optHideChildCheckbox, optHideRootChildCheckbox) {
		// Add a list of four-tuples to the tree:   parentTitle parentVal childTitle childVal
		// If parentTitleColName === ROOT then root is the parent of all nodes in the table.  Only childTitle and childVal need to be specified.
		//
		// optHideChildCheckbox hides checkboxes for all nodes that are below children of the root
		// optHideRootChildCheckbox hides checkboxes for all nodes that are children of the root
		
		// find the column numbers
		var colNames = ssiRes.getColumnNames();
		var parentTitleCol = colNames.indexOf(parentTitleColName);
		var parentValCol = colNames.indexOf(parentValColName);
		var childTitleCol = colNames.indexOf(childTitleColName);
		var childValCol = colNames.indexOf(childValColName);
		
		// validate column numbers
		if (parentTitleColName !== InstanceTree.ROOT && parentTitleCol == -1) {
			var msg = "Internal error in instancetree.addInstanceDataTable: can't find column " + parentTitleColName;
			alert(msg);
			throw msg;
		}
		if (parentTitleColName !== InstanceTree.ROOT && parentValCol == -1) {
			var msg = "Internal error in instancetree.addInstanceDataTable: can't find column " + parentValColName;
			alert(msg);
			throw msg;
		}
		if (childTitleCol == -1) {
			var msg = "Internal error in instancetree.addInstanceDataTable: can't find column " + childTitleColName;
			alert(msg);
			throw msg;
		}
		if (childValCol == -1) {
			var msg = "Internal error in instancetree.addInstanceDataTable: can't find column " + childValColName;
			alert(msg);
			throw msg;
		}

		for (var i=0; i < ssiRes.getRowCount(); i++) {
			if (parentTitleColName === InstanceTree.ROOT) {
				this.addInstanceData(	InstanceTree.ROOT,
										InstanceTree.ROOT,
										ssiRes.getRsData(i, childTitleCol),
										ssiRes.getRsData(i, childValCol), 
										optHideChildCheckbox,
										optHideRootChildCheckbox
									);
			} else {
				this.addInstanceData(	ssiRes.getRsData(i, parentTitleCol),
										ssiRes.getRsData(i, parentValCol),
										ssiRes.getRsData(i, childTitleCol),
										ssiRes.getRsData(i, childValCol), 
										optHideChildCheckbox,
										optHideRootChildCheckbox
									);
			}
		}
		
	},
	
	addInstanceData : function(parentTitle, parentVal, childTitle, childVal, optHideChildCheckbox, optHideRootChildCheckbox) {
		// add a single data point
		// presumes that Val's are unique, and Titles are not.
		//
		// if parentTitle is ROOT then add to root of tree, otherwise
		// if parent doesn't exist, add it to the top level of the tree
		// if parent appears more than once, add child to each one
		//
		// optHideChildCheckbox hides checkboxes for all nodes that are below children of the root
		// optHideRootChildCheckbox hides checkboxes for all nodes that are children of the root

    	var newNode = null;
    	var hashKey = null;
    	
		var hideChildCheckbox = (typeof optHideChildCheckbox === 'undefined') ? false : optHideChildCheckbox;
		var hideRootChildCheckbox = (typeof optHideRootChildCheckbox === 'undefined') ? false : optHideRootChildCheckbox;
		
    	// track duplicates if noDupFlag is set
    	if (this.noDupFlag) {
    		var hashKey = parentTitle + ':' + parentVal + ':' + childTitle + ':' + childVal;
    		if ((this.itemHash.hasOwnProperty(hashKey)) && (this.itemHash[hashKey] == 1)) {
    			return;
    		}
    	}

    	if (parentTitle === InstanceTree.ROOT) {
    		newNode = this.tree.getRoot().addChild({
	    		title: childTitle,     // title is not unique
	    		//key: classKey,       // key is auto-assigned
	    		//tooltip: classKey,
	    		isFolder: true,
				hideCheckbox: hideRootChildCheckbox,
	    	});
	    	newNode.data.value = childVal;
	    	
    	} else {
	    	this.tree.visit(function(node){
	    	    if(node.data.value === parentVal) {
	    	    	newNode = node.addChild({
	    	    		title: childTitle,     // title is not unique
	    	    		//key: classKey,       // key is auto-assigned
	    	    		//tooltip: classKey,
	    	    		isFolder: true,
						hideCheckbox: hideChildCheckbox,
	    	    	});
	    	    	newNode.data.value = childVal;
	    	        return true; // continue traversal 
	    	    }
	    	});
    	}
    	
    	// if add failed then parent wasn't found.
    	// add the parent and call again.
    	if (newNode == null) {
    		this.addInstanceData(InstanceTree.ROOT, InstanceTree.ROOT, parentTitle, parentVal, optHideChildCheckbox, optHideRootChildCheckbox);
    		this.addInstanceData(parentTitle, parentVal, childTitle, childVal, optHideChildCheckbox, optHideRootChildCheckbox);
    		
    	  // else the add succeeded, so set the hashKey if noDupFlag
    	} else if (this.noDupFlag) {
    		this.itemHash[hashKey] = 1;
    	}
        
	},
	
	getSelectedValues : function () {
		var n = this.tree.getSelectedNodes();
		var ret = [];
		for (var i=0; i < n.length; i++) {
			ret.push(n[i].data.value);
		}
		return ret;
	},
	
	getSelectedTitles : function () {
		var n = this.tree.getSelectedNodes();
		var ret = [];
		for (var i=0; i < n.length; i++) {
			ret.push(n[i].data.title);
		}
		return ret;
	},
	
	// get values of the root's children
	getRootChildValues : function () {
		var n = this.tree.getRoot().getChildren();
		var ret = [];
		if(!n){
			return ret;
		}
		for (var i=0; i < n.length; i++) {
			ret.push(n[i].data.value);
		}
		return ret;
	},
	
	getAncestorValues : function(val) {
		// get vals of all ancestors of node with val
		var ret = [];
		var root = this.tree.getRoot();
		root.data.title = "root";
		this.tree.getRoot().visit(function(node){
			// if node matches
			if (node.data.value == val) {
				node.visitParents(	function(node) {
						if (node.data.title !== "root"){
							ret.push(node.data.value);
						}
					},
					false   // don't include self in the visitParents
				);
				return false;  // stop after self is found
			} else {
				return true;
			}
		});
		return ret;
	},
	
	hasAncestorValue : function(val, ancestorVal) {
		// return true if the node with this val has an ancestor with this ancestorVal
		var ancestorVals = this.getAncestorValues(val);
		for (var i = 0; i < ancestorVals.length; i++) {
			if(ancestorVals[i] == ancestorVal){
				return true;
			}
		}
		return false;
	},
	
	getDescendentValues : function(val) {
		// get vals of all decendents of node with val
		var ret = [];
		this.tree.getRoot().visit(function(node){
			// if node matches
			if (node.data.value == val) {
				//alert("found self " + val);
				node.visit(	function(node) {
								ret.push(node.data.value);
							},
							false   // don't include self in the visit
						  );
				return false;  // stop after self is found
			} else {
				return true;
			}
		});
		return ret;
	},
	
	getNodeByKey : function(keyName) {
		return this.tree.getNodeByKey(keyName);
	},
	
	getNodeByVal : function(val) {
		// return first node matching val
		var ret = null;
		this.tree.getRoot().visit(function(node){
			if (node.data.value == val) {
				ret = node;
				return false;
			}
		});
		return ret;
	},
	
	nodeGetKeyname : function(node) {
		return node.data.key;
	},
	

	
	hideAll : function(val) { 
		var val = (typeof val === 'undefined') ? true : val;
		
		// collapse, de-select, unhide all nodes
		this.tree.getRoot().visit(function(node){
				if (node.li) {
					node.li.hidden=val;
				}
			});
	},
	
	selectAll : function(val) { 
		var val = (typeof val === 'undefined') ? true : val;

		// collapse, de-select, unhide all nodes
		this.tree.getRoot().visit(function(node){
				node.select(val);
			});
	},

	deselectAll : function() {
		this.tree.getRoot().find("#Not_Find_ABLE#");
        },

	powerFind : function(pattern) {
		// hides all non-matching nodes
		
		// special case: blank pattern means unhide all
		if (pattern === "") {
			this.hideAll(false);
			return;
		}
		
		this.tree.enableUpdate(false);

		// deselect and unhide all
		this.hideAll(true);
		
		var lowPat = pattern.toLowerCase();

		// visit all nodes
		this.tree.getRoot().visit(function(node){
				// if node matches
			if (node.data.title.toLowerCase().indexOf(lowPat) > -1) {
				// visit self and parents. unhide.
				node.visitParents(	function(node) {
										if (node.li) {
											node.li.hidden = false;
										}
									},
									true   // include self in the visitParents
								 );
				
			}
			});
		this.expandAll();
    	this.tree.enableUpdate(true);
	},

	find : function(pattern) {
		// opens and selects all matches.  Hides all non-matches
		this.tree.getRoot().find(pattern);
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
        this.itemHash = {};
    	this.tree.enableUpdate(true);
    },
    sortAll : function() {
    	this.tree.enableUpdate(false);
    	this.tree.getRoot().sortChildren(this.cmp, true);
    	this.tree.enableUpdate(true);
    },
    remove : function(node) {
      var parentTitle = InstanceTree.ROOT;
      var parentVal = InstanceTree.ROOT;
      var childTitle = node.data.title;
      var childVal = node.data.value;

      var parent = node.getParent();
      if (parent != null) {
        parentTitle = parent.data.title;
        if (!parentTitle)
          parentTitle = InstanceTree.ROOT;
        parentVal = parent.data.value;
        if (!parentVal)
          parentVal = InstanceTree.ROOT;
      }

      var hashKey = parentTitle + ':' + parentVal + ':' + childTitle + ':' + childVal;
      node.remove();
      this.itemHash[hashKey] = 0;
    },
	// move the (first) node with the given value to the given tree
	moveNodeToTree : function(toTree, val){
		this.moveOrCopyNodeToTree(toTree, val, true);  // true to move
	},
	// copy the (first) node with the given value to the given tree
	copyNodeToTree : function(toTree, val){
		this.moveOrCopyNodeToTree(toTree, val, false);  // false to copy
	},
	// move or copy the (first) node with the given value to the given tree
	// move (boolean): true to move, false to copy
	moveOrCopyNodeToTree : function(toTree, val, move){
		var node = this.getNodeByVal(val);
		if (node != null) {
			var toTreeRoot = toTree.tree.getRoot();
			var nodeCopy = node.toDict(true, null);
			toTreeRoot.addChild(nodeCopy);
			if(move){
				this.remove(node);  // if move (vs copy), then remove from this tree
			}
			toTree.sortAll ();
		}
	},
	// move a node from this tree to another tree
	moveNodesToTree : function (toTree, selectedOnly) { // selectedOnly (boolean) = only move selected elements
		if (selectedOnly)
			var nodes = this.tree.getSelectedNodes(true);
		else
			var nodes = this.tree.getRoot().getChildren();

		if (nodes != null) {
			var toTreeRoot = toTree.tree.getRoot ();
			for (var i = nodes.length - 1; i >= 0; i--) {
				var nodeCopy = nodes[i].toDict (true, null);
				toTreeRoot.addChild (nodeCopy);
//				nodes[i].remove();
                                this.remove(nodes[i]);
			}
			toTree.sortAll ();
		}
	},    
    cmp : function(a,b) {
    	if (a.data.title < b.data.title) 
    		return -1;
    	else if (a.data.title > b.data.title)
    		return 1;
    	else
    		return 0;
    },
    
};
