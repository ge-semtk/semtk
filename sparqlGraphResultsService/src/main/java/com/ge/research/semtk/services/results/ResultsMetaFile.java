package com.ge.research.semtk.services.results;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ge.research.semtk.auth.ThreadAuthenticator;

public class ResultsMetaFile {
	private static final String META_SUFFIX = "_meta.json";
	private static final String JSON_PROP_FILENAME = "filename";
	private static final String JSON_PROP_PATH = "path";
	
	private String path = null;
	private String filename = null;
	private String username = ThreadAuthenticator.ANONYMOUS;
	
	/**
	 * Create a file for writing
	 */
	public ResultsMetaFile() {}
	
	/**
	 * Create a file for reading
	 * @param filePath
	 * @throws Exception
	 */
	public ResultsMetaFile(String filePath) throws Exception {
		if (! fileIsInstanceOf(filePath)) {
			throw new Exception ("File is not a ResultsMetaFile: " + filePath);
		}
		this.read(filePath);
	}
	
	public ResultsMetaFile(FileReader reader) throws Exception {

		this.read(reader);
	}
	
	public static boolean fileIsInstanceOf(String filePath) {
		return filePath.endsWith(META_SUFFIX);
	}
	
	public static String getSuffix() {
		return META_SUFFIX;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getUserName() {
		return username;
	}

	public void setUserName(String username) {
		this.username = username;
	}
	
	public String getFileName() {
		return filename;
	}

	public void setFileName(String filename) {
		this.filename = filename;
	}
	
	public void read(String filePath) throws Exception {
		this.read(new FileReader(filePath));
	}
	
	public void read(FileReader reader) throws Exception {
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = (JSONObject) parser.parse(reader);
		this.filename = (String) jsonObject.get(JSON_PROP_FILENAME);
        this.path = (String) jsonObject.get(JSON_PROP_PATH);
	}
	
	public void write(String filePath) throws IOException {
		JSONObject metaInfo = new JSONObject();
		metaInfo.put(JSON_PROP_FILENAME, this.filename);
		metaInfo.put(JSON_PROP_PATH, this.path);
		
		FileWriter fw = new FileWriter(filePath);
		fw.write(metaInfo.toJSONString());
		fw.flush();
		fw.close();
	}
	
	/**
	 * Delete file pointed to by this meta-file
	 */
	public void deleteTargetPath() {
		File f = new File(this.getPath());
		if (f.exists()) {
			f.delete();
		}
	}
}
