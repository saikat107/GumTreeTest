package edu.virginia.cs.gumtreetest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;

import org.hamcrest.core.IsInstanceOf;

import com.github.gumtreediff.tree.ITree;


public class Util {
	/**
	 * This method is for logging and debugging.
	 * @param message
	 */
	public static void logln(Object message){
		StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
		System.out.print(caller + "\t");
		Util.println(message);
	}
	
	public static void log(Object message){
		StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
		System.out.print(caller + "\t");
		Util.print(message  + " ");
	}
	
	public static void println(Object message){
		Util.print(message);
		System.out.println();
	}
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
	
	
	public static String joinPath(String basePath, String childName){
		File baseFile = new File(basePath);
		File childFile = new File(baseFile, childName);
		return childFile.getPath();
	}
	
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
	
	
	public static boolean hasBlockComment(String leftOver){
		return leftOver.contains("/*");
	}
	
	public static boolean hasLineComment(String leftOver){
		return leftOver.contains("//");
	}
	
	public static String removeComment(String leftOver) {
		while(hasBlockComment(leftOver))
			leftOver = removeBlockComment(leftOver);
		while(hasLineComment(leftOver))
			leftOver = removeLineComment(leftOver);
		return leftOver;
	}

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

	private static void writeDataRecusrsive(ITree root, PrintStream writer){
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
	
	public static void printSourceTree(ITree root, PrintStream sourceDataFile, PrintStream sourceTreeFile) {
		writeDataRecusrsive(root, sourceDataFile);	
		sourceDataFile.println();
		writeTreeRecursive(root, sourceTreeFile);
		sourceTreeFile.println();
	}

	public static void printDestTree(ITree root, PrintStream destFile) {
		destFile.print(root.getType() + " -> ");
		List<ITree> children = root.getChildren();
		if(children.size() == 0){
			destFile.println(root.getLabel());
			return;
		}
		for(ITree child : children){
			destFile.print(child.getType() + " ");
		}
		destFile.println();
		for(ITree child : children){
			printDestTree(child, destFile);
		}
	}

}
