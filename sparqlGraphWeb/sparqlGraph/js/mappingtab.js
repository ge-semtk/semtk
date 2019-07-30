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
 *  MappingTab - the GUI elements for building an ImportSpec
 *
 *  Basic programmer notes:
 *     ondragover
 *         - preventDefault will allow a drop
 *         - stopPropagation will stop the question from being bubbled up to a parent.
 */

define([	// properly require.config'ed
        	'sparqlgraph/js/importcolumn',
         	'sparqlgraph/js/importcsvspec',
        	'sparqlgraph/js/mappingitem',
        	'sparqlgraph/js/importspec',
        	'sparqlgraph/js/importtext',
        	'sparqlgraph/js/importtrans',
        	'sparqlgraph/js/importmapping',
         	'sparqlgraph/js/modaliidx',
            'sparqlgraph/js/importmappingmodal',
         	'sparqlgraph/js/importtransformmodal',
         	'sparqlgraph/js/iidxhelper',

         	'jquery',

			// shimmed
         	//'logconfig',

		],

	function(ImportColumn, ImportCsvSpec, MappingItem, ImportSpec, ImportText, ImportTransform, ImportMapping, ModalIidx, ImportMappingModal, ImportTransformModal, IIDXHelper, $) {


		//============ local object  MappingTab =============
		var MappingTab = function(optionsdiv, canvasdiv, colsdiv, csvCallback, alertCallback) {
		    this.importCsvSpec = null;
		    this.importSpec = new ImportSpec(alertCallback);

		    this.optionsdiv = optionsdiv;
		    this.canvasDiv = canvasdiv;
		    this.rightDiv = colsdiv;

		    this.columnDiv = null;
		    this.colsToolDiv = null;
		    this.textDiv = null;
		    this.transformDiv = null;

		    this.csvDropzone = null;

		    this.changedFlag = false;   // WARNING:  not completely implemented / tested
		    this.csvFile = null;
		    this.csvCallback = csvCallback;
		    this.alertCallback = alertCallback;          //  alert(htmlMessage, optTitleText)
		    this.dragEvTargetId = null;
		    this.iSpecHash = {};     // iSpecHash[elem.id] = the ImportSpec object
		                            //   column names    before drag-and-drop - ImportColumn
		                            //   text names      before drag-and-drop - ImportText
		                            //
		                            //   transform names before and after drag-drop - ImportTransform
		                            //
		                            //   text or column  after drag and drop - MappingItems
                                    //   drop area for each row (table cells) - ImportRow
            this.buttonList = [];
		};


		MappingTab.PREFIX_TRANSFORM = "trans_";
		MappingTab.PREFIX_TRANSFORM_ITEM = "itrans_";
		MappingTab.PREFIX_COL = "col_";
		MappingTab.PREFIX_COL_ITEM = "icol_";
		MappingTab.PREFIX_TEXT = "txt_";
		MappingTab.PREFIX_TEXT_ITEM = "itxt_";
		MappingTab.PREFIX_ROW = "row_";

		MappingTab.classColumnUnused = "label label-inverse";
		MappingTab.classColumnUsed =   "label label-success";
		MappingTab.classTransformUnused = "label";
		MappingTab.classTransformUsed =   "label"; // same
		MappingTab.classTextUnused = "label label-info";
		MappingTab.classTextUsed =   "label label-info"; // same


		MappingTab.prototype = {
			clear : function () {
				this.importCsvSpec = null;

			    this.importSpec = new ImportSpec(this.importSpec.alertCallback);

			    this.uniqueIndex = 0;
                this.changedFlag = false;
			    this.iSpecHash = {};
                this.buttonList = [];
			},

			getCsvFile : function () {
				return this.csvFile;
			},

			getBaseURI : function () {
				var elem = document.getElementById("import_tab_uri_input");
				if (elem) {
					return elem.value;
				} else {
					return "";
				}
			},

			getUndeflatablePropItems : function () {
				return this.importSpec.getUndeflatablePropItems();
			},

			getUniqueIndexStr : function () {
				this.uniqueIndex += 1;
				return this.uniqueIndex.toString();
			},

			getChangedFlag : function() {
                return this.changedFlag;
            },

			setChangedFlag : function (bool) {
				this.changedFlag = bool;
			},

			draw : function() {
				kdlLogEvent("Import Tab Draw");

				// create an ImportSpec if there is none
				if (this.importCsvSpec == null) {
					this.importCsvSpec = new ImportCsvSpec();
				}

				// try to stop the default Firefox drop
				window.ondragover = function (e) { e.stopPropagation(); e.preventDefault();};
				window.ondrop     = function (e) { e.stopPropagation(); e.preventDefault();};

				// starting over
				this.iSpecHash = {};
                this.buttonList = [];

				// --- draw columns side (right) ---
				this.rightDiv.innerHTML = "";

				// tools div
				this.colsToolsDiv = document.createElement("div");
				this.rightDiv.appendChild(this.colsToolsDiv);
				this.colsToolsDiv.appendChild( this.createTrashCan() );
				this.colsToolsDiv.appendChild( this.createClearAllBut() );


				// cols list div
				this.columnDiv = document.createElement("div");
				this.columnDiv.style = "border: 1px solid gray; padding: 1em;";
				this.columnDiv.style.maxHeight = "20em";
				this.columnDiv.style.overflow = "auto";

				// cols list header
				var header = document.createElement("h3");
				header.innerHTML = "Columns:";
				this.columnDiv.appendChild(header);
				this.csvDropzone = IIDXHelper.createDropzone("icon-table",
										"",
										this.isDroppableFile.bind(this),
										this.ondropCsvFile.bind(this)
									);
				this.columnDiv.appendChild( this.csvDropzone );
				if (this.csvFile !== null) {
					IIDXHelper.setDropzoneLabel(this.csvDropzone, this.csvFile.name, IIDXHelper.DROPZONE_FULL);
				} else {
					IIDXHelper.setDropzoneLabel(this.csvDropzone, "Drop CSV file", IIDXHelper.DROPZONE_EMPTY);
				}

				// cols list
				var colList = this.importSpec.getSortedColumns();
				for (var i=0; i < colList.length; i++) {
					var span = this.createColElem(colList[i], false);

					this.columnDiv.appendChild(span);
					this.columnDiv.appendChild(document.createElement("br"));
				}

				this.rightDiv.appendChild(this.columnDiv);

				// text div
				this.textDiv = document.createElement("div");
				this.textDiv.style = "border: 1px solid gray; padding: 1em;";

				var header2 = document.createElement("h3");
				header2.innerHTML = "Text: ";
				header2.appendChild(this.createTextNewBut());
				this.textDiv.appendChild(header2);

				// text list
				var textList = this.importSpec.getSortedTexts();
				for (var i=0; i < textList.length; i++) {
					var elem = this.createTextElem(textList[i], false);

					this.textDiv.appendChild(elem);
					this.textDiv.appendChild(document.createElement("br"));
				}

				this.rightDiv.appendChild(this.textDiv);

				// transform div
				this.transformDiv = document.createElement("div");
				this.transformDiv.style = "border: 1px solid gray; padding: 1em;";

				var header3 = document.createElement("h3");
				header3.innerHTML = "Transforms: ";
				header3.appendChild(this.createTransformNewBut());
				this.transformDiv.appendChild(header3);


				// transform list
				var transList = this.importSpec.getSortedTransforms();
				for (var i=0; i < transList.length; i++) {
					var elem = this.createTransformElem(transList[i], false);

					// put it on the transform div
					this.transformDiv.appendChild(document.createTextNode(" "));
					this.transformDiv.appendChild(elem);
				}

				this.rightDiv.appendChild(this.transformDiv);

				// --- draw options section ---
				this.optionsdiv.innerHTML = "";

				// baseURI
				var form = IIDXHelper.buildHorizontalForm();
				var inputURI;
				inputURI = document.createElement("input");
				inputURI.id = "import_tab_uri_input";
				inputURI.type = "text";
				inputURI.class = "input-large";
				inputURI.value = this.importSpec.getBaseURI();
				inputURI.onchange = function (e) {
					var val = document.getElementById("import_tab_uri_input").value;
					this.importSpec.setBaseURI(val);
				}.bind(this);

				var group = IIDXHelper.buildControlGroup("Base URI: ", inputURI);
				form.appendChild(group);
				this.optionsdiv.appendChild(form);

				// --- draw canvas side (left) ---
				this.canvasDiv.innerHTML = "";

				if (this.importSpec.nodegroup == null || this.importSpec.nodegroup.getNodeCount() == 0) {
					// no nodegroup
					var alertDiv = document.createElement("div");
					alertDiv.classList.add("alert");
					alertDiv.classList.add("alert-warning");
					alertDiv.style.margin = "10px";
					alertDiv.innerHTML = "<h3>Nodegroup is empty.</h3><p>Use the Query tab to specify classes which will be mapped to input data.";
					this.canvasDiv.appendChild(alertDiv);

				} else {
					var table = this.createCanvasTable();
					this.importSpec.updateEmptyUriLookupModes();
					// rows
					var rowList = this.importSpec.getSortedMappings();
					for (var i=0; i < rowList.length; i++) {
						this.drawMappingRow(table, rowList[i]);
					}

					this.canvasDiv.appendChild(table);

					// usage style changes
					this.updateUseStyles();
				}

                this.validateRows();
                this.updateAllUriLookupButtons();
			},

            getUriLookupButton : function(rowElem) {
                return rowElem.parentElement.children[1].children[0];
            },

			createClearAllBut : function () {
				// create the "New" button for the text div
				var elem = document.createElement("button");
				elem.style.marginBottom = "0.5em";
				elem.style.marginLeft = "0.5em";

				elem.innerHTML = "<i class='icon-remove'></i> Clear";
				elem.onclick = this.clearAll.bind(this);
				return elem;
			},

			clearAll : function () {

				var doClearAll = function() {
					this.importSpec.clearMost();
					this.draw();
				};

				// confirmation dialog
				ModalIidx.okCancel("Clear Import Template",
									"Do you want to clear the import template?",

									doClearAll.bind(this),

									"Go Ahead");
			},

            // initial draw of a mapping row
			drawMappingRow : function (table, iMapping) {
				// append one item row to an HTML table
				var row = table.insertRow(-1);
				var cell = null;

                // COL  Name: if URI label with sparqlID, else use property keyname
				cell = row.insertCell(-1);
                if (iMapping.isNode()) {
					cell.innerHTML = "<b>" + iMapping.getName() + "</b>";
				} else {
					cell.innerHTML = iMapping.getName();
				}

                cell = row.insertCell(-1);

                // create mapping dialog button
                var but = IIDXHelper.createIconButton("icon-tasks", function(){});
                but.id = IIDXHelper.getNextId("but");
                this.iSpecHash[but.id] = iMapping;
                this.buttonList.push(but);

                this.updateUriLookupButton(but, iMapping);
                cell.appendChild(but);

                // button onclick launches dialog
                but.onclick = function(mapping, ispec, cb) {
                    var m = new ImportMappingModal(mapping, ispec, cb);
                    m.launch();
                }.bind(this,
                       iMapping,
                       this.importSpec,
                       this.updateAllUriLookupButtons.bind(this, iMapping));

                // COL area for dropping things into
				cell = row.insertCell(-1);
				cell.id = MappingTab.PREFIX_ROW + this.getUniqueIndexStr();
				this.iSpecHash[cell.id] = iMapping;

				cell.style="border: 1px solid #aaaaaa";
				cell.innerHTML="";
				cell.ondragover = 	function (ev) {

					if (this.isUriRowDroppable(ev)) {
						ev.preventDefault();
						ev.stopPropagation();
					} else {
						return false;
					}

				}.bind(this);

				cell.ondrop = this.ondropUriRow.bind(this);

                // put in any Item elements that already exist
				var itemList = iMapping.getItemList();
				for (var i=0; i < itemList.length; i++) {
					var elem = this.createItemElem(itemList[i]);
					cell.appendChild(elem);
				}

                this.setRowHelpBlock(cell);
			},

            setRowHelpBlock : function(rowCell) {

                // return if this isn't a URI (node) row
                var mapping = this.iSpecHash[rowCell.id];
                if (!mapping || ! mapping.isNode()) {
                    return;
                }

                // add help-block to any empty row
                if (rowCell.childElementCount == 0) {
                    var s = document.createElement("span");
                    s.innerHTML = "--Generate UUID--";
                    s.classList.add("help-block");
                    rowCell.appendChild(s);

                // remove help-block from any non-empty row
                } else if (rowCell.childElementCount > 1) {
                    for (var i=0; i < rowCell.childElementCount; i++) {
                        if (rowCell.children[i].classList.contains("help-block")) {
                            rowCell.removeChild(rowCell.children[i]);
                        }
                    }
                }

            },

            // Either an error or dragging to garbage
            // could have caused a URI lookup with no mapping items.
            // Remove these.
            removeUriLookupsWithNoItems : function() {
                for (var i=0; i < this.buttonList.length; i++) {
                    var but = this.buttonList[i];
                    var map = this.iSpecHash[but.id];

                    // remove URI Lookup if there is no mapping
                    // TODO do we need a dialog here
                    if (map.getItemList().length == 0 && map.getUriLookupNodes().length > 0) {
                        map.setUriLookupNodes([]);
                    }
                }
            },

            updateAllUriLookupButtons : function() {

                // now update all the buttons
                for (var i=0; i < this.buttonList.length; i++) {
                    var but = this.buttonList[i];
                    var map = this.iSpecHash[but.id];

                    this.updateUriLookupButton(but, map);
                }
            },

            // enable button if
            //     - URILookup fields are used, or
            //     - there are mapping items (so the URILookup fields COULD be set)
            //
            // make the mapping button "primary" if any URILookup field has been set set
            updateUriLookupButton : function(button, mapping) {
                var disable = false;
                if (    mapping.getUriLookupNodes().length > 0 ||
                        this.importSpec.getLookupMappings(mapping).length > 0) {
                    button.classList.add("btn-primary");
                    button.disabled = false;
                } else {
                    button.classList.remove("btn-primary");
                    disable = (mapping.getItemList().length == 0);
                    button.disabled = disable;
                }

                // Set the hover title
                if (disable) {
                    if (mapping.isNode()) {
                        button.title = "Add mapping items or set a field to look up this URI first";
                    } else {
                        button.title = "Add mapping items first";
                    }
                } else {
                    button.value = null;
                }
            },

            // get the GUI string for the URI mode
            getUriLookupModeStr : function(iMapping) {
                var mode = iMapping.getUriLookupMode();
                var ret = "";
                var itemCount = iMapping.getItemList().length;

                if (mode == ImportMapping.LOOKUP_MODE_NO_CREATE)   { ret = "Lookup"; }
                else if (mode == ImportMapping.LOOKUP_MODE_CREATE) { ret = "Lookup/Add"; }
                else if (itemCount > 0)                            { ret = "Ingest:"; }
                else                                               { ret = "UUID"; }

                return ret;
            },

            checkUriLookupErrors() {

            },

			createItemElem : function (item) {
				// create an element from an importSpec item

				if (item.getType() === MappingItem.TYPE_TEXT) {
					var textElem = this.createTextElem(item.getTextObj(), true);
					this.iSpecHash[textElem.id] = item;   // overwrite so element points to MappingItem not ImportText
					return textElem;

				} else {
					var colElem = this.createColElem(item.getColumnObj(), true);
					this.iSpecHash[colElem.id] = item;   // overwrite so element points to MappingItem not ImportColumn

					var tList = item.getTransformList();
					// add the transform
					if (tList != null) {
						for (var i=0; i < tList.length; i++) {
							var transElem = this.createTransformElem(tList[i], true);
							colElem.appendChild(transElem);
						}
					}
					return colElem;
				}
			},

			createTextNewBut : function () {
				// create the "New" button for the text div
				var elem = document.createElement("button");

				elem.innerHTML = elem.innerHTML = "<i class='icon-plus'></i> New";
				elem.onclick = this.launchTextModal.bind(this, null);
				return elem;
			},

			launchTextModal : function (textElem) {
				// launch a modal to edit a transformation
				// transElem may be NULL if this is a new transform that doesn't exist yet
				var m = new ModalIidx();

				var body = document.createElement("div");

				var nameForm = IIDXHelper.buildInlineForm();
				body.appendChild(nameForm);

				var elem;
				elem = document.createElement("label");
				elem.innerHTML = "Text: ";
				nameForm.appendChild(elem);

				elem = document.createElement("input");
				elem.id = "modal_text";
				elem.type = "text";
				elem.class = "input-small";
				elem.value = (textElem != null) ? this.iSpecHash[textElem.id].getText() : "";
				nameForm.appendChild(elem);

                table = document.createElement("table");
                body.appendChild(table);
                table.classList.add("table");
                table.classList.add("table-bordered");
                IIDXHelper.tableAddRow(table, ["special value", "effect"]);
                IIDXHelper.tableAddRow(table, ["<button onclick='document.getElementById(\"modal_text\").value=\"%ingestTime\"'>%ingestTime</button>", "Insert local datetime of ingestion"]);

				var modalValidate = function() {
					var text = document.getElementById("modal_text").value;
					if (text == null || text.length == 0) {
						return "Text can not be empty";
					} else {
						return null;
					}
				};

				var modalCallback = function(textElem) {
					var text = document.getElementById("modal_text").value;
					if (textElem == null) {
						textElem = this.createText(text);
					}
					this.textModalCallback(textElem, text);
				};

				var cancel = function() {};

				m.showOKCancel("Edit Text", body, modalValidate.bind(this), modalCallback.bind(this, textElem), cancel);
			},

			createText : function (text) {
				// PEC TODO: works differently from createTransform and createWhatElse?
				// create new text:
				//     in importSpec
				//     and new element on textDiv

				// create an HTML element
				var iText = new ImportText(text);
				this.importSpec.addText(iText);
				var e = this.createTextElem(iText, false);

				// put it on the text div
				this.textDiv.appendChild(document.createTextNode(" "));
				this.textDiv.appendChild(e);

				return e;
			},

			createTextElem : function (iText, copyFlag) {
				// create a text element

				var elem = document.createElement("span");

				if (copyFlag) {
					elem.id = MappingTab.PREFIX_TEXT_ITEM + this.getUniqueIndexStr();
				} else {
					elem.id = MappingTab.PREFIX_TEXT + this.getUniqueIndexStr();
				}
				this.iSpecHash[elem.id] = iText;

				elem.className = MappingTab.classTextUnused;
				elem.innerHTML = iText.getText();

				// draggable
				elem.style.cursor = "grab";
				elem.style.verticalAlign = "text-bottom";
				elem.style.marginLeft="0.5em";

				elem.draggable = true;
				elem.ondragstart = this.dragStart.bind(this);
				elem.ondragend = this.dragEnd.bind(this);

				if (copyFlag) {
					// you can drop things onto a copy
					elem.ondragover = 	function (ev) {

											if (this.isTextDroppable(ev)) {
												ev.preventDefault();
												ev.stopPropagation();
											} else {
												return true;  // prevent the Uri row drop
											}
										}.bind(this);
					elem.ondrop = this.ondropTextItem.bind(this);
					elem.onclick = function(){};
				} else {
					// you can edit the originals
					elem.onclick = this.launchTextModal.bind(this, elem);
				}

				return elem;
			},

			createTransform : function (iTrans) {
				// process a newly created transform:
				//     in importSpec
				//     and new element on transformDiv

				// create an HTML element

				this.importSpec.addTransform(iTrans);
				var e = this.createTransformElem(iTrans, false);

				// put it on the trans div
				this.transformDiv.appendChild(document.createTextNode(" "));
				this.transformDiv.appendChild(e);

				return e;
			},

			createTransformElem : function (iTrans, isCopyFlag) {
				// create a label containing a transform name
				// it can be dragged around, etc.
				var span = document.createElement("span");

				var name = iTrans.getName();

				// unique id
				if (isCopyFlag) {
					span.id = MappingTab.PREFIX_TRANSFORM_ITEM;
				} else {
					span.id = MappingTab.PREFIX_TRANSFORM;
				}
				span.id += name.replace(/[^A-Za-z0-9]/g,"_") + this.getUniqueIndexStr();
				this.iSpecHash[span.id] = iTrans;

				// appearance
				span.className = MappingTab.classTransformUnused;
				span.innerHTML = name;

				// draggable
				span.style.cursor = "grab";
				span.style.verticalAlign = "text-bottom";
				span.style.marginLeft="0.5em";
				span.draggable = true;
				span.ondragstart = this.dragStart.bind(this);
				span.ondragend = this.dragEnd.bind(this);


				// drop
				span.ondragover = 	function (ev) {

										if (this.isTransformDroppable(ev)) {
											ev.preventDefault();
											ev.stopPropagation();
										} else {
											return true;
										}
									}.bind(this);

				span.ondrop = this.ondropTransformItem.bind(this);



				if (isCopyFlag) {
					// copies are not editable
					span.onclick = function(){};
				} else {
					// originals are editable
					span.onclick = function (ev) { this.launchTransformModal(ev.target); }.bind(this);
				}

				return span;
			},
			createTransformNewBut : function () {
				// create the "New" button for the transform div
				var elem = document.createElement("button");

				elem.innerHTML = "<i class='icon-plus'></i> New";
				elem.onclick = function (ev) {
					this.launchTransformModal(null);
				}.bind(this);
				return elem;
			},

			launchTransformModal : function (transElem) {
				// get iTrans
				var iTrans = null;
				if (transElem) {
					iTrans = this.iSpecHash[transElem.id];
					kdlLogEvent("Import Tab Edit Transform", "name", iTrans.getName());
				} else {
					kdlLogEvent("Import Tab Create Transform");
				}

				// launch modal
				var m = new ImportTransformModal(iTrans);
				m.show(this.transformModalCallback.bind(this, transElem));
			},

			transformModalCallback : function (transElem, iTrans) {
				if (transElem == null) {
					transElem = this.createTransform(iTrans);
				}

				// set main element
				var name = iTrans.getName();
				transElem.innerHTML = name;

				// change names on any elements already in use
				this.visitTransformInItemElements(iTrans, function (elem) { elem.innerHTML = name; });

				kdlLogEvent("Import Tab Edit Transform OK");
			},

			load : function (nodegroup, importJson) {
				this.clear();

				this.importSpec.updateNodegroup(nodegroup);

				if (importJson !== null) {
					this.importSpec.fromJson(importJson);
				}
			},

			updateNodegroup : function (nodegroup) {
				this.importSpec.updateNodegroup(nodegroup);
				this.draw();
			},

			updateUseStyles : function (optIObj) {
				// search the screen for anything that changes color based on usage.
				// if optIObj then only do that object, otherwise do them all.
				// check usage and switch the class.

				var iObj = (typeof optIObj === 'undefined' ? null : optIObj);

				// columns
				var children = this.columnDiv.childNodes;
				for (var i=0; i < children.length; i++) {
					if (children[i].id in this.iSpecHash) {
						var elemIObj = this.iSpecHash[children[i].id];
						if (iObj == null || elemIObj == iObj) {
							if (elemIObj.getUse() > 0) {
								children[i].className = MappingTab.classColumnUsed;
							} else {
								children[i].className = MappingTab.classColumnUnused;
							}
							if (iObj !== null) {return;}
						}
					}
				}
				// text
				children = this.textDiv.childNodes;
				for (var i=0; i < children.length; i++) {
					if (children[i].id in this.iSpecHash) {
						var elemIObj = this.iSpecHash[children[i].id];
						if (iObj == null || elemIObj == iObj) {
							if (elemIObj.getUse() > 0) {
								children[i].className = MappingTab.classTextUsed;
							} else {
								children[i].className = MappingTab.classTextUnused;
							}
							if (iObj !== null) {return;}
						}
					}
				}
				// transforms
				children = this.transformDiv.childNodes;
				for (var i=0; i < children.length; i++) {
					if (children[i].id in this.iSpecHash) {
						var elemIObj = this.iSpecHash[children[i].id];
						if (iObj == null || elemIObj == iObj) {
							if (elemIObj.getUse() > 0) {
								children[i].className = MappingTab.classTransformUsed;
							} else {
								children[i].className = MappingTab.classTransformUnused;
							}
							if (iObj !== null) {return;}
						}
					}
				}
			},

			loadCsvCallback : function () {
				kdlLogEvent("Import Tab Load CSV success");

				// confusing things might have happened, so simply redraw from scratch
				this.draw();
			},

			ondropCsvFile : function(ev) {
				if (ev.dataTransfer.files.length != 1) {
					this.alertCallback("Only single file drops are supported.");

				} else if (ev.dataTransfer.files[0].name.slice(-4).toLowerCase() != ".csv") {
					this.alertCallback("Only CSV file drops are supported.");

					// PEC TODO:  added 'true' so there is no confirmation message
				} else if (this.importSpec.getNumColumns() == 0 || true || confirm("Do you want to overwrite unsaved column names?")){
					this.importSpec.updateColsFromCsvAsync(ev.dataTransfer.files[0], this.loadCsvCallback.bind(this));
					this.csvFile = ev.dataTransfer.files[0];
					IIDXHelper.setDropzoneLabel(this.csvDropzone, this.csvFile.name, IIDXHelper.DROPZONE_FULL);
					this.csvCallback(this.csvFile);
					this.changedFlag = false;
				}
				IIDXHelper.DROPZONE_FULL = 1;
				IIDXHelper.DROPZONE_EMPTY = 2;
			},


			createTrashCan : function () {
				var elem = document.createElement("icon");
				elem.id = "trashCan";
				elem.className = "icon-trash";
				elem.style = "font-size: 2.5em; color: gray";

				// dragover changes color.  dropping or dragging out restores color.
				elem.ondrop =  		function (ev) {

										if (this.isTrashable(ev)) {
											this.ondropTrash(ev);
											ev.currentTarget.style.color = "gray";
										}
										ev.stopPropagation();
							   		}.bind(this);

				elem.ondragover = 	function (ev) {
										if (this.isTrashable(ev)) {
											ev.target.style.color = "blue";
											ev.preventDefault();
										}
										ev.stopPropagation(); // don't ask parents to drop
									}.bind(this);

				elem.ondragleave = 	function (ev) {
										ev.target.style.color = "gray";
									};
				return elem;
			},


			textModalCallback : function (textElem, text) {
				// edit a text button from this.textDiv
				kdlLogEvent("Import Tab Edit Text Success", "text", text);

				this.setChangedFlag(true);

				// set the text button
				textElem.innerHTML = text;
				var iText = this.iSpecHash[textElem.id];

				// set the ImportText value
				iText.setText(text);

				// fix all text items over in rows so they show the new name
				this.visitItemElements(iText, function(elem) {elem.innerHTML = text;});
			},

			visitItemElements : function (iTextOrCol, applyFunc) {
				// iTextOrCol is ImportText or ImportColumn
				// visit each itemElement that hashes to an MappingItem who's value matches iTextOrCol
				// apply applyFunc(elem) to each html element
				var numApplied = 0;

				for (var elemId in this.iSpecHash) {

					// find all matching items
					var iItem = this.iSpecHash[elemId];
					if (this.idIsTextItem(elemId) || this.idIsColumnItem(elemId)) {
						if (iItem.getColumnOrTextObj() == iTextOrCol) {
							applyFunc( document.getElementById(elemId) );
							numApplied += 1;
						}
					}

					// quit as soon as we've changed them all
					if (numApplied == iTextOrCol.getUse()) {
						return;
					}
				}
			},

			visitTransformInItemElements : function (iTrans, applyFunc) {
				// iTrans is ImportTransform
				// visit each element that hashes to iTrans from the URIRows
				// and apply applyFunc(elem) to each html element

				var numApplied = 0;
				for (var elemId in this.iSpecHash) {

					// find all matching items
					if (this.idIsTransformInItem(elemId)) {
						if (this.iSpecHash[elemId] == iTrans) {
							applyFunc( document.getElementById(elemId) );
							numApplied += 1;

							if (numApplied == iTrans.getUse()) {
								return;
							}
						}
					}
				}
			},



			createColElem : function (iCol, isCopyFlag) {
				// create a label containing a column name
				// it can be dragged around
				var span = document.createElement("span");

				// unique id
				if (isCopyFlag) {
					span.id = MappingTab.PREFIX_COL_ITEM + iCol.getColName().replace(/[^A-Za-z0-9]/g,"_") + this.getUniqueIndexStr();
				} else {
					span.id = MappingTab.PREFIX_COL + iCol.getColName().replace(/[^A-Za-z0-9]/g,"_") + this.getUniqueIndexStr();
				}
				this.iSpecHash[span.id] = iCol;

				// appearance
				span.className = MappingTab.classColumnUnused;
				span.innerHTML = iCol.getColName();

				// draggable
				span.style.cursor = "grab";
				span.style.verticalAlign = "text-bottom";
				span.style.marginLeft="0.5em";
				span.draggable = true;
				span.ondragstart = this.dragStart.bind(this);
				span.ondragend = this.dragEnd.bind(this);


				// drop
				if (isCopyFlag) {

					span.ondragover = 	function (ev) {

											if (this.isColDroppable(ev)) {
												ev.preventDefault();
												ev.stopPropagation();
											} else {
												return true;  // prevent the Uri row drop
											}
										}.bind(this);
					span.ondrop = this.ondropColItem.bind(this);
				}

				return span;
			},

			createCanvasTable : function () {
				// create table
				var table = document.createElement("table");
				table.id = "canvas-table";
                table.style.marginRight="1ch";

				// insert an empty header just to set widths
				var headerRow = table.insertRow(-1);

				//  col0:  name
				var cell = headerRow.insertCell(-1);
                cell.style.width="20%";

				// col1  button
				var cell = headerRow.insertCell(-1);
				cell.style+=";white-space: nowrap;";

                //  col2 drop area
				cell = headerRow.insertCell(-1);
				cell.width="100%";

				return table;
			},

			dragStart : function (ev) {
				console.log("drag start " + ev.target.id);
			    ev.dataTransfer.setData("elementId", ev.target.id);
			    this.dragEvTargetId = ev.target.id;
			},

			dragEnd : function (ev) {
				console.log("drag end " + ev.target.id);
				this.dragEvTargetId = null;
			},


			prepDropElement : function (dragElem) {
				// when something is about to be dropped,
				//    - if we're dropping a copy: create elem and item.   Set this.iSpecHash[elem.id] = item
				//    - delete from html parent element if appropriate
				//    - remove hashed item from it's spot in ImportSpec, if appropriate
				//
				// RETURN:  the new elem or dragElem
				//          this.iSpecHash[ret.id] = any new ImportSpec item

				var dropElem = dragElem;

				if (dragElem.parentNode == this.columnDiv) {
			    	//====  pulling in a column from cols list ====
					var iCol = this.iSpecHash[dragElem.id];
					var item = new MappingItem(MappingItem.TYPE_COLUMN, iCol, []);
				    dropElem = this.createItemElem(item);

			    } else if (dragElem.parentNode == this.textDiv) {
			    	//====  pulling in a transform from transform list ====
			    	var iText = this.iSpecHash[dragElem.id];
					var item = new MappingItem(MappingItem.TYPE_TEXT, iText, []);
				    dropElem = this.createItemElem(item);

			    } else if (dragElem.parentNode == this.transformDiv) {
			    	//====  pulling in a transform from transform list ====
			    	var trans = this.iSpecHash[dragElem.id];
				    dropElem = this.createTransformElem(trans, true);

			    } else {
			    	//==== if it came from a different row, move it ====

			    	// remove element from its old position in the importSpec
			    	if (this.elemIsTextItem(dragElem) || this.elemIsColumnItem(dragElem)) {
			    		// this is a col or text: get the row item and remove it
			    		var iMapping = this.iSpecHash[dragElem.parentNode.id];
			    		var item = this.iSpecHash[dragElem.id];
			    		iMapping.delItem(item);

			    	} else if (this.elemIsTransformInItem(dragElem)) {
			    		// this is a trans item, so remove it from col
			    		var iColItem = this.iSpecHash[dragElem.parentNode.id];
			    		var iTrans = this.iSpecHash[dragElem.id];
			    		iColItem.delTransform(iTrans);
			    	}

			    	// remove from html parent
                    var formerParent = dragElem.parentNode;
			    	formerParent.removeChild(dragElem);
                    this.setRowHelpBlock(formerParent);

			    	// drop item is this element
			    	dropElem = dragElem;
			    }

				return dropElem;
			},

			// =========  ondrop() callbacks for each target ============

			ondropUriRow : function (ev) {

				 var id = this.dragEvTargetId;   // local element ID
				 if (id == null) { return; }

				 var dragElem = document.getElementById(id);
				 var rowElem = ev.currentTarget;
				 var dropElem = this.prepDropElement(dragElem);
				 var dropItem = this.iSpecHash[dropElem.id];

				 var insertBeforeElem = null;
				 var insertBeforeIObj = null;


				 // if event bubbled to here, find the column or text item target it bubbled through
				 if (ev.target != rowElem && ! ev.target.classList.contains("help-block")) {
					 insertBeforeElem = ev.target;
					 while (!this.elemIsColumnItem(insertBeforeElem) && !this.elemIsTextItem(insertBeforeElem)) {
						 insertBeforeElem = insertBeforeElem.parentNode;
						 if (insertBeforeElem == rowElem) { throw "MappingTab.ondropUriRow() internal error."; }
					 }
					 insertBeforeIObj = this.iSpecHash[insertBeforeElem.id];
				 }

				 // Transforms can't be dropped on UriRows.  Everything else: go for it
				 if (! this.elemIsTransform(dragElem)) {

					 // do html
					 rowElem.insertBefore(dropElem, insertBeforeElem);
                     this.setRowHelpBlock(rowElem);

					 // insert in right spot in importSpec
					 var iMapping = this.iSpecHash[rowElem.id];
					 iMapping.addItem(dropItem, insertBeforeIObj);
                     this.validateRows();
                     this.updateAllUriLookupButtons();
				     this.updateUseStyles(dropItem.getColumnOrTextObj());
				     this.setChangedFlag(true);
				 }

				// halt further actions
				ev.preventDefault();
				ev.stopPropagation();
			},

			ondropTextItem : function (ev) {
				var id = this.dragEvTargetId;   // local element ID
				if (id == null) { return; }

				var elem = document.getElementById(id);
				var insertBefore = ev.currentTarget;


				// if this isn't a little wiggle that HTML5 reports as dropping element on itsself
				if (elem.id == insertBefore.id) {
					ev.preventDefault();
					ev.stopPropagation();
				}
				// else propagate.   Nothing actually drops on a text

			},

			ondropColItem : function (ev) {
				var id = this.dragEvTargetId;   // local element ID
				if (id == null) { return; }

				var dragElem = document.getElementById(id);
				var targetElem = ev.currentTarget;

				if (dragElem.id == targetElem.id) {
					// ignore little wiggle that html5 interprets as item being dropped on itsself
					ev.preventDefault();
					ev.stopPropagation();
				}
				else if (this.elemIsTransform(dragElem) || this.elemIsTransformInItem(dragElem)) {
					var dropElem = this.prepDropElement(dragElem);
					targetElem.appendChild(dropElem);

					// insert in right spot in importSpec
					var item = this.iSpecHash[targetElem.id];
					var iTrans = this.iSpecHash[dragElem.id];
					item.addTransform(iTrans, null);

					// halt further actions
					ev.preventDefault();
					ev.stopPropagation();
				}
				// else propagate

			},

            // Dropping something onto a transform
			ondropTransformItem : function (ev) {
				// only transforms drop on transforms, so dragElem and targetElem are both transforms


				var id = this.dragEvTargetId;   // local element ID
				if (id == null) { return; }

				var dragElem = document.getElementById(id);
				var targetElem = ev.currentTarget;
				var colElem = targetElem.parentNode;

				if (dragElem.id == targetElem.id) {
					// ignore a wiggle that HTML5 interprets as item dropped on itsself
					ev.preventDefault();
					ev.stopPropagation();

					// process transform drag items
				} else if (this.idIsTransform(dragElem.id) || this.idIsTransformInItem(dragElem.id)) {

					var dropElem = this.prepDropElement(dragElem);

					// do html
					var insertBefore = targetElem;
					colElem.insertBefore(dropElem, insertBefore);

					// insert in right spot in importSpec
					var iCol = this.iSpecHash[colElem.id];
					var item = this.iSpecHash[dropElem.id];
					var beforeItem = this.iSpecHash[insertBefore.id];
					iCol.addTransform(item, beforeItem);

					ev.preventDefault();
					ev.stopPropagation();

				} else {
					// else propagate
				}

			},

			ondropTrash : function (ev) {
			    var id = this.dragEvTargetId;
			    if (id == null) { return; }

			    var dragElem = document.getElementById(id);
			    var dragParent = dragElem.parentNode;


			    // update ImportSpec
			    if (this.elemIsColumnItem(dragElem) || this.elemIsTextItem(dragElem)) {
			    	// update iSpec
			    	var item = this.iSpecHash[dragElem.id];
			    	var iMapping = this.iSpecHash[dragParent.id];
			    	iMapping.delItem(item);

			    	// manage appearance
			    	var iObj = item.getColumnOrTextObj();
			    	this.updateUseStyles(iObj);

			    } else if (this.elemIsTransformInItem(dragElem)) {
			    	// update iSpec
			    	var iColItem = this.iSpecHash[dragParent.id];
			    	var iTrans = this.iSpecHash[dragElem.id];
			    	iColItem.delTransform(iTrans);

			    } else if (this.elemIsTransform(dragElem)) {
			    	var iTrans = this.iSpecHash[dragElem.id];
			    	this.importSpec.delTransform(iTrans);

			    } else if (this.elemIsText(dragElem)) {
			    	var iText = this.iSpecHash[dragElem.id];
			    	this.importSpec.delText(iText);
			    }

                // remove from html parent
                var formerParent = dragElem.parentNode;
                formerParent.removeChild(dragElem);
                this.setRowHelpBlock(formerParent);

			    // update iSpecHash
			    delete this.iSpecHash[dragElem.id];

			    // House-keeping
			    this.setChangedFlag(true);
			    this.validateRows();
                this.updateAllUriLookupButtons();

			    // halt further actions
			    ev.currentTarget.style.cursor = "default";
			    ev.preventDefault();
			    ev.stopPropagation();
			},

			// ======= what is droppable on what =======
			isUriRowDroppable: function (ev) {
				// console.log("isUriRowDroppable ev.target.id=" + ev.target.id + " this.dragEvTargetId=" + this.dragEvTargetId);
				// if dragged thing is col or text or textTool
				var id = this.dragEvTargetId;
				if (id) {
					var elem = document.getElementById(id);
					// new text, any col item, any text item,  NOT transforms
					if (this.elemIsColumn(elem) || this.elemIsColumnItem(elem) || this.elemIsText(elem) || this.elemIsTextItem(elem)) {
						return true;
					}
				}
				return false;
			},

			isColDroppable: function (ev) {
				var id = this.dragEvTargetId;
				if (id == null) { return false; }

				var elem = document.getElementById(id);
				// new text, any col item, any text item, any transform
				if (    this.elemIsColumn(elem)     ||
						this.elemIsColumnItem(elem) ||
						this.elemIsText(elem)       ||
						this.elemIsTextItem(elem)   ||
						this.elemIsTransform(elem)  ||
						this.elemIsTransformInItem(elem)    ) {
					return true;
				}

				return false;
			},

			isTextDroppable: function (ev) {
				return this.isUriRowDroppable(ev);
			},

			isTransformDroppable: function (ev) {
				return (this.isColDroppable(ev));
			},

			isDroppableFile : function (ev) {
				var id = this.dragEvTargetId;
				if (id) {
					// if we gave it an elementId then it isn't external
					return false;
				} else {
					// seems like there isn't enough info in ev.dataTransfer.files
					// to determine.  So be generous and handle problems on the drop.
					return true;

				}
			},

			isTrashable : function (ev) {
				//
				var id = this.dragEvTargetId;
				if (id) {

					var elem = document.getElementById(id);

					// all items are trashable
					if (this.elemIsColumnItem(elem) || this.elemIsTextItem(elem) || this.elemIsTransformInItem(elem)) {
						return true;
					}

					// unused Transforms
					else if (this.elemIsTransform(elem) && this.iSpecHash[id].getUse() == 0) {
						return true;
					}

					// unused Text
					else if (this.elemIsText(elem) && this.iSpecHash[id].getUse() == 0) {
						return true;
					}
				}
				return false;
			},

			// ==== element types ====
			// text hashes to ImportText
			elemIsText :          function (elem) { return this.elemIdStartsWith(elem.id, MappingTab.PREFIX_TEXT); },
			// text in an item hashes to MappingItem
			elemIsTextItem :      function (elem) { return this.elemIdStartsWith(elem.id, MappingTab.PREFIX_TEXT_ITEM); },
			// column hashes to ImportColumn
			elemIsColumn :        function (elem) { return this.elemIdStartsWith(elem.id, MappingTab.PREFIX_COL); },
			// column in an item hashes to Import Item
			elemIsColumnItem :    function (elem) { return this.elemIdStartsWith(elem.id, MappingTab.PREFIX_COL_ITEM); },
			// transform hashes to ImportTransform
			elemIsTransform :     function (elem) { return this.elemIdStartsWith(elem.id, MappingTab.PREFIX_TRANSFORM); },
			// transform in an item also hashes to ImportTransform
			elemIsTransformInItem : function (elem) { return this.elemIdStartsWith(elem.id, MappingTab.PREFIX_TRANSFORM_ITEM); },
			// row hashes to ImportMapping
			elemIsRow :           function (elem) { return this.elemIdStartsWith(elem.id, MappingTab.PREFIX_ROW); },

			// === ID versions ===
			idIsText :          function (id) { return this.elemIdStartsWith(id, MappingTab.PREFIX_TEXT); },
			idIsTextItem :      function (id) { return this.elemIdStartsWith(id, MappingTab.PREFIX_TEXT_ITEM); },
			idIsColumn :        function (id) { return this.elemIdStartsWith(id, MappingTab.PREFIX_COL); },
			idIsColumnItem :    function (id) { return this.elemIdStartsWith(id, MappingTab.PREFIX_COL_ITEM); },
			idIsTransform :     function (id) { return this.elemIdStartsWith(id, MappingTab.PREFIX_TRANSFORM); },
			idIsTransformInItem : function (id) { return this.elemIdStartsWith(id, MappingTab.PREFIX_TRANSFORM_ITEM); },
			idIsRow :           function (id) { return this.elemIdStartsWith(id, MappingTab.PREFIX_ROW); },


			elemIdStartsWith : function (id, prefix) {
				return id.slice(0, prefix.length) == prefix;
			},

			//==== other stuff ====

			setImportCsvSpec : function (importCsvSpec) {
				this.importCsvSpec = importCsvSpec;
			},

			isSpecLoaded : function () {
				return (this.importCsvSpec != null);
			},

			validateRows: function () {
                this.removeUriLookupsWithNoItems();
                this.importSpec.updateEmptyUriLookupModes();
			},

            toJson : function (optDeflateFlag) {
				var deflateFlag = (typeof optDeflateFlag == "undefined") ? false : optDeflateFlag;

                return this.importSpec.toJson();
            },
        };

		return MappingTab;            // return the constructor
	}

);
