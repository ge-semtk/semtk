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

//VERSION: 84
var g = {
	help : {
		buildHtml : "SPARQLgraph build 752 on 2016-06-08",
		aboutHtml : "Knowledge Discovery Lab<br>\
				     GE Research, Niskayuna<br>\
				    ",

		legalNoticeHtml : ' Copyright © 2014-2017  General Electric Company.  <br>\
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
			base : "http://www.google.com",
			blog : "http://www.google.com",
			
			// base#tab
			queryTab : "QueryTab",
			importTab : "MapInputTab",
			uploadTab : "ImportTab",

		}
	},
	
	service : {
		ingestion :{
			url : "http://vesuvius37.crd.ge.com:12091/ingestion/",
		},
		sparqlQueryTEDS_AWS_VPC:{
			url : "http://localhost:2450/sparqlQueryService/",
		},
		sparqlQuery:{
			url : "http://vesuvius37.crd.ge.com:12050/sparqlQueryService/",
		},
        status : {
			"url" : "http://vesuvius37.crd.ge.com:12051/status/",    
		},
		results : {
			"url" : "http://vesuvius37.crd.ge.com:12052/results/",     
		},
		nodeGroupStore:{
			url : "http://vesuvius37.crd.ge.com:12056/nodeGroupStore/",
		},
		nodeGroup:{
			url : "http://vesuvius37.crd.ge.com:12059/nodeGroup/",
		},
        nodeGroupExec:{
			url : "http://vesuvius37.crd.ge.com:12058/nodeGroupExecution/",
		},
	},
	
	tab : {
		query : "queryTab",
		mapping : "mappingTab",
		upload : "uploadTab",
	}
};



