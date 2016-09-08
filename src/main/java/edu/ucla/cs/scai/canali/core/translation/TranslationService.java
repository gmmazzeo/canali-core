/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.translation;

import edu.ucla.cs.scai.canali.core.autocompleter.AutocompleteObject;
import edu.ucla.cs.scai.canali.core.autocompleter.AutocompleteService;
import edu.ucla.cs.scai.canali.core.index.tokens.RankCountOperatorToken;
import edu.ucla.cs.scai.canali.core.index.tokens.RankOperatorToken;
import edu.ucla.cs.scai.canali.core.index.tokens.Token;
import edu.ucla.cs.scai.canali.core.index.tokens.TopKOperatorToken;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class TranslationService {

    public static final String TYPE_PROPERTY = "rdf:type";
    public static final String ABSTRACT_PROPERTY = "dbpedia-owl:abstract";
    public static final String LABEL_PROPERTY = "rdfs:label";
    public static final String SUBCLASSOF_PROPERTY = "rdfs:subClassOf*";
    public static final String COMMENT_PROPERTY = "rdfs:comment";
    public static final String THUMBNAIL_PROPERTY = "dbpedia-owl:thumbnail";
    public static final String URL_PROPERTY = "foaf:isPrimaryTopicOf";

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
    }

    public TranslationWrapper translateQuery(ArrayList<AutocompleteObject> states, String endpoint, int limit, boolean disableSubclass) throws Exception {
        if (states.isEmpty()) {
            throw new Exception("Empty sequence of states");
        }
        int[] stateIndex = new int[1];
        int[] variableCounter = new int[1];
        LinkedList<String> variableNameStack = new LinkedList<>();
        variableNameStack.addLast("ans");
        //the accepted symbol of the first state (that in position 0) determines the type of aggregate function
        int aggregateFunction = QueryModel.NONE;
        if (states.get(0).labels.equals("What is the count of") || states.get(0).labels.equals("Give me the count of")) {
            aggregateFunction = QueryModel.COUNT;
        }
        boolean yesNoQuestion = states.get(0).labels.startsWith("Is") || states.get(0).labels.startsWith("Are");

        //there is a function for each state
        //remember that each state contains the symbol that caused the transition from the previous state
        //therefore, the symbol that causes the transition to the following state is stored in the following state
        //contraints are created using the exit symbols
        QueryModel qm;
        switch (states.get(0).state) {
            case AutocompleteService.ACCEPT_CONCEPT_STATE_S1:
                qm = acceptConceptS1(states, stateIndex, variableCounter, variableNameStack, aggregateFunction, new HashMap<>());
                break;
            case AutocompleteService.ACCEPT_PROPERTY_FOR_CONSTRAINT_STATE_S3:
                qm = new QueryModel("ans", null, null, QueryModel.SUBJECT_TYPE, QueryModel.NONE);
                HashMap<Integer, String> variableMap = new HashMap<>();
                variableMap.put(0, "ans");
                acceptPopertyForConstraintS4(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            default:
                throw new Exception("Expected S1 or S4 in position 0");
        }
        if (!states.get(stateIndex[0]).state.equals(AutocompleteService.FINAL_STATE_SF)) {
            throw new Exception("Not all states were accepted");
        }
        //then creates sparql query and executes it
        TranslationWrapper res = translateSparql(qm, endpoint, yesNoQuestion, limit, disableSubclass);
        return res;
    }

    private QueryModel acceptConceptS1(ArrayList<AutocompleteObject> states, int[] stateIndex, int[] variableCounter, LinkedList<String> variableNameStack, int aggregateFunction, HashMap<Integer, String> variableMap) throws Exception {
        //the last variable of variableNameStack is the variable representing the concept that is going to be accepted

        //check if the state exists and if it actually the expected state
        if (stateIndex[0] >= states.size() || !states.get(stateIndex[0]).state.equals(AutocompleteService.ACCEPT_CONCEPT_STATE_S1)) {
            throw new Exception("Expected ACCEPT_CONCEPT in position " + stateIndex[0]);
        }
        //check is the next state exists
        if (stateIndex[0] + 1 >= states.size()) {
            throw new Exception("Expected ACCEPT_CONCEPT or ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION in position " + (stateIndex[0] + 1));
        }

        QueryModel qm;
        switch (states.get(stateIndex[0] + 1).tokenType) {
            case AutocompleteService.PROPERTY: //the exit-symbol is an property, thus next state is ACCEPT_CONCEPT_STATE_S1
                //the variable at the top of the stack is mapped to the state index of the accepted property
                variableMap.put(stateIndex[0] + 1, variableNameStack.getLast());

                //a new variable is introduced as subject of the accepted property
                variableCounter[0]++;
                String subjectVar = "var" + variableCounter[0];

                qm = new QueryModel(subjectVar, variableNameStack.getLast(), states.get(stateIndex[0] + 1).restrictedText, QueryModel.VALUE_TYPE, aggregateFunction);
                String aUri = states.get(stateIndex[0] + 1).labels;
                //the introduced variable is bound to the variable at the top of the stack through the accepted property
                qm.getConstraints().add(new QueryConstraint(subjectVar, aUri, variableNameStack.getLast()));
                stateIndex[0]++;
                variableNameStack.addLast(subjectVar);
                //the query model representing the subject is created
                QueryModel qm2 = acceptConceptS1(states, stateIndex, variableCounter, variableNameStack, QueryModel.NONE, variableMap);
                //and then merged
                qm.merge(qm2);
                return qm;
            case AutocompleteService.ENTITY: //the exit-symbol is an entity, thus next state is ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2
                qm = new QueryModel(variableNameStack.getLast(), null, null, QueryModel.SUBJECT_TYPE, aggregateFunction);
                qm.bindingVariablesToEntities.put(variableNameStack.removeLast(), states.get(stateIndex[0] + 1).labels); //the variable is removed because it is no longer a variable
                //qm.getConstraints().add(new QueryConstraint(variableNameStack.removeLast(), "isEntity", states.get(stateIndex[0] + 1).labels)); //the variable is removed because it is no longer a variable
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                return qm;
            case AutocompleteService.CLASS: //the exit-symbol is a class, thus next state is ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2
                //the variable at the top of the stack is mapped to the state index of the accepted class
                variableMap.put(stateIndex[0] + 1, variableNameStack.getLast());

                qm = new QueryModel(variableNameStack.getLast(), null, null, QueryModel.SUBJECT_TYPE, aggregateFunction);
                qm.getConstraints().add(new QueryConstraint(variableNameStack.getLast(), TYPE_PROPERTY, states.get(stateIndex[0] + 1).labels));
                if (states.get(stateIndex[0] + 1).keywords != null) {
                    qm.getConstraints().add(new QueryConstraint(variableNameStack.getLast(), ABSTRACT_PROPERTY, variableNameStack.getLast() + "abs"));
                    for (String k : states.get(stateIndex[0] + 1).keywords) {
                        qm.getFilters().add(new QueryFilter(variableNameStack.getLast() + "abs", "contains", k.toLowerCase(), QueryFilter.STRING));
                    }
                }
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                return qm;
            default:
                throw new Exception("Unexpected token type " + states.get(stateIndex[0] + 1).tokenType + " at state in position " + (stateIndex[0] + 1));
        }
    }

    private void acceptConstraintOrFinalPunctuationS2(QueryModel qm, ArrayList<AutocompleteObject> states, int[] stateIndex, int[] variableCounter, LinkedList<String> variableNameStack, HashMap<Integer, String> variableMap) throws Exception {
        //the last variable of variableNameStack is the variable to which the constraint will be applied

        //check if the state exists and if it actually the expected state
        if (stateIndex[0] >= states.size() || !states.get(stateIndex[0]).state.equals(AutocompleteService.ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2)) {
            throw new Exception("Expected ACCEPT_CONCEPT_OR_FINAL_PUNCTUATION in position " + stateIndex[0]);
        }
        //check is the next state exists
        if ((stateIndex[0] + 1) >= states.size()) {
            throw new Exception("Expected another state in position " + stateIndex[0] + " after a ACCEPT_CONCEPT_OR_FINAL_PUNCTUATION state");
        }

        switch (states.get(stateIndex[0] + 1).tokenType) {
            case AutocompleteService.FINAL_PUNCTUATION: //the exit-symbol is the final puntuation, thus next state is FINAL_STATE_SF
                stateIndex[0]++;
                break;
            case AutocompleteService.CONSTRAINT_PREFIX: //the exit-symbol is a constraint prefix, thus next state is ACCEPT_PROPERTY_FOR_CONSTRAINT_STATE
                stateIndex[0]++;
                acceptPopertyForConstraintS4(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.PROPERTY: //the exit-symbol is a constraint prefix, thus next state is ACCEPT_OPERATOR_OR_DIRECT_OPERAND                

                if (variableNameStack.isEmpty()) {
                    throw new Exception("No free variables for applying constraints starting at position " + (stateIndex[0] + 1));
                }
                variableCounter[0]++;
                String newVar = "var" + variableCounter[0];
                String aUri = states.get(stateIndex[0] + 1).labels;
                qm.getConstraints().add(new QueryConstraint(variableMap.get(states.get(stateIndex[0] + 1).relatedTokenPosition), aUri, newVar));
                variableNameStack.addLast(newVar);

                //the new variable is mapped to the accepted state position
                variableMap.put(stateIndex[0] + 1, newVar);

                stateIndex[0]++;
                acceptOperatorOrDirectOperandS3(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.DIRECT_OPERATOR:
                stateIndex[0]++;
                acceptDirectOperandS5(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.INDIRECT_OPERATOR:
                stateIndex[0]++;
                int attIndex = stateIndex[0] - 1;
                while (!states.get(attIndex).tokenType.equals(AutocompleteService.PROPERTY)) {
                    attIndex--;
                }
                acceptIndirectOperandS6(qm, states, stateIndex, variableCounter, variableNameStack, states.get(attIndex).labels, variableMap); //is -2 always correct?
                break;
            case AutocompleteService.UNARY_OPERATOR:
                stateIndex[0]++;
                acceptUnaryOperandS10(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            default:
                throw new Exception("Unexpected token type " + states.get(stateIndex[0] + 1).tokenType + " at state in position " + (stateIndex[0] + 1));
        }
    }

    private void acceptPopertyForConstraintS4(QueryModel qm, ArrayList<AutocompleteObject> states, int[] stateIndex, int[] variableCounter, LinkedList<String> variableNameStack, HashMap<Integer, String> variableMap) throws Exception {
        //check if the state exists and if it actually the expected state
        if (stateIndex[0] >= states.size() || !states.get(stateIndex[0]).state.equals(AutocompleteService.ACCEPT_PROPERTY_FOR_CONSTRAINT_STATE_S3)) {
            throw new Exception("Expected ACCEPT_PROPERTY_FOR_CONSTRAINT in position " + stateIndex[0]);
        }
        //check if the next state exists
        if ((stateIndex[0] + 1) >= states.size()) {
            throw new Exception("Expected another state in position " + stateIndex[0] + " after a ACCEPT_PROPERTY_FOR_CONSTRAINT state");
        }

        switch (states.get(stateIndex[0] + 1).tokenType) {
            case AutocompleteService.PROPERTY: //the exit-symbol is an property, thus next state is ACCEPT_OPERATOR_OR_DIRECT_OPERAND_STATE
                if (variableNameStack.isEmpty()) {
                    throw new Exception("No free variables for applying constraints starting at position " + (stateIndex[0] + 1));
                }
                variableCounter[0]++;
                String newVar = "var" + variableCounter[0];
                String aUri = states.get(stateIndex[0] + 1).labels;
                //OLD: qm.getConstraints().add(new QueryConstraint(variableNameStack.getLast(), aUri, newVar));
                qm.getConstraints().add(new QueryConstraint(variableMap.get(states.get(stateIndex[0] + 1).relatedTokenPosition), aUri, newVar));
                variableNameStack.addLast(newVar);

                //the new variable is mapped to the accepted state position
                variableMap.put(stateIndex[0] + 1, newVar);

                stateIndex[0]++;
                acceptOperatorOrDirectOperandS3(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.RANK_OPERATOR:
                stateIndex[0]++;
                RankOperatorToken op = null;
                try {
                    op = RankOperatorToken.fromLabel(states.get(stateIndex[0]).labels);
                } catch (Exception e) {
                    try {
                        op = RankCountOperatorToken.fromLabel(states.get(stateIndex[0]).labels);
                    } catch (Exception e2) {
                        throw new Exception("Unexpected token label " + states.get(stateIndex[0] + 1).labels + " at state in position " + (stateIndex[0] + 1));
                    }
                }
                acceptPopertyForRankS9(qm, states, stateIndex, variableCounter, variableNameStack, op, variableMap);
                break;
            case AutocompleteService.TOPK_OPERATOR:
                stateIndex[0]++;
                acceptPopertyForRankS9(qm, states, stateIndex, variableCounter, variableNameStack, TopKOperatorToken.fromLabel(states.get(stateIndex[0]).labels), variableMap);
                break;
            default:
                throw new Exception("Unexpected token type " + states.get(stateIndex[0] + 1).tokenType + " at state in position " + (stateIndex[0] + 1));
        }
    }

    private void acceptOperatorOrDirectOperandS3(QueryModel qm, ArrayList<AutocompleteObject> states, int[] stateIndex, int[] variableCounter, LinkedList<String> variableNameStack, HashMap<Integer, String> variableMap) throws Exception {
        //check if the state exists and if it actually the expected state
        if (stateIndex[0] >= states.size() || !states.get(stateIndex[0]).state.equals(AutocompleteService.ACCEPT_OPERATOR_OR_DIRECT_OPERAND_STATE_S4)) {
            throw new Exception("Expected ACCEPT_PROPERTY_FOR_CONSTRAINT in position " + stateIndex[0]);
        }
        //check if the next state exists
        if (stateIndex[0] + 1 >= states.size()) {
            throw new Exception("Expected another state in position " + (stateIndex[0] + 1) + "after an ACCEPT_OPERATOR_OR_OPERAND");
        }

        switch (states.get(stateIndex[0] + 1).tokenType) {
            case AutocompleteService.CONSTRAINT_PREFIX:
                stateIndex[0]++;
                acceptPopertyForConstraintS4(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.PROPERTY:
                variableCounter[0]++;
                String newVar2 = "var" + variableCounter[0];
                qm.getConstraints().add(new QueryConstraint(newVar2, states.get(stateIndex[0] + 1).labels, variableNameStack.removeLast()));
                variableNameStack.addLast(newVar2);

                //the new variable is mapped to the accepted state position
                variableMap.put(stateIndex[0] + 1, newVar2);

                stateIndex[0]++;
                QueryModel qm2 = acceptConceptS1(states, stateIndex, variableCounter, variableNameStack, QueryModel.NONE, variableMap);
                qm.merge(qm2);
                break;
            case AutocompleteService.CLASS:
                qm.getConstraints().add(new QueryConstraint(variableNameStack.getLast(), TYPE_PROPERTY, states.get(stateIndex[0] + 1).labels));
                if (states.get(stateIndex[0] + 1).keywords != null) {
                    qm.getConstraints().add(new QueryConstraint(variableNameStack.getLast(), ABSTRACT_PROPERTY, variableNameStack.getLast() + "abs"));
                    for (String k : states.get(stateIndex[0] + 1).keywords) {
                        qm.getFilters().add(new QueryFilter(variableNameStack.getLast() + "abs", "contains", k.toLowerCase(), QueryFilter.STRING));
                    }
                }
                //the variable at the top of the stack is mapped to the accepted state position
                variableMap.put(stateIndex[0] + 1, variableNameStack.getLast());

                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.ENTITY:
                qm.bindingVariablesToEntities.put(variableNameStack.removeLast(), states.get(stateIndex[0] + 1).labels);
                //qm.getConstraints().add(new QueryConstraint(variableNameStack.removeLast(), "isEntity", states.get(stateIndex[0] + 1).labels));
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.LITERAL_DATE:
                qm.getFilters().add(new QueryFilter(variableNameStack.removeLast(), "=", states.get(stateIndex[0] + 1).labels, QueryFilter.DATE));
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.LITERAL_STRING:
                qm.getFilters().add(new QueryFilter(variableNameStack.removeLast(), "=", states.get(stateIndex[0] + 1).restrictedText, QueryFilter.STRING));
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.LITERAL_NUMERIC:
                qm.getFilters().add(new QueryFilter(variableNameStack.removeLast(), "=", states.get(stateIndex[0] + 1).labels, QueryFilter.NUMERIC));
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.LITERAL_PERCENTAGE:
                qm.getFilters().add(new QueryFilter(variableNameStack.removeLast(), "=", states.get(stateIndex[0] + 1).labels, QueryFilter.PERCENTAGE));
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.DIRECT_OPERATOR:
                stateIndex[0]++;
                acceptDirectOperandS5(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.INDIRECT_OPERATOR:
                stateIndex[0]++;
                acceptIndirectOperandS6(qm, states, stateIndex, variableCounter, variableNameStack, states.get(stateIndex[0] - 1).labels, variableMap);
                break;
            default:
                throw new Exception("Unexpected token type " + states.get(stateIndex[0]).tokenType + " at state in position " + (stateIndex[0]));
        }
    }

    public void acceptDirectOperandS5(QueryModel qm, ArrayList<AutocompleteObject> states, int[] stateIndex, int[] variableCounter, LinkedList<String> variableNameStack, HashMap<Integer, String> variableMap) throws Exception {
        //check if the state exists and if it actually the expected state
        if (stateIndex[0] >= states.size() || !states.get(stateIndex[0]).state.equals(AutocompleteService.ACCEPT_DIRECT_OPERAND_STATE_S5)) {
            throw new Exception("Expected ACCEPT_DIRECT_OPERAND_STATE in position " + stateIndex[0]);
        }
        //check if the next state exists
        if (stateIndex[0] + 1 >= states.size()) {
            throw new Exception("Expected another state in position " + (stateIndex[0] + 1) + "after an ACCEPT_DIRECT_OPERAND_STATE");
        }

        switch (states.get(stateIndex[0] + 1).tokenType) {
            case AutocompleteService.PROPERTY:
                variableCounter[0]++;
                String newVar2 = "var" + variableCounter[0];
                if (states.get(stateIndex[0]).labels.equals("=")) {
                    String valVar = variableNameStack.removeLast();
                    qm.getConstraints().add(new QueryConstraint(newVar2, states.get(stateIndex[0] + 1).labels, valVar));

                    //the variable valVar the stack is mapped to the accepted state position
                    variableMap.put(stateIndex[0] + 1, valVar);

                    variableNameStack.addLast(newVar2);
                    stateIndex[0]++;
                    QueryModel qm2 = acceptConceptS1(states, stateIndex, variableCounter, variableNameStack, QueryModel.NONE, variableMap);
                    qm.merge(qm2);
                } else {
                    variableCounter[0]++;
                    String newVar3 = "var" + variableCounter[0];
                    qm.getConstraints().add(new QueryConstraint(newVar2, states.get(stateIndex[0] + 1).labels, newVar3));

                    //newVar3 is mapped to the accepted state position
                    variableMap.put(stateIndex[0] + 1, newVar3);

                    qm.getFilters().add(new QueryFilter(variableNameStack.removeLast(), states.get(stateIndex[0]).labels, newVar3, QueryFilter.VARIABLE));
                    variableNameStack.addLast(newVar2);
                    stateIndex[0]++;
                    QueryModel qm2 = acceptConceptS1(states, stateIndex, variableCounter, variableNameStack, QueryModel.NONE, variableMap);
                    qm.merge(qm2);
                }
                break;
            case AutocompleteService.CLASS:
                if (states.get(stateIndex[0]).labels.equals("=")) {
                    qm.getConstraints().add(new QueryConstraint(variableNameStack.getLast(), TYPE_PROPERTY, states.get(stateIndex[0] + 1).labels));
                    if (states.get(stateIndex[0] + 1).keywords != null) {
                        qm.getConstraints().add(new QueryConstraint(variableNameStack.getLast(), ABSTRACT_PROPERTY, variableNameStack.getLast() + "abs"));
                        for (String k : states.get(stateIndex[0] + 1).keywords) {
                            qm.getFilters().add(new QueryFilter(variableNameStack.getLast() + "abs", "contains", k.toLowerCase(), QueryFilter.STRING));
                        }
                    }
                    //the variable at the top of the stack is mapped to the accepted state position
                    variableMap.put(stateIndex[0] + 1, variableNameStack.getLast());

                    stateIndex[0]++;
                    acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                } else {
                    throw new Exception("Malformed constraint: a variable can not be " + states.get(stateIndex[0]).restrictedText + " " + states.get(stateIndex[0] + 1).restrictedText);
                }
                break;
            case AutocompleteService.ENTITY:
                if (states.get(stateIndex[0]).labels.equals("=")) {
                    qm.bindingVariablesToEntities.put(variableNameStack.removeLast(), states.get(stateIndex[0] + 1).labels);
                    //qm.getConstraints().add(new QueryConstraint(variableNameStack.removeLast(), "isEntity", states.get(stateIndex[0] + 1).labels));
                    stateIndex[0]++;
                    acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                } else {
                    throw new Exception("Malformed constraint: a variable can not be " + states.get(stateIndex[0]).restrictedText + " " + states.get(stateIndex[0] + 1).restrictedText);
                }
                break;
            case AutocompleteService.LITERAL_DATE:
                qm.getFilters().add(new QueryFilter(variableNameStack.removeLast(), states.get(stateIndex[0]).labels, states.get(stateIndex[0] + 1).labels, QueryFilter.DATE));
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.LITERAL_STRING:
                qm.getFilters().add(new QueryFilter(variableNameStack.removeLast(), states.get(stateIndex[0]).labels, states.get(stateIndex[0] + 1).restrictedText, QueryFilter.STRING));
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.LITERAL_NUMERIC:
                qm.getFilters().add(new QueryFilter(variableNameStack.removeLast(), states.get(stateIndex[0]).labels, states.get(stateIndex[0] + 1).labels, QueryFilter.NUMERIC));
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.LITERAL_PERCENTAGE:
                qm.getFilters().add(new QueryFilter(variableNameStack.removeLast(), states.get(stateIndex[0]).labels, states.get(stateIndex[0] + 1).labels, QueryFilter.PERCENTAGE));
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.POSSESSIVE_DETERMINER:
                String operator = states.get(stateIndex[0]).labels;
                stateIndex[0]++;
                acceptSelfPopertyAsDirectOperandS7(qm, states, stateIndex, variableCounter, variableNameStack, operator, variableMap);
                break;
            default:
                throw new Exception("Unexpected token type " + states.get(stateIndex[0]).tokenType + " at state in position " + (stateIndex[0]));
        }
    }

    public void acceptIndirectOperandS6(QueryModel qm, ArrayList<AutocompleteObject> states, int[] stateIndex, int[] variableCounter, LinkedList<String> variableNameStack, String property, HashMap<Integer, String> variableMap) throws Exception {
        //check if the state exists and if it actually the expected state
        if (stateIndex[0] >= states.size() || !states.get(stateIndex[0]).state.equals(AutocompleteService.ACCEPT_INDIRECT_OPERAND_STATE_S6)) {
            throw new Exception("Expected ACCEPT_INDIRECT_OPERAND_STATE in position " + stateIndex[0]);
        }
        //check if the next state exists
        if (stateIndex[0] + 1 >= states.size()) {
            throw new Exception("Expected another state in position " + (stateIndex[0] + 1) + "after an ACCEPT_INDIRECT_OPERAND_STATE");
        }

        //the variable at the top of the stack is mapped to the accepted state position
        variableMap.put(stateIndex[0] + 1, variableNameStack.getLast()); //???

        switch (states.get(stateIndex[0] + 1).tokenType) {
            case AutocompleteService.PROPERTY:
                variableCounter[0]++;
                String propertyVar = "var" + variableCounter[0];
                variableCounter[0]++;
                String propertyEntityVar = "var" + variableCounter[0];
                qm.getConstraints().add(new QueryConstraint(propertyEntityVar, states.get(stateIndex[0] + 1).labels, propertyVar));

                //the variable propertyVar is mapped to the accepted state position
                variableMap.put(stateIndex[0] + 1, propertyVar);

                if (states.get(stateIndex[0]).labels.equals("=")) {
                    String valVar = variableNameStack.removeLast();
                    qm.getConstraints().add(new QueryConstraint(propertyVar, property, valVar));
                } else {
                    variableCounter[0]++;
                    String newVar3 = "var" + variableCounter[0];
                    qm.getConstraints().add(new QueryConstraint(propertyVar, property, newVar3));
                    qm.getFilters().add(new QueryFilter(variableNameStack.removeLast(), states.get(stateIndex[0]).labels, newVar3, QueryFilter.VARIABLE));
                }
                variableNameStack.addLast(propertyEntityVar);
                stateIndex[0]++;
                QueryModel qm2 = acceptConceptS1(states, stateIndex, variableCounter, variableNameStack, QueryModel.NONE, variableMap);
                qm.merge(qm2);
                break;
            case AutocompleteService.CLASS:
                variableCounter[0]++;
                String classVar = "var" + variableCounter[0];
                qm.getConstraints().add(new QueryConstraint(classVar, TYPE_PROPERTY, states.get(stateIndex[0] + 1).labels));
                if (states.get(stateIndex[0] + 1).keywords != null) {
                    qm.getConstraints().add(new QueryConstraint(variableNameStack.getLast(), ABSTRACT_PROPERTY, variableNameStack.getLast() + "abs"));
                    for (String k : states.get(stateIndex[0] + 1).keywords) {
                        qm.getFilters().add(new QueryFilter(variableNameStack.getLast() + "abs", "contains", k.toLowerCase(), QueryFilter.STRING));
                    }
                }
                //the variable classVar is mapped to the accepted state position
                variableMap.put(stateIndex[0] + 1, classVar);

                if (states.get(stateIndex[0]).labels.equals("=")) {
                    qm.getConstraints().add(new QueryConstraint(classVar, property, variableNameStack.getLast()));
                } else {
                    variableCounter[0]++;
                    String var2 = "var" + variableCounter[0];
                    qm.getConstraints().add(new QueryConstraint(classVar, property, var2));
                    qm.getFilters().add(new QueryFilter(variableNameStack.getLast(), states.get(stateIndex[0]).labels, var2, QueryFilter.VARIABLE));
                }
                variableNameStack.addLast(classVar);
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.ENTITY:
                variableCounter[0]++;
                String entityVar = "var" + variableCounter[0];
                qm.bindingVariablesToEntities.put(entityVar, states.get(stateIndex[0] + 1).labels);
                //qm.getConstraints().add(new QueryConstraint(entityVar, "isEntity", states.get(stateIndex[0] + 1).labels));
                if (states.get(stateIndex[0]).labels.equals("=")) {
                    qm.getConstraints().add(new QueryConstraint(entityVar, property, variableNameStack.getLast()));
                } else {
                    variableCounter[0]++;
                    String var2 = "var" + variableCounter[0];
                    qm.getConstraints().add(new QueryConstraint(entityVar, property, var2));
                    qm.getFilters().add(new QueryFilter(variableNameStack.getLast(), states.get(stateIndex[0]).labels, var2, QueryFilter.VARIABLE));
                }
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            case AutocompleteService.POSSESSIVE_DETERMINER:
                String operator = states.get(stateIndex[0]).labels;
                stateIndex[0]++;
                acceptSelfPopertyAsIndirectOperandS8(qm, states, stateIndex, variableCounter, variableNameStack, property, operator, variableMap);
                break;
            default:
                throw new Exception("Unexpected token type " + states.get(stateIndex[0]).tokenType + " at state in position " + (stateIndex[0]));
        }
    }

    //rank < 0 means ascending order
    public void acceptPopertyForRankS9(QueryModel qm, ArrayList<AutocompleteObject> states, int[] stateIndex, int[] variableCounter, LinkedList<String> variableNameStack, Token e, HashMap<Integer, String> variableMap) throws Exception {
        //check if the state exists and if it actually the expected state
        if (stateIndex[0] >= states.size() || !states.get(stateIndex[0]).state.equals(AutocompleteService.ACCEPT_PROPERTY_FOR_RANK_STATE_S9)) {
            throw new Exception("Expected ACCEPT_PROPERTY_FOR_RANK_STATE in position " + stateIndex[0]);
        }
        //check if the next state exists
        if (stateIndex[0] + 1 >= states.size()) {
            throw new Exception("Expected another state in position " + (stateIndex[0] + 1) + "after an ACCEPT_PROPERTY_FOR_RANK_STATE");
        }

        switch (states.get(stateIndex[0] + 1).tokenType) {
            case AutocompleteService.PROPERTY:
                variableCounter[0]++;
                String var2 = "var" + variableCounter[0];
                qm.getConstraints().add(new QueryConstraint(variableMap.get(states.get(stateIndex[0] + 1).relatedTokenPosition), states.get(stateIndex[0] + 1).labels, var2));
                if (e instanceof RankOperatorToken) {
                    RankOperatorToken r = (RankOperatorToken) e;
                    qm.setSortingAndLimit(var2, states.get(stateIndex[0] + 1).restrictedText, r.getSmallest(), 1, r.getRank() - 1);
                } else if (e instanceof TopKOperatorToken) {
                    TopKOperatorToken t = (TopKOperatorToken) e;
                    qm.setSortingAndLimit(var2, states.get(stateIndex[0] + 1).restrictedText, t.getAscending(), t.getK(), 0);
                } else {
                    throw new Exception("Invalid operator - topK or rank expected");
                }

                //the variable var2 is mapped to the accepted state position
                variableMap.put(stateIndex[0] + 1, var2);

                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            default:
                throw new Exception("Unexpected token type " + states.get(stateIndex[0] + 1).tokenType + " at state in position " + (stateIndex[0] + 1));
        }
    }

    public void acceptSelfPopertyAsDirectOperandS7(QueryModel qm, ArrayList<AutocompleteObject> states, int[] stateIndex, int[] variableCounter, LinkedList<String> variableNameStack, String operator, HashMap<Integer, String> variableMap) throws Exception {
        //check if the state exists and if it is actually the expected state
        if (stateIndex[0] >= states.size() || !states.get(stateIndex[0]).state.equals(AutocompleteService.ACCEPT_SELF_PROPERTY_AS_DIRECT_OPERAND_STATE_S7)) {
            throw new Exception("Expected ACCEPT_SELF_PROPERTY_AS_DIRECT_OPERAND in position " + stateIndex[0]);
        }

        //check if the next state exists
        if (stateIndex[0] + 1 >= states.size()) {
            throw new Exception("Expected another state in position " + (stateIndex[0] + 1) + "after an ACCEPT_SELF_PROPERTY_AS_DIRECT_OPERAND");
        }

        switch (states.get(stateIndex[0] + 1).tokenType) {
            case AutocompleteService.PROPERTY:
                String valueVar = variableNameStack.removeLast();
                //OLD: String subjVar = variableNameStack.getLast();
                String subjVar = variableMap.get(states.get(stateIndex[0] + 1).relatedTokenPosition);
                if (operator.equals("=")) {
                    qm.getConstraints().add(new QueryConstraint(subjVar, states.get(stateIndex[0] + 1).labels, valueVar));

                    //the variable valueVar is mapped to the accepted state position
                    variableMap.put(stateIndex[0] + 1, valueVar);

                    variableNameStack.addLast(valueVar);
                } else {
                    variableCounter[0]++;
                    String newVar3 = "var" + variableCounter[0];
                    qm.getConstraints().add(new QueryConstraint(subjVar, states.get(stateIndex[0] + 1).labels, newVar3));

                    //the variable newVar3 is mapped to the accepted state position
                    variableMap.put(stateIndex[0] + 1, variableNameStack.getLast());

                    qm.getFilters().add(new QueryFilter(valueVar, operator, newVar3, QueryFilter.VARIABLE));
                    variableNameStack.addLast(newVar3);
                }
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            default:
                throw new Exception("Unexpected token type " + states.get(stateIndex[0]).tokenType + " at state in position " + (stateIndex[0]));
        }
    }

    public void acceptSelfPopertyAsIndirectOperandS8(QueryModel qm, ArrayList<AutocompleteObject> states, int[] stateIndex, int[] variableCounter, LinkedList<String> variableNameStack, String property, String operator, HashMap<Integer, String> variableMap) throws Exception {
        //check if the state exists and if it is actually the expected state
        if (stateIndex[0] >= states.size() || !states.get(stateIndex[0]).state.equals(AutocompleteService.ACCEPT_SELF_PROPERTY_AS_INDIRECT_OPERAND_STATE_S8)) {
            throw new Exception("Expected ACCEPT_SELF_PROPERTY_AS_INDIRECT_OPERAND in position " + stateIndex[0]);
        }

        //check if the next state exists
        if (stateIndex[0] + 1 >= states.size()) {
            throw new Exception("Expected another state in position " + (stateIndex[0] + 1) + "after an ACCEPT_SELF_PROPERTY_AS_INDIRECT_OPERAND");
        }

        switch (states.get(stateIndex[0] + 1).tokenType) {
            case AutocompleteService.PROPERTY:
                String valueVar = variableNameStack.removeLast();
                //OLD: String subjVar = variableNameStack.getLast();
                String subjVar = variableMap.get(states.get(stateIndex[0] + 1).relatedTokenPosition);
                variableCounter[0]++;
                String newVar2 = "var" + variableCounter[0];
                qm.getConstraints().add(new QueryConstraint(subjVar, states.get(stateIndex[0] + 1).labels, newVar2));
                if (operator.equals("=")) {
                    qm.getConstraints().add(new QueryConstraint(newVar2, property, valueVar));

                    //the variable valueVar is mapped to the accepted state position
                    variableMap.put(stateIndex[0] + 1, valueVar);
                } else {
                    variableCounter[0]++;
                    String newVar3 = "var" + variableCounter[0];
                    qm.getConstraints().add(new QueryConstraint(newVar2, property, newVar3));

                    //the variable newVar3 is mapped to the accepted state position
                    variableMap.put(stateIndex[0] + 1, newVar3);
                    qm.getFilters().add(new QueryFilter(valueVar, operator, newVar3, QueryFilter.VARIABLE));
                }
                variableNameStack.addLast(newVar2);
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            default:
                throw new Exception("Unexpected token type " + states.get(stateIndex[0]).tokenType + " at state in position " + (stateIndex[0]));
        }
    }

    public void acceptUnaryOperandS10(QueryModel qm, ArrayList<AutocompleteObject> states, int[] stateIndex, int[] variableCounter, LinkedList<String> variableNameStack, HashMap<Integer, String> variableMap) throws Exception {
        //check if the state exists and if it actually the expected state
        if (stateIndex[0] >= states.size() || !states.get(stateIndex[0]).state.equals(AutocompleteService.ACCEPT_PROPERTY_FOR_UNARY_OPERATOR_S10)) {
            throw new Exception("Expected ACCEPT_PROPERTY_FOR_UNARY_OPERATOR_S10 in position " + stateIndex[0]);
        }
        //check if the next state exists
        if (stateIndex[0] + 1 >= states.size()) {
            throw new Exception("Expected another state in position " + (stateIndex[0] + 1) + "after an ACCEPT_PROPERTY_FOR_UNARY_OPERATOR_S10");
        }

        switch (states.get(stateIndex[0] + 1).tokenType) {
            case AutocompleteService.PROPERTY:
                variableCounter[0]++;
                String newVar2 = "var" + variableCounter[0];
                variableNameStack.addLast(newVar2);
                variableMap.put(stateIndex[0] + 1, newVar2);
                if (states.get(stateIndex[0]).labels.equals("not exists")) {
                    qm.getConstraints().add(new NegatedQueryConstraint(variableMap.get(states.get(stateIndex[0] + 1).relatedTokenPosition), states.get(stateIndex[0] + 1).labels, newVar2));
                } else if (states.get(stateIndex[0]).labels.equals("exists")) { //
                    qm.getConstraints().add(new QueryConstraint(variableMap.get(states.get(stateIndex[0] + 1).relatedTokenPosition), states.get(stateIndex[0] + 1).labels, newVar2));
                } else {
                    throw new Exception("Unexpected label " + states.get(stateIndex[0]).labels + " at state in position " + (stateIndex[0]));
                }
                stateIndex[0]++;
                acceptConstraintOrFinalPunctuationS2(qm, states, stateIndex, variableCounter, variableNameStack, variableMap);
                break;
            default:
                throw new Exception("Unexpected token type " + states.get(stateIndex[0] + 1).tokenType + " at state in position " + (stateIndex[0] + 1));
        }
    }

    private StringBuilder getNewQueryStringBuilder() {
        StringBuilder sb = new StringBuilder("");
        for (Map.Entry<String, String> e : prefixes.entrySet()) {
            sb.append("PREFIX ").append(e.getKey()).append("\t<").append(e.getValue()).append(">\n");
        }
        sb.append("\n");
        return sb;
    }

    /*
     / Simplify isEntity constraints and returns, when possibile, the entity corresponding to the entity variable
     */
    public String simplifyIsEntiy(QueryModel qm) {
        /*
         HashMap<String, String> mappedEntities = new HashMap<>();
         for (Iterator<QueryConstraint> it = qm.getConstraints().iterator(); it.hasNext();) {
         QueryConstraint qc = it.next();
         if (qc.getPoperty().equals("isEntity")) {
         mappedEntities.put(qc.getSubject(), qc.getValue());
         it.remove();
         }
         }
         for (QueryConstraint qc : qm.getConstraints()) {
         String e = mappedEntities.get(qc.getSubject());
         if (e != null) {
         qc.setSubject(e);
         }
         e = mappedEntities.get(qc.getValue());
         if (e != null) {
         qc.setValue(e);
         }
         }
         return mappedEntities.get(qm.getEntityVariable());
         */
        return qm.bindingVariablesToEntities.get(qm.getEntityVariable());
    }

    private String convertUriOrVariableToString(String s, HashMap<String, String> bindingVariablesToEntities, String entityVariable, String valueVariable) {
        if (s == null) {
            return null;
        }
        if (!s.startsWith("http")) {
            //check if starts with a prefix
            for (String p : prefixes.keySet()) {
                if (s.startsWith(p)) {
                    return s;
                }
            }
            //it is a variable
            if (bindingVariablesToEntities.containsKey(s) && !s.equals(entityVariable) && !s.equals(valueVariable)) {
                return "<" + bindingVariablesToEntities.get(s) + ">";
            }
            return "?" + s;
        }
        //starts with http
        for (Map.Entry<String, String> e : inversePrefixes.entrySet()) {
            if (s.startsWith(e.getKey())) {
                return s.replace(e.getKey(), e.getValue());
            }
        }
        return "<" + s + ">";
    }
    
    private String convertUriOrVariableToString(String s) {
        if (s == null) {
            return null;
        }
        if (!s.startsWith("http")) {
            //check if starts with a prefix
            for (String p : prefixes.keySet()) {
                if (s.startsWith(p)) {
                    return s;
                }
            }
            //it is a variable
            return "?" + s;
        }
        //starts with http
        for (Map.Entry<String, String> e : inversePrefixes.entrySet()) {
            if (s.startsWith(e.getKey())) {
                return s.replace(e.getKey(), e.getValue());
            }
        }
        return "<" + s + ">";
    }
    

    private String convertPopertyUrisToString(String s) {
        if (s == null) {
            return null;
        }
        String[] ss = s.split("\\|");
        String res = convertUriOrVariableToString(ss[0]);
        for (int i = 1; i < ss.length; i++) {
            res += "|" + convertUriOrVariableToString(ss[i]);
        }
        return res;
    }

    private void fillStringBuilderWithConstraint(StringBuilder sb, QueryConstraint qc, boolean disableSubClass, HashMap<String, String> bindingVariablesToEntities, String entityVariable, String valueVariable) {
        if (qc instanceof NegatedQueryConstraint) {
            sb.append("\tFILTER NOT EXISTS {");
        } else {
            sb.append("\t");
        }
        if (qc.getProperty().equals(TYPE_PROPERTY)) { //it is a constraint specifying the rdf:type of the subject
            if (disableSubClass) {
                sb
                        //the subject must be of the type specified by the value
                        .append(convertUriOrVariableToString(qc.getSubject(), bindingVariablesToEntities, entityVariable, valueVariable))
                        .append(" ")
                        .append(convertUriOrVariableToString(qc.getProperty()))
                        .append(" ")
                        .append(convertUriOrVariableToString(qc.getValue()));
            } else {
                typeCounter++;
                String typeVariable = "type" + typeCounter;
                sb
                        //the subject must be of the type specified by the value or of a type that is subClassOf the value
                        .append(convertUriOrVariableToString(qc.getSubject(), bindingVariablesToEntities, entityVariable, valueVariable))
                        .append(" ")
                        .append(convertUriOrVariableToString(qc.getProperty()))
                        .append(" ")
                        .append(convertUriOrVariableToString(typeVariable))
                        .append(". ")
                        .append(convertUriOrVariableToString(typeVariable))
                        .append(" ")
                        .append(convertUriOrVariableToString(SUBCLASSOF_PROPERTY)) //improve this by creating a constant string
                        .append(" ")
                        .append(convertUriOrVariableToString(qc.getValue()));
            }
        } else { //it is a general property
            if (qc.getInvertedProperty() != null) {
                sb
                        .append("\n\t\t{")
                        .append(convertUriOrVariableToString(qc.getSubject(), bindingVariablesToEntities, entityVariable, valueVariable))
                        .append(" ")
                        .append(convertPopertyUrisToString(qc.getProperty()))
                        .append(" ")
                        .append(convertUriOrVariableToString(qc.getValue(), bindingVariablesToEntities, entityVariable, valueVariable))
                        .append("}\n\t\tUNION\n\t\t{")
                        .append(convertUriOrVariableToString(qc.getValue(), bindingVariablesToEntities, entityVariable, valueVariable))
                        .append(" ")
                        .append(convertPopertyUrisToString(qc.getInvertedProperty()))
                        .append(" ")
                        .append(convertUriOrVariableToString(qc.getSubject(), bindingVariablesToEntities, entityVariable, valueVariable))
                        .append("}");
            } else {
                sb
                        .append(convertUriOrVariableToString(qc.getSubject(), bindingVariablesToEntities, entityVariable, valueVariable))
                        .append(" ")
                        .append(convertPopertyUrisToString(qc.getProperty()))
                        .append(" ")
                        .append(convertUriOrVariableToString(qc.getValue(), bindingVariablesToEntities, entityVariable, valueVariable));
            }
            if (qc instanceof NegatedQueryConstraint) {
                sb.append("}");
            } else {
                sb.append("");
            }
        }
    }

    private String toString(RDFNode node) {
        if (node == null) {
            return "null";
        }
        if (node.isLiteral()) {
            Literal lNode = node.asLiteral();
            return lNode.getValue().toString();
        } else {
            Resource rNode = node.asResource();
            return rNode.getURI().replace("http://dbpedia.org/resource/", "").replace("_", " ");
        }
    }

    private void extendWithOrderByAndLimt(QueryModel qm, StringBuilder sb, int limit) {
        if (qm.sortingVariable != null) {
            sb.append("\n");
            if (qm.ascendingSorting) {
                sb.append("ORDER BY ?").append(qm.sortingVariable).append(" \n");
            } else {
                sb.append("ORDER BY DESC(?").append(qm.sortingVariable).append(") \n");
            }
            if (qm.offset != null && qm.offset > 0) {
                sb.append("OFFSET ").append(qm.offset).append(" \n");
            }
            sb.append("LIMIT ").append(qm.limit);
        } else {
            sb.append("\nLIMIT ").append(limit);
        }
    }

    private void createBindings(StringBuilder sb, QueryModel qm) {
        for (Map.Entry<String, String> binding : qm.bindingVariablesToEntities.entrySet()) {
            if (binding.getKey().equals(qm.getEntityVariable()) || binding.getKey().equals(qm.getValueVariable())) {
                sb
                        .append("\tBIND(")
                        .append(convertUriOrVariableToString(binding.getValue()))
                        .append(" AS ")
                        .append(convertUriOrVariableToString(binding.getKey()))
                        .append(")")
                        .append(" .\n");
            }
        }
    }

    private String replaceBindings(String q, QueryModel qm) {
        for (Map.Entry<String, String> binding : qm.bindingVariablesToEntities.entrySet()) {
            //q = q.replace(convertUriOrVariableToString(binding.getKey()), convertUriOrVariableToString(binding.getValue()));
        }
        return q;
    }

    public TranslationWrapper translateSparql(QueryModel qm, String endpoint, boolean yesNoQuestion, int limit, boolean disableSubclass) {

        StringBuilder sb = getNewQueryStringBuilder();
        String queryString = null;

        if (yesNoQuestion) {
            sb.append("ASK ");
            sb.append("\n");
            sb.append("WHERE {\n");
            createBindings(sb, qm);
            for (QueryConstraint qc : qm.getConstraints()) {
                fillStringBuilderWithConstraint(sb, qc, disableSubclass, qm.bindingVariablesToEntities, qm.entityVariable, qm.valueVariable);
                sb.append(" . \n");
            }
            for (QueryFilter qf : qm.getFilters()) {
                qf.fillStringBuilder(sb);
                sb.append(" .\n");
            }
            sb.append("}");
            queryString = sb.toString();
            //queryString=replaceBindings(queryString, qm);
            return new TranslationWrapper(queryString, endpoint, yesNoQuestion, QueryModel.NONE, null, null, null, null, null);
        } else {
            if (qm.aggregateFunction == QueryModel.NONE) { //qm.getEntityVariable() is always defined, qm.getValueVariable() is undefined for queries whose result is a set of subjects of triples, is undefined for queries whose result is a set of values of triples
                //if (qm.bindingVariablesToEntities.containsKey(qm.getEntityVariable())) {
                //    sb.append("SELECT DISTINCT <").append(qm.bindingVariablesToEntities.get(qm.getEntityVariable()));
                //    sb.append("> AS ?").append(qm.getEntityVariable());
                //} else {
                sb.append("SELECT DISTINCT ?").append(qm.getEntityVariable());
                //}                
                if (qm.getValueVariable() != null) {
                    sb.append(" ?elabel ?eurl ?").append(qm.getValueVariable());
                }
                sb.append(" ?label ?url ?picture ?abstract ");
                if (qm.sortingVariable != null) {
                    sb.append("?").append(qm.sortingVariable);
                }
                sb.append("\n");
                StringBuilder sbf = new StringBuilder();
                sbf.append("WHERE {\n");
                createBindings(sbf, qm);
                for (QueryConstraint qc : qm.getConstraints()) {
                    fillStringBuilderWithConstraint(sbf, qc, disableSubclass, qm.bindingVariablesToEntities, qm.entityVariable, qm.valueVariable);
                    sbf.append(" . \n");
                }
                sbf.append("\tOPTIONAL {?").append(qm.getValueVariable() != null ? qm.getValueVariable() : qm.getEntityVariable());
                //sb.append(" " + LABEL_PROPERTY + " ?label. FILTER(langMatches(lang(?label), \"EN\"))}. \n");
                sbf.append(" " + LABEL_PROPERTY + " ?label. FILTER(!bound(?label) || LANG(?label) = \"\" || LANGMATCHES(LANG(?label), \"en\"))} . \n");
                sbf.append("\tOPTIONAL {?").append(qm.getValueVariable() != null ? qm.getValueVariable() : qm.getEntityVariable());
                sbf.append(" " + THUMBNAIL_PROPERTY + " ?picture}. \n");
                sbf.append("\tOPTIONAL {?").append(qm.getValueVariable() != null ? qm.getValueVariable() : qm.getEntityVariable());
                sbf.append(" " + URL_PROPERTY + " ?url. FILTER(regex(?url,'^http://en.wikipedia.org/wiki/','i'))} . \n");
                sbf.append("\tOPTIONAL {?").append(qm.getValueVariable() != null ? qm.getValueVariable() : qm.getEntityVariable());
                sbf.append(" " + COMMENT_PROPERTY + " ?abstract. FILTER(langMatches(lang(?abstract), \"EN\"))} . \n");
                if (qm.getValueVariable() != null) {
                    sbf.append("\tOPTIONAL {?").append(qm.getEntityVariable());
                    //sb.append(" " + LABEL_PROPERTY + " ?elabel. FILTER(langMatches(lang(?elabel), \"EN\"))}. \n");
                    sbf.append(" " + LABEL_PROPERTY + " ?elabel. FILTER(!bound(?elabel) || LANG(?elabel) = \"\" || LANGMATCHES(LANG(?elabel), \"en\"))} . \n");
                    sbf.append("\tOPTIONAL {?").append(qm.getEntityVariable());
                    sbf.append(" " + URL_PROPERTY + " ?eurl. FILTER(regex(?eurl,'^http://en.wikipedia.org/wiki/','i'))} . \n");
                }
                for (QueryFilter qf : qm.getFilters()) {
                    qf.fillStringBuilder(sbf);
                    sbf.append(" .\n");
                }
                sbf.append("}");
                extendWithOrderByAndLimt(qm, sbf, limit);
                queryString = sbf.toString();
                //queryString=replaceBindings(queryString, qm);
                sb.append(queryString);
                queryString = sb.toString();
                return new TranslationWrapper(queryString, endpoint, false, QueryModel.NONE, qm.getValueVariable(), qm.getEntityVariable(), qm.sortingVariable, qm.getPopertyText(), qm.sortingPopertyText);
            } else if (qm.aggregateFunction == QueryModel.COUNT) {
                StringBuilder sb2 = null;
                if (qm.getValueVariable() != null) {
                    sb.append("SELECT (COUNT(DISTINCT ?").append(qm.getValueVariable()).append(") AS ?count").append(qm.getValueVariable()).append(") ?").append(qm.getEntityVariable()).append(" ?elabel ?eurl ");
                    sb2 = getNewQueryStringBuilder(); //compute also the count without aggregation
                    sb2.append("SELECT (COUNT(DISTINCT ?").append(qm.getValueVariable()).append(") AS ?count").append(qm.getValueVariable()).append(") ");
                } else {
                    sb.append("SELECT (COUNT(DISTINCT ?").append(qm.getEntityVariable()).append(") AS ?count").append(qm.getEntityVariable()).append(") ");
                }
                if (qm.sortingVariable != null) {
                    sb.append("?").append(qm.sortingVariable);
                }
                sb.append("\n");
                sb.append("WHERE {\n");
                createBindings(sb, qm);
                for (QueryConstraint qc : qm.getConstraints()) {
                    fillStringBuilderWithConstraint(sb, qc, disableSubclass, qm.bindingVariablesToEntities, qm.entityVariable, qm.valueVariable);
                    sb.append(" . \n");
                }
                if (qm.getValueVariable() != null) {
                    sb.append("\tOPTIONAL {?").append(qm.getEntityVariable());
                    sb.append(" " + LABEL_PROPERTY + " ?elabel. FILTER(langMatches(lang(?elabel), \"EN\"))}. \n");
                    sb.append("\tOPTIONAL {?").append(qm.getEntityVariable());
                    sb.append(" " + URL_PROPERTY + " ?eurl. FILTER(regex(?eurl,'^http://en.wikipedia.org/wiki/','i'))}. \n");
                }
                for (QueryFilter qf : qm.getFilters()) {
                    qf.fillStringBuilder(sb);
                    sb.append(" .\n");
                }
                sb.append("}\n");
                if (qm.getValueVariable() != null) {
                    sb.append("GROUP BY ?").append(qm.getEntityVariable()).append(" ?elabel ?eurl \n");
                }
                extendWithOrderByAndLimt(qm, sb, limit);
                queryString = sb.toString();
                //queryString=replaceBindings(queryString, qm);
                if (sb2 != null) {
                    if (qm.sortingVariable != null) {
                        sb2.append("?").append(qm.sortingVariable);
                    }
                    sb2.append("\n");
                    sb2.append("WHERE {\n");
                    createBindings(sb2, qm);
                    for (QueryConstraint qc : qm.getConstraints()) {
                        fillStringBuilderWithConstraint(sb2, qc, disableSubclass, qm.bindingVariablesToEntities, qm.entityVariable, qm.valueVariable);
                        sb2.append(" . \n");
                    }
                    for (QueryFilter qf : qm.getFilters()) {
                        qf.fillStringBuilder(sb2);
                        sb2.append(" .\n");
                    }
                    sb2.append("}\n");
                    extendWithOrderByAndLimt(qm, sb2, limit);
                    String queryString2 = sb2.toString();
                    //queryString2=replaceBindings(queryString2, qm);
                    queryString += "\n\n" + queryString2;
                    return new TranslationWrapper(queryString, endpoint, false, QueryModel.COUNT, qm.getValueVariable(), qm.getEntityVariable(), qm.sortingVariable, qm.getPopertyText(), qm.sortingPopertyText, queryString2);
                } else {
                    return new TranslationWrapper(queryString, endpoint, false, QueryModel.COUNT, qm.getValueVariable(), qm.getEntityVariable(), qm.sortingVariable, qm.getPopertyText(), qm.sortingPopertyText);
                }
            }
        }
        return new TranslationWrapper("Query translation failed");
    }
}
