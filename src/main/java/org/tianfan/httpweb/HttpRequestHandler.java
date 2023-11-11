package org.tianfan.httpweb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import jakarta.activation.MimetypesFileTypeMap;
import org.apache.ibatis.session.SqlSession;
import org.tianfan.httpmysql.MyBatisUtil;
import org.tianfan.httpmysql.MySqlUtils;
import org.tianfan.httpmysql.mapper.UserMapper;
import org.tianfan.httpmysql.pojo.User;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
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
        if (!uri.equals(path)){
            ctx.fireChannelRead(req);
        }

        if (uri.equals(path)&&resMap.get("method").equals(Constant.Get)){
            handleResource(ctx, resMap);
        } else if (uri.equals(path)&&resMap.get("method").equals(Constant.Post)){
            handlePostResource(ctx, resMap,req);
        }else {
            handleNotFound(ctx, resMap);
        }
    }

    private void handlePostResource(ChannelHandlerContext ctx, Map<String, String> resMap, FullHttpRequest req) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();

        Map<String, Object> requestData = new HashMap<>();
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(req);
        List<InterfaceHttpData> bodyHttpDatas = decoder.getBodyHttpDatas();
        for (InterfaceHttpData data : bodyHttpDatas) {
            if (data instanceof Attribute) {
                Attribute attribute = (Attribute) data;

                System.out.println(attribute.getName());
                System.out.println(attribute.getValue());
                requestData.put(attribute.getName(), attribute.getValue());
            } else if (data instanceof FileUpload) {
                FileUpload fileUpload = (FileUpload) data;
                System.out.println("文件名：" + fileUpload.getName());
            }
        }


        // 处理POST请求的内容
        SqlSession sqlSession = MyBatisUtil.getSqlSession();
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        //调用方法
        List<User> users = userMapper.getUsers();
        System.out.println(users);
        for (User user:users){
            if (user.getUsername().equals(requestData.get("username"))&&user.getPassword().equals(requestData.get("password")))
            {
                handleResource(ctx,resMap);
                return;
            }
        }
        resMap.put("uri","/loginFail");
        // 清空StringBuilder
        contentBuilder.setLength(0);
        handleResource(ctx,resMap);
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
            ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            future.addListener(ChannelFutureListener.CLOSE);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            // 记录日志或者重新抛出异常
            throw new RuntimeException("处理文件时发生IOException", e);
        } finally {
            if (raf != null) {
                try {
                    ctx.close();
                    raf.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        Channel channel = ctx.channel();
        if (channel.isActive()) {
            // 记录异常信息
            System.err.println("Exception caught: " + cause.getMessage());
            ctx.close();
        }
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
