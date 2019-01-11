package edu.columbia.cs.dataset;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Scanner;

import edu.virginia.cs.gumtreetest.Util;

public class DatasetPreparer {
	public static void prepareDataset(String inputDir, String outputDir) {
		
	}
	
	public static void main(String[] args) throws Exception {
		String inputBasePath = args[0]; //"icse_datasets";
		String outputBasePath = args[1];
		String datasetSignature = args[2]; //"all/50";
		String inputDir = inputBasePath + "/" + datasetSignature;
		
		File obFile = new File(outputBasePath + "/" + datasetSignature);
		if(!obFile.exists()) {
			obFile.mkdirs();
		}
		
		String outDir = outputBasePath + "/" + datasetSignature ;
		
		
		createJavaFiles(inputDir, outDir, "train");
		createJavaFiles(inputDir, outDir, "eval");
		createJavaFiles(inputDir, outDir, "test");
		
	}

	private static void createJavaFiles(String inputDir, String outDir, String folderName)
			throws FileNotFoundException {
		String inFolder = inputDir + "/" + folderName;
		String outFolder = outDir + "/" + folderName;
		File outFolderFile = new File(outFolder);
		if(!outFolderFile.exists()) {
			outFolderFile.mkdir();
		}
		String beforeFile = inFolder + "/before.txt";
		String afterFile = inFolder + "/after.txt";
		Scanner scanB = new Scanner(new File(beforeFile));
		Scanner scanA = new Scanner(new File(afterFile));
		int ln = 0;
		File outFolderBefore = new File(outFolder + "/before");
		File outFolderAfter = new File(outFolder + "/after");
		if(!outFolderBefore.exists()) {
			outFolderBefore.mkdir();
		}
		if(!outFolderAfter.exists()) {
			outFolderAfter.mkdir();
		}
		PrintStream fileNames = new PrintStream(outDir + "/" + folderName + "-files.txt");
		while(scanB.hasNextLine()) {
			ln++;
			String beforeLine = scanB.nextLine();
			String afterLine = scanA.nextLine();
			beforeLine = "public class A {\n" + beforeLine + "\n}\n";
			afterLine = "public class A {\n" + afterLine + "\n}\n";
			File b = new File(outFolder + "/before/" + ln + ".java");
			File a = new File(outFolder + "/after/" + ln + ".java");
			PrintStream beforeStream = new PrintStream(b);
			PrintStream afterStream = new PrintStream(a);
			beforeStream.print(beforeLine);
			afterStream.print(afterLine);
			beforeStream.close();
			afterStream.close();
			String f = b.getAbsolutePath() + "\t" + a.getAbsolutePath();
			//Util.logln(b.getAbsolutePath());
			//Util.logln(a.getAbsolutePath()+"\n\n");
			fileNames.println(f);
		}
		scanB.close();
		scanA.close();
		fileNames.close();
		Util.logln(folderName + "\t" + ln);
	}

}
