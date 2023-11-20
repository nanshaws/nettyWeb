package org.tianfan.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import okhttp3.*;
import org.tianfan.websocket.message.JsonParse;
import org.tianfan.websocket.message.RoleContent;
import org.tianfan.websocket.message.Text;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class WebSocketServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    public static final String hostUrl = "ws(s)://spark-api.xf-yun.com/v3.1/chat";
    public static final String appid = "fa5ba61a";
    public static final String apiSecret = "NzcwM2E2NWI2YzNiZDJmYzVhODU4ZTNi";
    public static final String apiKey = "8ea25fa8faa44c410f5f148ec8124c70";
    public static String totalAnswer=""; // 大模型的答案汇总
    public static List<RoleContent> historyList=new ArrayList<>(); // 对话历史存储集合
    public static  String NewQuestion = "";
    private Boolean wsCloseFlag=false;
    public static final Gson gson = new Gson();

    @Override
    public void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {

        String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);
        OkHttpClient client = new OkHttpClient.Builder().build();
        String url = authUrl.toString().replace("http://", "ws://").replace("https://", "wss://");
        Request request = new Request.Builder().url(url).build();
            totalAnswer="";
        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                // 连接成功后的操作
                super.onOpen(webSocket, response);
                System.out.print("大模型：");
                MyThread myThread = new MyThread(webSocket);
                myThread.start();
                System.out.println("WebSocket connected");
            }

            public static boolean canAddHistory(){  // 由于历史记录最大上线1.2W左右，需要判断是能能加入历史
                int history_length=0;
                for(RoleContent temp:historyList){
                    history_length=history_length+temp.content.length();
                }
                if(history_length>12000){
                    historyList.remove(0);
                    historyList.remove(1);
                    historyList.remove(2);
                    historyList.remove(3);
                    historyList.remove(4);
                    return false;
                }else{
                    return true;
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                // System.out.println(userId + "用来区分那个用户的结果" + text);
                JsonParse myJsonParse = gson.fromJson(text, JsonParse.class);
                if (myJsonParse.header.code != 0) {
                    System.out.println("发生错误，错误码为：" + myJsonParse.header.code);
                    System.out.println("本次请求的sid为：" + myJsonParse.header.sid);
                    webSocket.close(1000, "");
                }
                List<Text> textList = myJsonParse.payload.choices.text;
                for (Text temp : textList) {
                    System.out.print(temp.content);
                    totalAnswer=totalAnswer+temp.content;
                }
                if (myJsonParse.header.status == 2) {
                    // 可以关闭连接，释放资源
                    System.out.println();
                    System.out.println("*************************************************************************************");
                    if(canAddHistory()){
                        RoleContent roleContent=new RoleContent();
                        roleContent.setRole("assistant");
                        roleContent.setContent(totalAnswer);
                        historyList.add(roleContent);
                    }else{
                        historyList.remove(0);
                        RoleContent roleContent=new RoleContent();
                        roleContent.setRole("assistant");
                        roleContent.setContent(totalAnswer);
                        historyList.add(roleContent);
                    }
                    wsCloseFlag = true;
                }
                // 接收到消息后的操作
                System.out.println("Received message: " + text);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                // 连接关闭后的操作
                System.out.println("WebSocket closed");
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                try {
                    if (null != response) {
                        int code = response.code();
                        System.out.println("onFailure code:" + code);
                        System.out.println("onFailure body:" + response.body().string());
                        if (101 != code) {
                            System.out.println("connection failed");
                            System.exit(0);
                        }
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                // 连接失败后的操作
                System.out.println("WebSocket connection failed"+t);
            }
        };

        WebSocket webSocket = client.newWebSocket(request, listener);

        // 处理消息
        System.out.println("Received message: " + msg.text());

        ctx.channel().writeAndFlush(new TextWebSocketFrame("Server received: " + msg.text()));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 添加连接
        System.out.println("Client connected: " + ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 断开连接
        System.out.println("Client disconnected: " + ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 异常处理
        cause.printStackTrace();
        ctx.close();
    }


    // 鉴权方法
    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        // 时间
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        // 拼接
        String preStr = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + url.getPath() + " HTTP/1.1";
        // System.err.println(preStr);
        // SHA256加密
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);

        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
        // Base64加密
        String sha = Base64.getEncoder().encodeToString(hexDigits);
        // System.err.println(sha);
        // 拼接
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        // 拼接地址
        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath())).newBuilder().//
                addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8))).//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();

        // System.err.println(httpUrl.toString());
        return httpUrl.toString();
    }

    class MyThread extends Thread {
        private WebSocket webSocket;

        public MyThread(WebSocket webSocket) {
            this.webSocket = webSocket;
        }

        public void run() {
            try {
                JSONObject requestJson=new JSONObject();

                JSONObject header=new JSONObject();  // header参数
                header.put("app_id",appid);
                header.put("uid",UUID.randomUUID().toString().substring(0, 10));

                JSONObject parameter=new JSONObject(); // parameter参数
                JSONObject chat=new JSONObject();
                chat.put("domain","generalv2");
                chat.put("temperature",0.5);
                chat.put("max_tokens",4096);
                parameter.put("chat",chat);

                JSONObject payload=new JSONObject(); // payload参数
                JSONObject message=new JSONObject();
                JSONArray text=new JSONArray();

                // 历史问题获取
                if(historyList.size()>0){
                    for(RoleContent tempRoleContent:historyList){
                        text.add(JSON.toJSON(tempRoleContent));
                    }
                }

                // 最新问题
                RoleContent roleContent=new RoleContent();
                roleContent.role="user";
                roleContent.content=NewQuestion;
                text.add(JSON.toJSON(roleContent));
                historyList.add(roleContent);


                message.put("text",text);
                payload.put("message",message);


                requestJson.put("header",header);
                requestJson.put("parameter",parameter);
                requestJson.put("payload",payload);
                // System.err.println(requestJson); // 可以打印看每次的传参明细
                webSocket.send(requestJson.toString());
                // 等待服务端返回完毕后关闭
                while (true) {
                    // System.err.println(wsCloseFlag + "---");
                    Thread.sleep(200);
                    if (wsCloseFlag) {
                        break;
                    }
                }
                webSocket.close(1000, "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}