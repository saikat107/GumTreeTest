package edu.columbia.cs.dataset;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.github.gumtreediff.tree.ITree;

import codemining.ast.AstNodeSymbol;
import codemining.ast.TreeNode;
import codemining.ast.java.AbstractJavaTreeExtractor;
import codemining.ast.java.BinaryJavaAstTreeExtractor;
import codemining.ast.java.JavaAstTreeExtractor;
import edu.virginia.cs.gumtreetest.Argument;
import edu.virginia.cs.gumtreetest.Config;
import edu.virginia.cs.gumtreetest.DiffParser;
import edu.virginia.cs.gumtreetest.TreeUtil;
//import edu.virginia.cs.gumtreetest.NodePair;
import edu.virginia.cs.gumtreetest.Util;

public class IcseDatasetParserWithCodeminingTool {
	public static Argument arg = null;
	private String parentFilePath = null;
	private String childfilePath = null;
	private String parentText = null;
	private String childText = null;
	private NodeForIcseData parentNode = null;
	private NodeForIcseData childNode = null;
	private NodeForIcseData parentBinarizedNode = null;
	private String parentCodeString;
	private String parentTreeString;
	private String parentOrgTreeString;
	private String childCodeString;
	private String childTreeString;
	private String parentTypeCodeString;
	private String childTypeCodeString;
	private String childTypeTreeString;
	private String parentOriginalTypeTreeString;
	private String allowedTokensString = "";
	private int nonLeafIdx = 200;
	private int currentIdx = 0;
	public IcseDatasetParserWithCodeminingTool(String parentFile, String childFile, String srcText, String destText) {
		this.parentFilePath = parentFile;
		this.childfilePath = childFile;
		this.parentText = srcText;
		this.childText = destText;
		this.parentNode = processFileExtractTree(parentFilePath, parentText);
		this.childNode = processFileExtractTree(childfilePath, childText);
		this.parentBinarizedNode = binarizeTree(this.parentNode);
		setIndices(this.parentBinarizedNode);
		extractAllowedToken();
		//Util.logln(getParentTreeString(false));
		//Util.logln(getParentOrgTreeString(false));
		//Util.logln(allowedTokensString);
		
	}
	
	private void extractAllowedToken() {
		Set<String> allowedTokens = extractAllVariablesInScope();
		for (String token : allowedTokens) {
			this.allowedTokensString = (this.allowedTokensString + token + " ");
		}
	}

	private NodeForIcseData binarizeTree(NodeForIcseData node) {
		return TreeUtil.binarizeAST(node);
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
			for(int cid = sz -1; cid >= 0; cid--){
				List<TreeNode<Integer>> ch  = children.get(cid);
				int cz = ch.size();
				for(int idx = cz -1;  idx >= 0; idx--){
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
		NodeForIcseData retNode = rootNode.children.get(0).children.get(0);
		return retNode;
	}
	
	private void setIndices(NodeForIcseData root) {
		if (root.children.size() == 0) {
			root.id = this.currentIdx;
			this.currentIdx += 1;
		} else {
			root.id = this.nonLeafIdx;
			this.nonLeafIdx += 1;
			for (NodeForIcseData child : root.children) {
				setIndices(child);
			}
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		DateFormat stfmt = new SimpleDateFormat("hh:mm:ss");
		Date start = new Date();
		String startTime = stfmt.format(start);
		arg = Argument.preprocessArgument(args);
		Util.logln(arg);
		File outputFile = new File(arg.outputFilePath());
		if (outputFile.exists()) {
			Util.deleteDirectory(outputFile);
		}
		outputFile.mkdirs();

		DateFormat fmt = new SimpleDateFormat("HH-mm-ss");
		Date d = new Date();
		//PrintStream debugStream = new PrintStream(new File("debug-" + arg.maxChangeSize() + "-" + arg.maxTreeSize()
		//		+ "-" + arg.replace() + "-" + arg.astOnly() + "-" + fmt.format(d) + ".txt"));

		String allFileDirectory = arg.outputFilePath() ;
		File allFile = new File(allFileDirectory);
		if (!allFile.exists()) {
			allFile.mkdirs();
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
						srcText = srcText.replaceAll(" class", " . class");
						destText = destText.replaceAll(" class", " . class");
						for(int idx = 0; idx < Util.PUNCTUATIONS.length; idx++) {
							String punc = Util.PUNCTUATIONS[idx];
							String rges = Util.REGEXES[idx];
							String fStr = " " + punc;
							String rFStre = " " + rges;
							if (srcText.contains(fStr)) {
								srcText = srcText.replaceAll(rFStre, punc);
							}
							if(destText.contains(fStr)) {
								destText = destText.replaceAll(rFStre, punc);
							}
						}
						//Util.logln();
						//Util.logln(parentFile);
						IcseDatasetParserWithCodeminingTool parser = new IcseDatasetParserWithCodeminingTool(parentFile, childFile, srcText, destText);
						boolean successfullyParsed = parser.checkSuccessFullParse(parser.parentNode, parser.childNode,
								arg.replace(), arg.excludeStringChange());
						if (successfullyParsed) {
							Date current = new Date();
							String cTime = stfmt.format(current);
							Util.logln(startTime + " -> " + cTime + "\t" + totalFileCount);
							printDataToDirectory(allFileDirectory, Arrays.asList(new IcseDatasetParserWithCodeminingTool[] { parser }));
							totalFileCount++;
						}
						//Util.dfsPrint(parser.parentNode);
						//Util.dfsPrint(parser.childNode);
					} catch (Exception localException) {
						localException.printStackTrace();
					}
				//}
				//debugStream.println(filePath + " " + parserList.size());
				//Util.logln(filePath);
				//printTrainAndTestData(parserList);
				//filePathScanner.close();
				//allParsedResults.put(filePath, parserList);
			} catch (Exception localException1) {
			}
		}
		allFilePathsScanner.close();
	}
	private static void printDataToDirectory(String baseDir, List<IcseDatasetParserWithCodeminingTool> parsers) {
		try {
			File baseDirFile = new File(baseDir);
			if (!baseDirFile.exists()) {
				baseDirFile.mkdir();
			}
			PrintStream parentCode = new PrintStream(new FileOutputStream(baseDir + "/parent.code", true));
			PrintStream parentTree = new PrintStream(new FileOutputStream(baseDir + "/parent.tree", true));
			PrintStream childCode = new PrintStream(new FileOutputStream(baseDir + "/child.code", true));
			PrintStream childTree = new PrintStream(new FileOutputStream(baseDir + "/child.tree", true));
			PrintStream parentOrgTree = new PrintStream(new FileOutputStream(baseDir + "/parent.org.tree", true));
			PrintStream parentTypeCode = new PrintStream(new FileOutputStream(baseDir + "/parent.type.code", true));
			PrintStream childTypeCode = new PrintStream(new FileOutputStream(baseDir + "/child.type.code", true));
			PrintStream parentTypeTree = new PrintStream(new FileOutputStream(baseDir + "/parent.org.type.tree", true));
			PrintStream childTypeTree = new PrintStream(new FileOutputStream(baseDir + "/child.type.tree", true));
			PrintStream tokenMasks = new PrintStream(new FileOutputStream(baseDir + "/allowed.tokens", true));
			PrintStream fileNames = new PrintStream(new FileOutputStream(baseDir + "/files.txt", true));
			for (IcseDatasetParserWithCodeminingTool parser : parsers) {
				parentCode.println(parser.parentCodeString);
				parentTree.println(parser.parentTreeString);
				childCode.println(parser.childCodeString);
				childTree.println(parser.childTreeString);
				parentOrgTree.println(parser.parentOrgTreeString);
				parentTypeCode.println(parser.parentTypeCodeString);
				childTypeCode.println(parser.childTypeCodeString);
				parentTypeTree.println(parser.parentOriginalTypeTreeString);
				childTypeTree.println(parser.childTypeTreeString);
				tokenMasks.println(parser.allowedTokensString);
				fileNames.println(parser.parentFilePath);
				flushAllPrintStreams(parentCode, parentTree, childCode, childTree, parentOrgTree, parentTypeCode,
						childTypeCode, parentTypeTree, childTypeTree, tokenMasks);
				fileNames.flush();
			}
			closeAllPrintStreams(parentCode, parentTree, childCode, childTree, parentOrgTree, parentTypeCode,
					childTypeCode, parentTypeTree, childTypeTree, tokenMasks);
			fileNames.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
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
	
	private String getParentCodeString(boolean replace) {
		return Util.getCodeRecusrsive(this.parentNode, replace);
	}
	
	private String getParentTreeString(boolean replace) {
		return Util.getSourceTree(this.parentBinarizedNode);
	}
	
	private String getParentOrgTreeString(boolean replace) {
		return Util.getDestTree(this.parentNode, replace);
	}
	
	private String getChildCodeString(boolean replace) {
		return Util.getCodeRecusrsive(this.childNode, replace);
	}
	
	private String getChildTreeString(boolean replace) {
		return Util.getDestTree(this.childNode, replace);
	}
	
	private String getParentTypeCodeString() {
		return Util.getCodeRecusrsive(this.parentNode, false);
	}
	
	private String getChildTypeCodeString() {
		return Util.getCodeRecusrsive(this.childNode, false);
	}
	
	private String getChildTypeTreeString() {
		return Util.getDestTree(this.childNode, false);
	}
	
	private String getParentOriginalTypeTreeString() {
		return Util.getDestTree(this.parentNode, false);
	}
	
	private static void closeAllPrintStreams(PrintStream parentCode, PrintStream parentTree, PrintStream childCode,
			PrintStream childTree, PrintStream parentOrgTree, PrintStream parentTypeCode, PrintStream childTypeCode,
			PrintStream parentTypeTree, PrintStream childTypeTree, PrintStream alloedTokens) {
		parentCode.close();
		parentTree.close();
		childCode.close();
		childTree.close();
		parentOrgTree.close();
		parentTypeCode.close();
		parentTypeTree.close();
		childTypeCode.close();
		childTypeTree.close();
		alloedTokens.close();
	}

	private static void flushAllPrintStreams(PrintStream parentCode, PrintStream parentTree, PrintStream childCode,
			PrintStream childTree, PrintStream parentOrgTree, PrintStream parentTypeCode, PrintStream childTypeCode,
			PrintStream parentTypeTree, PrintStream childTypeTree, PrintStream alloedTokens) {
		parentCode.flush();
		parentTree.flush();
		childCode.flush();
		childTree.flush();
		parentOrgTree.flush();
		parentTypeCode.flush();
		childTypeCode.flush();
		parentTypeTree.flush();
		childTypeTree.flush();
		alloedTokens.flush();
	}
	
	public Set<String> extractAllVariablesInScope() {
		Set<String> variables = new HashSet<String>();
			Stack<NodeForIcseData> st = new Stack<NodeForIcseData>();
			st.push(parentNode);
			st.push(childNode);
			while(!st.isEmpty()){
				NodeForIcseData curr = st.pop();
				if(curr.nodeTypeOriginal == Config.ASTTYPE_TAG.SIMPLE_NAME){
					String var = curr.text;
					variables.add(var);
				}
				for(NodeForIcseData child : curr.children){
					st.push(child);
				}
			}
		
		return variables;
	}
	
}
