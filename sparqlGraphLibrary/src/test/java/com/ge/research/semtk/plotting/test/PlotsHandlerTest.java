package com.ge.research.semtk.plotting.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ge.research.semtk.plotting.PlotsHandler;
import com.ge.research.semtk.utility.Utility;

public class PlotsHandlerTest {

	@Test
	public void getPlotNames() throws Exception {
		
		PlotsHandler plotsHandler = new PlotsHandler(Utility.getJSONArrayFromFilePath("src/test/resources/plots-spec.json"));
		assertEquals(plotsHandler.getNumPlots(), 2);
		assertEquals(plotsHandler.getPlotNames().length, 2);
		assertEquals(plotsHandler.getPlotNames()[0], "Plotly Chart 1");
		assertEquals(plotsHandler.getPlotNames()[1], "Plotly Chart 2");
		
		
		try{
			plotsHandler = new PlotsHandler(Utility.getJSONArrayFromFilePath("src/test/resources/plots-spec-missingname.json"));
			plotsHandler.getPlotNames();
			fail("Missing expected exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Plot spec is missing 'name'"));
		}
	}
	
}
