/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.translation;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class QueryModel {

    public static final int SUBJECT_TYPE = 1, VALUE_TYPE = 2;

    public static final int NONE = 0, COUNT = 1, AVERAGE = 2, SUM = 3;

    boolean ascendingSorting;

    String entityVariable, valueVariable, propertyText, sortingVariable, sortingPopertyText;

    Integer offset, limit;

    int aggregateFunction;

    public ArrayList<QueryConstraint> constraints = new ArrayList<>();
    public HashMap<String, String> bindingVariablesToEntities = new HashMap<>();
    public ArrayList<QueryFilter> filters = new ArrayList<>();
    int type; //SUBJECT_TYPE or VALUE_TYPE

    public QueryModel(String entityVariable, String valueVariable, String propertyText, int type) {
        this(entityVariable, valueVariable, propertyText, type, NONE);
    }

    public QueryModel(String entityVariable, String valueVariable, String propertyText, int type, int aggregateFunction) {
        this.entityVariable = entityVariable;
        this.valueVariable = valueVariable;
        this.propertyText = propertyText;
        this.type = type;
        this.aggregateFunction = aggregateFunction;
    }

    public String getEntityVariable() {
        return entityVariable;
    }

    public void setEntityVariable(String entityVariable) {
        this.entityVariable = entityVariable;
    }

    public String getValueVariable() {
        return valueVariable;
    }

    public void setValueVariable(String propertyVariable) {
        this.valueVariable = propertyVariable;
    }

    public String getPopertyText() {
        return propertyText;
    }

    public void setPopertyText(String propertyText) {
        this.propertyText = propertyText;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public ArrayList<QueryConstraint> getConstraints() {
        return constraints;
    }

    public void setContraints(ArrayList<QueryConstraint> contraints) {
        this.constraints = contraints;
    }

    public ArrayList<QueryFilter> getFilters() {
        return filters;
    }

    public void setFilters(ArrayList<QueryFilter> filters) {
        this.filters = filters;
    }

    public void merge(QueryModel qm) throws Exception {
        constraints.addAll(qm.constraints);
        filters.addAll(qm.filters);
        bindingVariablesToEntities.putAll(qm.bindingVariablesToEntities);
        setSortingAndLimit(qm.sortingVariable, qm.sortingPopertyText, qm.ascendingSorting, qm.limit, qm.offset);
    }

    public void setSortingAndLimit(String sortingVariable, String sortingPopertyText, boolean ascendingSorting, Integer limit, Integer offset) throws Exception {
        if (sortingVariable != null) {
            if (this.sortingVariable != null) {
                throw new Exception("Multiple sorting not supported");
            }
            this.sortingVariable = sortingVariable;
            this.sortingPopertyText = sortingPopertyText;
            this.ascendingSorting = ascendingSorting;            
            this.limit = limit;
            this.offset = offset;
        }
    }
}
