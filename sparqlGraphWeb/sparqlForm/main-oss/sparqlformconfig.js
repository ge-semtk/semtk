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

//VERSION: 85
define([], function() {
	var Config = {
		services : { 
			"query" : {
				"url" : "http://localhost:12050/sparqlQueryService",  
			},
			"status" : {
				"url" : "http://localhost:12051/status/",    
			},
			"results" : {
				"url" : "http://localhost:12052/results/",     
			},
            nodeGroup:{
                url : "http://localhost:12059/nodeGroup/",
            },
            "nodeGroupExec" : {
			     url : "http://localhost:12058/nodeGroupExecution/",
            },
		},
		help : {
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
		},
	};
	
	return Config;
});



