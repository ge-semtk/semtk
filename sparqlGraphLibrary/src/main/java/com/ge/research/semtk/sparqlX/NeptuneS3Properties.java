package com.ge.research.semtk.sparqlX;

import com.ge.research.semtk.properties.Properties;

public class NeptuneS3Properties extends Properties {

	// optional for Neptune upload Owl
	private String s3ClientRegion;   
	private String s3BucketName;
	private String awsIamRoleArn;

	public String getS3ClientRegion() {
		return s3ClientRegion;
	}

	public void setS3ClientRegion(String s3ClientRegion) {
		this.s3ClientRegion = s3ClientRegion;
	}

	public String getS3BucketName() {
		return s3BucketName;
	}

	public void setS3BucketName(String s3BucketName) {
		this.s3BucketName = s3BucketName;
	}

	public String getAwsIamRoleArn() {
		return awsIamRoleArn;
	}

	public void setAwsIamRoleArn(String awsIamRoleArn) {
		this.awsIamRoleArn = awsIamRoleArn;
	}
	
	public void validate() throws Exception {
		super.validate();
		checkNone("s3ClientRegion", s3ClientRegion);
		checkNone("s3BucketName", s3BucketName);
		checkNone("awsIamRoleArn", awsIamRoleArn);
		
	}
}

