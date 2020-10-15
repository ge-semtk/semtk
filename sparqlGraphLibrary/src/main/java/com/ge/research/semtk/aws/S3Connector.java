package com.ge.research.semtk.aws;

import java.io.DataInputStream;
import java.io.IOException;

import com.arangodb.internal.util.IOUtils;
import com.ge.research.semtk.load.FileSystemConnector;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Connector to put/get files from AWS S3.
 * 
 * NOTE: No IAM role is explicitly used here - the code will use the IAM role attached to the EC2 node.
 */
public class S3Connector extends FileSystemConnector {
	private String region = null;
	private String name = null;
	private S3Client s3Client = null;

	/** 
	 * Create from standard environment variables
	 * @throws Exception
	 */
	
	public S3Connector(String region, String name) {
		this.region = region;
		this.name = name;
		this.s3Client = this.buildS3Client();		
		
		// test it
		ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(this.name).maxKeys(1).build();
		this.s3Client.listObjectsV2Paginator(request);
	}
	
	private S3Client buildS3Client() {
		Region region = Region.of(this.getRegion());
		return S3Client.builder().region(region).build();	
	}
	
	public String toString() {
		return "S3BucketConfig:" + 
				" name=" +       (this.name == null       ? "null" : this.name) +
				" region=" +     (this.region == null     ? "null" : this.region);
	}
	public String getRegion() {
		return region;
	}

	public String getName() {
		return name;
	}
	
	public void putObject(String fileName, byte [] data) throws Exception {
		
		if (this.checkExists(fileName)) {
			throw new Exception("Key already exists: " + fileName);
		}
		PutObjectResponse res = this.s3Client.putObject(PutObjectRequest.builder().bucket(this.name).key(fileName).build(),
				RequestBody.fromBytes(data));
	
        
		if (res.sdkHttpResponse().isSuccessful() == false) {
			throw new Exception("Error putting object into s3: " + res.sdkHttpResponse().statusText());
		}
	}
	
	public boolean checkExists(String fileName) throws Exception {
		try {
			ResponseInputStream<GetObjectResponse> s3objectResponse = this.s3Client.getObject(GetObjectRequest.builder().bucket(this.name).key(fileName).build());
			return true;
		} catch (NoSuchKeyException nske) {
			return false;
		}
	}
	
	public byte[] getObject(String fileName) throws IOException {
		ResponseInputStream<GetObjectResponse> s3objectResponse = this.s3Client.getObject(GetObjectRequest.builder().bucket(this.name).key(fileName).build());
		
		DataInputStream is = new DataInputStream(s3objectResponse);
	    return IOUtils.toByteArray(is);
	}
	
	public void deleteObject(String fileName) {
		this.s3Client.deleteObject(DeleteObjectRequest.builder().bucket(this.name).key(fileName).build());
	}

}
