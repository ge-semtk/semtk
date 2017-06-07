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

public enum SupportedOperations {

	MATCHES, 				// value matches one of the operands (accepts collections)
	REGEX, 					// value matches the string indicated by the given operand
	GREATERTHAN, 			// value is greater than the operand
	GREATERTHANOREQUALS, 	// value is greater than or equal to the operand
	LESSTHAN, 				// value is less than the operand
	LESSTHANOREQUALS, 		// value is less than or equal to the operand
	VALUEBETWEEN,			// value is between the given operands, including both endpoints
	VALUEBETWEENUNINCLUSIVE // value is between the given operands, not including endpoints
}
