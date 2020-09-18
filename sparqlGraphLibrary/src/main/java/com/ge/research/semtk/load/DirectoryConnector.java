package com.ge.research.semtk.load;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;

import com.ge.research.semtk.load.FileBucketConnector;


public class DirectoryConnector extends FileBucketConnector {
	String folderStr = null;
	/** 
	 * 
	 * Create from standard environment variables
	 * @throws IOException 
	 * @throws Exception
	 */
	
	public DirectoryConnector(String path) throws Exception {
		this.folderStr = path;
		
		File f = new File(this.folderStr);
		if (! f.exists() || ! f.isDirectory()) {
			throw new Exception(path + " is not a valid directory path");
		}
	}
	
	public void putObject(String keyName, byte [] data) throws Exception {
		File f = Paths.get(this.folderStr, keyName).toFile();
		FileOutputStream output = new FileOutputStream(f);
		IOUtils.write(data, output);
		output.close();
	}
	
	public byte[] getObject(String keyName) throws IOException {
		File f = Paths.get(this.folderStr, keyName).toFile();
		FileInputStream is = new FileInputStream(f);
		
		byte [] ret = IOUtils.toByteArray(is);
		is.close();
		return ret;
	}
	
	public boolean checkExists(String keyName) throws Exception {
		File f = Paths.get(this.folderStr, keyName).toFile();
		return f.exists() && f.isDirectory();
		
	}
	
	public void deleteObject(String keyName) {
		File f = Paths.get(this.folderStr, keyName).toFile();
		f.delete();
	}

}
