package ru.ifmo.ctddev.polyarnyi.task1.grep;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Date: 16.02.14 at 2:53
 *
 * @author Nickolay Polyarniy aka PolarNick
 */

public class MultiEncodingSearch {

    private final AhoCorasick ahoCorasick;

    public MultiEncodingSearch(List<String> patterns, List<String> encodings) {
        List<int[]> patternsInAllEncodings = new ArrayList<>(patterns.size() * encodings.size());
        for (String pattern : patterns) {
            for (String encoding : encodings) {
                byte[] bytes = pattern.getBytes(Charset.forName(encoding));
                int[] a = new int[bytes.length];
                for (int i = 0; i < bytes.length; i++) {
                    a[i] = toUnsignedByte(bytes[i]);
                }
                patternsInAllEncodings.add(a);
            }
        }
        ahoCorasick = new AhoCorasick(patternsInAllEncodings, 0, 255);
    }

    public void reset() {
        ahoCorasick.resetText();
    }

    public boolean proceed(byte nextByte) {
        return ahoCorasick.processText(toUnsignedByte(nextByte));
    }

    private static int toUnsignedByte(byte aByte) {
        return 128 + aByte;
    }

}