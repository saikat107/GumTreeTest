package edu.virginia.cs.gumtreetest;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

public class DiffParser {
	
	private static int nonLeafIdx = 200;
	private static int currentIdx = 0;
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
		srcFileText = Util.readFile(this.srcPath);
		destFileText = Util.readFile(this.destPath);
		parseASTDiff();
	}
	
	public void writeDiffs(PrintStream srcDataWriter, PrintStream srcTreeWriter, PrintStream destGrammarWriter){
		Util.printSourceTree(cParentSrc, srcDataWriter, srcTreeWriter);
		Util.printDestTree(cParentDest, destGrammarWriter);
	}
	
	public void parseASTDiff() throws IOException{
		TreeContext srcContext = new JdtTreeGenerator().generateFromFile(srcPath);
		TreeContext destContext = new JdtTreeGenerator().generateFromFile(destPath);
		ITree srcTree = srcContext.getRoot();
		ITree destTree = destContext.getRoot();
		Matcher m = Matchers.getInstance().getMatcher(srcTree, destTree);
		m.match();
		MappingStore store = m.getMappings();
		
		extractCommonParentsChainFromActions(srcTree, destTree, store);
		findCommonParentNode(srcCommonParent, destCommonParent, store);
		fixAST(cParentSrc, srcFileText);
		//fixAST(cParentDest, destFileText) Destination AST does not need to be fixed
		cParentSrc = binarizeAST(cParentSrc);
		//cParentDest = binarizeAST(cParentDest); Destination AST is not needed to be binarized
		setIndices(cParentSrc);	
		setTreeMetadata(cParentSrc);
		setTreeMetadata(cParentDest);
		alreadyParsed = true;
	}
	
	private void setTreeMetadata(ITree root){
		setCountOfLeafNodes(root);
		setTreeHeight(root);
		setTreeDepth(root, 0);
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
	}
	
	private void addNewNodeFromLeftOver(ITree root, List<ITree> newChildren, String leftOver) {
		if(leftOver != null){
			leftOver = leftOver.trim();
			if(leftOver.length() != 0){
				leftOver = Util.removeComment(leftOver).trim();
				if(leftOver.length() != 0){
					ITree child = new Tree(root.getType(), leftOver);
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
						break;
					}
				}
				else if (destCommonParent.contains(cDestParent)){
					cParentSrc = cSrcParent;
					cParentDest = cDestParent;
					break;
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
						break;
					}
				}
				else if (srcCommonParent.contains(cSrcParent)){
					cParentSrc = cSrcParent;
					cParentDest = cDestParent;
					break;
				}
			}
		}
	}

	private void extractCommonParentsChainFromActions(ITree srcTree, ITree destTree, MappingStore store) {
		ActionGenerator gen = new ActionGenerator(srcTree, destTree, store);
		gen.generate();
		List<Action> actions = gen.getActions();
		for(Action action : actions){
			List<ITree> parents = action.getNode().getParents();
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
	
	private ITree normalizeToSubTree(List<ITree> nodes){
		if(nodes.size() == 1){
			return nodes.get(0);
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
	
	private ITree binarizeAST(ITree root) {
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
		DiffParser parser = new DiffParser("src/edu/virginia/cs/gumtreetest/Parent.java", "src/edu/virginia/cs/gumtreetest/Child.java");
		parser.writeDiffs(System.out, System.out, System.out);
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
