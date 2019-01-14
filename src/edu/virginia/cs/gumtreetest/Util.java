package edu.virginia.cs.gumtreetest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;

@SuppressWarnings("unchecked")
public class Util {
	
	public static String getCommandExecutionResult(String command) {
		String result = "";
		try {
			Process p = Runtime.getRuntime().exec(command);
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while(true) {
				String line = reader.readLine();
				if(line == null) break;
				result += (line.trim() + "\n");
			}
			reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while(true) {
				String line = reader.readLine();
				if(line == null) break;
				result += (line.trim() + "\n");
			}
		}catch(IOException ex) {
			result = "EXCEPTION RUNNING THE COMMAND";
		}
		return result;
	}
	
	public static String executeProgram(String project, String bugId, String projectPath) {
		//Util.logln("Executing  :" + projectPath);
		//TODO TO IMPLEMENT THE TESTCASE RUNNING INFRASTRUCTURE
		boolean successfullyCompiled = TCRunUtil.checkSuccessfulCompilation(projectPath);
		if(!successfullyCompiled) {
			return "FAIL:C";
		}
		String testPass = "";
		try {
			testPass = TCRunUtil.checkTriggerTestCasePassing(project, bugId, projectPath);
		} catch (IOException e) {
			return "FAIL:E"; 
		}
		/*if(testPass) {
			return "PASS";
		}
		else {
			return "FAIL:"+passed;
		}*/
		return testPass;
	}
	
	public static boolean writeFile(String filePath, String text) {
		File file = new File(filePath);
		//boolean exists = file.exists();
		try {
			PrintWriter writer = new PrintWriter(file);
			writer.println(text);
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * @author saikat
	 * This method is for logging and debugging.
	 * @param message
	 */
	@SuppressWarnings("unused")
	public static void logln(Object message){
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		Date date = new Date();
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
	@SuppressWarnings("rawtypes")
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
				System.out.print("\n");
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
		StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
		System.out.print(caller + "\n");
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
			System.out.println(curr.getLabel() + " " + curr.getMetadata("subs_name"));
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
	 * @param root
	 */
	public static void nodePrint(ITree root){
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
			List<ITree> children = curr.getChildren();
			int cz = children.size();
			if(cz == 0){
				Util.logln(curr.getLabel() + " " + curr.getType());
			}
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
	public static String getCodeRecusrsive(ITree root, boolean replace){
		if(root.getChildren().size() == 0){
			Object name = null;
			if(replace){
				name = root.getMetadata("subs_name");
			}
			if(name == null){
				name = root.getLabel() ;
			}
//			Util.logln(name);
			return  name.toString().trim() + " ";
			//return root.getLabel().trim() + " ";
		}
		else{
			String code = "";
			for(ITree child: root.getChildren()){
				code += getCodeRecusrsive(child, replace);
			}
			return code;
		}
	}
	
	
	public static String getTypedCodeRecusrsive(ITree root){
		if(root.getChildren().size() == 0){
			Object name = null;
			if(name == null){
				String type = (String)root.getMetadata(Config.METADATA_TAG.DATA_TYPE);
				name = type ;
			}
			return  name.toString().trim() + " ";
		}
		else{
			String code = "";
			for(ITree child: root.getChildren()){
				code += getTypedCodeRecusrsive(child);
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
	public static String getDestTree(ITree root, boolean replace){
		return "AST_ROOT_SC2NF " + getDestTreeRecursive(root, replace);
	}
	
	/**
	 * @author saikat
	 * @param root
	 * @return
	 */
	public static String getDestTypeTree(ITree root){
		return "AST_ROOT_SC2NF " + getDestTypeTreeRecursive(root);
	}

	/**
	 * @author saikat
	 * @param root
	 * @return
	 */
	private static String getDestTreeRecursive(ITree root, boolean replace) {
		String returnStr = "";
		List<ITree> children = root.getChildren();
		returnStr += ("` " + root.getType() + " ");
		if(children.size() == 0){
			Object name = null;
			if(replace) {
				name = root.getMetadata("subs_name");
			}
			if(name == null){
				name = root.getLabel();
			}
			returnStr += ("` " + name + " `` `` ");
		}
		else{
			for(ITree child : children){
				returnStr += getDestTreeRecursive(child, replace);
			}
			returnStr += " `` ";
		}
		return returnStr;
	}
	
	/**
	 * @author saikat
	 * @param root
	 * @return
	 */
	private static String getDestTypeTreeRecursive(ITree root) {
		String returnStr = "";
		List<ITree> children = root.getChildren();
		returnStr += ("` " + root.getType() + " ");
		if(children.size() == 0){
			Object name = null;
			if(name == null){
				name = root.getMetadata(Config.METADATA_TAG.DATA_TYPE);
			}
			returnStr += ("` " + name + " `` `` ");
		}
		else{
			for(ITree child : children){
				returnStr += getDestTypeTreeRecursive(child);
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
			return 234;
		}
		else if(leftOver.compareTo(".") == 0){
			return 235;
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
		else if(leftOver.compareTo("&&") == 0){
			return 249;
		}
		else if(leftOver.compareTo("||") == 0){
			return 250;
		}
		else if(leftOver.compareTo(">=") == 0){
			return 251;
		}
		else if(leftOver.compareTo("<=") == 0){
			return 252;
		}
		else if(Config.JavaKeywords.isKeyWord(leftOver)){
			return Config.JavaKeywords.getKeyWordType(leftOver);
		}
		else{
			//Util.log(leftOver);
			return Config.ASTTYPE_TAG.REST_OF_LEFTOVER;
		}
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
		
	
	public static boolean deleteDirectory(File directoryToBeDeleted) {
	    File[] allContents = directoryToBeDeleted.listFiles();
	    if (allContents != null) {
	        for (File file : allContents) {
	            deleteDirectory(file);
	        }
	    }
	    return directoryToBeDeleted.delete();
	}


	public static void dfsPrintMetaData(ITree root, String metadata) {
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
			System.out.print(curr.getType() + " ");
			Object data = curr.getMetadata(metadata);
			if(data!= null){
				System.out.println(data);
			}
			else{
				System.out.println();
			}
			List<ITree> children = curr.getChildren();
			int cz = children.size();
			for(int idx = cz-1;  idx >=0; idx --){
				ITree child = children.get(idx);
				nodes.push(child);
				level.push(l+1);
			}
		}
	}


	public static List<ITree> getDecomposedLeftOver(ITree root, String leftOver, int position) {
		List<ITree> nodes = new ArrayList<ITree>();
		List<String> keywords = Config.JavaKeywords.keywordList;
		for(String keyword : keywords){
			if(leftOver.contains(keyword)){
				int bi = 0;
				int ki = leftOver.indexOf(keyword);
				String firstStr = leftOver.substring(bi, ki);
				String secondStr = leftOver.substring(ki, ki + keyword.length());
				String thirdStr = leftOver.substring(ki + keyword.length());
				List<ITree> firstNodes = Util.getDecomposedLeftOver(root, firstStr, position);
				ITree kNode = new Tree(Util.getNodeTypeFromLeftOver(root, secondStr), secondStr);
				kNode.setPos(position + ki);
				List<ITree> thirdNodes = Util.getDecomposedLeftOver(root, thirdStr,position + ki + keyword.length());
				nodes.addAll(firstNodes);
				nodes.add(kNode);
				nodes.addAll(thirdNodes);
				for(ITree node:nodes){
					node.setParent(root);
				}
				return nodes;
			}
		}
		List<String> punctuations = Config.JavaKeywords.punctuationsList;
		for(String punctuation : punctuations){
			if(leftOver.contains(punctuation)){
				int bi = 0;
				int ki = leftOver.indexOf(punctuation);
				String firstStr = leftOver.substring(bi, ki);
				String secondStr = leftOver.substring(ki, ki + punctuation.length());
				String thirdStr = leftOver.substring(ki + punctuation.length());
				List<ITree> firstNodes = Util.getDecomposedLeftOver(root, firstStr, position);
				ITree kNode = new Tree(Util.getNodeTypeFromLeftOver(root, secondStr), secondStr);
				kNode.setPos(position + ki);
				List<ITree> thirdNodes = Util.getDecomposedLeftOver(root, thirdStr,position + ki + punctuation.length());
				nodes.addAll(firstNodes);
				nodes.add(kNode);
				nodes.addAll(thirdNodes);
				for(ITree node:nodes){
					node.setParent(root);
				}
				return nodes;
			}
		}
		if(leftOver.trim().length() > 0){
			ITree node = new Tree(Util.getNodeTypeFromLeftOver(root, leftOver.trim()), leftOver.trim());
			node.setParent(root);
			nodes.add(node);
		}
		return nodes;
	}
	
//	public static void main(String[] args) {
//		String test = ".this();";
//		List<ITree> nodes = Util.getDecomposedLeftOver(null, test,0);
//		for(ITree node : nodes){
//			Util.logln(node.toTreeString());
//		}
//	}


	public static boolean checkIfChangeBelongToAnyMethod(ITree cParentSrc, ITree cParentDest) {
		List<ITree> sourceAncestors = cParentSrc.getParents();
		List<ITree> destAncestors = cParentDest.getParents();
		boolean ret = false;
		for (ITree parent : sourceAncestors){
			if(parent.getType() == Config.ASTTYPE_TAG.METHOD_DECLARATION){
				ret = true;
				break;
			}
		}
		boolean ret2 = false;
		for (ITree parent : destAncestors){
			if(parent.getType() == Config.ASTTYPE_TAG.METHOD_DECLARATION){
				ret2 = true;
				break;
			}
		}
		return ret && ret2;
	}


	/**
	 * This method will extract all the variables and method name from the method the change is from 
	 * @param root
	 * @return A list of variable
	 */
	public static Set<String> extractAllVariablesInScope(ITree root) {
		Set<String> variables = new HashSet<String>();
		List<ITree> parents = root.getParents();
		ITree methodNode = null;
		for(ITree parent : parents){
			if(parent.getType() == Config.ASTTYPE_TAG.METHOD_DECLARATION){
				methodNode = parent;
			}
		}
		if(methodNode != null){
			Stack<ITree> st = new Stack<ITree>();
			st.push(methodNode);
			while(!st.isEmpty()){
				ITree curr = st.pop();
				if(curr.getType() == Config.ASTTYPE_TAG.SIMPLE_NAME || curr.getType() == Config.ASTTYPE_TAG.COMPLEX_NAME){
					String var = curr.getLabel();
					variables.add(var);
				}
				for(ITree child : curr.getChildren()){
					st.push(child);
				}
			}
		}
		return variables;
	}
	
	
	private static final String[] PUNCTUATIONS = { "~", "`", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "-", "+", "=", 
			    "|", "\"", "'", ";", ":", "<", ">", ",", ".", "?", "/" };
	private static final String[] REGEXES = { "~", "`", "!", "@", "#", "\\$", "%", "\\^", "&", "\\*", "\\(", "\\)", "\\-", "\\+", "\\=", 
			    "\\|", "\"", "'", ";", ":", "\\<", "\\>", ",", "\\.", "\\?", "/" };
			  
	public static String getFormattedCode(ITree root){
		String code = getRunnableCodeRecusrsive(root);
		for (int i = 0; i < PUNCTUATIONS.length; i++) {
			String punc = PUNCTUATIONS[i];
			String rges = REGEXES[i];
			String fStr = " " + punc;
			String rFStre = " " + rges;
			if (code.contains(fStr)) {
				code = code.replaceAll(rFStre, punc);
			}
			String sStr = punc + " ";
			String rSStre = rges + " ";
			if (code.contains(sStr)) {
				code = code.replaceAll(rSStre, punc);
			}
		}
		if (code.contains("{")) {
			code = code.replaceAll("\\{", "{\n\t");
		}
		if (code.contains("}")) {
			code = code.replaceAll("}", "}\n");
		}
		if (code.contains(";")) {
			code = code.replaceAll(";", ";\n");
		}
		if (code.contains("[ ")) {
			code = code.replaceAll("\\[ ", "[");
		}
		if (code.contains(" [")) {
			code = code.replaceAll(" \\[", "[");
		}
		if (code.contains(" [ ")) {
			code = code.replaceAll(" \\[ ", "[");
		}
		if (code.contains(" [")) {
			code = code.replaceAll(" \\]", "]");
		}
		return code;
	}
			  
	private static String getRunnableCodeRecusrsive(ITree root) {
		if (root.getChildren().size() == 0) {
			Object name = null;
			if (name == null) {
				name = root.getLabel();
			}
			String token = name.toString().trim();
			if (token.equalsIgnoreCase("STRING_CONSTANT")) {
				token = "\"\"";
			} else if (token.equalsIgnoreCase("NUMBER_CONSTANT")) {
				token = "0";
			} else if (token.equalsIgnoreCase("NUMBER_CONSTANT")) {
				token = "'\n'";
			}
			return token + " ";
		}
		String code = "";
		for (ITree child : root.getChildren()) {
			code = code + getRunnableCodeRecusrsive(child);
		}
		return code;
	}

}
