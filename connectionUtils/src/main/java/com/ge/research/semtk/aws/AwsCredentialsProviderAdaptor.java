package com.ge.research.semtk.aws;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * Adapt a aws-sdk-v2 credentials provider to a v1 credentials provider
 * 
 * Modified slightly from https://github.com/awslabs/aws-request-signing-apache-interceptor/issues/5
 * to get rid of the non-portable lombok annotations
 */
//@Deprecated 
public class AwsCredentialsProviderAdaptor implements AWSCredentialsProvider {
    final AwsCredentialsProvider credentialsProvider;

	public AwsCredentialsProviderAdaptor(AwsCredentialsProvider cred) {
		this.credentialsProvider = cred;
	}
    @Override
    public AWSCredentials getCredentials() {
        return new AWSCredentials() {
            AwsCredentials credentials = credentialsProvider.resolveCredentials();

            @Override
            public String getAWSAccessKeyId() {
                return credentials.accessKeyId();
            }

            @Override
            public String getAWSSecretKey() {
                return credentials.secretAccessKey();
            }
        };
    }

    @Override
    public void refresh() { }
}