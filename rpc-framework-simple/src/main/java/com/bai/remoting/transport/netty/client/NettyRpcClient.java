package com.bai.remoting.transport.netty.client;

import com.bai.enums.CompressTypeEnum;
import com.bai.enums.SerializationTypeEnum;
import com.bai.factory.SingletonFactory;
import com.bai.registry.ServiceDiscovery;
import com.bai.registry.zk.ZkServiceDiscoveryImpl;
import com.bai.remoting.constants.RpcConstants;
import com.bai.remoting.dto.RpcMessage;
import com.bai.remoting.dto.RpcRequest;
import com.bai.remoting.dto.RpcResponse;
import com.bai.remoting.transport.RpcRequestTransport;
import com.bai.remoting.transport.netty.codec.RpcMessageDecoder;
import com.bai.remoting.transport.netty.codec.RpcMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class NettyRpcClient implements RpcRequestTransport {
    private final ServiceDiscovery serviceDiscovery;
    private final UnprocessedRequests unprocessedRequests;
    private final ChannelProvider channelProvider;
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;

    public NettyRpcClient() {
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //  连接的超时时间。
                //  如果超过此时间或无法建立连接，则连接失败。
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        // 如果15秒内没有数据发送到服务器，则发送心跳请求
                        p.addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                        p.addLast(new RpcMessageEncoder()); //实现了ChannelHandler接口!上面这个也是
                        p.addLast(new RpcMessageDecoder()); //实现了ChannelHandler接口!
                        p.addLast(new NettyRpcClientHandler()); //实现了ChannelHandler接口!
                    }
                });
        this.serviceDiscovery = new ZkServiceDiscoveryImpl();
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
    }

    /**
     * 连接服务器并获取Channel，以便可以向服务器发送rpc消息
     *
     * @param inetSocketAddress server address
     * @return the channel
     */
    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
                log.info("连接到服务器时发生错误...");
                throw new IllegalStateException();
            }
        });
        return completableFuture.get(); //会阻塞直到complete方法被调用
    }

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest); //获取到要连接的ip+port
        Channel channel = getChannel(inetSocketAddress);
        log.info("获取Channel成功[{}][{}]", channel, channel.getClass().getName());
        if (channel.isActive()) {
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture); // UUID - future
            RpcMessage rpcMessage = RpcMessage.builder()
                    .data(rpcRequest)
                    .codec(SerializationTypeEnum.KYRO.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .messageType(RpcConstants.REQUEST_TYPE)
                    .build();
            channel.writeAndFlush(rpcMessage).addListener(new ChannelFutureListener() { //操作完成回调函数
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        log.info("客户端发送消息成功[{}]", rpcMessage);
                    } else {
                        future.channel().close();
                        resultFuture.completeExceptionally(future.cause());
                        log.error("发送失败...", future.cause());
                    }
                }
            });
        } else {
            throw new IllegalStateException();
        }
        return resultFuture;
    }

    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        if (channel == null) {
            channel = doConnect(inetSocketAddress);
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }

    public void close() {
        eventLoopGroup.shutdownGracefully();
        log.info("服务端EventLoopGroup已经关闭");
    }
}
