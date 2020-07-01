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

        // This object holds a json which has all the fields in their correct types.
        //     (note date variants are strings)
        // CONFIG defines what keys are legal beyond colName

        // read from json, unused things will be undefined
		var ColumnValidator = function (jsonOrName) {
            // change plain empty name into json
            var other = null;
            if (typeof jsonOrName == "string") {
                other = { "colName" : jsonOrName };
            } else {
                other = jsonOrName;
            }

            // colname is special
			this.json = { "colName" : other.colName };

            // set all validators.  Might be undefined.
            for (var config of ColumnValidator.CONFIG) {
                this.json[config.key] = other[config.key];
            }
		};

        // list retains order
        ColumnValidator.CONFIG = [
            { key: "mustExist",     "alias": "column exists",   "type": "enum",  "enum": [true]},
            { key: "notEmpty",      "alias": "not empty",       "type": "enum",  "enum": [true]},
            { key: "regexMatches",  "alias": "regex match",    "type": "regex"},
            { key: "regexNoMatch",  "alias": "regex no match", "type": "regex"},
            { key: "type",          "alias": "type",           "type": "enum", "enum": ["int", "float", "date", "datetime", "time"]},
            { key: "lt",            "alias": "<",              "type": "arith" },
            { key: "gt",            "alias": ">",              "type": "arith" },
            { key: "lte",           "alias": "<=",             "type": "arith" },
            { key: "gte",           "alias": ">=",             "type": "arith" },
            { key: "ne",            "alias": "!=",             "type": "arith" }
        ];

		ColumnValidator.prototype = {
            getColName : function() {
                return this.json.colName;
            },

            isEmpty : function() {
                return this.getValues().length == 0;
            },

            deepCopy : function() {
                return new ColumnValidator(this.toJson());
            },

			/**
			 * Create json with only used fields present
             * will return null if there are any errors
             *
             * All fields are strings
			 */
			toJson : function () {
                if (this.getAllErrorsHTML() != null) {
                    return null;
                }

                // change arith numbers to numbers instead of strings
                if (this.json.type == "int" || this.json.type == "float") {
                    for (var config of ColumnValidator.CONFIG) {
                        if (config.type == "arith" && this.json[config.key] != undefined) {
                            this.json[config.key] = Number.parseFloat(this.json[config.key])
                        }
                    }
                }

                var ret = { "colName": this.json.colName };

                // set only those which are defined
                for (var c of ColumnValidator.CONFIG) {
                    if (c.type == "regex") {
                        if (this.json[c.key] != undefined && this.json[c.key].length > 0) {
                            ret[c.key] = this.json[c.key];
                        }
                    } else {
                        if (this.json[c.key] != undefined) {
                            ret[c.key] = this.json[c.key];
                        }
                    }
                }

                return ret;
			},

            // set a value.  if val is missing then use a default
            // switch val to the correct type
            setValidator : function(keyOrAlias, optVal) {
                var key = this.getKey(keyOrAlias);
                var val = typeof optVal == "undefined" ? this.getDefaultValue(key) : optVal;

                this.json[key] = this.getTypedVal(key, val);
            },

            clearValidator : function(keyOrAlias) {
                var key = this.getKey(keyOrAlias);
                this.json[key] = undefined;
            },

            // translate a val to the correct type
            getTypedVal : function(key, val) {
                var config = this.getConfig(key);

                if (config.type == "enum") {
                    var t = typeof config.enum[0];
                    if (t == "boolean") {
                        return Boolean(val);
                    } else {
                        return String(val);
                    }
                } else if (config.type == "regex") {
                    return String(val);
                } else if (config.type == "arith") {
                    if (this.json.type == "int") {
                        return parseInt(val);
                    } else if (this.json.type == "float") {
                        return parseFloat(val);
                    } else {
                        return String(val);
                    }
                }
            },

            isOpEnum : function(keyOrAlias) {
                var key = this.getKey(keyOrAlias);
                var config = this.getConfig(key);
                return config.type == "enum";
            },

            getEnumLegalValues : function(keyOrAlias) {
                var key = this.getKey(keyOrAlias);
                var config = this.getConfig(key);
                ret = [];
                for (var e of config.enum) {
                    ret.push(String(e));
                }
                return ret;
            },

            // get error message or null
            errorCheckValueStr : function(keyOrAlias, valStr) {
                var key = this.getKey(keyOrAlias);
                var config = this.getConfig(key);
                var errorTxt = null;
                if (config.type == "enum" && config.enum.indexOf(this.getTypedVal(key, valStr)) == -1) {
                    errorTxt = "must be one of: " + String(config.enum);
                } else if (config.type == "arith") {
                    if (this.json.type == undefined) {
                        errorTxt = "can not be used without validating the type.";
                    } else if (this.json.type == "int") {
                        errorTxt = this.getIntError(valStr);
                    } else if (this.json.type == "float") {
                        errorTxt = this.getFloatError(valStr);
                    } else if (this.json.type == "date") {
                        errorTxt = this.getDateError(valStr);
                    } else if (this.json.type == "time") {
                        errorTxt = this.getTimeError(valStr);
                    } else if (this.json.type == "datetime") {
                        errorTxt = this.getDatetimeError(valStr);
                    }
                }
                if (errorTxt != null) {
                    errorTxt = config.alias + ": " + errorTxt;
                }
                return errorTxt;
            },

            // get an html list of all errors, or null
            getAllErrorsHTML : function() {
                var html = "<h3>Fix the following errors:</h3><list>";
                for (var config of ColumnValidator.CONFIG) {
                    if (this.json[config.key] != undefined) {
                        var err = this.errorCheckValueStr(config.key, String(this.json[config.key]));
                        if (err != null) {
                            html += "<li>" + err + "</li>";
                        }
                    }
                }
                html += "</list>";
                if (html.search("<li>") == -1) {
                    return null;
                } else {
                    return html;
                }
            },

            // loop through to find config
            getConfig : function(key) {
                for (var config of ColumnValidator.CONFIG) {
                    if (config.key == key) {
                        return config;
                    }
                }
                return null;
            },

            // loop through to find config
            getKey : function(keyOrAlias) {
                for (var config of ColumnValidator.CONFIG) {
                    if (config.key == keyOrAlias || config.alias == keyOrAlias) {
                        return config.key;
                    }
                }
                return null;
            },

            // for every SET validation, get [{ name alias valueStr}]
            getValues : function() {
                var ret = [];

                for (var config of ColumnValidator.CONFIG) {
                    if (this.json[config.key] != undefined) {
                        ret.push({"name": config.key, "alias": config.alias, "value": this.json[config.key]});
                    }
                }
                return ret;
            },

            // get list of operations [{ name:x, alias:x, avail:x, defaultVal: x}]
            // avail-can it be added without error
            getValidOperations : function() {
                var ret = [];

                for (var config of ColumnValidator.CONFIG) {
                    var avail = false;
                    if (this.json[config.key] == undefined) {
                        if (config.type == "arith" && this.json.type != undefined) {
                            avail = true;
                        } else if (config.type != "arith") {
                            avail = true;
                        }
                    }

                    //https://www.w3schools.com/js/tryit.asp?filename=tryjs_date_string_iso1
                    // var d=Date()   dateTime
                    //         time would be "2000-12-25T" + validate
                    //         date is a datetime where valid.indexOf(":") == -1

                    ret.push({"name": config.key, "alias": config.alias, "avail": avail, "defaultVal": this.getDefaultValue(config.key)});

                }
                return ret;
            },

            // get {key: k, alias: a}
            getFirstUnusedOperation: function() {
                for (var config of ColumnValidator.CONFIG) {
                    var avail = false;
                    if (this.json[config.key] == undefined) {
                        if (config.type == "arith" && this.json.type != undefined) {
                            avail = true;
                        } else if (config.type != "arith") {
                            avail = true;
                        }
                    }

                    if (avail) {
                        return {key: config.key, alias: config.alias};
                    }

                }
                return null;
            },

            getDefaultValue : function (key) {
                var defaultVal = "";
                var config = this.getConfig(key);
                if (config.type == "arith") {
                    if (this.json.type == "int") {
                        defaultVal = 0;
                    } else if (this.json.type == "float") {
                        defaultVal = 0.0;
                    } else if (this.json.type == "date") {
                        defaultVal = "2020-12-25";
                    } else if (this.json.type == "datetime") {
                        defaultVal = "2020-12-25T14:56:30";
                    } else if (this.json.type == "time") {
                        defaultVal = "14:56:30";
                    }
                } else if (config.type == "enum") {
                    defaultVal = config.enum[0];
                } else if (config.type == "regex") {
                    defaultVal = "[0-9]+";
                }
                return defaultVal;
            },

            getIntError : function(str) {
                return this.validInt(str) ? null : "Invalid integer";
            },
            validInt : function(str) {
                return this.validFloat(str) && Number.isInteger(Number.parseFloat(str));
            },
            getFloatError : function(str) {
                return this.validFloat(str) ? null : "Invalid floating point number";
            },
            validFloat : function(str) {
                // parseFloat() seems to allow trailing garbage, so
                // check all chars are legal and last char is legal too
                return str.search("[^-+\.0-9eE]") == -1 && str.search("[-+eE]$") == -1 && ! isNaN(Number.parseFloat(str))
            },
            getDatetimeError : function(str) {
                return this.validDatetime(str) ? null : "Invalid datetime. e.g. 2020-12-25T14:56:30";
            },
            validDatetime : function (str) {
                var d = str + "+00:00";   // add timezone, making it invalid if there was already one
                return d.length() > 5 && String(new Date(d)).indexOf("nvalid") == -1;
            },
            getTimeError : function(str) {
                return this.validTime(str) ? null : "Invalid time.  e.g. 14:56:30";
            },
            validTime : function (str) {
                var d = "2000-12-25T" + str + "+00:00";   // add date and timezone, making it invalid if they were already there
                return d.indexOf(":") > -1 && String(new Date(d)).indexOf("nvalid") == -1;
            },

            getDateError : function(str) {
                return this.validDate(str) ? null : "Invalid date.  e.g. 2000-12-25";
            },
            validDate : function (str) {
                return str.length() > 5 && this.validDatetime(str) && str.indexOf(":") == -1;
            },

		};

		return ColumnValidator;            // return the constructor
	}
);
