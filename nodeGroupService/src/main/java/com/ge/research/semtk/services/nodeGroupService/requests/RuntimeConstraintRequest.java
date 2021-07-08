/**
 ** Copyright 2018 General Electric Company
 **
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

package com.ge.research.semtk.services.nodeGroupService.requests;

import com.ge.research.semtk.belmont.runtimeConstraints.SupportedOperations;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;

public class RuntimeConstraintRequest {

    @ApiModelProperty(
            position = 1,
            value = "sparqlID",
            required = true,
            example = "123456")
    private String sparqlID;

    @ApiModelProperty(
            position = 2,
            value = "operation",
            notes = "\tMATCHES  // value matches one of the operands (accepts collections)\n" +
                    "\tREGEX  // value matches the string indicated by the given operand\n" +
                    "\tGREATERTHAN  // value is greater than the operand\n" +
                    "\tGREATERTHANOREQUALS // value is greater than or equal to the operand\n" +
                    "\tLESSTHAN // value is less than the operand\n" +
                    "\tLESSTHANOREQUALS // value is less than or equal to the operand\n" +
                    "\tVALUEBETWEEN // value is between the given operands, including both endpoints\n" +
                    "\tVALUEBETWEENUNINCLUSIVE  // value is between the given operands, not including endpoints\n",
            allowableValues = "NOTMATCHES, MATCHES, REGEX, GREATERTHAN, GREATERTHANOREQUALS, LESSTHAN, LESSTHANOREQUALS, VALUEBETWEEN, VALUEBETWEENUNINCLUSIVE",
            required = true,
            example = "MATCHES")
    private SupportedOperations operation;

    @ApiModelProperty(
            position = 3,
            value = "operandList",
            required = true,
            dataType = "[Ljava.lang.String;")
    private ArrayList<String> operandList;

    public String getSparqlID() {
        return sparqlID;
    }

    public void setSparqlID(String sparqlID) {
        this.sparqlID = sparqlID;
    }

    public SupportedOperations getOperation() {
        return operation;
    }

    public void setOperation(SupportedOperations operation) {
        this.operation = operation;
    }

    public ArrayList<String> getOperandList() {
        return operandList;
    }

    public void setOperandList(ArrayList<String> operandList) {
        this.operandList = operandList;
    }


}
