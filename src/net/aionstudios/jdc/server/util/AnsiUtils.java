package net.aionstudios.jdc.server.util;

public class AnsiUtils {
	
	public static final String ANSI_CLS = "\033[2J\033[;H";
	public static final String ANSI_HOME = "\u001b[H";
	public static final String ANSI_BOLD = "\u001b[1m";
	public static final String ANSI_AT55 = "\u001b[10;10H";
	public static final String ANSI_REVERSEON = "\u001b[7m";
	public static final String ANSI_NORMAL = "\u001b[0m";
	public static final String ANSI_WHITEONBLUE = "\u001b[37;44m";
	  
	public static final String BLACK = "\u001B[0;30m";
	public static final String RED = "\u001B[0;31m";
	public static final String GREEN = "\u001B[0;32m";
	public static final String YELLOW = "\u001B[0;33m";
	public static final String BLUE = "\u001B[0;34m";
	public static final String MAGENTA = "\u001B[0;35m";
	public static final String CYAN = "\u001B[0;36m";
	public static final String WHITE = "\u001B[0;37m";
	
	private static int ATT = 0;
	private static int FORE = 37;
	private static int BACK = 40;
	
	public static int ATT_NORMAL = 0;
	public static int ATT_BOLD = 1;
	public static int ATT_UL = 4;
	public static int ATT_BLINK = 5;
	public static int ATT_REV = 7;
	public static int ATT_NO = 8;
	
	public static void setAttribute(int att){
		ATT = att;
	}
	
	public static int FORE_BLACK = 30;
	public static int FORE_RED = 31;
	public static int FORE_GREEN = 32;
	public static int FORE_YELLOW = 33;
	public static int FORE_BLUE = 34;
	public static int FORE_MAGENTA = 35;
	public static int FORE_CYAN = 36;
	public static int FORE_WHITE = 37;
	
	public static void setForeground(int fore){
		FORE = fore;
	}
	
	public static int BACK_BLACK = 40;
	public static int BACK_RED = 41;
	public static int BACK_GREEN = 42;
	public static int BACK_YELLOW = 43;
	public static int BACK_BLUE = 44;
	public static int BACK_MAGENTA = 45;
	public static int BACK_CYAN = 46;
	public static int BACK_WHITE = 47;
	
	public static String genAnsiCode(){
		return "\u001b["+ATT+";"+FORE+";"+BACK+"m";
	}
	
	public static void setBackground(int back){
		BACK = back;
	}
	
	public static String genAddAnsiCode(int att, int fore, int back){
		setAttribute(att);
		setForeground(fore);
		setBackground(back);
		return genAnsiCode();
	}

}
