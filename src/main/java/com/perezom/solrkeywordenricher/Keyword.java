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
    
    private double score;
    private int frequency;
    private String lemmatized;
    private String[] original;

    public Keyword() {
    }

    public Keyword(double score, int frequency, String lemmatized, String[] original) {
        this.score = score;
        this.frequency = frequency;
        this.lemmatized = lemmatized;
        this.original = original;
    }

    
    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
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
