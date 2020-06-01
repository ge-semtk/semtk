package com.ge.research.semtk.standaloneExecutables;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.test.IntegrationTestUtility;


// TODO add test for spaces in nodegroup json path
// TODO add test with overridden SPARQL connection
public class StoreNodeGroupTest_IT {
	
	static String serviceURL = null;
	
	@BeforeClass
	public static void setup() throws Exception {
		serviceURL = IntegrationTestUtility.getNodeGroupStoreFullURL();
	}

    @Test
    public void test() throws Exception {
    	// TODO confirm nodegroups don't exist
    	StoreNodeGroup.processCSVFile(serviceURL, "./src/test/resources/nodegroupsTest.csv");
    	// TODO check that nodegroups were created, then delete them
    }
    
    @Test
    public void testFileWithHeaderOnly() throws Exception {
          StoreNodeGroup.processCSVFile(serviceURL, "./src/test/resources/csvBoundaryHeaderOnly.csv");
    }

}
