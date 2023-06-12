package com.ge.research.semtk.nodegroupexecution.test;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.api.nodeGroupExecution.StitchingStep;
import com.ge.research.semtk.api.nodeGroupExecution.StitchingThread;
import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.ResultsClient;
import com.ge.research.semtk.nodeGroupStore.client.NodeGroupStoreRestClient;
import com.ge.research.semtk.resultSet.Table;
import com.ge.research.semtk.services.nodegroupStore.NgStore.StoredItemTypes;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.utility.Utility;

public class StitchingTest_IT {
	private static NodeGroupStoreRestClient nodeGroupStoreRestClient = null;	
	private static String ngPrefix = "junit-";
	private static final String CREATOR = "JUnit StitchingTest_IT";
	
	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();	
		
		// model and data
		TestGraph.clearGraph();
		TestGraph.uploadOwlResource(StitchingTest_IT.class, "/annotationBattery.owl");
		TestGraph.uploadOwlResource(StitchingTest_IT.class, "/annotationBatteryStitchData.owl");
				
		// manage store
		nodeGroupStoreRestClient = IntegrationTestUtility.getNodeGroupStoreRestClient();		
		IntegrationTestUtility.cleanupNodegroupStore(nodeGroupStoreRestClient, CREATOR);
		
		String nodegroups[] = new String [] {
				"annotationBatteryIdName", 
				"annotationBatteryIdCellId"
				};
		
		for (String ng :  nodegroups) {
			String path = StitchingTest_IT.class.getResource("/" + ng + ".json").getPath();
			nodeGroupStoreRestClient.storeItem(ngPrefix + ng, "junit", CREATOR, path, StoredItemTypes.PrefabNodeGroup, null, true);
		}
	}
	@AfterClass
    public static void teardown() throws Exception {
        // delete stored nodegroups when done with all tests
		IntegrationTestUtility.cleanupNodegroupStore(nodeGroupStoreRestClient, CREATOR);
		TestGraph.clearGraph();
    } 
	
	@Test
	public void test() throws Exception {
		// [{ "nodegroupId": "junit-annotationBatteryIdName" }, { "nodegroupId": "junit-annotationBatteryIdCellId", "keyColumns": ["id"]}]
		StitchingStep steps[] = new StitchingStep[] {
				new StitchingStep(ngPrefix + "annotationBatteryIdName", null),
				new StitchingStep(ngPrefix + "annotationBatteryIdCellId", new String [] { "id" })
				};
		String jobId = JobTracker.generateJobId();
		StitchingThread thread = new StitchingThread(steps, TestGraph.getSparqlConn(), 
				IntegrationTestUtility.getNodeGroupStoreRestClient(),
				IntegrationTestUtility.getDispatchRestClient(),
				IntegrationTestUtility.getResultsClient(),
				IntegrationTestUtility.getServicesSei(),
				jobId);
				
		thread.start();
		JobTracker tracker = new JobTracker(IntegrationTestUtility.getServicesSei());
		tracker.waitForSuccess(jobId, 90000);
		Table tab = IntegrationTestUtility.getResultsClient().getTableResultsJson(jobId, null);
		assertEquals("Wrong number of rows stitched", 4, tab.getNumRows());
		assertEquals("Wrong number of cols stitched", 3, tab.getNumColumns());
	}
	
	@Test
	public void testOppositeOrder() throws Exception {
		StitchingStep steps[] = new StitchingStep[] {
				new StitchingStep(ngPrefix + "annotationBatteryIdCellId", null),
				new StitchingStep(ngPrefix + "annotationBatteryIdName", new String [] { "id" })
				};
		String jobId = JobTracker.generateJobId();
		StitchingThread thread = new StitchingThread(steps, TestGraph.getSparqlConn(), 
				IntegrationTestUtility.getNodeGroupStoreRestClient(),
				IntegrationTestUtility.getDispatchRestClient(),
				IntegrationTestUtility.getResultsClient(),
				IntegrationTestUtility.getServicesSei(),
				jobId);
				
		thread.start();
		JobTracker tracker = new JobTracker(IntegrationTestUtility.getServicesSei());
		tracker.waitForSuccess(jobId, 90000);
		Table tab = IntegrationTestUtility.getResultsClient().getTableResultsJson(jobId, null);
		assertEquals("Wrong number of rows stitched", 4, tab.getNumRows());
		assertEquals("Wrong number of cols stitched", 3, tab.getNumColumns());
	}

}
