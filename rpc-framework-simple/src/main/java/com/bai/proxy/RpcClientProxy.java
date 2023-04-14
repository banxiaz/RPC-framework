package com.bai.proxy;

import com.bai.config.RpcServiceConfig;
import com.bai.enums.RpcErrorMessageEnum;
import com.bai.enums.RpcResponseCodeEnum;
import com.bai.exception.RpcException;
import com.bai.remoting.dto.RpcRequest;
import com.bai.remoting.dto.RpcResponse;
import com.bai.remoting.transport.RpcRequestTransport;
import com.bai.remoting.transport.netty.client.NettyRpcClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 服务器接收客户端消息，根据客户端消息调用相应的方法，然后将结果返回给客户端。
 * 使用到了反射
 */
@Slf4j
public class RpcClientProxy implements InvocationHandler {
    private static final String INTERFACE_NAME = "interfaceName";

    //用于向服务器发送请求。有两种实现：socket and netty
    private final RpcRequestTransport rpcRequestTransport; // NettyRpcClient
    private final RpcServiceConfig rpcServiceConfig;

    public RpcClientProxy(RpcRequestTransport rpcRequestTransport) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.rpcServiceConfig = new RpcServiceConfig();
    }

    public RpcClientProxy(RpcRequestTransport rpcRequestTransport, RpcServiceConfig rpcServiceConfig) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.rpcServiceConfig = rpcServiceConfig;
    }

    /**
     * newProxyInstance方法一共有三个参数
     * loader :类加载器，用于加载代理对象。
     * interfaces : 被代理类实现的一些接口；
     * h : 实现了 InvocationHandler 接口的对象；
     *
     * @param clazz 被代理类实现的一些接口
     * @param <T>
     * @return 代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }

    /**
     * 要实现动态代理的话，还必须需要实现InvocationHandler 来自定义处理逻辑，当使用代理对象调用方法时，实际上会调用此方法。
     * 但是代理对象是通过getProxy方法获得的对象。
     *
     * @param proxy  动态生成的代理类对象
     * @param method 与代理类对象调用的方法相对应
     * @param args   当前 method 方法的参数
     * @return
     * @throws Throwable
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("进入代理类的invoke()方法，这个代理对象是[{}]", proxy.getClass().getName());
        log.info("这一次调用的方法是 method: [{}]", method.getName()); // method: [hello]

        // RpcRequest(requestId=e1c7a92a-2154-43a9-8dfd-bedec8e361f9,
        // interfaceName=com.bai.HelloService,
        // methodName=hello,
        // parameters=[HelloEntity(message=111, description=222)],
        // paramTypes=[class com.bai.HelloEntity],
        // version=version2,
        // group=test2)
        RpcRequest rpcRequest = RpcRequest.builder()
                .methodName(method.getName())
                .parameters(args)
                .interfaceName(method.getDeclaringClass().getName())
                .paramTypes(method.getParameterTypes())
                .requestId(UUID.randomUUID().toString())
                .group(rpcServiceConfig.getGroup())
                .version(rpcServiceConfig.getVersion())
                .build();
        log.info("构建好了rpcRequest [{}]", rpcRequest);

        RpcResponse<Object> rpcResponse = null;
        if (rpcRequestTransport instanceof NettyRpcClient) {
            CompletableFuture<RpcResponse<Object>> completableFuture = (CompletableFuture<RpcResponse<Object>>) rpcRequestTransport.sendRpcRequest(rpcRequest);
            rpcResponse = completableFuture.get();
        }
        this.check(rpcResponse, rpcRequest);
        log.info("动态代理完成，结果也已经收到了，现在返回...");
        return rpcResponse.getData();

    }

    private void check(RpcResponse<Object> rpcResponse, RpcRequest rpcRequest) {
        if (rpcResponse == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())) {
            throw new RpcException(RpcErrorMessageEnum.REQUEST_NOT_MATCH_RESPONSE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (rpcResponse.getCode() == null || !rpcResponse.getCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
    }
}
