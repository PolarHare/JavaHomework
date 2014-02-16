package ru.ifmo.ctddev.polyarnyi.task1.grep;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Date: 16.02.14 at 1:04
 *
 * @author Nickolay Polyarniy aka PolarNick
 */
public class AhoCorasickTest {

    private static final List<int[]> patterns = createArrayList(
            toInts("hers"),
            toInts("he"),
            toInts("his"),
            toInts("she")
    );

    @Test
    public void test1() throws Exception {
        String encoding = "UTF-8";
        AhoCorasick<String> search = new AhoCorasick<>(patterns, Collections.nCopies(patterns.size(), encoding), ' ', 255);
        int[] text = toInts("hers he his hers she hasdf");
        for (int i = 0; i < text.length; i++) {
            Assert.assertEquals("At index = " + i, isOneOfPatternEnd(text, i) ? encoding : null, search.processText(text[i]));
        }
    }

    private static boolean isOneOfPatternEnd(int[] text, int i) {
        for (int[] pattern : patterns) {
            if (isEndOfPattern(text, i, pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEndOfPattern(int[] text, int upToIndex, int[] pattern) {
        if (upToIndex + 1 < pattern.length) {
            return false;
        }
        for (int i = 0; i < pattern.length; i++) {
            if (text[upToIndex - pattern.length + 1 + i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }

    private static int[] toInts(String string) {
        int[] result = new int[string.length()];
        for (int i = 0; i < string.length(); i++) {
            result[i] = string.charAt(i);
        }
        return result;
    }

    private static List<int[]> createArrayList(int[]... a) {
        List<int[]> result = new ArrayList<>(a.length);
        Collections.addAll(result, a);
        return result;
    }

}
