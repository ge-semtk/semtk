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
// VERSION=1
// basic config information needed to run the logging

var KDLEasyLoggerConfig = function(){
	
};

KDLEasyLoggerConfig.prototype = {
		getProtocol : function(){
		},
		getServerName : function() {
		},
		getServerPort : function() {
		},
		getLogLocation : function() {
		},
		getApplicationID : function() {
		},
		getLoggingURLPrefix : function() {
		},
		setApplicationID : function(aID){
		},
		setLogLocation : function(locus){
		},
		setProtocol : function (proto) {
		},
		setLogServerAndPort : function(server, port){
		}
};

eLogger = null;	
eLoggerAlertCount = 0;

kdlLogEvent = function (action, optDetailKey1, optDetailVal1, optDetailKey2, optDetailVal2) { 
};