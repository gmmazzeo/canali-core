/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.tokens;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class TopKOperatorToken extends Token {
    
    public static TopKOperatorToken fromText(String l) {
        boolean ascending = l.endsWith("smallest");
        l=l.replace("one of the ", "");
        Pattern p = Pattern.compile("^\\d+");
        Matcher m = p.matcher(l);
        int rank = 1;
        if (m.find()) {
            try {
                rank = Integer.parseInt(m.group());
            } catch (Exception e) {
            }
        }
        return new TopKOperatorToken(rank, ascending);
    }
    
    public static TopKOperatorToken fromLabel(String l) {
        String[] s=l.split("-");        
        return new TopKOperatorToken(Integer.parseInt(s[1]), s[0].equals("least"));
    }
    
    public String getLabel() {
        if (ascending) {
            return "least-"+k;
        } else {
            return "top-"+k;
        }
    }

    boolean ascending; //ascending=false means the greatest, ascendig=true means the smallest
    int k;

    public TopKOperatorToken(int k, boolean ascending) {
        super();
        this.k = k;
        this.ascending = ascending;
    }

    @Override
    public String getText() {
        return "one of the " + k + (ascending ? " smallest" : " greatest");
    }

    public int getK() {
        return k;
    }

    public boolean getAscending() {
        return ascending;
    }

}
