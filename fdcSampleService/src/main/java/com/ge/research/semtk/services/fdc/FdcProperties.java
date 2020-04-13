package com.ge.research.semtk.services.fdc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.ge.research.semtk.properties.Properties;

@Configuration
@ConfigurationProperties(prefix="fdc.sample", ignoreUnknownFields = true)
public class FdcProperties extends Properties {
	
	private int deleteme = 10;
	
	public FdcProperties() {
		super();
		setPrefix("fdc.sample");
	}

	public void validate() throws Exception {
		super.validate();
		this.checkRangeInclusive("deleteme", deleteme, 9, 11);
	}
	
	public int getDeleteMe() {
		return deleteme;
	}
	public void setDeleteMe(int s) {
		this.deleteme = s;
	}
	
	
}
