package org.tianfan.httpjson;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.CharsetUtil;
import java.util.List;

public class JsonCodec extends MessageToMessageCodec<ByteBuf, Object> {
    private static final String HEADER = "JSON";
    private static final byte[] HEADER_BYTES = HEADER.getBytes(CharsetUtil.UTF_8);

    @Override
    public void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        Gson gson = new Gson();
        String json = gson.toJson(msg);
        byte[] jsonBytes = json.getBytes(CharsetUtil.UTF_8);
        ByteBuf buf = ctx.alloc().buffer();
        buf.writeBytes(HEADER_BYTES);
        buf.writeInt(jsonBytes.length);
        buf.writeBytes(jsonBytes);
        out.add(buf);
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Gson gson = new Gson();
        if (in.readableBytes() < HEADER_BYTES.length + 4) {
            return;
        }
        in.markReaderIndex();
        byte[] headerBytes = new byte[HEADER_BYTES.length];
        in.readBytes(headerBytes);
        String header = new String(headerBytes, CharsetUtil.UTF_8);
        if (!HEADER.equals(header)) {
            in.resetReaderIndex();
            return;
        }
        int length = in.readInt();
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }
        byte[] jsonBytes = new byte[length];
        in.readBytes(jsonBytes);
        String json = new String(jsonBytes, CharsetUtil.UTF_8);
        Object obj = gson.fromJson(json, Object.class);
        out.add(obj);
    }
}