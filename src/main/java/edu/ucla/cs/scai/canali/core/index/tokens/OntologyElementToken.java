/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.tokens;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.HashSet;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public abstract class OntologyElementToken extends IndexedToken {

    public String uri;
    public String label;
    
    protected OntologyElementToken() {
        super();
    }

    public OntologyElementToken(String uri, String label, int multiplicity, boolean prefix) {
        super(multiplicity, prefix);
        this.uri = uri;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        uri=in.readUTF();
        label=in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeUTF(uri);
        out.writeUTF(label);
    }
}
