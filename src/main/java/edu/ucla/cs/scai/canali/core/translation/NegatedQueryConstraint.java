/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.translation;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class NegatedQueryConstraint extends QueryConstraint {

    public NegatedQueryConstraint(String subject, String property, String value) {
        super(subject, property, value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        fillStringBuilder(sb);
        return sb.toString();
    }

    public void fillStringBuilder(StringBuilder sb) {
        sb.append("FILTER NOT EXISTS (");
        if (subject.startsWith("http:")) {
            sb.append("<").append(subject).append(">");
        } else {
            sb.append("?").append(subject);
        }
        sb.append(" <").append(property).append("> ");
        if (value.startsWith("http:")) {
            sb.append("<").append(value).append(">");
        } else {
            sb.append("?").append(value);
        }
        sb.append(")");
    }
}
