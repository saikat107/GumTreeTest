package edu.virginia.cs.gumtreetest.type;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import java.util.Scanner;
import java.util.TreeMap;

import edu.virginia.cs.gumtreetest.Argument;
import edu.virginia.cs.gumtreetest.Util;

public class TypeExtractor extends ASTVisitor{
	CompilationUnit cu = null;
	String fileText = "";
	static Argument arg;
	Map<Integer, String> positionToTypeMap = new TreeMap<>();
	public TypeExtractor(String fileName) throws IOException{
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		fileText = readFile(fileName);
		Map<String, String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		parser.setCompilerOptions(options);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(fileText.toCharArray());
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		String[] sources = { "" };
		String[] classpath = { System.getProperty("java.home") + "/lib/rt.jar" };
		parser.setUnitName(fileName);
		parser.setEnvironment(classpath, sources, new String[] { "UTF-8" }, true);
		cu = (CompilationUnit) parser.createAST(null);
		cu.accept(this);
		ASTNode root = cu.getRoot();
		extractPotisionToType(root);
	}
	private String readFile(String fileName) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String fileText = "";
		int c = 0;
		while((c = reader.read()) != -1){
			fileText += ((char) c);
		}
		reader.close();
		return fileText;
	}
	
	@SuppressWarnings("rawtypes")
	private void extractPotisionToType(ASTNode root) {
		String type = "";
		if (root.getNodeType() == ASTNode.SIMPLE_NAME) {
			String nodeBody = root.toString().trim();
			ITypeBinding binding = ((SimpleName) root).resolveTypeBinding();
			if (binding != null)
				type = binding.getQualifiedName(); 
			
			if(nodeBody.trim().compareTo(type) != 0){
				if(! type.trim().endsWith(nodeBody)){
					type = type + "_VAR";
				}
				else{
					type = nodeBody;
				}
			}
			int pos = root.getStartPosition();
			int p = pos + root.getLength();
			while(p < fileText.length()-1){
				char ch = fileText.charAt(p);
				if(ch == ' ' || ch == '\t' || ch == '\n'){
					p++;
					continue;
				}
				else if(ch == '('){
					type = "method_name";
					break;
				}
				else{
					break;
				}
			}
			type = type.replaceAll(" ", "");
			positionToTypeMap.put(pos, type);
		}
		List list = root.structuralPropertiesForType();
		if(list.size() != 0){
			for (int i = 0; i < list.size(); i++) {
				StructuralPropertyDescriptor curr = (StructuralPropertyDescriptor) list.get(i);
				Object child = root.getStructuralProperty(curr);
				if (child instanceof ASTNode) {
					extractPotisionToType((ASTNode)child);
				}
				else if (child instanceof List) {
					List children = (List) child;
					for (Object el : children) {
						if (el instanceof ASTNode) {
							extractPotisionToType((ASTNode)el);
						}
					}
				}
			}
		}
	}
	
	public Map<Integer, String> getDataTypeMap(){
		return positionToTypeMap;
	}
	
	public static void main(String[] args) throws Exception {

		arg = Argument.preprocessArgument(args);
		String filePaths = arg.srcFilePath();
		Scanner scan = new Scanner(new File(filePaths));
		while(scan.hasNextLine()){
			String fileName = scan.nextLine();
			TypeExtractor ext = new TypeExtractor(fileName);
			Map<Integer, String> map = ext.positionToTypeMap;
			for(Integer pos : map.keySet()){
				Util.logln(pos + " " + map.get(pos));
			}
		}
		scan.close();
	}

}
