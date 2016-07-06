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
        	'sparqlgraph/js/importtrans',
         	'sparqlgraph/js/modaliidx',
         	'sparqlgraph/js/iidxhelper',

			// shimmed
         	//'logconfig',
		],

	function(ImportTransform, ModalIidx, IIDXHelper) {
	
		/*
		 *    A column name or text or some item used to build a triple value
		 */
		var ImportTransformModal = function (iTrans) {
			 this.iTrans = iTrans;
			 this.body = null;
		};
		
		ImportTransformModal.VAR = "text";
		
		ImportTransformModal.prototype = {
			buildBody : function () {
				
				this.body = document.createElement("div");
				
				var nameForm = document.createElement("form");
				nameForm.className = "form-horizontal";
				nameForm.onsubmit = function(){return false;};    // NOTE: forms shouldn't submit automatically on ENTER
				this.body.appendChild(nameForm);
				
				var fieldset = document.createElement("fieldset");
				nameForm.appendChild(fieldset);
				
				// Name
				var input;
				input = document.createElement("input");
				input.id = "tm_name_text";
				input.type = "text";
				input.class = "input-small";
				
				var g = IIDXHelper.buildControlGroup("Name", input);
				fieldset.appendChild(g);
				
				// Type
				var select;
				select = document.createElement("select");
				select.id = "tm_type_select";
				select.onchange = this.changeType.bind(this);
				
				var g = IIDXHelper.buildControlGroup("Type", select);
				fieldset.appendChild(g);
				
				// arg1
				var input1;
				input1 = document.createElement("input");
				input1.id = "tm_arg1_text";
				input1.type = "text";
				input1.class = "input-medium";
				
				var g1 = IIDXHelper.buildControlGroup("Arg1", input1);
				g1.id = "group-arg1";
				fieldset.appendChild(g1);
				
				// arg2
				var input2;
				input2 = document.createElement("input");
				input2.id = "tm_arg2_text";
				input2.type = "text";
				input2.class = "input-medium";
				
				var g2 = IIDXHelper.buildControlGroup("Arg2", input2);
				g2.id = "group-arg2";
				fieldset.appendChild(g2);
			},
			
			fillBody : function () {
				
				//====== name ======
				var input = document.getElementById("tm_name_text");
				input.value = (this.iTrans !== null) ? this.iTrans.getName() : "";
				
				
				//====== type select ======
				var select = document.getElementById("tm_type_select");
				
				// remove all options
				while (select.options.length > 0) { select.options.remove(0);}
				
				// add
				
				for (var i=0; i < ImportTransform.TRANSFORMS.length; i++) {
					// add each option
					var option = document.createElement("option");
					option.text = ImportTransform.TRANSFORMS[i][0];
					option.value = ImportTransform.TRANSFORMS[i][1];
					select.options.add(option);
					
					// select if it matches iTrans
					if (this.iTrans && this.iTrans.getType() == valueList[i]) {
						select.selectedIndex = i;
					}
				}
				
	
				var argNames = ImportTransform.getArgNames(select.value);
				
				// set arg values
				document.getElementById("tm_arg1_text").value = this.iTrans ? this.iTrans.getArg1() : "";
				document.getElementById("tm_arg2_text").value = this.iTrans ? this.iTrans.getArg2() : "";

				// set arg labels etc.
				this.changeType();
			},
			
			changeType : function () {
				// set argument names and enable/disable based on select.value
				
				var select = document.getElementById("tm_type_select");
				var argNames = ImportTransform.getArgNames(select.value);
				
				// arg1 - set label
				var g1 = document.getElementById("group-arg1");
				IIDXHelper.setControlGroupLabel(g1, argNames[0]);
				
				// arg1 - enable or disable
				var textElem1 = document.getElementById("tm_arg1_text");
				if (argNames[0] == "") { 
					textElem1.value = ""; 
					textElem1.classList.add("disabled");
					textElem1.disabled = true;
				} else {
					textElem1.classList.remove("disabled");
					textElem1.disabled = false;
				}
			
				// arg2 - set label
				var g2 = document.getElementById("group-arg2");
				IIDXHelper.setControlGroupLabel(g2, argNames[1]);
				
				// arg2 - enable or disable
				var textElem2 = document.getElementById("tm_arg2_text");
				if (argNames[1] == "") { 
					textElem2.value = ""; 
					textElem2.classList.add("disabled");
					textElem2.disabled = true;
					
				} else {
					textElem2.classList.remove("disabled");
					textElem2.disabled = false;
				}
				
			},
			
			validate : function() {
				var name = document.getElementById("tm_name_text").value;
				var type = document.getElementById("tm_type_select").value;
				var arg1 = document.getElementById("tm_arg1_text").value;
				var arg2 = document.getElementById("tm_arg2_text").value;
				
				if (name.length < 1) {
					return "Name can not be empty.";
				} else if (arg1.length < 1) {
					return "Arg 1 can not be empty.";
				} else {
					return null;
				}
			},
			
			okCallback : function(userCallback) {
				kdlLogEvent("Created New Transform");
				
				var name = document.getElementById("tm_name_text").value;
				var type = document.getElementById("tm_type_select").value;
				var arg1 = document.getElementById("tm_arg1_text").value;
				var arg2 = document.getElementById("tm_arg2_text").value;
				
				if (this.iTrans == null) {
					this.iTrans = new ImportTransform(name, type, arg1, arg2);
					userCallback(this.iTrans, true);
				} else {
					this.iTrans.setName(name);
					this.iTrans.setType(type);
					this.iTrans.setArg1(arg1);
					this.iTrans.setArg2(arg2);

					userCallback(this.iTrans, false);
				}
			},
			
			cancelCallback : function () {
				kdlLogEvent("Cancel Edit Transform");

			},
				
			show : function (userCallback) {
				this.buildBody();
				
				// launch
				var m = new ModalIidx("transformModal");
				m.showOKCancel(	"Transform", 
								this.body, 
								this.validate.bind(this), 
								this.okCallback.bind(this, userCallback), 
								this.cancelCallback.bind(this));
				
				kdlLogEvent("Launch Transform Modal");
				this.updateAll();
			},
			
			updateAll : function () {
				
				this.fillBody();
			},
			
		};
	
		return ImportTransformModal;            
	}
);