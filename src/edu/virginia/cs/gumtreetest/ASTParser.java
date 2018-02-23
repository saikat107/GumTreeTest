package edu.virginia.cs.gumtreetest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.Stack;

import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

public class ASTParser {
	
	//private String filePath;
	ITree root;
	
	public ASTParser(String filePath) throws IOException{
		//this.filePath = filePath;
		Run.initGenerators();
		TreeContext fileContext = new JdtTreeGenerator().generateFromFile(filePath);
		root = fileContext.getRoot();
	}
	
	public ITree getRoot(){
		return this.root;
	}
	
	public Map<String, Set<List<String>>> getGrammar(Map<String, Set<List<String>>> grammar){		
		Stack<ITree> qu = new Stack<ITree>();
		qu.add(root);
		while(!qu.isEmpty()){
			ITree curr = qu.pop();
			String ruleHead = String.valueOf(curr.getType());
			Set<List<String>> rules = grammar.get(ruleHead);
			if(rules == null){
				rules = new HashSet<>();
			}
			List<ITree> children = curr.getChildren();
			List<String> currentRule = new ArrayList<>();
			if(children.size() != 0){
				for(ITree child : children){
					currentRule.add(String.valueOf(child.getType()));
					qu.add(child);
				}
			}
			else{
				currentRule.add("\"" + curr.getLabel() + "\"");
			}
			rules.add(currentRule);
			grammar.put(ruleHead, rules);
		}
		
		return grammar;
	}
	
	public static void main(String[] args) {
		Map<String, Set<List<String>>> grammar = new HashMap<>();
		List<String> allFiles = Util.getAllFiles("src\\edu\\virginia\\cs\\gumtreetest", true);
		Util.log(allFiles);
		try {
			for(String filePath : allFiles){
				if(filePath.endsWith(".java")){
					ASTParser parser = new ASTParser(filePath);
					grammar = parser.getGrammar(grammar);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Set<String> ruleHeads  = grammar.keySet();
		for(String head : ruleHeads){
			String ruleStr = head + " -> ";
			Set<List<String>> rules = grammar.get(head);
			for(List<String> rule : rules){
				for(String prod : rule){
					ruleStr += (prod + " ");
				}
				ruleStr += " | ";
			}
			Util.logln(ruleStr);
		}
	}

}
