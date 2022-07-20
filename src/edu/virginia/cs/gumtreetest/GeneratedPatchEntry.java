package edu.virginia.cs.gumtreetest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GeneratedPatchEntry{
	public int patchNumber;
	public String parentCode;
	public String childCode;
	public String parentTreeStr;
	public String childTreeStr;
	public String parentFilePath;
	public String childFilePath;
	public List<String> generatedCodes;
	public String treeEditDistStr;
	public int verdict;
	public String javaChildFilePath;
	public String project;
	public String bugId;
	public String filePathFormatted;
	public String checkedOutProjectDirectory;

	public GeneratedPatchEntry(){
		  this.generatedCodes = new ArrayList<>();
	}
  
	private void generateChildFilePath(){
		String checkedOutDirectory = "/localtmp/Defects4jCheckedOut/";
		String prefix = "/zf8/sc2nf/CCRecom_exp/Defects4j/";
		String fPath = this.parentFilePath.substring(prefix.length());
		String[] parts = fPath.split("/");
		this.project = parts[0];
		this.bugId = parts[1];
		String filePath = parts[3];
		this.filePathFormatted = filePath.replaceAll("_", "/");
		this.childFilePath = (prefix + this.project + "/" + this.bugId + "/child/" + filePath);
		this.checkedOutProjectDirectory = checkedOutDirectory + this.project + "/" + this.bugId + "-fixed/";
		this.javaChildFilePath = checkedOutProjectDirectory + this.filePathFormatted;
	}
  
	public static List<GeneratedPatchEntry> parsePatchResults(
			String patchFilePath, int beamSize) throws IOException{
		List<GeneratedPatchEntry> patches = new ArrayList<>();
		Scanner patchScanner = new Scanner(new File(patchFilePath));
		skipLine(patchScanner, 4);
		while (patchScanner.hasNextLine()){
			GeneratedPatchEntry example = new GeneratedPatchEntry();
			patchScanner.nextLine();
			String numberLine = patchScanner.nextLine().trim();
			example.patchNumber = Integer.parseInt(numberLine.split(":")[1].trim());
			skipLine(patchScanner, 1);
			example.parentCode = patchScanner.nextLine().trim();
			skipLine(patchScanner, 2);
			example.childCode = patchScanner.nextLine().trim();
			skipLine(patchScanner, 2);
			example.parentTreeStr = patchScanner.nextLine().trim();
			skipLine(patchScanner, 2);
			example.childTreeStr = patchScanner.nextLine().trim();
			skipLine(patchScanner, 1);
			example.parentFilePath = patchScanner.nextLine().trim();
			skipLine(patchScanner, 2);
			example.treeEditDistStr = patchScanner.nextLine().trim();
			skipLine(patchScanner, 2);
			String verdict = patchScanner.nextLine().trim();
			if(verdict.equalsIgnoreCase("Correct")) {
				example.verdict = 1;
			}
			else {
				example.verdict = 0;
			}
			skipLine(patchScanner, 1);
			for (int i = 0; i < beamSize; i++){
				skipLine(patchScanner, 2);
				example.generatedCodes.add(patchScanner.nextLine().trim().split("\t")[1]);
				skipLine(patchScanner, 2);
			}
			skipLine(patchScanner, 3);
			example.generateChildFilePath();
			
			patches.add(example);
		}
		patchScanner.close();
		return patches;
	}
  
	public static void skipLine(Scanner scan, int ln){
		for (int i = 0; i < ln; i++) {
			scan.nextLine();
		}
	}
  
	public static void main(String[] args) throws IOException{
		String parseFilePath = args[0];
		String outputPath = args[1];
		PrintWriter out = new PrintWriter(new File(outputPath));
		List<GeneratedPatchEntry> patches = parsePatchResults(parseFilePath, 200);
		for (GeneratedPatchEntry en : patches){
			String childFilePath = en.childFilePath;
			String chidlTreeStr = en.childTreeStr;
			chidlTreeStr = TreeUtil.reFormatTreeStr(chidlTreeStr);
			List<String> patchCodes = en.generatedCodes;
			List<String> modifiedCodes = 
					TreeUtil.getPatchedFileFromChildFile(
							childFilePath, chidlTreeStr, patchCodes);
			String originalFileText = Util.readFile(en.javaChildFilePath);
			String output = en.project + "\t" + en.bugId ;
			Util.logln(en.project + " " + en.bugId);
			for(String newCode: modifiedCodes) {
				boolean successfullyWritten = Util.writeFile(en.javaChildFilePath, newCode);
				String status = Util.executeProgram(
						en.project, en.bugId, en.checkedOutProjectDirectory);
				output += ("\t" + status);
				System.out.print(status + " ");
			}
			output += "\n";
			Util.writeFile(en.javaChildFilePath, originalFileText);
			Util.logln("\n" + output);
			out.println(output);
		}
		out.close();
		/*String command = "PATH=$PATH:$D4J_HOME/framework/bin";
		Process p1 = Runtime.getRuntime().exec(command);*/
		
		//String command = "/home/sc2nf/defects4j/framework/bin/defects4j compile -w /home/sc2nf/Desktop/Math1f";
		//System.out.println(Util.getCommandExecutionResult(command));			
	}
}
