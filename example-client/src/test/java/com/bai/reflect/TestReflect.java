package com.bai.reflect;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

interface A {
    void sayHi(int age, String name);

    void sayHello(String name, int age);
}

class B implements A {

    @Override
    public void sayHi(int age, String name) {
        System.out.println("This is sayHi method");
    }

    @Override
    public void sayHello(String name, int age) {
        System.out.println("This is sayHello method");
    }
}

public class TestReflect {
    @Test
    void test() {
        Class<B> bClass = B.class;
        for (Class<?> anInterface : bClass.getInterfaces()) {
            System.out.println(anInterface.toString());
        }
        System.out.println();
        for (Method method : bClass.getDeclaredMethods()) {
            System.out.println(method.toString());
            System.out.println(method.getName());
        }
        System.out.println();

        B b = new B();
        try {
            Method syaHi = bClass.getMethod("sayHi", int.class, String.class);
            syaHi.invoke(b, 1, "123");
            System.out.println();
            Method sayHello = bClass.getMethod("sayHello", String.class, int.class);
            sayHello.invoke(b, "123", 1);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        System.out.println(int.class);
    }
}
