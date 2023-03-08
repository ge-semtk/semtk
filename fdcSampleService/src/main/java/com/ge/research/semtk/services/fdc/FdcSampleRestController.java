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

package com.ge.research.semtk.services.fdc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.resultSet.SimpleResultSet;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.dispatch.FdcServiceManager;
import com.ge.research.semtk.springutilib.requests.FdcRequest;
import com.ge.research.semtk.springutillib.controllers.NodegroupProviderRestController;
import com.ge.research.semtk.springutillib.properties.AuthProperties;
import com.ge.research.semtk.springutillib.properties.EnvironmentProperties;
import com.ge.research.semtk.springutillib.properties.OntologyInfoServiceProperties;
import com.ge.research.semtk.springutillib.properties.ServicesGraphProperties;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

import io.swagger.v3.oas.annotations.Operation;

/**
 * Sample FDC service
 */
@CrossOrigin
@RestController
@RequestMapping("/fdcSample")
@ComponentScan(basePackages = {"com.ge.research.semtk.springutillib"})
public class FdcSampleRestController extends NodegroupProviderRestController {
	private static final String SERVICE_NAME = "fdcSample";

	@Autowired
	private FdcProperties fdc_props;
	@Autowired
	private AuthProperties auth_prop;
	@Autowired
	private OntologyInfoServiceProperties oinfo_props;
	@Autowired
	private ServicesGraphProperties servicesgraph_prop;
	@Autowired 
	private ApplicationContext appContext;
	
	@PostConstruct
    public void init() {
		EnvironmentProperties env_prop = new EnvironmentProperties(appContext, EnvironmentProperties.SEMTK_REQ_PROPS, EnvironmentProperties.SEMTK_OPT_PROPS);
		env_prop.validateWithExit();
		
		fdc_props.validateWithExit();
		
		// --- put a sample FDC config in to the services sei if needed --- //
		
		int msec = 0;
		while (true) {
			try {
				// load all sorts of extra things to allow access to services graph and oInfo service
				auth_prop.validateWithExit();
				AuthorizationManager.authorizeWithExit(auth_prop);
				servicesgraph_prop.validateWithExit();
				oinfo_props.validateWithExit();
				
				SparqlEndpointInterface servicesSei = servicesgraph_prop.buildSei();
				Table servicesFDCConfig = FdcServiceManager.junitGetFdcConfig(servicesSei, oinfo_props.getClient());
				
				// if FDC config is not loaded
				if (!Arrays.asList(servicesFDCConfig.getColumn("fdcClass")).contains("http://research.ge.com/semtk/fdcSample/test#AircraftLocation")) {
					String configOwl = Utility.getResourceAsString(this, "/fdcTestSetup/fdcConfigSample.owl");
					configOwl = configOwl.replace("localhost:12070", "localhost/" + String.valueOf(fdc_props.getPort()));
					servicesSei.authUploadOwl(configOwl.getBytes());	
				}
				
				return;
				
			} catch (Exception e) {
				if (msec < 60000) {
					try {
						Thread.sleep(20000);
						msec += 20000;
						
					} catch (Exception ee) {
						LocalLogger.logToStdErr("Error trying to load sample FDC config to services graph");
						LocalLogger.printStackTrace(e);
					}
					
				} else {
					LocalLogger.logToStdErr("Error trying to load sample FDC config to services graph");
					LocalLogger.printStackTrace(e);
					return;
				}
			}
		}
	}
	
	
	@CrossOrigin
	@RequestMapping(value="/distance", method= RequestMethod.POST)
	public JSONObject distance(@RequestBody FdcRequest request) {
		final String ENDPOINT_NAME = "distance";
		try {
			// build a hashmap of the incoming tables and check their column names
			HashMap<String,Table> paramTables = request.buildTableHash();
			FdcRequest.validateTableHash(paramTables, "1", new String [] {"latitude1", "longitude1", "location1"});
			FdcRequest.validateTableHash(paramTables, "2", new String [] {"latitude2", "longitude2", "location2"});
			
			// build empty results table
			Table resTab = new Table(
					new String [] {"location1", "location2", "distanceNm"},
					new String [] {"uri", "uri", "double"} );
			
			// add row for each cross-product of rows from table 1 and table 2
			Table table1 = paramTables.get("1");
			Table table2 = paramTables.get("2");
			for (int i=0; i < table1.getNumRows(); i++) {
				double lat1 = table1.getCellAsFloat(i, "latitude1");
				double lon1 = table1.getCellAsFloat(i, "longitude1");
				String loc1 = table1.getCell(i, "location1");
				
				for (int j=0; j < table2.getNumRows(); j++) {
					double lat2 = table2.getCellAsFloat(j, "latitude2");
					double lon2 = table2.getCellAsFloat(j, "longitude2");
					String loc2 = table2.getCell(j, "location2");

					double distKm = haversine(lat1, lon1, lat2, lon2);
					double distNm = distKm * 0.539957;
					
					// only insert reasonable values.  NaN and large doubles may be crashing virtuoso.
					if (!Double.isNaN(distNm) && distNm >= 0.0 && distNm < 20000.0) {
						resTab.addRow(new String [] { loc1, loc2, String.valueOf(distNm)});
					}
				}
			}
			
			// return a semTK table result set
			TableResultSet results = new TableResultSet();
			results.setSuccess(true);
			results.addResults(resTab);
			return results.toJson();
			
		} catch(Exception e){
	    	SimpleResultSet res = new SimpleResultSet();
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
		    LocalLogger.printStackTrace(e);
		    return res.toJson();
		}  
	}
	
	@CrossOrigin
	@RequestMapping(value="/aircraftLocation", method= RequestMethod.POST)
	public JSONObject aircraftLocation(@RequestBody FdcRequest request) {
		final String ENDPOINT_NAME = "aircraftLocation";
		try {
			// convert to hash paramTables.get("1") = semTkTable number "1"
			HashMap<String,Table> paramTables = request.buildTableHash();
			// verify input table contains correct columns
			FdcRequest.validateTableHash(paramTables, "1", new String [] {"aircraftUri", "tailNumber"});
			
			// build empty results table
			Table resTab = new Table(
					new String [] {"aircraftUri", "latitude", "longitude"},
					new String [] {"uri", "double", "double"} );
			
			// add rows to the results
			Table table1 = paramTables.get("1");
			for (int i=0; i < table1.getNumRows(); i++) {
				// put every aircraft over Niskayuna, NY
				resTab.addRow(new String [] { table1.getCell(i, "aircraftUri"), String.valueOf("42.8246365"), String.valueOf("-73.8859444")});
			}
			
			// return semTK table resultSet
			TableResultSet results = new TableResultSet();
			results.setSuccess(true);
			results.addResults(resTab);
			return results.toJson();
			
		} catch(Exception e){
	    	SimpleResultSet res = new SimpleResultSet();
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
		    LocalLogger.printStackTrace(e);
		    return res.toJson();
		}  
	}
	
	@Operation(
			summary="Generate fake elevation for testing only",
			description="elevation = floor(latitude) + 1000"
			)
	@CrossOrigin
	@RequestMapping(value="/elevation", method= RequestMethod.POST)
	public JSONObject elevation(@RequestBody FdcRequest request) {
		final String ENDPOINT_NAME = "elevation";
		try {
			HashMap<String,Table> paramTables = request.buildTableHash();
			FdcRequest.validateTableHash(paramTables, "1", new String [] {"location", "latitude", "longitude"});
			Table table1 = paramTables.get("1");

			// create elev = floor(latitude) + 1000
			Table resTab = new Table( new String [] {"location", "elevation"}, new String [] {"uri", "int"});
			
			for (int i=0; i < table1.getNumRows(); i++) {
				String loc = table1.getCellAsString(i, "location");
				int elev = (int) Math.floor(table1.getCellAsFloat(i, "latitude")) + 1000;
			
				resTab.addRow(new String [] {loc, String.valueOf(elev)});
			}
			
			// package results
			TableResultSet results = new TableResultSet();
			results.setSuccess(true);
			results.addResults(resTab);
			return results.toJson();
			
		} catch(Exception e){
	    	SimpleResultSet res = new SimpleResultSet();
	    	res.setSuccess(false);
	    	res.addRationaleMessage(SERVICE_NAME, ENDPOINT_NAME, e);
		    LocalLogger.printStackTrace(e);
		    return res.toJson();
		}  
	}
	
	
	
	
	// https://rosettacode.org/wiki/Haversine_formula#Java
	public static final double R = 6372.8; // In kilometers
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
 
        double a = Math.pow(Math.sin(dLat / 2),2) + Math.pow(Math.sin(dLon / 2),2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }
    
	
}
