package edu.virginia.cs.gumtreetest;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import org.hamcrest.core.IsInstanceOf;

import com.github.gumtreediff.tree.ITree;

public class Util {
	public static void logln(Object msg){
		StackTraceElement elem = Thread.currentThread().getStackTrace()[2];
		System.out.print(elem);
		if (msg instanceof Collection) {
			Collection list = (Collection) msg;
			System.out.print("[");
			for(Object obj: list){
				if (obj instanceof ITree) {
					ITree tr = (ITree) obj;
					System.out.print(tr.getId() + " ");
				}
				else{
					System.out.print(obj + " ");
				}
			}
			System.out.println("]");
		}
		else{
			System.out.println(msg);
		}
	}

	public static void dfsPrint(ITree root){
		Stack<ITree> nodes = new Stack<>();
		Stack<Integer> level = new Stack<>();
		nodes.push(root);
		level.push(0);
		while(!nodes.isEmpty()){
			ITree curr = nodes.pop();
			int l = level.pop();
			for(int i = 0; i < l; i++){
				System.out.print('\t');
			}
			System.out.println(curr.getType() + " " + curr.getId() + " " + curr.getLabel());
			List<ITree> children = curr.getChildren();
			int cz = children.size();
			for(int idx = cz-1;  idx >=0; idx --){
				ITree child = children.get(idx);
				nodes.push(child);
				level.push(l+1);
			}
		}
	}

}
