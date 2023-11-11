package org.tianfan.example;

import com.google.gson.Gson;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpServer {
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     p.addLast(new HttpServerCodec());
                     p.addLast(new HttpObjectAggregator(65536));
                     p.addLast(new ChannelInboundHandlerAdapter() {
                         @Override
                         public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                             if (msg instanceof FullHttpRequest) {
                                 FullHttpRequest req = (FullHttpRequest) msg;
                                 if (req.method() == HttpMethod.GET) {
                                     handleGetRequest(ctx, req);
                                 } else if (req.method() == HttpMethod.POST) {
                                     handlePostRequest(ctx, req);
                                 } else {
                                     sendErrorResponse(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
                                 }
                             } else {
                                 super.channelRead(ctx, msg);
                             }
                         }

                         @Override
                         public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                             cause.printStackTrace();
                             ctx.close();
                         }
                     });
                 }
             });

            b.bind(PORT).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static void handleGetRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        // Handle GET request
        sendResponse(ctx, HttpResponseStatus.OK, "Hello, world!");
    }

    private static void handlePostRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {

        Map<String, Object> requestData = new HashMap<>();

        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(req);
        List<InterfaceHttpData> bodyHttpDatas = decoder.getBodyHttpDatas();
        for (InterfaceHttpData data : bodyHttpDatas) {
            if (data instanceof Attribute) {
                Attribute attribute = (Attribute) data;

                requestData.put(attribute.getName(), attribute.getValue());
            } else if (data instanceof FileUpload) {
                FileUpload fileUpload = (FileUpload) data;
                System.out.println("文件名：" + fileUpload.getName());
            }
        }

        sendResponse(ctx, HttpResponseStatus.OK, requestData);
    }

    private static void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, Object data) throws Exception {
        Gson gson=new Gson();
        String json = gson.toJson(data);
        char[] charArray = json.toCharArray();
        ByteBuf content = Unpooled.copiedBuffer(charArray, CharsetUtil.UTF_8);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        ctx.writeAndFlush(response);
    }

    private static void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        ctx.writeAndFlush(response);
    }
}