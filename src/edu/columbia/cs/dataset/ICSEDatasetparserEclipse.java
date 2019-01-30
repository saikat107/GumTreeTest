package edu.columbia.cs.dataset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import com.github.gumtreediff.tree.ITree;

import codemining.ast.AstNodeSymbol;
import codemining.ast.TreeNode;
import codemining.ast.TreeNode.NodeParents;
import codemining.ast.java.AbstractJavaTreeExtractor;
import codemining.ast.java.BinaryJavaAstTreeExtractor;
import codemining.ast.java.JavaAstTreeExtractor;
import edu.virginia.cs.gumtreetest.Util;

public class ICSEDatasetparserEclipse {
	public static void main(String[] args) throws Exception {
		String filePath = "data/Parent1.java";
		String documentText = Util.readFile(filePath);	
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		Map<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		parser.setCompilerOptions(options);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(documentText.toCharArray());
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		String[] sources = { "" };
		String[] classpath = { System.getProperty("java.home") + "/lib/rt.jar" };
		parser.setUnitName(filePath);
		parser.setEnvironment(classpath, sources, new String[] { "UTF-8" }, true);
		CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		ASTNode root = cu.getRoot();
		JavaAstTreeExtractor extractorBase = new JavaAstTreeExtractor();
		BinaryJavaAstTreeExtractor extractorBinary = new BinaryJavaAstTreeExtractor(extractorBase);
		
		AbstractJavaTreeExtractor ext = extractorBinary;
		TreeNode<Integer> finalTree = ext.getTree(root);

		NodeForIcseData rootNode = new NodeForIcseData();
		rootNode.parentNodeTypeOriginal = -9999999;
		Stack<NodeForIcseData> myStack = new Stack<NodeForIcseData>();
		myStack.push(rootNode);
		Stack<TreeNode<Integer>> nodes = new Stack<>();
		nodes.push(finalTree);
		while(!nodes.isEmpty()){
			TreeNode<Integer> curr = nodes.pop();
			int nt = curr.getData();
			AstNodeSymbol sym = ext.getSymbol(nt);
			String value = "";
			for(String proName : sym.getSimpleProperties()){
				if (proName == "booleanValue" || !(sym.getSimpleProperty(proName) instanceof Boolean)){
					value += sym.getSimpleProperty(proName);
				}
			}
			NodeForIcseData currNode = myStack.pop();
			currNode.nodeTypeOriginal = sym.nodeType;
			currNode.selfNode = curr;
			currNode.text = value;
			List<List<TreeNode<Integer>>> children =  curr.getChildrenByProperty();
			int sz = children.size();
			currNode.children = new ArrayList<NodeForIcseData>();
			for(int cid = sz - 1; cid >= 0; cid--){
				List<TreeNode<Integer>> ch  = children.get(cid);
				int cz = ch.size();
				for(int idx = cz-1;  idx >=0; idx --){
					TreeNode<Integer> child = ch.get(idx);
					nodes.push(child);
					NodeForIcseData childNode = new NodeForIcseData();
					childNode.parentNodeTypeOriginal = sym.nodeType;
					childNode.parentNode = curr;
					childNode.parent = currNode;
					currNode.children.add(childNode);
					myStack.push(childNode);
				}
			}
		}

		Util.fixBinaryAST(rootNode.children.get(0).children.get(0));
		Util.dfsPrint(rootNode.children.get(0).children.get(0));
		/*
		String code = extractorBinary.getCodeFromTree(finalTree);
		Util.logln(code);
		Stack<ASTNode> nodes1 = new Stack<>();
		Stack<Integer> level1 = new Stack<>();
		nodes1.push(root);
		level1.push(0);
		while(!nodes1.isEmpty()){
			ASTNode node = nodes1.pop();
			int l = level1.pop();
			for(int i = 0; i < l; i++){
				System.out.print('\t');
			}
			System.out.println(node.getNodeType() + " " + node.getLength());
			List list = node.structuralPropertiesForType();
			for (int i = 0; i < list.size(); i++) {
				StructuralPropertyDescriptor curr = (StructuralPropertyDescriptor) list.get(i);
				Object child = node.getStructuralProperty(curr);
				if (child instanceof ASTNode) {
					nodes1.push((ASTNode)child);
					level1.push(l+1);
				}
				else if (child instanceof List) {
					List children = (List) child;
					for (Object el : children) {
						if (el instanceof ASTNode) {
							nodes1.push((ASTNode)el);
							level1.push(l+1);
						}
					}
				}
			}
		}*/
	}

}
