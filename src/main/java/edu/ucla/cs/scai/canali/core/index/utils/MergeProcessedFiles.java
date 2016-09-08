/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.utils;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class MergeProcessedFiles {

    public static void main(String[] args) throws Exception {
        String[] source = new String[]{"/home/massimo/canalikbs/dbpedia/2015-10/processed/",
            "/home/massimo/canalikbs/biomedical/qald4/processed/",
            "/home/massimo/canalikbs/musicbrainz/qald3/processed/"};
        String[] fileNames = new String[]{"additional_property_labels",
            "additional_class_labels", "additional_entity_labels",
            "property_labels", "class_labels", "class_parents",
            "entity_classes", "entity_labels", "triples"};

        String target = "/home/massimo/canalikbs/merged/processed/";
        for (String fin : fileNames) {
            try (PrintWriter out = new PrintWriter(new FileOutputStream(target + fin, false), true)) {
                for (String inPath : source) {
                    try (BufferedReader in = new BufferedReader(new FileReader(inPath + fin))) {
                        String l;
                        while ((l = in.readLine()) != null) {
                            out.println(l);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
