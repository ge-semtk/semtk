/**
 ** Copyright 2016 General Electric Company
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

package com.ge.research.semtk.belmont.runtimeConstraints;

/**
 * Runtimne constraint operations
 * @author 200001934
 *
 */
public enum SupportedOperations {

	MATCHES(1,1000), 		        // value matches one of the operands (accepts collections)
	REGEX(1,1), 			        // value matches the string indicated by the given operand
	GREATERTHAN(1,1), 			    // value is greater than the operand
	GREATERTHANOREQUALS(1,1), 	    // value is greater than or equal to the operand
	LESSTHAN(1,1), 				    // value is less than the operand
	LESSTHANOREQUALS(1,1), 		    // value is less than or equal to the operand
	VALUEBETWEEN(2,2),			    // value is between the given operands, including both endpoints
	VALUEBETWEENUNINCLUSIVE(2,2);    // value is between the given operands, not including endpoints
	
	private int minOperands = -1;
	private int maxOperands = -1;
	
	private SupportedOperations(int minOpnds, int maxOpnds) {
		this.minOperands = minOpnds;
		this.maxOperands = maxOpnds;
	}

	public int getMinOperands() {
		return minOperands;
	}

	public int getMaxOperands() {
		return maxOperands;
	}
}
