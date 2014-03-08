package ru.ifmo.ctddev.zyulyaev.task3;

import java.util.Collection;
import java.util.List;

/**
 * Created by seidhe on 07/03/14.
 */
public abstract class Test<E extends Comparable<? super Collection<? extends E>>, EX extends Exception>
        extends SuperTest
        implements I1<Double>, I2<Collection<? extends E>> {
    <T extends E> Test(T t) throws IllegalArgumentException, EX {
    }
    public abstract <T extends E> List<int[][]>[][] test(T t);
    protected abstract List<?> test(int a);
    public abstract List<? extends int[]> test();

    public abstract <E extends Collection<?>> void test2(E e);
    public abstract void test2(E e);
}
