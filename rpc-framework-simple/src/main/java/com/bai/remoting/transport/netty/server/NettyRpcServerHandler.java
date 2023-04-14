package com.bai.remoting.transport.netty.server;

import com.bai.enums.CompressTypeEnum;
import com.bai.enums.RpcResponseCodeEnum;
import com.bai.enums.SerializationTypeEnum;
import com.bai.factory.SingletonFactory;
import com.bai.remoting.constants.RpcConstants;
import com.bai.remoting.dto.RpcMessage;
import com.bai.remoting.dto.RpcRequest;
import com.bai.remoting.dto.RpcResponse;
import com.bai.remoting.servicehandler.RpcRequestHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Customize the ChannelHandler of the server to process the data sent by the client.
 * <p>
 * 如果继承自 SimpleChannelInboundHandler 的话就不要考虑 ByteBuf 的释放 ，{@link SimpleChannelInboundHandler} 内部的
 * channelRead 方法会替你释放 ByteBuf ，避免可能导致的内存泄露问题。详见《Netty进阶之路 跟着案例学 Netty》
 *
 * @author shuang.kou
 * @createTime 2020年05月25日 20:44:00
 * <p>
 * https://blog.csdn.net/bklydxz/article/details/118969307?spm=1001.2014.3001.5502
 */
@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {
    private final RpcRequestHandler rpcRequestHandler;

    public NettyRpcServerHandler() {
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    // 在消息入界的过程中处理消息
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.info("进入了channelRead方法");
        try {
            if (msg instanceof RpcMessage) {
                log.info("服务端收到了消息：[{}]", msg);
                //服务端收到了消息：[RpcMessage(messageType=1,
                // codec=1,
                // compress=0,
                // requestId=0,
                // data=RpcRequest(requestId=8114d010-4548-4c08-8bc0-1c08eee676a6,
                // interfaceName=com.bai.HelloService,
                // methodName=hello,
                // parameters=[HelloEntity(message=111, description=222)],
                // paramTypes=[class com.bai.HelloEntity],
                // version=version2, group=test2))]
                byte messageType = ((RpcMessage) msg).getMessageType();
                RpcMessage rpcMessage = new RpcMessage();
                rpcMessage.setCodec(SerializationTypeEnum.KYRO.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                log.info("为响应数据设置了一些属性 [{}] [{}]", SerializationTypeEnum.KYRO.getCode(), CompressTypeEnum.GZIP.getCode());
                if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                    rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                    rpcMessage.setData(RpcConstants.PONG);
                } else {
                    RpcRequest rpcRequest = (RpcRequest) ((RpcMessage) msg).getData();
                    // 执行目标方法（客户端需要执行的方法）并返回方法结果
                    Object result = rpcRequestHandler.handle(rpcRequest);
                    log.info("服务端得到了结果[{}]", result.toString());
                    rpcMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                        RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                        rpcMessage.setData(rpcResponse);
                    } else {
                        RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                        rpcMessage.setData(rpcResponse);
                        log.error("现在不可写，消息已丢弃...");
                    }
                }
                log.info("服务端现在返回消息[{}]", rpcMessage);
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                log.info("发生空闲检查，因此关闭连接");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("服务器捕获异常");
        cause.printStackTrace();
        ctx.close();
    }
}

/**
 * 在 Netty 中，IO 事件被分为 Inbound 事件和 Outbound 事件。
 * 属于OUT的事件：connect, write, flush
 * 属于IN的事件：accept, read
 * 对于ChannelPipeline添加的顺序详细补充：handler其实是分组的，分为Inbound和Outbound，组内是有顺序的
 * 对于 Inbound 操作，按照添加顺序执行每个 Inbound 类型的 handler；而对于 Outbound 操作，是反着来的，从后往前，顺次执行 Outbound 类型的 handler。
 * 那我们在开发的时候怎么写呢？其实也很简单，从最外层开始写，一步步写到业务处理层，把 Inbound 和 Outbound 混写在一起。比如 encode 和 decode 是属于最外层的处理逻辑，先写它们。假设 decode 以后是字符串，那再进来一层应该可以写进来和出去的日志。再进来一层可以写 字符串 <=> 对象 的相互转换。然后就应该写业务层了。
 * 定义处理 Inbound 事件的 handler 需要实现 ChannelInboundHandler，定义处理 Outbound 事件的 handler 需要实现 ChannelOutboundHandler。
 * <p>
 * ChannelHandlerContext 可以说是 ChannelPipeline 的核心，它代表了 ChannelHandler 和 ChannelPipeline 之间的关联，我们首先要知道一个 ChannelPipeline 内部会维护一个双向链表，
 * 每当一个 ChannelHandler 被添加到 ChannelPipeline 中时，它都会被包装成为一个 ChannelHandlerContext，组成链表的各个节点。
 * childHandler 中指定的 handler 不是给 NioServerSocketChannel 使用的，是给 NioSocketChannel 使用的
 */