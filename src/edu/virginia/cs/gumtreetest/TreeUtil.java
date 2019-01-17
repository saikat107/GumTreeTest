package edu.virginia.cs.gumtreetest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

public class TreeUtil {
	
	/**
	 * @author saikat
	 * @param nodes
	 * @return
	 * @throws InternalError
	 */
	public static ITree normalizeToSubTree(List<ITree> nodes) throws InternalError{
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
	
	
	/**
	 * @author saikat
	 * @param root
	 * @return
	 * @throws InternalError
	 */
	public static ITree binarizeAST(ITree root) throws InternalError{
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

	
	/**
	 * @author saikat
	 * @param root
	 * @param d
	 */
	public static void setTreeDepth(ITree root, int d){
		root.setMetadata(Config.METADATA_TAG.DEPTH, d);
		List<ITree> children = root.getChildren();
		for(ITree child : children){
			setTreeDepth(child, d + 1);
		}
	}

	
	/**
	 * @author saikat
	 * @param root
	 * @return
	 */
	public static int setTreeHeight(ITree root){
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
	
	
	/**
	 * @author saikat
	 * @param root
	 * @return
	 */
	public static int setCountOfLeafNodes(ITree root){
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

	
	/**
	 * @author saikat
	 * @param root
	 */
	public static void setTreeMetadata(ITree root){
		setCountOfLeafNodes(root);
		int height = setTreeHeight(root);
		TreeUtil.setTreeDepth(root, 0);
		if(height > 100){
			throw new InternalError("Too long tree");
		}
	}

	
	/**
	 * @author saikat
	 * @param root
	 */
	public static void removeEmptyNode(ITree root){
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

	
	public static void removeLabelFromNonTerminal(ITree root) {
		if(!root.isLeaf()) {
			root.setLabel("");
			List<ITree> children = root.getChildren();
			for(ITree child : children) {
				removeLabelFromNonTerminal(child);
			}
		}
	}
	
	/**
	 * @author saikat
	 * @param root
	 * @param src
	 */
	public static void fixAST(ITree root, String src, boolean astOnly) {
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
					if(!astOnly){
						addNewNodeFromLeftOver(root, newChildren, leftOver, bi);
					}
				}
				child.setParent(root);
				child.setLabel(child.getLabel().trim());
				if(!astOnly){
					if(child.getLabel().equals("") && child.isLeaf()){
						String leftOver = src.substring(child.getPos(), child.getEndPos()).trim();
						if(child.getType() == Config.ASTTYPE_TAG.ARRAY_IDX){
							leftOver = leftOver.replaceAll(" ", "").replaceAll("\t", "").replaceAll("\n", "").replaceAll("\r", "").replaceAll("[ ]+", " ");
							if(leftOver.equals("[]")){
								child.setLabel("[]");
							}
							else{
								child.setLabel("[");
								child.setType(221);
							}
						}
						else if(child.getType() == Config.ASTTYPE_TAG.BODY){
							child.setLabel("");
							String l = "{}";
							ITree t = child.deepCopy();
							t.setLabel(l);
							t.setType(Util.getNodeTypeFromLeftOver(child, l));
							t.setParent(child);
							t.setChildren(new ArrayList<ITree>());
							child.addChild(t);
						}
						else{
							if(leftOver.length() != 0){
								leftOver = Util.removeComment(leftOver).trim();
								if(leftOver.length() != 0){
									leftOver = leftOver.trim();
									leftOver = leftOver.replaceAll(" ", "").
											replaceAll("\t", "").replaceAll("\n", "").replaceAll("\r", "").replaceAll("[ ]+", " ");
									int type = Util.getNodeTypeFromLeftOver(root, leftOver);
									child.setType(type);
									child.setLabel(leftOver);
								}
							}
						}
					}
				}
				if(child.getType() != Config.ASTTYPE_TAG.JAVADOC && 
						root.getType() != Config.ASTTYPE_TAG.INSIDE_JAVADOC1 &&
						root.getType() != Config.ASTTYPE_TAG.INSIDE_JAVADOC2)
					newChildren.add(child);
				bi = child.getEndPos();
			}
			fi = root.getEndPos();
			if(fi > bi){
				String leftOver = src.substring(bi, fi);
				if(!astOnly) addNewNodeFromLeftOver(root, newChildren, leftOver, bi);
			}
			ITree child = null;
			List<ITree> orgChildren = root.getChildren();
			for(int i = 0; i < orgChildren.size(); i++){
				child = orgChildren.get(i);
				fixAST(child, src, astOnly);
			}
		}
		root.setChildren(newChildren);
		if(root.getType() == Config.ASTTYPE_TAG.NUMBER_CONSTANT){
			root.setLabel("NUMBER_CONSTANT");
		}
		else if (root.getType() == Config.ASTTYPE_TAG.STRING_CONSTANT){
			root.setLabel("STRING_CONSTANT");
		}
		else if(root.getType() == Config.ASTTYPE_TAG.CHAR_CONST){
			root.setLabel("CHAR_CONS");
		}
		else if(root.getType() == Config.ASTTYPE_TAG.JAVADOC ||
				root.getType() == Config.ASTTYPE_TAG.INSIDE_JAVADOC1 ||
				root.getType() == Config.ASTTYPE_TAG.INSIDE_JAVADOC2){
			root.setChildren(new ArrayList<ITree>());
			root.setLabel("JAVADOC");
		}
	}

	
	public static void fixASTKeepValues(ITree root, String src) {
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
					addNewNodeFromLeftOver(root, newChildren, leftOver, bi);
				}
				child.setParent(root);
				child.setLabel(child.getLabel().trim());
				
					if(child.getLabel().equals("") && child.isLeaf()){
						String leftOver = src.substring(child.getPos(), child.getEndPos()).trim();
						if(child.getType() == Config.ASTTYPE_TAG.ARRAY_IDX){
							leftOver = leftOver.replaceAll(" ", "").replaceAll("\t", "").replaceAll("\n", "").replaceAll("\r", "").replaceAll("[ ]+", " ");
							if(leftOver.equals("[]")){
								child.setLabel("[]");
							}
							else{
								child.setLabel("[");
								child.setType(221);
							}
						}
						else if(child.getType() == Config.ASTTYPE_TAG.BODY){
							child.setLabel("");
							String l = "{}";
							ITree t = child.deepCopy();
							t.setLabel(l);
							t.setType(Util.getNodeTypeFromLeftOver(child, l));
							t.setParent(child);
							t.setChildren(new ArrayList<ITree>());
							child.addChild(t);
						}
						else{
							if(leftOver.length() != 0){
								leftOver = Util.removeComment(leftOver).trim();
								if(leftOver.length() != 0){
									leftOver = leftOver.trim();
									leftOver = leftOver.replaceAll(" ", "").
											replaceAll("\t", "").replaceAll("\n", "").replaceAll("\r", "").replaceAll("[ ]+", " ");
									int type = Util.getNodeTypeFromLeftOver(root, leftOver);
									child.setType(type);
									child.setLabel(leftOver);
								}
							}
						}
					}
				
				if(child.getType() != Config.ASTTYPE_TAG.JAVADOC && 
						root.getType() != Config.ASTTYPE_TAG.INSIDE_JAVADOC1 &&
						root.getType() != Config.ASTTYPE_TAG.INSIDE_JAVADOC2)
					newChildren.add(child);
				bi = child.getEndPos();
			}
			fi = root.getEndPos();
			if(fi > bi){
				String leftOver = src.substring(bi, fi);
				addNewNodeFromLeftOver(root, newChildren, leftOver, bi);
			}
			ITree child = null;
			List<ITree> orgChildren = root.getChildren();
			for(int i = 0; i < orgChildren.size(); i++){
				child = orgChildren.get(i);
				fixASTKeepValues(child, src);
			}
		}
		root.setChildren(newChildren);
		
		if(root.getType() == Config.ASTTYPE_TAG.JAVADOC ||
				root.getType() == Config.ASTTYPE_TAG.INSIDE_JAVADOC1 ||
				root.getType() == Config.ASTTYPE_TAG.INSIDE_JAVADOC2){
			root.setChildren(new ArrayList<ITree>());
			root.setLabel("/*JAVADOC*/");
		}
	}
	
	/**
	 * @author saikat
	 * @param root
	 * @param newChildren
	 * @param leftOver
	 */
	public static void addNewNodeFromLeftOver(ITree root, List<ITree> newChildren, String leftOver, int position) {
		if(leftOver != null){
			leftOver = leftOver.trim();
			if(leftOver.length() != 0){
				leftOver = Util.removeComment(leftOver).trim();
				if(leftOver.length() != 0){
					leftOver = leftOver.trim();
					leftOver = leftOver.replaceAll(" ", "").replaceAll("\t", "").replaceAll("\n", "").replaceAll("\r", "");
					int type = Util.getNodeTypeFromLeftOver(root, leftOver);
					if(type == Config.ASTTYPE_TAG.REST_OF_LEFTOVER){
						List<ITree> nodes = Util.getDecomposedLeftOver(root, leftOver, position);
						for(ITree node : nodes){
							newChildren.add(node);
						}
					}
					else{	
						ITree child = new Tree(type, leftOver);
						child.setId(3);
						child.setParent(root);
						newChildren.add(child);
						child.setPos(position);
					}
				}
			}
		}
	}
	
	
	/**
	 * @author saikat
	 * @param root
	 * @return
	 */
	public static int countNumberOfNodes(ITree root) {
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


	public static void discoverMethodNames(ITree srcTree, boolean partOfMethod) {
		if(srcTree.getType() == Config.ASTTYPE_TAG.METHOD_DECLARATION){
			partOfMethod = true;
		}
		srcTree.setMetadata("method", partOfMethod);
		for(ITree child : srcTree.getChildren()){
			discoverMethodNames(child, partOfMethod);
		}
	}
	
	
	public static boolean matches(ITree first, ITree second) {
		if(first.getType() == second.getType()) {
			if(first.getType() == Config.ASTTYPE_TAG.NUMBER_CONSTANT 
					|| first.getType() == Config.ASTTYPE_TAG.STRING_CONSTANT 
					|| first.getType() == Config.ASTTYPE_TAG.CHAR_CONST) {
				return true;
			}
			if(first.getLabel().trim().compareTo(second.getLabel()) == 0) {
				if(first.getChildren().size() == second.getChildren().size()) {
					List<ITree> firstChildren = first.getChildren();
					List<ITree> secondChildren = second.getChildren();
					int numChildren = firstChildren.size();
					boolean allMatch = true;
					for(int i = 0; i < numChildren; i++) {
						ITree firstChild = firstChildren.get(i);
						ITree secondChild = secondChildren.get(i);
						allMatch &= matches(firstChild, secondChild);
					}
					return allMatch;
				}
			}
		}
		return false;
	}
	
	public static ITree findSubTree(ITree root, ITree subTree) {
		if(matches(root, subTree)) {
			return root;
		}
		for(ITree child : root.getChildren()) {
			ITree sTree = findSubTree(child, subTree);
			if(sTree != null) {
				return sTree;
			}
		}
		return null;
	}
	
	
	public static ITree createTreeFromTreeString(String treeStr) {
		//Util.logln(treeStr);
		String []tokens = treeStr.trim().split(" ");
		Stack<Object> st = new Stack<>();
		for(String token : tokens) {
			token = token.trim();
			if(token.length() == 0) continue;
			if(token.compareTo("`") == 0) {
				st.push(token);
			}
			else if(token.compareTo("``") == 0) {
				List<Object> children = new ArrayList<>();
				Object topOfStack = st.pop();
				String topStackStr = "";
				if(topOfStack instanceof ITree) {
					topStackStr = ((ITree) topOfStack).toShortString();
				}
				else{
					topStackStr = topOfStack.toString();
				}
				while(topStackStr.compareTo("`") != 0) {
					children.add(topOfStack);
					topOfStack = st.pop();
					if(topOfStack instanceof ITree) {
						topStackStr = ((ITree) topOfStack).toShortString();
					}
					else{
						topStackStr = topOfStack.toString();
					}
				}
				if(st.isEmpty()) {
					st.push(children.get(0));
				}
				else {
					topOfStack = st.pop();
					ITree tree = (ITree) topOfStack;
					for(Object child : children) {
						ITree ch = (ITree) child;
						ch.setParent(tree);
						tree.addChild(ch);
					}
					st.push(tree);
				}
			}
			else {
				String label = token;
				int type = -58989;
				try {
					type = Integer.parseInt(token);
					label = "";
				}
				catch(NumberFormatException ex) {
					
				}
				ITree node = new Tree(type, label);
				st.push(node);
			}
		}
		ITree root = (ITree) st.pop();
		fixGeneratedTree(root);
		//Util.dfsPrint(root);
		return root.getChild(0);
	}
	
	private static void fixGeneratedTree(ITree root) {
		List<ITree> children = root.getChildren();
		if(root.isLeaf()) return;
		if(children.size() == 1 && children.get(0).getType() == -58989) {
			root.setLabel(children.get(0).getLabel());
			root.setChildren(new ArrayList<ITree>());
		}
		else {
			for(ITree child : children) {
				fixGeneratedTree(child);
			}
		}
	}


	public static ITree createCopyTree(ITree src) {
		ITree root = new Tree(src.getType(), src.getLabel());
		for(ITree child : src.getChildren()) {
			root.addChild(createCopyTree(child));
		}
		return root;
	}
	
	/**
	 * 
	 * @param parentFilePath Path of the buggy file
	 * @param parentTree Tree String of the buggy file
	 * @param patchCode plain fixed code as generated by Codit
	 * @return final patched file
	 * @throws IOException
	 */
	public static String getPatchedFile(String parentFilePath, String parentTree, String patchCode) throws IOException {
		String srcText = Util.readFile(parentFilePath);
		TreeContext srcContext = new JdtTreeGenerator().generateFromFile(parentFilePath);
		ITree srcTree = srcContext.getRoot();
		fixAST(srcTree, srcText, false);
		String dStr = parentTree;
		ITree dTree = createTreeFromTreeString(dStr);
		ITree srcNode = findSubTree(srcTree, dTree);		
		srcNode.setLabel(patchCode);
		srcNode.setChildren(new ArrayList<>());
		String finalCode = Util.getFormattedCode(srcTree);
		Util.logln(finalCode);
		return finalCode;
	}
	
	
	/**
	 * Given a child file and index of child method number, it will 
	 * @param parentFilePath Path of the buggy file
	 * @param parentTree Tree String of the buggy file
	 * @param patchCode plain fixed code as generated by Codit
	 * @return final patched file
	 * @throws IOException
	 */
	public static List<String> getPatchedFileFromChildFile(String childFilePath, 
			String childTree, List<String> patchCodes) throws IOException {
		List<String> modifiedCodes = new ArrayList<String>();
		String dStr = childTree;
		ITree dTree = createTreeFromTreeString(dStr);
		//Util.dfsPrint(dTree);
		String srcText = Util.readFile(childFilePath);
		for(String patchCode : patchCodes) {
			TreeContext srcContext = new JdtTreeGenerator().generateFromFile(childFilePath);
			ITree srcTree = srcContext.getRoot();
			fixASTKeepValues(srcTree, srcText);
			removeLabelFromNonTerminal(srcTree);
			//Util.dfsPrint(srcTree);
			ITree srcNode = findSubTree(srcTree, dTree);
			srcNode.setLabel(patchCode);
			srcNode.setChildren(new ArrayList<>());
			String finalCode = Util.getFormattedCode(srcTree);
			//Util.logln(finalCode);
			modifiedCodes.add(finalCode);
		}
		return modifiedCodes;
	}
	
	public static void main(String[] args) throws Exception {
		/**
		 * TODO 
		 * 1. Parse the test result file
		 * 2. Write script/framework to run the tests 
		 * 3. List all the failing tests
		 * 4. Write script for parsing the test results
		 * 5. Execute all
		 */
		
		
		
		
		/*String childFile = "test/Closure_9.java";
		String childTree = "(AST_ROOT_SC2NF (60 (43 (42{val=String})) (59 (42{val=moduleName}) (204{val==}) (32 (42{val=guessCJSModuleName}) (214{val=(}) (32 (42{val=script}) (235{val=.}) (42{val=getSourceFileName}) (216{val=()})) (215{val=)}))) (227{val=;})))";
		childTree = reFormatTreeStr(childTree);
		List<String> patchCodes = new ArrayList<String>();
		for(int i = 2; i < args.length; i++) {
			patchCodes.add(args[i]);
		}
		patchCodes.add("String moduleName = guessCJSModuleName ( script . getSourceFileName () ) ;");
		
		/*String treeStr = "(AST_ROOT_SC2NF (32 (42{val=getName}) (216{val=()})))";
		treeStr = reFormatTreeStr(treeStr);
		ITree tree = createTreeFromTreeString(treeStr);
		Util.dfsPrint(tree);*/
	}


	/**
	 * Reformat python style tree string to java style tree string
	 * @param treeStr
	 * @return formatted treeStr
	 */
	public static String reFormatTreeStr(String treeStr) {
		StringBuffer buffer = new StringBuffer();
		for(int i = 0; i < treeStr.length(); i++) {
			char ch = treeStr.charAt(i);
			if(ch == '(') {
				if(i == 0 || treeStr.charAt(i-1) != '=') {
					buffer.append("` ");
				}
				else if(treeStr.charAt(i+1) == ')') {
					i++;
					buffer.append("()");
				}
				else {
					buffer.append("(");
				}
			}
			else if (ch == ')') {
				if(treeStr.charAt(i-1) != '=') {
					buffer.append(" ``");
				}
				else {
					buffer.append(")");
				}
			}
			else {
				buffer.append(treeStr.charAt(i));
			}
		}
		treeStr = buffer.toString();
		String []parts = treeStr.split(" ");
		buffer = new StringBuffer();
		for(String part: parts) {
			if(part.contains("{val=")) {
				part = part.substring(0, part.length()-1);
				int bidx = part.indexOf("{val=");
				String beforePart = part.substring(0, bidx);
				String afterPart = part.substring(bidx+5);
				buffer.append(beforePart);
				buffer.append(" ` ");
				buffer.append(afterPart);
				buffer.append(" `` ");
			}
			else {
				buffer.append(part);
				buffer.append(" ");
			}
		}
		treeStr = buffer.toString();
		return treeStr;
	}
}
