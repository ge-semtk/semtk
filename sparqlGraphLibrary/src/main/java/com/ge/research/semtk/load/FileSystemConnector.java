package com.ge.research.semtk.load;


/**
 * Connect to an abstract place to dump files by unique key
 * @author 200001934
 *
 */
public abstract class FileSystemConnector {
	public abstract void putObject(String keyName, byte [] data) throws Exception;
	
	public abstract byte[] getObject(String keyName) throws Exception ;
	
	public abstract void deleteObject(String keyName) ;
}