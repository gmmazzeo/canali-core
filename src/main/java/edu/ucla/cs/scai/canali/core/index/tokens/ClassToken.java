/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.tokens;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class ClassToken extends OntologyElementToken {
    
    public ClassToken() {
        super();
    }

    public ClassToken(String uri, String label, int multiplicity, boolean prefix) {
        super(uri, label, multiplicity, prefix);
    }

    @Override
    public String getType() {
        return CLASS;
    }

    @Override
    public String getText() {
        return label.toLowerCase();
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out); //To change body of generated methods, choose Tools | Templates.
    }
}
