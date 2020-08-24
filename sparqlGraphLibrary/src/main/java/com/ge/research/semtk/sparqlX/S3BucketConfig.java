package com.ge.research.semtk.sparqlX;

public class S3BucketConfig {
	String region = null;
	String name = null;
	String iamRoleArn = null;

	/** 
	 * Create from standard environment variables
	 * @throws Exception
	 */
	public S3BucketConfig() throws Exception {
		this.region = System.getenv("NEPTUNE_UPLOAD_S3_CLIENT_REGION");
		this.name = System.getenv("NEPTUNE_UPLOAD_S3_BUCKET_NAME");
		this.iamRoleArn = System.getenv("NEPTUNE_UPLOAD_S3_AWS_IAM_ROLE_ARN");
		String failedVariables = "";
		
		if (region == null || region.isEmpty()) {
			failedVariables += "NEPTUNE_UPLOAD_S3_CLIENT_REGION ";
		}
		if (this.name == null || this.name.isEmpty()) {
			failedVariables += "NEPTUNE_UPLOAD_S3_BUCKET_NAME ";
		}
		if (this.iamRoleArn == null || this.iamRoleArn.isEmpty()) {
			failedVariables += "NEPTUNE_UPLOAD_S3_AWS_IAM_ROLE_ARN ";
		}
		if (!failedVariables.isEmpty()) {
			throw new Exception("Config error: can't perform Neptune upload with blank variable(s) in SemTK service environment: \n" + failedVariables);
		}
	}
	
	public S3BucketConfig(String region, String name, String iamRoleArn) {
		this.region = region;
		this.name = name;
		this.iamRoleArn = iamRoleArn;
		
	}
	
	public void verifySetup() throws Exception {
		if (this.region == null || this.region.isEmpty() || this.region.startsWith("$") ||
				this.name == null || this.name.isEmpty() || this.region.startsWith("$") ||
				this.iamRoleArn == null || this.iamRoleArn.isEmpty() 
				) {
			throw new Exception("S3 bucket configuration contains empty or null values: " + this.toString());
		}
	}
	
	public String toString() {
		return "S3BucketConfig:" + 
				" name=" +       (this.name == null       ? "null" : this.name) +
				" region=" +     (this.region == null     ? "null" : this.region) +
				" iamRoleArn=" + (this.iamRoleArn == null ? "null" : this.iamRoleArn);
	}
	public String getRegion() {
		return region;
	}

	public String getName() {
		return name;
	}

	public String getIamRoleArn() {
		return iamRoleArn;
	}

}
