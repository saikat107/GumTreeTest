package edu.virginia.cs.gumtreetest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import edu.virginia.cs.gumtreetest.visitors.DataTypeVisitor;
import edu.virginia.cs.gumtreetest.visitors.VariableVisitor;

public class DiffParser {
	
	private static Argument arg;
	private  int nonLeafIdx = 200;
	private  int currentIdx = 0;
	private String srcPath;
	private String destPath;
	private String srcFileText;
	private String destFileText;
	private String allowedTokensString = "";
	private static double testPercentage = 0.20;
	List<ITree> srcCommonParent = null;
	List<ITree> destCommonParent = null;
	ITree cParentSrc = null;
	ITree cParentDest = null;
	ITree cParentOrg = null;
	private boolean alreadyParsed = false;
	private String parentCodeString = null;
	private String parentTreeString = null;
	private String parentOrgTreeString = null;
	private String childCodeString = null;
	private String parentTypeCodeString = null;
	private String childTreeString = null;
	private String childTypeCodeString = null;
	private String childTypeTreeString = null;
	private String parentOriginalTypeTreeString = null;
	
	public DiffParser(String srcPath, String destPath, String srcText, String destText) throws IOException{
		Run.initGenerators();
		this.srcPath = srcPath;
		this.destPath = destPath;
		currentIdx = 0;
		nonLeafIdx = 200;
		srcFileText = srcText;
		destFileText = destText;
		
//		parseASTDiff();
	}
	
	/**
	 * @author saikat
	 * @param srcDataWriter
	 * @param srcTreeWriter
	 * @param destCodeWriter
	 * @param destTreeWriter
	 */
	public void writeDiffs(PrintStream srcDataWriter, PrintStream srcTreeWriter, PrintStream destCodeWriter, PrintStream destTreeWriter){
		if(alreadyParsed){
			Util.printSourceTree(cParentSrc, srcDataWriter, srcTreeWriter);
			Util.printDestCodeAndTree(cParentDest, destCodeWriter, destTreeWriter);
		}
	}
	
	
	public static List<ITree> getMethodNodes(ITree root){
		List<ITree> methods = new ArrayList<ITree>();
		Stack<ITree> st = new Stack<ITree>();
		st.push(root);
		while(!st.isEmpty()){
			ITree curr = st.pop();
			if(curr.getType() == Config.ASTTYPE_TAG.METHOD_DECLARATION){
				methods.add(curr);
			}
			else{
				for(ITree child : curr.getChildren()){
					st.add(child);
				}
			}
		}
		return methods;
	}
	
	/**
	 * @author saikat
	 * @return
	 * @throws IOException
	 */
	public boolean parseASTDiff(ITree srcTree, ITree destTree) throws Exception{
		//# FIXME Extract all the variable that is in scope and and save a list along with the data 
		/*Util.dfsPrint(srcTree);
		Util.dfsPrint(destTree);
		Util.logln("\n\n\n");*/
		Matcher m = Matchers.getInstance().getMatcher(srcTree, destTree);
		m.match();
		MappingStore store = m.getMappings();		
		extractCommonParentsChainFromActions(srcTree, destTree, store);
		
		findCommonParentRoots(srcCommonParent, destCommonParent, store);
		if(cParentSrc == null || cParentDest == null){
			return false;
		}
		if(cParentSrc.getType() == Config.ASTTYPE_TAG.JAVADOC || cParentDest.getType() == Config.ASTTYPE_TAG.JAVADOC ||
				cParentSrc.getType() == Config.ASTTYPE_TAG.INSIDE_JAVADOC1 || cParentDest.getType() == Config.ASTTYPE_TAG.INSIDE_JAVADOC1 ||
				cParentSrc.getType() == Config.ASTTYPE_TAG.INSIDE_JAVADOC2 || cParentDest.getType() == Config.ASTTYPE_TAG.INSIDE_JAVADOC2){
			alreadyParsed = false;
			return false;
		}
		if(cParentSrc == null || cParentDest == null){
			return false;
		}
		int snc = TreeUtil.countNumberOfNodes(cParentSrc);
		int dnc = TreeUtil.countNumberOfNodes(cParentDest);
		//Util.logln("Src Node : " + snc + " Dest Node : " + dnc);
		if(snc > arg.maxChangeSize() || dnc > arg.maxChangeSize()){
			alreadyParsed = false;
			return false;
		}	
		/*if(arg.onlyMethodChange() && !Util.checkIfChangeBelongToAnyMethod(cParentSrc, cParentDest)){
			alreadyParsed = false;
			return false;
		}*/
		
		alreadyParsed = true;
		findCommonParentNode(srcCommonParent, destCommonParent, store);
		Map<String, String> sTable = VariableVisitor.getSymbolTable(cParentSrc);
		setMetaDataToDestTree(cParentSrc, cParentDest, store, sTable);
		// Util.logln(cParentSrc.toTreeString());
		cParentOrg = cParentSrc.deepCopy();
		TreeUtil.fixAST(cParentSrc, srcFileText, false);
		TreeUtil.fixAST(cParentOrg, srcFileText, arg.astOnly());
		TreeUtil.fixAST(cParentDest, destFileText, arg.astOnly()); //Destination AST does not need to be fixed
		TreeUtil.removeEmptyNode(cParentOrg);
		TreeUtil.removeEmptyNode(cParentDest);	
		List<String> allVariablesInMethod = new ArrayList<String>(Util.extractAllVariablesInScope(cParentSrc));
		for(String token : allVariablesInMethod){
			allowedTokensString += (token + " ");
		}
		try{
			
			cParentSrc = TreeUtil.binarizeAST(cParentSrc);
			setIndices(cParentSrc);	
			//int srcLeafNodeCount = currentIdx;
			currentIdx = 0;
			setIndices(cParentOrg);
			TreeUtil.setTreeMetadata(cParentSrc);
			TreeUtil.setTreeMetadata(cParentDest);
			TreeUtil.setTreeMetadata(cParentOrg);
			currentIdx = 0;
			setIndices(cParentDest);
			DataTypeVisitor.setDataTypes(cParentSrc, srcPath);
			DataTypeVisitor.setDataTypes(cParentDest, destPath);
			DataTypeVisitor.setDataTypes(cParentOrg, srcPath);
		}catch(Exception ex){
			Util.logln(ex.getMessage() + " " + srcPath); 
			alreadyParsed = false;
			return false;
		}
		return true;
	}
	
	
	private void setMetaDataToDestTree(ITree srcTree, ITree destTree, MappingStore store, Map<String, String> sTable) {
		Stack<ITree> st = new Stack<ITree>();
		st.push(destTree);
		while(!st.isEmpty()){
			ITree curr = st.pop();
			if(curr.getType() == Config.ASTTYPE_TAG.SIMPLE_NAME){
				String name = curr.getLabel();
				if(sTable.containsKey(name)){
//					Util.logln(name+ " " + sTable.get(name));
					curr.setMetadata("subs_name", sTable.get(name));
				}
				else{
						curr.setMetadata("subs_name", name);
//						Util.logln(name);
				}
			}
			st.addAll(curr.getChildren());
		}
	}

	/**
	 * @author saikat
	 * @param root
	 */
	private void setIndices(ITree root){
		if(root.getChildren().size() == 0){
			root.setId(currentIdx);
			currentIdx++;
		}
		else{
			root.setId(nonLeafIdx);
			nonLeafIdx++;
			for(ITree child: root.getChildren()){
				setIndices(child);
			}
		}
	}
	
	
	private void findCommonParentRoots(List<ITree> srcCommongParents, List<ITree> destCommonParentRoots, MappingStore store){
		if(srcCommongParents != null){
			for(ITree srcNode : srcCommongParents){
				ITree destNode = store.getDst(srcNode);
				if(destCommonParentRoots != null){
					if(destCommonParentRoots.contains(destNode)){
						cParentSrc = srcNode;
						cParentDest = destNode;
						break;
					}
				}
				else{
					cParentSrc = srcNode;
					cParentDest = destNode;
					break;
				}
			}
		}
		else if(destCommonParentRoots != null){
			for(ITree destNode : destCommonParentRoots){
				ITree srcNode = store.getSrc(destNode);
				if(srcCommongParents != null){
					if(srcCommongParents.contains(srcNode)){
						cParentSrc = srcNode;
						cParentDest = destNode;
						break;
					}
				}
				else{
					cParentSrc = srcNode;
					cParentDest = destNode;
					break;
				}
			}
		}
	}
	
	
	/**
	 * @author saikat
	 * @param srcCommonParent
	 * @param destCommonParent
	 * @param store
	 */
	private void findCommonParentNode(List<ITree> srcCommonParent, List<ITree> destCommonParent, MappingStore store) {
		if(srcCommonParent != null){
			for(ITree cSrcParent : srcCommonParent){
				ITree cDestParent = store.getDst(cSrcParent);
				if(cDestParent == null){
					continue;
				}
				if(destCommonParent == null){
					if(cDestParent != null){
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						int snc = TreeUtil.countNumberOfNodes(cParentSrc.getParent());
						int dnc = TreeUtil.countNumberOfNodes(cParentDest.getParent());
						//Util.logln(snc + " " + dnc);
						if(snc > arg.maxTreeSize() || dnc >  arg.maxTreeSize()){
							break;
						}
					}
					if(cSrcParent.getParent() == null || cDestParent.getParent() == null){
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						break;
					}
				}
				else if (destCommonParent.contains(cDestParent)){
					cParentSrc = cSrcParent;
					cParentDest = cDestParent;
					int snc = TreeUtil.countNumberOfNodes(cParentSrc.getParent());
					int dnc = TreeUtil.countNumberOfNodes(cParentDest.getParent());
					//Util.logln(snc + " " + dnc);
					if(snc > arg.maxTreeSize() || dnc >  arg.maxTreeSize()){
						break;
					}
					if(cSrcParent.getParent() == null || cDestParent.getParent() == null){
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						break;
					}
				}
			}
		}
		else if(destCommonParent != null){
			for(ITree cDestParent : destCommonParent){
				ITree cSrcParent = store.getSrc(cDestParent);
				if(cSrcParent == null){
					continue;
				}
				if(srcCommonParent == null){
					if(cSrcParent != null){
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						int snc = TreeUtil.countNumberOfNodes(cParentSrc.getParent());
						int dnc = TreeUtil.countNumberOfNodes(cParentDest.getParent());
						//Util.logln(snc + " " + dnc);
						if(snc > arg.maxTreeSize() || dnc >  arg.maxTreeSize()){
							break;
						}
					}
					if(cSrcParent.getParent() == null || cDestParent.getParent() == null){
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						break;
					}
				}
				else if (srcCommonParent.contains(cSrcParent)){
					cParentSrc = cSrcParent;
					cParentDest = cDestParent;
					int snc = TreeUtil.countNumberOfNodes(cParentSrc.getParent());
					int dnc = TreeUtil.countNumberOfNodes(cParentDest.getParent());
					Util.logln(snc + " " + dnc);
					if(snc > arg.maxTreeSize() || dnc >  arg.maxTreeSize()){
						break;
					}
					if(cSrcParent.getParent() == null || cDestParent.getParent() == null){
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						break;
					}
				}
				
			}
		}
		/*if(TreeUtil.countNumberOfNodes(cParentSrc) > MAXIMUM_ALLOWED_NODE || 
				TreeUtil.countNumberOfNodes(cParentDest) > MAXIMUM_ALLOWED_NODE){
			cParentSrc = null;
			cParentDest = null;
		}*/
	}

	/**
	 * @author saikat
	 * @param srcTree
	 * @param destTree
	 * @param store
	 */
	private void extractCommonParentsChainFromActions(ITree srcTree, ITree destTree, MappingStore store) throws Exception{
		ActionGenerator gen = new ActionGenerator(srcTree, destTree, store);
		gen.generate();
		List<Action> actions = gen.getActions();
		for(Action action : actions){
			ITree incidentNode = action.getNode();
			List<ITree> parents = incidentNode.getParents();
			
			if (action.getName() == "INS"){
				parents.add(0, action.getNode().getParent());
				if (destCommonParent == null){
					destCommonParent = parents;
				}
				else{
					destCommonParent = Util.getCommonParents(destCommonParent, parents);
				}
			}
			else if(action.getName() == "MOV"){
				if (srcCommonParent == null){
					srcCommonParent = parents;
				}
				else{
					srcCommonParent = Util.getCommonParents(srcCommonParent, parents);
				}
				ITree destNode = store.getDst(action.getNode());
				if(destNode == null){
					continue;
				}
				List<ITree> destParents = destNode.getParents();
				destParents.add(0, destNode);
				if (destCommonParent == null){
					destCommonParent = destParents;
				}
				else{
					destCommonParent = Util.getCommonParents(destCommonParent, destParents);
				}
			}
			else{
				parents.add(0, action.getNode().getParent());
				if (srcCommonParent == null){
					srcCommonParent = parents;
				}
				else{
					srcCommonParent = Util.getCommonParents(srcCommonParent, parents);
				}
			}
		}
	}
	
	
	
	public static void main(String[] args) throws UnsupportedOperationException, IOException {
		int numberOfAllFile = 0;
		arg = Argument.preprocessArgument(args);
		Util.logln(arg);
		File outputFile = new File(arg.outputFilePath());
		if(outputFile.exists()){
			Util.deleteDirectory(outputFile);
		}
		PrintStream debugStream = new PrintStream(new File("debug.txt"));
		outputFile.mkdir();
		String allFileDirectory = arg.outputFilePath() + "/all";
		File allFile = new File(allFileDirectory);
		if(!allFile.exists()) {
			allFile.mkdir();
		}
		
		Scanner allFilePathsScanner = new Scanner(new File(arg.allPathsFile()));
		Map<String, List<DiffParser>> allParsedResults = new HashMap<String, List<DiffParser>>();
		while(allFilePathsScanner.hasNextLine()){
			String filePath = allFilePathsScanner.nextLine().trim();
			Scanner filePathScanner = new Scanner(new File(filePath));
			List<DiffParser> parserList = new ArrayList<DiffParser>();
			// #TODO Print after every project is finished. 
			while(filePathScanner.hasNextLine()){
				String bothPath = filePathScanner.nextLine().trim();
				String []filePathParts = bothPath.split("\t");
				String parentFile = filePathParts[0];
				String childFile = filePathParts[1];
				//Util.logln(parentFile);
				String srcText = Util.readFile(parentFile);
				String destText = Util.readFile(childFile);
				TreeContext srcContext = new JdtTreeGenerator().generateFromFile(parentFile);
				TreeContext destContext = new JdtTreeGenerator().generateFromFile(childFile);
				ITree srcTree = srcContext.getRoot();
				ITree destTree = destContext.getRoot();
				List<NodePair> methodPairs = getMethodPairs(srcTree, destTree, srcText, destText);
				for(NodePair pair : methodPairs){
					DiffParser parser = new DiffParser(parentFile, childFile, srcText, destText);
					boolean successfullyParsed = parser.checkSuccessFullParse(
							pair.srcNode, pair.tgtNode, arg.replace(), arg.excludeStringChange());
					if(successfullyParsed){
						parserList.add(parser);
						numberOfAllFile++;
						printDataToDirectory(allFileDirectory, Arrays.asList(parser));
						Util.logln(numberOfAllFile);
					}
				}
			}
			debugStream.println(filePath + " " + parserList.size());
			debugStream.flush();
			Util.logln(filePath);
			printTrainAndTestData(parserList);
			filePathScanner.close();
			allParsedResults.put(filePath, parserList);
		}
		allFilePathsScanner.close();
		debugStream.close();
//		printTrainAndTestData(allParsedResults);
	}
	

	private static List<NodePair> getMethodPairs(ITree srcTree, ITree destTree, String srcText, String destText) {
		Matcher m = Matchers.getInstance().getMatcher(srcTree, destTree);
		m.match();
		MappingStore store = m.getMappings();
		
		List<ITree> methodNodes = getMethodNodes(srcTree);
		
		List<NodePair> methods = new ArrayList<NodePair>();
		
		for(ITree method : methodNodes){
			ITree dest = store.getDst(method);
			if(dest != null){
				methods.add(new NodePair(method, dest, srcText, destText));
			}
		}	
		/*for(NodePair pair : methods){
			Util.println("----------------------- SRC Tree ------------------------\n" + pair.srcNode.toTreeString() + 
					"\n----------------------- Dest Tree ------------------------\n" + pair.tgtNode.toTreeString() 
					 + "---------------------------------------------------------------");
		}*/
		return methods;
	}

	private static void printTrainAndTestData(Map<String, List<DiffParser>> allParsedResults) {
		String trainDirectory = arg.outputFilePath() + "/train";
		String testDirectory = arg.outputFilePath() + "/test";
		List<DiffParser> trainParsers = new ArrayList<DiffParser>();
		List<DiffParser> testParsers = new ArrayList<DiffParser>();
		Set<String> projects = allParsedResults.keySet();
		for(String project : projects){
			List<DiffParser> parsers = allParsedResults.get(project);
			int totalNumber = parsers.size();
			int testNumber = (int)Math.ceil(totalNumber * testPercentage);
			int trainNumber = totalNumber - testNumber;
			for(int i = 0 ; i < totalNumber; i++){
				if(i < trainNumber){
					trainParsers.add(parsers.get(i));
				}
				else{
					testParsers.add(parsers.get(i));
				}
			}
		}
		printDataToDirectory(trainDirectory, trainParsers);
		printDataToDirectory(testDirectory, testParsers);
	}

	private static void printTrainAndTestData(List<DiffParser> parsers) {
		String trainDirectory = arg.outputFilePath() + "/train";
		String testDirectory = arg.outputFilePath() + "/test";
		List<DiffParser> trainParsers = new ArrayList<DiffParser>();
		List<DiffParser> testParsers = new ArrayList<DiffParser>();
		int totalNumber = parsers.size();
		int testNumber = (int)Math.ceil(totalNumber * testPercentage);
		int trainNumber = totalNumber - testNumber;
		for(int i = 0 ; i < totalNumber; i++){
			if(i < trainNumber){
				trainParsers.add(parsers.get(i));
			}
			else{
				testParsers.add(parsers.get(i));
			}
		}
		printDataToDirectory(trainDirectory, trainParsers);
		printDataToDirectory(testDirectory, testParsers);
	}
	

	private static void printDataToDirectory(String baseDir, List<DiffParser> parsers) {
		// #TODO Append the files
		//Util.logln(baseDir + " " + parsers.size());
		try {
			File baseDirFile = new File(baseDir);
			if(!baseDirFile.exists()){
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
			for(DiffParser parser : parsers){
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
				fileNames.println(parser.srcPath);
				flushAllPrintStreams(parentCode, parentTree, childCode, childTree, parentOrgTree,
						parentTypeCode, childTypeCode, parentTypeTree, childTypeTree, tokenMasks);
				fileNames.flush();
				/*Util.logln("\n" + parser.parentCodeString + "\n" + parser.parentOrgTreeString + "\n" + parser.childCodeString + "\n" + parser.childTreeString);
				Util.logln(parser.parentTypeCodeString);
				Util.logln(parser.childTypeCodeString);
				Util.logln(parser.parentOriginalTypeTreeString);
				Util.logln(parser.childTypeTreeString);*/
			}
			closeAllPrintStreams(parentCode, parentTree, childCode, childTree, parentOrgTree, parentTypeCode, childTypeCode,
					parentTypeTree, childTypeTree, tokenMasks);
			fileNames.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
	}

	private boolean checkSuccessFullParse(ITree srcNode, ITree destNode, boolean replace, boolean excludeStringChange) {
		try{
			
			boolean success = this.parseASTDiff(srcNode, destNode);
			if(! success) return false;
			parentCodeString = this.getParentCodeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			parentTreeString = this.getParentTreeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			parentOrgTreeString = this.getParentOrgTreeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			childCodeString = this.getChildCodeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			childTreeString = this.getChildTreeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			parentTypeCodeString = this.getParentTypeCodeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			childTypeCodeString = this.getChildTypeCodeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			childTypeTreeString = this.getChildTypeTreeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			parentOriginalTypeTreeString = this.getParentOriginalTypeTreeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			if(parentCodeString== null || parentTreeString==null || 
					childCodeString== null || childTreeString== null ){
				return false;
			}
			if(excludeStringChange && (parentCodeString.compareTo(childCodeString)==0)){
				return false;
			}
		}catch(Exception ex){
			return false;
		}
		return true;	
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

	private String getParentOriginalTypeTreeString() {
		if (!alreadyParsed) return null;
		return Util.getDestTypeTree(cParentOrg);
	}

	private String getChildTypeTreeString() {
		if (!alreadyParsed) return null;
		return Util.getDestTypeTree(cParentDest);
	}

	private String getChildTypeCodeString() {
		if (!alreadyParsed) return null;
		return Util.getTypedCodeRecusrsive(cParentDest);
	}

	private String getParentTypeCodeString() {
		if (!alreadyParsed) return null;
		return Util.getTypedCodeRecusrsive(cParentSrc);
	}

	/**
	 * @author saikat
	 * @return
	 */
	private String getChildTreeString(boolean replace) {
		if (!alreadyParsed) return null;
		else{
			return Util.getDestTree(cParentDest, replace);
		}
	}
	
	/**
	 * @author saikat
	 * @return
	 */
	private String getParentOrgTreeString(boolean replace) {
		if (!alreadyParsed) return null;
		else{
			return Util.getDestTree(cParentOrg, replace);
		}
	}

	/**
	 * @author saikat
	 * @return
	 */
	private String getChildCodeString(boolean replace) {
		if (!alreadyParsed) return null;
		else{
			//Util.logln(replace);
			return Util.getCodeRecusrsive(cParentDest, replace);
		}
	}

	/**
	 * @author saikat
	 * @return
	 */
	private String getParentTreeString(boolean replace) {
		if (!alreadyParsed) return null;
		else{
			return Util.getSourceTree(cParentSrc);
		}
	}

	/**
	 * @author saikat
	 * @return
	 */
	private String getParentCodeString(boolean replace) {
		if (!alreadyParsed) return null;
		else{
			return Util.getCodeRecusrsive(cParentSrc, replace);
		}
	}

}

class NodePair{
	ITree srcNode;
	ITree tgtNode;
	String srcText;
	String tgtText;
	public NodePair(ITree s, ITree d, String sText, String dText){
		this.srcNode = s;
		this.tgtNode = d;
		this.srcText = sText;
		this.tgtText = dText;
	}
}
