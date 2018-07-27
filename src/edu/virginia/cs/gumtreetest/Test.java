package edu.virginia.cs.gumtreetest;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Test {
	public static void main(String[] args) throws FileNotFoundException {
		PrintStream parentCode = new PrintStream(new FileOutputStream("data/output.txt", true));
		parentCode.println("Hello");
		parentCode.close();
	}

}
