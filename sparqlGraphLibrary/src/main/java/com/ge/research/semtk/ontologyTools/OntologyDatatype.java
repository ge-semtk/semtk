/**
 ** Copyright 2021 General Electric Company
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

import java.util.HashSet;

import com.ge.research.semtk.sparqlX.XSDSupportedType;

/** 
 * Represents Datatype 
 * 
 * ?x a rdfs:Datatype                #instead of class
 * ?x owl:equivalentClass ?blank     # is typically written by SADL
 * ?blank owl:onDatatype ?equivType  # ?equivType is an XSDSupportedType
 * 
 * @author 200001934
 *
 */
public class OntologyDatatype extends AnnotatableElement{

	private OntologyName  name = null;
	private HashSet<XSDSupportedType> xsdTypes = new HashSet<XSDSupportedType>();
	private HashSet<String> strTypes = new HashSet<String>();

	public OntologyDatatype(String name, String equivalentType) throws Exception {
		this.name  = new OntologyName(name);
		this.addEquivalentType(equivalentType);
	}
	
	public String getName() {
		return this.name.getFullName();
	}
	
	public String getNameString(Boolean stripNamespace){
		if(stripNamespace){
			return this.name.getLocalName();
		}
		else{
			return this.name.getFullName();
		}
	}
	
	public void addEquivalentType(String equivalentType) throws Exception {
		this.strTypes.add(equivalentType);
		OntologyName typeName = new OntologyName(equivalentType);
		this.xsdTypes.add(XSDSupportedType.getMatchingValue(typeName.getLocalName()));
	}
	
	public HashSet<String> getEquivalentTypes() {
		return this.strTypes;
	}
	
	public HashSet<XSDSupportedType> getEquivalentXSDTypes() throws Exception {
		return this.xsdTypes;
	}
}
