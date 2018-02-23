package edu.virginia.cs.gumtreetest;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
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
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

public class DiffParser {
	
	public static int MAXIMUM_ALLOWED_NODE = 2000;
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
	
	public void writeDiffs(PrintStream srcDataWriter, PrintStream srcTreeWriter, PrintStream destCodeWriter, PrintStream destTreeWriter){
		if(alreadyParsed){
			Util.printSourceTree(cParentSrc, srcDataWriter, srcTreeWriter);
			Util.printDestCodeAndTree(cParentDest, destCodeWriter, destTreeWriter);
		}
	}
	
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
		fixAST(cParentSrc, srcFileText);
		fixAST(cParentDest, destFileText); //Destination AST does not need to be fixed
		removeEmptyNode(cParentDest);
		
		try{
			cParentSrc = binarizeAST(cParentSrc);
			setIndices(cParentSrc);	
			setTreeMetadata(cParentSrc);
			setTreeMetadata(cParentDest);
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
	
	
	private void setTreeMetadata(ITree root){
		setCountOfLeafNodes(root);
		int height = setTreeHeight(root);
		setTreeDepth(root, 0);
		if(height > 100){
			throw new InternalError("Too long tree");
		}
	}
	
	
	private void removeEmptyNode(ITree root){
		if(Util.isLeafNode(root) && root.getLabel().trim().length() == 0){
			root.setLabel("<EMPTY>");
		}
		else{
			List<ITree> children = root.getChildren();
			for(ITree child : children){
				removeEmptyNode(child);
			}
		}
	}
	
	
	private void setTreeDepth(ITree root, int d){
		root.setMetadata(Config.METADATA_TAG.DEPTH, d);
		List<ITree> children = root.getChildren();
		for(ITree child : children){
			setTreeDepth(child, d + 1);
		}
	}
	
	private int setTreeHeight(ITree root){
		List<ITree> children = root.getChildren();
		int height = -1;
		if(children.size() == 0){
			height = 0;
		}
		else{
			for(ITree child : children){
				int h = 1 + setTreeHeight(child);
				if(h > height){
					height = h;
				}
			}
		}
		root.setMetadata(Config.METADATA_TAG.HEIGHT, height);
		return height;
	}
	
	private int setCountOfLeafNodes(ITree root){
		int leaf = 1;
		List<ITree> children = root.getChildren();
		if (children.size() != 0){
			leaf = 0;
			for(ITree child : children){
				leaf += setCountOfLeafNodes(child);
			}
		}
		root.setMetadata(Config.METADATA_TAG.NUM_LEAF_NODE, leaf);
		return leaf;
	}
	

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
	
	private void fixAST(ITree root, String src) {
		int bi = root.getPos();
		int fi = 0;
		List<ITree> children = root.getChildren();
		List<ITree> newChildren = new ArrayList<>();
		int numChildren = children.size();
		children = root.getChildren();
		if(numChildren > 0){
			for(int i = 0; i < numChildren; i++){
				ITree child = children.get(i);
				fi = child.getPos();
				if(fi >= bi){
					String leftOver = src.substring(bi, fi);
					addNewNodeFromLeftOver(root, newChildren, leftOver);
				}
				child.setParent(root);
				newChildren.add(child);
				bi = child.getEndPos();
			}
			fi = root.getEndPos();
			if(fi > bi){
				String leftOver = src.substring(bi, fi);
				addNewNodeFromLeftOver(root, newChildren, leftOver);
			}
			ITree child = null;
			List<ITree> orgChildren = root.getChildren();
			for(int i = 0; i < orgChildren.size(); i++){
				child = orgChildren.get(i);
				fixAST(child, src);
			}
		}
		root.setChildren(newChildren);
		if(root.getType() == 34){
			root.setLabel("NUMBER_CONSTANT");
		}
		else if (root.getType() == 45){
			root.setLabel("STRING_CONSTANT");
		}
	}
	
	private void addNewNodeFromLeftOver(ITree root, List<ITree> newChildren, String leftOver) {
		if(leftOver != null){
			leftOver = leftOver.trim();
			if(leftOver.length() != 0){
				leftOver = Util.removeComment(leftOver).trim();
				if(leftOver.length() != 0){
					int type = Util.getNodeTypeFromLeftOver(root, leftOver);
//					Util.logln(type);
					ITree child = new Tree(type, leftOver);
					child.setId(3);
					child.setParent(root);
					newChildren.add(child);
				}
			}
		}
	}

	private void findCommonParentNode(List<ITree> srcCommonParent, List<ITree> destCommonParent, MappingStore store) {
		if(srcCommonParent != null){
			for(ITree cSrcParent : srcCommonParent){
				ITree cDestParent = store.getDst(cSrcParent);
				if(destCommonParent == null){
					if(cDestParent != null){
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						int snc = countNumberOfNodes(cParentSrc.getParent());
						int dnc = countNumberOfNodes(cParentDest.getParent());
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
					int snc = countNumberOfNodes(cParentSrc.getParent());
					int dnc = countNumberOfNodes(cParentDest.getParent());
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
						int snc = countNumberOfNodes(cParentSrc.getParent());
						int dnc = countNumberOfNodes(cParentDest.getParent());
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
					int snc = countNumberOfNodes(cParentSrc.getParent());
					int dnc = countNumberOfNodes(cParentDest.getParent());
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

	private int countNumberOfNodes(ITree root) {
		if(root == null){
			return 0;
		}
		else{
			List<ITree> children = root.getChildren();
			int nodes = 1;
			for(ITree child : children){
				nodes += countNumberOfNodes(child);
			}
			return nodes;
		}
	}

	private void extractCommonParentsChainFromActions(ITree srcTree, ITree destTree, MappingStore store) {
		ActionGenerator gen = new ActionGenerator(srcTree, destTree, store);
		gen.generate();
		List<Action> actions = gen.getActions();
		for(Action action : actions){
			ITree incidentNode = action.getNode();
			List<ITree> parents = incidentNode.getParents();
			
			if (action.getName() == "INS"){
				parents.add(0, action.getNode());
				if (destCommonParent == null){
					destCommonParent = parents;
				}
				else{
					destCommonParent = getCommonParents(destCommonParent, parents);
				}
			}
			else if(action.getName() == "MOV"){
				if (srcCommonParent == null){
					srcCommonParent = parents;
				}
				else{
					srcCommonParent = getCommonParents(srcCommonParent, parents);
				}
				ITree destNode = store.getDst(action.getNode());
				List<ITree> destParents = destNode.getParents();
				destParents.add(0, destNode);
				if (destCommonParent == null){
					destCommonParent = destParents;
				}
				else{
					destCommonParent = getCommonParents(destCommonParent, destParents);
				}
			}
			else{
				parents.add(0, action.getNode());
				if (srcCommonParent == null){
					srcCommonParent = parents;
				}
				else{
					srcCommonParent = getCommonParents(srcCommonParent, parents);
				}
			}
		}
	}
	
	private ITree normalizeToSubTree(List<ITree> nodes) throws InternalError{
		if(nodes.size() == 1){
			return nodes.get(0);
		}
		else if(nodes.size() > 100){
			throw new InternalError("Large number of nodes");
		}
		else{
			List<ITree> binaryChildren = new ArrayList<ITree>();
			ITree leftChild = nodes.get(0);
			List<ITree> right = new ArrayList<ITree>();
			for(int i = 1; i < nodes.size(); i++){
				right.add(nodes.get(i));
			}
			ITree rightChild = normalizeToSubTree(right);
			binaryChildren.add(leftChild);
			binaryChildren.add(rightChild);
			ITree head = new Tree(Config.INTERMEDIATE_NODE_TYPE , "");	
			head.setChildren(binaryChildren);
			return head;
		}
	}
	
	private ITree binarizeAST(ITree root) throws InternalError{
		List<ITree> children = root.getChildren();
		List<ITree> newChildren = new ArrayList<ITree>();
		if(children.size() == 0){
			return root;
		}
		else if(children.size() == 1){
			return binarizeAST(children.get(0));
		}
		else{
			for(ITree child : children){
				newChildren.add(binarizeAST(child));
			}
			ITree secondRoot = normalizeToSubTree(newChildren);
			root = secondRoot;
		}
		return root;
	}
	
	public static void main(String[] args) throws UnsupportedOperationException, IOException {
		if(args.length < 4){
			Util.logln("java -jar DiffParser.jar <Parent file list> <child file list> <output dir> <Maximum allowed node>");
			System.exit(0);
		}
		String parentFileList = args[0];
		String childlFileList = args[1];
		String outputDir = args[2];
		//MAXIMUM_ALLOWED_NODE = Integer.parseInt(args[3]);
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

	private String getChildTreeString() {
		if (!alreadyParsed) return null;
		else{
			return Util.getDestTree(cParentDest);
		}
	}

	private String getChildCodeString() {
		if (!alreadyParsed) return null;
		else{
			return Util.getCodeRecusrsive(cParentDest);
		}
	}

	private String getParentTreeString() {
		if (!alreadyParsed) return null;
		else{
			return Util.getSourceTree(cParentSrc);
		}
	}

	private String getParentCodeString() {
		if (!alreadyParsed) return null;
		else{
			return Util.getCodeRecusrsive(cParentSrc);
		}
	}

	private static List<ITree> getCommonParents(List<ITree> commonParent, List<ITree> parents) {
		for(ITree parent : parents){
			if(commonParent.contains(parent)){
				commonParent = parent.getParents();
				commonParent.add(0, parent);
				//Util.logln(destCommonParent);
				break;
			}
		}
		return commonParent;
	}

}
