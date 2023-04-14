package com.bai.config;

import com.bai.registry.zk.util.CuratorUtils;
import com.bai.remoting.transport.netty.server.NettyRpcServer;
import com.bai.utils.concurrent.threadpool.ThreadPoolFactoryUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * When the server  is closed, do something such as unregister all services
 */
@Slf4j
public class CustomShutdownHook {
    private static final CustomShutdownHook CUSTOM_SHUTDOWN_HOOK = new CustomShutdownHook();

    public static CustomShutdownHook getCustomShutdownHook() {
        return CUSTOM_SHUTDOWN_HOOK;
    }

    public void clearAll() {
        log.info("添加 addShutdownHook for clearAll");
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //TODO
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost(), NettyRpcServer.PORT);
                    CuratorUtils.clearRegistry(CuratorUtils.getZkClient(), inetSocketAddress);
                } catch (UnknownHostException ignored) {
                    ignored.printStackTrace();
                }
                ThreadPoolFactoryUtil.shutDownAllThreadPool();
            }
        }));
    }
}
