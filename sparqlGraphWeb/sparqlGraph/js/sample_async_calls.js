
// This sample code will run with an html file like the one shown below.
// A known version of jquery is loaded from semtk.research.ge.com
// Then this file is loaded from the local folder
//
// <head>
//    <script type="text/javascript" src="http://semtk.research.ge.com/sparqlGraph/jquery/jquery.js"></script>
//	  <script type="text/javascript" src="./sample_async_calls.js"></script>
// </head>
// <body>
// </body>


/// -------- top-level code for running an async SemTK query --------

var NGE_URL="http://vesuvius-test:12058/nodeGroupExecution";


$('document').ready(function(){

    var sampleFailureCallback = function(xhr) {
        alert("FAILURE  xhr: " + JSON.stringify(xhr));
    };

    var sampleSuccessCallback = function(tableJSON) {
        alert("SUCCESS " + JSON.stringify(tableJSON));
    };

    var samplePercentCallback = function(percent) {
        console.log("PERCENT " + String(percent));
    };

    var payload={
      "nodeGroupId": "demoNodegroup",
      "sparqlConnection": "NODEGROUP_DEFAULT"
    };

    postToEndpoint(
        NGE_URL + "/dispatchById",
        JSON.stringify(payload),
        getAsyncSemtkCallback(sampleSuccessCallback, sampleFailureCallback, samplePercentCallback),
        sampleFailureCallback);
});

/// -------- Sample Jquery/ajax code for hitting a REST getEndpoint --------

var postToEndpoint = function (url, data, successCallback, failureCallback) {
    //
    // successCallback(resultSetJSON)
    // failureCallback(xhr, status, err)
    //

    console.log("calling " + url + " with " + data);
    $.ajax({
        url: url,
        type: 'POST',
        dataType: 'json',
        contentType: 'application/json',
        data: data,
        timeout: 30000,
        processData: false,

        success: function(j) { console.log("returned: " + JSON.stringify(j)); successCallback(j); },
        error:   function(j) { console.log("returned: " + JSON.stringify(j)); failureCallback(j); },
        statusCode: {
          // Moved this into userFailureCallback to avoid double callbacks
          // 401: function() {
          //    this.userFailureCallback("Authorization failure.<br>(Token may have expired.)");
          // }.bind(this),
           403: function() {
               // never happens
               this.statusCodeCallback(403);
               alert("403 was caught.  Miracle.");
           }.bind(this),
        }
    });

};

/// -------- Re-usable callback chain code --------

// getAsyncSemtkCallback - builds a callback chain
//
// PARAMS:
//   successCallback - takes a JSON table
//   failureCallback - takes an error message string
//   percentCallback - takes an integer
var getAsyncSemtkCallback = function(successCallback, failureCallback, percentCallback) {
    return gotJobIdCallback.bind(this, successCallback, failureCallback, percentCallback);
};

var getResultsTableCallback = function(success, failure, tableResult) {
    if (tableResult.hasOwnProperty("status") && tableResult.status=="success") {
        success("job succeeded: " + JSON.stringify(tableResult.table["@table"]));
    } else {
        failure("/getResultsTable failed: " + JSON.stringify(tableResult));
    }
};

var jobSucceeded = function(success, failure, jobId) {
    var data = JSON.stringify({
        "jobId": jobId,
    })
    postToEndpoint(NGE_URL + "/getResultsTable", data, getResultsTableCallback.bind(this, success, failure), failure);
};

var statusMessageCallback = function(failure, statusMessageResult) {
    if (statusMessageResult.hasOwnProperty("status") && statusMessageResult.status=="success" && statusMessageResult.hasOwnProperty("simpleresults") && statusMessageResult.simpleresults.hasOwnProperty("message")) {
        failure("job failed with message: " + statusMessageResult.simpleresults.message);
    } else {
        failure("/jobStatusMessage failed: " + JSON.stringify(statusMessageResult));
    }
};

var jobFailed = function(success, failure, percent, jobId) {
    var data = JSON.stringify({
        "jobId": jobId,
    })
    postToEndpoint(NGE_URL + "/jobStatusMessage", data, statusMessageCallback.bind(this, failure), failure);
};

var statusCallback = function(success, failure, jobId, statusResults) {
    if (statusResults.hasOwnProperty("status") && statusResults.status=="success" && statusResults.hasOwnProperty("simpleresults") && statusResults.simpleresults.hasOwnProperty("status")) {
        if (statusResults.simpleresults.status == "Success") {
            jobSucceeded(success, failure, jobId);
        } else {
            jobFailed(success, failure, jobId);
        }
    } else {
        failure("/jobStatus failed: " + JSON.stringify(statusResults));
    }
};

var jobComplete = function(success, failure, jobId) {
    var data = JSON.stringify({
        "jobId": jobId,
    })
    postToEndpoint(NGE_URL + "/jobStatus", data, statusCallback.bind(this, success, failure, jobId), failure);
};

var waitCallback = function(success, failure, percent, jobId, waitResults) {
    if (waitResults.hasOwnProperty("status") && waitResults.status=="success" && waitResults.hasOwnProperty("simpleresults") && waitResults.simpleresults.hasOwnProperty("percentComplete")) {
        var p = parseInt(waitResults.simpleresults.percentComplete)
        percent(p);
        if (p < 100) {
            waitUntilComplete(success, failure, percent, jobId);
        } else {
            jobComplete(success, failure, jobId);
        }
    } else {
        failure("/waitForPercentOrMsec failed: " + JSON.stringify(waitResults));
    }
};

var waitUntilComplete = function(success, failure, percent, jobId) {
    var data = JSON.stringify({
        "jobId": jobId,
        "maxWaitMsec":200,    // short for demo purposes.  Make this 1-20 seconds (1000-20000)
        "percentComplete":100
    })
    postToEndpoint(NGE_URL + "/waitForPercentOrMsec", data, waitCallback.bind(this,success, failure, percent, jobId), failure);
};

var gotJobIdCallback = function(success, failure, percent, jobIdResults) {
    if (jobIdResults.hasOwnProperty("status") && jobIdResults.status=="success" && jobIdResults.hasOwnProperty("simpleresults") && jobIdResults.simpleresults.hasOwnProperty("JobId")) {
        waitUntilComplete(success, failure, percent, jobIdResults.simpleresults.JobId)
    } else {
        failure("/dispatchById failed: " + JSON.stringify(jobIdResults));
    }
};
