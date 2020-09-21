package com.ge.research.semtk.load;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;

import com.ge.research.semtk.load.FileSystemConnector;


public class DirectoryConnector extends FileSystemConnector {
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
	
	public void putObject(String fileName, byte [] data) throws Exception {
		File f = Paths.get(this.folderStr, fileName).toFile();
		
		if (this.checkExists(fileName)) {
			throw new Exception("Key already exists: " + fileName);
		}
		FileOutputStream output = new FileOutputStream(f);
		IOUtils.write(data, output);
		output.close();
	}
	
	public byte[] getObject(String fileName) throws IOException {
		File f = Paths.get(this.folderStr, fileName).toFile();
		FileInputStream is = new FileInputStream(f);
		
		byte [] ret = IOUtils.toByteArray(is);
		is.close();
		return ret;
	}
	
	public boolean checkExists(String fileName) throws Exception {
		File f = Paths.get(this.folderStr, fileName).toFile();
		return f.exists();
		
	}
	
	public void deleteObject(String fileName) {
		File f = Paths.get(this.folderStr, fileName).toFile();
		f.delete();
	}

}
