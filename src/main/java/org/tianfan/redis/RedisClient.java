package org.tianfan.redis;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RedisClient {

    String host;    //   目标主机
    int port;       //   目标主机端口

    String context;
    public RedisClient(String host,int port){
        this.host = host;
        this.port = port;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void start() throws Exception{
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new RedisClientInitializer());

            Channel channel = bootstrap.connect(host, port).sync().channel();
            System.out.println(" connected to host : " + host + ", port : " + port);
            System.out.println(" type redis's command to communicate with redis-server or type 'quit' to shutdown ");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            ChannelFuture lastWriteFuture = null;
                System.out.print(">");
                lastWriteFuture = channel.writeAndFlush(context);
                lastWriteFuture.addListener(new GenericFutureListener<ChannelFuture>() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            System.err.print("write failed: ");
                            future.cause().printStackTrace(System.err);
                        }
                    }
                });
            if (lastWriteFuture != null) {
                lastWriteFuture.sync();
            }
            System.out.println(" bye ");
        }finally {
            group.shutdownGracefully();
        }
    }


}
