package edu.virginia.cs.gumtreetest;


import java.io.IOException;
import java.util.List;


public class TCRunUtil {
	private static String defects4j = "defects4j";
	
	/*public static void extractTriggerTests()throws Exception{
		System.out.println("triggerTests = new HashMap<String, List<String>>();");
		for(int i = 0; i < 2; i++) {
			String project = projects[i];
			int numBugs = bugs[i];
			for(int bid = 1; bid <= numBugs; bid++) {
				List<String> testCases = new ArrayList<String>();
				String path = "/home/sc2nf/defects4j/framework/projects/" + project + "/trigger_tests/" + bid;
				Scanner pathScanner = new Scanner(new File(path));
				while(pathScanner.hasNextLine()) {
					String line = pathScanner.nextLine();
					if(line.startsWith("---")) {
						String tc = line.substring(4).trim();
						testCases.add(tc);
					}
				}
				String key = project+bid;
				String out = "triggerTests.put(\"" + key + "\", Arrays.asList(new String[] {";
				for(String tc : testCases) {
					out += "\"" + tc + "\", ";
				}
				out += "}));";
				System.out.println(out);
				pathScanner.close();
			}
		}
	}*/
	
	public static boolean checkSuccessfulCompilation(String basePath) {
		String command = defects4j + " compile -w " + basePath;
		String result = Util.getCommandExecutionResult(command);
		if(result.contains("BUILD FAILED")) {
			return false;
		}
		return true;
	}
	
	
	public static boolean checkAllTestCasePassing(String basePath) {
		String command = defects4j + " test -w " + basePath;
		String result = Util.getCommandExecutionResult(command);
		if(result.contains("BUILD FAILED")) {
			return false;
		}
		else {
			String tcLine = result.substring(0, result.indexOf("\n"));
			int failedTestCase = Integer.parseInt((tcLine.split(":")[1]).trim());
			if(failedTestCase > 0) {
				return false;
			}
		}
		return true;
	}
	
	public static String checkTriggerTestCasePassing(
			String project, String bugID, String basePath) throws IOException {
		String key = project+bugID;
		if(!TriggerTests.containsKey(key)) {
			throw new IOException("Inavlid Project Name : " + project + "Or Invalid BugId : " + bugID);
		}
		List<String> testCases = TriggerTests.get(key);
		int totalTestCases = testCases.size();
		int passed = 0;
		for(String testCase : testCases) {
			String command = defects4j + " test -w " + basePath + " -t " + testCase;
			String result = Util.getCommandExecutionResult(command);
			String tcLine = result.substring(0, result.indexOf("\n"));
			int failedTestCase = Integer.parseInt((tcLine.split(":")[1]).trim());
			if(failedTestCase == 0) {
				passed++;
			}
		}
		Util.logln(project + " " + bugID + "\nTotal Test Case : " + totalTestCases + "\t Passed : " + passed); 
		if(passed == totalTestCases) {
			return "PASS";
		}
		else {
			return "FAIL:" + passed + "/" + totalTestCases;
		}
	}

}
