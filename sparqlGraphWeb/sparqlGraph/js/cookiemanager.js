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
 * Paul Cuddihy
 * Modeled after simple example on w3schools.com
 */

var CookieManager = function(document) {
    this.document = document;
    this.warnedFlag = false;
};

CookieManager.WARNING_SIZE = 6000;    // this limit is not a hard one.  So it might not work.
                                      // further, the cookies change size next time we load due to 
                                      //    cookies expiring
                                      //    cookies from other apps on this machine (including GE SSO  !!)

CookieManager.prototype = {
		
	setCookie : function (cname, cvalue, expiredays) {
		// untested: add or change a value in the cookie
		// expiredays is optional
		var d = new Date();
		var exp = expiredays ? expiredays : 365;
		
		d.setTime(d.getTime() + (exp * 24 * 60 * 60 * 1000));
		var expires = "expires=" + d.toGMTString();
		this.document.cookie = cname + "=" + cvalue + "; " + expires;
		
		if (this.document.cookie.length > CookieManager.WARNING_SIZE && ! this.warnedFlag) {
			// note this routine doesn't know what else to tell the user.
			alert("Cookies are getting large: " + this.document.cookie.length + " bytes.\n" +
				  "Your browser may act unpredictably next time you visit this URL.\n" +
				  "Occured while writing this cookie:\n" +
				  "  Cookie name is: " + cname + "\n" +
				  "  Value starts with: " + cvalue.slice(0,24));
			this.warnedFlag = true;
			
		
				  
		}
	},
	

	getCookie : function (cname) {
		// retrieve a cookie value, or null
		var name = cname + "=";
		var ca = this.document.cookie.split(';');
		
		for(var i=0; i < ca.length; i++) {
		  var c = ca[i].trim();
		  if (c.indexOf(name)==0) 
			  return c.substring(name.length,c.length);
		}
		return null;
	},
	
	delCookie : function (cname) {
		this.document.cookie = cname + '=;expires=Thu, 01 Jan 1970 00:00:01 GMT;';
		this.warnedFlag = false;
		
		//var re = new RegExp(cname + "=.*;");
		//var cookie2 = this.document.cookie.replace(re, "");
		//this.document.cookie = cookie2;
	},
	
	getIndexedCookie : function (cname, index) {
		return this.getCookie(this.indexName(cname, index));
	},
	
	setIndexedCookie : function (cname, index, cvalue, expiredays) {
		return this.setCookie(this.indexName(cname, index), cvalue, expiredays);
	},
	
	delIndexedCookie : function (cname, index) {
		return this.delCookie(this.indexName(cname, index));
	},
	
	indexName : function (cname, index) {
		return cname + '_' + index.toString();
	}

};

