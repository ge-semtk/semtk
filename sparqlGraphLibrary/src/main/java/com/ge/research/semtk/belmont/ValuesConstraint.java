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

package com.ge.research.semtk.belmont;

import java.util.ArrayList;

/**
 * Parent ValueConstraint is synced with legacy javascript.
 * This is a clean implementation of VALUES constraints.
 *
 */
public class ValuesConstraint extends ValueConstraint {
	private Returnable item = null;
	private ArrayList<String> values = new ArrayList<String>();
	
	public ValuesConstraint(Returnable item) {
		this.item = item;
	}
	
	public ValuesConstraint(Returnable item, String value) throws Exception {
		this.item = item;
		this.addValue(value);
	}
	
	public void clearValues() {
		this.values.clear();
		this.constraint = "";
	}
	
	public void addValue(String value) throws Exception {
		values.add(value);
		this.update();
	}
	
	/**
	 * Keep legacy parent class's this.constraint string up-to-date
	 * @throws Exception
	 */
	private void update() throws Exception {
		this.constraint = ValueConstraint.buildValuesConstraint(this.item, this.values);
	}
}
