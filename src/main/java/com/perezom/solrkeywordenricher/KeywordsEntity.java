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
public class KeywordsEntity {

        private String text;
        private int threshold;
        private static final String DORIS_API_KEY = "59b252b6511e69032defce27";

        public KeywordsEntity(String text) {
            this.text = text;
            this.threshold = 10;
        }

        public KeywordsEntity(String text, int threshold) {
            this.text = text;
            this.threshold = threshold;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getThreshold() {
            return threshold;
        }

        public void setThreshold(int threshold) {
            this.threshold = threshold;
        }
    }