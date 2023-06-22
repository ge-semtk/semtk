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
        	'col-reorder-amd',
            'bootstrap/bootstrap-dropdown'

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
    };

    /*
     * classes - input-mini, input-small, input-medium, input-large, input-xlarge
     */
    IIDXHelper.createTextInput = function (id, optClassName, optDatalist) {
        var className = (typeof optClassName !== "undefined") ? optClassName : "input-xlarge";
        var elem = document.createElement("input");
        if (typeof id != "undefined" && id != null) {
            elem.id = id;
        }
        elem.type = "text";
        elem.classList.add(className);

        if (typeof optDatalist !== "undefined") {
            elem.setAttribute("list", optDatalist.id);
            elem.setAttribute("autocomplete", "off");
        }
        return elem;
    };

    IIDXHelper.createDataList = function (id, valList) {
        var elem = document.createElement("datalist");
        elem.id = id;

        for (var i=0; i < valList.length; i++) {
            var option = document.createElement("option");
            option.innerHTML = valList[i];
            elem.appendChild(option);
        }
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

    IIDXHelper.removeMargins = function(e) {
        e.style.margin = "0px 0px 0px 0px";
    };

    IIDXHelper.appendCheckBox =  function(elem, checkbox, label) {
        elem.appendChild(document.createTextNode( '\u00A0\u00A0' ) );
        elem.appendChild(checkbox);
        elem.appendChild(document.createTextNode(" " + label));
    };

    IIDXHelper.createVAlignedCheckbox = function (optId, optChecked, optClass, optOnclick) {
        var checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.style.verticalAlign = "middle";
        checkbox.style.position = "relative";
        checkbox.style.bottom = ".25em";

        if (typeof optId != "undefined")      checkbox.id = optId;
        if (typeof optChecked != "undefined") checkbox.checked = optChecked;
        if (typeof optClass != "undefined")   checkbox.classList.add(optClass);
        if (typeof optOnclick != "undefined") checkbox.onclick = optOnclick;

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

    IIDXHelper.tableAddRow = function (tableElem, rowArray) {
        var tr = document.createElement("tr");
        tableElem.appendChild(tr);

        for (var i=0; i < rowArray.length; i++) {
            var td = document.createElement("td");
            tr.appendChild(td);
            td.innerHTML = rowArray[i];
        }
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
    IIDXHelper.createSelect = function (id, textValArray, selectedTexts, optMultiFlag, optClassName, optDisabledList) {
        var select =  document.createElement("select");

        if (typeof id != "undefined" && id != null && id != "") {
            select.id = id;
        }

        if(typeof optClassName != "undefined"){
            select.className = optClassName;
        }
        select.multiple = ((typeof optMultiFlag) != "undefined" && optMultiFlag);
        IIDXHelper.addOptions(select, textValArray, selectedTexts, optDisabledList);
        return select;
    };

	/*
		Create a filter input that affects the contents of a select
	 */
	IIDXHelper.createSelectFilter = function(select) {
		var input = document.createElement("input");
		input.type="text";
		input.placeholder="Search..";
		input.style.marginBottom="1px";
		input.onkeyup= function() {
			// filter function
			var pat = input.value;
			var inverse = false;
			if (pat == "^" || pat == "") {
				// ignore "^" by itsself
				pat = ".*";
			} else if (pat.startsWith("^")) {
				// negate "^pattern"
				pat = pat.slice(1);
				inverse = true;
			} 
			var reg = new RegExp(pat, 'i');
			
			for (var i=0; i < select.length; i++) {
				if (inverse) {
					select[i].hidden =   reg.test(select[i].text);
				} else {
					select[i].hidden = ! reg.test(select[i].text);
				}
			}
		};
		return input;
	};
	
    /*
     * See createSelect for description of addTextValArray
     */
    IIDXHelper.addOptions = function (select, textValArray, selectedTexts, optDisabledList) {
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

            if (typeof optDisabledList != "undefined" && (optDisabledList.indexOf(text) > -1) ) {
                IIDXHelper.setOptionDisabled(option, true);
            }
            select.options.add(option);
        }
    };

    IIDXHelper.setOptionDisabled = function(option, value) {
        if (value) {
            option.disabled = true;
            option.style.backgroundColor = "gray";
            option.style.textDecoration="line-through";
        } else {
            option.disabled = false;
            option.style.backgroundColor = "";
            option.style.textDecoration="";
        }
    };

    IIDXHelper.removeAllOptions = function (select) {
        while (select.length > 0) {
           select.remove(select.length-1);
        }
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
     * Get list of values for each selected option
     *
     * Handle performance and compatibility.
     * Work for single or multi-select.
     */
    IIDXHelper.getSelectTexts = function (select) {
        var ret = [];

        // fast but not universally supported
        if (select.selectedOptions != undefined) {
            for (var i=0; i < select.selectedOptions.length; i++) {
                ret.push(select.selectedOptions[i].text);
            }

        // compatible, but can be painfully slow
        } else {
            for (var i=0; i < select.options.length; i++) {
                if (select.options[i].selected) {
                    ret.push(select.options[i].text);
                }
            }
        }
        return ret;
    };

    IIDXHelper.setSelectOptionDisabled = function (select, itemValue, disabledValue) {

        for (var i=0; i < select.options.length; i++) {
            if (select.options[i].value == itemValue) {
                IIDXHelper.setOptionDisabled(select.options[i], disabledValue);
            }
        }
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
     * Set a select's selectedIndex to the first option with given val
     * If none, de-select all
     * @returns {void}
     */
    IIDXHelper.selectFirstMatchingVal = function (select, val) {
        select.selectedIndex = -1;
        for (var i=0; i < select.options.length; i++) {
            if (select.options[i].value == val) {
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

    IIDXHelper.createElement = function (tag, innerHTML, className) {
        var elem = document.createElement(tag);
        if (innerHTML) {
            elem.innerHTML = innerHTML;
        }
        if (className) {
            elem.classList.add(className);
        }
        return elem;
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

    IIDXHelper.createButton = function (text, callback, optClassList, optId, optMouseOver) {
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
        if (typeof optMouseOver != "undefined") {
            butElem.title = optMouseOver;
        }
        return butElem;
    };


    IIDXHelper.createIconButtonOLD = function(iconClass, optCallback) {
        var ret = document.createElement("a");
        ret.classList.add("btn");
        var icon = document.createElement("icon");
        icon.className = iconClass;
        ret.appendChild(icon);

        if (typeof optCallback != undefined) {
            ret.onclick = optCallback;
        }
        return ret;
    };

    IIDXHelper.createIconButton = function(iconClass, callback, optClassList, optId, optText, optMouseOver) {
        var butElem = document.createElement("button");
        butElem.appendChild(IIDXHelper.createIcon(iconClass));
        if (optText) {
            butElem.innerHTML += " " + optText;
        }
        butElem.onclick = callback;
        if (typeof optClassList != "undefined") {
            for (var i=0; i < optClassList.length; i++) {
                butElem.classList.add(optClassList[i]);
            }
        }
        if (typeof optId != "undefined") {
            butElem.id = optId;
        }
        if (typeof optMouseOver != "undefined") {
            butElem.title = optMouseOver;
        }
        return butElem;
    };

    IIDXHelper.createIcon = function(iconClass) {
        var icon = document.createElement("icon");
        icon.className = iconClass;
        return icon;
    };

    IIDXHelper.createNbspText = function() {
        return document.createTextNode("\u00A0");
    };

    IIDXHelper.createBoldText = function(text) {
        var b = document.createElement("b");
        b.appendChild(document.createTextNode(text));
        return b
    };

    IIDXHelper.appendTextLine = function(elem, text) {
        elem.appendChild(document.createTextNode(text));
        elem.appendChild(document.createElement("br"));
    };

    IIDXHelper.appendSpace = function(elem) {
        elem.appendChild(IIDXHelper.createNbspText());
    }

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

    IIDXHelper.buildControlGroup = function (labelText, controlDOM, optHelpText, optHelpId) {
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

        var s = document.createElement("span");  // formerly "p"
        s.className = "help-inline";             // formerly "help-block"
        s.innerHTML = (typeof optHelpText !== "undefined") ? optHelpText : "";
        if (optHelpId) {
            s.id = optHelpId;
        }
        controlsDiv.appendChild(s);

        controlGroupDiv.appendChild(controlsDiv);
        return controlGroupDiv;
    };

    IIDXHelper.changeControlGroupHelpText = function (controlGroupDiv, text, elemClass) {
        // elemClass can be: "", "warning", "error", "success"
        controlGroupDiv.className = "control-group " + elemClass;

        var cntlDiv = controlGroupDiv.getElementsByTagName("div")[0];
        var span = cntlDiv.getElementsByTagName("span")[0];
        span.innerHTML = text;
    };

    // search callback takes elem param - so it can ask elem.value to get search pattern
    IIDXHelper.createSearchDiv = function (searchCallback, searchThis) {

        var div = document.createElement("div");
        div.classList.add("input-append");

        var input = document.createElement("input");
        div.appendChild(input);
        input.type="text";
        input.classList.add("input-medium");
        input.classList.add("search-query");
        input.align="left";

        var searchbut = IIDXHelper.createIconButton("icon-search", searchCallback.bind(searchThis, input), ["btn", "btn-icon"]);
        div.appendChild(searchbut);
        searchbut.type="submit";

        return div;
    }

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
     * Change html tags to plain html text,
     * and \n to html <br>
     */
    IIDXHelper.htmlSafe = function(str) {
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/%/g, "&percnt;").replace(/[\n]/, "<br>");
    },

    /**
      * if str is ONLY a URL then change to an anchor
      * otherwise return as-is
      */
    IIDXHelper.urlToAnchor = function(str) {
        if (str.search("^https?:\/\/[^\\s]+$") == 0) {
            return "<a href=\"" + str + "\">" + str + "</a>";
        } else {
            return str;
        }
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

        var icon = IIDXHelper.createIcon(iconName);
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
    IIDXHelper.progressBarCreate = function (emptyDivWithId, classes, optCancelCallback) {
        // create a table
        var table = document.createElement("table");
        var tr = document.createElement("tr");
        table.appendChild(tr);
        table.style.width = "100%";
        table.tableLayout="auto";
        emptyDivWithId.appendChild(table);

        var tdLeft = document.createElement("td");
        tr.appendChild(tdLeft);

        var tdRight = document.createElement("td");
        tdRight.style.width = "1%";
        tdRight.style.whiteSpace = "no-wrap";
        tr.appendChild(tdRight);

        var div = document.createElement("div");
        div.className = "progress " + classes;
        div.style.margin = "0";
        tdLeft.appendChild(div);

        var bar = document.createElement("div");
        bar.classList.add("bar");
        bar.style.width = "0%";
        bar.id = IIDXHelper.getNextId("bar");
        div.appendChild(bar);
        IIDXHelper.idHash[emptyDivWithId.id] = bar.id;

        if (optCancelCallback) {
            var but = IIDXHelper.createIconButton("icon-remove-sign", optCancelCallback, ["btn", "btn-danger"], undefined, undefined, "Cancel" );
            tdRight.appendChild(but);
        }
    };

    /**
     * Set a progress bar's percentage
     */
    IIDXHelper.progressBarSetPercent = function(emptyDivWithId, percent, optMessage) {
        var bar = document.getElementById(IIDXHelper.idHash[emptyDivWithId.id]);
        bar.style.width = String(percent) + "%";

        var html = "<b><font color='" + ((percent < 20) ? "black" : "grey") + "'>";
        if (typeof optMessage !== "undefined") {
            html += optMessage;
        } else {
            html += String(percent) + "%";
        }
        html += "</font></b>"
        bar.innerHTML = html;
    };

    /**
     * Clean up a progress bar, returning the empty div
     */
    IIDXHelper.progressBarRemove = function(emptyDivWithId) {
        emptyDivWithId.className = "";
        emptyDivWithId.innerHTML = "";
        delete IIDXHelper.idHash[emptyDivWithId.id];
    };

    /**
     * Build a simple <list> <li>
     */
    IIDXHelper.buildList = function(listOfTexts) {
        var list = document.createElement("list");
        for (var t of listOfTexts) {
            var li = document.createElement("li");
            li.innerHTML = t;
            list.appendChild(li);
        }
        return list;
    };
    IIDXHelper.buildUserIdButton = function (userId, logoutURL) {

        var div = document.createElement("div");
        div.classList.add("btn-group");

        var but1 = document.createElement("button");
        but1.classList.add("btn");
        but1.classList.add("btn-info");
        but1.appendChild(IIDXHelper.createIcon("icon-user"));
        but1.appendChild(document.createTextNode(" " + userId));
        div.appendChild(but1);

        var but2 = document.createElement("button");
        but2.classList.add("btn");
        but2.classList.add("btn-info");
        but2.classList.add("dropdown-toggle");
        but2.setAttribute("data-toggle", "dropdown");

        but2.appendChild(IIDXHelper.createIcon("icon-chevron-down"));
        div.appendChild(but2);

        var ul = document.createElement("ul");
        ul.classList.add("dropdown-menu");
        var item = document.createElement("li");
        var a = document.createElement("a");
        a.innerHTML = "logout";
        a.href = logoutURL;
        item.appendChild(a);
        ul.appendChild(item);
        div.appendChild(ul);

        return div;
    };

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

    IIDXHelper.buildAnchorWithCallback = function(labelHTML, callback) {
        var anchor = document.createElement("a");
        anchor.onclick = callback;
        anchor.innerHTML = labelHTML;
        return anchor;
    };

    // Build a header for SPARQLgraph results
    //
    // controlsDom - items to control the results display (top left)
    // moreHtmlOrDom - additional information as HTML or DOM (e.g. "only graphing first X points")
    // menuLabelList and menuCallbackList - dropdown menu (e.g. to download results)  (top right)
    IIDXHelper.buildResultsHeaderTable = function (controlsDom, moreHtmlOrDom, menuLabelList, menuCallbackList) {

        var headerTable = document.createElement("table");
        headerTable.width = "100%";
        var tr = document.createElement("tr");
        var td;

        // add controls
        if (controlsDom) {
            td = document.createElement("td");
            td.appendChild(controlsDom);
            tr.appendChild(td);
        }
        
        // add middle element
        td = document.createElement("td");
        td.align="right";
        // header can be html or dom
        if (typeof(moreHtmlOrDom) == "string") {
            td.innerHTML = moreHtmlOrDom;
        } else {
            td.appendChild(moreHtmlOrDom);
        }
        tr.appendChild(td);

        // add menu
        td = document.createElement("td");
        td.appendChild(IIDXHelper.buildMenuDiv(menuLabelList, menuCallbackList));
        tr.appendChild(td);
        
        // done
        headerTable.appendChild(tr);
        return headerTable;
    };

    IIDXHelper.buildTableElem = function(headers, rows, optSortCol, optSortDesc) {
        if (optSortCol) {
            if (optSortDesc) {
                rows.sort((a, b) => a[optSortCol] < b[optSortCol] ? 1 : -1)
            } else {
                rows.sort((a, b) => a[optSortCol] > b[optSortCol] ? 1 : -1)
            }
        }
        var dom = document.createElement("table");

        var tr = document.createElement("tr");
        dom.appendChild(tr);

        for (var h of headers) {
            var th = document.createElement("th");
            tr.appendChild(th);
            th.innerHTML = IIDXHelper.htmlSafe(h);
        }

        for (var row of rows) {
            var tr = document.createElement("tr");
            dom.appendChild(tr);

            for (var cell of row) {
                var td = document.createElement("td");
                tr.appendChild(td);
                td.innerHTML = IIDXHelper.htmlSafe(cell);
            }
        }

        return dom;

    };

    IIDXHelper.truncateTableRows = function(t, size) {
        var s = size + 1; // don't count the first (header row)
        if (t.rows.length > s) {
            var origSize = t.rows.length - 1;  // don't count header
            while (t.rows.length > s) {
          	     t.deleteRow(s);
            }
            var r = t.insertRow();
            t.rows[s].innerHTML = "<td colspan='0'>...[" + origSize + " rows] truncated to " + size + "</td>";
        }
    };

    IIDXHelper.buildDatagridInDiv = function (div, colsCallback, dataCallback, optFinishedCallback, optSortList) {
        //
        // PARAMS:
        //   div - html div in which to put the menu and table
        //   gridFilterFlag - an iidx pass-through
        //   colsCallback - datagridAoColumns type of callback
        //   dataCallback - datagridAaData type of callback
        //   menuStringList - list of menu items
        //   menuCallbackList - list of callbacks for menu items
        //   optFinishedCallback - call when done
        //   optSortList - default sort.  default is [[0,'asc']].  For no sorting, use []
        //
        // RETURNS the table element

        // make up some random names
        var dataTableName = IIDXHelper.getNextId("table");
        var tableId = IIDXHelper.getNextId("table");

        var finishedCallback = (typeof optFinishedCallback == 'undefined' || optFinishedCallback == null) ? function(){} : optFinishedCallback;
        var sortList = (typeof optSortList == 'undefined') ? [[ 0, 'asc' ]] : optSortList;

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
                'aaSorting':   sortList,
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
