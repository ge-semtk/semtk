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
	
		/**
		 * Transformation that can be added to many ImportMappings
		 * API users should not call this constructor
		 * @description <font color="red">Users of {@link SemtkAPI} should not call this constructor.</font><br>Use {@link SemtkImportAPI#createImportTransform} instead
		 * @alias ImportTransform
		 * @class
		 */
		var ImportTransform = function (name, transType, arg1, arg2 ) {
			this.name = name;
			this.transType = transType;
			this.arg1 = arg1;
			this.arg2 = arg2;
			this.use = 0;
		};
		
		ImportTransform.TRANSFORMS = [ 	["Hash","hashCode","",""],
		                               	["Replace All",  "replaceAll",  "search", "replace"],
		                              	["To Uppercase", "toUpperCase", "",       ""],
		                              	["To Lowercase", "toLowerCase", "",       ""]
									 ];
		
		ImportTransform.getArgNames = function(type) {
			var i = 0;
			while ( i < ImportTransform.TRANSFORMS.length && ImportTransform.TRANSFORMS[i][1] !== type) {
				i += 1;
			}
			return [ImportTransform.TRANSFORMS[i][2], ImportTransform.TRANSFORMS[i][3]];
		}

		ImportTransform.prototype = {
				
			//
			// NOTE any methods without jsdoc comments is NOT meant to be used by API users.
			//      These methods' behaviors are not guaranteed to be stable in future releases.
			//
			
			/**
			 * Get the name
			 * @returns {string} display name of this transform
			 */
			getName : function () {
				return this.name;
			},
			
			/**
			 * Get the type
			 * @returns {string} one of first column in ImportTransform.TRANSFORMS
			 */
			getType : function () {
				return this.transType;
			},
			
			getArgNames : function() {
				return ImportTransform.getArgNames(this.transType);
			},
			
			/**
			 * Get the first arg
			 * @returns {string} as specified by third col in ImportTransform.TRANSFORMS
			 */
			getArg1 : function () {
				return this.arg1;
			},
			
			/**
			 * Get the second arg
			 * @returns {string} as specified by fourth col in ImportTransform.TRANSFORMS
			 */
			getArg2 : function () {
				return this.arg2;
			},
			
			/**
			 * How many times is this item used in a MappingItem
			 * @returns {int}
			 */
			getUse : function () {
				return this.use;
			},
			
			setName : function (x) {
				this.name = x;
			},
			
			setType : function (x) {
				this.transType = x;
			},
			
			setArg1 : function (x) {
				this.arg1 = x;
			},
			
			setArg2 : function (x) {
				this.arg2 = x;
			},
			
			incrUse : function (x) {
				return this.use += x;
			},
			
			// TODO: handle transform arguments and error checking
			fromJson : function (jObj) {
				this.name = jObj.name;
				this.transType = jObj.transType;
				this.arg1 = jObj.arg1;
				this.arg2 = jObj.arg2;
				
				this.use = 0;
				return jObj.transId;
			},
			
			// TODO: handle transform arguments and error checking
			toJson : function (id) {
				return {
					"transId" : id,
					"name" : this.name,
					"transType" : this.transType,
					"arg1" : this.arg1,
					"arg2" : this.arg2,
				};
			}
		};
	
		return ImportTransform;            // return the constructor
	}
);