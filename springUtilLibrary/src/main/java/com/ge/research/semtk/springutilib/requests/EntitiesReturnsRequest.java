package com.ge.research.semtk.springutilib.requests;

import java.util.ArrayList;

import com.ge.research.semtk.ontologyTools.ClassInstance;
import com.ge.research.semtk.ontologyTools.ReturnProperty;

import io.swagger.v3.oas.annotations.media.Schema;

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

public class EntitiesReturnsRequest {



	@Schema(
			name = "entities",
			required = true
			)	
	private ClassInstance[] entities;
	@Schema(
			name = "returns",
			required = true
			)	
	private ReturnProperty[] returns;

	public ClassInstance[] getEntities() {
		return entities;
	}

	public ArrayList<ClassInstance> buildEntitiesList() {
		ArrayList<ClassInstance> ret = new ArrayList<ClassInstance>();
		for (int i=0; i < this.entities.length; i++) {
			ret.add(this.entities[i]);
		}
		return ret;
	}
	
	public void setEntities(ClassInstance[] entities) {
		this.entities = entities;
	}
		
	public ReturnProperty[] getReturns() {
		return returns;
	}
	
	public ArrayList<ReturnProperty> buildReturnsList() {
		ArrayList<ReturnProperty> ret = new ArrayList<ReturnProperty>();
		for (int i=0; i < this.returns.length; i++) {
			ret.add(this.returns[i]);
		}
		return ret;
	}
	public void setReturns(ReturnProperty[] returns) {
		this.returns = returns;
	}
}