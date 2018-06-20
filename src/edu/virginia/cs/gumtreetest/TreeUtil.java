package edu.virginia.cs.gumtreetest;

import java.util.ArrayList;
import java.util.List;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;

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

	
	/**
	 * @author saikat
	 * @param root
	 * @param src
	 */
	public static void fixAST(ITree root, String src) {
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
								leftOver = leftOver.replaceAll(" ", "").replaceAll("\t", "").replaceAll("\n", "").replaceAll("\r", "").replaceAll("[ ]+", " ");
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
				fixAST(child, src);
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
}
