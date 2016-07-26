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
 * Sample of using Google Analytics for your logging.
 * See https://github.com/ge-semtk/semtk/wiki/Installing-Google-Analytics
 */


var KDLEasyLoggerConfig = function(){

  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','https://www.google-analytics.com/analytics.js','ga');

  ga('create', 'YOUR_GOOGLE_ANALYTICS_TRACKING_CODE', 'auto');
  ga('send', 'pageview');

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
        if (eLogger == null) eLogger = new KDLEasyLoggerConfig();

        ga('send', 'event', 'all', action, optDetailKey1, optDetailVal1);
};

kdlLogAndAlert = function (msg) {
        ga('send', 'event', 'all', 'alert', 'msg', msg);
        alert(msg);
};

kdlLogAndThrow = function (msg) {
        ga('send', 'event', 'all', 'exception', 'msg', msg);
        throw msg;
};

kdlLogNewWindow = function (action, url) {
        ga('send', 'event', 'all', 'url ' + action, 'url', msg);
        window.open(url);
};
