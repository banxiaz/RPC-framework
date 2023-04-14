package com.bai.provider.impl;

import com.bai.config.RpcServiceConfig;
import com.bai.enums.RpcErrorMessageEnum;
import com.bai.exception.RpcException;
import com.bai.provider.ServiceProvider;
import com.bai.registry.ServiceRegistry;
import com.bai.registry.zk.ZkServiceRegistryImpl;
import com.bai.remoting.transport.netty.server.NettyRpcServer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ZkServiceProviderImpl implements ServiceProvider {
    /**
     * key: rpc service name(interface name + version + group)
     * value: service object
     */
    private final Map<String, Object> serviceMap;
    private final Set<String> registeredService;
    private final ServiceRegistry serviceRegistry;

    public ZkServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        registeredService = ConcurrentHashMap.newKeySet();
        serviceRegistry = new ZkServiceRegistryImpl();
    }

    @Override
    public void addService(RpcServiceConfig rpcServiceConfig) { //RpcServiceConfig(version=version2, group=test2, service=com.bai.serviceimpl.HelloServiceImpl@34033bd0)
        String rpcServiceName = rpcServiceConfig.getRpcServiceName(); //com.bai.HelloServicetest2version2
        if (registeredService.contains(rpcServiceName)) {
            return;
        }
        registeredService.add(rpcServiceName);// com.bai.HelloServicetest2version2
        serviceMap.put(rpcServiceName, rpcServiceConfig.getService()); //com.bai.HelloServicetest2version2 -> {HelloServiceImpl@560}
        log.info("添加服务：[{}] 和接口：[{}]", rpcServiceName, rpcServiceConfig.getService().getClass().getInterfaces());
    }

    @Override
    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (service == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress(); // 10.162.32.77
            this.addService(rpcServiceConfig); //添加服务到map和set了
            serviceRegistry.registerService(rpcServiceConfig.getRpcServiceName(), new InetSocketAddress(host, NettyRpcServer.PORT));
        } catch (UnknownHostException e) {
            log.error("在获取主机地址时发生了错误", e);
        }
    }
}
