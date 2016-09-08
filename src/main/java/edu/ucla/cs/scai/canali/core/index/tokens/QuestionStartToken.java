/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.index.tokens;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class QuestionStartToken extends IndexedSyntacticToken {

    public static final int PLAIN = 0, COUNT = 1, YESNO = 2;

    int questionType;

    public QuestionStartToken(String text, int multiplicity, int questionType, boolean prefix) {
        super(text, multiplicity, prefix);
        this.questionType = questionType;
    }

    @Override
    public String getType() {
        return QUESTION_START;
    }

    public int getQuestionType() {
        return questionType;
    }

}
