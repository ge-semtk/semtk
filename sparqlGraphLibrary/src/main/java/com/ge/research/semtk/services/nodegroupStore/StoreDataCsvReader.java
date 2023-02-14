package com.ge.research.semtk.services.nodegroupStore;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Hashtable;

import com.ge.research.semtk.services.nodegroupStore.NgStore.StoredItemTypes;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class StoreDataCsvReader extends StoreDataCsv {
	CSVReader br = null;
	PrintWriter writer = null;
	Hashtable<String, Integer> colHash = null;
	int colCount = 0;
	int lineNumber;
	String [] parsedLine = null;

	public StoreDataCsvReader(String csvFileName, PrintWriter writer) throws Exception {
		this.br = new CSVReader(new FileReader(csvFileName));
		this.lineNumber = 0;
		this.checkHeaders();
		
	}
	
	private void checkHeaders() throws Exception {
		String[] parsedLine = br.readNext(); // header line

		// hash column names
		this.colHash = new Hashtable<String, Integer>();
		int col=0;
		for (String headerName: parsedLine) {
			colHash.put(headerName.toLowerCase(), Integer.valueOf(col));
			col++;
		}
		this.colCount = this.colHash.keySet().size();
		
		// check column names
		for (String head : required_headers) {
			if (!this.colHash.containsKey(head)) 
				throw new Exception("Nodegroup csv is missing required header: " + head);
		}
	}
	
	/**
	 * Read next line, returning line error message, "", or null
	 * @return warnings, empty string if none, null if no more lines
	 * @throws CsvValidationException
	 * @throws IOException
	 */
	public String readNext() throws CsvValidationException, IOException {
		
		this.lineNumber += 1;
		this.parsedLine = this.br.readNext();
		
		if (this.parsedLine == null) {
			return null;
		} else if (parsedLine.length == 0) {
			return "Ignoring blank line number: "+ this.lineNumber;
		} else  if (parsedLine.length < colCount) {
			return "Ignoring! Missing column in line: "+Arrays.toString(parsedLine);
		} else if (parsedLine.length > colCount ) {
			return "Ignoring! Found Too many: "+parsedLine.length+" columns in line: "+Arrays.toString(parsedLine);
		} else {
			return "";
		}
	}
	// required columns already checked
	public String getId() { return this.parsedLine[this.colHash.get(ID)]; }
	public String getComments() { return this.parsedLine[this.colHash.get(COMMENTS)]; }
	public String getCreator() { return this.parsedLine[this.colHash.get(CREATOR)]; }
	public String getJsonFile() { return this.parsedLine[this.colHash.get(JSON_FILE)]; }
	
	// optional column defaults to null
	public StoredItemTypes getItemType() { 
		return this.colHash.containsKey("itemtype") ? NgStore.StoredItemTypes.valueOf(parsedLine[colHash.get("itemtype")]) : null;
	}
}
