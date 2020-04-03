package com.ge.research.semtk.standaloneExecutables;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.test.IntegrationTestUtility;

import static junit.framework.TestCase.assertTrue;

public class TestStoreNodeGroup_IT {
	
	static String serviceURL = null;
	
	@BeforeClass
	public static void setup() throws Exception {
		serviceURL = IntegrationTestUtility.getNodeGroupStoreFullURL();
	}
    
    @Test
    public void testFileWithEmptyLine() {

        try {
            StoreNodeGroup.processCSVFile(serviceURL, "./src/test/resources/csvBoundaryEmptyLines.csv");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testFileWithHeaderOnly() {

        try {
           StoreNodeGroup.processCSVFile(serviceURL, "./src/test/resources/csvBoundaryHeaderOnly.csv");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testUploadToVesuviusTest() {

        try {
            StoreNodeGroup.processCSVFile(serviceURL, "./src/test/resources/nodegroupsTest.csv");
            // test does not check that anything actually worked
            // it should also delete these nodegroups
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

}
