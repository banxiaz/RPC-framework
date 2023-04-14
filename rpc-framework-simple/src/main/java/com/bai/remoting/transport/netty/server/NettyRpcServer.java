package com.bai.remoting.transport.netty.server;

import com.bai.config.CustomShutdownHook;
import com.bai.config.RpcServiceConfig;
import com.bai.factory.SingletonFactory;
import com.bai.provider.ServiceProvider;
import com.bai.provider.impl.ZkServiceProviderImpl;
import com.bai.remoting.transport.netty.codec.RpcMessageDecoder;
import com.bai.remoting.transport.netty.codec.RpcMessageEncoder;
import com.bai.utils.RuntimeUtil;
import com.bai.utils.concurrent.threadpool.ThreadPoolFactoryUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * 服务器接收客户端消息，根据客户端消息调用相应的方法，然后将结果返回给客户端。
 * 相关链接 https://www.zhihu.com/question/469794520
 * https://blog.csdn.net/bklydxz?type=blog
 */
@Slf4j
public class NettyRpcServer {
    public static final int PORT = 9998;
    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);

    // 注册服务方法
    public void registerService(RpcServiceConfig rpcServiceConfig) {
        serviceProvider.publishService(rpcServiceConfig);
    }

    @SneakyThrows
    public void start() {
        CustomShutdownHook.getCustomShutdownHook().clearAll();
        String host = InetAddress.getLocalHost().getHostAddress(); // 10.162.32.77
        //配置主从Reactor多线程
        //通常只有一个主处理组，用来监听连接accept等事件
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                RuntimeUtil.cpus() * 2,
                ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false)
        );

        try {
            ServerBootstrap b = new ServerBootstrap(); //ServerBootstrap(ServerBootstrapConfig(group: NioEventLoopGroup, childGroup: NioEventLoopGroup))
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)// TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.SO_KEEPALIVE, true)// 是否开启 TCP 底层心跳机制
                    .option(ChannelOption.SO_BACKLOG, 128)//表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {// 当客户端第一次进行请求的时候才会进行初始化
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // 30 秒之内没有收到客户端请求的话就关闭连接
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS)); //心跳机制在这里
                            p.addLast(new RpcMessageEncoder());
                            p.addLast(new RpcMessageDecoder());
                            p.addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                        }
                    });
            ChannelFuture f = b.bind(host, PORT).sync();// 绑定端口，同步等待绑定成功
            f.channel().closeFuture().sync();// 等待服务端监听端口关闭
        } catch (InterruptedException e) {
            log.error("开启服务器时发生了一些错误...", e);
        } finally {
            log.error("准备关闭 bossGroup and workerGroup...");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }

    }
}
