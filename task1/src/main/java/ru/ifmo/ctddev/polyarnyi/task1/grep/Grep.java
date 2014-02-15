package ru.ifmo.ctddev.polyarnyi.task1.grep;

import com.google.common.collect.Lists;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Date: 15.02.14 at 23:05
 *
 * @author Nickolay Polyarniy aka PolarNick
 */
public class Grep extends SimpleFileVisitor<Path> {

    private static final String USAGE = "Proper Usage is: \"java Grep string1 string2 ... stringN\"\n" +
            "Or to enter strings one by one in console: \"java Grep -\"";
    private static final String STRINGS_FROM_CONSOLE = "-";
    private static final String ERROR_IO_EXCEPTION = "Input/Output error has occurred!";

    public static void main(String[] args) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            if (args.length == 0) {
                System.out.println(USAGE);
            } else if (args.length == 1 && STRINGS_FROM_CONSOLE.equals(args[0])) {
                String s = in.readLine();
                while (s != null && !s.isEmpty()) {
                    searchLinesContains(Lists.newArrayList(s), System.out);
                    s = in.readLine();
                }
            } else {
                searchLinesContains(Lists.newArrayList(args), System.out);
            }
        } catch (IOException e) {
            System.out.println(ERROR_IO_EXCEPTION);
        }
    }

    private static void searchLinesContains(List<String> patterns, PrintStream out) throws IOException {
        MultiEncodingSearch searcher = new MultiEncodingSearch(patterns, Lists.newArrayList("UTF-8", "KOI8-R", "CP1251", "CP866"));
        Path curPath = Paths.get("");
        Files.walkFileTree(curPath, new Grep(searcher, out));
    }

    private final MultiEncodingSearch searcher;
    private final PrintStream out;

    public Grep(MultiEncodingSearch searcher, PrintStream out) {
        this.searcher = searcher;
        this.out = out;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
        searcher.reset();
        try {
            BufferedReader in = new BufferedReader(new FileReader(file.toFile()));
            String str = in.readLine();
            while (str != null) {
                byte[] bytes = str.getBytes();
                for (byte cur : bytes) {
                    if (searcher.proceed(toUnsignedByte(cur))) {
                        out.println(file.toString() + ": " + str);
                        break;
                    }
                }
                str = in.readLine();
            }
            return FileVisitResult.CONTINUE;
        } catch (FileNotFoundException e) {
            return FileVisitResult.CONTINUE;
        }
    }

    public static class MultiEncodingSearch {

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

        public boolean proceed(int nextByte) {
            return ahoCorasick.processText(nextByte);
        }

    }

    public static class AhoCorasick {

        private final int minCharacter;
        private final int alphabetSize;
        private final Node root;

        private Node current;

        public AhoCorasick(List<int[]> patterns, int minCharacter, int maxCharacter) {
            this.minCharacter = minCharacter;
            this.alphabetSize = maxCharacter - minCharacter + 1;
            this.root = new Node(-1, null);

            for (int[] pattern : patterns) {
                addPattern(pattern);
            }
            resetText();
        }

        public void resetText() {
            this.current = this.root;
        }

        public boolean processText(int character) {
            character -= minCharacter;
            Node cur = current.getGo(character);
            boolean result = cur.isLeaf;
            current = cur;
            while (cur != root) {
                cur = cur.getUp();
                result = result || cur.isLeaf;
            }
            return result;
        }

        private void addPattern(int[] pattern) {
            Node cur = root;
            for (int c : pattern) {
                c -= minCharacter;
                if (cur.sons[c] == null) {
                    cur.sons[c] = new Node(c, cur);
                }
                cur = cur.sons[c];
            }
            cur.isLeaf = true;
        }

        private class Node {

            final Node[] sons = new Node[alphabetSize];
            final Node[] go = new Node[alphabetSize];
            Node suffixLink = null;
            Node up = null;
            boolean isLeaf = false;

            Node parent;
            int characterToParent;

            private Node(int characterToParent, Node parent) {
                this.characterToParent = characterToParent;
                this.parent = parent;
            }

            private Node getSuffixLink() {
                if (suffixLink == null) {
                    if (this == root || parent == root) {
                        suffixLink = root;
                    } else {
                        suffixLink = parent.getSuffixLink().getGo(characterToParent);
                    }
                }
                return suffixLink;
            }

            private Node getGo(int c) {
                if (go[c] == null) {
                    if (sons[c] != null) {
                        go[c] = sons[c];
                    } else {
                        go[c] = (this == root) ? root : getSuffixLink().getGo(c);
                    }
                }
                return go[c];
            }

            private Node getUp() {
                if (up == null) {
                    if (getSuffixLink().isLeaf) {
                        up = getSuffixLink();
                    } else if (getSuffixLink() == root) {
                        up = root;
                    } else {
                        up = getSuffixLink().getUp();
                    }
                }
                return up;
            }

            @Override
            public String toString() {
                if (getSuffixLink() == null) {
                    return toStringSuffix();
                } else {
                    return toStringSuffix() + " ->" + getSuffixLink().toStringSuffix();
                }
            }

            public String toStringSuffix() {
                Node cur = this;
                String result = "";
                while (cur != root) {
                    result = (char) (minCharacter + cur.characterToParent) + result;
                    cur = cur.parent;
                }
                return result;
            }
        }

    }

    private static int toUnsignedByte(byte aByte) {
        return 128 + aByte;
    }

}
