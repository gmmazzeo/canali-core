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
public class LiteralPercentageToken extends LiteralNumericToken {

    public LiteralPercentageToken(String val) throws Exception {
        super(val.replace("%", "").trim());
        if (!val.endsWith("%")) {
            throw new Exception("Bad percentage format");
        }
    }

    @Override
    public String getText() {
        return super.getText()+"%";
    }

}
