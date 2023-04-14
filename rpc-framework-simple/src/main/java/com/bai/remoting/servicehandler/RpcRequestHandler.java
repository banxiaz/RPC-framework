package com.bai.remoting.servicehandler;

import com.bai.exception.RpcException;
import com.bai.factory.SingletonFactory;
import com.bai.provider.ServiceProvider;
import com.bai.provider.impl.ZkServiceProviderImpl;
import com.bai.remoting.dto.RpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class RpcRequestHandler {
    private final ServiceProvider serviceProvider;

    public RpcRequestHandler() {
        serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }

    public Object handle(RpcRequest rpcRequest) {
        Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
        log.info("service是什么啊...[{}] [{}]", service, service.getClass().getName());
        //service是什么啊...[com.bai.serviceimpl.HelloServiceImpl@35306e66] [com.bai.serviceimpl.HelloServiceImpl]
        return invokeTargetMethod(rpcRequest, service);
    }

    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            log.info("使用反射条件[{}],[{}]获得了方法[{}]", rpcRequest.getMethodName(), rpcRequest.getParamTypes(), method);
            //使用反射条件[hello],[[class com.bai.HelloEntity]]获得了方法[public java.lang.String com.bai.serviceimpl.HelloServiceImpl.hello(com.bai.HelloEntity)]
            result = method.invoke(service, rpcRequest.getParameters());
            log.info("服务[{}]成功通过反射调用了方法[{}]", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
            log.info("获得的结果是[{}]", result);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RpcException(e.getMessage(), e);
        }
        return result;
    }
}
