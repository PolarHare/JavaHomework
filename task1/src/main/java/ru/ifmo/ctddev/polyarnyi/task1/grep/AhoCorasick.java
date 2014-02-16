package ru.ifmo.ctddev.polyarnyi.task1.grep;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 16.02.14 at 2:51
 *
 * @author Nickolay Polyarniy aka PolarNick
 */
public class AhoCorasick<Data> {

    private final int minCharacter;
    private final int alphabetSize;
    private final Node root;

    private Node current;

    public AhoCorasick(List<int[]> patterns, List<Data> data, int minCharacter, int maxCharacter) {
        Preconditions.checkArgument(patterns.size() == data.size(), "Data must be per pattern!");
        this.minCharacter = minCharacter;
        this.alphabetSize = maxCharacter - minCharacter + 1;
        this.root = new Node(-1, null);

        for (int i = 0; i < patterns.size(); i++) {
            addPattern(patterns.get(i), data.get(i));
        }
        resetText();
    }

    public void resetText() {
        this.current = this.root;
    }

    public Data processText(int character) {
        character -= minCharacter;
        Node cur = current.getGo(character);
        Data result = cur.dataAtThisPattern;
        current = cur;
        while (cur != root) {
            cur = cur.getUp();
            if (result == null) {
                result = cur.dataAtThisPattern;
            }
        }
        return result;
    }

    private void addPattern(int[] pattern, Data dataForPattern) {
        Node cur = root;
        for (int c : pattern) {
            c -= minCharacter;
            if (cur.sons.get(c) == null) {
                cur.sons.set(c, new Node(c, cur));
            }
            cur = cur.sons.get(c);
        }
        cur.isLeaf = true;
        cur.dataAtThisPattern = dataForPattern;
    }

    private class Node {

        final List<Node> sons = new ArrayList<>(alphabetSize);
        final List<Node> go = new ArrayList<>(alphabetSize);
        Node suffixLink = null;
        Node up = null;
        Data dataAtThisPattern = null;
        boolean isLeaf = false;

        Node parent;
        int characterToParent;

        private Node(int characterToParent, Node parent) {
            this.characterToParent = characterToParent;
            this.parent = parent;
            while (sons.size() < alphabetSize) sons.add(null);
            while (go.size() < alphabetSize) go.add(null);
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
            if (go.get(c) == null) {
                if (sons.get(c) != null) {
                    go.set(c, sons.get(c));
                } else {
                    go.set(c, (this == root) ? root : getSuffixLink().getGo(c));
                }
            }
            return go.get(c);
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