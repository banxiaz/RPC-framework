package com.bai.registry.zk;

import com.bai.registry.ServiceRegistry;
import com.bai.registry.zk.util.CuratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;

@Slf4j
public class ZkServiceRegistryImpl implements ServiceRegistry {
    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {// com.bai.HelloServicetest2version2 , /10.162.32.77:9998
        log.info("开始进行服务注册...");
        log.info("需要注册的服务名字为[{}]，它的InetSocketAddress地址为[{}}", rpcServiceName, inetSocketAddress);
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString(); // /bai-rpc/com.bai.HelloServicetest2version2/10.162.32.77:9998
        CuratorFramework zkClient = CuratorUtils.getZkClient(); // OK!
        log.info("将要注册到zookeeper中的路径为[{}]", servicePath); // 将要注册到zookeeper中的路径为[/bai-rpc/com.bai.HelloServicetest2version2/10.162.32.77:9998]
        CuratorUtils.createPersistentNode(zkClient, servicePath);
        log.info("服务注册完成...");
    }
}
