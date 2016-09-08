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
public class RankOperatorToken extends Token {

    public static RankOperatorToken fromText(String l) {
        boolean smallest = l.endsWith("smallest");
        l=l.replace("the ", "");
        Pattern p = Pattern.compile("^\\d+");
        Matcher m = p.matcher(l);
        int rank = 1;
        if (m.find()) {
            try {
                rank = Integer.parseInt(m.group());
            } catch (Exception e) {
            }
        }
        return new RankOperatorToken(rank, smallest);
    }

    public static RankOperatorToken fromLabel(String l) {
        String[] s=l.split("-");        
        return new RankOperatorToken(Integer.parseInt(s[1]), s[0].equals("last"));
    }
    
    public String getLabel() {
        if (smallest) {
            return "last-"+rank;
        } else {
            return "first-"+rank;
        }
    }    
    
    int rank;
    boolean smallest; //if last is false, the element represent the rank-th greatest, otherwise it represents the rank-th smallest

    public RankOperatorToken(int rank, boolean smallest) {
        this.rank = rank;
        this.smallest = smallest;
    }

    private String getOrdinal() {
        if (rank == 1) {
            return "";
        }
        if (rank % 10 == 1) {
            if ((rank % 100) / 10 == 1) {
                return rank + "th ";
            }
            return rank + "st ";
        }
        if (rank % 10 == 2) {
            if ((rank % 100) / 10 == 1) {
                return rank + "th ";
            }
            return rank + "nd ";
        }
        if (rank % 10 == 3) {
            if ((rank % 100) / 10 == 1) {
                return rank + "th ";
            }
            return rank + "rd ";
        }
        return rank + "th ";
    }
    
    @Override
    public String getText() {
        return "the " + getOrdinal() + (smallest ? "smallest" : "greatest");
    }

    public int getRank() {
        return rank;
    }

    public boolean getSmallest() {
        return smallest;
    }
}
