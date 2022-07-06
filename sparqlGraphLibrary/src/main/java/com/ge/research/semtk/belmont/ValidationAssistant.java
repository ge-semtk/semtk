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
import java.util.HashSet;

import com.ge.research.semtk.ontologyTools.OntologyClass;
import com.ge.research.semtk.ontologyTools.OntologyInfo;
import com.ge.research.semtk.ontologyTools.OntologyProperty;
import com.ge.research.semtk.ontologyTools.OntologyRange;

public class ValidationAssistant {
	
	/**
	 * Return a list of classes a node could be changed to, 
	 * starting with those with fewest validation errors in the nodegroup.
	 * @param oInfo - the OntologyInfo
	 * @param ng - deflated Nodegroup
	 * @param itemStr indicating which node
	 * @return ArrayList<String> where each entry is "full_class_uri,err_count" where err_count of 0 keeps the nodegroup valid
	 * @throws Exception
	 */
	public static ArrayList<String> suggestNodeClass(OntologyInfo oInfo, NodeGroup ng, NodeGroupItemStr itemStr) throws Exception {
		
		assert(itemStr.getType() == Node.class);
		
		Node snode = itemStr.getSnode();
		OntologyClass oClass = oInfo.getClass(snode.getUri());
		
		// get return array and score everything 0
		ArrayList<String> ret = oInfo.getClassNames();
		HashMap<String, Integer> errCount = new HashMap<String,Integer>();
		for (String name : oInfo.getClassNames()) {
			errCount.put(name, 0);
		}
		
		// look through incoming edges.  
		ArrayList<NodeItem> incoming = ng.getConnectingNodeItems(snode);
		for (NodeItem nItem : incoming) {
			
			// don't presume nItem.getRangeUris is validated.  Check oInfo instead.
			// skip this if incoming edge isn't valid
			Node incomingNode = ng.getNodeItemParentSNode(nItem);
			OntologyClass incomingOClass = oInfo.getClass(incomingNode.getUri());
			OntologyProperty nItemOProp = oInfo.getProperty(nItem.getUriConnectBy());
			
			// if class exists and has the property, then suggest the ranges
			if (incomingOClass != null && nItemOProp != null && incomingOClass.getProperty(nItemOProp.getNameStr()) != null) {
		
				OntologyRange oRange = nItemOProp.getRange(incomingOClass, oInfo);
				
				// loop through oInfo and
				// Add one to errCount of any that isn't in the range of incoming
				for (String classUri : oInfo.getClassNames()) {
					if (! oInfo.classIsInRange(oInfo.getClass(classUri), oRange)) {
						errCount.put(classUri, errCount.get(classUri) + 1);
					}
				}
			}
		}
		
		// get URI for each data property that is used
		// (presuming the nodegroup was properly deflated, all existing are in use)
		for (PropertyItem pItem : snode.getPropertyItems()) {
			
			OntologyProperty pItemOProp = oInfo.getProperty(pItem.getUriRelationship());
			
			// loop through oInfo and
			// Add one to errCount of any class that doesn't have this property
			for (String classUri : oInfo.getClassNames()) {
				if (! oInfo.getInheritedProperties(oInfo.getClass(classUri)).contains(pItemOProp)) {
					errCount.put(classUri, errCount.get(classUri) + 1);
				}
			}
		}
		
		// get URI for each object property that is used
		// (presuming the nodegroup was properly deflated, all existing are in use)
		for (NodeItem nItem : snode.getNodeItemList()) {
			OntologyProperty nItemOProp = oInfo.getProperty(nItem.getUriConnectBy());
			
			int numConnected = nItem.getNodeList().size();
			
			for (String classUri : oInfo.getClassNames()) {
				// If class doesn't have this object prop,
				// Add one to errCount for each connected node
				if (! oInfo.getInheritedProperties(oInfo.getClass(classUri)).contains(nItemOProp)) {
					errCount.put(classUri, errCount.get(classUri) + numConnected);
				} else {

					// for each node connected by this object prop
					for (Node n : nItem.getNodeList()) {
						// increment errCount if when class has this property
						// but the node isn't in the range
						OntologyRange oRange = nItemOProp.getRange(oInfo.getClass(classUri), oInfo);
						if (!oInfo.classIsInRange(oInfo.getClass(n.getFullUriName()), oRange)) {
							errCount.put(classUri, errCount.get(classUri) + 1);
						}
					}
				}
			}
		}
		
		HashMap<String, Integer> superSubHash = new HashMap<String,Integer>();
		for (String classUri : oInfo.getClassNames()) {
			if (oInfo.isSubclassOf(classUri, snode.getUri())) {
				superSubHash.put(classUri, 1);
			} else if (oInfo.isSubclassOf(snode.getUri(), classUri)) {
				superSubHash.put(classUri, 2);
			} else {
				superSubHash.put(classUri, 3);
			}
		}
		
		// sort by errCount ascending, super/sub-class, classUri
		ret.sort(new Comparator<String>() {
		    @Override
		    public int compare(String o1, String o2) {
		    	if (errCount.get(o1) != errCount.get(o2)) {
		    		return errCount.get(o1).compareTo(errCount.get(o2));
		    	} else if (superSubHash.get(o1) != superSubHash.get(o2)) {
		    		return superSubHash.get(o1).compareTo(superSubHash.get(o2));
		    	} else {
		    		return (o1.compareTo(o2));
		    	}
		    }
		});
		
		// remove self from list
		ret.remove(snode.getUri());
		
		// add ",errCount"
		ArrayList<String> ret2 = new ArrayList<String>();
		for (String r : ret) {
			ret2.add(r + "," + errCount.get(r));
		}
		return ret2;
		
	}
	
	public static String getSuggestionClass(String suggestion) {
		return suggestion.split(",")[0];
	}
	
	public static int getSuggestionErrorCount(String suggestion) {
		return Integer.valueOf(suggestion.split(",")[1]);
	}
	
	/**
	 * Rather hacked-together (?) original version
	 * @param oInfo
	 * @param ng
	 * @param itemStr
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<String> suggestNodeClassOriginal(OntologyInfo oInfo, NodeGroup ng, NodeGroupItemStr itemStr) throws Exception {
		
		assert(itemStr.getType() == Node.class);
		
		Node snode = itemStr.getSnode();
		OntologyClass oClass = oInfo.getClass(snode.getUri());
		
		// get return array and score everything 0
		ArrayList<String> ret = oInfo.getClassNames();
		HashMap<String, Integer> score = new HashMap<String,Integer>();
		for (String name : oInfo.getClassNames()) {
			score.put(name, 0);
		}
		
		// look through incoming edges.  Score above 1000, grouped by class hierarchy
		int localScore = 1000;
		ArrayList<NodeItem> incoming = ng.getConnectingNodeItems(snode);
		for (NodeItem nItem : incoming) {
			// don't presume nItem.getRangeUris is validated.  Check oInfo instead.
			Node incomingNode = ng.getNodeItemParentSNode(nItem);
			OntologyClass incomingClass = oInfo.getClass(incomingNode.getUri());
			OntologyProperty nItemOProp = oInfo.getProperty(nItem.getUriConnectBy());
			
			// if class exists and has the property, then suggest the ranges
			if (incomingClass != null && incomingClass.getProperty(nItemOProp.getNameStr()) != null) {
			
				OntologyRange oRange = nItemOProp.getRange(incomingClass, oInfo);
		
				for (String range : oRange.getUriList()) {
					if (oInfo.classIsA(oClass, oInfo.getClass(range))) {
						// add incoming node range
						score.put(range, ++localScore);
						// also add range's superclasses
						
						for (String rangeSuper : oInfo.getSuperclassNames(range)) {
							score.put(rangeSuper, ++localScore);
						}
					}
				}
			}
		}
		
		// get URI for each property and node item
		ArrayList<String> snodeProps = new ArrayList<String>();
		for (PropertyItem pItem : snode.getPropertyItems()) {
			snodeProps.add(pItem.getUriRelationship());
		}
		for (NodeItem nItem : snode.getNodeItemList()) {
			snodeProps.add(nItem.getUriConnectBy());
		}
		
		// look for other classes in ontology that share props/nodes
		// score each class
		for (String uri : oInfo.getClassNames()) {
			
			// get string list of inherited properties
			ArrayList<OntologyProperty> otherOntProps = oInfo.getInheritedProperties(oInfo.getClass(uri));
			ArrayList<String> otherProps = new ArrayList<String>();
			for (OntologyProperty op : otherOntProps) {
				otherProps.add(op.getNameStr());
			}
			
			// cross-compare in both directions
			int match = 0;
			int mismatch = 0;
			for (String p : otherProps) {
				if (snodeProps.contains(p)) {
					match += 1;
				} else {
					mismatch += 1;
				}
			}
			for (String p : snodeProps) {
				if (otherProps.contains(p)) {
					match += 1;
				} else {
					mismatch += 1;
				}
			}
			match /= 2; // each match was counted twice
			
			Integer s = (match * 100) / Math.max(1, match + mismatch);  
			if (s > score.get(uri)) {
				score.put(uri, s);
			}
		}
		
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
