package edu.virginia.cs.gumtreetest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
		public static String HEIGHT = "height";
		public static String NUM_LEAF_NODE = "leaf";
		public static String DEPTH = "depth";
		public static String DATA_TYPE = "dType";
	}
	
	public static class JavaKeywords {
		public static final String[] keywords = {"abstract", "continue", "for", "new", "switch", "assert",  "default", "goto", "package", "synchronized", "boolean", "do", "if", "private", "this", "break", "double", "implements", "protected", "throw", "byte",	"else",	"import", "public", "throws", "case", "enum", "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class",	"finally", "long", "strictfp", "volatile", "const", "float", "native", "super",	"while"};
		public static List<String> keywordList = Arrays.asList(keywords);
		public static final String[] punctuations = {"~", "`", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "-", "_", "+", "=",
				"[", "{", "]", "}", "|", "\\", ";", ":", "'", "\"", ",", "<", ".", ">", "?", "/"};
		public static final List<String> punctuationsList = Arrays.asList(punctuations);
		
		public static final Map<String, Integer> compositeKeyWorkMap = new TreeMap<>();
		static{
			compositeKeyWorkMap.put("for(", 400 + keywordList.indexOf("for"));
			compositeKeyWorkMap.put("while(", 400 + keywordList.indexOf("while"));
			compositeKeyWorkMap.put("if(", 400 + keywordList.indexOf("if"));
			compositeKeyWorkMap.put("catch(", 400 + keywordList.indexOf("catch"));
			compositeKeyWorkMap.put("synchronized(", 400 + keywordList.indexOf("synchronized"));
			compositeKeyWorkMap.put("super(", 400 + keywordList.indexOf("super"));
			compositeKeyWorkMap.put("this(", 400 + keywordList.indexOf("this"));
			
		}
		
		public static boolean isKeyWord(String word){
			return keywordList.contains(word);
		}
		
		private static String transformWord(String word){
			String mWord = "";
			if(word.contains(" ")){
				String [] parts = word.split("[ ]+");
				for(String part : parts){
					mWord += part;
				}
			}
			else{
				mWord = word;
			}
			return mWord;
		}
		
		public static boolean isCompositeKeyword(String word){
			String mWord = transformWord(word);
			return compositeKeyWorkMap.containsKey(mWord);
		}
		
		public static int getCompositeKeyWordType(String key){
			String mWord = transformWord(key);
			if(compositeKeyWorkMap.containsKey(mWord)){
				return compositeKeyWorkMap.get(mWord);
			}
			else{
				return -1;
			}
		}
		
		public static int getKeyWordType(String key){
			if(keywordList.contains(key)){
				return 300 + keywordList.indexOf(key);
			}
			return -1;
		}
	}
	/*
	 * if(leftOver.compareTo("+") == 0){
			return 200;
		}
		else if(leftOver.compareTo("-") == 0){
			return 201;
		}
		else if(leftOver.compareTo("*") == 0){
			return 202;
		}
		else if(leftOver.compareTo("/") == 0){
			return 203;
		}
		else if(leftOver.compareTo("=") == 0){
			return 204;
		}
		else if(leftOver.compareTo("~") == 0){
			return 205;
		}
		else if(leftOver.compareTo("`") == 0){
			return 206;
		}
		else if(leftOver.compareTo("!") == 0){
			return 207;
		}
		else if(leftOver.compareTo("@") == 0){
			return 208;
		}
	 */
	public static class ASTTYPE_TAG{
		public static final int JAVADOC = 29;
		public static final int NUMBER_CONSTANT = 34;
		public static final int STRING_CONSTANT = 45;
		public static final int REST_OF_LEFTOVER = 500;
		public static final int IMPORT_STATEMENT = 26;
		public static final int CLASS_DEFINITION = 55;
		public static final int METHOD_DECLARATION = 31;
		public static final int METHOD_MODIFIER = 83;
		public static final int VARIABLE_DECLARATION = 60;
		public static final int VARIABLE_INITIALIZATION = 59;
		public static final int BODY  = 8;
		public static final int FUNCTION_PARAMETER = 44;
		public static final int SIMPLE_NAME = 42;
		public static final int COMPLEX_NAME = 40;
		public static final int PREMITIVE_DATATYPE = 39;
		public static final int ARTIFICIAL_DATATYPE = 43;
		public static final int STATEMENT = 21;
		public static final int TRY_STATEMENT = 54;
		public static final int CATCH_BLOCK = 12;
		public static final int FOR_STATEMENT = 24;
		public static final int WHILE_STATEMENT = 61;
		public static final int DO_WHILE_STATEMENT = 19;
		public static final int CONDITIONAL_STATEMENT = 27;
		public static final int IF_STATEMENT = 25;
		public static final int METHOD_CALL = 32;
		public static final int CLASS_VARIABLE_DECLARATION = 23;
		public static final int STATIC_BLOCK = 28;
		public static final int CHAR_CONST = 13;
		public static final int ARRAY_IDX = 85;
		
		public static final int INSIDE_JAVADOC1 = 65;
		public static final int INSIDE_JAVADOC2 = 66;
	}
}

