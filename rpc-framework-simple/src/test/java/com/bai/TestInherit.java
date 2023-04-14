package com.bai;

import org.junit.jupiter.api.Test;

class A {
}

class B extends A {
}

public class TestInherit {
    @Test
    public void test() {
        A a1 = new B();
        A a2=new A();
        B a11 = (B) a1;
        System.out.println(a1.getClass().getName());
        System.out.println(a2.getClass().getName());
        System.out.println(a11.getClass().getName());
    }
}
