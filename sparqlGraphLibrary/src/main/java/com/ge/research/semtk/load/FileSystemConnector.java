package com.ge.research.semtk.load;

import com.ge.research.semtk.utility.Utility;

/**
 * Connect to an abstract place to dump files by unique key
 * @author 200001934
 *
 */
public abstract class FileSystemConnector {
	
	public abstract void putObject(String keyName, byte [] data) throws Exception;
	
	public abstract byte[] getObject(String keyName) throws Exception;
	
	public abstract void deleteObject(String keyName);
	
	/**
	 * Copy a file from the file system to local
	 */
	public void getObjectAsLocalFile(String keyname, String localFilePath) throws Exception{
		byte[] fileContents = getObject(keyname);
		Utility.writeFile(localFilePath, fileContents);
	}

}