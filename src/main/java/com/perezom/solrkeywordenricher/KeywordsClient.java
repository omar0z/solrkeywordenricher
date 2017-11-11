/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.perezom.solrkeywordenricher;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author Omar
 */
class KeywordsClient {

    private List<SolrInputDocument> documents;
    private ConcurrentUpdateSolrClient solrClient;
    private Client httpClient;

    public KeywordsClient() {
        documents = new ArrayList<>();
        solrClient = new ConcurrentUpdateSolrClient("http://localhost:8983/solr", 32, 8);
        httpClient = ClientBuilder.newClient();
    }
    
    public Response process(){
        return null;
    }

}
