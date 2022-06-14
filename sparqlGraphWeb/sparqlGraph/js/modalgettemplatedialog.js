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
    'sparqlgraph/js/msiclientingestion',
    'sparqlgraph/js/modaliidx',
    'sparqlgraph/js/iidxhelper',
    'sparqlgraph/js/sparqlgraphjson',

    // shimmed
    'jquery'
    ],

	function(MsiClientIngestion, ModalIidx, IIDXHelper, SparqlGraphJson, jquery) {

		/**
		 *
		 */
		var ModalGetTemplateDialog= function (ingestServiceUrl, conn, oInfo) {
            this.iClient = new MsiClientIngestion(ingestServiceUrl);
            this.ngCallback = function() {};
            this.conn = conn;
            
            noId = undefined;
            classNames = oInfo.getClassNames();
            textVals = [];
            for (uri of classNames) {
				textVals.push([uri.split("/").at(-1), uri]);
			}
			textVals.sort(function(a,b){return a[1].localeCompare(b[1]);})
			this.classSelect = IIDXHelper.createSelect(noId, textVals, [], false, "input-xlarge");
			this.idRegexInput = IIDXHelper.createTextInput(noId, "input-xlarge");	
			this.okCallback = function(){throw new Error("this.okCallback is not set");};
		};


		ModalGetTemplateDialog.prototype = {

            show : function () {
                IIDXHelper.showDiv(this.div);
            },

            hide : function () {
                IIDXHelper.hideDiv(this.div);
            },

			okButtonCallback : function() {
				// already confirmed by validateCallback()
				var classUri = IIDXHelper.getSelectValues(this.classSelect)[0];
				var idRegex = this.idRegexInput.value;
				var okCallback2 = function(sgJsonJson) {
						this.okCallback(classUri, sgJsonJson);
				}.bind(this);
				this.iClient.execGetClassTemplate(classUri, this.conn, idRegex, okCallback2);  
			},
			
			validateCallback : function() {
				if (IIDXHelper.getSelectValues(this.classSelect).length != 1) {
					return "No class is selected.";
				} else {
					return null;
				}
			},

			//
			//  callback(classUri, sgJsonJson) -
			//
	        launch : function (okCallback) {
				this.okCallback = okCallback;
				
				div = document.createElement("div");
				
				form = IIDXHelper.buildHorizontalForm()
    			fieldset = IIDXHelper.addFieldset(form)
    			
 				fieldset.appendChild(IIDXHelper.buildControlGroup("class: ", this.classSelect, "Retrieve this class' template nodegroup"));
 				fieldset.appendChild(IIDXHelper.buildControlGroup("id regex: ", this.idRegexInput, "Regex to identify unique id properties"));
 				
     			div.appendChild(form);
     			
				// launch the modal
				ModalIidx.okCancel(	"Retrieve class template nodegroup",
                   	div,
					this.okButtonCallback.bind(this),
					undefined,
					undefined,
					this.validateCallback.bind(this)
					);
			},
	
		};
	
		return ModalGetTemplateDialog;
	}
);
