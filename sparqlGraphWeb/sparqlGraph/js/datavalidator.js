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
            'sparqlgraph/js/columnvalidator'

			// shimmed

		],

	function(ColumnValidator) {


		var DataValidator = function (json) {
            if (typeof json == "undefined" || json == null || json == []) {
                this.colValidatorList = [];
            } else if (! Array.isArray(json)) {
                throw new Error("Expected array of column validators");
            } else {
                this.colValidatorList = [];
                for (var j of json) {
                    this.colValidatorList.push(new ColumnValidator(j));
                }
            }
		};

		DataValidator.prototype = {

			/**
			 * Return object or null
			 * @returns {string} display name of this transform
			 */
			get : function (colName) {
                for (var c of this.colValidatorList) {
                    if (c.getColName() == colName) {
                        return c;
                    }
                }
				return null;
			},

            put : function(columnValidator) {
                var colName = columnValidator.getColName();

                // remove existing
                for (var i=0; i < this.colValidatorList.length; i++) {
                    if (this.colValidatorList[i].getColName() == colName) {
                        this.colValidatorList.splice(i,1);
                        break;
                    }
                }
                // add new one
				if (! columnValidator.isEmpty()) {
                	this.colValidatorList.push(columnValidator);
				}
            },

			// TODO: handle transform arguments and error checking
			toJson : function (id) {
				var ret = [];
                for (var c of this.colValidatorList) {
					if (! c.isEmpty()) {
                    	ret.push(c.toJson());
					}
                }
				return ret;
			}
		};

		return DataValidator;            // return the constructor
	}
);
