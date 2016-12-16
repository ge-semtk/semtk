package com.ge.research.semtk.belmont.runtimeConstraints;

import java.util.ArrayList;

import com.ge.research.semtk.belmont.XSDSupportUtil;
import com.ge.research.semtk.belmont.XSDSupportedTypes;

public class ConstraintUtil {

	
	// static methods to return strings that describe the constraints to be applied.
	// these would most likely be used on a client side where the nodegroup is not manipulated and only the values are being 
	// handled.
	
	// applicable to:
	// values found in the XSDSupportedTypes enum.
	
	public static void checkValidType(String xsdValueName) throws Exception{
		if(!XSDSupportUtil.supportedType(xsdValueName)){
			throw new Exception("unrecognized type: " + xsdValueName + ". does not match XSD types defined in " +  XSDSupportedTypes.class.getName()); 
		}
	}
		
	public static String getMatchesConstraint(String sparqlId, String val, String XSDValueTypeName) throws Exception{
		// first check the validity of the passed type name.
		checkValidType(XSDValueTypeName);
		
		// assuming that we passed the test, let's create the constraints
		// the values looks a lot like this: VALUES ?trNum { '1278'^^<http://www.w3.org/2001/XMLSchema#int> } 

		String retval = "VALUES " + sparqlId + " {'" + val + "'" + XSDSupportUtil.getXsdSparqlTrailer(XSDValueTypeName) + "}";		
		return retval;
	}

	public static String getMatchesOneOfConstraint(String sparqlId, ArrayList<String> val, String XSDValueTypeName) throws Exception{
		// first check the validity of the passed type name.
		checkValidType(XSDValueTypeName);
	
		// VALUES ?trNum { '1278'^^<http://www.w3.org/2001/XMLSchema#int> '1279'^^<http://www.w3.org/2001/XMLSchema#int> } 
	
		String retval = "VALUES " + sparqlId + " { ";
		// go through each passed value and add them.
		for(String cv : val){
			retval += "'" + cv + "'" + XSDSupportUtil.getXsdSparqlTrailer(XSDValueTypeName) + " "; 
		}
		
		retval += " }";
		
		return retval;
	}
	
	public static String getRegexConstraint(String sparqlId, String val, String XSDValueTypeName) throws Exception{
		// first check the validity of the passed type name.
		checkValidType(XSDValueTypeName);
		
		String retval = "";
		// this should look like this: FILTER regex(?testTitle, "example")
		
		// for right now, it seems safe to assume that regexes are only valid on strings.
		
		if(XSDSupportUtil.regexIsAvailable(XSDValueTypeName)){
			retval = "FILTER regex(" + sparqlId + " ,\"" + val + "\")";
		}
		else{
			// regex is not considered supported here.
			throw new Exception("requested type (" + XSDValueTypeName + ") for the sparqlId (" + sparqlId + ") does not support regex constraints");
		}
		return retval;
	}
	
	public static String getGreaterThanConstraint(String sparqlId, String val, String XSDValueTypeName, boolean greaterOrEqual) throws Exception{
		checkValidType(XSDValueTypeName);
		
		String retval = "";
		
		if(XSDSupportUtil.numericOperationAvailable(XSDValueTypeName)){
			// looks like: FILTER (?trNum = 1)
			if(!greaterOrEqual){
				retval = "FILTER (" + sparqlId + " > " + val + ")";
			}
			else{
				retval = "FILTER (" + sparqlId + " >= " + val + ")";
			}
		}
		else{
			// regex is not considered supported here.
			throw new Exception("requested type (" + XSDValueTypeName + ") for the sparqlId (" + sparqlId + ") does not support numeric operation constraints");
		}
		return retval;
	}
	
	public static String getLessThanConstraint(String sparqlId, String val, String XSDValueTypeName, boolean lessOrEqual) throws Exception{
		checkValidType(XSDValueTypeName);
		
		String retval = "";
		
		if(XSDSupportUtil.numericOperationAvailable(XSDValueTypeName)){
			// looks like: FILTER (?trNum = 1)
			if(!lessOrEqual){
				retval = "FILTER (" + sparqlId + " < " + val + ")";
			}
			else{
				retval = "FILTER (" + sparqlId + " <= " + val + ")";
			}
		}
		else{
			// regex is not considered supported here.
			throw new Exception("requested type (" + XSDValueTypeName + ") for the sparqlId (" + sparqlId + ") does not support numeric operation constraints");
		}
		return retval;
	}
	
	public static String getNumberBetweenConstraint(String sparqlId, String valLow, String valHigh, String XSDValueTypeName, boolean greaterOrEqual, boolean lessThanOrEqual) throws Exception{
		checkValidType(XSDValueTypeName);
		
		String retval = "";
		
		if(XSDSupportUtil.numericOperationAvailable(XSDValueTypeName)){
			// looks like: FILTER (?trNum > 1 && ?trNum < 55)
			
			StringBuilder ret = new StringBuilder("FILTER (");
			
			if(greaterOrEqual){
				ret.append(" " + sparqlId + " >= " + valLow + " ");
			}
			else{
				ret.append(" " + sparqlId + " > " + valLow + " ");
			}
			
			// add a conjunction
			ret.append(" && ");
			
			if(lessThanOrEqual){
				ret.append(" " + sparqlId + " <= " + valHigh + " ");
			}
			else{
				ret.append(" " + sparqlId + " < " + valHigh + " ");
			}
			ret.append(")");
			retval = ret.toString();
		}
		else{
			// regex is not considered supported here.
			throw new Exception("requested type (" + XSDValueTypeName + ") for the sparqlId (" + sparqlId + ") does not support numeric operation constraints");
		}
		return retval;
	}

	public static String getTimePeriodBeforeConstraint(String sparqlId, String val, String XSDValueTypeName, boolean onOrBefore) throws Exception{
		checkValidType(XSDValueTypeName);
		
		String retval = "";
		
		if(XSDSupportUtil.dateOperationAvailable(XSDValueTypeName)){
			//   FILTER (?date > "2014-05-23T10:20:13+05:30"^^xsd:dateTime)
			
			StringBuilder ret = new StringBuilder("FILTER (");
			
			if(onOrBefore){
				ret.append(sparqlId + " <= " + val + XSDSupportUtil.getXsdSparqlTrailer(XSDValueTypeName));
			}
			else{
				ret.append(sparqlId + " < " + val + XSDSupportUtil.getXsdSparqlTrailer(XSDValueTypeName));
			}
			ret.append(")");
			retval = ret.toString();
		}
		else{
			// regex is not considered supported here.
			throw new Exception("requested type (" + XSDValueTypeName + ") for the sparqlId (" + sparqlId + ") does not support date operation constraints");
		}
		return retval;
		
	}
	
	public static String getTimePeriodAfterConstraint(String sparqlId, String val, String XSDValueTypeName, boolean onOrAfter) throws Exception{
		checkValidType(XSDValueTypeName);
		
		String retval = "";
		
		if(XSDSupportUtil.dateOperationAvailable(XSDValueTypeName)){
			//   FILTER (?date > "2014-05-23T10:20:13+05:30"^^xsd:dateTime)
			
			StringBuilder ret = new StringBuilder("FILTER (");
			
			if(onOrAfter){
				ret.append(sparqlId + " >= " + val + XSDSupportUtil.getXsdSparqlTrailer(XSDValueTypeName));
			}
			else{
				ret.append(sparqlId + " > " + val + XSDSupportUtil.getXsdSparqlTrailer(XSDValueTypeName));
			}
			ret.append(")");
			retval = ret.toString();
		}
		else{
			// regex is not considered supported here.
			throw new Exception("requested type (" + XSDValueTypeName + ") for the sparqlId (" + sparqlId + ") does not support date operation constraints");
		}
		return retval;
		
	}
	
	public static String getTimePeriodBetweenConstraint(String sparqlId, String valLow, String valHigh, String XSDValueTypeName, boolean onOrAfter, boolean onOrBefore) throws Exception {
		checkValidType(XSDValueTypeName);
		
		String retval = "";
		
		if(XSDSupportUtil.dateOperationAvailable(XSDValueTypeName)){
			//   FILTER (?date > "2014-05-23T10:20:13+05:30"^^xsd:dateTime)
			StringBuilder ret = new StringBuilder("FILTER (");
			
			if(onOrAfter){
				ret.append(" " + sparqlId + " >= " + valLow + XSDSupportUtil.getXsdSparqlTrailer(XSDValueTypeName) + " ");
			}
			else{
				ret.append(" " + sparqlId + " > " + valLow + XSDSupportUtil.getXsdSparqlTrailer(XSDValueTypeName)  + " ");
			}
			
			// add a conjunction
			ret.append(" && ");
			
			if(onOrBefore){
				ret.append(" " + sparqlId + " <= " + valHigh + XSDSupportUtil.getXsdSparqlTrailer(XSDValueTypeName)  + " ");
			}
			else{
				ret.append(" " + sparqlId + " < " + valHigh + XSDSupportUtil.getXsdSparqlTrailer(XSDValueTypeName)  + " ");
			}
			ret.append(")");
			retval = ret.toString();

		}
		else{
			// regex is not considered supported here.
			throw new Exception("requested type (" + XSDValueTypeName + ") for the sparqlId (" + sparqlId + ") does not support date operation constraints");
		}
		return retval;
		
	}
}
