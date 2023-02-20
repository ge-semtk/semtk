package com.ge.research.semtk.standaloneExecutables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.Permission;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.test.IntegrationTestUtility; 


// TODO add test for spaces in nodegroup json path
// TODO add test with overridden SPARQL connection
public class StoreNodeGroupTest_IT {
	
	// For changing the System.Exit() calls in main() to SecurityException
	private static SecurityManager defaultManager = null;
	private static class MySecurityManager extends SecurityManager {
		@Override public void checkExit(int status) throws SecurityException {
			throw new SecurityException("Exit " + status);
		}

		@Override public void checkPermission(Permission perm) {
			// Allow other activities by default
		}
	}
	
	static String serviceURL = null;
	
	@BeforeClass
	public static void setup() throws Exception {
		serviceURL = IntegrationTestUtility.getNodeGroupStoreFullURL();
		
		// Change the System.Exit() calls in main() to SecurityException
		defaultManager = System.getSecurityManager();
    	MySecurityManager secManager = new MySecurityManager();
        System.setSecurityManager(secManager);
	}
	@AfterClass
	public static void cleanup() throws Exception {
		// Restore the default so the test can System.exit() properly
		System.setSecurityManager(defaultManager);
	}

    @Test
    public void test() throws Exception {
    	// TODO confirm nodegroups don't exist
    	
    	try {
    		
	    	IntegrationTestUtility.cleanupNodegroupStore("junit");
	    	try {
	    		StoreNodeGroup.main(new String [] {serviceURL, "./src/test/resources/nodegroupsTest.csv"});
	    	} catch (SecurityException e) {
	    		assertTrue("No exit 0 detected", e.toString().contains("0"));
	    	}
	    	assertEquals(4, IntegrationTestUtility.countItemsInStoreByCreator("junit"));
    	} finally {
    		IntegrationTestUtility.cleanupNodegroupStore("junit");
    	}
    	
    }
    
    @Test
    public void testFileWithHeaderOnly() throws Exception {
    	try {
          StoreNodeGroup.main(new String [] {serviceURL, "./src/test/resources/csvBoundaryHeaderOnly.csv"});
    	} catch (SecurityException e) {
    		assertTrue("No exit 0 detected", e.toString().contains("0"));
    	}
    }

}
