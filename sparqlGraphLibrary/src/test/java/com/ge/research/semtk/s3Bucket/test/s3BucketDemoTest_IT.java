package com.ge.research.semtk.s3Bucket.test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

import org.junit.Test;

import com.arangodb.internal.util.IOUtils;
import com.ge.research.semtk.sparqlX.S3BucketConfig;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class s3BucketDemoTest_IT {

	@Test
	public void test() throws IOException {
		// pull S3 environment variables, skipping the test if they aren't set
		S3BucketConfig config;
		try {
			config = new S3BucketConfig();
		} catch (Exception e) {
			assumeTrue(e.getMessage(), false);
			return;
		}
		
		// build an AWS S3Client
		Region region = Region.of(config.getRegion());
		S3Client s3 = S3Client.builder().region(region).build();	
		
		// create an s3 object from some text
		String keyName = UUID.randomUUID().toString() + ".txt";
		String origStr = "This is the content.";
		byte data[] = origStr.getBytes();
		
		// write object to S3
		PutObjectResponse res = s3.putObject(PutObjectRequest.builder().bucket(config.getName()).key(keyName).build(),
				RequestBody.fromBytes(data));
        
		assertTrue(
				"Error putting object into s3: " + res.sdkHttpResponse().statusText(),
				res.sdkHttpResponse().isSuccessful() );
		
		// read it back
		ResponseInputStream<GetObjectResponse> s3objectResponse = s3.getObject(GetObjectRequest.builder().bucket(config.getName()).key(keyName).build());
		
		DataInputStream is = new DataInputStream(s3objectResponse);
	    byte result[] = IOUtils.toByteArray(is);
		
	    String resStr = new String(result);
	    assertTrue(
	    		"returned string did not match: " + resStr, 
	    		resStr.equals(origStr));
	    
	    // delete it
		s3.deleteObject(DeleteObjectRequest.builder().bucket(config.getName()).key(keyName).build());
	}

}

