package com.bai.loadbalance;

import com.bai.remoting.dto.RpcRequest;
import com.bai.utils.CollectionUtil;

import java.util.List;

public abstract class AbstractLoadBalance implements LoadBalance {
    @Override
    public String selectServiceAddress(List<String> serviceUrlAddresses, RpcRequest rpcRequest) {
        if (CollectionUtil.isEmpty(serviceUrlAddresses)) {
            return null;
        }
        if (serviceUrlAddresses.size() == 1) {
            return serviceUrlAddresses.get(0);
        }
        return doSelect(serviceUrlAddresses, rpcRequest);
    }

    protected abstract String doSelect(List<String> serviceUrlAddresses, RpcRequest rpcRequest);
}
