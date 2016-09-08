/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.utils;

import java.util.HashMap;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class Trie {

    boolean word;
    int subTreesSize;
    HashMap<Character, Trie> children = new HashMap<>();

    public void add(String s) {
        add(s.toLowerCase().toCharArray(), 0);
    }

    private void add(char[] chars, int pos) {
        if (pos == chars.length) {
            word = true;
        } else {
            subTreesSize++;
            Trie child = children.get(chars[pos]);
            if (child == null) {
                child = new Trie();
                children.put(chars[pos], child);
            }
            child.add(chars, pos + 1);
        }
    }

    public int subTreesSize(String s) {
        return subTreesSize(s.toLowerCase().toCharArray(), 0);
    }

    private int subTreesSize(char[] chars, int pos) {
        if (pos == chars.length) {
            return subTreesSize;
        }
        Trie child = children.get(chars[pos]);
        if (child == null) {
            return 0;
        }
        return child.subTreesSize(chars, pos + 1);
    }

    public String getOneSuffix(String text) {
        String s = getOneSuffix(text.toLowerCase().toCharArray(), 0);
        if (s == null) {
            return null;
        }
        return text + s;
    }

    public String getOneSuffix(char[] chars, int pos) {
        if (pos == chars.length) {
            if (children.isEmpty()) {
                return null;
            }
            String res = "";
            char c = children.keySet().iterator().next();
            res += c;
            Trie node = children.get(c);
            while (!node.word) {
                c = node.children.keySet().iterator().next();
                res += c;
                node = node.children.get(c);
            }
            return res;
        }
        Trie child = children.get(chars[pos]);
        if (child == null) {
            return null;
        }
        return child.getOneSuffix(chars, pos + 1);
    }
}
