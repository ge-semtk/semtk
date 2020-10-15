package com.ge.research.semtk.load.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.ge.research.semtk.load.DirectoryConnector;
import com.google.common.io.Files;

public class DirectoryConnectorTest {

	@Test
	public void test() throws Exception, IOException {
		// make a temp dir and set up
		
		File tempDir = Files.createTempDir();
		String dirPath = tempDir.getAbsolutePath();
		DirectoryConnector dirconn = new DirectoryConnector(dirPath);
				
		// create an object from some text
		String keyName = UUID.randomUUID().toString() + ".txt";
		String origStr = "This is the content.";
		byte data[] = origStr.getBytes();
		
		// write object
		dirconn.putObject(keyName, data);
		
		// write object
		try {
			dirconn.putObject(keyName, data);
			fail("Missing expected exception on duplicate key");
		} catch (Exception e) {
			
		}
		
		
		// read it back
	    byte result[] = dirconn.getObject(keyName);
		
	    String resStr = new String(result);
	    assertTrue(
	    		"returned string did not match: " + resStr, 
	    		resStr.equals(origStr));
	    
	    // delete it
		dirconn.deleteObject(keyName);
		
		assertFalse("Still exists after deleting: " + keyName, dirconn.checkExists(keyName));
		
		FileUtils.deleteDirectory(tempDir);

	}

}
