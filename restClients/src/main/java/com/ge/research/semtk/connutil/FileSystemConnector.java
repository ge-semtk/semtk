package com.ge.research.semtk.connutil;

import com.ge.research.semtk.utility.Utility;

/**
 * File system connector for writing and reading files
 * @author 200001934
 */
public abstract class FileSystemConnector {
	
	public abstract void putObject(String fileName, byte [] data) throws Exception;
	
	public abstract byte[] getObject(String fileName) throws Exception;
	
	public abstract void deleteObject(String fileName);
	
	public abstract boolean checkExists(String fileName) throws Exception;
	
	// NOTE: future could overload all with functions that take String [] pathComponents instead of fileName
	//       and build a path properly given underlying resource
	
	/**
	 * Copy a file from the file system to local
	 */
	public void getObjectAsLocalFile(String fileName, String localFilePath) throws Exception{
		byte[] fileContents = getObject(fileName);
		Utility.writeFile(localFilePath, fileContents);
	}

}