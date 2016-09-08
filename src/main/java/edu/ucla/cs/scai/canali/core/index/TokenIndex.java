/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index;

import edu.ucla.cs.scai.canali.core.index.tokens.RankOperatorToken;
import edu.ucla.cs.scai.canali.core.index.tokens.LiteralDateToken;
import edu.ucla.cs.scai.canali.core.index.tokens.LiteralToken;
import edu.ucla.cs.scai.canali.core.index.tokens.QuestionStartToken;
import edu.ucla.cs.scai.canali.core.index.tokens.LiteralStringToken;
import edu.ucla.cs.scai.canali.core.index.tokens.PossessiveDeterminerToken;
import edu.ucla.cs.scai.canali.core.index.tokens.DirectBinaryOperatorToken;
import edu.ucla.cs.scai.canali.core.index.tokens.LiteralNumericToken;
import edu.ucla.cs.scai.canali.core.index.tokens.UnaryOperatorToken;
import edu.ucla.cs.scai.canali.core.index.tokens.IndexedToken;
import edu.ucla.cs.scai.canali.core.index.tokens.TopKOperatorToken;
import edu.ucla.cs.scai.canali.core.index.tokens.LiteralPercentageToken;
import edu.ucla.cs.scai.canali.core.index.tokens.IndirectBinaryOperatorToken;
import edu.ucla.cs.scai.canali.core.index.tokens.PropertyToken;
import edu.ucla.cs.scai.canali.core.index.tokens.AugmentedClassToken;
import edu.ucla.cs.scai.canali.core.index.tokens.ClassToken;
import edu.ucla.cs.scai.canali.core.index.tokens.ConstraintPrefixToken;
import edu.ucla.cs.scai.canali.core.index.tokens.EntityToken;
import edu.ucla.cs.scai.canali.core.index.tokens.OntologyElementToken;
import static edu.ucla.cs.scai.canali.core.index.tokens.LiteralToken.*;
import edu.ucla.cs.scai.canali.core.index.tokens.OperatorToken;
import edu.ucla.cs.scai.canali.core.index.tokens.RankCountOperatorToken;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.RAMDirectory;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class TokenIndex {

    //The DEFAULT_PATH is used when the system property kb.index.dir is not defined
    //static final String DEFAULT_PATH = "/home/massimo/aquawd/processed-dbpedia-ontology-extended/";    
    //static final String PATH = "/home/massimo/aquawd/processed-dbpedia-ontology/";
    //static final String PATH = "/home/massimo/aquawd/processed-biomedical-ontology/";
    //static final String PATH = "/home/massimo/aquawd/processed-musicbrainz-old-ontology/";
    //static final String DEFAULT_PATH = "/home/massimo/canalikbs/dbpedia/2015-10/index/";
    static final String DEFAULT_PATH = "/home/massimo/canalikbs/merged/index/";
    //static final String DEFAULT_PATH = "/home/massimo/canalikbs/biomedical/qald4/index/";
    protected static final HashMap<Integer, IndexedToken> elements;
    protected static final HashMap<String, Integer> ontologyElementsIdByUri;
    protected static final Analyzer analyzer;
    protected static final Directory directory;

    private static final String PATH;

    static {
        HashMap<String, Analyzer> analyzerMap = new HashMap<>();
        analyzerMap.put("label", new EnglishAnalyzer(CharArraySet.EMPTY_SET));
        analyzerMap.put("id", new WhitespaceAnalyzer());
        analyzerMap.put("type", new WhitespaceAnalyzer());
        analyzerMap.put("domainOf", new WhitespaceAnalyzer());
        analyzerMap.put("rangeOf", new WhitespaceAnalyzer());
        analyzer = new PerFieldAnalyzerWrapper(new WhitespaceAnalyzer(), analyzerMap);
        String path = System.getProperty("kb.index.dir");
        if (path != null) {
            System.out.println("Directory with index set as " + path);
        } else {
            System.out.println("Directory with index not set. Set kb.index.dir in System.properties. Using default dir " + DEFAULT_PATH);
            path = DEFAULT_PATH;
        }
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        PATH = path;
        ontologyElementsIdByUri = new HashMap<>();
        HashMap<Integer, IndexedToken> tempElements = null;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(PATH + "elements"))) {
            System.out.println("Loading ontology elements from " + PATH + "elements");
            tempElements = (HashMap<Integer, IndexedToken>) in.readObject();
            for (IndexedToken e : tempElements.values()) {
                if (e instanceof OntologyElementToken) {
                    ontologyElementsIdByUri.put(((OntologyElementToken) e).getUri(), e.getId());
                }
            }
            IndexedToken.counter = in.readInt();
        } catch (Exception ex) {
            Logger.getLogger(TokenIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
        elements = tempElements;

        //now load the Lucene index from file
        directory = new RAMDirectory();
        FSDirectory tempDirectory;
        System.out.println("Loading ontology index from disk");
        try {
            tempDirectory = FSDirectory.open(Paths.get(PATH + "lucene"));
            for (String file : tempDirectory.listAll()) {
                directory.copyFrom(tempDirectory, file, file, IOContext.DEFAULT);
            }
        } catch (IOException ex) {
            Logger.getLogger(TokenIndex.class.getName()).log(Level.SEVERE, null, ex);
        }
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
        try (IndexWriter writer = new IndexWriter(directory, iwc)) {
            indexQuestionStarts(writer);
            indexConstraintPrefixes(writer);
            indexOperators(writer);
            indexPossessiveAdjective(writer);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void addElement(IndexWriter writer, IndexedToken e) throws Exception {
        Document doc = new Document();
        doc.add(new Field("label", e.getText(), TextField.TYPE_NOT_STORED));
        doc.add(new IntField("id", e.getId(), IntField.TYPE_STORED));
        doc.add(new Field("type", e.getType(), TextField.TYPE_NOT_STORED));
        writer.addDocument(doc);
        elements.put(e.getId(), e);
    }

    private static void indexQuestionStarts(IndexWriter writer) throws Exception {
        addElement(writer, new QuestionStartToken("Give me the", IndexedToken.UNDEFINED, QuestionStartToken.PLAIN, true));
        addElement(writer, new QuestionStartToken("Give me the count of", IndexedToken.PLURAL, QuestionStartToken.COUNT, false));
        addElement(writer, new QuestionStartToken("What is", IndexedToken.SINGULAR, QuestionStartToken.PLAIN, true));
        addElement(writer, new QuestionStartToken("What is the", IndexedToken.SINGULAR, QuestionStartToken.PLAIN, true));
        addElement(writer, new QuestionStartToken("What is the count of", IndexedToken.PLURAL, QuestionStartToken.COUNT, false));
        addElement(writer, new QuestionStartToken("What are", IndexedToken.PLURAL, QuestionStartToken.PLAIN, true));
        addElement(writer, new QuestionStartToken("What are the", IndexedToken.PLURAL, QuestionStartToken.PLAIN, false));
        addElement(writer, new QuestionStartToken("What has", IndexedToken.SINGULAR, QuestionStartToken.PLAIN, false));
        addElement(writer, new QuestionStartToken("Who has", IndexedToken.SINGULAR, QuestionStartToken.PLAIN, false));
        addElement(writer, new QuestionStartToken("Who is", IndexedToken.SINGULAR, QuestionStartToken.PLAIN, true));
        addElement(writer, new QuestionStartToken("Who is the", IndexedToken.SINGULAR, QuestionStartToken.PLAIN, false));
        addElement(writer, new QuestionStartToken("Who are the", IndexedToken.PLURAL, QuestionStartToken.PLAIN, false));
        addElement(writer, new QuestionStartToken("Is", IndexedToken.SINGULAR, QuestionStartToken.YESNO, true));
        addElement(writer, new QuestionStartToken("Is the", IndexedToken.SINGULAR, QuestionStartToken.YESNO, false));
        addElement(writer, new QuestionStartToken("Is there a", IndexedToken.SINGULAR, QuestionStartToken.YESNO, false));
        addElement(writer, new QuestionStartToken("Are", IndexedToken.PLURAL, QuestionStartToken.YESNO, true));
        addElement(writer, new QuestionStartToken("Are the", IndexedToken.PLURAL, QuestionStartToken.YESNO, false));
        addElement(writer, new QuestionStartToken("Are there ", IndexedToken.PLURAL, QuestionStartToken.YESNO, false));
    }

    private static void indexConstraintPrefixes(IndexWriter writer) throws Exception {
        addElement(writer, new ConstraintPrefixToken("having", true));
        addElement(writer, new ConstraintPrefixToken("and having", true));
        addElement(writer, new ConstraintPrefixToken("with", true));
        addElement(writer, new ConstraintPrefixToken("and with", true));
        addElement(writer, new ConstraintPrefixToken("and", true));
    }

    private static void indexOperators(IndexWriter writer) throws Exception {
        addElement(writer, new DirectBinaryOperatorToken("equal to", "=", true));
        addElement(writer, new DirectBinaryOperatorToken("greater than", ">", true));
        addElement(writer, new DirectBinaryOperatorToken("less than", "<", true));
        addElement(writer, new DirectBinaryOperatorToken("different than", "!=", true));
        addElement(writer, new DirectBinaryOperatorToken("=", "=", false));
        addElement(writer, new DirectBinaryOperatorToken(">", ">", false));
        addElement(writer, new DirectBinaryOperatorToken("<", "<", false));
        addElement(writer, new DirectBinaryOperatorToken(">=", ">=", false));
        addElement(writer, new DirectBinaryOperatorToken("<=", "<=", false));
        addElement(writer, new DirectBinaryOperatorToken("<>", "!=", false));
        addElement(writer, new DirectBinaryOperatorToken("with year equal to", "year=", false));
        addElement(writer, new DirectBinaryOperatorToken("with year greater than", "year>", false));
        addElement(writer, new DirectBinaryOperatorToken("with year less than", "year<", false));
        addElement(writer, new DirectBinaryOperatorToken("with month equal to", "month=", false));
        addElement(writer, new IndirectBinaryOperatorToken("the same as that of", "=", false));
        addElement(writer, new IndirectBinaryOperatorToken("equal to that of", "=", false));
        addElement(writer, new IndirectBinaryOperatorToken("greater than that of", ">", false));
        addElement(writer, new IndirectBinaryOperatorToken("less than that of", "<", false));
        addElement(writer, new IndirectBinaryOperatorToken("different than that of", "!=", false));
        addElement(writer, new UnaryOperatorToken("with any", "exists", false));
        addElement(writer, new UnaryOperatorToken("with some", "exists", false));
        addElement(writer, new UnaryOperatorToken("having some", "exists", false));
        addElement(writer, new UnaryOperatorToken("without any", "not exists", false));
        addElement(writer, new UnaryOperatorToken("without specified", "not exists", false));
    }

    private static void indexPossessiveAdjective(IndexWriter writer) throws Exception {
        addElement(writer, new PossessiveDeterminerToken("its", IndexedToken.SINGULAR));
        addElement(writer, new PossessiveDeterminerToken("her", IndexedToken.SINGULAR));
        addElement(writer, new PossessiveDeterminerToken("his", IndexedToken.SINGULAR));
        addElement(writer, new PossessiveDeterminerToken("their", IndexedToken.PLURAL));
    }

    private HashSet<String> getAdmissableLiterals(String[] rangeOfProperties) {
        HashSet<String> admissableLiterals = new HashSet<>();
        if (rangeOfProperties == null) { //context rules where disabled
            admissableLiterals.add(DATE);
            admissableLiterals.add(STRING);
            admissableLiterals.add(DOUBLE);
        } else {
            for (String rangeOfProperty : rangeOfProperties) {
                PropertyToken ae = getPropertyByUri(rangeOfProperty);
                if (ae.hasBasicTypeRange(DATE)) {
                    admissableLiterals.add(DATE);
                }
                if (ae.hasBasicTypeRange(STRING)) {
                    admissableLiterals.add(STRING);
                }
                if (ae.hasBasicTypeRange(DOUBLE)) {
                    admissableLiterals.add(DOUBLE);
                }
            }
        }
        return admissableLiterals;
    }

    public ArrayList<LiteralToken> getLiteralElements(String search, String[] rangeOfProperties, boolean dateToNumber) {
        ArrayList<LiteralToken> res = new ArrayList<>();
        HashSet<String> admissableLiterals = getAdmissableLiterals(rangeOfProperties);
        if (admissableLiterals.contains(DATE)) {
            if (dateToNumber) {
                try {
                    res.add(new LiteralNumericToken(search));
                } catch (Exception e) {
                }
            } else {
                try {
                    res.add(new LiteralDateToken(search));
                } catch (Exception e) {
                }
            }
        }
        if (admissableLiterals.contains(DOUBLE)) {
            try {
                res.add(new LiteralNumericToken(search));
            } catch (Exception e) {
            }
            try {
                res.add(new LiteralPercentageToken(search));
            } catch (Exception e) {
            }
        }
        if (admissableLiterals.contains(STRING) || admissableLiterals.contains(DATE)) {
            try {
                if (search.startsWith("\"")) {
                    if (search.endsWith("\"") && search.length() > 1) {
                        res.add(new LiteralStringToken(search));
                    } else {
                        res.add(new LiteralStringToken(search + "\""));
                    }
                } else {
                    if (search.endsWith("\"") && search.length() > 1) {
                        res.add(new LiteralStringToken("\"" + search));
                    } else {
                        res.add(new LiteralStringToken("\"" + search + "\""));
                    }                    
                }
            } catch (Exception e) {
            }
        }
        return res;
    }

    public LiteralNumericToken getIntegerLiteralElement(String search) {
        try {
            return new LiteralNumericToken(search);
        } catch (Exception e) {
        }
        return null;
    }

    public ArrayList<RankOperatorToken> getRankOperatorElements(String search) {
        ArrayList<RankOperatorToken> res = new ArrayList<>();
        if (search == null) {
            return res;
        }
        search = search.trim().toLowerCase();
        if (search.equals("t") || search.equals("th") || search.equals("the")) {
            res.add(new RankOperatorToken(1, false));
            res.add(new RankOperatorToken(1, true));
            res.add(new RankOperatorToken(2, false));
            res.add(new RankOperatorToken(2, true));
            return res;
        }
        if (search.startsWith("the ")) {
            search = search.replaceFirst("the ", "").trim();
            try {
                String[] ss = search.split("\\s");
                int rank = Integer.parseInt(ss[0]);
                if (rank <= 0) {
                    return res;
                }
                if (ss.length > 1) {
                    if (ss[1].startsWith("g")) {
                        res.add(new RankOperatorToken(rank, false));
                        res.add(new RankCountOperatorToken(rank, false));
                    } else if (ss[1].startsWith("s")) {
                        res.add(new RankOperatorToken(rank, true));
                        res.add(new RankCountOperatorToken(rank, true));
                    } else {
                        res.add(new RankOperatorToken(rank, false));
                        res.add(new RankOperatorToken(rank, true));
                        res.add(new RankCountOperatorToken(rank, false));
                        res.add(new RankCountOperatorToken(rank, true));
                    }
                } else {
                    res.add(new RankOperatorToken(rank, false));
                    res.add(new RankOperatorToken(rank, true));
                    res.add(new RankCountOperatorToken(rank, false));
                    res.add(new RankCountOperatorToken(rank, true));
                }
            } catch (Exception e) {
                res.add(new RankOperatorToken(1, false));
                res.add(new RankOperatorToken(2, false));
                res.add(new RankOperatorToken(1, true));
                res.add(new RankOperatorToken(2, true));
                res.add(new RankCountOperatorToken(1, false));
                res.add(new RankCountOperatorToken(2, false));
                res.add(new RankCountOperatorToken(1, true));
                res.add(new RankCountOperatorToken(2, true));
            }
        }

        return res;
    }

    public ArrayList<TopKOperatorToken> getTopKOperatorElements(String search) {
        ArrayList<TopKOperatorToken> res = new ArrayList<>();
        if (search == null) {
            return res;
        }
        search = search.trim().toLowerCase();
        if (search.equals("o") || search.equals("on") || search.equals("one")
                || search.equals("one ") || search.equals("one o") || search.equals("one of")
                || search.equals("one of ") || search.equals("one of t")
                || search.equals("one of th") || search.equals("one of the")) {
            res.add(new TopKOperatorToken(10, false));
            res.add(new TopKOperatorToken(10, true));
            return res;
        }
        if (search.startsWith("one of the ")) {
            search = search.replaceFirst("one of the ", "").trim();
            try {
                String[] ss = search.split("\\s");
                int k = Integer.parseInt(ss[0]);
                if (k <= 0) {
                    return res;
                }
                if (ss.length > 1) {
                    if (ss[1].startsWith("g")) {
                        res.add(new TopKOperatorToken(k, false));
                    } else if (ss[1].startsWith("s")) {
                        res.add(new TopKOperatorToken(k, true));
                    } else {
                        res.add(new TopKOperatorToken(k, false));
                        res.add(new TopKOperatorToken(k, true));
                    }
                } else {
                    res.add(new TopKOperatorToken(k, false));
                    res.add(new TopKOperatorToken(k, true));
                }
            } catch (Exception e) {
                res.add(new TopKOperatorToken(10, false));
                res.add(new TopKOperatorToken(10, true));
            }
        }

        return res;
    }

    public ArrayList<IndexedToken> getTokenElements(String search,
            String domainsOfProperty[], String rangesOfProperty[],
            String[] propertyDomains,
            int maxResults, String... tokenClasses) {
        ArrayList<IndexedToken> res = new ArrayList<>();
        if (search == null) {
            search = "";
        }
        boolean classFound = false;
        boolean classAcceptable = false;
        HashSet<String> admissableLiterals = getAdmissableLiterals(rangesOfProperty);
        try {
            BooleanQuery globalQuery = new BooleanQuery();
            BooleanQuery typeQuery = new BooleanQuery();
            if (tokenClasses != null && tokenClasses.length > 0) {
                for (int i = 0; i < tokenClasses.length; i++) {
                    BooleanQuery subTypeQuery = new BooleanQuery();
                    subTypeQuery.add(new TermQuery(new Term("type", tokenClasses[i])), BooleanClause.Occur.MUST);
                    switch (tokenClasses[i]) {
                        case IndexedToken.PROPERTY:
                            if (domainsOfProperty != null && domainsOfProperty.length > 0) {
                                BooleanQuery domainOfQuery = new BooleanQuery();
                                for (String domainOfProperty : domainsOfProperty) {
                                    domainOfQuery.add(new TermQuery(new Term("domainOfProperty", QueryParser.escape(domainOfProperty))), BooleanClause.Occur.SHOULD);
                                }
                                subTypeQuery.add(domainOfQuery, BooleanClause.Occur.MUST);
                            }
                            if (rangesOfProperty != null && rangesOfProperty.length > 0) {
                                BooleanQuery rangeOfQuery = new BooleanQuery();
                                for (String rangeOfProperty : rangesOfProperty) {
                                    rangeOfQuery.add(new TermQuery(new Term("rangeOfProperty", QueryParser.escape(rangeOfProperty))), BooleanClause.Occur.SHOULD);
                                }
                                subTypeQuery.add(rangeOfQuery, BooleanClause.Occur.MUST);
                            }
                            if (propertyDomains != null && propertyDomains.length > 0) {
                                BooleanQuery domainQuery = new BooleanQuery();
                                for (String propertyDomain : propertyDomains) {
                                    domainQuery.add(new TermQuery(new Term("propertyDomain", QueryParser.escape(propertyDomain))), BooleanClause.Occur.SHOULD);
                                }
                                subTypeQuery.add(domainQuery, BooleanClause.Occur.MUST);
                            }
                            break;
                        case IndexedToken.ENTITY:
                            if (domainsOfProperty != null && domainsOfProperty.length > 0) {
                                BooleanQuery domainOfQuery = new BooleanQuery();
                                for (String domainOfProperty : domainsOfProperty) {
                                    domainOfQuery.add(new TermQuery(new Term("domainOfProperty", QueryParser.escape(domainOfProperty))), BooleanClause.Occur.SHOULD);
                                }
                                subTypeQuery.add(domainOfQuery, BooleanClause.Occur.MUST);
                            }
                            if (rangesOfProperty != null && rangesOfProperty.length > 0) {
                                BooleanQuery rangeOfQuery = new BooleanQuery();
                                for (String rangeOfProperty : rangesOfProperty) {
                                    rangeOfQuery.add(new TermQuery(new Term("rangeOfProperty", QueryParser.escape(rangeOfProperty))), BooleanClause.Occur.SHOULD);
                                }
                                subTypeQuery.add(rangeOfQuery, BooleanClause.Occur.MUST);
                            }
                            break;
                        case IndexedToken.CLASS:
                            classAcceptable = true;
                            if (domainsOfProperty != null && domainsOfProperty.length > 0) {
                                BooleanQuery domainOfQuery = new BooleanQuery();
                                for (String domainOfProperty : domainsOfProperty) {
                                    domainOfQuery.add(new TermQuery(new Term("domainOfProperty", QueryParser.escape(domainOfProperty))), BooleanClause.Occur.SHOULD);
                                }
                                subTypeQuery.add(domainOfQuery, BooleanClause.Occur.MUST);
                            }
                            if (rangesOfProperty != null && rangesOfProperty.length > 0) {
                                BooleanQuery rangeOfQuery = new BooleanQuery();
                                for (String rangeOfProperty : rangesOfProperty) {
                                    rangeOfQuery.add(new TermQuery(new Term("rangeOfProperty", QueryParser.escape(rangeOfProperty))), BooleanClause.Occur.SHOULD);
                                }
                                subTypeQuery.add(rangeOfQuery, BooleanClause.Occur.MUST);
                            }
                            break;
                    }
                    typeQuery.add(subTypeQuery, BooleanClause.Occur.SHOULD);
                }
                if (tokenClasses.length > 1) {
                    //typeQuery.setMinimumNumberShouldMatch(1);
                }
                globalQuery.add(typeQuery, BooleanClause.Occur.MUST);
            }

            BooleanQuery searchQuery = new BooleanQuery();
            String[] ss = search.split(" ");
            for (String s : ss) {
                searchQuery.add(new TermQuery(new Term("label", QueryParser.escape(s))), BooleanClause.Occur.SHOULD);
            }
            //searchQuery.setMinimumNumberShouldMatch(1);
            globalQuery.add(searchQuery, BooleanClause.Occur.MUST);
            QueryParser parser = new QueryParser("", analyzer);
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                String queryString = globalQuery.toString(); //I need this because the parser works differently of different search features - look at its definition
                ScoreDoc[] hits = searcher.search(parser.parse(queryString), maxResults * 5).scoreDocs;
                for (ScoreDoc r : hits) {
                    Document doc = searcher.doc(r.doc);
                    IndexedToken element = elements.get(doc.getField("id").numericValue().intValue());
                    if (element instanceof DirectBinaryOperatorToken || element instanceof IndirectBinaryOperatorToken) {
                        String op = ((OperatorToken) element).getSymbol();
                        if (op.startsWith("year") || op.startsWith("month")) {
                            if (admissableLiterals.contains(DATE)) {
                                res.add(element);
                            }
                        } else if (op.equals("=") || !admissableLiterals.isEmpty()) {
                            res.add(element);
                        }
                    } else {
                        res.add(element);
                        if (element instanceof ClassToken) {
                            String fullText = search.toLowerCase();
                            fullText = fullText.toLowerCase();
                            boolean isPrefix = true;
                            if (fullText.endsWith(".")) {
                                fullText = fullText.substring(0, fullText.length() - 1);
                                isPrefix = false;
                            } else if (fullText.endsWith("?")) {
                                fullText = fullText.substring(0, fullText.length() - 1);
                                isPrefix = false;
                            } else if (fullText.endsWith(" and having")) {
                                fullText = fullText.substring(0, fullText.length() - 11);
                                isPrefix = false;
                            } else if (fullText.endsWith(" and with")) {
                                fullText = fullText.substring(0, fullText.length() - 9);
                                isPrefix = false;
                            } else if (fullText.endsWith(" having")) {
                                fullText = fullText.substring(0, fullText.length() - 7);
                                isPrefix = false;
                            } else if (fullText.endsWith(" with")) {
                                fullText = fullText.substring(0, fullText.length() - 5);
                                isPrefix = false;
                            }
                            fullText = fullText.trim();
                            classFound = true;
                            ClassToken ct = (ClassToken) element;
                            HashSet<String> searchWords = new HashSet(Arrays.asList(fullText.split(" ")));
                            HashSet<String> classWords = new HashSet(Arrays.asList((ct).getText().split(" "))); //this does not work with plural forms                            
                            searchWords.removeAll(classWords);
                            if (!searchWords.isEmpty()) {
                                AugmentedClassToken act = new AugmentedClassToken(ct, searchWords, isPrefix);
                                res.add(act);
                            }
                        }
                    }
                    if (res.size() == maxResults) {
                        //break;
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(TokenIndex.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        if (classAcceptable && !classFound) {
            System.out.println("Try class + keywords for " + search);
        }
        return res;
    }

    public PropertyToken getPropertyByUri(String uri) {
        Integer id = ontologyElementsIdByUri.get(uri);
        if (id == null) {
            return null;
        }
        IndexedToken e = elements.get(id);
        if (e == null || !(e instanceof PropertyToken)) {
            return null;
        }
        return (PropertyToken) e;
    }

    public static void main(String... args) {
        //new TokenIndex().getTokenElements("gi", null, null, null, null, 20, IndexedToken.QUESTION_START);
    }

    public ArrayList<EntityToken> getEntityElements(String search, int limit) {
        ArrayList<EntityToken> res = new ArrayList<>();
        for (IndexedToken i : getTokenElements(search, null, null, null, limit, IndexedToken.ENTITY)) {
            res.add((EntityToken) i);
        }
        return res;
    }

    public ArrayList<PropertyToken> getPropertyElements(String search, int limit) {
        ArrayList<PropertyToken> res = new ArrayList<>();
        for (IndexedToken i : getTokenElements(search, null, null, null, limit, IndexedToken.PROPERTY)) {
            res.add((PropertyToken) i);
        }
        return res;
    }

    public boolean containsElement(String property) {
        return ontologyElementsIdByUri.containsKey(property);
    }

    public HashSet<String>[][] describeProperty(String label, int limit) {
        HashSet<String>[][] res = new HashSet[2][];
        res[0] = new HashSet[2];
        res[1] = new HashSet[3];
        Integer idA = ontologyElementsIdByUri.get(label);
        if (idA == null) {
            return res;
        }
        IndexedToken e = elements.get(idA);
        if (e == null || !(e instanceof PropertyToken)) {
            return res;
        }
        PropertyToken a = (PropertyToken) e;

        BooleanQuery globalQuery = new BooleanQuery();
        BooleanQuery typeQuery = new BooleanQuery();
        BooleanQuery subTypeQuery = new BooleanQuery();
        subTypeQuery.add(new TermQuery(new Term("type", IndexedToken.CLASS)), BooleanClause.Occur.MUST);
        typeQuery.add(subTypeQuery, BooleanClause.Occur.MUST);
        subTypeQuery = new BooleanQuery();
        subTypeQuery.add(new TermQuery(new Term("type", IndexedToken.PROPERTY)), BooleanClause.Occur.MUST);
        typeQuery.add(subTypeQuery, BooleanClause.Occur.MUST);
        globalQuery.add(typeQuery, BooleanClause.Occur.MUST);
        globalQuery.add(new TermQuery(new Term("domainOfProperty", QueryParser.escape(label))), BooleanClause.Occur.MUST);

        res[0][0] = new HashSet<>();
        res[0][1] = new HashSet<>();
        QueryParser parser = new QueryParser("", analyzer);
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            String queryString = globalQuery.toString(); //I need this because the parser works differently of different search features - look at its definition
            ScoreDoc[] hits = searcher.search(parser.parse(queryString), 1000).scoreDocs;
            for (ScoreDoc r : hits) {
                Document doc = searcher.doc(r.doc);
                IndexedToken element = elements.get(doc.getField("id").numericValue().intValue());
                if (element instanceof PropertyToken) {
                    res[0][1].add(((PropertyToken) element).uri);
                } else {
                    res[0][0].add(((ClassToken) element).uri);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(TokenIndex.class.getName()).log(Level.SEVERE, null, ex);
        }

        globalQuery = new BooleanQuery();
        typeQuery = new BooleanQuery();
        subTypeQuery = new BooleanQuery();
        subTypeQuery.add(new TermQuery(new Term("type", IndexedToken.CLASS)), BooleanClause.Occur.MUST);
        typeQuery.add(subTypeQuery, BooleanClause.Occur.MUST);
        subTypeQuery = new BooleanQuery();
        subTypeQuery.add(new TermQuery(new Term("type", IndexedToken.PROPERTY)), BooleanClause.Occur.MUST);
        typeQuery.add(subTypeQuery, BooleanClause.Occur.MUST);
        globalQuery.add(typeQuery, BooleanClause.Occur.MUST);
        globalQuery.add(new TermQuery(new Term("rangeOfProperty", QueryParser.escape(label))), BooleanClause.Occur.MUST);

        res[1][0] = new HashSet<>();
        res[1][1] = new HashSet<>();
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            String queryString = globalQuery.toString(); //I need this because the parser works differently of different search features - look at its definition
            ScoreDoc[] hits = searcher.search(parser.parse(queryString), 1000).scoreDocs;
            for (ScoreDoc r : hits) {
                Document doc = searcher.doc(r.doc);
                IndexedToken element = elements.get(doc.getField("id").numericValue().intValue());
                if (element instanceof PropertyToken) {
                    res[1][1].add(((PropertyToken) element).uri);
                } else {
                    res[1][0].add(((ClassToken) element).uri);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(TokenIndex.class.getName()).log(Level.SEVERE, null, ex);
        }

        String[] atts = new String[1];
        atts[0] = label;
        res[1][2] = new HashSet<>();
        for (String l : getAdmissableLiterals(atts)) {
            res[1][2].add(l);
        }
        return res;
    }

}
