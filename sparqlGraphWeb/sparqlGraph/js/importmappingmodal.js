/**
 ** Copyright 2016-2018 General Electric Company
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

// Modal for a line in the import Spec

define([	// properly require.config'ed
    'sparqlgraph/js/modaliidx',
    'sparqlgraph/js/iidxhelper',
    'sparqlgraph/js/importmapping',
    'sparqlgraph/js/selecttable',
    'jquery',

    // shimmed
    'sparqlgraph/js/belmont',
    ],

	function(ModalIidx, IIDXHelper, ImportMapping, SelectTable, $) {

		/**
		 *  callback(OrderElement[])
		 */
		var ImportMappingModal= function (mapping, importSpec, callback) {
            // copy sparqlIDs
            this.mapping = mapping;
            this.importSpec = importSpec;
            this.uriLookupSelect = null;
            this.uriLookupModeSelect = null;
            this.callback = callback;

            this.div = null;
		};


		ImportMappingModal.prototype = {


            validateCallback : function() {
				if (false) {
					return "Something is wrong";
				} else {
					return null;
				}
			},

			okCallback : function() {
                var uriLookupSparqlIDs = IIDXHelper.getSelectValues(this.uriLookupSelect);
                var uriLookupNodes = [];

                // change sparqlID's into belmont Nodes
                for (var i=0; i < uriLookupSparqlIDs.length; i++) {
                    var node = this.importSpec.getNodegroup().getNodeByBindingOrSparqlID(uriLookupSparqlIDs[i]);
                    if (node == null) {
                        throw new Error("Internal error.  Bad sparql id: " + uriLookupSparqlIDs[i]);
                    }
                    uriLookupNodes.push(node);
                }

                this.mapping.setUriLookupNodes(uriLookupNodes);

                // URILookupMode (nodes only)
                if (this.mapping.isNode()) {
                    var modeArr = IIDXHelper.getSelectValues(this.uriLookupModeSelect);
                    if (modeArr.length > 1) {
                        throw new Error("Internal: multiple URI lookup modes for same node");
                    }
                    var mode = (modeArr.length == 0) ? null : modeArr[0];

                    // keep the mode null if it is NO_CREATE and there is nothing actually looking it up
                    if (this.mapping.getUriLookupMode() == null &&
                        mode == ImportMapping.LOOKUP_MODE_NO_CREATE &&
                        this.importSpec.getLookupMappings(this.mapping).length == 0) {
                        mode = null
                    }

                    this.mapping.setUriLookupMode(mode);
                }

                this.callback();
			},

			cancelCallback : function() {

            },


            /**
              *
              */
            launch : function () {
                var myName = this.mapping.getName();

                // get all possible lookup sparqlIds, excluding myName
                var uriSparqlIDs = this.importSpec.getUriSparqlIDs();
                var myIndex = uriSparqlIDs.indexOf(myName);
                if (myIndex > -1) {
                    uriSparqlIDs.splice(myIndex, 1);
                }

                // get current lookup sparql ids
                var uriLookup = this.mapping.getUriLookupIDs();

                this.div = document.createElement("div");

                // URI Lookup title
                IIDXHelper.appendTextLine(this.div, "Use " + myName + " to look up:");

                // URI Lookup select
                this.uriLookupSelect = IIDXHelper.createSelect(null, uriSparqlIDs, uriLookup, true);
                this.div.appendChild(this.uriLookupSelect);
                this.div.appendChild(document.createElement("hr"));

                // URI Lookup Mode (nodes only)
                if (this.mapping.isNode()) {

                    // Lookup Mode title
                    var lookingMeUp = this.importSpec.getLookupMappings(this.mapping);
                    var lookingMeUpStr = "";
                    if (lookingMeUp.length > 0) {
                        lookingMeUpStr = myName + " is being looked up by: ";
                        for (var i=0; i < lookingMeUp.length; i++) {
                            if (i != 0) {
                                lookingMeUpStr += ", ";
                            }
                            lookingMeUpStr += lookingMeUp[i].getName() + " ";
                        }
                    } else {
                        lookingMeUpStr = myName + " is not being looked up"
                    }

                    IIDXHelper.appendTextLine(this.div, lookingMeUpStr);
                    this.div.appendChild(document.createTextNode("Lookup mode: "));

                    // Lookup Mode select
                    var modeStr = "Error if missing";
                    if (this.mapping.getUriLookupMode() == ImportMapping.LOOKUP_MODE_CREATE) {
                        modeStr = "Create if missing";
                    } else if (this.mapping.getUriLookupMode() == ImportMapping.LOOKUP_MODE_ERR_IF_EXISTS) {
                        modeStr = "Error if exists";
                    }
                    this.uriLookupModeSelect = IIDXHelper.createSelect(null,
                                                                       [["Error if missing",  ImportMapping.LOOKUP_MODE_NO_CREATE],
                                                                        ["Create if missing", ImportMapping.LOOKUP_MODE_CREATE   ],
                                                                        ["Error if exists", ImportMapping.LOOKUP_MODE_ERR_IF_EXISTS]],
                                                                       modeStr,
                                                                       false);
                    this.uriLookupModeSelect.disabled = lookingMeUp.length == 0;
                    this.uriLookupModeSelect.size = 1;
                    IIDXHelper.removeMargins(this.uriLookupModeSelect);
                    this.div.appendChild(this.uriLookupModeSelect);
                }

                // launch the modal
                var m = new ModalIidx();
                m.showOKCancel(
                                myName + " lookup settings",
                                this.div,
                                this.validateCallback.bind(this),
                                this.okCallback.bind(this),
                                this.cancelCallback.bind(this),
                                "OK");

            },


		};

		return ImportMappingModal;
	}
);
