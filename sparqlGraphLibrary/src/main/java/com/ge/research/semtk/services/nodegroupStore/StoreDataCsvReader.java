package com.ge.research.semtk.services.nodegroupStore;

import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;

import com.ge.research.semtk.services.nodegroupStore.NgStore.StoredItemTypes;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class StoreDataCsvReader extends StoreDataCsv {
	String filename = null;
	CSVReader br = null;
	Hashtable<String, Integer> colHash = null;
	int colCount = 0;
	int lineNumber;
	String [] parsedLine = null;

	public StoreDataCsvReader(String csvFileName) throws Exception {
		this.br = new CSVReader(new FileReader(csvFileName));
		this.filename = csvFileName;
		this.lineNumber = 0;
		this.checkHeaders();
	}

	private void checkHeaders() throws Exception {
		String[] parsedLine = br.readNext(); // header line

		// hash column names
		this.colHash = new Hashtable<String, Integer>();
		int col=0;
		for (String headerName: parsedLine) {
			colHash.put(headerName.trim().toLowerCase(), Integer.valueOf(col));
			col++;
		}
		this.colCount = this.colHash.keySet().size();
		
		// check column names
		for (String head : required_headers) {
			if (!this.colHash.containsKey(head)) 
				throw new Exception("Nodegroup csv is missing required header: " + head);
		}
		
		// check for errors
		String [] line;
		int lineNumber = 1;
		while ((line = this.br.readNext()) != null) {
			if (line.length != 0 && line.length != this.colCount) {
				throw new Exception("Poorly formed csv: wrong number of columns in row " + lineNumber );
			}
			lineNumber++;
		}
		
		// close and re-open
		this.br.close();
		this.br = new CSVReader(new FileReader(filename));
		this.br.readNext();
		
	}
	
	/**
	 * Read next line, returning line error message, "", or null
	 * @return parsed line, but see the get*() functions to get the values more easily
	 * @throws CsvValidationException
	 * @throws IOException
	 */
	public String [] readNext() throws CsvValidationException, IOException {
		
		this.lineNumber += 1;
		this.parsedLine = this.br.readNext();
		if (this.parsedLine == null) {
			this.br.close();
			return null;
		} else if (parsedLine.length == 0) {
			return this.readNext();
		} else {
			return this.parsedLine;
		}
	}
	// required columns already checked
	public String getId() { return this.parsedLine[this.colHash.get(ID)]; }
	public String getComments() { return this.parsedLine[this.colHash.get(COMMENTS)]; }
	public String getCreator() { return this.parsedLine[this.colHash.get(CREATOR)]; }
	public String getJsonFile() { return this.parsedLine[this.colHash.get(JSON_FILE)]; }
	
	// optional column defaults to null
	public StoredItemTypes getItemType() { 
		return this.colHash.containsKey("itemtype") ? NgStore.StoredItemTypes.valueOf(parsedLine[colHash.get("itemtype")].trim()) : null;
	}
}
