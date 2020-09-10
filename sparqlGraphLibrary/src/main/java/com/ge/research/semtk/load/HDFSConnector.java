package com.ge.research.semtk.load;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

import com.ge.research.semtk.load.FileSystemConnector;



public class HDFSConnector extends FileSystemConnector {
	Configuration configHDFS = null;
	String pathStr = null;
	/** 
	 * Create from standard environment variables
	 * @throws IOException 
	 * @throws Exception
	 */
	
	public HDFSConnector(String fullURL, String path) throws IOException {
		this.configHDFS = new Configuration();
		this.configHDFS.set("fs.defaultFS", fullURL);
		this.pathStr = path;
	}
	

	public void putObject(String keyName, byte [] data) throws Exception {
//		FileSystem fs = FileSystem.get(this.configHDFS);
//		FSDataOutputStream fsDataOutputStream = fs.create(new Path(pathStr, keyName), true);
//        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fsDataOutputStream,StandardCharsets.UTF_8));
//        bufferedWriter.write(new String(data, StandardCharsets.UTF_8));
//        bufferedWriter.close();
//        fs.close();
	}
	
	public byte[] getObject(String keyName) throws IOException {
//		FileSystem fs = FileSystem.get(this.configHDFS);
//
//        Path hdfsReadPath = new Path(pathStr, keyName);
//        //Init input stream
//        FSDataInputStream inputStream = fs.open(hdfsReadPath);
//        fs.co
//        //Classical input stream usage
//        String out = IOUtils(inputStream, StandardCharsets.UTF_8);
//       
//        inputStream.close();
//        fs.close();
		return null;
	}
	
	public void deleteObject(String keyName) {
	}

}
