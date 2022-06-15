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

define([	// properly require.config'ed   bootstrap-modal
        	'sparqlgraph/js/iidxhelper',
        	'jquery',
        	'bootstrap/bootstrap-modal',
         	'bootstrap/bootstrap-transition',

			// shimmed
            // 'logconfig',
		],

	function(IIDXHelper, $) {

		/*
		 *    A column name or text or some item used to build a triple value
		 */
		var ModalIidx = function (optUniqueId) {
			// optUniqueId is the html element id.  Only needed if there is a conflict or nested modals.
			this.id = (typeof optUniqueId !== "undefined") ? optUniqueId : "myModal";
			this.div = null;

            this.butCallbacks = [];
		};

		ModalIidx.CONSTANT = "text";

		ModalIidx.HTML_SAFE = true;      // flag: disrespect anything that looks like html markup.  Show raw tag characters
		ModalIidx.HTML_ALLOW = false;    //       respect html markup.

		ModalIidx.alert = function (titleTxt, msgHtml, cleanHtmlFlag, optCallback) {
			// clean up the html tags if flag is set
			var msgHtml2 = (typeof cleanHtmlFlag === "undefined" || ! cleanHtmlFlag) ? msgHtml : IIDXHelper.htmlSafe(msgHtml);

			// simple alert dialog
		    kdlLogEvent("Alert", "title", titleTxt, "message", msgHtml);

			var m = new ModalIidx("ModalIidxAlert");
			var div = document.createElement("div");
			div.innerHTML = msgHtml2;
            m.onHide(optCallback);
			m.showOK(titleTxt, div, function(){});
            return m;
		};

        //
        // msgHtmlOrDom - can be an html string or a dom
        //
		ModalIidx.okCancel = function (titleTxt, msgHtmlOrDom, okCallback, optOkButtonText, optCancelCallback, optValidate, optOkClass) {
			// ok cancel

            var dom;

			var m = new ModalIidx("ModalIidxOkCancel");
            if (typeof msgHtmlOrDom == "string") {
    			dom = document.createElement("div");
    			dom.innerHTML = msgHtmlOrDom;
            } else {
                dom = msgHtmlOrDom;
            }
            kdlLogEvent("OkCancel", "title", titleTxt, "message", dom.innerHTML.substr(0, 100));
			m.showOKCancel(	titleTxt,
							dom,
							optValidate ? optValidate : function() {return null;},
							okCallback,
							optCancelCallback,
							optOkButtonText,
                            undefined,
                            optOkClass
							);
            return m;
		};

		ModalIidx.clearCancelSubmit = function (titleTxt, dom, clearCallback, submitCallback, optOKButText, optWidthPercent, optValidateCallback) {
            var validateCallback = optValidateCallback ? optValidateCallback : function(){return null;};
		    kdlLogEvent("clearCancelSubmit", "title", titleTxt);

			var div = document.createElement("div");
			div.appendChild(dom);

			var m = new ModalIidx("clearCancelSubmit");
			m.showClearCancelSubmit(titleTxt,
									div,
									validateCallback,
									clearCallback,
									submitCallback,
									optOKButText,
									optWidthPercent
									);
            return m;
		};

		/**
		 * Show a select,
		 * submitCallback(v)  where v is selected value or null.
		 */
		ModalIidx.selectOption = function (titleTxt, textValArray, submitCallback, optOKButText, optWidthPercent, optDisabledList) {
			kdlLogEvent("selectOption", "title", titleTxt);
			var div = document.createElement("div");
			var select = IIDXHelper.createSelect("mdSelectOption_select", textValArray, [], false, "", optDisabledList);
			select.size="20";
			select.style.width = "95%";
			div.appendChild(select);

			var m = new ModalIidx("selectOption");
			m.showClearCancelSubmit(titleTxt,
									div,
									function() {return null;},
									function() {select.selectedIndex = -1; },
									function() {
										var val = select.selectedIndex == -1 ? null : select.options[select.selectedIndex].value;
										submitCallback(val);
									},
									optOKButText,
									optWidthPercent
									);
		};

        ModalIidx.choose = function (headerText, msgHtml, buttonNameList, callbackList, optWidthPercent) {
            kdlLogEvent("choicesDialog", "title", headerText);
			var div = document.createElement("div");
            div.innerHTML = msgHtml;

            var m = new ModalIidx("ModalIidx_choicesDialog");
            m.showChoices(headerText, div, buttonNameList, callbackList, optWidthPercent);
        };
        
        /*
         * Create a simple text input dialog with a label 
         *   callback(input_string)
         *   inputClass - one of: input-small input-medium input-large input-xlarge
         */
        ModalIidx.input = function (headerText, labelHtml, defaultVal, inputClass, callback, optWidthPercent) {
        
            var m = new ModalIidx("ModalIidx_inputDialog");
            m.showInput(headerText, labelHtml, defaultVal, inputClass, callback, optWidthPercent);
        };

        /*
         * slightly more modern listDialog supporting multi-select
         *
         * textValArray is an array of texts, or [text, val] pairs
         * selTextsList are texts for options that should be selected by default
         * valListCallback([val list]) where values will match text if no values were given
         *
         * optHideCallback will be called any time the dialog is hidden
         */
        ModalIidx.multiListDialog = function (headerText, buttonLabel, textValArray, selTextsList, valListCallback, optMultiFlag, optHideCallback, optSize, optWidthPercent) {
            var multiFlag = (typeof optMultiFlag != "undefined") ? optMultiFlag          : true;
            var size = (typeof optSize != "undefined") ? optSize                         : "6";

            var div = document.createElement("div");
            div.align = "center";

            var select = IIDXHelper.createSelect("ModalIidx_showList_sel", textValArray, selTextsList, multiFlag);
            select.size = size;
            select.style.width = "100%";
            div.appendChild(select);

            // callbacks
            var listDialogSubmit = function (sel, cback) {
                cback(IIDXHelper.getSelectValues(sel));
            }.bind(this, select, valListCallback);

            var listDialogValidate = function (sel) {
                if (sel.selectedIndex == -1) {
                    return "Nothing is selected";
                } else {
                    return null;
                }
            }.bind(this, select);

            var m = new ModalIidx("ModalIidx_multiListDialog");
            m.onHide(optHideCallback);
            m.showOKCancel(headerText, div, listDialogValidate, listDialogSubmit, function(){}, buttonLabel, optWidthPercent);

        };

        // old ModalDialog.listDialog
        ModalIidx.listDialog = function (headerText, buttonLabel, nameArray, valArray, defaultIndex, callback, optWidthPercent, extraDOM) {

            var div = document.createElement("div");
            div.align = "center";

            // create the select
            var textValArray = [];
            for (var i=0; i < nameArray.length; i++) {
                textValArray.push([nameArray[i], i.toString()]);
            }

            var select = IIDXHelper.createSelect("ModalIidx_showList_sel", textValArray, [nameArray[defaultIndex]]);
            select.size = "6";
            select.style.width = "100%";
            div.appendChild(select);

            if (extraDOM) {
                div.appendChild(extraDOM);
            }

            // callbacks
            var listDialogSubmit = function (sel, vArr, cback) {
                cback(vArr[sel.selectedIndex]);
            }.bind(this, select, valArray, callback);

            var listDialogValidate = function (sel) {
                if (sel.selectedIndex == -1) {
                    return "Nothing is selected";
                } else {
                    return null;
                }
            }.bind(this, select);

            var m = new ModalIidx("ModalIidx_listDialog");
            m.showOKCancel(headerText, div, listDialogValidate, listDialogSubmit, function(){}, buttonLabel, optWidthPercent);

        };


        ModalIidx.createInfoButton = function(helpHTML, optId, optFloatRight) {
            var button = IIDXHelper.createIconButton("icon-info-sign", ModalIidx.alert.bind(this, "Info", helpHTML, false, function(){}), ["icon-white", "btn-small", "btn-info"], optId);
            if (optFloatRight) {
                button.style.float="right";
            }
            return button;
        };

        ModalIidx.createWikiButton = function(page, optId) {
            return IIDXHelper.createIconButton("icon-info-sign",
                                                function() {
                                                    window.open(g.help.url.base + "/" + page, "_blank","location=yes");
                                                },
                                                ["icon-white", "btn-small", "btn-default"],
                                                optId);
        };

        ModalIidx.createLinkButton = function(link, optId) {
            return IIDXHelper.createIconButton("icon-info-sign",
                                                function() {
                                                    window.open(link, "_blank","location=yes");
                                                },
                                                ["icon-white", "btn-small", "btn-default"],
                                                optId);
        };

		ModalIidx.prototype = {

            showOK : function (headerText, bodyDOM, callback, optWidthPercent) {
                // show a modal with header, body and callback.

                this.div = this.createModalDiv(optWidthPercent);

                if (headerText && headerText.length > 0) {
                    var header = this.createHeader(headerText);
                    this.div.appendChild(header);
                }

                //----- body -----
                var body = document.createElement("div");
                body.className="modal-body";
                body.appendChild(bodyDOM);
                this.div.appendChild(body);

                var footer = this.createOKFooter(callback);
                this.div.appendChild(footer);

                $(this.div).modal('show');
            },

            showOKCancel : function (headerText, bodyDOM, validate, callbackSuccess, callbackCancel, optOkButtonText, optWidthPercent, optOkClass) {
                // show a modal with header, body and callback.
                // validate must return one of:
                //      error message : display an alert
                //      null : call callbackSuccess

                var okButText = (typeof optOkButtonText == "undefined") ? "OK" : optOkButtonText;
                this.div = this.createModalDiv(optWidthPercent);

                var header = this.createHeader(headerText);
                this.div.appendChild(header);

                //----- body -----
                var body = document.createElement("div");
                body.className="modal-body";
                body.appendChild(bodyDOM);
                this.div.appendChild(body);

                var footer = this.createFooter("Cancel", callbackCancel, okButText, validate, callbackSuccess, optOkClass);
                this.div.appendChild(footer);

                $(this.div).modal('show');
            },

            showClearCancelSubmit : function (headerText, bodyDOM, validateCallback, clearCallback, submitCallback, optSubmitButtonText, optWidthPercent) {
                // show a modal with header, body and callback.
                // validate must return one of:
                //      error message : display an alert
                //      null : call callbackSuccess

                this.div = this.createModalDiv(optWidthPercent);

                var header = this.createHeader(headerText);
                this.div.appendChild(header);

                //----- body -----
                var body = document.createElement("div");
                body.className="modal-body";
                body.appendChild(bodyDOM);
                this.div.appendChild(body);

                var footer = this.createClearCancelSubmitFooter(clearCallback, validateCallback, submitCallback, optSubmitButtonText);
                this.div.appendChild(footer);

                $(this.div).modal('show');
            },

            showClearCancelValSubmit : function (headerText, bodyDOM, clearCallback, valSubmitCallback, optSubmitButtonText, optWidthPercent) {
                // show a modal with header, body and callback.
                // valSubmitCallback(closeModalCallback) - is responsible for validating, acting, and closing the modal (or not) all in one

                this.div = this.createModalDiv(optWidthPercent);

                var header = this.createHeader(headerText);
                this.div.appendChild(header);

                //----- body -----
                var body = document.createElement("div");
                body.className="modal-body";
                body.appendChild(bodyDOM);
                this.div.appendChild(body);

                var footer = this.createClearCancelValSubmitFooter(clearCallback, valSubmitCallback, optSubmitButtonText);
                this.div.appendChild(footer);

                $(this.div).modal('show');
            },

            showChoices : function (headerText, bodyDOM, buttonNameList, callbackList, optWidthPercent, optButtonClassList, optButtonIdList) {
                // show modal with optional number of buttons
                // matching number of callbacks
                // no validation functionality
                // last button is always the primary choice

                this.div = this.createModalDiv(optWidthPercent);

                var header = this.createHeader(headerText);
                this.div.appendChild(header);

                //----- body -----
                var body = document.createElement("div");
                body.className="modal-body";
                body.appendChild(bodyDOM);
                this.div.appendChild(body);


                // make the last button the default and others from optButtonClassList if any
                var classList = [];
                for (var i=0; i < buttonNameList.length; i++) {
                    if (i == buttonNameList.length - 1) {
                        classList.push("btn-primary");
                    } else if (optButtonClassList && optButtonClassList[i]) {
                        classList.push(optButtonClassList[i]);
                    } else {
                        classList.push("");
                    }
                }

                // ----- footer -----
                var footer = this.createChoicesFooter(buttonNameList, callbackList, classList, optButtonIdList);
                this.div.appendChild(footer);

                $(this.div).modal('show');
            },
 
			showInput : function (headerText, label, defaultVal, inputClass, callback, optWidthPercent) {
				this.div = this.createModalDiv(optWidthPercent);

                var header = this.createHeader(headerText);
                this.div.appendChild(header);

                //----- body -----
                var body = document.createElement("div");
                body.className="modal-body";
                this.div.appendChild(body);
                
                var form = IIDXHelper.buildHorizontalForm();
				var fieldset = IIDXHelper.addFieldset(form);
				var input =  IIDXHelper.createTextInput(undefined, inputClass);
				input.value = defaultVal;
				fieldset.appendChild(IIDXHelper.buildControlGroup(label + ":", input));
				body.appendChild(form);
				
				  // ----- footer -----
                var footer = this.createCancelSubmitFooter(function(){}, function() { callback(input.value);}, "OK");
                this.div.appendChild(footer);

                $(this.div).modal('show');
			},
			
            createModalDiv : function (optWidthPercent) {
                // make sure modal exists and is attached to document.body

                // get rid of modal <div> if it already exists
                var leftover = document.getElementById(this.id);
                if (leftover) {
                    document.body.removeChild(leftover);
                }

                // create the modal div
                var modal = document.createElement("div");
                modal.className = "modal hide fade";
                var style = "display: none;";

                if (typeof optWidthPercent == "number") {
                    modal.style.margin = "0 auto auto 0";
                    modal.style.width = optWidthPercent + "%";
                    modal.style.left = (100 - optWidthPercent)/2 + "%";
                    style += "margin: 0 auto auto 0; width: " + optWidthPercent + "%; left: " + (100 - optWidthPercent)/2 + "%;";
                }

                modal.id = this.id;
                document.body.appendChild(modal);

                return modal;
            },

            hide : function () {
                $(this.div).modal('hide');
            },

            createHeader : function(title) {
                //----- header -----
                var header = document.createElement("div");
                header.className="modal-header";

                var h3 = document.createElement("h3");
                h3.innerHTML = title;
                header.appendChild(h3);

                return header;
            },

            createOKFooter : function(callback) {
                //----- footer -----
                var footer = document.createElement("div");
                footer.className = "modal-footer";

                var callback1 = function (cb) {
                    cb();
                    this.hide();
                }.bind(this, callback);

                var a = IIDXHelper.createButton("OK", callback1, ["btn-primary"]);
                footer.appendChild(a);

                return footer;
            },

            createCancelSubmitFooter : function(validateCallback, submitCallback, optSubmitButText) {
                //----- footer -----
                var footer = document.createElement("div");
                footer.className = "modal-footer";

                var callback2 = function () {
                    this.hide();
                }.bind(this);
                var a2 = IIDXHelper.createButton("Cancel", callback2, []);  // used to be "btn-danger"
                footer.appendChild(a2);

                var callback3 = function (valCb, subCb) {
                    var msg = valCb();
                    if (msg) {
                        alert(msg);
                    } else {
                        subCb();
                        this.hide();
                    }
                }.bind(this, validateCallback, submitCallback);
                var submitButText = typeof optSubmitButText != "undefined" ? optSubmitButText : "Submit";
                var a3 = IIDXHelper.createButton(submitButText, callback3, ["btn-primary"]);
                footer.appendChild(a3);

                return footer;
            },

            createClearCancelSubmitFooter : function(clearCallback, validateCallback, submitCallback, optSubmitButText) {
                //----- footer -----
                var footer = document.createElement("div");
                footer.className = "modal-footer";

                var callback1 = function (cb) {
                    cb();
                    return false;
                }.bind(this, clearCallback);
                var a1 = IIDXHelper.createButton("Clear", callback1 );
                footer.appendChild(a1);

                var callback2 = function () {
                    this.hide();
                }.bind(this);
                var a2 = IIDXHelper.createButton("Cancel", callback2);  // , ["btn-danger"]
                footer.appendChild(a2);

                var callback3 = function (valCb, subCb) {
                    var msg = valCb();
                    if (msg) {
                        alert(msg);
                    } else {
                        subCb();
                        this.hide();
                    }
                }.bind(this, validateCallback, submitCallback);

                var submitButText = typeof optSubmitButText != "undefined" ? optSubmitButText : "Submit";
                var a3 = IIDXHelper.createButton(submitButText, callback3, ["btn-primary"]);
                footer.appendChild(a3);

                return footer;
            },

            createClearCancelValSubmitFooter : function(clearCallback, valSubmitCallback, optSubmitButText) {
                //----- footer -----
                var footer = document.createElement("div");
                footer.className = "modal-footer";

                var callback1 = function (cb) {
                    cb();
                    return false;
                }.bind(this, clearCallback);
                var a1 = IIDXHelper.createButton("Clear", callback1 );
                footer.appendChild(a1);

                var callback2 = function () {
                    this.hide();
                }.bind(this);
                var a2 = IIDXHelper.createButton("Cancel", callback2); //, ["btn-danger"]
                footer.appendChild(a2);

                var callback3 = function (vsCb) {
                        vsCb(   this.hide.bind(this) );
                }.bind(this, valSubmitCallback);

                var submitButText = typeof optSubmitButText != "undefined" ? optSubmitButText : "Submit";
                var a3 = IIDXHelper.createButton(submitButText, callback3, ["btn-primary"]);
                footer.appendChild(a3);

                return footer;
            },

            createFooter : function(but1text, callback1, but2text, validate2, callback2, optOkClass) {
                //----- footer -----
                var footer = document.createElement("div");
                footer.className = "modal-footer";

                var a1 = IIDXHelper.createButton(but1text, callback1); //, (but1text=="Cancel") ? ["btn-danger"] : undefined);
                a1.setAttribute("data-dismiss", "modal");
                footer.appendChild(a1);

                var callback1 = function () {
                    var msg = validate2();
                    if (msg) {
                        if (msg.startsWith("<") || msg.indexOf("<br>") > -1 || msg.indexOf("<BR>") > -1) {
                            ModalIidx.alert("Validation Failure", msg, false);
                        } else {
                            alert(msg);
                        }
                    } else {
                        callback2();
                        this.hide();
                    }
                }.bind(this);

                var a2 = IIDXHelper.createButton(but2text, callback1, [optOkClass || "btn-primary"]);
                footer.appendChild(a2);

                return footer;
            },

            /**
              *  Create arbitrary number of buttons.
              *  All buttons hide the dialog when clicked.
              *  classList has extra classes for each button:
              *      - ""
              *      - btn-primary
              *      - btn-danger
             **/
            createChoicesFooter : function(buttonNameList, callbackList, classList, optButtonIdList) {
                var footer = document.createElement("div");
                footer.className = "modal-footer";

                for (var i=0; i < buttonNameList.length; i++) {

                    var click = function (callback) {
                        callback();
                        this.hide();
                    }.bind(this, callbackList[i]);

                    var butClassList = (classList.length > i  && classList[i] != "") ? [classList[i]] : undefined;
                    var a1 = IIDXHelper.createButton(buttonNameList[i], click, butClassList);
                    a1.setAttribute("data-dismiss", "modal");
                    if (optButtonIdList && optButtonIdList[i]) {
                        a1.id = optButtonIdList[i];
                    }

                    footer.appendChild(a1);
                }

                return footer;
            },

            /*
             * Disable everything that looks like a button
             * Since <a> can be class "btn" the only way to disable is to remove the callback
             */
            disableButtons : function () {
                var butList = this.div.getElementsByClassName("btn");
                for (var i=0; i < butList.length; i++) {
                    butList[i].disabled = true;
                    butList[i].style.backgroundColor="lightgray";  // .classList.add("btn-disabled") doesn't seem to work on these since they can be <a>
                    this.butCallbacks[i] = butList[i].onclick
                    butList[i].onclick = function(){};
                }
            },

            /*
             * Undo disableButtons()
             */
            enableButtons : function () {
                var butList = this.div.getElementsByClassName("btn");
                for (var i=0; i < butList.length; i++) {
                    butList[i].disabled = false;
                    butList[i].style.backgroundColor="";
                    butList[i].onclick = this.butCallbacks[i]
                }
            },

            /*
             * Call this callback when modal finishes, no matter what
             */
            onHide : function(optCallback) {
                $(document).unbind('hide');
                if (typeof optCallback != "undefined" && optCallback) {
                    $(document).on('hide', '#'+this.id, optCallback);
                }
            }
		};

		return ModalIidx;            // return the constructor
	}
);
