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
public class PossessiveDeterminerToken extends IndexedSyntacticToken {

    public PossessiveDeterminerToken(String text, int multiplicity) {
        super(text, multiplicity, false);
    }

    @Override
    public String getType() {
        return POSSESSIVE_DETERMINER;
    }
}
