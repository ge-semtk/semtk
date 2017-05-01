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
	
		/**
		 * An item to be added to a single ImportMapping
		 * API users should not call this constructor
		 * Use SemtkImportAPI.createTextMappingItem() or SemtkImportAPI.createColumnMappingItem() instead
		 * @description <font color="red">Users of {@link SemtkAPI} should not call this constructor.</font><br>Use {@link SemtkImportAPI#createMappingItemText} or {@link SemtkImportAPI#createMappingItemColumn} instead
		 * @alias MappingItem
		 * @class
		 */
		var MappingItem = function (itemType, iObj, transformList) {
			this.itemType = itemType;   
			this.iObj = iObj;        // column name or text iObj
			this.transformList = transformList;      
		};
		
		MappingItem.TYPE_TEXT = "text";
		MappingItem.TYPE_COLUMN = "column";
		
		MappingItem.prototype = {
			//
			// NOTE any methods without jsdoc comments is NOT meant to be used by API users.
			//      These methods' behaviors are not guaranteed to be stable in future releases.
			//
			
			/**
			 * Add an import transform
			 * @param {ImportTransform} trans the import transform to add
			 * @param {ImportTransform} insertBefore insert before this transform, append to end of list if null
			 */
			addTransform : function (trans, insertBefore) {
				if (insertBefore == null) {
					this.transformList.push(trans);
				} else {
					var pos = this.transformList.indexOf(insertBefore);
					this.transformList.splice(pos, 0, trans);
				}
			},
			
			/**
			 * Delete the given import transform<br>
			 * silently failing if it doesn't exist
			 * @param {ImportTransform} trans
			 */
			delTransform : function (trans) {
				var index = this.transformList.indexOf(trans);
				if (index > -1) {
					this.transformList.splice(index, 1);
				}
			},
			
			/**
			 * @description Get the mapping item type
			 * @returns {string} MappingItem.TYPE_TEXT or MappingItem.TYPE_COLUMN
			 */
			getType : function () {
				return this.itemType;
			},
			
			/**
			 * @description Get this item's ImportText
			 * @returns {ImportText}
			 * @throws exception if this is not a text item
			 */
			getTextObj : function () {
				if (this.itemType != MappingItem.TYPE_TEXT) {kdlLogAndThrow("Internal error in MappingItem.getTextObj().  Item is not a text item.");}
				return this.iObj;
			},
			
			/**
			 * @description Get this item's ImportColumn
			 * @returns {ImportColumn}
			 * @throws exception if this is not a column item
			 */
			getColumnObj : function () {
				if (this.itemType != MappingItem.TYPE_COLUMN) {kdlLogAndThrow("Internal error in MappingItem.getColumnObj().  Item is not a column item.");}
				return this.iObj;
			},
			
			getColumnOrTextObj : function () {
				return this.iObj;
			},
			
			/**
			 * @description Get this item's list of transforms
			 * @returns {ImportTransform[]}
			 */
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
					this.itemType = MappingItem.TYPE_TEXT;
					this.iObj = idHash[jObj.textId];
					this.iObj.incrUse(1);
					this.transformList = [];
					
				} else {
					this.itemType = MappingItem.TYPE_COLUMN;
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
				
				if (this.itemType == MappingItem.TYPE_TEXT) {
					
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
	
		return MappingItem;            // return the constructor
	}
);