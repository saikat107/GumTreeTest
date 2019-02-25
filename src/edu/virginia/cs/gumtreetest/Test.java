package edu.virginia.cs.gumtreetest;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;

public class Test {
	
	private static boolean isThereChange(ITree t1, ITree t2) {
		Matcher m = Matchers.getInstance().getMatcher(t1, t2);
		m.match();
		MappingStore store = m.getMappings();
		ActionGenerator gen = new ActionGenerator(t1, t2, store);
		gen.generate();
		List<Action> actions = gen.getActions();
		return actions.size() != 0;
	}
	
	/**
	 * Checks whether there is a code change between java file file1 and file2
	 * Always return a list of boolean of length 2
	 * first position in the return list denotes whether there is a code difference between 2 files
	 * second position in the return list denotes whether there is a code difference between methods of those 2 files
	 * @param file1
	 * @param file2
	 * @return
	 */
	private static List<Boolean> getChanges(String file1, String file2){
		List<Boolean> changes = new ArrayList<Boolean>();
		try {
			String srcText = Util.readFile(file1);
			String destText = Util.readFile(file2);
			TreeContext srcContext = new JdtTreeGenerator().generateFromString(srcText);
			TreeContext destContext = new JdtTreeGenerator().generateFromString(destText);
			ITree srcTree = srcContext.getRoot();
			ITree destTree = destContext.getRoot();
			if(isThereChange(srcTree, destTree)) { //There is change in file
				changes.add(true);
				boolean methodChangeFound = false;
				List<NodePair> methodsPairs = DiffParser.getMethodPairs(srcTree, destTree, srcText, destText);
				for(NodePair methods: methodsPairs) {
					if(isThereChange(methods.srcNode, methods.tgtNode)) {
						methodChangeFound = true; // There is at least 1 method changes
						break;
					}
				}
				changes.add(methodChangeFound);
			}
			else {
				changes.add(false); //No File Change, hence no method change
				changes.add(false);
			}
			
		}catch(IOException ex) {
			ex.printStackTrace();
		}
		return changes;
	}
	
	
	public static void main(String[] args) throws FileNotFoundException {
		if(args.length != 2) {
			usage();
		}
		String file1 = args[0];
		String file2 = args[1];
		List<Boolean> changes = getChanges(file1, file2);
		System.out.println("File Change : " + changes.get(0));
		System.out.println("Method Change : " + changes.get(1));
	}

	private static void usage() {
		Util.logln("java Test <File1> <File2>");
		System.exit(-1);
	}

}
