/**
 ** Copyright 2016-17 General Electric Company
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
    };

    IIDXHelper.buildInlineForm = function () {
        var form = document.createElement("form");
        form.className = "form-inline";
        form.onsubmit = function(){return false;};    // NOTE: forms shouldn't submit automatically on ENTER
        return form;
    };

    /*
     * @example
     *    form = IIDXHelper.buildHorizontalForm()
     *    fieldset = IIDXHelper.addFieldset(form)
     *    fieldset.appendChild(IIDXHelper.buildControlGroup("label: ", IIDXHelper.createTextInput("myId")));
     *    myDom.appendChild(form);
     */
    IIDXHelper.buildHorizontalForm = function (tightFlag) {
        // tightFlag overrides IIDX giant bottom margin
        var form = document.createElement("form");
        form.className = "form-horizontal";
        form.onsubmit = function(){return false;};    // NOTE: forms shouldn't submit automatically on ENTER
        if (tightFlag) form.style.marginBottom = "1ch";
        return form;
    };

    IIDXHelper.addFieldset = function (horizForm) {
        var fieldset = document.createElement("fieldset");
        horizForm.appendChild(fieldset);
        return fieldset;
    };
    
    IIDXHelper.fsAddTextInput = function (fieldSet, label, optId, optClassName, optInitValue, optDisabled) {
        var id = (typeof optId != "undefined" && optId != null) ? optId : null;
        var className = (typeof optClassName != "undefined" && optClassName != null) ? optClassName : undefined;
        var input = IIDXHelper.createTextInput(id, className);
        
        if (typeof optInitValue != undefined && optInitValue != null) {
            input.value = optInitValue;
        }
        
        if (typeof optDisabled != undefined && optDisabled != null) {
            input.disabled = optDisabled;
        }

        fieldSet.appendChild(IIDXHelper.buildControlGroup(label, input));
    }

    /*
     * classes - input-mini, input-small, input-medium, input-large, input-xlarge
     */
    IIDXHelper.createTextInput = function (id, optClassName) {
        var className = (typeof optClassName !== "undefined") ? optClassName : "input-xlarge";
        var elem = document.createElement("input");
        if (typeof id != "undefined" && id != null) { 
            elem.id = id;
        }
        elem.type = "text";
        elem.classList.add(className);
        return elem;
    };

    IIDXHelper.createTextArea = function (id, rows, optClassName) {
        var className = (typeof optClassName !== "undefined") ? optClassName : "input-xlarge";
        var elem = document.createElement("textarea");
        if (typeof id != "undefined" && id != null) { 
            elem.id = id;
        }
        elem.rows = rows;
        elem.classList.add(className);
        return elem;
    };

    IIDXHelper.createVAlignedCheckbox = function () {
        var checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.style.verticalAlign = "middle";
        checkbox.style.position = "relative";
        checkbox.style.bottom = ".25em";
        return checkbox;
    };

    IIDXHelper.createLeftRightTable = function (leftDom, rightDom) {
        // create a two column table with 100% width and right side right justified
        var table = document.createElement("table");
        table.style.width = "100%";
        var tr = document.createElement("tr");
        var td = document.createElement("td");
        td.appendChild(leftDom);
        tr.appendChild(td);

        td = document.createElement("td");
        td.align = "right";
        td.appendChild(rightDom);
        tr.appendChild(td);

        table.appendChild(tr);
        return table;
    };

    /*
     * Create a dropdown element.
     * id is the element id
     * textValArray is one of:
     *     just values:    [ "textAndVal1",     "textAndVal2", ...]
     *     just values:    [ ["textAndVal1"],   ["textAndVal2"], ...]
     *     text val pairs: [ ["text1", "val1"], ["text2", "val2"], ...]
     * selectedTexts is a list of selected items
     * optClassName can be used to control the width of the select box (e.g. input-mini for a narrow dropdown)
     */
    IIDXHelper.createSelect = function (id, textValArray, selectedTexts, optMultiFlag, optClassName) {
        var select =  document.createElement("select");
        select.id = id;
        if(typeof optClassName != "undefined"){
            select.className = optClassName;
        }
        select.multiple = ((typeof optMultiFlag) != "undefined" && optMultiFlag);
        var option;
        var text;
        var val;
        for (var i=0; i < textValArray.length; i++) {
            option = document.createElement("option");
            
            if (typeof textValArray[i] == "string") {
                text = textValArray[i];
                val = textValArray[i];
            } else if (textValArray[i].length == 1) {
                text = textValArray[i][0];
                val = textValArray[i][0];
            } else {
                text = textValArray[i][0];
                val = textValArray[i][1];
            }
            option.text = text;
            option.value = val;
            option.selected = (selectedTexts.indexOf(text) > -1);
            select.options.add(option);
        }

        return select;
    };

    /* create a collapsible div
     * with a button and title
     * and innerDom inside it.
     */
    IIDXHelper.createCollapsibleDiv = function (title, innerDom, openFlag) {
        var outerDiv = document.createElement("div");
        var innerDiv = document.createElement("div");
        var but = document.createElement("a");
        var icon = document.createElement("icon");
        
        innerDiv.appendChild(innerDom);
        innerDiv.appendChild(document.createElement("hr"));
        
        but.onclick = function(div, ic) {
            if (ic.className.endsWith("right")) {
                div.style.display = "inline";
                ic.className = "icon-chevron-down";
            } else {
                div.style.display = "none";
                ic.className = "icon-chevron-right";
            }
        }.bind(this, innerDiv, icon);
        
        but.classList.add("btn");
        but.appendChild(icon);
        but.onclick(innerDiv, icon);
        if (openFlag) {
            but.onclick(innerDiv, icon);
        }

        outerDiv.appendChild(but);
        outerDiv.appendChild(document.createTextNode(" " + title));
        outerDiv.appendChild(document.createElement("br"));
        outerDiv.appendChild(innerDiv);
        
        return outerDiv;
    };
    
    IIDXHelper.createIconButton = function(iconClass) {
        var ret = document.createElement("a");
        ret.classList.add("btn");
        var icon = document.createElement("icon");
        icon.className = iconClass;
        ret.appendChild(icon);
        return ret;
    };
    
    /*
     * Get list of values for each selected option
     *
     * Handle performance and compatibility. 
     * Work for single or multi-select.
     */
    IIDXHelper.getSelectValues = function (select) {
        var ret = [];
        
        // fast but not universally supported
        if (select.selectedOptions != undefined) {
            for (var i=0; i < select.selectedOptions.length; i++) {
                ret.push(select.selectedOptions[i].value);
            }

        // compatible, but can be painfully slow
        } else {
            for (var i=0; i < select.options.length; i++) {
                if (select.options[i].selected) {
                    ret.push(select.options[i].value);
                }
            }
        }
        return ret;
    };
    
    /*
     * Set a select's selectedIndex to the first option with given text
     * If none, de-select all
     * @returns {void}
     */
    IIDXHelper.selectFirstMatchingText = function (select, text) {
        select.selectedIndex = -1;
        for (var i=0; i < select.options.length; i++) {
            if (select.options[i].text == text) {
                select.selectedIndex = i;
                return;
            }
        }
    };
    
    /*
     * does a select contain an option with given text
     * @returns {boolean}
     */
    IIDXHelper.selectContainsText = function (select, text) {
        for (var i=0; i < select.options.length; i++) {
            if (select.options[i].text == text) {
                return true;
            }
        }
        return false;
    };
    
    /* Creates a label element with the given text.  Optionally provide a tooltip. */
    IIDXHelper.createLabel = function (labelText, optTooltip) {
        var labelElem = document.createElement("label");
        labelElem.className = "control-label";
        labelElem.innerHTML = labelText;
        if(optTooltip != "undefined"){
            labelElem.title = optTooltip;
        }
        return labelElem;
    };
    
    IIDXHelper.createButton = function (text, callback, optClassList, optId) {
        var butElem = document.createElement("a");
        butElem.innerHTML = text;
        butElem.onclick = callback;
        butElem.classList.add("btn");
        if (typeof optClassList != "undefined") {
            for (var i=0; i < optClassList.length; i++) {
                butElem.classList.add(optClassList[i]);
            }
        }
        if (typeof optId != "undefined") {
            butElem.id = optId;
        }
        return butElem;
    };
    
    IIDXHelper.createNbspText = function() {
        return document.createTextNode("\u00A0");
    };
    
    IIDXHelper.createBoldText = function(text) {
        var b = document.createElement("b");
        b.appendChild(document.createTextNode(text));
        return b
    };
    
    /*
     * Create a button group.
     * optDataToggleAttribute can be set to "buttons-radio" (single-select) or "buttons-checkbox" (multi-select)
     */
    IIDXHelper.createButtonGroup = function (id, optionsArray, optDataToggleAttribute) {
        var dataToggleAttribute = (typeof optDataToggleAttribute != "undefined") ? optDataToggleAttribute : "buttons-radio";
        var buttonGroupDiv = document.createElement("div");
        buttonGroupDiv.id = id;
        buttonGroupDiv.className = "btn-group";
        buttonGroupDiv.setAttribute("data-toggle", dataToggleAttribute);  // checkbox or radio
        for(var i=0; i < optionsArray.length; i++){
            var button = document.createElement("button");
            button.className = "btn";
            button.id=operand1ElementId + "-" + optionsArray[i];
            button.innerHTML = optionsArray[i];
            buttonGroupDiv.appendChild(button);
        }
        return buttonGroupDiv;
    }

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

    IIDXHelper.downloadFile = function (data, filename, mimetype) {
        // http://stackoverflow.com/questions/13405129/javascript-create-and-save-file
        // This handles bigger files than the OLD version
        // because a blob doesn't need to be URI-encoded
        // Paul April 2017
        var file = new Blob([data], {type: mimetype});

        if (window.navigator.msSaveOrOpenBlob) // IE10+
            window.navigator.msSaveOrOpenBlob(file, filename);
        else { // Others
            var url = URL.createObjectURL(file);
            IIDXHelper.downloadUrl(url, filename);
        }
    };
    
    IIDXHelper.downloadUrl = function (url, optFilename) {

        var a = document.createElement("a");
        a.href = url;
        if (typeof optFilename != "undefined") {
            a.download = optFilename;
        }
        document.body.appendChild(a);
        a.click();
        setTimeout(function() {
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);  
        }, 0); 
        
    };

    IIDXHelper.downloadFileOLD = function (data, filename) {
        // build an anchor and click on it
        // URI encodes the data
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

    IIDXHelper.fileDialog = function (callback) {
        
        var input = document.createElement("input");
        input.type = "file";
        input.style = "display:none";
        document.body.appendChild(input);
        
        var cb = function(elem, userCallback, e) {
            userCallback(e.target.files[0]);
            input.parentElement.removeChild(input);
        }.bind(this, input, callback);
        
        input.addEventListener('change', cb, false);
        input.click();
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
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/%/g, "&percnt;");
    },
    
    IIDXHelper.removeHtml = function(str) {
        return String(str).replace(/<(br|p|h1|h2|h3)>/g, '\n').replace(/<[^>]+>/g, '').replace("&nbsp", ' ');
    },

    IIDXHelper.isFirefox = function () {
        return (navigator.userAgent.toLowerCase().indexOf('firefox') > -1);
    },

    IIDXHelper.regexSafe = function(str) {
        // thanks stack overflow: http://stackoverflow.com/questions/3561493/is-there-a-regexp-escape-function-in-javascript
        return String(str).replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
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

        if (menuLabelList.length > 0) {
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
        }
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
        //
        // RETURNS the table element

        // make up some random names
        var dataTableName = IIDXHelper.getNextId("table");
        var tableId = IIDXHelper.getNextId("table");

        var finishedCallback = (typeof optFinishedCallback == 'undefined' || optFinishedCallback == null) ? function(){} : optFinishedCallback;

        // search (moved into the grid)
        // var searchHTML = '<input type="text" id="table_filter" class="input-medium search-query" data-filter-table="' + dataTableName + '"><button class="btn btn-icon"><i class="icon-search"></i></button>';

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

        // search cell (moved into the grid)
        //td = document.createElement("td");
        //td.align="right";
        //td.innerHTML = searchHTML;
        //tr.appendChild(td);

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

        return gridTable;
    };
    
    /**
     * For an IIDX datagrid table (e.g. created by buildDatagridInDiv())
     * return a list containing column[colname].innerText of every selected row.
     */
    IIDXHelper.getSelectedRowsVals = function(table, colname) {
        var ret = [];
        var retCol = -1;

        // find column to return
        for (var i=0; i < table.tHead.rows[0].cells.length; i++) {
            if (table.tHead.rows[0].cells[i].innerText == colname) {
                retCol = i;
                break;
            }
        }

        // error if column doesn't exist
        if (retCol == -1) {
            throw new Error("Datagrid table does not have a column: " + colname);
        }

        for (var i=0; i < table.rows.length; i++) {
            if (table.rows[i].classList.contains("row_selected")) {
                ret.push(table.rows[i].cells[retCol].innerText);
            }
        }

        return ret;

    };

    IIDXHelper.buildTabs = function(nameList, divList) {
        var panel = IIDXHelper.getNextId("panel");
        var ret = document.createElement("span");
        
        var ul = document.createElement("ul");
        ul.classList.add("nav");
        ul.classList.add("nav-tabs");
        ul.classList.add("nav-justified");
        ret.appendChild(ul);
        
        for (var i=0; i < nameList.length; i++) {
            var li = document.createElement("li");
            li.classList.add("nav-item");
            if (i==0) li.classList.add("active");
            ul.appendChild(li);
            
            var a = document.createElement("a");
            a.classList.add("nav-link");
            if (i==0) a.classList.add("active");
            a.dataToggle = "tab";
            a.href = "#" + panel + String(i);
            a.role = "tab";
            a.innerHTML = nameList[i];
            a.onclick = function (e) {
                e.preventDefault();
                $(this).tab('show');
            };
            li.appendChild(a);
        }
        
        var div = document.createElement("div");
        div.classList.add("tab-content");
        div.classList.add("card");
        ret.appendChild(div);
        
        for (var i=0; i < divList.length; i++) {
            var pdiv = document.createElement("div");
            pdiv.classList.add("tab-pane");
            pdiv.classList.add("fade");
            
            if (i==0) {
                pdiv.classList.add("in");
                pdiv.classList.add("show");
                pdiv.classList.add("active");
            }
            
            pdiv.id = panel + String(i);
            pdiv.role = "tabpanel";
            pdiv.appendChild(document.createElement("br"));
            pdiv.appendChild(divList[i]);
            div.appendChild(pdiv);
        }
        
        
        return ret;
    };
    
    IIDXHelper.buildSelectDatagridInDiv = function (div, colsCallback, dataCallback, optFinishedCallback) {
        //
        // PARAMS:
        //   div - html div in which to put the menu and table
        //   gridFilterFlag - an iidx pass-through
        //   colsCallback - datagridAoColumns type of callback
        //   dataCallback - datagridAaData type of callback
        //   menuStringList - list of menu items
        //   menuCallbackList - list of callbacks for menu items
        //   optFinishedCallback - call when done
        //
        // RETURNS the table element

        // make up some random names
        var dataTableName = IIDXHelper.getNextId("table");
        var tableId = IIDXHelper.getNextId("table");

        var finishedCallback = (typeof optFinishedCallback == 'undefined' || optFinishedCallback == null) ? function(){} : optFinishedCallback;

        // header Table
        var headerTable = document.createElement("table");
        headerTable.width = "100%";

        var tr = document.createElement("tr");
        var td;

        // grid
        var gridTable = document.createElement("table");
        gridTable.id = tableId;
        gridTable.className = "table table-bordered table-condensed";
        gridTable.setAttribute("data-table-name", dataTableName);
        div.appendChild(gridTable);

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

        $(function() {
            $("#"+tableId).dataTable({
                'aoColumns':   colsCallback(),
                'aaData':      dataCallback(),
                'paging':     false,
            });
            // avoid flash of unstyled content
            $("#"+tableId).css( { visibility: 'visible' } );
            finishedCallback();
        });

        return gridTable;
    };
    return IIDXHelper;            
});
