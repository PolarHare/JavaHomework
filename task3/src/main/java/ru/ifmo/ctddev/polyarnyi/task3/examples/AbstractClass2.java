package ru.ifmo.ctddev.polyarnyi.task3.examples;

import java.util.List;

/**
 * Date: 08.03.14 at 20:48
 *
 * @author Nickolay Polyarniy aka PolarNick
 */
public abstract class AbstractClass2<E extends List<E>> extends AbstractClass<E, E, E, Integer> {

    private AbstractClass2() {
        super(0, null);
    }

    protected AbstractClass2(E[] a) {
        super(239, a);
    }

    public abstract E getSuperValue();

}
