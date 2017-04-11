package edu.illinois.uiuc.sp17.cs425.team4.MP2;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;

public class PrepareBatchFile {
	public static void main(String[] args) throws FileNotFoundException {
		PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream("100kKeysBatchFile.txt")), true);
		for (int i = 1; i <= 100000; i++) {
			ps.println(String.format("SET key%s value %s",i, i ));
		}
		
		for (int i = 1; i <=1000; i++) {
			int k = new Random().nextInt(100000);
			ps.println(String.format("GET key%s",k));
		}
		
		ps.close();
	}
}
