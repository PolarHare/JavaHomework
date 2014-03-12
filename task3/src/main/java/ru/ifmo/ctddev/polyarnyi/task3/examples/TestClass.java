package ru.ifmo.ctddev.polyarnyi.task3.examples;

import java.io.Closeable;

/**
 * Date: 12.03.14 at 21:02
 *
 * @author Nickolay Polyarniy aka PolarNick
 */
public abstract class TestClass<T extends Comparable<T> & Closeable> implements I1<T> {

    public abstract T foo123(T t);
    public abstract T[] foo123(T[] t);

}

interface I1<E extends Comparable<E>> extends I2  {

    public abstract E[] foo123(E[] t);
    public abstract E foo123(E t);

}

interface I2<R> {
    public abstract R[] foo123(R[] t);
    public abstract R foo123(R t);

}