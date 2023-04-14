package com.bai.loadbalance;

import com.bai.remoting.dto.RpcRequest;

import java.util.List;

/**
 * 负载均衡策略的接口
 */
public interface LoadBalance {
    /**
     * Choose one from the list of existing service addresses list
     * 从现有服务地址列表中选择一个
     * @param serviceUrlList Service address list
     * @param rpcRequest rpc请求
     * @return target service address
     */
    String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest);
}
