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
public class PropertyToken extends OntologyElementToken {

    HashSet<String> basicTypeRanges = new HashSet<>();
    HashSet<String> propertyAndClassDomain = new HashSet<>();
    boolean hasOutProperties;
    boolean hasLiteralRange;
    int form;

    public static final int VERBAL = 1, NOMINAL = 2;
    
    public PropertyToken() {
        super();
    }

    public PropertyToken(String uri, String label, int multiplicity, int form, boolean hasOutProperties, boolean hasLiteralRange, boolean prefix) {
        super(uri, label, multiplicity, prefix);
        this.form = form;
        this.hasOutProperties = hasOutProperties;
        this.hasLiteralRange = hasLiteralRange;
    }

    public int getForm() {
        return form;
    }

    @Override
    public String getType() {
        return PROPERTY;
    }

    @Override
    public String getText() {
        return label.toLowerCase();
    }

    public boolean addBasicTypeRange(String s) {
        return basicTypeRanges.add(s);
    }

    public boolean addBasicTypeRanges(HashSet<String> s) {
        return basicTypeRanges.addAll(s);
    }

    public boolean hasBasicTypeRange(String s) {
        return basicTypeRanges.contains(s);
    }

    public boolean hasOutProperties() {
        return hasOutProperties;
    }

    public boolean hasLiteralRange() {
        return hasLiteralRange;
    }

    public boolean hasPropertyOrClassDomain(String uri) {
        return propertyAndClassDomain.contains(uri);
    }

    public boolean addPropertyOrClassDomain(String uri) {
        return propertyAndClassDomain.add(uri);
    }

    public void setPropertyAndClassDomain(HashSet<String> propertyAndClassDomain) {
        this.propertyAndClassDomain = propertyAndClassDomain;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        basicTypeRanges = (HashSet<String>) in.readObject();
        propertyAndClassDomain = (HashSet<String>) in.readObject();
        hasOutProperties = in.readBoolean();
        hasLiteralRange = in.readBoolean();        
        form = in.readInt();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(basicTypeRanges);
        out.writeObject(propertyAndClassDomain);
        out.writeBoolean(hasOutProperties);
        out.writeBoolean(hasLiteralRange);
        out.writeInt(form);
    }

}
