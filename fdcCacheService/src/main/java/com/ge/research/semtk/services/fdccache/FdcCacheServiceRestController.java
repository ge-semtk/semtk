/**
 ** Copyright 2020 General Electric Company
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

package com.ge.research.semtk.services.fdccache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.fdccache.FdcCacheSpecRunner;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.springutillib.headers.HeadersManager;
import com.ge.research.semtk.springutillib.properties.AuthProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.springutillib.properties.ServicesGraphProperties;
import com.ge.research.semtk.utility.LocalLogger;

import io.swagger.annotations.ApiOperation;

/**
 * Cache service
 */
@CrossOrigin
@RestController
@RequestMapping("/fdcCache")
@ComponentScan(basePackages = {"com.ge.research.semtk.springutillib"})
public class FdcCacheServiceRestController {
	
	private static final String SERVICE_NAME = "fdcCacheService";
	
	@Autowired 
	private ApplicationContext appContext;
	@Autowired
	AuthProperties auth_prop; 
	@Autowired
	CacheNgExecProperties ngexec_prop; 
	@Autowired
	CacheNgStoreProperties ngstore_prop; 
	@Autowired
	CacheOInfoProperties oinfo_prop; 
	@Autowired
	ServicesGraphProperties servicesgraph_prop; 
	
	@PostConstruct
    public void init() {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();	
		
		auth_prop.validateWithExit();
		AuthorizationManager.authorizeWithExit(auth_prop);
		
		ngexec_prop.validateWithExit();
		ngstore_prop.validateWithExit();
		oinfo_prop.validateWithExit();
		servicesgraph_prop.validateWithExit();
	}
	
	@ApiOperation(
			value="Start a cache process, bootstrap with table results from step 1's input nodegroup"
			//notes="nnn"
			)
	@CrossOrigin
	@RequestMapping(value="/cacheUsingTableBootstrap", method= RequestMethod.POST)
	public JSONObject cacheUsingTableBootstrap(@RequestBody TableBootstrapRequest request,  @RequestHeader HttpHeaders headers) {
		HeadersManager.setHeaders(headers);		
		final String ENDPOINT_NAME = "cacheUsingTableBootstrap";
		
		SimpleResultSet results = new SimpleResultSet();
		try {
			Table bootstrapTable = request.buildBootstrapTable();
			FdcCacheSpecRunner runner = new FdcCacheSpecRunner(
					request.getSpecId(), 
					request.buildSparqlConnection(), 
					request.getRecacheAfterSec(),
					servicesgraph_prop.buildSei(), 
					oinfo_prop.getClient(), 
					ngexec_prop.getClient(), 
					ngstore_prop.getClient());
			runner.setBootstrapTable(bootstrapTable);
			runner.start();
			results.setSuccess(true);
			results.addResult(results.JOB_ID_RESULT_KEY, runner.getJobId());
			return results.toJson();
			
		} catch(Exception e){
	    	SimpleResultSet res = new SimpleResultSet();
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
		    LocalLogger.printStackTrace(e);
		    return res.toJson();
		}  
	}
	
}
