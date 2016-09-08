/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.autocompleter;

import com.google.gson.Gson;

/**
 *
 * AutocompleteObject is the type of the objects exchanged between the client
 * and the server.
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class AutocompleteObject implements Comparable<AutocompleteObject> {

    public String text, restrictedText, state, labels, tokenType, finalPunctuation;
    public Integer relatedTokenPosition;
    public boolean isPrefix;
    public boolean mustBeAccepted;
    public double similarity;
    public double prefixSimilarity;
    public String remainder;
    public String[] keywords;

    public AutocompleteObject(String json) {
        Gson gson = new Gson();
        JsonStructure obj = gson.fromJson(json, JsonStructure.class);
        text = obj.t;
        restrictedText = obj.r;
        if (restrictedText == null) {
            restrictedText = text;
        }
        state = obj.s;
        labels = obj.l;
        tokenType = obj.k;
        relatedTokenPosition = obj.c;
        finalPunctuation = obj.p;
        keywords = obj.f;
    }

    public AutocompleteObject(String text, String restrictedText, String state, String labels, String tokenType, String finalPunctuation, Integer relatedTokenPosition, boolean isPrefix, String freeText) {
        this.restrictedText = restrictedText;
        this.text = text;
        this.state = state;
        this.labels = labels;
        this.tokenType = tokenType;
        this.finalPunctuation = finalPunctuation;
        this.relatedTokenPosition = relatedTokenPosition;
        this.isPrefix = isPrefix;
        this.keywords = freeText == null ? null : freeText.split(" ");
    }

    public String toJson() {
        JsonStructure json = new JsonStructure();
        json.t = text;
        json.r = restrictedText;
        json.s = state;
        json.l = labels;
        json.k = tokenType;
        json.c = relatedTokenPosition;
        json.p = finalPunctuation;
        json.ip = isPrefix;
        json.mba = mustBeAccepted;
        json.b = remainder;
        json.sim = similarity;
        json.f = keywords;
        Gson gson = new Gson();
        return gson.toJson(json);
    }

    @Override
    public int compareTo(AutocompleteObject o) { //sort by decreasing similarity
        return Double.compare(o.similarity, similarity);
    }

    private class JsonStructure {

        public String t, //text
                r, //restricted text
                s, //state
                l, //labels
                k, //token type
                p, //final punctuation
                b; //remainder
        public String[] f; //keywords
        public Integer c; //related token position
        public boolean ip; //isPrefix
        public boolean mba; //must be accepted
        public double sim; //similarity
    }
}
