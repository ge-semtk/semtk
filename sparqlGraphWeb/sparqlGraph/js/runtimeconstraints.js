/**
 ** Copyright 2016 General Electric Company
 **
 ** Authors:  Paul Cuddihy, Justin McHugh, Jenny Williams
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

// Note: this is not a direct port of RuntimeConstraintItems.java, as it differing functionality

define([	// properly require.config'ed
        
			// shimmed
			
		],


	function() {
	
        //  Supported operators (from com.ge.research.semtk.belmont.runtimeConstraints.SupportedOperations)
        //  MATCHES, 				// value matches one of the operands (accepts collections)
        //	REGEX, 					// value matches the string indicated by the given operand
        //	GREATERTHAN, 			// value is greater than the operand
        //	GREATERTHANOREQUALS, 	// value is greater than or equal to the operand
        //	LESSTHAN, 				// value is less than the operand
        //	LESSTHANOREQUALS, 		// value is less than or equal to the operand
        //	VALUEBETWEEN,			// value is between the given operands, including both endpoints
        //	VALUEBETWEENUNINCLUSIVE // value is between the given operands, not including endpoints
        var SUPPORTED_OPERATORS = ["MATCHES", "REGEX", "GREATERTHAN", "GREATHERTHANOREQUALS", "LESSTHAN", "LESSTHANOREQUALS", "VALUEBETWEEN", "VALUEBETWEENUNINCLUSIVE"];
    
		/**
		 * Runtime constraints object
		 */
		var RuntimeConstraints = function () {
            this.constraintsJson = [];  // a list of jsons, each for a single constraint
		};
			
		RuntimeConstraints.prototype = {

			// These methods' behaviors are not guaranteed to be stable in future releases.
			           
            // add a constraint
            add : function(sparqlId, operator, operands){
                
                // confirm that the given operator is legal
                if(!SUPPORTED_OPERATORS.includes(operator)){
                    // if get this alert, then a fix is needed in the code
                    alert("Cannot add runtime constraint for " + sparqlId + ": illegal  operator (" + operator + ")");
                    return;
                }
                
                // build the json for this constraint
                var s = '{';
                s += '"SparqlID": "' + sparqlId + '",';
                s += '"Operator": "' + operator + '",';
                s += '"Operands":["';
                for(i = 0; i < operands.length; i++){
                    s += operands[i];
                    if(i < operands.length - 1){
                        s+= ", ";
                    }
                }
                s += '"]'; 
                s += '}';
                this.constraintsJson.push(s);
            },
			
            // gather all constraints into a single json
            toJson : function () {
                var s = "RuntimeConstraints: ["; 
                for(i = 0; i < this.constraintsJson.length; i++){
                    s += this.constraintsJson[i];
                    s += ",";
                }
                s += "]";
                return s;
			},
		};
	
		return RuntimeConstraints;            // return the constructor
	}
);