package com.mup.pop3;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.LineEncoder;
import io.netty.handler.codec.string.LineSeparator;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;

public class Pop3Server {
	private final int SERVICE_TYPE_FOLDER = 1;
	private final int SERVICE_TYPE_ZCP = 0;

	private final String CONFIG_FILE = "pop3server.xml";
	private final int DEFAULT_READ_TIMEOUT = 120;
	
	private final static Logger log = LoggerFactory.getLogger(Pop3ServerHandler.class);
	private static Pop3Server instance = null;

	private EventLoopGroup parentGroup;
	private EventLoopGroup childGroup;
	private int serverPort = 110;
	private int backLog = 10;
	private int serviceType = SERVICE_TYPE_ZCP;
	private boolean isStarted = false;
	private XMLConfiguration config = null;

	Map<String, String> folderList = null;

	private Pop3Server() {

	}

	public static Pop3Server getInstance() {
		if (instance == null) {
			instance = new Pop3Server();
		}
		return instance;
	}

	public boolean loadProperty() {
		try
		{
			config = new XMLConfiguration();
			File configFile = ConfigFile.getFile(CONFIG_FILE);
			if (configFile == null) {
				log.error("Pop3Server cannot find configuration file({})", CONFIG_FILE);
				System.exit(1);
			}
			log.info("Starting Pop3Server ({})", configFile.getAbsolutePath());

			config.load(configFile);

			serverPort = config.getInt("server.port");
			backLog = config.getInt("server.backlog");
			if( "folder".equalsIgnoreCase(config.getString("server.serviceType")) ) {
				serviceType = SERVICE_TYPE_FOLDER;
				log.info("SERVICE TYPE(folder|zcp) = folder");
			}
			else {
				log.info("SERVICE TYPE(folder|zcp) = zcp");
			}
			
			folderList = new HashMap<String, String>();
			for (int i = 0;; i++) {
				String path = config.getString("folders.folder(" + i + ").path");
				if (path == null) {
					break;
				}
				String catg = config.getString("folders.folder(" + i + ").category");

				folderList.put(path, catg);
			}

		}
		catch (Exception e) {
			log.error("loadProperty Error: " + e.getMessage());
			return false;
		}
		return true;
	}

	public boolean start() {
		if (isStarted) {
			return false;
		}

		if (this.loadProperty() == false) {
			return false;
		}

		parentGroup = new NioEventLoopGroup(1);
		childGroup = new NioEventLoopGroup();

		ServerBootstrap serverBootstrap = new ServerBootstrap();
		
		// Create mailbox

		serverBootstrap.group(parentGroup, childGroup);
		serverBootstrap.channel(NioServerSocketChannel.class);
		serverBootstrap.option(ChannelOption.SO_BACKLOG, backLog);
		serverBootstrap.handler(new LoggingHandler(LogLevel.DEBUG));
		serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel socketChannel) throws Exception {
				Mailbox mbox = null;
				
				if( serviceType == SERVICE_TYPE_ZCP ) {
					ZimbraMailBox zmbox = ZimbraMailBox.create(config.getString("zimbra.serviceUrl"));
					zmbox.setFolderList(folderList);
					if (config.getString("zimbra.messageIdStore") != null) {
						zmbox.setDbFile(config.getString("zimbra.messageIdStore"));
					}
					if (config.getInteger("zimbra.searchRequestLimit", -1) > 0) {
						zmbox.setSearchLimit(config.getInteger("zimbra.searchRequestLimit", -1));
					}
					mbox = zmbox;
				}
				else {
					mbox = EmlFilesMailbox.create().setFolderList(folderList);
				}

				socketChannel.pipeline().addLast(new ReadTimeoutHandler(DEFAULT_READ_TIMEOUT))
						.addLast(new LineBasedFrameDecoder(4096, true, true))
						.addLast(new StringDecoder(CharsetUtil.UTF_8),
								new LineEncoder(LineSeparator.WINDOWS, CharsetUtil.UTF_8))
						.addLast(new Pop3MessageCodec(), new LoggingHandler(LogLevel.DEBUG))
						.addLast(new Pop3ServerHandler(mbox));
			}
		});

		ChannelFuture channelFuture = serverBootstrap.bind(serverPort).awaitUninterruptibly();
		if (!channelFuture.isSuccess()) {
			log.error("bind({}) error : {}", serverPort, channelFuture.cause().getMessage());
			this.shutdown();
			return false;
		}
		// channelFuture.channel().closeFuture();
		isStarted = true;
		log.info("Started Pop3Server with parameter. (port={}, backLog={}}", serverPort, backLog);
		return isStarted;
	}

	public boolean isStarted() {
		return isStarted;
	}
	public void shutdown() {
		if (parentGroup != null) {
			parentGroup.shutdownGracefully();
			parentGroup.terminationFuture().syncUninterruptibly();
		}
		if (childGroup != null) {
			childGroup.shutdownGracefully();
			childGroup.terminationFuture().syncUninterruptibly();
		}
		isStarted = false;
		log.info("Stopped Pop3Server");
	}
}

