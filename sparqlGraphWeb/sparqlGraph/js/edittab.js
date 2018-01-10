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
            'sparqlgraph/js/belmont'
         	
		],

	function(IIDXHelper, ModalIidx, OntologyInfo, $) {
		
		
		//============ local object  EditTab =============
		var EditTab = function(treediv, canvasdiv, buttondiv, searchtxt) {
		    this.treediv = treediv;
            
            this.canvasdiv = document.createElement("div");
            this.canvasdiv.style.margin="1ch";
            canvasdiv.appendChild(this.canvasdiv);
            
            this.buttondiv = buttondiv;
            this.buttonspan = document.createElement("span");
            this.buttonspan.style.marginRight = "3ch";
            this.searchtxt = searchtxt;
            this.oInfo = null;
            this.oTree = null;
            
            this.initDynaTree();
            this.initButtonDiv();
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
            
            initButtonDiv : function() {
                
                this.buttondiv.innerHTML = "";
                var table = document.createElement("table");
                this.buttondiv.appendChild(table);
                table.width = "100%";
                
                // match first column's width to treediv
                var colgroup = document.createElement("colgroup");
                table.appendChild(colgroup);
                
                var col = document.createElement("col");
                colgroup.appendChild(col);
                col.width = this.treediv.offsetWidth;
                
                var tbody = document.createElement("tbody");
                table.appendChild(tbody);
                
                var tr = document.createElement("tr");
                tbody.appendChild(tr);
                
                // cell 1/3
                var td1 = document.createElement("td");
                tr.appendChild(td1);
                td1.appendChild(IIDXHelper.createButton("New namespace", this.createNamespace.bind(this)));
                
                // cell 2/3
                var td2 = document.createElement("td");
                tr.appendChild(td2);
                td2.appendChild(this.buttonspan);
                
                // cell 3/3
                var td3 = document.createElement("td");
                tr.appendChild(td3);
                
                var div3 = document.createElement("div");
                td3.appendChild(div3);
                td3.align="right";
                td3.appendChild(IIDXHelper.createBoldText("Download: "));
                
                var but1 = IIDXHelper.createButton("Owl/Rdf", this.butDownloadOwl.bind(this));
                td3.appendChild(but1);
                var but2 = IIDXHelper.createButton("SADL", this.butDownloadSadl.bind(this));
                td3.appendChild(IIDXHelper.createNbspText());
                td3.appendChild(but2);
                var but3 = IIDXHelper.createButton("JSON", this.butDownloadJson.bind(this));
                td3.appendChild(IIDXHelper.createNbspText());
                td3.appendChild(but3);
            },
            
            butDownloadOwl : function() {
                alert("Not Implemented");
            },
            
            butDownloadSadl : function() {
                alert("Not Implemented");
            },
            
            butDownloadJson : function() {
                IIDXHelper.downloadFile(JSON.stringify(this.oInfo.toJson(), null, 2), "oinfo.json", "application/json")
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
                
                if (node == null) {
                    this.canvasdiv.innerHTML = "";
                    this.buttonspan.innerHTML = "";
                    
                } else {
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
                }
            },
            
            createNamespace : function () {
                var fullName;
                var count = 1;
                
                // generate a first shot a the new namespace name
                if (this.oInfo.getNumClasses() > 0) {
                    var randomClass = this.oInfo.getClass(this.oInfo.getClassNames()[0]);
                    fullName = randomClass.getNamespaceStr();
                    fullName = fullName.substring(0, fullName.lastIndexOf("/")+1) + "new";
                } else {
                    fullName = "new/new";
                }
                
                // change namespace name if it already exists
                while (this.oTree.getNodesByURI(fullName).length > 0) {
                    count += 1;
                    if (count > 2) {
                        fullName = fullName.substring(0, fullName.lastIndexOf("_"));
                    }
                    fullName = fullName + "_" + count;
                }
                
                // now make up a new classname
                // oInfo can't hold a namespace with no classes in it
                var className = fullName + "#NewClass";
                var newClass = new OntologyClass(className, "");
                this.oInfo.addClass(newClass);
                this.oTree.update(this.oInfo);
                
                // activate and expand the new node
                var nameNode = this.oTree.getNodesByURI(fullName)[0];
                nameNode.activate(true);
                nameNode.expand(true);
            },
            
            editNamespace : function (node) {
                var name = node.data.value;
            
                var nameForm = IIDXHelper.buildHorizontalForm(true);
                this.canvasdiv.innerHTML = "";
                this.canvasdiv.appendChild(nameForm);
                
                nameForm.appendChild(document.createTextNode("Namespace: "));
                var nameInput = IIDXHelper.createTextInput("et_nameInput");
                nameInput.value = name;
                nameInput.onchange = this.onchangeNamespace.bind(this, node, nameInput);
                nameForm.appendChild(nameInput);
                
                this.setNamespaceButtons(name);
            },
            
            setClassButtons : function (oClass) {
                
                var deleteCallback = function (cl) {
                    this.oInfo.deleteClass(cl);
                    this.selectedNodeCallback(null);
                    this.oTree.update(this.oInfo);
                }.bind(this, oClass);
                
                this.buttonspan.innerHTML = "";
                this.buttonspan.appendChild(IIDXHelper.createBoldText("Class: "));
                this.buttonspan.appendChild(IIDXHelper.createNbspText());
                this.buttonspan.appendChild(IIDXHelper.createButton("Delete", deleteCallback));
            },
            
            setNamespaceButtons : function (name) {
                
                var deleteCallback = function (ns) {
                    this.oInfo.deleteNamespace(ns);
                    this.selectedNodeCallback(null);
                    this.oTree.update(this.oInfo);
                }.bind(this, name);
                
                var addClassCallback = function(ns) {
                    var className = "NewClass";
                    
                    // make sure name is unique
                    var existingNames = this.oInfo.getClassNames();
                    var i=-1;
                    var fullName;
                    do {
                        fullName = ns + "#" + className + ((i>-1)?i.toString():"");
                        i += 1;
                    } while (existingNames.indexOf(fullName) > -1);
                    
                    // add new class
                    var newClass = new OntologyClass(fullName, "");
                    this.oInfo.addClass(newClass);
                    this.oTree.update(this.oInfo);
                    this.oTree.activateByValue(fullName);
                    
                }.bind(this, name);
                
                this.buttonspan.innerHTML = "";
                this.buttonspan.appendChild(IIDXHelper.createBoldText("Namespace: "));
                this.buttonspan.appendChild(IIDXHelper.createNbspText());
                this.buttonspan.appendChild(IIDXHelper.createButton("Delete", deleteCallback));
                this.buttonspan.appendChild(IIDXHelper.createNbspText());
                this.buttonspan.appendChild(IIDXHelper.createButton("New Class", addClassCallback));
            },
            
            onchangeNamespace : function (node, input) {
                var oldName = node.data.value;
                var newName = input.value;
                // PEC HERE: test this
                this.oInfo.renameNamespace(oldName, newName);
                this.oTree.update(this.oInfo, [oldName], [newName]);
            },
            
            /*
             * top-level function for editing a class
             */
            editClass : function (node) {
                var oClass = this.oInfo.getClass(node.data.value);
                var splitName = this.oInfo.splitName(oClass.getName());
                
                // ---- definition ----
                var defineDiv = document.createElement("div");
                var nameForm = IIDXHelper.buildHorizontalForm(true);
                defineDiv.appendChild(nameForm);
                nameForm.appendChild(document.createTextNode(splitName[0] + " # "));
                var nameInput = IIDXHelper.createTextInput("et_nameInput");
                nameInput.value = splitName[1];
                nameInput.onchange = this.onchangeClassName.bind(this, oClass, nameInput);
                nameForm.appendChild(nameInput);
                
                var superList = this.oInfo.getSuperclassNames(oClass.getNameStr());
                var h3 = document.createElement("h3");
                defineDiv.appendChild(h3);
                h3.innerHTML = "Superclasses:";
                var ul = document.createElement("ul");
                defineDiv.appendChild(ul);
                for (var i=0; i < superList.length; i++) {
                    var li = document.createElement("li");
                    li.innerHTML = superList[i];
                    ul.appendChild(li);
                }
                
                var annot = this.buildAnnotatorDom(oClass);
                defineDiv.appendChild(annot);
                
                
                // ---- properties ----
                var propsDiv = document.createElement("div");
                var propList = oClass.getProperties();
                for (var i=0; i < propList.length; i++) {
                    propsDiv.appendChild(this.buildPropertyEditDom(propList[i]));
                }
                
                // ---- inherited ----
                var inheritDiv = document.createElement("div");                
                var propList = this.oInfo.getInheritedProperties(oClass, true);
                for (var i=0; i < propList.length; i++) {
                    inheritDiv.appendChild(this.buildPropertyDisplayDom(propList[i]));
                }
                
                // ---- one of ----
                var enumDiv = document.createElement("div");
                var enumNames = this.oInfo.getClassEnumList(oClass);
                for (var i=0; i < enumNames.length; i++) {
                    enumDiv.appendChild(document.createTextNode(enumNames[i]));
                    enumDiv.appendChild(document.createElement("br"));
                }
                
                // ---- tabs ----
                var nameList = ["Definition", "Properties", "Inherited Props", "One of"];
                var divList = [defineDiv, propsDiv, inheritDiv, enumDiv];
                this.canvasdiv.innerHTML = "";
                this.canvasdiv.appendChild(IIDXHelper.buildTabs(nameList, divList));
                
                this.setClassButtons(oClass);
            },
            
            buildNamespaceSelect : function(selected) {
                var namespaces = this.oInfo.getNamespaceNames();
                // change namespaces into [local, full]
                for (var i=0; i < namespaces.length; i++) {
                    namespaces[i] = [OntologyInfo.localizeNamespace(namespaces[i]), namespaces[i]];
                }
                var sel = IIDXHelper.createSelect(null, namespaces, [selected], false);
                sel.style.width = "auto";
                return sel;
            },
            
             buildRangeSelect : function(oProp) {
                var rangeList = OntologyInfo.getSadlRangeList().concat(this.oInfo.getClassNames());
                 // change namespaces into [local, full]
                for (var i=0; i < rangeList.length; i++) {
                    rangeList[i] = [this.oInfo.getPrefixedName(new OntologyName(rangeList[i])), rangeList[i]];
                }
                var sel = IIDXHelper.createSelect(null, rangeList, [oProp.getRange().getName()]);
                sel.style.width = "auto";
                return sel;
            },
            
            buildPropertyEditDom : function (oProp) {
                var ret = document.createElement("span");
                var splitName = this.oInfo.splitName(oProp.getName());
                var splitRange = this.oInfo.splitName(oProp.getRange().getName());

                // domain namespace
                var nsSelect = this.buildNamespaceSelect(oProp.getName());
                ret.appendChild(nsSelect);
                // PEC HERE: callback on nsSelect
                
                ret.appendChild(document.createTextNode("#"));
                var rangeText = IIDXHelper.createTextInput(null);
                rangeText.value = splitName[1];
                ret.appendChild(rangeText);
                // PEC HERE: callback on rangeText
                
                var rangeSel = this.buildRangeSelect(oProp);
                ret.appendChild(rangeSel);
                // PEC HERE: callback on rangeSel
                
                ret.appendChild(document.createElement("br"));

                var propAnnot = this.buildAnnotatorDom(oProp);
                ret.appendChild(propAnnot);
                
                return ret;
            },
            
            buildPropertyDisplayDom : function (oProp) {
                var ret = document.createElement("span");
                var splitName = this.oInfo.splitName(oProp.getName());
                var splitRange = this.oInfo.splitName(oProp.getRange().getName());

                var pStr = splitName.join(":") + " " + splitRange.join(":");
                ret.appendChild(document.createTextNode(pStr));
                ret.appendChild(document.createElement("br"));

                var propAnnot = this.buildAnnotatorDom(oProp);
                ret.appendChild(propAnnot);
                
                return ret;
            },
            
            buildAnnotatorDom : function (oItem) {
                
                // create the dom and table
                var dom = document.createElement("div");
                var table = document.createElement("table");
                table.style.width="100%";
                dom.appendChild(table);
                var tr = document.createElement("tr");
                table.appendChild(tr);
                
                // top left cell is "labels"
                var td = document.createElement("td");
                td.style.verticalAlign = "top";
                td.style.width = "12ch";
                td.align="right";
                td.innerHTML = "labels: ";
                tr.appendChild(td);
                
                // top right will contain label inputs
                var labelsTd = document.createElement("td");
                var labelsId = IIDXHelper.getNextId("annotate");
                labelsTd.id = labelsId;
                tr.appendChild(labelsTd);
                var labelsForm = IIDXHelper.buildHorizontalForm(true);
                labelsTd.appendChild(labelsForm);
                
                tr = document.createElement("tr");
                table.appendChild(tr);
                
                // bottom left is "comments"
                td = document.createElement("td");
                td.style.verticalAlign = "top";
                td.align="right";
                td.innerHTML = "comments: ";
                tr.appendChild(td);
                
                // bottom right will contain comment inputs
                var commentsTd = document.createElement("td");
                var commentsId = IIDXHelper.getNextId("annotate");
                commentsTd.id = commentsId;
                tr.appendChild(commentsTd);
                
                // callback to add labels
                var addLabel = function(oItem, labelStr, td, before) {
                    var text = IIDXHelper.createTextInput(null, "input-medium");
                    text.value = labelStr;
                    text.onchange = this.onChangeLabel.bind(this, oItem, td);
                    td.insertBefore(text, before);
                    td.insertBefore(document.createTextNode(" "), before);
                }.bind(this);
                
                // add labels button
                var but1 = IIDXHelper.createIconButton("icon-plus");
                but1.onclick = addLabel.bind(this, oItem, "", labelsForm, but1);
                labelsForm.appendChild(but1);
                
                // fill in the labels
                var labels = oItem.getAnnotationLabels();
                for (var i=0; i < labels.length; i++) {
                    addLabel(oItem, labels[i], labelsForm, but1);
                }
                
                // callback to add comment
                var addComment = function(oItem, commentStr, td, before) {
                    var text = IIDXHelper.createTextArea(null, 2);
                    text.style.width="100%";
                    text.style.boxSizing = "border-box";
                    text.value = commentStr;
                    text.onchange = this.onChangeComment.bind(this, oItem, td);
                    td.insertBefore(text, before);
                    td.insertBefore(document.createTextNode(" "), before);
                }.bind(this);
                
                // add comments button
                var but2 = IIDXHelper.createIconButton("icon-plus");
                but2.onclick = addComment.bind(this, oItem, "", commentsTd, but2);
                commentsTd.appendChild(but2);
                
                // fill in the comments
                var comments = oItem.getAnnotationComments();
                for (var i=0; i < comments.length; i++) {
                    addComment(oItem, comments[i], commentsTd, but2);
                }
                
                var openFlag = (oItem.getAnnotationComments().length + oItem.getAnnotationLabels().length) > 0;
                return IIDXHelper.createCollapsibleDiv("Annotations", dom, openFlag);
            },
            
            /* 
             * clear oItem annotation labels 
             * and replace by all values in textParent child elements
             */
            onChangeLabel(oItem, textParent) {
                var c = textParent.childNodes;
                var strList = [];
                oItem.clearAnnotationLabels();
                for (var i=0; i < c.length; i++) {
                    if (c[i].value) {
                        oItem.addAnnotationLabel(c[i].value); // blanks etc are ignored
                    }
                }
            },
            
            /* 
             * clear oItem annotation labels 
             * and replace by all values in textParent child elements
             */
            onChangeComment(oItem, textParent) {
                var c = textParent.childNodes;
                var strList = [];
                oItem.clearAnnotationComments();
                for (var i=0; i < c.length; i++) {
                    if (c[i].value) {
                        oItem.addAnnotationComment(c[i].value); // blanks etc are ignored
                    }
                }
            },
            
            /**
              * change the class URI (not the namespace)
              */
            onchangeClassName : function(oClass, nameInput) {
                var newURI = oClass.getNamespaceStr() + "#" + nameInput.value;
                var oldURI = oClass.getNameStr();
                
                try {
                    var oc = this.oInfo.editClassName(oClass, newURI);
                    
                } catch (err) {
                    // show error and put text box back to old name
                    ModalIidx.alert("Bad URI", err);
                    nameInput.value = oClass.getNameStr(true);
                    return;
                }
                
                this.oTree.update(this.oInfo, [oldURI], [newURI]);
            },
            
            editProperty : function (node) {
                var name = node.data.value;
                this.canvasdiv.innerHTML = "Property<br>" + name;
                this.setPropertyButtons();
                
            },
            
            
            setPropertyButtons : function (name) {
                
                this.buttonspan.innerHTML = "";
            },
            
        };
		
		return EditTab;            // return the constructor
	}
	
);
