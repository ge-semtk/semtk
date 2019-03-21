package com.ge.research.semtk.services.sparql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.ge.research.semtk.auth.AuthorizationManager;

@Component
public class SparqlQueryServiceStartup implements ApplicationListener<ApplicationReadyEvent> {
	
	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {
		
	}		
}
