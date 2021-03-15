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
package com.ge.research.semtk.belmont;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import com.ge.research.semtk.ontologyTools.OntologyInfo;

public class ValidationAssistant {
	
	static ArrayList<String> suggestNodeClass(OntologyInfo oInfo, NodeGroup ng, NodeGroupItemStr itemStr) {
		
		assert(itemStr.getType() == Node.class);
		Node snode = itemStr.getSnode();
		
		// get return array and score everything 0
		ArrayList<String> ret = oInfo.getClassNames();
		HashMap<String, Integer> score = new HashMap<String,Integer>();
		for (String name : oInfo.getClassNames()) {
			score.put(name, 0);
		}
		
		// look through incoming edges.  Score above 1000, grouped by class hier
		int localScore = 1000;
		ArrayList<NodeItem> incoming = ng.getConnectingNodeItems(snode);
		for (NodeItem nItem : incoming) {
			String range = nItem.getUriValueType();
			// add incoming node range
			score.put(range, ++localScore);
			// also add range's superclasses
			
			for (String rangeSuper : oInfo.getSuperclassNames(range)) {
				score.put(rangeSuper, ++localScore);
			}
		}
		
		// look for other classes in ontology that share props/nodes
		// HERE
		
		// sort by score descending
		ret.sort(new Comparator<String>() {
		    @Override
		    public int compare(String o1, String o2) {
		        return score.get(o2).compareTo(score.get(o1));
		    }
		});
		return ret;
	}
}
