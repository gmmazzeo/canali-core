/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class CreateCompleteTripleFileFromProcessedFiles {

    String processedPath, targetPath;
    final static String label = "http://www.w3.org/2000/01/rdf-schema#label";
    final static String type = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    final static String subClassOf = "http://www.w3.org/2000/01/rdf-schema#subClassOf";

    public CreateCompleteTripleFileFromProcessedFiles(String processedPath, String targetPath) {
        if (!processedPath.endsWith(File.separator)) {
            processedPath += File.separator;
        }
        if (!targetPath.endsWith(File.separator)) {
            targetPath += File.separator;
        }
        this.processedPath = processedPath;
        this.targetPath = targetPath;
    }

    public void start() throws Exception {
        String regex1 = "<([^<>]*)>\\s<([^<>]*)>\\s<([^<>]*)>";
        String regex2 = "<([^<>]*)>\\s<([^<>]*)>\\s\"(.*)\"";
        Pattern p1 = Pattern.compile(regex1);
        Pattern p2 = Pattern.compile(regex2);
        try (PrintWriter out1 = new PrintWriter(new FileWriter(targetPath + "full_triples.nt", false), true);
                PrintWriter out2 = new PrintWriter(new FileWriter(targetPath + "full_entity_labels.nt", false), true);
                PrintWriter out3 = new PrintWriter(new FileWriter(targetPath + "full_entity_classes.nt", false), true);
                PrintWriter out4 = new PrintWriter(new FileWriter(targetPath + "full_class_hierarchy.nt", false), true)) {
            //copy all the triples as they are
            System.out.println("Copying Triples");
            BufferedReader in = new BufferedReader(new FileReader(processedPath + "triples"));            
            String l;
            /*
            while ((l = in.readLine()) != null) {
                Matcher m1 = p1.matcher(l);
                if (!m1.find()) {
                    Matcher m2 = p2.matcher(l);
                    if (!m2.find()) {
                        System.out.println("Invalid line: " + l);
                    } else {
                        out1.println(l);
                    }
                } else {
                    out1.println(l);
                }
            }
            in.close();
            */
            System.out.println("Copying entity labels");
            in = new BufferedReader(new FileReader(processedPath + "entity_labels"));
            while ((l = in.readLine()) != null) {
                String[] s = l.split("\\t");
                out2.println("<" + s[0] + "> <" + label + "> \"" + StringEscapeUtils.escapeJava(s[1]) + "\"@en .");
            }
            in.close();
            /*
            System.out.println("Copying additional entity labels");
            try {
                in = new BufferedReader(new FileReader(processedPath + "additional_entity_labels"));
                while ((l = in.readLine()) != null) {
                    String[] s = l.split("\\t");
                    out.println("<" + s[0] + "> <" + label + "> \"" + s[1] + "\"@en .");
                }
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
                    */
            System.out.println("Copying entity classes");
            in = new BufferedReader(new FileReader(processedPath + "entity_classes"));
            while ((l = in.readLine()) != null) {
                String[] s = l.split("\\t");
                out3.println("<" + s[0] + "> <" + type + "> <" + s[1] + "> .");
            }
            in.close();
            System.out.println("Copying class hierarchy");
            in = new BufferedReader(new FileReader(processedPath + "class_parents"));
            while ((l = in.readLine()) != null) {
                String[] s = l.split("\\t");
                out4.println("<" + s[0] + "> <" + subClassOf + "> <" + s[1] + "> .");
            }
            in.close();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args == null || args.length != 2) {
            //System.out.println("Use path of processed files and destination directory as parameters");
            //System.exit(0);
            args = new String[2];
            args[0] = "/home/massimo/canalikbs/merged/processed/";
            args[1] = "/home/massimo/canalikbs/merged/processed/";
        }
        new CreateCompleteTripleFileFromProcessedFiles(args[0], args[1]).start();
    }
}
