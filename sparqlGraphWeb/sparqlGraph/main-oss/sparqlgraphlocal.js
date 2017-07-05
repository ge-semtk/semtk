// This file is not a typical javascript object, but a script
// that needs to be loaded by the HTML.
// Its other half must also be loaded byt the HTML:  sparqlform.js


	/**
	 * No status bar yet.  Just print the number.
	 * @param msgText
	 * @param percent
	 */

	var setQueryFlagCheckboxes = function () {
		document.getElementById("chkboxAvoidMicroSvc").checked = gQueryMicroserviceFlag == "direct";
	};
	

	
	
	

