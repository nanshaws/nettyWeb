package org.tianfan.xintiao;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

public class MyServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * @param ctx 上下文
     * @param evt 事件
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            // 将 event 向下转型 IdleStateEvent
            IdleStateEvent event = (IdleStateEvent) evt;
            String eventType = null;
            switch (event.state()) {
                case READER_IDLE:
                    eventType = "读空闲";
                    System.out.println("读空闲");
                    break;
                case WRITER_IDLE:
                    eventType = "写空闲";
                    System.out.println("写空闲");
                    break;
                case ALL_IDLE:
                    eventType = "读写空闲";
                    System.out.println("读写空闲");
                    break;
            }
            System.out.println(ctx.channel().remoteAddress() + "--超时事件--" + eventType);
            System.out.println("服务器做相应的处理..");

            // 如果发生空闲，我们关闭通道
            // ctx.channel().close();

            // 给客户端发送消息
            ctx.writeAndFlush("您已经超时了！");
        }
    }
}
