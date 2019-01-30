package edu.columbia.cs.dataset;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.c.CTreeGenerator;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import edu.virginia.cs.gumtreetest.Argument;
import edu.virginia.cs.gumtreetest.Config;
import edu.virginia.cs.gumtreetest.TreeUtil;
import edu.virginia.cs.gumtreetest.Util;
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

import javax.naming.ldap.Rdn;

import org.hamcrest.core.IsInstanceOf;

public class IcseDatasetParser {
	private static Argument arg;
	private int nonLeafIdx = 200;
	private int currentIdx = 0;
	private String srcPath;
	private String destPath;
	private String srcFileText;
	private String destFileText;
	private String allowedTokensString = "";
	private static double testPercentage = 0.2D;
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

	public IcseDatasetParser(String srcPath, String destPath, String srcText, String destText) throws IOException {
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

	private boolean getOriginalASTS(ITree srcTree, ITree destTree) {
		Matcher m = Matchers.getInstance().getMatcher(srcTree, destTree);
		m.match();
		MappingStore store = m.getMappings();
		//extractCommonParentsChainFromActions(srcTree, destTree, store);
		//findCommonParentRoots(this.srcCommonParent, this.destCommonParent, store);
		//if ((this.cParentSrc == null) || (this.cParentDest == null)) {
			//Util.logln("One of those are Null!");
		//	return false;
		//}
		//if ((this.cParentSrc.getType() == 29) || (this.cParentDest.getType() == 29) || (this.cParentSrc.getType() == 65)
		//		|| (this.cParentDest.getType() == 65) || (this.cParentSrc.getType() == 66)
		//		|| (this.cParentDest.getType() == 66)) {
		//	this.alreadyParsed = false;
		//	return false;
		//}
		//if ((this.cParentSrc == null) || (this.cParentDest == null)) {
		//	return false;
		//}
		//int snc = TreeUtil.countNumberOfNodes(this.cParentSrc);
		//int dnc = TreeUtil.countNumberOfNodes(this.cParentDest);
		//if ((snc > arg.maxChangeSize()) || (dnc > arg.maxChangeSize())) {
		//	this.alreadyParsed = false;
		//	return false;
		//}
		//Util.dfsPrint(srcTree);
		//Util.dfsPrint(destTree);
		this.alreadyParsed = true;
		this.cParentSrc = srcTree;
		this.cParentDest = destTree;
		//findCommonParentNode(this.srcCommonParent, this.destCommonParent, store);
		Map<String, String> sTable = VariableVisitor.getSymbolTable(this.cParentSrc);
		setMetaDataToDestTree(this.cParentSrc, this.cParentDest, store, sTable);

		this.cParentOrg = this.cParentSrc.deepCopy();
		TreeUtil.fixAST(this.cParentSrc, this.srcFileText, false);
		TreeUtil.fixAST(this.cParentOrg, this.srcFileText, arg.astOnly());
		TreeUtil.fixAST(this.cParentDest, this.destFileText, arg.astOnly());
		TreeUtil.removeEmptyNode(this.cParentOrg);
		TreeUtil.removeEmptyNode(this.cParentDest);
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
			this.currentIdx = 0;
			setIndices(this.cParentDest);
			//DataTypeVisitor.setDataTypes(this.cParentSrc, this.srcPath);
			//DataTypeVisitor.setDataTypes(this.cParentDest, this.destPath);
			//DataTypeVisitor.setDataTypes(this.cParentOrg, this.srcPath);
		} catch (Exception ex) {
			Util.logln(ex.getMessage() + " " + this.srcPath);
			this.alreadyParsed = false;
			return false;
		}
		return true;
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
			Util.logln("One of those are Null!");
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
		if ((snc > arg.maxChangeSize()) || (dnc > arg.maxChangeSize())) {
			this.alreadyParsed = false;
			return false;
		}
		this.alreadyParsed = true;
		findCommonParentNode(this.srcCommonParent, this.destCommonParent, store);
		Map<String, String> sTable = VariableVisitor.getSymbolTable(this.cParentSrc);
		setMetaDataToDestTree(this.cParentSrc, this.cParentDest, store, sTable);

		this.cParentOrg = this.cParentSrc.deepCopy();
		TreeUtil.fixAST(this.cParentSrc, this.srcFileText, false);
		TreeUtil.fixAST(this.cParentOrg, this.srcFileText, arg.astOnly());
		TreeUtil.fixAST(this.cParentDest, this.destFileText, arg.astOnly());
		TreeUtil.removeEmptyNode(this.cParentOrg);
		TreeUtil.removeEmptyNode(this.cParentDest);
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
			this.currentIdx = 0;
			setIndices(this.cParentDest);
			//DataTypeVisitor.setDataTypes(this.cParentSrc, this.srcPath);
			//DataTypeVisitor.setDataTypes(this.cParentDest, this.destPath);
			//DataTypeVisitor.setDataTypes(this.cParentOrg, this.srcPath);
		} catch (Exception ex) {
			Util.logln(ex.getMessage() + " " + this.srcPath);
			this.alreadyParsed = false;
			return false;
		}
		return true;
	}

	private void setMetaDataToDestTree(ITree srcTree, ITree destTree, MappingStore store, Map<String, String> sTable) {
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

	private void setIndices(ITree root) {
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

	private void findCommonParentRoots(List<ITree> srcCommongParents, List<ITree> destCommonParentRoots,
			MappingStore store) {
		//for(ITree s: srcCommongParents) {
		//	Util.logln(s.toTreeString());
		//}
		//for(ITree d: destCommonParentRoots) {
		//	Util.logln(d.toTreeString());
		//}
		//Util.logln(srcCommonParent.size());
		//Util.logln(destCommonParent.size());
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

	private void findCommonParentNode(List<ITree> srcCommonParent, List<ITree> destCommonParent, MappingStore store) {
		
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

	private void extractCommonParentsChainFromActions(ITree srcTree, ITree destTree, MappingStore store)
			throws Exception {
		ActionGenerator gen = new ActionGenerator(srcTree, destTree, store);
		gen.generate();
		List<Action> actions = gen.getActions();
		//Util.logln(srcTree.toTreeString());
		//Util.logln(destTree.toTreeString());
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

		DateFormat fmt = new SimpleDateFormat("HH-mm-ss");
		Date d = new Date();
		//PrintStream debugStream = new PrintStream(new File("debug-" + arg.maxChangeSize() + "-" + arg.maxTreeSize()
		//		+ "-" + arg.replace() + "-" + arg.astOnly() + "-" + fmt.format(d) + ".txt"));
		Util.logln(fmt.format(d));
		String allFileDirectory = arg.outputFilePath();
		//+ "/all";
		File allFile = new File(allFileDirectory);
		if (!allFile.exists()) {
			allFile.mkdirs();
		}
		int totalFileCount = 0;
		Scanner allFilePathsScanner = new Scanner(new File(arg.allPathsFile()));
		Map<String, List<IcseDatasetParser>> allParsedResults = new HashMap<String, List<IcseDatasetParser>>();
		while (allFilePathsScanner.hasNextLine()) {
			try {
				//Scanner filePathScanner = new Scanner(new File(filePath));
				List<IcseDatasetParser> parserList = new ArrayList<IcseDatasetParser>();
				//while (filePathScanner.hasNextLine()) {
				try {
					String bothPath = allFilePathsScanner.nextLine().trim();
					String[] filePathParts = bothPath.split("\t");
					String parentFile = filePathParts[0];
					String childFile = filePathParts[1];
					//Util.logln(parentFile + " " + childFile);
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
					//Util.logln(srcText);
					TreeContext srcContext = new JdtTreeGenerator().generateFromString(srcText);
					TreeContext destContext = new JdtTreeGenerator().generateFromString(destText);
					ITree srcTree = srcContext.getRoot();
					ITree destTree = destContext.getRoot();
					//Util.dfsPrint(srcTree);
					//Util.dfsPrint(destTree);
					List<NodePair> methodPairs = getMethodPairs(srcTree, destTree, srcText, destText);
					for (NodePair pair : methodPairs) {
						IcseDatasetParser parser = new IcseDatasetParser(parentFile, childFile, srcText, destText);
						boolean original = true;//!arg.replace();
						boolean successfullyParsed = parser.checkSuccessFullParse(pair.srcNode, pair.tgtNode,
								original, arg.excludeStringChange(), arg.replace());
						if (successfullyParsed) {
							//Date current = new Date();
							//String cTime = stfmt.format(current);
							Util.logln(totalFileCount);
							printDataToDirectory(allFileDirectory, Arrays.asList(new IcseDatasetParser[] { parser }));
							//Util.logln(parser.parentCodeString);
							Util.logln(parser.childCodeString);
							totalFileCount++;
							parserList.add(parser);
						}
						/*else {
							System.out.println("git diff " + parentFile + " " + childFile);
						}*/
					}
				} catch (Exception localException) {
				}
				
				//Util.logln(filePath);
				//printTrainAndTestData(parserList);
				//allParsedResults.put(filePath, parserList);
			} catch (Exception localException1) {
			}
		}
		allFilePathsScanner.close();
		//debugStream.close();
	}

	private static List<NodePair> getMethodPairs(ITree srcTree, ITree destTree, String srcText, String destText) {
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
	private static void printTrainAndTestData(Map<String, List<IcseDatasetParser>> allParsedResults) {
		String trainDirectory = arg.outputFilePath() + "/train";
		String testDirectory = arg.outputFilePath() + "/test";
		List<IcseDatasetParser> trainParsers = new ArrayList<IcseDatasetParser>();
		List<IcseDatasetParser> testParsers = new ArrayList<IcseDatasetParser>();
		Set<String> projects = allParsedResults.keySet();
		for (String project : projects) {
			List<IcseDatasetParser> parsers = allParsedResults.get(project);
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

	private static void printTrainAndTestData(List<IcseDatasetParser> parsers) {
		String trainDirectory = arg.outputFilePath() + "/train";
		String testDirectory = arg.outputFilePath() + "/test";
		List<IcseDatasetParser> trainParsers = new ArrayList<IcseDatasetParser>();
		List<IcseDatasetParser> testParsers = new ArrayList<IcseDatasetParser>();
		int totalNumber = parsers.size();
		int testNumber = (int) Math.ceil(totalNumber * testPercentage);
		int trainNumber = totalNumber - testNumber;
		for (int i = 0; i < totalNumber; i++) {
			if (i < trainNumber) {
				trainParsers.add((IcseDatasetParser) parsers.get(i));
			} else {
				testParsers.add((IcseDatasetParser) parsers.get(i));
			}
		}
		printDataToDirectory(trainDirectory, trainParsers);
		printDataToDirectory(testDirectory, testParsers);
	}

	private static void printDataToDirectory(String baseDir, List<IcseDatasetParser> parsers) {
		//Util.logln(baseDir + " " + parsers.size());
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
			for (IcseDatasetParser parser : parsers) {
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
			}
			closeAllPrintStreams(parentCode, parentTree, childCode, childTree, parentOrgTree, parentTypeCode,
					childTypeCode, parentTypeTree, childTypeTree, tokenMasks);
			fileNames.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private boolean checkSuccessFullParse(ITree srcNode, ITree destNode, 
			boolean original, boolean excludeStringChange, boolean replace) {
		try {
			boolean success = false;
			if(!original) {
				success = parseASTDiff(srcNode, destNode);
			}
			else {
				success = getOriginalASTS(srcNode, destNode);
			}
			if (!success) {
				Util.logln("Not Successful");
				return false;
			}
			this.parentCodeString = getParentCodeString(replace).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.parentTreeString = getParentTreeString(replace).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.parentOrgTreeString = getParentOrgTreeString(replace).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)",
					"");
			this.childCodeString = getChildCodeString(replace).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.childTreeString = getChildTreeString(replace).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.parentTypeCodeString = getParentCodeString(replace).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.childTypeCodeString = getChildCodeString(replace).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.childTypeTreeString = getChildTreeString(replace).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.parentOriginalTypeTreeString = getParentOrgTreeString(replace)
					.replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			if ((this.parentCodeString == null) || (this.parentTreeString == null) || (this.childCodeString == null)
					|| (this.childTreeString == null)) {
				Util.logln("something is null");
				return false;
			}
			/*if ((excludeStringChange) && (this.parentCodeString.compareTo(this.childCodeString) == 0)) {
				return false;
			}*/
		} catch (Exception ex) {
			ex.printStackTrace();
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
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getDestTypeTree(this.cParentOrg);
	}

	private String getChildTypeTreeString() {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getDestTypeTree(this.cParentDest);
	}

	private String getChildTypeCodeString() {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getTypedCodeRecusrsive(this.cParentDest);
	}

	private String getParentTypeCodeString() {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getTypedCodeRecusrsive(this.cParentSrc);
	}

	private String getChildTreeString(boolean replace) {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getDestTree(this.cParentDest, replace);
	}

	private String getParentOrgTreeString(boolean replace) {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getDestTree(this.cParentOrg, replace);
	}

	private String getChildCodeString(boolean replace) {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getCodeRecusrsive(this.cParentDest, replace);
	}

	private String getParentTreeString(boolean replace) {
		if (!this.alreadyParsed) {
			return null;
		}
		return Util.getSourceTree(this.cParentSrc);
	}

	private String getParentCodeString(boolean replace) {
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
