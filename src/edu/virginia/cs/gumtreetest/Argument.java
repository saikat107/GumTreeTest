package edu.virginia.cs.gumtreetest;

//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;

public class Argument {
	private String allPathsFile = null;
	private String sFilePath = null;
	private String dFilePath = null;
	private String oPath = null;
	private int mChangeSize = -1;
	private int mTreeSize = -1;
	private int defaultMc = 20;
	private int defaultMt = 50;
	private boolean r = false;
	private boolean onlyMethod = false;
	private boolean exclStringChg = false;
	private boolean astOnly = false;
	
	public String allPathsFile(){
		return this.allPathsFile;
	}
	
	public String srcFilePath(){
		return this.sFilePath;
	}
	
	public String destFilePath(){
		return this.dFilePath;
	}
	
	public String outputFilePath(){
		return this.oPath;
	}
	
	public int maxChangeSize(){
		return this.mChangeSize;
	}
	
	public int maxTreeSize(){
		return this.mTreeSize;
	}
	
	public boolean replace(){
		return this.r;
	}
	
	public boolean onlyMethodChange(){
		return this.onlyMethod;
	}
	
	public boolean excludeStringChange(){
		return this.exclStringChg;
	}
	
	public boolean astOnly(){
		return this.astOnly;
	}
	
	public String toString(){
		return "SrcFile\t: " + this.allPathsFile + "\n"
				+ "Out Folder\t: " + this.oPath + "\n"
				+ "Replace Var\t: " + this.r + "\n"
				+ "Max Change\t: " + this.maxChangeSize() + "\n"
				+ "Max Tree\t: " + this.maxTreeSize() + "\n" 
				+ "Method Change Only\t:" + this.onlyMethodChange() + "\n"
				+ "AST Only Change\t:" + this.astOnly();
				
	}
	
	//private static List<String> commandList = Arrays.asList("--src", "-s", "--dest", "-d", "--out", "-o", "--replace", "-r", "--maxchange", "-mc", "--maxtree", "-mt", 
	//"--method_only", "-mo", "--exlude_string_change", "-esc");
	
	public static Argument preprocessArgument(String []args){
		Argument arg = new Argument();
		for(int i = 0 ; i < args.length; i++){
			String command = args[i];
			switch (command) {
			case "--exlude_string_change":
			case "-esc":
				arg.exclStringChg = true;
				break;
			case "--all":
			case "-a":
				arg.allPathsFile = args[i+1];
				i++;
				break;
			case "--out":
			case "-o":
				arg.oPath = args[i+1];
				i++;
				break;
			case "--replace":
			case "-r":
				arg.r = true;
				break;
			case "--maxchange":
			case "-mc":
				arg.mChangeSize = Integer.parseInt(args[i+1]);
				if(arg.mTreeSize != -1){
					if(arg.mTreeSize < arg.mChangeSize){
						arg.mTreeSize = arg.mChangeSize;
					}
				}
				i++;
				break;
			case "--ast_only":
			case "-ast":
				arg.astOnly = true;
				break;
			case "--maxtree":
			case "-mt":
				arg.mTreeSize = Integer.parseInt(args[i+1]);
				if(arg.mChangeSize != -1){
					if(arg.mChangeSize > arg.mTreeSize){
						arg.mChangeSize = arg.mTreeSize;
					}
				}
				i++;
				break;
			case "--method_only":
			case "-mo":
				arg.onlyMethod = true;
				break;
			default:
				System.out.println("Invalid Command Line Argument : " + command);
				System.exit(-1);			

			}
		}
		if(arg.mChangeSize == -1){
			arg.mChangeSize = Math.min(arg.defaultMc, arg.mTreeSize);
		}
		if(arg.mTreeSize == -1){
			arg.mTreeSize = Math.max(arg.defaultMt, arg.mChangeSize);
		}
		return arg;
	}
	
}
