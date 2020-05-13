/**
 ** Copyright 2016-17 General Electric Company
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

// SparqlForm config variables
// GOAL: everything that changes with deployment is in here

define([], function() {
	var Config = {
		"services" : {
			"query" : {
				url : "${WEB_PROTOCOL}://${WEB_SPARQL_QUERY_HOST}:${WEB_SPARQL_QUERY_PORT}/sparqlQueryService/",
			},
			"status" : {
				"url" : "${WEB_PROTOCOL}://${WEB_STATUS_HOST}:${WEB_STATUS_PORT}/status/",  // window.location.origin + "/status/",
			},
			"results" : {
				"url" : "${WEB_PROTOCOL}://${WEB_RESULTS_HOST}:${WEB_RESULTS_PORT}/results/",
			},
			"dispatcher" : {
				"url" : "${WEB_PROTOCOL}://${WEB_DISPATCH_HOST}:${WEB_DISPATCH_PORT}/dispatcher/",
			},
            nodeGroup:{
				url : "${WEB_PROTOCOL}://${WEB_NODEGROUP_HOST}:${WEB_NODEGROUP_PORT}/nodeGroup/",
            },
            "nodeGroupExec" : {
				url : "${WEB_PROTOCOL}://${WEB_NODEGROUPEXECUTION_HOST}:${WEB_NODEGROUPEXECUTION_PORT}/nodeGroupExecution/",
            },
            nodeGroupStore:{
				url : "${WEB_PROTOCOL}://${WEB_NODEGROUPSTORE_HOST}:${WEB_NODEGROUPSTORE_PORT}/nodeGroupStore/",
            },
			ontologyInfo:{
				url : "${WEB_PROTOCOL}://${WEB_ONTOLOGYINFO_HOST}:${WEB_ONTOLOGYINFO_PORT}/ontologyinfo/",
            },
		},
		"help" : {
			"aboutHtml" : '<b>SparqlForm Opensource build 2.0</b><br>\
				Knowledge Discovery Lab<br>\
				Contact: Paul Cuddihy cuddihy@ge.com<br>\
				GE Research, Niskayuna<br>',

			"legalNoticeHtml" : ' Copyright © 2014-2017  General Electric Company.  <br>\
			    <b>Licensed under the Apache License, Version 2.0 (the "License") </b>  <br>\
			    you may not use this file except in compliance with the License.  <br>\
			    You may obtain a copy of the License at  <br>\
			    <br>\
 				<a href="http://www.apache.org/licenses/LICENSE-2.0" target="_blank">http://www.apache.org/licenses/LICENSE-2.0</a><br>\
			    <br>\
			    Unless required by applicable law or agreed to in writing, software  \
			    distributed under the License is distributed on an "AS IS" BASIS,  \
			    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. \
			    See the License for the specific language governing permissions and  \
			    limitations under the License.  <br>\
			    <br>\
			    <b>Covered by US Patent 9,760,614/b><br>\
			    <b>Other Patents Pending.</b><br>\
			    <br>\
			    <u>Includes other open source code:</u><br>\
			    - vis.js (C) 2010-2017 Almende B.V.  Licensed under MIT and Apache 2.0<br>\
			    ',
		},
        "itemDialog" : {
            "limit"    : 10000,   // max number of choices shown
            "maxValues": 100      // max size of VALUES clause
        },
        "timeout" : {
            "short" : 5000,
            "long"  : 30000
        },
        "resultsTable" : {
            "sampleSize" : 200
        }
	};

	return Config;
});
