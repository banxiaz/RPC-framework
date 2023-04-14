package com.bai;

import com.bai.config.RpcServiceConfig;
import com.bai.remoting.transport.netty.server.NettyRpcServer;
import com.bai.serviceimpl.HelloServiceImpl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyServerMain {
    public static void main(String[] args) {
        // 获取nettyRpcServer服务器对象
        NettyRpcServer nettyRpcServer = new NettyRpcServer();
        // 获取HelloService的一个实现类
        HelloService helloService = new HelloServiceImpl();
        // 创建rpcServiceConfig对象
        RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                .group("test2")
                .version("version2")
                .service(helloService)
                .build(); //RpcServiceConfig(version=version2, group=test2, service=com.bai.serviceimpl.HelloServiceImpl@34033bd0)
        // 注册服务
        nettyRpcServer.registerService(rpcServiceConfig);

        // 服务器启动，开始监听端口...
        nettyRpcServer.start();
        log.info("调用了 ettyRpcServer.start()");
    }
}

// 调用顺序为 NettyServerMain NettyRpcServer
// RpcMessageDecoder(解码)
// NettyRpcServerHandler(接收) RpcRequestHandler(反射) NettyRpcServerHandler(发送)
// RpcMessageEncoder(编码)