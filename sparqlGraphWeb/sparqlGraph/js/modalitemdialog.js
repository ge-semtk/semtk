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

        // PEC TODO:  getValuesCallback must have limit of 10,000 or "..." feature fails
		var ModalItemDialog = function(item, nodegroup, getValuesCallback, callback, data, optGhostItem, optGhostNodegroup) {
			// callback(item, sparqlID, optionalFlag, constraintStr, data)
			//          where sparqlID and contraintStr could be ""
			//          optionalFlag can be null, true, or false
			// data.textId
			//
			// GHOST ITEM and NODEGROUP:
			//  GhostNodegroup : copy of nodegroup with changes made (e.g. remove trigger class in sparqlForm)
			//  GhostItem : corresponding to item
			//  These will be used to generate the SPARQL
			this.item = item;
			this.nodegroup = nodegroup;
			this.getValuesCallback = getValuesCallback;
			this.callback = callback;
			this.data = data;

			this.limit = 10000;
			this.prevName = "";

            // Virtuoso has performance and maybe downright crash troubles
            // if a values clause has too many params.
            // Actual limit is in the 2,000 - 10,000 range
            this.maxValues = 1000;

			this.sparqlformFlag = false;
		};


		ModalItemDialog.VALS_SELECT = 0;
		ModalItemDialog.CONSTRAINT_TEXT = 1;
		ModalItemDialog.SPARQL_ID_TEXT = 2;
		ModalItemDialog.UNUSED = 3;
		ModalItemDialog.RETURN_CHECK = 4;
        ModalItemDialog.CONSTRUCT_CHECK = 45;
		ModalItemDialog.AUTO_TEXT = 5;
		ModalItemDialog.AUTO_TEXT_LIST = 6;
		ModalItemDialog.RT_CONSTRAINED_CHECK = 7;
		ModalItemDialog.DELETE_CHECK = 8;
		ModalItemDialog.DELETE_SELECT = 9;
        ModalItemDialog.RETURN_TYPE_CHECK = 10;
        ModalItemDialog.TYPE_SPARQL_ID_SPAN = 11;
        ModalItemDialog.OPTMINUNI_SELECT = 12;

        // reserved function field numbers 20-30

        ModalItemDialog.UNION_NONE = 1000;
        ModalItemDialog.UNION_NEW =  1001;

		ModalItemDialog.prototype = {
            /*
             * maximum number of suggested values that will be shown
             */
            setLimit : function(l) {
                this.limit = l;
            },

            /*
             * maximum number of values in a values clause
             */
            setMaxValues : function(mv) {
                this.maxValues = mv;
            },

			selectValsOnChange : function () {
				// build the text field using the selected fields in the <select>

				var select = this.getFieldElement(ModalItemDialog.VALS_SELECT);
				var opt;
				var valList = IIDXHelper.getSelectValues(select);

				// build new constraints
                if (valList.length > this.maxValues) {
                    if (this.sparqlformFlag) {
                        ModalIidx.alert("Too many items selected",
                                        "Selecting more than " +  this.maxValues + " values is not supported<br>" +
                                        "due to performance impact.<br>" +
                                        "Consider using REGEX.");
                        // SparqlForm: disallow large FILTER IN clause.
                        this.setFieldValue(ModalItemDialog.CONSTRAINT_TEXT, "");
                        return;

                    } else {
                        ModalIidx.alert("Large values clause",
                                        "Warning: Large number of values selected: " + valList.length + "<br>" +
                                        "Query engine may error or misbehave.<br>" +
                                        "Consider using REGEX.");
                        // SparqlGraph: allow large FILTER IN clause, at user's peril.
                    }
                }

                // swap in sparqlID
				var savedID = this.item.getSparqlID();

				this.item.setSparqlID(this.getSparqlIDFromText());
				this.setFieldValue(ModalItemDialog.CONSTRAINT_TEXT, this.item.buildFilterInConstraint(valList));

				// swap sparqlID back out
				this.item.setSparqlID(savedID);
			},

			clear : function () {
				// clear button

				// uncheck "return"
				if (! this.sparqlformFlag) {
					var returnCheck = this.getFieldElement(ModalItemDialog.RETURN_CHECK);
					returnCheck.checked = false;
					this.selectValsOnChange();   // handles any disabling fields
				}

                // return type
                var retTypeCheck = this.getFieldElement(ModalItemDialog.RETURN_TYPE_CHECK);
                if (retTypeCheck != null) {
                    retTypeCheck.checked = false;
                }

                // delete check
                var delCheck = this.getFieldElement(ModalItemDialog.DELETE_CHECK);
                if (delCheck != null) {
                    delCheck.checked = false;
                }

                // delete select
                var delSelect = this.getFieldElement(ModalItemDialog.DELETE_SELECT);
                if (delSelect != null) {
                    delSelect.selectedIndex = 0;
                }

				// choose first (default) item in the union select
				var optMinSelect = this.getFieldElement(ModalItemDialog.OPTMINUNI_SELECT);
				if (optMinSelect != null) {
                    optMinSelect.selectedIndex = "0";
            	}

				// uncheck "runtime constrained"
				var rtConstrainedCheck = this.getFieldElement(ModalItemDialog.RT_CONSTRAINED_CHECK);
				rtConstrainedCheck.checked = false;

				// unselect everything
				var select = this.getFieldElement(ModalItemDialog.VALS_SELECT);
				var len = select.length;
				for (var i=0; i < len ;i++) {
					select[i].selected = false;
				}

				// clear the constraint
				this.setFieldValue(ModalItemDialog.CONSTRAINT_TEXT, "");

                // functions
                for (var f of SemanticNodeGroup.FUNCTION_LIST) {
                    this.getFieldElement(this.getFunctionFieldNumber(f)).checked = false;
                }

				// note that we leave the sparqlID
			},

			submit : function () {

				var sparqlIDElem = this.getFieldElement(ModalItemDialog.SPARQL_ID_TEXT);
				var sparqlID = this.getSparqlIDFromText();

				var returnChecked = this.getFieldElement(ModalItemDialog.RETURN_CHECK).checked;
                var returnTypeChecked = this.getFieldElement(ModalItemDialog.RETURN_TYPE_CHECK) ? this.getFieldElement(ModalItemDialog.RETURN_TYPE_CHECK).checked : false;
				var rtConstrainedChecked = this.getFieldElement(ModalItemDialog.RT_CONSTRAINED_CHECK).checked;
				var constraintTxt = this.getFieldValue(ModalItemDialog.CONSTRAINT_TEXT);

				// delMarker will be boolean for propItem, int for snode, maybe null for either
				var delMarker;
				if (this.getFieldElement(ModalItemDialog.DELETE_CHECK) != null) {
					delMarker = this.getFieldElement(ModalItemDialog.DELETE_CHECK).checked;
				} else if (this.getFieldElement(ModalItemDialog.DELETE_SELECT) != null) {
					var select = this.getFieldElement(ModalItemDialog.DELETE_SELECT);
					delMarker = parseInt(select[select.selectedIndex].value);
				} else {
					delMarker = null;
				}

                var functions = this.getCheckedFunctions();

                var constructedChecked = this.item.getItemType() == "SemanticNode" ? this.getFieldElement(ModalItemDialog.CONSTRUCT_CHECK).checked : undefined;

                // return a list containing just the text field
				this.callback(	this.item,
								(returnChecked || rtConstrainedChecked || constraintTxt != "" || delMarker != null) ? sparqlID : "",
								returnChecked,
                                returnTypeChecked,
                                this.getOptMinFromSelect(),
                                this.getUnionFromSelect(),
								delMarker,
								rtConstrainedChecked,
								constraintTxt,
								this.data,
                                functions,
                                constructedChecked
                            );
			},

            getFunctionFieldNumber : function (f) {
                return f + 20;
            },

            // Get UNION
            // including possible UNION_NONE and UNION_NEW

            getUnionFromSelect : function () {
                var selectElem = this.getFieldElement(ModalItemDialog.OPTMINUNI_SELECT);
                if (selectElem) {
                    var selValue = parseInt(IIDXHelper.getSelectValues(selectElem)[0]);
                    return selValue < 100 ? ModalItemDialog.UNION_NONE : selValue - 100;
                } else {
                    // early on and select doesn't exist yet, grab from this.item.
                    var ret = this.nodegroup.getUnionKey(this.getItemSnode(), this.getItemProp());
                    return (ret == null) ? ModalItemDialog.UNION_NONE : ret;
                }
            },

            getOptMinFromSelect : function () {
                var selectElem = this.getFieldElement(ModalItemDialog.OPTMINUNI_SELECT);
                if (selectElem) {
                    // normal operation
                    var selValue = parseInt(IIDXHelper.getSelectValues(selectElem)[0]);
                    return selValue >= 100 ? ModalItemDialog.OPT_MINUS_NONE : selValue;
                } else {
                    // early on and select doesn't exist yet, grab from this.item.
                    return this.item.getOptionalMinus();
                }
            },

			setStatus : function (msg) {
				document.getElementById("mcdstatus").innerHTML= "<font color='red'>" + msg + "</font>";
			},

			setStatusAlert : function (msg, severity) {
				// make status box an iidx alert
				// severity = "alert-success", "alert-info", "alert-error", or ""
				document.getElementById("mcdstatus").innerHTML =' <div class="alert ' + severity + '">' + msg + '</div>';
			},

			setRunningQuery : function (flag) {
				if (flag) {
					this.setStatus("Running query...");
				} else {
					this.setStatus("");
			    };
			},

            /* run query to suggest values
             *
             */
			query : function () {

				this.setRunningQuery(true);

				// run query
				this.getValuesCallback(this.getValuesSuccess.bind(this),
                                       this.getValuesFailure.bind(this),
                                       function(){});
			},

            getValuesFailure : function (htmlMsg) {
                this.setRunningQuery(false);
                ModalIidx.alert("Error retrieving values", htmlMsg);
                this.setStatusAlert("Error retrieving values");
            },

            getValuesSuccess : function (res) {
                this.setRunningQuery(false);

                if (res.getRowCount() < 1) {
					this.setStatusAlert("No possible values exist.<br>Constraints are too tight or no instance data exists.");

				} else {

					// get all the return values

					var element = [];

					for (var i=0; i <res.getRowCount(); i++) {
						element[i] = {	name: res.getRsData(i, 0, this.sparqlformFlag ? res.NAMESPACE_NO : res.NAMESPACE_YES),  // strip ns from name if sparqlform
								      	val: res.getRsData(i, 0, res.NAMESPACE_YES)
								     };

                        //PEC TODO: the following old code was true for val of 0
                        //          I'm not sure why this is even here.
						//if (element[i].val == "" || element[i].name == "") {
                        if (element[i].name == null) {
							this.setStatusAlert("Error: One of the values returned is NULL");
							return;
						};
					}

					element = element.sort(function(a,b){
												if (a.name < b.name) return -1;
												if (a.name > b.name) return 1;
												return 0;
											});

					// insert "..." if we hit the limit
					if (res.getRowCount() == this.limit) {
						this.setStatusAlert("Too many values.  A random subset of " + this.limit + " are shown.");
						element[this.limit] = {name: "", val: ""};
						element[this.limit].name = "...";
						element[this.limit].val = null;
					};

					this.fillValsSelect(element);
					this.fillAutoText(element);
				};
			},

			getSparqlIDFromText : function() {
				// get sparqlID out of text box, ensure single leading '?', remove spaces
				// may return ""

				var ret = this.getFieldValue(ModalItemDialog.SPARQL_ID_TEXT).trim();
				while (ret[1] === '?') {
					ret = ret.slice(1);
				}
				if (ret.length > 0 && ret[0] !== '?') {
					ret = '?' + ret;
				}
				return ret;
			},

			buildRegex : function() {
				// swap in sparqlID
				var savedID = this.item.getSparqlID();

				this.item.setSparqlID(this.getSparqlIDFromText());

				//  build the regex, swap id back out
				var newConstraint = this.item.buildFilterConstraint(null, null);
				this.item.setSparqlID(savedID);

				this.setFieldValue(ModalItemDialog.CONSTRAINT_TEXT, newConstraint);
			},

			fillValsSelect : function (element) {
				// element should be an array of items with ".name" and ".val" fields
				// populate the SELECT with given parallel arrays of names and values
				var select = this.getFieldElement(ModalItemDialog.VALS_SELECT);

				var constraintSparql = this.getFieldValue(ModalItemDialog.CONSTRAINT_TEXT);
				var valuesFlag = (constraintSparql.search(" IN ") > -1);
				select.options.length = 0;

				for (var i=0; i < element.length; i++) {
					var el = document.createElement("option");
					// fill the element

					el.textContent = element[i].name;

					el.value = element[i].val;

					// check to see if it should be selected
					if (valuesFlag) {
						var searchFor = "['<]" + IIDXHelper.regexSafe(element[i].val) + "['>]";
						if (constraintSparql.search(searchFor) > -1) {
							el.selected = true;
						}
					}

					// add element
					select.appendChild(el);
				}
			},

			fillAutoText : function (element) {
				// element should be an array of items with ".name" and ".val" fields
				// init the autoselect list and the value in the text box
				var autoTextList = this.getFieldElement(ModalItemDialog.AUTO_TEXT_LIST);
				var constraintSparql = this.getFieldValue(ModalItemDialog.CONSTRAINT_TEXT);
				var valuesFlag = (constraintSparql.search(" IN  ") > -1);
				autoTextList.options.length = 0;

				for (var i=0; i < element.length; i++) {
					var el = document.createElement("option");
					// fill the element

					el.value = element[i].name;

					// check to see if it should be selected
					if (valuesFlag) {
						var searchFor = "['<]" + IIDXHelper.regexSafe(element[i].val) + "['>]";
						if (constraintSparql.search(searchFor) > -1) {
							el.selected = true;
						}
					}

					// add element
					autoTextList.appendChild(el);
				}

			},

			fillOptions : function (element, listElem) {
				var constraintSparql = this.getFieldValue(ModalItemDialog.CONSTRAINT_TEXT);
				var valuesFlag = (constraintSparql.search(" IN  ") > -1);
				listElem.options.length = 0;

				for (var i=0; i < element.length; i++) {
					var el = document.createElement("option");
					// fill the element

					el.textContent = element[i].name;

					el.value = element[i].val;

					// check to see if it should be selected
					if (valuesFlag) {
						var searchFor = "['<]" + IIDXHelper.regexSafe(element[i].val) + "['>]";
						if (constraintSparql.search(searchFor) > -1) {
							el.selected = true;
						}
					}

					// add element
					listElem.appendChild(el);
				}
			},

			binarySearch : function(ar, el, compare_fn) {
			    var m = 0;
			    var n = ar.length - 1;
			    while (m <= n) {
			        var k = (n + m) >> 1;
			        var cmp = compare_fn(el, ar[k]);
			        if (cmp > 0) {
			            m = k + 1;
			        } else if(cmp < 0) {
			            n = k - 1;
			        } else {
			            return k;
			        }
			    }
			    return -m - 1;
			},

			toggleListElement : function () {
				// using the value from AUTO_TEXT
				// toggle the corresponding value in SELECT
				// and, if successful, empty out AUTO_TEXT
				var text = this.getFieldValue(ModalItemDialog.AUTO_TEXT);
				var select = this.getFieldElement(ModalItemDialog.VALS_SELECT);

				// find text in select
				var pos = this.binarySearch(select, text, function(text, el) {
																if (text === el.textContent) return 0;
																else if (text < el.textContent) return -1;
																else return 1;
															});
				// if found: toggle, re-write SPARQL, erase
				if (pos > -1) {
					if (select[pos].selected) {
						select[pos].selected = false;
					} else {
						select[pos].selected = true;
					}

					this.selectValsOnChange();
					this.getFieldElement(ModalItemDialog.AUTO_TEXT).value="";
				}

			},

			sparqlIDOnFocus : function () {
				this.prevName = this.getSparqlIDFromText();
			},

            // for union operations that require this.item to be in snode,prop form
            getItemSnode : function() {
                if (this.item instanceof PropertyItem) {
                    return gNodeGroup.getPropertyItemParentSNode(this.item);
                } else {
                    return this.item;
                }
            },

            // for union operations that require this.item to be in snode,prop form
            getItemProp : function () {
                if (this.item instanceof PropertyItem) {
                    return this.item;
                } else {
                    return undefined;
                }
            },

            getNameHash : function () {
                var ret = null;
                var newUnion = this.getUnionFromSelect();

                // get snode and prop for union operations
                var snode = this.getItemSnode();
                var prop = this.getItemProp();

                var prevUnion = this.nodegroup.getUnionKey(snode, prop);
                var prevIsReturned = undefined;
                var prevSparqlId = undefined;
                if (prop != undefined) {
                    prevIsReturned = prop.getIsReturned();
                    prevSparqlId = prop.getSparqlID();
                }

                // temporarily add to union shown in the dialog
                this.nodegroup.rmFromUnions(snode, prop);
                if (newUnion < ModalItemDialog.UNION_NONE) {
                    gNodeGroup.addToUnion(newUnion, snode, prop);
                }
                // temporarily make property returned
                if (prevIsReturned == false) {
                    prop.setSparqlID("?Throw__Away___ModalItemDialog");
                    this.nodegroup.setIsReturned(prop.getSparqlID(), true);
                }

                // calculate all variable names
                ret = gNodeGroup.getAllVariableNamesHash(snode, prop);

                // restore union
                if (newUnion < ModalItemDialog.UNION_NONE) {
                    this.nodegroup.rmFromUnions(snode, prop);
                    if (prevUnion != null && prevUnion > -1 && prevUnion < ModalItemDialog.UNION_NONE) {
                        gNodeGroup.addToUnion(prevUnion, snode, prop);
                    }
                }
                // retstore is returned
                if (prevIsReturned == false) {
                    prop.setSparqlID(prevSparqlId);
                    this.nodegroup.setIsReturned(prop.getSparqlID(), false);
                }

                return ret;
            },
            unionSelectOnFocus : function() {
                this.sparqlIDOnFocus();
            },
            unionSelectOnFocusOut : function() {
                this.sparqlIDOnFocusOut();
            },

            functionCheckCallback : function() {
                if (this.getCheckedFunctions().length > 0) {
                    this.getFieldElement(ModalItemDialog.RETURN_CHECK).checked = false;
                    this.returnCheckCallback();
                }
            },

            returnCheckCallback : function() {
                // property items:
                // user can't change getIsConstruct
                // it is determined by isReturned and parentSNode.isConstructed()
                var returnCheck = this.getFieldElement(ModalItemDialog.RETURN_CHECK);

                if (this.item.getItemType() == "PropertyItem") {

                    var constructCheck = this.getFieldElement(ModalItemDialog.CONSTRUCT_CHECK);
                    var snode = this.nodegroup.getPropertyItemParentSNode(this.item);

                    constructCheck.checked = returnCheck.checked && snode.getIsConstructed();
                }

                if (returnCheck.checked) {
                    for (var f of SemanticNodeGroup.FUNCTION_LIST) {
                        this.getFieldElement(this.getFunctionFieldNumber(f)).checked = false;
                    }
                }
            },

			sparqlIDOnFocusOut : function() {

				var displayedName = this.getSparqlIDFromText();
				var f = new SparqlFormatter();

				// handle blank sparqlID
				if (displayedName === "") {
					newName = f.genSparqlID("ID", this.getNameHash());
					ModalIidx.alert("Blank name is invalid", "Using " + newName + ".");

				} else {
                    // for legality-checking: make sure newName has "?"
                    if (displayedName[0][0] !== "?") {
                        displayedName = "?" + displayedName;
                    }

                    // make sure new name is legal
                    var newName = f.genSparqlID(displayedName, this.getNameHash());
                    if (newName != displayedName) {
                        ModalIidx.alert("SparqlID Invalid", "Using " + newName + " instead.");
                    }

                    this.updateConstraintSparqlID(this.prevName, newName);

                    // update ruturn_type checkbox text
                    var span = this.getFieldElement(ModalItemDialog.TYPE_SPARQL_ID_SPAN);
                    if (span) span.innerHTML = newName + "_type";
                }

				// set the new sparqlID (without the leading '?')
				this.setFieldValue(ModalItemDialog.SPARQL_ID_TEXT, newName.slice(1));

			},

			updateConstraintSparqlID : function(oldID, newID) {
				var constraint = this.getFieldValue(ModalItemDialog.CONSTRAINT_TEXT);

				var delims0 = [' ', ',', '\\('];
				var delims1 = [' ', ',', '('];
				for (var i=0; i < delims0.length; i++) {
					constraint = constraint.replace(new RegExp('\\' + oldID + delims0[i], 'g'), newID + delims1[i]);
				}

				this.setFieldValue(ModalItemDialog.CONSTRAINT_TEXT, constraint);

			},

            getOptMinusText : function (optMin) {
                if (optMin == PropertyItem.OPT_MINUS_NONE) {
                    optMinusText = "- no optional/minus/union -";
                } else if (optMin == PropertyItem.OPT_MINUS_OPTIONAL) {
                    optMinusText = "optional";
                } else if (optMin == PropertyItem.OPT_MINUS_MINUS) {
                    optMinusText = "minus";
                } else if (optMin == PropertyItem.OPT_MINUS_EXIST) {
                    optMinusText = "exists";
                }
                return optMinusText;
            },

            //  Build the dialog.
            //  300 line function probably needs fixing (ya think?)
			show : function (optSparqlformFlag) {
				if (typeof optSparqlformFlag != "undefined") {
					this.sparqlformFlag = optSparqlformFlag;
				}

				var dom = document.createElement("fieldset");
				dom.id = "modalitemdialogdom";

				var title = this.item.getSparqlID().slice(1); // old: this.item.getKeyName();

                var elem;
				var returnCheck;
                var returnClassCheck;
				var runtimeConstrainedCheck;
				var sparqlIDTxt;
				var optMinSelect;
				var table;
				var tr;
				var td;
				var but;
                var anythingSetFlag = false;   // something on this item is non-default, not first time editing

				// table for return items
				table = document.createElement("table");
				dom.appendChild(table);

				tr = document.createElement("tr");
				table.appendChild(tr);

				// row 1 col 1:  "Name: "
				td = document.createElement("td");
				td.style.verticalAlign = "top";
				tr.appendChild(td);
				elem = document.createElement("b");
				elem.appendChild(document.createTextNode("Name: " ) );
				td.appendChild(elem);

				// row 1 col 2   "text input box"
				td = document.createElement("td");
				tr.appendChild(td);

				sparqlIDTxt = document.createElement("input");
				sparqlIDTxt.type = "text";
				sparqlIDTxt.style.margin = 0;
				sparqlIDTxt.id = this.getFieldID(ModalItemDialog.SPARQL_ID_TEXT);

                var binding = this.item.getBinding();
                var sparqlID = this.item.getSparqlID();
				if (sparqlID === "") {
					var f = new SparqlFormatter();
					sparqlID = f.genSparqlID(this.item.getKeyName(), this.getNameHash());
				}
				sparqlIDTxt.value = binding ? binding.slice(1) : sparqlID.slice(1);

				sparqlIDTxt.onfocus    = this.sparqlIDOnFocus.bind(this);
				sparqlIDTxt.onfocusout = this.sparqlIDOnFocusOut.bind(this);
				sparqlIDTxt.style.disabled = this.sparqlformFlag;
				td.appendChild(sparqlIDTxt);
                anythingSetFlag = anythingSetFlag || (binding != null && binding != sparqlID);

				// row 1 col 3  "returned checkbox"
				td = document.createElement("td");
				td.style.verticalAlign = "top";
				tr.appendChild(td);

				// return checkbox
				returnCheck = IIDXHelper.createVAlignedCheckbox(
                                this.getFieldID(ModalItemDialog.RETURN_CHECK),
                                this.item.getIsReturned() || this.item.getIsBindingReturned(),
                                "btn",
                                this.returnCheckCallback.bind(this)
                                );

                IIDXHelper.appendCheckBox(td, returnCheck, "select");
                anythingSetFlag = anythingSetFlag || returnCheck.checked;

                // construct checkbox
				constructCheck = IIDXHelper.createVAlignedCheckbox(
                                this.getFieldID(ModalItemDialog.CONSTRUCT_CHECK),
                                false, // gets set below
                                "btn"
                                );
                if (this.item.getItemType() == "PropertyItem") {
                    // PropItem: isConstructed is determined by isReturned and parent.isConstructed
                    constructCheck.disabled = true;
                    var snode = this.nodegroup.getPropertyItemParentSNode(this.item);
                    constructCheck.checked = this.item.getIsReturned() && snode.getIsConstructed();
                } else {
                    // SemanticNode: only settable if not returned
                    if (returnCheck.checked) {
                        constructCheck.checked = true;
                        constructCheck.disabled = true;
                    } else {
                        constructCheck.disabled = false;
                        constructCheck.checked = this.item.getIsConstructed();
                        // NOT constructing the non-default behavior
                        anythingSetFlag = anythingSetFlag || ! constructCheck.checked;
                    }
                }

                IIDXHelper.appendCheckBox(td, constructCheck, "construct");

                if (this.item.getItemType() == "PropertyItem") {
                    td.append(document.createTextNode(" "));
                    td.appendChild(ModalIidx.createInfoButton("To construct this data property, check 'select' and make sure the parent node is constructed."));
                }
				// row #2
				tr = document.createElement("tr");
				table.appendChild(tr);

				// cell 2,1: headings
				td = document.createElement("td");
                td.style.verticalAlign="top";
                tr.appendChild(td);
                b = document.createElement("b");
                td.appendChild(b);
                if (!this.sparqlformFlag && this.item.getItemType() == "SemanticNode") {
                    b.appendChild(document.createTextNode("Type:"));
                    b.appendChild(document.createElement("br"));
                }
                b.appendChild(document.createElement("br"));
                b.appendChild(document.createTextNode("Funcs:"));


				// cell 2,2: class info if Semantic node
				td = document.createElement("td");
                td.style.verticalAlign="top";
				tr.appendChild(td);
                if (!this.sparqlformFlag && this.item.getItemType() == "SemanticNode") {
                    returnClassCheck = IIDXHelper.createVAlignedCheckbox(
                                            this.getFieldID(ModalItemDialog.RETURN_TYPE_CHECK),
                                            this.item.getIsTypeReturned(),
                                            "btn"
                                            );

                    td.appendChild(returnClassCheck);
                    td.appendChild(document.createTextNode("return "));
                    var span = document.createElement("span");
                    span.id = this.getFieldID(ModalItemDialog.TYPE_SPARQL_ID_SPAN);
                    span.innerHTML = this.item.getTypeSparqlID();

                    td.appendChild(span);
                    td.appendChild(document.createElement("br"));
                    anythingSetFlag = anythingSetFlag || returnClassCheck.checked;
                }

                // cell 2,2 continued: functions

                if (!this.sparqlformFlag) {
                    td.appendChild(document.createElement("br"));

                    td.appendChild(this.buildFunctionTable());
                    anythingSetFlag = anythingSetFlag || this.item.getFunctions().length > 0;
                }

				// cell 2,3: batch of controls:  opt/min, delete, runtime constrain
				td = document.createElement("td");
				td.style.verticalAlign = "top";
				tr.appendChild(td);

				// if sparqlform and this is a node, look for singleNodeItem
				var singleNodeItem = null;

				if (this.sparqlformFlag && this.item.getItemType() == "SemanticNode") {
					singleNodeItem = this.nodegroup.getSingleConnectedNodeItem(this.item);
				}
				// is optional applicable to this item
				var showSelect = (this.item.getItemType() == "PropertyItem" || this.item.getItemType() == "SemanticNode" || singleNodeItem != null);

				// optional select
				if (showSelect) {
                    var selectedText = "";
                    var optMinus;
                    if (this.item.getItemType() == "PropertyItem") {
                        optMinus = this.item.getOptMinus();
                        selectedText = this.getOptMinusText(optMinus);

					} else if (this.item.getItemType() == "SemanticNode") {
                        optMinus = NodeItem.OPT_MINUS_NONE;
                        selectedText = "";

                    } else {
                        var targetNode = singleNodeItem.getSNodes()[0];
                        optMinus = singleNodeItem.getOptionalMinus(targetNode);
                        if (optMinus == NodeItem.OPTIONAL_TRUE || optMinus == NodeItem.OPTIONAL_REVERSE) {
                            selectedText = this.getOptMinusText(NodeItem.OPT_MINUS_OPTIONAL);
                        } else if (optMinus == NodeItem.MINUS_TRUE || optMinus == NodeItem.MINUS_REVERSE) {
                            selectedText = this.getOptMinusText(NodeItem.OPT_MINUS_MINUS);
                        } else {
                            selectedText = this.getOptMinusText(NodeItem.OPT_MINUS_NONE);
                        }
					}

                    // optional minus stuff is single digits
                    // unions will be 100 + [0-100]
                    // UNION_NONE or UNION_NEW are > 999
                    var selectList;

                    if (this.item.getItemType() == "PropertyItem") {
                        selectList = [
                            [this.getOptMinusText(PropertyItem.OPT_MINUS_NONE), PropertyItem.OPT_MINUS_NONE],
        					[this.getOptMinusText(PropertyItem.OPT_MINUS_OPTIONAL), PropertyItem.OPT_MINUS_OPTIONAL],
                            [this.getOptMinusText(PropertyItem.OPT_MINUS_MINUS), PropertyItem.OPT_MINUS_MINUS],
                            [this.getOptMinusText(PropertyItem.OPT_MINUS_EXIST), PropertyItem.OPT_MINUS_EXIST],
                            ["- new union -",    ModalItemDialog.UNION_NEW + 100]
                        ];
                    } else {
                        selectList = [
                            ["- no union -",     ModalItemDialog.UNION_NONE + 100],
                            ["- new union -",    ModalItemDialog.UNION_NEW + 100]
                        ];
                    }
                    selectList.push();

                    // add union keys
                    this.nodegroup.updateUnionMemberships();

                    var unionKeys;
                    var itemUnionKey;
                    if (this.item instanceof SemanticNode) {
                        unionKeys = this.nodegroup.getLegalUnions(this.item);
                        itemUnionKey = this.nodegroup.getUnionKey(this.item);
                    } else {
                        var snode = this.nodegroup.getPropertyItemParentSNode(this.item);
                        unionKeys = this.nodegroup.getLegalUnions(snode, this.item);
                        itemUnionKey = this.nodegroup.getUnionKey(snode, this.item);
                    }

                    for (var key of unionKeys) {
                        selectList.push([this.nodegroup.getUnionNameStr(key), key + 100]);
                    }

                    // set selected
                    var selected = [];
                    if (itemUnionKey != null) {
                        selected = [this.nodegroup.getUnionNameStr(itemUnionKey)];
                    } else {
                        selected = [selectedText];
                    }

                    var select = IIDXHelper.createSelect(
                            this.getFieldID(ModalItemDialog.OPTMINUNI_SELECT),
                            selectList,
                            selected
                        );
                    select.onfocus    = this.unionSelectOnFocus.bind(this);
                    select.onfocusout = this.unionSelectOnFocusOut.bind(this);
                    anythingSetFlag = anythingSetFlag || select.selectedIndex != 0;
				}

				runtimeConstrainedCheck = IIDXHelper.createVAlignedCheckbox(
                                            this.getFieldID(ModalItemDialog.RT_CONSTRAINED_CHECK),
                                            this.item.getIsRuntimeConstrained(),
                                            "btn"
                                        );
                anythingSetFlag = anythingSetFlag || runtimeConstrainedCheck.checked;

				// Top section is handled totally differently with sparqlformFlag
				if (this.sparqlformFlag) {
					// create a right-justified div just for optional
					var div = document.createElement("div");
					div.align = "right";

					// assemble
					if (showSelect) {
						div.appendChild(select);
                        div.appendChild(document.createElement("br"));
					}
					div.appendChild(runtimeConstrainedCheck)
					div.appendChild(document.createTextNode(" runtime constrained"));
					dom.appendChild(div);

					// make top table invisible
					table.style.display = "none";

				} else {
					// normal operation: put optional check into the top table
					if (showSelect) {
                        td.appendChild( document.createTextNode( '\u00A0\u00A0' ) );
                        td.appendChild(select);
                        td.appendChild(document.createElement("br"));
					}


					// deletion
					if (this.item.getItemType() == "PropertyItem") {
                        // deleting a property is just a check box
						var deleteCheck = IIDXHelper.createVAlignedCheckbox(
                                            this.getFieldID(ModalItemDialog.DELETE_CHECK),
						                    this.item.getIsMarkedForDeletion()
                                        );
                        IIDXHelper.appendCheckBox(td, deleteCheck, "mark for delete");
                        anythingSetFlag = anythingSetFlag || deleteCheck.checked;

					} else {
                        // deleting a Semantic node has many options
						var options = [];
                        var selectedText = [];
						for (var key in NodeDeletionTypes) {
							options.push([key, NodeDeletionTypes[key]]);

                            if (NodeDeletionTypes[key] == this.item.getDeletionMode()) {
                                selectedText.push(key);
                            }
						}
						var deleteSelect = IIDXHelper.createSelect(   this.getFieldID(ModalItemDialog.DELETE_SELECT),
                                                                      options,
                                                                      selectedText);

                        // TODO: last two aren't implemented on the other end
                        //       They probably shouldn't be in belmont.js
                        deleteSelect.options[3].disabled = true;
                        deleteSelect.options[4].disabled = true;

                        td.appendChild( document.createTextNode( '\u00A0\u00A0' ) );
						td.appendChild(deleteSelect);
                        anythingSetFlag = anythingSetFlag || deleteSelect.selectedIndex != 0;
					}

					td.appendChild(document.createElement("br"));
                    IIDXHelper.appendCheckBox(td, runtimeConstrainedCheck, "runtime constrained");

				}

				// regex button
				but = document.createElement("button");
				but.classList.add("btn");
				but.id = "btnRegex";
				but.type = "button";
				but.onclick = this.buildRegex.bind(this);
				but.innerHTML = "Regex Template";

				elem = document.createElement("b");
				elem.innerHTML = "Constraint SPARQL";

				dom.appendChild( IIDXHelper.createLeftRightTable(elem, but) );


				// Constraint text area
				elem = document.createElement("textarea");
				elem.rows = "3";
				elem.classList.add("input-xlarge");
				elem.id = this.getFieldID(ModalItemDialog.CONSTRAINT_TEXT);
				elem.value = this.item.getConstraints();
				elem.style.width = "100%";
				elem.style.maxWidth = "100%";
				elem.style.boxSizing = "border-box";
                elem.onkeydown = function(event) { return event.keyCode != 13; };   // disallow line returns

				dom.appendChild(elem);
                anythingSetFlag = anythingSetFlag || this.item.getConstraints().length > 0;

				// -----  Auto-complete section ------
				var list = document.createElement("datalist");
				list.id = this.getFieldID(ModalItemDialog.AUTO_TEXT_LIST);
				dom.appendChild(list);

				if (this.sparqlformFlag) {
					div.appendChild(document.createTextNode(" "));
				}

				// auto-complete text
				elem = document.createElement("input");
				elem.type = "text";
				elem.setAttribute("list", list.id);
				elem.id = this.getFieldID(ModalItemDialog.AUTO_TEXT);
				elem.style.boxSizing = "border-box";
				elem.onkeypress = function(e) {
									    if(e.keyCode == 13)
									    {
									        this.toggleListElement();
									    }
							      }.bind(this);


			    // auto-complete button
			    but = document.createElement("button");
			    but.innerHTML = "+/-";
			    but.onclick = this.toggleListElement.bind(this);

			    // put auto-complete text and button on a search form
			    var form = document.createElement("form");
				form.classList.add("form-search");
				form.style.margin = 0;
				form.onsubmit = function(){return false;};    // NOTE: forms shouldn't submit automatically on ENTER
				form.appendChild(document.createTextNode("Search: "));
				div = document.createElement("div");
				div.classList.add("input-append");
				div.appendChild(elem);
				div.appendChild(but);
				form.appendChild(div);

				elem = document.createElement("b");
				elem.innerHTML = "Values:";

				dom.appendChild( IIDXHelper.createLeftRightTable(elem, form) );

				// ------ value <select> ------
				elem = document.createElement("select");
				elem.multiple = true;
				elem.id = this.getFieldID(ModalItemDialog.VALS_SELECT);
				elem.size = "10";
				elem.onchange = this.selectValsOnChange.bind(this);
				elem.style.width = "100%";
				dom.appendChild(elem);


				// ----- buttons div under the select -----
				var div = document.createElement("div");
				//div.classList.add("form-actions");
				div.style.textAlign = "left";
				dom.appendChild(div);


				// suggest values button
				elem = document.createElement("button");
				elem.classList.add("btn");
				elem.id = "btnSuggest";
				elem.type = "button";
				elem.onclick = this.query.bind(this);
				if (this.sparqlformFlag) {
					elem.style.display = "none";   // hide the "Suggest" button if suggestions have already been generated
				}
				elem.innerHTML = "Suggest Values";
				div.appendChild(elem);
				// ----- done with buttons under select -----

				// status
				elem = document.createElement("div");
				elem.id = "mcdstatus";
				elem.style.textAlign = "left";
				dom.appendChild(elem);

				ModalIidx.clearCancelSubmit(title, dom, this.clear.bind(this), this.submit.bind(this));

				if (this.sparqlformFlag) {
					this.query();
				} else {
                    if (! anythingSetFlag) {
                        returnCheck.checked = true;
                        this.returnCheckCallback();
                    }
                }

				// tooltips
				$("#" + this.getFieldID(ModalItemDialog.OPTMINUNI_SELECT)).tooltip({placement: "left"});

			},

            // table of aggregate SPARQL function buttons
            buildFunctionTable : function() {
                table = document.createElement("table");
                table.style.width="100%";
                table.style.border="1px solid lightgray"

                tr = document.createElement("tr");
                table.appendChild(tr);

                tr.appendChild(this.buildFunctionTd("MIN", this.getFieldID(this.getFunctionFieldNumber(SemanticNodeGroup.FUNCTION_MIN))));
                tr.appendChild(this.buildFunctionTd("MAX", this.getFieldID(this.getFunctionFieldNumber(SemanticNodeGroup.FUNCTION_MAX))));
                tr.appendChild(this.buildFunctionTd("COUNT", this.getFieldID(this.getFunctionFieldNumber(SemanticNodeGroup.FUNCTION_COUNT))));

                tr = document.createElement("tr");
                table.appendChild(tr);

                tr.appendChild(this.buildFunctionTd("AVG", this.getFieldID(this.getFunctionFieldNumber(SemanticNodeGroup.FUNCTION_AVG))));
                tr.appendChild(this.buildFunctionTd("SUM", this.getFieldID(this.getFunctionFieldNumber(SemanticNodeGroup.FUNCTION_SUM))));
                tr.appendChild(this.buildFunctionTd("SAMPLE", this.getFieldID(this.getFunctionFieldNumber(SemanticNodeGroup.FUNCTION_SAMPLE))));

                tr = document.createElement("tr");
                table.appendChild(tr);

                td = this.buildFunctionTd("GROUP_CONCAT", this.getFieldID(this.getFunctionFieldNumber(SemanticNodeGroup.FUNCTION_GROUP_CONCAT)));
                td.colSpan="3";
                tr.appendChild(td);

                return table;
            },

            buildFunctionTd : function(text, id) {
                td = document.createElement("td");
                td.appendChild(document.createTextNode(" "));
                td.appendChild(IIDXHelper.createVAlignedCheckbox(id, this.item.getFunctions().indexOf(text) > -1, "btn", this.functionCheckCallback.bind(this)));
                td.appendChild(document.createTextNode(" " + text + " "));
                return td;
            },

            getCheckedFunctions : function() {
                var functions = [];
                for (var f of SemanticNodeGroup.FUNCTION_LIST) {
                    if (this.isFunctionChecked(f)) {
                        functions.push(SemanticNodeGroup.getFunctionName(f));
                    }
                }
                return functions;
            },

            isFunctionChecked : function(f) {
                return document.getElementById(this.getFieldID(this.getFunctionFieldNumber(f))).checked;
            },

			// ------ manage unique id's ------
			// would be actually unique if there were GUID in the constructor.  overkill.
			getFieldID : function(n) {
				return "modalItemField_" + n;
			},

			getFieldValue : function(n) {
				return document.getElementById(this.getFieldID(n)).value;
			},

			getFieldElement : function(n) {
				return document.getElementById(this.getFieldID(n));
			},

			setFieldValue : function(n, val) {
				document.getElementById(this.getFieldID(n)).value = val;
			},
		};

		return ModalItemDialog;            // return the constructor
	}
);
