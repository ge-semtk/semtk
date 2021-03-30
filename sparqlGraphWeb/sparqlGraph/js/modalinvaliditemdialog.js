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

            getSelectedURI : function() {
                var select  = document.getElementById("ModalInvalidItemDialog.URISelect");
                return IIDXHelper.getSelectValues(select)[0];
            },

			submit : function () {
                this.callback(this.item, this.target, this.getSelectedURI());
			},

            show : function () {
                if (this.item.getItemType() == "SemanticNode") {
                    this.ngClient.execAsyncSuggestNodeClass(this.nodegroup, this.conn, this.item, this.show2.bind(this), ModalIidx.alert.bind("Call to suggestNodeClass failed"));

                } else if (this.item.getItemType() == "PropertyItem") {
                    // find all properties for this CLASS
                    var snode = this.nodegroup.getPropertyItemParentSNode(this.item);
                    var oPropList = this.oInfo.getInheritedProperties(new OntologyClass(snode.getURI()));
                    var uriList = [];
                    for (var op of oPropList) {
                        // separate propItems from nodeItems
                        if (! this.oInfo.containsClass(op.getRangeStr())) {
                            uriList.push(op.getNameStr());
                        }
                    }
                    this.show2(uriList);

                } else if (this.item.getItemType() == "NodeItem") {
                    // find all nodes for this CLASS
                    var snode = this.nodegroup.getNodeItemParentSNode(this.item);
                    var oPropList = this.oInfo.getInheritedProperties(new OntologyClass(snode.getURI()));
                    var uriList = [];
                    var targetClass = new OntologyClass(this.target.getURI());

                    for (var op of oPropList) {
                        // separate propItems from nodeItems
                        var rangeStr = op.getRangeStr();
                        if (this.oInfo.containsClass(rangeStr)) {
                            var rangeClass = new OntologyClass(rangeStr);

                            // if link to target node valid, put it at the top of the list, else on bottom
                            if (this.oInfo.classIsA(targetClass, rangeClass)) {
                                uriList.unshift(op.getNameStr());
                            } else {
                                uriList.push(op.getNameStr());
                            }
                        }
                    }
                    this.show2(uriList);

                } else {
                    alert("Not implemented in ModalInvalidItemDialog.show()");
                }
            },

			show2 : function (uriList) {
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
                    title = "Change relationship URI of " + this.item.getIsReturned() ? this.item.getBindingOrSparqlID() : this.item.getKeyName();
                    s.innerHTML = this.item.getURI();
                } else if (this.item.getItemType() == "NodeItem") {
                    title = "Change relationship URI of " + this.item.getKeyName();
                    s.innerHTML = this.item.getURI();
                }
                fieldset.appendChild(IIDXHelper.buildControlGroup("Current URI: ", s));

                // URI select
                var selectList = uriList;
                var selected = uriList.length > 0 ? [uriList[0]] : [];

				var select = IIDXHelper.createSelect("ModalInvalidItemDialog.URISelect", selectList, selected, false, "input-xlarge");
                var width = 20;
                for (var c of uriList) {
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

				ModalIidx.clearCancelSubmit(title, dom, this.clear.bind(this), this.submit.bind(this), "OK", 85, this.validate.bind(this));
			},

            setStatus : function (msg) {
				document.getElementById("miistatus").innerHTML= "<font color='red'>" + msg + "</font>";
			},

            updateAll : function (e, proposedURI) {
                var errMsg = this.validate();
                if (errMsg != null) {
                    this.setStatus(errMsg.replaceAll("\n", "<br>"));
                } else {
                    this.setStatus("");
                }
            },

            validate : function() {
                var uri = this.getSelectedURI();
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
