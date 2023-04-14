package com.bai.config;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcServiceConfig {
    private String version = ""; // service version
    private String group = ""; // 当接口有多个实现类时，按组区分
    private Object service; // 目标服务

    public String getRpcServiceName() {
        return this.getServiceName() + this.getGroup() + this.getVersion();
    }

    public String getServiceName() {
        // getCanonicalName()获取所传类从java语言规范定义的格式输出
        return this.service.getClass().getInterfaces()[0].getCanonicalName();
    }
}
