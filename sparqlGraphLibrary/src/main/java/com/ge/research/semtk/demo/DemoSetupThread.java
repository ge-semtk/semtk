package com.ge.research.semtk.demo;

import java.io.InputStream;

import org.json.simple.JSONObject;

import com.ge.research.semtk.auth.AuthorizationManager;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.load.DataLoader;
import com.ge.research.semtk.load.dataset.CSVDataset;
import com.ge.research.semtk.load.dataset.Dataset;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.services.nodegroupStore.NgStore;
import com.ge.research.semtk.sparqlX.SparqlConnection;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.utility.LocalLogger;
import com.ge.research.semtk.utility.Utility;

public class DemoSetupThread extends Thread {
	private SparqlEndpointInterface demoSei = null;
	private SparqlEndpointInterface storeSei = null;
	
	

	public DemoSetupThread(SparqlEndpointInterface storeSei, SparqlEndpointInterface servicesSei) {
		this.storeSei = storeSei;
		this.demoSei = servicesSei;
		this.demoSei.setGraph("http://semtk/demo");
	}
	
	public void run() {
		boolean success = this.runIngest();
		
		LocalLogger.logToStdOut("loading demo...");

		if (! success)  {
			LocalLogger.logToStdErr("Error loading demo.  Waiting 30 sec and trying again.");
			try {
				Thread.sleep(30000);
			} catch (Exception e) {
			}
			success = this.runIngest();
			if (!success) {
				LocalLogger.logToStdErr("Error loading demo second time.  Giving up.");
			}
		}
	}
	
	public boolean runIngest() {
		boolean success = false;
		try {
			AuthorizationManager.setSemtkSuper();
			
			// setup demoSei and demoConn
			SparqlConnection demoConn = new SparqlConnection("demoConn", this.demoSei);

			// put demoNodegroup into the store
			JSONObject sgJsonJson = Utility.getResourceAsJson(this, "/nodegroups/demoNodegroup.json");
			SparqlGraphJson sgJson = new SparqlGraphJson(sgJsonJson);
			sgJson.setSparqlConn(demoConn);
			
			NgStore store = new NgStore(this.storeSei);	
			JSONObject connJson = sgJson.getSparqlConnJson();
			store.deleteNodeGroup("demoNodegroup");
			store.insertNodeGroup(sgJsonJson, connJson, "demoNodegroup", "demo comments", "semTK", true);
	
			// load demo model owl
			InputStream owlStream = JobTracker.class.getResourceAsStream("/semantics/OwlModels/hardware.owl");
			OntologyInfo.uploadOwlModelIfNeeded(demoSei, owlStream);
			owlStream = JobTracker.class.getResourceAsStream("/semantics/OwlModels/testconfig.owl");
			OntologyInfo.uploadOwlModelIfNeeded(demoSei, owlStream);
			
			// ingest demo csv
			demoSei.clearPrefix("http://demo/prefix");  // extra safe.  No clear graph inside nodegroup store
			String data = Utility.getResourceAsString(this, "demoNodegroup_data.csv");
			Dataset ds = new CSVDataset(data, true);
			DataLoader dl = new DataLoader(sgJson, ds, demoSei.getUserName(), demoSei.getPassword());
			dl.importData(false);
			success = true;
		} catch (Exception e) {
			LocalLogger.printStackTrace(new Exception("Error setting up demo", e));
			success = false;
		} finally {
			AuthorizationManager.clearSemtkSuper();
		}
		return success;
	}
}
