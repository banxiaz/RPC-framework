package com.bai;

import com.bai.utils.concurrent.threadpool.ThreadPoolFactoryUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestThreadPoolFactoryUtil {
    @Test
    public void testThreadPoolFactoryUtil() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 5, 10000, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10), new ThreadPoolExecutor.DiscardPolicy());
        for (int i = 0; i < 20; i++) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    System.out.println("hello");
                }
            });
        }

        ThreadPoolFactoryUtil.createCustomThreadPoolIfAbsent("hello");
        ThreadPoolFactoryUtil.printThreadPoolStatus(executor);
    }
}
