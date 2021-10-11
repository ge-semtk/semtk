package com.ge.research.semtk.aws;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * @author 200001934
 * Interceptor to add AWS_SESSION_TOKEN if it is in the environment
 */
public class AWSSessionTokenApacheInterceptor implements HttpRequestInterceptor {
	private String token = null;
	
	public AWSSessionTokenApacheInterceptor(String token) {
		this.token = token;
	}
	@Override
	public void process(final HttpRequest request, final HttpContext context)
			throws HttpException, IOException { 
	
		if (this.token != null) {
			request.addHeader("x-amz-security-token", token);
		}
	}
}
