define([	// properly require.config'ed   bootstrap-modal
        	'sparqlgraph/js/semtk_api', 
        	'sparqlgraph/js/importspec',
        	'sparqlgraph/js/importcolumn',
        	'sparqlgraph/js/importtext',
        	'sparqlgraph/js/importtrans',
        	'sparqlgraph/js/importmapping',
        	'sparqlgraph/js/mappingitem',

			// shimmed

		],

	function(SemtkAPI, ImportSpec, ImportColumn, ImportText, ImportTransform, ImportMapping, MappingItem) {
	
		/**
		 * This is part of the SemtkAPI, separated for clarity.
		 * These methods support editing the import specification.
		 * @description <font color="red">Users of {@link SemtkAPI} should not call this constructor.</font><br>Use {@link SemtkAPI#getSemtkImportAPI} instead 
		 * @alias SemtkImportAPI
		 * @class
		 * @constructor
		 * @param semtkAPI
		 */
		var SemtkImportAPI = function(semtkApi) {
			//   Programmer notes:  this object is just an API extension
			//   All state should be stored under the mappingTab.
			//   This object is not guaranteed to stay in existence.
			//
			//   This api provides an interface to ImportSpec, and exposes some of its member objects
			
			// Some pointers for convenience
			this.semtk = semtkApi;                             // the semtk global
			this.mappingTab = semtkApi.mappingTab;             // this pointer isn't changable
			this.importSpec = semtkApi.mappingTab.importSpec   // but these contents are
		};
		
		SemtkImportAPI.prototype = {
			
			/**
			 * Create a column that can be shared by many ImportMappings.
			 * Use as a constructor
			 * @throws exception if column name already exists
			 * @param {string} name column name
			 * @returns {ImportColumn} which can be added to an MappingItem
			 */
			createImportColumn : function (name) {
				if (this.getImportColumn(name) != null) {
					throw "Column with this name already exists: " + name;
				}
				var iCol = new ImportColumn(name);
				this.importSpec.addColumn(iCol);
				return iCol;
			},
		
			/**
			 * Create text that can be shared by many ImportMappings.
			 * Use as a constructor
			 * @throws exception if text name already exists
			 * @param {string} text the text
			 * @returns {ImportText} which can be added to an MappingItem
			 */
			createImportText : function (text) {
				if (this.getImportText(text) != null) {
					throw "Text with this text already exists: " + text;
				}
				var iText = new ImportText(text);
				this.importSpec.addText(iText);
				return iText;
			},
			
			/**
			 * Create a transform that can be shared by many ImportMappings.
			 * Use as a constructor
			 * @throws exception if name already exists
			 * @param {string} name
			 * @param {string} transType for legal values, see ImportTransform.TRANSFORMS first item in each list
			 * @param {string} arg1 arg if indicated by row in ImportTransform.TRANSFORMS
			 * @param {string} arg2 arg if indicated by row in ImportTransform.TRANSFORMS
			 * @returns {ImportTransform} which can be added to an MappingItem
			 */
			createImportTransform : function (name, transType, arg1, arg2 ) {
				if (this.getImportTransform(name) != null) {
					throw "Transform with this name already exists: " + name;
				}
				var iTrans = new ImportTransform(name, transType, arg1, arg2);
				this.importSpec.addTransform(iTrans);
				return iTrans;
			},
			
			/**
			 * Create a new text import item for a mapping
			 * @param {ImportText} iText
			 * @returns {MappingItem} which can be added to a mapping
			 */
			createMappingItemText : function (iText) {
				return new MappingItem(MappingItem.TYPE_TEXT, iText, []);
			},
			
			/**
			 * Create a new column import item for a mapping
			 * @param {ImportColumn} iCol
			 * @param {ImportTransform[]} transformList optional
			 * @returns {MappingItem} which can be added to a mapping
			 */
			createMappingItemColumn : function (iCol, transformList) {
				var tList = (typeof transformList === "undefined") ? [] : transformList
						
				return new MappingItem(MappingItem.TYPE_COLUMN, iCol, tList);
			},
			
			/**
			 * Retrieve an ImportColumn by column name
			 * @param {string} colName column name
			 * @returns {ImportColumn} the column item or null
			 */
			getImportColumn : function (colName) {
				var iCols = this.importSpec.getSortedColumns();
				for (var i=0; i < iCols.length; i++) {
					if (iCols[i].getColName() === colName) {
						return iCols[i];
					}
				}
				return null;
			},
			
			/**
			 * Get all ImportColumns
			 * @returns {ImportColumn[]}
			 */
			getImportColumns : function () {
				return this.importSpec.getSortedColumns();
			},
			
			/**
			 * Get all ImportTexts
			 * @returns {ImportText[]}
			 */
			getImportTexts : function () {
				return this.importSpec.getSortedTexts();
			},
			
			/**
			 * Get all ImportTransforms
			 * @returns {ImportTransform[]}
			 */
			getImportTransforms : function () {
				return this.importSpec.getSortedTransforms();
			},
			
			/**
			 * Retrieve an ImportText by text
			 * @param {string} text text
			 * @returns {ImportText} the text item or null
			 */
			getImportText : function (text) {
				var iTexts = this.importSpec.getSortedTexts();
				for (var i=0; i < iTexts.length; i++) {
					if (iTexts[i].getText() === text) {
						return iTexts[i];
					}
				}
				return null;
			},
			
			/**
			 * Retrieve an ImportTransform by name
			 * @param {string} name transform name
			 * @returns {ImportTransform} the transform or null
			 */
			getImportTransform : function (name) {
				var iTrans = this.importSpec.getSortedTransforms();
				for (var i=0; i < iTrans.length; i++) {
					if (iTrans[i].getName() === name) {
						return iTrans[i];
					}
				}
				return null;
			},
			
			/**
			 * Get base URI
			 * @returns {string} uri prefix for imported data
			 */
			getBaseURI : function () {
				return this.importSpec.getBaseURI();
			},
			
			/**
			 * Get the ImportMapping a node SPARQL ID and property URI
			 * @param {string} nodeSparqlId Class node SPARQL ID
			 * @param {string} propertyUri  Property full URI or null
			 * @returns {InputMapping}
			 */
			getImportMapping : function (nodeSparqlId, propertyUri) {
				return this.importSpec.getMapping(nodeSparqlId, propertyUri);
			},
			
			/**
			 * Get an ImportMapping for each item that could be imported.
			 * @returns {ImportMapping[]} List of ImportMappings
			 */
			getImportMappings : function () {
				return this.importSpec.getSortedRows();
			},
			
			/** 
			 * Get a list of all node sparql id / property URI pairs for the nodegroup
			 * Each pair will have an ImportMapping
			 * Property URI is null for the class SPARQL IDs
			 * @returns { string[][] } list all valid pairs in the nodegroup [node sparqlId, paramURI]
			 */
			getClassPropPairs : function () {
				var mappings = this.getImportMappings();
				var ret = [];
				for (var i=0; i < mappings.length; i++) {
					ret.push( [mappings[i].getNodeSparqlId(),mappings[i].getPropUri() ] );
				}
				return ret;
			},
			
			/**
			 * Set base URI
			 * @param {string} uri 
			 * @returns none
			 */
			setBaseURI : function (uri) {
				this.importSpec.setBaseURI(uri);
			},
			
			/**
			 * Re-syncs the the import spec to the nodegroup.
			 * This should be done before saving or editing if nodegroup might have changed.
			 * @returns none
			 */
			syncToNodegroup : function () {
				this.importSpec.updateNodegroup(semtk.nodegroup);
			}
		};
		
		return SemtkImportAPI;            // return the constructor
	}
);