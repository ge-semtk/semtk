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

import java.util.ArrayList;

public class AnnotatableElement {
	ArrayList<String> comments = new ArrayList<String>();
	ArrayList<String> labels = new ArrayList<String>();
	
	public AnnotatableElement() {
		
	}
	
	/**
	 * Add comment, silently ignoring duplicates, nulls, isEmpty
	 * @param label
	 */
	public void addAnnotationComment(String comment) {
		if (comment != null && ! comment.isEmpty() && ! this.comments.contains(comment)) {
			this.comments.add(comment);
		}
	}
	
	/**
	 * Add label, silently ignoring duplicates, nulls, isEmpty
	 * @param label
	 */
	public void addAnnotationLabel(String label) {
		if (label != null && ! label.isEmpty() && ! this.labels.contains(label)) {
			this.labels.add(label);
		}
	}
	
	public ArrayList<String> getAnnotationComments() {
		return comments;
	}

	public ArrayList<String> getAnnotationLabels() {
		return labels;
	}

	/**
	 * Return ; separated list of comments, or ""
	 * @return
	 */
	public String getAnnotationCommentsString() {
		StringBuilder ret = new StringBuilder("");
		for (int i=0; i < this.comments.size(); i++) {
			ret.append( (i>0 ? " ; " : "") + this.comments.get(i));
		}
		return ret.toString();
	}
	
	/**
	 * Return ; separated list of comments, or ""
	 * @return
	 */
	public String getAnnotationLabelsString() {
		StringBuilder ret = new StringBuilder("");
		for (int i=0; i < this.labels.size(); i++) {
			ret.append( (i>0 ? " ; " : "") + this.labels.get(i));
		}
		return ret.toString();
	}
	
	/**
     * Generate <rdfs:label> and <rdfs:comment> lines for an element
     * @param elem
     * @return
     */
    public String generateAnnotationRdf(String tab) {
    	StringBuilder sb = new StringBuilder("");
    	
    	for (int i=0; i < this.comments.size(); i++) {
    		sb.append(tab).append("<rdfs:comment>").append(comments.get(i)).append("</rdfs:comment>\n");
    	}
    	
    	for (int i=0; i < this.labels.size(); i++) {
    		sb.append(tab).append("<rdfs:label>").append(labels.get(i)).append("</rdfs:label>\n");
    	}
    	
    	return sb.toString();
    }
    
    public String generateAnnotationsSADL() {
    	StringBuilder sb = new StringBuilder("");
    	
    	for (int i=0; i < this.comments.size(); i++) {
    		sb.append("(note \"").append(comments.get(i)).append("\")");
    	}
    	
    	for (int i=0; i < this.labels.size(); i++) {
    		sb.append("(alias \"").append(labels.get(i)).append("\")");
    	}
    	
    	return sb.toString();
    }
}
