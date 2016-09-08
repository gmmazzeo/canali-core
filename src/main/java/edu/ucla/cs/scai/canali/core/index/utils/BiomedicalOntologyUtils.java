/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
//In order to run this file, you must download the following file
//http://greententacle.techfak.uni-bielefeld.de/~cunger/qald/4/data/biomed_dumps.zip
//Uncompress the zip file in your working directyory, thus obtaining
//diseasome.nt, drugbank_dump.nt, and sider_dump.nt
//in diseadom replace 
//<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/diseases> <http://www.w3.org/2002/07/owl#sameAs>  <http://dbpedia.org/ontology/Disease>
//with 
//<http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/diseases> <http://www.w3.org/2002/07/owl#equivalentClass>  <http://dbpedia.org/ontology/Disease>
//replace the label of drugs in sider, by making it singular instead of plural
public class BiomedicalOntologyUtils {

    String downloadedFilesPath, destinationPath;
    HashMap<String, Integer> entityIds = new HashMap<>();
    HashMap<String, Integer> classIds = new HashMap<>();
    HashMap<String, Integer> propertyIds = new HashMap<>();
    String[] entityById;
    HashSet<Integer>[] sameAsEdges;
    String[] classById;
    HashSet<Integer>[] equivalentClassEdges;
    String[] propertyById;
    HashSet<Integer>[] equivalentPropertyEdges;
    HashSet<String> classes = new HashSet<>();
    HashSet<String> properties = new HashSet<>();
    HashMap<String, HashSet<String>> classLabels = new HashMap<>();
    HashMap<String, HashSet<String>> propertyLabels = new HashMap<>();
    HashMap<String, HashSet<String>> entityLabels = new HashMap<>();
    HashMap<String, HashSet<String>> entityClasses = new HashMap<>();
    int[] sameAs;
    int[] equivalentClass;
    int[] equivalentProperty;
    private static final String[] fileNames = {"diseasome.nt", "drugbank_dump.nt", "sider_dump.nt"};

    public BiomedicalOntologyUtils(String downloadedFilesPath, String destinationPath) throws Exception {
        if (!downloadedFilesPath.endsWith(File.separator)) {
            downloadedFilesPath += File.separator;
        }
        if (!destinationPath.endsWith(File.separator)) {
            destinationPath += File.separator;
        }
        this.downloadedFilesPath = downloadedFilesPath;
        this.destinationPath = destinationPath;
    }

    private void computeSameAsGroups() throws IOException {
        //load all entities and assign an id to them
        //dbpedia entites are loaded first
        String regex = "(\\s|\\t)*<([^<>]*)>(\\s|\\t)*<([^<>]*)>(\\s|\\t)*(<|\")(.*)(>|\")";
        Pattern p = Pattern.compile(regex);
        for (String fileName : fileNames) {
            try (
                    BufferedReader in = new BufferedReader(new FileReader(downloadedFilesPath + fileName))) {
                String l = in.readLine();
                while (l != null) {
                    Matcher m = p.matcher(l);
                    if (m.find()) {
                        String s = m.group(2);
                        if ((s.startsWith("http://www.dbpedia.org/resource") || s.startsWith("http://dbpedia.org/resource")) && !entityIds.containsKey(s) && !classIds.containsKey(s) && !propertyIds.containsKey(s)) {
                            entityIds.put(s, entityIds.size() + 1);
                        }
                        String v = m.group(7);
                        if ((v.startsWith("http://www.dbpedia.org/resource") || v.startsWith("http://dbpedia.org/resource")) && !entityIds.containsKey(v) && !classIds.containsKey(s) && !propertyIds.containsKey(s)) {
                            entityIds.put(v, entityIds.size() + 1);
                        }
                    }
                    l = in.readLine();
                }
            }
        }

        //now non-dpedia entities are loaded: http://www4.wiwiss.fu-berlin.de, http://data.linkedct.org, http://purl.org, http://bio2rdf.org, http://www.ncbi.nlm.nih.gov        
        for (String fileName : fileNames) {
            try (
                    BufferedReader in = new BufferedReader(new FileReader(downloadedFilesPath + fileName))) {
                String l;
                while ((l = in.readLine()) != null) {
                    Matcher m = p.matcher(l);
                    if (m.find()) {
                        String s = m.group(2);
                        if (s.startsWith("http://www4.wiwiss.fu-berlin.de")
                                && !entityIds.containsKey(s) && !classIds.containsKey(s) && !propertyIds.containsKey(s)) {
                            entityIds.put(s, entityIds.size() + 1);
                        }
                        String v = m.group(7);
                        if ((v.startsWith("http://www4.wiwiss.fu-berlin.de")
                                //|| v.startsWith("http://data.linkedct.org")
                                || v.startsWith("http://129.128.185.122"))
                                && !entityIds.containsKey(v) && !classIds.containsKey(s) && !propertyIds.containsKey(s)) {
                            entityIds.put(v, entityIds.size() + 1);
                        }
                    }
                    if (entityIds.size()==12413) {
                        System.out.println();
                    }
                }
            }
        }

        //create the sameAsEdges sets        
        sameAsEdges = new HashSet[entityIds.size() + 1];
        entityById = new String[entityIds.size() + 1];
        for (Map.Entry<String, Integer> e : entityIds.entrySet()) {
            entityById[e.getValue()] = e.getKey();
        }

        for (String fileName : fileNames) {
            try (
                    BufferedReader in = new BufferedReader(new FileReader(downloadedFilesPath + fileName))) {
                String l;
                while ((l = in.readLine()) != null) {
                    Matcher m = p.matcher(l);
                    if (m.find()) {
                        String a = m.group(4);
                        if (a.equals("http://www.w3.org/2002/07/owl#sameAs")) {
                            String s = m.group(2);
                            int idS = entityIds.get(s);
                            String v = m.group(7);
                            Integer idV = entityIds.get(v);
                            if (idV == null) {
                                continue;
                            }
                            if (sameAsEdges[idS] == null) {
                                sameAsEdges[idS] = new HashSet<>();
                            }
                            sameAsEdges[idS].add(idV);
                            if (sameAsEdges[idV] == null) {
                                sameAsEdges[idV] = new HashSet<>();
                            }
                            sameAsEdges[idV].add(idS);
                        }/* else if (a.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                         String s = m.group(2);
                         String v = m.group(7);
                         if (v.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property")) {
                         properties.add(s);
                         } else if (v.equals("http://www.w3.org/2000/01/rdf-schema#Class")) {
                         classes.add(s);
                         }
                         }*/

                    }
                    l = in.readLine();
                }
            }
        }
        sameAs = new int[entityIds.size() + 1];

        int i = 1;
        while (i < sameAs.length) {

            LinkedList<Integer> q = new LinkedList<>();
            q.addLast(i);
            while (!q.isEmpty()) {
                int j = q.removeFirst();
                if (sameAs[j] != 0) {
                    if (sameAs[j] != i) {
                        System.out.println("Error");
                        System.exit(0);
                    }
                } else {
                    sameAs[j] = i;
                    if (sameAsEdges[j] != null) {
                        for (int k : sameAsEdges[j]) {
                            q.addLast(k);
                        }
                    }
                }
            }

            i++;
            while (i < sameAs.length && sameAs[i] != 0) {
                i++;
            }
        }
    }

    private void computeEquivalentClassGroups() throws IOException {
        //load all classes and assign an id to them
        //dbpedia classes are loaded first
        String regex = "(\\s|\\t)*<([^<>]*)>(\\s|\\t)*<([^<>]*)>(\\s|\\t)*(<|\")(.*)(>|\")";
        Pattern p = Pattern.compile(regex);
        for (String fileName : fileNames) {
            try (
                    BufferedReader in = new BufferedReader(new FileReader(downloadedFilesPath + fileName))) {
                String l = in.readLine();
                while (l != null) {
                    Matcher m = p.matcher(l);
                    if (m.find()) {
                        String s = m.group(2);
                        String a = m.group(4);
                        String v = m.group(7);
                        if (a.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") && v.equals("http://www.w3.org/2000/01/rdf-schema#Class")
                                && (s.startsWith("http://www.dbpedia.org") || s.startsWith("http://dbpedia.org")) && !classIds.containsKey(s)) {
                            classIds.put(s, classIds.size() + 1);
                        } else if (a.equals("http://www.w3.org/2002/07/owl#equivalentClass")) {
                            if ((s.startsWith("http://www.dbpedia.org") || s.startsWith("http://dbpedia.org")) && !classIds.containsKey(s)) {
                                classIds.put(s, classIds.size() + 1);
                            }
                            if ((v.startsWith("http://www.dbpedia.org") || v.startsWith("http://dbpedia.org")) && !classIds.containsKey(v)) {
                                classIds.put(v, classIds.size() + 1);
                            }
                        }
                    }
                    l = in.readLine();
                }
            }
        }

        //now non-dpedia classes are loaded
        for (String fileName : fileNames) {
            try (
                    BufferedReader in = new BufferedReader(new FileReader(downloadedFilesPath + fileName))) {
                String l = in.readLine();
                while (l != null) {
                    Matcher m = p.matcher(l);
                    if (m.find()) {
                        String s = m.group(2);
                        String a = m.group(4);
                        String v = m.group(7);
                        if (a.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") && v.equals("http://www.w3.org/2000/01/rdf-schema#Class")
                                && !(s.startsWith("http://www.dbpedia.org") || s.startsWith("http://dbpedia.org")) && !classIds.containsKey(s)) {
                            classIds.put(s, classIds.size() + 1);
                        } else if (a.equals("http://www.w3.org/2002/07/owl#equivalentClass")) {
                            if (!(s.startsWith("http://www.dbpedia.org") || s.startsWith("http://dbpedia.org")) && !classIds.containsKey(s)) {
                                classIds.put(s, classIds.size() + 1);
                            }
                            if (!(v.startsWith("http://www.dbpedia.org") || v.startsWith("http://dbpedia.org")) && !classIds.containsKey(v)) {
                                classIds.put(v, classIds.size() + 1);
                            }
                        }
                    }
                    l = in.readLine();
                }
            }
        }

        //create the equivalentClassEdges sets        
        equivalentClassEdges = new HashSet[classIds.size() + 1];
        classById = new String[classIds.size() + 1];
        for (Map.Entry<String, Integer> e : classIds.entrySet()) {
            classById[e.getValue()] = e.getKey();
        }

        for (String fileName : fileNames) {
            try (
                    BufferedReader in = new BufferedReader(new FileReader(downloadedFilesPath + fileName))) {
                String l = in.readLine();
                while (l != null) {
                    Matcher m = p.matcher(l);
                    if (m.find()) {
                        String a = m.group(4);
                        if (a.equals("http://www.w3.org/2002/07/owl#equivalentClass")) {
                            String s = m.group(2);
                            int idS = classIds.get(s);
                            String v = m.group(7);
                            int idV = classIds.get(v);
                            if (equivalentClassEdges[idS] == null) {
                                equivalentClassEdges[idS] = new HashSet<>();
                            }
                            equivalentClassEdges[idS].add(idV);
                            if (equivalentClassEdges[idV] == null) {
                                equivalentClassEdges[idV] = new HashSet<>();
                            }
                            equivalentClassEdges[idV].add(idS);
                        }/* else if (a.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                         String s = m.group(2);
                         String v = m.group(7);
                         if (v.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property")) {
                         properties.add(s);
                         } else if (v.equals("http://www.w3.org/2000/01/rdf-schema#Class")) {
                         classes.add(s);
                         }
                         }*/

                    }
                    l = in.readLine();
                }
            }
        }
        //manually add an equivalence
        //http://www4.wiwiss.fu-berlin.de/sider/resource/sider/drugs equivalentClass 
        {
            String s = "http://www4.wiwiss.fu-berlin.de/sider/resource/sider/drugs";
            int idS = classIds.get(s);
            String v = "http://dbpedia.org/ontology/Drug";
            int idV = classIds.get(v);
            if (equivalentClassEdges[idS] == null) {
                equivalentClassEdges[idS] = new HashSet<>();
            }
            equivalentClassEdges[idS].add(idV);
            if (equivalentClassEdges[idV] == null) {
                equivalentClassEdges[idV] = new HashSet<>();
            }
            equivalentClassEdges[idV].add(idS);
        }

        equivalentClass = new int[classIds.size() + 1];

        int i = 1;
        while (i < equivalentClass.length) {

            LinkedList<Integer> q = new LinkedList<>();
            q.addLast(i);
            while (!q.isEmpty()) {
                int j = q.removeFirst();
                if (equivalentClass[j] != 0) {
                    if (equivalentClass[j] != i) {
                        System.out.println("Error");
                        System.exit(0);
                    }
                } else {
                    equivalentClass[j] = i;
                    if (equivalentClassEdges[j] != null) {
                        for (int k : equivalentClassEdges[j]) {
                            q.addLast(k);
                        }
                    }
                }
            }

            i++;
            while (i < equivalentClass.length && equivalentClass[i] != 0) {
                i++;
            }
        }
    }

    private void computeEquivalentPropertyGroups() throws IOException {
        //load all classes and assign an id to them
        //dbpedia properties are loaded first
        String regex = "(\\s|\\t)*<([^<>]*)>(\\s|\\t)*<([^<>]*)>(\\s|\\t)*(<|\")(.*)(>|\")";
        Pattern p = Pattern.compile(regex);
        for (String fileName : fileNames) {
            try (
                    BufferedReader in = new BufferedReader(new FileReader(downloadedFilesPath + fileName))) {
                String l = in.readLine();
                while (l != null) {
                    Matcher m = p.matcher(l);
                    if (m.find()) {
                        String s = m.group(2);
                        String a = m.group(4);
                        String v = m.group(7);
                        if (a.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") && v.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property")
                                && (s.startsWith("http://www.dbpedia.org") || s.startsWith("http://dbpedia.org")) && !propertyIds.containsKey(s)) {
                            propertyIds.put(s, propertyIds.size() + 1);
                        } else if (a.equals("http://www.w3.org/2002/07/owl#equivalentProperty")) {
                            if ((s.startsWith("http://www.dbpedia.org") || s.startsWith("http://dbpedia.org")) && !propertyIds.containsKey(s)) {
                                propertyIds.put(s, propertyIds.size() + 1);
                            }
                            if ((v.startsWith("http://www.dbpedia.org") || v.startsWith("http://dbpedia.org")) && !propertyIds.containsKey(v)) {
                                propertyIds.put(v, propertyIds.size() + 1);
                            }
                        }
                    }
                    l = in.readLine();
                }
            }
        }

        //now non-dpedia properties are loaded
        for (String fileName : fileNames) {
            try (
                    BufferedReader in = new BufferedReader(new FileReader(downloadedFilesPath + fileName))) {
                String l = in.readLine();
                while (l != null) {
                    Matcher m = p.matcher(l);
                    if (m.find()) {
                        String s = m.group(2);
                        String a = m.group(4);
                        String v = m.group(7);
                        if (a.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") && v.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property")
                                && !(s.equals("http://www.w3.org/2000/01/rdf-schema#label") || s.equals("http://www.w3.org/2002/07/owl#sameAs") || s.startsWith("http://www.dbpedia.org") || s.startsWith("http://dbpedia.org")) && !propertyIds.containsKey(s)) {
                            propertyIds.put(s, propertyIds.size() + 1);
                        } else if (a.equals("http://www.w3.org/2002/07/owl#equivalentProperty")) {
                            if (!(s.startsWith("http://www.dbpedia.org") || s.startsWith("http://dbpedia.org")) && !propertyIds.containsKey(s)) {
                                propertyIds.put(s, propertyIds.size() + 1);
                            }
                            if (!(v.startsWith("http://www.dbpedia.org") || v.startsWith("http://dbpedia.org")) && !propertyIds.containsKey(v)) {
                                propertyIds.put(v, propertyIds.size() + 1);
                            }
                        }
                    }
                    l = in.readLine();
                }
            }
        }

        //create the equivalentPropertyEdges sets        
        equivalentPropertyEdges = new HashSet[propertyIds.size() + 1];
        propertyById = new String[propertyIds.size() + 1];
        for (Map.Entry<String, Integer> e : propertyIds.entrySet()) {
            propertyById[e.getValue()] = e.getKey();
        }

        for (String fileName : fileNames) {
            try (
                    BufferedReader in = new BufferedReader(new FileReader(downloadedFilesPath + fileName))) {
                String l = in.readLine();
                while (l != null) {
                    Matcher m = p.matcher(l);
                    if (m.find()) {
                        String a = m.group(4);
                        if (a.equals("http://www.w3.org/2002/07/owl#equivalentProperty")) {
                            String s = m.group(2);
                            int idS = propertyIds.get(s);
                            String v = m.group(7);
                            int idV = propertyIds.get(v);
                            if (equivalentPropertyEdges[idS] == null) {
                                equivalentPropertyEdges[idS] = new HashSet<>();
                            }
                            equivalentPropertyEdges[idS].add(idV);
                            if (equivalentPropertyEdges[idV] == null) {
                                equivalentPropertyEdges[idV] = new HashSet<>();
                            }
                            equivalentPropertyEdges[idV].add(idS);
                        }/* else if (a.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                         String s = m.group(2);
                         String v = m.group(7);
                         if (v.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property")) {
                         properties.add(s);
                         } else if (v.equals("http://www.w3.org/2000/01/rdf-schema#Class")) {
                         classes.add(s);
                         }
                         }*/

                    }
                    l = in.readLine();
                }
            }
        }
        equivalentProperty = new int[propertyIds.size() + 1];

        int i = 1;
        while (i < equivalentProperty.length) {

            LinkedList<Integer> q = new LinkedList<>();
            q.addLast(i);
            while (!q.isEmpty()) {
                int j = q.removeFirst();
                if (equivalentProperty[j] != 0) {
                    if (equivalentProperty[j] != i) {
                        System.out.println("Error");
                        System.exit(0);
                    }
                } else {
                    equivalentProperty[j] = i;
                    if (equivalentPropertyEdges[j] != null) {
                        for (int k : equivalentPropertyEdges[j]) {
                            q.addLast(k);
                        }
                    }
                }
            }

            i++;
            while (i < equivalentProperty.length && equivalentProperty[i] != 0) {
                i++;
            }
        }
    }

    private String fromCamelCaseOrUnderscore(String s) {
        s = s.replaceAll("_", " ");
        s = StringUtils.join(
                StringUtils.splitByCharacterTypeCamelCase(s),
                ' '
        );
        s = s.replace("  ", " ");
        s = s.replace("  ", " ");
        return s;
    }

    private void loadLabelsAndClasses() throws IOException {
        String regex = "(\\s|\\t)*<([^<>]*)>(\\s|\\t)*<([^<>]*)>(\\s|\\t)*(<|\")(.*)(>|\")";
        Pattern p = Pattern.compile(regex);
        for (String fileName : fileNames) {
            try (
                    BufferedReader in = new BufferedReader(new FileReader(downloadedFilesPath + fileName))) {
                String l;
                while ((l = in.readLine()) != null) {
                    Matcher m = p.matcher(l);
                    if (m.find()) {
                        String a = m.group(4);
                        String s = m.group(2);
                        String v = m.group(7);
                        if (a.equals("http://www.w3.org/2000/01/rdf-schema#label")) {
                            if (classIds.get(s) != null) {
                                s = classById[equivalentClass[classIds.get(s)]];
                                HashSet<String> labels = classLabels.get(s);
                                if (labels == null) {
                                    labels = new HashSet<>();
                                    classLabels.put(s, labels);
                                }
                                labels.add(fromCamelCaseOrUnderscore(v));
                            } else if (propertyIds.get(s) != null) {
                                s = propertyById[equivalentProperty[propertyIds.get(s)]];
                                HashSet<String> labels = propertyLabels.get(s);
                                if (labels == null) {
                                    labels = new HashSet<>();
                                    propertyLabels.put(s, labels);
                                }
                                labels.add(fromCamelCaseOrUnderscore(v));
                            } else if (entityIds.get(s) != null) {
                                s = entityById[sameAs[entityIds.get(s)]];
                                HashSet<String> labels = entityLabels.get(s);
                                if (labels == null) {
                                    labels = new HashSet<>();
                                    entityLabels.put(s, labels);
                                }
                                labels.add(fromCamelCaseOrUnderscore(v));
                            } else {
                                System.out.println("Subject unknown in " + l);
                            }
                        } else if (a.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                            if (entityIds.get(s) != null) {
                                s = entityById[sameAs[entityIds.get(s)]];
                                if (classIds.get(v) != null) {
                                    HashSet<String> classes = entityClasses.get(s);
                                    if (classes == null) {
                                        classes = new HashSet<>();
                                        entityClasses.put(s, classes);
                                    }
                                    v = classById[equivalentClass[classIds.get(v)]];
                                    classes.add(v);
                                } else {
                                    if (!v.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property") && !v.equals("http://www.w3.org/2000/01/rdf-schema#Class")) {
                                        System.out.println("Value unknown in " + l);
                                    }
                                }
                            } else {
                                if (!v.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property") && !v.equals("http://www.w3.org/2000/01/rdf-schema#Class")) {
                                    System.out.println("Subject unknown in " + l);
                                }
                            }
                        }

                        //System.out.println("Skipped " + l + " for invalid subject: " + s);
                        l = in.readLine();
                    }
                }
            }
        }
        //now manually add some labels

        String[] additionalLabels = new String[]{
            "http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/targets\ttarget",
            "http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/genes\tgene",
            "http://www4.wiwiss.fu-berlin.de/sider/resource/sider/drugs\tdrug",
            //"http://www4.wiwiss.fu-berlin.de/drugbank/vocab/resource/class/Offer\toffer",
            "http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/references\treference",
            "http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/drugs\tdrug",
            "http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/drug_interactions\tdrug interaction",
            "http://www4.wiwiss.fu-berlin.de/sider/resource/sider/side_effects\tside effect",
            "http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/diseases\tdisease",
            "http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/enzymes\tenzyme"};

        for (String pair : additionalLabels) {
            String[] st = pair.split("\t");
            String s = classById[equivalentClass[classIds.get(st[0])]];
            HashSet<String> labels = classLabels.get(s);
            if (labels == null) {
                labels = new HashSet<>();
                classLabels.put(s, labels);
            }
            labels.add(st[1]);
        }

        //now process additional property labels file
        /*
         try (BufferedReader in = new BufferedReader(new FileReader(downloadedFilesPath + "additional_property_labels"))) {
         String l;
         while ((l = in.readLine()) != null) {
         StringTokenizer st = new StringTokenizer(l, " \t");
         String prop=st.nextToken();
         String lab=st.nextToken();
         String suffix="";
         if (prop.endsWith("Inv")) {
         prop=prop.substring(0, prop.length()-3);
         suffix="Inv";
         }
         prop = propertyById[equivalentProperty[propertyIds.get(prop)]]+suffix;
         HashSet<String> labels = propertyLabels.get(prop);
         if (labels == null) {
         labels = new HashSet<>();
         propertyLabels.put(prop, labels);
         }
         labels.add(fromCamelCaseOrUnderscore(lab));
         }
         } catch (Exception e) {
         e.printStackTrace();
         }
         */
        //now extract a label from the URI of properties
        for (int i = 1; i < propertyById.length; i++) {
            String s = propertyById[i];
            if (s.equals("http://www.w3.org/2000/01/rdf-schema#seeAlso") || s.equals("http://xmlns.com/foaf/0.1/page")) {
                continue;
            }
            String[] ss = s.split("\\/");
            String l = ss[ss.length - 1].replaceAll("\\_", " ");
            s = propertyById[equivalentProperty[propertyIds.get(s)]];
            HashSet<String> labels = propertyLabels.get(s);
            if (labels == null) {
                labels = new HashSet<>();
                propertyLabels.put(s, labels);
            }
            if (labels.isEmpty()) {
                l = fromCamelCaseOrUnderscore(l).toLowerCase();
                System.out.println("Artificial label " + l + " for property " + s);
                labels.add(l);
            }
        }

        //now extract a label from the URI of entities
        for (int i = 1; i < entityById.length; i++) {
            String s = entityById[i];
            String[] ss = s.split("\\/");
            String l = ss[ss.length - 1].replaceAll("\\_", " ");
            s = entityById[sameAs[entityIds.get(s)]];
            HashSet<String> labels = entityLabels.get(s);
            if (labels == null) {
                labels = new HashSet<>();
                entityLabels.put(s, labels);
            }
            if (labels.isEmpty()) {
                l = fromCamelCaseOrUnderscore(l).toLowerCase();
                System.out.println("Artificial label " + l + " for entity " + s);
                labels.add(l); //todo: handle camel case
            }
        }
    }

    private void createClassParentsFile() throws IOException { //it just creates an empty file
        try (
                PrintWriter out = new PrintWriter(new FileOutputStream(destinationPath + "class_parents"))) {
        }
    }

    private void createClassLabelsFile() throws IOException {
        try (PrintWriter out = new PrintWriter(new FileOutputStream(destinationPath + "class_labels"))) {
            for (Map.Entry<String, HashSet<String>> e : classLabels.entrySet()) {
                for (String l : e.getValue()) {
                    out.println(e.getKey() + "\t" + l);
                }
            }
        }
    }

    private void createPropertyLabelsFile() throws IOException { //it just creates an empty file
        try (PrintWriter out = new PrintWriter(new FileOutputStream(destinationPath + "property_labels"))) {
            for (Map.Entry<String, HashSet<String>> e : propertyLabels.entrySet()) {
                for (String l : e.getValue()) {
                    out.println(e.getKey() + "\t" + l);
                }
            }
        }
    }

    private void createEntityLabelsFile() throws IOException {
        try (
                PrintWriter out = new PrintWriter(new FileOutputStream(destinationPath + "entity_labels"))) {
            for (Map.Entry<String, HashSet<String>> e : entityLabels.entrySet()) {
                for (String l : e.getValue()) {
                    out.println(e.getKey() + "\t" + l);
                }
            }
        }
        for (int i = 1; i < entityById.length; i++) {
            String e = entityById[i];
            if (!entityLabels.containsKey(entityById[sameAs[entityIds.get(e)]])) {
                System.out.println(e + " without label");
            }
        }
    }

    private void createEntityClassesFile() throws IOException { //it just creates an empty file
        try (
                PrintWriter out = new PrintWriter(new FileOutputStream(destinationPath + "entity_classes"))) {
            for (Map.Entry<String, HashSet<String>> e : entityClasses.entrySet()) {
                for (String l : e.getValue()) {
                    out.println(e.getKey() + "\t" + l);
                }
            }
        }
    }

    private void createTriplesFile() throws IOException {
        String regex = "(\\s|\\t)*<([^<>]*)>(\\s|\\t)+<([^<>]*)>(\\s|\\t)+(.*)(\\s|\\t)+\\.$";
        Pattern p = Pattern.compile(regex);
        int dropped = 0;
        int droppedPageSeeAlso = 0;
        int total = 0;
        try (PrintWriter out = new PrintWriter(new FileOutputStream(destinationPath + "triples"))) {
            for (String fileName : fileNames) {
                try (
                        BufferedReader in = new BufferedReader(new FileReader(downloadedFilesPath + fileName))) {
                    String l;
                    while ((l = in.readLine()) != null) {
                        total++;
                        Matcher m = p.matcher(l);
                        if (m.find()) {
                            String a = m.group(4);
                            try {
                                if (a.equals("http://xmlns.com/foaf/0.1/page") || a.equals("http://www.w3.org/2000/01/rdf-schema#seeAlso")) {
                                    String s = m.group(2);
                                    s = entityById[sameAs[entityIds.get(s)]];
                                    if (entityLabels.containsKey(s)) {
                                        String v = m.group(6).trim().replaceAll("\\t", "");
                                        out.println("<" + s + "> <" + a + "> " + v + " .");
                                    } else {
                                        System.out.println("Unlabeled subject - Dropped " + l);
                                        dropped++;
                                    }
                                    continue;
                                }
                                a = propertyById[equivalentProperty[propertyIds.get(a)]];
                            } catch (Exception e) {
                                if (!a.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") && !a.equals("http://www.w3.org/2000/01/rdf-schema#label")
                                        && !a.equals("http://www.w3.org/2002/07/owl#sameAs") && !a.equals("http://www.w3.org/2000/01/rdf-schema#label")
                                        && !a.equals("http://www.w3.org/2002/07/owl#equivalentProperty") && !a.equals("http://www.w3.org/2002/07/owl#equivalentClass")
                                        && !a.equals("http://www.w3.org/2000/01/rdf-schema#seeAlso") && !a.equals("http://xmlns.com/foaf/0.1/page")) {
                                    System.out.println("Failed in resolving property " + a + " Dropped " + l);
                                    dropped++;
                                }
                                continue;
                            }
                            if (propertyLabels.containsKey(a)) {
                                String s = m.group(2);
                                s = entityById[sameAs[entityIds.get(s)]];
                                if (entityLabels.containsKey(s)) {
                                    String v = m.group(6).trim().replaceAll("\\t", "");
                                    if (v.startsWith("<")) {
                                        v = v.substring(1, v.length() - 1); //removes the <>
                                        try {
                                            v = entityById[sameAs[entityIds.get(v)]];
                                        } catch (Exception e) {
                                            //handle the external links
                                            if (v.contains("www.rxlist.com") && a.equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/rxlistLink")
                                                    || v.contains("www.pdb.org") && a.equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/pdbIdPage")
                                                    || v.contains("www.uniprot.org") && a.equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/swissprotPage")
                                                    || v.contains("pfam.sanger.ac.uk") && a.equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/pfamDomainFunctionPage")
                                                    || v.contains("www.genenames.org") && a.equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/hgncIdPage")
                                                    || v.contains("www.genenames.org") && a.equals("http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/hgncIdPage")
                                                    || v.contains("symbol.bio2rdf.org") && a.equals("http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/bio2rdfSymbol")
                                                    || v.contains("symbol.bio2rdf.org") && a.equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/bio2rdfSymbol")
                                                    || v.contains("www.ncbi.nlm.nih.gov") && a.equals("http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/omimPage")
                                                    || v.contains("www.ncbi.nlm.nih.gov") && a.equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/genbankIdProteinPage")
                                                    || v.contains("www.ncbi.nlm.nih.gov") && a.equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/genbankIdGenePage")
                                                    || v.contains("bio2rdf.org") && a.equals("http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/omim")
                                                    || v.contains("bio2rdf.org") && a.equals("http://dbpedia.org/ontology/omimLink")
                                                    || v.contains("bio2rdf.org") && a.equals("http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/hgncId")
                                                    || v.contains("bio2rdf.org") && a.equals("http://dbpedia.org/ontology/hgncidLink")
                                                    || v.contains("bio2rdf.org") && a.equals("http://www4.wiwiss.fu-berlin.de/diseasome/resource/diseasome/geneId")
                                                    || v.contains("bio2rdf.org") && a.equals("http://dbpedia.org/ontology/entrezgeneLink")
                                                    || v.contains("bio2rdf.org") && a.equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/swissprotId")
                                                    || v.contains("bio2rdf.org") && a.equals("http://dbpedia.org/ontology/uniprotLink")
                                                    || v.contains("bio2rdf.org") && a.equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/casRegistryNumber")
                                                    || v.contains("bio2rdf.org") && a.equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/keggDrugId")
                                                    || v.contains("bio2rdf.org") && a.equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/keggCompoundId")
                                                    || v.contains("bio2rdf.org") && a.equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/chebiId")
                                                    || v.contains("bio2rdf.org") && a.equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/pdbId")
                                                    || v.contains("bio2rdf.org") && a.equals("http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugbank/pfamDomainFunction")
                                                    || v.contains("bio2rdf.org") && a.equals("http://dbpedia.org/ontology/pfamLink")) {
                                                out.println("<" + s + "> <" + a + "> <" + v + "> .");
                                            } else {
                                                System.out.println("Failed in resolving value " + v + " Dropped " + l);
                                                dropped++;
                                            }
                                            continue;
                                        }
                                        if (entityLabels.containsKey(v)) {
                                            out.println("<" + s + "> <" + a + "> <" + v + "> .");
                                        } else {
                                            System.out.println("Unlabeled value - Dropped " + l);
                                            dropped++;
                                        }
                                    } else {
                                        out.println("<" + s + "> <" + a + "> " + v + " .");
                                    }
                                } else {
                                    System.out.println("Unlabeled subject - Dropped " + l);
                                    dropped++;
                                }
                            } else {
                                System.out.println("Unlabeled property Dropped " + l);
                                dropped++;
                            }
                        } else {
                            System.out.println("Could not recognize " + l);
                            dropped++;
                        }
                    }
                }
            }
        }
        System.out.println("Dropped " + dropped + " lines over " + total);
        System.out.println(droppedPageSeeAlso + " with seeAlso or page property were dropped");
    }

    public static void main(String... args) throws Exception {
        if (true) {

        }
        long start = System.currentTimeMillis();
        System.out.println("Started at " + new Date());
        BiomedicalOntologyUtils biokb = new BiomedicalOntologyUtils("/home/massimo/canalikbs/biomedical/qald4/downloaded/", "/home/massimo/canalikbs/biomedical/qald4/processed/");
        biokb.computeEquivalentPropertyGroups();
        biokb.computeEquivalentClassGroups();
        biokb.computeSameAsGroups();
        biokb.createClassParentsFile();
        biokb.loadLabelsAndClasses();
        biokb.createClassLabelsFile();
        biokb.createPropertyLabelsFile();
        biokb.createEntityLabelsFile();
        biokb.createEntityClassesFile();
        biokb.createTriplesFile();
        biokb.createBasicTypesLiteralTypesFile();
        System.out.println("Ended at " + new Date());
        long time = System.currentTimeMillis() - start;
        long sec = time / 1000;
        System.out.println("The process took " + (sec / 60) + "'" + (sec % 60) + "." + (time % 1000) + "\"");
    }

    public void createBasicTypesLiteralTypesFile() throws Exception {
        System.out.println("Saving basic types");
        try (PrintWriter out = new PrintWriter(new FileOutputStream(destinationPath + "basic_types_literal_types", false), true)) {
            out.println("http://dbpedia.org/datatype/centimetre\tDouble");
            out.println("http://dbpedia.org/datatype/cubicCentimetre\tDouble");
            out.println("http://dbpedia.org/datatype/cubicKilometre\tDouble");
            out.println("http://dbpedia.org/datatype/cubicMetre\tDouble");
            out.println("http://dbpedia.org/datatype/cubicMetrePerSecond\tDouble");
            out.println("http://dbpedia.org/datatype/day\tDouble");
            out.println("http://dbpedia.org/datatype/gramPerKilometre\tDouble");
            out.println("http://dbpedia.org/datatype/hour\tDouble");
            out.println("http://dbpedia.org/datatype/inhabitantsPerSquareKilometre\tDouble");
            out.println("http://dbpedia.org/datatype/kelvin\tDouble");
            out.println("http://dbpedia.org/datatype/kilogram\tDouble");
            out.println("http://dbpedia.org/datatype/kilogramPerCubicMetre\tDouble");
            out.println("http://dbpedia.org/datatype/kilometre\tDouble");
            out.println("http://dbpedia.org/datatype/kilometrePerHour\tDouble");
            out.println("http://dbpedia.org/datatype/kilometrePerSecond\tDouble");
            out.println("http://dbpedia.org/datatype/kilowatt\tDouble");
            out.println("http://dbpedia.org/datatype/litre\tDouble");
            out.println("http://dbpedia.org/datatype/megabyte\tDouble");
            out.println("http://dbpedia.org/datatype/metre\tDouble");
            out.println("http://dbpedia.org/datatype/millimetre\tDouble");
            out.println("http://dbpedia.org/datatype/minute\tDouble");
            out.println("http://dbpedia.org/datatype/newtonMetre\tDouble");
            out.println("http://dbpedia.org/datatype/second\tDouble");
            out.println("http://dbpedia.org/datatype/squareKilometre\tDouble");
            out.println("http://dbpedia.org/datatype/squareMetre\tDouble");
            out.println("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString\tString");
            out.println("http://dbpedia.org/datatype/usDollar\tDouble");
            out.println("http://dbpedia.org/datatype/euro\tDouble");
            out.println("http://dbpedia.org/datatype/poundSterling\tDouble");
            out.println("http://dbpedia.org/datatype/swedishKrona\tDouble");
            out.println("http://dbpedia.org/datatype/philippinePeso\tDouble");
            out.println("http://dbpedia.org/datatype/singaporeDollar\tDouble");
            out.println("http://dbpedia.org/datatype/indianRupee\tDouble");
            out.println("http://dbpedia.org/datatype/mauritianRupee\tDouble");
            out.println("http://dbpedia.org/datatype/canadianDollar\tDouble");
            out.println("http://dbpedia.org/datatype/hongKongDollar\tDouble");
            out.println("http://dbpedia.org/datatype/zambianKwacha\tDouble");
            out.println("http://dbpedia.org/datatype/moroccanDirham\tDouble");
            out.println("http://dbpedia.org/datatype/ghanaianCedi\tDouble");
            out.println("http://dbpedia.org/datatype/peruvianNuevoSol\tDouble");
            out.println("http://dbpedia.org/datatype/thaiBaht\tDouble");
            out.println("http://dbpedia.org/datatype/nicaraguanCórdoba\tDouble");
            out.println("http://dbpedia.org/datatype/malaysianRinggit\tDouble");
            out.println("http://dbpedia.org/datatype/unitedArabEmiratesDirham\tDouble");
            out.println("http://dbpedia.org/datatype/ethiopianBirr\tDouble");
            out.println("http://dbpedia.org/datatype/egyptianPound\tDouble");
            out.println("http://dbpedia.org/datatype/tanzanianShilling\tDouble");
            out.println("http://dbpedia.org/datatype/azerbaijaniManat\tDouble");
            out.println("http://dbpedia.org/datatype/indonesianRupiah\tDouble");
            out.println("http://dbpedia.org/datatype/botswanaPula\tDouble");
            out.println("http://dbpedia.org/datatype/bangladeshiTaka\tDouble");
            out.println("http://dbpedia.org/datatype/czechKoruna\tDouble");
            out.println("http://dbpedia.org/datatype/belizeDollar\tDouble");
            out.println("http://dbpedia.org/datatype/ukrainianHryvnia\tDouble");
            out.println("http://dbpedia.org/datatype/bulgarianLev\tDouble");
            out.println("http://dbpedia.org/datatype/icelandKrona\tDouble");
            out.println("http://dbpedia.org/datatype/sriLankanRupee\tDouble");
            out.println("http://dbpedia.org/datatype/armenianDram\tDouble");
            out.println("http://dbpedia.org/datatype/pakistaniRupee\tDouble");
            out.println("http://dbpedia.org/datatype/southAfricanRand\tDouble");
            out.println("http://dbpedia.org/datatype/romanianNewLeu\tDouble");
            out.println("http://dbpedia.org/datatype/colombianPeso\tDouble");
            out.println("http://dbpedia.org/datatype/russianRouble\tDouble");
            out.println("http://dbpedia.org/datatype/algerianDinar\tDouble");
            out.println("http://dbpedia.org/datatype/sierraLeoneanLeone\tDouble");
            out.println("http://dbpedia.org/datatype/netherlandsAntilleanGuilder\tDouble");
            out.println("http://dbpedia.org/datatype/nigerianNaira\tDouble");
            out.println("http://dbpedia.org/datatype/hungarianForint\tDouble");
            out.println("http://dbpedia.org/datatype/estonianKroon\tDouble");
            out.println("http://dbpedia.org/datatype/georgianLari\tDouble");
            out.println("http://dbpedia.org/datatype/gambianDalasi\tDouble");
            out.println("http://dbpedia.org/datatype/gibraltarPound\tDouble");
            out.println("http://dbpedia.org/datatype/kuwaitiDinar\tDouble");
            out.println("http://dbpedia.org/datatype/brazilianReal\tDouble");
            out.println("http://dbpedia.org/datatype/maldivianRufiyaa\tDouble");
            out.println("http://dbpedia.org/datatype/jordanianDinar\tDouble");
            out.println("http://dbpedia.org/datatype/israeliNewSheqel\tDouble");
            out.println("http://dbpedia.org/datatype/saudiRiyal\tDouble");
            out.println("http://dbpedia.org/datatype/serbianDinar\tDouble");
            out.println("http://dbpedia.org/datatype/iranianRial\tDouble");
            out.println("http://dbpedia.org/datatype/omaniRial\tDouble");
            out.println("http://dbpedia.org/datatype/nepaleseRupee\tDouble");
            out.println("http://dbpedia.org/datatype/argentinePeso\tDouble");
            out.println("http://dbpedia.org/datatype/honduranLempira\tDouble");
            out.println("http://dbpedia.org/datatype/papuaNewGuineanKina\tDouble");
            out.println("http://dbpedia.org/datatype/qatariRial\tDouble");
            out.println("http://dbpedia.org/datatype/moldovanLeu\tDouble");
            out.println("http://dbpedia.org/datatype/bosniaAndHerzegovinaConvertibleMarks\tDouble");
            out.println("http://dbpedia.org/datatype/kazakhstaniTenge\tDouble");
            out.println("http://dbpedia.org/datatype/malawianKwacha\tDouble");
            out.println("http://dbpedia.org/datatype/newTaiwanDollar\tDouble");
            out.println("http://dbpedia.org/datatype/chileanPeso\tDouble");
            out.println("http://dbpedia.org/datatype/southKoreanWon\tDouble");
            out.println("http://dbpedia.org/datatype/bahrainiDinar\tDouble");
            out.println("http://dbpedia.org/datatype/latvianLats\tDouble");
            out.println("http://dbpedia.org/datatype/jamaicanDollar\tDouble");
            out.println("http://dbpedia.org/datatype/namibianDollar\tDouble");
            out.println("http://dbpedia.org/datatype/latvianLats\tDouble");
            out.println("http://dbpedia.org/datatype/turkishLira\tDouble");
            out.println("http://dbpedia.org/datatype/danishKrone\tDouble");
            out.println("http://dbpedia.org/datatype/norwegianKrone\tDouble");
            out.println("http://dbpedia.org/datatype/kenyanShilling\tDouble");
            out.println("http://dbpedia.org/datatype/renminbi\tDouble");
            out.println("http://dbpedia.org/datatype/polishZłoty\tDouble");
            out.println("http://dbpedia.org/datatype/ugandaShilling\tDouble");
            out.println("http://dbpedia.org/datatype/japaneseYen\tDouble");
            out.println("http://dbpedia.org/datatype/newZealandDollar\tDouble");
            out.println("http://dbpedia.org/datatype/rwandaFranc\tDouble");
            out.println("http://dbpedia.org/datatype/swissFranc\tDouble");
            out.println("http://dbpedia.org/datatype/australianDollar\tDouble");
            out.println("http://dbpedia.org/datatype/mexicanPeso\tDouble");
            out.println("http://dbpedia.org/datatype/lithuanianLitas\tDouble");
            out.println("http://dbpedia.org/datatype/croatianKuna\tDouble");
            out.println("http://dbpedia.org/datatype/engineConfiguration\tString");
            out.println("http://dbpedia.org/datatype/fuelType\tString");
            out.println("http://dbpedia.org/datatype/valvetrain\tString");
            out.println("http://dbpedia.org/datatype/rod\tDouble");
            out.println("http://dbpedia.org/datatype/degreeRankine\tDouble");
            out.println("http://dbpedia.org/datatype/stone\tDouble");
            out.println("http://dbpedia.org/datatype/perCent\tDouble");
            out.println("http://dbpedia.org/datatype/gram\tDouble");
            out.println("http://dbpedia.org/datatype/pond\tDouble");
            out.println("http://dbpedia.org/datatype/inch\tDouble");
            out.println("http://dbpedia.org/datatype/pound\tDouble");
            out.println("http://dbpedia.org/datatype/megahertz\tDouble");
            out.println("http://dbpedia.org/datatype/gramPerCubicCentimetre\tDouble");
            out.println("http://dbpedia.org/datatype/micrometre\tDouble");
            out.println("http://dbpedia.org/datatype/tonne\tDouble");
            out.println("http://dbpedia.org/datatype/squareFoot\tDouble");
            out.println("http://dbpedia.org/datatype/nanometre\tDouble");
            out.println("http://dbpedia.org/datatype/foot\tDouble");
            out.println("http://dbpedia.org/datatype/gigalitre\tDouble");
            out.println("http://dbpedia.org/datatype/acre\tDouble");
            out.println("http://dbpedia.org/datatype/horsepower\tDouble");
            out.println("http://dbpedia.org/datatype/milePerHour\tDouble");
            out.println("http://dbpedia.org/datatype/mile\tDouble");
            out.println("http://dbpedia.org/datatype/nautialMile\tDouble");
            out.println("http://dbpedia.org/datatype/footPerMinute\tDouble");
            out.println("http://dbpedia.org/datatype/metrePerSecond\tDouble");
            out.println("http://dbpedia.org/datatype/ampere\tDouble");
            out.println("http://dbpedia.org/datatype/degreeCelsius\tDouble");
            out.println("http://dbpedia.org/datatype/astronomicalUnit\tDouble");
            out.println("http://dbpedia.org/datatype/millibar\tDouble");
            out.println("http://dbpedia.org/datatype/milligram\tDouble");
            out.println("http://dbpedia.org/datatype/byte\tDouble");
            out.println("http://dbpedia.org/datatype/degreeFahrenheit\tDouble");
            out.println("http://dbpedia.org/datatype/decimetre\tDouble");
            out.println("http://dbpedia.org/datatype/watt\tDouble");
            out.println("http://dbpedia.org/datatype/knot\tDouble");
            out.println("http://dbpedia.org/datatype/perMil\tDouble");
            out.println("http://dbpedia.org/datatype/megawatt\tDouble");
            out.println("http://dbpedia.org/datatype/kilowattHour\tDouble");
            out.println("http://dbpedia.org/datatype/kilopascal\tDouble");
            out.println("http://dbpedia.org/datatype/kilobyte\tDouble");
            out.println("http://dbpedia.org/datatype/pferdestaerke\tDouble");
            out.println("http://dbpedia.org/datatype/footPerSecond\tDouble");
            out.println("http://dbpedia.org/datatype/gigawattHour\tDouble");
            out.println("http://dbpedia.org/datatype/bit\tDouble");
            out.println("http://dbpedia.org/datatype/myanmaKyat\tDouble");
            out.println("http://dbpedia.org/datatype/tonganPaanga\tDouble");
            out.println("http://dbpedia.org/datatype/millilitre\tDouble");
            out.println("http://dbpedia.org/datatype/nanosecond\tDouble");
            out.println("http://dbpedia.org/datatype/bar\tDouble");
            out.println("http://dbpedia.org/datatype/gigabyte\tDouble");
            out.println("http://dbpedia.org/datatype/bahamianDollar\tDouble");
            out.println("http://dbpedia.org/datatype/volt\tDouble");
            out.println("http://dbpedia.org/datatype/kilolightYear\tDouble");
            out.println("http://dbpedia.org/datatype/pascal\tDouble");
            out.println("http://dbpedia.org/datatype/gigametre\tDouble");
            out.println("http://dbpedia.org/datatype/terabyte\tDouble");
            out.println("http://dbpedia.org/datatype/kilogramForce\tDouble");
            out.println("http://dbpedia.org/datatype/hectare\tDouble");
            out.println("http://dbpedia.org/datatype/megalitre\tDouble");
            out.println("http://dbpedia.org/datatype/gigawatt\tDouble");
            out.println("http://dbpedia.org/datatype/terawattHour\tDouble");
            out.println("http://dbpedia.org/datatype/kilohertz\tDouble");
            out.println("http://dbpedia.org/datatype/hertz\tDouble");
            out.println("http://dbpedia.org/datatype/newton\tDouble");
            out.println("http://dbpedia.org/datatype/meganewton\tDouble");
            out.println("http://dbpedia.org/datatype/lightYear\tDouble");
            out.println("http://dbpedia.org/datatype/megabit\tDouble");
            out.println("http://dbpedia.org/datatype/cubicInch\tDouble");
            out.println("http://dbpedia.org/datatype/squareMile\tDouble");
            out.println("http://dbpedia.org/datatype/cubicFeetPerSecond\tDouble");
            out.println("http://dbpedia.org/datatype/joule\tDouble");
            out.println("http://dbpedia.org/datatype/seychellesRupee\tDouble");
            out.println("http://dbpedia.org/datatype/yard\tDouble");
            out.println("http://dbpedia.org/datatype/squareCentimetre\tDouble");
            out.println("http://dbpedia.org/datatype/microlitre\tDouble");
            out.println("http://dbpedia.org/datatype/calorie\tDouble");
            out.println("http://dbpedia.org/datatype/cubicHectometre\tDouble");
            out.println("http://dbpedia.org/datatype/brakeHorsepower\tDouble");
            out.println("http://dbpedia.org/datatype/gramPerMillilitre\tDouble");
            out.println("http://dbpedia.org/datatype/milliwatt\tDouble");
            out.println("http://dbpedia.org/datatype/poundPerSquareInch\tDouble");
            out.println("http://dbpedia.org/datatype/hectolitre\tDouble");
            out.println("http://dbpedia.org/datatype/millipond\tDouble");
            out.println("http://dbpedia.org/datatype/saintHelenaPound\tDouble");
            out.println("http://dbpedia.org/datatype/ounce\tDouble");
            out.println("http://dbpedia.org/datatype/bermudianDollar\tDouble");
            out.println("http://dbpedia.org/datatype/kilocalorie\tDouble");
            out.println("http://dbpedia.org/datatype/hectopascal\tDouble");
            out.println("http://dbpedia.org/datatype/millihertz\tDouble");
            out.println("http://dbpedia.org/datatype/kilovolt\tDouble");
            out.println("http://dbpedia.org/datatype/kilojoule\tDouble");
            out.println("http://dbpedia.org/datatype/grain\tDouble");
            out.println("http://dbpedia.org/datatype/albanianLek\tDouble");
            out.println("http://dbpedia.org/datatype/milliampere\tDouble");
            out.println("http://dbpedia.org/datatype/cubicMillimetre\tDouble");
            out.println("http://dbpedia.org/datatype/usGallon\tDouble");
            out.println("http://dbpedia.org/datatype/cubicFoot\tDouble");
            out.println("http://dbpedia.org/datatype/gigahertz\tDouble");
            out.println("http://dbpedia.org/datatype/centilitre\tDouble");
            out.println("http://dbpedia.org/datatype/decilitre\tDouble");
            out.println("http://dbpedia.org/datatype/tonneForce\tDouble");
            out.println("http://dbpedia.org/datatype/sãoToméAndPríncipeDobra\tDouble");
            out.println("http://dbpedia.org/datatype/eritreanNakfa\tDouble");
            out.println("http://dbpedia.org/datatype/megapond\tDouble");
            out.println("http://dbpedia.org/datatype/megapascal\tDouble");
            out.println("http://dbpedia.org/datatype/giganewton\tDouble");
            out.println("http://dbpedia.org/datatype/megavolt\tDouble");
            out.println("http://dbpedia.org/datatype/kilopond\tDouble");
            out.println("http://dbpedia.org/datatype/bolivianBoliviano\tDouble");
            out.println("http://dbpedia.org/datatype/mongolianTögrög\tDouble");
            out.println("http://dbpedia.org/datatype/furlong\tDouble");
            out.println("http://dbpedia.org/datatype/belarussianRuble\tDouble");
            out.println("http://dbpedia.org/datatype/lebanesePound\tDouble");
            out.println("http://dbpedia.org/datatype/laoKip\tDouble");
            out.println("http://dbpedia.org/datatype/guineaFranc\tDouble");
            out.println("http://dbpedia.org/datatype/gramForce\tDouble");
            out.println("http://dbpedia.org/datatype/poundFoot\tDouble");
            out.println("http://dbpedia.org/datatype/cubicYard\tDouble");
            out.println("http://dbpedia.org/datatype/sudanesePound\tDouble");
            out.println("http://dbpedia.org/datatype/somaliShilling\tDouble");
            out.println("http://dbpedia.org/datatype/syrianPound\tDouble");
            out.println("http://dbpedia.org/datatype/megawattHour\tDouble");
            out.println("http://dbpedia.org/datatype/dominicanPeso\tDouble");
            out.println("http://dbpedia.org/datatype/caymanIslandsDollar\tDouble");
            out.println("http://dbpedia.org/datatype/microsecond\tDouble");
            out.println("http://dbpedia.org/datatype/capeVerdeEscudo\tDouble");
            out.println("http://dbpedia.org/datatype/imperialGallon\tDouble");
            out.println("http://dbpedia.org/datatype/squareHectometre\tDouble");
            out.println("http://dbpedia.org/datatype/squareDecimetre\tDouble");
            out.println("http://dbpedia.org/datatype/kilolitre\tDouble");
            out.println("http://dbpedia.org/datatype/hectometre\tDouble");
            out.println("http://dbpedia.org/datatype/standardAtmosphere\tDouble");
            out.println("http://dbpedia.org/datatype/zimbabweanDollar\tDouble");
            out.println("http://dbpedia.org/datatype/congoleseFranc\tDouble");
            out.println("http://dbpedia.org/datatype/arubanGuilder\tDouble");
            out.println("http://dbpedia.org/datatype/kilometresPerLitre\tDouble");
            out.println("http://dbpedia.org/datatype/cubicDecimetre\tDouble");
            out.println("http://dbpedia.org/datatype/venezuelanBolívar\tDouble");
            out.println("http://dbpedia.org/datatype/wattHour\tDouble");
            out.println("http://dbpedia.org/datatype/trinidadAndTobagoDollar\tDouble");
            out.println("http://dbpedia.org/datatype/erg\tDouble");
            out.println("http://dbpedia.org/datatype/fijiDollar\tDouble");
            out.println("http://dbpedia.org/datatype/malagasyAriary\tDouble");
            out.println("http://dbpedia.org/datatype/bhutaneseNgultrum\tDouble");
            out.println("http://dbpedia.org/datatype/kiloampere\tDouble");
            out.println("http://dbpedia.org/datatype/squareMillimetre\tDouble");
            out.println("http://dbpedia.org/datatype/imperialBarrelOil\tDouble");
            out.println("http://dbpedia.org/datatype/macanesePataca\tDouble");
            out.println("http://dbpedia.org/datatype/samoanTala\tDouble");
            out.println("http://www.w3.org/2001/XMLSchema#gMonthDay\tDate");
            out.println("http://www.w3.org/2001/XMLSchema#anyURI\tString");
            out.println("http://www.w3.org/2001/XMLSchema#boolean\tBoolean");
            out.println("http://www.w3.org/2001/XMLSchema#date\tDate");
            out.println("http://www.w3.org/2001/XMLSchema#dateTime\tDate");
            out.println("http://www.w3.org/2001/XMLSchema#double\tDouble");
            out.println("http://www.w3.org/2001/XMLSchema#float\tDouble");
            out.println("http://www.w3.org/2001/XMLSchema#gYear\tDate");
            out.println("http://www.w3.org/2001/XMLSchema#gYearMonth\tDate");
            out.println("http://www.w3.org/2001/XMLSchema#integer\tDouble");
            out.println("http://www.w3.org/2001/XMLSchema#int\tDouble");
            out.println("http://www.w3.org/2001/XMLSchema#nonNegativeInteger\tDouble");
            out.println("http://www.w3.org/2001/XMLSchema#positiveInteger\tDouble");
            out.println("http://www.w3.org/2001/XMLSchema#string\tString");
        }
    }

}
