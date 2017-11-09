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
import java.util.concurrent.RecursiveTask;

import org.json.simple.JSONObject;

import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Stores all parameters passed to an individual sparql subquery and its eventual response.
 */
public class SparqlSubquery extends RecursiveTask<Void> {
    private static final long serialVersionUID = 1L;

    private String sparqlServerUrl;
    private String sparqlServerType;
    private String sparqlDataset;
    private String sparqlQuery;
    private String resultsColumnNameSuffix;
//    private JSONObject response;
    private Table responseTable;
    private ArrayList<String> columnNamesInResponse;

    public SparqlSubquery(JSONObject sq) throws Exception {
    	// let's build the subquery object we need from the serialized JSON representing it.
    	// on failure, toss exception to caller.

    	this.sparqlDataset = (String) sq.get("dataset");
    	this.sparqlServerType = (String) sq.get("servertype");
    	this.sparqlServerUrl = (String) sq.get("serverurl");
    	this.resultsColumnNameSuffix = (String) sq.get("resultssuffix");
    	this.sparqlQuery = (String) sq.get("query");

    	// check for empty/null and throw exception if it happened:
    	if (isMissingAny(sparqlDataset, sparqlServerType, sparqlServerUrl, sparqlQuery)) {
    		throw new Exception("subQuery did not have full collection of required parameters. dataset, servertype, server url, query are required");
    	}
    	// there was no way to promise that we have unique names for outgoing columns. this could pose a problem. warn user/system to logs.
    	if (isMissingAny(resultsColumnNameSuffix)) {
    		LocalLogger.logToStdOut("no suffix given for subquery. column names will be returned without modification.");
    	}
    }

    private boolean isMissingAny(String... parameters) {
        boolean missing = false;
        for (String parameter : parameters) {
            if (parameter == null || parameter.isEmpty()) {
                missing = true;
                break;
            }
        }
        return missing;
    }

    public String getSparqlServerUrl() {
        return sparqlServerUrl;
    }

    public void setSparqlServerUrl(String sparqlServerUrl) {
        this.sparqlServerUrl = sparqlServerUrl;
    }

    public String getSparqlServerType() {
        return sparqlServerType;
    }

    public void setSparqlServerType(String sparqlServerType) {
        this.sparqlServerType = sparqlServerType;
    }

    public String getSparqlDataset() {
        return sparqlDataset;
    }

    public void setSparqlDataset(String sparqlDataset) {
        this.sparqlDataset = sparqlDataset;
    }

    public String getSparqlQuery() {
        return sparqlQuery;
    }

    public void setSparqlQuery(String sparqlQuery) {
        this.sparqlQuery = sparqlQuery;
    }

    public String getResultsColumnNameSuffix() {
        return (resultsColumnNameSuffix != null) ? resultsColumnNameSuffix : "";
    }

    public void setResultsColumnNameSuffix(String resultsSuffix) {
        this.resultsColumnNameSuffix = resultsSuffix;
    }

    public Table getResponseTable() {
        return responseTable;
    }

    public ArrayList<String> getColumnNamesInResponse() {
        if (columnNamesInResponse == null) {
            columnNamesInResponse = new ArrayList<>();
            if (responseTable != null) {
                // Get the column names in the response table
                /*
            	JSONObject head = (JSONObject) response.get("head");
                JSONArray vars = (head != null) ? (JSONArray) head.get("vars") : null;
                if (vars != null) {
                    for (int i = 0; i < vars.size(); i++) {
                        columnNamesInResponse.add((String) vars.get(i));
                    }
                }
		*/
		for (String name : responseTable.getColumnNames ())
		    columnNamesInResponse.add (name);
            }
        }
        return columnNamesInResponse;
    }

    // Invoked internally by parent SparqlParallelQueries instance
    @Override
    protected Void compute(){
        try {
//LocalLogger.logToStdOut ("About to runSparqlQuery");
            runSparqlQuery();
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }
        return null;
    }

    // Run the semantic query and save the response
    public void runSparqlQuery() throws Exception {
        columnNamesInResponse = null;      
//LocalLogger.logToStdOut ("Parallel query:\n" + sparqlQuery);
    	SparqlEndpointInterface sei = SparqlEndpointInterface.getInstance(sparqlServerType, sparqlServerUrl, sparqlDataset);
//        response = sei.executeQuery(sparqlQuery);
        TableResultSet resultSet = (TableResultSet) sei.executeQueryAndBuildResultSet(sparqlQuery, SparqlResultTypes.TABLE);
        responseTable = resultSet.getTable();
//LocalLogger.logToStdOut ("Parallel query response:\n" + responseTable.toCSVString());
//LocalLogger.logToStdOut ("Parallel query response size = " + responseTable.getRows().size());
    }

} /* end of file */
