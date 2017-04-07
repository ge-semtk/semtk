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
		 * Column that can be added to many ImportMappings
		 * @description <font color="red">Users of {@link SemtkAPI} should not call this constructor.</font><br>Use {@link SemtkImportAPI#createImportColumn} instead
		 * @alias ImportColumn
		 * @class
		 */
		var ImportColumn= function (colName) {
			this.colName = colName;
			this.use = 0;
		};
		
		
		ImportColumn.prototype = {
			//
			// NOTE any methods without jsdoc comments is NOT meant to be used by API users.
			//      These methods' behaviors are not guaranteed to be stable in future releases.
			//
				
			getId : function () {
				return this.id;
			},
			
			/**
			 * Get the column name
			 * @returns {int}
			 */
			getColName : function () {
				return this.colName;
			},
			
			/**
			 * How many times is this item used in a MappingItem
			 * @returns {int}
			 */
			getUse : function () {
				return this.use;
			},
			
			incrUse : function (x) {
				return this.use += x;
			},
			
			fromJson : function (jObj) {
				this.colName = jObj.colName;
				this.use = 0;
				return jObj.colId;
			},
			
			toJson : function (id) {
				return {
					"colId" :    id,
					"colName"  : this.colName,
				}
			}
		};
	
		return ImportColumn;
	}
);