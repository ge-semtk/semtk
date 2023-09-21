/**
 ** Copyright 2023 General Electric Company
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
define([	// properly require.config'ed
            'sparqlgraph/js/tree',
            'sparqlgraph/js/visjshelper',

         	'jquery',
         	
			// shimmed
         	'sparqlgraph/dynatree-1.2.5/jquery.dynatree',
		],
	   function(Tree, VisJsHelper, $) {
		/*
		 * RestrictionTree
		 * A tree of cardinality restriction violations that appears in Restriction Explore mode.
		 * 
		 * Note: in the codebase, "violations" includes both too-many and too-few instances.  In the UI, too-few are referred to as "incomplete data".
		 */
		var RestrictionTree = function(dynaTree) {
		    this.tree = dynaTree;
		    this.setExceedsOnlyMode(0);  // matches initial dropdown selection
		};
		
		// column indexes in the violations table that is used to populate the tree
		RestrictionTree.COLINDEX_CLASSNAME = 0;
		RestrictionTree.COLINDEX_PREDICATE = 1;
		RestrictionTree.COLINDEX_RESTRICTION = 2;
		RestrictionTree.COLINDEX_LIMIT = 3;
		RestrictionTree.COLINDEX_INSTANCECOUNT = 4;
		RestrictionTree.COLINDEX_ACTUALCOUNT = 5;
		RestrictionTree.COLINDEX_URILIST = 6;
		
		RestrictionTree.prototype = {
			
			/**
			 * Set/unset exceed-only mode
			 */
			setExceedsOnlyMode : function(flag){
				this.exceedsOnlyMode = flag;
			},
			
			/**
			 * Get exceed-only mode
			 */
			getExceedsOnlyMode : function(){
				return this.exceedsOnlyMode;
			},
			
			/**
			 * Set sort mode
			 * sortModeInt - an integer indicating how to sort (0 for title, 1 for count, 2 for percentage)
			 */
			setSortMode : function(sortModeInt){
				this.sortMode = sortModeInt;
			},	
			
			/**
			 * Sort the tree
			 */
			sort : function(){
				if(this.sortMode == 0){
					this.tree.getRoot().sortChildren(this.compareTitle, false); 
				}else if(this.sortMode == 1){
					this.tree.getRoot().sortChildren(this.compareClassViolationCount, false); 
				}else if(this.sortMode == 2){
					this.tree.getRoot().sortChildren(this.compareClassViolationPercentage, false); 
				}else{
					alert("Error: unrecognized sort mode");
				}
			},
			
			// compare function for sorting
			compareClassViolationCount : function (a, b) {
				a = a.data.classViolationCount;
				b = b.data.classViolationCount;
				return a > b ? -1 : a < b ? 1 : 0; 
			},
			
			// compare function for sorting
			compareClassViolationPercentage : function (a, b) {
				a = a.data.classViolationPercentage;
				b = b.data.classViolationPercentage;
				return a > b ? -1 : a < b ? 1 : 0; 
			},
		
			/**
			 * Build the restriction tree
			 * tableRes - results object containing the violation table
			 */
			draw: function(tableRes) {
		
				// get local classname (e.g. http://nature/Sky#Cloud => Sky#Cloud)
				var getLocalClassname = function(className){
					return className.includes("/") ? className.slice(className.lastIndexOf("/") + 1, className.length) : className;
				}
		
				// get human-readable restriction text (e.g. mincardinality => "at least")
				var getReadableRestriction = function(restriction) {
					return (restriction.includes("max")) ? "at most" : (restriction.includes("min") ? "at least" : "exactly");
				}
		
				// get the number of violations for a given class
				// TODO this is called multiple times for a given class - avoid this by creating a hash
				var getNumViolationsForClass = function(table, className, exceedsOnlyMode) {
					var count = 0;
					for (var i = 0; i < table.rows.length; i++) {
		
						const row = table.rows[i];
						const thisClassName = row[RestrictionTree.COLINDEX_CLASSNAME];
						const limit = row[RestrictionTree.COLINDEX_LIMIT];
						const actualCount = row[RestrictionTree.COLINDEX_ACTUALCOUNT];
		
						// skip this row if only want to show "exceeds maximum" violations and this isn't one
						if (exceedsOnlyMode && (actualCount <= limit)) {
							continue;
						}
						
						if (thisClassName == className) {
							count += JSON.parse(row[RestrictionTree.COLINDEX_URILIST]).length;  // add URI list length to count
						}
					}
					return count;
				}
		
				// get violations table
				if(typeof tableRes !== 'undefined'){
			    	this.violationTableRes = tableRes;  // store it for future reuse
			    }
			    if(typeof this.violationTableRes === 'undefined'){
					console.error("Error: no restriction tree table available");
					return;
				}
				var table = this.violationTableRes.getTable();
		
				this.clear();		// clear tree
				for (var i = 0; i < table.rows.length; i++) {
		
					const row = table.rows[i];
					const className = row[RestrictionTree.COLINDEX_CLASSNAME];
					const classInstanceCount = row[RestrictionTree.COLINDEX_INSTANCECOUNT];
					const classViolationCount = getNumViolationsForClass(table, className, this.getExceedsOnlyMode()); // may be calling this more often than needed - make hash if affecting performance
					const classViolationPercentage = Math.min(100, Math.round((classViolationCount / classInstanceCount) * 100));  // may be >100% if a single instance has multiple violations...cap at 100% 
					const limit = row[RestrictionTree.COLINDEX_LIMIT];
					const predicate = row[RestrictionTree.COLINDEX_PREDICATE];
					const predicateLocal = VisJsHelper.stripPrefix(predicate);
					const restrictionDescription = "restriction \"has " + getReadableRestriction(row[RestrictionTree.COLINDEX_RESTRICTION]) + " " + limit + " " + predicateLocal + "\"";
		
					const actualCount = row[RestrictionTree.COLINDEX_ACTUALCOUNT];
					const uriList = JSON.parse(row[RestrictionTree.COLINDEX_URILIST]);
					const violationDescription = uriList.length + " instances have " + actualCount + " " + predicateLocal + "(s)";
		
					// skip this row if only want to show "exceeds maximum" violations and this isn't one
					if (this.getExceedsOnlyMode() && (actualCount <= limit)) {
						continue;
					}
		
					let node1 = this.addNode(getLocalClassname(className) + " (" + classViolationCount + " items across " + classViolationPercentage.toFixed(0) + "% of instances)", className, true, undefined);	// tree node indicating class
					this.setNodeAttribute(node1, "classViolationCount", classViolationCount); 				// used for sorting
					this.setNodeAttribute(node1, "classViolationPercentage", classViolationPercentage);		// used for sorting
					let node2 = this.addNode(restrictionDescription, "", true, node1); 	// tree node describing restriction (e.g. "must have at least 2 codes")
					let node3 = this.addNode(violationDescription, "", true, node2);	// tree node describing violations (e.g. "5 violate with 0 codes")
					for (const uri of uriList) {				
						let node4 = this.addNode(uri, "", false, node3);		// tree node for URI
						this.setNodeAttribute(node4, "classUri", className);	// used for constructing
						this.setNodeAttribute(node4, "predicate", predicate);  	// used for constructing
					}
				}
				
				// if no tree entries, then add a "none" entry
				if(!this.tree.getRoot().hasChildren()){				
					this.tree.getRoot().addChild({
						title: "None",
						isFolder: false,
						hideCheckbox: true,  // don't need a checkbox
						icon: false
					});
				}
				
				// sort the tree
				this.sort();
			},
		}
		
		// RestrictionTree extends Tree
		Object.setPrototypeOf(RestrictionTree.prototype, Tree.prototype);
		
		return RestrictionTree;
	}
		
);