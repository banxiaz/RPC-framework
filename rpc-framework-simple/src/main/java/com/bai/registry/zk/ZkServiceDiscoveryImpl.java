package com.bai.registry.zk;

import com.bai.enums.RpcErrorMessageEnum;
import com.bai.exception.RpcException;
import com.bai.loadbalance.loadbalancer.RandomLoadBalance;
import com.bai.registry.ServiceDiscovery;
import com.bai.registry.zk.util.CuratorUtils;
import com.bai.remoting.dto.RpcRequest;
import com.bai.loadbalance.LoadBalance;
import com.bai.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    private final LoadBalance loadBalance;

    public ZkServiceDiscoveryImpl() {

        this.loadBalance = new RandomLoadBalance();
    }

    public ZkServiceDiscoveryImpl(LoadBalance loadBalance) {
        this.loadBalance = loadBalance;
    }

    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        log.info("开始进行服务发现...");
        String rpcServiceName = rpcRequest.getRpcServiceName(); // com.bai.HelloServicetest2version2
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
        log.info("需要进行服务发现的rpcServiceName是[{}]", rpcServiceName);
        log.info("并且得到的serviceUrlList是[{}]", serviceUrlList);
        if (CollectionUtil.isEmpty(serviceUrlList)) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        //load balance
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("通过负载均衡后，成功发现服务地址[{}]", targetServiceUrl);
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        log.info("这个服务的IP和port为[{}]:[{}]", host, port);
        return new InetSocketAddress(host, port);
    }
}
