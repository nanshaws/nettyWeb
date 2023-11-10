package org.tianfan.httpweb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import jakarta.activation.MimetypesFileTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpUtil.is100ContinueExpected;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  private   String path=null;

  private   String method=null;

    public HttpRequestHandler(String path,String method){
        this.path=path;
        this.method=method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setpath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        //100 Continue
        if (is100ContinueExpected(req)) {
            ctx.write(new DefaultFullHttpResponse(
                           HttpVersion.HTTP_1_1,
                           HttpResponseStatus.CONTINUE));
        }
		// 获取请求的uri
        String uri = req.uri();
        Map<String,String> resMap = new HashMap<>();
        resMap.put("method",req.method().name());
        resMap.put("uri",uri);
        if (uri.equals(path)&&resMap.get("method").equals(method)){
            handleResource(ctx, resMap);
        }else {
            handleNotFound(ctx, resMap);
        }
    }

    private void handleNotFound(ChannelHandlerContext ctx, Map<String, String> resMap) {

        ByteBuf content = Unpooled.copiedBuffer("URL not found", CharsetUtil.UTF_8);
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, content);
        ChannelFuture future = ctx.writeAndFlush(response);
        future.addListener(ChannelFutureListener.CLOSE);
    }

    private void handleResource(ChannelHandlerContext ctx, Map<String, String> resMap) {
        String[] strings = resMap.get("uri").split("/");
        String url = this.getClass().getClassLoader().getResource("").getPath()+strings[1]+".html";
        File file = new File(url);
        if (!file.exists()) {
            handleNotFound(ctx, resMap);
            return;
        }
        if (file.isDirectory()) {
            handleDirectory(ctx, resMap, file);
            return;
        }
        handleFile(ctx, resMap, file);

    }

    private void handleFile(ChannelHandlerContext ctx, Map<String, String> resMap, File file) {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            HttpHeaders headers = getContentTypeHeader(file);
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, headers);
            ctx.write(response);
            ctx.write(new DefaultFileRegion(raf.getChannel(), 0, raf.length()));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        future.addListener(ChannelFutureListener.CLOSE);

    }

    private HttpHeaders getContentTypeHeader(File file) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        HttpHeaders headers = new DefaultHttpHeaders();
        String contentType = mimeTypesMap.getContentType(file);
        if (contentType.equals("text/plain")) {
            //由于文本在浏览器中会显示乱码，此处指定为utf-8编码
            contentType = "text/plain;charset=utf-8";
        }
        headers.set(HttpHeaderNames.CONTENT_TYPE, contentType);
        return headers;
    }


    private void handleDirectory(ChannelHandlerContext ctx, Map<String, String> resMap, File file) {
        StringBuilder sb = new StringBuilder();
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isHidden() || !f.canRead()) {
                    continue;
                }
                String name = f.getName();
                sb.append(name).append("<br/>");
            }
        }
        ByteBuf buffer = ctx.alloc().buffer(sb.length());
        buffer.writeCharSequence(sb.toString(), CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        ChannelFuture future = ctx.writeAndFlush(response);
        future.addListener(ChannelFutureListener.CLOSE);

    }
}
