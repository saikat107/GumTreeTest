package edu.columbia.cs.diffparser.method;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

import edu.virginia.cs.gumtreetest.TreeUtil;
import edu.virginia.cs.gumtreetest.Util;

public class MethodLevelDiffParser {
	
	public static class Args {
		
	}
	public static Args arg;
	public int nonLeafIdx = 200;
	public int currentIdx = 0;
	public String srcPath;
	public String destPath;
	public String srcFileText;
	public String destFileText;

	public MethodLevelDiffParser(String srcPath, String destPath, String srcText, String destText) throws IOException {
		Run.initGenerators();
		this.srcPath = srcPath;
		this.destPath = destPath;
		this.srcFileText = srcText;
		this.destFileText = destText;
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

	public static ITree removeJavaDocNodes(ITree srcTree) {
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
			curr.setChildren(newChildren);
		}
		return srcTree;
	}

	public static List<NodePair> getMethodPars(String parentFile, String childFile) throws IOException {
		TreeContext srcContext = new JdtTreeGenerator().generateFromFile(parentFile);
		TreeContext destContext = new JdtTreeGenerator().generateFromFile(childFile);
		String srcText = Util.readFile(parentFile);
		String destText = Util.readFile(childFile);
		ITree srcTree = srcContext.getRoot();
		ITree destTree = destContext.getRoot();
		List<NodePair> methodPairs = MethodLevelDiffParser.getMethodPairs(srcTree, destTree, srcText, destText);
		return methodPairs;
	}
	
	public static boolean isChanged(ITree src, ITree dest) {
		Matcher m = Matchers.getInstance().getMatcher(src, dest);
		m.match();
		MappingStore store = m.getMappings();
		ActionGenerator gen = new ActionGenerator(src, dest, store);
		gen.generate();
		List<Action> actions = gen.getActions();
		return actions.size() > 0;
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
	
	public List<NodePair> getChangedMethods(){
		List<NodePair> changedMethodPairs = new ArrayList<NodePair>();
		try {
			List<NodePair> methodPairs = getMethodPars(this.srcPath, this.destPath);
			for(NodePair pair : methodPairs) {
				TreeUtil.fixAST(pair.srcNode, this.srcFileText, false);
				TreeUtil.fixAST(pair.tgtNode, this.destFileText, false);
				ITree src = pair.srcNode;
				ITree tgt = pair.tgtNode;
				String srcCode = Util.getCodeRecusrsive(src, false);
				String destCode = Util.getCodeRecusrsive(tgt, false);
				if (srcCode.compareTo(destCode) != 0) {
					pair.srcText = srcCode;
					pair.tgtText = destCode;
					changedMethodPairs.add(pair);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return changedMethodPairs;
	}
	
	public static void main(String[] args) throws IOException {	
		String filePairPath = args[0];
		String destFolder = args[1];
		File destFolderFile = new File(destFolder);
		if(!destFolderFile.exists()) {
			destFolderFile.mkdirs();
		}
		PrintWriter buggyFile = new PrintWriter(new File(destFolder, "buggy.java")); 
		PrintWriter fixedFile = new PrintWriter(new File(destFolder, "fixed.java")); 
		PrintWriter bugids = new PrintWriter(new File(destFolder, "buggids.txt"));
		PrintWriter filePaths = new PrintWriter(new File(destFolder, "filePaths.txt"));
		Scanner scanner = new Scanner(new File(filePairPath));
		Set<String> bugIdsSet = new TreeSet<String>();
		PrintWriter allCodeLengthList = new PrintWriter(new File("allCodeLength.tsv"));
		PrintWriter takenCodeLengthList = new PrintWriter(new File("takenCodeLength.tsv"));
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine().trim();
			String []parts = line.split("\t");
			String bugid = parts[0].trim();
			System.out.println(bugid);
			String srcFile = parts[1].trim();
			String tgtFile = parts[2].trim();
			String srcFileText = Util.readFile(srcFile);
			String tgtFileText = Util.readFile(tgtFile);
			MethodLevelDiffParser parser = new MethodLevelDiffParser(srcFile, tgtFile, srcFileText, tgtFileText);
			List<NodePair> changedMethod = parser.getChangedMethods();
			for(NodePair pair : changedMethod) {
				if(pair.srcLength < 256 && pair.tgtLength < 256) {
					takenCodeLengthList.println(pair.srcLength + "\t" + pair.tgtLength);
					bugIdsSet.add(bugid);
					bugids.println(bugid);
					filePaths.println(srcFile);
					buggyFile.println(pair.srcText);
					buggyFile.println(pair.tgtText);
				}
				allCodeLengthList.println(pair.srcLength + "\t" + pair.tgtLength);
			}
		}
		buggyFile.close();
		fixedFile.close();
		scanner.close();
		bugids.close();
		filePaths.close();
		
		allCodeLengthList.close();
		takenCodeLengthList.close();
		PrintWriter takenBugids = new PrintWriter(new File("takenBugids.tsv"));
		for(String bid : bugIdsSet) {
			takenBugids.println(bid);
		}
		takenBugids.close();
	}
}

class NodePair {
	ITree srcNode;
	ITree tgtNode;
	String srcText;
	String tgtText;
	int srcLength;
	int tgtLength;

	public NodePair(ITree s, ITree d, String sText, String dText) {
		this.srcNode = s;
		this.tgtNode = d;
		this.srcText = sText;
		this.tgtText = dText;
		this.srcLength = this.srcText.split(" ").length;
		this.tgtLength = this.tgtText.split(" ").length;
	}
	
	public String toString() {
		this.srcLength = this.srcText.split(" ").length;
		this.tgtLength = this.tgtText.split(" ").length;
		return "Src: " + this.srcText + "\n"
				+ "\tLength: " + this.srcLength + "\n"
				+ "Tgt: " + this.tgtText + "\n"
				+ "\tLength: " + this.tgtLength + "\n";
	}
}
