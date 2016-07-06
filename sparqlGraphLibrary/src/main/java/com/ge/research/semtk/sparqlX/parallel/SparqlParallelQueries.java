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


package com.ge.research.semtk.sparqlX.parallel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;

import org.apache.commons.math3.util.Pair;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Sets;

/**
 * Stores all parameters passed to SparqlParallelQueryServlet and its eventual response.
 */
public class SparqlParallelQueries extends RecursiveTask<Void> {
	private static final long serialVersionUID = 1L;

	private List<SparqlSubquery> subqueries;
	private String subqueryType;
	private boolean isSubqueryOptional;
	private Set<String> columnsToFuseOn;
	private Set<String> columnsToReturn;

	private TableResultSet gResultTable = null;

	@SuppressWarnings("unchecked")
	public SparqlParallelQueries(String subqueriesJson, String subqueryType, boolean isSubqueryOptional, String columnsToFuseOn, String columnsToReturn) throws Exception {
		// parse the json array and build the subquery objects as we go
		gResultTable = null;

		JSONArray subqueries = (JSONArray)(new JSONParser()).parse(subqueriesJson);    		
		this.subqueries = new ArrayList<>(subqueries.size());
		for (int i = 0; i < subqueries.size(); i++) {
			JSONObject subquery = (JSONObject) subqueries.get(i);
			// let the constructor do the heavy lifting here
			this.subqueries.add(new SparqlSubquery(subquery));
		}

		this.subqueryType = subqueryType;
		this.isSubqueryOptional = isSubqueryOptional;
		this.columnsToFuseOn = new LinkedHashSet<>(Arrays.asList(columnsToFuseOn.split(",")));
		this.columnsToReturn = new LinkedHashSet<>(Arrays.asList(columnsToReturn.split(",")));

		if (this.subqueries.size() == 0) {
			// this was completely invalid a call as we have no subqueries to process
			throw new Exception("subqueries json does not contain any subqueries.");
		}
	}

	public List<SparqlSubquery> getSubqueries() {
		return subqueries;
	}

	public void setSubqueries(List<SparqlSubquery> subqueries) {
		this.subqueries = subqueries;
	}

	public String getSubqueryType() {
		return subqueryType;
	}

	public void setSubqueryType(String subqueryType) {
		this.subqueryType = subqueryType;
	}

	public boolean isOptionalSubqueries() {
		return isSubqueryOptional;
	}

	public void setOptionalSubqueries(boolean optionalSubqueries) {
		this.isSubqueryOptional = optionalSubqueries;
	}

	public Set<String> getColumnsToFuseOn() {
		return columnsToFuseOn;
	}

	public void setColumnsToFuseOn(String... columnsToFuseOn) {
		this.columnsToFuseOn.clear();
		this.columnsToFuseOn.addAll(Arrays.asList(columnsToFuseOn));
	}

	public Set<String> getColumnsToReturn() {
		return columnsToReturn;
	}

	public void setColumnsToReturn(String... columnsToReturn) {
		this.columnsToReturn.clear();
		this.columnsToReturn.addAll(Arrays.asList(columnsToReturn));
	}

	public void runQueries() {
		// Use fork-join technique
		// https://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html
		ForkJoinPool.commonPool().invoke(this);
	}

	// Invoked internally by runQueries() method
	@Override
	protected Void compute() {
		invokeAll(subqueries);
		return null;
	}

	public TableResultSet returnFusedResultsTable () throws Throwable, Exception {
		JSONObject tmp = null;
		if (gResultTable == null)
			tmp = returnFusedResults ();

		return gResultTable;
	}

	public JSONObject returnFusedResults() throws Throwable, Exception {
		// Make sure that all the queries returned successfully, else rethrow whatever exception occurred
		for (SparqlSubquery subquery : subqueries) {
			Throwable t = subquery.getException();
			if (t != null) {
				throw t;
			}
		}

		// Collect all the vars from the subqueries into a multiset so we will know which vars are unique and won't need to be renamed
		HashMultiset<String> multiSet = HashMultiset.create();
		for (SparqlSubquery subquery : subqueries) {
			ArrayList<String> columnsInResponse = subquery.getColumnNamesInResponse();
			multiSet.addAll(columnsInResponse);
		}

		// Build unique var names from the columns to return and the subqueries' suffixes
		JSONArray vars = new JSONArray();
		Set<String> distinctVars = new LinkedHashSet<>();
		for (SparqlSubquery subquery : subqueries) {
			String suffix = subquery.getResultsColumnNameSuffix();
			ArrayList<String> columnsInResponse = subquery.getColumnNamesInResponse();
			for (String column : columnsToReturn) {
				if (columnsInResponse.contains(column)) {
					if (multiSet.count(column) == 1) {
						distinctVars.add(column);
					} else if (columnsToFuseOn.contains(column)) {
						distinctVars.add(column);
					} else {
						distinctVars.add(column + suffix);
					}
				}
			}
		}
		for (String var : distinctVars) {
			vars.add(var);
		}

		// Collect the distinct bindings from the subqueries into sets by the columns to fuse on.
		Map<String/*fusedCols*/, Map<String/*suffix*/, Set<Pair<SparqlSubquery/*subquery*/, ArrayList<String>/*binding*/>>>> fusedColsToBindings = new LinkedHashMap<>();
		StringBuilder sb = new StringBuilder();
		for (SparqlSubquery subquery : subqueries) {
			// Get the subquery's bindings
			String suffix = subquery.getResultsColumnNameSuffix();
			Table resultsTable = subquery.getResponseTable();
			if ((resultsTable != null) && (resultsTable.getNumRows() != 0)) {
				System.out.println("Query " + suffix + " has " + resultsTable.getNumRows() + " rows");
				ArrayList<String> columnsInResponse = subquery.getColumnNamesInResponse();
				for (ArrayList<String> row : resultsTable.getRows()) {
					// Compose the lookup key from the columns to fuse on
					sb.setLength(0);
					for (String column : columnsToFuseOn) {
						if (columnsInResponse.contains(column)) {
							String value = (String) row.get(resultsTable.getColumnIndex(column));
							if (sb.length() > 0) sb.append("\t");
							sb.append(value);
						}
					}
					String fusedCols = sb.toString();

					// Add the binding to the set of distinct bindings for the same fusedCols and same suffix
					Map<String/*suffix*/, Set<Pair<SparqlSubquery/*subquery*/, ArrayList<String>/*binding*/>>> suffixToBindings = fusedColsToBindings.get(fusedCols);
					if (suffixToBindings == null) {
						suffixToBindings = new LinkedHashMap<>();
						fusedColsToBindings.put(fusedCols, suffixToBindings);
					}
					Set<Pair<SparqlSubquery/*subquery*/, ArrayList<String>/*binding*/>> distinctBindings = suffixToBindings.get(suffix);
					if (distinctBindings == null) {
						distinctBindings = new LinkedHashSet<>();
						suffixToBindings.put(suffix, distinctBindings);
					}
					distinctBindings.add(new Pair<>(subquery, row));
				}
			} else {
				System.out.println("Query " + suffix + " has 0 rows");
			}
		}
//System.out.println ("XXXXXXXXXXXXXXXXXXXXXXXXXXX  GOT HERE  XXXXXXXXXXXXXXXXXXXXX");

		// Build the array of output bindings with unique var names
		int numOutputBindings = 0;
		//JsonArrayBuilder outputBindings = Json.createArrayBuilder();
		JSONArray outputBindings = new JSONArray();
		ArrayList<ArrayList<String>> outputRows = new ArrayList<ArrayList<String>> ();
		ArrayList<String> outputColumnNames = new ArrayList<String> ();
		for (Map<String/*suffix*/, Set<Pair<SparqlSubquery/*subquery*/, ArrayList<String>/*binding*/>>> suffixToBindings : fusedColsToBindings.values()) {
			// Collect each subquery's set of distinct bindings for the same fusedCols
			List<Set<Pair<SparqlSubquery/*subquery*/, ArrayList<String>/*binding*/>>> fusedColsBindings = new ArrayList<>();
			for (SparqlSubquery subquery : subqueries) {
				String suffix = subquery.getResultsColumnNameSuffix();
				Set<Pair<SparqlSubquery/*subquery*/, ArrayList<String>/*binding*/>> distinctBindings = suffixToBindings.get(suffix);
				if (fusedColsBindings != null && distinctBindings != null) {
					fusedColsBindings.add(distinctBindings);
				} else if (!isSubqueryOptional) {
					// Don't output any bindings for this particular fusedCols
					fusedColsBindings = null;
				}
			}

			if (fusedColsBindings != null) {
				// Iterate over every possible list that can be formed by choosing one element from each of the above sets in order
				Set<List<Pair<SparqlSubquery/*subquery*/, ArrayList<String>/*binding*/>>> allFusedColsBindings = Sets.cartesianProduct(fusedColsBindings);
				for (List<Pair<SparqlSubquery/*subquery*/, ArrayList<String>/*binding*/>> fusedColsBinding : allFusedColsBindings) {
					// Create the new output binding
					//JsonObjectBuilder outputBinding = Json.createObjectBuilder();
					ArrayList<String> outputRow = new ArrayList<String> ();
					String addedCols = ",";
					for (Pair<SparqlSubquery/*subquery*/, ArrayList<String>> rowPair : fusedColsBinding) {
						// Add each var to the output binding, uniquified by the subquery's suffix when needed
						String suffix = rowPair.getFirst().getResultsColumnNameSuffix();
						ArrayList<String> columnsInResponse = rowPair.getFirst().getColumnNamesInResponse();
						for (String column : columnsToReturn) {
							if (columnsInResponse.contains(column)) {
								if (addedCols.indexOf("," + column + ",") >= 0)
									continue;
								String var = rowPair.getSecond().get(rowPair.getFirst().getResponseTable().getColumnIndex(column));
								if (multiSet.count(column) == 1) {
									outputRow.add (var);
									addedCols += column + ",";
									if (numOutputBindings == 0)
										outputColumnNames.add (column);
								} else if (columnsToFuseOn.contains(column)) {
									outputRow.add (var);
									addedCols += column + ",";
									if (numOutputBindings == 0)
										outputColumnNames.add (column);
								} else {
									outputRow.add (var);
									addedCols += column + suffix + ",";
									if (numOutputBindings == 0)
										outputColumnNames.add (column + suffix);
								}
							}
						}
					}
					outputRows.add (outputRow);
					numOutputBindings++;
				}
			}
		}
		String[] outputColumnNamesArray = (String[]) outputColumnNames.toArray(new String[0]);
		String[] outputColumnTypes = new String[outputColumnNamesArray.length];
		for (int i = 0; i < outputColumnTypes.length; i++)
			outputColumnTypes[i] = "String";

		Table outputTable = new Table (outputColumnNamesArray, outputColumnTypes, outputRows);

		System.out.println("Fused response has " + numOutputBindings + " results");

		gResultTable = new TableResultSet(true);
//		System.out.println(outputTable.toCSVString());
		gResultTable.addResults(outputTable);
		return gResultTable.toJson();
	}

} /* end of file */
