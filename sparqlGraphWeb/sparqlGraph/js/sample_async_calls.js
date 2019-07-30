
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

    // your job failure callback
    var sampleFailureCallback = function(title, x) {
        // x could be a
        //   table (ingestion failure),
        //   an xhr with "readyState", or
        //   semtk resultset
        alert(title + ": " + JSON.stringify(x));

    };

    // your job success callback
    var sampleSuccessCallback = function(x) {
        // two types of possible successes
        if (typeof x == "string") {
            alert("INGEST SUCCESS MESSAGE:" + x);
        } else {
            alert("SUCCESS TABLE JSON: " + JSON.stringify(x));
        }
    };

    // your status bar callback
    var samplePercentCallback = function(percent) {
        console.log("PERCENT " + String(percent));
    };

    // example of running a select
    var runSelect = function() {
        var payload={
          "nodeGroupId": "demoNodegroup",
          "sparqlConnection": "NODEGROUP_DEFAULT"
        };

        var ingestFlag = false;
        var asyncCallback = getAsyncSemtkCallback(sampleSuccessCallback, sampleFailureCallback, samplePercentCallback, ingestFlag);
        postToEndpoint(
            NGE_URL + "/dispatchById",
            JSON.stringify(payload),
            asyncCallback,
            sampleFailureCallback);
    }

    // example of running an ingest
    var runIngest = function() {
        var payload={
          "templateId": "demoNodegroup",
          "sparqlConnection": "NODEGROUP_DEFAULT",
          "csvContent" : "test_number,layer_code,meas_units,meas_tag,meas_name,value,timestamp\n4343,layer10,F,temp,temperature,200.8,2017-03-23T10:03:16"
        };

        var ingestFlag = true;
        var asyncCallback = getAsyncSemtkCallback(sampleSuccessCallback, sampleFailureCallback, samplePercentCallback, ingestFlag);

        postToEndpoint(
            NGE_URL + "/ingestFromCsvStringsByIdAsync",
            JSON.stringify(payload),
            asyncCallback,
            sampleFailureCallback);
    }

    // run the two jobs 7 seconds apart
    setTimeout(runSelect, 1);
    setTimeout(runIngest, 7000);


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
        error:   function(j) { console.log("returned: " + JSON.stringify(j)); failureCallback("HTTP error", j); },
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
var getAsyncSemtkCallback = function(successCallback, failureCallback, percentCallback, ingestFlag) {
    return gotJobIdCallback.bind(this, successCallback, failureCallback, percentCallback, ingestFlag);
};

var getResultsTableCallback = function(success, failure, ingestFlag, tableResult) {
    if (tableResult.hasOwnProperty("status") && tableResult.status=="success") {
        if (ingestFlag) {
            failure("ingest job failed", tableResult.table["@table"]);
        } else {
            success(tableResult.table["@table"]);
        }
    } else {
        failure("/getResultsTable failed", tableResult);
    }
};

var jobSucceeded = function(success, failure, ingestFlag, jobId) {
    var data = JSON.stringify({
        "jobId": jobId,
    })
    if (ingestFlag) {
        postToEndpoint(NGE_URL + "/jobStatusMessage", data, jobStatusMessageCallback.bind(this, success, failure, ingestFlag), failure);
    } else {
        postToEndpoint(NGE_URL + "/getResultsTable", data, getResultsTableCallback.bind(this, success, failure, ingestFlag), failure);
    }
};

var jobStatusMessageCallback = function(success, failure, ingestFlag, statusMessageResult) {
    if (statusMessageResult.hasOwnProperty("status") && statusMessageResult.status=="success" && statusMessageResult.hasOwnProperty("simpleresults") && statusMessageResult.simpleresults.hasOwnProperty("message")) {
        if (ingestFlag) {
            success(statusMessageResult.simpleresults.message);
        } else {
            failure("job failed" + statusMessageResult);
        }
    } else {
        failure("/jobStatusMessage failed", statusMessageResult);
    }
};

var jobFailed = function(success, failure, percent, ingestFlag, jobId) {
    var data = JSON.stringify({
        "jobId": jobId,
    });
    if (ingestFlag) {
        postToEndpoint(NGE_URL + "/getResultsTable", data, getResultsTableCallback.bind(this, success, failure, ingestFlag), failure);

    } else {
        postToEndpoint(NGE_URL + "/jobStatusMessage", data, jobStatusMessageCallback.bind(this, success, failure, ingestFlag), failure);
    }
};

var statusCallback = function(success, failure, ingestFlag, jobId, statusResults) {
    if (statusResults.hasOwnProperty("status") && statusResults.status=="success" && statusResults.hasOwnProperty("simpleresults") && statusResults.simpleresults.hasOwnProperty("status")) {
        if (statusResults.simpleresults.status == "Success") {
            jobSucceeded(success, failure, ingestFlag, jobId);
        } else {
            jobFailed(success, failure, ingestFlag, jobId);
        }
    } else {
        failure("/jobStatus failed", statusResults);
    }
};

var jobComplete = function(success, failure, ingestFlag, jobId) {
    var data = JSON.stringify({
        "jobId": jobId,
    })
    postToEndpoint(NGE_URL + "/jobStatus", data, statusCallback.bind(this, success, failure, ingestFlag, jobId), failure);
};

var waitCallback = function(success, failure, percent, ingestFlag, jobId, waitResults) {
    if (waitResults.hasOwnProperty("status") && waitResults.status=="success" && waitResults.hasOwnProperty("simpleresults") && waitResults.simpleresults.hasOwnProperty("percentComplete")) {
        var p = parseInt(waitResults.simpleresults.percentComplete)
        percent(p);
        if (p < 100) {
            waitUntilComplete(success, failure, percent, ingestFlag, jobId);
        } else {
            jobComplete(success, failure, ingestFlag, jobId);
        }
    } else {
        failure("/waitForPercentOrMsec failed", waitResults);
    }
};

var waitUntilComplete = function(success, failure, percent, ingestFlag, jobId) {
    var data = JSON.stringify({
        "jobId": jobId,
        "maxWaitMsec":200,    // short for demo purposes.  Make this 1-20 seconds (1000-20000)
        "percentComplete":100
    })
    postToEndpoint(NGE_URL + "/waitForPercentOrMsec", data, waitCallback.bind(this,success, failure, percent, ingestFlag, jobId), failure);
};

var gotJobIdCallback = function(success, failure, percent, ingestFlag, jobIdResults) {
    if (jobIdResults.hasOwnProperty("status") && jobIdResults.status=="success" && jobIdResults.hasOwnProperty("simpleresults") && jobIdResults.simpleresults.hasOwnProperty("JobId")) {
        waitUntilComplete(success, failure, percent, ingestFlag, jobIdResults.simpleresults.JobId)
    } else {
        failure("/dispatchById failed", jobIdResults);
    }
};
