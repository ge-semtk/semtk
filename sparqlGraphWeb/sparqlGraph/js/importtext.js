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

define([	// properly require.config'ed
        
			// shimmed
			
		],

	function() {
	
		/*
		 *    text value used to build triple
		 */
		var ImportText = function (text) {
			this.text = text;
			this.use = 0;
		};
			
		ImportText.prototype = {

			
			getText : function () {
				return this.text;
			},
			
			getUse : function () {
				return this.use;
			},
			
			setText : function(text) {
				this.text = text;
			},
			
			incrUse : function (x) {
				return this.use += x;
			},
			
			fromJson : function (jObj) {
				this.text = jObj.text;
				this.use = 0;
				return jObj.textId;
			},
			
			toJson : function (id) {
				return {
					"textId" : id,
					"text"   : this.text,
				};
			},
		};
	
		return ImportText;            // return the constructor
	}
);