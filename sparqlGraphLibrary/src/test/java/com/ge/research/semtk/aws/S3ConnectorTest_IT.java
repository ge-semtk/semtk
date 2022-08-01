package com.ge.research.semtk.aws;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

import org.junit.Test;

import com.ge.research.semtk.utility.LocalLogger;

public class S3ConnectorTest_IT {

	@Test
	public void test() throws Exception, IOException {
		// pull S3 environment variables, skipping the test if they aren't set
		S3Connector s3conn;
		try {
			String region = System.getenv("NEPTUNE_UPLOAD_S3_CLIENT_REGION");
			String name = System.getenv("NEPTUNE_UPLOAD_S3_BUCKET_NAME");
			LocalLogger.logToStdOut("S3ConnectorTest_IT test with region " + region + ", bucket " + name);
			s3conn = new S3Connector(region, name);
		} catch (Exception e) {
			assumeTrue("S3 is not configured: " + e.getMessage(), false);
			return;
		}
				
		// create an s3 object from some text
		String keyName = UUID.randomUUID().toString() + ".txt";
		String origStr = "This is the content.";
		byte data[] = origStr.getBytes();
		
		// write object to S3
		s3conn.putObject(keyName, data);
		
		// read it back
		byte result[] = s3conn.getObject(keyName);
		String resStr = new String(result);
		assertTrue(
				"returned string did not match: " + resStr, 
				resStr.equals(origStr));
	    
	    // delete it
		s3conn.deleteObject(keyName);
		assertFalse("Still exists after deleting: " + keyName, s3conn.checkExists(keyName));

	}

}

