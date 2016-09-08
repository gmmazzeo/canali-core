/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.query;

import java.util.ArrayList;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class ResultWrapper {

    String query;

    ArrayList<ResultObject> results;

    String error;

    public ResultWrapper(String query, ArrayList<ResultObject> results) {
        this.query = query;
        this.results = results;
    }

    public ResultWrapper(String query, String error) {
        this.query = query;
        this.error = error;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public ArrayList<ResultObject> getResults() {
        return results;
    }

    public void setResults(ArrayList<ResultObject> results) {
        this.results = results;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

}
