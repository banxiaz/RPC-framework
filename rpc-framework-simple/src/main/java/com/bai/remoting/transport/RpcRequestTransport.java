package com.bai.remoting.transport;

import com.bai.remoting.dto.RpcRequest;

public interface RpcRequestTransport {
    /**
     * 发送rpc请求到服务端和得到服务端的结果
     * @param rpcRequest 发送的RpcRequest请求
     * @return 得到的结果
     */
    Object sendRpcRequest(RpcRequest rpcRequest);
}
