package com.ge.research.semtk.belmont;


/**
 * Class to help manage the string keys of NodeGroup's unionHash and tmpUnionMembershipsHash
 * Move back and forth between objects and a string representation.
 * @author 200001934
 *
 */
public class UnionKeyStr {
	String str = null;   // string representation
	
	Node snode = null;    // all the components optional except snode
	NodeItem nItem = null;
	PropertyItem pItem = null;
	Node target = null;
	Boolean reverseFlag = null;
	
	public UnionKeyStr(String str, NodeGroup ng) {
		this.str = str;
		String[] list = str.split("\\|");
		this.snode = ng.getNodeBySparqlID(list[0]);
		
		if (list.length == 2) {
			this.pItem = snode.getPropertyByURIRelation(list[1]);
			
		} else if (list.length > 2) {
			this.nItem = snode.getNodeItem(list[1]);
			this.target = ng.getNodeBySparqlID(list[2]);
			if (list.length > 3) {
				this.reverseFlag = Boolean.parseBoolean(list[3]);
			}
			
		}
	}

	public UnionKeyStr(Node snode, NodeItem item, Node target, Boolean reverseFlag) {
		this.snode = snode;
		this.nItem = item;
		this.target = target;
		this.reverseFlag = reverseFlag;
		this.str = snode.getSparqlID() + "|" + item.getUriConnectBy() + "|" + target.getSparqlID() + "|" + reverseFlag.toString();  
    }
	
	public UnionKeyStr(Node snode, NodeItem item, Node target) {
		this.snode = snode;
		this.nItem = item;
		this.target = target;
		this.str = snode.getSparqlID() + "|" + item.getUriConnectBy() + "|" + target.getSparqlID();
	}
	
	public UnionKeyStr(Node snode, PropertyItem item) {
		this.snode = snode;
		this.pItem = item;
		this.str = snode.getSparqlID() + "|" + item.getUriRelationship();
	}
	public UnionKeyStr(Node snode) {
		this.snode = snode;
		this.str = snode.getSparqlID();
    }
	
	public Class<?> getType() {
		if (nItem != null) return NodeItem.class;
		else if (pItem != null) return PropertyItem.class;
		else return Node.class;
	}

	public String getStr() {
		return this.str;
	}
	
	public String getStrNoRevFlag() {
		return rmRevFlag(this.str);
	}
	
	public static String rmRevFlag(String str) {
		if (str.endsWith("|true")) { 
			return str.substring(0, str.length()-5);
		} else if (str.endsWith("|false")) { 
			return str.substring(0, str.length()-6);
		} else {
			return str;
		}
	}

	public Node getSnode() {
		return snode;
	}

	public NodeItem getnItem() {
		return nItem;
	}

	public PropertyItem getpItem() {
		return pItem;
	}

	public Node getTarget() {
		return target;
	}

	public Boolean getReverseFlag() {
		return reverseFlag;
	}
	
}
