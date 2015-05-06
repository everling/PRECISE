package precise_repl;

import java.util.Scanner;

public class Input {

	private static Scanner io;
	
	public static void init(){
		io = new Scanner(System.in);
	}
	
	public static String getLine(){
		return io.nextLine();
	}
	
	
	public static void close(){
		io.close();
	}
}
