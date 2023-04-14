package com.bai.serviceimpl;

import com.bai.HelloEntity;
import com.bai.HelloService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HelloServiceImpl implements HelloService {
    static {
        log.info("HelloServiceImpl实现了HelloService接口，现在被实例化...");
    }

    @Override
    public String hello(HelloEntity hello) {
        log.info("HelloServiceImpl收到[{}]", hello.getMessage() + " " + hello.getDescription());
        String result = "Hello description is " + hello.getDescription();
        log.info("HelloServiceImpl返回: {}.", result);
        return result;
    }
}
