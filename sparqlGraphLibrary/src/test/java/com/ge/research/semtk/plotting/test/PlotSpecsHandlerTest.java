package com.ge.research.semtk.plotting.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ge.research.semtk.plotting.PlotSpecsHandler;
import com.ge.research.semtk.utility.Utility;

public class PlotSpecsHandlerTest {

	@Test
	public void getPlotNames() throws Exception {
		
		PlotSpecsHandler plotSpecsHandler = new PlotSpecsHandler(Utility.getJSONArrayFromFilePath("src/test/resources/plots-spec.json"));
		assertEquals(plotSpecsHandler.getNumPlotSpecs(), 2);
		assertEquals(plotSpecsHandler.getPlotSpecNames().length, 2);
		assertEquals(plotSpecsHandler.getPlotSpecNames()[0], "Plotly Chart 1");
		assertEquals(plotSpecsHandler.getPlotSpecNames()[1], "Plotly Chart 2");
		
		
		try{
			plotSpecsHandler = new PlotSpecsHandler(Utility.getJSONArrayFromFilePath("src/test/resources/plots-spec-missingname.json"));
			plotSpecsHandler.getPlotSpecNames();
			fail("Missing expected exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Plot spec is missing 'name'"));
		}
	}
	
}
