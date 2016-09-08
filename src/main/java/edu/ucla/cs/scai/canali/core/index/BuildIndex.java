/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index;

import edu.ucla.cs.scai.canali.core.index.tokens.PropertyToken;
import edu.ucla.cs.scai.canali.core.index.tokens.ClassToken;
import edu.ucla.cs.scai.canali.core.index.tokens.EntityToken;
import edu.ucla.cs.scai.canali.core.index.tokens.IndexedToken;
import static edu.ucla.cs.scai.canali.core.index.tokens.LiteralToken.*;
import edu.ucla.cs.scai.canali.core.index.tokens.OntologyElementToken;
import edu.ucla.cs.scai.canali.core.index.utils.Trie;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 *
 * This class contains the function for creating a Lucene directory, that can be
 * used by the AQUA system, starting from an ontology stored in the following
 * text files (the format for each file is specified in the method using it)
 *
 * triples: contain the triples subject, property, value
 *
 * property_labels: each row contains and property URI and a label - the same
 * property URI can appear on multiple rows (multiple labels)
 *
 * class_labels: each row contains a class URI and a label - the same
 * class URI can appear on multiple rows (multiple labels)
 *
 * class_parents each row contains a class URI and the URI of one of its
 * class parents
 *
 * entity_labels: each row contains an entity URI and a label - the same entity
 * URI can appear on multiple rows (multiple labels)
 *
 * entity_classes: each row contains an entity URI and the URI of one of its
 * classes
 *
 * basic_types_literal_types: each row contains a basic type URI and either
 * Double, Date, String, or Boolean
 *
 * additional_property_labels: other property labels
 *
 * additional_class_labels: other class labels
 *
 * additional_entity_labels: other class labels
 */
public class BuildIndex {

    String basePathInput, basePathOutput;
    int[] entityTriplesSubjects;
    int[] entityTriplesProperties;
    int[] entityTriplesValues;
    //ArrayList<int[]> entityTriples = new ArrayList<>();
    //HashMap<String, ArrayList<int[]>> literalTriples = new HashMap<>();
    HashMap<String, int[]> literalTriplesSubjects = new HashMap<>();
    HashMap<String, int[]> literalTriplesProperties = new HashMap<>();
    HashSet<String> literalTypes = new HashSet<>();
    HashMap<String, Integer> entityIdFromUriWithPrefix = new HashMap<>();
    HashMap<String, Integer> propertyIdFromUri = new HashMap<>();
    HashMap<String, Integer> classIdFromUri = new HashMap<>();
    String[] entityUriWithPrefix;
    String[] propertyUri;
    String[] classUri;
    HashSet<String>[] propertyLabels;
    HashSet<String>[] entityLabels;
    HashSet<String>[] classLabels;
    HashSet<Integer>[] entityClasses;
    HashMap<String, String> basicTypesMapping = new HashMap<>();
    HashSet<Integer>[] classParents;
    HashSet<Integer>[] classAncestors;
    HashSet<Integer>[] classChildren;
    HashSet<Integer>[] classDescendants;
    HashSet<Integer>[] entityOutProperties;
    HashSet<Integer>[] entityInProperties;
    HashSet<Integer>[] classOutProperties;
    HashSet<Integer>[] classInProperties;
    HashSet<Integer>[] propertyInProperties;
    HashSet<Integer>[] propertyOutProperties;
    int[] propertyCount;
    boolean[] propertyHasLiteralRange;
    HashMap<String, HashSet<Integer>> literalTypesInProperties = new HashMap<>();
    int iEntityTriples = 0;
    HashMap<String, Integer> iLiteralTriples = new HashMap<>();
    public final static String THING = "http://www.w3.org/2002/07/owl#Thing";
    int thingId;
    boolean printFiles = false;
    int minPropertyLength = 2;

    static final HashMap<String, String> uriToPrefix = new HashMap<>();
    static final HashMap<String, String> prefixToUri = new HashMap<>();

    static final boolean DROP_UNLABELED_ENTITIES = false;

    static {
        uriToPrefix.put("http://musicbrainz.org/area/", "1:");
        uriToPrefix.put("http://musicbrainz.org/artist/", "2:");
        uriToPrefix.put("http://musicbrainz.org/artist/", "3:");
        uriToPrefix.put("http://musicbrainz.org/record/", "4:");
        uriToPrefix.put("http://musicbrainz.org/place/", "5:");
        uriToPrefix.put("http://musicbrainz.org/recording/", "6:");
        uriToPrefix.put("http://musicbrainz.org/place/", "7:");
        uriToPrefix.put("http://musicbrainz.org/place/", "8:");
        uriToPrefix.put("http://musicbrainz.org/tag/", "9:");
        uriToPrefix.put("http://musicbrainz.org/track/", "a:");
        uriToPrefix.put("http://musicbrainz.org/work/", "b:");
        for (Map.Entry<String, String> e : uriToPrefix.entrySet()) {
            prefixToUri.put(e.getValue(), e.getKey());
        }
    }

    public BuildIndex(String basePathInput, String basePathOutput) {
        if (!basePathInput.endsWith(File.separator)) {
            basePathInput += File.separator;
        }
        this.basePathInput = basePathInput;
        if (!basePathOutput.endsWith(File.separator)) {
            basePathOutput += File.separator;
        }
        this.basePathOutput = basePathOutput;
        literalTypesInProperties.put(DOUBLE, new HashSet<Integer>());
        literalTypesInProperties.put(STRING, new HashSet<Integer>());
        literalTypesInProperties.put(DATE, new HashSet<Integer>());
        literalTypesInProperties.put(BOOLEAN, new HashSet<Integer>());
        //literalTriples.put(STRING, new ArrayList<int[]>());
        //literalTriples.put(BOOLEAN, new ArrayList<int[]>());
        //literalTriples.put(DOUBLE, new ArrayList<int[]>());
        //literalTriples.put(DATE, new ArrayList<int[]>());
        literalTypes.add(STRING);
        literalTypes.add(BOOLEAN);
        literalTypes.add(DOUBLE);
        literalTypes.add(DATE);
        for (String type : literalTypes) {
            iLiteralTriples.put(type, 0);
        }
    }

    private Integer getEntityIdFromUri(String uri) {
        for (String p : uriToPrefix.keySet()) {
            if (uri.startsWith(p)) {
                uri = uri.replace(p, uriToPrefix.get(p));
                break;
            }
        }
        return entityIdFromUriWithPrefix.get(uri);
    }

    private String getEntityCompleteUri(String uri) {
        for (String p : prefixToUri.keySet()) {
            if (uri.startsWith(p)) {
                return uri.replace(p, prefixToUri.get(p));
            }
        }
        return uri;
    }

    private void putEntityIdFromUri(String uri, int id) {
        for (String p : uriToPrefix.keySet()) {
            if (uri.startsWith(p)) {
                uri = uri.replace(p, uriToPrefix.get(p));
                break;
            }
        }
        entityIdFromUriWithPrefix.put(uri, id);
    }

    private void updateTriples(String subj, String attr, String entityVal, String literalType) {
        Integer idSbj = getEntityIdFromUri(subj);//entityIdFromUri.get(subj);
        Integer idAttr = propertyIdFromUri.get(attr);
        if (entityVal != null) {
            Integer idVal = getEntityIdFromUri(entityVal);//entityIdFromUri.get(entityVal);
            entityTriplesSubjects[iEntityTriples] = idSbj;
            entityTriplesProperties[iEntityTriples] = idAttr;
            entityTriplesValues[iEntityTriples] = idVal;
            iEntityTriples++;
            //now, create the inverted triple
            Integer idInvAttr = propertyIdFromUri.get(attr + "Inv");
            entityTriplesSubjects[iEntityTriples] = idVal;
            entityTriplesProperties[iEntityTriples] = idInvAttr;
            entityTriplesValues[iEntityTriples] = idSbj;
            iEntityTriples++;
        } else {
            int pos = iLiteralTriples.get(literalType);
            literalTriplesSubjects.get(literalType)[pos] = idSbj;
            literalTriplesProperties.get(literalType)[pos] = idAttr;
            iLiteralTriples.put(literalType, pos + 1);
        }
    }
    /*
     The file triples must contain one row per triple <subject, property, value>, with the form
     <uri_subject> <uri_property> <uri_value>
     or
     <uri_subject> <uri_property> "value"
     or
     <uri_subject> <uri_property> "value"<basic_type>
     e.g.
     <http://dbpedia.org/resource/01_Communique> <http://dbpedia.org/ontology/industry> <http://dbpedia.org/resource/Software>
     or
     <http://dbpedia.org/resource/Kikuzo_Kisaka> <http://xmlns.com/foaf/0.1/name> "Kisaka, Kikuzo"@en
     or
     <http://dbpedia.org/resource/Kiko_Casilla> <http://dbpedia.org/ontology/alias> "Casilla, Francisco"
     or
     <http://dbpedia.org/resource/Kiki_S%C3%B8rum> <http://dbpedia.org/ontology/birthDate> "1939-01-16"^^<http://www.w3.org/2001/XMLSchema#date>
     Triples whose value is an entity are loaded into a list of of arrays of integers containing 3 elements: subject id, property id, value id. Ids are assigned as new entities are processed.
     Triples whose value is a literal are loaded into one of 4 lists of arrays of integers, depending on the type of literal, containing 2 elements: subject id, property id.
     Entities and propertys that are not found in this file will be ignored in the following.
     */

    private void loadTriples() throws Exception {
        HashMap<String, Integer> propertyFrequency = new HashMap<>();
        HashSet<String> shortProperties = new HashSet<>();
        if (minPropertyLength > 1) {
            System.out.println("Finding propertys to be ignored because they have lenght less than " + minPropertyLength);
            int i=0;
            try (BufferedReader in = new BufferedReader(new FileReader(basePathInput + "property_labels"))) {
                String l;
                while ((l = in.readLine()) != null) {
                    i++;
                    if (l.length() > 0) {
                        try {
                        StringTokenizer st = new StringTokenizer(l, "\t<> ");
                        String uri = st.nextToken().trim();
                        if (uri.startsWith("http")) {
                            String label = st.hasMoreTokens() ? st.nextToken().trim() : "";
                            if (label.length() < minPropertyLength && !shortProperties.contains(uri)) {
                                shortProperties.add(uri);
                                System.out.println("Property " + uri + " will be ignored, having label " + label);
                                propertyFrequency.put(uri, 0);
                            }
                        }
                        } catch (Exception e) {
                            System.out.println("Error at line "+i+": "+l);
                            e.printStackTrace();
                        }
                    }
                }
            }
            System.out.println(shortProperties.size() + " propertys will be ignored, having lenght less than " + minPropertyLength);
        }
        int maxNumberOfProperties = 100000;
        System.out.println("Finding the the " + maxNumberOfProperties + " most frequent propertys of the propertys whose label has at least two characters");
        try (BufferedReader in = new BufferedReader(new FileReader(basePathInput + "triples"))) {
            String l = in.readLine();
            int n = 0;
            while (l != null && l.length() > 0) {
                if (l.contains("classDegree")) {
                    System.out.print("");
                }
                StringTokenizer st = new StringTokenizer(l, "<> \t");
                String subject = st.nextToken();
                String property = st.nextToken();
                String value = st.nextToken();
                if (subject.startsWith("http") && property.startsWith("http") && !shortProperties.contains(property)) {
                    if (value.startsWith("http") || value.startsWith("ftp:")) { //it is an entity
                        Integer c = propertyFrequency.get(property);
                        if (c == null) {
                            propertyFrequency.put(property, 1);
                        } else {
                            propertyFrequency.put(property, 1 + c);
                        }
                    } else { //it is a literal
                        if (value.endsWith("^^")) { //it is a basic type
                            String type = StringEscapeUtils.unescapeJava(st.nextToken());
                            String literalType = basicTypesMapping.get(type);
                            if (literalType != null) {
                                Integer c = propertyFrequency.get(property);
                                if (c == null) {
                                    propertyFrequency.put(property, 1);
                                } else {
                                    propertyFrequency.put(property, 1 + c);
                                }
                            } else {
                                System.out.println("Basic type not recognized in " + l);
                            }
                        } else {
                            if (value.startsWith("\"")) { //it is a String
                                Integer c = propertyFrequency.get(property);
                                if (c == null) {
                                    propertyFrequency.put(property, 1);
                                } else {
                                    propertyFrequency.put(property, 1 + c);
                                }
                            } else {
                                System.out.println("Basic type not recognized in " + l);
                            }
                        }
                    }
                    n++;
                    if (n % 1000000 == 0) {
                        System.out.println("Scanned " + (n / 1000000) + "M triples");
                    }
                } else {
                    //System.out.println("Invalid triple: " + l);
                }
                l = in.readLine();
            }
        }
        shortProperties = null;
        System.gc();
        ArrayList<Map.Entry<String, Integer>> f = new ArrayList<>(propertyFrequency.entrySet());
        Collections.sort(f, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return Integer.compare(o2.getValue(), o1.getValue());
            }
        });
        int minFreq = 1;
        if (f.size() > maxNumberOfProperties) {
            minFreq = f.get(maxNumberOfProperties - 1).getValue();
            if (f.get(maxNumberOfProperties).equals(f.get(maxNumberOfProperties - 1))) {
                minFreq++;
            }
        }
        for (Map.Entry<String, Integer> e : f) {
            System.out.println(e.getKey() + "\t" + e.getValue());
        }
        System.out.println("Keeping propertys with at least " + minFreq + " occurrences");
        HashSet<String> acceptedProperties = new HashSet<>();
        for (Map.Entry<String, Integer> e : propertyFrequency.entrySet()) {
            if (e.getValue() >= minFreq) {
                acceptedProperties.add(e.getKey());
            }
        }
        System.out.println(acceptedProperties.size() + " propertys kept over " + f.size());
        f = null;
        propertyFrequency = null;
        System.gc();
        System.out.println("Mapping entities and property URIs to ids");
        int nEntityTriples = 0;
        HashMap<String, Integer> nLiteralTriples = new HashMap<>();
        for (String type : literalTypes) {
            nLiteralTriples.put(type, 0);
        }
        HashSet<String> unrecognizedBasicTypes = new HashSet<>();
        //count entity-valued and literal-valued triples
        //and
        //create the association between uris and ids for entities        
        try (BufferedReader in = new BufferedReader(new FileReader(basePathInput + "triples"))) {
            String l = in.readLine();
            int n = 0;
            while (l != null && l.length() > 0) {
                StringTokenizer st = new StringTokenizer(l, "<> \t");
                String subject = st.nextToken();
                String property = st.nextToken();
                if (!acceptedProperties.contains(property)) {
                    l = in.readLine();
                    continue;
                }
                String value = st.nextToken();
                if (subject.startsWith("http") && property.startsWith("http")) {
                    Integer idSbj = getEntityIdFromUri(subject); //entityIdFromUri.get(subject);
                    if (idSbj == null) {
                        idSbj = entityIdFromUriWithPrefix.size() + 1;//entityIdFromUri.size() + 1;
                        putEntityIdFromUri(subject, idSbj); //entityIdFromUri.put(subject, idSbj);
                    }
                    Integer idAttr = propertyIdFromUri.get(property);
                    if (idAttr == null) {
                        idAttr = propertyIdFromUri.size() + 1;
                        propertyIdFromUri.put(property, idAttr);
                    }
                    if (value.startsWith("http") || value.startsWith("ftp:")) { //it is an entity
                        Integer idVal = getEntityIdFromUri(value); //entityIdFromUri.get(value);
                        if (idVal == null) {
                            idVal = entityIdFromUriWithPrefix.size() + 1;//entityIdFromUri.size() + 1;
                            putEntityIdFromUri(value, idVal);//entityIdFromUri.put(value, idVal);
                        }
                        Integer idInvAttr = propertyIdFromUri.get(property + "Inv");
                        if (idInvAttr == null) {
                            idInvAttr = propertyIdFromUri.size() + 1;
                            propertyIdFromUri.put(property + "Inv", idInvAttr);
                        }
                        nEntityTriples += 2;
                    } else { //it is a literal
                        if (value.endsWith("^^")) { //it is a basic type
                            String type = StringEscapeUtils.unescapeJava(st.nextToken());
                            String literalType = basicTypesMapping.get(type);
                            if (literalType != null) {
                                nLiteralTriples.put(literalType, nLiteralTriples.get(literalType) + 1);
                            } else {
                                if (!unrecognizedBasicTypes.contains(type)) {
                                    System.out.println("Unrecognized type: " + type);
                                    System.out.println("in line: " + l);
                                    unrecognizedBasicTypes.add(type);
                                }
                            }
                        } else {
                            if (value.startsWith("\"")) { //it is a String
                                nLiteralTriples.put(STRING, nLiteralTriples.get(STRING) + 1);
                            }
                        }
                    }
                    n++;
                    if (n % 1000000 == 0) {
                        System.out.println("Loaded " + (n / 1000000) + "M triples");
                    }
                } else {
                    System.out.println("Invalid triple: " + l);
                }
                l = in.readLine();
            }
        }
        System.out.println("Number of triples with entity value: " + nEntityTriples);
        for (String type : literalTypes) {
            System.out.println("Number of triples with " + type + " value: " + nLiteralTriples.get(type));
        }
        entityTriplesSubjects = new int[nEntityTriples];
        entityTriplesProperties = new int[nEntityTriples];
        entityTriplesValues = new int[nEntityTriples];
        for (String type : literalTypes) {
            literalTriplesSubjects.put(type, new int[nLiteralTriples.get(type)]);
            literalTriplesProperties.put(type, new int[nLiteralTriples.get(type)]);
        }
        //load the triples into the arrays creaded above
        System.out.println("Loading triples");
        try (BufferedReader in = new BufferedReader(new FileReader(basePathInput + "triples"))) {
            String l = in.readLine();
            int n = 0;
            while (l != null && l.length() > 0) {
                StringTokenizer st = new StringTokenizer(l, "<> \t");
                String sbj = st.nextToken();
                String attr = st.nextToken();
                if (!acceptedProperties.contains(attr)) {
                    l = in.readLine();
                    continue;
                }
                String val = st.nextToken();
                if (sbj.startsWith("http") && attr.startsWith("http")) {
                    if (val.startsWith("http") || val.startsWith("ftp:")) { //it is an entity
                        updateTriples(sbj, attr, val, null);
                    } else { //it is a literal
                        if (val.endsWith("^^")) { //it is a basic type
                            String type = StringEscapeUtils.unescapeJava(st.nextToken());
                            String literalType = basicTypesMapping.get(type);
                            if (literalType != null) {
                                updateTriples(sbj, attr, null, literalType);
                            } else {
                                if (!unrecognizedBasicTypes.contains(type)) {
                                    System.out.println("Unrecognized type: " + type);
                                    System.out.println("in line: " + l);
                                    unrecognizedBasicTypes.add(type);
                                }
                            }
                        } else {
                            if (val.startsWith("\"")) { //it is a String
                                updateTriples(sbj, attr, null, STRING);
                            } else {
                                System.out.println("Unexpected line: " + l);
                            }
                        }
                    }
                    n++;
                    if (n % 1000000 == 0) {
                        System.out.println("Loaded " + (n / 1000000) + "M triples");
                    }
                } else {
                    System.out.println("Invalid triple: " + l);
                }
                l = in.readLine();
            }
        }
        System.out.println("Entity value triples: " + entityTriplesSubjects.length);
        for (String type : literalTriplesSubjects.keySet()) {
            System.out.println(type + " value triples: " + literalTriplesSubjects.get(type).length);
        }
        propertyUri = new String[propertyIdFromUri.size() + 1];
        for (Map.Entry<String, Integer> e : propertyIdFromUri.entrySet()) {
            propertyUri[e.getValue()] = e.getKey();
        }
        entityUriWithPrefix = new String[entityIdFromUriWithPrefix.size() + 1];
        for (Map.Entry<String, Integer> e : entityIdFromUriWithPrefix.entrySet()) {
            entityUriWithPrefix[e.getValue()] = e.getKey();
        }
        //entityUri = new String[entityIdFromUri.size() + 1];
        //for (Map.Entry<String, Integer> e : entityIdFromUri.entrySet()) {
        //    entityUri[e.getValue()] = e.getKey();
        //}
        entityLabels = new HashSet[entityIdFromUriWithPrefix.size() + 1]; //entityLabels = new HashSet[entityIdFromUri.size() + 1];
        entityClasses = new HashSet[entityIdFromUriWithPrefix.size() + 1]; //entityClasses = new HashSet[entityIdFromUri.size() + 1];
        propertyLabels = new HashSet[propertyIdFromUri.size() + 1];
        entityOutProperties = new HashSet[entityIdFromUriWithPrefix.size() + 1]; //entityOutProperties = new HashSet[entityIdFromUri.size() + 1];
        entityInProperties = new HashSet[entityIdFromUriWithPrefix.size() + 1]; //entityInProperties = new HashSet[entityIdFromUri.size() + 1];
        propertyOutProperties = new HashSet[propertyIdFromUri.size() + 1];
        propertyInProperties = new HashSet[propertyIdFromUri.size() + 1];
        propertyHasLiteralRange = new boolean[propertyIdFromUri.size() + 1];
        propertyCount = new int[propertyIdFromUri.size() + 1];
    }
    /*
     The file property_labels must contain one row per property, with the form
     uri \t label
     e.g.
     http://dbpedia.org/ontology/wimbledonMixed      wimbledon mixed
     uris and labels can be inside angular brackets, e.g.
     <http://dbpedia.org/ontology/wimbledonMixed>      <wimbledon mixed>
     or
     <http://dbpedia.org/ontology/wimbledonMixed>      wimbledon mixed
     or
     http://dbpedia.org/ontology/wimbledonMixed      <wimbledon mixed>
     no other separators are supposed to be used
     The labels of propertys that are not used in triples are ignored
     */

    private void processePropertyLabelsFile(String fileName) throws Exception {
        try (BufferedReader in = new BufferedReader(new FileReader(basePathInput + fileName))) {
            String l = in.readLine();
            while (l != null) {
                if (l.length() > 0) {
                    StringTokenizer st = new StringTokenizer(l, "\t<>");
                    String uri = st.nextToken().trim();
                    Integer id = propertyIdFromUri.get(uri);
                    if (id != null) { //we ignore the labels of propertys not used in triples
                        try {
                            String label = st.nextToken().trim();
                            if (label.length() > 1) {
                                //System.out.println(uri + "\t" + label);
                                if (propertyLabels[id] == null) {
                                    propertyLabels[id] = new HashSet<>();
                                }
                                propertyLabels[id].add(label);
                                if (uri.endsWith("Inv")) {
                                    System.out.println("Label \"" + label + "\" for inverted property " + uri);
                                }
                                Integer idInv = propertyIdFromUri.get(uri + "Inv");
                                if (idInv != null) {
                                    if (propertyLabels[idInv] == null) {
                                        propertyLabels[idInv] = new HashSet<>();
                                    }
                                    propertyLabels[idInv].add(label + " [inverted]");
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Line: " + l);
                        }
                    }
                }
                l = in.readLine();
            }
        }

    }

    private void loadPropertyLabels() throws Exception {
        System.out.println("Loading property labels");
        processePropertyLabelsFile("property_labels");
        System.out.println("Loading additional property labels");
        try {
            processePropertyLabelsFile("additional_property_labels");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //now, we drop the propertys without a label from the map of uri -> id
        for (int i = 1; i < propertyLabels.length; i++) {
            if (propertyLabels[i] == null) {
                propertyIdFromUri.remove(propertyUri[i]);
                propertyUri[i] = null;
            }
        }
    }
    /*
     The file class_labels must contain one row per class, with the form
     uri \t label
     e.g.
     http://dbpedia.org/ontology/ProtectedArea       protected area
     uris and labels can be inside angular brackets, e.g.
     <http://dbpedia.org/ontology/ProtectedArea>      <protected area>
     or
     <http://dbpedia.org/ontology/ProtectedArea>      protected area
     or
     http://dbpedia.org/ontology/ProtectedArea      <protected area>
     no other separators are supposed to be used
     */

    private void processClassLabelsFile(String fileName, ArrayList<HashSet<String>> labels) throws Exception {
        try (BufferedReader in = new BufferedReader(new FileReader(basePathInput + fileName))) {
            String l = in.readLine();
            while (l != null) {
                if (l.length() > 0) {
                    StringTokenizer st = new StringTokenizer(l, "\t<>");
                    try {
                        String uri = st.nextToken().trim();
                        String label = st.nextToken().trim();
                        //System.out.println(uri + "\t" + label);
                        if (!classIdFromUri.containsKey(uri)) {
                            classIdFromUri.put(uri, labels.size() + 1);
                            labels.add(new HashSet<String>());
                            labels.get(labels.size() - 1).add(label);
                        } else {
                            labels.get(classIdFromUri.get(uri) - 1).add(label);
                        }
                    } catch (Exception e) {
                        System.out.println("Error with line " + l);
                        e.printStackTrace();
                    }
                }
                l = in.readLine();
            }
        }
    }

    private void loadClassLabels() throws Exception {
        ArrayList<HashSet<String>> labels = new ArrayList<>();
        classIdFromUri.put(THING, 1);
        thingId = 1;
        labels.add(new HashSet<String>());
        labels.get(0).add("thing");
        System.out.println("Loading class labels");
        processClassLabelsFile("class_labels", labels);
        try {
            processClassLabelsFile("additional_class_labels", labels);
        } catch (Exception e) {
            e.printStackTrace();
        }
        classLabels = new HashSet[labels.size() + 1];
        int i = 1;
        for (HashSet<String> l : labels) {
            classLabels[i] = l;
            i++;
        }
        classUri = new String[classIdFromUri.size() + 1];
        for (Map.Entry<String, Integer> e : classIdFromUri.entrySet()) {
            classUri[e.getValue()] = e.getKey();
        }
    }
    /*
     The file class_parents must contain one or more rows per class, with the form
     uri \t uri_parent
     e.g.
     http://dbpedia.org/ontology/Actor       http://dbpedia.org/ontology/Artist
     uris can be inside angular brackets, e.g.
     <http://dbpedia.org/ontology/Actor>       <http://dbpedia.org/ontology/Artist>
     or
     <http://dbpedia.org/ontology/Actor>       http://dbpedia.org/ontology/Artist
     or
     http://dbpedia.org/ontology/Actor       <http://dbpedia.org/ontology/Artist>
     no other separators are supposed to be used
     */

    private void loadClassHierarchy() throws Exception {
        System.out.println("Loading class parents and building the hierarchy");
        //firs, we initialize class parents
        classParents = new HashSet[classIdFromUri.size() + 1];
        for (int i = 1; i < classParents.length; i++) {
            classParents[i] = new HashSet<>();
            //we don't initialize class ancestors because the null value is used to check if the class has not been processed yet
        }
        try (BufferedReader in = new BufferedReader(new FileReader(basePathInput + "class_parents"))) {
            String l = in.readLine();
            while (l != null) {
                if (l.length() > 0) {
                    StringTokenizer st = new StringTokenizer(l, "\t<>");
                    String claz = st.nextToken().trim();
                    String parent = st.nextToken().trim();
                    //we are interested only in the hierarchical relationships between
                    //classes defined inside our ontology
                    Integer cId = classIdFromUri.get(claz);
                    Integer pId = classIdFromUri.get(parent);
                    if (cId != null && pId != null && !pId.equals(cId)) {
                        classParents[cId].add(pId);
                    }
                }
                l = in.readLine();
            }
            //now add Thing to empty sets of parents
            for (int cId = 1; cId < classParents.length; cId++) {
                if (classParents[cId].isEmpty()) {
                    classParents[cId].add(thingId);
                }
            }
            classParents[thingId].clear();
            //now, for each class compute the set of its ancestors
            classAncestors = new HashSet[classIdFromUri.size() + 1];
            for (int cId = 1; cId < classAncestors.length; cId++) {
                computeClassAncestors(cId);
            }
            //now, reduce the set of class parents, by keeping only the most specific classes
            for (int cId = 1; cId < classParents.length; cId++) {
                HashSet<Integer> currentParents = classParents[cId];
                HashSet<Integer> reducedParents = new HashSet<>();
                for (Integer pId : currentParents) {
                    //check if reducedParents contains an ancestor of parent,
                    //or if parent is an ancestor of any class in reducedParents
                    boolean add = true;
                    for (Iterator<Integer> it = reducedParents.iterator(); it.hasNext();) {
                        Integer c = it.next();
                        if (classAncestors[c].contains(pId)) {
                            add = false; //we don't add parent, beacause c is a descendant of parent
                            break;
                        } else if (classAncestors[pId].contains(c)) {
                            it.remove(); //we remove c beacause parent is a descendant of c
                        }
                    }
                    if (add) {
                        reducedParents.add(pId);
                    }
                }
                classParents[cId] = reducedParents;
            }
            //now, compute the class children for each class
            classChildren = new HashSet[classIdFromUri.size() + 1];
            for (int cId = 1; cId < classChildren.length; cId++) {
                classChildren[cId] = new HashSet<>();
            }
            for (int cId = 1; cId < classParents.length; cId++) {
                for (Integer pId : classParents[cId]) {
                    classChildren[pId].add(cId);
                }
            }
            //now compute the class descendants for each class
            classDescendants = new HashSet[classIdFromUri.size() + 1];
            for (int cId = 1; cId < classDescendants.length; cId++) {
                computeClassDescendants(cId);
            }
        }
    }
    /*
     Compute the set of ancestors of a class
     */

    private void computeClassAncestors(int cId) {
        if (classAncestors[cId] != null) {
            return; //it was already computed
        }
        classAncestors[cId] = new HashSet<>();
        for (Integer pId : classParents[cId]) {
            //the parent is an ancestor
            classAncestors[cId].add(pId);
            computeClassAncestors(pId);
            //and the ancestors of the parent are ancestors as well
            classAncestors[cId].addAll(classAncestors[pId]);
        }
    }
    /*
     Compute the set of descendants of a class
     */

    private void computeClassDescendants(int cId) {
        if (classDescendants[cId] != null) {
            return; //it was already computed
        }
        classDescendants[cId] = new HashSet<>();
        for (Integer pId : classChildren[cId]) {
            //the parent is an ancestor
            classDescendants[cId].add(pId);
            computeClassDescendants(pId);
            //and the ancestors of the parent are ancestors as well
            classDescendants[cId].addAll(classDescendants[pId]);
        }
    }
    /*
     The file entity_labels must contain one row per entity, with the form
     uri \t label
     e.g.
     http://dbpedia.org/resource/Agatha_Christie     Agatha Christie
     uris can be inside angular brackets, e.g.
     <http://dbpedia.org/resource/Agatha_Christie>       <Agatha Christie>
     or
     <http://dbpedia.org/resource/Agatha_Christie>       Agatha Christie
     or
     http://dbpedia.org/resource/Agatha_Christie       <Agatha Christie>
     no other separators are supposed to be used
     */

    private void processEntityLabelsFile(String fileName) throws Exception {
        try (BufferedReader in = new BufferedReader(new FileReader(basePathInput + fileName))) {
            String l = in.readLine();
            while (l != null) {
                if (l.length() > 0) {
                    StringTokenizer st = new StringTokenizer(l, "\t<>");
                    String uri = st.nextToken();
                    Integer id = getEntityIdFromUri(uri); //entityIdFromUri.get(uri);
                    if (id != null) { //we ignore the labels of entities not used in triples
                        try {
                            String label = st.nextToken();
                            //System.out.println(uri + "\t" + label);
                            if (entityLabels[id] == null) {
                                entityLabels[id] = new HashSet<>();
                            }
                            entityLabels[id].add(label);
                        } catch (Exception e) {
                            System.out.println("Failed to add label: " + l);
                        }
                    } else {
                        //System.out.println("Ignored label of "+uri);
                    }
                }
                l = in.readLine();
            }
        }
    }

    private void loadEntityLabels() throws Exception {
        System.out.println("Loading entity labels");
        processEntityLabelsFile("entity_labels");
        try {
            processEntityLabelsFile("additional_entity_labels");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //now, we drop the entities without a label from the map of uri -> id
        for (int i = 1; i < entityLabels.length; i++) {
            if (entityLabels[i] == null) {
                if (DROP_UNLABELED_ENTITIES) {
                    entityIdFromUriWithPrefix.remove(entityUriWithPrefix[i]);//entityIdFromUri.remove(entityUri[i]);
                    entityUriWithPrefix[i] = null; //entityUri[i] = null;
                } else { //create a label
                    entityLabels[i] = new HashSet<>();
                    entityLabels[i].add(entityUriWithPrefix[i]);
                }
            }
        }
    }
    /*
     The file entity_classes must contain one row per pair <entity, class>, with the form
     uri_entity \t uri_class
     e.g.
     http://dbpedia.org/resource/Autism      http://dbpedia.org/ontology/Disease
     uris can be inside angular brackets, e.g.
     <http://dbpedia.org/resource/Autism>      <http://dbpedia.org/ontology/Disease>
     or
     <http://dbpedia.org/resource/Autism>      http://dbpedia.org/ontology/Disease
     or
     http://dbpedia.org/resource/Autism      <http://dbpedia.org/ontology/Disease>
     no other separators are supposed to be used
     */

    private void loadEntityClasses() throws Exception {
        System.out.println("Loading entity classes, and keeping only the most specific");
        int count = 0;
        HashSet<Integer> notEmptyClasses = new HashSet<>();
        try (BufferedReader in = new BufferedReader(new FileReader(basePathInput + "entity_classes"))) {
            String l = in.readLine();
            while (l != null) {
                try {
                    StringTokenizer st = new StringTokenizer(l, "\t<>");
                    String uriE = st.nextToken();
                    String uriC = st.nextToken();
                    Integer idE = getEntityIdFromUri(uriE);//entityIdFromUri.get(uriE);
                    Integer idC = classIdFromUri.get(uriC);
                    if (!uriC.equals(THING) && idE != null && idC != null && entityLabels[idE] != null && classLabels[idC] != null) {
                        //we ignore the classes without label and the classes of entities not used in triples
                        //we also ignore thing as class, since every entity is implicitly a thing
                        HashSet<Integer> classes = entityClasses[idE];
                        if (classes == null) {
                            classes = new HashSet<>();
                            entityClasses[idE] = classes;
                            count++;
                        }
                        //check if classes contains an ancestor of uriC,
                        //or if uriC is an ancestor of any class in classes
                        boolean add = true;
                        for (Iterator<Integer> it = classes.iterator(); it.hasNext();) {
                            Integer c = it.next();
                            if (classAncestors[c].contains(idC)) {
                                add = false; //we don't add class, beacause c is a descendant of class
                                break;
                            } else if (classAncestors[idC].contains(c)) {
                                it.remove(); //we remove c beacause uriC is a descendant of c
                            }
                        }
                        if (add) {
                            classes.add(idC);
                            notEmptyClasses.add(idC);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Failed to load class: " + l);
                }
                l = in.readLine();
            }
        }
        System.out.println(count + " entities have been assigned a non-thing class");
        count = 0;
        //now, set Thing as class of entities without a class
        for (int i = 1; i < entityClasses.length; i++) {
            if (entityClasses[i] == null && entityLabels[i] != null) {
                entityClasses[i] = new HashSet<>();
                entityClasses[i].add(thingId);
                count++;
            }
        }
        System.out.println(count + " entities have been assigned thing class");
        //now drop the classes without entities and without descendant classes - asking for those classes would produce empty results could confuse the user
        for (int i = 1; i < classLabels.length; i++) {
            if (i != thingId && !notEmptyClasses.contains(i) && classDescendants[i].isEmpty()) {
                classLabels[i] = null;
                classUri[i] = null;
            }
        }
    }
    /*
     The file basic_type_literal_type must contain one row per basic type, with the form
     uri \t literal_type
     where literal type can be Double, Date, String, Boolean
     e.g.
     http://dbpedia.org/datatype/centimetre  Double
     uri and basic type can be inside angular brackets, e.g.
     <http://dbpedia.org/datatype/centimetre>  <Double>
     or
     <http://dbpedia.org/datatype/centimetre>  <Double>
     or
     <http://dbpedia.org/datatype/centimetre>  <Double>
     no other separators are supposed to be used
     */

    private void loadBasicTypesMapping() throws Exception {
        System.out.println("Loading basic types mappings");
        try (BufferedReader in = new BufferedReader(new FileReader(basePathInput + "basic_types_literal_types"))) {
            String l = in.readLine();
            while (l != null) {
                StringTokenizer st = new StringTokenizer(l, "\t<>");
                String uri = st.nextToken();
                String literal = st.nextToken();
                //System.out.println(uri + "\t" + label);
                basicTypesMapping.put(uri, literal);
                l = in.readLine();
            }
        }
    }

    //thios not used for the following reason: if an entity of class C has a property p, then C will have the property p - if no entities of C have property p, we can safely avoid to ask for "p of C", even if a superclass of C has p - in fact, no result would be returned
    private void propagatePropertiesToDescendantClasses(HashSet<Integer>[] classProperties, int claz) {
        HashSet<Integer> properties = classProperties[claz];
        if (properties == null) {
            properties = new HashSet<>();
            classProperties[claz] = properties;
        }
        for (Integer child : classChildren[claz]) {
            if (classProperties[child] == null) {
                classProperties[child] = new HashSet<>();
            }
            classProperties[child].addAll(properties);
            propagatePropertiesToDescendantClasses(classProperties, child);
        }
    }

        private void propagatePropertiesToAncestorClasses(HashSet<Integer>[] classProperties, int claz) {
        for (int childClass : classChildren[claz]) {
            propagatePropertiesToAncestorClasses(classProperties, childClass);
        }
        HashSet<Integer> properties = classProperties[claz];
        if (properties == null) {
            properties = new HashSet<>();
            classProperties[claz] = properties;
        }
        for (Integer child : classChildren[claz]) {
            properties.addAll(classProperties[child]);
        }
    }

    private HashSet<Integer> findCommonLowestAncestor(int c1, int c2) {
        HashSet<Integer> res = new HashSet<>();
        if (c1 == c2) {
            res.add(c1);
            return res;
        }
        if (classAncestors[c1].contains(c2)) {
            res.add(c2);
            return res;
        } else if (classAncestors[c2].contains(c1)) {
            res.add(c1);
            return res;
        }
        //find common ancestors
        HashSet<Integer> temp = new HashSet<>();
        for (int a : classAncestors[c1]) {
            if (classAncestors[c2].contains(a)) {
                temp.add(a);
            }
        }
        for (int candidateClass : temp) {
            boolean add = true;
            for (Iterator<Integer> it = res.iterator(); it.hasNext();) {
                int existingClass = it.next();
                if (classAncestors[existingClass].contains(candidateClass)) { //candidate class is an ancestor of existing class
                    add = false;
                    break;
                } else if (classAncestors[candidateClass].contains(existingClass)) { //existing class is an ancestor of candodate class
                    it.remove();
                    //don't break - candidate class could be the ancestor of other existing classes in res
                }
            }
            if (add) {
                res.add(candidateClass);
            }
        }
        return res;
    }

    private HashSet<Integer> findCommonLowestAncestor(Set<Integer> classes) {
        HashSet<Integer> finalClasses = new HashSet<>(classes);
        while (finalClasses.size() > 1) {
            Iterator<Integer> it = finalClasses.iterator();
            int c1 = it.next();
            it.remove();
            int c2 = it.next();
            it.remove();
            HashSet<Integer> cas = findCommonLowestAncestor(c1, c2);
            for (int ca : cas) {
                finalClasses.add(ca);
            }
        }
        return finalClasses;
    }

    private void updateOutAndInEntityAndLiteralTypeProperties(Integer property, Integer subj, Integer entityVal, String literalType) {
        if (entityOutProperties[subj] == null) {
            entityOutProperties[subj] = new HashSet<>();
        }
        entityOutProperties[subj].add(property);
        if (entityVal != null) {
            if (entityInProperties[entityVal] == null) {
                entityInProperties[entityVal] = new HashSet<>();
            }
            entityInProperties[entityVal].add(property);
        }
        if (literalType != null) {
            literalTypesInProperties.get(literalType).add(property);
        }
    }

    private void processTriples() throws Exception {
        int droppedEntityTriples = 0;
        int droppedLiteralTriples = 0;
        try (PrintWriter out = new PrintWriter(new FileOutputStream(basePathInput + "dropped_triples", false), true)) {
            System.out.println("Dropping triples with undefined elements");
            for (int i = 0; i < entityTriplesSubjects.length; i++) {
                int sbj = entityTriplesSubjects[i];
                int val = entityTriplesValues[i];
                int attr = entityTriplesProperties[i];
                if (entityUriWithPrefix[sbj] == null || entityUriWithPrefix[val] == null || propertyUri[attr] == null) {//if (entityUri[sbj] == null || entityUri[val] == null || propertyUri[attr] == null) {
                    out.println(entityUriWithPrefix[sbj] + "\t" + propertyUri[attr] + "\t" + entityUriWithPrefix[val]);
                    entityTriplesSubjects[i] = 0;
                    droppedEntityTriples++;
                } else {
                    propertyCount[attr]++;
                }
            }
            for (String type : literalTypes) {
                for (int i = 0; i < literalTriplesSubjects.get(type).length; i++) {
                    int sbj = literalTriplesSubjects.get(type)[i];
                    int attr = literalTriplesProperties.get(type)[i];
                    if (entityUriWithPrefix[sbj] == null || propertyUri[attr] == null) {//if (entityUri[sbj] == null || propertyUri[attr] == null) {
                        out.println(entityUriWithPrefix[sbj] + "\t" + propertyUri[attr]);
                        literalTriplesSubjects.get(type)[i] = 0;
                        droppedLiteralTriples++;
                    } else {
                        propertyCount[attr]++;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Dropped " + droppedEntityTriples + " triples with entity value and " + droppedLiteralTriples + " with literal value");

        System.out.println("Scanning the triples to compute out-propertys of entities and in-propertys of entities and literal basic types");
        //first compute out-propertys and in-propertys of entities
        //and in-propertys of basic types
        //<sbj, attr, val>, where val is an entity -> add attr to out-propertys of sbj and in-propertys of val
        int c = 0;
        for (int i = 0; i < entityTriplesSubjects.length; i++) {
            if (entityTriplesSubjects[i] == 0) { //it was previously dropped
                continue;
            }
            int sbj = entityTriplesSubjects[i];
            int attr = entityTriplesProperties[i];
            int val = entityTriplesValues[i];
            if (entityUriWithPrefix[sbj] != null && propertyUri[attr] != null && entityUriWithPrefix[val] != null) {//if (entityUri[sbj] != null && propertyUri[attr] != null && entityUri[val] != null) {
                updateOutAndInEntityAndLiteralTypeProperties(attr, sbj, val, null);
            }
            c++;
            if (c % 1000000 == 0) {
                System.out.println("Processed " + (c / 1000000) + "M triples");
            }
        }
        entityTriplesSubjects = null;
        entityTriplesProperties = null;
        entityTriplesValues = null;
        System.gc();
        //<sbj, attr, type>, where type is a basic type -> add attr to out-propertys of sbj and in-propertys of type
        for (String literalType : literalTypes) {
            for (int i = 0; i < literalTriplesSubjects.get(literalType).length; i++) {
                if (literalTriplesSubjects.get(literalType)[i] == 0) { //it was previously dropped
                    continue;
                }
                int sbj = literalTriplesSubjects.get(literalType)[i];
                int attr = literalTriplesProperties.get(literalType)[i];
                if (entityUriWithPrefix[sbj] != null && propertyUri[attr] != null) {//if (entityUri[sbj] != null && propertyUri[attr] != null) {
                    updateOutAndInEntityAndLiteralTypeProperties(attr, sbj, null, literalType);
                    propertyHasLiteralRange[attr] = true;
                }
                c++;
                if (c % 1000000 == 0) {
                    System.out.println("Processed " + (c / 1000000) + "M triples");
                }
            }
        }
        literalTriplesSubjects = null;
        literalTriplesProperties = null;
        System.gc();
        System.out.println("Scanning the entity out-propertys to compute out-propertys of classes");
        //now it is possible to compute the out-propertys and in-propertys of classes
        //entityOutProperties of e contains a -> add a to classOutProperties of all the classes of e
        classOutProperties = new HashSet[classUri.length];
        for (int i = 1; i < entityOutProperties.length; i++) {
            if (entityOutProperties[i] != null && entityClasses[i] != null) {
                for (int property : entityOutProperties[i]) {
                    for (int claz : entityClasses[i]) {
                        if (classOutProperties[claz] == null) {
                            classOutProperties[claz] = new HashSet<>();
                        }
                        classOutProperties[claz].add(property);
                    }
                }
            }
        }
        //System.out.println("Propagating the out-propertys to descendant classes");
        //propagatePropertiesToDescendantClasses(classOutProperties, thingId);
        System.out.println("Propagating the out-propertys to ancestor classes");
        propagatePropertiesToAncestorClasses(classOutProperties, thingId);
        if (printFiles) {
            System.out.println("Writing the classOutProperties");
            try (PrintWriter out = new PrintWriter(new FileOutputStream(basePathOutput + "class_out_propertys", false), true)) {
                for (int i = 1; i < classOutProperties.length; i++) {
                    if (i % 10 == 0) {
                        out.flush();
                    }
                    if (classOutProperties[i] != null) {
                        out.print(classUri[i]);
                        for (Integer a : classOutProperties[i]) {
                            out.print("\t" + propertyUri[a]);
                        }
                        out.println();
                        /*
                         System.out.print(classLabels[i] + " - outProperties: ");
                         for (Integer a : classOutProperties[i]) {
                         System.out.print("\t" + propertyLabels[a]);
                         }
                         System.out.println();*/
                    }
                }
            }
        }
        System.out.println("Scanning the entity in-propertys to compute in-propertys of classes");
        //entityInProperties of e contains a -> add a to classInProperties of all the classes of e
        classInProperties = new HashSet[classUri.length];
        for (int i = 1; i < entityInProperties.length; i++) {
            if (entityInProperties[i] != null && entityClasses[i] != null) {
                for (int property : entityInProperties[i]) {
                    for (int claz : entityClasses[i]) {
                        if (classInProperties[claz] == null) {
                            classInProperties[claz] = new HashSet<>();
                        }
                        classInProperties[claz].add(property);
                    }
                }
            }
        }
        //System.out.println("Propagating the in-propertys to descendant classes");
        //propagatePropertiesToDescendantClasses(classInProperties, thingId);
        System.out.println("Propagating the in-propertys to ancestor classes");
        propagatePropertiesToAncestorClasses(classOutProperties, thingId);
        if (printFiles) {
            System.out.println("Writing the classInProperties");
            try (PrintWriter out = new PrintWriter(new FileOutputStream(basePathOutput + "class_in_propertys", false), true)) {
                for (int i = 1; i < classInProperties.length; i++) {
                    if (i % 10 == 0) {
                        out.flush();
                    }
                    if (classInProperties[i] != null) {
                        out.print(classUri[i]);
                        for (Integer a : classInProperties[i]) {
                            out.print("\t" + propertyUri[a]);
                        }
                        out.println();
                        /*
                         System.out.print(classLabels[i] + " - inProperties: ");
                         for (Integer a : classInProperties[i]) {
                         System.out.print("\t" + propertyLabels[a]);
                         }
                         System.out.println();*/
                    }
                }
            }
        }
        System.out.println("Scanning the triples to compute out- and in-propertys of propertys");
        //now it is possible to compute the out-propertys and in-propertys of propertys
        //<t[0], t[1], t[2]> -> add t[1] to outProperties[property] for each property in entityInProperties[t[0]]
        /* OLD WAY
         c = 0;
         propertyOutProperties = new HashSet[propertyUri.length];
         for (int[] t : entityTriples) {
         if (entityInProperties[t[0]] != null) {
         for (int property : entityInProperties[t[0]]) {
         if (propertyOutProperties[property] == null) {
         propertyOutProperties[property] = new HashSet<>();
         }
         propertyOutProperties[property].add(t[1]);
         }
         }
         c++;
         if (c % 1000000 == 0) {
         System.out.println("Processed " + (c / 1000000) + "M triples");
         }
         }
         for (LinkedList<int[]> lt : literalTriples.values()) {
         for (int[] t : lt) {
         if (entityInProperties[t[0]] != null) {
         for (int property : entityInProperties[t[0]]) {
         if (propertyOutProperties[property] == null) {
         propertyOutProperties[property] = new HashSet<>();
         }
         propertyOutProperties[property].add(t[1]);
         }
         }
         c++;
         if (c % 1000000 == 0) {
         System.out.println("Processed " + (c / 1000000) + "M triples");
         }
         }
         }
         */
        for (int entity = 1; entity < entityInProperties.length; entity++) {
            if (entityInProperties[entity] != null) {
                for (int property : entityInProperties[entity]) {
                    if (entityOutProperties[entity] != null && !entityOutProperties[entity].isEmpty()) {
                        if (propertyOutProperties[property] == null) {
                            propertyOutProperties[property] = new HashSet<>();
                        }
                        propertyOutProperties[property].addAll(entityOutProperties[entity]);
                    }
                    if (propertyInProperties[property] == null) {
                        propertyInProperties[property] = new HashSet<>();
                    }
                    propertyInProperties[property].addAll(entityInProperties[entity]);
                }
            }
        }
        //I will use the literalTypesInProperties when I index the property with rangeOf
        /*
         for (String type : literalTypes) {
         for (int property : literalTypesInProperties.get(type)) {
         if (propertyInProperties[property] == null) {
         propertyInProperties[property] = new HashSet<>();
         }
         propertyInProperties[property].addAll(literalTypesInProperties.get(type));
         }
         }
         */
        //System.out.println("Scanning the triples to compute in-propertys of propertys");
        //<t[0], t[1], t[2]> -> add t[1] to inProperties[property] for each property in entityInProperties[t[2]]
        /*OLD WAY
         c = 0;
         propertyInProperties = new HashSet[propertyUri.length];
         for (int[] t : entityTriples) {
         if (entityInProperties[t[2]] != null) {
         for (int property : entityInProperties[t[2]]) {
         if (propertyInProperties[property] == null) {
         propertyInProperties[property] = new HashSet<>();
         }
         propertyInProperties[property].add(t[1]);
         }
         }
         c++;
         if (c % 1000000 == 0) {
         System.out.println("Processed " + (c / 1000000) + "M triples");
         }
         }
         //<t[0], t[1], type> -> add t[1] to inProperties[property] for each property in literalValueInProperties[type]
         //it is possible to avoid the scan of the triples, since we already know the inProperty of each literal basic type
         for (String type : literalTriples.keySet()) {
         for (int property1 : literalTypesInProperties.get(type)) {
         if (propertyInProperties[property1] == null) {
         propertyInProperties[property1] = new HashSet<>();
         }
         for (int property2 : literalTypesInProperties.get(type)) {
         propertyInProperties[property1].add(property2);
         }
         }
         }
         */
        //write the in/Out-Entity/Class-Properties
        if (printFiles) {
            System.out.println("Writing the entityInProperties");
            try (PrintWriter out = new PrintWriter(new FileOutputStream(basePathOutput + "entity_in_propertys", false), true)) {
                for (int i = 1; i < entityInProperties.length; i++) {
                    if (i % 100 == 0) {
                        out.flush();
                    }
                    if (entityInProperties[i] != null) {
                        out.print(entityUriWithPrefix[i]);//out.print(entityUri[i]);
                        for (Integer a : entityInProperties[i]) {
                            out.print("\t" + propertyUri[a]);
                        }
                        out.println();
                        if (i % 100000 == 0) {
                            System.out.print(entityLabels[i] + " - inProperties: ");
                            for (Integer a : entityInProperties[i]) {
                                System.out.print("\t" + propertyLabels[a]);
                            }
                            System.out.println();
                        }
                    }
                }
            }
        }
        if (printFiles) {
            System.out.println("Writing the entityOutProperties");
            try (PrintWriter out = new PrintWriter(new FileOutputStream(basePathOutput + "entity_out_propertys", false), true)) {
                for (int i = 1; i < entityOutProperties.length; i++) {
                    if (i % 100 == 0) {
                        out.flush();
                    }
                    if (entityOutProperties[i] != null) {
                        out.print(entityUriWithPrefix[i]);//out.print(entityUri[i]);
                        for (Integer a : entityOutProperties[i]) {
                            out.print("\t" + propertyUri[a]);
                        }
                        out.println();
                        if (i % 100000 == 0) {
                            System.out.print(entityLabels[i] + " - outProperties: ");
                            for (Integer a : entityOutProperties[i]) {
                                System.out.print("\t" + propertyLabels[a]);
                            }
                            System.out.println();
                        }
                    }
                }
            }
        }
        //write the literalInProperties
        if (printFiles) {
            try (PrintWriter out = new PrintWriter(new FileOutputStream(basePathOutput + "literal_types_in_propertys", false), true)) {
                for (Map.Entry<String, HashSet<Integer>> e : literalTypesInProperties.entrySet()) {
                    out.print(e.getKey());
                    for (Integer a : e.getValue()) {
                        out.print("\t" + propertyUri[a]);
                    }
                    out.println();
                    /*
                     System.out.print(e.getKey() + " - inProperties: ");
                     for (Integer a : e.getValue()) {
                     System.out.print("\t" + propertyLabels[a]);
                     }
                     System.out.println();
                     */
                }
            }
        }
        if (printFiles) {
            try (PrintWriter out = new PrintWriter(new FileOutputStream(basePathOutput + "property_in_propertys", false), true)) {
                for (int property = 1; property < propertyUri.length; property++) {
                    if (property % 10 == 0) {
                        out.flush();
                    }
                    if (propertyInProperties[property] != null && !propertyInProperties[property].isEmpty()) {
                        out.print(propertyUri[property]);
                        for (Integer a : propertyInProperties[property]) {
                            out.print("\t" + propertyUri[a]);
                        }
                        out.println();
                        /*
                         System.out.print(propertyLabels[property] + " - inProperties: ");
                         for (Integer a : propertyInProperties[property]) {
                         System.out.print("\t" + propertyLabels[a]);
                         }
                         System.out.println();
                         */
                    }
                }
            }
        }
        if (printFiles) {
            try (PrintWriter out = new PrintWriter(new FileOutputStream(basePathOutput + "property_out_propertys", false), true)) {
                for (int property = 1; property < propertyUri.length; property++) {
                    if (property % 10 == 0) {
                        out.flush();
                    }
                    if (propertyOutProperties[property] != null && !propertyOutProperties[property].isEmpty()) {
                        out.print(propertyUri[property]);
                        for (Integer a : propertyOutProperties[property]) {
                            out.print("\t" + propertyUri[a]);
                        }
                        out.println();
                        /*
                         System.out.print(propertyLabels[property] + " - outProperties: ");
                         for (Integer a : propertyOutProperties[property]) {
                         System.out.print("\t" + propertyLabels[a]);
                         }
                         System.out.println();
                         */
                    }
                }
            }
        }
    }

    private static void indexOntologyElement(IndexWriter writer, OntologyElementToken e, Collection<String> domainOf, Collection<String> rangeOf, Collection<String> extendedDomain) throws Exception {
        Document doc = new Document();
        doc.add(new Field("label", e.getLabel(), TextField.TYPE_NOT_STORED));
        doc.add(new IntField("id", e.getId(), IntField.TYPE_STORED));
        doc.add(new Field("type", e.getType(), StringField.TYPE_NOT_STORED));
        if (domainOf != null) {
            for (String d : domainOf) { //the first element is the URI
                doc.add(new Field("domainOfProperty", d, StringField.TYPE_NOT_STORED));
            }
        }
        if (rangeOf != null) {
            for (String r : rangeOf) { //the first element is the URI
                doc.add(new Field("rangeOfProperty", r, StringField.TYPE_NOT_STORED));
            }
        }
        if (extendedDomain != null) {
            for (String d : extendedDomain) { //the first element is the URI
                doc.add(new Field("propertyDomain", d, StringField.TYPE_NOT_STORED));
            }
        }
        writer.addDocument(doc);
    }
    /*
     private static void indexNonOntologyElement(IndexWriter writer, IndexedToken e, Collection<String> domainOf, Collection<String> rangeOf, Collection<String> extendedDomain) throws Exception {
     Document doc = new Document();
     doc.add(new Field("label", e.getText(), TextField.TYPE_NOT_STORED));
     doc.add(new Field("id", e.getId(), TextField.TYPE_STORED));
     doc.add(new Field("type", e.getType(), TextField.TYPE_NOT_STORED));
     if (domainOf != null) {
     for (String d : domainOf) { //the first element is the URI
     doc.add(new Field("domainOfProperty", d, StringField.TYPE_NOT_STORED));
     }
     }
     if (rangeOf != null) {
     for (String r : rangeOf) { //the first element is the URI
     doc.add(new Field("rangeOfProperty", r, StringField.TYPE_NOT_STORED));
     }
     }
     if (extendedDomain != null) {
     for (String d : extendedDomain) { //the first element is the URI
     doc.add(new Field("propertyDomain", d, StringField.TYPE_NOT_STORED));
     }
     }
     writer.addDocument(doc);
     }
     */

    private void indexEntities(IndexWriter writer, HashMap<Integer, IndexedToken> elements) throws Exception {
        for (int i = 1; i < entityUriWithPrefix.length; i++) {//for (int i = 1; i < entityUri.length; i++) {
            if (entityUriWithPrefix[i] != null) {//if (entityUri[i] != null) {
                HashSet<String> domainOf = new HashSet<>();
                HashSet<String> rangeOf = new HashSet<>();
                if (entityOutProperties[i] != null) {
                    for (int a : entityOutProperties[i]) {
                        domainOf.add(propertyUri[a]);
                    }
                }
                if (entityInProperties[i] != null) {
                    for (int a : entityInProperties[i]) {
                        rangeOf.add(propertyUri[a]);
                    }
                }
                for (String label : entityLabels[i]) {
                    EntityToken element = new EntityToken(getEntityCompleteUri(entityUriWithPrefix[i]), label, false);//EntityToken element = new EntityToken(entityUri[i], label);
                    indexOntologyElement(writer, element, domainOf, rangeOf, null);
                    elements.put(element.getId(), element);
                }
            }
        }
        entityOutProperties = null;
        entityInProperties = null;
        System.gc();
    }

    private void indexClasses(IndexWriter writer, HashMap<Integer, IndexedToken> elements) throws Exception {
        HashSet<Character> vowels = new HashSet<>();
        vowels.add('a');
        vowels.add('e');
        vowels.add('i');
        vowels.add('o');
        vowels.add('u');
        for (int i = 1; i < classUri.length; i++) {
            if (classUri[i] != null) {
                HashSet<String> domainOf = new HashSet<>();
                HashSet<String> rangeOf = new HashSet<>();
                if (classOutProperties[i] == null) {
                    classOutProperties[i] = new HashSet<>();
                }
                for (int a : classOutProperties[i]) {
                    domainOf.add(propertyUri[a]);
                }
                if (classInProperties[i] == null) {
                    classInProperties[i] = new HashSet<>();
                }
                for (int a : classInProperties[i]) {
                    rangeOf.add(propertyUri[a]);
                }
                for (String label : classLabels[i]) {
                    label = label.toLowerCase();
                    ClassToken elementSingular = new ClassToken(classUri[i], label, IndexedToken.SINGULAR, false);
                    indexOntologyElement(writer, elementSingular, domainOf, rangeOf, null);
                    elements.put(elementSingular.getId(), elementSingular);
                    //now create the plural form
                    String pLabel;
                    if (label.endsWith("y") && !vowels.contains(label.charAt(label.length() - 2))) {
                        pLabel = label.substring(0, label.length() - 1) + "ies";
                    } else if (label.endsWith("s") || label.endsWith("sh") || label.endsWith("ch") || label.endsWith("x") || label.endsWith("z")) {
                        pLabel = label + "es";
                    } else if (label.equals("person")) {
                        pLabel = "people";
                    } else {
                        pLabel = label + "s";
                    }
                    ClassToken elementPlural = new ClassToken(classUri[i], pLabel, IndexedToken.PLURAL, false);
                    indexOntologyElement(writer, elementPlural, domainOf, rangeOf, null);
                    elements.put(elementPlural.getId(), elementPlural);
                }
            }
        }
    }

    private void indexProperties(IndexWriter writer, HashMap<Integer, IndexedToken> elements) throws Exception {
        //precompute the domains of propertys
        HashSet<String>[] propertyDomains = new HashSet[propertyUri.length];
        //the domain of an property a is the set of classes and propertys having a in their outProperty
        for (int claz = 1; claz < classOutProperties.length; claz++) {
            if (classOutProperties[claz] != null && classUri[claz] != null) {
                for (int a : classOutProperties[claz]) {
                    if (propertyDomains[a] == null) {
                        propertyDomains[a] = new HashSet<>();
                    }
                    propertyDomains[a].add(classUri[claz]);
                }
            }
        }
        for (int property = 1; property < propertyOutProperties.length; property++) {
            if (propertyOutProperties[property] != null) {
                for (int a : propertyOutProperties[property]) {
                    if (propertyDomains[a] == null) {
                        propertyDomains[a] = new HashSet<>();
                    }
                    propertyDomains[a].add(propertyUri[property]);
                }
            }
        }
        //precompute the literal ranges of every property
        HashSet<String>[] propertyLiteralRanges = new HashSet[propertyUri.length];
        for (int i = 1; i < propertyLiteralRanges.length; i++) {
            propertyLiteralRanges[i] = new HashSet<>();
        }
        for (String literalType : literalTypesInProperties.keySet()) {
            for (int property : literalTypesInProperties.get(literalType)) {
                propertyLiteralRanges[property].add(literalType);
            }
        }
        for (int property = 1; property < propertyUri.length; property++) {            
            if (propertyUri[property] != null) {
                HashSet<String> domainOf = new HashSet<>();
                if (propertyOutProperties[property] != null) {
                    for (int a : propertyOutProperties[property]) {
                        domainOf.add(propertyUri[a]);
                    }
                }
                HashSet<String> rangeOf = new HashSet<>();
                if (propertyInProperties[property] != null) {
                    for (int a : propertyInProperties[property]) {
                        rangeOf.add(propertyUri[a]);
                    }
                }
                for (String type : literalTypes) {
                    if (literalTypesInProperties.get(type).contains(property)) {
                        for (int a : literalTypesInProperties.get(type)) {
                            rangeOf.add(propertyUri[a]);
                        }
                    }
                }
                for (String label : propertyLabels[property]) {
                    HashSet<String> aDomains = propertyDomains[property];
                    PropertyToken element = new PropertyToken(propertyUri[property], label, IndexedToken.UNDEFINED, IndexedToken.UNDEFINED, propertyOutProperties[property] != null && !propertyOutProperties[property].isEmpty(), propertyHasLiteralRange[property], false);
                    indexOntologyElement(writer, element, domainOf, rangeOf, aDomains);
                    element.setPropertyAndClassDomain(aDomains);
                    element.addBasicTypeRanges(propertyLiteralRanges[property]);
                    elements.put(element.getId(), element);
                }
            }
        }
        classOutProperties = null;
        classInProperties = null;
        propertyOutProperties = null;
        propertyInProperties = null;
        System.gc();
    }

    public void start() throws Exception {
        long t = System.currentTimeMillis();
        loadBasicTypesMapping();
        System.out.println(System.currentTimeMillis() - t);
        t = System.currentTimeMillis();
        loadTriples();
        System.out.println(System.currentTimeMillis() - t);
        t = System.currentTimeMillis();
        loadPropertyLabels();
        System.out.println(System.currentTimeMillis() - t);
        t = System.currentTimeMillis();
        loadClassLabels();
        System.out.println(System.currentTimeMillis() - t);
        t = System.currentTimeMillis();
        loadClassHierarchy();
        System.out.println(System.currentTimeMillis() - t);
        t = System.currentTimeMillis();
        loadEntityLabels();
        System.out.println(System.currentTimeMillis() - t);
        t = System.currentTimeMillis();
        loadEntityClasses();
        System.out.println(System.currentTimeMillis() - t);
        t = System.currentTimeMillis();
        entityIdFromUriWithPrefix = null;//entityIdFromUri = null;
        classIdFromUri = null;
        propertyIdFromUri = null;
        System.gc();
        processTriples();
        System.out.println(System.currentTimeMillis() - t);
        t = System.currentTimeMillis();
        HashMap<String, Analyzer> analyzerMap = new HashMap<>();
        analyzerMap.put("label", new EnglishAnalyzer(CharArraySet.EMPTY_SET));
        analyzerMap.put("id", new WhitespaceAnalyzer());
        analyzerMap.put("type", new WhitespaceAnalyzer());
        analyzerMap.put("domainOfProperty", new WhitespaceAnalyzer());
        analyzerMap.put("rangeOfProperty", new WhitespaceAnalyzer());
        analyzerMap.put("propertyDomain", new WhitespaceAnalyzer());
        Analyzer analyzer = new PerFieldAnalyzerWrapper(new WhitespaceAnalyzer(), analyzerMap);
        HashMap<Integer, IndexedToken> elements = new HashMap<>();
        try (FSDirectory directory = FSDirectory.open(Paths.get(basePathOutput + "lucene"))) {
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            try (IndexWriter writer = new IndexWriter(directory, iwc)) {
                System.out.println("Indexing entities");
                indexEntities(writer, elements);
                System.out.println(System.currentTimeMillis() - t);
                t = System.currentTimeMillis();
                System.out.println("Indexing classes");
                indexClasses(writer, elements);
                System.out.println(System.currentTimeMillis() - t);
                t = System.currentTimeMillis();
                System.out.println("Indexing propertys");
                indexProperties(writer, elements);
                System.out.println(System.currentTimeMillis() - t);
                t = System.currentTimeMillis();
                /*
                 System.out.println("Indexing constraint uriToPrefix");
                 indexConstraintPrefixes(writer, elements);
                 System.out.println(System.currentTimeMillis() - t);
                 t = System.currentTimeMillis();
                 */
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //save elements to file
        System.out.println("Creating the trie");
        Trie trie = new Trie();
        int c = 0;
        for (IndexedToken it : elements.values()) {
            trie.add(it.getText());
            c++;
            if (c % 100000 == 0) {
                System.out.println(c + " elements added to the trie");
            }
        }
        System.out.println(c + " elements added to the trie");
        c = 0;
        for (IndexedToken it : elements.values()) {
            String suffix = trie.getOneSuffix(it.getText());
            if (suffix != null) {
                it.setPrefix(true);
                c++;
            }
        }
        System.out.println(c + " are prefix of another element");
        System.out.println("Serializing the tokens");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(basePathOutput + "elements"))) {
            oos.writeObject(elements);
            oos.writeInt(IndexedToken.counter);
        }
    }

    public static void main(String... args) throws Exception {
        String fn1 = null, fn2 = null;
        if (args != null && args.length == 2) {
            fn1 = args[0];
            fn2 = args[1];
        } else {
            //fn1 = "/home/massimo/aquawd/dbpedia-ontology-extended-files/";
            //fn2 = "/home/massimo/aquawd/processed-dbpedia-ontology-extended/";
            //fn1 = "/home/massimo/aquawd/dbpedia-ontology-files/";
            //fn2 = "/home/massimo/aquawd/processed-dbpedia-ontology/";
            //fn1 = "/home/massimo/aquawd/musicbrainz-old-ontology-files/";
            //fn2 = "/home/massimo/aquawd/processed-musicbrainz-old-ontology/";
            //fn1 = "/home/massimo/aquawd/biomedical-ontology-files/";
            //fn2 = "/home/massimo/aquawd/processed-biomedical-ontology/";     
            //fn1 = "/home/massimo/canalikbs/dbpedia/2015-10/processed/";
            //fn2 = "/home/massimo/canalikbs/dbpedia/2015-10/index_new/";
            //fn1 = "/home/massimo/canalikbs/musicbrainz/qald3/processed/";
            //fn2 = "/home/massimo/canalikbs/musicbrainz/qald3/index/";
            //fn1 = "/home/massimo/canalikbs/biomedical/qald4/processed/";
            //fn2 = "/home/massimo/canalikbs/biomedical/qald4/index/";
            fn1 = "/home/massimo/canalikbs/movie/processed/";
            fn2 = "/home/massimo/canalikbs/movie/index/";            

        }
        long start = System.currentTimeMillis();
        System.out.println("Started at " + new Date());
        new BuildIndex(fn1, fn2).start();
        System.out.println("Ended at " + new Date());
        long time = System.currentTimeMillis() - start;
        long sec = time / 1000;
        System.out.println("The process took " + (sec / 60) + "'" + (sec % 60) + "." + (time % 1000) + "\"");
    }
}
