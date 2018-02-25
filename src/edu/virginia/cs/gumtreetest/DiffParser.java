package edu.virginia.cs.gumtreetest;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;


import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

public class DiffParser {
	
	public static int MAXIMUM_ALLOWED_NODE = 143;
	public static int MINIMUM_ALLOWED_NODE = 3;
	
	private  int nonLeafIdx = 200;
	private  int currentIdx = 0;
	private String srcPath;
	private String destPath;
	private String srcFileText;
	private String destFileText;
	List<ITree> srcCommonParent = null;
	List<ITree> destCommonParent = null;
	ITree cParentSrc = null;
	ITree cParentDest = null;
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
		TreeContext srcContext = new JdtTreeGenerator().generateFromFile(srcPath);
		TreeContext destContext = new JdtTreeGenerator().generateFromFile(destPath);
		ITree srcTree = srcContext.getRoot();
		ITree destTree = destContext.getRoot();
		Matcher m = Matchers.getInstance().getMatcher(srcTree, destTree);
		m.match();
		MappingStore store = m.getMappings();
		extractCommonParentsChainFromActions(srcTree, destTree, store);
		findCommonParentNode(srcCommonParent, destCommonParent, store);
		if(cParentSrc == null || cParentDest == null){
			return false;
		}
		TreeUtil.fixAST(cParentSrc, srcFileText);
		TreeUtil.fixAST(cParentDest, destFileText); //Destination AST does not need to be fixed
		TreeUtil.removeEmptyNode(cParentDest);
//		Util.dfsPrint(cParentSrc);
		try{
			cParentSrc = TreeUtil.binarizeAST(cParentSrc);
			setIndices(cParentSrc);	
			TreeUtil.setTreeMetadata(cParentSrc);
			TreeUtil.setTreeMetadata(cParentDest);
			int srcLeafNodeCount = currentIdx;
			currentIdx = 0;
			setIndices(cParentDest);
			if(srcLeafNodeCount > MAXIMUM_ALLOWED_NODE * 2){
				alreadyParsed = false;
			}
			else{
				alreadyParsed = true;
			}
		}catch(InternalError ex){
			Util.logln(ex.getMessage() + " " + srcPath); 
			alreadyParsed = false;
		}
		return true;
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
						Util.logln(snc + " " + dnc);
						if(snc > MAXIMUM_ALLOWED_NODE || dnc >  MAXIMUM_ALLOWED_NODE){
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
					if(snc > MAXIMUM_ALLOWED_NODE || dnc >  MAXIMUM_ALLOWED_NODE){
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
						if(snc > MAXIMUM_ALLOWED_NODE || dnc >  MAXIMUM_ALLOWED_NODE){
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
					if(snc > MAXIMUM_ALLOWED_NODE || dnc >  MAXIMUM_ALLOWED_NODE){
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
		/*if(countNumberOfNodes(cParentSrc) > MAXIMUM_ALLOWED_NODE || countNumberOfNodes(cParentDest) > MAXIMUM_ALLOWED_NODE){
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
		if(args.length < 3){
			Util.logln("java -jar DiffParser.jar <Parent file list> <child file list> <output dir> [<Maximum allowed node>]");
			System.exit(0);
		}
		String parentFileList = args[0];
		String childlFileList = args[1];
		String outputDir = args[2];
		if(args.length == 4){
			MAXIMUM_ALLOWED_NODE = Integer.parseInt(args[3]);
		}
		Util.logln(parentFileList + " " + childlFileList + " " + outputDir + " " + MAXIMUM_ALLOWED_NODE);
		Scanner parentScanner = new Scanner(new File(parentFileList));
		Scanner childScanner = new Scanner(new File(childlFileList));
		PrintStream parentCode = new PrintStream(outputDir + "/parent.code");
		PrintStream parentTree = new PrintStream(outputDir + "/parent.tree");
		PrintStream childCode = new PrintStream(outputDir + "/child.code");
		PrintStream childTree = new PrintStream(outputDir + "/child.tree");
		String parentFile = "";
		String childFile = "";
		while(parentScanner.hasNextLine()){
			try{
				parentFile = parentScanner.nextLine();
				childFile = childScanner.nextLine();
				DiffParser parser = new DiffParser(parentFile, childFile);	
				String parentCodeString = parser.getParentCodeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");
				String parentTreeString = parser.getParentTreeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");;
				String childCodeString = parser.getChildCodeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");;
				String childTreeString = parser.getChildTreeString().replaceAll("([\n]+[\r]*)|([\r]+[\n]*)", "");;
				if(parentCodeString!= null && parentTreeString!=null && childCodeString!= null && childTreeString!= null){
					parentCode.println(parentCodeString);
					parentTree.println(parentTreeString);
					childCode.println(childCodeString);
					childTree.println(childTreeString);
					parentCode.flush();
					parentTree.flush();
					childCode.flush();
					childTree.flush();
					
					Util.logln(parentCodeString);
					Util.logln(parentTreeString);
					Util.logln(childCodeString);
					Util.logln(childTreeString);
				}
				else{
					Util.logln("One of the String is null " + parentFile);
				}
			}catch(Exception ex){
//				ex.printStackTrace();
				Util.logln(ex.getMessage() + " " + parentFile);
				continue;
			}
		}
		parentScanner.close();
		childScanner.close();
		parentCode.close();
		parentTree.close();
		childCode.close();
		childTree.close();
	}

	/**
	 * @author saikat
	 * @return
	 */
	private String getChildTreeString() {
		if (!alreadyParsed) return null;
		else{
			return Util.getDestTree(cParentDest);
		}
	}

	/**
	 * @author saikat
	 * @return
	 */
	private String getChildCodeString() {
		if (!alreadyParsed) return null;
		else{
			return Util.getCodeRecusrsive(cParentDest);
		}
	}

	/**
	 * @author saikat
	 * @return
	 */
	private String getParentTreeString() {
		if (!alreadyParsed) return null;
		else{
			return Util.getSourceTree(cParentSrc);
		}
	}

	/**
	 * @author saikat
	 * @return
	 */
	private String getParentCodeString() {
		if (!alreadyParsed) return null;
		else{
			return Util.getCodeRecusrsive(cParentSrc);
		}
	}

}
