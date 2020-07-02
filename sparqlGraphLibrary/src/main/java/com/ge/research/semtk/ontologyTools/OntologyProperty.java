/**
 ** Copyright 2016 General Electric Company
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


package com.ge.research.semtk.ontologyTools;

import com.ge.research.semtk.ontologyTools.OntologyName;
import com.ge.research.semtk.ontologyTools.OntologyRange;

/** 
 * Represents Datatype or Object Property
 * @author 200001934
 *
 */
public class OntologyProperty extends AnnotatableElement{

	private OntologyName  name = null;
	private OntologyRange range = null;
	// if we wanted, for sparql gen without an oInfo, we could store to json for nodegroup:
	// private boolean hasSubProps
	
	public OntologyProperty(String name, String range){
		this.name  = new OntologyName(name);
		this.range = new OntologyRange(range);
	}
	
	public OntologyName getName(){
		return this.name;
	}
	
	public OntologyRange getRange(){
		return this.range;
	}
	
	public String getNameStr() {
		return this.getNameStr(false);
	}
	public String getNameStr(Boolean stripNamespace){
		if(stripNamespace){
			return this.name.getLocalName();
		}
		else{	// don't strip namespace
			return this.name.getFullName();
		}
	}
	
	public String getRangeStr() {
		return this.getRangeStr(false);
	}
	public String getRangeStr(Boolean stripNamespace){
		if(stripNamespace){
			return this.range.getLocalName();
		}
		else{	// don't strip the namespace
			return this.range.getFullName();
		}
	}
	
	public Boolean powerMatch(String pattern){
		String pat = pattern.toLowerCase();
		Boolean retval = this.getNameStr(true).toLowerCase().contains(pat) || this.getRangeStr(true).toLowerCase().contains(pat);
		return retval;
	}
}
