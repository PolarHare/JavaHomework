package ru.ifmo.ctddev.zyulyaev.task3;

import java.util.Collection;
import java.util.List;

/**
 * Created by seidhe on 09/03/14.
 */
public abstract class SuperTest {
    protected abstract List<?> test(int a);
}

interface I1<E extends Number> {
    public void test(E... es);
}

interface I2<E extends Collection> {
    public void test(E... es);
}
