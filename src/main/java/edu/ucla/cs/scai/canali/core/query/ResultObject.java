/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucla.cs.scai.canali.core.query;

/**
 *
 * @author Giuseppe M. Mazzeo <mazzeo@cs.ucla.edu>
 */
public class ResultObject {

    String l, u, a, p, le, ue, pt, ide, id, spt, spv;
    Boolean ask;

    public ResultObject(boolean ask) {
        this.ask = ask;
    }

    public ResultObject(String label, String url, String abstr, String picture, String labelEntity, String urlEntity, String propertyText, String uri, String uriEntity, String sortinPropertyText, String sortingPropertyValue) {
        this.l = label;
        this.u = url;
        this.a = abstr;
        this.p = picture;
        this.le = labelEntity;
        this.ue = urlEntity;
        this.pt = propertyText;
        this.id = uri;
        this.ide = uriEntity;
        if (uri != null) {
            id = uri + "/" + label;
        } else if (uriEntity != null) {
            id = uriEntity + "/" + label;
        }
        this.spt = sortinPropertyText;
        this.spv = sortingPropertyValue;
    }

    public String getL() {
        return l;
    }

    public String getU() {
        return u;
    }

    public String getA() {
        return a;
    }

    public String getP() {
        return p;
    }

    public String getLe() {
        return le;
    }

    public void setLe(String le) {
        this.le = le;
    }

    public String getUe() {
        return ue;
    }

    public void setUe(String ue) {
        this.ue = ue;
    }

    public String getPt() {
        return pt;
    }

    public void setPt(String pt) {
        this.pt = pt;
    }

    public String getIde() {
        return ide;
    }

    public void setIde(String ide) {
        this.ide = ide;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSpt() {
        return spt;
    }

    public void setSpt(String spt) {
        this.spt = spt;
    }

    public String getSpv() {
        return spv;
    }

    public void setSpv(String spv) {
        this.spv = spv;
    }

    public Boolean getAsk() {
        return ask;
    }

    public void setAsk(Boolean ask) {
        this.ask = ask;
    }
}
