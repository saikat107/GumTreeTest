package edu.virginia.cs.gumtreetest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

public class Main {
	public static void main(String[] args) throws UnsupportedOperationException, IOException {
		Run.initGenerators();
		String srcPath = "src/edu/virginia/cs/gumtreetest/Parent.java";
		String destPath = "src/edu/virginia/cs/gumtreetest/Child.java";
		TreeContext srcContext = new JdtTreeGenerator().generateFromFile(srcPath);
		TreeContext destContext = new JdtTreeGenerator().generateFromFile(destPath);
		ITree srcTree = srcContext.getRoot();
		ITree destTree = destContext.getRoot();
		//Util.dfsPrint(srcTree);
		//Util.dfsPrint(destTree);
		Matcher m = Matchers.getInstance().getMatcher(srcTree, destTree);
		m.match();
		MappingStore store = m.getMappings();
		Set<Mapping> matchSet = store.asSet();
		for(Mapping map : matchSet){
			//System.out.println(Thread.currentThread().getStackTrace()[1]);
			//System.out.println(map.getFirst().getId() + " " + map.getSecond().getId());
		}
		
		ActionGenerator gen = new ActionGenerator(srcTree, destTree, store);
		gen.generate();
		List<Action> actions = gen.getActions();
		List<ITree> srcCommonParent = null;
		List<ITree> destCommonParent = null;
		for(Action action : actions){
			//System.out.println(action);
			List<ITree> parents = action.getNode().getParents();
			
			if (action.getName() == "INS"){
				parents.add(0, action.getNode());
				//Util.logln(parents);
				if (destCommonParent == null){
					destCommonParent = parents;
				}
				else{
					for(ITree parent : parents){
						if(destCommonParent.contains(parent)){
							destCommonParent = parent.getParents();
							destCommonParent.add(0, parent);
							//Util.logln(destCommonParent);
							break;
						}
					}
				}
			}
			else if(action.getName() == "MOV"){
				//Util.logln(parents);
				if (srcCommonParent == null){
					srcCommonParent = parents;
				}
				else{
					for(ITree parent : parents){
						if(srcCommonParent.contains(parent)){
							srcCommonParent = parent.getParents();
							srcCommonParent.add(0, parent);
							//Util.logln(srcCommonParent);
							break;
						}
					}
				}
			}
			else{
				parents.add(0, action.getNode());
				//Util.logln(parents);
				if (srcCommonParent == null){
					srcCommonParent = parents;
				}
				else{
					for(ITree parent : parents){
						if(srcCommonParent.contains(parent)){
							srcCommonParent = parent.getParents();
							srcCommonParent.add(0, parent);
							//Util.logln(srcCommonParent);
							break;
						}
					}
				}
			}
		
		}
		ITree cParentSrc = null;
		ITree cParentDest = null;
		if(srcCommonParent != null){
			for(ITree cSrcParent : srcCommonParent){
				ITree cDestParent = store.getDst(cSrcParent);
				if(destCommonParent == null){
					if(cDestParent != null){
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						break;
					}
				}
				else if (destCommonParent.contains(cDestParent)){
					cParentSrc = cSrcParent;
					cParentDest = cDestParent;
					break;
				}
			}
		}
		else if(destCommonParent != null){
			for(ITree cDestParent : destCommonParent){
				ITree cSrcParent = store.getSrc(cDestParent);
				if(srcCommonParent == null){
					if(cSrcParent != null){
						cParentSrc = cSrcParent;
						cParentDest = cDestParent;
						break;
					}
				}
				else if (srcCommonParent.contains(cSrcParent)){
					cParentSrc = cSrcParent;
					cParentDest = cDestParent;
					break;
				}
			}
		}
		Util.dfsPrint(cParentSrc);
		cParentSrc.setMetadata("methodSig", "public void test()");
		Util.dfsPrint(cParentDest);
		Util.logln(cParentSrc.getClass());
		ITree t = new Tree(5, "jik");
		t.setId(0);
		t.addChild(new Tree(8, "djkdk"));
		Util.dfsPrint(t);
		Util.logln(cParentSrc.getMetadata("methodSig"));
	}

}
