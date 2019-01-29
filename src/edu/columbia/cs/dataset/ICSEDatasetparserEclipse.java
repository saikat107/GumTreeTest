package edu.columbia.cs.dataset;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import com.github.gumtreediff.tree.ITree;

import codemining.ast.TreeNode;
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
		TreeNode<Integer> finalTree = extractorBase.getTree(root);
		//Stack<TreeNode<Integer>> st = new Stack<TreeNode<Integer>>();
		
		
		Stack<TreeNode<Integer>> nodes = new Stack<>();
		Stack<Integer> level = new Stack<>();
		nodes.push(finalTree);
		level.push(0);
		while(!nodes.isEmpty()){
			TreeNode<Integer> curr = nodes.pop();
			int l = level.pop();
			for(int i = 0; i < l; i++){
				System.out.print('\t');
			}
			System.out.print(curr.getData() + " " );
			System.out.println(extractorBase.getCodeFromTree(curr).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", ""));
			List<List<TreeNode<Integer>>> children =  curr.getChildrenByProperty();
			int sz = children.size();
			for(int cid = sz - 1; cid >= 0; cid--){
				List<TreeNode<Integer>> ch  = children.get(cid);
				int cz = ch.size();
				for(int idx = 0;  idx <cz; idx ++){
					TreeNode<Integer> child = ch.get(idx);
					nodes.push(child);
					level.push(l+1);
				}
			}
		}
		
		
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
		}
	}

}
