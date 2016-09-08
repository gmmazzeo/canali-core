/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.tokens;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class LiteralDateToken extends LiteralToken {

    Date date;

    public LiteralDateToken(String val) throws Exception {
        try {
            date = new SimpleDateFormat("MM/dd/yyyy").parse(val);
        } catch (Exception e1) {
            try {
                date = new SimpleDateFormat("dd/MM/yyyy").parse(val);
            } catch (Exception e2) {
                try {
                    date = new SimpleDateFormat("yyyy-MM-dd").parse(val);
                } catch (Exception e3) {
                    try {
                        date = new SimpleDateFormat("yyyy/MM/dd").parse(val);
                    } catch (Exception e4) {
                        throw new Exception("Unrecognized date format");
                    }
                }
            }
        }
    }

    @Override
    public String getText() {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

}
