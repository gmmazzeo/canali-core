/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.utils;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.parser.NxParser;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
//In order to run this file, you must download the following files
public class MusicBrainzOntologyUtils {

    HashMap<String, HashSet<String>> propertyLabels = new HashMap<>();
    HashMap<String, HashSet<String>> classLabels = new HashMap<>();
    HashMap<String, HashSet<String>> classParents = new HashMap<>();
    HashSet<String> properties = new HashSet<>();
    HashSet<String> classes = new HashSet<>();
    String downloadedFilesPath, destinationPath;
    File[] files;

    public MusicBrainzOntologyUtils(String downloadedFilesPath, String destinationPath) throws Exception {
        if (!downloadedFilesPath.endsWith(File.separator)) {
            downloadedFilesPath += File.separator;
        }
        if (!destinationPath.endsWith(File.separator)) {
            destinationPath += File.separator;
        }
        this.downloadedFilesPath = downloadedFilesPath;
        this.destinationPath = destinationPath;
        File folder = new File(downloadedFilesPath);
        files = folder.listFiles();
        Arrays.sort(files);
        System.out.println("Loading musicbrainz ontology");
        readHashMapFromFile("class_parents", classParents, "http://www.w3.org/2002/07/owl#Thing");
        classes.addAll(classParents.keySet());
        readHashMapFromFile("class_labels", classLabels, "");
        classes.addAll(classLabels.keySet());
        System.out.println("Classes:");
        for (String c : classes) {
            System.out.println(c);
        }
        readHashMapFromFile("property_labels", propertyLabels, "");
        properties.addAll(propertyLabels.keySet());
        System.out.println("Properties:");
        for (String a : properties) {
            System.out.println(a);
        }

        //may be some entries will need to be manually added
    }

    private void readHashMapFromFile(String fileName, HashMap<String, HashSet<String>> hashmap, String defaultValue) throws Exception {
        System.out.println("Reading file " + destinationPath + fileName);
        try (BufferedReader in = new BufferedReader(new FileReader(destinationPath + fileName))) {
            String l = in.readLine();
            while (l != null) {
                if (l.length() > 0) {
                    StringTokenizer st = new StringTokenizer(l, "\t");
                    String c = st.nextToken();
                    String p = st.hasMoreTokens() ? st.nextToken() : defaultValue;
                    HashSet<String> ps = hashmap.get(c);
                    if (ps == null) {
                        ps = new HashSet<>();
                        hashmap.put(c, ps);
                    }
                    ps.add(p);
                }
                l = in.readLine();
            }
        }
    }

    public HashSet<String> processFiles2() throws Exception {
        HashSet<String> res = new HashSet<>();
        System.out.println("Processing files");
        HashSet<String> ignoreProperties = new HashSet<>();
        ignoreProperties.add("http://www.w3.org/2000/01/rdf-schema#seeAlso");
        ignoreProperties.add("http://www.w3.org/2000/01/rdf-schema#comment");
        ignoreProperties.add("http://www.w3.org/2002/07/owl#sameAs");
        ignoreProperties.add("http://xmlns.com/foaf/0.1/isPrimaryTopicOf");
        ignoreProperties.add("http://open.vocab.org/terms/sortLabel");
        //ignoreProperties.add("http://www.w3.org/2004/02/skos/core#altLabel");
        ignoreProperties.add("http://purl.org/muto/core#taggedResource");
        ignoreProperties.add("http://purl.org/NET/c4dm/event.owl#factor");
        System.setErr(new PrintStream("/dev/null"));
        try (
                PrintWriter entityClasses = new PrintWriter(new FileOutputStream(destinationPath + "entity_classes", false), true);
                PrintWriter entityLabels = new PrintWriter(new FileOutputStream(destinationPath + "entity_labels", false), true);
                PrintWriter triples = new PrintWriter(new FileOutputStream(destinationPath + "triples", false), true)) {
            for (int k = files.length - 1; k >= 0; k--) {
                File f = files[k];
                if (!f.getCanonicalPath().endsWith(".nt")) {
                    continue;
                }
                System.out.println("Processing " + f.getCanonicalPath());

                NxParser nxp = new NxParser();
                nxp.parse(new FileInputStream(f));

                while (nxp.hasNext()) {
                    try {
                        Node[] ns = nxp.next();
                        if (ns.length == 3) {
                            if (ns[0] instanceof Resource) {
                                String eUri = ((Resource) ns[0]).toURI().toString();
                                if (ns[1] instanceof Resource) {
                                    String aUri = ((Resource) ns[1]).toURI().toString();
                                    if ("http://xmlns.com/foaf/0.1/name".equals(aUri) || "http://purl.org/dc/elements/1.1/title".equals(aUri)) {
                                        if (ns[2] instanceof Literal) {
                                            String l = eUri + "\t" + ((Literal) ns[2]).getLabel();
                                            StringTokenizer st = new StringTokenizer(l, "\t<>");
                                            try {
                                                st.nextToken();
                                                st.nextToken();
                                                entityLabels.println(l);
                                                triples.println(ns[0] + " " + ns[1] + " " + ns[2] + " .");
                                            } catch (Exception e) {
                                            }
                                        } else {
                                            System.out.println("Invalid name: " + ns[2]);
                                        }
                                    } else if ("http://www.w3.org/2000/01/rdf-schema#label".equals(aUri) || "http://purl.org/muto/core#tagLabel".equals(aUri) || "http://www.w3.org/2004/02/skos/core#altLabel".equals(aUri)) {
                                        if (ns[2] instanceof Literal) {
                                            String l = eUri + "\t" + ((Literal) ns[2]).getLabel();
                                            StringTokenizer st = new StringTokenizer(l, "\t<>");
                                            try {
                                                st.nextToken();
                                                st.nextToken();
                                                entityLabels.println(l);
                                            } catch (Exception e) {
                                            }
                                        } else {
                                            System.out.println("Invalid label: " + ns[2]);
                                        }
                                    } else if ("http://www.w3.org/1999/02/22-rdf-syntax-ns#type".equals(aUri)) {
                                        if (ns[2] instanceof Resource) {
                                            entityClasses.println(eUri + "\t" + ((Resource) ns[2]).toURI().toString());
                                        } else {
                                            System.out.println("Invalid class: " + ns[2]);
                                        }
                                    } else if (properties.contains(aUri)) {
                                        triples.println(ns[0] + " " + ns[1] + " " + ns[2] + " .");
                                    } else if (!ignoreProperties.contains(aUri)) {
                                        System.out.println("Unknown property: " + aUri);
                                    }
                                } else {
                                    System.out.println("Invalid pattern: property is " + ns[1]);
                                }
                            } else {
                                System.out.println("Invalid pattern: subject is " + ns[0]);
                            }
                        } else {
                            System.out.println("Invalid pattern: " + ns.length + " elements instead of 3");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return res;
    }

    public void createEntityClassesFile(HashSet<String> acceptableEntities, HashSet<String> acceptableClasses) throws Exception {
        System.out.println("Saving entity classes");
        try (PrintWriter out = new PrintWriter(new FileOutputStream(destinationPath + "entity_classes", false), true);
                BufferedReader in1 = new BufferedReader(new FileReader(downloadedFilesPath + "instance_types_en.nt"));
                BufferedReader in2 = new BufferedReader(new FileReader(downloadedFilesPath + "instance_types_heuristic_en.nt"))) {
            String regex = "<(.*)> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <(.*)>";
            Pattern p = Pattern.compile(regex);
            String l = in1.readLine();
            while (l != null) {
                Matcher m = p.matcher(l);
                if (m.find()) {
                    String eUri = m.group(1);
                    String cUri = m.group(2);
                    if (acceptableEntities.contains(eUri) && acceptableClasses.contains(cUri)) {
                        out.println(eUri + "\t" + cUri);
                    }
                }
                l = in1.readLine();
            }
            l = in2.readLine();
            while (l != null) {
                Matcher m = p.matcher(l);
                if (m.find()) {
                    String eUri = m.group(1);
                    String cUri = m.group(2);
                    if (acceptableEntities.contains(eUri) && acceptableClasses.contains(cUri)) {
                        out.println(eUri + "\t" + cUri);
                    }
                }
                l = in2.readLine();
            }
        }
    }

    public static void main(String... args) throws Exception {
        long start = System.currentTimeMillis();
        System.out.println("Started at " + new Date());
        MusicBrainzOntologyUtils musicbrainz = new MusicBrainzOntologyUtils("/home/massimo/aquawd/musicbrainz-downloaded-files/", "/home/massimo/aquawd/musicbrainz-files/");
        HashSet<String> entities = musicbrainz.processFiles2();
        System.out.println("Ended at " + new Date());
        long time = System.currentTimeMillis() - start;
        long sec = time / 1000;
        System.out.println("The process took " + (sec / 60) + "'" + (sec % 60) + "." + (time % 1000) + "\"");
    }

}
