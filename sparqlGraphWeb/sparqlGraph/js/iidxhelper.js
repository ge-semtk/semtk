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

define([	// properly require.config'ed
        	
        	'jquery',
			// shimmed
        	'datagrids', 
        	'col-reorder-amd'

		],

	function($) {
		
		var IIDXHelper = function () {
		};
		
		IIDXHelper.counter = 0;
		IIDXHelper.idHash = {};   // hash random things to an element by its id
		
		/*
		 * create a unique id with base name
		 */
		IIDXHelper.getNextId = function(base) {
			var ret = base + "_" + String(IIDXHelper.counter);
			IIDXHelper.counter += 1;
			return ret;
		},
		
		IIDXHelper.buildInlineForm = function () {
			var form = document.createElement("form");
			form.className = "form-inline";
			form.onsubmit = function(){return false;};    // NOTE: forms shouldn't submit automatically on ENTER
			return form;
		};
		
		IIDXHelper.buildHorizontalForm = function () {
			var form = document.createElement("form");
			form.className = "form-horizontal";
			form.onsubmit = function(){return false;};    // NOTE: forms shouldn't submit automatically on ENTER
			return form;
		};
		
		IIDXHelper.buildControlGroup = function (labelText, controlDOM, optHelpText) {
			// take a text label, DOM control, and optional help text
			// Assemble a representative "control-group" div and return it
			
			var controlGroupDiv = document.createElement("div");
			controlGroupDiv.className = "control-group";
			
			var labelElem = document.createElement("label");
			labelElem.className = "control-label";
			labelElem.innerHTML = labelText;
			controlGroupDiv.appendChild(labelElem);
			
			var controlsDiv = document.createElement("div");
			controlsDiv.className = "controls";
			controlsDiv.appendChild(controlDOM);
			
			if (typeof optHelpText !== "undefined") {
				var p = document.createElement("p");
				p.className = "help-block";
				p.innerHTML = optHelpText;
				controlsDiv.appendChild(p);
			}
			
			controlGroupDiv.appendChild(controlsDiv);
			return controlGroupDiv;
		};
		
		IIDXHelper.downloadFile = function (data, filename) {
	    	// build an anchor and click on it
			$('<a>invisible</a>')
				.attr('id','downloadFile')
				.attr('href','data:text/csv;charset=utf8,' + encodeURIComponent(data))
				.attr('download', filename)
				.appendTo('body');
			$('#downloadFile').ready(function() {
				$('#downloadFile').get(0).click();
			});
			
			// remove the evidence
			var parent = document.getElementsByTagName("body")[0];
			var child = document.getElementById("downloadFile");
			parent.removeChild(child);
	    };
	    
		IIDXHelper.setControlGroupState = function (group, state) {
			// state can be "error" "warning" "success" or ""
			
			group.classList.remove("error", "warning", "success");
			group.classList.add(state);
		};
		
		IIDXHelper.hideControlGroup = function (group) {
			// state can be "error" "warning" "success" or ""
			group.style.visibility = "hidden";
		};
		
		/**
		 * Change html tags to plain html text
		 */
		IIDXHelper.htmlSafe = function(str) {
		    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
		},
		
		IIDXHelper.unhideControlGroup = function () {
			// state can be "error" "warning" "success" or ""
			group.style.visibility = "";
		};
		
		IIDXHelper.setControlGroupLabel = function (group, newText) {
			// change innerHTML of first control-label in the control-troup
			var nodeList = group.getElementsByClassName("control-label");
			nodeList[0].innerHTML = newText;
		};
		
		
		
		IIDXHelper.createDropzone = function (iconName, labelText, isDroppableCallback, doDropCallback) {
			// create a dropzone element.  It changes colors nicely, etc.
			// callbacks take event parameters.
			
			var icon = document.createElement("icon");
	
			icon.className = iconName;
			icon.style = "font-size: 3em; color: orange";
			
			var div = document.createElement("div");
			div.className = "label-inverse";
			var center = document.createElement("center");
			center.style.color = "orange";
			div.appendChild(center);
								
			center.appendChild(icon);
			
			var br = document.createElement("br");
			br.style.color = "orange";    // NOTE: hide the color on the <BR> element 
			center.appendChild(br);
			
			var label = document.createTextNode(labelText);
			label.id = IIDXHelper.getNextId("dropLabel");
			center.appendChild(label);
			
			// dragover changes color.  dropping or dragging out restores color.
			
			div.ondrop = function (ev) {
				if (isDroppableCallback(ev)) {
					doDropCallback(ev, label);
						
					center.style.color = br.style.color;
					icon.style.color = br.style.color;
				}
				
				ev.preventDefault();
				ev.stopPropagation();
			};

			div.ondragover = function (ev) { 
				if (ev.target.nodeType == 1) {
					if (isDroppableCallback(ev)) {
						center.style.color = "blue"; 
						icon.style.color = "blue"; 
						ev.preventDefault();
					}

					ev.stopPropagation();
				}
			};

			div.ondragleave = function (ev) { 
				center.style.color = br.style.color;
				icon.style.color = br.style.color;

			};
			
			return div;
		};
		
		IIDXHelper.DROPZONE_FULL = 1;
		IIDXHelper.DROPZONE_EMPTY = 2;
		
		IIDXHelper.setDropzoneLabel = function (div, msgTxt, optEmptyFullFlag) {
			var center = div.childNodes[0];
			var icon = center.childNodes[0];
			var br = center.childNodes[1];
			var label = center.childNodes[2];
			label.data = msgTxt;
			
			if (typeof optEmptyFullFlag !== "undefined") {
				if (optEmptyFullFlag == IIDXHelper.DROPZONE_FULL) {
					center.style.color = "black";
					br.style.color = "black";
					icon.style.color = "black";
				} else {
					center.style.color = "orange";
					br.style.color = "orange";
					icon.style.color = "orange";
				}
			}
		};
		
		/**
		 * create a progress bar inside an empty div that has:
		 *     an id
		 *     no classes
		 *     no innerHTML
		 * classes:  choose zero or one off each list:
		 *     progress-success, progress-info, progress-warning, etc.  (sets color)
		 *     progress-striped
		 *     active
		 */
		IIDXHelper.progressBarCreate = function (emptyDivWithId, classes) {
			emptyDivWithId.className = "progress " + classes;
			var bar = document.createElement("div");
			bar.classList.add("bar");
			bar.style.width = "0%";
			bar.id = IIDXHelper.getNextId("bar");
			emptyDivWithId.appendChild(bar);
			IIDXHelper.idHash[emptyDivWithId.id] = bar.id;
		};
		
		/**
		 * Set a progress bar's percentage
		 */
		IIDXHelper.progressBarSetPercent = function(emptyDivWithId, percent, optMessage) {
			var bar = document.getElementById(IIDXHelper.idHash[emptyDivWithId.id]);
			bar.style.width = String(percent) + "%";
			
			if (typeof message !== "undefined") {
				bar.innerHTML = "<b>" + optMessage + "</b>";
			} else {
				bar.innerHTML = "<b>" + String(percent) + "% </b>";
			}
		}
		
		/**
		 * Clean up a progress bar, returning the empty div
		 */
		IIDXHelper.progressBarRemove = function(emptyDivWithId) {
			emptyDivWithId.className = "";
			emptyDivWithId.innerHTML = "";
			delete IIDXHelper.idHash[emptyDivWithId.id];
		}
		
		IIDXHelper.buildMenuDiv = function (menuLabelList, menuCallbackList) {
			// menu
			var menuDiv = document.createElement("div");
			menuDiv.className = "btn-group";
			menuDiv.align = "right";
			menuDiv.style.width = "100%";
			
			var button = document.createElement("button");
			button.className = "btn dropdown-toggle";
			button.setAttribute("data-toggle", "dropdown");
			var icon = document.createElement("i");
			icon.className = "icon-chevron-down";
			button.appendChild(icon);
			menuDiv.appendChild(button);
			
			// menu list
			var list = document.createElement("ul");
			list.className = "dropdown-menu pull-right";
			
			// menu list items
			var item;
			var anchor;
			for (var i=0; i < menuCallbackList.length; i++) {
				item = document.createElement("li");
				anchor = document.createElement("a");
				anchor.onclick = menuCallbackList[i];
				anchor.innerHTML = menuLabelList[i];
				item.appendChild(anchor);
				list.appendChild(item);
			}
			
			menuDiv.appendChild(list);
			return menuDiv;
		};
		
		IIDXHelper.buildDatagridInDiv = function (div, headerHTML, colsCallback, dataCallback, menuLabelList, menuCallbackList, optFinishedCallback) {
			//
			// PARAMS:
			//   div - html div in which to put the menu and table
			//   gridFilterFlag - an iidx pass-through
			//   colsCallback - datagridAoColumns type of callback
			//   dataCallback - datagridAaData type of callback
			//   menuStringList - list of menu items
			//   menuCallbackList - list of callbacks for menu items
			//   optFinishedCallback - call when done

			// make up some random names
			var dataTableName = IIDXHelper.getNextId("table");
			var tableId = IIDXHelper.getNextId("table");
			
			var finishedCallback = (typeof optFinishedCallback == 'undefined' || optFinishedCallback == null) ? function(){} : optFinishedCallback;
			
			// search
			var searchHTML = '<input type="text" id="table_filter" class="input-medium search-query" data-filter-table="' + dataTableName + '"><button class="btn btn-icon"><i class="icon-search"></i></button>';

			var menuDiv = IIDXHelper.buildMenuDiv(menuLabelList, menuCallbackList);
			
			// header Table
			var headerTable = document.createElement("table");
			headerTable.width = "100%";
			
			var tr = document.createElement("tr");
			var td;
			
			// header cell
			td = document.createElement("td");
			td.align="left";
			td.innerHTML = headerHTML;
			tr.appendChild(td);
			
			// search cell
			td = document.createElement("td");
			td.align="right";
			td.innerHTML = searchHTML;
			tr.appendChild(td);
			
			// menu cell
			td = document.createElement("td");
			td.appendChild(menuDiv);
			tr.appendChild(td);
			
			// add row to table & table to div
			headerTable.appendChild(tr);
			div.innerHTML = "";
			div.appendChild(headerTable);

			// grid
			var gridTable = document.createElement("table");
			gridTable.id = tableId;
			gridTable.className = "table table-bordered table-condensed";
			gridTable.setAttribute("data-table-name", dataTableName);
			div.appendChild(gridTable);
			
			// define a variable since 'this' isn't legal inside the require js function / function 
			// add the data to the table and set it up as a datagrid
			var me = this;  // work-around require scoping
			
			// fix numeric sorts so 
			//   (1) ints and floats co-exist:  parseFloat handles ints, floats, strings
			//   (2) numbers and blanks co-exist  (zero > NaN)
			$.fn.dataTableExt.oSort['numeric-asc'] = function(a,b) {
				var x = parseFloat(a);   
				var y = parseFloat(b);
				return ((isNaN(x) || x < y) ? -1 : ((isNaN(y) || x > y) ? 1 : 0));
			};
			$.fn.dataTableExt.oSort['numeric-desc'] = function(a,b) {
				var x = parseFloat(a);
				var y = parseFloat(b);
				return ((isNaN(x) || x < y) ? 1 : ((isNaN(y) || x > y) ? -1 : 0));
			};

			// --- not implemented
				me.sortAscendHash = {};
				me.sortDescendHash = {};
				
				// apply any special sorts
				for (var stype in me.sortAscendHash) {
					$.fn.dataTableExt.oSort[stype + '-asc'] = me.sortAscendHash[stype];
				}
				for (var stype in me.sortDescendHash) {
					$.fn.dataTableExt.oSort[stype + '-desc'] = me.sortDescendHash[stype];
				}
            //--- end not implemented
				
			$(function() {
				$("#"+tableId).iidsBasicDataGrid({
					'aoColumns':   colsCallback(),
					'aaData':      dataCallback(),
					'plugins': ['R'], //enable the col-reorder plugin (assumes 'col-reorder-amd' is on the page)
					'useFloater': false,
					'isResponsive': true
				});
				// avoid flash of unstyled content
				$("#"+tableId).css( { visibility: 'visible' } );
				finishedCallback();
			});
		};
		
		return IIDXHelper;            
	}
);