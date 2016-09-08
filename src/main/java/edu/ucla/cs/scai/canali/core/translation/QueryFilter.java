/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.translation;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class QueryFilter {

    public static final int STRING = 0, DATE = 1, NUMERIC = 2, PERCENTAGE = 3, VARIABLE = 4;

    String variable, operator, operand;
    int type;

    public QueryFilter(String variable, String operator, String operand, int type) {
        this.variable = variable;
        this.operator = operator;
        this.operand = operand;
        this.type = type;
    }

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getOperand() {
        return operand;
    }

    public void setOperand(String operand) {
        this.operand = operand;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        fillStringBuilder(sb);
        return sb.toString();
    }

    public void fillStringBuilder(StringBuilder sb) {
        //TODO: this is just a POC - the filter must be created according to the type
        sb.append("FILTER(");
        if (type == VARIABLE) {
            sb.append("?").append(variable).append(" ").append(operator).append(" ").append("?").append(operand);
        } else if (type == STRING) {
            switch (operator) {
                case "=":
                case "year=":
                    sb.append("REGEX(str(?").append(variable).append("), \"^").append(operand).append("\", \"i\")");
                    break;
                case "<":
                case "<=":
                case ">":
                case ">=":
                    sb.append("str(?").append(variable).append(") ").append(operator).append(" \"").append(operand).append("\"");
                    break;
                case "year<":
                    sb.append("str(?").append(variable).append(") < \"").append(operand).append("\"");
                    break;
                case "year>":
                    sb.append("str(?").append(variable).append(") > \"").append(operand).append("\"");
                    break;
                case "year<=":
                    sb.append("str(?").append(variable).append(") <= \"").append(operand).append("\"");
                    break;
                case "year>=":
                    sb.append("str(?").append(variable).append(") >= \"").append(operand).append("\"");
                    break;
                case "month=":
                    sb.append("REGEX(str(?").append(variable).append("), \"^(\\\\d)*-").append(operand).append("\")");
                    break;
                case "contains":
                    sb.append("REGEX(str(?").append(variable).append("), \"").append(operand).append("\", \"i\")");
                    break;                    
                default:
                    sb.append("REGEX(str(?").append(variable).append("), \"^").append(operand).append("\")");
            }
            /*
             if (operator.equals("=")) {
             sb.append("REGEX(?").append(variable).append(", \"").append(operand).append("\", \"i\")");
             } else {
             sb.append("str(?").append(variable).append(") ").append(operator).append(" ").append("\"").append(operand).append("\"");
             }
             */
        } else if (type == NUMERIC) {
            switch (operator) {
                case "year=":
                    sb.append("REGEX(str(?").append(variable).append("), \"^").append(operand).append("\", \"i\")");
                    break;
                case "year<":
                    sb.append("str(?").append(variable).append(") < \"").append(operand).append("\"");
                    break;
                case "year>":
                    sb.append("str(?").append(variable).append(") > \"").append(operand).append("\"");
                    break;
                case "year<=":
                    sb.append("str(?").append(variable).append(") <= \"").append(operand).append("\"");
                    break;
                case "year>=":
                    sb.append("str(?").append(variable).append(") >= \"").append(operand).append("\"");
                    break;
                case "month=":
                    sb.append("REGEX(str(?").append(variable).append("), \"^(\\\\d)*-").append(operand).append("\")");
                    break;
                default:
                    sb.append("xsd:double(?").append(variable).append(") ").append(operator).append(" ").append(operand);
            }
        } else if (type == PERCENTAGE) {
            sb.append("xsd:double(?").append(variable).append(") ").append(operator).append(" ").append(operand);
        } else if (type == DATE) { //https://pablomendes.wordpress.com/2011/05/19/sparql-xsddate-weirdness/
            switch (operator) {
                case "=":
                case "year=":
                    sb.append("REGEX(str(?").append(variable).append("), \"^").append(operand).append("\")");
                    break;
                case "<":
                case "<=":
                case ">":
                case ">=":
                    sb.append("str(?").append(variable).append(") ").append(operator).append(" \"").append(operand).append("\"");
                    break;
                case "year<":
                    sb.append("str(?").append(variable).append(") < \"").append(operand).append("\"");
                    break;
                case "year>":
                    sb.append("str(?").append(variable).append(") > \"").append(operand).append("\"");
                    break;
                case "year<=":
                    sb.append("str(?").append(variable).append(") <= \"").append(operand).append("\"");
                    break;
                case "year>=":
                    sb.append("str(?").append(variable).append(") >= \"").append(operand).append("\"");
                    break;
                case "month=":
                    sb.append("REGEX(str(?").append(variable).append("), \"^(\\\\d)*-").append(operand).append("\")");
                    break;
                default:
                    sb.append("REGEX(str(?").append(variable).append("), \"^").append(operand).append("\")");
            }
        }
        sb.append(")");
    }
}
