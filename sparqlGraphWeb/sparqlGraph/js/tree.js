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

         	'jquery',
         	
			// shimmed
         	'sparqlgraph/dynatree-1.2.5/jquery.dynatree',
		],
	   function($) {
		/*
		 * Tree
		 * A tree that will be extended (e.g. for the control pane of various explore modes)
		 */
		var Tree = function(dynaTree) {
		    this.tree = dynaTree;
		};
		
		Tree.prototype = {

			/**
			 * Remove all tree nodes
			 */
			clear : function(){
				this.tree.getRoot().removeChildren();
			},

			/**
			 * Determine if the tree is empty or not
			 */
			isEmpty : function(){
				return !this.tree.getRoot().hasChildren();
			},

			/**
			 * Compare titles (for sorting)
			 */
			compareTitle : function (a, b) {
				a = a.data.title.toLowerCase();
				b = b.data.title.toLowerCase();
				return a > b ? 1 : a < b ? -1 : 0; 
			},
			
			/**
			 * Compare num leaf nodes (for sorting)
			 */
			compareLeafCount : function (a, b) {
				a = this.getNumLeaves(a);
				b = this.getNumLeaves(b);
				return a > b ? -1 : a < b ? 1 : 0; // descending order 
			},

			/**
			 * Add a node to the tree
			 * 
			 * title - node label
			 * tooltip - node tooltip
			 * isFolder - true if this is not a leaf node
			 * optParentNode - adds new node as child node to this node if provided, else uses root as parent
			 */
			addNode : function(title, tooltip, isFolder, optParentNode) {
		
				// format node title
				title = title.replaceAll("<","&lt").replaceAll(">","&gt");
		
				// if parent node not provided, use root
				// generate new id for this node 
				var parentNode;
				var id;
				if(typeof optParentNode === "undefined"){
					parentNode = this.tree.getRoot();
					id = title;
				}else{
					parentNode = optParentNode;
					id = parentNode.data.id + title;
				}
				// if node already exists then return it without adding
				if(this.getNode(id) != undefined){
					return this.getNode(id);
				}
						
				// add the node
				parentNode.addChild({
					title: title,
					id: id,
					tooltip: tooltip,
					isFolder: isFolder,
					hideCheckbox: isFolder,  // only want checkboxes on leaf nodes
					icon: false
				});
				
				return this.getNode(id);
			},	

			/**
			 * Set a node attribute
			 */
			setNodeAttribute: function(node, key, value) {
				node.data[key] = value;
			},

			/**
			 * Get a node by its unique id
			 */
			getNode : function(id) {
				var ret = undefined;
				this.tree.getRoot().visit(function(node){
					if (node.data.id === id) {
						ret = node;
					}
				});
				return ret;
			},

			/**
			 * Count the number of leaf nodes under a given node
			 */
			getNumLeaves : function(node){
				if(node.hasChildren() === false){ // use triple-equals per dynatree documentation
					return 1; // it's a leaf
				}else{
					var count = 0;
					for(const n of node.getChildren()){
						count += this.getNumLeaves(n);
					}
					return count;
				}
			},

			/**
			 * To each child of the root node, append leaf count "(X items)"
		     */
			addLeafCountsToRootChildren: function() {
				if(this.tree.getRoot().hasChildren()){
					for(const node of this.tree.getRoot().getChildren()){
						node.setTitle(node.data.title + " (" + this.getNumLeaves(node) + " items)");  // sets attribute and also redraws
					}
				}
			},
		}
		return Tree;
	}
		
);