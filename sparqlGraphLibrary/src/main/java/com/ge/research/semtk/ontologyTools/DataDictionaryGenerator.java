package com.ge.research.semtk.ontologyTools;

import java.util.ArrayList;
import java.util.Collections;

import com.ge.research.semtk.resultSet.Table;

/**
 * A utility class to generate data dictionaries from an ontology.
 */
public class DataDictionaryGenerator {

	/**
	 * Generate a tabular report describing this ontology
	 * @param oInfo the ontology info object
	 * @param stripNamespace true to remove namespaces from classes and properties
	 */
	public static Table generate(OntologyInfo oInfo, boolean stripNamespace) throws Exception{
		
		String[] cols = {"CLASS","PROPERTY","DATA TYPE","LABEL(S)","COMMENT(S)"};
		String[] colTypes = {"String","String","String","String","String"};
		Table table = new Table(cols, colTypes);
		
		ArrayList<String> classNames = oInfo.getClassNames();
		Collections.sort(classNames);
		for(String className : classNames){

			OntologyClass classs = oInfo.getClass(className);

			// add a row for the class 
			ArrayList<String> row = new ArrayList<String>();
			row.add(classs.getNameString(stripNamespace));
			row.add("");
			row.add(oInfo.classIsEnumeration(className) ? "enumeration: " + oInfo.getEnumerationStrings(className) : "");
			row.add(classs.getAnnotationLabelsString());
			row.add(classs.getAnnotationCommentsString());
			table.addRow(row);
			
			// add a row for each property (inherited or otherwise) of the class
			for(OntologyProperty p : oInfo.getInheritedProperties(classs)){		

				// flag if a property is shared by multiple domains
				// (shared properties should not use different labels/comments in different domains, because OWL does not differentiate)
				String sharedPropertyWarning = oInfo.getPropertyDomain(p).size() <= 1 ? "" : " (SHARED PROPERTY)"; 
						
				row = new ArrayList<String>();
				row.add(classs.getNameString(stripNamespace));
				row.add(p.getNameStr(stripNamespace) + sharedPropertyWarning);
				row.add(p.getRangeStr(stripNamespace));
				row.add(p.getAnnotationLabelsString() + (p.getAnnotationLabelsString().isEmpty() ? "" : sharedPropertyWarning));
				row.add(p.getAnnotationCommentsString() + (p.getAnnotationCommentsString().isEmpty() ? "" : sharedPropertyWarning));
				table.addRow(row);
			}
		}
		return table;
	}
	
}
