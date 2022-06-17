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
    'sparqlgraph/js/msiclientnodegroupstore',
    'sparqlgraph/js/modaliidx',
    'sparqlgraph/js/iidxhelper',
    'sparqlgraph/js/selecttable',
    'sparqlgraph/js/sparqlgraphjson',

    // shimmed
    'jquery'
    ],

	function(MsiClientNodeGroupStore, ModalIidx, IIDXHelper, SelectTable, SparqlGraphJson, jquery) {

		/**
		 *
		 */
		var ModalStoreDialog= function (user, serviceUrl, optItemType) {
            this.div = null;
            this.tableDiv = null;
            this.selTable = null;
            this.user = user;
            this.id = "";
            this.serviceUrl = serviceUrl;
            this.itemType = optItemType || MsiClientNodeGroupStore.TYPE_NODEGROUP;
            this.userRetrieveJsonStrCallback = function () {};
		};


		ModalStoreDialog.prototype = {

            // user is sent in at creation or entered during storage of a nodegroup
            // can be "" but never null or undefined.
            getUser : function () {
                return this.user;
            },

            // get the nodegroup id
            getId : function() {
                return this.id;
            },

            show : function () {
                IIDXHelper.showDiv(this.div);
            },

            hide : function () {
                IIDXHelper.hideDiv(this.div);
            },

			loadButtonCallback : function() {
				var idList = this.selTable.getSelectedValues("ID");
				var mq = new MsiClientNodeGroupStore(this.serviceUrl);
                mq.getStoredItemByIdToStr(idList[0], this.itemType, this.userRetrieveJsonStrCallback);
			},

			cancelButtonCallback : function() {

            },
            
            renameStoredItemCallback : function(newId, resultSet) {
				if (! resultSet.isSuccess()) {
					ModalIidx.alert("Service failed", resultSet.getGeneralResultHtml());
				} else {
					this.refreshStoredItems([newId]);
					this.storeChangedCallback();
				}
			},
			
            // have new name for an item
            renameGotNewIdCallback : function(id, newId) {
				if (newId == id) { return; }
				
				var mq = new MsiClientNodeGroupStore(this.serviceUrl);
                mq.renameStoredItem(id, newId, this.itemType, this.renameStoredItemCallback.bind(this, newId));
			},
			
            // user hit the "rename" button
            renameButtonCallback : function() {
				// rowClickCallback makes sure there is exactly 1 selected before we get here
				var id = this.selTable.getSelectedValues("ID")[0];
				
				ModalIidx.input("Rename", "Id", id, "input-large", this.renameGotNewIdCallback.bind(this, id));
            },

            deleteButtonCallback : function() {
				// other URI controls (button disable, etc) assure list is length 1
                var idList = this.selTable.getSelectedValues("ID");
                
                this.deleteNodeGroupList(idList);
           
            },

			/**
				Update UI buttons after a row has been clicked
			 */
            rowClickCallback : function(tr_UNUSED) {
                var selIndices = this.selTable.getSelectedIndices();
                var renameBut = document.getElementById("msdRenameBut");
                var cancelBut = document.getElementById("msdCancelBut");
                var deleteBut = document.getElementById("msdDeleteBut");
                var loadBut = document.getElementById("msdLoadBut");

                cancelBut.removeAttribute("disabled");

				if (selIndices.length == 0) {
					renameBut.setAttribute("disabled", true);
                    deleteBut.setAttribute("disabled", true);
                    loadBut.setAttribute("disabled", true);
				} else if (selIndices.length == 1) {
                    renameBut.removeAttribute("disabled");
                    deleteBut.removeAttribute("disabled");
                    loadBut.removeAttribute("disabled");
				} else {
                    renameBut.setAttribute("disabled", true);
                    deleteBut.removeAttribute("disabled");
                    loadBut.setAttribute("disabled", true);
                }
            },

			//
			// Update this.selTable with data
			// returns:  success - boolean
			//
			gotMetadataRefresh : function(selectIdList, resultSet) {
				if (! resultSet.isSuccess()) {
					ModalIidx.alert("Service failed", resultSet.getGeneralResultHtml());
					return false;
				} else {

                    // build this.div with a SelectTable
                    var sortFlag = true;
                    var colList = ["creator", "ID", "creationDate", "comments"];
                    var undefVal = "";

                    var widthList = [15, 30, 15, 40];
                    var scrollSave = this.selTable ? this.selTable.getScrollY() : 0;
                    if (this.selTable) {
						this.selTable.replaceRows(resultSet.tableGetNamedRows(colList, undefVal, sortFlag));
					} else {
	                    this.selTable = new SelectTable(resultSet.tableGetNamedRows(colList, undefVal, sortFlag),
	                                                    colList,
	                                                    widthList,
	                                                    10,
	                                                    true,  // multiFlag
	                                                    undefined,
	                                                    this.rowClickCallback.bind(this)
	                                                    );
	                }
                    this.tableDiv.innerHTML = "";  
                    this.tableDiv.appendChild(this.selTable.getTableDom());
                    this.selTable.selectValues("ID", selectIdList);
                    this.selTable.setScrollY(scrollSave);
                    return true;
                }
			}, 
			
            /**
              * got nodegroup store contents.  Launch dialog.
             **/
            gotMetadataLaunchDialog : function (resultSet) {
				this.div = document.createElement("div");
				this.tableDiv = document.createElement("div");
				this.div.appendChild(this.tableDiv);
				
				if (this.gotMetadataRefresh([], resultSet)) {
                    
                    // button bar
                    var buttonDiv = document.createElement("div");
                    buttonDiv.align="right";
                    this.div.appendChild(document.createElement("hr"));
                    this.div.appendChild(buttonDiv);
                    buttonDiv.appendChild(IIDXHelper.createButton("Rename", this.renameButtonCallback.bind(this), [], "msdRenameBut", undefined));
                    buttonDiv.appendChild(IIDXHelper.createNbspText());
                    buttonDiv.appendChild(IIDXHelper.createButton("Delete", this.deleteButtonCallback.bind(this), [], "msdDeleteBut", undefined));


                    // launch the modal
                    var m = new ModalIidx();
                    m.showChoices(  this.title,
                                    this.div,
                                    ["Close", "Load"],
                                    [this.cancelButtonCallback.bind(this), this.loadButtonCallback.bind(this)],
                                    90,
                                    ["","btn-primary"],
                                    ["msdCancelBut", "msdLoadBut"]
                                );


                    this.rowClickCallback("");
				}
			},

            
            /**
              * load the id
              */
            retrieveJsonStrToCaller : function (userRetrieveJsonStrCallback, idList) {
               
            },


            /**
              * Callback after deletion
              * @param id is nodegroup just deleted
              * @param idList are additional nodegroups to delete
              */
            deleteNodeGroupListCallback : function (id, idList, resultSet) {

                // if there is a failure...
                // PEC TODO: search for "0 triples" is a temporary hack until we have a new results set type
				if (! resultSet.isSuccess() || resultSet.getSimpleResultsHtml().indexOf("0 triples") > -1) {

                    // print results using SimpleResults in case it got that far.  It will default to GeneralResults.
                    var msg = "<b>Failure deleting: " + id + "</b>";
                    msg += "<p>" + resultSet.getSimpleResultsHtml();

                    // stop processing and print list of unprocessed ids
                    if (idList.length > 0) {
                        msg += "<p><p><b>Deletes were not attempted on remaining ids:<br></b>" + idList;
                    }
					ModalIidx.alert("Service failed", msg);
					this.refreshStoredItems([]);
					this.storeChangedCallback();

				} else {
                    // if there's more to do
                    if (idList.length > 0) {
						// recurse down the list
                        this.deleteNodeGroupList(idList);
                    } else {
                        //ModalIidx.alert("Delete Nodegroup", resultSet.getSimpleResultsHtml());
                        this.refreshStoredItems([]);
                        this.storeChangedCallback();
                    }
				}
			},
            
            
            deleteNodeGroupList : function (idList) {
                var mq = new MsiClientNodeGroupStore(this.serviceUrl);

                // delete idList[0] and queue up idList[1..end]
                mq.deleteStoredItem(idList[0], this.itemType, this.deleteNodeGroupListCallback.bind(this, idList[0], idList.slice(1)));
            },
               

            /**
              * External call to retrieve a nodegroup from the store
              *
              * userRetrieveJsonStrCallback(jsonStr, id) - called when closing after user loaded a nodegroup
              * storeChangedCallback() - called any time a nodegroup is deleted or renamed
              */
            launchOpenStoreDialog : function (userRetrieveJsonStrCallback, storeChangedCallback) {
                     
                this.title = "Nodegroup store";
                this.userRetrieveJsonStrCallback = userRetrieveJsonStrCallback;
                this.storeChangedCallback = storeChangedCallback;

                var mq = new MsiClientNodeGroupStore(this.serviceUrl);
                
                // clean out left-over selections from previous launch
                if (this.selTable) {
                	this.selTable.deselectAll();
				}
    		    mq.getStoredItemsMetadata(this.itemType, this.gotMetadataLaunchDialog.bind(this));
  
            },

			/**
			 *  re-query and re-draw the select table
			 */
			refreshStoredItems : function(selIdList) {
				var mq = new MsiClientNodeGroupStore(this.serviceUrl);
    		    mq.getStoredItemsMetadata(this.itemType, this.gotMetadataRefresh.bind(this, selIdList));
			},
			
			/* ************************************************************************************************** 
              Totally different dialog but functionally related
              so I'm slamming it into this file
             ************************************************************************************************** */

			/**
			 *    Store a nodegroup
			 *    
			 *    itemStr - nodegroup json string
			 *    suggestedId - prefill the "id" field
			 *    doneCallback - no params
			 */
            launchStoreDialog : function (itemStr, suggestedId, doneCallback) {
                // get all existing meta data, and call launchStoreDialog2
                var mq = new MsiClientNodeGroupStore(this.serviceUrl);
    		    mq.getStoredItemsMetadata(this.itemType, this.launchStoreDialog2.bind(this, itemStr, suggestedId, doneCallback));
            },

            launchStoreDialog2 : function (itemStr, suggestedId, doneCallback, resultSet) {
                // check that meta data returned
                if (! resultSet.isSuccess()) {
                    ModalIidx.alert("Failure retrieving store contents",
                                    resultSet.getSimpleResultsHtml()
                                   );
                    return;

				}

                // storing of dialog succeeded
                var successCallback = function (id, closeModal, resultSet) {
                    if (! resultSet.isSuccess()) {
                        ModalIidx.alert("Service failed", resultSet.getGeneralResultHtml());
                    } else {
                        // store returns nothing
                        //ModalIidx.alert("Store Nodegroup", resultSet.getSimpleResultsHtml());
                        ModalIidx.alert("Store Nodegroup", "Successfully stored " + id);
                        closeModal()
                        doneCallback();
                    }
                };

                // user hit "Clear"
                var clearCallback = function () {
                    document.getElementById("sngIdText").value="";
                    document.getElementById("sngCommentsText").value="";
                    document.getElementById("sngCreatorText").value=this.user;

                };

                // make the call to store a nodegoup
                var submitCallbackStore = function (item, closeModal) {
                    var id = document.getElementById("sngIdText").value;
                    var comments = document.getElementById("sngCommentsText").value;
                    var creator = document.getElementById("sngCreatorText").value;

                    var mq = new MsiClientNodeGroupStore(this.serviceUrl);

                    // save id/user/creator
                    this.id = id;
                    this.user = creator;
                    mq.storeItem(item, creator, id, comments, this.itemType, successCallback.bind(this, id, closeModal));
                }.bind(this);

                // delete nodegroup and submit if successful
                var submitCallbackDeleteAndStore = function (id, item, closeModal) {
                    var mq = new MsiClientNodeGroupStore(this.serviceUrl);
                    mq.deleteStoredItem(id, this.itemType, submitCallbackStore.bind(this, item, closeModal));
                };

                // user hit "Submit"
                // check everything, then start callback chain
                var valSubmitCallback = function (rs, item, closeModal) {
                    var id = document.getElementById("sngIdText").value;
                    var comments = document.getElementById("sngCommentsText").value;
                    var creator = document.getElementById("sngCreatorText").value;

                    if (id == null || id.length == 0) {
                        ModalIidx.alert("Validation error", "ID cannot be empty.");
                    } else if (creator == null || creator.length == 0) {
                        ModalIidx.alert("Validation error", "Creator cannot be empty.");
                    } else if (comments == null || comments.length == 0) {
                        ModalIidx.alert("Validation error", "Comments cannot be empty.");
                    } else if (rs.getStringResultsColumn("ID").indexOf(id) > -1) {
                        // delete and store if user agrees
                        ModalIidx.okCancel("Nodegroup already exists",
                                           "Do you want to overwrite " + id + " in the store?",
                                            submitCallbackDeleteAndStore.bind(this, id, item, closeModal));
                    } else {
                        // simple store
                        submitCallbackStore(item, closeModal);
                    }
                }.bind(this, resultSet, itemStr);

                // update comment based on id
                var updateMetaData = function (rs) {
                    var creatorTxt = document.getElementById("sngCreatorText");
                    var idTxt = document.getElementById("sngIdText");
                    var commentTxt = document.getElementById("sngCommentsText");
                    var ngCreator = "";

                    // search for ID
                    var idCol = rs.getColumnNumber("ID");
                    for (var i=0; i < rs.getRowCount(); i++) {
                        if (rs.getRsData(i, idCol) == idTxt.value) {
                            // update comments on screen
                            commentTxt.value = rs.getRsData(i, rs.getColumnNumber("comments"));
                            // save copy of creator
                            ngCreator = rs.getRsData(i, rs.getColumnNumber("creator"));
                            break;
                        }
                    }

                    // leave comments unchanged when id changes

                    // handle creator
                    var creatorGroup = document.getElementById("sngCreatorGroup");
                    if (ngCreator != "" && ngCreator != this.user) {
                       IIDXHelper.changeControlGroupHelpText(creatorGroup,
                                                             "last saved by " + ngCreator,
                                                             "warning");
                    } else {
                       IIDXHelper.changeControlGroupHelpText(creatorGroup, "", "");
                    }
                }.bind(this, resultSet);

                // build form
                var div = document.createElement("div");
                var form = IIDXHelper.buildHorizontalForm();
                div.appendChild(form);
                var fieldset = IIDXHelper.addFieldset(form)

                // build idListElem and idText
                resultSet.sort("ID");
                var idListElem = IIDXHelper.createDataList("storeIDs", resultSet.getStringResultsColumn("ID"));
                div.appendChild(idListElem);
                var idText =      IIDXHelper.createTextInput("sngIdText", "input-xlarge", idListElem);
                idText.onchange = updateMetaData;
                var cleanSuggestion = (suggestedId || "").substring(0,128);
                idText.value = cleanSuggestion;
                

                // build the simple inputs
                var commentText = IIDXHelper.createTextArea("sngCommentsText", 2);
                var creatorText = IIDXHelper.createTextInput("sngCreatorText");

                // arrange the form
                fieldset.appendChild(IIDXHelper.buildControlGroup("id: ", idText));
                fieldset.appendChild(IIDXHelper.buildControlGroup("comments: ", commentText));

                var creatorGroup = IIDXHelper.buildControlGroup("my id: ", creatorText);
                creatorGroup.id = "sngCreatorGroup";
                fieldset.appendChild(document.createElement("hr"));
                fieldset.appendChild(creatorGroup);

                // launch
                var m = new ModalIidx("storeNodeGroupDialog");
                m.showClearCancelValSubmit("Save nodegroup to store",
                                        div,
                                        clearCallback,
                                        valSubmitCallback
                                        );
                updateMetaData();

                document.getElementById("sngCreatorText").value=this.user;

            },

		};

		return ModalStoreDialog;
	}
);
