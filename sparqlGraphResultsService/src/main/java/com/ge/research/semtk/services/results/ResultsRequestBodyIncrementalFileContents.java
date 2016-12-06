package com.ge.research.semtk.services.results;

public class ResultsRequestBodyIncrementalFileContents extends ResultsRequestBodyFileExtContents {
	public int segmentNumber;
	
	public void setSegmentNumber(int segmentNumber){ this.segmentNumber = segmentNumber;}
	public int  getSegmentNumber(){ return this.segmentNumber; }

}
