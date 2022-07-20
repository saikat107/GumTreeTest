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

import me.tongfei.progressbar.ProgressBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
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

public class TufanoDiffParser {
	public static Argument arg;
	public String srcPath;
	public String destPath;
	public String srcFileText;
	public String destFileText;
	List<ITree> srcCommonParent = null;
	List<ITree> destCommonParent = null;
	ITree cParentSrc = null;
	ITree cParentDest = null;
	ITree parentOrg = null;
	ITree childOrg = null;
	public boolean alreadyParsed = false;
	public String parentBuggyCodeString = null;
	public String parentBuggyTreeString = null;
	public String parentFullCodeString = null;
	public String parentFullTreeString = null;
	
	public String childFixedCodeString = null;
	public String childFixedTreeString = null;
	public String childFullCodeString = null;
	public String childFullTreeString = null;


	public String sequenceRString = "";
	public int beginPos = 0;
	public int endPos = 0;
	public String commitMessage = "";
	
	public static Set<String> takenPatches = new TreeSet<String>();

	public TufanoDiffParser(String srcPath, String destPath, String srcText, String destText, String commit) throws IOException {
		Run.initGenerators();
		this.srcPath = srcPath;
		this.destPath = destPath;
		this.srcFileText = srcText;
		this.destFileText = destText;
		this.commitMessage = commit;
	}
	
	static class CommitPair{
		public String project;
		public String commit;
		
		public CommitPair(String p, String c) {
			this.project = p;
			this.commit = c;
		}
	}


	public static List<ITree> getMethodNodes(ITree root) {
		// Used
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
		// Used
		this.parentOrg = srcTree.deepCopy();
		this.childOrg = destTree.deepCopy();
		TreeUtil.fixAST(this.parentOrg, this.srcFileText, false);
		TreeUtil.fixAST(this.childOrg, this.destFileText, false);
		TreeUtil.removeEmptyNode(this.parentOrg);
		TreeUtil.removeEmptyNode(this.childOrg);
		
		
		Matcher m = Matchers.getInstance().getMatcher(srcTree, destTree);
		m.match();
		MappingStore store = m.getMappings();
		extractCommonParentsChainFromActions(srcTree, destTree, store);
		findCommonParentRoots(this.srcCommonParent, this.destCommonParent, store);
		// findCommonParentNode(this.srcCommonParent, this.destCommonParent, store);
		if (this.cParentDest == null) {
			this.cParentDest = destTree.deepCopy();
		}
		if (this.cParentSrc == null) {
			this.cParentSrc = srcTree.deepCopy();
		}
		this.alreadyParsed = true;

		this.beginPos = this.cParentSrc.getPos();
		this.endPos = this.cParentSrc.getEndPos();
		
		TreeUtil.fixAST(this.cParentSrc, this.srcFileText, false);
		TreeUtil.fixAST(this.cParentDest, this.destFileText, false);
		TreeUtil.removeEmptyNode(this.cParentSrc);
		TreeUtil.removeEmptyNode(this.cParentDest);
		return true;
	}
	

	public void findCommonParentRoots(List<ITree> srcCommongParents, List<ITree> destCommonParentRoots,
			MappingStore store) {
		// Used
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
		// Used
		if (srcCommonParent != null) {	
			for (ITree cSrcParent : srcCommonParent) {
				ITree cDestParent = store.getDst(cSrcParent);
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
		// Used
		ActionGenerator gen = new ActionGenerator(srcTree, destTree, store);
		gen.generate();
		List<Action> actions = gen.getActions();
		for (Action action : actions) {
			ITree incidentNode = action.getNode();
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

	private static Map<String, CommitPair> parseCommitMessages(String commitFile) throws FileNotFoundException {
		// Used
		Scanner commitScanner = new Scanner(new File(commitFile));
		Map<String, CommitPair> c2m = new HashMap<String, CommitPair>();
		while(commitScanner.hasNextLine()) {
			String line = commitScanner.nextLine().trim();
			String[] parts = line.split(",");
			if (parts.length < 4) continue;
			String project = parts[1].trim();
			String []projectRepo = project.split("/");
			String userName = projectRepo[projectRepo.length - 2];
			String repoName = projectRepo[projectRepo.length - 1];
			String finalName = userName + "/" + repoName;
			c2m.put(
					parts[0].trim(), 
					new CommitPair(finalName, parts[3].trim())
			);
		}
		commitScanner.close();
		return c2m;
	}

	public static List<NodePair> getMethodPairs(ITree srcTree, ITree destTree, String srcText, String destText) {
		// Used
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

	public static void printDataToDirectory(String baseDir, List<TufanoDiffParser> parsers) {
		// Used
		try {
			File baseDirFile = new File(baseDir);
			if (!baseDirFile.exists()) {
				baseDirFile.mkdir();
			}
			PrintStream buggyOnly = new PrintStream(new FileOutputStream(baseDir + "/data.buggy_only", true));
			PrintStream buggyTree = new PrintStream(new FileOutputStream(baseDir + "/data.buggy_tree", true));
			PrintStream prevFullCode = new PrintStream(new FileOutputStream(baseDir + "/data.prev_full_code", true));
			PrintStream prevFullTree = new PrintStream(new FileOutputStream(baseDir + "/data.prev_full_tree", true));
			
			PrintStream fixedOnly = new PrintStream(new FileOutputStream(baseDir + "/data.fixed_only", true));
			PrintStream fixedTree = new PrintStream(new FileOutputStream(baseDir + "/data.fixed_tree", true));
			PrintStream nextFullCode = new PrintStream(new FileOutputStream(baseDir + "/data.next_full_code", true));
			PrintStream nextFullTree = new PrintStream(new FileOutputStream(baseDir + "/data.next_full_tree", true));
			PrintStream fileNames = new PrintStream(new FileOutputStream(baseDir + "/file_paths.txt", true));
			PrintStream commits = new PrintStream(new FileOutputStream(baseDir + "/data.commit_msg", true));
			for (TufanoDiffParser parser : parsers) {
				buggyOnly.println(parser.parentBuggyCodeString);
				buggyTree.println(parser.parentBuggyTreeString);
				prevFullCode.println(parser.parentFullCodeString);
				prevFullTree.println(parser.parentFullTreeString);
				fixedOnly.println(parser.childFixedCodeString);
				fixedTree.println(parser.childFixedTreeString);
				nextFullCode.println(parser.childFullCodeString);
				nextFullTree.println(parser.childFullTreeString);
				commits.println(parser.commitMessage);
				fileNames.println(parser.srcPath + "\t" + parser.destPath);
			}
			closeAllPrintStreams(
					buggyOnly, buggyTree, prevFullCode, prevFullTree, fixedOnly, 
					fixedTree, nextFullCode, nextFullTree, fileNames, commits
			);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	public boolean checkSuccessFullParse(ITree srcNode, ITree destNode, boolean replace, boolean excludeStringChange) {
		// Used
		try {
			boolean success = parseASTDiff(srcNode, destNode);
			if (!success || !alreadyParsed) {
				return false;
			}
			this.parentFullCodeString = Util.getCodeRecusrsive(this.parentOrg, false).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.parentFullTreeString = Util.getDestTree(this.parentOrg, false).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			
			this.parentBuggyCodeString = Util.getCodeRecusrsive(this.cParentSrc, false).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.parentBuggyTreeString = Util.getDestTree(this.cParentSrc, false).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			
			this.childFixedCodeString = Util.getCodeRecusrsive(this.cParentDest, false).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.childFixedTreeString = Util.getDestTree(this.cParentDest, false).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			
			this.childFullCodeString = Util.getCodeRecusrsive(this.childOrg, false).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			this.childFullTreeString = Util.getDestTree(this.childOrg, false).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			
			this.commitMessage = this.commitMessage.replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
			
			String bugText = this.parentBuggyCodeString;
			String fullText = this.parentFullCodeString;
			int index = fullText.indexOf(bugText); 
			if (index >= 0) {
				this.sequenceRString = fullText.substring(0, index) + "<BUG_START> " + bugText + " <BUG_END> " + fullText.substring(index + bugText.length()).trim();
			}
			else {
				this.sequenceRString = "<BUG_START> " + fullText.trim() +  " <BUG_END>";
			}
			if ((this.parentFullCodeString == null) || (this.parentFullTreeString == null) 
					|| (this.parentBuggyCodeString == null) || (this.parentBuggyTreeString == null) 
					|| (this.childFixedCodeString == null) || (this.childFixedTreeString == null) 
					|| (this.childFullCodeString == null) || (this.childFullTreeString == null)) {
				return false;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static void closeAllPrintStreams(PrintStream ... streams) {
		for(PrintStream st : streams) {
			st.close();
		}
	}

	public static void flushAllPrintStreams(PrintStream ... streams) {
		for(PrintStream st : streams) {
			st.flush();
		}
	}
	
	public static void main(String[] args) throws Exception {
		// java -jar TufanoDataParser.jar --all <all_file_path> --out <Output_Dir> --commit_file <Commit_CSV_PATH>
		DateFormat stfmt = new SimpleDateFormat("MM/dd/yy hh:mm:ss");
		Date start = new Date();
		String startTime = stfmt.format(start);
		arg = Argument.preprocessArgument(args);
		
		if(arg.getCommitFile() == null) {
			System.out.println("Must Provide the Commit File for Tufano data parsing");
			System.exit(0);
		}	
		Util.logln(arg);
		Map<String, CommitPair> commitIdToMessage = parseCommitMessages(arg.getCommitFile());
		
		String allFileDirectory = arg.outputFilePath();
		File allFile = new File(allFileDirectory);
		if (!allFile.exists()) {
			allFile.mkdirs();
		}
		
		int totalMethodCount = 0;
		Scanner allFilePathsScanner = new Scanner(new File(arg.allPathsFile()));
		File tmpDir = new File("tmp");
		if(!tmpDir.exists()) {
			tmpDir.mkdirs();
		}
		
		PrintStream exceptionIds = new PrintStream(new FileOutputStream(arg.getExceptionFile()), true);
		int idx = -1;
		int exceptionCount = 0;
		List<String> allLines = new ArrayList<String>();
		while (allFilePathsScanner.hasNextLine()) {
			allLines.add(allFilePathsScanner.nextLine().trim());
		}
		
		Map<String, Integer> countStat = new HashMap<>();
		//while (allFilePathsScanner.hasNextLine()) {
		for(String filePath: ProgressBar.wrap(allLines, "Input Parsing")) {
			idx++;
			String[] parts = filePath.split("\t");
			String pf = parts[0];
			String cf = parts[1];
			String commitId = parts[2];
			try {
				CommitPair cpair = commitIdToMessage.get(commitId);
				String commitMessage = cpair.commit;
				String srcText = Util.readFile(pf);
				String destText = Util.readFile(cf);
				
				srcText = "public class parent {\n" + srcText + "\n}";
				destText = "public class child {\n" + destText + "\n}";
				String parentFile = pf;
				String childFile = cf;
					
				PrintWriter pw = new PrintWriter(new File(parentFile));
				pw.print(srcText);
				pw.close();
						
				PrintWriter cw = new PrintWriter(new File(childFile));
				cw.print(destText);
				cw.close();
						
				TreeContext srcContext = new JdtTreeGenerator().generateFromFile(parentFile);
				TreeContext destContext = new JdtTreeGenerator().generateFromFile(childFile);
				ITree srcTree = srcContext.getRoot();
				ITree destTree = destContext.getRoot();
				List<NodePair> methodPairs = TufanoDiffParser.getMethodPairs(srcTree, destTree, srcText, destText);
				assert methodPairs.size() == 1;
				NodePair pair = methodPairs.get(0);
				TufanoDiffParser parser = new TufanoDiffParser(parentFile, childFile, srcText, destText, commitMessage);
				boolean successfullyParsed = parser.checkSuccessFullParse(pair.srcNode, pair.tgtNode,
							arg.replace(), arg.excludeStringChange());
				if (successfullyParsed) {
					Date current = new Date();
					String cTime = stfmt.format(current);
					if(idx % 1000 == 0) {
						Util.logln(startTime + " -> " + cTime + "\t" + totalMethodCount);
					}
					TufanoDiffParser.printDataToDirectory(allFileDirectory, Arrays.asList(new TufanoDiffParser[] { parser }));
					totalMethodCount++;
					String project = cpair.project;
					int count = 0;
					if(countStat.containsKey(project)) {
						count = countStat.get(project);
					}
					count++;
					countStat.put(project, count);
				}
				else {
					throw new Exception();
				}
			}catch(Exception ex) {
				exceptionIds.println(idx + "\t" + pf);
				exceptionCount++;
			}
		}
		PrintStream statStream = new PrintStream(new FileOutputStream("project-Statistics.log", true));
		statStream.println(args.toString());
		statStream.println("============================================================================");
		statStream.println(countStat.keySet().size() + "");
		for (String key : countStat.keySet()) {
			System.out.println(key + "\t" + countStat.get(key));
			statStream.println(key + "\t" + countStat.get(key));
		}
		statStream.close();
		exceptionIds.close();
		if(tmpDir.exists()) {
			Util.deleteDirectory(tmpDir);
		}
		Util.logln("Total methods counted : " + totalMethodCount + "\tExceptions " + exceptionCount);
		allFilePathsScanner.close();
	}
}
