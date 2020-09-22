package com.ge.research.semtk.load;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;

import com.ge.research.semtk.load.FileSystemConnector;


/**
 * Connect to a given directory on a file system.
 */
public class DirectoryConnector extends FileSystemConnector {
	
	private String folderStr = null;
	
	public DirectoryConnector(String path) throws Exception {
		this.folderStr = path;
		File f = new File(this.folderStr);
		if (! f.exists() || ! f.isDirectory()) {
			throw new Exception(path + " is not a valid directory path");
		}
	}
	
	public String toString() {
		return "DirectoryConnector: folderStr=" + (this.folderStr == null ? "null" : this.folderStr);
	}
	
	public void putObject(String fileName, byte [] data) throws Exception {
		File f = Paths.get(this.folderStr, fileName).toFile();
		
		if (this.checkExists(fileName)) {
			throw new Exception("File already exists: " + fileName);
		}
		
		FileOutputStream output = null;
		try{
			output = new FileOutputStream(f);
			IOUtils.write(data, output);
		}catch(Exception e){
			throw e;
		}finally{
			output.close();
		}
	}

	public byte[] getObject(String fileName) throws IOException {
		File f = Paths.get(this.folderStr, fileName).toFile();
		FileInputStream is = null;
		byte[] ret;
		try{
			is = new FileInputStream(f);
			ret = IOUtils.toByteArray(is);
		}catch(Exception e){
			throw e;
		}finally{
			is.close();
		}
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
