package edu.virginia.cs.gumtreetest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GeneratedPatchEntry {
	public int patchNumber;
	public String parentCode;
	public String childCode;
	public String parentTreeStr;
	public String childTreeStr;
	public String parentFilePath;
	public String childFilePath;
	public List<String> generatedCodes;
	public String treeEditDistStr;
	public String verdict;
	public String javaChildFilePath;
	public String project;
	public String bugId;
	public String filePathFormatted;
	
	public GeneratedPatchEntry() {
		generatedCodes = new ArrayList<>();
	}
	
	private void generateChildFilePath() {
		String checkedOutDirectory = "/localtmp/Defects4jCheckedOut/";
		String prefix = "/zf8/sc2nf/CCRecom_exp/Defects4j/";
		String fPath = parentFilePath.substring(prefix.length());
		String []parts = fPath.split("/");
		project = parts[0];
		bugId = parts[1];
		String filePath = parts[3];
		filePathFormatted = filePath.replaceAll("_", "/");
		childFilePath = prefix + project + "/" + bugId + "/child/" + filePath;
		//Util.logln(childFilePath);
		javaChildFilePath = checkedOutDirectory + project + "/" + bugId + "-fixed/" + filePathFormatted;
	}
	
	public static List<GeneratedPatchEntry> parsePatchResults(String patchFilePath, int beamSize) throws IOException{
		List<GeneratedPatchEntry> patches = new ArrayList<GeneratedPatchEntry>();
		Scanner patchScanner = new Scanner(new File(patchFilePath));
		skipLine(patchScanner, 4);
		while(patchScanner.hasNextLine()) {
			GeneratedPatchEntry example = new GeneratedPatchEntry();
			patchScanner.nextLine();
			String numberLine = patchScanner.nextLine().trim();
			example.patchNumber = Integer.parseInt((numberLine.split(":")[1]).trim());
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
			example.verdict = patchScanner.nextLine().trim();
			skipLine(patchScanner, 1);
			for(int i = 0; i < beamSize; i++) {
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
	
	public static void skipLine(Scanner scan, int ln) {
		for(int i =0 ; i < ln; i++) scan.nextLine();
	}
	
	public static void main(String[] args) throws IOException {
		String parseFilePath = args[0];
		List<GeneratedPatchEntry> patches = parsePatchResults(parseFilePath, 200);
		int i = 0;
		for(GeneratedPatchEntry en : patches) {
			String childFilePath  = en.childFilePath;
			String chidlTreeStr = en.childTreeStr;
			chidlTreeStr = TreeUtil.reFormatTreeStr(chidlTreeStr);
			List<String> patchCodes = en.generatedCodes;
			List<String> modifiedCodes = TreeUtil.getPatchedFileFromChildFile(childFilePath, chidlTreeStr, patchCodes);
			//for(String code : modifiedCodes) {
				Util.logln(en.verdict + " " + modifiedCodes.size() + " " + i);
				//Util.logln(modifiedCodes.get(0));
			//}
			i++;
		}
	}

}
