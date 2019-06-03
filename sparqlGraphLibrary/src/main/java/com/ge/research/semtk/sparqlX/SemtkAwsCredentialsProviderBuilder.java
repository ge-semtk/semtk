package com.ge.research.semtk.sparqlX;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.utility.LocalLogger;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;

public class SemtkAwsCredentialsProviderBuilder {
	
	private static String awsArnRole = null;
	private static StaticCredentialsProvider lastProvider = null;
	private static Date expirationDate = null;
	private static String token = null;
	
	static final String SECURITY_CREDENTIALS_URL = "http://169.254.169.254/latest/meta-data/iam/security-credentials";
	
	
	public static void tryMe() {
		AssumeRoleRequest roleRequest = AssumeRoleRequest.builder().roleArn("bu-pw-teds-athena").build();
		StsClient stsClient = StsClient.create();
		stsClient.assumeRole(roleRequest);

		Credentials cred = stsClient.getSessionToken().credentials();
		LocalLogger.logToStdOut("TryMe: " + cred.accessKeyId() + "--" + cred.secretAccessKey() + "==" + cred.sessionToken());
		stsClient.close();
	}

	/**
	 * Try several ways to find credentials
	 * 	1) security provider - temporary session credentials
	 *  2) instance provider
	 *  3) default provider
	 *  
	 *  The CredentialsProvider returned here may generate either
	 *      AwsSessionCredentials (has .getSessionToken())
	 *      AwsCredentials (does not have a token)
	 *      
	 * @return StaticCredentialsProvider or null
	 * @throws Exception
	 */
	public static StaticCredentialsProvider getAWSCredentialsProvider() throws Exception {
		
		// if this is the first call
		if (lastProvider == null) {
			retrieveAWSArnRole();
			
			if (!awsArnRole.isEmpty()) {
				// if we got a role from credentials-provider, we should also get temporary credentials
				lastProvider = StaticCredentialsProvider.create(getTemporaryCredentials(awsArnRole));
				
			} else {
				
				// try instance creds
				try {
					AwsCredentials instanceCredentials =  InstanceProfileCredentialsProvider.create().resolveCredentials();
					lastProvider = StaticCredentialsProvider.create(instanceCredentials);
				} catch (Exception e) {

					// try Default Credentials
					AwsCredentials defaultCredentials = DefaultCredentialsProvider.create().resolveCredentials();
					lastProvider = StaticCredentialsProvider.create(defaultCredentials);
				}
			}
			
			// if previous credentials provider has expired (within 60 sec of doing so)
		} else if (expirationDate != null && expirationDate.getTime() - Calendar.getInstance().getTime().getTime() < 60000) {
			
			// get new temporary credentials
			lastProvider = StaticCredentialsProvider.create(getTemporaryCredentials(awsArnRole));
			
		}
		
		return lastProvider;
	}

	/**
	 * Get credentials for role, or throw an exception
	 * @return credentials
	 * @throws Exception
	 */
	private static AwsSessionCredentials getTemporaryCredentials(String role) throws Exception {
		AwsSessionCredentials ret = null;
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(SECURITY_CREDENTIALS_URL + "/" + role);
		CloseableHttpResponse response = httpclient.execute(httpGet);
		if (response.getStatusLine().getStatusCode() != 200) {
			// non-retryable
			throw new Exception("Error retrieving temporary credentials: " + response.getStatusLine());
		}

		// read the temporary credentials JSON
		try {
			HttpEntity entity = response.getEntity();
			String responseTxt = EntityUtils.toString(entity, "UTF-8");
			JSONObject responseObj = (JSONObject) new JSONParser().parse(responseTxt);
			String accessKey =     (String) responseObj.get("AccessKeyId");
			String secretKey =     (String) responseObj.get("SecretAccessKey");
			token =         (String) responseObj.get("Token");
			expirationDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse((String) responseObj.get("Expiration"));
			ret = AwsSessionCredentials.create(accessKey, secretKey, token);

			EntityUtils.consume(entity);
		} finally {
			response.close();
		}
		
		return ret;
	}

	/**
	 * Use the AWS credentials provider to ask for a single role.
	 * If this doesn't work, awsArnRole will be set to "".
	 * 
	 * @return awsArnRole or "" if security-provider doesn't work here
	 * @throws Exception - got an answer and can't understand it
	 */
	private static void retrieveAWSArnRole() throws Exception {
		
		// set role to "" so we don't try this over and over
		awsArnRole = "";

		// try a role-provider
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(SECURITY_CREDENTIALS_URL);
		CloseableHttpResponse response = httpclient.execute(httpGet);

		// set this.awsArnRole if the credentials-provider returns one
		try {
			// silently ignore failure:  no temporary credentials
			if (response.getStatusLine().getStatusCode() == 200) {
				HttpEntity entity = response.getEntity();
				String responseTxt = EntityUtils.toString(entity, "UTF-8");
				if (responseTxt != null && !responseTxt.isEmpty()) {
					String arnRoles[] = responseTxt.split("[ \t\n]+");
					if (arnRoles.length > 1) {
						throw new Exception("Did not recieve a single role from the credentials provider.  response: " + responseTxt);
					} else if (arnRoles.length == 1) {
						awsArnRole = arnRoles[0];
					}
				}
				EntityUtils.consume(entity);
			}
		} finally {
			response.close();
		}

	}
}
