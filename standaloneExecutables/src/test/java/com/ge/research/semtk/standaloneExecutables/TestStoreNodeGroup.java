package com.ge.research.semtk.standaloneExecutables;

import org.junit.Test;

import static junit.framework.TestCase.assertTrue;

public class TestStoreNodeGroup {

    @Test
    public void testFileWithEmptyLine() {

        try {
            StoreNodeGroup.processCSVFile("http://vesuvius-test.crd.ge.com:12056", "./src/test/resources/csvBoundaryEmptyLines.csv");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testFileWithHeaderOnly() {

        try {
           StoreNodeGroup.processCSVFile("http://vesuvius-test.crd.ge.com:12056", "./src/test/resources/csvBoundaryHeaderOnly.csv");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testUploadToVesuviusTest() {

        try {
            StoreNodeGroup.processCSVFile("http://vesuvius-test.crd.ge.com:12056", "./src/test/resources/nodegroupsTest.csv");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

}
