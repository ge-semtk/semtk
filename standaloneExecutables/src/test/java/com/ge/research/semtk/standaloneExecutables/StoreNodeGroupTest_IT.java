package com.ge.research.semtk.standaloneExecutables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    	try {
	    	IntegrationTestUtility.cleanupNodegroupStore("junit");
	    	StoreNodeGroup.main(new String [] {serviceURL, "./src/test/resources/nodegroupsTest.csv"});
	    	assertEquals(4, IntegrationTestUtility.countItemsInStoreByCreator("junit"));
    	} finally {
    		IntegrationTestUtility.cleanupNodegroupStore("junit");
    	}
    	
    }
    
    @Test
    public void testFileWithHeaderOnly() throws Exception {
          StoreNodeGroup.main(new String [] {serviceURL, "./src/test/resources/csvBoundaryHeaderOnly.csv"});
    }

}
