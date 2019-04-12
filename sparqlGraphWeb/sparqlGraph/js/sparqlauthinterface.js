/**
 ** Copyright 2016 General Electric Company
 **
 ** Authors:  Paul Cuddihy, Justin McHugh
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

/*
 * Interface to execute queries on SPARQL auth
 */


function SPARQLAuthInterface(servletDirURL, sparqlServerURL, sparqlServerType, graph) {
  this.baseURL = servletDirURL;
  this.sparqlServerURL = sparqlServerURL;
  this.sparqlServerType = sparqlServerType;
  this.graph = graph;
  this.successCallback = null;
  this.failureCallback = null;
  this.statusCallback = null;
}

SPARQLAuthInterface.prototype = {

  // execute a freeform query.  Can pass in a success callback, or none to use the default.
  executeQuery : function(query, optSuccessCallback){
    if(!optSuccessCallback){
      optSuccessCallback = this.sparqlAuthSuccessCallback;
    }
    this.runSparqlAuthServlet(query, optSuccessCallback, this.sparqlAuthFailureCallback, this.sparqlAuthStatusCallback);
  },

  executeQueryPost : function(query, optSuccessCallback){
	    if(!optSuccessCallback){
	      optSuccessCallback = this.sparqlAuthSuccessCallback;
	    }
	    this.runSparqlAuthServletPost(query, optSuccessCallback, this.sparqlAuthFailureCallback, this.sparqlAuthStatusCallback);
	  },

  // delete a URI.  Can pass in a success callback, or none to use the default.
  deleteURI : function(uri, uriType, optSuccessCallback){
    if(!optSuccessCallback){
      optSuccessCallback = this.sparqlAuthSuccessCallback;
    }
    this.runSparqlAuthServletDeleteURI(uri, uriType, "subjectobject", optSuccessCallback, this.sparqlAuthFailureCallback, this.sparqlAuthStatusCallback);
  },

  // delete a URI only when it is a triple subject.  Can pass in a success callback, or none to use the default.
  deleteURISubject : function(uri, uriType, optSuccessCallback){
    if(!optSuccessCallback){
      optSuccessCallback = this.sparqlAuthSuccessCallback;
    }
    this.runSparqlAuthServletDeleteURI(uri, uriType, "subject", optSuccessCallback, this.sparqlAuthFailureCallback, this.sparqlAuthStatusCallback);
  },

  // delete a URI only when it is a triple subject, with a given predicate.  Can pass in a success callback, or none to use the default.
  deleteURISubjectPredicate : function(uri, uriType, predicate, optSuccessCallback){
    if(!optSuccessCallback){
      optSuccessCallback = this.sparqlAuthSuccessCallback;
    }
    this.runSparqlAuthServletDeleteURI(uri, uriType, "subject", optSuccessCallback, this.sparqlAuthFailureCallback, this.sparqlAuthStatusCallback, predicate);
  },

  // delete a URI only when it is a triple object.  Can pass in a success callback, or none to use the default.
  deleteURIObject : function(uri, uriType, optSuccessCallback){
    if(!optSuccessCallback){
      optSuccessCallback = this.sparqlAuthSuccessCallback;
    }
    this.runSparqlAuthServletDeleteURI(uri, uriType, "object", optSuccessCallback, this.sparqlAuthFailureCallback, this.sparqlAuthStatusCallback);
  },

  sparqlAuthSuccessCallback : function(result){
    // result.getStatus can be success or failure, even if in success callback
    if(result.getStatus() == "success"){
      // do nothing
    }else{
      alert("SPARQL Auth: " + result.getRationale());
    }
  },

  sparqlAuthFailureCallback : function(result){
    alert("SPARQL Auth failure: " + result.getRationale());
  },
  sparqlAuthStatusCallback : function(result){
    alert("SPARQL Auth status: " + result.getRationale());
  },

  // execute a query
  runSparqlAuthServlet : function(query, successCallback, failureCallback, statusCallback) {
    if (query == null) {
      this.failureCallback("No query to run.");
      return;
    }
    this.successCallback = successCallback;
    this.failureCallback = failureCallback;
    this.statusCallback = statusCallback;

    var url = this.baseURL + '/SparqlAuthServlet' +
    "?sparqlServerURL="  + encodeURIComponent(this.sparqlServerURL) +
    "&sparqlServerType=" + encodeURIComponent(this.sparqlServerType) +
    "&sparqlDataset="    + encodeURIComponent(this.graph) +
    "&sparqlQuery="      + encodeURIComponent(query) +
    "";
    //alert('URL: ' + url + '\n\nDECODED: ' + decodeURIComponent(url));
    var th = this;
    $.ajax({url: url,
      type: 'GET',
      success: function (x) {
        th.callbackSPARQLAuthQuerySuccess(x);
      },
      failure: function (x) {
        th.callbackSPARQLAuthQueryFailure(x);
      },
      datatype: "json"
    });
  },

  runSparqlAuthServletPost : function(query, successCallback, failureCallback, statusCallback) {
	    if (query == null) {
	      this.failureCallback("No query to run.");
	      return;
	    }
	    this.successCallback = successCallback;
	    this.failureCallback = failureCallback;
	    this.statusCallback = statusCallback;

	    var url = this.baseURL + '/SparqlAuthServlet';

	    //alert('URL: ' + url + '\n\nDECODED: ' + decodeURIComponent(url));
	    var th = this;
	    $.ajax({url: url,
	      type: 'POST',
	      dataType: "json",
	      data	 : { "sparqlQuery" : query, "sparqlDataset" : this.graph, "sparqlServerType" :  this.sparqlServerType, "sparqlServerURL" : this.sparqlServerURL},
	      success: function (x) {
	        th.callbackSPARQLAuthQuerySuccess(x);
	      },
	      failure: function (x) {
	        th.callbackSPARQLAuthQueryFailure(x);
	      },
	      datatype: "json"
	    });
	  },

  callbackSPARQLAuthQuerySuccess : function(responseJson) {
    this.successCallback(new SPARQLAuthResult(responseJson));
  },

  callbackSPARQLAuthQueryFailure : function(responseJson) {
   alert("SPARQL Auth failed");
  },

  // delete a URI.  Specify a URI, type of the URI, and triple position (subject, object, or subjectobject).  Optionally specify a predicate as well.
  runSparqlAuthServletDeleteURI : function(uri, uriType, position, successCallback, failureCallback, statusCallback, optPredicate) {
    if (uri == null) {
      this.failureCallback("No URI specified.");
      return;
    }
    if (uriType == null) {
      this.failureCallback("No URI type specified.");
      return;
    }
    if (position == null) {
      this.failureCallback("No delete position specified.");  // "subject", "object", or "subjectobject"
      return;
    }
    this.successCallback = successCallback;
    this.failureCallback = failureCallback;
    this.statusCallback = statusCallback;

    var url = this.baseURL + '/SparqlAuthServlet' +
    "?sparqlServerURL="  + encodeURIComponent(this.sparqlServerURL) +
    "&sparqlServerType=" + encodeURIComponent(this.sparqlServerType) +
    "&sparqlDataset="    + encodeURIComponent(this.graph) +
    "&uriToDelete="      + encodeURIComponent(uri) +
    "&uriTypeToDelete="  + encodeURIComponent(uriType) +
    "&deletePosition="   + encodeURIComponent(position) +
    "";
    if(optPredicate){
      url += "&deletePredicate=" + encodeURIComponent(optPredicate);
    }
    //alert('URL: ' + url + '\n\nDECODED: ' + decodeURIComponent(url));
    var th = this;
    $.ajax({url: url,
      type: 'GET',
      success: function (x) {
        th.callbackSPARQLAuthQuerySuccess(x);
      },
      failure: function (x) {
        th.callbackSPARQLAuthQueryFailure(x);
      },
      datatype: "json"
    });
  },

};

function SPARQLAuthResult(resultJSON) {
  this.jsonObj = resultJSON;
};

SPARQLAuthResult.prototype = {
  NAMESPACE_YES : 1,
  NAMESPACE_NO : 2,
  NAMESPACE_ONLY : 3,

  isSuccess : function() {
    return this.getStatus() == "success";
  },

  getStatus : function() {
    if (this.jsonObj != null && this.jsonObj.hasOwnProperty('status'))
      return this.jsonObj.status;
    else
      return "<null status>";
  },

  getRationale : function() {
    if (this.jsonObj != null && this.jsonObj.hasOwnProperty('rationale'))
      return this.jsonObj.rationale;
    else
      return "<null rationale>";
  },

};
