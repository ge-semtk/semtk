package com.ge.research.semtk.springsvclib.security.props;

// your service's properties should extend this, so useSecurity is available
public class SecurityProperties {
	private boolean useSecurity = false;

	public boolean useSecurity() {
		return useSecurity;
	}

	public void setUseSecurity(boolean useSecurity) {
		this.useSecurity = useSecurity;
	}
	
}
