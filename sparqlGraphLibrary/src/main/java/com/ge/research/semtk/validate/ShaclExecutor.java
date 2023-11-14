/**
 ** Copyright 2023 General Electric Company
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
package com.ge.research.semtk.validate;

import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.InputStream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.engine.Target;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.shacl.validation.Severity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.ontologyTools.OntologyName;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

/**
 * Validate RDF data using SHACL
 */
public class ShaclExecutor {
	
	private Graph dataGraph;
	private Shapes shapes;
	private ValidationReport report;
	private String shapesAndReportTurtle; // turtle string with raw shapes and raw report
	private HashMap<Shape, HashMap<Target, Collection<Node>>> shapeTargetFocusHash = new HashMap<Shape, HashMap<Target, Collection<Node>>>(); // hash to avoid redundant processing
	
	private JobTracker tracker = null;
	private String jobId = null;
	private int startPercent = 0;
	private int endPercent = 100;

	public static final String JSON_KEY_ENTRIES = "reportEntries";
	
	private static final String JSON_KEY_SOURCESHAPE = "sourceShape";		// the URI of the source shape (e.g. http://DeliveryBasketExample#Shape_Fruit)
	private static final String JSON_KEY_TARGETTYPE = "targetType";			// the target type.  One of: targetNode, targetClass, targetSubjectsOf, targetObjectsOf
	private static final String JSON_KEY_TARGETOBJECT = "targetObject";		// the target object.  Can be URI or literal (e.g. http://DeliveryBasketExample#FruitBasket, http://DeliveryBasketExample#holds, "pear55")
	private static final String JSON_KEY_PATH = "path";						// the relevant path (e.g. <http://DeliveryBasketExample#holds>, ^<http://DeliveryBasketExample#holds>, <http://DeliveryBasketExample#holds>/<http://DeliveryBasketExample#identifier>, more)
	private static final String JSON_KEY_CONSTRAINT = "constraint"; 		// the constraint (e.g. maxCount[3])
	public static final String JSON_KEY_SEVERITY = "severity";				// Violation, Info, or Warning (e.g. from http://www.w3.org/ns/shacl#Violation)
	private static final String JSON_KEY_FOCUSNODE = "focusNode";			// the offending item, could be URI or literal (e.g. http://DeliveryBasketExample#basket100)
	private static final String JSON_KEY_VALUE = "value";					// sometimes populated (e.g. for failed DataType constraint)
	private static final String JSON_KEY_MESSAGE = "message"; 				// a message (auto-generated unless defined in SHACL shape) e.g. "maxCount[1]: Invalid cardinality: expected max 1: Got count = 2".  No other way to get actual, so best practice is to let it auto-generate 
	private static final String JSON_KEY_MESSAGETRANSFORMED = "messageTransformed"; 	// a message transformed to be more readable than the original 

	/**
	 * Constructor
	 * @param shaclTtl SHACL shapes string
	 * @param conn connection containing model/data
	 */
	public ShaclExecutor(String shaclTtl, SparqlConnection conn) throws Exception{
		this(shaclTtl, conn, null, null, 0, 100);
	}
	
	/**
	 * Constructor
	 * @param shaclTtl SHACL shapes string
	 * @param conn connection containing model/data
	 * @param tracker the job tracker
	 * @param jobId the job id for setting job status
	 * @param percentStart the start percent for setting job status
	 * @param percentEnd the end percent for setting job status
	 */
	public ShaclExecutor(String shaclTtl, SparqlConnection conn, JobTracker tracker, String jobId, int percentStart, int percentEnd) throws Exception{
		
		this.tracker = tracker;
		this.jobId = jobId;
		startPercent = percentStart;
		endPercent = percentEnd;

		if(this.tracker != null) { this.tracker.setJobPercentComplete(this.jobId, startPercent, "Parsing SHACL shapes"); }
		shapes = parseShapes(shaclTtl);
		if(this.tracker != null) { this.tracker.setJobPercentComplete(this.jobId, (int)(startPercent + (endPercent - startPercent)*0.3), "Creating Jena graph"); }
		dataGraph = conn.getJenaGraph();
		if(this.tracker != null) { this.tracker.setJobPercentComplete(this.jobId, (int)(startPercent + (endPercent - startPercent)*0.6), "Running SHACL"); }
		executeShacl();
		if(this.tracker != null) { this.tracker.setJobPercentComplete(this.jobId, endPercent); }
	}
	
	/**
	 * Constructor
	 * @param shaclTtl SHACL shapes string
	 * @param dataTtlInputStream data/model in a TTL input stream
	 */
	public ShaclExecutor(String shaclTtl, InputStream dataTtlInputStream) throws Exception{
		shapes = parseShapes(shaclTtl);
		dataGraph = Utility.getJenaGraphFromTurtle(dataTtlInputStream);
		executeShacl();
	}
	
	// run SHACL shapes on a Jena Graph object, get report object and raw ttl
	private void executeShacl() throws Exception {
		
		try {
			report = ShaclValidator.get().validate(shapes, dataGraph);
		}catch(Exception e) {
			throw new Exception("Error creating SHACL validation report: " + e.getMessage(), e);
		}
		
		// create TTL file with shapes and report
		Model shapesAndReport = ModelFactory.createModelForGraph(shapes.getGraph());
		shapesAndReport.add(ModelFactory.createModelForGraph(report.getGraph()));
		shapesAndReportTurtle = Utility.getTurtleFromJenaGraph(shapesAndReport.getGraph());
	}	
	
	/**
	 * Return true if the data validates successfully
	 */
	public boolean conforms() {
		return report.conforms();
	}
	
	/**
	 * Get the raw shapes and raw report information in Turtle format.
	 * This content is produced by the Jena shapes parser and validator, with no post-processing.
	 */
	public String getResultsRawTurtle() {
		return shapesAndReportTurtle;
	}
	
	/**
	 * Get the SHACL results.
	 * This content includes post-processing to gather key information for each result.
	 */
	public JSONObject getResults() throws Exception {
		return getResults(Severity.Info);
	}
	
	/**
	 * Get the SHACL results (for the given severity level and above)
	 * This content includes post-processing to gather key information for each result.
	 */
	@SuppressWarnings("unchecked")
	public JSONObject getResults(Severity severityLevel) throws Exception {

		JSONArray reportEntriesJsonArr = new JSONArray();	
		for(ReportEntry entry : report.getEntries()) {
	    	
	    	// get the focus node, source shape and its target
	    	Node focusNode = entry.focusNode();
	    	Shape sourceShape = getShape(entry.source().toString());
	    	Target sourceShapeTarget = getMatchingTarget(sourceShape, focusNode);	// a source shape may have multiple targets - determine which one produced our focus node
	    	
	    	// assemble the JSON content
			String sourceShapeUri = sourceShape.getShapeNode().getURI();							// see notes in JSON_KEY_* above
	    	String sourceShapeTargetType = sourceShapeTarget.getTargetType().toString();  			// see notes in JSON_KEY_* above
	    	String sourceShapeTargetObject = sourceShapeTarget.getObject().toString();				// see notes in JSON_KEY_* above
	    	String path = (entry.resultPath() != null) ? entry.resultPath().toString() : null;		// see notes in JSON_KEY_* above.  Path will exist iff the constraint is on a PropertyShape
	    	String constraint = getConstraintString(entry);											// see notes in JSON_KEY_* above
	    	String severity = getLocalName(entry.severity().level().toString());					// see notes in JSON_KEY_* above
	    	String focusNodeStr = focusNode.isURI() ? focusNode.getURI() : focusNode.toString();	// see notes in JSON_KEY_* above
	    	String value = entry.value() != null ? entry.value().toString() : "";					// see notes in JSON_KEY_* above
	    	String message = entry.message();														// see notes in JSON_KEY_* above
	    	String messageTransformed = transformMessage(message);									// see notes in JSON_KEY_* above
					
	    	if(compare(entry.severity(), severityLevel) >= 0) {
	    		JSONObject entryJson = new JSONObject();
	    		entryJson.put(JSON_KEY_SOURCESHAPE, sourceShapeUri);
	    		entryJson.put(JSON_KEY_TARGETTYPE, sourceShapeTargetType);
	    		entryJson.put(JSON_KEY_TARGETOBJECT, sourceShapeTargetObject);
	    		entryJson.put(JSON_KEY_PATH, (path != null) ? path : "");
	    		entryJson.put(JSON_KEY_CONSTRAINT, constraint);
	    		entryJson.put(JSON_KEY_SEVERITY, severity);
	    		entryJson.put(JSON_KEY_FOCUSNODE, focusNodeStr);
	    		entryJson.put(JSON_KEY_VALUE, value);
	    		entryJson.put(JSON_KEY_MESSAGE, message);
	    		entryJson.put(JSON_KEY_MESSAGETRANSFORMED, messageTransformed);
	    		reportEntriesJsonArr.add(entryJson);
	    	}
	    }
		
		JSONObject resultsJson = new JSONObject();
		resultsJson.put(JSON_KEY_ENTRIES, reportEntriesJsonArr);
	    return resultsJson;
	}	

	// a source shape may have multiple targets - determine which one produced our focus node
	private Target getMatchingTarget(Shape sourceShape, Node focusNode) throws Exception {
		
		// if hash doesn't already have this sourceShape, then add it (hashing yields major speedup for big data graphs)
		if(shapeTargetFocusHash.get(sourceShape) == null) {
			HashMap<Target, Collection<Node>> targetFocusHash = new HashMap<Target, Collection<Node>>(); // maps target to its focus nodes
			Iterator<Target> targetsIter = sourceShape.getTargets().iterator();
			while(targetsIter.hasNext()) {
				Target target = targetsIter.next();	
				targetFocusHash.put(target, target.getFocusNodes(dataGraph));
			}
			shapeTargetFocusHash.put(sourceShape, targetFocusHash);
		}
				
		// get target from the hash
		HashMap<Target, Collection<Node>> map = shapeTargetFocusHash.get(sourceShape);
		for(Target target : map.keySet()) {
			if(map.get(target).contains(focusNode)) {
				return target;
			}
		}
		throw new Exception("Unexpected error: didn't find target");
	}
	
	// get a constraint string (e.g. "DatatypeConstraint[xsd:int]") from an entry
	private String getConstraintString(ReportEntry entry) {
		String s = entry.constraint().toString(); // normally something helpful like "DatatypeConstraint[xsd:int]", but sometimes like this "org.apache.jena.shacl.engine.constraint.ReportConstraint@16c8b7bd"
		int index = s.indexOf("org.apache.jena.shacl.engine.constraint.ReportConstraint@");
		if(index == -1) {
			return s;
		}else {
			return "org.apache.jena.shacl.engine.constraint.ReportConstraint@XXXXXXXX";  // substituting XXXXXXXX to enable unit testing - original content likely not needed
		}
	}
	
	// parse shapes
	private Shapes parseShapes(String shaclTtl) throws Exception {
		try {
			return Shapes.parse(Utility.getJenaGraphFromTurtle(shaclTtl));
		}catch(Exception e) {
			throw new Exception("Error parsing SHACL shapes: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Find a shape with the given string from the results
	 * @param shapeStr the shape string, e.g. 904d05c172ca9ceece3fe110063b826f
	 * @return the shape
	 * @throws Exception 
	 */
	private Shape getShape(String shapeStr) throws Exception {
		Iterator<Shape> shapesIter = shapes.iterator();
		while(shapesIter.hasNext()) {
			Shape s = shapesIter.next();
			// check if the top-level shape node has the given string
			if(s.getShapeNode().toString().equals(shapeStr)) {
				return s;
			}			
			// check if the shape's property shapes have the given string
			for(Shape ps : s.getPropertyShapes()) {	
				if(ps.getShapeNode().toString().equals(shapeStr)) {
					return s;
				}
			}
		}
		throw new Exception("Could not find shape " + shapeStr);
	}
	
	/**
	 * Make a SHACL message more human friendly.
	 * This method transforms auto-generated messages corresponding to various constraint types (https://www.w3.org/TR/shacl/#core-components)
	 * This is a best-effort approach because of the many different message formats possible (including custom messages).  If no match found, then log it and return an empty string.
	 * 
	 * @param message 	the message to transform
	 * @return 			a transformed message, or empty if could not transform it
	 */
	private String transformMessage(String message) {
		
		try {

			Pattern pattern = null;  // note: need the () subpattern groupings for exec() to work
			Matcher matcher = null;
			
			// =========== "Value Type Constraint Components"
			
			// sh:class
			// ClassConstraint[<http://DeliveryBasketExample#Peach>]: Expected class :<http://DeliveryBasketExample#Peach> for <http://DeliveryBasketExample#fruit100d>
			pattern = Pattern.compile("ClassConstraint\\[(.+)\\]: Expected class :(.+) for (.+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect " + matcher.group(3) + " to be an instance of " + matcher.group(2);
			}
	
			// sh:datatype
			// DatatypeConstraint[xsd:bool]: Expected xsd:bool : Actual xsd:string : Node "id0"
			pattern = Pattern.compile("DatatypeConstraint(.+): Expected (.+) : Actual (.+) : Node (.+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect datatype " + matcher.group(2) + " (got " + matcher.group(4) + ", type " + matcher.group(3) + ")";
			}
			
			// sh:nodeKind
			// NodeKind[IRI] : Expected IRI for "10"
			pattern = Pattern.compile("NodeKind\\[(.+)\\] : Expected (.+) for (.+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect node kind " + matcher.group(2) + " for " + matcher.group(3);
			}
			
			// ===========  "Cardinality Constraint Components"
			
			// sh:minCount
			// minCount[1]: Invalid cardinality: expected min 1: Got count = 0
			pattern = Pattern.compile("minCount\\[(\\d+)\\]: Invalid cardinality: expected min (\\d+): Got count = (\\d+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect count >= " + matcher.group(2) + " (got " + matcher.group(3) + ")";
			}

			// sh:maxCount
			// maxCount[3]: Invalid cardinality: expected max 3: Got count = 4
			pattern = Pattern.compile("maxCount\\[(\\d+)\\]: Invalid cardinality: expected max (\\d+): Got count = (\\d+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect count <= " + matcher.group(2) + " (got " + matcher.group(3) + ")";
			}
	
			// ===========  "Value Range Constraint Components"
			
			// sh:minInclusive, sh:minExclusive, sh:maxInclusive, sh:maxExclusive
			// Data value "0.5"^^xsd:double is not greater than or equal to 1
			pattern = Pattern.compile("Data value \\\"(.+)\\\"\\^\\^xsd:(.+) is not (.+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect a " + matcher.group(2) + " " + matcher.group(3) + " (got " + matcher.group(1) + ")";
			}
			
			// ===========  "String-based Constraint Components"
	
			// sh:minLength
			// MinLengthConstraint[5]: String too short: id0
			pattern = Pattern.compile("MinLengthConstraint\\[(\\d+)\\]: String too short: (.+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect string length >= " + matcher.group(1) + " (\"" + matcher.group(2) + "\")";
			}			
			// sh:maxLength
			// MaxLengthConstraint[5]: String too long: id0
			pattern = Pattern.compile("MaxLengthConstraint\\[(\\d+)\\]: String too long: (.+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect string length <= " + matcher.group(1) + " (\"" + matcher.group(2) + "\")";
			}
	
			// sh:pattern
			// Pattern[(.+)peach(.+)]: Does not match: 'http://DeliveryBasketExample#basket100'
			pattern = Pattern.compile("Pattern\\[(.+)\\]: Does not match: '(.+)'");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Does not match pattern " + matcher.group(1) + ": \"" + matcher.group(2) + "\"";
			}
			
			// TODO add sh:languageIn
			// TODO add sh:uniqueLang
			
			// ===========  "Property Pair Constraint Components"
			
			// sh:equals
			// Equals[<http://DeliveryBasketExample#careOfName>]: not equal: value node "Rebecca Recipient" is not in ["Carey careof"]
			pattern = Pattern.compile("Equals\\[(.+)\\]: not equal(.+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect to equal " + getLocalName(matcher.group(1));
			}
			
			// sh:disjoint
			// Disjoint[<http://DeliveryBasketExample#careOfName>]: not disjoint: "Unknown Addressee" is in ["Unknown Addressee"]
			pattern = Pattern.compile("Disjoint\\[(.+)\\]: not disjoint(.+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect to be disjoint with " + getLocalName(matcher.group(1));
			}
			
			// sh:lessThan, sh:lessThanOrEquals
			// LessThan[<http://DeliveryBasketExample#expirationDate>]: value node "2023-01-01"^^xsd:date is not less than "1999-01-01"^^xsd:date
			// LessThanOrEquals[<http://DeliveryBasketExample#expirationDate>]: value node "2023-01-01"^^xsd:date is not less than or equal to "1999-01-01"^^xsd:date
			pattern = Pattern.compile("LessThan(.*)\\[(.+)\\]: value node \"(.+)\"\\^\\^xsd:(.+) is not (.+) \"(.+)\"\\^\\^xsd:(.+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect to be " + matcher.group(5) + " " + getLocalName(matcher.group(2));
			}

			// ===========  "Logical Constraint Components" (these take shapes as arguments)
			
			// sh:not
			// Not[NodeShape[30e4dc59675ddbcdb75c01a2287258dc]] at focusNode http://DeliveryBasketExample#addressTwoZips
			// Not[PropertyShape[e6e0b7919760c0c84e28e6a95804080a -> <http://DeliveryBasketExample#zipCodePlusFour>]] at focusNode <http://DeliveryBasketExample#addressTwoZips>
			pattern = Pattern.compile("Not\\[(.+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect to not conform to the provided shape";
			}	
			
			// sh:and
			// And at focusNode <http://DeliveryBasketExample#basket1>
			pattern = Pattern.compile("And at focusNode (.+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect to conform to all provided shapes";
			}
			
			// sh:or
			// Or at focusNode <http://DeliveryBasketExample#basket1>
			pattern = Pattern.compile("Or at focusNode (.+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect to conform to at least one of the provided shapes";
			}
			
			// sh:xone
			// Xone has 2 conforming shapes at focusNode <http://DeliveryBasketExample#addressTwoZips>
			pattern = Pattern.compile("Xone has (.+) conforming shapes at focusNode (.+)");	// confirmed works on 0 and 2
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect to conform to exactly one of the provided shapes (conforms to " + matcher.group(1) + ")";
			}		
			
			// ===========  "Shape-based Components" (these take shapes as arguments)
			
			// sh:node
			// Node[<http://DeliveryBasketExample#AtLeastOneRecipientShape>] at focusNode <http://DeliveryBasketExample#addressWithoutRecipient>
			pattern = Pattern.compile("Node\\[(.+)\\] at focusNode (.+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect to conform to shape " + getLocalName(matcher.group(1));
			}
			
			// sh:property
			// believe this would be used only in conjunction with another constraint (e.g. minCount to enforce path exists), whose messages are handled elsewhere
	
			// sh:qualifiedValueShape, sh:qualifiedMinCount
			// QualifiedValueShape[2,_,false]: Min = 2 but got 1 validations
			pattern = Pattern.compile("QualifiedValueShape\\[(.+)\\]: Min = (.+) but got (.+) validations");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect at least " + matcher.group(2) + " items that conform to a shape (got " + matcher.group(3) + ")";
			}
			// sh:qualifiedValueShape, sh:qualifiedMaxCount
			// QualifiedValueShape[_,2,false]: Max = 2 but got 3 validations
			pattern = Pattern.compile("QualifiedValueShape\\[(.+)\\]: Max = (.+) but got (.+) validations");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect at most " + matcher.group(2) + " items that conform to a shape (got " + matcher.group(3) + ")";
			}
			
			// ===========  "Other Constraint Components"
			
			// sh:closed
			// Closed[http://DeliveryBasketExample#includes] Property = rdf:type : Object = <http://DeliveryBasketExample#FruitBasket>
			// Closed[http://DeliveryBasketExample#includes] Property = <http://DeliveryBasketExample#capacity> : Object = "10"^^xsd:double
			pattern = Pattern.compile("Closed(.+) Property = (.+) : Object = (.+)");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect to only have properties " + matcher.group(1) + " (has " + matcher.group(2) + ")";
			}
			
			// TODO add sh:hasValue

			// sh:in
			// InConstraint["53217"^^http://www.w3.org/2001/XMLSchema#int, "53211"^^http://www.w3.org/2001/XMLSchema#int ] : RDF term "10027"^^xsd:int not in expected values
			pattern = Pattern.compile("InConstraint\\[(.+)\\] : RDF term (.+) not in expected values");
			matcher = pattern.matcher(message);
			if (matcher.matches()) {
				return "Expect " + matcher.group(2) + " to be one of: " + matcher.group(1);
			}

		}catch(Exception e) {
			LocalLogger.logToStdErr("Error transforming SHACL message '" + message + "': " + e);
			LocalLogger.printStackTrace(e);
		}
		
		LocalLogger.logToStdOut("No SHACL message transform applies: " + message);
		return "";
	}
	
	// Get a URI's local name
	// e.g. <http://DeliveryBasketExample#FruitBasket> => FruitBasket
	// Removes angled brackets if present (omitting angled brackets from regex because may not be present, e.g. for blank nodes)	
	private String getLocalName(String uri) {
		if(uri.startsWith("<") && uri.endsWith(">")) {
			uri = uri.substring(0, uri.length() - 1);
		}
		return (new OntologyName(uri)).getLocalName(); // if # exists, returns substring following it 
	}
	
	/**
	 * Compare SHACL severity levels
	 */
	public static int compare(Severity severity1, Severity severity2) throws Exception {
		if(severity1.equals(severity2)) { return 0; }
		if(severity1.equals(Severity.Violation)) { return 1; }
		if(severity1.equals(Severity.Info)) { return -1; }
		if(severity1.equals(Severity.Warning)) {
			if(severity2.equals(Severity.Violation)) { return -1; }
			else { return 1; }
		}
		throw new Exception("Unexpected error comparing severities");
	}
	
}
