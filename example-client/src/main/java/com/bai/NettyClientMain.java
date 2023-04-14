package com.bai;

import com.bai.config.RpcServiceConfig;
import com.bai.proxy.RpcClientProxy;
import com.bai.remoting.transport.RpcRequestTransport;
import com.bai.remoting.transport.netty.client.NettyRpcClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyClientMain {
    public static void main(String[] args) {
        String group = "test2";
        String version = "version2";
        RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                .group(group)
                .version(version)
                .build();

        RpcRequestTransport rpcClient = new NettyRpcClient();
        RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient, rpcServiceConfig); //涉及到生成动态代理类
        HelloService proxy = rpcClientProxy.getProxy(HelloService.class); //获取到动态代理对象proxy

        log.info("开始进行RPC调用...");
        //其实是使用代理对象调用了接口的方法，被转到invoke()方法上了
        String apple = proxy.hello(new HelloEntity("apple", "That means I need an apple"));
        log.info("RPC调用完成，获取到的结果是[{}]", apple);

    }
}

// 调用顺序为 NettyClientMain RpcClientProxy NettyRpcClient(建立连接 + 收发数据)
// RpcMessageEncoder NettyRpcClientHandler(心跳机制) RpcMessageDecoder

// 客户端         服务端
// encode
//               decode
//               serverHandler
//               encode
// decode
// clientHandler
