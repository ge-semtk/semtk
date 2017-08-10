/**
 ** Copyright 2017 General Electric Company
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
 *  EditTab - the GUI elements for building an ImportSpec
 *
 *  Basic programmer notes:
 *     ondragover
 *         - preventDefault will allow a drop
 *         - stopPropagation will stop the question from being bubbled up to a parent. 
 */

define([	// properly require.config'ed
         	'sparqlgraph/js/iidxhelper',
            'sparqlgraph/js/modaliidx',
            'sparqlgraph/js/ontologyinfo',
            
         	'jquery',
         	
			// shimmed
         	'sparqlgraph/dynatree-1.2.5/jquery.dynatree',
            'sparqlgraph/js/ontologytree',
         	
		],

	function(IIDXHelper, ModalIidx, OntologyInfo, $) {
		
		
		//============ local object  EditTab =============
		var EditTab = function(treediv, canvasdiv, searchtxt) {
		    this.treediv = treediv;
            this.canvasdiv = canvasdiv;
            this.searchtxt = searchtxt;
            this.oInfo = null;
            this.oTree = null;
            
            var form = document.createElement("form");
            form.classList.add("form-horizontal");
            form.style.marginTop = "1ch";
            form.style.marginLeft = "1ch";
            this.canvasdiv.appendChild(form);
            
            // this.legend
            var center = document.createElement("center");
            this.legend = document.createElement("legend");
            center.appendChild(this.legend);
            form.appendChild(center);
            
            // this.fieldset
            this.fieldset = document.createElement("fieldset");
            form.appendChild(this.fieldset);
            
            this.initDynaTree();
        }
		
		EditTab.CONSTANT = "constant";

		
		EditTab.prototype = {
            
            /*
             * Initialize an empty dynatree
             */
			initDynaTree : function() {
                
                var treeSelector = "#" + this.treediv.id;
                
                $(treeSelector).dynatree({
                    onActivate: function(node) {
                        // A DynaTreeNode object is passed to the activation handler
                        // Note: we also get this event, if persistence is on, and the page is reloaded.
                        this.selectedNodeCallback(node);

                    }.bind(this),
                    onDblClick: function(node) {
                        // A DynaTreeNode object is passed to the activation handler
                        // Note: we also get this event, if persistence is on, and the page is reloaded.
                        // console.log("You double-clicked " + node.data.title);
                    }.bind(this),

                    dnd: {
                        onDragStart: function(node) {
                        /** This function MUST be defined to enable dragging for the tree.
                         *  Return false to cancel dragging of node.
                         */
                           // console.log("dragging " + gOTree.nodeGetURI(node));
                           // gDragLabel = gOTree.nodeGetURI(node);
                            return true;
                        }.bind(this),

                        onDragStop: function(node, x, y, z, aa) {
                           // console.log("dragging " + gOTree.nodeGetURI(node) + " stopped.");
                           // gDragLabel = null;
                        }.bind(this)
                    },	

                    persist: true,
                });
                     
                this.oTree = new OntologyTree($(treeSelector).dynatree("getTree")); 
            },
            
            doSearch : function() {
                this.oTree.find(this.searchtxt.value);
            },
    
            doCollapse : function() {
                this.searchtxt.value="";
                this.oTree.collapseAll();
            },

            doExpand : function() {
                this.oTree.expandAll();
            },
            
            draw : function () {
                this.oTree.showAll();
            },
            
            setOInfo : function (oInfo) {
                this.oInfo = new OntologyInfo(oInfo.toJson());  // deepCopy
                this.oTree.setOInfo(this.oInfo);
            },
            
            selectedNodeCallback : function (node) {
                var name = node.data.value;
                if (name.indexOf("#") == -1) {
                    this.editNamespace(node);
                    
                } else if (this.oInfo.containsClass(name)) {
                    this.editClass(node);
                    
                } else if (this.oInfo.containsProperty(name)) {
                    this.editProperty(node);
                    
                } else {
                    throw new Error("Can't find selected node in oInfo.");
                }
            },
            
            editNamespace : function (node) {
                var name = node.data.value;
                this.legend.innerHTML = "Namespace<br>" + name;
                this.fieldset.innerHTML = "";
            },
            
            editClass : function (node) {
                var oClass = this.oInfo.getClass(node.data.value);
                var namespace = oClass.getNamespaceStr();
                var name = oClass.getNameStr(true);
                
                this.legend.innerHTML = "Class " + name;
                
                this.fieldset.innerHTML = "";
                IIDXHelper.fsAddTextInput(this.fieldset, "Namespace:", null, "input-xlarge", namespace, true);
                IIDXHelper.fsAddTextInput(this.fieldset, "Superclass:", null, "input-xlarge", oClass.getParentNameStr(), true);

                var subtitle = null;
                subtitle = document.createElement("legend");
                subtitle.innerHTML = "Inherited Properties";
                this.fieldset.appendChild(subtitle);
                
                subtitle = document.createElement("legend");
                subtitle.innerHTML = "Properties";
                this.fieldset.appendChild(subtitle);
                
                var propList = oClass.getProperties();
                for (var i=0; i < propList.length; i++) {
                    IIDXHelper.fsAddTextInput(this.fieldset, propList[i].getName().getFullName(), null, "input-xlarge", propList[i].getRange().getFullName(), true);

                }
                
                subtitle = document.createElement("legend");
                subtitle.innerHTML = "OneOf Restrictions";
                this.fieldset.appendChild(subtitle);
            },
            
            editProperty : function (node) {
                var name = node.data.value;
                this.legend.innerHTML = "Property<br>" + name;
                this.fieldset.innerHTML = "";
            },
            
        };
		
		return EditTab;            // return the constructor
	}
	
);
