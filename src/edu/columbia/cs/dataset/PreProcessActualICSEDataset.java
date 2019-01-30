package edu.columbia.cs.dataset;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PreProcessActualICSEDataset {
	public static void main(String[] args) throws Exception {
		String base = args[0];
		String beforeBase = base + "before_process";
		String afterBase = base + "after_process";
		String[] projects = {"android", "google", "ovirt"};
		String[] kinds = {"small", "medium"};
		String[] nums = {"50", "50-100"}; 
		String[] types = {"train", "test", "eval"};
		for(int i = 0; i < 2; i++) {
			String kind = kinds[i];
			String num = nums[i];
			for(String type: types) {
				String allAfterProcessignBeforePath = afterBase + "/" + "all" + "/" + kind + "/" + type + "/before";
				String allAfterProcessingAfterPath = afterBase + "/" + "all" + "/" + kind + "/" + type + "/after";
				File outFolder = new File(allAfterProcessignBeforePath);
				if(!outFolder.exists()) {
					outFolder.mkdirs();
				}
				outFolder = new File(allAfterProcessingAfterPath);
				if(!outFolder.exists()) {
					outFolder.mkdirs();
				}
				String allfilaPathsFileName = afterBase +
						"/all/" + kind + "_" + type + ".txt";
				PrintWriter allFW = new PrintWriter(allfilaPathsFileName);
				for(String project : projects) {
					String beforeProcessingPath = beforeBase + "/" + 
							project + "/" + kind + "/" + project + "-" + num + "-" + type + ".txt";
					File inpFile = new File(beforeProcessingPath);
					System.out.println(beforeProcessingPath + "\t" + inpFile.exists());
					List<Example> examples = parseExamples(inpFile);
					
					
					
					String afterProcessignBeforePath = afterBase + "/" + project + "/" + kind + "/" + type + "/before";
					String afterProcessingAfterPath = afterBase + "/" + project + "/" + kind + "/" + type + "/after";
					outFolder = new File(afterProcessignBeforePath);
					if(!outFolder.exists()) {
						outFolder.mkdirs();
					}
					outFolder = new File(afterProcessingAfterPath);
					if(!outFolder.exists()) {
						outFolder.mkdirs();
					}
					
					String filaPathsFileName = afterBase + "/" +
							project + "/" + kind + "_" + type + ".txt";
					PrintWriter fW = new PrintWriter(filaPathsFileName);
					for(int exid = 0; exid < examples.size(); exid++) {
						String beforeFile = afterProcessignBeforePath + "/" + (exid + 1) + ".java";
						String afterFile = afterProcessingAfterPath + "/" + (exid + 1) + ".java";
						PrintWriter prB = new PrintWriter(beforeFile);
						prB.print(examples.get(exid).concreteCodeBefore);
						prB.close();
						PrintWriter prA = new PrintWriter(afterFile);
						prA.print(examples.get(exid).concreteCodeAfter);
						prA.close();
						fW.println(beforeFile + "\t" + afterFile);
						String allBeforeFile = allAfterProcessignBeforePath + "/" + project + "_" + (exid + 1) + ".java";
						String allAfterFile = allAfterProcessingAfterPath + "/" + project + "_" + (exid + 1) + ".java";
						PrintWriter prBAll = new PrintWriter(allBeforeFile);
						prBAll.print(examples.get(exid).concreteCodeBefore);
						prBAll.close();
						PrintWriter prAAll = new PrintWriter(allAfterFile);
						prAAll.print(examples.get(exid).concreteCodeAfter);
						prAAll.close();
						allFW.println(allBeforeFile + "\t" + allAfterFile);
					}
					fW.close();
				}
				allFW.close();
			}
		}
	}

	private static List<Example> parseExamples(File inpFile) {
		List<Example> ret = new ArrayList<Example>();
		try {
			Scanner inScanner = new Scanner(inpFile);
			while(inScanner.hasNextLine()) {
				Example ex = new Example();
				inScanner.nextLine(); // ====
				ex.abstractCodeBefore = inScanner.nextLine().trim();
				ex.abstractCodeAfter = inScanner.nextLine().trim();
				inScanner.nextLine(); // ----
				String line = inScanner.nextLine();
				ex.concreteCodeBefore = "class A{\n";
				while(!line.startsWith("----")) {
					ex.concreteCodeBefore += ("\t" + line + "\n");
					line = inScanner.nextLine();
				}
				ex.concreteCodeBefore += "}\n";
				line = inScanner.nextLine();
				ex.concreteCodeAfter = "class A{\n";
				while(!line.startsWith("----")) {
					ex.concreteCodeAfter += ("\t" + line + "\n");
					line = inScanner.nextLine();
				}
				ex.concreteCodeAfter += "}\n";
				inScanner.nextLine();
				ret.add(ex);
			}
			inScanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return ret;
	}
}

class Example{
	public String abstractCodeBefore = null;
	public String abstractCodeAfter = null;
	public String concreteCodeBefore = null;
	public String concreteCodeAfter = null;
	
	public String toString() {
		return "ABS B\n" + abstractCodeBefore + "\nABS A\n" + 
	abstractCodeAfter + "\nConc B\n" + concreteCodeBefore + "\nConc A\n" + concreteCodeAfter;
	}
	
}
