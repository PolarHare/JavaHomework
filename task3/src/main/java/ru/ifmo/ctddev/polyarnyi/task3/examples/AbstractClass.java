package ru.ifmo.ctddev.polyarnyi.task3.examples;

/**
 * Date: 08.03.14 at 13:31
 *
 * @author Nickolay Polyarniy aka PolarNick
 */
public abstract class AbstractClass {

    public AbstractClass(int value) {

    }

    public abstract int getInt();
    abstract Object getA();

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
