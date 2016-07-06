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


package com.ge.research.semtk.servlet;

import java.io.*;
import java.util.ArrayList;

import javax.servlet.*;
import javax.servlet.http.*;

import org.json.simple.*;

import com.ge.research.semtk.resultSet.GeneralResultSet;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;

/* 
 * Executes SPARQL statements requiring authentication (insert & more).
 * Allows freeform inserts, but limits delete/clear capability.
 * 
 * To run an insert statement, use parameter "sparqlQuery" 
 * To delete a URI, use parameters uriToDelete, uriTypeToDelete, deletePosition, deletePredicate (optional)
 */

public class SparqlAuthRulesServlet extends HttpServlet {
	String blockedWords[] = { "CLEAR GRAPH", "DELETE" };
	String requiredWords[] = { "INSERT" };
	String uName = "dba";
	String passw = "dba";
	String[] rulesQuery = {
			"select distinct ?query where { ?x a <http://research.ge.com/kdl/rules#UpdateRule> . ?x <http://research.ge.com/kdl/rules#input_property>  ?p filter regex(\"",
			"\", str(?p)) . ?x <http://research.ge.com/kdl/rules#rule_text> ?query . }" };

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException,
			IllegalArgumentException {

		PrintWriter out = response.getWriter();
		String errMsg = "";

		JSONObject jsonResult = new JSONObject();
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json");

		// get parameters
		String sparqlServerUrl = request.getParameter("sparqlServerURL"); // auth
																			// version,
																			// e.g.
																			// http://vesuvius37.crd.ge.com:2420/sparql-auth
		String sparqlServerType = request.getParameter("sparqlServerType");
		String sparqlDataset = request.getParameter("sparqlDataset");
		String sparqlQuery = request.getParameter("sparqlQuery");
		String uriToDelete = request.getParameter("uriToDelete"); // e.g.
																	
		String uriTypeToDelete = request.getParameter("uriTypeToDelete"); // e.g
																			// 
		String deletePosition = request.getParameter("deletePosition"); // delete
																		// when
																		// URI
																		// is
																		// subject,
																		// object,
																		// or
																		// both
		String callback = request.getParameter("callback");
		String deletePredicate = request.getParameter("deletePredicate"); // delete
																			// given
																			// this
																			// predicate
																			// (used
																			// in
																			// combination
																			// with
																			// a
																			// subject/object URI)
		String uriTarget = request.getParameter("derivedProperty");			// the property we want to see the rules run for. we are not going to worry about the input properties
		
		try {

			// check required parameters
			SparqlToXUtils.genExceptionIfNull(sparqlServerUrl,
					"Missing 'sparqlServerUrl' argument", null);
			SparqlToXUtils.genExceptionIfNull(sparqlServerType,
					"Missing 'sparqlServerType' argument", null);
			SparqlToXUtils.genExceptionIfNull(sparqlDataset,
					"Missing 'sparqlDataset' argument", null);

			// validate the query if provided
			if (sparqlQuery != null) {

				if (uriToDelete != null || uriTypeToDelete != null) {
					throw new IllegalArgumentException(
							"Accepts parameters sparqlQuery OR uriToDelete/uriTypeToDelete, but not both");
				}
				SparqlToXUtils.validateSparqlInsertQuery(sparqlQuery,
						blockedWords);

				// else generate a "safe" delete query
			} else if (uriToDelete != null && uriTypeToDelete != null) {

				System.out.println("Delete URI " + uriToDelete + " of type "
						+ uriTypeToDelete);

				// check that the URI is of the expected type (to avoid
				// unintentional deletes)
				if (!SparqlToXUtils.checkURIType(uriToDelete, uriTypeToDelete,
						sparqlServerUrl, sparqlDataset, sparqlServerType.toLowerCase(),
						this.uName, this.passw)) {
					String msg = "No URI " + uriToDelete + " exists of type "
							+ uriTypeToDelete;
					System.out.println(msg);
					throw new Exception(msg);
				}

				if (deletePosition == null
						|| (!deletePosition.equals("subject")
								&& !deletePosition.equals("object") && !deletePosition
									.equals("subjectobject"))) {
					throw new IllegalArgumentException(
							"Need valid value for parameter deletePostion.  Valid choices are \"subject\",\"object\", or \"subjectobject\"");
				}

				if (deletePosition.equals("subject")) {
					if (deletePredicate != null) {
						System.out
								.println("Delete "
										+ uriToDelete
										+ " where it is a triple subject only, with predicate "
										+ deletePredicate);
						sparqlQuery = SparqlToXUtils
								.generateDeleteURISubjectQuery(uriToDelete,
										deletePredicate);
					} else {
						System.out.println("Delete " + uriToDelete
								+ " where it is a triple subject only");
						sparqlQuery = SparqlToXUtils
								.generateDeleteURISubjectQuery(uriToDelete);
					}
				} else if (deletePosition.equals("object")) {
					// TODO add predicate feature as above
					System.out.println("Delete " + uriToDelete
							+ " where it is a triple object only");
					sparqlQuery = SparqlToXUtils
							.generateDeleteURIObjectQuery(uriToDelete);
				} else if (deletePosition.equals("subjectobject")) {
					// TODO add predicate feature as above
					System.out.println("Delete " + uriToDelete
							+ " where it is a triple subject or object");
					sparqlQuery = SparqlToXUtils
							.generateDeleteURIQuery(uriToDelete);
				} else {
					throw new Exception("Invalid deletePosition");
				}

			} else if (uriTarget != null) {
				// prepare the query for getting all of the rules themselves
				sparqlQuery = "select distinct ?derived_property ?rule_text where { ?UpdateRule <http://research.ge.com/kdl/rules#derived_property> ?derived_property . " +  
				" VALUES ?derived_property { '" + uriTarget + "'^^<http://www.w3.org/2001/XMLSchema#string> }  . " 
				+ "?UpdateRule <http://research.ge.com/kdl/rules#rule_text> ?rule_text . ?UpdateRule a <http://research.ge.com/kdl/rules#UpdateRule>. }"; 
			
				System.out.println("### BULK RULES QUERY WAS: " + sparqlQuery);
				
			} else {
				throw new IllegalArgumentException(
						"Must provide parameters sparqlQuery OR uriToDelete/uriTypeToDelete");
			}

			// execute the intended semantic query
			// The SparqlEndpointInterface either throws an exception
			// or holds on to the results and gives us convenience functions
			SparqlEndpointInterface endpoint = null;
			System.out.println("SPARQL RULES QUERY :: " + sparqlQuery);
			System.out.println("SPARQL RULES QUERY :: ABOUT TO RUN");
			endpoint = SparqlEndpointInterface.executeQuery(sparqlServerUrl,
					sparqlDataset, sparqlServerType, sparqlQuery,
					this.uName, this.passw, SparqlResultTypes.TABLE);
			
			if (uriTarget == null) {
				// do we even have a reason to run rules?
				boolean runRules = false;
				for (String k : this.requiredWords) {
					if (sparqlQuery.contains(k.toUpperCase())) {
						runRules = true;
					}
					if (sparqlQuery.contains(k.toLowerCase())) {
						runRules = true;
					}
				}

				if (runRules) {
					// get the sparql rules.
					SparqlEndpointInterface ruleEndpoint = null;
					String sparqlQueryClean = sparqlQuery.replace("\"",
							"&QUOTE "); // replace("\"",
										// "'");
					sparqlQueryClean = sparqlQueryClean.replace("\n", " ");
					sparqlQueryClean = sparqlQueryClean.replace("\r", " ");

					String rulesQry = rulesQuery[0] + sparqlQueryClean
							+ rulesQuery[1]; // build our query to get the
												// rules.

					System.out.println("SPARQL RULES QUERY :: " + rulesQry);

					ruleEndpoint = SparqlEndpointInterface.executeQuery(
							sparqlServerUrl, sparqlDataset, sparqlServerType,
							rulesQry, this.uName, this.passw, SparqlResultTypes.TABLE);
					ArrayList<String> AS = ruleEndpoint.getResultsColumnName();

					System.out.println("SPARQL RULES QUERY COLUMNS:: ");
					for (String b : AS) {
						System.out.println(b);
					}

					String[] queriesToRun = ruleEndpoint
							.getStringResultsColumn("query");

					System.out.println("SPARQL RULES QUERY :: RETURNED "
							+ queriesToRun.length + " ROWS");
					System.out
							.println("SPARQL RULES TEST RAN INTENTIONAL QUERY :: "
									+ sparqlQuery);

					// execute any applicable sparql rules.
					for (String qr : queriesToRun) {

						System.out
								.println("SPARQL RULES TEST RUNNING :: " + qr);

						SparqlEndpointInterface re = null;
						re = SparqlEndpointInterface.executeQuery(sparqlServerUrl,
								sparqlDataset, sparqlServerType, qr,
								this.uName, this.passw, SparqlResultTypes.TABLE);
					}
				}
			}
			else { 	// run all the rules we found. this has one really big potential downside:
					// there is no clean way to abort if any of the rules break. 
				String [] ruleCollection = endpoint.getStringResultsColumn("rule_text");
					
				// run the rules. assume that order does not count (much)
				for (String rule : ruleCollection) {
					// strip the rule to get only the rule itself...
					String ruleActual = rule;
					if(ruleActual.endsWith("\"^^<http://www.w3.org/2001/XMLSchema#string>")){
						ruleActual = ruleActual.replaceAll("\"^^<http://www.w3.org/2001/XMLSchema#string>", " ");
					}
					if(ruleActual.startsWith("\"")){
						ruleActual = ruleActual.substring(1); // kill the first quotation mark. 
					}
					// run this rule. 
					SparqlEndpointInterface ruleEndpoint = null;
					
					System.out.println("### BULK INSERT DEPENDENT QUERY WAS: " + ruleActual);
					
					ruleEndpoint = SparqlEndpointInterface.executeQuery(
							sparqlServerUrl, sparqlDataset, sparqlServerType,
							ruleActual, this.uName, this.passw, SparqlResultTypes.CONFIRM);
					
			
				}
			}
			// create a response.
			JSONObject retval = new JSONObject();
			retval.put("status", GeneralResultSet.SUCCESS);
			retval.put("message", "operations succeeded.");
			JSONObject tbl = new JSONObject();
			tbl.put("rows", null);
			tbl.put("col_names", null);
			tbl.put("col_type", null);
			tbl.put("col_count", 0);
			tbl.put("row_count", 0);
			retval.put(TableResultSet.RESULTS_BLOCK_NAME, tbl);

			// respond
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json");
			PrintWriter retWrite = response.getWriter();
			if (callback != null) {
				// support "proper" json-p
				retWrite.println(callback + "("
						+ JSONObject.toJSONString(retval) + ");");
			} else {
				retWrite.println(JSONObject.toJSONString(retval));
			}
		} catch (Exception E) {
			// make sure the cleanup is unconditional....

			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json");
			PrintWriter retWrite = response.getWriter();
			JSONObject retval = new JSONObject();
			retval.put("status", GeneralResultSet.FAILURE);
			retval.put("message", "operations failed.");
			JSONObject tbl = new JSONObject();
			tbl.put("rows", null);
			tbl.put("col_names", null);
			tbl.put("col_type", null);
			tbl.put("col_count", 0);
			tbl.put("row_count", 0);
			retval.put(TableResultSet.RESULTS_BLOCK_NAME, tbl);

			// do we have a failure cause.
			String potenitallyBlank = E.getMessage();

			if (potenitallyBlank == "" || potenitallyBlank == null) {
				potenitallyBlank = "an exception that includes no message was triggered";
			}

			// explain, as best as we can, what failed:
			retval.put("rationale", potenitallyBlank);
			if (callback != null) {
				// support "proper" json-p
				retWrite.println(callback + "("
						+ JSONObject.toJSONString(retval) + ");");
			} else {
				retWrite.println(JSONObject.toJSONString(retval));
			}
		}
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		throw new IOException("Servlet does not support POST requests.");
	}

}
