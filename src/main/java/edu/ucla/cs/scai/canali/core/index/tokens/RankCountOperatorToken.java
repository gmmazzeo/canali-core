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
public class RankCountOperatorToken extends RankOperatorToken {

    public static RankCountOperatorToken fromText(String l) {
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
        return new RankCountOperatorToken(rank, smallest);
    }

    public static RankCountOperatorToken fromLabel(String l) {
        String[] s=l.split("-");        
        return new RankCountOperatorToken(Integer.parseInt(s[2]), s[0].equals("last"));
    }
    
    public String getLabel() {
        if (smallest) {
            return "last-count-"+rank;
        } else {
            return "first-count-"+rank;
        }
    }    
    
    public RankCountOperatorToken(int rank, boolean smallest) {
        super(rank, smallest);
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
        return "the " + getOrdinal() + (smallest ? "smallest" : "greatest") + " count of";
    }
}
