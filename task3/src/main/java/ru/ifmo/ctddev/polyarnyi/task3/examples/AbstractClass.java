package ru.ifmo.ctddev.polyarnyi.task3.examples;

import java.io.UnsupportedEncodingException;
import java.nio.file.AccessDeniedException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Date: 08.03.14 at 13:31
 *
 * @author Nickolay Polyarniy aka PolarNick
 */
public abstract class AbstractClass<E, B extends List<C>, C, D> implements List<E> {

    public AbstractClass(int value, E[] array) throws AccessDeniedException, UnsupportedEncodingException {
    }

    public abstract D getD();

    public <T extends Comparable<Map<Integer[], T>> & Set<T> & Comparator<E>> List<T> getA(T arg1, Set<? super Integer> arg2) {
        return null;
    }

    public abstract <D> D getABC();

    public abstract int getInt(java.awt.List a);

    abstract Object getA();

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
