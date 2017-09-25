package com.mup.pop3;

public abstract class CommandMessage {
	private String[] argv;
	private String message;
	private int command;

	public CommandMessage() {

	}

	public CommandMessage(String msg) {
		setMessage(msg);
	}
	public void setMessage(String msg) {
		argv = msg.split("\\s+");
		command = getCommand(argv[0]);
	}
	public String getMessage() {
		return message;
	}

	public int getCommand() {
		return command;
	}

	public abstract int getCommand(String cmd);

	public String getCommandString() {
		return getArgument(0);
	}
	public String getParam(int i) {
		return getArgument(i + 1);
	}
	private String getArgument(int i) {
		if (i < argv.length) {
			return argv[i];
		}
		return null;
	}

}
