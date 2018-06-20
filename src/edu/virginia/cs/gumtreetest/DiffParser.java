package edu.virginia.cs.gumtreetest;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
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
	List<ITree> srcCommonParent = null;
	List<ITree> destCommonParent = null;
	ITree cParentSrc = null;
	ITree cParentDest = null;
	ITree cParentOrg = null;
	private boolean alreadyParsed = false;
	
	public DiffParser(String srcPath, String destPath) throws IOException{
		Run.initGenerators();
		this.srcPath = srcPath;
		this.destPath = destPath;
		currentIdx = 0;
		nonLeafIdx = 200;
		srcFileText = Util.readFile(this.srcPath);
		destFileText = Util.readFile(this.destPath);
		parseASTDiff();
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
	
	/**
	 * @author saikat
	 * @return
	 * @throws IOException
	 */
	public boolean parseASTDiff() throws IOException{
		//# FIXME Extract all the variable that is in scope and and save a list along with the data 
		TreeContext srcContext = new JdtTreeGenerator().generateFromFile(srcPath);
		TreeContext destContext = new JdtTreeGenerator().generateFromFile(destPath);
		ITree srcTree = srcContext.getRoot();
		ITree destTree = destContext.getRoot();
		
		Matcher m = Matchers.getInstance().getMatcher(srcTree, destTree);
		m.match();
		MappingStore store = m.getMappings();
		
		extractCommonParentsChainFromActions(srcTree, destTree, store);
		
		findCommonParentRoots(srcCommonParent, destCommonParent, store);
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
		Util.logln("Src Node : " + snc + " Dest Node : " + dnc);
		if(snc > arg.maxChangeSize() || dnc > arg.maxChangeSize()){
			alreadyParsed = false;
			return false;
		}	
		if(arg.onlyMethodChange() && !Util.checkIfChangeBelongToAnyMethod(cParentSrc, cParentDest)){
			alreadyParsed = false;
			return false;
		}
		
		alreadyParsed = true;
		findCommonParentNode(srcCommonParent, destCommonParent, store);
		Map<String, String> sTable = VariableVisitor.getSymbolTable(cParentSrc);
		setMetaDataToDestTree(cParentSrc, cParentDest, store, sTable);
		TreeUtil.fixAST(cParentSrc, srcFileText);
		TreeUtil.fixAST(cParentDest, destFileText); //Destination AST does not need to be fixed
		TreeUtil.removeEmptyNode(cParentDest);	
		List<String> allVariablesInMethod = new ArrayList<String>(Util.extractAllVariablesInScope(cParentSrc));
		for(String token : allVariablesInMethod){
			allowedTokensString += (token + " ");
		}
		try{
			cParentOrg = cParentSrc.deepCopy();
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
		}catch(InternalError ex){
			Util.logln(ex.getMessage() + " " + srcPath); 
			alreadyParsed = false;
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
		else if(destCommonParent != null){
			for(ITree cDestParent : destCommonParent){
				ITree cSrcParent = store.getSrc(cDestParent);
				if(srcCommonParent == null){
					if(cSrcParent != null){
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						int snc = TreeUtil.countNumberOfNodes(cParentSrc.getParent());
						int dnc = TreeUtil.countNumberOfNodes(cParentDest.getParent());
						Util.logln(snc + " " + dnc);
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
	private void extractCommonParentsChainFromActions(ITree srcTree, ITree destTree, MappingStore store) {
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
		arg = Argument.preprocessArgument(args);
		Util.logln(arg);
		File outputFile = new File(arg.outputFilePath());
		if(!outputFile.exists()){
			outputFile.mkdir();
		}
		Scanner parentScanner = new Scanner(new File(arg.srcFilePath()));
		Scanner childScanner = new Scanner(new File(arg.destFilePath()));
		PrintStream parentCode = new PrintStream(arg.outputFilePath() + "/parent.code");
		PrintStream parentTree = new PrintStream(arg.outputFilePath() + "/parent.tree");
		PrintStream childCode = new PrintStream(arg.outputFilePath() + "/child.code");
		PrintStream childTree = new PrintStream(arg.outputFilePath() + "/child.tree");
		PrintStream parentOrgTree = new PrintStream(arg.outputFilePath() + "/parent.org.tree");
		PrintStream parentTypeCode = new PrintStream(arg.outputFilePath() + "/parent.type.code");
		PrintStream childTypeCode = new PrintStream(arg.outputFilePath() + "/child.type.code");
		PrintStream parentTypeTree = new PrintStream(arg.outputFilePath() + "/parent.org.type.tree");
		PrintStream childTypeTree = new PrintStream(arg.outputFilePath() + "/child.type.tree");
		PrintStream tokenMasks = new PrintStream(arg.outputFilePath() + "/allowed.tokens");
		
		String parentFile = "";
		String childFile = "";
		while(parentScanner.hasNextLine()){
			try{
				parentFile = parentScanner.nextLine();
				childFile = childScanner.nextLine();
				DiffParser parser = new DiffParser(parentFile, childFile);	
				String parentCodeString = parser.getParentCodeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
				String parentTreeString = parser.getParentTreeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
				String parentOrgTreeString = parser.getParentOrgTreeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
				String childCodeString = parser.getChildCodeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
				String childTreeString = parser.getChildTreeString(arg.replace()).replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
				String parentTypeCodeString = parser.getParentTypeCodeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
				String childTypeCodeString = parser.getChildTypeCodeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
				String childTypeTreeString = parser.getChildTypeTreeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
				String parentOriginalTypeTreeString = parser.getParentOriginalTypeTreeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
				if(parentCodeString!= null && parentTreeString!=null && 
						childCodeString!= null && childTreeString!= null ){
					if(arg.excludeStringChange() && (parentCodeString.compareTo(childCodeString)==0)){
						
					}
					else{
						parentCode.println(parentCodeString);
						parentTree.println(parentTreeString);
						childCode.println(childCodeString);
						childTree.println(childTreeString);
						parentOrgTree.println(parentOrgTreeString);
						parentTypeCode.println(parentTypeCodeString);
						childTypeCode.println(childTypeCodeString);
						parentTypeTree.println(parentOriginalTypeTreeString);
						childTypeTree.println(childTypeTreeString);
						tokenMasks.println(parser.allowedTokensString);
						flushAllPrintStreams(parentCode, parentTree, childCode, childTree, parentOrgTree,
								parentTypeCode, childTypeCode, parentTypeTree, childTypeTree, tokenMasks);
					}
					/*Util.logln(parentCodeString + "\n" + parentOrgTreeString + "\n" + childCodeString + "\n" + childTreeString);
					Util.logln(parentTypeCodeString);
					Util.logln(childTypeCodeString);
					Util.logln(parentOriginalTypeTreeString);
					Util.logln(childTypeTreeString);*/
				}
				else{
					//Util.logln("One of the String is null " + parentFile);
				}
			}catch(Exception ex){
				continue;
			}
		}
		parentScanner.close();
		childScanner.close();
		closeAllPrintStreams(parentCode, parentTree, childCode, childTree, parentOrgTree, parentTypeCode, childTypeCode,
				parentTypeTree, childTypeTree, tokenMasks);
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
			Util.logln(replace);
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
