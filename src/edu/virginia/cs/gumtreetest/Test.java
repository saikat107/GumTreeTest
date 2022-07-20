package edu.virginia.cs.gumtreetest;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Test {
	public static void main(String[] args) throws FileNotFoundException {
		String fullText = "{ int len = s . length () ; if ( len == 0 ) { return false ; } for ( int index = 0 ; index < len ; index ++ ) { char c = s . charAt ( index ) ; if ( c < '0' || c > '9' ) { return false ; } } return len == 1 || s . charAt ( 0 ) != '0' ; }";
		String bugText = "{ char c = s . charAt ( index ) ; if ( c < '0' || c > '9' ) { return false ; } }";
		int index = fullText.indexOf(bugText);
		String nextText = fullText.substring(0, index) + "<BUG_START> " + bugText + " <BUG_END> " + fullText.substring(index + bugText.length()).trim();
		System.out.println(nextText);
		
	}

}
