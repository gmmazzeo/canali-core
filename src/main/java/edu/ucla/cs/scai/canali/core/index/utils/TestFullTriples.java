/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class TestFullTriples {

    public static void main(String[] args) throws Exception {
        String regex1 = "<([^<>]*)>\\s<([^<>]*)>\\s<([^<>]*)>";
        String regex2 = "<([^<>]*)>\\s<([^<>]*)>\\s\"(.*)\"";
        Pattern p1 = Pattern.compile(regex1);
        Pattern p2 = Pattern.compile(regex2);
        int i = 0;
        try (BufferedReader in = new BufferedReader(new FileReader("/home/massimo/canalikbs/merged/processed/full_triples.nt"))) {
            String l;
            while ((l = in.readLine()) != null) {
                i++;
                Matcher m1 = p1.matcher(l);
                if (!m1.find()) {
                    Matcher m2 = p2.matcher(l);
                    if (!m2.find()) {
                        System.out.println("Invalid line " + i + " : " + l);
                    } else {
                        //System.out.println("Literal value: "+l);
                    }
                } else {
                    //System.out.println("Object value: "+l);
                }
            }
        }
    }
}
