package com.bai.utils;

/**
 * 获取CPU的核心数
 */
public class RuntimeUtil {
    public static int cpus() {
        return Runtime.getRuntime().availableProcessors();
    }
}
