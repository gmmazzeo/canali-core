/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.tokens;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class DirectBinaryOperatorToken extends OperatorToken {
    
    public DirectBinaryOperatorToken(String text, String symbol, boolean prefix) {
        super(text, symbol, prefix);
    }

    @Override
    public String getType() {
        return DIRECT_OPERATOR;
    }
}
