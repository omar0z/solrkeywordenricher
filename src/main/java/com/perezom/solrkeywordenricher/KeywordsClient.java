/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.perezom.solrkeywordenricher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

/**
 *
 * @author Omar
 */
class KeywordsClient {

    private SolrDocumentList documents;
    private final ConcurrentUpdateSolrClient solrClient;
    private final List<Callable<SolrDocument>> callables;
    private final List<SolrInputDocument> enrichedDocuments;
    private final ExecutorService executor;
    private static final Logger LOG = Logger.getLogger(KeywordsClient.class.getName());
    private static final String KEYWORD_SERVICE_URI = "http://s-cnect-drive-in-d/service/keyword-extraction";

    public KeywordsClient() {
        this.documents = new SolrDocumentList();
        this.solrClient = new ConcurrentUpdateSolrClient("http://localhost:8983/solr/fet-h2020-ria-noHPC-1", 32, 8);
        this.callables = new ArrayList<>();
        this.executor = Executors.newWorkStealingPool();
        this.enrichedDocuments = new ArrayList();
    }

    public Response.ResponseBuilder process() {
        getDocumentsFromSolr();
        prepareDocs();
        sendToSolr();
        return Response.ok();
    }

    private void getDocumentsFromSolr() {
        LOG.info("Getting Solr documents from localhost...");
        SolrQuery query = new SolrQuery();
        query.setRequestHandler("/select");
        query.set("q", "*:*");
        query.set("rows", 4000);
        try {
            this.documents = this.solrClient.query(query).getResults();
            LOG.log(Level.INFO, "Request succeeded. Documents retrieved: {0} documents", this.documents.size());

        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(KeywordsClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void prepareDocs() {
        LOG.info("Preparing tasks...");
        Client client = ClientBuilder.newClient();
        for (SolrDocument doc : this.documents) {
            this.enrichedDocuments.add(enrichDocument(doc, client));
        }
        //client.close();
        LOG.info("Tasks ready to be executed.");
    }

    private SolrInputDocument enrichDocument(SolrDocument doc, Client client) {

        Response response = client.target(KEYWORD_SERVICE_URI)
                .request(MediaType.APPLICATION_FORM_URLENCODED)
                .post(Entity.entity(new KeywordsEntity((String) doc.get("projectAbstract")), MediaType.APPLICATION_JSON));

        KeywordsResponseEntity responseEntity = response.readEntity(KeywordsResponseEntity.class);

        List<String> keywords = new ArrayList<>();
        LOG.info(responseEntity.getKeywords().toString());
        responseEntity.getKeywords()
                .stream()
                .forEach((Keyword keywordObject) -> {
                    keywords.add(keywordObject.getLemmatized());
                });

        SolrInputDocument inputDoc = new SolrInputDocument();
        doc.getFieldNames().stream().forEach((name) -> {
            inputDoc.addField(name, doc.getFieldValue(name));
        });

        inputDoc.addField("keywords_ss", keywords);

        return inputDoc;

    }

    private void sendToSolr() {
        LOG.info("Executing tasks...");
        try {
            for (SolrInputDocument doc : this.enrichedDocuments) {
                LOG.log(Level.INFO, "Executing task on thread {0}", Thread.currentThread().getName());
                LOG.info(doc.get("keywords_ss").toString());
                this.solrClient.add(doc);

                LOG.info("Executed task successfully!!");
            }
            this.solrClient.commit();
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(KeywordsClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
