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
    'sparqlgraph/js/msiclientnodegroupstore',
    'sparqlgraph/js/modaliidx',
    'sparqlgraph/js/iidxhelper',
    'sparqlgraph/js/sparqlgraphjson',
    'sparqlgraph/js/runtimeconstraints',

    // shimmed
    'jquery'
    ],

	function(MsiClientNodeGroupStore, ModalIidx, IIDXHelper, SparqlGraphJson, RuntimeConstraints, jquery) {
	
        //   Supported runtime-constrainable types (from com.ge.research.semtk.load.utility.ImportSpecHandler):
        //   from the XSD data types:
        //   string | boolean | decimal | int | integer | negativeInteger | nonNegativeInteger | 
        //   positiveInteger | nonPositiveInteger | long | float | double | duration | 
        //   dateTime | time | date | unsignedByte | unsignedInt | anySimpleType |
        //   gYearMonth | gYear | gMonthDay;
        //   added for the runtimeConstraint:
        //   NODE_URI
    
        // dropdown operator choices for different types
        var operatorChoicesForStrings = ["=", "=",
                                   "regex", "regex"];
        var operatorChoicesForUris = ["=", "="];
        var operatorChoicesForNumerics = ["=", "=",
                                    ">", ">",
                                    ">=", ">=",
                                    "<", "<",
                                    "<=", "<="];
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
		 */
		var ModalRuntimeConstraintDialog= function () {
            this.div = null;
            this.sparqlIds = null;
            this.callback = function () {};
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
            
            
            /**
             * Build runtime constraint json and return it.
             * Basic validation has already been done in the validateCallback.
             */
			okCallback : function() {
                
                var runtimeConstraints = new RuntimeConstraints();
                
                // for each runtime constrainable item, add a runtime constraint
                for(j = 0; j < this.sparqlIds.length; j++){
                    
                    var sparqlId = this.sparqlIds[j];
                    var valueType = this.valueTypes[j];
                    var operator1Element = document.getElementById("operator1" + sparqlId);
                    var operand1Element = document.getElementById("operand1" + sparqlId);
                    var operator2Element = document.getElementById("operator2" + sparqlId);
                    var operand2Element = document.getElementById("operand2" + sparqlId);
                    
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
                            runtimeConstraints.add(sparqlId, "MATCHES", [operand1]);
                        }else if(operator1 == "regex"){
                            runtimeConstraints.add(sparqlId, "REGEX", [operand1]);
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
                        operator1 = operator1Element.value;
                        operand1 = operand1Element.value;    // TODO support multiple operands for MATCHES
                        operator2 = operator2Element.value;
                        operand2 = operand2Element.value;
                        
                        if(operand1.trim() && !operand2.trim()){  
                            switch(operator1.trim()){
                                case("="):
                                    // TODO support multiple operands for MATCHES
                                    runtimeConstraints.add(sparqlId, "MATCHES", [operand1], false);  
                                    break;
                                case("<"):
                                    runtimeConstraints.add(sparqlId, "LESSTHAN", [operand1], false);
                                    break;
                                case("<="):
                                    runtimeConstraints.add(sparqlId, "LESSTHANOREQUALS", [operand1], false);
                                    break;  
                                case(">"):
                                    runtimeConstraints.add(sparqlId, "GREATERTHAN", [operand1], false);
                                    break;
                                case(">="):
                                    runtimeConstraints.add(sparqlId, "GREATERTHANOREQUALS", [operand1], false);
                                    break; 
                                default:
                                    // if get this alert, then a fix is needed in the code
                                    alert("Skipping unsupported operator for " + sparqlId + ": " + operator1.value);
                                    // TODO cancel instead of skipping
                            }
                        }else if(operand1.trim() && operand2.trim()){
                            // user gave upper and lower bounds
                            if(operator1.trim() == ">" && operator2.trim() == "<"){
                                runtimeConstraints.add(sparqlId, "VALUEBETWEENUNINCLUSIVE", [operand1, operand2], false);
                            }else if(operator1.trim() == "<" && operator2.trim() == ">"){
                                runtimeConstraints.add(sparqlId, "VALUEBETWEENUNINCLUSIVE", [operand2, operand1], false);
                            }else if(operator1.trim() == ">=" && operator2.trim() == "<="){
                                runtimeConstraints.add(sparqlId, "VALUEBETWEEN", [operand1, operand2], false);
                            }else if(operator1.trim() == "<=" && operator2.trim() == ">="){
                                runtimeConstraints.add(sparqlId, "VALUEBETWEEN", [operand2, operand1], false);
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
                
                // call the callback with a RuntimeConstraints object                
                this.callback(runtimeConstraints);
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
                        this.div.appendChild(IIDXHelper.createSelect(operator1ElementId, operatorChoicesForStrings, "=", "input-mini"));
                        this.div.appendChild(IIDXHelper.createTextInput(operand1ElementId, "input-xlarge"));
                    }else if(valueType == "NODE_URI"){
                        this.div.appendChild(IIDXHelper.createSelect(operator1ElementId, operatorChoicesForUris, "=", "input-mini"));
                        this.div.appendChild(IIDXHelper.createTextInput(operand1ElementId, "input-xlarge"));
                    }else if(valueType == "BOOLEAN"){
                        this.div.appendChild(IIDXHelper.createButtonGroup(operand1ElementId, operandChoicesForBoolean, "buttons-radio"));
                    }else if(isNumericType(valueType)){
                        this.div.appendChild(IIDXHelper.createSelect(operator1ElementId, operatorChoicesForNumerics, ">", "input-mini"));
                        this.div.appendChild(IIDXHelper.createTextInput(operand1ElementId, "input-xlarge"));
                        this.div.appendChild(IIDXHelper.createLabel("  ")); // forces a newline
                        this.div.appendChild(IIDXHelper.createSelect(operator2ElementId, operatorChoicesForNumerics, "<", "input-mini"));
                        this.div.appendChild(IIDXHelper.createTextInput(operand2ElementId, "input-xlarge"));
                    }else{
                        // TODO support all data types, and also handle in validateCallback and okCallback
                        this.div.appendChild(IIDXHelper.createLabel("...not supported yet..."));
                    }
                } 
                    
                // launch the modal
                var m = new ModalIidx();                            
                m.showClearCancelSubmit(
                                this.title,
                                this.div, 
                                this.validateCallback.bind(this),
                                this.clearCallback, 
                                this.okCallback.bind(this)
                                );
				
			},            
            
		};
	
		return ModalRuntimeConstraintDialog;
	}
);