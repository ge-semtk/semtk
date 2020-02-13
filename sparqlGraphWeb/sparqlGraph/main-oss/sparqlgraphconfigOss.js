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

// SparqlGraph config variables
// GOAL: everything that changes with deployment is in here

var g = {
	help : {
		buildHtml : "SPARQLgraph<br>",
		aboutHtml : "AI and Machine Learning<br>\
				     GE Research, Niskayuna<br>\
				    ",

		legalNoticeHtml : ' Copyright © 2014-2018  General Electric Company.  <br>\
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
						    <b>Patents Pending.</b><br>\
						    <br>\
						    <u>Includes other open source code:</u><br>\
							<i>Raphael 1.3.1 - JavaScript Vector Library. </i><br>\
		    				Distributed under MIT license.<br>\
						    - Copyright (c) 2008 - 2009 Dmitry Baranovskiy (http://raphaeljs.com)  <br>\
						    <br>\
							<i>Dracula Graph Layout and Drawing Framework 0.0.3alpha. </i><br>\
		    				Distributed under MIT license.<br>\
						    - (c) 2010 Philipp Strathausen <strathausen@gmail.com>, http://strathausen.eu Contributions by Jake Stothard <stothardj@gmail.com>.<br>\
						    based on the Graph JavaScript framework, version 0.0.1<br>\
						    - (c) 2006 Aslak Hellesoy <aslak.hellesoy@gmail.com><br>\
						    - (c) 2006 Dave Hoover <dave.hoover@gmail.com><br>\
						    <br>\
						    <i>Curry - Function currying.</i><br>\
		    				Licensed under BSD (http://www.opensource.org/licenses/bsd-license.php)<br>\
						    - Copyright (c) 2008 Ariel Flesler - aflesler(at)gmail(dot)com | http://flesler.blogspot.com<br>\
						    ',

		url : {
			base : "https://github.com/ge-semtk/semtk/wiki",
			blog : "https://github.com/ge-semtk/semtk/wiki",

			// base#tab
			demo : "demo",

		}
	},

    demoNg : "demoNodegroup.json",

	customization: {
		bannerText: "${WEB_CUSTOM_BANNER_TEXT}",
		startupDialogTitle: "${WEB_CUSTOM_STARTUP_DIALOG_TITLE}",
		startupDialogHtml: "${WEB_CUSTOM_STARTUP_DIALOG_HTML}",
		autoRunDemo: "${WEB_CUSTOM_AUTO_RUN_DEMO_FLAG}"
	},

	service : {
		ingestion :{
			url : "${WEB_PROTOCOL}://${WEB_INGESTION_HOST}:${WEB_INGESTION_PORT}/ingestion/",
		},
		sparqlQuery:{
			url : "${WEB_PROTOCOL}://${WEB_SPARQL_QUERY_HOST}:${WEB_SPARQL_QUERY_PORT}/sparqlQueryService/",
		},
        status : {
			"url" : "${WEB_PROTOCOL}://${WEB_STATUS_HOST}:${WEB_STATUS_PORT}/status/",  // window.location.origin + "/status/",
		},
		results : {
			"url" : "${WEB_PROTOCOL}://${WEB_RESULTS_HOST}:${WEB_RESULTS_PORT}/results/",
		},
        dispatcher : {
			"url" : "${WEB_PROTOCOL}://${WEB_DISPATCH_HOST}:${WEB_DISPATCH_PORT}/dispatcher/",
		},
        hive : {
			"url" : "${WEB_PROTOCOL}://${WEB_HIVE_HOST}:${WEB_HIVE_PORT}/hiveService/",
		},
		nodeGroupStore:{
			url : "${WEB_PROTOCOL}://${WEB_NODEGROUPSTORE_HOST}:${WEB_NODEGROUPSTORE_PORT}/nodeGroupStore/",
		},
        ontologyInfo:{
			url : "${WEB_PROTOCOL}://${WEB_ONTOLOGYINFO_HOST}:${WEB_ONTOLOGYINFO_PORT}/ontologyinfo/",
		},
        nodeGroupExec:{
			url : "${WEB_PROTOCOL}://${WEB_NODEGROUPEXECUTION_HOST}:${WEB_NODEGROUPEXECUTION_PORT}/nodeGroupExecution/",
		},
        nodeGroup:{
			url : "${WEB_PROTOCOL}://${WEB_NODEGROUP_HOST}:${WEB_NODEGROUP_PORT}/nodeGroup/",
		},
	},
	tab : {
		query : "queryTab",
		mapping : "mappingTab",
		upload : "uploadTab",
	},
	userEndpoint : "${WEB_USER_ENDPOINT}",

	longTimeoutMsec: 8000,
	shortTimeoutMsec: 5000,
};
