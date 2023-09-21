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
		 * ShaclTree
		 * A tree of SHACL validation results that appears in Shacl Validation Explore mode.
		 */
		var ShaclTree = function(dynaTree) {
		    this.tree = dynaTree;
		    this.setSeverityMode(ShaclTree.SEVERITYMODE_INFO);  // default to info, matches initial dropdown selection
		};
		
		// json keys
		ShaclTree.JSON_KEY_SOURCESHAPE = "sourceShape";
		ShaclTree.JSON_KEY_TARGETTYPE = "targetType";
		ShaclTree.JSON_KEY_TARGETOBJECT = "targetObject";
		ShaclTree.JSON_KEY_PATH = "path";
		ShaclTree.JSON_KEY_SEVERITY = "severity";
		ShaclTree.JSON_KEY_FOCUSNODE = "focusNode";
		ShaclTree.JSON_KEY_MESSAGE = "message";
		ShaclTree.JSON_KEY_MESSAGETRANSFORMED = "messageTransformed";
		
		// severity modes
		ShaclTree.SEVERITYMODE_VIOLATION = "Violation";
		ShaclTree.SEVERITYMODE_WARNING = "Warning";
		ShaclTree.SEVERITYMODE_INFO = "Info";
		ShaclTree.prototype = {
			
			/**
			 * Set severity mode
			 */
			setSeverityMode : function(severity){
				this.severityMode = severity;
			},
			
			/**
			 * Get severity mode
			 */
			getSeverityMode : function(){
				return this.severityMode;
			},

			/**
			 * Set sort mode
			 * sortModeInt - an integer indicating how to sort (0 for title, 1 for count)
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
					this.tree.getRoot().sortChildren(this.compareLeafCount.bind(this), false);
				}else{
					alert("Error: unrecognized sort mode");
				}
			},

			/**
			 * Compare severities
			 */
			compareSeverity : function (a, b) {
				
				// confirm that each is a valid severity level
				const severities = [ShaclTree.SEVERITYMODE_VIOLATION, ShaclTree.SEVERITYMODE_WARNING, ShaclTree.SEVERITYMODE_INFO]
				if(a == null || b == null || !severities.includes(a) || !severities.includes(b)){
					throw new Error("Cannot compare severity '" + a + "' to severity '" + b + "'");
				}
				
				if(a == b){
					return 0;
				}else if(a == ShaclTree.SEVERITYMODE_VIOLATION){
					return 1;		// a is Violation, b is Warning or Info
				}else if (a == ShaclTree.SEVERITYMODE_WARNING){
					if(b == ShaclTree.SEVERITYMODE_VIOLATION){
						return -1;	// a is Warning, b is Violation
					}else{
						return 1;	// a is Warning, b is Info
					}
				}else{
					return -1;		// a is Info, b is Warning or Violation
				}
			},

			/**
			 * Build the tree
			 * json - object containing an xhr element with the SHACL results
			 */
			draw: function(json) {

				// get results json
				if(typeof json !== 'undefined'){
					this.shaclJsonSaved = json.xhr;  // store it for future reuse
			    }
			    if(typeof this.shaclJsonSaved === 'undefined'){
					console.error("Error: no SHACL results available");
					return;
				}
		
				this.clear();		// clear tree
				
				for (var key in this.shaclJsonSaved.reportEntries) {
                    var entry = this.shaclJsonSaved.reportEntries[key];

					const sourceShape = entry[ShaclTree.JSON_KEY_SOURCESHAPE];	// e.g. http://DeliveryBasketExample#Shape_Fruit
					const targetType = entry[ShaclTree.JSON_KEY_TARGETTYPE];		// One of: targetNode, targetClass, targetSubjectsOf, targetObjectsOf
					const targetObject = entry[ShaclTree.JSON_KEY_TARGETOBJECT]; 	// If targetClass, expect a class URI.  If targetSubjectsOf/targetObjectsOf, expect a predicate URI.  If targetNode, expect a URI or literal
					const path = entry[ShaclTree.JSON_KEY_PATH];					// e.g. <http://DeliveryBasketExample#holds>, <http://DeliveryBasketExample#holds>/<http://DeliveryBasketExample#identifier>, more
					const message = entry[ShaclTree.JSON_KEY_MESSAGE];			// e.g. "maxCount[3]: Invalid cardinality: expected max 3: Got count = 4"  (this one is auto-generated, may be custom if provided)
					var messageTransformed = entry[ShaclTree.JSON_KEY_MESSAGETRANSFORMED];	// e.g. more user-friendly version of the original message (if empty use original)
					const severity = entry[ShaclTree.JSON_KEY_SEVERITY];			// e.g. "Info", "Warning", "Violation"
					const focusNode = entry[ShaclTree.JSON_KEY_FOCUSNODE];

					// screen entries for severity level
					if(this.compareSeverity(severity, this.getSeverityMode()) < 0){
						continue;
					}
					
					// level 0 node (source shape)
					let node0 = this.addNode("Shape: " + VisJsHelper.stripPrefix(sourceShape), "", true, undefined);
					
					// level 1 node (target type + target object)
					// use human-friendly text for target type (e.g. targetObjectsOf => Objects Of)
					let node1 = this.addNode("Target: " + targetType.substring(6).replace(/([A-Z])/g, ' $1') + " " +  VisJsHelper.stripPrefix(targetObject), targetType + " " + targetObject, true, node0);
					
					// level 2 node (path)
					let node2 = this.addNode("Path: " + path, path, true, node1);   
					
					// level 3 node (severity + message)
					if(messageTransformed == null || messageTransformed == ""){
						messageTransformed = message;  // if empty, use original message
					}
					let node3 = this.addNode(severity.toUpperCase() + ": " + messageTransformed, message, true, node2);
					
					// level 4 node (focus node)
					this.addNode(focusNode, "", false, node3);
				}
				
				// add leaf counts (e.g. "5 items") to top-level nodes
				this.addLeafCountsToRootChildren();
				
				// if no tree entries, then add a "none" entry
				if(!this.tree.getRoot().hasChildren()){				
					this.tree.getRoot().addChild({
						title: "None",
						isFolder: false,
						hideCheckbox: true,  // don't need a checkbox
						icon: false
					});
				}
				
				// sort		
				this.sort();
			},
		}
		
		// ShaclTree extends Tree
		Object.setPrototypeOf(ShaclTree.prototype, Tree.prototype);
		
		return ShaclTree;
	}
		
);