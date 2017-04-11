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
