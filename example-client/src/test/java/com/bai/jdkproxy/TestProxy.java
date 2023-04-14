package com.bai.jdkproxy;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

class MyInvocationHandler implements InvocationHandler {
    /**
     * @param proxy 动态生成的代理类对象
     * @param method 与代理类对象调用的方法相对应，调用不同的方法会有不同的Method对象
     * @param args 当前method方法的参数
     * @return 原方法需要返回的值
     * @throws Throwable 异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println(proxy.getClass().getName());
        System.out.println(method);
        System.out.println(Arrays.toString(args));
        return null;
    }
}

public class TestProxy {
    @Test
    void test() {
        // 获取代理类对象 com.sun.proxy.$Proxy10
        Hello hello = (Hello) Proxy.newProxyInstance(
                Hello.class.getClassLoader(), //用来加载代理对象的类加载器
                new Class<?>[]{Hello.class}, // 需要被代理实现的一些接口
                new MyInvocationHandler() // 实现了 InvocationHandler 接口的对象，
                // 对接口的方法的调用，全部会转到invoke（）方法上
        );

        System.out.println(hello); // 默认调用了public java.lang.String java.lang.Object.toString()
        System.out.println(1);
        hello.sayHello(123, "123");
        System.out.println(1);
        hello.sayHi("123", 123);


    }
}
