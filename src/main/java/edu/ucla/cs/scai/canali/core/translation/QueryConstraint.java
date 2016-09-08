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
public class QueryConstraint {

    String subject, property, value;
    String invertedProperty; //used when the parameter property of the constructor contains bothregular and inverted properties

    public QueryConstraint(String subject, String property, String value) {

        String[] atts = property.split("\\|");
        StringBuilder tempA = new StringBuilder();
        StringBuilder tempIA = new StringBuilder();
        for (int i = 0; i < atts.length; i++) {
            if (atts[i].endsWith("Inv")) {
                if (tempIA.length() > 0) {
                    tempIA.append("|");
                }
                tempIA.append(atts[i].substring(0, atts[i].length() - 3));
            } else {
                if (tempA.length() > 0) {
                    tempA.append("|");                    
                }
                tempA.append(atts[i]);
            }
        }

        if (tempA.length() > 0) {
            this.subject = subject;
            this.value = value;
            this.property = tempA.toString();
            if (tempIA.length() > 0) {
                invertedProperty = tempIA.toString();
            }
        } else {
            this.subject = value;
            this.value = subject;
            this.property = tempIA.toString();
        }
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getInvertedProperty() {
        return invertedProperty;
    }

    public void setInvertedProperty(String invertedProperty) {
        this.invertedProperty = invertedProperty;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        fillStringBuilder(sb);
        return sb.toString();
    }

    public void fillStringBuilder(StringBuilder sb) {
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
    }
}
