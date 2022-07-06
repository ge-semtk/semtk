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
 *  A simple HTML modal dialog.
 *
 *
 * In your HTML, you need:
 * 		1) the stylesheet
 * 			<link rel="stylesheet" type="text/css" href="../css/modaldialog.css" />
 *
 * 		2) an empty div named "modaldialog"
 * 			<div id="modaldialog"></div>
 */

define([	// properly require.config'ed
        	'sparqlgraph/js/iidxhelper',
        	'sparqlgraph/js/modaliidx',
        	'jquery',

			// shimmed
        	'sparqlgraph/js/belmont'
		],
	function(IIDXHelper, ModalIidx, $) {

		var ModalInvalidItemDialog = function(nodeGroupServiceClient, item, targetSNode, nodegroup, conn, oInfo, importSpec, itemUriCallback) {
			// callback(snode, item, data, optionalFlag, deleteFlag)

            this.ngClient = nodeGroupServiceClient;
			this.item = item;            // item which failed validation
            this.target = targetSNode;   // null except for nodeItems
            this.nodegroup = nodegroup;  // nodegroup which failed validation
            this.conn = conn;            // to get oInfo for model
            this.oInfo = oInfo;
			this.callback = itemUriCallback;

            // if item is a prop, find props that are already mapped.  Merging with these would lose information.
            this.mergeConflictURIs = (this.item.getItemType() == "PropertyItem") ? importSpec.getMappedProperties(this.nodegroup.getPropertyItemParentSNode(this.item)) : [];
		};

		ModalInvalidItemDialog.prototype = {
			clear : function () {

			},

            getSelectedDomain : function() {
                var select  = document.getElementById("ModalInvalidItemDialog.DomainSelect");
                return IIDXHelper.getSelectValues(select)[0];
            },

			submit : function () {
                this.callback(this.item, this.target, this.getSelectedDomain(), "domain");
			},

            show : function () {
                if (this.item.getItemType() == "SemanticNode") {
                    this.ngClient.execAsyncSuggestNodeClass(this.nodegroup, this.conn, this.item, this.show1.bind(this), ModalIidx.alert.bind("Call to suggestNodeClass failed"));

                } else if (this.item.getItemType() == "PropertyItem") {
                    // find all properties for this CLASS
                    var snode = this.nodegroup.getPropertyItemParentSNode(this.item);
                    var oClass = this.oInfo.getClass(snode.getURI());
                    var oPropList = this.oInfo.getInheritedProperties(oClass);
                    var domainList = [];
                    for (var op of oPropList) {
                        // separate propItems from nodeItems
                        if (this.oInfo.isDataProperty(op, oClass)) {
                            domainList.push(op.getNameStr());
                        }
                    }
                    this.show2(domainList);

                } else if (this.item.getItemType() == "NodeItem") {
                    // find all nodes for this CLASS
                    var snode = this.nodegroup.getNodeItemParentSNode(this.item);
                    var oPropList = this.oInfo.getInheritedProperties(new OntologyClass(snode.getURI()));
                    var domainList = [];
					var oClass = this.oInfo.getClass(snode.getURI());
					
                    for (var op of oPropList) {
                        // separate out only nodeItems
                        var oRange = op.getRange(oClass, this.oInfo);
                        if (oRange.isComplex() || this.oInfo.containsClass(oRange.getSimpleUri())) {
							for (var rangeUri of op.getAllRangeUris()) {
	                            var rangeClass = new OntologyClass(rangeUri);
	                            var invalidLinks = 0;
	                            for (var target of this.item.getSNodes()) {
	                                var targetClass = new OntologyClass(target.getURI());
	                                if (!this.oInfo.classIsA(targetClass, rangeClass)) {
	                                    invalidLinks = 1;
	                                    break;
	                                }
	                            }
	                        }
                            // if link to target node valid, put it at the top of the list, else on bottom
                            if (invalidLinks == 0) {
                                domainList.unshift(op.getNameStr());
                            } else {
                                domainList.push(op.getNameStr());
                            }
                        }
                    }
                    this.show2(domainList);

                } else {
                    alert("Not implemented in ModalInvalidItemDialog.show()");
                }
            },
            
            // split return from REST call into two arrays
			show1 : function (result_list) {
				var domainList = [];
				var errCountList = [];
				for (var r of result_list) {
					var pair = r.split(",");
					domainList.push(pair[0]);
					errCountList.push(pair[1]);
				}
				this.show2(domainList, errCountList);
			},
			
			show2 : function (domainList, optErrCountList) {
				var dom = document.createElement("fieldset");
				dom.id = "ModalInvalidItemDialogdom";

                // ********  horizontal form with fieldset *********
                var form = IIDXHelper.buildHorizontalForm();
                dom.appendChild(form);
                var fieldset = IIDXHelper.addFieldset(form);

                // title and headers

                var title = "unknown item type";
                var s = document.createElement("span");
                if (this.item.getItemType() == "SemanticNode") {
                    title = "Change Class URI of " + this.item.getSparqlID();
                    s.innerHTML = this.item.getURI();
                } else if (this.item.getItemType() == "PropertyItem") {
                    title = "Change data property " + this.item.getIsReturned() ? this.item.getBindingOrSparqlID() : this.item.getKeyName();
                    s.innerHTML = this.item.getURI();
                    domainList.push("<delete property>");
                } else if (this.item.getItemType() == "NodeItem") {
                    title = "Change object property " + this.item.getKeyName();
                    s.innerHTML = this.item.getURI();
                }
                fieldset.appendChild(IIDXHelper.buildControlGroup("Current URI: ", s));

                // domain select
                var selectList = []; 
                
                // for semantic nodes, annotate select with superclass/subclass
                if (this.item.getItemType() == "SemanticNode") {
					var superNames = this.oInfo.getSuperclassNames(this.item.getURI());
					var subNames = this.oInfo.getSubclassNames(this.item.getURI());
					for (var i=0; i < domainList.length; i++) {
						var d = domainList[i];
						var eCount = optErrCountList[i];
						var eFlag = eCount > 0 ? "[Err] ": "";
						if (superNames.indexOf(d) > -1) {
							selectList.push([eFlag + d + " (superclass)", d]);
						} else if (subNames.indexOf(d) > -1) {
							selectList.push([eFlag + d + " (subclass)", d]);
						} else {
							selectList.push([eFlag + d, d]);
						}
					}
				} else {
					for (var d of domainList) {
						selectList.push([d,d]);
					}
				}

                var selected = domainList.length > 0 ? [domainList[0]] : [];

				var select = IIDXHelper.createSelect("ModalInvalidItemDialog.DomainSelect", selectList, selected, false, "input-xlarge");
                var width = 20;
                for (var c of domainList) {
                    if (c.length > width)
                        width = c.length;
                }
				select.style.width = width + "ch";
                select.onchange = this.updateAll.bind(this);

                var span = document.createElement("span");
                span.appendChild(select);
				fieldset.appendChild(IIDXHelper.buildControlGroup("New URI: ", span));

				// *********** end form ***********

                // status
				elem = document.createElement("div");
				elem.id = "miistatus";
				elem.style.textAlign = "left";
				dom.appendChild(elem);

				ModalIidx.clearCancelSubmit(title, dom, this.clear.bind(this), this.submit.bind(this), "OK", 65, this.validate.bind(this));
                this.updateAll();
			},

            setStatus : function (msg) {
				document.getElementById("miistatus").innerHTML= "<font color='red'>" + msg + "</font>";
			},

            updateAll : function (e, proposedURI) {
                var errMsg = this.validate();
                if (errMsg != null) {
                    this.setStatus(errMsg.replaceAll("\n", "<br>"));
                } else if (this.item.getItemType() == "PropertyItem" && this.item.hasConstraints()) {
                    var currRange = this.item.getRangeURI();
                    var oClass = this.oInfo.getClass(this.nodegroup.getPropertyItemParentSNode(this.item).getURI());
                    var oProp = oClass.getProperty(this.getSelectedDomain());
                    var rangeUri = (oProp != null) ? oProp.getRange(oClass, this.oInfo()).getSimpleUri() : null;
                    if (rangeUri != currRange) {
                        if (oRange != null) {
                            this.setStatus("Property has constraints.  Make sure they are compatible with change to " + oRange.getLocalName());
                        } else {
                            this.setStatus("Note: Property to be deleted has constraints.");
                        }
                    } else {
                        this.setStatus("");
                    }

                } else {
                    this.setStatus("");
                }
            },

            validate : function() {
                var uri = this.getSelectedDomain();
                if (this.mergeConflictURIs.indexOf(uri) > -1) {
                    return this.item.getSparqlID() + "->" + uri.split("#")[1] + " has import mappings, which would be overwritten by this change." +
                            "\nResolve this conflict before continuing.";
                } else {
                    return null;
                }
            },

		};

		return ModalInvalidItemDialog;            // return the constructor
	}
);
