package com.bai.registry.zk.util;

import com.bai.enums.RpcConfigEnum;
import com.bai.utils.PropertiesFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Curator(zookeeper client) utils
 * 查看常用命令 help
 * 创建节点 create /node "描述字符串“
 * 更新节点数据内容 set /node "更新的描述字符串“
 * 获取节点的数据 get /node
 * 查看某个目录下的子节点 ls /
 * 查看节点状态 stat /node
 * 删除节点 delete /node 注意如果你要删除某一个节点，那么这个节点必须无子节点才行
 *
 * HashMap原本为单线程环境设计，允许存储key和value为null的值，当get()时对于不存在的key返回null，好像有歧义，但是我们可以通过containsKey()判断是否存在这个key
 * ConcurrentHashMap为多线程设计，我们获取到null值时无法判断到底是key对应的值是null，还是原本就没有这个key
 * https://blog.csdn.net/cy973071263/article/details/126354336
 */
@Slf4j
public class CuratorUtils {
    private static final int BASE_SLEEP_TIME = 1000; // 重试之间等待的初始时间
    private static final int MAX_RETRIES = 3; //最大重试次数
    public static final String ZK_REGISTER_ROOT_PATH = "/bai-rpc";
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();
    private static CuratorFramework zkClient;
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";

    private CuratorUtils() {
    }

    /**
     * 获取和zookeeper的连接
     *
     * @return zkClient连接
     */
    public static CuratorFramework getZkClient() {
        // check if user has set zk address
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());// null
        String zookeeperAddress = properties != null && properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null ?
                properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) : DEFAULT_ZOOKEEPER_ADDRESS; //properties为null 用到了默认的地址
        log.info("获取到zookeeperAddress [{}]", zookeeperAddress);
        // if zkClient has been started, return directly
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            log.info("获取已有的zkClient成功");
            return zkClient;
        }
        // Retry strategy. Retry 3 times, and will increase the sleep time between retries.
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        zkClient = CuratorFrameworkFactory.builder().
                connectString(zookeeperAddress).
                retryPolicy(retryPolicy).
                build();
        zkClient.start();
        try {
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Time out waiting to connect to ZK!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("获取新建的zkClient成功");
        return zkClient;
    }

    /**
     * 创建永久节点。与临时节点不同，客户端断开连接时不会删除永久节点
     * 持久（PERSISTENT）节点
     * 临时（EPHEMERAL）节点
     * 持久顺序（PERSISTENT_SEQUENTIAL）节点
     * 临时顺序（EPHEMERAL_SEQUENTIAL）节点
     *
     * @param path node path
     */
    public static void createPersistentNode(CuratorFramework zkClient, String path) { //path->/bai-rpc/com.bai.HelloServicetest2version2/10.162.32.77:9998
        try {
            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                log.info("当前节点已经存在，这个节点是:[{}]", path);
            } else {
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("节点创建成功，这个节点是[{}]", path); //节点创建成功，这个节点是[/bai-rpc/com.bai.HelloServicetest2version2/10.162.32.77:9998]
            }
            REGISTERED_PATH_SET.add(path);
        } catch (Exception e) {
            log.error("创建永久节点失败，这个节点是[{}]", path);
        }
    }

    /**
     * Gets the children under a node
     *
     * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version1
     * @return All child nodes under the specified node
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) { //com.bai.HelloServicetest2version2
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }
        List<String> result = null;
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName; ///bai-rpc/com.bai.HelloServicetest2version2
        try {
            result = zkClient.getChildren().forPath(servicePath); //10.162.32.77:9998 10.162.32.138:9998
            SERVICE_ADDRESS_MAP.put(rpcServiceName, result); //com.bai.HelloServicetest2version2 -> {ArrayList@1834}  size = 2
            registerWatcher(rpcServiceName, zkClient);
        } catch (Exception e) {
            log.error("从路径[{}]获取子节点失败", servicePath);
        }
        return result;
    }

    /**
     * Empty the registry of data
     */
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {
            try {
                if (p.endsWith(inetSocketAddress.toString())) {
                    zkClient.delete().forPath(p);
                }
            } catch (Exception e) {
                log.error("清除路径[{}]上的注册失败", p);
            }
        });
        log.info("服务器上的所有注册服务均被清除[{}]", REGISTERED_PATH_SET.toString());

    }

    /**
     * 注册以监听指定节点的更改
     */
    private static void registerWatcher(String rpcServiceName, CuratorFramework zkClient) throws Exception {
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);
        PathChildrenCacheListener pathChildrenCacheListener = ((curatorFramework, pathChildrenCacheEvent) -> {
            List<String> serviceAddress = curatorFramework.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddress);
        });
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        pathChildrenCache.start();
        log.info("注册监听器成功！");
    }
}
