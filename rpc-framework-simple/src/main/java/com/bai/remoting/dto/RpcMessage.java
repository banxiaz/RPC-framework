package com.bai.remoting.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcMessage {
    private byte messageType; //rpc的信息类型，请求还是响应
    private byte codec; // 序列化类似
    private byte compress; // 压缩类型
    private int requestId; // 请求ID
    private Object data; // 携带的数据 请求还是响应
}
