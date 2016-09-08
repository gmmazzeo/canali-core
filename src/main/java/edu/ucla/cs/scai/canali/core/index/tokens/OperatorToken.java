/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.tokens;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public abstract class OperatorToken extends IndexedToken {

    String text, symbol;

    public OperatorToken(String text, String symbol, boolean prefix) {
        super(IndexedToken.SINGULAR, prefix);
        this.text = text;
        this.symbol = symbol;
    }

    @Override
    public String getText() {
        return text;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        text = in.readUTF();
        symbol = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeUTF(text);
        out.writeUTF(symbol);
    }
}
