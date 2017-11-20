package edu.virginia.cs.gumtreetest;

public class Config {
	public static final String PATCH_PATH = "/home/saikat/zf8/sc2nf/Research/acid_test/patches/";
	public static final String LINE_NUMBER_INFO_FILE_NAME = "line_numbers.csv";
	public static final String OUTPUT_DIR = "C:\temp";
	public static final String PARENT_CODE_OUTPUT_PATH = OUTPUT_DIR + "/parent.data";
	public static final String PARENT_TREE_OUTPUT_PATH = OUTPUT_DIR + "/parent.tree";
	public static final String CHILD_CODE_OUTPUT_PATH = OUTPUT_DIR + "/child.data";
	public static final String CHILD_TREE_OUTPUT_PATH = OUTPUT_DIR + "/child.tree";
	public static final int START_ID_OF_LEAF_NODE = 1;
	public static final int START_ID_OF_NON_LEAF_NODE = 201;
	public static final int INTERMEDIATE_NODE_TYPE = 300;
	public static class METADATA_TAG{
		static String HEIGHT = "height";
		static String NUM_LEAF_NODE = "leaf";
		static String DEPTH = "depth";
	}
}

