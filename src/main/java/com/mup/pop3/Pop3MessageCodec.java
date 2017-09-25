package com.mup.pop3;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

public class Pop3MessageCodec extends MessageToMessageDecoder<String> {

	@Override
	protected void decode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {
		Pop3Command message = new Pop3Command(msg);
		out.add(message);
	}
}
