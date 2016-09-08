/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.tokens;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public abstract class IndexedToken extends Token implements Externalizable {

    public static final String CLASS = "cl", ENTITY = "en", PROPERTY = "pr", CONSTRAINT_CONNECTIVE = "cc", DIRECT_OPERATOR = "dop", INDIRECT_OPERATOR = "iop", UNARY_OPERATOR = "uop", QUESTION_START = "qs", POSSESSIVE_DETERMINER = "poss", TOPK_OPERATOR = "tk";

    public static final int UNDEFINED = 0, SINGULAR = 1, PLURAL = 2;

    public static int counter = 0;

    private int id;

    private boolean prefix;

    int multiplicity;
    
    protected IndexedToken() {        
    }

    public IndexedToken(int multiplicity, boolean prefix) {
        this.id = ++counter;
        this.multiplicity = multiplicity;
        this.prefix = prefix;
    }

    public int getId() {
        return id;
    }

    public int getMultiplicity() {
        return multiplicity;
    }

    public abstract String getType();

    public boolean isPrefix() {
        return prefix;
    }

    public void setPrefix(boolean prefix) {
        this.prefix = prefix;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        id = in.readInt();
        multiplicity = in.readInt();
        prefix = in.readBoolean();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(id);
        out.writeInt(multiplicity);
        out.writeBoolean(prefix);
    }
}
