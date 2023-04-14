package com.bai.registry;

import com.bai.remoting.dto.RpcRequest;

import java.net.InetSocketAddress;

public interface ServiceDiscovery {
    /**
     * lookup service by rpcServiceName
     *
     * @param rpcRequest rpc service pojo
     * @return service address
     */
    //TODO
    InetSocketAddress lookupService(RpcRequest rpcRequest);
}
