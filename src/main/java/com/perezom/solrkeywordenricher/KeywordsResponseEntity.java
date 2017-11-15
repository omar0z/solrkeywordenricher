/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.perezom.solrkeywordenricher;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author perezom
 */
public class KeywordsResponseEntity {
    
    private List<Keyword> keywords;

    public KeywordsResponseEntity() {
        this.keywords = new ArrayList<>();
    }

    public KeywordsResponseEntity(List<Keyword> keywords) {
        this.keywords = keywords;
    }

    public List<Keyword> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<Keyword> keywords) {
        this.keywords = keywords;
    }
}
