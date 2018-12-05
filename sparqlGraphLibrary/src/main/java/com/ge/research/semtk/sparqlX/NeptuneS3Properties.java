package com.ge.research.semtk.sparqlX;

public class NeptuneS3Properties {

	// optional for Neptune upload Owl
	private String s3ClientRegion;   
	private String s3BucketName;
	private String s3AccessId;
	private String s3Secret;
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

	public String getS3AccessId() {
		return s3AccessId;
	}

	public void setS3AccessId(String s3AccessId) {
		this.s3AccessId = s3AccessId;
	}

	public String getS3Secret() {
		return s3Secret;
	}

	public void setS3Secret(String s3Secret) {
		this.s3Secret = s3Secret;
	}

}

