package edu.virginia.cs.gumtreetest.visitors;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.github.gumtreediff.tree.ITree;

import edu.virginia.cs.gumtreetest.Config;
import edu.virginia.cs.gumtreetest.type.TypeExtractor;

public class DataTypeVisitor {
	public static void setDataTypes(ITree root, String filePath){
		try {
			TypeExtractor ext = new TypeExtractor(filePath);
			Map<Integer, String> types = ext.getDataTypeMap();
			Stack<ITree> st = new Stack<>();
			st.push(root);
			while(!st.empty()){
				ITree curr = st.pop();
				if(curr.isLeaf()){
						int pos = curr.getPos();
						String type = curr.getLabel();
						if(types.containsKey(pos)){
							type = types.get(pos);
						}
						curr.setMetadata(Config.METADATA_TAG.DATA_TYPE, type);
				}
				else{
					List<ITree> children = curr.getChildren();
					for(ITree child : children){
						st.push(child);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
