package edu.virginia.cs.gumtreetest;

import java.io.File;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collection;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


import com.github.gumtreediff.tree.ITree;


public class Util {
	/**
	 * @author saikat
	 * This method is for logging and debugging.
	 * @param message
	 */
	public static void logln(Object message){
		StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
		System.out.print(caller + "\t");
		Util.println(message);
	}
	
	
	/**
	 * @author saikat
	 * @param message
	 */
	public static void log(Object message){
		StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
		System.out.print(caller + "\t");
		Util.print(message  + " ");
	}
	
	/**
	 * @author saikat
	 * @param message
	 */
	public static void println(Object message){
		Util.print(message);
		System.out.println();
	}
	
	/**
	 * @author saikat
	 * @param message
	 */
	public static void print(Object message){
		if (message instanceof Collection) {
			Collection msg = (Collection) message;
			System.out.print("[");
			for(Object obj : msg){
				Util.print(obj);
				System.out.print( " ");
			}
			System.out.println("]");
		}
		else if (message instanceof Map) {
			Map map = (Map) message;
			System.out.println("{");
			Set<Object>keys = map.keySet();
			for(Object key : keys){
				Util.print(key);
				System.out.print(" : ");
				Util.print(map.get(key));
			}
			System.out.println("}");
		}
		else{
			System.out.print(message);
		}
	}
	
	
	/**
	 * @author saikat
	 * @param basePath
	 * @param childName
	 * @return
	 */
	public static String joinPath(String basePath, String childName){
		File baseFile = new File(basePath);
		File childFile = new File(baseFile, childName);
		return childFile.getPath();
	}
	
	
	/**
	 * @author saikat
	 * @param directory
	 * @param absolutePath
	 * @return
	 */
	public static List<String> getAllFiles(String directory, boolean absolutePath){
		List<String>allFiles = new ArrayList<String>();
		File baseDirectory = new File(directory);
		String []allFileNames = baseDirectory.list();
		for(String fileName : allFileNames){
			if(absolutePath){
				allFiles.add(Util.joinPath(directory, fileName));
			}
			else{
				allFiles.add(fileName);
			}
		}
		return allFiles;
	} 
	
	/*public static List<PatchEntry> getPatchEntries(String lineNoCsvFilePath){
		List<PatchEntry> patchEntries = new ArrayList<PatchEntry>();
		try {
			Scanner lnScanner = new Scanner(new File(lineNoCsvFilePath));
			while(lnScanner.hasNextLine()){
				String line = lnScanner.nextLine();
				String[] parts = line.split(",");
				PatchEntry entry = new PatchEntry(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
				patchEntries.add(entry);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return patchEntries;
	}*/
	
	/**
	 * @author saikat
	 * @param leftOver
	 * @return
	 */
	public static boolean hasBlockComment(String leftOver){
		return leftOver.contains("/*");
	}
	
	/**
	 * @author saikat
	 * @param leftOver
	 * @return
	 */
	public static boolean hasLineComment(String leftOver){
		return leftOver.contains("//");
	}
	
	
	/**
	 * @author saikat
	 * @param leftOver
	 * @return
	 */
	public static String removeComment(String leftOver) {
		while(hasBlockComment(leftOver))
			leftOver = removeBlockComment(leftOver);
		while(hasLineComment(leftOver))
			leftOver = removeLineComment(leftOver);
		return leftOver;
	}

	/**
	 * @author saikat
	 * @param leftOver
	 * @return
	 */
	public static String removeBlockComment(String leftOver) {
		int startCommentIdx = leftOver.indexOf("/*");
		if(startCommentIdx == -1){
			return leftOver;
		}
		int endIndex = leftOver.indexOf("*/", startCommentIdx);
		if(endIndex != -1){
			String beforeComment = leftOver.substring(0, startCommentIdx);
			String afterComment = leftOver.substring(endIndex + 2);
			leftOver = beforeComment + " " + afterComment;
		}
		else{
			leftOver = leftOver.substring(0, startCommentIdx);
		}
		return leftOver.trim();
	}
	
	
	/**
	 * @author saikat
	 * @param leftOver
	 * @return
	 */
	public static String removeLineComment(String leftOver) {
		int startCommentIdx = leftOver.indexOf("//");
		if(startCommentIdx == -1){
			return leftOver;
		}
		int endIndex = leftOver.indexOf("\n", startCommentIdx);
		if(endIndex != -1){
			String beforeComment = leftOver.substring(0, startCommentIdx);
			String afterComment = leftOver.substring(endIndex + 1);
			leftOver = beforeComment + " " + afterComment;
		}
		else{
			leftOver = leftOver.substring(0, startCommentIdx);
		}
		return leftOver.trim();
	}
	

	/**
	 * @author saikat
	 * @param root
	 */
	public static void dfsPrint(ITree root){
		if(root == null){
			return;
		}
		Stack<ITree> nodes = new Stack<>();
		Stack<Integer> level = new Stack<>();
		nodes.push(root);
		level.push(0);
		while(!nodes.isEmpty()){
			ITree curr = nodes.pop();
			int l = level.pop();
			for(int i = 0; i < l; i++){
				System.out.print('\t');
			}
			System.out.println(curr.getType() + " " + curr.getId() + " " + curr.getLabel());
			List<ITree> children = curr.getChildren();
			int cz = children.size();
			for(int idx = cz-1;  idx >=0; idx --){
				ITree child = children.get(idx);
				nodes.push(child);
				level.push(l+1);
			}
		}
	}

	/**
	 * @author saikat
	 * @param srcPath
	 * @return
	 * @throws IOException
	 */
	public static String readFile(String srcPath) throws IOException {
		String text = "";
		FileReader inputStrem = new FileReader(new File(srcPath));
		int c;
		while((c = inputStrem.read()) != -1){
			text += ((char)c);
		}
		inputStrem.close();
		return text;
	}

	/**
	 * @author saikat
	 * @param root
	 * @param writer
	 */
	public static void writeDataRecusrsive(ITree root, PrintStream writer){
		if(root.getChildren().size() == 0){
			if(writer != null){
				writer.print(root.getLabel().trim() + " ");
				writer.flush();
			}
			else{
				System.out.print(root.getLabel() + " ");
			}
		}
		else{
			for(ITree child: root.getChildren()){
				writeDataRecusrsive(child, writer);
			}
		}
	}
	
	
	/**
	 * @author saikat
	 * @param root
	 * @return
	 */
	public static String getCodeRecusrsive(ITree root){
		if(root.getChildren().size() == 0){
			return root.getLabel().trim() + " ";
		}
		else{
			String code = "";
			for(ITree child: root.getChildren()){
				code += getCodeRecusrsive(child);
			}
			return code;
		}
	}
	
	/**
	 * @author saikat
	 * @param root
	 * @param writer
	 */
	private static void writeTreeRecursive(ITree root, PrintStream writer){
		if(root == null || root.getChildren().size() == 0){
			return;
		}
		if(root.getChildren().size() == 2){
			ITree leftChild = root.getChildren().get(0);
			ITree rightChild = root.getChildren().get(1);
			writeTreeRecursive(leftChild, writer);
			writeTreeRecursive(rightChild, writer);
			if(writer != null){
				writer.print("[" + leftChild.getId() + "," + rightChild.getId() + "," + root.getId() + "] ");
				writer.flush();
			}
			else{
				System.out.print("[" + leftChild.getId() + "," + rightChild.getId() + "," + root.getId() + "] ");
			}
		}
	}
	
	/**
	 * @author saikat
	 * @param root
	 * @return
	 */
	public static String getSourceTree(ITree root){
		if(root == null || root.getChildren().size() == 0){
			return "";
		}
		if(root.getChildren().size() == 2){
			String treeStr = "";
			ITree leftChild = root.getChildren().get(0);
			ITree rightChild = root.getChildren().get(1);
			treeStr += getSourceTree(leftChild);
			treeStr += getSourceTree(rightChild);
			treeStr += ("[" + leftChild.getId() + "," + rightChild.getId() + "," + root.getId() + "] ");
			return treeStr;
		}
		return null;
	}
	
	/**
	 * @author saikat
	 * @param root
	 * @param sourceDataFile
	 * @param sourceTreeFile
	 */
	public static void printSourceTree(ITree root, PrintStream sourceDataFile, PrintStream sourceTreeFile) {
		writeDataRecusrsive(root, sourceDataFile);	
		sourceDataFile.println();
		writeTreeRecursive(root, sourceTreeFile);
		sourceTreeFile.println();
	}

	/**
	 * @author saikat
	 * @param root
	 * @param destFile
	 */
	public static void printDestTree(ITree root, PrintStream destFile) {
		destFile.print("AST_ROOT_SC2NF ");
		Util.printRecuriveTree(root, destFile);
	}
	
	/**
	 * @author saikat
	 * @param root
	 * @return
	 */
	public static String getDestTree(ITree root){
		return "AST_ROOT_SC2NF " + getDestTreeRecursive(root);
	}

	/**
	 * @author saikat
	 * @param root
	 * @return
	 */
	private static String getDestTreeRecursive(ITree root) {
		String returnStr = "";
		List<ITree> children = root.getChildren();
		returnStr += ("` " + root.getType() + " ");
		if(children.size() == 0){
			returnStr += ("` " + root.getLabel() + " `` `` ");
		}
		else{
			for(ITree child : children){
				returnStr += getDestTreeRecursive(child);
			}
			returnStr += " `` ";
		}
		return returnStr;
	}

	/**
	 * @author saikat
	 * @param root
	 * @param file
	 */
	public static void writeRecursiveGrammar(ITree root, PrintStream file){
		List<ITree> children = root.getChildren();
		if(children.size() == 0){
			file.println(root.getType() + " -> " + root.getLabel());
		}
		else{
			file.print(root.getType() + " -> ");
			for(ITree child : children){
				file.print(child.getType() + " ");
			}
			file.println();
			for(ITree child : children){
				writeRecursiveGrammar(child, file);
			}
		}
	}
	
	/**
	 * @author saikat
	 * @param node
	 * @return
	 */
	public static boolean isLeafNode(ITree node){
		return node.getChildren().size()==0;
	}
	
	/**
	 * @author saikat
	 * @param root
	 * @param destFile
	 */
	private static void printRecuriveTree(ITree root, PrintStream destFile) {
		List<ITree> children = root.getChildren();
		destFile.print("` " + root.getType() + " ");
		if(children.size() == 0){
			destFile.print("` " + root.getLabel() + " `` ");
		}
		else{
			for(ITree child : children){
				printRecuriveTree(child, destFile);
			}
		}
		destFile.print("`` ");
		
	}

	/**
	 * @author saikat
	 * @param cParentDest
	 * @param destCodeWriter
	 * @param destTreeWriter
	 */
	public static void printDestCodeAndTree(ITree cParentDest, PrintStream destCodeWriter, PrintStream destTreeWriter) {
		Util.writeDataRecusrsive(cParentDest, destCodeWriter);
		destCodeWriter.println();
		Util.printDestTree(cParentDest, destTreeWriter);
		destTreeWriter.println();
	}

	/**
	 * @author saikat
	 * @param root
	 * @param leftOver
	 * @return
	 */
	public static int getNodeTypeFromLeftOver(ITree root, String leftOver) {
		leftOver = leftOver.trim();
		if(leftOver.compareTo("+") == 0){
			return 200;
		}
		else if(leftOver.compareTo("-") == 0){
			return 201;
		}
		else if(leftOver.compareTo("*") == 0){
			return 202;
		}
		else if(leftOver.compareTo("/") == 0){
			return 203;
		}
		else if(leftOver.compareTo("=") == 0){
			return 204;
		}
		else if(leftOver.compareTo("~") == 0){
			return 205;
		}
		else if(leftOver.compareTo("`") == 0){
			return 206;
		}
		else if(leftOver.compareTo("!") == 0){
			return 207;
		}
		else if(leftOver.compareTo("@") == 0){
			return 208;
		}
		else if(leftOver.compareTo("#") == 0){
			return 209;
		}
		else if(leftOver.compareTo("$") == 0){
			return 210;
		}
		else if(leftOver.compareTo("%") == 0){
			return 211;
		}
		else if(leftOver.compareTo("^") == 0){
			return 212;
		}
		else if(leftOver.compareTo("&") == 0){
			return 213;
		}
		else if(leftOver.compareTo("(") == 0){
			return 214;
		}
		else if(leftOver.compareTo(")") == 0){
			return 215;
		}
		else if(leftOver.compareTo("()") == 0){
			return 216;
		}
		else if(leftOver.compareTo("_") == 0){
			return 217;
		}
		else if(leftOver.compareTo("{") == 0){
			return 218;
		}
		else if(leftOver.compareTo("}") == 0){
			return 219;
		}
		else if(leftOver.compareTo("{}") == 0){
			return 220;
		}
		else if(leftOver.compareTo("[") == 0){
			return 221;
		}
		else if(leftOver.compareTo("]") == 0){
			return 222;
		}
		else if(leftOver.compareTo("[]") == 0){
			return 223;
		}
		else if(leftOver.compareTo("|") == 0){
			return 224;
		}
		else if(leftOver.compareTo("\\") == 0){
			return 225;
		}
		else if(leftOver.compareTo(":") == 0){
			return 226;
		}
		else if(leftOver.compareTo(";") == 0){
			return 227;
		}
		else if(leftOver.compareTo("\'") == 0){
			return 228;
		}
		else if(leftOver.compareTo("\"") == 0){
			return 229;
		}
		else if(leftOver.compareTo("<") == 0){
			return 230;
		}
		else if(leftOver.compareTo(">") == 0){
			return 231;
		}
		else if(leftOver.compareTo("<>") == 0){
			return 232;
		}
		else if(leftOver.compareTo("?") == 0){
			return 233;
		}
		else if(leftOver.compareTo(",") == 0){
			return 235;
		}
		else if(leftOver.compareTo(".") == 0){
			return 236;
		}
		else if(leftOver.compareTo("==") == 0){
			return 236;
		}
		else if(leftOver.compareTo("+=") == 0){
			return 237;
		}
		else if(leftOver.compareTo("-=") == 0){
			return 238;
		}
		else if(leftOver.compareTo("*=") == 0){
			return 239;
		}
		else if(leftOver.compareTo("/=") == 0){
			return 240;
		}
		else if(leftOver.compareTo("%=") == 0){
			return 241;
		}
		else if(leftOver.compareTo("!=") == 0){
			return 242;
		}
		else if(leftOver.compareTo("&=") == 0){
			return 243;
		}
		else if(leftOver.compareTo("|=") == 0){
			return 244;
		}
		else if(leftOver.compareTo("^=") == 0){
			return 245;
		}
		else if(leftOver.compareTo("~=") == 0){
			return 246;
		}
		else if(leftOver.compareTo("++") == 0){
			return 247;
		}
		else if(leftOver.compareTo("--") == 0){
			return 248;
		}
		else if(leftOver.compareTo("public") == 0){
			return 249;
		}
		else if(leftOver.compareTo("private") == 0){
			return 250;
		}
		else if(leftOver.compareTo("protected") == 0){
			return 251;
		}
		else if(leftOver.compareTo("final") == 0){
			return 252;
		}
		return root.getType();
	}

	/**
	 * @author saikat
	 * @param commonParent
	 * @param parents
	 * @return
	 */
	public static List<ITree> getCommonParents(List<ITree> commonParent, List<ITree> parents) {
		for(ITree parent : parents){
			if(commonParent.contains(parent)){
				commonParent = parent.getParents();
				commonParent.add(0, parent);
				break;
			}
		}
		return commonParent;
	}

}
