package com.bai.provider;

import com.bai.config.RpcServiceConfig;

/**
 * 存储并提供服务对象
 */
public interface ServiceProvider {
    /**
     * @param rpcServiceConfig rpc服务相关属性
     */
    void addService(RpcServiceConfig rpcServiceConfig);

    /**
     * @param rpcServiceName rpc服务名
     * @return 服务对象
     */
    Object getService(String rpcServiceName);

    /**
     * @param rpcServiceConfig rpc服务相关属性
     */
    void publishService(RpcServiceConfig rpcServiceConfig);
}
