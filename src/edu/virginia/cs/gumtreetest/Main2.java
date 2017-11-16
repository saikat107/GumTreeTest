package edu.virginia.cs.gumtreetest;

import java.io.IOException;

import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.tree.ITree;

public class Main2 {
	public static void main(String[] args) throws UnsupportedOperationException, IOException {
		Run.initGenerators();
		String filePath = "src/edu/virginia/cs/gumtreetest/Main.java";
		ITree src = Generators.getInstance().getTree(filePath).getRoot();
		Iterable<ITree> nodes = src.breadthFirst();
		for(ITree node : nodes){
			System.out.println(node.getLabel());
		}
	}

}
