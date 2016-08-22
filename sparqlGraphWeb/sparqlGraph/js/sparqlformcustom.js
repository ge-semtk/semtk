// This file is not a typical javascript object, but a script
// that needs to be loaded by the HTML.
// Its other half must also be loaded byt the HTML:  sparqlform.js


require([	'sparqlgraph/js/sparqlform',
         	'sparqlgraph/js/modaliidx',
         	'local/customconfig',
         	
         	'jquery',
		
			// rest are shimmed
         	'sparqlgraph/js/htmlformgroup',          // PEC TODO:  Make this require.js
         	
		],

	function(SparqlForm, ModalIidx, g, $) {

	
		// ****   Start on load proceedure ****
	
		onLoadCustom = function () {
			// check for firefox
			var is_firefox = navigator.userAgent.toLowerCase().indexOf('firefox') > -1;
			if (!is_firefox) {
				alertUser("Only FireFox is currently supported.");
				return;
			}
			
			kdlLogEvent("Custom: Page Load");
	
			// load customDiv.html
			$("#customDiv").load("customDiv.html")
			
			gHtmlFormGroup = new HtmlFormGroup(this.document, g.conn, g.fields, g.query, setStatus, alertUser, preQueryCallback, postQueryCallback, doneUpdatingCallback, undefined, false, true);
			setStatus("");
		};
		
		preQueryCallback = function() {
			// PEC TODO
			console.log("preQueryCallback");
		};
		
		postQueryCallback = function() {
			// PEC TODO
			console.log("postQueryCallback");
		};
		
		doneUpdatingCallback = function() {
			// PEC TODO
			console.log("doneUpdatingCallback");
		};

	}

);