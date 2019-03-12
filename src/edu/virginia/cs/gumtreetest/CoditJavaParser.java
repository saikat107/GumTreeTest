package edu.virginia.cs.gumtreetest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

public class CoditJavaParser {
	static Argument arg = null;
	
	public static void main(String[] args) throws Exception {
		DateFormat stfmt = new SimpleDateFormat("MM/dd/yy hh:mm:ss");
		Date start = new Date();
		String startTime = stfmt.format(start);
		arg = Argument.preprocessArgument(args);
		Util.logln(arg);
		
		
		File outputFile = new File(arg.outputFilePath());
		if (outputFile.exists()) {
			Util.deleteDirectory(outputFile);
		}
		outputFile.mkdirs();

		DateFormat fmt = new SimpleDateFormat("HH-mm-ss");
		Date d = new Date();
		
		String allFileDirectory = arg.outputFilePath();
		File allFile = new File(allFileDirectory);
		if (!allFile.exists()) {
			allFile.mkdirs();
		}
		int totalFileCount = 0;
		int totalMethodCount = 0;
		Scanner allFilePathsScanner = new Scanner(new File(arg.allPathsFile()));
		
		while (allFilePathsScanner.hasNextLine()) {
			try {
				String filePath = allFilePathsScanner.nextLine().trim();
				try {
					String parentFile = filePath;
					String childFile = filePath.replace("/parent/", "/child/");
					//Util.logln(parentFile + " " + childFile);
					String srcText = Util.readFile(parentFile);
					String destText = Util.readFile(childFile);
					TreeContext srcContext = new JdtTreeGenerator().generateFromFile(parentFile);
					TreeContext destContext = new JdtTreeGenerator().generateFromFile(childFile);
					ITree srcTree = srcContext.getRoot();
					ITree destTree = destContext.getRoot();
					List<NodePair> methodPairs = DiffParser.getMethodPairs(srcTree, destTree, srcText, destText);
					boolean s = false;
					for (NodePair pair : methodPairs) {
						DiffParser parser = new DiffParser(parentFile, childFile, srcText, destText);
						DiffParser.arg = arg;
						//Util.dfsPrint(pair.srcNode);
						boolean successfullyParsed = parser.checkMethodChange(pair.srcNode, pair.tgtNode,
								arg.replace(), arg.excludeStringChange(), arg.maximumMethodSize());
						if (successfullyParsed) {
							Date current = new Date();
							String cTime = stfmt.format(current);
							Util.logln(startTime + " -> " + cTime + "\t" + totalMethodCount);
							DiffParser.printDataToDirectory(allFileDirectory, Arrays.asList(new DiffParser[] { parser }));
							totalMethodCount++;
							s = true;
						}
					}
					if(s) {
						totalFileCount++;
						Util.logln(totalFileCount);
					}
				} catch (Exception localException) {
				}
			} catch (Exception localException1) {
			}
		}
		allFilePathsScanner.close();
	}

}
