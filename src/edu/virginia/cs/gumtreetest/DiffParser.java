/*package edu.virginia.cs.gumtreetest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

	public static Argument arg;
	public int nonLeafIdx = 200;
	public int currentIdx = 0;
	public String srcPath;
	public String destPath;
	public String srcFileText;
	public String destFileText;
	public String allowedTokensString = "";
	public static double testPercentage = 0.20;
	List<ITree> srcCommonParent = null;
	List<ITree> destCommonParent = null;
	ITree cParentSrc = null;
	ITree cParentDest = null;
	ITree cParentOrg = null;
	public boolean alreadyParsed = false;
	public String parentCodeString = null;
	public String parentTreeString = null;
	public String parentOrgTreeString = null;
	public String childCodeString = null;
	public String parentTypeCodeString = null;
	public String childTreeString = null;
	public String childTypeCodeString = null;
	public String childTypeTreeString = null;
	public String parentOriginalTypeTreeString = null;

	public DiffParser(String srcPath, String destPath, String srcText, String destText) throws IOException {
		Run.initGenerators();
		this.srcPath = srcPath;
		this.destPath = destPath;
		currentIdx = 0;
		nonLeafIdx = 200;
		srcFileText = srcText;
		destFileText = destText;

		// parseASTDiff();
	}

	public void writeDiffs(PrintStream srcDataWriter, PrintStream srcTreeWriter, PrintStream destCodeWriter,
			PrintStream destTreeWriter) {
		if (alreadyParsed) {
			Util.printSourceTree(cParentSrc, srcDataWriter, srcTreeWriter);
			Util.printDestCodeAndTree(cParentDest, destCodeWriter, destTreeWriter);
		}
	}

	public static List<ITree> getMethodNodes(ITree root) {
		List<ITree> methods = new ArrayList<ITree>();
		Stack<ITree> st = new Stack<ITree>();
		st.push(root);
		while (!st.isEmpty()) {
			ITree curr = st.pop();
			if (curr.getType() == Config.ASTTYPE_TAG.METHOD_DECLARATION) {
				methods.add(curr);
			} else {
				for (ITree child : curr.getChildren()) {
					st.add(child);
				}
			}
		}
		return methods;
	}

	public boolean parseASTDiff(ITree srcTree, ITree destTree) throws Exception {
		// # FIXME Extract all the variable that is in scope and and save a list along
		// with the data
		//Util.dfsPrint(srcTree);
		//Util.dfsPrint(destTree);
		//Util.logln("\n\n\n");
		Matcher m = Matchers.getInstance().getMatcher(srcTree, destTree);
		m.match();
		MappingStore store = m.getMappings();
		extractCommonParentsChainFromActions(srcTree, destTree, store);

		findCommonParentRoots(srcCommonParent, destCommonParent, store);
		if (cParentSrc == null || cParentDest == null) {
			Util.logln("One of those are Null");
			return false;
		}
		if (cParentSrc.getType() == Config.ASTTYPE_TAG.JAVADOC || cParentDest.getType() == Config.ASTTYPE_TAG.JAVADOC
				|| cParentSrc.getType() == Config.ASTTYPE_TAG.INSIDE_JAVADOC1
				|| cParentDest.getType() == Config.ASTTYPE_TAG.INSIDE_JAVADOC1
				|| cParentSrc.getType() == Config.ASTTYPE_TAG.INSIDE_JAVADOC2
				|| cParentDest.getType() == Config.ASTTYPE_TAG.INSIDE_JAVADOC2) {
			alreadyParsed = false;
			return false;
		}
		if (cParentSrc == null || cParentDest == null) {
			return false;
		}
		int snc = TreeUtil.countNumberOfNodes(cParentSrc);
		int dnc = TreeUtil.countNumberOfNodes(cParentDest);
		// Util.logln("Src Node : " + snc + " Dest Node : " + dnc);
		if (snc > arg.maxChangeSize() || dnc > arg.maxChangeSize()) {
			alreadyParsed = false;
			return false;
		}
		if (arg.onlyMethodChange() && !Util.checkIfChangeBelongToAnyMethod(cParentSrc, cParentDest)) {
			alreadyParsed = false;
			return false;
		}

		alreadyParsed = true;
		findCommonParentNode(srcCommonParent, destCommonParent, store);
		Map<String, String> sTable = VariableVisitor.getSymbolTable(cParentSrc);
		setMetaDataToDestTree(cParentSrc, cParentDest, store, sTable);
		// Util.logln(cParentSrc.toTreeString());
		cParentOrg = cParentSrc.deepCopy();
		TreeUtil.fixAST(cParentSrc, srcFileText, false);
		TreeUtil.fixAST(cParentOrg, srcFileText, arg.astOnly());
		TreeUtil.fixAST(cParentDest, destFileText, arg.astOnly()); // Destination AST does not need to be fixed
		TreeUtil.removeEmptyNode(cParentOrg);
		TreeUtil.removeEmptyNode(cParentDest);
		List<String> allVariablesInMethod = new ArrayList<String>(Util.extractAllVariablesInScope(cParentSrc));
		for (String token : allVariablesInMethod) {
			allowedTokensString += (token + " ");
		}
		try {

			cParentSrc = TreeUtil.binarizeAST(cParentSrc);
			setIndices(cParentSrc);
			// int srcLeafNodeCount = currentIdx;
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
		} catch (Exception ex) {
			Util.logln(ex.getMessage() + " " + srcPath);
			alreadyParsed = false;
			return false;
		}
		return true;
	}

	public void setMetaDataToDestTree(ITree srcTree, ITree destTree, MappingStore store, Map<String, String> sTable) {
		Stack<ITree> st = new Stack<ITree>();
		st.push(destTree);
		while (!st.isEmpty()) {
			ITree curr = st.pop();
			if (curr.getType() == Config.ASTTYPE_TAG.SIMPLE_NAME) {
				String name = curr.getLabel();
				if (sTable.containsKey(name)) {
					// Util.logln(name+ " " + sTable.get(name));
					curr.setMetadata("subs_name", sTable.get(name));
				} else {
					curr.setMetadata("subs_name", name);
					// Util.logln(name);
				}
			}
			st.addAll(curr.getChildren());
		}
	}

	public void setIndices(ITree root) {
		if (root.getChildren().size() == 0) {
			root.setId(currentIdx);
			currentIdx++;
		} else {
			root.setId(nonLeafIdx);
			nonLeafIdx++;
			for (ITree child : root.getChildren()) {
				setIndices(child);
			}
		}
	}

	public void findCommonParentRoots(List<ITree> srcCommongParents, List<ITree> destCommonParentRoots,
			MappingStore store) {
		if (srcCommongParents != null) {
			for (ITree srcNode : srcCommongParents) {
				ITree destNode = store.getDst(srcNode);
				if (destCommonParentRoots != null) {
					if (destCommonParentRoots.contains(destNode)) {
						cParentSrc = srcNode;
						cParentDest = destNode;
						break;
					}
				} else {
					cParentSrc = srcNode;
					cParentDest = destNode;
					break;
				}
			}
		} else if (destCommonParentRoots != null) {
			for (ITree destNode : destCommonParentRoots) {
				ITree srcNode = store.getSrc(destNode);
				if (srcCommongParents != null) {
					if (srcCommongParents.contains(srcNode)) {
						cParentSrc = srcNode;
						cParentDest = destNode;
						break;
					}
				} else {
					cParentSrc = srcNode;
					cParentDest = destNode;
					break;
				}
			}
		}
	}

	public void findCommonParentNode(List<ITree> srcCommonParent, List<ITree> destCommonParent, MappingStore store) {
		if (srcCommonParent != null) {
			for (ITree cSrcParent : srcCommonParent) {
				ITree cDestParent = store.getDst(cSrcParent);
				if (cDestParent == null) {
					continue;
				}
				if (destCommonParent == null) {
					if (cDestParent != null) {
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						int snc = TreeUtil.countNumberOfNodes(cParentSrc.getParent());
						int dnc = TreeUtil.countNumberOfNodes(cParentDest.getParent());
						// Util.logln(snc + " " + dnc);
						if (snc > arg.maxTreeSize() || dnc > arg.maxTreeSize()) {
							break;
						}
					}
					if (cSrcParent.getParent() == null || cDestParent.getParent() == null) {
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						break;
					}
				} else if (destCommonParent.contains(cDestParent)) {
					cParentSrc = cSrcParent;
					cParentDest = cDestParent;
					int snc = TreeUtil.countNumberOfNodes(cParentSrc.getParent());
					int dnc = TreeUtil.countNumberOfNodes(cParentDest.getParent());
					// Util.logln(snc + " " + dnc);
					if (snc > arg.maxTreeSize() || dnc > arg.maxTreeSize()) {
						break;
					}
					if (cSrcParent.getParent() == null || cDestParent.getParent() == null) {
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						break;
					}
				}
			}
		} else if (destCommonParent != null) {
			for (ITree cDestParent : destCommonParent) {
				ITree cSrcParent = store.getSrc(cDestParent);
				if (cSrcParent == null) {
					continue;
				}
				if (srcCommonParent == null) {
					if (cSrcParent != null) {
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						int snc = TreeUtil.countNumberOfNodes(cParentSrc.getParent());
						int dnc = TreeUtil.countNumberOfNodes(cParentDest.getParent());
						// Util.logln(snc + " " + dnc);
						if (snc > arg.maxTreeSize() || dnc > arg.maxTreeSize()) {
							break;
						}
					}
					if (cSrcParent.getParent() == null || cDestParent.getParent() == null) {
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						break;
					}
				} else if (srcCommonParent.contains(cSrcParent)) {
					cParentSrc = cSrcParent;
					cParentDest = cDestParent;
					int snc = TreeUtil.countNumberOfNodes(cParentSrc.getParent());
					int dnc = TreeUtil.countNumberOfNodes(cParentDest.getParent());
					// Util.logln(snc + " " + dnc);
					if (snc > arg.maxTreeSize() || dnc > arg.maxTreeSize()) {
						break;
					}
					if (cSrcParent.getParent() == null || cDestParent.getParent() == null) {
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						break;
					}
				}

			}
		}
		if (TreeUtil.countNumberOfNodes(cParentSrc) > 200
				|| TreeUtil.countNumberOfNodes(cParentDest) > 200) {
			cParentSrc = null;
			cParentDest = null;
		}
	}

	public void extractCommonParentsChainFromActions(ITree srcTree, ITree destTree, MappingStore store)
			throws Exception {
		ActionGenerator gen = new ActionGenerator(srcTree, destTree, store);
		gen.generate();
		List<Action> actions = gen.getActions();
		for (Action action : actions) {
			ITree incidentNode = action.getNode();
			List<ITree> parents = incidentNode.getParents();

			if (action.getName() == "INS") {
				parents.add(0, action.getNode().getParent());
				if (destCommonParent == null) {
					destCommonParent = parents;
				} else {
					destCommonParent = Util.getCommonParents(destCommonParent, parents);
				}
			} else if (action.getName() == "MOV") {
				if (srcCommonParent == null) {
					srcCommonParent = parents;
				} else {
					srcCommonParent = Util.getCommonParents(srcCommonParent, parents);
				}
				ITree destNode = store.getDst(action.getNode());
				if (destNode == null) {
					continue;
				}
				List<ITree> destParents = destNode.getParents();
				destParents.add(0, destNode);
				if (destCommonParent == null) {
					destCommonParent = destParents;
				} else {
					destCommonParent = Util.getCommonParents(destCommonParent, destParents);
				}
			} else {
				parents.add(0, action.getNode().getParent());
				if (srcCommonParent == null) {
					srcCommonParent = parents;
				} else {
					srcCommonParent = Util.getCommonParents(srcCommonParent, parents);
				}
			}
		}
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

		String allFileDirectory = arg.outputFilePath() + "/all";
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
				Scanner filePathScanner = new Scanner(new File(filePath));
				List<DiffParser> parserList = new ArrayList<DiffParser>();
				// #TODO Print after every project is finished.
				while (filePathScanner.hasNextLine()) {
					try {
						String bothPath = filePathScanner.nextLine().trim();
						String[] filePathParts = bothPath.split("\t");
						String parentFile = filePathParts[0];
						String childFile = filePathParts[1];
						// Util.logln(parentFile);
						String srcText = Util.readFile(parentFile);
						String destText = Util.readFile(childFile);
						TreeContext srcContext = new JdtTreeGenerator().generateFromFile(parentFile);
						TreeContext destContext = new JdtTreeGenerator().generateFromFile(childFile);
						ITree srcTree = srcContext.getRoot();
						ITree destTree = destContext.getRoot();
						List<NodePair> methodPairs = getMethodPairs(srcTree, destTree, srcText, destText);
						for (NodePair pair : methodPairs) {
							DiffParser parser = new DiffParser(parentFile, childFile, srcText, destText);
							// Util.logln(pair.srcNode.toTreeString());
							// Util.logln(pair.tgtNode.toTreeString());
							boolean successfullyParsed = parser.checkSuccessFullParse(pair.srcNode, pair.tgtNode,
									arg.replace(), arg.excludeStringChange());
							if (successfullyParsed) {
								Date current = new Date();
								String cTime = stfmt.format(current);
								Util.logln(startTime + " -> " + cTime + "\t" + totalFileCount);
								printDataToDirectory(allFileDirectory, Arrays.asList(parser));
								totalFileCount++;
								parserList.add(parser);
							}
						}
					} catch (Exception ex) {

					}
				}
				debugStream.println(filePath + " " + parserList.size());
				debugStream.flush();
				Util.logln(filePath);
				printTrainAndTestData(parserList);
				filePathScanner.close();
				allParsedResults.put(filePath, parserList);
			} catch (Exception ex) {

			}
		}
		allFilePathsScanner.close();
		debugStream.close();
		// printTrainAndTestData(allParsedResults);
	}

	public static List<NodePair> getMethodPairs(ITree srcTree, ITree destTree, String srcText, String destText) {
		Matcher m = Matchers.getInstance().getMatcher(srcTree, destTree);
		m.match();
		MappingStore store = m.getMappings();

		List<ITree> methodNodes = getMethodNodes(srcTree);

		List<NodePair> methods = new ArrayList<NodePair>();

		for (ITree method : methodNodes) {
			ITree dest = store.getDst(method);
			if (dest != null) {
				methods.add(new NodePair(method, dest, srcText, destText));
			}
		}
		//for (NodePair pair : methods) {
		//	Util.println("----------------------- SRC Tree ------------------------\n" + pair.srcNode.toTreeString()
		//			+ "\n----------------------- Dest Tree ------------------------\n" + pair.tgtNode.toTreeString()
		//			+ "---------------------------------------------------------------");
		//}
		return methods;
	}

	public static void printTrainAndTestData(Map<String, List<DiffParser>> allParsedResults) {
		String trainDirectory = arg.outputFilePath() + "/train";
		String testDirectory = arg.outputFilePath() + "/test";
		List<DiffParser> trainParsers = new ArrayList<DiffParser>();
		List<DiffParser> testParsers = new ArrayList<DiffParser>();
		Set<String> projects = allParsedResults.keySet();
		for (String project : projects) {
			List<DiffParser> parsers = allParsedResults.get(project);
			int totalNumber = parsers.size();
			int testNumber = (int) Math.ceil(totalNumber * testPercentage);
			int trainNumber = totalNumber - testNumber;
			for (int i = 0; i < totalNumber; i++) {
				if (i < trainNumber) {
					trainParsers.add(parsers.get(i));
				} else {
					testParsers.add(parsers.get(i));
				}
			}
		}
		printDataToDirectory(trainDirectory, trainParsers);
		printDataToDirectory(testDirectory, testParsers);
	}

	public static void printTrainAndTestData(List<DiffParser> parsers) {
		String trainDirectory = arg.outputFilePath() + "/train";
		String testDirectory = arg.outputFilePath() + "/test";
		List<DiffParser> trainParsers = new ArrayList<DiffParser>();
		List<DiffParser> testParsers = new ArrayList<DiffParser>();
		int totalNumber = parsers.size();
		int testNumber = (int) Math.ceil(totalNumber * testPercentage);
		int trainNumber = totalNumber - testNumber;
		for (int i = 0; i < totalNumber; i++) {
			if (i < trainNumber) {
				trainParsers.add(parsers.get(i));
			} else {
				testParsers.add(parsers.get(i));
			}
		}
		printDataToDirectory(trainDirectory, trainParsers);
		printDataToDirectory(testDirectory, testParsers);
	}

	public static void printDataToDirectory(String baseDir, List<DiffParser> parsers) {
		// #TODO Append the files
		Util.logln(baseDir + " " + parsers.size());
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
			for (DiffParser parser : parsers) {
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
				flushAllPrintStreams(parentCode, parentTree, childCode, childTree, parentOrgTree, parentTypeCode,
						childTypeCode, parentTypeTree, childTypeTree, tokenMasks);
				fileNames.flush();
				Util.logln("\n" + parser.parentCodeString + "\n" + parser.parentOrgTreeString + "\n"
						+ parser.childCodeString + "\n" + parser.childTreeString);
				Util.logln(parser.parentTypeCodeString);
				Util.logln(parser.childTypeCodeString);
				Util.logln(parser.parentOriginalTypeTreeString);
				Util.logln(parser.childTypeTreeString);
			}
			closeAllPrintStreams(parentCode, parentTree, childCode, childTree, parentOrgTree, parentTypeCode,
					childTypeCode, parentTypeTree, childTypeTree, tokenMasks);
			fileNames.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public boolean checkSuccessFullParse(ITree srcNode, ITree destNode, boolean replace, boolean excludeStringChange) {
		try {

			boolean success = this.parseASTDiff(srcNode, destNode);
			if (!success)
				return false;
			parentCodeString = this.getParentCodeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			parentTreeString = this.getParentTreeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			parentOrgTreeString = this.getParentOrgTreeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)",
					"");
			childCodeString = this.getChildCodeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			childTreeString = this.getChildTreeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			parentTypeCodeString = this.getParentTypeCodeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			childTypeCodeString = this.getChildTypeCodeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			childTypeTreeString = this.getChildTypeTreeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			parentOriginalTypeTreeString = this.getParentOriginalTypeTreeString()
					.replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			if (parentCodeString == null || parentTreeString == null || childCodeString == null
					|| childTreeString == null) {
				return false;
			}
			if (excludeStringChange && (parentCodeString.compareTo(childCodeString) == 0)) {
				return false;
			}
		} catch (Exception ex) {
			return false;
		}
		return true;
	}

	public static void closeAllPrintStreams(PrintStream parentCode, PrintStream parentTree, PrintStream childCode,
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

	public static void flushAllPrintStreams(PrintStream parentCode, PrintStream parentTree, PrintStream childCode,
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

	public String getParentOriginalTypeTreeString() {
		if (!alreadyParsed)
			return null;
		return Util.getDestTypeTree(cParentOrg);
	}

	public String getChildTypeTreeString() {
		if (!alreadyParsed)
			return null;
		return Util.getDestTypeTree(cParentDest);
	}

	public String getChildTypeCodeString() {
		if (!alreadyParsed)
			return null;
		return Util.getTypedCodeRecusrsive(cParentDest);
	}

	public String getParentTypeCodeString() {
		if (!alreadyParsed)
			return null;
		return Util.getTypedCodeRecusrsive(cParentSrc);
	}

	public String getChildTreeString(boolean replace) {
		if (!alreadyParsed)
			return null;
		else {
			return Util.getDestTree(cParentDest, replace);
		}
	}

	public String getParentOrgTreeString(boolean replace) {
		if (!alreadyParsed)
			return null;
		else {
			return Util.getDestTree(cParentOrg, replace);
		}
	}

	public String getChildCodeString(boolean replace) {
		if (!alreadyParsed)
			return null;
		else {
			// Util.logln(replace);
			return Util.getCodeRecusrsive(cParentDest, replace);
		}
	}

	public String getParentTreeString(boolean replace) {
		if (!alreadyParsed)
			return null;
		else {
			return Util.getSourceTree(cParentSrc);
		}
	}

	public String getParentCodeString(boolean replace) {
		if (!alreadyParsed)
			return null;
		else {
			return Util.getCodeRecusrsive(cParentSrc, replace);
		}
	}

}

class NodePair {
	ITree srcNode;
	ITree tgtNode;
	String srcText;
	String tgtText;

	public NodePair(ITree s, ITree d, String sText, String dText) {
		this.srcNode = s;
		this.tgtNode = d;
		this.srcText = sText;
		this.tgtText = dText;
	}
}*/

package edu.virginia.cs.gumtreetest;

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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

public class DiffParser {
	public static Argument arg;
	public int nonLeafIdx = 200;
	public int currentIdx = 0;
	public String srcPath;
	public String destPath;
	public String srcFileText;
	public String destFileText;
	public String allowedTokensString = "";
	public static double testPercentage = 0.2D;
	List<ITree> srcCommonParent = null;
	List<ITree> destCommonParent = null;
	ITree cParentSrc = null;
	ITree cParentDest = null;
	ITree cParentOrg = null;
	public boolean alreadyParsed = false;
	public String parentCodeString = null;
	public String parentTreeString = null;
	public String parentOrgTreeString = null;
	public String childCodeString = null;
	public String parentTypeCodeString = null;
	public String childTreeString = null;
	public String childTypeCodeString = null;
	public String childTypeTreeString = null;
	
	public String childFullCodeString = null;
	public String childFullTreeString = null;
	public String parentOriginalTypeTreeString = null;
	public ITree destCommonParentFixOnly = null;
	public ITree srcCommonParentFixOnly = null;
	public String sequenceRString = "";
	public int beginPos = 0;
	public int endPos = 0;
	
	public static Set<String> takenPatches = new TreeSet<String>();

	public DiffParser(String srcPath, String destPath, String srcText, String destText) throws IOException {
		Run.initGenerators();
		this.srcPath = srcPath;
		this.destPath = destPath;
		this.currentIdx = 0;
		this.nonLeafIdx = 200;
		this.srcFileText = srcText;
		this.destFileText = destText;
	}

	public void writeDiffs(PrintStream srcDataWriter, PrintStream srcTreeWriter, PrintStream destCodeWriter,
			PrintStream destTreeWriter) {
		if (this.alreadyParsed) {
			Util.printSourceTree(this.cParentSrc, srcDataWriter, srcTreeWriter);
			Util.printDestCodeAndTree(this.cParentDest, destCodeWriter, destTreeWriter);
		}
	}

	public static List<ITree> getMethodNodes(ITree root) {
		List<ITree> methods = new ArrayList<ITree>();
		Stack<ITree> st = new Stack<ITree>();
		st.push(root);
		while (!st.isEmpty()) {
			ITree curr = (ITree) st.pop();
			if (curr.getType() == 31) {
				methods.add(curr);
			} else {
				for (ITree child : curr.getChildren()) {
					st.add(child);
				}
			}
		}
		return methods;
	}

	public boolean parseASTDiff(ITree srcTree, ITree destTree) throws Exception {
		//Util.dfsPrint(srcTree);
		//Util.dfsPrint(destTree);
		Matcher m = Matchers.getInstance().getMatcher(srcTree, destTree);
		m.match();
		MappingStore store = m.getMappings();
		extractCommonParentsChainFromActions(srcTree, destTree, store);
		findCommonParentRoots(this.srcCommonParent, this.destCommonParent, store);
		if ((this.cParentSrc == null) || (this.cParentDest == null)) {
			//Util.logln("One of those are Null!");
			return false;
		}
		if ((this.cParentSrc.getType() == 29) || (this.cParentDest.getType() == 29) || (this.cParentSrc.getType() == 65)
				|| (this.cParentDest.getType() == 65) || (this.cParentSrc.getType() == 66)
				|| (this.cParentDest.getType() == 66)) {
			this.alreadyParsed = false;
			return false;
		}
		if ((this.cParentSrc == null) || (this.cParentDest == null)) {
			return false;
		}
		int snc = TreeUtil.countNumberOfNodes(this.cParentSrc);
		int dnc = TreeUtil.countNumberOfNodes(this.cParentDest);
		Util.logln(snc + " " + dnc);
		if (dnc > arg.maxChangeSize()) {
			this.alreadyParsed = false;
			return false;
		}
		this.alreadyParsed = true;
		this.destCommonParentFixOnly = this.cParentDest.deepCopy();
		this.srcCommonParentFixOnly = this.cParentSrc.deepCopy();
		this.beginPos = this.srcCommonParentFixOnly.getPos();
		this.endPos = this.srcCommonParentFixOnly.getEndPos();
		findCommonParentNode(this.srcCommonParent, this.destCommonParent, store);
		Map<String, String> sTable = VariableVisitor.getSymbolTable(this.cParentSrc);
		setMetaDataToDestTree(this.cParentSrc, this.cParentDest, store, sTable);

		this.cParentOrg = this.cParentSrc.deepCopy();
		TreeUtil.fixAST(this.cParentSrc, this.srcFileText, false);
		TreeUtil.fixAST(this.cParentOrg, this.srcFileText, arg.astOnly());
		TreeUtil.fixAST(this.cParentDest, this.destFileText, arg.astOnly());
		TreeUtil.fixAST(this.destCommonParentFixOnly, this.destFileText, arg.astOnly());
		TreeUtil.fixAST(this.srcCommonParentFixOnly, this.srcFileText, arg.astOnly());
		TreeUtil.removeEmptyNode(this.cParentOrg);
		TreeUtil.removeEmptyNode(this.cParentDest);
		TreeUtil.removeEmptyNode(this.destCommonParentFixOnly);
		TreeUtil.removeEmptyNode(this.srcCommonParentFixOnly);
		List<String> allVariablesInMethod = new ArrayList<String>(Util.extractAllVariablesInScope(this.cParentSrc));
		for (String token : allVariablesInMethod) {
			this.allowedTokensString = (this.allowedTokensString + token + " ");
		}
		try {
			this.cParentSrc = TreeUtil.binarizeAST(this.cParentSrc);
			setIndices(this.cParentSrc);

			this.currentIdx = 0;
			setIndices(this.cParentOrg);
			TreeUtil.setTreeMetadata(this.cParentSrc);
			TreeUtil.setTreeMetadata(this.cParentDest);
			TreeUtil.setTreeMetadata(this.cParentOrg);
			TreeUtil.setTreeMetadata(this.destCommonParentFixOnly);
			TreeUtil.setTreeMetadata(this.srcCommonParentFixOnly);
			this.currentIdx = 0;
			setIndices(this.cParentDest);
			DataTypeVisitor.setDataTypes(this.cParentSrc, this.srcPath);
			DataTypeVisitor.setDataTypes(this.cParentDest, this.destPath);
			DataTypeVisitor.setDataTypes(this.destCommonParentFixOnly, this.destPath);
			DataTypeVisitor.setDataTypes(this.srcCommonParentFixOnly, this.srcPath);
			DataTypeVisitor.setDataTypes(this.cParentOrg, this.srcPath);
		} catch (Exception ex) {
			Util.logln(ex.getMessage() + " " + this.srcPath);
			this.alreadyParsed = false;
			return false;
		}
		catch (Error ex) {
			Util.logln(ex.getMessage() + " " + this.srcPath);
			this.alreadyParsed = false;
			return false;
		}
		return true;
	}
	
	
	private boolean methodASTDiff(ITree srcTree, ITree destTree, int maxMethodSize) {
		Matcher m = Matchers.getInstance().getMatcher(srcTree, destTree);
		m.match();
		MappingStore store = m.getMappings();
		ActionGenerator gen = new ActionGenerator(srcTree, destTree, store);
		gen.generate();
		List<Action> actions = gen.getActions();
		if(actions.size() == 0) {
			return false;
		}
		Map<String, String> sTable = VariableVisitor.getSymbolTable(srcTree);
		setMetaDataToDestTree(srcTree, destTree, store, sTable);
		
		TreeUtil.fixAST(srcTree, this.srcFileText, false);
		TreeUtil.fixAST(destTree, this.destFileText, false);
		//Util.dfsPrint(srcTree);
		srcTree = removeJavaDocNodes(srcTree);
		destTree = removeJavaDocNodes(destTree);
		String srcCode = Util.getCodeRecusrsive(srcTree, false);
		String destCode = Util.getCodeRecusrsive(destTree, false);
		if(srcCode.compareTo(destCode) == 0) {
			return false;
		}
		//Util.logln("\n" + srcCode + "\n" + destCode);
		int partsSrcCode = srcCode.split(" ").length;
		int partsTgtCode = destCode.split(" ").length;
		
		if(partsSrcCode > maxMethodSize || partsTgtCode > maxMethodSize) {
			return false;
		}
		this.cParentSrc = srcTree;
		this.cParentDest = destTree;
		this.cParentOrg = srcTree.deepCopy();
		//Util.logln(partsSrcCode + " " + partsTgtCode);
		List<String> allVariablesInMethod = new ArrayList<String>(Util.extractAllVariablesInScope(this.cParentSrc));
		for (String token : allVariablesInMethod) {
			this.allowedTokensString = (this.allowedTokensString + token + " ");
		}
		// TODO check correctness
		//Util.logln(this.allowedTokensString);
		try {
			this.cParentSrc = TreeUtil.binarizeAST(this.cParentSrc);
			setIndices(this.cParentSrc);

			this.currentIdx = 0;
			setIndices(this.cParentOrg);
			TreeUtil.setTreeMetadata(this.cParentSrc);
			TreeUtil.setTreeMetadata(this.cParentDest);
			TreeUtil.setTreeMetadata(this.cParentOrg);
			this.currentIdx = 0;
			setIndices(this.cParentDest);
			DataTypeVisitor.setDataTypes(this.cParentSrc, this.srcPath);
			DataTypeVisitor.setDataTypes(this.cParentDest, this.destPath);
			DataTypeVisitor.setDataTypes(this.cParentOrg, this.srcPath);
		} catch (Exception ex) {
			Util.logln(ex.getMessage() + " " + this.srcPath);
			this.alreadyParsed = false;
			return false;
		}
		this.alreadyParsed = true;
		return true;
	}

	public ITree removeJavaDocNodes(ITree srcTree) {
		Stack<ITree> st = new Stack<>();
		st.push(srcTree);
		while(!st.isEmpty()) {
			ITree curr = st.pop();
			List<ITree> newChildren = new ArrayList<>();
			List<ITree> children = curr.getChildren();
			for(ITree child : children) {
				if(child.getType() < 14 || child.getType() > 20) {
					newChildren.add(child);
				}
			}
			for(ITree child : newChildren) {
				st.push(child);
			}
		}
		return srcTree;
	}

	public void setMetaDataToDestTree(ITree srcTree, ITree destTree, MappingStore store, Map<String, String> sTable) {
		int maxNode = getMaxNodeNumber(sTable);
		maxNode++;
		Stack<ITree> st = new Stack<ITree>();
		st.push(destTree);
		while (!st.isEmpty()) {
			ITree curr = (ITree) st.pop();
			if (curr.getType() == Config.ASTTYPE_TAG.SIMPLE_NAME || curr.getType() == Config.ASTTYPE_TAG.COMPLEX_NAME) {
				String name = curr.getLabel();
				if (sTable.containsKey(name)) {
					curr.setMetadata("subs_name", sTable.get(name));
				} else {
					curr.setMetadata("subs_name", "t" + maxNode);
					sTable.put(name, "t" + maxNode);
					maxNode++;
				}
			}
			st.addAll(curr.getChildren());
		}
	}

	private int getMaxNodeNumber(Map<String, String> sTable) {
		List<Integer> numbers = new ArrayList<Integer>();
		for(String key: sTable.keySet()) {
			String val = sTable.get(key);
			numbers.add(Integer.parseInt(val.substring(1)));
		}
		int max = -1;
		for(Integer a : numbers) {
			if(a > max) {
				max = a;
			}
		}
		return max;
	}

	public void setIndices(ITree root) {
		if (root.getChildren().size() == 0) {
			root.setId(this.currentIdx);
			this.currentIdx += 1;
		} else {
			root.setId(this.nonLeafIdx);
			this.nonLeafIdx += 1;
			for (ITree child : root.getChildren()) {
				setIndices(child);
			}
		}
	}

	public void findCommonParentRoots(List<ITree> srcCommongParents, List<ITree> destCommonParentRoots,
			MappingStore store) {
		if (srcCommongParents != null) {
			for (ITree srcNode : srcCommongParents) {
				ITree destNode = store.getDst(srcNode);
				if (destCommonParentRoots != null) {
					if (destCommonParentRoots.contains(destNode)) {
						this.cParentSrc = srcNode;
						this.cParentDest = destNode;
						break;
					}
				} else {
					this.cParentSrc = srcNode;
					this.cParentDest = destNode;
					break;
				}
			}
		} 
		if (destCommonParentRoots != null) {
			for (ITree destNode : destCommonParentRoots) {
				ITree srcNode = store.getSrc(destNode);
				//Util.logln(destNode);
				//Util.logln(srcNode);
				if (srcCommongParents != null) {
					if (srcCommongParents.contains(srcNode)) {
						this.cParentSrc = srcNode;
						this.cParentDest = destNode;
						break;
					}
				} else {
					this.cParentSrc = srcNode;
					this.cParentDest = destNode;
					break;
				}
			}
		}
	}

	public void findCommonParentNode(List<ITree> srcCommonParent, List<ITree> destCommonParent, MappingStore store) {
		
		if (srcCommonParent != null) {
			
			for (ITree cSrcParent : srcCommonParent) {
				ITree cDestParent = store.getDst(cSrcParent);
				//Util.logln(cSrcParent.toTreeString());
				//Util.logln(cDestParent.toTreeString());
				if (cDestParent != null) {
					if (destCommonParent == null) {
						if (cDestParent != null) {
							this.cParentSrc = cSrcParent;
							this.cParentDest = cDestParent;
							int snc = TreeUtil.countNumberOfNodes(this.cParentSrc.getParent());
							int dnc = TreeUtil.countNumberOfNodes(this.cParentDest.getParent());
							if ((snc > arg.maxTreeSize()) || (dnc > arg.maxTreeSize())) {
								break;
							}
						}
						if ((cSrcParent.getParent() == null) || (cDestParent.getParent() == null)) {
							this.cParentSrc = cSrcParent;
							this.cParentDest = cDestParent;
							break;
						}
					} else if (destCommonParent.contains(cDestParent)) {
						this.cParentSrc = cSrcParent;
						this.cParentDest = cDestParent;
						int snc = TreeUtil.countNumberOfNodes(this.cParentSrc.getParent());
						int dnc = TreeUtil.countNumberOfNodes(this.cParentDest.getParent());
						if ((snc > arg.maxTreeSize()) || (dnc > arg.maxTreeSize())) {
							break;
						}
						if ((cSrcParent.getParent() == null) || (cDestParent.getParent() == null)) {
							this.cParentSrc = cSrcParent;
							this.cParentDest = cDestParent;
							break;
						}
					}
				}
			}
		} else if (destCommonParent != null) {
			for (ITree cDestParent : destCommonParent) {
				ITree cSrcParent = store.getSrc(cDestParent);
				if (cSrcParent != null) {
					if (srcCommonParent == null) {
						if (cSrcParent != null) {
							this.cParentSrc = cSrcParent;
							this.cParentDest = cDestParent;
							int snc = TreeUtil.countNumberOfNodes(this.cParentSrc.getParent());
							int dnc = TreeUtil.countNumberOfNodes(this.cParentDest.getParent());
							if ((snc > arg.maxTreeSize()) || (dnc > arg.maxTreeSize())) {
								break;
							}
						}
						if ((cSrcParent.getParent() == null) || (cDestParent.getParent() == null)) {
							this.cParentSrc = cSrcParent;
							this.cParentDest = cDestParent;
							break;
						}
					} else if (srcCommonParent.contains(cSrcParent)) {
						this.cParentSrc = cSrcParent;
						this.cParentDest = cDestParent;
						int snc = TreeUtil.countNumberOfNodes(this.cParentSrc.getParent());
						int dnc = TreeUtil.countNumberOfNodes(this.cParentDest.getParent());
						if ((snc > arg.maxTreeSize()) || (dnc > arg.maxTreeSize())) {
							break;
						}
						if ((cSrcParent.getParent() == null) || (cDestParent.getParent() == null)) {
							this.cParentSrc = cSrcParent;
							this.cParentDest = cDestParent;
							break;
						}
					}
				}
			}
		}
	}

	public void extractCommonParentsChainFromActions(ITree srcTree, ITree destTree, MappingStore store)
			throws Exception {
		ActionGenerator gen = new ActionGenerator(srcTree, destTree, store);
		gen.generate();
		List<Action> actions = gen.getActions();
		//Util.logln(srcTree.toTreeString());
		//Util.logln(destTree.toTreeString());
//		boolean insertOrDeleteOnly = true;
//		for(Action action:actions) {
//			if(action.getName() != "INS" && action.getName() != "DEL") {
//				insertOrDeleteOnly = false;
//			}
//		}
//		if(insertOrDeleteOnly) {
//			this.alreadyParsed = false;
//			return;
//		}
		for (Action action : actions) {
			//Util.logln(action.getName());
			ITree incidentNode = action.getNode();
			//Util.logln(action.getName() + " " + incidentNode.toTreeString());
			List<ITree> parents = incidentNode.getParents();
			if (action.getName() == "INS") {
				parents.add(0, action.getNode().getParent());
				if (this.destCommonParent == null) {
					this.destCommonParent = parents;
				} else {
					this.destCommonParent = Util.getCommonParents(this.destCommonParent, parents);
				}
			} else if (action.getName() == "MOV") {
				if (this.srcCommonParent == null) {
					this.srcCommonParent = parents;
				} else {
					this.srcCommonParent = Util.getCommonParents(this.srcCommonParent, parents);
				}
				ITree destNode = store.getDst(action.getNode());
				if (destNode != null) {
					List<ITree> destParents = destNode.getParents();
					destParents.add(0, destNode);
					if (this.destCommonParent == null) {
						this.destCommonParent = destParents;
					} else {
						this.destCommonParent = Util.getCommonParents(this.destCommonParent, destParents);
					}
				}
			} else {
				parents.add(0, action.getNode().getParent());
				if (this.srcCommonParent == null) {
					this.srcCommonParent = parents;
				} else {
					this.srcCommonParent = Util.getCommonParents(this.srcCommonParent, parents);
				}
			}
		}
		//Util.logln(this.srcCommonParent.size() + " " + this.destCommonParent.size());
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
		outputFile.mkdirs();

		String allFileDirectory = arg.outputFilePath();
		File allFile = new File(allFileDirectory);
		if (!allFile.exists()) {
			allFile.mkdirs();
		}
		int totalFileCount = 0;
		int totalMethodCount = 0;
		Scanner allFilePathsScanner = new Scanner(new File(arg.allPathsFile()));
		
		
		while (allFilePathsScanner.hasNextLine()) {
			try {
				String filePath = allFilePathsScanner.nextLine().trim();
				try {
					String childFile = null;
					String parentFile = null;
					if (filePath.contains("\t")) {
						String[] parts = filePath.split("\t");
						parentFile = parts[1];
						childFile = parts[2];
					}
					else {
						parentFile = filePath;
						childFile = filePath.replace("/parent/", "/child/");
					}
					Util.logln(parentFile);
					// Util.logln(parentFile + " " + childFile);
					String srcText = Util.readFile(parentFile);
					String destText = Util.readFile(childFile);
					TreeContext srcContext = new JdtTreeGenerator().generateFromFile(parentFile);
					TreeContext destContext = new JdtTreeGenerator().generateFromFile(childFile);
					ITree srcTree = srcContext.getRoot();
					ITree destTree = destContext.getRoot();
					List<NodePair> methodPairs = DiffParser.getMethodPairs(srcTree, destTree, srcText, destText);
					boolean s = false;
					for (NodePair pair : methodPairs) {
						DiffParser parser = new DiffParser(parentFile, childFile, srcText, destText);
						//Util.dfsPrint(pair.srcNode);
						boolean successfullyParsed = parser.checkSuccessFullParse(pair.srcNode, pair.tgtNode,
								arg.replace(), arg.excludeStringChange());
						if (successfullyParsed) {
							Date current = new Date();
							String cTime = stfmt.format(current);
							Util.logln(startTime + " -> " + cTime + "\t" + totalMethodCount);
							DiffParser.printDataToDirectory(allFileDirectory, Arrays.asList(new DiffParser[] { parser }));
							totalMethodCount++;
							s = true;
						}
					}
					if(s) {
						totalFileCount++;
						Util.logln(totalFileCount);
					}
				} catch (Exception localException) {
				} catch (Error ex) {
				}
				
			} catch (Exception localException1) {
			} catch (Error ex) {
			}
		}
		Util.logln("Total methods counted : " + totalMethodCount + "\tAcross " + totalFileCount + " Files");
		allFilePathsScanner.close();
	}

	public static List<NodePair> getMethodPairs(ITree srcTree, ITree destTree, String srcText, String destText) {
		Matcher m = Matchers.getInstance().getMatcher(srcTree, destTree);
		m.match();
		MappingStore store = m.getMappings();

		List<ITree> methodNodes = getMethodNodes(srcTree);

		List<NodePair> methods = new ArrayList<NodePair>();
		for (ITree method : methodNodes) {
			ITree dest = store.getDst(method);
			if (dest != null) {
				methods.add(new NodePair(method, dest, srcText, destText));
			}
		}
		return methods;
	}

	@SuppressWarnings("unused")
	public static void printTrainAndTestData(Map<String, List<DiffParser>> allParsedResults) {
		String trainDirectory = arg.outputFilePath() + "/train";
		String testDirectory = arg.outputFilePath() + "/test";
		List<DiffParser> trainParsers = new ArrayList<DiffParser>();
		List<DiffParser> testParsers = new ArrayList<DiffParser>();
		Set<String> projects = allParsedResults.keySet();
		for (String project : projects) {
			List<DiffParser> parsers = allParsedResults.get(project);
			int totalNumber = parsers.size();
			int testNumber = (int) Math.ceil(totalNumber * testPercentage);
			int trainNumber = totalNumber - testNumber;
			for (int i = 0; i < totalNumber; i++) {
				if (i < trainNumber) {
					trainParsers.add(parsers.get(i));
				} else {
					testParsers.add(parsers.get(i));
				}
			}
		}
		printDataToDirectory(trainDirectory, trainParsers);
		printDataToDirectory(testDirectory, testParsers);
	}

	public static void printTrainAndTestData(List<DiffParser> parsers) {
		String trainDirectory = arg.outputFilePath() + "/train";
		String testDirectory = arg.outputFilePath() + "/test";
		List<DiffParser> trainParsers = new ArrayList<DiffParser>();
		List<DiffParser> testParsers = new ArrayList<DiffParser>();
		int totalNumber = parsers.size();
		int testNumber = (int) Math.ceil(totalNumber * testPercentage);
		int trainNumber = totalNumber - testNumber;
		for (int i = 0; i < totalNumber; i++) {
			if (i < trainNumber) {
				trainParsers.add((DiffParser) parsers.get(i));
			} else {
				testParsers.add((DiffParser) parsers.get(i));
			}
		}
		printDataToDirectory(trainDirectory, trainParsers);
		printDataToDirectory(testDirectory, testParsers);
	}

	public static void printDataToDirectory(String baseDir, List<DiffParser> parsers) {
		//Util.logln(baseDir + " " + parsers.size());
		try {
			File baseDirFile = new File(baseDir);
			if (!baseDirFile.exists()) {
				baseDirFile.mkdir();
			}
			PrintStream parentCode = new PrintStream(new FileOutputStream(baseDir + "/parent.code", true));
			//PrintStream parentTree = new PrintStream(new FileOutputStream(baseDir + "/parent.tree", true));
			PrintStream childCode = new PrintStream(new FileOutputStream(baseDir + "/child.code", true));
			PrintStream childTree = new PrintStream(new FileOutputStream(baseDir + "/child.tree", true));
			PrintStream parentOrgTree = new PrintStream(new FileOutputStream(baseDir + "/parent.tree", true));
			PrintStream sequenceR = new PrintStream(new FileOutputStream(baseDir + "/parent.seqr", true));
			PrintStream childFullCode = new PrintStream(new FileOutputStream(baseDir + "/child.full.code", true));
			PrintStream childFullTree = new PrintStream(new FileOutputStream(baseDir + "/child.full.tree", true));
			//PrintStream parentTypeCode = new PrintStream(new FileOutputStream(baseDir + "/parent.type.code", true));
			//PrintStream childTypeCode = new PrintStream(new FileOutputStream(baseDir + "/child.type.code", true));
			//PrintStream parentTypeTree = new PrintStream(new FileOutputStream(baseDir + "/parent.org.type.tree", true));
			//PrintStream childTypeTree = new PrintStream(new FileOutputStream(baseDir + "/child.type.tree", true));
			//PrintStream tokenMasks = new PrintStream(new FileOutputStream(baseDir + "/allowed.tokens", true));
			PrintStream fileNames = new PrintStream(new FileOutputStream(baseDir + "/files.txt", true));
			PrintStream positions = new PrintStream(new FileOutputStream(baseDir + "/positions.txt", true));
			for (DiffParser parser : parsers) {
				parentCode.println(parser.parentCodeString);
				//parentTree.println(parser.parentTreeString);
				childCode.println(parser.childCodeString);
				childTree.println(parser.childTreeString);
				parentOrgTree.println(parser.parentOrgTreeString);
				sequenceR.println(parser.sequenceRString);
				childFullCode.println(parser.childFullCodeString != null? parser.childFullCodeString : parser.childCodeString);
				childFullTree.println(parser.childFullTreeString != null? parser.childFullTreeString : parser.childTreeString);
				//parentTypeCode.println(parser.parentTypeCodeString);
				//childTypeCode.println(parser.childTypeCodeString);
				//parentTypeTree.println(parser.parentOriginalTypeTreeString);
				//childTypeTree.println(parser.childTypeTreeString);
				//tokenMasks.println(parser.allowedTokensString);
				fileNames.println(parser.srcPath);
				flushAllPrintStreams(parentCode, childCode, childTree, parentOrgTree, sequenceR, childFullCode, childFullTree);
				fileNames.flush();
				positions.println(parser.beginPos + "\t" + parser.endPos);
				positions.flush();
			}
			closeAllPrintStreams(parentCode,childCode, childTree, parentOrgTree, sequenceR, childFullCode, childFullTree);
			fileNames.close();
			positions.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public boolean checkMethodChange(ITree srcNode, ITree tgtNode, boolean replace, boolean excludeStringChange,
			int maximumMethodSize) {
		
		try {
			boolean success = methodASTDiff(srcNode, tgtNode, maximumMethodSize);
			if (!success) {
				return false;
			}
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
			ex.printStackTrace();
			//Util.dfsPrint(srcNode);
			return false;
		}
		return true;
	}

	public boolean checkSuccessFullParse(ITree srcNode, ITree destNode, boolean replace, boolean excludeStringChange) {
		try {
			boolean success = parseASTDiff(srcNode, destNode);
			if (!success || !alreadyParsed) {
				//Util.dfsPrint(srcNode);
				return false;
			}
			String bugText = Util.getCodeRecusrsive(this.srcCommonParentFixOnly, arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.parentCodeString = getParentCodeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.childCodeString = getChildCodeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			String fullText = this.parentCodeString;
			String key = fullText + "\t->\t" + this.childCodeString;
			if(takenPatches.contains(key)) {
				Util.logln("\n\n" + key + " Already in taken patched\n\n");
				return false;
			}else {
				Util.logln(key + " Added to patches");
				takenPatches.add(key);
			}
			int index = fullText.indexOf(bugText); 
			if (index >= 0) {
				this.sequenceRString = fullText.substring(0, index) + "<BUG_START> " + bugText + " <BUG_END> " + fullText.substring(index + bugText.length()).trim();
			}
			else {
				this.sequenceRString = "<BUG_START> " + fullText.trim() +  " <BUG_END>";
			}
			this.parentTreeString = getParentTreeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.parentOrgTreeString = getParentOrgTreeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			
			this.childTreeString = getChildTreeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.parentTypeCodeString = getParentTypeCodeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.childTypeCodeString = getChildTypeCodeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.childTypeTreeString = getChildTypeTreeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.parentOriginalTypeTreeString = getParentOriginalTypeTreeString()
					.replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			
			this.childFullCodeString = Util.getCodeRecusrsive(this.cParentDest, arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.childFullTreeString = Util.getDestTree(this.cParentDest, arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
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
	
	public static void closeAllPrintStreams(PrintStream parentCode, PrintStream childCode,
			PrintStream childTree, PrintStream parentOrgTree, PrintStream sequenceR,
			PrintStream childFullCode, PrintStream childFullTree) {
		parentCode.close();
		//parentTree.flush();
		childCode.close();
		childTree.close();
		parentOrgTree.close();
		sequenceR.close();
		childFullCode.close();
		childFullTree.close();
//		parentTypeCode.flush();
//		childTypeCode.flush();
//		parentTypeTree.flush();
//		childTypeTree.flush();
//		alloedTokens.flush();
	}

	public static void closeAllPrintStreams(PrintStream parentCode, PrintStream parentTree, PrintStream childCode,
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

	public static void flushAllPrintStreams(PrintStream parentCode, PrintStream childCode,
			PrintStream childTree, PrintStream parentOrgTree, PrintStream sequenceR,
			PrintStream childFullCode, PrintStream childFullTree) {
		parentCode.flush();
		//parentTree.flush();
		childCode.flush();
		childTree.flush();
		parentOrgTree.flush();
		sequenceR.flush();
		childFullCode.flush();
		childFullTree.flush();
//		parentTypeCode.flush();
//		childTypeCode.flush();
//		parentTypeTree.flush();
//		childTypeTree.flush();
//		alloedTokens.flush();
	}

	public String getParentOriginalTypeTreeString() {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getDestTypeTree(this.cParentOrg);
	}

	public String getChildTypeTreeString() {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getDestTypeTree(this.destCommonParentFixOnly);
	}

	public String getChildTypeCodeString() {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getTypedCodeRecusrsive(this.destCommonParentFixOnly);
	}

	public String getParentTypeCodeString() {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getTypedCodeRecusrsive(this.cParentSrc);
	}

	public String getChildTreeString(boolean replace) {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getDestTree(this.destCommonParentFixOnly, replace);
	}

	public String getParentOrgTreeString(boolean replace) {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getDestTree(this.cParentOrg, replace);
	}

	public String getChildCodeString(boolean replace) {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getCodeRecusrsive(this.destCommonParentFixOnly, replace);
	}

	public String getParentTreeString(boolean replace) {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getSourceTree(this.cParentSrc);
	}

	public String getParentCodeString(boolean replace) {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getCodeRecusrsive(this.cParentSrc, replace);
	}

	
}

class NodePair {
	ITree srcNode;
	ITree tgtNode;
	String srcText;
	String tgtText;

	public NodePair(ITree s, ITree d, String sText, String dText) {
		this.srcNode = s;
		this.tgtNode = d;
		this.srcText = sText;
		this.tgtText = dText;
	}
}
