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

        ModalLinkDialog.UNION_NONE = 1000;
        ModalLinkDialog.UNION_NEW =  1001;
        ModalLinkDialog.EMPTY_TEXT = "";
        ModalLinkDialog.NEW_TEXT = "- new union -";
		ModalLinkDialog.prototype = {


			clear : function () {
				document.getElementById("ModalLinkDialog.optionalCheck").checked = false;
				document.getElementById("ModalLinkDialog.deleteCheck").checked = false;
			},

			submit : function () {

                // unpack these two UI elements
                var optMinus = parseInt(document.getElementById("ModalLinkDialog.optMinusSelect").value);
                var reverse = document.getElementById("ModalLinkDialog.reverseCheck").checked;
                // ...into these three callback params
                var union = parseInt(document.getElementById("ModalLinkDialog.unionSelect").value);

                if (reverse) {
                    if (optMinus == NodeItem.OPTIONAL_TRUE) {
                        optMinus = NodeItem.OPTIONAL_REVERSE;
                    } else if (optMinus == NodeItem.MINUS_TRUE) {
                        optMinus = NodeItem.MINUS_REVERSE;
                    }
                }


				// return a list containing just the text field
				this.callback(	this.sourceSNode,
								this.item,
								this.targetSNode,
								this.data,
								optMinus,
                                document.getElementById("ModalLinkDialog.qualifierSelect").value,
                                union,
								reverse,
								document.getElementById("ModalLinkDialog.deleteSelect").value == "true",
								document.getElementById("ModalLinkDialog.deleteCheck").checked
							  );
			},

            // update the select after other inputs on the screen have changed
            // call with initUnionText to force union, otherwise null rebuilds list and keeps value
            updateAll : function (e, itemUnionKey) {
                var itemUnionText = (itemUnionKey == null) ? ModalLinkDialog.EMPTY_TEXT : this.nodegroup.getUnionNameStr(Math.abs(itemUnionKey));

                var reverseCheckbox = document.getElementById("ModalLinkDialog.reverseCheck");
                var unionSelect = document.getElementById("ModalLinkDialog.unionSelect");
                var optMinusSelect = document.getElementById("ModalLinkDialog.optMinusSelect");
                var optMinus = parseInt(optMinusSelect.value);

                // get current union selection, or null
                var selectedUnionTexts = itemUnionText ? [itemUnionText] : IIDXHelper.getSelectTexts(unionSelect);

                // add unions valid for this value of reverseCheckbox
                var unionList = [
                    [ModalLinkDialog.EMPTY_TEXT, ModalLinkDialog.UNION_NONE],
                    [ModalLinkDialog.NEW_TEXT,    ModalLinkDialog.UNION_NEW]
                ];

                this.nodegroup.updateUnionMemberships();
                var unionKeys = this.nodegroup.getLegalUnions(this.sourceSNode, this.item, this.targetSNode, reverseCheckbox.checked);

                for (var key of unionKeys) {
                    unionList.push([this.nodegroup.getUnionNameStr(key), key]);
                }

                // ---- Enable / disable rules ----

                // union disables changing reverseCheckbox or opt / minus
                var unionFlag = (   selectedUnionTexts.length > 0 &&
                                    selectedUnionTexts[0] != ModalLinkDialog.EMPTY_TEXT &&
                                    selectedUnionTexts[0] != ModalLinkDialog.NEW_TEXT
                                );
                reverseCheckbox.disabled = unionFlag;

                // union disables OPTIONAL_TRUE
                var unionFlag = (   selectedUnionTexts.length > 0 &&
                                    selectedUnionTexts[0] != ModalLinkDialog.EMPTY_TEXT
                                );
                IIDXHelper.setSelectOptionDisabled(optMinusSelect, NodeItem.OPTIONAL_TRUE, unionFlag);

                // OPTIONAL_TRUE disables unions
                unionSelect.disabled = (optMinus == NodeItem.OPTIONAL_TRUE);

                IIDXHelper.removeAllOptions(unionSelect);
                IIDXHelper.addOptions(unionSelect, unionList, selectedUnionTexts);
            },

			show : function () {
				var dom = document.createElement("fieldset");
				dom.id = "ModalLinkDialogdom";
				var title = this.sourceSNode.getSparqlID() + "-- " + this.item.getKeyName() + " --" + this.targetSNode.getSparqlID();

				// ********  horizontal form with fieldset *********
				var form = IIDXHelper.buildHorizontalForm();
				dom.appendChild(form);
				var fieldset = IIDXHelper.addFieldset(form);
                var optMinus = this.item.getOptionalMinus(this.targetSNode);

				// optional minus select
                var selectList = [
                    [" ",                NodeItem.OPTIONAL_FALSE   ],
                    ["optional",         NodeItem.OPTIONAL_TRUE    ],
                    ["minus",            NodeItem.MINUS_TRUE    ]
                ];

                // set selected
                var selected = [];
                var optMinus = this.item.getOptionalMinus(this.targetSNode);

                if (optMinus == NodeItem.OPTIONAL_TRUE || optMinus == NodeItem.OPTIONAL_REVERSE) {
                    selected = ["optional"];
                } else if (optMinus == NodeItem.MINUS_TRUE || optMinus == NodeItem.MINUS_REVERSE) {
                    selected = ["minus"];
                }

				var select = IIDXHelper.createSelect("ModalLinkDialog.optMinusSelect", selectList, selected);
				select.style.width = "20ch";
                select.onchange = this.updateAll.bind(this);
                var span = document.createElement("span");
                span.appendChild(select);

                // add reverse check
                var itemUnionKey = this.nodegroup.getUnionKey(this.sourceSNode, this.item, this.targetSNode);
                var unionReverse = (itemUnionKey < 0);
                var reverseCheck = IIDXHelper.createVAlignedCheckbox("ModalLinkDialog.reverseCheck",
                    unionReverse || optMinus == NodeItem.OPTIONAL_REVERSE || optMinus== NodeItem.MINUS_REVERSE );
                reverseCheck.onchange = this.updateAll.bind(this);
                IIDXHelper.appendCheckBox(span, reverseCheck, "reversed");
				fieldset.appendChild(IIDXHelper.buildControlGroup("Opt/Minus: ", span));

                // union select: create empty placeholder.  updates at the end of this function.
				var unionSelect = IIDXHelper.createSelect("ModalLinkDialog.unionSelect",[],[]);
				unionSelect.style.width = "20ch";
                unionSelect.onchange = this.updateAll.bind(this);

                var span = document.createElement("span");
                span.appendChild(unionSelect);

				fieldset.appendChild(IIDXHelper.buildControlGroup("Union: ", span));

                // ---- Qualifier ----
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

				// ---- delete query checkbox ----
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

                this.updateAll(null, itemUnionKey);
			},

		};

		return ModalLinkDialog;            // return the constructor
	}
);
