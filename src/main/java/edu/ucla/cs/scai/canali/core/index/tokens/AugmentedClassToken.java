/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.tokens;

import java.util.HashSet;
import java.util.Iterator;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class AugmentedClassToken extends IndexedToken {

    ClassToken classToken;
    HashSet<String> keywords;
    String freeText;

    public AugmentedClassToken(ClassToken classToken, HashSet<String> keywords, boolean isPrefix) {
        super(classToken.getMultiplicity(), isPrefix);
        this.classToken = classToken;
        this.keywords = keywords;
    }

    public ClassToken getClassToken() {
        return classToken;
    }

    public void setClassToken(ClassToken classToken) {
        this.classToken = classToken;
    }

    public HashSet<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(HashSet<String> keywords) {
        this.keywords = keywords;
    }

    @Override
    public String getType() {
        return classToken.getType();
    }

    @Override
    public String getText() {
        StringBuilder sb=new StringBuilder(classToken.getText());
        if (keywords!=null && !keywords.isEmpty()) {
            Iterator<String> it=keywords.iterator();
            sb.append(" (").append(it.next());
            while (it.hasNext()) {
                sb.append(" ").append(it.next());
            }
            sb.append(")");
        }
        return sb.toString();
    }
    
    public String getFreeText() {
        return freeText;
    }

}
