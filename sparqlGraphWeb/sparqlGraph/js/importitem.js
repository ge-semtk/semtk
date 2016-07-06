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
    		// 'logconfig',
		],

	function() {
	
		/*
		 *    A column name or text or some item used to build a triple value
		 */
		var ImportItem = function (itemType, iObj, transformList) {
			this.itemType = itemType;   
			this.iObj = iObj;        // column name or text iObj
			this.transformList = transformList;      
		};
		
		ImportItem.TYPE_TEXT = "text";
		ImportItem.TYPE_COLUMN = "column";
		
		ImportItem.prototype = {
			addTransform : function (trans, insertBefore) {
				if (insertBefore == null) {
					this.transformList.push(trans);
				} else {
					var pos = this.transformList.indexOf(insertBefore);
					this.transformList.splice(pos, 0, trans);
				}
			},
			
			delTransform : function (trans) {
				var index = this.transformList.indexOf(trans);
				if (index > -1) {
					this.transformList.splice(index, 1);
				}
			},
			
			getType : function () {
				return this.itemType;
			},
			
			getTextObj : function () {
				if (this.itemType != ImportItem.TYPE_TEXT) {kdlLogAndThrow("Internal error in ImportItem.getTextObj().  Item is not a text item.");}
				return this.iObj;
			},
			
			getColumnObj : function () {
				if (this.itemType != ImportItem.TYPE_COLUMN) {kdlLogAndThrow("Internal error in ImportItem.getColumnObj().  Item is not a column item.");}
				return this.iObj;
			},
			
			getColumnOrTextObj : function () {
				return this.iObj;
			},
			
			getTransformList : function () {
				return this.transformList;
			},
			
			incrUse : function (n) {
				// increment use of everything in this item
				this.iObj.incrUse(n);
				for (var i=0; i < this.transformList.length; i++) {
					this.transformList[i].incrUse(n);
				}
			},
			
			fromJson : function (jObj, idHash) {
				// columns and texts and transforms are loaded just by id
				if (jObj.hasOwnProperty("text")) {
					this.itemType = ImportItem.TYPE_TEXT;
					this.iObj = idHash[jObj.textId];
					this.iObj.incrUse(1);
					this.transformList = [];
					
				} else {
					this.itemType = ImportItem.TYPE_COLUMN;
					this.iObj = idHash[jObj.colId];
					this.iObj.incrUse(1);
					this.transformList = [];
					if (jObj.hasOwnProperty("transformList")) {
						for (var i=0; i < jObj.transformList.length; i++) {
							var iTrans = idHash[jObj.transformList[i]];
							iTrans.incrUse(1);
							this.transformList.push(iTrans);
						}
					}
				}
			},
			
			toJson : function (idHash) {
				// use id hash to put ImportColumns, ImportText, ImportTransform
				// into the Json by ID only
				var ret = {};
				
				if (this.itemType == ImportItem.TYPE_TEXT) {
					
					ret.textId = this.helpGetKeyFromHash(idHash, this.iObj);
					ret.text = this.iObj.getText();
					
				} else {
					ret.colId = this.helpGetKeyFromHash(idHash, this.iObj);
					ret.colName = this.iObj.getColName();
					
					// add transform if it isn't empty
					if (this.transformList != null && this.transformList.length > 0) {
						ret.transformList = [];
						for (var i=0; i < this.transformList.length; i++) {
							// just push the transform key
							ret.transformList.push(this.helpGetKeyFromHash(idHash, this.transformList[i]));
						}
					};
				}
				return ret;
			},
			
			helpGetKeyFromHash : function(idHash, item) {
				// generic helper to find a key of an item in a hash
				for (var k in idHash) {
					if (idHash[k] === item) {
						return k;
					}
				}
				return null;
			},
		};
	
		return ImportItem;            // return the constructor
	}
);