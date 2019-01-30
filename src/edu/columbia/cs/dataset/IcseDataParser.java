package edu.columbia.cs.dataset;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import codemining.ast.AstNodeSymbol;
import codemining.ast.TreeNode;
import codemining.ast.java.AbstractJavaTreeExtractor;
import codemining.ast.java.BinaryJavaAstTreeExtractor;
import codemining.ast.java.JavaAstTreeExtractor;
import edu.virginia.cs.gumtreetest.Argument;
import edu.virginia.cs.gumtreetest.DiffParser;
//import edu.virginia.cs.gumtreetest.NodePair;
import edu.virginia.cs.gumtreetest.Util;

public class IcseDataParser {
	public static Argument arg = null;
	private String parentFilePath = null;
	private String childfilePath = null;
	private String parentText = null;
	private String childText = null;
	private NodeForIcseData parentNode = null;
	private NodeForIcseData childNode = null;
	private String parentCodeString;
	private String parentTreeString;
	private String parentOrgTreeString;
	private String childCodeString;
	private String childTreeString;
	private String parentTypeCodeString;
	private String childTypeCodeString;
	private String childTypeTreeString;
	private String parentOriginalTypeTreeString;
	public IcseDataParser(String parentFile, String childFile, String srcText, String destText) {
		this.parentFilePath = parentFile;
		this.childfilePath = childFile;
		this.parentText = srcText;
		this.childText = destText;
		this.parentNode = processFileExtractTree(parentFilePath, parentText);
		this.childNode = processFileExtractTree(childfilePath, childText);
	}
	private NodeForIcseData processFileExtractTree(String filePath, String documentText) {
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

		Util.fixBinaryAST(rootNode);
		return rootNode.children.get(0).children.get(0);
	}
	public static void main(String[] args) throws FileNotFoundException {
		DateFormat stfmt = new SimpleDateFormat("MM/dd/yy hh:mm:ss");
		Date start = new Date();
		String startTime = stfmt.format(start);
		arg = Argument.preprocessArgument(args);
		Util.logln(arg);
		File outputFile = new File(arg.outputFilePath());
		if (outputFile.exists()) {
			Util.deleteDirectory(outputFile);
		}
		outputFile.mkdir();

		DateFormat fmt = new SimpleDateFormat("HH-mm-ss");
		Date d = new Date();
		PrintStream debugStream = new PrintStream(new File("debug-" + arg.maxChangeSize() + "-" + arg.maxTreeSize()
				+ "-" + arg.replace() + "-" + arg.astOnly() + "-" + fmt.format(d) + ".txt"));

		String allFileDirectory = arg.outputFilePath() ;
		File allFile = new File(allFileDirectory);
		if (!allFile.exists()) {
			allFile.mkdir();
		}
		int totalFileCount = 0;
		Scanner allFilePathsScanner = new Scanner(new File(arg.allPathsFile()));
		Map<String, List<DiffParser>> allParsedResults = new HashMap<String, List<DiffParser>>();
		while (allFilePathsScanner.hasNextLine()) {
			try {
				String filePath = allFilePathsScanner.nextLine().trim();
				//Util.logln(filePath);
				//Scanner filePathScanner = new Scanner(new File(filePath));
				//List<DiffParser> parserList = new ArrayList<DiffParser>();
				//while (filePathScanner.hasNextLine()) {
					try {
						String bothPath = filePath.trim();
						String[] filePathParts = bothPath.split("\t");
						String parentFile = filePathParts[0];
						String childFile = filePathParts[1];
							
						String srcText = Util.readFile(parentFile);
						String destText = Util.readFile(childFile);
						//Util.logln();
						Util.logln(parentFile);
						IcseDataParser parser = new IcseDataParser(parentFile, childFile, srcText, destText);
						boolean successfullyParsed = parser.checkSuccessFullParse(parser.parentNode, parser.childNode,
								arg.replace(), arg.excludeStringChange());
						if (successfullyParsed) {
							Date current = new Date();
							String cTime = stfmt.format(current);
							Util.logln(startTime + " -> " + cTime + "\t" + totalFileCount);
							printDataToDirectory(allFileDirectory, Arrays.asList(new IcseDataParser[] { parser }));
							totalFileCount++;
						}
						Util.dfsPrint(parser.parentNode);
						Util.dfsPrint(parser.childNode);
					} catch (Exception localException) {
						localException.printStackTrace();
					}
				//}
				//debugStream.println(filePath + " " + parserList.size());
				debugStream.flush();
				//Util.logln(filePath);
				//printTrainAndTestData(parserList);
				//filePathScanner.close();
				//allParsedResults.put(filePath, parserList);
			} catch (Exception localException1) {
			}
		}
		allFilePathsScanner.close();
		debugStream.close();
	}
	private static void printDataToDirectory(String allFileDirectory, List<IcseDataParser> parsers) {
		// TODO Auto-generated method stub
		
	}
	private boolean checkSuccessFullParse(NodeForIcseData parentNode, NodeForIcseData childNode, boolean replace, boolean excludeStringChange) {
		try {
			boolean success = true;

			this.parentCodeString = getParentCodeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.parentTreeString = getParentTreeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.parentOrgTreeString = getParentOrgTreeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)",
					"");
			this.childCodeString = getChildCodeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.childTreeString = getChildTreeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.parentTypeCodeString = getParentTypeCodeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.childTypeCodeString = getChildTypeCodeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.childTypeTreeString = getChildTypeTreeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.parentOriginalTypeTreeString = getParentOriginalTypeTreeString()
					.replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			if ((this.parentCodeString == null) || (this.parentTreeString == null) || (this.childCodeString == null)
					|| (this.childTreeString == null)) {
				return false;
			}
			if ((excludeStringChange) && (this.parentCodeString.compareTo(this.childCodeString) == 0)) {
				return false;
			}
		} catch (Exception ex) {
			return false;
		}
		return true;
	}
	
	private String getParentOriginalTypeTreeString() {
		// TODO Auto-generated method stub
		return null;
	}
	private String getChildTypeTreeString() {
		// TODO Auto-generated method stub
		return null;
	}
	private String getChildTypeCodeString() {
		// TODO Auto-generated method stub
		return null;
	}
	private String getParentTypeCodeString() {
		// TODO Auto-generated method stub
		return null;
	}
	private String getChildTreeString(boolean replace) {
		// TODO Auto-generated method stub
		return null;
	}
	private String getChildCodeString(boolean replace) {
		// TODO Auto-generated method stub
		return null;
	}
	private String getParentOrgTreeString(boolean replace) {
		// TODO Auto-generated method stub
		return null;
	}
	private String getParentTreeString(boolean replace) {
		// TODO Auto-generated method stub
		return null;
	}
	private String getParentCodeString(boolean replace) {
		return null;//Util.getCodeRecusrsive(this.parentNode, replace);
	}

}
