package ru.ifmo.ctddev.polyarnyi.task2;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * Date: 02.03.14 at 20:16
 *
 * @author Nickolay Polyarniy aka PolarNick
 */
public class NavigableArraySetTest {

    private static final Comparator<Integer> NATURAL_ORDER = new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            return o1 - o2;
        }
    };

    private static final List<Integer> valuesWithDublication = Lists.newArrayList(1, 1, 2, 3, 4, 5, 6, 2, 1, 3, 7, 8, 9);
    private static final List<Integer> values = Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9);

    @Test
    public void testCreationAndToArray() throws Exception {
        NavigableArraySet<Integer> set = new NavigableArraySet<>(values, NATURAL_ORDER);
        assertEquals(set, values);
    }

    @Test
    public void testCreationWithDublication() throws Exception {
        NavigableArraySet<Integer> set = new NavigableArraySet<>(valuesWithDublication, NATURAL_ORDER);
        assertEquals(set, values);
    }

    @Test
    public void testCreationFromComparable() throws Exception {
        NavigableArraySet<Integer> set = NavigableArraySet.createWithNaturalOrder(valuesWithDublication);
        assertEquals(set, values);
    }

    @Test
    public void testLower() throws Exception {
        Assert.assertEquals(Integer.valueOf(2), createSet(1, 2, 6).lower(5));
    }

    @Test
    public void testLower2() throws Exception {
        Assert.assertEquals(Integer.valueOf(6), createSet(1, 2, 6).lower(10));
    }

    @Test
    public void testLowerNull1() throws Exception {
        Assert.assertEquals(null, createSet(1, 2, 6).lower(1));
    }

    @Test
    public void testLowerNull2() throws Exception {
        Assert.assertEquals(null, createSet(1, 2, 6).lower(-10));
    }

    @Test
    public void testCeiling() throws Exception {
        Assert.assertEquals(Integer.valueOf(2), createSet(1, 2, 6).ceiling(2));
    }

    @Test
    public void testFloor() throws Exception {
        Assert.assertEquals(Integer.valueOf(6), createSet(1, 2, 6).floor(10));
    }

    @Test
    public void testHigherNull() throws Exception {
        Assert.assertEquals(null, createSet(1, 2, 6).higher(6));
    }

    @Test
    public void testHigher() throws Exception {
        Assert.assertEquals(Integer.valueOf(6), createSet(1, 2, 6).higher(2));
    }

    @Test
    public void testHigher2() throws Exception {
        Assert.assertEquals(Integer.valueOf(6), createSet(1, 2, 6).higher(3));
    }

    @Test
    public void testSubSet() throws Exception {
        SortedSet<Integer> set = createSet(1, 3, 6, 7, 9);
        SortedSet<Integer> subSet = set.subSet(3, 7);
        assertEquals(subSet, Lists.newArrayList(3, 6, 7));
    }

    @Test
    public void testHeadSet() throws Exception {
        SortedSet<Integer> set = createSet(1, 3, 6, 7, 9);
        SortedSet<Integer> subSet = set.headSet(8);
        assertEquals(subSet, Lists.newArrayList(1, 3, 6, 7));
    }

    @Test
    public void testTailSet() throws Exception {
        SortedSet<Integer> set = createSet(1, 3, 6, 7, 9);
        SortedSet<Integer> subSet = set.tailSet(8);
        assertEquals(subSet, Lists.newArrayList(9));
    }

    @Test
    public void testSubSetInclusive() throws Exception {
        NavigableSet<Integer> set = createSet(1, 3, 6, 7, 9);
        SortedSet<Integer> subSet = set.subSet(3, true, 7, false);
        assertEquals(subSet, Lists.newArrayList(3, 6));
    }

    private static NavigableSet<Integer> createSet(Integer... a) {
        return NavigableArraySet.createWithNaturalOrder(Arrays.asList(a));
    }

    private static void assertEquals(SortedSet<Integer> set, List<Integer> expectedData) {
        Assert.assertEquals(expectedData.size(), set.size());
        Object[] toArray = set.toArray();
        Assert.assertEquals(expectedData.size(), toArray.length);
        for (int i = 0; i < expectedData.size(); i++) {
            Assert.assertEquals(expectedData.get(i), toArray[i]);
        }
    }
}
