package edu.columbia.cs.dataset;

import java.util.List;
import java.util.ArrayList;

import codemining.ast.TreeNode;

public class NodeForIcseData {
	public int nodeTypeOriginal = -99999;
	//public int nodeTypeModified = -99999;
	public int parentNodeTypeOriginal = -1;
	//public int parentNodeTypeModified = -1;
	public TreeNode<Integer> selfNode = null;
	public TreeNode<Integer> parentNode = null;
	public NodeForIcseData parent = null;
	public String text = null;
	public List<NodeForIcseData> children = new ArrayList<NodeForIcseData>();
	
	public int id = 0;
	
	public NodeForIcseData(NodeForIcseData other){
		this.nodeTypeOriginal = other.nodeTypeOriginal;
		this.parentNodeTypeOriginal = other.parentNodeTypeOriginal;
		this.selfNode = other.selfNode;
		this.parentNode = other.parentNode;
		this.parent = other.parent;
		this.text = other.text;
	}
	
	public NodeForIcseData(){
		
	}

}
