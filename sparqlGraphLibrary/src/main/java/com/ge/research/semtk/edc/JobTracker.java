/**
 ** Copyright 2016 General Electric Company
 **
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


package com.ge.research.semtk.edc;

import java.net.URL;
import java.util.UUID;

import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;
import com.ge.research.semtk.sparqlX.SparqlToXUtils;

public class JobTracker {
	JobEndpointProperties prop = null;
	SparqlEndpointInterface endpoint = null;
	
	public JobTracker (JobEndpointProperties edc_prop) throws Exception {
		this.prop = edc_prop;
		this.endpoint = SparqlEndpointInterface.getInstance(	this.prop.getJobEndpointType(),
																this.prop.getJobEndpointServerUrl(), 
																this.prop.getJobEndpointDataset(),
																this.prop.getJobEndpointUsername(),
																this.prop.getJobEndpointPassword());
	}
	
	/**
	 * Return percent complete as a string, or throw exception
	 * @param jobId
	 * @return
	 * @throws Exception if jobId doesn't exist or job has no percentComplete
	 */
	public int getJobPercentComplete(String jobId) throws Exception {	    
		   
	    String query = String.format("  \n" +
	        "prefix job:<http://research.ge.com/semtk/services/job#>  \n" +
	    	"prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#>  \n" +
	    	"	  \n" +
	    	"	select distinct ?percentComplete where {  \n" +
	    	"	   ?Job a job:Job.  \n" +
	    	"	   ?Job job:id '%s'^^XMLSchema:string .  \n" +
	    	"	   ?Job job:percentComplete ?percentComplete .  \n" +
	    	"	}",
	    	SparqlToXUtils.safeSparqlString(jobId));

	    endpoint.executeQuery(query, SparqlResultTypes.TABLE);
	    
	    String trList[] = endpoint.getStringResultsColumn("percentComplete");
	    
	    if (trList.length > 1) {
	    	throw new Exception(String.format("Job %s has %d percentComplete entries.  Expecting 1.", jobId, trList.length));
	    } else if (trList.length == 0) {
	    	if (! jobExists(jobId) ) {
	    		throw new Exception(String.format("Can't find Job %s", jobId));
	    	} else {
	    		throw new Exception(String.format("Can't find percent complete for Job %s",  jobId));
	    	}
	    } else {
	    	int ret = Integer.parseInt(trList[0]);
	    	
	    	if (ret < 0 || ret > 100) {
	    		throw new Exception(String.format("Trouble parsing percent complete of job %s into an int 0-100.  Value = '%s'", jobId, trList[0]));
	    	}
		    return ret;
	    }
	}
	
	/**
	 * Set percentComplete for a job, 
	 *   creating the job if it doesn't exist.
	 * @param jobId
	 * @param percentComplete 0-99
	 * @throws Exception
	 */
	
	public void setJobPercentComplete(String jobId, int percentComplete) throws Exception {	    
		setJobPercentComplete(jobId, percentComplete, "");
	}
	
	public void setJobPercentComplete(String jobId, int percentComplete, String message) throws Exception {	    
	   
	    if (! jobExists(jobId)) {
	    	createJob(jobId);
	    }
	    
	    if (percentComplete < 0) { 
	    	throw new Exception (String.format("Can't set job %s percent complete to negative percent complete value: %d", jobId, percentComplete));
	    }
	    if (percentComplete > 99) { 
	    	throw new Exception (String.format("Can't set job %s percent complete to 100%% or above.  Set success or failure instead.", jobId));
	    }

	    String query = String.format("  \n" +
	        "prefix job:<http://research.ge.com/semtk/services/job#> \n" +
	        "prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> \n" +
	        " \n" +
	        "DELETE {\n" +
	        "   ?Job job:percentComplete ?percentComplete .\n" +
	        "   ?Job job:status ?status." +
	        "   ?Job job:statusMessage ?statusMessage." +
	        "} \n" +
	        "INSERT {\n" +
	        "   ?Job job:percentComplete '%d'^^XMLSchema:integer. \n" +
	        "   ?Job job:status job:InProgress. \n" +
	        "   ?Job job:statusMessage '%s'^^XMLSchema:string." +
	        "} \n" +
	        "where { \n" +
	        "   ?Job a job:Job. \n" +
	        "   ?Job job:id '%s'^^XMLSchema:string. \n" +
	        "   optional {?Job job:percentComplete ?percentComplete.} \n" +
	        "   optional {?Job job:status ?status.} \n" +
	        "   optional {?Job job:statusMessage ?statusMessage.}" +
	        "}",
	    	percentComplete, SparqlToXUtils.safeSparqlString(message), SparqlToXUtils.safeSparqlString(jobId));
	    System.err.println(query);
	    try {
	    	endpoint.executeQuery(query, SparqlResultTypes.CONFIRM);
	    } catch (Exception e) {
	    	throw new Exception(e.getMessage());
	    }
	}
	
	/**
	 * Set job to a status other than Success,
	 *    creating job if it doesn't exist
	 * @param jobId
	 * @param statusMessage
	 */
	public void setJobFailure(String jobId, String statusMessage) throws Exception {
		
		if (! jobExists(jobId)) {
	    	createJob(jobId);
	    }
	  
	    String query = String.format("  \n" +
	        "prefix job:<http://research.ge.com/semtk/services/job#> \n" +
	        "prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> \n" +
	        " \n" +
	        "DELETE {\n" +
			"   ?Job job:percentComplete ?percentComplete . \n" +
			"   ?Job job:statusMessage ?statusMessage . \n" +
	        "   ?Job job:status ?status." +
	        "} \n" +
	        "INSERT {\n" +
			"   ?Job job:percentComplete '100'^^XMLSchema:integer.  \n" +
	        "   ?Job job:statusMessage '%s'^^XMLSchema:string. \n" +
	        "   ?Job job:status job:Failure. \n" +
	        "} \n" +
	        "where { \n" +
	        "   ?Job a job:Job. \n" +
	        "   ?Job job:id '%s'^^XMLSchema:string . \n" +
			"   optional {?Job job:percentComplete ?percentComplete .} \n" +
	        "   optional {?Job job:statusMessage ?statusMessage.} \n" +
	        "   optional {?Job job:status ?status.} \n" +
	        "}",
	        
	        SparqlToXUtils.safeSparqlString(statusMessage), SparqlToXUtils.safeSparqlString(jobId));
	    System.err.println(query);
	    try {
	    	endpoint.executeQuery(query, SparqlResultTypes.CONFIRM);
	    } catch (Exception e) {
	    	throw new Exception(e.getMessage());
	    }
	}
	
	/**
	 * Get job status local fragment as a string
	 * @param jobId
	 * @return
	 * @throws Exception if jobId can't be found or has not status
	 */
	public String getJobStatus(String jobId) throws Exception {
		String query = String.format("  \n" +
				"prefix job:<http://research.ge.com/semtk/services/job#>  \n" +
				"prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#>  \n" +
				"	  \n" +
				"	select distinct ?status where {  \n" +
				"	   ?Job a job:Job.  \n" +
				"	   ?Job job:id '%s'^^XMLSchema:string .  \n" +
				"	   ?Job job:status ?status .  \n" +
				"	}",
				SparqlToXUtils.safeSparqlString(jobId));

		endpoint.executeQuery(query, SparqlResultTypes.TABLE);

		String trList[] = endpoint.getStringResultsColumn("status");

		if (trList.length > 1) {
			throw new Exception(String.format("Job %s has %d status entries.  Expecting 1.", jobId, trList.length));
		} else if (trList.length == 0) {
			if (! jobExists(jobId) ) {
	    		throw new Exception(String.format("Can't find Job %s", jobId));
	    	} else {
	    		throw new Exception(String.format("Can't find status for Job %s",  jobId));
	    	}
		} else {
			return trList[0].split("#")[1];
		}
	}
	
	/**
	 * Get job status message
	 * @param jobId
	 * @return message string
	 * @throws Exception if jobId can't be found 
	 */
	public String getJobStatusMessage(String jobId) throws Exception {
		String query = String.format("  \n" +
				"prefix job:<http://research.ge.com/semtk/services/job#>  \n" +
				"prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#>  \n" +
				"	  \n" +
				"	select distinct ?statusMessage where {  \n" +
				"	   ?Job a job:Job.  \n" +
				"	   ?Job job:id '%s'^^XMLSchema:string .  \n" +
				"	   ?Job job:statusMessage ?statusMessage .  \n" +
				"	}",
				SparqlToXUtils.safeSparqlString(jobId));

		endpoint.executeQuery(query, SparqlResultTypes.TABLE);

		String trList[] = endpoint.getStringResultsColumn("statusMessage");

		if (trList.length > 1) {
			throw new Exception(String.format("Job %s has %d statusMessage entries.  Expecting 1.", jobId, trList.length));
		} else if (trList.length == 0) {
			if (! jobExists(jobId) ) {
	    		throw new Exception(String.format("Can't find Job %s", jobId));
	    	} else {
	    		throw new Exception(String.format("Can't find status message for Job %s",  jobId));
	    	}
		} else {
			return trList[0];
		}
	}

	/**
	 * Set job percent complete to 100, 
	 *   creating it if it doesn't exist.
	 * @param jobId
	 * @throws Exception
	 */
public void setJobSuccess(String jobId) throws Exception {
	setJobSuccess(jobId, "");
}
	
public void setJobSuccess(String jobId, String statusMessage) throws Exception {
		
		if (! jobExists(jobId)) {
	    	createJob(jobId);
	    }
	    
	    String query = String.format("  \n" +
	        "prefix job:<http://research.ge.com/semtk/services/job#> \n" +
	        "prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> \n" +
	        " \n" +
	        "DELETE {\n" +
			"   ?Job job:percentComplete ?percentComplete . \n" +
			"   ?Job job:statusMessage ?statusMessage . \n" +
	        "   ?Job job:status ?status." +
	        "} \n" +
	        "INSERT {\n" +
			"   ?Job job:percentComplete '100'^^XMLSchema:integer.  \n" +
	        "   ?Job job:statusMessage '%s'^^XMLSchema:string. \n" +
	        "   ?Job job:status job:Success. \n" +
	        "} \n" +
	        "where { \n" +
	        "   ?Job a job:Job. \n" +
	        "   ?Job job:id '%s'^^XMLSchema:string . \n" +
			"   optional {?Job job:percentComplete ?percentComplete .} \n" +
	        "   optional {?Job job:statusMessage ?statusMessage.} \n" +
	        "   optional {?Job job:status ?status.} \n" +
	        "}",
	        
	        SparqlToXUtils.safeSparqlString(statusMessage), SparqlToXUtils.safeSparqlString(jobId));
	    System.err.println(query);
	    try {
	    	endpoint.executeQuery(query, SparqlResultTypes.CONFIRM);
	    } catch (Exception e) {
	    	throw new Exception(e.getMessage());
	    }
	}

	
	/**
	 * Set the results URL for a job,
	 *    creating it if it doesn't exist.
	 * @param jobId
	 * @param fullResultsURL
	 * @throws Exception
	 */
	public void setJobResultsURL(String jobId, URL fullResultsURL) throws Exception {
		
		if (! jobExists(jobId)) {
	    	createJob(jobId);
	    }
		
		URL sampleResultsURL = null;	// temporary measure to disable sample while not changing SPARQL query.  Matching functionality of removed utility method. 
		String uriFullURL = "URL_" + UUID.randomUUID().toString();
		String uriSampleURL = "URL_" + UUID.randomUUID().toString();

		String query = String.format("  \n" +
		        "prefix job:<http://research.ge.com/semtk/services/job#> \n" +
		        "prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> \n" +
		        " \n" +
		        "DELETE { \n" +
		        "   ?Job job:fullResultsURL ?fullURI. \n" +
		        "   ?fullURI job:full ?fullURL .  \n" +
		        "   ?Job job:sampleResultsURL ?sampleURI. \n" +
		        "   ?sampleURI job:full ?sampleURL . \n" +
		        "} \n" +
		        "INSERT { \n" +
		        "   ?Job job:fullResultsURL <%s>. \n" +
		        "   <%s> job:full '%s'^^XMLSchema:string .  \n" +
		        "   ?Job job:sampleResultsURL <%s>. \n" +
		        "   <%s> job:full '%s'^^XMLSchema:string . \n" +
		        "} \n" +
		        "where { \n" +
		        "   ?Job a job:Job. \n" +
		        "   ?Job job:id '%s'^^XMLSchema:string . \n" +
		        "   optional {?Job job:fullResultsURL ?fullURI. \n" +
		        "             ?fullURI job:full ?fullURL . } \n" +
		        "   optional {?Job job:sampleResultsURL ?sampleURI. \n" +
		        "             ?sampleURI job:full ?sampleURL . } \n" +
		        "}",
		        uriFullURL, 
		        uriFullURL, 
		        SparqlToXUtils.safeSparqlString(fullResultsURL.toString()), 
		        uriSampleURL, 
		        uriSampleURL, 
		        ((sampleResultsURL != null) ? SparqlToXUtils.safeSparqlString(sampleResultsURL.toString()) : ""), 
		        SparqlToXUtils.safeSparqlString(jobId));
		    System.err.println(query);
		    try {
		    	endpoint.executeQuery(query, SparqlResultTypes.CONFIRM);
		    } catch (Exception e) {
		    	throw new Exception(e.getMessage());
		    }
	}
	
	/**
	 * Get a jobId's full results URL
	 * @param jobId
	 * @return URL which could be null
	 * @throws Exception if jobId can't be found or it doesn't have exactly one full URL
	 */
	public URL getFullResultsURL(String jobId) throws Exception {	 
		String query = String.format("  \n" +
	        "prefix job:<http://research.ge.com/semtk/services/job#>  \n" +
	    	"prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#>  \n" +
	    	"	  \n" +
	    	"	select distinct ?fullUrl where {  \n" +
	    	"	   ?Job a job:Job.  \n" +
	    	"	   ?Job job:id '%s'^^XMLSchema:string.  \n" +
	    	"	   ?Job job:fullResultsURL ?URL.  \n" +
	    	"      ?URL job:full ?fullUrl . \n" +
	    	"	}",
	    	SparqlToXUtils.safeSparqlString(jobId));

	    endpoint.executeQuery(query, SparqlResultTypes.TABLE);
	    
	    String trList[] = endpoint.getStringResultsColumn("fullUrl");
	    
	    if (trList.length > 1) {
	    	throw new Exception(String.format("Job %s has %d full restults URL entries.  Expecting 1.", jobId, trList.length));
	    } else if (trList.length == 0) {
	    	if (! jobExists(jobId) ) {
	    		throw new Exception(String.format("Can't find Job %s", jobId));
	    	} else {
	    		throw new Exception(String.format("Can't find full URL for Job %s",  jobId));
	    	}
	    } else if (trList[0].equals("")) {
	    	return null;
	    } else {
	    	return new URL(trList[0]);
	    }
	}
	
	/**
	 * Get a jobId's full results URL
	 * @param jobId
	 * @return URL which could be null
	 * @throws Exception if jobID can't be found or it doesn't have exactly one sample URL
	 */
	public URL getSampleResultsURL(String jobId) throws Exception {	 
		String query = String.format("  \n" +
	        "prefix job:<http://research.ge.com/semtk/services/job#>  \n" +
	    	"prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#>  \n" +
	    	"	  \n" +
	    	"	select distinct ?sampleUrl where {  \n" +
	    	"	   ?Job a job:Job.  \n" +
	    	"	   ?Job job:id '%s'^^XMLSchema:string.  \n" +
	    	"	   ?Job job:sampleResultsURL ?URL.  \n" +
	    	"      ?URL job:full ?sampleUrl . \n" +
	    	"	}",
	    	SparqlToXUtils.safeSparqlString(jobId));

	    endpoint.executeQuery(query, SparqlResultTypes.TABLE);
	    
	    String trList[] = endpoint.getStringResultsColumn("sampleUrl");
	    
	    if (trList.length > 1) {
	    	throw new Exception(String.format("Job %s has %d full restults URL entries.  Expecting 1.", jobId, trList.length));
	    } else if (trList.length == 0) {
	    	if (! jobExists(jobId) ) {
	    		throw new Exception(String.format("Can't find Job %s", jobId));
	    	} else {
	    		throw new Exception(String.format("Can't find sample URL for Job %s",  jobId));
	    	}
	    } else if (trList[0].equals("")) {
	    	return null;
	    } else {
	    	return new URL(trList[0]);
	    }
	}
	
	/**
	 * Create a Job with given jobId.  
	 * @param jobId
	 * @throws Exception
	 */
	public void createJob(String jobId) throws Exception {	    
	    
		// Caller must first ensure that job doesn't exist
		
		String uri = String.format("<Job_%s>", UUID.randomUUID().toString());
	    String query = String.format("  \n" +
	        "prefix job:<http://research.ge.com/semtk/services/job#> \n" +
	        "prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> \n" +

	        " \n" +
	        "INSERT  {%s a job:Job.  %s job:id '%s'^^XMLSchema:string. %s job:percentComplete '0'^^XMLSchema:integer. } \n",
	    	uri, uri, SparqlToXUtils.safeSparqlString(jobId), uri);
	    System.err.println(query);
	    try {
	    	endpoint.executeQuery(query, SparqlResultTypes.CONFIRM);
	    } catch (Exception e) {
	    	throw new Exception(e.getMessage());
	    }
	}
	
	/**
	 * Delete a Job with given jobId if it exists
	 * @param jobId
	 * @throws Exception 
	 */
	public void deleteJob(String jobId) throws Exception {	    
		
		
	    String query = String.format("  \n" +
	        "prefix job:<http://research.ge.com/semtk/services/job#> \n" +
	        "prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> \n" +

	        " \n" +
	        "DELETE  {  \n" +
			"   ?Job ?y ?z.    \n" +
			"   ?z ?zo ?zp.  \n" +
			"} \n" +
			"where { \n" +
			"   ?Job a job:Job. \n" +
			"   ?Job job:id ?id. \n" +
			"      VALUES ?id { '%s'^^XMLSchema:string }. \n" +
			"   ?Job ?y ?z." +
			"   optional { ?z ?zo ?zp. }  \n" +
			"}",
			SparqlToXUtils.safeSparqlString(jobId));
	    System.err.println(query);
	    try {
	    	endpoint.executeQuery(query, SparqlResultTypes.CONFIRM);
	    } catch (Exception e) {
	    	throw new Exception(e.getMessage());
	    }
	}
	/**
	 * Does jobId exist
	 * @param jobId
	 * @return true if job with jobId exists
	 * @throws Exception
	 */
	public boolean jobExists(String jobId) throws Exception {

		String query = String.format("  \n" +
				"prefix job:<http://research.ge.com/semtk/services/job#> \n" +
		        "prefix XMLSchema:<http://www.w3.org/2001/XMLSchema#> \n" +
				"SELECT count(?Job) \n" +
				"where { \n" +
				"   ?Job a job:Job. \n" +
				"   ?Job job:id ?id. \n" +
				"      VALUES ?id { '%s'^^XMLSchema:string }. \n" +
				"}",
				SparqlToXUtils.safeSparqlString(jobId));
		System.err.println(query);
		try {
			endpoint.executeQuery(query, SparqlResultTypes.TABLE);
		    String trList[] = endpoint.getStringResultsColumn("callret-0");
		    if (trList.length == 1) {
		    	if (trList[0].equals("1")) { 
		    		return true; 
		    	} else if (trList[0].equals("0")) { 
		    		return false; 
		    	} else {
		    		throw new Exception (String.format("Found %s jobs with jobId=%s", trList[0], jobId));
		    	}
		    } else {
	    		throw new Exception (String.format("Error processing results of count query for jobId=%s.  %d rows returned.", jobId, trList.length));
		    }

		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	/**
	 * Return when job with given jobId is at least percentComplete % complete
	 * @param jobId
	 * @param percentComplete
	 * @param maxWaitMsec
	 * @throws Exception if maxWaitMsec milliseconds pass without a return
	 */
	public void waitForPercentComplete(String jobId, int percentComplete, int maxWaitMsec) throws Exception {
		int totalMsec = 0;
		int sleepMsec = 0;
		
		// wait maximum of this.prop.jobMaxWatiMsec
		while (totalMsec < maxWaitMsec) {
			if (getJobPercentComplete(jobId) >= percentComplete) {
				return;
			}
			// wait 1/4 seconds longer each time until 3 seconds
			if (sleepMsec < 3000) {
				sleepMsec += 250;
			}
			Thread.sleep(sleepMsec);
			totalMsec += sleepMsec;
		}
		throw new Exception(String.format("Maximum wait time of %d Msec has passed without job %s reaching %d percent complete.", maxWaitMsec, jobId, percentComplete));
	}
}
