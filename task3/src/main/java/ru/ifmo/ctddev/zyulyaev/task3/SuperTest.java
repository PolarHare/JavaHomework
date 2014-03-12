package ru.ifmo.ctddev.zyulyaev.task3;

import java.util.Collection;
import java.util.List;

/**
 * Created by seidhe on 09/03/14.
 */
public abstract class SuperTest<T> {
    protected abstract List<?> test(int a);

    public abstract void test3(T t);
}

class A {}
class B extends A {}

interface I1<E extends Number> {
    public void test(E... es);
    public A test5();
}

interface I2<E extends Collection> {
    public void test(E... es);
    public B test5();
}
