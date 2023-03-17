package com.ge.research.semtk.utility.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.ge.research.semtk.utility.Logger;
import com.ge.research.semtk.utility.Logger.Levels;

public class LoggerTest {

	@Test
	public void test() throws Exception {
		assertEquals(Logger.getMessage("INFO: loaded data"), "loaded data");
		assertEquals(Logger.getLevel("INFO: loaded data"), Levels.INFO);
		assertEquals(Logger.getLevel("WARNING: missing column"), Levels.WARNING);
		assertEquals(Logger.getLevel("ERROR: load failed"), Levels.ERROR);
		
		try {
			Logger.getLevel("MESSAGE: loaded data");
			fail(); // should not get here
		}catch(Exception e) {
			assert(e.getMessage().contains("Unrecognized level"));
		}
	}
	
}
