/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.translation;

import java.util.ArrayList;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class TranslationWrapper {

    String query, endPoint;
    String error;
    String id;
    static String alphabet = "1234567890QWERTYUIOPLKJHGFDSAZXCVBNM";
    boolean yesNoQuestion;
    int aggregateFunction;
    String valueVariable, entityVariable, sortingVariable, propertyText, sortingPropertyText;
    String alternativeQuery;

    public TranslationWrapper(String query, String endPoint, boolean yesNoQuestion, int aggregateFunction, String valueVariable, String entityVariable, String sortingVariable, String propertyText, String sortingPropertyText) {
        this.query = query;
        this.endPoint = endPoint;
        this.yesNoQuestion = yesNoQuestion;
        this.aggregateFunction = aggregateFunction;
        this.valueVariable = valueVariable;
        this.entityVariable = entityVariable;
        this.sortingVariable = sortingVariable;
        this.propertyText = propertyText;
        this.sortingPropertyText = sortingPropertyText;
        id = "";
        for (int i = 0; i < 10; i++) {
            id += alphabet.charAt((int) (Math.random() * alphabet.length()));
        }
    }
    
    public TranslationWrapper(String query, String endPoint, boolean yesNoQuestion, int aggregateFunction, String valueVariable, String entityVariable, String sortingVariable, String propertyText, String sortingPropertyText, String alternativeQuery) {
        this(query, endPoint, yesNoQuestion, aggregateFunction, valueVariable, entityVariable, sortingVariable, propertyText, sortingPropertyText);
        this.alternativeQuery = alternativeQuery;
    }
    

    public TranslationWrapper(String error) {
        this.error = error;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(String endPoint) {
        this.endPoint = endPoint;
    }

    public boolean isYesNoQuestion() {
        return yesNoQuestion;
    }

    public void setYesNoQuestion(boolean yesNoQuestion) {
        this.yesNoQuestion = yesNoQuestion;
    }

    public int getAggregateFunction() {
        return aggregateFunction;
    }

    public void setAggregateFunction(int aggregateFunction) {
        this.aggregateFunction = aggregateFunction;
    }

    public String getValueVariable() {
        return valueVariable;
    }

    public void setValueVariable(String valueVariable) {
        this.valueVariable = valueVariable;
    }

    public String getEntityVariable() {
        return entityVariable;
    }

    public void setEntityVariable(String entityVariable) {
        this.entityVariable = entityVariable;
    }

    public String getSortingVariable() {
        return sortingVariable;
    }

    public void setSortingVariable(String sortingVariable) {
        this.sortingVariable = sortingVariable;
    }

    public String getPropertyText() {
        return propertyText;
    }

    public void setPropertyText(String propertyText) {
        this.propertyText = propertyText;
    }

    public String getSortingPropertyText() {
        return sortingPropertyText;
    }

    public void setSortingPropertyText(String sortingPropertyText) {
        this.sortingPropertyText = sortingPropertyText;
    }

    public String getAlternativeQuery() {
        return alternativeQuery;
    }

    public void setAlternativeQuery(String alternativeQuery) {
        this.alternativeQuery = alternativeQuery;
    }

}
