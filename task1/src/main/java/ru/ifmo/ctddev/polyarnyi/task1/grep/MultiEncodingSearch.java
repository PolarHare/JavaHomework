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

    private final AhoCorasick<String> ahoCorasick;

    public MultiEncodingSearch(List<String> patterns, List<String> encodings) {
        List<int[]> patternsInAllEncodings = new ArrayList<>(patterns.size() * encodings.size());
        List<String> patternEncodings = new ArrayList<>(patterns.size() * encodings.size());
        for (String pattern : patterns) {
            for (String encoding : encodings) {
                byte[] bytes = pattern.getBytes(Charset.forName(encoding));
                int[] a = new int[bytes.length];
                for (int i = 0; i < bytes.length; i++) {
                    a[i] = toUnsignedByte(bytes[i]);
                }
                patternsInAllEncodings.add(a);
                patternEncodings.add(encoding);
            }
        }
        ahoCorasick = new AhoCorasick<>(patternsInAllEncodings, patternEncodings, 0, 255);
    }

    public void reset() {
        ahoCorasick.resetText();
    }

    public String proceed(int nextByte) {
        return ahoCorasick.processText(toUnsignedByte(nextByte));
    }

    private static int toUnsignedByte(int aByte) {
        return (256 + aByte) % 256;
    }

}