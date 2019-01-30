package edu.virginia.cs.gumtreetest.visitors;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import com.github.gumtreediff.tree.ITree;


import edu.virginia.cs.gumtreetest.Config;
import edu.virginia.cs.gumtreetest.Util;

public class VariableVisitor {
	
	public static Map<String, String> getSymbolTable(ITree root){
		int identifierNumber = 1;
		Map<String, String> symbolTree = new TreeMap<String, String>();
		Stack<ITree> stack = new Stack<ITree>();
		stack.push(root);
		while(!stack.isEmpty()){
			ITree curr = stack.pop();
			if(curr.getType() == Config.ASTTYPE_TAG.SIMPLE_NAME || curr.getType() == Config.ASTTYPE_TAG.COMPLEX_NAME){
				String name = curr.getLabel();
				if(!symbolTree.containsKey(name)){
					symbolTree.put(name, "t"+identifierNumber);
					identifierNumber++;
				}
				String identifierName = symbolTree.get(name);
				curr.setMetadata("subs_name", identifierName);
			}
			stack.addAll(curr.getChildren());
		}
		return symbolTree;
	}
	
	public static Set<String> getNames(ITree root){
		Set<String> names = new TreeSet<String>();
		Stack<ITree> stack = new Stack<ITree>();
		stack.push(root);
		while(!stack.isEmpty()){
			ITree curr = stack.pop();
			int type = curr.getType();
			if(type == Config.ASTTYPE_TAG.METHOD_DECLARATION){
				//Util.logln(getMethodName(curr));
			}
			else if(type == Config.ASTTYPE_TAG.METHOD_CALL){
				/*List<ITree> children = curr.getChildren();
				for(ITree child : children){
					Util.logln(child.getLabel());
					names.add(child.getLabel());
				}*/
			}
			else if(type == Config.ASTTYPE_TAG.FUNCTION_PARAMETER){
				List<ITree> children = curr.getChildren();
				ITree parameterName = children.get(children.size() - 1);
				ITree parameterType = children.get(children.size() - 2);
				Util.logln(parameterName.getLabel() + " " + parameterType.getLabel());
				names.add(parameterName.getLabel());
			}
			else if (type == Config.ASTTYPE_TAG.VARIABLE_DECLARATION) {
				List<ITree> children = curr.getChildren();
				String typeName = "";
				for(int i = 0; i < children.size(); i++){
					ITree child = children.get(i);
					if(child.getType() == Config.ASTTYPE_TAG.PREMITIVE_DATATYPE ||
							child.getType() == Config.ASTTYPE_TAG.ARTIFICIAL_DATATYPE){
						typeName = child.getLabel();
					}
					else if(child.getType() == Config.ASTTYPE_TAG.VARIABLE_INITIALIZATION){
						Util.logln(child.getChildren().get(0).getLabel() + " " + typeName);
						names.add(child.getChildren().get(0).getLabel());
					}
				}
			}
			else if (type == Config.ASTTYPE_TAG.CLASS_VARIABLE_DECLARATION) {
				List<ITree> children = curr.getChildren();
				String typeName = "";
				for(int i = 0; i < children.size(); i++){
					ITree child = children.get(i);
					if(child.getType() == Config.ASTTYPE_TAG.PREMITIVE_DATATYPE ||
							child.getType() == Config.ASTTYPE_TAG.ARTIFICIAL_DATATYPE){
						typeName = child.getLabel();
					}
					else if(child.getType() == Config.ASTTYPE_TAG.VARIABLE_INITIALIZATION){
						Util.logln(child.getChildren().get(0).getLabel() + " " + typeName);
						names.add(child.getChildren().get(0).getLabel());
					}
				}
			}
			stack.addAll(curr.getChildren());
		}
		return names;
	}
	
	public String getVariableName(ITree root){
		return null;
	}
	
	public String getMethodName(ITree root){
		assert root.getType() == Config.ASTTYPE_TAG.METHOD_DECLARATION;
		List<ITree> children = root.getChildren();
		//Util.logln(children);
		for(ITree child : children){
			if(child.getType() == Config.ASTTYPE_TAG.SIMPLE_NAME){
				return child.getLabel();
			}
		}
		return null;
	}

}
