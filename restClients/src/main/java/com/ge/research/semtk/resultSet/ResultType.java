/**
 ** Copyright 2018 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */
package com.ge.research.semtk.resultSet;

/**
 * Special table column types inside SemTK
 * @author 200001934
 *
 */
public enum ResultType {
	BINARY_FILE_ID ("binaryFileId"),
	JOB_ID ("jobId"),
	URL ("URL");
	
	private static final String prefix = "http://semtk.ge.com/resulttype#";
	private String shortName = null;
	
	ResultType(String shortName) {
		this.shortName = shortName;
	}
	
	public String getShortName() {
		return this.shortName;
	}
	
	public String getPrefixedName() {
		return prefix + this.getShortName();
	}
}
