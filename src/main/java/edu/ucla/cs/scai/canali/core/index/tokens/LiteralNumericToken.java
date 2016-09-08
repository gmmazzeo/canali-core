/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.tokens;

import java.text.DecimalFormat;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class LiteralNumericToken extends LiteralToken {

    double val;

    public LiteralNumericToken(String val) throws Exception {
        this.val = Double.parseDouble(val.trim());
    }

    @Override
    public String getText() {
        return new DecimalFormat("#0.####").format(val);
    }

    public double getVal() {
        return val;
    }

}
