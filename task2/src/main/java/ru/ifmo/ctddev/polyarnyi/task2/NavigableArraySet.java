package ru.ifmo.ctddev.polyarnyi.task2;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.util.*;

/**
 * This is immutable implementation of {@link java.util.NavigableSet} based on array. Mostly all operations of search
 * has an {@code O(log(N))} asymptotic behavior, where {@code N} - is a size of this set.
 *
 * @author Nickolay Polyarniy aka PolarNick
 */
public class NavigableArraySet<E> implements NavigableSet<E> {

    private final List<E> array;
    private final Comparator<? super E> comparator;
    private boolean naturalOrderUsed = false;

    private NavigableArraySet(Comparator<? super E> comparator, List<E> array) {
        this.array = array;
        this.comparator = comparator;
    }

    public static <T extends Comparable<? super T>> NavigableArraySet<T> createWithNaturalOrder(Collection<T> collection) {
        NavigableArraySet<T> result = new NavigableArraySet<>(collection, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return o1.compareTo(o2);
            }
        });
        result.naturalOrderUsed = true;
        return result;
    }

    public NavigableArraySet(Collection<E> collection, Comparator<? super E> comparator) {
        ArrayList<E> all = new ArrayList<>(collection);
        Collections.sort(all, comparator);
        ArrayList<E> onlyDifferents = new ArrayList<>(all.size());
        if (all.size() > 0) {
            onlyDifferents.add(all.get(0));
        }
        for (int i = 1; i < all.size(); i++) {
            if (comparator.compare(all.get(i - 1), all.get(i)) != 0) {
                onlyDifferents.add(all.get(i));
            }
        }
        onlyDifferents.trimToSize();
        this.array = onlyDifferents;
        this.comparator = comparator;
    }

    private static <E> int binarySearch(List<E> list, Comparator<? super E> comparator, E element, boolean inversed) {
        int l = -1;
        int r = list.size();
        while (l < r - 1) {
            int m = (l + r) / 2;
            E middleElement = list.get(inversed ? list.size() - 1 - m : m);
            if (comparator.compare(middleElement, element) * (inversed ? -1 : 1) < 0) {
                l = m;
            } else {
                r = m;
            }
        }
        return inversed ? list.size() - 1 - r : r;
    }

    private int binarySearch(E element, boolean inversed) {
        return binarySearch(array, comparator, element, inversed);
    }

    private E find(E element, boolean inversed) {
        int index = binarySearch(element, inversed);
        if (index == -1 || index == array.size()) {
            return null;
        } else {
            return array.get(index);
        }
    }

    private E findNextAfter(E element, boolean inversed) {
        int index = binarySearch(element, inversed);
        if (index < 0 || index >= array.size()) {
            return null;
        }
        int cornerIndex;
        int toAdd;
        if (inversed) {
            cornerIndex = 0;
            toAdd = -1;
        } else {
            cornerIndex = array.size()-1;
            toAdd = 1;
        }
        if (array.get(index).equals(element)) {
            if (index == cornerIndex) {
                return null;
            }
            index += toAdd;
        }
        return array.get(index);
    }

    @Override
    public E lower(E e) {
        return findNextAfter(e, true);
    }

    @Override
    public E floor(E e) {
        return find(e, true);
    }

    @Override
    public E ceiling(E e) {
        return find(e, false);
    }

    @Override
    public E higher(E e) {
        return findNextAfter(e, false);
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    public boolean isEmpty() {
        return array.size() == 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return find((E) o, false) != null;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            Iterator<E> iterator = array.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public E next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Object[] toArray() {
        return array.toArray();
    }

    @Override
    public <E> E[] toArray(E[] a) {
        return array.toArray(a);
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object object : c) {
            if (!contains(object)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new NavigableArraySet<>(Ordering.from(comparator).reverse(), Lists.reverse(array));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new NavigableArraySet<>(Ordering.from(comparator).reverse(), Lists.reverse(array)).iterator();
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return headSet(toElement, toInclusive).tailSet(fromElement, fromInclusive);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        int toIndex = binarySearch(toElement, true);
        if (toIndex == array.size()) {
            return new NavigableArraySet<>(comparator, new ArrayList<E>());
        }
        if (toIndex == array.size() || (!inclusive && array.get(toIndex).equals(toElement))) {
            toIndex--;
        }
        toIndex++;
        return new NavigableArraySet<>(comparator, array.subList(0, toIndex));
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        int fromIndex = binarySearch(fromElement, false);
        if (fromIndex == array.size()) {
            return new NavigableArraySet<>(comparator, new ArrayList<E>());
        }
        if (fromIndex == -1 || (!inclusive && array.get(fromIndex).equals(fromElement))) {
            fromIndex++;
        }
        return new NavigableArraySet<>(comparator, array.subList(fromIndex, array.size()));
    }

    @Override
    public Comparator<? super E> comparator() {
        if (naturalOrderUsed) {
            return null;
        } else {
            return comparator;
        }
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, true);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, true);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E first() {
        return array.get(0);
    }

    @Override
    public E last() {
        return array.get(array.size() - 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NavigableArraySet that = (NavigableArraySet) o;

        if (array != null ? !array.equals(that.array) : that.array != null) return false;
        if (comparator != null ? !comparator.equals(that.comparator) : that.comparator != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
