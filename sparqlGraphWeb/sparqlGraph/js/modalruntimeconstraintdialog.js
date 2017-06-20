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
        var operatorsForStrings = ["=", "=",
                                   "REGEX", "REGEX"];
        var operatorsForNumerics = ["=", "=",
                                    ">", ">",
                                    ">=", ">=",
                                    "<", "<",
                                    "<=", "<="];
    
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
            validateCallback : function() {
                      
                // validate each constraint item
                for(i = 0; i < this.sparqlIds.length; i++){
                    
                    var sparqlId = this.sparqlIds[i];
                    // TODO need/use itemType?
                    var valueType = this.valueTypes[i];
                    var operator1Element = document.getElementById("operator1" + sparqlId);
                    var operand1Element = document.getElementById("operand1" + sparqlId);
                    var operator2Element = document.getElementById("operator2" + sparqlId);
                    var operand2Element = document.getElementById("operand2" + sparqlId);    
                    
                    if(valueType == "STRING"){                        
                        // no checks needed yet
                        
                    }else if(valueType == "INT"){ // TODO add other numeric types
                        if(isNaN(operand1Element.value.trim())){  // TODO do check for specific data types (e.g. int, float)
                            // user entered a non-numeric value
                            return "Error: invalid entry for " + sparqlId + ": " + operand1Element.value; 
                        }
                        
                        // if a second operand was entererd
                        if(operand2Element.value){
                            if(isNaN(operand2Element.value)){  // TODO do check for specific data types (e.g. int, float)
                                // user entered non-numeric value
                                return "Error: invalid entry for " + sparqlId + ": " + operand2Element.value; 
                            }
                            if(!operand1Element.value){
                                // user entered the second operand, but not the first
                                return "Error: invalid entry for " + sparqlId + ": second operand entered without first operand";
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
                    // TODO need/use itemType?
                    var valueType = this.valueTypes[j];
                    var operator1Element = document.getElementById("operator1" + sparqlId);
                    var operand1Element = document.getElementById("operand1" + sparqlId);
                    var operator2Element = document.getElementById("operator2" + sparqlId);
                    var operand2Element = document.getElementById("operand2" + sparqlId);
                    
                    if(!operator1Element){
                        continue;   // data type is not supported yet, so user could not have entered it - skip
                    }
                    
                    // collect user input and create runtime constraint object (behavior varies per data type)
                    // TODO: separate each datatype code block into its own function
                    if(valueType == "STRING"){
                        operator1 = operator1Element.value;
                        operand1 = operand1Element.value;    // TODO support multiple operands for MATCHES
                        if(!operand1.trim()){  
                            // user did not enter an operand - skip
                        }else if(operator1 == "="){
                            runtimeConstraints.add(sparqlId, "MATCHES", [operand1]);
                        }else if(operator1 == "REGEX"){
                            runtimeConstraints.add(sparqlId, "REGEX", [operand1]);
                        }else{
                            // if get this alert, then a fix is needed in the code
                            alert("Skipping unsupported operator for " + sparqlId + ": " + operator1.value);
                            // TODO cancel instead of skipping?
                        }
                    }else if(valueType == "INT"){
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
                        // TODO SUPPORT ALL TYPES
                        alert("Type " + valueType + " not supported...add it");   
                    }

                }
                
                // call the callback with a RuntimeConstraints object                
                this.callback(runtimeConstraints);
			},
            
            // TODO I don't think this is being used.  Remove it?
			cancelCallback : function() {
                alert("cancelCallback");
            },
            
            
            /**
              * Got runtime constrainable items for the node group.  Build and launch a dialog for user to populate them.
              */
            launchRuntimeConstraintCallback : function (multiFlag, resultSet) { 
				if (! resultSet.isSuccess()) {
					ModalIidx.alert("Service failed", resultSet.getGeneralResultHtml());
				} else {

                    this.div = document.createElement("div");
                    
                    this.sparqlIds = resultSet.getColumnStringsByName("valueId");
                    this.itemTypes = resultSet.getColumnStringsByName("itemType");   // TODO USE THIS
                    this.valueTypes = resultSet.getColumnStringsByName("valueType");
//					this.sparqlIds = ["flavor","circumference", "frosting"]; // TODO REMOVE - FOR TESTING ONLY
//                    this.valueTypes = ["STRING","INT","BOOLEAN"];          // TODO REMOVE - FOR TESTING ONLY                      

                    // create UI components for all runtime-constrained items 
                    // TODO improve formatting
                    for(i = 0; i < this.sparqlIds.length; i++){
                        
                        sparqlId = this.sparqlIds[i];                   // e.g. ?circumference
                        valueType = this.valueTypes[i];                 // e.g. STRING, INT, etc
						operator1ElementId = "operator1" + sparqlId;	// e.g. "operator1?circumference"
						operand1ElementId = "operand1" + sparqlId;		// e.g. "operand1?circumference"
						operator2ElementId = "operator2" + sparqlId;	// e.g. "operator2?circumference"
						operand2ElementId = "operand2" + sparqlId;		// e.g. "operand2?circumference"
                        
                        this.div.appendChild(IIDXHelper.createLabel(sparqlId + " (" + valueType + "):"));
                        // create UI components for operator/operand (varies per data type)
                        if(valueType == "STRING"){
                            this.div.appendChild(IIDXHelper.createSelect(operator1ElementId, operatorsForStrings));
                            this.div.appendChild(IIDXHelper.createTextInput(operand1ElementId));
                        }else if(valueType == "INT"){
                            this.div.appendChild(IIDXHelper.createSelect(operator1ElementId, operatorsForNumerics, ">"));
                            this.div.appendChild(IIDXHelper.createTextInput(operand1ElementId));
                            this.div.appendChild(IIDXHelper.createSelect(operator2ElementId, operatorsForNumerics, "<"));
                            this.div.appendChild(IIDXHelper.createTextInput(operand2ElementId));
                        }else{
                            // TODO support all data types, and also handle in validateCallback and okCallback
                            this.div.appendChild(IIDXHelper.createLabel("...type not supported yet"));
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
				}
			},            
                        
            /**
              *  Call nodegroup store to get runtime constraints for the given nodegroup id
              *  Then launch dialog with callback linked to "OK"
              */
            launchDialogById : function (nodegroupId, callback, multiFlag) {
                this.title = "Enter runtime constraints (**work in progress**)";
                this.callback = callback;

                var mq = new MsiClientNodeGroupStore(g.service.nodeGroupStore.url);
    		    mq.getNodeGroupRuntimeConstraints(nodegroupId, this.launchRuntimeConstraintCallback.bind(this, multiFlag));
            },
            
            // TODO add launchDialogByNodegroup
            
		};
	
		return ModalRuntimeConstraintDialog;
	}
);