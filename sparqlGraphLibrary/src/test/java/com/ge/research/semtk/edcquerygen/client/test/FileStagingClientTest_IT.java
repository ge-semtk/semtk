package com.ge.research.semtk.edcquerygen.client.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.edc.JobTracker;
import com.ge.research.semtk.edc.client.FileStagingClient;
import com.ge.research.semtk.resultSet.TableResultSet;
import com.ge.research.semtk.test.IntegrationTestUtility;
import com.ge.research.semtk.utility.Utility;
import static org.junit.Assume.*
;
public class FileStagingClientTest_IT {

	private static String SERVICE_PROTOCOL;
	private static String SERVICE_SERVER;
	private static int SERVICE_PORT;
	private static String SERVICE_ENDPOINT = "fileStaging/stageFile";	

	@BeforeClass
	public static void setup() throws Exception {
		IntegrationTestUtility.authenticateJunit();		
		SERVICE_PROTOCOL = IntegrationTestUtility.get("protocol");
		SERVICE_SERVER = IntegrationTestUtility.get("filestaging.server");
		SERVICE_PORT = IntegrationTestUtility.getInt("filestaging.port");
		
		assumeTrue("No file staging service server is configured for JUNIT", ! SERVICE_SERVER.isEmpty());
	}	
	
	@Test
	public void test() throws Exception {
			
		// write a test file to /tmp
		String uuid = UUID.randomUUID().toString();
		String testFileName = "junit-" + uuid + ".txt";
		String testFilePath = "/tmp/" + testFileName;
		String testFileContents = "This is content for file " + testFileName; 
		Utility.writeFile(testFilePath, testFileContents.getBytes());		// write the file to disk (/tmp)
		String stagedFileName = testFileName + ".staged";					// this will be the name of the staged file (also in /tmp - assumes fileStagingService is set to directory mode on /tmp)
		
		// stage the file
		FileStagingClient client = new FileStagingClient(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT, SERVICE_ENDPOINT);
		String jobId = JobTracker.generateJobId();
		client.setJobId(jobId);
		TableResultSet result = (TableResultSet)client.execute(testFileName + "###" + stagedFileName);

		// check that the file is staged
		assertTrue(result.getRationaleAsString("\n"), result.getSuccess());
		assertEquals(result.getResults().getRows().size(), 1);
		String stagedFileId = result.getTable().getCell(0,"fileID");
		String stagedFileContents = IntegrationTestUtility.getResultsClient().execReadBinaryFile(stagedFileId);
		assertEquals(stagedFileContents, testFileContents);
		
		// remove the temp files
		(new File(testFilePath)).delete();
		IntegrationTestUtility.getResultsClient().execDeleteJob(jobId);  // TODO this is not deleting the staged file - should it?
	}
	
	@Test
	public void testStageNonexistentFile() throws Exception{
		FileStagingClient client = new FileStagingClient(SERVICE_PROTOCOL, SERVICE_SERVER, SERVICE_PORT, SERVICE_ENDPOINT);
		String jobId = JobTracker.generateJobId();
		client.setJobId(jobId);
		TableResultSet result = (TableResultSet)client.execute("nonexistentfile.txt###stagedfile.txt");
		assertFalse(result.getSuccess());
		assertTrue(result.getRationaleAsString("").contains("No such file or directory") || result.getRationaleAsString("").contains("The system cannot find the file specified"));
	}

}
