package com.ge.research.semtk.aws;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

import org.junit.Test;

import com.arangodb.internal.util.IOUtils;
import com.ge.research.semtk.aws.S3Connector;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class S3ConnectorTest_IT {

	@Test
	public void test() throws Exception, IOException {
		// pull S3 environment variables, skipping the test if they aren't set
		S3Connector s3conn;
		try {
			String region = System.getenv("NEPTUNE_UPLOAD_S3_CLIENT_REGION");
			String name = System.getenv("NEPTUNE_UPLOAD_S3_BUCKET_NAME");
			s3conn = new S3Connector(region, name);
		} catch (Exception e) {
			assumeTrue(e.getMessage(), false);
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
	}

}

