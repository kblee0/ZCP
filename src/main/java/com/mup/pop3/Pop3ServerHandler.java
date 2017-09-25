package com.mup.pop3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class Pop3ServerHandler extends SimpleChannelInboundHandler<CommandMessage> {
	private final static Logger log = LoggerFactory.getLogger(Pop3ServerHandler.class);
	private Mailbox mailBox = null;

	private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

	private String user;

	private boolean userNameGiven = false;
	private boolean authenticated = false;

	public Pop3ServerHandler() {
		super();
	}

	public Pop3ServerHandler(Mailbox box) {
		super();
		mailBox = box;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.info("POP3 channel active. (client={})", ctx.channel().remoteAddress().toString());

		writeOk(ctx, "POP3 server ready");
		ctx.flush();

		channels.add(ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		mailBox.close();
		log.info("POP3 channel inactive. (client={})", ctx.channel().remoteAddress().toString());
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		log.debug("channelRead");

		log.debug("COMMAND = : " + msg.getCommand());
		log.debug("MESSAGE = : " + msg.getMessage());

		if (!authenticated) {
			switch (msg.getCommand()) {
			case Pop3Command.CAPA:
				commandCapa(ctx, msg);
				break;
			case Pop3Command.USER:
				commandUser(ctx, msg);
				break;
			case Pop3Command.APOP:
				commandApop(ctx, msg);
				break;
			case Pop3Command.PASS:
				commandPass(ctx, msg);
				break;
			case Pop3Command.QUIT:
				commandQuit(ctx, msg);
				break;
			default:
				writeErr(ctx, "Unknown command or not implemented - " + msg.getMessage());
				break;
			}
		} else {
			switch (msg.getCommand()) {
			case Pop3Command.CAPA:
				commandCapa(ctx, msg);
				break;
			case Pop3Command.LIST:
				commandList(ctx, msg);
				break;
			case Pop3Command.UIDL:
				commandUidl(ctx, msg);
				break;
			case Pop3Command.STAT:
				commandStat(ctx, msg);
				break;
			case Pop3Command.RETR:
				commandRetr(ctx, msg);
				break;
			case Pop3Command.TOP:
				commandTop(ctx, msg);
				break;
			case Pop3Command.DELE:
				commandDele(ctx, msg);
				break;
			case Pop3Command.NOOP:
				commandNoop(ctx, msg);
				break;
			case Pop3Command.QUIT:
				commandQuit(ctx, msg);
				break;
			default:
				writeErr(ctx, "Unknown command or not implemented - " + msg.getMessage());
				break;
			}
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		log.debug("channelReadComplete");
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	public void commandCapa(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		writeOk(ctx, "Capability list follows");
		writeLine(ctx, "TOP");
		writeLine(ctx, "USER");
		writeLine(ctx, "EXPIRE NEVER");
		// writeLine(ctx, "SASL NTLM");
		// writeLine(ctx, "RESP-CODES");
		// writeLine(ctx, "LOGIN-DELAY");
		// writeLine(ctx, "PIPELINING");
		writeLine(ctx, "UIDL");
		writeLine(ctx, "IMPLEMENTATION SACM/1.0");
		writeLine(ctx, ".");
	}

	public void commandApop(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		authenticated = false;
		writeErr(ctx, "APOP command not implemented.");
	}
	public void commandUser(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		user = msg.getParam(0);
		userNameGiven = true;

		if ("nopassword".equals(user)) {
			commandPass(ctx, msg);
			return;
		}
		writeOk(ctx, "Please give password.");
	}

	public void commandPass(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		if( !userNameGiven ) {
			writeErr(ctx, "user name is required.");
			return;
		}
		String password = msg.getParam(0);
		authenticated = mailBox.open(user, password);
		if (!authenticated) {
			writeErr(ctx, "login failed.");
			return;
		}
		mailBox.refresh();
		writeMboxStat(ctx);
	}

	public void commandQuit(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		mailBox.updateTransaction();
		writeOk(ctx, "Bye.");
		ctx.flush().close();
	}

	public void commandList(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		if( msg.getParam(0) == null ) {
			writeMboxStat(ctx);
			for (Map.Entry<Integer, MailItem> elm : mailBox.getMbox().entrySet()) {
				int no = elm.getKey();
				MailItem item = elm.getValue();
				writeLine(ctx, no + " " + item.getSize());
			}
			writeLine(ctx, ".");
		}
		else {
			int no = Integer.parseInt(msg.getParam(0));
			MailItem item = mailBox.getMail(no);
			if( item != null ) {
				writeOk(ctx, no + " " + item.getSize());
			}
			else {
				writeErr(ctx, "no such message or message deleted.");
			}
		}
	}

	public void commandUidl(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		if (msg.getParam(0) == null) {
			writeMboxStat(ctx);
			for (Map.Entry<Integer, MailItem> elm : mailBox.getMbox().entrySet()) {
				int no = elm.getKey();
				MailItem item = elm.getValue();
				writeLine(ctx, no + " " + item.getUid());
			}
			writeLine(ctx, ".");
		} else {
			int no = Integer.parseInt(msg.getParam(0));
			MailItem item = mailBox.getMail(no);
			if (item != null) {
				writeOk(ctx, no + " " + item.getUid());
			} else {
				writeErr(ctx, "no such message or message deleted.");
			}
		}
	}

	public void commandStat(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		writeOk(ctx, mailBox.getCount() + " " + mailBox.getSize());
	}

	public void commandRetr(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		if (msg.getParam(0) == null) {
			writeErr(ctx, "message number missing." + msg.getMessage());
			return;
		} else {
			int no = Integer.parseInt(msg.getParam(0));
			MailItem item = mailBox.getMail(no);
			if(item == null) {
				writeErr(ctx, "no such message or message deleted.");
				return;
			}
			writeOk(ctx, item.getSize() + " octets");
			if (item.getCategory() != null) {
				writeLine(ctx, "Keywords: " + item.getCategory());
			}
			try {
				writeInputStream(ctx, mailBox.getMailInputStream(no), -1);
			} catch (IOException e) {
				log.error(" Item read error. " + item.toString());
				log.error(e.toString());
			}

			writeLine(ctx, ".");
		}
	}

	public void commandTop(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		if (msg.getParam(0) == null || msg.getParam(1) == null) {
			writeErr(ctx, "message number & line number missing." + msg.getMessage());
			return;
		} else {
			int no = Integer.parseInt(msg.getParam(0));
			int lineNo = Integer.parseInt(msg.getParam(1));
			MailItem item = mailBox.getMail(no);
			if (item == null) {
				writeErr(ctx, "no such message or message deleted.");
				return;
			}
			writeOk(ctx, "Message follows.");
			if (item.getCategory() != null) {
				writeLine(ctx, "Keywords: " + item.getCategory());
				lineNo--;
			}
			try {
				writeInputStream(ctx, mailBox.getMailInputStream(no), lineNo);
			} catch (IOException e) {
				log.error(" Item read error. " + item.toString());
				log.error(e.toString());
			}

			writeLine(ctx, ".");
		}
	}

	public void commandDele(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		if (msg.getParam(0) == null) {
			writeErr(ctx, "message number missing." + msg.getMessage());
			return;
		}
		int no = Integer.parseInt(msg.getParam(0));

		switch (mailBox.deleteMail(no)) {
		case 1:
			writeOk(ctx, "Message Deleted.");
			break;
		case 0:
			writeErr(ctx, "Message already deleted.");
			break;
		default:
			writeErr(ctx, "No such message.");

		}
	}

	public void commandNoop(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
		writeOk(ctx, null);
	}

	public void writeMboxStat(ChannelHandlerContext ctx) throws Exception {
		writeOk(ctx, mailBox.getCount() + " message(s) (" + mailBox.getSize() + " octets).");
	}
	private void writeOk(ChannelHandlerContext ctx, String msg) throws Exception {
		String res = null;
		if (msg == null) {
			res = "+OK";
		} else {
			res = "+OK " + msg;
		}
		log.debug("SEND:" + res);

		ctx.write(res);
	}

	private void writeErr(ChannelHandlerContext ctx, String msg) throws Exception {
		String res = null;
		if (msg == null) {
			res = "-ERR";
		} else {
			res = "-ERR " + msg;
		}
		log.debug("SEND:" + res);
		ctx.write(res);
	}

	private void writeLine(ChannelHandlerContext ctx, String msg) throws Exception {
		log.debug("SEND:" + msg);
		ctx.write(msg);
	}

	public void writeInputStream(ChannelHandlerContext ctx, InputStream input, int limit) throws Exception {
		if (limit == 0) {
			return;
		}
		InputStreamReader inputStreamReader = new InputStreamReader(input, "UTF-8");
		BufferedReader reader = new BufferedReader(inputStreamReader);

		String line = null;
		try {
			while ((line = reader.readLine()) != null && (limit > 0 || limit < 0)) {
				if (".".equals(line)) {
					writeLine(ctx, "..");
				}
				else {
					writeLine(ctx, line);
				}
				limit--;
			}
		} finally {
			reader.close();
		}
	}

}

