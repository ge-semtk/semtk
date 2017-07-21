/**
 ** Copyright 2016 General Electric Company
 **
 ** Authors:  Paul Cuddihy, Jenny Williams
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
    'sparqlgraph/js/modaliidx',
    'sparqlgraph/js/iidxhelper',
    'sparqlgraph/js/runtimeconstraints',

    // shimmed
    'jquery'
    ],

	function(ModalIidx, IIDXHelper, RuntimeConstraints, jquery) {
	
        //   Supported runtime-constrainable types (from com.ge.research.semtk.load.utility.ImportSpecHandler):
        //   from the XSD data types:
        //   string | boolean | decimal | int | integer | negativeInteger | nonNegativeInteger | 
        //   positiveInteger | nonPositiveInteger | long | float | double | duration | 
        //   dateTime | time | date | unsignedByte | unsignedInt | anySimpleType |
        //   gYearMonth | gYear | gMonthDay;
        //   added for the runtimeConstraint:
        //   NODE_URI
    
        // dropdown operator choices for different types
        var operatorChoicesForStrings = ["=", "regex"];
        var operatorChoicesForUris = ["="];
        var operatorChoicesForNumerics = [  "=",">", ">=", "<", "<="];
        var operandChoicesForBoolean = ["true","false","unspecified"];
    
        var isNumericType = function(dataType){
            // TODO only INT has been tested so far
            if(dataType == "INT" || dataType.indexOf("INTEGER") >= 0){
                return true;                
            }
            if(dataType == "DECIMAL" || dataType == "LONG" || dataType == "FLOAT" || dataType == "DOUBLE"){
                return true;
            }
            return false;
        }
        
        var isInteger = function(value){
            if((parseFloat(value) == parseInt(value)) && !isNaN(value)){
                return true;
            } else {
                return false;
            }
        }
    
		/**
		 * A dialog allowing users to populate runtime constraints.
         * Callback provides json for runtime constraint object.  Sample: 
         *
         * suggestValuesFunc(rtConstraints, item, msiOrQsResultCallback, failureCallback, statusCallback)
		 */
		var ModalRuntimeConstraintDialog = function (suggestValuesFunc) {
            this.m = new ModalIidx();      
            this.div = null;
            this.sparqlIds = null;
            this.callback = function () {};
            this.suggestValuesFunc = suggestValuesFunc;
		};		
		
        /* 
         * store a value or a list in an operand element
         */
        ModalRuntimeConstraintDialog.setOperandElemValue = function (elem, valOrList) {
            
            if (typeof valOrList == "object" && valOrList.length) {
                var list = valOrList;
                if (list.length == 1) {
                    elem.value = list[0];
                } else if (list.length > 1) {
                    elem.value = JSON.stringify(list);
                } else {
                    elem.value = "";
                }
            } else {
                // handle regular values
                elem.value = valOrList;
            }
        };
    
        /*
         * Get an operand element's value as a list
         */
        ModalRuntimeConstraintDialog.getOperandValueList = function (elem) {
            
            // get lists packed by the dialog
            if (elem.value.length > 0 && elem.value[0] == '[') {
                try {
                    return JSON.parse(elem.value);
                } catch (e) {
                    // any failed-to-parse list is just a literal
                    return [ elem.value.trim() ];
                }
            }
            return [ elem.value.trim() ];
        };
    
        /*
         * is operand element's value a mangled list
         */
        ModalRuntimeConstraintDialog.isOperandValueSuspicious = function (elem) {
            
            if (elem.value.length > 0 && elem.value[0] == '[') {
                try {
                    return JSON.parse(elem.value);
                }  catch (e) {
                    return true;
                }
            }
            return false;
        };
    
		ModalRuntimeConstraintDialog.prototype = {
            
            show : function () {
                IIDXHelper.showDiv(this.div);
            },
            
            hide : function () {
                IIDXHelper.hideDiv(this.div);
            },
            
            clearCallback : function() {
                alert("clearCallback...do something");
			},
            
            
            // check for error conditions where we want to give the user a chance to correct their entries
            // TODO add more detailed checks for other numeric datatypes
            validateCallback : function() {
                      
                // validate each constraint item
                for(i = 0; i < this.sparqlIds.length; i++){
                    
                    var sparqlId = this.sparqlIds[i];
                    var valueType = this.valueTypes[i];
                    var operator1Element = document.getElementById("operator1" + sparqlId);
                    var operand1Element = document.getElementById("operand1" + sparqlId);
                    var operator2Element = document.getElementById("operator2" + sparqlId);
                    var operand2Element = document.getElementById("operand2" + sparqlId);    
                    
                    if(valueType == "NODE_URI"){                        
                        if(operand1Element.value.trim().indexOf(" ") > -1){
                            return "Error: invalid entry for " + sparqlId + ": uri cannot contain spaces";
                        }  
                    }else if(isNumericType(valueType)){ 
                        if(isNaN(operand1Element.value.trim())){    
                            return "Error: invalid entry for " + sparqlId.substring(1) + ": entry must be numeric"; 
                        }
                        if(valueType.indexOf("INT") >= 0 && operand1Element.value && !isInteger(operand1Element.value.trim())){
                            return "Error: invalid entry for " + sparqlId.substring(1) + ": entry must be an integer"
                        }
                        
                        // if a second operand was entererd
                        if(operand2Element.value){
                            if(isNaN(operand2Element.value)){  
                                return "Error: invalid entry for " + sparqlId.substring(1) + ": " + operand2Element.value; 
                            }
                            if(valueType.indexOf("INT") >= 0 && !isInteger(operand2Element.value.trim())){
                                return "Error: invalid entry for " + sparqlId.substring(1) + ": entry must be an integer"
                            }
                            if(!operand1Element.value){
                                // user entered the second operand, but not the first
                                return "Error: invalid entry for " + sparqlId.substring(1) + ": second operand entered without first operand";
                            }
                            var operatorCombination = operator1Element.value + operator2Element.value;  // quick way to check 
                            if(operatorCombination != "<>" && operatorCombination != "><" && operatorCombination != "<=>=" && operatorCombination != ">=<="){
                                // disallow any combos other than < > or <= >=
                                return "Error: unsupported combination of operators for " + sparqlId;
                            }
                        }
                    }          
                    
                }                
				return null;    // all checks passed
			},
            
            
            buildRuntimeConstraints : function (optSkipSparqlId) {
                var skipSparqlId = typeof optSkipSparqlId != "undefined" ? optSkipSparqlId : "";
                
                var runtimeConstraints = new RuntimeConstraints();
                
                // for each runtime constrainable item, add a runtime constraint
                for(j = 0; j < this.sparqlIds.length; j++){
                    
                    var sparqlId = this.sparqlIds[j];
                    
                    if (sparqlId == skipSparqlId) {
                        continue;
                    }
                    
                    var valueType = this.valueTypes[j];
                    var operator1Element = document.getElementById("operator1" + sparqlId);
                    var operand1Element = document.getElementById("operand1" + sparqlId);
                    var operator2Element = document.getElementById("operator2" + sparqlId);
                    var operand2Element = document.getElementById("operand2" + sparqlId);
                    
                    var operandValList1 = ModalRuntimeConstraintDialog.getOperandValueList(operand1Element);
                    var operandValList2 = operand2Element ? ModalRuntimeConstraintDialog.getOperandValueList(operand2Element) : [];

                    if(!operand1Element){
                        continue;   // data type is not supported yet, so user could not have entered it - skip
                    }
                    
                    // collect user input and create runtime constraint object (behavior varies per data type)
                    if(valueType == "STRING" || valueType == "NODE_URI"){
                        operator1 = operator1Element.value;
                        operand1 = operand1Element.value.trim();    // TODO support multiple operands for MATCHES
                        if(!operand1.trim()){  
                            // user did not enter an operand - skip
                        }else if(operator1 == "="){
                            runtimeConstraints.add(sparqlId, "MATCHES", operandValList1);
                        }else if(operator1 == "regex"){
                            runtimeConstraints.add(sparqlId, "REGEX", operandValList1);
                        }else{
                            // if get this alert, then a fix is needed in the code
                            alert("Skipping unsupported operator for " + sparqlId + ": " + operator1.value);
                            // TODO cancel instead of skipping?
                        }
                    }else if(valueType == "BOOLEAN"){         
                        var booleanSelected = null;
                        for(var i=0; i < operandChoicesForBoolean.length; i++){
                            if(document.getElementById("operand1" + sparqlId + "-" + operandChoicesForBoolean[i]).className == "btn active"){
                                booleanSelected = operandChoicesForBoolean[i];
                            }
                        }
                        switch(booleanSelected){
                            case("true"):
                                runtimeConstraints.add(sparqlId, "MATCHES", "1");
                                break;
                            case("false"):
                                runtimeConstraints.add(sparqlId, "MATCHES", "0");
                                break;
                            // do nothing if unspecified
                        }
                    }else if(isNumericType(valueType)){
                        var operator1 = operator1Element.value;
                        var operand1 = operand1Element.value;    // TODO support multiple operands for MATCHES
                        var operator2 = operator2Element.value;
                        var operand2 = operand2Element.value;
                        
                        if(operand1.trim() && !operand2.trim()){  
                            var operand1List = [operand1];       // PEC TODO: ignoring multiple values
                            
                            switch(operator1.trim()){
                                case("="):
                                    runtimeConstraints.add(sparqlId, "MATCHES", operandValList1);  
                                    break;
                                case("<"):
                                    // TODO complain or prevent multiple operands
                                    runtimeConstraints.add(sparqlId, "LESSTHAN", operand1List);
                                    break;
                                case("<="):
                                    runtimeConstraints.add(sparqlId, "LESSTHANOREQUALS", operand1List);
                                    break;  
                                case(">"):
                                    runtimeConstraints.add(sparqlId, "GREATERTHAN", operand1List);
                                    break;
                                case(">="):
                                    runtimeConstraints.add(sparqlId, "GREATERTHANOREQUALS", operand1List);
                                    break; 
                                default:
                                    // if get this alert, then a fix is needed in the code
                                    alert("Skipping unsupported operator for " + sparqlId + ": " + operator1.value);
                                    // TODO cancel instead of skipping
                            }
                        }else if(operand1.trim() && operand2.trim()){
                            var operandRange = [operand2, operand1];
                            
                            // user gave upper and lower bounds
                            if(operator1.trim() == ">" && operator2.trim() == "<"){
                                runtimeConstraints.add(sparqlId, "VALUEBETWEENUNINCLUSIVE", operandRange);
                            }else if(operator1.trim() == "<" && operator2.trim() == ">"){
                                runtimeConstraints.add(sparqlId, "VALUEBETWEENUNINCLUSIVE", operandRange);
                            }else if(operator1.trim() == ">=" && operator2.trim() == "<="){
                                runtimeConstraints.add(sparqlId, "VALUEBETWEEN", operandRange);
                            }else if(operator1.trim() == "<=" && operator2.trim() == ">="){
                                runtimeConstraints.add(sparqlId, "VALUEBETWEEN", operandRange);
                            }else{
                                // should never get here if validation is implemented correctly
                                alert("Skipping unsupported combination of operators for " + sparqlId + ": must be < and >, or <= and >=");
                                // TODO cancel instead of skipping
                            }
                        }
                        
                    }else{
                        alert("Type " + valueType + " not supported...add it");   
                    }

                }
                return runtimeConstraints;
            },
            
            /**
             * Build runtime constraint json and return it.
             * Basic validation has already been done in the validateCallback.
             */
			okCallback : function() {
                
                // call the callback with a RuntimeConstraints object                
                this.callback(this.buildRuntimeConstraints());
			},    
            
            /**
              * Got runtime constrainable items for the node group.  Build and launch a dialog for user to populate them.
              * resultSet contains a table with runtime constrainable items.
              */
            launchDialog : function (resultSet, callback) { 

                this.title = "Enter runtime constraints";
                this.callback = callback;

                this.div = document.createElement("div");

                this.sparqlIds = resultSet.getColumnStringsByName("valueId");
                //this.itemTypes = resultSet.getColumnStringsByName("itemType");   
                this.valueTypes = resultSet.getColumnStringsByName("valueType");
                //this.sparqlIds = ["?flavor","?circumference", "?frosting"]; // TODO REMOVE - FOR TESTING ONLY
                //this.valueTypes = ["STRING","INT","DOUBLE"];          // TODO REMOVE - FOR TESTING ONLY                      

                var isEquals = function(operatorId) {
                    return document.getElementById(operatorId).value == "=";
                };
                // create UI components for all runtime-constrained items 
                for(i = 0; i < this.sparqlIds.length; i++){

                    sparqlId = this.sparqlIds[i];                   // e.g. ?circumference
                    valueType = this.valueTypes[i];                 // e.g. STRING, INT, etc
                    operator1ElementId = "operator1" + sparqlId;	// e.g. "operator1?circumference"
                    operand1ElementId = "operand1" + sparqlId;		// e.g. "operand1?circumference"
                    operator2ElementId = "operator2" + sparqlId;	// e.g. "operator2?circumference"
                    operand2ElementId = "operand2" + sparqlId;		// e.g. "operand2?circumference"

                    this.div.appendChild(IIDXHelper.createLabel(sparqlId.substring(1) + ":", valueType));  // value type is a tooltip
                    // create UI components for operator/operand (varies per data type)
                    if(valueType == "STRING"){
                        this.div.appendChild(IIDXHelper.createSelect(operator1ElementId, operatorChoicesForStrings, ["="], false,  "input-mini"));
                        this.div.appendChild(IIDXHelper.createTextInput(operand1ElementId, "input-xlarge"));
                       
                        this.div.appendChild(IIDXHelper.createButton(">>", this.suggestValues.bind(this, operand1ElementId, sparqlId, isEquals.bind(this, operator1ElementId))));

                    }else if(valueType == "NODE_URI"){
                        this.div.appendChild(IIDXHelper.createSelect(operator1ElementId, operatorChoicesForUris, ["="], false, "input-mini"));
                        this.div.appendChild(IIDXHelper.createTextInput(operand1ElementId, "input-xlarge"));
                        this.div.appendChild(IIDXHelper.createButton(">>", this.suggestValues.bind(this, operand1ElementId, sparqlId, isEquals.bind(this, operator1ElementId))));

                    }else if(valueType == "BOOLEAN"){
                        this.div.appendChild(IIDXHelper.createButtonGroup(operand1ElementId, operandChoicesForBoolean, "buttons-radio"));
                        
                    }else if(isNumericType(valueType)){
                        this.div.appendChild(IIDXHelper.createSelect(operator1ElementId, operatorChoicesForNumerics, [">"], false, "input-mini"));
                        this.div.appendChild(IIDXHelper.createTextInput(operand1ElementId, "input-xlarge"));
                        this.div.appendChild(IIDXHelper.createButton(">>", this.suggestValues.bind(this, operand1ElementId, sparqlId, isEquals.bind(this, operator1ElementId))));
                        
                        this.div.appendChild(IIDXHelper.createLabel("  ")); // forces a newline
                        
                        this.div.appendChild(IIDXHelper.createSelect(operator2ElementId, operatorChoicesForNumerics, ["<"], false, "input-mini"));
                        this.div.appendChild(IIDXHelper.createTextInput(operand2ElementId, "input-xlarge"));
                        this.div.appendChild(IIDXHelper.createButton(">>", this.suggestValues.bind(this, operand2ElementId, sparqlId, isEquals.bind(this, operator2ElementId))));
                        
                    }else{
                        // TODO support all data types, and also handle in validateCallback and okCallback
                        this.div.appendChild(IIDXHelper.createLabel("...not supported yet..."));
                    }

                }
                
                // status
				var elem = document.createElement("div");
				elem.id = "mrtcstatus";
				elem.style.textAlign = "left";
				this.div.appendChild(elem);

                // launch the modal
                                      
                this.m.showClearCancelSubmit(
                                this.title,
                                this.div, 
                                this.validateCallback.bind(this),
                                this.clearCallback, 
                                this.okCallback.bind(this),
                                "Run"
                                );
            },            

            setStatus : function (msg) {
				document.getElementById("mrtcstatus").innerHTML= "<font color='red'>" + msg + "</font>";
			},
            
            suggestValues : function (elemId, sparqlId, multiCallback) {

                // successfully got a list of choices
                var suggestValueSuccess = function (elemId, sparqlId, mCallback, res) {
                    
                    // chose some choices
                    var choseCallback = function(elemId, valList) {
                        var elem = document.getElementById(elemId);
                        ModalRuntimeConstraintDialog.setOperandElemValue(elem, valList);
                        
                    }.bind(this, elemId);
                    
                    // get list of choices
                    res.sort();
                    var choiceList = res.getColumnStrings(0);
                    
                    this.m.disableButtons();
                    
                    // Launch a list of choices or an alert
                    if (choiceList.length > 0) {
                        ModalIidx.multiListDialog("Pick value of " + sparqlId, 
                                                  "OK", 
                                                  choiceList, 
                                                  ModalRuntimeConstraintDialog.getOperandValueList(document.getElementById(elemId)), 
                                                  choseCallback,
                                                  multiCallback(),
                                                  this.m.enableButtons.bind(this.m)
                                                 );
                    } else {
                        ModalIidx.alert("Constraints too tight",
                                        "No possible choices exist in the instance data.<br>Other constraints may be too tight.",
                                        false,
                                        this.m.enableButtons.bind(this.m)
                                       )
                    }
                    this.setStatus("");
                    
                }.bind(this, elemId, sparqlId, multiCallback);

                var failureCallback = function (msgHTML) {
                    ModalIidx.alert("Failure getting values", msgHTML);
                    this.setStatus("");
                }.bind(this);

                var statusCallback = function (percent) {
                    if (percent == NaN || percent == "NaN") {
                        console.log("not a number");
                    }
                    this.setStatus("Querying values " + percent.toString() + "%");
                }.bind(this);

                this.suggestValuesFunc(this.buildRuntimeConstraints(sparqlId),
                                       sparqlId,
                                       suggestValueSuccess,
                                       failureCallback,
                                       statusCallback,
                                      );

            },
        };
	
		return ModalRuntimeConstraintDialog;
	}
);