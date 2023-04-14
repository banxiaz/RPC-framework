package com.bai.remoting.transport.netty.client;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储并获取通道对象
 */
@Slf4j
public class ChannelProvider {
    private final Map<String, Channel> channelMap;

    public ChannelProvider() {
        channelMap = new ConcurrentHashMap<>();
    }

    public Channel get(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        if (channelMap.containsKey(key)) {
            Channel channel = channelMap.get(key);
            if (channel != null && channel.isActive()) {
                return channel;
            } else {
                channelMap.remove(key);
            }
        }
        return null;
    }

    public void set(InetSocketAddress inetSocketAddress, Channel channel) {
        String key = inetSocketAddress.toString();
        channelMap.put(key, channel);
    }

    public void remove(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        channelMap.remove(key);
        log.info("Channel map size :[{}]", channelMap.size());
    }
}

/**
 * Channel 接口是 Netty 对网络操作抽象类。通过 Channel 我们可以进行 I/O 操作。
 * 一旦客户端成功连接服务端，就会新建一个 Channel 同该用户端进行绑定
 * 比较常用的Channel接口实现类是 ：NioServerSocketChannel（服务端） NioSocketChannel（客户端）这两个 Channel 可以和 BIO 编程模型中的ServerSocket以及Socket两个概念对应上。
 * 对于 NioSocketChannel，由于它充当客户端的功能，它的创建时机在 connect(…) 的时候；
 * 对于 NioServerSocketChannel 来说，它充当服务端功能，它的创建时机在绑定端口 bind(…) 的时候。
 *
 */