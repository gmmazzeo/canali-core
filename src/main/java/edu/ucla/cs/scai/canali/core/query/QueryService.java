/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.query;

import edu.ucla.cs.scai.canali.core.autocompleter.AutocompleteObject;
import edu.ucla.cs.scai.canali.core.index.TokenIndex;
import edu.ucla.cs.scai.canali.core.translation.QueryModel;
import edu.ucla.cs.scai.canali.core.translation.TranslationService;
import edu.ucla.cs.scai.canali.core.translation.TranslationWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.system.JenaSystem;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class QueryService {

    public static final String DEFAULT_END_POINT;

    public static final HashMap<String, String> prefixes = new HashMap<>();
    public static final HashMap<String, String> inversePrefixes = new HashMap<>();

    int typeCounter = 0;

    static {
        prefixes.put("xsd:", "http://www.w3.org/2001/XMLSchema#");
        prefixes.put("rdfs:", "http://www.w3.org/2000/01/rdf-schema#");
        prefixes.put("foaf:", "http://xmlns.com/foaf/0.1/");
        prefixes.put("rdf:", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        //prefixes.put("dbpedia:", "http://dbpedia.org/resource/");
        prefixes.put("dbpedia-owl:", "http://dbpedia.org/ontology/");
        prefixes.put("dbpprop:", "http://dbpedia.org/property/");
        for (Map.Entry<String, String> e : prefixes.entrySet()) {
            inversePrefixes.put(e.getValue(), e.getKey());
        }
        String endpoint = System.getProperty("sparql.endpoint");
        if (endpoint == null) {
            DEFAULT_END_POINT = "http://dbpedia.org/sparql";//"http://mars.cs.ucla.edu:8890/sparql";
        } else {
            DEFAULT_END_POINT = endpoint;
        }
        System.out.println("DEFAULT SPARQL ENDPOINT: " + DEFAULT_END_POINT);
        System.out.println("Init Jena...");
        JenaSystem.init();
        System.out.println("Completed");
    }

    public ResultWrapper answerQuery(ArrayList<AutocompleteObject> states, String endpoint, int limit, boolean disableSubclass) throws Exception {
        if (states.isEmpty()) {
            throw new Exception("Empty sequence of states");
        }
        TranslationService ts = new TranslationService();
        TranslationWrapper tw = ts.translateQuery(states, endpoint, limit, disableSubclass);
        ResultWrapper res = executeSparql(tw);
        return res;
    }

    public ResultWrapper executeSparql(TranslationWrapper tw) {

        if (!tw.getEndPoint().startsWith("http")) {
            tw.setEndPoint(DEFAULT_END_POINT);
        }

        ArrayList<ResultObject> res = new ArrayList<>();

        if (tw.isYesNoQuestion()) {
            try {
                Query query = QueryFactory.create(tw.getQuery());
                QueryExecution qexec = QueryExecutionFactory.sparqlService(tw.getEndPoint(), query);
                boolean ans = qexec.execAsk();
                res.add(new ResultObject(ans));
            } catch (Exception e) {
                return new ResultWrapper(tw.getQuery(), e.getMessage());
            }
        } else {
            if (tw.getAggregateFunction() == QueryModel.NONE) { //qm.getEntityVariable() is always defined, qm.getValueVariable() is undefined for queries whose result is a set of subjects of triples, is undefined for queries whose result is a set of values of triples
                try {   
                    Query query = QueryFactory.create(tw.getQuery(), Syntax.syntaxSPARQL_11);
                    //QueryExecution qexec = new QueryEngineHTTP(tw.getEndPoint(), query);
                    QueryExecution qexec = QueryExecutionFactory.sparqlService(tw.getEndPoint(), query);
                    ResultSet rs = qexec.execSelect();
                    for (; rs.hasNext();) {
                        QuerySolution qs = rs.next();
                        RDFNode node = qs.get("label");
                        String label = null;
                        String uri = null;
                        if (node == null) { //use the URI as label
                            node = qs.get(tw.getValueVariable() != null ? tw.getValueVariable() : tw.getEntityVariable());
                            if (node.isLiteral()) { //in case propertyVariable is true and the answer is a literal
                                label = node.asLiteral().getLexicalForm();
                            } else {
                                uri = node.asResource().getURI();
                                label = uri.replace("http://dbpedia.org/resource/", "").replace("_", " ");
                            }
                        } else {
                            label = node.asLiteral().getLexicalForm();
                            node = qs.get(tw.getValueVariable() != null ? tw.getValueVariable() : tw.getEntityVariable());
                            uri = node.asResource().getURI();
                        }
                        node = qs.get("url");
                        String url = null;
                        if (node != null) {
                            url = node.asResource().toString();
                        }
                        node = qs.get("picture");
                        String picture = null;
                        if (node != null) {
                            picture = node.asResource().toString();
                        }
                        node = qs.get("abstract");
                        String abstr = null;
                        if (node != null) {
                            abstr = node.asLiteral().getLexicalForm();
                        }
                        String elabel = null;
                        String eUri = null;
                        if (tw.getValueVariable() != null) {
                            node = qs.get(tw.getEntityVariable());
                            eUri = node.asResource().getURI();
                            node = qs.get("elabel");
                            if (node != null) {
                                elabel = node.asLiteral().getLexicalForm();
                            } else { //use the uri as label
                                String[] ls = eUri.split("/");
                                elabel = ls[ls.length - 1];
                            }
                        }
                        node = qs.get("eurl");
                        String eurl = null;
                        if (node != null) {
                            eurl = node.asResource().toString();
                        }
                        String sortingValue = null;
                        if (tw.getSortingVariable() != null) {
                            RDFNode sortingNode = qs.get(tw.getSortingVariable());
                            if (sortingNode != null) {
                                sortingValue = sortingNode.asLiteral().getLexicalForm();
                            }
                        }
                        res.add(new ResultObject(label, url, abstr, picture, elabel, eurl, tw.getPropertyText(), uri, eUri, tw.getSortingPropertyText(), sortingValue));
                    }
                } catch (Exception e) {
                    System.out.println("Error with query\n" + tw.getQuery());
                    e.printStackTrace();
                    return new ResultWrapper(tw.getQuery(), e.getMessage());
                }
            } else if (tw.getAggregateFunction() == QueryModel.COUNT) {
                try {
                    Query query = QueryFactory.create(tw.getQuery());
                    QueryExecution qexec = QueryExecutionFactory.sparqlService(tw.getEndPoint(), query);
                    ResultSet rs = qexec.execSelect();
                    for (; rs.hasNext();) {
                        QuerySolution qs = rs.next();
                        RDFNode node = qs.get("count" + (tw.getValueVariable() != null ? tw.getValueVariable() : tw.getEntityVariable()));
                        String label = node.asLiteral().getLexicalForm();
                        String elabel = null;
                        String eUri = null;
                        if (tw.getValueVariable() != null) {
                            node = qs.get(tw.getEntityVariable());
                            eUri = node.asResource().getURI();
                            node = qs.get("elabel");
                            if (node != null) {
                                elabel = node.asLiteral().getLexicalForm();
                            } else { //use the uri as label
                                String[] ls = eUri.split("/");
                                elabel = ls[ls.length - 1];
                            }
                        }
                        node = qs.get("eurl");
                        String eurl = null;
                        if (node != null) {
                            eurl = node.asResource().toString();
                        }
                        String sortingValue = null;
                        if (tw.getSortingVariable() != null) {
                            RDFNode sortingNode = qs.get(tw.getSortingVariable());
                            if (sortingNode != null) {
                                sortingValue = sortingNode.asLiteral().getLexicalForm();
                            }
                        }
                        res.add(new ResultObject(label, null, null, null, elabel, eurl, "count of " + tw.getPropertyText(), null, eUri, tw.getSortingPropertyText(), sortingValue));
                    }
                } catch (Exception e) {
                    System.out.println("Error with query\n" + tw.getQuery());
                    e.printStackTrace();
                    return new ResultWrapper(tw.getQuery(), e.getMessage());
                }
                if (tw.getAlternativeQuery() != null) {
                    try {
                        Query query = QueryFactory.create(tw.getAlternativeQuery());
                        QueryExecution qexec = QueryExecutionFactory.sparqlService(tw.getEndPoint(), query);
                        ResultSet rs = qexec.execSelect();
                        for (; rs.hasNext();) {
                            QuerySolution qs = rs.next();
                            RDFNode node = qs.get("count" + tw.getValueVariable());
                            String label = node.asLiteral().getLexicalForm();
                            String sortingValue = null;
                            if (tw.getSortingVariable() != null) {
                                RDFNode sortingNode = qs.get(tw.getSortingVariable());
                                if (sortingNode != null) {
                                    sortingValue = sortingNode.asLiteral().getLexicalForm();
                                }
                            }
                            res.add(new ResultObject(label, null, null, null, " ", null, "overall count of distinct " + tw.getPropertyText(), null, null, tw.getSortingPropertyText(), sortingValue));
                        }
                    } catch (Exception e) {
                        System.out.println("Error with query\n" + tw.getAlternativeQuery());
                        e.printStackTrace();
                        return new ResultWrapper(tw.getAlternativeQuery(), e.getMessage());
                    }
                }
            }
            //remove duplicates
            HashSet<String> resultUris = new HashSet<>();
            for (Iterator<ResultObject> it = res.iterator(); it.hasNext();) {
                ResultObject o = it.next();
                if (!resultUris.add(o.getId())) {
                    it.remove();
                }
            }
        }
        return new ResultWrapper(tw.getQuery(), res);
    }

    public HashMap<String, String>[] describeEntity(String label, String endpoint, int limit) {
        if (!endpoint.startsWith("http")) {
            endpoint = DEFAULT_END_POINT;
        }
        String queryString = "describe <" + label + ">";
        HashMap<String, String>[] res = new HashMap[2];
        res[0] = new HashMap<>();
        res[1] = new HashMap<>();
        TokenIndex index = new TokenIndex();

        try {
            Query query = QueryFactory.create(queryString);
            QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, query);
            Model model = qexec.execDescribe();
            for (StmtIterator it = model.listStatements(); it.hasNext();) {
                Triple t = it.next().asTriple();
                String property = t.getPredicate().getURI();
                if (!index.containsElement(property)) {
                    continue;
                }
                if (t.getSubject().getURI().equals(label)) {
                    String v;
                    if (t.getObject().isLiteral()) {
                        v = t.getObject().getLiteralLexicalForm();
                    } else {
                        v = t.getObject().getURI();
                        if (!index.containsElement(v)) {
                            continue;
                        }
                    }
                    if (v.length() > 100) {
                        v = v.substring(0, 97) + "...";
                    }
                    String curVal = res[0].get(property);
                    if (curVal == null) {
                        res[0].put(property, v);
                    } else {
                        res[0].put(property, curVal + "; " + v);
                    }
                } else {
                    String s = t.getSubject().getURI();
                    if (index.containsElement(s)) {
                        res[1].put(property, s);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error with query\ndescribe <" + label + ">");
            e.printStackTrace();
            res[0].put("Error", e.getMessage());
        }
        return res;
    }
}