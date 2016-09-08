/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.autocompleter;

import edu.ucla.cs.scai.canali.core.index.tokens.PropertyToken;
import edu.ucla.cs.scai.canali.core.index.tokens.ClassToken;
import edu.ucla.cs.scai.canali.core.index.tokens.ConstraintPrefixToken;
import edu.ucla.cs.scai.canali.core.index.tokens.EntityToken;
import edu.ucla.cs.scai.canali.core.index.tokens.IndexedToken;
import edu.ucla.cs.scai.canali.core.index.tokens.LiteralDateToken;
import edu.ucla.cs.scai.canali.core.index.tokens.LiteralToken;
import edu.ucla.cs.scai.canali.core.index.tokens.LiteralNumericToken;
import edu.ucla.cs.scai.canali.core.index.tokens.LiteralStringToken;
import edu.ucla.cs.scai.canali.core.index.TokenIndex;
import edu.ucla.cs.scai.canali.core.index.tokens.AugmentedClassToken;
import edu.ucla.cs.scai.canali.core.index.tokens.DirectBinaryOperatorToken;
import edu.ucla.cs.scai.canali.core.index.tokens.IndirectBinaryOperatorToken;
import edu.ucla.cs.scai.canali.core.index.tokens.PossessiveDeterminerToken;
import edu.ucla.cs.scai.canali.core.index.tokens.QuestionStartToken;
import edu.ucla.cs.scai.canali.core.index.tokens.RankOperatorToken;
import edu.ucla.cs.scai.canali.core.index.tokens.Token;
import edu.ucla.cs.scai.canali.core.index.tokens.TopKOperatorToken;
import edu.ucla.cs.scai.canali.core.index.tokens.UnaryOperatorToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * Given a query Q, consisting of m words, q1,...,qm
 * we try to segment Q and assign each segment to a Token
 * consistently with the grammar and the KB
 * The problem is solved using dynamic programming
 * F(i,k) is the optimal segmentation of the first i words of Q
 * into k segments (i>=k)
 * F(i,1) is the maximum similarity between an acceptable token and q1,..,qi
 * We denote with sim(i,j) the maximum similarity between an acceptable token and 
 * qi,...,qj (1<=i<=j<=m)
 * Then, F(1,k) = sim(1,i)
 * For i>1, we can recursively compute F as
 * F(i,k) = max{F(l,k-1) + sim(l+1,i)}, with k-1<=l<i
 * Observe that we
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class SegmentationService {

    public static final String INITIAL_STATE_S0 = "0",
            FINAL_STATE_SF = "f",
            ACCEPT_CONCEPT_STATE_S1 = "1",
            ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2 = "2",
            ACCEPT_OPERATOR_OR_DIRECT_OPERAND_STATE_S3 = "3",
            ACCEPT_PROPERTY_FOR_CONSTRAINT_STATE_S4 = "4",
            ACCEPT_DIRECT_OPERAND_STATE_S5 = "5",
            ACCEPT_INDIRECT_OPERAND_STATE_S6 = "6",
            ACCEPT_SELF_PROPERTY_AS_DIRECT_OPERAND_STATE_S7 = "7",
            ACCEPT_SELF_PROPERTY_AS_INDIRECT_OPERAND_STATE_S8 = "8",
            ACCEPT_PROPERTY_FOR_RANK_STATE_S9 = "9",
            ACCEPT_PROPERTY_FOR_UNARY_OPERATOR_S10 = "10";

    public static final String CLASS = "class", ENTITY = "entity",
            PROPERTY = "property",
            DIRECT_OPERATOR = "direct_operator",
            INDIRECT_OPERATOR = "indirect_operator",
            CONSTRAINT_PREFIX = "constraint_prefix",
            FINAL_PUNCTUATION = "final_punctuation",
            LITERAL_NUMERIC = "literal_numeric",
            LITERAL_DATE = "literal_date", LITERAL_STRING = "literal_string",
            LITERAL_PERCENTAGE = "literal_percentage",
            QUESTION_START = "question_start",
            POSSESSIVE_DETERMINER = "possessive_determiner",
            TOPK_OPERATOR = "topk_operator",
            RANK_OPERATOR = "rank_operator",
            UNARY_OPERATOR = "unary_operator",
            TEXT = "text";

    private LinkedList<AutocompleteObject> extendPropertyWithContext(PropertyToken property, String nextState, String[] openVariablesUri, Integer[] openVariablesPosition) {
        LinkedList<AutocompleteObject> res = new LinkedList<>();
        if (openVariablesUri != null) {
            for (int i = 0; i < openVariablesUri.length; i++) {
                String[] domains = openVariablesUri[i].split("\\|");
                for (String domain : domains) {
                    if (property.hasPropertyOrClassDomain(domain)) {
                        res.add(new AutocompleteObject(property.getText(), property.getText(), nextState, property.getUri(), SegmentationService.PROPERTY, null, openVariablesPosition[i], property.isPrefix(), null));
                        break;
                    }
                }
            }
        }
        return res;
    }

    private String[] splitUris(String[] uris) {
        ArrayList<String> res = new ArrayList<>();
        for (String u : uris) {
            String[] ri = u.split("\\|");
            for (String s : ri) {
                res.add(s);
            }
        }
        return res.toArray(new String[0]);
    }

    //the edit distance is modified giving weight 1 to swap of adjacent chars
    //it is assumed that the input strings are in lowercase and not null
    private double[] editDistance(String typing, String result) {
        double[][] matrix = new double[result.length() + 1][typing.length() + 1];

        for (int i = 0; i <= result.length(); i++) {
            matrix[i][0] = i;
        }

        for (int j = 0; j <= typing.length(); j++) {
            matrix[0][j] = j;
        }

        // Fill in the the matrix
        for (int i = 1; i <= result.length(); i++) {
            for (int j = 1; j <= typing.length(); j++) {
                if (result.charAt(i - 1) == typing.charAt(j - 1)) {
                    matrix[i][j] = matrix[i - 1][j - 1];
                } else if (i > 1 && j > 1 && result.charAt(i - 1) == typing.charAt(j - 2) && result.charAt(i - 2) == typing.charAt(j - 1)) {
                    matrix[i][j] = Math.min(
                            Math.min(matrix[i - 2][j - 2] + 1, //swap
                                    matrix[i - 1][j - 1] + 1), // substitution
                            Math.min(matrix[i][j - 1] + 1, // deletion from the typed string
                                    matrix[i - 1][j] + 1)); // insertion in the typed string
                } else {
                    matrix[i][j] = Math.min(matrix[i - 1][j - 1] + 1, // substitution
                            Math.min(matrix[i][j - 1] + 1, // deletion from the typed string
                                    matrix[i - 1][j] + 1)); // insertion in the typed string
                }
            }
        }
        double[] res = new double[2];
        res[0] = matrix[result.length()][typing.length()];
        res[1] = matrix[Math.min(typing.length(), result.length())][typing.length()];
        return res;
    }

    //the edit distance is modified, giving less weight to insertion
    //and giving weight 1 to swap of adjacent chars
    //it is assumed that the input strings are in lowercase and not null
    private double[] modifiedEditDistance(String typing, String result) {
        double[][] matrix = new double[result.length() + 1][typing.length() + 1];

        for (int i = 0; i <= result.length(); i++) {
            matrix[i][0] = 0.5 * i;
        }

        for (int j = 0; j <= typing.length(); j++) {
            matrix[0][j] = j;
        }

        // Fill in the rest of the matrix
        for (int i = 1; i <= result.length(); i++) {
            for (int j = 1; j <= typing.length(); j++) {
                if (result.charAt(i - 1) == typing.charAt(j - 1)) {
                    matrix[i][j] = matrix[i - 1][j - 1];
                } else if (i > 1 && j > 1 && result.charAt(i - 1) == typing.charAt(j - 2) && result.charAt(i - 2) == typing.charAt(j - 1)) {
                    matrix[i][j] = Math.min(
                            Math.min(matrix[i - 2][j - 2] + 1, //swap
                                    matrix[i - 1][j - 1] + 1), // substitution
                            Math.min(matrix[i][j - 1] + 1, // deletion from the typed string
                                    matrix[i - 1][j] + 0.5)); // insertion in the typed string
                } else {
                    matrix[i][j] = Math.min(matrix[i - 1][j - 1] + 1, // substitution
                            Math.min(matrix[i][j - 1] + 1, // deletion from the typed string
                                    matrix[i - 1][j] + 0.5)); // insertion in the typed string
                }
            }
        }
        double[] res = new double[2];
        res[0] = matrix[result.length()][typing.length()];
        res[1] = matrix[0][typing.length()];
        for (int i = 1; i < result.length(); i++) {
            if (res[1] < matrix[i][typing.length()]) {
                res[1] = matrix[i][typing.length()];
            }
        }
        return res;
    }

    private double[] computeSimilarity(AutocompleteObject a, String lowerCaseQuery) {
        int matchedWordsLength = 0;
        if (a.tokenType.startsWith("literal_")) {
            return new double[]{1, 1};
        } else {
            String t = a.text.toLowerCase();
            double[] ed = editDistance(lowerCaseQuery, t);
            double[] res = new double[2];
            res[0] = 1 - ed[0] / (Math.max(lowerCaseQuery.length(), t.length()) + matchedWordsLength);
            res[1] = 1 - ed[1] / (Math.min(lowerCaseQuery.length(), t.length()) + matchedWordsLength);
            return res;
        }
    }

    private ArrayList<AutocompleteObject> filterAndSort(String query, ArrayList<AutocompleteObject> l, double threshold) {
        ArrayList<AutocompleteObject> res = new ArrayList<>();
        query = query.toLowerCase();
        for (AutocompleteObject a : l) {
            double[] sim = computeSimilarity(a, query);
            a.similarity = sim[0];
            a.prefixSimilarity = sim[1];
            if (a.similarity >= threshold || a.isPrefix && a.prefixSimilarity >= threshold) {
                res.add(a);
            }
        }
        Collections.sort(res);
        return res;
    }

    private boolean prefixContained(String s, ArrayList<AutocompleteObject> l, double threshold) {
        for (AutocompleteObject a : l) {
            if (a.prefixSimilarity >= threshold) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<AutocompleteObject> getAutocompleResults(String query, String lastAcceptedProperty, String[] openVariablesUri, Integer[] openVariablesPosition, String currentState, String finalPunctuation, boolean disableContextRules, boolean autoAcceptance, boolean dateToNumber, boolean useKeywords) {
        ArrayList<AutocompleteObject> res = new ArrayList<>();
        boolean queryEndsWithSpace = query.charAt(query.length() - 1) == ' ';
        query = query.trim();
        if (autoAcceptance) { //try to split the tokens
            if (query.length() > 1) { //try to separate the final punctuation from previous tokens
                if (query.charAt(query.length() - 1) == '?') {
                    if (query.charAt(query.length() - 1) != ' ') {
                        query = query.substring(0, query.length() - 1) + " ?";
                    }
                } else if (query.charAt(query.length() - 1) == '.') {
                    if (query.charAt(query.length() - 1) != ' ') {
                        query = query.substring(0, query.length() - 1) + " .";
                    }
                }
            }
            StringTokenizer st = new StringTokenizer(query, " ");
            String partialQuery = "";
            query = query.replace("\"", " \" "); //add spaces around quotes
            ArrayList<String> queryWords = new ArrayList<>();
            while (st.hasMoreTokens()) {
                String queryWord = st.nextToken();
                if (queryWord.equals("\"")) { //it's the start of a string literal value
                    String nextToken;
                    while (st.hasMoreTokens() && !(nextToken = st.nextToken()).equals("\"")) {
                        queryWord += " " + nextToken;
                    }
                }
                if (partialQuery.length() > 0) {
                    partialQuery += " ";
                }
                partialQuery += queryWord;
                queryWords.add(queryWord);
            }
            int currentWordsUsed = queryWords.size(); //initially, try to accept all the words as a unique token
            String remainder = "";
            while (currentWordsUsed > 0) {
                ArrayList<AutocompleteObject> filteredResults = getFilteredAndSortedResults(partialQuery, lastAcceptedProperty, openVariablesUri, openVariablesPosition, currentState, finalPunctuation, disableContextRules, 0.90, dateToNumber);
                if (!filteredResults.isEmpty()) {
                    boolean prefixFound = false;
                    if (currentWordsUsed == queryWords.size()) { //all the query has been used - we need to check if any of the results is prefix of another token                        
                        for (AutocompleteObject a : filteredResults) {
                            if (a.keywords == null) {
                                if (a.prefixSimilarity >= 0.9 && a.similarity < 0.9 //the similarity of the token could be improved by typing other text
                                        || a.similarity > 0.95 && a.isPrefix //this token matches very well, but it is prefix of another token
                                        ) {
                                    prefixFound = true;
                                    break;
                                }
                            } else { //the free text can always be extended
                                prefixFound = a.isPrefix; //I don't understand this anymore
                            }
                            if (prefixFound) { //goal achieved
                                break;
                            }
                        }
                    }
                    if (filteredResults.get(0).similarity >= 0.95) { //a very good token has been found
                        int k = 0;
                        while (k < filteredResults.size() && filteredResults.get(k).similarity == filteredResults.get(0).similarity) {
                            filteredResults.get(k).remainder = remainder;
                            filteredResults.get(k).mustBeAccepted = !prefixFound;
                            res.add(filteredResults.get(k));
                            k++;
                        }
                        boolean couldBeShortened = currentWordsUsed > 1;
                        while (couldBeShortened) {
                            couldBeShortened = false;
                            currentWordsUsed--;
                            if (remainder.length() > 0) {
                                remainder = queryWords.get(currentWordsUsed) + " " + remainder;
                            } else {
                                remainder = queryWords.get(currentWordsUsed);
                            }
                            partialQuery = partialQuery.substring(0, partialQuery.length() - queryWords.get(currentWordsUsed).length() - 1);
                            double oldSimilarity = res.get(0).similarity;
                            double maxSimilarity = oldSimilarity;
                            String lowerCasePartialQuery = partialQuery.toLowerCase();
                            for (AutocompleteObject a : res) {
                                double newSimilarity = computeSimilarity(a, lowerCasePartialQuery)[0];
                                if (newSimilarity > oldSimilarity) {
                                    couldBeShortened = true;
                                    a.similarity = newSimilarity;
                                    a.remainder = remainder;
                                    if (newSimilarity > maxSimilarity) {
                                        maxSimilarity = newSimilarity;
                                    }
                                }
                            }
                            if (couldBeShortened) {
                                for (Iterator<AutocompleteObject> it = res.iterator(); it.hasNext();) {
                                    if (it.next().similarity < maxSimilarity) {
                                        it.remove();
                                    }
                                }
                            }
                        }
                        if (currentState.equals(ACCEPT_CONCEPT_STATE_S1) || currentState.equals(ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2)
                                || currentState.equals(ACCEPT_OPERATOR_OR_DIRECT_OPERAND_STATE_S3) || currentState.equals(ACCEPT_DIRECT_OPERAND_STATE_S5)
                                || currentState.equals(ACCEPT_INDIRECT_OPERAND_STATE_S6)) {
                            //must not contain must_accept tokens
                            //res.add(new AutocompleteObject("("+query+")", query, ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, "("+query+")", AutocompleteService.TEXT, finalPunctuation, null, true));
                        }
                        return res;
                    }
                }
                currentWordsUsed--;
                if (remainder.length() > 0) {
                    remainder = queryWords.get(currentWordsUsed) + " " + remainder;
                } else {
                    remainder = queryWords.get(currentWordsUsed);
                }
                if (currentWordsUsed > 0) {
                    partialQuery = partialQuery.substring(0, partialQuery.length() - queryWords.get(currentWordsUsed).length() - 1);
                }
            }
        }
        return getFilteredAndSortedResults(query, lastAcceptedProperty, openVariablesUri, openVariablesPosition, currentState, finalPunctuation, disableContextRules, 0.5, dateToNumber);
    }

    public ArrayList<AutocompleteObject> getFilteredAndSortedResults(String query, String lastAcceptedProperty, String[] openVariablesUri, Integer[] openVariablesPosition, String currentState, String finalPunctuation, boolean disableContextRules, double threshold, boolean dateToNumber) {
        ArrayList<AutocompleteObject> unfiltered = getSimpleAutocompleResults(query, lastAcceptedProperty, openVariablesUri, openVariablesPosition, currentState, finalPunctuation, disableContextRules, dateToNumber);
        ArrayList<AutocompleteObject> filtered = filterAndSort(query, unfiltered, threshold);
        //I don't understand the following piece of code anymore
        if (!disableContextRules //the auto-acceptance must be enabled
                && (filtered.isEmpty() || filtered.get(0).similarity < 0.9) //there are no results with high similarity
                && (lastAcceptedProperty != null || openVariablesUri != null && openVariablesUri.length > 0)) {
            ArrayList<AutocompleteObject> unfiltered2 = getSimpleAutocompleResults(query, lastAcceptedProperty, openVariablesUri, openVariablesPosition, currentState, finalPunctuation, true, dateToNumber);
            ArrayList<AutocompleteObject> filtered2 = filterAndSort(query, unfiltered2, 0.9);
            for (AutocompleteObject a : filtered2) {
                if (a.similarity < 0.9) {
                    break;
                }
                a.relatedTokenPosition = -1;
                a.mustBeAccepted = false;
                filtered.add(a);
            }
        }

        return filtered;
    }

    public ArrayList<AutocompleteObject> getSimpleAutocompleResults(String query, String lastAcceptedProperty, String[] openVariablesUri, Integer[] openVariablesPosition, String currentState, String finalPunctuation, boolean disableContextRules, boolean dateToNumber) {
        ArrayList<AutocompleteObject> res = new ArrayList<>();
        TokenIndex ontology = new TokenIndex();
        if (disableContextRules) { //TODO: handle this at a lower level (lucene query)
            lastAcceptedProperty = null;
            //openVariablesUri = null;
        }
        String[] lastAcceptedPropertys = (lastAcceptedProperty == null || lastAcceptedProperty.length() == 0) ? new String[0] : lastAcceptedProperty.split("\\|");
        switch (currentState) {
            case INITIAL_STATE_S0:  //Only a question start can be accepted: e.g., Give me ...
                for (Token e : ontology.getTokenElements(query, null, null, null, 20, IndexedToken.QUESTION_START)) {
                    String punctuation = "?";
                    if (e.getText().startsWith("Give ")) {
                        punctuation = ".";
                    }
                    boolean isPrefix = ((QuestionStartToken) e).isPrefix();
                    if (e.getText().startsWith("What has") || e.getText().startsWith("Who has")) {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_PROPERTY_FOR_CONSTRAINT_STATE_S4, e.getText(), QUESTION_START, punctuation, null, isPrefix, null));
                    } else {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_CONCEPT_STATE_S1, e.getText(), QUESTION_START, punctuation, null, isPrefix, null));
                    }
                }
                break;
            case ACCEPT_CONCEPT_STATE_S1:
                //ACCEPT_CONCEPT can be reached only from the initial state or through an property
                //being a last accepted property, an elements x can be accepted if x is in domain(A)                
                for (Token e : ontology.getTokenElements(query, lastAcceptedPropertys, null, null, 50, IndexedToken.CLASS, IndexedToken.PROPERTY, IndexedToken.ENTITY)) {
                    if (e instanceof ClassToken) {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, ((ClassToken) e).getUri(), SegmentationService.CLASS, null, null, ((ClassToken) e).isPrefix(), null));
                    } else if (e instanceof AugmentedClassToken) {
                        res.add(new AutocompleteObject(e.getText(), ((AugmentedClassToken) e).getClassToken().getText(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, ((AugmentedClassToken) e).getClassToken().getUri(), SegmentationService.CLASS, null, null, ((AugmentedClassToken) e).isPrefix(), ((AugmentedClassToken) e).getFreeText()));
                    } else if (e instanceof EntityToken) {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, ((EntityToken) e).getUri(), SegmentationService.ENTITY, null, null, ((EntityToken) e).isPrefix(), null));
                    } else if (e instanceof PropertyToken) {
                        res.add(new AutocompleteObject(e.getText() + (e.getText().endsWith(" of") ? " for" : " of"), e.getText(), ACCEPT_CONCEPT_STATE_S1, ((PropertyToken) e).getUri(), SegmentationService.PROPERTY, null, null, ((PropertyToken) e).isPrefix(), null));
                    }
                }
                break;
            case ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2:
                //being O the set of open variables, an properties a can be accepted if the intersection between O and domain(a) is not empty
                res.add(new AutocompleteObject(finalPunctuation, finalPunctuation, FINAL_STATE_SF, finalPunctuation, SegmentationService.FINAL_PUNCTUATION, null, null, false, null));
                ArrayList<IndexedToken> results;
                String[] acceptableTokens;
                if (lastAcceptedPropertys != null) { //the operators are always applied to last accepted property
                    acceptableTokens = new String[]{IndexedToken.CONSTRAINT_CONNECTIVE, IndexedToken.PROPERTY, IndexedToken.DIRECT_OPERATOR, IndexedToken.INDIRECT_OPERATOR, IndexedToken.UNARY_OPERATOR};
                } else {
                    acceptableTokens = new String[]{IndexedToken.CONSTRAINT_CONNECTIVE, IndexedToken.PROPERTY, IndexedToken.UNARY_OPERATOR};
                }
                if (openVariablesUri != null && openVariablesUri.length > 0 /*lastAcceptedFreeVariable != null*/) {
                    results = ontology.getTokenElements(query, null, null, splitUris(openVariablesUri), 20, acceptableTokens);
                    for (IndexedToken e : results) {
                        if (e instanceof ConstraintPrefixToken) {
                            res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_PROPERTY_FOR_CONSTRAINT_STATE_S4, e.getText(), SegmentationService.CONSTRAINT_PREFIX, null, null, ((ConstraintPrefixToken) e).isPrefix(), null));
                        } else if (e instanceof PropertyToken) {
                            res.addAll(extendPropertyWithContext((PropertyToken) e, ACCEPT_OPERATOR_OR_DIRECT_OPERAND_STATE_S3, openVariablesUri, openVariablesPosition));
                        } else if (e instanceof DirectBinaryOperatorToken) {
                            res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_DIRECT_OPERAND_STATE_S5, ((DirectBinaryOperatorToken) e).getSymbol(), SegmentationService.DIRECT_OPERATOR, null, null, ((DirectBinaryOperatorToken) e).isPrefix(), null));
                        } else if (e instanceof IndirectBinaryOperatorToken) {
                            res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_INDIRECT_OPERAND_STATE_S6, ((IndirectBinaryOperatorToken) e).getSymbol(), SegmentationService.INDIRECT_OPERATOR, null, null, ((IndirectBinaryOperatorToken) e).isPrefix(), null));
                        } else if (e instanceof UnaryOperatorToken) {
                            res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_PROPERTY_FOR_UNARY_OPERATOR_S10, ((UnaryOperatorToken) e).getSymbol(), SegmentationService.UNARY_OPERATOR, null, null, ((UnaryOperatorToken) e).isPrefix(), null));
                        }
                    }
                }
                break;
            case ACCEPT_OPERATOR_OR_DIRECT_OPERAND_STATE_S3:
                //being a last accepted property, an element x can be accepted if x is in range(a)                
                for (LiteralToken e : ontology.getLiteralElements(query, lastAcceptedPropertys, false)) {
                    if (e instanceof LiteralStringToken) {
                        res.add(new AutocompleteObject(e.getText(), e.getText().substring(1, e.getText().length() - 1), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, e.getText(), LITERAL_STRING, null, null, query.length() <= 1 || !query.endsWith("\""), null));
                    } else if (e instanceof LiteralNumericToken) {
                        res.add(new AutocompleteObject(query.trim(), query.trim(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, Double.toString(((LiteralNumericToken) e).getVal()), LITERAL_NUMERIC, null, null, true, null));
                    } else if (e instanceof LiteralDateToken) {
                        res.add(new AutocompleteObject(query.trim(), query.trim(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, e.getText(), LITERAL_DATE, null, null, true, null));
                    }
                }
                boolean composableProperty = false;
                for (String la : lastAcceptedPropertys) {
                    PropertyToken lastAcceptedPropertyElement = ontology.getPropertyByUri(la);
                    if (lastAcceptedPropertyElement.hasOutProperties()) {
                        composableProperty = true;
                        break;
                    }
                }
                if (composableProperty) { //the only difference is that the property can be composed (e.g., having capital having population)                    
                    acceptableTokens = new String[]{IndexedToken.CLASS, IndexedToken.PROPERTY, IndexedToken.ENTITY, IndexedToken.DIRECT_OPERATOR, IndexedToken.INDIRECT_OPERATOR, IndexedToken.CONSTRAINT_CONNECTIVE, IndexedToken.UNARY_OPERATOR};
                } else {
                    acceptableTokens = new String[]{IndexedToken.CLASS, IndexedToken.PROPERTY, IndexedToken.ENTITY, IndexedToken.DIRECT_OPERATOR, IndexedToken.INDIRECT_OPERATOR};
                }
                results = ontology.getTokenElements(query, null, lastAcceptedPropertys, null, 20, acceptableTokens);
                boolean propertyWithLiteralRange = false;
                if (lastAcceptedPropertys != null) {
                    for (String la : lastAcceptedPropertys) {
                        if (ontology.getPropertyByUri(la).hasLiteralRange()) {
                            propertyWithLiteralRange = true;
                            break;
                        }
                    }
                }
                for (Token e : results) {
                    if (e instanceof ClassToken) {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, ((ClassToken) e).getUri(), SegmentationService.CLASS, null, null, ((ClassToken) e).isPrefix(), null));
                    } else if (e instanceof AugmentedClassToken) {
                        res.add(new AutocompleteObject(e.getText(), ((AugmentedClassToken) e).getClassToken().getText(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, ((ClassToken) e).getUri(), SegmentationService.CLASS, null, null, ((ClassToken) e).isPrefix(), ((AugmentedClassToken) e).getFreeText()));
                    } else if (e instanceof EntityToken) {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, ((EntityToken) e).getUri(), SegmentationService.ENTITY, null, null, ((EntityToken) e).isPrefix(), null));
                    } else if (e instanceof PropertyToken) {
                        res.add(new AutocompleteObject(e.getText() + (e.getText().endsWith(" of") ? " for" : " of"), e.getText(), ACCEPT_CONCEPT_STATE_S1, ((PropertyToken) e).getUri(), SegmentationService.PROPERTY, null, null, ((PropertyToken) e).isPrefix(), null));
                    } else if (e instanceof DirectBinaryOperatorToken) {
                        if (((DirectBinaryOperatorToken) e).getSymbol().equals("=") || propertyWithLiteralRange) {
                            res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_DIRECT_OPERAND_STATE_S5, ((DirectBinaryOperatorToken) e).getSymbol(), SegmentationService.DIRECT_OPERATOR, null, null, ((DirectBinaryOperatorToken) e).isPrefix(), null));
                        }
                    } else if (e instanceof IndirectBinaryOperatorToken) {
                        if (((IndirectBinaryOperatorToken) e).getSymbol().equals("=") || propertyWithLiteralRange) {
                            res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_INDIRECT_OPERAND_STATE_S6, ((IndirectBinaryOperatorToken) e).getSymbol(), SegmentationService.INDIRECT_OPERATOR, null, null, ((IndirectBinaryOperatorToken) e).isPrefix(), null));
                        }
                    } else if (e instanceof ConstraintPrefixToken) {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_PROPERTY_FOR_CONSTRAINT_STATE_S4, e.getText(), SegmentationService.CONSTRAINT_PREFIX, null, null, ((ConstraintPrefixToken) e).isPrefix(), null));
                    } else if (e instanceof UnaryOperatorToken) {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_PROPERTY_FOR_UNARY_OPERATOR_S10, ((UnaryOperatorToken) e).getSymbol(), SegmentationService.UNARY_OPERATOR, null, null, ((UnaryOperatorToken) e).isPrefix(), null));
                    }
                }
                break;
            case ACCEPT_PROPERTY_FOR_CONSTRAINT_STATE_S4:
                //being O the set of open variables, an properties a can be accepted if the intersection between O and domain(a) is not empty
                if (openVariablesUri != null && openVariablesUri.length > 0) {
                    for (RankOperatorToken e : ontology.getRankOperatorElements(query)) {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_PROPERTY_FOR_RANK_STATE_S9, e.getLabel(), SegmentationService.RANK_OPERATOR, null, null, false, null));
                    }
                    for (TopKOperatorToken e : ontology.getTopKOperatorElements(query)) {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_PROPERTY_FOR_RANK_STATE_S9, e.getLabel(), SegmentationService.TOPK_OPERATOR, null, null, false, null));
                    }
                    results = ontology.getTokenElements(query, null, null, splitUris(openVariablesUri), 20, IndexedToken.PROPERTY);
                    for (IndexedToken e : results) {
                        if (e instanceof PropertyToken) {
                            res.addAll(extendPropertyWithContext((PropertyToken) e, ACCEPT_OPERATOR_OR_DIRECT_OPERAND_STATE_S3, openVariablesUri, openVariablesPosition));
                        }
                    }
                }
                break;
            case ACCEPT_DIRECT_OPERAND_STATE_S5:
                //in ACCEPT_DIRECT_OPERAND_STATE_S3 it is possible to arrive only through a direct operator
                //being a last accepted property, an element x can be accepted if x is in range(A)
                for (LiteralToken e : ontology.getLiteralElements(query, lastAcceptedPropertys, dateToNumber)) {
                    if (e instanceof LiteralStringToken) {
                        res.add(new AutocompleteObject(e.getText(), e.getText().substring(1, e.getText().length() - 1), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, e.getText(), LITERAL_STRING, null, null, query.length() <= 1 || !query.endsWith("\""), null));
                    } else if (e instanceof LiteralNumericToken) {
                        res.add(new AutocompleteObject(query.trim(), query.trim(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, Double.toString(((LiteralNumericToken) e).getVal()), LITERAL_NUMERIC, null, null, true, null));
                    } else if (e instanceof LiteralDateToken) {
                        res.add(new AutocompleteObject(query.trim(), query.trim(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, e.getText(), LITERAL_DATE, null, null, true, null));
                    }
                }
                for (Token e : ontology.getTokenElements(query, null, lastAcceptedPropertys, null, 20, IndexedToken.CLASS, IndexedToken.PROPERTY, IndexedToken.ENTITY, IndexedToken.POSSESSIVE_DETERMINER)) {
                    if (e instanceof ClassToken) {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, ((ClassToken) e).getUri(), SegmentationService.CLASS, null, null, ((ClassToken) e).isPrefix(), null));
                    } else if (e instanceof AugmentedClassToken) {
                        res.add(new AutocompleteObject(e.getText(), ((AugmentedClassToken) e).getClassToken().getText(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, ((ClassToken) e).getUri(), SegmentationService.CLASS, null, null, ((ClassToken) e).isPrefix(), ((AugmentedClassToken) e).getFreeText()));
                    } else if (e instanceof EntityToken) {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, ((EntityToken) e).getUri(), SegmentationService.ENTITY, null, null, ((EntityToken) e).isPrefix(), null));
                    } else if (e instanceof PropertyToken) {
                        res.add(new AutocompleteObject(e.getText() + (e.getText().endsWith(" of") ? " for" : " of"), e.getText(), ACCEPT_CONCEPT_STATE_S1, ((PropertyToken) e).getUri(), SegmentationService.PROPERTY, null, null, ((PropertyToken) e).isPrefix(), null));
                    } else if (e instanceof PossessiveDeterminerToken) {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_SELF_PROPERTY_AS_DIRECT_OPERAND_STATE_S7, e.getText(), SegmentationService.POSSESSIVE_DETERMINER, null, null, ((PossessiveDeterminerToken) e).isPrefix(), null));
                    }
                }
                break;
            case ACCEPT_INDIRECT_OPERAND_STATE_S6:
                //in ACCEPT_INDIRECT_OPERAND_STATE it is possible to arrive only through an indirect operator
                //being a last accepted property, an element x can be accepted if x is in domain(a).
                for (Token e : ontology.getTokenElements(query, lastAcceptedPropertys, null, null, 20, IndexedToken.CLASS, IndexedToken.PROPERTY, IndexedToken.ENTITY, IndexedToken.POSSESSIVE_DETERMINER)) {
                    if (e instanceof ClassToken) {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, ((ClassToken) e).getUri(), SegmentationService.CLASS, null, null, ((ClassToken) e).isPrefix(), null));
                    } else if (e instanceof AugmentedClassToken) {
                        res.add(new AutocompleteObject(e.getText(), ((AugmentedClassToken) e).getClassToken().getText(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, ((ClassToken) e).getUri(), SegmentationService.CLASS, null, null, ((ClassToken) e).isPrefix(), ((AugmentedClassToken) e).getFreeText()));
                    } else if (e instanceof EntityToken) {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, ((EntityToken) e).getUri(), SegmentationService.ENTITY, null, null, ((EntityToken) e).isPrefix(), null));
                    } else if (e instanceof PropertyToken) {
                        res.add(new AutocompleteObject(e.getText() + (e.getText().endsWith(" of") ? " for" : " of"), e.getText(), ACCEPT_CONCEPT_STATE_S1, ((PropertyToken) e).getUri(), SegmentationService.PROPERTY, null, null, ((PropertyToken) e).isPrefix(), null));
                    } else if (e instanceof PossessiveDeterminerToken) {
                        res.add(new AutocompleteObject(e.getText(), e.getText(), ACCEPT_SELF_PROPERTY_AS_INDIRECT_OPERAND_STATE_S8, e.getText(), SegmentationService.POSSESSIVE_DETERMINER, null, null, ((PossessiveDeterminerToken) e).isPrefix(), null));
                    }
                }
                break;
            case ACCEPT_SELF_PROPERTY_AS_DIRECT_OPERAND_STATE_S7:
                //in ACCEPT_SELF_PROPERTY_AS_DIRECT_OPERAND_STATE it is possible to arrive only through a possessive determiner
                //being a last accepted property, and O the set of open variables, excluding a, an property b can be accepted if O and domain(b) have non-empty intersection and b is in range(a)
                for (Token e : ontology.getTokenElements(query, null, lastAcceptedPropertys, splitUris(openVariablesUri)/*lastAcceptedFreeVariable.split(",")*/, 20, IndexedToken.PROPERTY)) {
                    if (e instanceof PropertyToken) {
                        if (e instanceof PropertyToken) {
                            res.addAll(extendPropertyWithContext((PropertyToken) e, ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, openVariablesUri, openVariablesPosition));
                        }
                    }
                }
                break;
            case ACCEPT_SELF_PROPERTY_AS_INDIRECT_OPERAND_STATE_S8:
                //in ACCEPT_SELF_PROPERTY_AS_INDIRECT_OPERAND_STATE it is possible to arrive only through a possessive determiner
                //being a last accepted property, and O the set of open variables, excluding a, an property b can be accepted if O and domain(b) have non-empty intersection and b is in domain(a).
                for (Token e : ontology.getTokenElements(query, lastAcceptedPropertys, null, splitUris(openVariablesUri)/*lastAcceptedFreeVariable.split(",")*/, 20, IndexedToken.PROPERTY)) {
                    if (e instanceof PropertyToken) {
                        if (e instanceof PropertyToken) {
                            res.addAll(extendPropertyWithContext((PropertyToken) e, ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, openVariablesUri, openVariablesPosition));
                        }
                    }
                }
                break;
            case ACCEPT_PROPERTY_FOR_RANK_STATE_S9:
                //being O the set of open variables, an property a can be accepted if O and domain(a) have not-empty intersection and a as a basic-type as range
                results = ontology.getTokenElements(query, null, null, splitUris(openVariablesUri) /*lastAcceptedFreeVariable.split(",")*/, 40, IndexedToken.PROPERTY);
                for (IndexedToken e : results) {
                    if (e instanceof PropertyToken) {
                        if (((PropertyToken) e).hasLiteralRange()) {
                            res.addAll(extendPropertyWithContext((PropertyToken) e, ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, openVariablesUri, openVariablesPosition));
                        }
                    }
                }
                break;
            case ACCEPT_PROPERTY_FOR_UNARY_OPERATOR_S10:
                //being O the set of open variables, an property a can be accepted if O and domain(a) have not-empty intersection
                results = ontology.getTokenElements(query, null, null, splitUris(openVariablesUri) /*lastAcceptedFreeVariable.split(",")*/, 40, IndexedToken.PROPERTY);
                for (IndexedToken e : results) {
                    if (e instanceof PropertyToken) {
                        res.addAll(extendPropertyWithContext((PropertyToken) e, ACCEPT_CONSTRAINT_OR_FINAL_PUNCTUATION_STATE_S2, openVariablesUri, openVariablesPosition));
                    }
                }
                break;
        }
        return res;
    }

    public ArrayList<AutocompleteObject> getEntityAutocompleResults(String query) {
        ArrayList<AutocompleteObject> res = new ArrayList<>();
        TokenIndex ontology = new TokenIndex();
        for (EntityToken e : ontology.getEntityElements(query, 20)) {
            res.add(new AutocompleteObject(e.getText(), e.getText(), INITIAL_STATE_S0, ((EntityToken) e).getUri(), SegmentationService.ENTITY, null, null, ((EntityToken) e).isPrefix(), null));
        }
        return res;
    }

    public ArrayList<AutocompleteObject> getPropertyAutocompleResults(String query) {
        ArrayList<AutocompleteObject> res = new ArrayList<>();
        TokenIndex ontology = new TokenIndex();
        for (PropertyToken a : ontology.getPropertyElements(query, 20)) {
            res.add(new AutocompleteObject(a.getText(), a.getText(), INITIAL_STATE_S0, ((PropertyToken) a).getUri(), SegmentationService.PROPERTY, null, null, ((PropertyToken) a).isPrefix(), null));
        }
        return res;
    }
}
