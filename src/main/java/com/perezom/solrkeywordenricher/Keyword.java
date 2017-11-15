/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.perezom.solrkeywordenricher;

/**
 *
 * @author perezom
 */
public class Keyword {
    private int lemma_count;
    private String lemmatized;
    private String[] original;

    public Keyword() {
    }

    public Keyword(int lemma_count, String lemmatized, String[] original) {
        this.lemma_count = lemma_count;
        this.lemmatized = lemmatized;
        this.original = original;
    }
    
    

    public int getLemma_count() {
        return lemma_count;
    }

    public void setLemma_count(int lemma_count) {
        this.lemma_count = lemma_count;
    }

    public String getLemmatized() {
        return lemmatized;
    }

    public void setLemmatized(String lemmatized) {
        this.lemmatized = lemmatized;
    }

    public String[] getOriginal() {
        return original;
    }

    public void setOriginal(String[] original) {
        this.original = original;
    }
    
    
}
