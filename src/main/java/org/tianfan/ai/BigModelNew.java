//package org.tianfan.ai;
//
//import com.alibaba.fastjson2.JSONObject;
//import com.google.gson.Gson;
//import io.netty.bootstrap.Bootstrap;
//import io.netty.channel.Channel;
//import io.netty.channel.ChannelFuture;
//
//import io.netty.channel.ChannelInitializer;
//import io.netty.channel.ChannelOption;
//
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.nio.NioSocketChannel;
//
//import io.netty.handler.codec.http.HttpClientCodec;
//import io.netty.handler.codec.http.HttpObjectAggregator;
//
//import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
//import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
//
//import io.netty.handler.codec.http.websocketx.WebSocketVersion;
//import io.netty.handler.ssl.SslContext;
//import io.netty.handler.ssl.SslContextBuilder;
//import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
//import okhttp3.HttpUrl;
//
//import javax.crypto.Mac;
//import javax.crypto.spec.SecretKeySpec;
//import java.net.URI;
//import java.net.URL;
//import java.nio.charset.StandardCharsets;
//import java.text.SimpleDateFormat;
//import java.util.*;
//
//public class BigModelNew {
//    // 地址与鉴权信息 https://spark-api.xf-yun.com/v1.1/chat 1.5地址 domain参数为general
//    // 地址与鉴权信息 https://spark-api.xf-yun.com/v2.1/chat 2.0地址 domain参数为generalv2
//    public static final String hostUrl = "wss://spark-api.xf-yun.com/v2.1/chat";
//    public static final String appid = "";
//    public static final String apiSecret = "";
//    public static final String apiKey = "";
//    public static List<RoleContent> historyList = new ArrayList<>(); // 对话历史存储集合
//    public static String totalAnswer = ""; // 大模型的答案汇总
//    // 环境治理的重要性 环保 人口老龄化 我爱我的祖国
//    public static String NewQuestion = "";
//    public static final Gson gson = new Gson(); // 个性化参数
//    private static Boolean totalFlag = true; // 控制提示用户是否输入
//
//    // 个性化参数
//    private String userId;
//    private Boolean wsCloseFlag;
//
//    // 构造函数
//    public BigModelNew(String userId, Boolean wsCloseFlag) {
//        this.userId = userId;
//        this.wsCloseFlag = wsCloseFlag;
//    }
//
//    // 主函数
//    public static void main(String[] args) throws Exception {
//        // 个性化参数入口，如果是并发使用，可以在这里模拟
//        while (true) {
//            if (totalFlag) {
//                Scanner scanner = new Scanner(System.in);
//                System.out.print("我：");
//                totalFlag = false;
//                NewQuestion = scanner.nextLine();
//                // 构建鉴权url
//                String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);
//                URI uri = new URI(authUrl);
//                SslContext sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
//                WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null, true, null);
//                NioEventLoopGroup group = new NioEventLoopGroup();
//                try {
//                    Bootstrap b = new Bootstrap();
//                    b.group(group)
//                            .channel(NioSocketChannel.class)
//                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
//                            .handler(new ChannelInitializer<Channel>() {
//                                @Override
//                                protected void initChannel(Channel ch) throws Exception {
//                                    ch.pipeline().addLast(
//                                            new HttpClientCodec(),
//                                            new HttpObjectAggregator(8192),
//                                            new WebSocketClientHandler(handshaker)
//                                    );
//                                }
//                            });
//                    ChannelFuture future = b.connect(uri.getHost(), uri.getPort()).sync();
//                    WebSocketClientHandler handler = (WebSocketClientHandler) future.channel().pipeline().last();
//                    handler.handshakeFuture().sync();
//                    JSONObject requestJson = new JSONObject();
//                    JSONObject header = new JSONObject();
//                    // header参数
//                    header.put("app_id", appid);
//                    header.put("uid", UUID.randomUUID().toString().substring(0, 10));
//                    JSONObject parameter = new JSONObject();
//                    // parameter参数
//                    JSONObject chat = new JSONObject();
//                    chat.put("domain", "generalv2");
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
//        URL url = new URL(hostUrl);
//        // 时间
//        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
//        format.setTimeZone(TimeZone.getTimeZone("GMT"));
//        String date = format.format(new Date());
//        // 拼接
//        String preStr = "host: " + url.getHost() + "\n" +
//                "date: " + date + "\n" +
//                "GET " + url.getPath() + " HTTP/1.1";
//        // System.err.println(preStr);
//        // SHA256加密
//        Mac mac = Mac.getInstance("hmacsha256");
//        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
//        mac.init(spec);
//
//        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
//        // Base64加密
//        String sha = Base64.getEncoder().encodeToString(hexDigits);
//        // System.err.println(sha);
//        // 拼接
//        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
//        // 拼接地址
//        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath())).newBuilder().//
//                addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8))).//
//                addQueryParameter("date", date).//
//                addQueryParameter("host", url.getHost()).//
//                build();
//
//        // System.err.println(httpUrl.toString());
//        return httpUrl.toString();
//    }
//    //返回的json结果拆解
//    class JsonParse {
//        Header header;
//        Payload payload;
//    }
//
//    class Header {
//        int code;
//        int status;
//        String sid;
//    }
//
//    class Payload {
//        Choices choices;
//    }
//
//    class Choices {
//        List<Text> text;
//    }
//
//    class Text {
//        String role;
//        String content;
//    }
//    class RoleContent{
//        String role;
//        String content;
//
//        public String getRole() {
//            return role;
//        }
//
//        public void setRole(String role) {
//            this.role = role;
//        }
//
//        public String getContent() {
//            return content;
//        }
//
//        public void setContent(String content) {
//            this.content = content;
//        }
//    }
//}
