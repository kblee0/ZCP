package com.mup.pop3;

public class Pop3Command extends CommandMessage {

	public final static int CAPA = 1;
	public final static int QUIT = 2;
	// AUTHORIZATION State
	public final static int APOP = 10;
	public final static int USER = 11;
	public final static int PASS = 12;

	// TRANSACTION State
	public final static int STAT = 21;
	public final static int LIST = 22;
	public final static int RETR = 23;
	public final static int DELE = 24;
	public final static int NOOP = 25;
	public final static int RSET = 26;

	// Optional POP3 Commands
	public final static int TOP = 31;
	public final static int UIDL = 32;

	public Pop3Command(String msg) {
		super(msg);
	}

	public int getCommand(String cmd) {
		if (cmd == null)
			return 0;
		else if ("CAPA".equals(cmd))
			return CAPA;
		else if ("APOP".equals(cmd))
			return APOP;
		else if ("USER".equals(cmd))
			return USER;
		else if ("PASS".equals(cmd))
			return PASS;
		else if ("QUIT".equals(cmd))
			return QUIT;
		else if ("STAT".equals(cmd))
			return STAT;
		else if ("LIST".equals(cmd))
			return LIST;
		else if ("RETR".equals(cmd))
			return RETR;
		else if ("DELE".equals(cmd))
			return DELE;
		else if ("NOOP".equals(cmd))
			return NOOP;
		else if ("RSET".equals(cmd))
			return RSET;
		else if ("TOP".equals(cmd))
			return TOP;
		else if ("UIDL".equals(cmd))
			return UIDL;
		return 0;
	}
}
