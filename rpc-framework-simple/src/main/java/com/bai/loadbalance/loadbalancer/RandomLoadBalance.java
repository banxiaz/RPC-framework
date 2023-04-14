package com.bai.loadbalance.loadbalancer;

import com.bai.loadbalance.AbstractLoadBalance;
import com.bai.remoting.dto.RpcRequest;

import java.util.List;
import java.util.Random;

public class RandomLoadBalance extends AbstractLoadBalance {
    @Override
    protected String doSelect(List<String> serviceUrlAddresses, RpcRequest rpcRequest) {
        Random random = new Random();
        return serviceUrlAddresses.get(random.nextInt(serviceUrlAddresses.size()));
    }
}
