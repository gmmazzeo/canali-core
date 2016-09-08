/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.parser.NxParser;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
//In order to run this file, you must download the following files
//http://greententacle.techfak.uni-bielefeld.de/~cunger/download/musicbrainz.tgz
//Uncompress the tgz file in your working directyory
//then convert rdf files to nt using the utility http://www.l3s.de/~minack/rdf2rdf/
//the files class_parents, class label, and property_labels were created manually
//class parents contains the following lines
/*
http://purl.org/ontology/mo/MusicGroup  http://purl.org/ontology/mo/MusicArtist
http://purl.org/ontology/mo/SoloMusicArtist     http://purl.org/ontology/mo/MusicArtist
http://purl.org/ontology/mo/Record      http://purl.org/ontology/mo/MusicalManifestation
http://purl.org/ontology/mo/Track       http://purl.org/ontology/mo/MusicalManifestation
*/
//class labels contains the following lines
/*
http://purl.org/ontology/mo/MusicArtist artist
http://purl.org/ontology/mo/MusicGroup  group
http://purl.org/ontology/mo/SoloMusicArtist     solo artist
http://purl.org/ontology/mo/Record      record
http://purl.org/vocab/bio/0.1/Birth     birth
http://purl.org/vocab/bio/0.1/Death     death
http://purl.org/ontology/mo/Track       track
http://purl.org/ontology/mo/membership  membership
http://purl.org/ontology/mo/MusicalManifestation        musical manifestaion
*/
//property labels contains the following lines
/*
http://purl.org/dc/elements/1.1/creator creator
http://purl.org/dc/elements/1.1/title   title
http://purl.org/vocab/relationship/collaboratesWith     collaboration
http://purl.org/ontology/mo/member_of   band
http://purl.org/vocab/bio/0.1/event     event
http://purl.org/vocab/relationship/siblingOf    sibling
http://purl.org/vocab/relationship/parentOf     child
http://purl.org/vocab/relationship/involvedWith involvment
http://purl.org/vocab/relationship/spouseOf     spouse
http://xmlns.com/foaf/0.1/maker maker
http://purl.org/ontology/mo/instrument  instrument
http://purl.org/ontology/mo/producer    producer
http://purl.org/ontology/mo/singer      singer
http://purl.org/ontology/mo/conductor   conductor
http://purl.org/ontology/mo/performingOrchestra orchestra
http://purl.org/ontology/mo/composer    composer
http://purl.org/ontology/mo/lyricist    lyricist
http://purl.org/ontology/mo/performer   performer
http://purl.org/ontology/mo/track       track
http://purl.org/ontology/mo/trackNum    track number
http://purl.org/dc/elements/1.1/description     description
http://purl.org/ontology/mo/release_type        release type
http://purl.org/ontology/mo/release_status      release status
http://purl.org/ontology/mo/group       gropu
http://purl.org/NET/c4dm/event.owl#agent        agent
http://purl.org/NET/c4dm/event.owl#time time
http://purl.org/NET/c4dm/timeline.owl#end       end
http://purl.org/NET/c4dm/timeline.owl#start     start
http://purl.org/vocab/bio/0.1/date      date
http://xmlns.com/foaf/0.1/name  name
http://www.w3.org/2000/01/rdf-schema#label      name
http://purl.org/NET/c4dm/timeline.owl#duration  duration
*/

public class MusicBrainzOldOntologyUtils {

    String downloadedFilesPath, destinationPath;
    File[] files;
    HashMap<String, HashSet<String>> propertyLabels = new HashMap<>();
    HashMap<String, HashSet<String>> classLabels = new HashMap<>();
    HashMap<String, HashSet<String>> classParents = new HashMap<>();
    HashSet<String> properties = new HashSet<>();
    HashSet<String> classes = new HashSet<>();

    public MusicBrainzOldOntologyUtils(String downloadedFilesPath, String destinationPath) throws Exception {
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

    public HashSet<String> processFiles() throws Exception {
        HashSet<String> res = new HashSet<>();
        System.out.println("Processing files");
        HashSet<String> ignoreProperties = new HashSet<>();
        //ignoreProperties.add("http://www.w3.org/2000/01/rdf-schema#seeAlso");
        //ignoreProperties.add("http://www.w3.org/2000/01/rdf-schema#comment");
        //ignoreProperties.add("http://www.w3.org/2002/07/owl#sameAs");
        //ignoreProperties.add("http://xmlns.com/foaf/0.1/isPrimaryTopicOf");
        //ignoreProperties.add("http://open.vocab.org/terms/sortLabel");
        //ignoreProperties.add("http://www.w3.org/2004/02/skos/core#altLabel");
        //ignoreProperties.add("http://purl.org/muto/core#taggedResource");
        //ignoreProperties.add("http://purl.org/NET/c4dm/event.owl#factor");
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
                            String eUri;
                            if (ns[0] instanceof Resource) {
                                eUri = ((Resource) ns[0]).toURI().toString();
                            } else if (ns[0].toString().startsWith("_:node")) {
                                eUri = "http://musicbrainz.org/" + ns[0].getLabel();
                                entityLabels.println(eUri + "\t" + ns[0].getLabel());
                            } else {
                                System.out.println("Invalid pattern: subject is " + ns[0]);
                                continue;
                            }
                            if (ns[1] instanceof Resource) {
                                String aUri = ((Resource) ns[1]).toURI().toString();
                                if ("http://xmlns.com/foaf/0.1/name".equals(aUri) || "http://purl.org/dc/elements/1.1/title".equals(aUri) || "http://www.w3.org/2000/01/rdf-schema#label".equals(aUri)) {
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
                                } else if ("http://purl.org/muto/core#tagLabel".equals(aUri) || "http://www.w3.org/2004/02/skos/core#altLabel".equals(aUri)) {
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
                                        String vUri = ((Resource) ns[2]).toURI().toString();
                                        if (classes.contains(vUri)) {
                                            entityClasses.println(eUri + "\t" + vUri);
                                        } else {
                                            System.out.println("Unknown class: " + vUri);
                                        }
                                    } else {
                                        System.out.println("Invalid class: " + ns[2]);
                                    }
                                } else if (properties.contains(aUri)) {
                                    String vUri;
                                    if (ns[2] instanceof Resource) {
                                        vUri = ((Resource) ns[2]).toURI().toString();
                                        triples.println("<" + eUri + "> <" + aUri + "> <" + vUri + "> .");
                                    } else if (ns[2].toString().startsWith("_:node")) {
                                        vUri = "http://musicbrainz.org/" + ns[2].getLabel();
                                        entityLabels.println(vUri + "\t" + ns[2].getLabel());
                                        triples.println("<" + eUri + "> <" + aUri + "> <" + vUri + "> .");
                                    } else if (ns[2] instanceof Literal) {
                                        triples.println("<" + eUri + "> <" + aUri + "> " + ns[2] + " .");
                                    } else {
                                        System.out.println("Invalid pattern: value is " + ns[2]);
                                        continue;
                                    }
                                } else if (!ignoreProperties.contains(aUri)) {
                                    System.out.println("Unknown property: " + aUri);
                                }
                            } else {
                                System.out.println("Invalid pattern: property is " + ns[1]);
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

    public void createBasicTypesLiteralTypesFile() throws Exception {
        System.out.println("Saving basic types");
        try (PrintWriter out = new PrintWriter(new FileOutputStream(destinationPath + "basic_types_literal_types", false), true)) {
            out.println("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString\tString");
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
            out.println("http://www.w3.org/2001/XMLSchema#nonNegativeInteger\tDouble");
            out.println("http://www.w3.org/2001/XMLSchema#positiveInteger\tDouble");
            out.println("http://www.w3.org/2001/XMLSchema#string\tString");
            out.println("http://www.w3.org/2001/XMLSchema#decimal\tDouble");
            //out.println("http://musicbrainz.org/mm/mm-2.1#beginDate\tDate");
            //out.println("http://musicbrainz.org/mm/mm-2.1#endDate\tDate");
            //out.println("http://musicbrainz.org/mm/mm-2.1#duration\tDouble");

        }
    }

    public static void main(String... args) throws Exception {
        long start = System.currentTimeMillis();
        System.out.println("Started at " + new Date());
        MusicBrainzOldOntologyUtils musicbrainz = new MusicBrainzOldOntologyUtils("/home/massimo/canalikbs/musicbrainz/qald3/downloaded/", "/home/massimo/canalikbs/musicbrainz/qald3/processed");
        musicbrainz.processFiles();
        musicbrainz.createBasicTypesLiteralTypesFile();
        System.out.println("Ended at " + new Date());
        long time = System.currentTimeMillis() - start;
        long sec = time / 1000;
        System.out.println("The process took " + (sec / 60) + "'" + (sec % 60) + "." + (time % 1000) + "\"");
    }

}
