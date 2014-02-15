package ru.ifmo.ctddev.polyarnyi.task1.grep;

import java.util.List;

/**
 * Date: 16.02.14 at 2:51
 *
 * @author Nickolay Polyarniy aka PolarNick
 */
public class AhoCorasick {

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