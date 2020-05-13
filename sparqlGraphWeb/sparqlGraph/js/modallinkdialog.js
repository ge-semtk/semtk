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
        	'sparqlgraph/js/belmont',
        	'bootstrap/bootstrap-tooltip',
        	'bootstrap/bootstrap-transition',
        	'sparqlgraph/jquery/jquery-ui-1.10.4.min'

		],
	function(IIDXHelper, ModalIidx, $) {

		var ModalLinkDialog = function(item, sourceSNode, targetSNode, nodegroup, callback, data) {
			// callback(snode, item, data, optionalFlag, deleteFlag)

			this.item = item;
			this.sourceSNode = sourceSNode;
			this.targetSNode = targetSNode;
			this.nodegroup = nodegroup;
			this.callback = callback;
			this.data = data;
		};

		ModalLinkDialog.prototype = {


			clear : function () {
				document.getElementById("ModalLinkDialog.optionalCheck").checked = false;
				document.getElementById("ModalLinkDialog.deleteCheck").checked = false;
			},

			submit : function () {

				// return a list containing just the text field
				this.callback(	this.sourceSNode,
								this.item,
								this.targetSNode,
								this.data,
								document.getElementById("ModalLinkDialog.optionalMinusSelect").value,
                                document.getElementById("ModalLinkDialog.qualifierSelect").value,
								document.getElementById("ModalLinkDialog.deleteSelect").value == "true",
								document.getElementById("ModalLinkDialog.deleteCheck").checked
							  );
			},


			show : function () {
				var dom = document.createElement("fieldset");
				dom.id = "ModalLinkDialogdom";
				var title = this.sourceSNode.getSparqlID() + "-- " + this.item.getKeyName() + " --" + this.targetSNode.getSparqlID();

				// ********  horizontal form with fieldset *********
				var form = IIDXHelper.buildHorizontalForm();
				dom.appendChild(form);
				var fieldset = IIDXHelper.addFieldset(form);

				// optional checkbox
				var select = IIDXHelper.createSelect("ModalLinkDialog.optionalMinusSelect",
													[[" ",                NodeItem.OPTIONAL_FALSE   ],
													 ["optional",         NodeItem.OPTIONAL_TRUE    ],
													 ["optional reverse", NodeItem.OPTIONAL_REVERSE ],
                                                     ["minus",            NodeItem.MINUS_TRUE    ],
													 ["minus reverse",    NodeItem.MINUS_REVERSE ]
													],
                                                    []
                                                    );

				select.style.width = "20ch";

				// set select.selectedIndex
				var optMinus = this.item.getOptionalMinus(this.targetSNode);
				for (var i=0; i < select.options.length; i++) {
					if (select.options[i].value == optMinus) {
						select.selectedIndex = i;
						break;
					}
				}

				fieldset.appendChild(IIDXHelper.buildControlGroup("Optional/Minus: ", select));

                // Qualifier
                select = IIDXHelper.createSelect("ModalLinkDialog.qualifierSelect",
												  [[" ", ""],
												   ["*", "*" ],
                                                   ["+", "+" ],
                                                   ["?", "?" ],
                                                   ["^", "^" ],
												  ],
												  [this.item.getQualifier(this.targetSNode)]);
                select.style.width = "20ch";
                fieldset.appendChild(IIDXHelper.buildControlGroup("Qualifier: ", select));

				// delete query checkbox
				select = IIDXHelper.createSelect("ModalLinkDialog.deleteSelect",
												  [[" "              , "false"],
												   ["mark for delete", "true" ]
												  ],
												  [this.item.getSnodeDeletionMarker(this.targetSNode) ? "mark for delete":" "]);
				select.style.width = "20ch";
				fieldset.appendChild(IIDXHelper.buildControlGroup("Delete query: ", select));

				// *********** end form ***********

				// delete

				dom.appendChild(document.createElement("hr"));

				var div = document.createElement("div");
				div.setAttribute("align", "right");
				dom.appendChild(div);

				deleteCheck = IIDXHelper.createVAlignedCheckbox();
				deleteCheck.id = "ModalLinkDialog.deleteCheck";
				deleteCheck.checked = false;
				div.appendChild(deleteCheck)

				var txt = document.createElement("span");
				txt.innerHTML = " delete this link";
				div.appendChild(txt);

				// compute a width: at least 40 but wide enough for the title too
				var width = title.length * 1.3;
				width = Math.max(40, width);
				width = Math.floor(width);

				ModalIidx.clearCancelSubmit(title, dom, this.clear.bind(this), this.submit.bind(this), "OK", width.toString() + "ch");

			},

		};

		return ModalLinkDialog;            // return the constructor
	}
);
